import type { KhachHang } from '../types';
import type { DoiDiemUI, GheUI, HoaDonUI, KhungGioUI, PhimUI, QuaTangUI, SuatChieuUI } from './models';

// Chỉ dùng ở catalogue UI; không gửi những giá trị này đến API.
export const khachMau: KhachHang = {
  id: 1, username: 'annguyen2002', hoTen: 'Nguyễn Văn A', ngaySinh: '2002-08-15', gioiTinh: 'Nam',
  diaChi: '96 Trần Phú, Hà Đông, Hà Nội', email: 'an.nguyen@gmail.com', sdt: '0912345678',
  vaiTro: 'KHACH_HANG', ngayDangKy: '2026-09-25', tongDiem: 250, hangThanhVien: null,
};
export const phimMau: PhimUI[] = [
  { id: 1, ma: 'PH01', ten: 'Mai', theLoai: 'Tâm lý, Tình cảm', thoiLuong: 131, doTuoi: 'C18' },
  { id: 2, ma: 'PH02', ten: 'Đào, Phở và Piano', theLoai: 'Lịch sử, Chiến tranh', thoiLuong: 100, doTuoi: 'C13' },
  { id: 3, ma: 'PH03', ten: 'Kung Fu Panda 4', theLoai: 'Hoạt hình, Hài', thoiLuong: 94, doTuoi: 'P' },
];
export const khungGioMau: KhungGioUI[] = [
  { id: 1, batDau: '09:00', ketThuc: '11:11' },
  { id: 2, batDau: '14:00', ketThuc: '16:11' },
  { id: 3, batDau: '19:00', ketThuc: '21:11' },
];
export const suatMau: SuatChieuUI = {
  id: 3, ma: 'SC03', phim: phimMau[0], ngay: '27/09/2026', khungGio: khungGioMau[2],
  phong: 'Phòng 1', dinhDang: '2D', giaCoSo: 90000,
};
export const gheMau: GheUI[] = ['A', 'B', 'C', 'D', 'E'].flatMap((hang, row) =>
  Array.from({ length: 8 }, (_, index) => {
    const so = index + 1;
    const vip = hang === 'B' || hang === 'C';
    const sweetbox = hang === 'E';
    const phuThu = sweetbox ? 50000 : vip ? 30000 : 0;
    return {
      id: row * 8 + so, hang, so, loai: sweetbox ? 'Sweetbox' : vip ? 'Ghế VIP' : 'Ghế Thường',
      phuThu, donGia: 90000 + phuThu,
      trangThai: (hang === 'C' && so < 3 ? 'DA_BAN' : hang === 'D' && so > 6 ? 'DANG_GIU' : 'TRONG'),
      dangChon: false, cap: sweetbox ? `E${Math.ceil(so / 2)}` : undefined,
    };
  }),
);
export const gheDaChon = gheMau.map((ghe) => ({ ...ghe, dangChon: (ghe.hang === 'A' || ghe.hang === 'B') && ghe.so === 3 }));
export const hoaDonMau: HoaDonUI = {
  ma: 'HD001', khachHang: khachMau.hoTen, suatChieu: suatMau,
  dsVe: gheDaChon.filter((ghe) => ghe.dangChon), trangThai: 'CHO_THANH_TOAN', thoiGian: '12:03 25/09/2026',
  conLai: '04:32', tongTien: 210000, giamGia: 0, thanhToan: 210000, diemDuKien: 10,
  tongDiemSau: 160, maVe: ['VE001', 'VE002'],
};
export const hoaDonVoucher: HoaDonUI = {
  ...hoaDonMau, maVoucher: 'VC-VIDU-12345', moTaVoucher: 'Miễn vé A3', giamGia: 90000, thanhToan: 120000,
};
export const quaMau: QuaTangUI[] = [
  { id: 1, ma: 'PT01', ten: 'Bắp rang bơ phô mai', loai: 'Bắp nước', diem: 50, tonKho: 120, moTa: 'Bắp ngọt cỡ vừa phủ phô mai' },
  { id: 2, ma: 'PT02', ten: 'Vé xem phim 2D Standard', loai: 'Vé phim', diem: 100, tonKho: 45, moTa: 'Áp dụng xem phim 2D tất cả các ngày' },
  { id: 3, ma: 'PT03', ten: 'Combo (1 Bắp + 1 Nước)', loai: 'Bắp nước', diem: 80, tonKho: 60, moTa: '1 bắp vừa + 1 nước ngọt 22oz' },
  { id: 4, ma: 'PT04', ten: 'Voucher giảm giá 50.000đ', loai: 'Voucher', diem: 60, tonKho: 0, moTa: 'Giảm trực tiếp trên hóa đơn vé' },
];
export const doiDiemMau: DoiDiemUI = {
  maPhieu: 'PDT-001', khachHang: khachMau.hoTen, thoiGian: '11:03 25/09/2026', qua: quaMau[1],
  diemTruoc: 250, diemTru: 100, diemSau: 150, maVoucher: 'VC-VIDU-12345', hanSuDung: '30 ngày kể từ ngày đổi điểm',
};
