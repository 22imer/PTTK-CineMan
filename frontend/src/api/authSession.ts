import type { ThongTinDangNhap } from '../types';
import { HTTPError, request } from './transport';

const KHOA_PHIEN = 'cineman-auth-session';
const KENH_PHIEN = 'cineman-auth';
const CSRF_HEADERS = { 'X-CSRF-Protection': '1' };
const CHO_XUNG_DOT_MS = 300;

export interface LoiKhoiPhuc {
  message: string;
  retryable: boolean;
  unsupported: boolean;
}

export type AuthSnapshot =
  | { status: 'dang-khoi-phuc'; generation: number }
  | { status: 'chua-dang-nhap'; generation: number }
  | { status: 'da-dang-nhap'; generation: number; phien: ThongTinDangNhap }
  | { status: 'loi-khoi-phuc'; generation: number; loi: LoiKhoiPhuc };

export class AuthUnsupportedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthUnsupportedError';
  }
}

export class SessionChangedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'SessionChangedError';
  }
}

let snapshot: AuthSnapshot = { status: 'dang-khoi-phuc', generation: 0 };
let generation = 0;
let invalidation = 0;
const listeners = new Set<() => void>();

let daKhoiTao = false;
let loiCoordination: string | null = null;
let kenh: BroadcastChannel | null = null;

let restorePromise: Promise<void> | null = null;
let canKhoiPhucLai: string | null = null;
let canDocLaiSauLogout = false;
let refreshPromise: Promise<ThongTinDangNhap> | null = null;

function phat(): void {
  for (const listener of listeners) {
    listener();
  }
}

export function getAuthSnapshot(): AuthSnapshot {
  return snapshot;
}

export function subscribeAuth(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function cho(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(), ms);
  });
}

function laLoiTamThoi(error: unknown): boolean {
  return (
    error instanceof HTTPError &&
    (error.httpStatus === 0 || error.httpStatus >= 500 || error.httpStatus === 409)
  );
}

function thongDiepLoi(error: unknown): string {
  return error instanceof Error && error.message
    ? error.message
    : 'Không thể khôi phục phiên đăng nhập.';
}

/**
 * Web Locks + BroadcastChannel là bắt buộc để tuần tự hoá login/refresh/logout và đồng bộ
 * đăng xuất/đổi tài khoản giữa các tab. Thiếu API thì báo lỗi rõ ràng, không tự race và
 * không dùng storage làm nơi giữ token.
 */
function khoiTaoCoordination(): string | null {
  if (daKhoiTao) {
    return loiCoordination;
  }
  daKhoiTao = true;

  if (typeof BroadcastChannel !== 'function') {
    loiCoordination =
      'Trình duyệt không hỗ trợ BroadcastChannel nên không thể đồng bộ phiên đăng nhập giữa các tab.';
    return loiCoordination;
  }

  const khoa = typeof navigator === 'undefined' ? undefined : navigator.locks;
  if (!khoa || typeof khoa.request !== 'function') {
    loiCoordination =
      'Trình duyệt hoặc origin hiện tại không cung cấp Web Locks (thường do không phải secure context), nên không thể đồng bộ đăng nhập/refresh giữa các tab.';
    return loiCoordination;
  }

  try {
    kenh = new BroadcastChannel(KENH_PHIEN);
    kenh.addEventListener('message', xuLyTinNhan);
  } catch {
    kenh = null;
    loiCoordination = 'Không thể mở kênh đồng bộ phiên đăng nhập giữa các tab.';
  }
  return loiCoordination;
}

async function trongLock<T>(viec: () => Promise<T>): Promise<T> {
  const loi = khoiTaoCoordination();
  if (loi) {
    throw new AuthUnsupportedError(loi);
  }
  try {
    return await navigator.locks.request(KHOA_PHIEN, { mode: 'exclusive' }, viec);
  } catch (error) {
    if (
      error instanceof AuthUnsupportedError ||
      error instanceof HTTPError ||
      error instanceof SessionChangedError
    ) {
      throw error;
    }
    if (
      error instanceof DOMException &&
      (error.name === 'SecurityError' || error.name === 'NotSupportedError')
    ) {
      throw new AuthUnsupportedError(
        'Trình duyệt từ chối Web Locks trên origin hiện tại nên không thể đồng bộ phiên đăng nhập.',
      );
    }
    if (error instanceof TypeError) {
      throw new AuthUnsupportedError(
        'Không thể dùng Web Locks trên origin hiện tại nên không thể đồng bộ phiên đăng nhập.',
      );
    }
    throw error;
  }
}

function phienHienTai(): ThongTinDangNhap | null {
  return snapshot.status === 'da-dang-nhap' ? snapshot.phien : null;
}

function phatDanhTinh(phien: ThongTinDangNhap): void {
  kenh?.postMessage({
    type: 'identity',
    sessionId: phien.sessionId,
    khachHangId: phien.khachHang.id,
  });
}

function apDungPhien(phien: ThongTinDangNhap, thongBaoTabKhac: boolean): void {
  const cu = phienHienTai();
  const thayDoi =
    cu === null ||
    cu.sessionId !== phien.sessionId ||
    cu.khachHang.id !== phien.khachHang.id;
  if (thayDoi) {
    generation += 1;
  }
  snapshot = { status: 'da-dang-nhap', generation, phien };
  phat();
  if (thayDoi && thongBaoTabKhac) {
    phatDanhTinh(phien);
  }
}

function xoaPhien(thongBaoTabKhac: boolean): void {
  const daCoPhien = snapshot.status !== 'chua-dang-nhap';
  const sessionBiXoa = snapshot.status === 'da-dang-nhap' ? snapshot.phien.sessionId : null;
  canKhoiPhucLai = null;
  canDocLaiSauLogout = false;
  generation += 1;
  invalidation += 1;
  if (!daCoPhien) {
    return;
  }
  snapshot = { status: 'chua-dang-nhap', generation };
  phat();
  if (thongBaoTabKhac && sessionBiXoa) {
    kenh?.postMessage({ type: 'logout', sessionId: sessionBiXoa });
  }
}

/** Gọi khi một request đã xác thực nhận 401 SESSION_INVALID. */
export function phienKhongHopLe(generationYeuCau: number, sessionId: string): void {
  const hienTai = phienHienTai();
  if (
    hienTai &&
    generation === generationYeuCau &&
    hienTai.sessionId === sessionId
  ) {
    xoaPhien(true);
  }
}

async function goiRefreshCoXungDot(): Promise<ThongTinDangNhap> {
  try {
    return await request<ThongTinDangNhap>('/api/auth/refresh', {
      method: 'POST',
      headers: CSRF_HEADERS,
    });
  } catch (error) {
    if (!(error instanceof HTTPError) || error.httpStatus !== 409) {
      throw error;
    }
    // Tab thắng đã đặt cookie mới; chờ cookie tới nơi rồi thử refresh đúng một lần.
    await cho(CHO_XUNG_DOT_MS);
    return request<ThongTinDangNhap>('/api/auth/refresh', {
      method: 'POST',
      headers: CSRF_HEADERS,
    });
  }
}

async function goiRefreshTrongLock(): Promise<ThongTinDangNhap> {
  return trongLock(goiRefreshCoXungDot);
}

function kiemTraCungPhien(
  generationYeuCau: number,
  sessionId: string,
): ThongTinDangNhap {
  const hienTai = phienHienTai();
  if (!hienTai || generation !== generationYeuCau || hienTai.sessionId !== sessionId) {
    throw new SessionChangedError(
      'Phiên đăng nhập đã thay đổi; yêu cầu cũ không được gửi lại dưới tài khoản mới.',
    );
  }
  return hienTai;
}

async function thucHienRefresh(
  generationYeuCau: number,
  sessionId: string,
): Promise<ThongTinDangNhap> {
  kiemTraCungPhien(generationYeuCau, sessionId);

  return trongLock(async () => {
    const truoc = kiemTraCungPhien(generationYeuCau, sessionId);

    let moi: ThongTinDangNhap;
    try {
      moi = await goiRefreshCoXungDot();
    } catch (error) {
      if (error instanceof HTTPError && error.httpStatus === 401) {
        phienKhongHopLe(generationYeuCau, sessionId);
      }
      throw error;
    }

    kiemTraCungPhien(generationYeuCau, sessionId);

    if (moi.sessionId !== sessionId || moi.khachHang.id !== truoc.khachHang.id) {
      apDungPhien(moi, true);
      throw new SessionChangedError(
        'Cookie phiên đã thuộc phiên/tài khoản khác; yêu cầu cũ không được gửi lại.',
      );
    }

    apDungPhien(moi, false);
    return moi;
  });
}

/**
 * Single-flight refresh cho một request đã nhận ACCESS_TOKEN_EXPIRED.
 * Chỉ trả token mới khi vẫn đúng generation + phiên; đổi tài khoản/phiên thì ném
 * SessionChangedError để caller không replay request nghiệp vụ.
 */
export async function refreshChoYeuCau(
  generationYeuCau: number,
  sessionId: string,
  tokenDaDung: string,
): Promise<string> {
  const loi = khoiTaoCoordination();
  if (loi) {
    throw new AuthUnsupportedError(loi);
  }

  const hienTai = kiemTraCungPhien(generationYeuCau, sessionId);
  if (hienTai.accessToken !== tokenDaDung) {
    return hienTai.accessToken;
  }

  if (!refreshPromise) {
    const p = thucHienRefresh(generationYeuCau, sessionId);
    refreshPromise = p;
    const giaiPhong = () => {
      if (refreshPromise === p) {
        refreshPromise = null;
      }
    };
    void p.then(giaiPhong, giaiPhong);
  }

  const dangCho = refreshPromise;
  if (dangCho) {
    await dangCho;
  }
  return kiemTraCungPhien(generationYeuCau, sessionId).accessToken;
}

export async function dangNhap(
  username: string,
  password: string,
): Promise<ThongTinDangNhap> {
  const loi = khoiTaoCoordination();
  if (loi) {
    throw new AuthUnsupportedError(loi);
  }

  return trongLock(async () => {
    const moi = await request<ThongTinDangNhap>('/api/auth/login', {
      method: 'POST',
      headers: CSRF_HEADERS,
      body: JSON.stringify({ username, password }),
    });

    // Login đã commit cookie/server trong lock: áp dụng và phát danh tính ngay cho các tab khác.
    // Vì Web Lock giữ đến hết callback, đây là phiên mới nhất tại thời điểm commit; không phụ thuộc
    // vào thông báo BroadcastChannel (có thể tới trễ) để quyết định có công bố hay không.
    phatDanhTinh(moi);
    apDungPhien(moi, false);
    return moi;
  });
}

/** Đồng bộ lại từ cookie hiện tại, không xóa một phiên thay thế mà tab này chưa biết. */
function dongBoLaiTuCookie(): void {
  canDocLaiSauLogout = true;
  void batDauKhoiPhuc();
}

/**
 * Chỉ báo đăng xuất thành công sau khi server xác nhận; lỗi mạng giữ nguyên phiên để thử lại.
 * Ý định đăng xuất được bind vào sessionId tại thời điểm gọi, gửi qua `X-Auth-Session` để backend
 * không thu hồi nhầm cookie của một phiên mới hơn đã thay thế trong lúc chờ Web Lock.
 */
export async function dangXuat(): Promise<void> {
  const loi = khoiTaoCoordination();
  if (loi) {
    throw new AuthUnsupportedError(loi);
  }

  const phienLucYeuCau = phienHienTai();
  if (!phienLucYeuCau) {
    throw new SessionChangedError('Không có phiên đăng nhập hiện tại để đăng xuất.');
  }
  const sessionId = phienLucYeuCau.sessionId;
  const headers = { ...CSRF_HEADERS, 'X-Auth-Session': sessionId };

  try {
    await trongLock(() =>
      request<unknown>('/api/auth/logout', {
        method: 'POST',
        headers,
      }),
    );
  } catch (error) {
    if (error instanceof HTTPError && error.httpStatus === 409) {
      dongBoLaiTuCookie();
    }
    throw error;
  }

  const hienTai = phienHienTai();
  if (hienTai && hienTai.sessionId === sessionId) {
    xoaPhien(true);
    return;
  }

  dongBoLaiTuCookie();
  throw new SessionChangedError(
    'Phiên đăng nhập đã thay đổi trong lúc đăng xuất; đã đồng bộ lại phiên hiện tại.',
  );
}

function tiepTucKhoiPhucNeuCan(): void {
  const canDocLai = canDocLaiSauLogout;
  canDocLaiSauLogout = false;
  if (canDocLai) {
    void batDauKhoiPhuc();
    return;
  }

  const mucTieu = canKhoiPhucLai;
  if (!mucTieu) {
    return;
  }
  canKhoiPhucLai = null;
  const hienTai = phienHienTai();
  if (hienTai && hienTai.sessionId === mucTieu) {
    return;
  }
  void batDauKhoiPhuc();
}

async function thucHienKhoiPhuc(lan: number): Promise<void> {
  try {
    const phien = await goiRefreshTrongLock();
    if (lan !== invalidation) {
      return;
    }
    apDungPhien(phien, false);
  } catch (error) {
    if (lan !== invalidation) {
      return;
    }
    if (error instanceof HTTPError && error.httpStatus === 401) {
      xoaPhien(false);
      return;
    }
    const unsupported = error instanceof AuthUnsupportedError;
    snapshot = {
      status: 'loi-khoi-phuc',
      generation,
      loi: {
        message: thongDiepLoi(error),
        retryable: !unsupported && laLoiTamThoi(error),
        unsupported,
      },
    };
    phat();
  }
}

function batDauKhoiPhuc(): Promise<void> {
  if (restorePromise) {
    return restorePromise;
  }

  const lan = invalidation;
  canKhoiPhucLai = null;
  canDocLaiSauLogout = false;
  snapshot = { status: 'dang-khoi-phuc', generation };
  phat();

  const p = thucHienKhoiPhuc(lan);
  restorePromise = p;
  const giaiPhong = () => {
    if (restorePromise === p) {
      restorePromise = null;
      tiepTucKhoiPhucNeuCan();
    }
  };
  void p.then(giaiPhong, giaiPhong);
  return p;
}

/** Khôi phục phiên từ cookie refresh lúc mở trang; gọi lại được khi lỗi mạng. */
export function khoiPhucPhien(): Promise<void> {
  const loi = khoiTaoCoordination();
  if (loi) {
    if (snapshot.status !== 'loi-khoi-phuc') {
      snapshot = {
        status: 'loi-khoi-phuc',
        generation,
        loi: { message: loi, retryable: false, unsupported: true },
      };
      phat();
    }
    return Promise.resolve();
  }

  if (snapshot.status === 'da-dang-nhap') {
    return Promise.resolve();
  }
  return batDauKhoiPhuc();
}

function xuLyTinNhan(event: MessageEvent): void {
  const duLieu: unknown = event.data;
  if (typeof duLieu !== 'object' || duLieu === null || !('type' in duLieu)) {
    return;
  }

  if (duLieu.type === 'logout') {
    if (!('sessionId' in duLieu) || typeof duLieu.sessionId !== 'string') {
      return;
    }
    const sessionIdBiThuHoi = duLieu.sessionId;

    const hienTai = phienHienTai();
    if (hienTai) {
      if (hienTai.sessionId === sessionIdBiThuHoi) {
        xoaPhien(false);
      }
      return;
    }

    if (restorePromise) {
      // Không xóa một phiên thay thế chưa biết: đọc lại cookie sau khi restore hiện tại kết thúc.
      if (canDocLaiSauLogout) {
        return;
      }
      canDocLaiSauLogout = true;
      invalidation += 1;
      if (snapshot.status !== 'dang-khoi-phuc') {
        snapshot = { status: 'dang-khoi-phuc', generation };
        phat();
      }
      return;
    }

    if (snapshot.status === 'dang-khoi-phuc') {
      canDocLaiSauLogout = true;
      void batDauKhoiPhuc();
    }
    return;
  }

  if (
    duLieu.type !== 'identity' ||
    !('sessionId' in duLieu) ||
    !('khachHangId' in duLieu)
  ) {
    return;
  }
  const sessionId = duLieu.sessionId;
  const khachHangId = duLieu.khachHangId;
  if (
    typeof sessionId !== 'string' ||
    typeof khachHangId !== 'number' ||
    !Number.isInteger(khachHangId)
  ) {
    return;
  }

  const hienTai = phienHienTai();
  if (
    hienTai &&
    hienTai.sessionId === sessionId &&
    hienTai.khachHang.id === khachHangId
  ) {
    return;
  }

  if (restorePromise) {
    if (canKhoiPhucLai === sessionId) {
      return;
    }
    canKhoiPhucLai = sessionId;
    invalidation += 1;
    if (snapshot.status !== 'dang-khoi-phuc') {
      snapshot = { status: 'dang-khoi-phuc', generation };
      phat();
    }
    return;
  }

  invalidation += 1;
  canKhoiPhucLai = null;
  snapshot = { status: 'dang-khoi-phuc', generation };
  phat();
  void khoiPhucPhien();
}
