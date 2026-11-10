interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string;
}

const NETWORK_ERROR = 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.';

/** Lỗi HTTP/network có mã trạng thái (và mã lỗi envelope) để UI xử lý nhánh xác thực. */
export class HTTPError extends Error {
  readonly httpStatus: number;
  readonly code: string | undefined;

  constructor(message: string, httpStatus: number, code?: string) {
    super(message);
    this.name = 'HTTPError';
    this.httpStatus = httpStatus;
    this.code = code;
  }
}

function serverError(httpStatus: number, statusText: string): string {
  const status = `${httpStatus} ${statusText}`.trim();
  return `Lỗi máy chủ (HTTP ${status}).`;
}

function maLoi(payload: object): string | undefined {
  if ('code' in payload && typeof payload.code === 'string') {
    return payload.code;
  }
  return undefined;
}

/**
 * Ranh giới mạng duy nhất tới backend: luôn gọi path tương đối `/api/...`, gửi kèm cookie
 * refresh (HttpOnly, Path=/api/auth) và giải envelope `{success,data,message,code?}`.
 * Mọi lỗi — HTTP, validation, mạng, phản hồi không hợp lệ — đều ném `HTTPError`.
 */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
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
    const message =
      typeof envelope.message === 'string' && envelope.message
        ? envelope.message
        : serverError(response.status, response.statusText);
    throw new HTTPError(message, response.status, maLoi(payload));
  }

  return envelope.data as T;
}
