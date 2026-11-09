package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Runs only against an explicitly supplied, disposable MySQL database. */
@EnabledIfEnvironmentVariable(named = "UNICODE_TEST_DB_URL", matches = "jdbc:mysql:.*")
@SpringBootTest(properties = {
        "spring.datasource.url=${UNICODE_TEST_DB_URL}",
        "spring.datasource.username=${UNICODE_TEST_DB_USER:cineman}",
        "spring.datasource.password=${UNICODE_TEST_DB_PASSWORD:cineman}"
})
class UnicodeRepairIntegrationTest {
    private static final List<String> TABLES = List.of(
            "tblThanhVien", "tblKhachHang", "tblPhim", "tblKhungGio", "tblPhongChieu",
            "tblSuatChieu", "tblLoaiGhe", "tblGhe", "tblQuaTang", "tblGiaoDichDoiDiem",
            "tblVoucherKhachHang", "tblHoaDon", "tblVe");

    @Autowired JdbcTemplate jdbc;

    @Test
    void repairsOldSeedWithoutResettingBusinessDataOrOverwritingHealthyEdits() throws Exception {
        jdbc.update("UPDATE tblKhachHang SET tongDiem=160 WHERE tblThanhVienId=1");
        jdbc.update("UPDATE tblQuaTang SET soLuong=44 WHERE id=2");
        jdbc.update("UPDATE tblPhim SET tenPhim='Tên phim mới – giữ nguyên' WHERE id=3");
        jdbc.update("UPDATE tblQuaTang SET moTa=NULL WHERE id=4");
        jdbc.update("""
                INSERT INTO tblThanhVien (username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro)
                SELECT 'unicode_healthy', password, 'Nguyễn Văn Mới 🎬', ngaySinh, gioiTinh,
                       'Hà Nội', 'unicode@example.com', '0990000027', vaiTro
                FROM tblThanhVien WHERE id=1
                """);
        jdbc.update("""
                INSERT INTO tblHoaDon (ngayTao, hetHan, trangThai, tblKhachHangId)
                VALUES (CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 300 SECOND), 'DA_THANH_TOAN', 1)
                """);
        int invoice = jdbc.queryForObject("SELECT MAX(id) FROM tblHoaDon", Integer.class);
        jdbc.update("""
                INSERT INTO tblVe (giaVe, trangThaiVe, tblHoaDonId, tblSuatChieuId, tblGheId)
                VALUES (90000, 'DA_BAN', ?, 3, 3)
                """, invoice);
        Map<String, List<Map<String, Object>>> expected = snapshot();

        corrupt("tblThanhVien", List.of("hoTen", "diaChi"), "id=1");
        corrupt("tblPhim", List.of("tenPhim", "theLoai", "daoDien", "moTa"), "id IN (1,2)");
        corrupt("tblQuaTang", List.of("tenQua", "moTa"), "id IN (1,2,3,4)");
        corrupt("tblPhongChieu", List.of("tenPhong"), "id IN (1,2)");
        corrupt("tblLoaiGhe", List.of("tenLoai"), "id IN (1,2,3)");
        assertThat(jdbc.queryForObject("SELECT hoTen FROM tblThanhVien WHERE id=1", String.class))
                .isEqualTo("Nguyá»…n VÄƒn A");
        assertThat(snapshot()).isNotEqualTo(expected);

        repair();
        assertThat(snapshot()).isEqualTo(expected);
        repair();
        assertThat(snapshot()).as("Rerunning the repair must not change any record").isEqualTo(expected);
    }

    private void corrupt(String table, List<String> columns, String where) {
        for (String column : columns) {
            jdbc.update("UPDATE " + table + " SET " + column + "=CONVERT(CAST(" + column
                    + " AS BINARY) USING latin1) WHERE " + where);
        }
    }

    private void repair() throws Exception {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("repair-unicode.sql"));
        }
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : TABLES) {
            String key = table.equals("tblKhachHang") ? "tblThanhVienId" : "id";
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY " + key));
        }
        return result;
    }
}
