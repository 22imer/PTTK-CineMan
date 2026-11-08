package com.cineman.dao;

import com.cineman.entity.Ghe;
import com.cineman.entity.LoaiGhe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Ghế — tblGhe (ARCHITECTURE §5). Ghế đọc theo phòng của suất chiếu, sắp theo id (A1..A8, B1..,
 * rồi Sweetbox E), khớp sơ đồ ghế ở frontend.
 */
@Repository
public class GheDAO extends DAO {

    static final String COT_GHE = """
            g.id AS gheId, g.soGhe, g.hangGhe,
            lg.id AS loaiGheId, lg.tenLoai, lg.phuThu""";

    public GheDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Ghế của phòng chứa suất chiếu, sắp theo id. */
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
}
