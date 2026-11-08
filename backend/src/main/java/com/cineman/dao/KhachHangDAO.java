package com.cineman.dao;

import com.cineman.entity.KhachHang;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KhachHangDAO extends DAO {

    public static final String VAI_TRO_KHACH_HANG = "KHACH_HANG";

    /** Hồ sơ an toàn: không bao giờ chọn cột mật khẩu. */
    private static final String SQL_HO_SO = """
            SELECT kh.tblThanhVienId AS id, tv.username, tv.hoTen, tv.ngaySinh, tv.gioiTinh,
                   tv.diaChi, tv.email, tv.sdt, tv.vaiTro,
                   kh.ngayDangKy, kh.tongDiem, kh.hangThanhVien
            FROM tblKhachHang kh
            JOIN tblThanhVien tv ON tv.id = kh.tblThanhVienId
            WHERE kh.tblThanhVienId = ?""";

    private final PasswordEncoder passwordEncoder;

    public KhachHangDAO(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        super(jdbcTemplate);
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lưu tài khoản (tblThanhVien, mật khẩu BCrypt) + hồ sơ khách hàng (tblKhachHang, 0 điểm) trong một giao dịch.
     * Trùng username/email/sđt → {@link org.springframework.dao.DuplicateKeyException}, không lưu gì.
     * Thành công: {@code kh} nhận id, ngày đăng ký, vai trò, 0 điểm; mật khẩu thô bị xóa khỏi {@code kh}.
     */
    @Transactional
    public boolean taoHoSo(KhachHang kh) {
        String matKhauBam = passwordEncoder.encode(kh.getPassword());
        GeneratedKeyHolder khoa = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO tblThanhVien (username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, kh.getUsername());
            ps.setString(2, matKhauBam);
            ps.setString(3, kh.getHoTen());
            ps.setDate(4, Date.valueOf(kh.getNgaySinh()));
            ps.setString(5, kh.getGioiTinh());
            ps.setString(6, kh.getDiaChi());
            ps.setString(7, kh.getEmail());
            ps.setString(8, kh.getSdt());
            ps.setString(9, VAI_TRO_KHACH_HANG);
            return ps;
        }, khoa);
        int id = Objects.requireNonNull(khoa.getKey()).intValue();
        LocalDate ngayDangKy = LocalDate.now();
        jdbcTemplate.update(
                "INSERT INTO tblKhachHang (tblThanhVienId, ngayDangKy, tongDiem, hangThanhVien) VALUES (?, ?, 0, NULL)",
                id, Date.valueOf(ngayDangKy));

        kh.setId(id);
        kh.setPassword(null);
        kh.setVaiTro(VAI_TRO_KHACH_HANG);
        kh.setNgayDangKy(ngayDangKy);
        kh.setTongDiem(0);
        kh.setHangThanhVien(null);
        return true;
    }

    /** Đọc hồ sơ an toàn (không mật khẩu) theo id khách hàng; {@code null} nếu không tồn tại. */
    public KhachHang layTheoId(int id) {
        if (id <= 0) {
            return null;
        }
        List<KhachHang> ds = jdbcTemplate.query(SQL_HO_SO, (rs, soThuTu) -> docHoSo(rs), id);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /**
     * Khóa hàng khách hàng ({@code SELECT ... FOR UPDATE}) rồi trả hồ sơ an toàn. Chỉ tuần tự hóa
     * khi được gọi bên trong transaction của người gọi (đổi điểm/thanh toán).
     */
    public KhachHang khoaTheoId(int id) {
        if (id <= 0) {
            return null;
        }
        List<Integer> ds = jdbcTemplate.query(
                "SELECT tblThanhVienId FROM tblKhachHang WHERE tblThanhVienId = ? FOR UPDATE",
                (rs, soThuTu) -> rs.getInt(1), id);
        return ds.isEmpty() ? null : layTheoId(id);
    }

    /** Đủ điểm để trừ? {@code maKhachHang} là id nội bộ dạng chuỗi số; điểm âm bị từ chối. */
    public boolean kiemTraDiem(String maKhachHang, int diem) {
        Integer id = docId(maKhachHang);
        if (id == null || diem < 0) {
            return false;
        }
        List<Integer> ds = jdbcTemplate.query("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId = ?",
                (rs, soThuTu) -> rs.getInt(1), id);
        return !ds.isEmpty() && ds.get(0) >= diem;
    }

    /** Trừ điểm có chặn âm ở tầng SQL; {@code false} nếu khách không tồn tại hoặc không đủ điểm. */
    public boolean truDiem(String maKhachHang, int diem) {
        Integer id = docId(maKhachHang);
        if (id == null || diem < 0) {
            return false;
        }
        if (diem == 0) {
            return kiemTraDiem(maKhachHang, 0);
        }
        return jdbcTemplate.update(
                "UPDATE tblKhachHang SET tongDiem = tongDiem - ? WHERE tblThanhVienId = ? AND tongDiem >= ?",
                diem, id, diem) > 0;
    }

    /** Cộng điểm; điểm âm bị từ chối để không tạo đường trừ điểm ngoài luồng. */
    public boolean congDiem(String maKhachHang, int diem) {
        Integer id = docId(maKhachHang);
        if (id == null || diem < 0) {
            return false;
        }
        if (diem == 0) {
            return kiemTraDiem(maKhachHang, 0);
        }
        return jdbcTemplate.update("UPDATE tblKhachHang SET tongDiem = tongDiem + ? WHERE tblThanhVienId = ?",
                diem, id) > 0;
    }

    private static Integer docId(String maKhachHang) {
        if (maKhachHang == null || maKhachHang.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(maKhachHang.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException ngoaiLe) {
            return null;
        }
    }

    private static KhachHang docHoSo(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setId(rs.getInt("id"));
        kh.setUsername(rs.getString("username"));
        kh.setHoTen(rs.getString("hoTen"));
        Date ngaySinh = rs.getDate("ngaySinh");
        if (ngaySinh != null) {
            kh.setNgaySinh(ngaySinh.toLocalDate());
        }
        kh.setGioiTinh(rs.getString("gioiTinh"));
        kh.setDiaChi(rs.getString("diaChi"));
        kh.setEmail(rs.getString("email"));
        kh.setSdt(rs.getString("sdt"));
        kh.setVaiTro(rs.getString("vaiTro"));
        Date ngayDangKy = rs.getDate("ngayDangKy");
        if (ngayDangKy != null) {
            kh.setNgayDangKy(ngayDangKy.toLocalDate());
        }
        kh.setTongDiem(rs.getInt("tongDiem"));
        kh.setHangThanhVien(rs.getString("hangThanhVien"));
        return kh;
    }
}
