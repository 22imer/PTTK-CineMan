package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Seed §8 khớp số liệu kịch bản; ràng buộc DB bảo vệ các bất biến nghiệp vụ. */
@SpringBootTest
@Transactional
class SchemaSeedTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sc03GheA3VaB3CoDonGiaTheoKichBan() {
        Integer a3 = donGia(3, "A", "3");
        Integer b3 = donGia(3, "B", "3");

        assertThat(a3).isEqualTo(90_000);
        assertThat(b3).isEqualTo(120_000);
        assertThat(a3 + b3).isEqualTo(210_000);
        assertThat(jdbc.queryForObject("""
                SELECT CONCAT(p.tenPhim, '|', sc.ngayChieu, '|', kg.gioBatDau, '|', pc.tenPhong, '|', pc.loaiPhong)
                FROM tblSuatChieu sc
                JOIN tblPhim p ON p.id = sc.tblPhimId
                JOIN tblKhungGio kg ON kg.id = sc.tblKhungGioId
                JOIN tblPhongChieu pc ON pc.id = sc.tblPhongChieuId
                WHERE sc.id = 3""", String.class))
                .isEqualTo("Mai|2026-09-27|19:00:00|Phòng 1|2D");
    }

    @Test
    void khachHangMauDangNhapDuocVaCo250Diem() {
        String hash = jdbc.queryForObject(
                "SELECT password FROM tblThanhVien WHERE username = 'annguyen2002'", String.class);
        Integer tongDiem = jdbc.queryForObject(
                "SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId = 1", Integer.class);

        assertThat(new BCryptPasswordEncoder().matches("Matkhau@123", hash)).isTrue();
        assertThat(tongDiem).isEqualTo(250);
    }

    @Test
    void voucherViDuThuocKhachHangMauLaVePhimConHan() {
        var row = jdbc.queryForMap("""
                SELECT v.trangThai, q.loaiQua, gd.tblKhachHangId, v.ngayNhan
                FROM tblVoucherKhachHang v
                JOIN tblGiaoDichDoiDiem gd ON gd.id = v.tblGiaoDichDoiDiemId
                JOIN tblQuaTang q ON q.id = gd.tblQuaTangId
                WHERE v.maVoucher = 'VC-VIDU-12345'""");

        assertThat(row.get("trangThai")).isEqualTo("CHUA_SU_DUNG");
        assertThat(row.get("loaiQua")).isEqualTo("VE_PHIM");
        assertThat(row.get("tblKhachHangId")).isEqualTo(1);
        assertThat(((java.sql.Date) row.get("ngayNhan")).toLocalDate().plusDays(30))
                .isAfter(java.time.LocalDate.now());
    }

    @Test
    void tonKhoQuaTheoKichBan() {
        assertThat(jdbc.queryForObject("SELECT soLuong FROM tblQuaTang WHERE id = 2", Integer.class)).isEqualTo(45);
        assertThat(jdbc.queryForObject("SELECT soDiem FROM tblQuaTang WHERE id = 2", Integer.class)).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT soLuong FROM tblQuaTang WHERE id = 4", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT giaTriGiam FROM tblQuaTang WHERE id = 4", Integer.class)).isEqualTo(50_000);
    }

    @Test
    void khongChoTonKhoHoacDiemAm() {
        assertThatThrownBy(() -> jdbc.update("UPDATE tblQuaTang SET soLuong = soLuong - 1 WHERE id = 4"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE tblKhachHang SET tongDiem = tongDiem - 251 WHERE tblThanhVienId = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void maVoucherLaDuyNhat() {
        jdbc.update("INSERT INTO tblGiaoDichDoiDiem (thoiGian, soDiemTru, diemConLai, tblKhachHangId, tblQuaTangId)"
                + " VALUES (CURRENT_TIMESTAMP, 100, 50, 1, 2)");
        Integer gdId = jdbc.queryForObject("SELECT MAX(id) FROM tblGiaoDichDoiDiem", Integer.class);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO tblVoucherKhachHang"
                + " (maVoucher, ngayNhan, trangThai, tblGiaoDichDoiDiemId)"
                + " VALUES ('VC-VIDU-12345', CURRENT_DATE, 'CHUA_SU_DUNG', ?)", gdId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void gheCoVeDaHuyDuocGiuLaiCungSuat() {
        Integer hd1 = taoHoaDon("DA_HUY");
        jdbc.update("INSERT INTO tblVe (giaVe, trangThaiVe, tblHoaDonId, tblSuatChieuId, tblGheId)"
                + " VALUES (90000, 'DA_HUY', ?, 3, 3)", hd1);
        Integer hd2 = taoHoaDon("CHO_THANH_TOAN");

        jdbc.update("INSERT INTO tblVe (giaVe, trangThaiVe, tblHoaDonId, tblSuatChieuId, tblGheId)"
                + " VALUES (90000, 'DANG_GIU', ?, 3, 3)", hd2);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM tblVe WHERE tblSuatChieuId = 3 AND tblGheId = 3", Integer.class)).isEqualTo(2);
    }

    private Integer donGia(int idSuatChieu, String hang, String so) {
        return jdbc.queryForObject("""
                SELECT sc.giaCoSo + lg.phuThu
                FROM tblSuatChieu sc
                JOIN tblGhe g ON g.tblPhongChieuId = sc.tblPhongChieuId
                JOIN tblLoaiGhe lg ON lg.id = g.tblLoaiGheId
                WHERE sc.id = ? AND g.hangGhe = ? AND g.soGhe = ?""", Integer.class, idSuatChieu, hang, so);
    }

    private Integer taoHoaDon(String trangThai) {
        jdbc.update("INSERT INTO tblHoaDon (ngayTao, hetHan, trangThai, tblKhachHangId)"
                + " VALUES (CURRENT_TIMESTAMP, DATEADD('SECOND', 300, CURRENT_TIMESTAMP), ?, 1)", trangThai);
        return jdbc.queryForObject("SELECT MAX(id) FROM tblHoaDon", Integer.class);
    }
}
