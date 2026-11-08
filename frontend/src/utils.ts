import type { Ghe, Ve } from './types';

/** Lấy thông điệp lỗi envelope/network do HTTPClient ném ra, fallback an toàn cho UI. */
export function thongDiepLoi(error: unknown, macDinh: string): string {
  return error instanceof Error && error.message ? error.message : macDinh;
}

/** 90000 → "90.000 đ". */
export function dinhDangTien(soTien: number): string {
  const giaTri = Number.isFinite(soTien) ? soTien : 0;
  return `${giaTri.toLocaleString('vi-VN')} đ`;
}

/** "2026-09-27" → "27/09/2026"; giữ nguyên nếu không khớp định dạng ISO. */
export function dinhDangNgay(ngayIso: string | null | undefined): string {
  if (!ngayIso) {
    return '—';
  }
  const [nam, thang, ngay] = ngayIso.split('-');
  if (!nam || !thang || !ngay) {
    return ngayIso;
  }
  return `${ngay}/${thang}/${nam}`;
}

/** "HH:mm:ss" → "HH:mm". */
export function dinhDangGio(gio: string | null | undefined): string {
  if (!gio) {
    return '—';
  }
  const [gioBatDau, phut] = gio.split(':');
  return gioBatDau && phut ? `${gioBatDau}:${phut}` : gio;
}

/** LocalDateTime "YYYY-MM-DDTHH:mm:ss" → "dd/mm/yyyy HH:mm". */
export function dinhDangNgayGio(thoiDiem: string | null | undefined): string {
  if (!thoiDiem) {
    return '—';
  }
  const [phanNgay, phanGio] = thoiDiem.split('T');
  if (!phanNgay || !phanGio) {
    return thoiDiem;
  }
  return `${dinhDangNgay(phanNgay)} ${dinhDangGio(phanGio)}`;
}

/** Cộng số ngày vào chuỗi ISO YYYY-MM-DD (tính theo lịch địa phương). */
export function congNgay(ngayIso: string, soNgay: number): string {
  const [nam, thang, ngay] = ngayIso.split('-').map(Number);
  if (!nam || !thang || !ngay) {
    return ngayIso;
  }
  const moc = new Date(nam, thang - 1, ngay + soNgay);
  const thangMoi = String(moc.getMonth() + 1).padStart(2, '0');
  const ngayMoi = String(moc.getDate()).padStart(2, '0');
  return `${moc.getFullYear()}-${thangMoi}-${ngayMoi}`;
}

/** Hạn voucher = ngày nhận + 30 ngày (R2), hiển thị dd/mm/yyyy. */
export function hanVoucher(ngayNhan: string | null | undefined): string {
  if (!ngayNhan) {
    return '—';
  }
  return dinhDangNgay(congNgay(ngayNhan, 30));
}

export function laSweetbox(ghe: Ghe): boolean {
  const tenLoai = (ghe.loaiGhe?.tenLoai || '').toLowerCase();
  return tenLoai.includes('sweetbox') || tenLoai.includes('đôi') || tenLoai.includes('doi');
}

/** Nhãn ghế hiển thị, ví dụ "A3". */
export function nhanGhe(ghe: Ghe): string {
  return `${ghe.hangGhe}${ghe.soGhe}`;
}

/** Mã vé hiển thị/QR — backend chỉ trả id vé, không có cột mã riêng. */
export function maVe(ve: Ve): string {
  return `VE-${String(ve.id).padStart(4, '0')}`;
}

/** Mã hiển thị = tiền tố + đầu số id (ARCHITECTURE §3): PH01, SC03, HD001, PT02, PDT-001. */
export function maPhim(id: number): string {
  return `PH${String(id).padStart(2, '0')}`;
}

export function maSuatChieu(id: number): string {
  return `SC${String(id).padStart(2, '0')}`;
}

export function maHoaDon(id: number): string {
  return `HD${String(id).padStart(3, '0')}`;
}

export function maQuaTang(id: number): string {
  return `PT${String(id).padStart(2, '0')}`;
}

/** Mã phiếu đổi điểm hiển thị, ví dụ "PDT-002". */
export function maPhieuDoiDiem(id: number): string {
  return `PDT-${String(id).padStart(3, '0')}`;
}

/** "DANG_GIU" → "Đang giữ", ... dùng cho nhãn trạng thái dễ đọc. */
export function nhanTrangThai(trangThai: string): string {
  const nhan: Record<string, string> = {
    CHO_THANH_TOAN: 'Chờ thanh toán',
    DA_THANH_TOAN: 'Đã thanh toán',
    THAT_BAI: 'Thanh toán thất bại',
    DA_HUY: 'Đã hủy',
    HET_HAN: 'Hết hạn',
    DANG_GIU: 'Đang giữ',
    DA_BAN: 'Đã bán',
    DA_SU_DUNG: 'Đã sử dụng',
    CHUA_SU_DUNG: 'Chưa sử dụng',
    MO_BAN: 'Mở bán',
  };
  return nhan[trangThai] || trangThai;
}

/** mm:ss cho đồng hồ giữ ghế. */
export function dinhDangDemNguoc(soGiay: number): string {
  const tong = Math.max(0, Math.floor(soGiay));
  const phut = Math.floor(tong / 60);
  const giay = tong % 60;
  return `${String(phut).padStart(2, '0')}:${String(giay).padStart(2, '0')}`;
}
