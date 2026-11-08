/**
 * Hồ sơ khách hàng do backend trả về sau khi đăng ký/đăng nhập.
 * Không chứa mật khẩu hay hash — server không bao giờ trả hai trường đó.
 */
export interface KhachHang {
  id: number;
  username: string;
  hoTen: string;
  /** Chuỗi ISO YYYY-MM-DD (LocalDate của backend). */
  ngaySinh: string;
  gioiTinh: string;
  diaChi: string;
  email: string;
  sdt: string;
  vaiTro: string;
  /** Chuỗi ISO YYYY-MM-DD (LocalDate của backend). */
  ngayDangKy: string;
  tongDiem: number;
  hangThanhVien: string | null;
}

/* ------------------------------------------------------------ đặt vé (wire contract) */

/** Phim — GET /api/phim. `doTuoi` chỉ hiển thị (R10). */
export interface Phim {
  id: number;
  tenPhim: string;
  theLoai: string;
  daoDien: string;
  thoiLuong: number;
  doTuoi: string;
  moTa: string | null;
  ngayKhoiChieu: string;
  ngayKetThuc: string;
}

/** Khung giờ — LocalTime của backend dạng "HH:mm:ss". */
export interface KhungGio {
  id: number;
  gioBatDau: string;
  gioKetThuc: string;
}

export interface LoaiGhe {
  id: number;
  tenLoai: string;
  phuThu: number;
}

export interface Ghe {
  id: number;
  soGhe: string;
  hangGhe: string;
  loaiGhe: LoaiGhe;
}

export interface PhongChieu {
  id: number;
  tenPhong: string;
  loaiPhong: string;
  sucChua: number;
  dsGhe?: Ghe[];
}

export interface SuatChieu {
  id: number;
  ngayChieu: string;
  giaCoSo: number;
  trangThai: string;
  phim: Phim;
  khungGio: KhungGio;
  phongChieu: PhongChieu;
}

export interface Ve {
  id: number;
  giaVe: number;
  /** DANG_GIU | DA_BAN | DA_HUY */
  trangThaiVe: string;
  suatChieu: SuatChieu;
  ghe: Ghe;
}

export interface VoucherKhachHang {
  id: number;
  maVoucher: string;
  ngayNhan: string;
  ngaySuDung: string | null;
  /** CHUA_SU_DUNG | DA_SU_DUNG */
  trangThai: string;
  giaoDichDoiDiem: GiaoDichDoiDiem;
}

export interface HoaDon {
  id: number;
  ngayTao: string;
  hetHan: string;
  /** CHO_THANH_TOAN | DA_THANH_TOAN | THAT_BAI | DA_HUY | HET_HAN */
  trangThai: string;
  khachHang?: KhachHang;
  dsVe: Ve[];
  voucher?: VoucherKhachHang | null;
}

/** Tóm tắt hóa đơn dùng chung cho tạo/áp voucher/thanh toán/hủy. */
export interface TomTatHoaDon {
  hoaDon: HoaDon;
  tongTienTruocVoucher: number;
  tongTien: number;
  diemCong: number;
  tongDiem: number;
  /** LocalDateTime của server, cùng múi giờ với `hoaDon.hetHan`. */
  serverNow: string;
}

/** GET /api/ghe?suatChieuId= */
export interface ThongTinGhe {
  dsGhe: Ghe[];
  dsVe: Ve[];
  suatChieu: SuatChieu;
}

/* ------------------------------------------------------------ đổi điểm (wire contract) */

/** Quà tặng — single-table, phân biệt bằng `loaiQua`. */
export interface QuaTang {
  id: number;
  tenQua: string;
  /** GIAM_GIA | VE_PHIM | BAP_NUOC */
  loaiQua: string;
  soDiem: number;
  soLuong: number;
  moTa: string | null;
  /** Chỉ có với GIAM_GIA. */
  giaTriGiam?: number | null;
}

export interface GiaoDichDoiDiem {
  id: number;
  thoiGian: string;
  soDiemTru: number;
  diemConLai: number;
  khachHang: KhachHang;
  quaTang: QuaTang;
}

/** GET /api/quatang */
export interface DanhSachQuaTang {
  dsQuaTang: QuaTang[];
  tongDiem: number;
}
