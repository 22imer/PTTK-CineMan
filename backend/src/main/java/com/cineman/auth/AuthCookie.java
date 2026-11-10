package com.cineman.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie refresh token (ARCHITECTURE §7.3): {@code CINEMAN_REFRESH}, HttpOnly, SameSite=Strict,
 * host-only (không Domain), {@code Path=/api/auth} để không gửi kèm API nghiệp vụ. {@code Max-Age} đúng
 * bằng thời gian còn lại của phiên tuyệt đối nên cookie không sống lâu hơn phiên; {@code Secure} bật
 * theo cấu hình cho HTTPS. Test dùng {@link Clock} để mốc thời gian tất định.
 */
@Component
public class AuthCookie {

    public static final String TEN = "CINEMAN_REFRESH";
    public static final String DUONG_DAN = "/api/auth";

    private final boolean secure;
    private final Clock clock;

    public AuthCookie(@Value("${auth.cookie-secure:false}") boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    /** Cookie phát hành sau login/refresh: hạn bằng phần còn lại của phiên, tối thiểu 0. */
    public ResponseCookie tao(String refreshToken, Instant sessionHetHan) {
        long conLaiGiay = Math.max(0L, Duration.between(bayGio(), sessionHetHan).getSeconds());
        return ResponseCookie.from(TEN, refreshToken)
                .httpOnly(true)
                .sameSite("Strict")
                .path(DUONG_DAN)
                .secure(secure)
                .maxAge(Duration.ofSeconds(conLaiGiay))
                .build();
    }

    /** Xóa cookie khi logout (Max-Age=0), giữ nguyên các thuộc tính bảo vệ. */
    public ResponseCookie xoa() {
        return ResponseCookie.from(TEN, "")
                .httpOnly(true)
                .sameSite("Strict")
                .path(DUONG_DAN)
                .secure(secure)
                .maxAge(Duration.ZERO)
                .build();
    }

    /** Giá trị refresh token từ cookie của request; {@code null} nếu không có/rỗng. */
    public static String doc(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TEN.equals(cookie.getName())) {
                String giaTri = cookie.getValue();
                return giaTri == null || giaTri.isBlank() ? null : giaTri;
            }
        }
        return null;
    }

    private Instant bayGio() {
        return Instant.ofEpochMilli(clock.millis());
    }
}
