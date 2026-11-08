package com.cineman.dao;

import com.cineman.entity.KhungGio;
import com.cineman.entity.Phim;
import com.cineman.entity.PhongChieu;
import com.cineman.entity.SuatChieu;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Suất chiếu — tblSuatChieu (ARCHITECTURE §5). Luôn kèm phim/khung giờ/phòng chiếu lồng nhau.
 *
 * <p>{@link #khoaTheoId} là locking read trên đúng hàng suất chiếu: transaction đặt vé khóa hàng này
 * trước khi kiểm tra trùng ghế, nên hai yêu cầu cùng suất chiếu được tuần tự hóa và mọi lần đọc
 * trùng ghế sau đó là dữ liệu mới nhất (MySQL REPEATABLE READ không dùng snapshot cũ).
 */
@Repository
public class SuatChieuDAO extends DAO {

    /** Cột suất chiếu + phim/khung giờ/phòng, dùng lại cho mapper của vé. */
    static final String COT_SUAT_CHIEU = """
            sc.id AS scId, sc.ngayChieu, sc.giaCoSo, sc.trangThai,
            p.id AS pId, p.tenPhim, p.theLoai, p.daoDien, p.thoiLuong, p.doTuoi, p.moTa,
            p.ngayKhoiChieu, p.ngayKetThuc,
            kg.id AS kgId, kg.gioBatDau, kg.gioKetThuc,
            pc.id AS pcId, pc.tenPhong, pc.loaiPhong, pc.sucChua""";

    static final String TU_SUAT_CHIEU = """
            FROM tblSuatChieu sc
            JOIN tblPhim p ON p.id = sc.tblPhimId
            JOIN tblKhungGio kg ON kg.id = sc.tblKhungGioId
            JOIN tblPhongChieu pc ON pc.id = sc.tblPhongChieuId""";

    public SuatChieuDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Suất {@code MO_BAN}; {@code ngayChieu} null = mọi ngày, {@code idKhungGio <= 0} = mọi khung giờ. */
    public List<SuatChieu> laydsSuatChieu(int idPhim, LocalDate ngayChieu, int idKhungGio) {
        StringBuilder sql = new StringBuilder("SELECT " + COT_SUAT_CHIEU + " " + TU_SUAT_CHIEU
                + " WHERE sc.trangThai = 'MO_BAN'");
        List<Object> thamSo = new ArrayList<>();
        if (idPhim > 0) {
            sql.append(" AND sc.tblPhimId = ?");
            thamSo.add(idPhim);
        }
        if (ngayChieu != null) {
            sql.append(" AND sc.ngayChieu = ?");
            thamSo.add(java.sql.Date.valueOf(ngayChieu));
        }
        if (idKhungGio > 0) {
            sql.append(" AND sc.tblKhungGioId = ?");
            thamSo.add(idKhungGio);
        }
        sql.append(" ORDER BY sc.ngayChieu, kg.gioBatDau, sc.id");
        return jdbcTemplate.query(sql.toString(), (rs, soThuTu) -> docSuatChieu(rs), thamSo.toArray());
    }

    /** Một suất chiếu đầy đủ theo id, {@code null} nếu không có. */
    public SuatChieu layTheoId(int idSuatChieu) {
        if (idSuatChieu <= 0) {
            return null;
        }
        List<SuatChieu> ds = jdbcTemplate.query(
                "SELECT " + COT_SUAT_CHIEU + " " + TU_SUAT_CHIEU + " WHERE sc.id = ?",
                (rs, soThuTu) -> docSuatChieu(rs), idSuatChieu);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /**
     * Khóa hàng suất chiếu ({@code SELECT ... FOR UPDATE}) rồi trả bản đầy đủ. Bắt buộc gọi trong
     * transaction; dùng làm khóa tuần tự cho đặt vé và nhả ghế hết hạn.
     */
    public SuatChieu khoaTheoId(int idSuatChieu) {
        if (idSuatChieu <= 0) {
            return null;
        }
        List<Integer> ds = jdbcTemplate.query(
                "SELECT id FROM tblSuatChieu WHERE id = ? FOR UPDATE",
                (rs, soThuTu) -> rs.getInt(1), idSuatChieu);
        return ds.isEmpty() ? null : layTheoId(idSuatChieu);
    }

    static SuatChieu docSuatChieu(ResultSet rs) throws SQLException {
        SuatChieu suatChieu = new SuatChieu();
        suatChieu.setId(rs.getInt("scId"));
        java.sql.Date ngayChieu = rs.getDate("ngayChieu");
        if (ngayChieu != null) {
            suatChieu.setNgayChieu(ngayChieu.toLocalDate());
        }
        suatChieu.setGiaCoSo(rs.getInt("giaCoSo"));
        suatChieu.setTrangThai(rs.getString("trangThai"));

        Phim phim = new Phim();
        phim.setId(rs.getInt("pId"));
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
        suatChieu.setPhim(phim);

        KhungGio khungGio = new KhungGio();
        khungGio.setId(rs.getInt("kgId"));
        Time batDau = rs.getTime("gioBatDau");
        if (batDau != null) {
            khungGio.setGioBatDau(batDau.toLocalTime());
        }
        Time ketThuc = rs.getTime("gioKetThuc");
        if (ketThuc != null) {
            khungGio.setGioKetThuc(ketThuc.toLocalTime());
        }
        suatChieu.setKhungGio(khungGio);

        PhongChieu phongChieu = new PhongChieu();
        phongChieu.setId(rs.getInt("pcId"));
        phongChieu.setTenPhong(rs.getString("tenPhong"));
        phongChieu.setLoaiPhong(rs.getString("loaiPhong"));
        phongChieu.setSucChua(rs.getInt("sucChua"));
        suatChieu.setPhongChieu(phongChieu);
        return suatChieu;
    }
}
