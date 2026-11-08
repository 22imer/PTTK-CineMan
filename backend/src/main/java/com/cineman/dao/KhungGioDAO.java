package com.cineman.dao;

import com.cineman.entity.KhungGio;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Khung giờ chiếu — tblKhungGio (ARCHITECTURE §5).
 *
 * <p>{@link #laydsKhungGioTheoPhim} chỉ trả khung giờ có suất {@code MO_BAN} của phim (và ngày nếu có),
 * để UI chỉ hiện lựa chọn thực sự đặt được (R11).
 */
@Repository
public class KhungGioDAO extends DAO {

    public KhungGioDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Toàn bộ khung giờ, sắp theo giờ bắt đầu. */
    public List<KhungGio> laydsKhungGio() {
        return jdbcTemplate.query("SELECT id, gioBatDau, gioKetThuc FROM tblKhungGio ORDER BY gioBatDau, id",
                (rs, soThuTu) -> docKhungGio(rs));
    }

    /** Khung giờ có suất mở bán cho phim; {@code ngayChieu} null = mọi ngày. */
    public List<KhungGio> laydsKhungGioTheoPhim(int idPhim, LocalDate ngayChieu) {
        if (idPhim <= 0) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT kg.id, kg.gioBatDau, kg.gioKetThuc
                FROM tblKhungGio kg
                JOIN tblSuatChieu sc ON sc.tblKhungGioId = kg.id
                WHERE sc.trangThai = 'MO_BAN' AND sc.tblPhimId = ?""");
        List<Object> thamSo = new ArrayList<>();
        thamSo.add(idPhim);
        if (ngayChieu != null) {
            sql.append(" AND sc.ngayChieu = ?");
            thamSo.add(java.sql.Date.valueOf(ngayChieu));
        }
        sql.append(" ORDER BY kg.gioBatDau, kg.id");
        return jdbcTemplate.query(sql.toString(), (rs, soThuTu) -> docKhungGio(rs), thamSo.toArray());
    }

    static KhungGio docKhungGio(ResultSet rs) throws SQLException {
        KhungGio khungGio = new KhungGio();
        khungGio.setId(rs.getInt("id"));
        Time batDau = rs.getTime("gioBatDau");
        if (batDau != null) {
            khungGio.setGioBatDau(batDau.toLocalTime());
        }
        Time ketThuc = rs.getTime("gioKetThuc");
        if (ketThuc != null) {
            khungGio.setGioKetThuc(ketThuc.toLocalTime());
        }
        return khungGio;
    }
}
