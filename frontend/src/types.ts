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
