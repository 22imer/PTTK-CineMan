interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
}

const NETWORK_ERROR = 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.';

/** Lỗi HTTP/network có mã trạng thái để UI xử lý các nhánh nghiệp vụ (409 hết hạn, 401 phiên...). */
export class HTTPError extends Error {
  readonly httpStatus: number;

  constructor(message: string, httpStatus: number) {
    super(message);
    this.name = 'HTTPError';
    this.httpStatus = httpStatus;
  }
}

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
   * Mọi lỗi — HTTP, validation, mạng, phản hồi không hợp lệ — đều ném `HTTPError`
   * (kế thừa `Error`, kèm `httpStatus`) cho UI hiển thị và xử lý theo mã trạng thái.
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
      throw new HTTPError(`${NETWORK_ERROR} (${reason})`, 0);
    }

    let rawBody: string;
    try {
      rawBody = await response.text();
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      throw new HTTPError(`${NETWORK_ERROR} (${reason})`, 0);
    }

    if (rawBody === '') {
      throw new HTTPError(
        response.ok
          ? `Máy chủ trả về phản hồi rỗng (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
        response.status,
      );
    }

    let payload: unknown;
    try {
      payload = JSON.parse(rawBody);
    } catch {
      throw new HTTPError(
        response.ok
          ? `Máy chủ trả về dữ liệu không phải JSON (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
        response.status,
      );
    }

    if (typeof payload !== 'object' || payload === null) {
      throw new HTTPError(
        response.ok
          ? `Máy chủ trả về dữ liệu không đúng định dạng (HTTP ${response.status}).`
          : serverError(response.status, response.statusText),
        response.status,
      );
    }

    const envelope = payload as ApiEnvelope<T>;
    if (!response.ok || envelope.success !== true) {
      throw new HTTPError(
        envelope.message || serverError(response.status, response.statusText),
        response.status,
      );
    }

    return envelope.data as T;
  }
}

export default HTTPClient;
