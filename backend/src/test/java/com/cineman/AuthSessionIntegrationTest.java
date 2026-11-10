package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.cineman.auth.AuthCookie;
import com.cineman.auth.AuthException;
import com.cineman.auth.AuthFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Vòng đời phiên JWT (ARCHITECTURE §7.3) trên H2 thật + MockMvc, thời gian điều khiển bằng
 * {@link TestClock}: đăng nhập, cookie refresh, hạn access 15 phút, hạn tuyệt đối 7 ngày, xoay token,
 * replay trong/ngoài cửa sổ 5 giây, token giả không thu hồi được phiên, logout có ràng buộc
 * {@code X-Auth-Session} và idempotent, CSRF/Origin, từ chối toàn cục path thô không chuẩn (mã hóa,
 * matrix param, traversal, '//', '/' cuối của route API), ràng buộc chủ sở hữu phiên và race refresh
 * đồng thời.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-session-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String REFRESH = "/api/auth/refresh";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String ME = "/api/auth/me";
    private static final String USERNAME = "annguyen2002";
    private static final String MAT_KHAU = "Matkhau@123";
    private static final Instant BASE = Instant.parse("2026-11-10T08:00:00Z");

    @TestConfiguration
    static class CauHinhDongHo {
        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock(BASE);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestClock dongHo;

    @Value("${auth.jwt-secret}")
    String jwtSecret;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void chuanBi() {
        dongHo.dat(BASE);
        jdbc.update("DELETE FROM tblPhienDangNhap");
    }

    // ---------- Đăng nhập & cookie ----------

    @Test
    void dangNhapTraEnvelopeCookieChiLuuHashVaKhongTaoJSESSIONID() throws Exception {
        MvcResult ketQua = dangNhap();
        JsonNode data = doc(ketQua).path("data");

        assertThat(ketQua.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(ketQua).path("success").asBoolean()).isTrue();
        assertThat(doc(ketQua).has("code")).isFalse();
        assertThat(data.path("khachHang").path("username").asText()).isEqualTo(USERNAME);
        assertThat(data.path("khachHang").has("password")).isFalse();
        assertThat(Instant.parse(data.path("expiresAt").asText())).isEqualTo(BASE.plus(Duration.ofMinutes(15)));
        assertThat(Instant.parse(data.path("sessionExpiresAt").asText())).isEqualTo(BASE.plus(Duration.ofDays(7)));
        String sessionId = data.path("sessionId").asText();
        assertThat(sessionId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(data.path("accessToken").asText()).matches("[\\w-]+\\.[\\w-]+\\.[\\w-]+");

        String cookie = setCookie(ketQua);
        assertThat(cookie)
                .startsWith(AuthCookie.TEN + "=")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth")
                .doesNotContain("Domain=")
                .doesNotContain("Secure");
        assertThat(ketQua.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(ketQua.getRequest().getSession(false)).isNull();

        String refresh = refreshToken(ketQua);
        assertThat(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain(refresh);
        String hash = jdbc.queryForObject(
                "SELECT refreshTokenHash FROM tblPhienDangNhap WHERE id = ?", String.class, sessionId);
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").isNotEqualTo(refresh);
        assertThat(jdbc.queryForObject(
                "SELECT previousTokenHash FROM tblPhienDangNhap WHERE id = ?", String.class, sessionId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT hetHan FROM tblPhienDangNhap WHERE id = ?", java.sql.Timestamp.class, sessionId)
                .toInstant()).isEqualTo(BASE.plus(Duration.ofDays(7)));
    }

    @Test
    void cookieMotMinhKhongXacThucNhungBearerDuoc() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String access = accessToken(dangNhap);

        MvcResult chiCookie = mvc.perform(get("/api/phim")
                .cookie(new Cookie(AuthCookie.TEN, refresh))).andReturn();
        assertThat(chiCookie.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(chiCookie).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);

        MvcResult khongToken = mvc.perform(get("/api/phim")).andReturn();
        assertThat(khongToken.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(khongToken).path("success").asBoolean()).isFalse();
        assertThat(doc(khongToken).path("message").asText()).isNotBlank();

        MvcResult coBearer = mvc.perform(get("/api/phim")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)).andReturn();
        assertThat(coBearer.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(coBearer).path("success").asBoolean()).isTrue();

        MvcResult me = mvc.perform(get(ME)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)).andReturn();
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(me).path("data").path("id").asInt()).isEqualTo(1);
        assertThat(doc(me).path("data").has("password")).isFalse();

        MvcResult meChiCookie = mvc.perform(get(ME)
                .cookie(new Cookie(AuthCookie.TEN, refresh))).andReturn();
        assertThat(meChiCookie.getResponse().getStatus()).isEqualTo(401);
    }

    // ---------- Hạn access 15 phút & làm mới ----------

    @Test
    void accessHetHanDungBienTraAccessTokenExpiredVaRefreshKhoiPhuc() throws Exception {
        MvcResult dangNhap = dangNhap();
        String access = accessToken(dangNhap);
        String refresh = refreshToken(dangNhap);
        String sessionId = sessionId(dangNhap);

        dongHo.tien(Duration.ofMinutes(15).minusMillis(1));
        assertThat(me(access).getResponse().getStatus()).isEqualTo(200);

        dongHo.tien(Duration.ofMillis(1));
        MvcResult hetHan = me(access);
        assertThat(hetHan.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(hetHan).path("code").asText()).isEqualTo(AuthException.MA_ACCESS_TOKEN_EXPIRED);

        MvcResult lamMoi = lamMoi(refresh);
        assertThat(lamMoi.getResponse().getStatus()).isEqualTo(200);
        JsonNode data = doc(lamMoi).path("data");
        assertThat(data.path("sessionId").asText()).isEqualTo(sessionId);
        assertThat(Instant.parse(data.path("expiresAt").asText()))
                .isEqualTo(BASE.plus(Duration.ofMinutes(15)).plus(Duration.ofMinutes(15)));
        assertThat(Instant.parse(data.path("sessionExpiresAt").asText())).isEqualTo(BASE.plus(Duration.ofDays(7)));
        assertThat(me(data.path("accessToken").asText()).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void refreshXoayTokenVaKhongKeoDaiHanPhien() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String sessionId = sessionId(dangNhap);

        dongHo.tien(Duration.ofDays(1));
        Instant bayGio = dongHo.instant();
        MvcResult lamMoi = lamMoi(refresh);
        assertThat(lamMoi.getResponse().getStatus()).isEqualTo(200);
        JsonNode data = doc(lamMoi).path("data");
        String refreshMoi = refreshToken(lamMoi);
        assertThat(refreshMoi).isNotEqualTo(refresh);
        assertThat(Instant.parse(data.path("sessionExpiresAt").asText())).isEqualTo(BASE.plus(Duration.ofDays(7)));
        assertThat(Instant.parse(data.path("expiresAt").asText())).isEqualTo(bayGio.plus(Duration.ofMinutes(15)));

        assertThat(jdbc.queryForObject(
                "SELECT previousTokenHash FROM tblPhienDangNhap WHERE id = ?", String.class, sessionId))
                .isEqualTo(bam(refresh));
        assertThat(jdbc.queryForObject(
                "SELECT refreshTokenHash FROM tblPhienDangNhap WHERE id = ?", String.class, sessionId))
                .isEqualTo(bam(refreshMoi))
                .isNotEqualTo(bam(refresh));
        assertThat(jdbc.queryForObject(
                "SELECT xoayLuc FROM tblPhienDangNhap WHERE id = ?", java.sql.Timestamp.class, sessionId)
                .toInstant()).isEqualTo(bayGio);
    }

    // ---------- Replay & tranh chấp ----------

    @Test
    void refreshLapTrong5GiayTra409KhongDoiCookieRoiNgoaiCuaSoThuHoiPhien() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refreshCu = refreshToken(dangNhap);
        MvcResult lamMoi = lamMoi(refreshCu);
        assertThat(lamMoi.getResponse().getStatus()).isEqualTo(200);
        String refreshMoi = refreshToken(lamMoi);
        String accessMoi = accessToken(lamMoi);

        // Đúng biên 5 giây: vẫn là trùng lặp → 409, không Set-Cookie, phiên không bị thu hồi.
        dongHo.tien(Duration.ofSeconds(5));
        MvcResult trung = lamMoi(refreshCu);
        assertThat(trung.getResponse().getStatus()).isEqualTo(409);
        assertThat(doc(trung).path("code").asText()).isEqualTo(AuthException.MA_REFRESH_CONFLICT);
        assertThat(trung.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(me(accessMoi).getResponse().getStatus()).isEqualTo(200);

        // Quá cửa sổ 1ms: replay thật → thu hồi phiên, trả 401 cho cả refresh và access đang còn hạn.
        dongHo.tien(Duration.ofMillis(1));
        MvcResult lap = lamMoi(refreshCu);
        assertThat(lap.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(lap).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);
        assertThat(lap.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(lamMoi(refreshMoi).getResponse().getStatus()).isEqualTo(401);
        MvcResult accessSauThuHoi = me(accessMoi);
        assertThat(accessSauThuHoi.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(accessSauThuHoi).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);
    }

    @Test
    void refreshDongThoiCungTokenMotThanhCongMotXungDot() throws Exception {
        String refresh = refreshToken(dangNhap());
        CountDownLatch sanSang = new CountDownLatch(2);
        CountDownLatch batDau = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var viec = (java.util.concurrent.Callable<Integer>) () -> {
                sanSang.countDown();
                batDau.await();
                return lamMoi(refresh).getResponse().getStatus();
            };
            var mot = pool.submit(viec);
            var hai = pool.submit(viec);
            sanSang.await();
            batDau.countDown();
            assertThat(List.of(mot.get(), hai.get()))
                    .containsExactlyInAnyOrder(200, 409);
        }
    }

    // ---------- Token giả / sai loại / sai chủ ----------

    @Test
    void tokenGiaHoacSaiChuKhongThuHoiDuocPhien() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String access = accessToken(dangNhap);
        String sessionId = sessionId(dangNhap);

        // Chữ ký bị sửa: 401 nhưng phiên vẫn sống.
        MvcResult giaChuKy = lamMoi(lamSaiChuKy(refresh));
        assertThat(giaChuKy.getResponse().getStatus()).isEqualTo(401);
        MvcResult lanHai = lamMoi(refresh);
        assertThat(lanHai.getResponse().getStatus()).isEqualTo(200);
        String refreshHienTai = refreshToken(lanHai);

        // Token ký bằng khóa khác nhưng cùng sid/sub: không được phép thu hồi phiên.
        String khoaKhac = kyToken("another-secret-32-bytes-minimum-000000", "cineman-refresh", "refresh",
                "1", sessionId, BASE.plus(Duration.ofDays(7)));
        assertThat(lamMoi(khoaKhac).getResponse().getStatus()).isEqualTo(401);
        MvcResult sauDo = lamMoi(refreshHienTai);
        assertThat(sauDo.getResponse().getStatus()).isEqualTo(200);

        // Refresh token dùng như Bearer access, và access ký đúng khóa nhưng sai subject/sid → 401.
        assertThat(me(refresh).getResponse().getStatus()).isEqualTo(401);
        assertThat(me(kyToken(jwtSecret, "cineman-api", "access", "2", sessionId,
                BASE.plus(Duration.ofMinutes(15)))).getResponse().getStatus()).isEqualTo(401);
        assertThat(me(kyToken(jwtSecret, "cineman-api", "access", "1", UUID.randomUUID().toString(),
                BASE.plus(Duration.ofMinutes(15)))).getResponse().getStatus()).isEqualTo(401);
        assertThat(me(kyToken(jwtSecret, "cineman-refresh", "refresh", "1", sessionId,
                BASE.plus(Duration.ofDays(7)))).getResponse().getStatus()).isEqualTo(401);
        assertThat(me(access).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void refreshKhongCoCookieHoacCookieRongTra401() throws Exception {
        MvcResult khongCookie = mvc.perform(post(REFRESH)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)).andReturn();
        assertThat(khongCookie.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(khongCookie).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);

        MvcResult cookieRong = mvc.perform(post(REFRESH)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, ""))).andReturn();
        assertThat(cookieRong.getResponse().getStatus()).isEqualTo(401);
    }

    // ---------- Logout ----------

    @Test
    void logoutThuHoiCaAccessVaRefreshVaGoiLaiVanThanhCong() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String access = accessToken(dangNhap);
        String phien = sessionId(dangNhap);

        MvcResult dangXuat = dangXuat(refresh, phien, true);
        assertThat(dangXuat.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(dangXuat).path("success").asBoolean()).isTrue();
        assertThat(doc(dangXuat).hasNonNull("code")).isFalse();
        assertThat(setCookie(dangXuat)).contains(AuthCookie.TEN + "=").contains("Max-Age=0");

        MvcResult accessSauDangXuat = me(access);
        assertThat(accessSauDangXuat.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(accessSauDangXuat).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);
        assertThat(lamMoi(refresh).getResponse().getStatus()).isEqualTo(401);

        // Thử lại sau khi mất phản hồi: cùng cookie + cùng session mong đợi, phiên đã thu hồi → thành công.
        MvcResult thuLai = dangXuat(refresh, phien, true);
        assertThat(thuLai.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(thuLai).path("success").asBoolean()).isTrue();

        // Không có cookie: idempotent, không lỗi.
        MvcResult khongCookie = mvc.perform(post(LOGOUT)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)).andReturn();
        assertThat(khongCookie.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void logoutSaiPhienMongDoiTra409KhongThuHoiPhienMoi() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refreshCu = refreshToken(dangNhap);
        String phienCu = sessionId(dangNhap);

        // Tab khác đăng nhập lại trên cùng cookie: phiên cũ bị thu hồi, cookie trình duyệt là phiên mới.
        MvcResult dangNhapLai = dangNhapBangCookie(refreshCu);
        String refreshMoi = refreshToken(dangNhapLai);
        String phienMoi = sessionId(dangNhapLai);
        assertThat(phienMoi).isNotEqualTo(phienCu);
        assertThat(lamMoi(refreshCu).getResponse().getStatus()).isEqualTo(401);

        // Tab cũ vẫn giữ sessionId cũ: không được thu hồi phiên mới, không xóa cookie mới.
        MvcResult lech = dangXuat(refreshMoi, phienCu, true);
        assertThat(lech.getResponse().getStatus()).isEqualTo(409);
        assertThat(doc(lech).path("code").asText()).isEqualTo(AuthException.MA_SESSION_CHANGED);
        assertThat(lech.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();

        // Phiên mới vẫn sống; cookie đã xoay nhưng cùng phiên nên logout đúng vẫn thành công.
        MvcResult conSong = lamMoi(refreshMoi);
        assertThat(conSong.getResponse().getStatus()).isEqualTo(200);
        String refreshSau = refreshToken(conSong);
        assertThat(dangXuat(refreshSau, phienMoi, true).getResponse().getStatus()).isEqualTo(200);
        // Thử lại sau khi mất phản hồi (cùng cookie đã thu hồi + cùng phiên mong đợi) vẫn 200.
        MvcResult thuLai = dangXuat(refreshSau, phienMoi, true);
        assertThat(thuLai.getResponse().getStatus()).isEqualTo(200);
        assertThat(setCookie(thuLai)).contains("Max-Age=0");
    }

    @Test
    void logoutCookieDaThuHoiHoacHetHanNhungSaiPhienMongDoiVan409() throws Exception {
        MvcResult a = dangNhap();
        String refreshA = refreshToken(a);
        String phienA = sessionId(a);

        // Phiên B thay A trên cùng cookie, rồi B bị thu hồi bởi logout đúng.
        MvcResult b = dangNhapBangCookie(refreshA);
        String refreshB = refreshToken(b);
        String phienB = sessionId(b);
        assertThat(phienB).isNotEqualTo(phienA);
        assertThat(dangXuat(refreshB, phienB, true).getResponse().getStatus()).isEqualTo(200);

        // Cookie của phiên đã thu hồi nhưng client mong đợi phiên A: vẫn 409, không Set-Cookie.
        MvcResult lechB = dangXuat(refreshB, phienA, true);
        assertThat(lechB.getResponse().getStatus()).isEqualTo(409);
        assertThat(doc(lechB).path("code").asText()).isEqualTo(AuthException.MA_SESSION_CHANGED);
        assertThat(lechB.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();

        // Cùng sid đã ký + phiên đã thu hồi vẫn là retry idempotent 200.
        MvcResult dungB = dangXuat(refreshB, phienB, true);
        assertThat(dungB.getResponse().getStatus()).isEqualTo(200);
        assertThat(setCookie(dungB)).contains("Max-Age=0");

        // Phiên hết hạn tuyệt đối + expected lệch: vẫn 409 dù cookie không còn hiệu lực; trùng sid thì 200.
        MvcResult c = dangNhap();
        String refreshC = refreshToken(c);
        String phienC = sessionId(c);
        dongHo.tien(Duration.ofDays(7));
        MvcResult lechHetHan = dangXuat(refreshC, phienA, true);
        assertThat(lechHetHan.getResponse().getStatus()).isEqualTo(409);
        assertThat(doc(lechHetHan).path("code").asText()).isEqualTo(AuthException.MA_SESSION_CHANGED);
        assertThat(lechHetHan.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(dangXuat(refreshC, phienC, true).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void logoutThieuHeaderPhienKhongThuHoiCookieHopLe() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);

        MvcResult thieu = dangXuat(refresh, null, true);
        assertThat(thieu.getResponse().getStatus()).isEqualTo(409);
        assertThat(doc(thieu).path("code").asText()).isEqualTo(AuthException.MA_SESSION_CHANGED);
        assertThat(thieu.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(lamMoi(refresh).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void logoutBangCookieGiaKhongThuHoiPhienVaKhongLoThongTin() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String phien = sessionId(dangNhap);

        MvcResult dangXuat = dangXuat(lamSaiChuKy(refresh), phien, true);
        assertThat(dangXuat.getResponse().getStatus()).isEqualTo(200);
        assertThat(doc(dangXuat).path("success").asBoolean()).isTrue();
        assertThat(dangXuat.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("SaiChuKy").doesNotContain("Token");
        assertThat(lamMoi(refresh).getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- CSRF & Origin ----------

    @Test
    void csrfVaOriginDuocKiemTraTruocKhiTaoHoacXoayPhien() throws Exception {
        MvcResult thieuCsrf = mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                .andReturn();
        assertThat(thieuCsrf.getResponse().getStatus()).isEqualTo(403);
        assertThat(doc(thieuCsrf).path("code").asText()).isEqualTo(AuthException.MA_CSRF_REQUIRED);
        assertThat(thieuCsrf.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tblPhienDangNhap", Integer.class)).isZero();

        MvcResult originLa = mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                .andReturn();
        assertThat(originLa.getResponse().getStatus()).isEqualTo(403);
        assertThat(doc(originLa).path("code").asText()).isEqualTo(AuthException.MA_ORIGIN_NOT_ALLOWED);

        MvcResult originHopLe = mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                .andReturn();
        assertThat(originHopLe.getResponse().getStatus()).isEqualTo(200);
        String refresh = refreshToken(originHopLe);

        MvcResult refreshThieuCsrf = mvc.perform(post(REFRESH)
                .cookie(new Cookie(AuthCookie.TEN, refresh))).andReturn();
        assertThat(refreshThieuCsrf.getResponse().getStatus()).isEqualTo(403);
        assertThat(doc(refreshThieuCsrf).path("code").asText()).isEqualTo(AuthException.MA_CSRF_REQUIRED);

        MvcResult logoutThieuCsrf = mvc.perform(post(LOGOUT)
                .cookie(new Cookie(AuthCookie.TEN, refresh))).andReturn();
        assertThat(logoutThieuCsrf.getResponse().getStatus()).isEqualTo(403);

        // Bị chặn ở boundary không được làm thay đổi phiên: refresh với header hợp lệ vẫn xoay bình thường.
        assertThat(lamMoi(refresh).getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- Đường dẫn không chuẩn ----------

    @Test
    void duongDanKhongChuanBiTuChoiToanCucTruocCsrfVaXacThuc() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String access = accessToken(dangNhap);
        String phien = sessionId(dangNhap);
        int soPhien = jdbc.queryForObject("SELECT COUNT(*) FROM tblPhienDangNhap", Integer.class);
        int soTaiKhoan = jdbc.queryForObject("SELECT COUNT(*) FROM tblThanhVien", Integer.class);
        assertThat(soPhien).isEqualTo(1);

        // Lỗ hổng đã tái hiện: matrix param làm raw path trượt danh sách CSRF nhưng MVC vẫn route logout.
        MvcResult logoutMatrix = mvc.perform(post("/api/auth/logout")
                .with(duongDanTho("/api/auth/logout;x=1"))
                .cookie(new Cookie(AuthCookie.TEN, refresh))
                .header(AuthFilter.HEADER_PHIEN, phien))
                .andReturn();
        assertThat(logoutMatrix.getResponse().getStatus()).isEqualTo(400);
        assertThat(doc(logoutMatrix).path("code").asText()).isEqualTo(AuthException.MA_INVALID_PATH);
        assertThat(logoutMatrix.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(me(access).getResponse().getStatus()).isEqualTo(200);

        // Login mã hóa/matrix param bị chặn trước khi tạo phiên, không cấp cookie.
        for (String uri : List.of("/api/auth/%6cogin", "/api/auth/login;x=1")) {
            MvcResult login = mvc.perform(post("/api/auth/login")
                    .with(duongDanTho(uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                    .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                    .andReturn();
            assertThat(login.getResponse().getStatus()).as(uri).isEqualTo(400);
            assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
            assertThat(doc(login).path("code").asText()).isEqualTo(AuthException.MA_INVALID_PATH);
        }

        // Đăng ký noncanonical không tạo tài khoản.
        MvcResult dangKyMatrix = mvc.perform(post("/api/auth/register")
                .with(duongDanTho("/api/auth/register;x=1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andReturn();
        assertThat(dangKyMatrix.getResponse().getStatus()).isEqualTo(400);

        // Path thô không chuẩn bị từ chối 400 trước route/auth: không Bearer cũng không được trả 401.
        for (String uri : List.of("/%61pi/phim", "/%2561pi/phim", "/api/../outside",
                "/api/./phim", "/api//phim", "/api/phim/")) {
            MvcResult khongChuan = mvc.perform(get("/api/phim").with(duongDanTho(uri))).andReturn();
            assertThat(khongChuan.getResponse().getStatus()).as(uri).isEqualTo(400);
            assertThat(doc(khongChuan).path("success").asBoolean()).as(uri).isFalse();
            assertThat(doc(khongChuan).path("code").asText()).as(uri)
                    .isEqualTo(AuthException.MA_INVALID_PATH);
        }

        // Có Bearer hợp lệ vẫn bị chặn; path ngoài API chứa mã hóa cũng bị từ chối (backend chỉ REST ASCII).
        assertThat(mvc.perform(get("/api/phim")
                .with(duongDanTho("/api/phim;x=1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)).andReturn()
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(mvc.perform(get("/").with(duongDanTho("/tinh%20nang"))).andReturn()
                .getResponse().getStatus()).isEqualTo(400);

        // Không thay đổi trạng thái: phiên/tài khoản y nguyên, phiên cũ vẫn sống và route chuẩn vẫn chạy.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tblPhienDangNhap", Integer.class)).isEqualTo(soPhien);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tblThanhVien", Integer.class)).isEqualTo(soTaiKhoan);
        assertThat(lamMoi(refresh).getResponse().getStatus()).isEqualTo(200);
        assertThat(me(access).getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- Hạn tuyệt đối & hồ sơ tươi ----------

    @Test
    void phienHetHanTuyetDoiSau7NgayKhongGiaHan() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        String access = accessToken(dangNhap);

        // Còn 1 giây trước mốc tuyệt đối: refresh vẫn được, nhưng hạn phiên không được kéo dài.
        dongHo.tien(Duration.ofDays(7).minusSeconds(1));
        MvcResult conHan = lamMoi(refresh);
        assertThat(conHan.getResponse().getStatus()).isEqualTo(200);
        JsonNode dataConHan = doc(conHan).path("data");
        assertThat(Instant.parse(dataConHan.path("sessionExpiresAt").asText()))
                .isEqualTo(BASE.plus(Duration.ofDays(7)));
        // Access token không được sống quá hạn tuyệt đối của phiên (còn 1 giây → exp = hạn phiên).
        assertThat(Instant.parse(dataConHan.path("expiresAt").asText()))
                .isEqualTo(Instant.parse(dataConHan.path("sessionExpiresAt").asText()));
        String refreshHienTai = refreshToken(conHan);

        // Đúng mốc 7 ngày: phiên hết hạn tuyệt đối, refresh và access đều SESSION_INVALID.
        dongHo.dat(BASE.plus(Duration.ofDays(7)));
        MvcResult refreshHetHan = lamMoi(refreshHienTai);
        assertThat(refreshHetHan.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(refreshHetHan).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);
        MvcResult accessHetHan = me(access);
        assertThat(accessHetHan.getResponse().getStatus()).isEqualTo(401);
        assertThat(doc(accessHetHan).path("code").asText()).isEqualTo(AuthException.MA_SESSION_INVALID);
    }

    @Test
    void refreshTraHoSoDocLaiTuDb() throws Exception {
        MvcResult dangNhap = dangNhap();
        String refresh = refreshToken(dangNhap);
        jdbc.update("UPDATE tblKhachHang SET tongDiem = 777, hangThanhVien = 'KIM_CUONG' WHERE tblThanhVienId = 1");
        try {
            MvcResult lamMoi = lamMoi(refresh);
            assertThat(lamMoi.getResponse().getStatus()).isEqualTo(200);
            assertThat(doc(lamMoi).path("data").path("khachHang").path("tongDiem").asInt()).isEqualTo(777);
            assertThat(doc(lamMoi).path("data").path("khachHang").path("hangThanhVien").asText())
                    .isEqualTo("KIM_CUONG");
            MvcResult me = me(accessToken(lamMoi));
            assertThat(doc(me).path("data").path("tongDiem").asInt()).isEqualTo(777);
        } finally {
            jdbc.update("UPDATE tblKhachHang SET tongDiem = 250, hangThanhVien = NULL WHERE tblThanhVienId = 1");
        }
    }

    // ---------- Helpers ----------

    private MvcResult dangNhap() throws Exception {
        return mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                .andReturn();
    }

    private MvcResult lamMoi(String refreshToken) throws Exception {
        return mvc.perform(post(REFRESH)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, refreshToken))).andReturn();
    }

    private MvcResult dangNhapBangCookie(String refreshToken) throws Exception {
        return mvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF)
                .cookie(new Cookie(AuthCookie.TEN, refreshToken))
                .content(json.writeValueAsBytes(Map.of("username", USERNAME, "password", MAT_KHAU))))
                .andReturn();
    }

    private MvcResult dangXuat(String refreshToken, String sessionMongDoi, boolean coCsrf) throws Exception {
        var request = post(LOGOUT).cookie(new Cookie(AuthCookie.TEN, refreshToken));
        if (coCsrf) {
            request.header(AuthFilter.HEADER_CSRF, AuthFilter.GIA_TRI_CSRF);
        }
        if (sessionMongDoi != null) {
            request.header(AuthFilter.HEADER_PHIEN, sessionMongDoi);
        }
        return mvc.perform(request).andReturn();
    }

    private MvcResult me(String accessToken) throws Exception {
        return mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andReturn();
    }

    /** Đặt request URI thô để kiểm tra đường dẫn mã hóa/matrix param đúng như container nhận. */
    private static RequestPostProcessor duongDanTho(String uri) {
        return request -> {
            request.setRequestURI(uri);
            return request;
        };
    }

    private JsonNode doc(MvcResult ketQua) throws Exception {
        return json.readTree(ketQua.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String accessToken(MvcResult ketQua) throws Exception {
        return doc(ketQua).path("data").path("accessToken").asText();
    }

    private String sessionId(MvcResult ketQua) throws Exception {
        return doc(ketQua).path("data").path("sessionId").asText();
    }

    private static String setCookie(MvcResult ketQua) {
        return ketQua.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private static String refreshToken(MvcResult ketQua) {
        String header = setCookie(ketQua);
        assertThat(header).as("Set-Cookie cho %s", ketQua.getRequest().getRequestURI()).isNotNull();
        String tienTo = AuthCookie.TEN + "=";
        int batDau = header.indexOf(tienTo);
        assertThat(batDau).as("Set-Cookie phải chứa %s: %s", AuthCookie.TEN, header).isGreaterThanOrEqualTo(0);
        int ketThuc = header.indexOf(';', batDau);
        return ketThuc < 0
                ? header.substring(batDau + tienTo.length())
                : header.substring(batDau + tienTo.length(), ketThuc);
    }

    /** SHA-256 hex giống {@code JwtTokenService.bam} để đối chiếu giá trị lưu DB. */
    private static String bam(String token) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }

    /** Sửa một ký tự đầu của phần chữ ký để chắc chắn byte chữ ký khác (không chỉ bit đệm base64url). */
    private static String lamSaiChuKy(String token) {
        int viTri = token.lastIndexOf('.') + 1;
        char goc = token.charAt(viTri);
        char thay = goc == 'A' ? 'B' : 'A';
        return token.substring(0, viTri) + thay + token.substring(viTri + 1);
    }

    private static String kyToken(String secret, String audience, String loai, String subject, String sid,
                                  Instant hetHan) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("cineman")
                .audience(audience)
                .subject(subject)
                .claim("sid", sid)
                .claim("token_type", loai)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(BASE))
                .expirationTime(Date.from(hetHan))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
