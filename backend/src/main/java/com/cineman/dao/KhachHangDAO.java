package com.cineman.dao;

import com.cineman.entity.KhachHang;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KhachHangDAO extends DAO {

    public static final String VAI_TRO_KHACH_HANG = "KHACH_HANG";

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
}
