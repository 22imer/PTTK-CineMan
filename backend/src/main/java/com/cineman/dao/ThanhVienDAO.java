package com.cineman.dao;

import com.cineman.entity.KhachHang;
import com.cineman.entity.ThanhVien;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

@Repository
public class ThanhVienDAO extends DAO {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /** Số điện thoại Việt Nam: 10 chữ số, bắt đầu bằng 0 (VD 0912345678). */
    private static final Pattern SDT = Pattern.compile("^0\\d{9}$");
    private static final int MAT_KHAU_TOI_THIEU = 8;
    /** BCrypt chỉ xét 72 byte đầu của mật khẩu → chặn sớm để hai mật khẩu khác nhau không thể trùng nhau. */
    private static final int MAT_KHAU_TOI_DA_BYTE = 72;
    // Giới hạn độ dài cột schema.sql (VARCHAR) — vượt quá phải trả 400, không để DB ném lỗi.
    private static final int USERNAME_TOI_DA = 50;
    private static final int HO_TEN_TOI_DA = 100;
    private static final int GIOI_TINH_TOI_DA = 100;
    private static final int DIA_CHI_TOI_DA = 100;
    private static final int EMAIL_TOI_DA = 100;
    private static final int SDT_TOI_DA = 100;

    private final PasswordEncoder passwordEncoder;

    public ThanhVienDAO(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        super(jdbcTemplate);
        this.passwordEncoder = passwordEncoder;
    }

    /** R8 + giới hạn độ dài cột schema: đủ trường bắt buộc, email/sđt đúng định dạng, mật khẩu không toàn khoảng trắng và 8–72 byte (khớp nhập lại kiểm ở controller). */
    public boolean kiemTraThongTin(KhachHang kh) {
        return coGiaTri(kh.getUsername()) && kh.getUsername().length() <= USERNAME_TOI_DA
                && matKhauHopLe(kh.getPassword())
                && coGiaTri(kh.getHoTen()) && kh.getHoTen().length() <= HO_TEN_TOI_DA
                && kh.getNgaySinh() != null
                && coGiaTri(kh.getGioiTinh()) && kh.getGioiTinh().length() <= GIOI_TINH_TOI_DA
                && coGiaTri(kh.getDiaChi()) && kh.getDiaChi().length() <= DIA_CHI_TOI_DA
                && kh.getEmail() != null && kh.getEmail().length() <= EMAIL_TOI_DA
                && EMAIL.matcher(kh.getEmail()).matches()
                && kh.getSdt() != null && kh.getSdt().length() <= SDT_TOI_DA
                && SDT.matcher(kh.getSdt()).matches();
    }

    /** Mật khẩu toàn khoảng trắng không phải mật khẩu hợp lệ (đồng nhất với điều kiện đăng nhập). */
    private static boolean matKhauHopLe(String matKhau) {
        return matKhau != null
                && !matKhau.isBlank()
                && matKhau.length() >= MAT_KHAU_TOI_THIEU
                && matKhau.getBytes(StandardCharsets.UTF_8).length <= MAT_KHAU_TOI_DA_BYTE;
    }

    /** {@code true} khi email và số điện thoại chưa thuộc thành viên nào. */
    public boolean kiemTraEmailSDT(String email, String sdt) {
        Integer dem = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tblThanhVien WHERE email = ? OR sdt = ?", Integer.class, email, sdt);
        return dem != null && dem == 0;
    }

    /**
     * Đúng username + mật khẩu (BCrypt) của một khách hàng → nạp thông tin vào {@code tv} và trả {@code true}.
     * Nếu {@code tv} là {@link KhachHang} thì nạp cả điểm/hạng. Mật khẩu thô trên {@code tv} bị xóa sau kiểm tra.
     */
    public boolean kiemTraDangNhap(ThanhVien tv) {
        String matKhau = tv.getPassword();
        tv.setPassword(null);
        if (tv.getUsername() == null || matKhau == null) {
            return false;
        }
        List<Boolean> ketQua = jdbcTemplate.query("""
                SELECT tv.id, tv.password, tv.hoTen, tv.ngaySinh, tv.gioiTinh, tv.diaChi, tv.email, tv.sdt, tv.vaiTro,
                       kh.ngayDangKy, kh.tongDiem, kh.hangThanhVien
                FROM tblThanhVien tv
                JOIN tblKhachHang kh ON kh.tblThanhVienId = tv.id
                WHERE tv.username = ?""", (rs, i) -> {
                    if (!passwordEncoder.matches(matKhau, rs.getString("password"))) {
                        return false;
                    }
                    tv.setId(rs.getInt("id"));
                    tv.setHoTen(rs.getString("hoTen"));
                    tv.setNgaySinh(rs.getDate("ngaySinh").toLocalDate());
                    tv.setGioiTinh(rs.getString("gioiTinh"));
                    tv.setDiaChi(rs.getString("diaChi"));
                    tv.setEmail(rs.getString("email"));
                    tv.setSdt(rs.getString("sdt"));
                    tv.setVaiTro(rs.getString("vaiTro"));
                    if (tv instanceof KhachHang kh) {
                        kh.setNgayDangKy(rs.getDate("ngayDangKy").toLocalDate());
                        kh.setTongDiem(rs.getInt("tongDiem"));
                        kh.setHangThanhVien(rs.getString("hangThanhVien"));
                    }
                    return true;
                }, tv.getUsername());
        return ketQua.size() == 1 && ketQua.get(0);
    }

    private static boolean coGiaTri(String s) {
        return s != null && !s.isBlank();
    }
}
