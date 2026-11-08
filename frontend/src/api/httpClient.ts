interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
}

const NETWORK_ERROR = 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.';

function serverError(httpStatus: number, statusText: string): string {
  const status = `${httpStatus} ${statusText}`.trim();
  return `Lỗi máy chủ (HTTP ${status}).`;
}

/**
 * Cầu nối duy nhất FE → 3 @RestController (ARCHITECTURE.md §7).
 * Luôn gọi path tương đối `/api/...` để cookie HttpSession đi qua Vite proxy.
 */
export class HTTPClient {
  /**
   * Trả về `data` của envelope {success,data,message}.
   * Mọi lỗi — HTTP, validation, mạng, phản hồi không hợp lệ — đều ném `Error(message)` cho UI hiển thị.
   */
  static async fetch<T>(path: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }
    if (typeof init?.body === 'string' && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    let response: Response;
    try {
      response = await fetch(path, {
        ...init,
        headers,
        credentials: 'include',
      });
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      throw new Error(`${NETWORK_ERROR} (${reason})`);
    }

    let rawBody: string;
    try {
      rawBody = await response.text();
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      throw new Error(`${NETWORK_ERROR} (${reason})`);
    }

    if (rawBody === '') {
      throw new Error(
        response.ok
          ? `Máy chủ trả về phản hồi rỗng (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
      );
    }

    let payload: unknown;
    try {
      payload = JSON.parse(rawBody);
    } catch {
      throw new Error(
        response.ok
          ? `Máy chủ trả về dữ liệu không phải JSON (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
      );
    }

    if (typeof payload !== 'object' || payload === null) {
      throw new Error(
        response.ok
          ? `Máy chủ trả về dữ liệu không đúng định dạng (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
      );
    }

    const envelope = payload as ApiEnvelope<T>;
    if (!response.ok || envelope.success !== true) {
      throw new Error(envelope.message || serverError(response.status, response.statusText));
    }

    return envelope.data as T;
  }
}

export default HTTPClient;
