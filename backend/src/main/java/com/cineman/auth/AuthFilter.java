package com.cineman.auth;

import com.cineman.controller.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Cổng xác thực duy nhất trước controller (ARCHITECTURE §7.3) — thay hoàn toàn {@code HttpSession}.
 *
 * <p>Với mọi {@code /api/**} (trừ các endpoint auth công khai), yêu cầu phải có Bearer access token hợp
 * lệ VÀ phiên DB tương ứng còn sống; danh tính đã xác thực được gắn vào request attribute
 * {@link PhienXacThuc#KHOA_REQUEST}. {@code /api/auth/me} cũng đi qua đây; login/refresh/logout chỉ cần
 * CSRF + Origin nên vẫn dùng được khi access token đã hết hạn.
 *
 * <p>POST login/refresh/logout bắt buộc header {@code X-CSRF-Protection: 1}; khi request có
 * {@code Origin} thì phải nằm trong {@code auth.allowed-origins}. Mọi response auth gắn
 * {@code Cache-Control: no-store}. Lỗi xác thực trả envelope JSON có {@code code} để client phân biệt
 * access hết hạn với phiên bị thu hồi, không lộ token/hash/stacktrace.
 *
 * <p>Filter chỉ phục vụ REST ASCII của backend, nên quyết định trên đúng path thô sau khi bỏ context
 * path: mọi path chứa '%' (mã hóa), ';' (matrix param), '\\', '//', segment '.'/'..' hoặc '/' cuối của
 * route API bị từ chối 400 trước mọi bước bỏ qua filter/CSRF/Bearer. Không giải mã hay dựng lại đường
 * dẫn — route MVC nhận đúng chuỗi đã kiểm tra, nên không tồn tại khoảng lệch để lách. Logout còn đối
 * chiếu header {@code X-Auth-Session} với {@code sid} trong refresh cookie ở controller để không thu hồi
 * nhầm phiên mới của tab khác.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthFilter extends OncePerRequestFilter {

    public static final String HEADER_CSRF = "X-CSRF-Protection";
    public static final String GIA_TRI_CSRF = "1";
    /** Phiên client mong đợi khi logout; filter/controller đối chiếu với sid trong refresh cookie. */
    public static final String HEADER_PHIEN = "X-Auth-Session";
    private static final String DUONG_DAN_ME = "/api/auth/me";
    private static final String GOC_API = "/api";
    private static final String TIEN_TO_API = "/api/";
    private static final String TIEN_TO_AUTH = "/api/auth/";
    private static final String TIEN_TO_BEARER = "Bearer ";
    private static final Set<String> POST_CAN_CSRF =
            Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PhienDangNhapService phienService;
    private final Set<String> allowedOrigins;

    public AuthFilter(PhienDangNhapService phienService,
                      @Value("${auth.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
                      List<String> allowedOrigins) {
        this.phienService = phienService;
        this.allowedOrigins = Set.copyOf(allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String duongDan = duongDanRaw(request);
        if (duongDan == null || !duongDan.startsWith("/") || duongDanKhongChuan(duongDan)) {
            // Từ chối trước cả quyết định "bỏ qua filter" lẫn CSRF/Bearer: không có đường dẫn thô chuẩn
            // thì không thể biết route MVC sẽ khớp gì.
            ghiLoi(response, HttpStatus.BAD_REQUEST,
                    AuthException.THONG_BAO_INVALID_PATH, AuthException.MA_INVALID_PATH);
            return;
        }
        if (!laApi(duongDan)) {
            chain.doFilter(request, response);
            return;
        }
        if (duongDan.startsWith(TIEN_TO_AUTH)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        }
        if ("POST".equals(request.getMethod()) && POST_CAN_CSRF.contains(duongDan) && !csrfHopLe(request, response)) {
            return;
        }
        if (!canXacThuc(duongDan)) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, TIEN_TO_BEARER, 0, TIEN_TO_BEARER.length())) {
            ghiLoi(response, HttpStatus.UNAUTHORIZED,
                    AuthException.THONG_BAO_SESSION_INVALID, AuthException.MA_SESSION_INVALID);
            return;
        }
        try {
            PhienXacThuc phien = phienService.xacThucAccessToken(header.substring(TIEN_TO_BEARER.length()).trim());
            request.setAttribute(PhienXacThuc.KHOA_REQUEST, phien);
        } catch (AuthException loi) {
            ghiLoi(response, loi.getStatus(), loi.getMessage(), loi.getMa());
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Đường dẫn thô không chuẩn — bị từ chối trước mọi bước khác. Backend chỉ có REST ASCII nên không
     * cần giải mã/dựng lại path: chính vì MVC có thể giải mã và bỏ matrix param khi khớp route, mọi
     * chuỗi mà nó còn có thể diễn giải khác đi đều bị chặn tại đây.
     */
    private static boolean duongDanKhongChuan(String duongDan) {
        if (duongDan.indexOf('%') >= 0 || duongDan.indexOf(';') >= 0
                || duongDan.indexOf('\\') >= 0 || duongDan.contains("//")) {
            return true;
        }
        boolean coSegmentCham = duongDan.equals("/.") || duongDan.equals("/..")
                || duongDan.contains("/./") || duongDan.contains("/../")
                || duongDan.endsWith("/.") || duongDan.endsWith("/..");
        if (coSegmentCham) {
            return true;
        }
        // Các route API đều không có '/' cuối; nếu MVC có bật trailing-slash matching thì cũng bị chặn.
        return duongDan.startsWith(TIEN_TO_API) && duongDan.endsWith("/");
    }

    private static String duongDanRaw(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        String contextPath = request.getContextPath();
        return uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
    }

    private static boolean laApi(String duongDan) {
        return duongDan.equals(GOC_API) || duongDan.startsWith(TIEN_TO_API);
    }

    /** CSRF trước, rồi Origin nếu trình duyệt gửi kèm; trả {@code false} khi đã ghi lỗi. */
    private boolean csrfHopLe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String csrf = request.getHeader(HEADER_CSRF);
        if (csrf == null || !GIA_TRI_CSRF.equals(csrf.trim())) {
            ghiLoi(response, HttpStatus.FORBIDDEN,
                    AuthException.THONG_BAO_CSRF_REQUIRED, AuthException.MA_CSRF_REQUIRED);
            return false;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank() && !allowedOrigins.contains(origin.trim())) {
            ghiLoi(response, HttpStatus.FORBIDDEN,
                    AuthException.THONG_BAO_ORIGIN_NOT_ALLOWED, AuthException.MA_ORIGIN_NOT_ALLOWED);
            return false;
        }
        return true;
    }

    /** Chỉ endpoint nghiệp vụ và {@code /api/auth/me} cần Bearer; các endpoint auth khác dùng cookie/body. */
    private static boolean canXacThuc(String duongDan) {
        if (DUONG_DAN_ME.equals(duongDan)) {
            return true;
        }
        return laApi(duongDan) && !duongDan.startsWith(TIEN_TO_AUTH);
    }

    private static void ghiLoi(HttpServletResponse response, HttpStatus status, String thongBao, String ma)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write(JSON.writeValueAsString(ApiResponse.fail(thongBao, ma)));
    }
}
