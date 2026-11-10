package com.cineman.dao;

import com.cineman.entity.PhienDangNhap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Phiên đăng nhập JWT — tblPhienDangNhap (ARCHITECTURE §7.3).
 *
 * <p>Mọi thời điểm truyền vào đều là {@link Instant} đã cắt về mili giây (khớp {@code DATETIME(3)}),
 * do tầng service lấy từ {@link java.time.Clock} để test biên thời gian tất định. Xoay token và thu hồi
 * là cập nhật có điều kiện nên hai request đồng thời không thể cùng thắng; {@code khoaTheoId} dùng
 * {@code SELECT ... FOR UPDATE} và chỉ có tác dụng khi được gọi trong transaction của người gọi.
 */
@Repository
public class PhienDangNhapDAO extends DAO {

    private static final String SQL_DOC = """
            SELECT id, tblKhachHangId, refreshTokenHash, previousTokenHash, xoayLuc, ngayTao, hetHan, thuHoiLuc
            FROM tblPhienDangNhap
            WHERE id = ?""";

    public PhienDangNhapDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Tạo phiên mới; {@code refreshTokenHash} là SHA-256 hex của refresh token vừa cấp. */
    public void taoPhien(String id, int khachHangId, String refreshTokenHash, Instant ngayTao, Instant hetHan) {
        jdbcTemplate.update("""
                INSERT INTO tblPhienDangNhap
                    (id, tblKhachHangId, refreshTokenHash, previousTokenHash, xoayLuc, ngayTao, hetHan, thuHoiLuc)
                VALUES (?, ?, ?, NULL, NULL, ?, ?, NULL)""",
                id, khachHangId, refreshTokenHash, thoiDiem(ngayTao), thoiDiem(hetHan));
    }

    /** Đọc phiên không khóa (dùng cho mỗi request được bảo vệ); {@code null} nếu không tồn tại. */
    public PhienDangNhap layTheoId(String id) {
        List<PhienDangNhap> ds = jdbcTemplate.query(SQL_DOC, (rs, soThuTu) -> docPhien(rs), id);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /** Khóa hàng phiên ({@code SELECT ... FOR UPDATE}) rồi đọc; dùng trong transaction xoay refresh token. */
    public PhienDangNhap khoaTheoId(String id) {
        List<PhienDangNhap> ds = jdbcTemplate.query(SQL_DOC + " FOR UPDATE",
                (rs, soThuTu) -> docPhien(rs), id);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /**
     * Xoay refresh token nguyên tử: chỉ thành công khi hash hiện hành vẫn đúng và phiên chưa bị thu hồi.
     * {@code hashCu} được lưu vào {@code previousTokenHash} để phát hiện replay trong cửa sổ ngắn.
     */
    public boolean xoayToken(String id, String hashCu, String hashMoi, Instant xoayLuc) {
        return jdbcTemplate.update("""
                UPDATE tblPhienDangNhap
                   SET previousTokenHash = ?, refreshTokenHash = ?, xoayLuc = ?
                 WHERE id = ? AND refreshTokenHash = ? AND thuHoiLuc IS NULL""",
                hashCu, hashMoi, thoiDiem(xoayLuc), id, hashCu) > 0;
    }

    /** Thu hồi phiên (idempotent): lần gọi sau không đổi {@code thuHoiLuc} đã ghi. */
    public boolean thuHoi(String id, Instant thuHoiLuc) {
        return jdbcTemplate.update(
                "UPDATE tblPhienDangNhap SET thuHoiLuc = ? WHERE id = ? AND thuHoiLuc IS NULL",
                thoiDiem(thuHoiLuc), id) > 0;
    }

    private static PhienDangNhap docPhien(ResultSet rs) throws SQLException {
        PhienDangNhap phien = new PhienDangNhap();
        phien.setId(rs.getString("id"));
        phien.setKhachHangId(rs.getInt("tblKhachHangId"));
        phien.setRefreshTokenHash(rs.getString("refreshTokenHash"));
        phien.setPreviousTokenHash(rs.getString("previousTokenHash"));
        phien.setXoayLuc(docThoiDiem(rs, "xoayLuc"));
        phien.setNgayTao(docThoiDiem(rs, "ngayTao"));
        phien.setHetHan(docThoiDiem(rs, "hetHan"));
        phien.setThuHoiLuc(docThoiDiem(rs, "thuHoiLuc"));
        return phien;
    }

    private static Instant docThoiDiem(ResultSet rs, String cot) throws SQLException {
        Timestamp ts = rs.getTimestamp(cot);
        return ts == null ? null : ts.toInstant();
    }

    private static Timestamp thoiDiem(Instant luc) {
        return luc == null ? null : Timestamp.from(Instant.ofEpochMilli(luc.toEpochMilli()));
    }
}
