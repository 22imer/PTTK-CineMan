package com.cineman.dao;

import com.cineman.entity.Phim;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Phim — tblPhim (ARCHITECTURE §5).
 *
 * <p>Danh mục đặt vé chỉ trả phim còn ít nhất một suất {@code MO_BAN}; ngày chiếu không so với đồng hồ
 * thực (seed SC03 27/09/2026 là mốc ví dụ có chủ đích).
 */
@Repository
public class PhimDAO extends DAO {

    private static final String CAC_COT = """
            p.id, p.tenPhim, p.theLoai, p.daoDien, p.thoiLuong, p.doTuoi, p.moTa,
            p.ngayKhoiChieu, p.ngayKetThuc""";

    public PhimDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Phim đang có suất mở bán, sắp theo id. */
    public List<Phim> laydsPhim() {
        return jdbcTemplate.query("""
                SELECT %s
                FROM tblPhim p
                WHERE EXISTS (SELECT 1 FROM tblSuatChieu sc
                              WHERE sc.tblPhimId = p.id AND sc.trangThai = 'MO_BAN')
                ORDER BY p.id""".formatted(CAC_COT), (rs, soThuTu) -> docPhim(rs));
    }

    static Phim docPhim(ResultSet rs) throws SQLException {
        Phim phim = new Phim();
        phim.setId(rs.getInt("id"));
        phim.setTenPhim(rs.getString("tenPhim"));
        phim.setTheLoai(rs.getString("theLoai"));
        phim.setDaoDien(rs.getString("daoDien"));
        phim.setThoiLuong(rs.getInt("thoiLuong"));
        phim.setDoTuoi(rs.getString("doTuoi"));
        phim.setMoTa(rs.getString("moTa"));
        java.sql.Date ngayKhoiChieu = rs.getDate("ngayKhoiChieu");
        if (ngayKhoiChieu != null) {
            phim.setNgayKhoiChieu(ngayKhoiChieu.toLocalDate());
        }
        java.sql.Date ngayKetThuc = rs.getDate("ngayKetThuc");
        if (ngayKetThuc != null) {
            phim.setNgayKetThuc(ngayKetThuc.toLocalDate());
        }
        return phim;
    }
}
