package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.cineman.auth.AuthCookie;
import com.cineman.auth.AuthFilter;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Hành vi HTTP của đăng ký/đăng nhập trên DB H2 thật (MySQL mode) + MockMvc.
 *
 * <p>DB riêng {@code auth-tests}, không dùng chung {@code cineman} của SchemaSeedTest.
 * Trước mỗi test xóa toàn bộ khách hàng (theo thứ tự khóa ngoại) trong DB này để fixture và số đếm
 * không phụ thuộc thứ tự test lẫn seed; mỗi test tự tạo dữ liệu của mình. Phiên được kiểm chứng qua
 * access token/cookie refresh thật (§7.3), không đọc nội bộ servlet session.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";
    private static final String REFRESH = "/api/auth/refresh";
    private static final String MAT_KHAU = "Matkhau@123";
    private static final String USERNAME = "it_khach_a";
    private static final String EMAIL = "it_khach_a@example.com";
    private static final String SDT = "0900000001";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @BeforeEach
    void xoaToanBoKhachHang() {
        // Xóa theo thứ tự phụ thuộc khóa ngoại của schema.sql; DB chỉ thuộc lớp test này.
        for (String bang : List.of("tblPhienDangNhap", "tblVe", "tblHoaDon", "tblVoucherKhachHang",
                "tblGiaoDichDoiDiem", "tblKhachHang", "tblThanhVien")) {
            jdbc.update("DELETE FROM " + bang);
        }
    }

    // ---------- Đăng ký thành công ----------

    @Test
    void dangKyThanhCongTraHoSoMoiLuuBcryptKhongDiemVaKhongLoMatKhau() throws Exception {
        MvcResult ketQua = dangKy(thanhVien(USERNAME, EMAIL, SDT));
        JsonNode body = doc(ketQua);

        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(body.hasNonNull("data")).isTrue();
        JsonNode data = body.path("data");
        assertThat(data.path("username").asText()).isEqualTo(USERNAME);
        assertThat(data.path("hoTen").asText()).isEqualTo("Nguyễn Văn Kiểm Thử");
        assertThat(data.path("email").asText()).isEqualTo(EMAIL);
        assertThat(data.path("sdt").asText()).isEqualTo(SDT);
        assertThat(data.path("ngaySinh").asText()).isEqualTo("2000-05-20");
        assertThat(data.path("vaiTro").asText()).isEqualTo("KHACH_HANG");
        assertThat(data.path("tongDiem").asInt()).isZero();
        assertThat(data.path("hangThanhVien").isNull()).isTrue();
        assertThat(data.path("ngayDangKy").asText()).isNotBlank();
        assertThat(data.has("password")).isFalse();

        int id = data.path("id").asInt();
        assertThat(id).isPositive();

        HoSo row = hoSoTheoUsername(USERNAME);
        assertThat(row.id()).isEqualTo(id);
        assertThat(row.vaiTro()).isEqualTo("KHACH_HANG");
        assertThat(row.tongDiem()).isZero();
        assertThat(row.hangThanhVien()).isNull();
        assertThat(row.ngayDangKy()).isNotNull();

        String hash = row.password();
        assertThat(hash).isNotEqualTo(MAT_KHAU);
        assertThat(hash).matches("^\\$2[aby]\\$\\d{2}\\$.+");
        assertThat(bcrypt.matches(MAT_KHAU, hash)).isTrue();

        String raw = ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain(MAT_KHAU);
        assertThat(raw).doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("$2y$");

        // Đăng ký không tự đăng nhập: không cấp access token lẫn cookie refresh.
        assertThat(ketQua.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(data.has("accessToken")).isFalse();
    }

    @Test
    void dangKyKhongTinIdVaiTroDiemHayHangTuClient() throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) body.get("kh");
        kh.put("id", 999_999);
        kh.put("vaiTro", "ADMIN");
        kh.put("tongDiem", 999_999);
        kh.put("hangThanhVien", "VIP");
        kh.put("ngayDangKy", "1999-01-01");

        MvcResult ketQua = dangKy(body);
        JsonNode data = doc(ketQua).path("data");

        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        assertThat(data.path("id").asInt()).isNotEqualTo(999_999).isPositive();
        assertThat(data.path("vaiTro").asText()).isEqualTo("KHACH_HANG");
        assertThat(data.path("tongDiem").asInt()).isZero();
        assertThat(data.path("hangThanhVien").isNull()).isTrue();
        assertThat(data.path("ngayDangKy").asText()).isNotEqualTo("1999-01-01");

        HoSo row = hoSoTheoUsername(USERNAME);
        assertThat(row.id()).isEqualTo(data.path("id").asInt());
        assertThat(row.vaiTro()).isEqualTo("KHACH_HANG");
        assertThat(row.tongDiem()).isZero();
        assertThat(row.hangThanhVien()).isNull();
    }

    @Test
    void dangKyKhongChanTuoi() throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) body.get("kh");
        String homNay = LocalDate.now().toString();
        kh.put("ngaySinh", homNay);

        MvcResult ketQua = dangKy(body);

        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(ketQua).path("data").path("ngaySinh").asText()).isEqualTo(homNay);
    }

    // ---------- R8: trường bắt buộc, định dạng, độ dài, nhập lại ----------

    @ParameterizedTest
    @ValueSource(strings = {"username", "password", "hoTen", "ngaySinh", "gioiTinh", "diaChi", "email", "sdt",
            "xacNhanMatKhau"})
    void dangKyThieuTruongBatBuocTra400VaKhongTaoTaiKhoan(String truong) throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        if ("xacNhanMatKhau".equals(truong)) {
            body.remove(truong);
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> kh = (Map<String, Object>) body.get("kh");
            kh.remove(truong);
        }

        assertLoiEnvelope(dangKy(body), 400);
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "hoTen", "gioiTinh", "diaChi"})
    void dangKyTruongBatBuocToanKhoangTrangTra400(String truong) throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) body.get("kh");
        kh.put(truong, "   ");

        assertLoiEnvelope(dangKy(body), 400);
        assertThat(demTaiKhoan()).isZero();
    }

    @Test
    void dangKyEmailSaiDinhDangTra400() throws Exception {
        String[] sai = {"khong-co-a-cong", "a@b", "a b@cineman.vn", "@cineman.vn", "khach@cineman"};
        for (int i = 0; i < sai.length; i++) {
            String username = "it_email_" + i;
            Map<String, Object> body = thanhVien(username, "hop-le-" + i + "@example.com", sdtTheoChiSo(i));
            @SuppressWarnings("unchecked")
            Map<String, Object> kh = (Map<String, Object>) body.get("kh");
            kh.put("email", sai[i]);

            assertLoiEnvelope(dangKy(body), 400);
        }
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    @Test
    void dangKySoDienThoaiSaiDinhDangTra400() throws Exception {
        String[] sai = {"090000000", "1900000000", "09000000000", "09000a0000", "+84900000001"};
        for (int i = 0; i < sai.length; i++) {
            String username = "it_sdt_" + i;
            Map<String, Object> body = thanhVien(username, username + "@example.com", "0900000001");
            @SuppressWarnings("unchecked")
            Map<String, Object> kh = (Map<String, Object>) body.get("kh");
            kh.put("sdt", sai[i]);

            assertLoiEnvelope(dangKy(body), 400);
        }
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    @Test
    void dangKyMatKhauBienBayVaTamKyTu() throws Exception {
        Map<String, Object> bayKyTu = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> khBay = (Map<String, Object>) bayKyTu.get("kh");
        khBay.put("password", "1234567");
        bayKyTu.put("xacNhanMatKhau", "1234567");

        assertLoiEnvelope(dangKy(bayKyTu), 400);
        assertThat(demTaiKhoan()).isZero();

        Map<String, Object> tamKyTu = thanhVien("it_matkhau_8", "it_matkhau_8@example.com", "0900000008");
        @SuppressWarnings("unchecked")
        Map<String, Object> khTam = (Map<String, Object>) tamKyTu.get("kh");
        khTam.put("password", "12345678");
        tamKyTu.put("xacNhanMatKhau", "12345678");

        MvcResult ketQua = dangKy(tamKyTu);
        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        String hash = jdbc.queryForObject(
                "SELECT password FROM tblThanhVien WHERE username = ?", String.class, "it_matkhau_8");
        assertThat(bcrypt.matches("12345678", hash)).isTrue();
    }

    @Test
    void dangKyMatKhauToanKhoangTrangTra400VaKhongTaoTaiKhoan() throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) body.get("kh");
        String toanKhoangTrang = "        ";
        kh.put("password", toanKhoangTrang);
        body.put("xacNhanMatKhau", toanKhoangTrang);

        assertLoiEnvelope(dangKy(body), 400);
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    @Test
    void dangKyMatKhauCoKhoangTrangHaiDauDangNhapDungChinhXac() throws Exception {
        String matKhau = "  Matkhau@123  ";
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) body.get("kh");
        kh.put("password", matKhau);
        body.put("xacNhanMatKhau", matKhau);

        MvcResult ketQuaDangKy = dangKy(body);
        assertThat(ketQuaDangKy.getResponse().getStatus()).isEqualTo(200);

        String hash = jdbc.queryForObject(
                "SELECT password FROM tblThanhVien WHERE username = ?", String.class, USERNAME);
        assertThat(bcrypt.matches(matKhau, hash)).isTrue();
        assertThat(bcrypt.matches(matKhau.trim(), hash)).isFalse();

        MvcResult dungNguyenVan = dangNhap(USERNAME, matKhau);
        assertThat(dungNguyenVan.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(dungNguyenVan).path("data").path("khachHang").path("username").asText())
                .isEqualTo(USERNAME);
        assertThat(doc(dungNguyenVan).path("data").path("accessToken").asText()).isNotBlank();
        assertThat(refreshToken(dungNguyenVan)).isNotBlank();

        MvcResult daCatKhoangTrang = dangNhap(USERNAME, matKhau.trim());
        assertLoiEnvelope(daCatKhoangTrang, 401);
        assertKhongCoCookie(daCatKhoangTrang);
    }

    @Test
    void dangKyXacNhanMatKhauKhongKhopTra400() throws Exception {
        Map<String, Object> body = thanhVien(USERNAME, EMAIL, SDT);
        body.put("xacNhanMatKhau", MAT_KHAU + "x");

        assertLoiEnvelope(dangKy(body), 400);
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    // ---------- Body thiếu / JSON sai / ngày sinh sai ----------

    @Test
    void dangKyBodyThieuHoacSaiJsonTraEnvelope400() throws Exception {
        List<String> bodies = List.of("null", "{", "{}", "{\"kh\":null,\"xacNhanMatKhau\":\"x\"}");
        for (String body : bodies) {
            MvcResult ketQua = mvc.perform(post(REGISTER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andReturn();
            assertLoiEnvelope(ketQua, 400);
            assertThat(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .doesNotContain("Exception");
        }

        MvcResult khongBody = mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)).andReturn();
        assertLoiEnvelope(khongBody, 400);

        Map<String, Object> ngaySinhSai = thanhVien(USERNAME, EMAIL, SDT);
        @SuppressWarnings("unchecked")
        Map<String, Object> kh = (Map<String, Object>) ngaySinhSai.get("kh");
        kh.put("ngaySinh", "20-05-2000");
        assertLoiEnvelope(dangKy(ngaySinhSai), 400);

        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    @Test
    void dangNhapBodyThieuHoacSaiJsonTraEnvelope400() throws Exception {
        for (String body : List.of("null", "{", "[1,2]")) {
            MvcResult ketQua = mvc.perform(post(LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                    .content(body)).andReturn();
            assertLoiEnvelope(ketQua, 400);
            assertThat(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .doesNotContain("Exception");
        }

        MvcResult khongBody = mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)).andReturn();
        assertLoiEnvelope(khongBody, 400);
    }

    // ---------- Trùng username / email / sđt ----------

    @Test
    void dangKyTrungUsernameTra409KhongGhiHoSo() throws Exception {
        dangKyThanhCong(USERNAME, EMAIL, SDT);
        int soTaiKhoan = demTaiKhoan();
        int soHoSo = demHoSo();

        Map<String, Object> trung = thanhVien(USERNAME, "it_khach_b@example.com", "0900000002");
        MvcResult ketQua = dangKy(trung);

        assertLoiEnvelope(ketQua, 409);
        assertThat(demTaiKhoan()).isEqualTo(soTaiKhoan);
        assertThat(demHoSo()).isEqualTo(soHoSo);
        assertThat(demTheoEmail("it_khach_b@example.com")).isZero();
        assertThat(hoSoTheoUsername(USERNAME).email()).isEqualTo(EMAIL);
    }

    @Test
    void dangKyTrungEmailTra409KhongGhiHoSo() throws Exception {
        dangKyThanhCong(USERNAME, EMAIL, SDT);
        int soTaiKhoan = demTaiKhoan();
        int soHoSo = demHoSo();

        Map<String, Object> trung = thanhVien("it_khach_b", EMAIL, "0900000002");
        MvcResult ketQua = dangKy(trung);

        assertLoiEnvelope(ketQua, 409);
        assertThat(demTaiKhoan()).isEqualTo(soTaiKhoan);
        assertThat(demHoSo()).isEqualTo(soHoSo);
        assertThat(demTheoUsername("it_khach_b")).isZero();
    }

    @Test
    void dangKyTrungSoDienThoaiTra409KhongGhiHoSo() throws Exception {
        dangKyThanhCong(USERNAME, EMAIL, SDT);
        int soTaiKhoan = demTaiKhoan();
        int soHoSo = demHoSo();

        Map<String, Object> trung = thanhVien("it_khach_b", "it_khach_b@example.com", SDT);
        MvcResult ketQua = dangKy(trung);

        assertLoiEnvelope(ketQua, 409);
        assertThat(demTaiKhoan()).isEqualTo(soTaiKhoan);
        assertThat(demHoSo()).isEqualTo(soHoSo);
        assertThat(demTheoUsername("it_khach_b")).isZero();
    }

    // ---------- Đăng nhập ----------

    @Test
    void dangNhapSaiMatKhauHoacSaiUsernameTra401VaKhongCapCookie() throws Exception {
        dangKyThanhCong(USERNAME, EMAIL, SDT);

        MvcResult saiMatKhau = dangNhap(USERNAME, "SaiMatKhau@123");
        assertLoiEnvelope(saiMatKhau, 401);
        assertKhongCoCookie(saiMatKhau);

        MvcResult saiUsername = dangNhap("it_khong_ton_tai", MAT_KHAU);
        assertLoiEnvelope(saiUsername, 401);
        assertKhongCoCookie(saiUsername);
    }

    @Test
    void dangNhapThieuThongTinTraEnvelope400VaKhongTaoPhien() throws Exception {
        List<String> bodies = List.of(
                "{}",
                "{\"username\":\"" + USERNAME + "\"}",
                "{\"password\":\"" + MAT_KHAU + "\"}",
                "{\"username\":\"   \",\"password\":\"" + MAT_KHAU + "\"}",
                "{\"username\":\"" + USERNAME + "\",\"password\":\"   \"}");

        for (String body : bodies) {
            MvcResult ketQua = mvc.perform(post(LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                    .content(body)).andReturn();
            assertLoiEnvelope(ketQua, 400);
            assertKhongCoCookie(ketQua);
            assertThat(ketQua.getRequest().getSession(false)).isNull();
            assertThat(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .doesNotContain("Exception");
        }
    }

    @Test
    void dangNhapSaiKhongCapCookieVaKhongThuHoiPhienDangCo() throws Exception {
        dangKyThanhCong(USERNAME, EMAIL, SDT);
        MvcResult thanhCong = dangNhap(USERNAME, MAT_KHAU);
        String refresh = refreshToken(thanhCong);

        MvcResult sai = mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, refresh))
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", "SaiMatKhau@123"))))
                .andReturn();

        assertLoiEnvelope(sai, 401);
        assertKhongCoCookie(sai);

        // Sai mật khẩu không được thu hồi phiên đang hợp lệ của cookie hiện tại.
        MvcResult lamMoi = lamMoi(refresh);
        assertThat(lamMoi.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(lamMoi).path("data").path("accessToken").asText()).isNotBlank();
    }

    @Test
    void dangNhapDungTraHoSoVaPhienJwtTuServer() throws Exception {
        int id = dangKyThanhCong(USERNAME, EMAIL, SDT);
        // Điểm/hạng đổi ở server: response phải phản chiếu dữ liệu server, không phải client.
        jdbc.update("UPDATE tblKhachHang SET tongDiem = 250, hangThanhVien = 'VANG' WHERE tblThanhVienId = ?", id);

        Map<String, Object> gui = new LinkedHashMap<>();
        gui.put("username", USERNAME);
        gui.put("password", MAT_KHAU);
        gui.put("id", 999_999);
        gui.put("vaiTro", "ADMIN");
        gui.put("tongDiem", 999_999);
        gui.put("hangThanhVien", "KIM_CUONG");

        MvcResult ketQua = guiDangNhap(gui);
        JsonNode body = doc(ketQua);

        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("success").asBoolean()).isTrue();
        JsonNode data = body.path("data");
        JsonNode hoSo = data.path("khachHang");
        assertThat(hoSo.path("id").asInt()).isEqualTo(id);
        assertThat(hoSo.path("username").asText()).isEqualTo(USERNAME);
        assertThat(hoSo.path("vaiTro").asText()).isEqualTo("KHACH_HANG");
        assertThat(hoSo.path("tongDiem").asInt()).isEqualTo(250);
        assertThat(hoSo.path("hangThanhVien").asText()).isEqualTo("VANG");
        assertThat(hoSo.path("ngayDangKy").asText()).isNotBlank();
        assertThat(hoSo.has("password")).isFalse();

        // Hợp đồng §7.3: access token + hạn access + hạn phiên tuyệt đối + id phiên.
        assertThat(data.path("accessToken").asText()).matches("[\\w-]+\\.[\\w-]+\\.[\\w-]+");
        Instant hetHanAccess = Instant.parse(data.path("expiresAt").asText());
        Instant hetHanPhien = Instant.parse(data.path("sessionExpiresAt").asText());
        assertThat(hetHanAccess).isAfter(Instant.now().minusSeconds(5));
        assertThat(hetHanPhien).isAfter(hetHanAccess);
        String sessionId = data.path("sessionId").asText();
        assertThat(sessionId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        String raw = ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain(MAT_KHAU);
        assertThat(raw).doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("$2y$");

        // Cookie refresh: HttpOnly, SameSite=Strict, Path /api/auth, host-only, không lộ trong body.
        String cookie = setCookie(ketQua);
        assertThat(cookie)
                .startsWith(AuthCookie.TEN + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth")
                .doesNotContain("Domain=");
        String refresh = refreshToken(ketQua);
        assertThat(raw).doesNotContain(refresh);
        assertThat(ketQua.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(ketQua.getRequest().getSession(false)).isNull();

        // DB chỉ lưu SHA-256 của refresh token.
        String hash = jdbc.queryForObject(
                "SELECT refreshTokenHash FROM tblPhienDangNhap WHERE id = ?", String.class, sessionId);
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").isNotEqualTo(refresh);
        assertThat(refresh.split("\\.")).hasSize(3);
    }

    @Test
    void dangNhapLaiTuCungCookieThuHoiPhienCuKhongAnhHuongThietBiKhac() throws Exception {
        int id = dangKyThanhCong(USERNAME, EMAIL, SDT);

        MvcResult trinhDuyet = guiDangNhap(Map.of("username", USERNAME, "password", MAT_KHAU));
        assertThat(trinhDuyet.getResponse().getStatus()).isEqualTo(200);
        String sessionCu = doc(trinhDuyet).path("data").path("sessionId").asText();
        String refreshCu = refreshToken(trinhDuyet);

        MvcResult thietBiKhac = guiDangNhap(Map.of("username", USERNAME, "password", MAT_KHAU));
        assertThat(doc(thietBiKhac).path("data").path("sessionId").asText()).isNotEqualTo(sessionCu);
        String refreshThietBiKhac = refreshToken(thietBiKhac);

        MvcResult dangNhapLai = guiDangNhapBangCookie(
                Map.of("username", USERNAME, "password", MAT_KHAU), refreshCu);
        assertThat(dangNhapLai.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(dangNhapLai).path("data").path("sessionId").asText()).isNotEqualTo(sessionCu);
        assertThat(doc(dangNhapLai).path("data").path("khachHang").path("id").asInt()).isEqualTo(id);

        // Phiên cũ của đúng trình duyệt đó bị thu hồi; thiết bị khác vẫn dùng bình thường.
        assertThat(lamMoi(refreshCu).getResponse().getStatus()).isEqualTo(401);
        MvcResult lamMoiThietBiKhac = lamMoi(refreshThietBiKhac);
        assertThat(lamMoiThietBiKhac.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(lamMoiThietBiKhac).path("data").path("khachHang").path("id").asInt()).isEqualTo(id);
    }

    // ---------- Ghi hồ sơ thất bại phải rollback cả tài khoản ----------

    @Test
    void loiGhiHoSoThuHaiRollbackCaTaiKhoan() throws Exception {
        // Ràng buộc CHECK thật trên bảng hồ sơ: INSERT thứ hai chắc chắn lỗi DB, INSERT thứ nhất vẫn hợp lệ.
        jdbc.execute("ALTER TABLE tblKhachHang ADD CONSTRAINT ck_tmp_auth_fail CHECK (tblThanhVienId < 0)");
        MvcResult ketQua = null;
        Throwable loi = null;
        try {
            ketQua = dangKy(thanhVien(USERNAME, EMAIL, SDT));
        } catch (Exception e) {
            // Lỗi không được controller xử lý (không phải 409 trùng khóa) cũng là phản hồi thất bại;
            // điều cốt lõi là lỗi xuất phát từ DB và trạng thái DB đã rollback.
            loi = e;
        } finally {
            jdbc.execute("ALTER TABLE tblKhachHang DROP CONSTRAINT ck_tmp_auth_fail");
        }

        if (loi != null) {
            assertThat(coTrongChuoiNguyenNhan(loi, DataIntegrityViolationException.class))
                    .as("lỗi phải là vi phạm ràng buộc DB, không phải lỗi dữ liệu đầu vào")
                    .isTrue();
        } else {
            assertThat(ketQua.getResponse().getStatus()).isEqualTo(500);
        }
        assertThat(demTheoUsername(USERNAME)).isZero();
        assertThat(demTaiKhoan()).isZero();
        assertThat(demHoSo()).isZero();
    }

    // ---------- Helpers ----------

    private Map<String, Object> thanhVien(String username, String email, String sdt) {
        Map<String, Object> kh = new LinkedHashMap<>();
        kh.put("username", username);
        kh.put("password", MAT_KHAU);
        kh.put("hoTen", "Nguyễn Văn Kiểm Thử");
        kh.put("ngaySinh", "2000-05-20");
        kh.put("gioiTinh", "Nam");
        kh.put("diaChi", "12 Lê Lợi, Quận 1");
        kh.put("email", email);
        kh.put("sdt", sdt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kh", kh);
        body.put("xacNhanMatKhau", MAT_KHAU);
        return body;
    }

    private MvcResult dangKy(Map<String, Object> body) throws Exception {
        return mvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))).andReturn();
    }

    private MvcResult dangNhap(String username, String password) throws Exception {
        return guiDangNhap(Map.of("username", username, "password", password));
    }

    private MvcResult guiDangNhap(Map<String, Object> body) throws Exception {
        return mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .content(json.writeValueAsBytes(body))).andReturn();
    }

    private MvcResult guiDangNhapBangCookie(Map<String, Object> body, String refreshToken) throws Exception {
        return mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, refreshToken))
                .content(json.writeValueAsBytes(body))).andReturn();
    }

    private MvcResult lamMoi(String refreshToken) throws Exception {
        return mvc.perform(post(REFRESH)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, refreshToken))).andReturn();
    }

    /** Header Set-Cookie của response (login/refresh/logout chỉ đặt tối đa một cookie). */
    private static String setCookie(MvcResult ketQua) {
        return ketQua.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    /** Giá trị refresh token trong Set-Cookie; fail rõ ràng nếu response không cấp cookie. */
    private static String refreshToken(MvcResult ketQua) {
        String header = setCookie(ketQua);
        assertThat(header)
                .as("Set-Cookie cho %s", ketQua.getRequest().getRequestURI())
                .isNotNull();
        String tienTo = AuthCookie.TEN + "=";
        int batDau = header.indexOf(tienTo);
        assertThat(batDau).as("Set-Cookie phải chứa %s: %s", AuthCookie.TEN, header).isGreaterThanOrEqualTo(0);
        int ketThuc = header.indexOf(';', batDau);
        return ketThuc < 0
                ? header.substring(batDau + tienTo.length())
                : header.substring(batDau + tienTo.length(), ketThuc);
    }

    /** Đăng ký hợp lệ và trả id tài khoản đã lưu. */
    private int dangKyThanhCong(String username, String email, String sdt) throws Exception {
        MvcResult ketQua = dangKy(thanhVien(username, email, sdt));
        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        return doc(ketQua).path("data").path("id").asInt();
    }

    private JsonNode doc(MvcResult ketQua) throws Exception {
        return json.readTree(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertLoiEnvelope(MvcResult ketQua, int status) throws Exception {
        assertThat(ketQua.getResponse().getStatus())
                .as("HTTP status cho %s", ketQua.getRequest().getRequestURI())
                .isEqualTo(status);
        JsonNode body = doc(ketQua);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.hasNonNull("data")).isFalse();
        assertThat(body.path("message").asText()).isNotBlank();
    }

    /** Response lỗi/không đăng nhập không được cấp cookie refresh. */
    private void assertKhongCoCookie(MvcResult ketQua) {
        assertThat(ketQua.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .as("Set-Cookie cho %s", ketQua.getRequest().getRequestURI())
                .isNull();
    }

    /** {@code true} khi {@code loai} xuất hiện ở chính {@code loi} hoặc trong chuỗi nguyên nhân của nó. */
    private static boolean coTrongChuoiNguyenNhan(Throwable loi, Class<? extends Throwable> loai) {
        for (Throwable t = loi; t != null && t != t.getCause(); t = t.getCause()) {
            if (loai.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    /** Hồ sơ đã lưu, đọc lại từ DB theo tên cột (JDBC tìm tên cột không phân biệt hoa/thường). */
    private HoSo hoSoTheoUsername(String username) {
        return jdbc.queryForObject("""
                SELECT tv.id AS id, tv.email AS email, tv.password AS password, tv.vaiTro AS vaitro,
                       kh.ngayDangKy AS ngaydangky, kh.tongDiem AS tongdiem, kh.hangThanhVien AS hangthanhvien
                FROM tblThanhVien tv
                JOIN tblKhachHang kh ON kh.tblThanhVienId = tv.id
                WHERE tv.username = ?""", (rs, i) -> new HoSo(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("vaitro"),
                rs.getDate("ngaydangky").toLocalDate(),
                rs.getInt("tongdiem"),
                rs.getString("hangthanhvien")), username);
    }

    private record HoSo(int id, String email, String password, String vaiTro, LocalDate ngayDangKy,
                        int tongDiem, String hangThanhVien) {
    }

    private int demTaiKhoan() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tblThanhVien", Integer.class);
    }

    private int demHoSo() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tblKhachHang", Integer.class);
    }

    private int demTheoUsername(String username) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tblThanhVien WHERE username = ?", Integer.class, username);
    }

    private int demTheoEmail(String email) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tblThanhVien WHERE email = ?", Integer.class, email);
    }

    private static String sdtTheoChiSo(int chiSo) {
        return String.format("09000000%02d", chiSo);
    }
}
