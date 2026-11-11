package com.cineman.dao;

import com.cineman.entity.Ghe;
import com.cineman.entity.LoaiGhe;
import com.cineman.entity.SuatChieu;
import com.cineman.entity.Ve;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Vé — tblVe (ARCHITECTURE §5). Ràng buộc "không hai vé DANG_GIU/DA_BAN cùng (suất, ghế)" kiểm tra ở
 * tầng ứng dụng bằng {@link #kiemTraVe} (locking read) bên trong transaction đã khóa hàng suất chiếu.
 */
@Repository
public class VeDAO extends DAO {

    public static final String DANG_GIU = "DANG_GIU";
    public static final String DA_BAN = "DA_BAN";
    public static final String DA_HUY = "DA_HUY";

    private static final String COT_GHE = """
            g.id AS gheId, g.soGhe, g.hangGhe,
            lg.id AS loaiGheId, lg.tenLoai, lg.phuThu""";

    private static final String COT_VE = "v.id AS veId, v.giaVe, v.trangThaiVe, "
            + SuatChieuDAO.COT_SUAT_CHIEU + ", " + COT_GHE;

    private static final String TU_VE = """
            FROM tblVe v
            JOIN tblSuatChieu sc ON sc.id = v.tblSuatChieuId
            JOIN tblPhim p ON p.id = sc.tblPhimId
            JOIN tblKhungGio kg ON kg.id = sc.tblKhungGioId
            JOIN tblPhongChieu pc ON pc.id = sc.tblPhongChieuId
            JOIN tblGhe g ON g.id = v.tblGheId
            JOIN tblLoaiGhe lg ON lg.id = g.tblLoaiGheId
            """;

    public VeDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Vé còn hiệu lực (đang giữ hoặc đã bán) của suất chiếu; vé DA_HUY bị bỏ qua. */
    public List<Ve> laydsVeConHieuLuc(SuatChieu suatChieu) {
        if (suatChieu == null || suatChieu.getId() <= 0) {
            return List.of();
        }
        return jdbcTemplate.query("SELECT " + COT_VE + " " + TU_VE + """
                 WHERE v.tblSuatChieuId = ? AND v.trangThaiVe IN ('DANG_GIU', 'DA_BAN')
                 ORDER BY v.id""", (rs, soThuTu) -> docVe(rs), suatChieu.getId());
    }

    /** Toàn bộ vé của hóa đơn (mọi trạng thái), dùng cho tóm tắt hóa đơn. */
    public List<Ve> laydsVeTheoHoaDon(int idHoaDon) {
        if (idHoaDon <= 0) {
            return List.of();
        }
        return jdbcTemplate.query("SELECT " + COT_VE + " " + TU_VE + """
                 WHERE v.tblHoaDonId = ?
                 ORDER BY v.id""", (rs, soThuTu) -> docVe(rs), idHoaDon);
    }

    /**
     * Toàn bộ ghế của phòng chứa suất chiếu (kể cả ghế chưa có vé), sắp theo id (A1..A8, B1.., rồi
     * Sweetbox E), khớp sơ đồ ghế ở frontend. Trạng thái giữ/bán lấy từ {@link #laydsVeConHieuLuc}.
     */
    public List<Ghe> laydsGhe(int idSuatChieu) {
        if (idSuatChieu <= 0) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT %s
                FROM tblGhe g
                JOIN tblLoaiGhe lg ON lg.id = g.tblLoaiGheId
                JOIN tblSuatChieu sc ON sc.tblPhongChieuId = g.tblPhongChieuId
                WHERE sc.id = ?
                ORDER BY g.id""".formatted(COT_GHE), (rs, soThuTu) -> docGhe(rs), idSuatChieu);
    }

    /**
     * Đã có vé DANG_GIU/DA_BAN cho (suất, ghế)? Dùng locking read để MySQL REPEATABLE READ đọc bản
     * mới nhất thay vì snapshot trước khi chờ khóa suất chiếu.
     */
    public boolean kiemTraVe(int idSuatChieu, int idGhe) {
        if (idSuatChieu <= 0 || idGhe <= 0) {
            return false;
        }
        List<Integer> ds = jdbcTemplate.query("""
                SELECT id FROM tblVe
                WHERE tblSuatChieuId = ? AND tblGheId = ? AND trangThaiVe IN ('DANG_GIU', 'DA_BAN')
                FOR UPDATE""", (rs, soThuTu) -> rs.getInt(1), idSuatChieu, idGhe);
        return !ds.isEmpty();
    }

    /** Giá ghế = giá cơ sở của suất + phụ thu loại ghế; tạo đối tượng vé DANG_GIU (giá do server tính). */
    public Ve tinhTienVe(SuatChieu suatChieu, Ghe ghe) {
        Objects.requireNonNull(suatChieu, "suatChieu");
        Objects.requireNonNull(ghe, "ghe");
        int phuThu = ghe.getLoaiGhe() != null ? ghe.getLoaiGhe().getPhuThu() : 0;
        Ve ve = new Ve();
        ve.setGiaVe(suatChieu.getGiaCoSo() + phuThu);
        ve.setTrangThaiVe(DANG_GIU);
        ve.setSuatChieu(suatChieu);
        ve.setGhe(ghe);
        return ve;
    }

    /** Lưu vé DANG_GIU vào hóa đơn trong transaction của người gọi; trả id sinh ra. */
    public int luuVe(Ve ve, int idHoaDon) {
        Objects.requireNonNull(ve, "ve");
        GeneratedKeyHolder khoa = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO tblVe (giaVe, trangThaiVe, tblHoaDonId, tblSuatChieuId, tblGheId)
                    VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, ve.getGiaVe());
            ps.setString(2, ve.getTrangThaiVe());
            ps.setInt(3, idHoaDon);
            ps.setInt(4, ve.getSuatChieu().getId());
            ps.setInt(5, ve.getGhe().getId());
            return ps;
        }, khoa);
        int id = Objects.requireNonNull(khoa.getKey()).intValue();
        ve.setId(id);
        return id;
    }

    /** Đổi trạng thái các vé đang giữ DANG_GIU của hóa đơn (DA_BAN khi thanh toán, DA_HUY khi hủy/hết hạn). */
    public boolean capNhatTrangThaiVe(int idHoaDon, String trangThai) {
        if (idHoaDon <= 0 || trangThai == null) {
            return false;
        }
        return jdbcTemplate.update(
                "UPDATE tblVe SET trangThaiVe = ? WHERE tblHoaDonId = ? AND trangThaiVe = 'DANG_GIU'",
                trangThai, idHoaDon) > 0;
    }

    /** Hủy một vé. */
    public boolean huyVe(int idVe) {
        if (idVe <= 0) {
            return false;
        }
        return jdbcTemplate.update("UPDATE tblVe SET trangThaiVe = 'DA_HUY' WHERE id = ?", idVe) > 0;
    }

    static Ghe docGhe(ResultSet rs) throws SQLException {
        Ghe ghe = new Ghe();
        ghe.setId(rs.getInt("gheId"));
        ghe.setSoGhe(rs.getString("soGhe"));
        ghe.setHangGhe(rs.getString("hangGhe"));
        LoaiGhe loaiGhe = new LoaiGhe();
        loaiGhe.setId(rs.getInt("loaiGheId"));
        loaiGhe.setTenLoai(rs.getString("tenLoai"));
        loaiGhe.setPhuThu(rs.getInt("phuThu"));
        ghe.setLoaiGhe(loaiGhe);
        return ghe;
    }

    static Ve docVe(ResultSet rs) throws SQLException {
        Ve ve = new Ve();
        ve.setId(rs.getInt("veId"));
        ve.setGiaVe(rs.getInt("giaVe"));
        ve.setTrangThaiVe(rs.getString("trangThaiVe"));
        ve.setSuatChieu(SuatChieuDAO.docSuatChieu(rs));
        ve.setGhe(docGhe(rs));
        return ve;
    }
}
