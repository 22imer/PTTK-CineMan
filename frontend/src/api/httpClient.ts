import {
  getAuthSnapshot,
  phienKhongHopLe,
  refreshChoYeuCau,
  SessionChangedError,
} from './authSession';
import { HTTPError, request } from './transport';

export { HTTPError } from './transport';

interface PhienYeuCau {
  accessToken: string;
  generation: number;
  sessionId: string;
}

const AUTH_KHONG_REFRESH: Record<string, true> = {
  '/api/auth/login': true,
  '/api/auth/refresh': true,
  '/api/auth/logout': true,
  '/api/auth/register': true,
};

function choPhepRefresh(path: string): boolean {
  return AUTH_KHONG_REFRESH[path.split('?')[0]] !== true;
}

function guiTokenCho(path: string): boolean {
  const duongDan = path.split('?')[0];
  return (
    duongDan !== '/api/auth/login' &&
    duongDan !== '/api/auth/refresh' &&
    duongDan !== '/api/auth/register'
  );
}

function themAuthorization(
  path: string,
  headersInit: HeadersInit | undefined,
  accessToken: string | null,
): Headers {
  const headers = new Headers(headersInit);
  if (accessToken && guiTokenCho(path) && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return headers;
}

/** Phản hồi của tài khoản/phiên cũ không được trả về cho caller sau khi phiên đã đổi. */
function baoVeKetQuaPhien<T>(phienYeuCau: PhienYeuCau, ketQua: T): T {
  const hienTai = getAuthSnapshot();
  if (
    hienTai.status !== 'da-dang-nhap' ||
    hienTai.generation !== phienYeuCau.generation ||
    hienTai.phien.sessionId !== phienYeuCau.sessionId
  ) {
    throw new SessionChangedError(
      'Phiên đăng nhập đã thay đổi trong lúc chờ phản hồi; bỏ qua kết quả của tài khoản cũ.',
    );
  }
  return ketQua;
}

/**
 * Cầu nối duy nhất FE → 3 @RestController (ARCHITECTURE.md §7).
 * Luôn gọi path tương đối `/api/...` để Vite proxy chuyển tiếp cùng origin; cookie refresh
 * HttpOnly (Path=/api/auth) do trình duyệt gửi kèm, còn access JWT chỉ nằm trong bộ nhớ
 * của `authSession` và được gắn thành Bearer cho từng request.
 */
export class HTTPClient {
  /**
   * Trả về `data` của envelope {success,data,message,code?}.
   * Mọi lỗi — HTTP, validation, mạng, phản hồi không hợp lệ — đều ném `HTTPError`
   * (kế thừa `Error`, kèm `httpStatus`/`code`) cho UI hiển thị và xử lý theo mã.
   *
   * Access hết hạn: refresh một lần rồi chỉ gửi lại đúng một lần nếu vẫn cùng
   * phiên/tài khoản/generation. Không retry lỗi mạng/5xx và không replay sau đổi tài khoản.
   */
  static async fetch<T>(path: string, init?: RequestInit): Promise<T> {
    const auth = getAuthSnapshot();
    const phien = auth.status === 'da-dang-nhap' ? auth.phien : null;
    const chuaDangNhapLucGui = auth.status === 'chua-dang-nhap';
    const phienYeuCau = phien
      ? {
          accessToken: phien.accessToken,
          generation: auth.generation,
          sessionId: phien.sessionId,
        }
      : null;

    try {
      const ketQua = await request<T>(path, {
        ...init,
        headers: themAuthorization(path, init?.headers, phienYeuCau?.accessToken ?? null),
      });
      if (phienYeuCau && guiTokenCho(path)) {
        return baoVeKetQuaPhien(phienYeuCau, ketQua);
      }
      if (chuaDangNhapLucGui && getAuthSnapshot().status !== 'chua-dang-nhap') {
        throw new SessionChangedError(
          'Trạng thái đăng nhập đã thay đổi trong lúc chờ phản hồi; bỏ qua kết quả cũ.',
        );
      }
      return ketQua;
    } catch (error) {
      if (!(error instanceof HTTPError)) {
        throw error;
      }

      if (error.httpStatus === 401 && error.code === 'SESSION_INVALID') {
        if (phienYeuCau) {
          phienKhongHopLe(phienYeuCau.generation, phienYeuCau.sessionId);
        }
        throw error;
      }

      if (
        error.httpStatus !== 401 ||
        error.code !== 'ACCESS_TOKEN_EXPIRED' ||
        !phienYeuCau ||
        !choPhepRefresh(path)
      ) {
        throw error;
      }

      const tokenMoi = await refreshChoYeuCau(
        phienYeuCau.generation,
        phienYeuCau.sessionId,
        phienYeuCau.accessToken,
      );

      try {
        const ketQua = await request<T>(path, {
          ...init,
          headers: themAuthorization(path, init?.headers, tokenMoi),
        });
        return baoVeKetQuaPhien(phienYeuCau, ketQua);
      } catch (loiSau) {
        if (
          loiSau instanceof HTTPError &&
          loiSau.httpStatus === 401 &&
          loiSau.code === 'SESSION_INVALID'
        ) {
          phienKhongHopLe(phienYeuCau.generation, phienYeuCau.sessionId);
        }
        throw loiSau;
      }
    }
  }
}

export default HTTPClient;
