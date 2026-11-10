package com.cineman.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Ký và kiểm tra JWT HS256 cho phiên đăng nhập (ARCHITECTURE §7.3).
 *
 * <p>Access và refresh là hai loại token tách biệt bằng claim {@code token_type} + {@code aud}:
 * access chỉ dùng cho API nghiệp vụ, refresh chỉ dùng ở {@code /api/auth/refresh|logout}. Nhờ vậy token
 * giả mạo hoặc token sai loại không thể tác động phiên, còn refresh token cũ (đã ký thật) vẫn nhận diện
 * được để thu hồi khi replay. Chữ ký/giải thuật/issuer/audience/loại/subject/sid được kiểm tra cố định;
 * hạn dùng so với {@link Clock} để test biên thời gian tất định. DB chỉ lưu {@link #bam(String)}.
 */
@Service
public class JwtTokenService {

    static final String ISSUER = "cineman";
    static final String AUD_ACCESS = "cineman-api";
    static final String AUD_REFRESH = "cineman-refresh";
    static final String LOAI_ACCESS = "access";
    static final String LOAI_REFRESH = "refresh";
    private static final String CLAIM_LOAI = "token_type";
    private static final String CLAIM_SID = "sid";
    private static final JWSAlgorithm THUAT_TOAN = JWSAlgorithm.HS256;
    private static final int KHOA_TOI_THIEU_BYTE = 32;

    /** Claims của access token sau khi đã xác thực chữ ký/issuer/audience/loại (chưa xét hạn). */
    public record AccessTokenClaims(int khachHangId, String sessionId, Instant hetHan) {
    }

    /** Claims của refresh token đã xác thực; {@code hetHan} là hạn phiên tuyệt đối. */
    public record RefreshTokenClaims(int khachHangId, String sessionId, Instant hetHan) {
    }

    private final MACSigner ky;
    private final MACVerifier xacThuc;
    private final Clock clock;

    public JwtTokenService(@Value("${auth.jwt-secret}") String jwtSecret, Clock clock) {
        byte[] khoa = jwtSecret == null ? new byte[0] : jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (khoa.length < KHOA_TOI_THIEU_BYTE) {
            throw new IllegalStateException(
                    "auth.jwt-secret (JWT_SECRET) bắt buộc và phải có ít nhất 32 byte UTF-8; "
                            + "không có khóa mặc định.");
        }
        try {
            this.ky = new MACSigner(khoa);
            this.xacThuc = new MACVerifier(khoa);
        } catch (JOSEException loi) {
            throw new IllegalStateException("auth.jwt-secret (JWT_SECRET) không dùng được cho HS256.", loi);
        }
        this.clock = clock;
    }

    public String taoAccessToken(String sessionId, int khachHangId, Instant phatHanh, Instant hetHan) {
        return tao(AUD_ACCESS, LOAI_ACCESS, sessionId, khachHangId, phatHanh, hetHan);
    }

    public String taoRefreshToken(String sessionId, int khachHangId, Instant phatHanh, Instant hetHan) {
        return tao(AUD_REFRESH, LOAI_REFRESH, sessionId, khachHangId, phatHanh, hetHan);
    }

    /**
     * Xác thực access token (chữ ký, HS256, issuer, audience, loại, subject, sid) và trả hạn dùng.
     * Không xét hạn ở đây để tầng phiên kiểm tra DB trước: phiên đã thu hồi phải trả SESSION_INVALID
     * thay vì ACCESS_TOKEN_EXPIRED (tránh client refresh vô ích).
     */
    public AccessTokenClaims docAccessToken(String token) {
        JWTClaimsSet claims = doc(token, AUD_ACCESS, LOAI_ACCESS, false);
        return new AccessTokenClaims(docKhachHangId(claims), docSessionId(claims), claims.getExpirationTime().toInstant());
    }

    /** Xác thực refresh token đầy đủ, kể cả hạn tuyệt đối của phiên. */
    public RefreshTokenClaims docRefreshToken(String token) {
        JWTClaimsSet claims = doc(token, AUD_REFRESH, LOAI_REFRESH, true);
        return new RefreshTokenClaims(docKhachHangId(claims), docSessionId(claims), claims.getExpirationTime().toInstant());
    }

    /**
     * Đọc refresh token hợp lệ về chữ ký/loại nhưng bỏ qua hạn dùng — dùng cho logout và thu hồi phiên
     * cũ khi login. Token giả mạo trả {@link Optional#empty()} để không thể thu hồi phiên của người khác.
     */
    public Optional<RefreshTokenClaims> docRefreshTokenKhongXetHan(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            JWTClaimsSet claims = doc(token, AUD_REFRESH, LOAI_REFRESH, false);
            return Optional.of(new RefreshTokenClaims(
                    docKhachHangId(claims), docSessionId(claims), claims.getExpirationTime().toInstant()));
        } catch (AuthException khongHopLe) {
            return Optional.empty();
        }
    }

    /** SHA-256 hex (chữ thường) của token — giá trị duy nhất được lưu trong tblPhienDangNhap. */
    public String bam(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException loi) {
            throw new IllegalStateException("Thiếu SHA-256 trong JVM.", loi);
        }
    }

    /** So sánh hash theo thời gian hằng để không lộ thông tin qua thời gian phản hồi. */
    public static boolean soKhopHash(String mot, String hai) {
        if (mot == null || hai == null) {
            return false;
        }
        return MessageDigest.isEqual(mot.getBytes(StandardCharsets.UTF_8), hai.getBytes(StandardCharsets.UTF_8));
    }

    private String tao(String audience, String loai, String sessionId, int khachHangId,
                       Instant phatHanh, Instant hetHan) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(audience)
                .subject(String.valueOf(khachHangId))
                .claim(CLAIM_SID, sessionId)
                .claim(CLAIM_LOAI, loai)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(phatHanh))
                .expirationTime(Date.from(hetHan))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(THUAT_TOAN), claims);
        try {
            jwt.sign(ky);
        } catch (JOSEException loi) {
            throw new IllegalStateException("Không thể ký token phiên.", loi);
        }
        return jwt.serialize();
    }

    private JWTClaimsSet doc(String token, String audience, String loai, boolean xetHan) {
        if (token == null || token.isBlank()) {
            throw AuthException.sessionKhongHopLe();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!THUAT_TOAN.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(xacThuc)) {
                throw AuthException.sessionKhongHopLe();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            boolean dung =
                    ISSUER.equals(claims.getIssuer())
                    && claims.getAudience() != null
                    && claims.getAudience().contains(audience)
                    && loai.equals(claims.getStringClaim(CLAIM_LOAI))
                    && claims.getSubject() != null && !claims.getSubject().isBlank()
                    && claims.getExpirationTime() != null
                    && claims.getStringClaim(CLAIM_SID) != null && !claims.getStringClaim(CLAIM_SID).isBlank();
            if (!dung) {
                throw AuthException.sessionKhongHopLe();
            }
            if (xetHan && !claims.getExpirationTime().toInstant().isAfter(bayGio())) {
                throw AuthException.sessionKhongHopLe();
            }
            return claims;
        } catch (ParseException | JOSEException loi) {
            throw AuthException.sessionKhongHopLe();
        }
    }

    private static int docKhachHangId(JWTClaimsSet claims) {
        try {
            return Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException loi) {
            throw AuthException.sessionKhongHopLe();
        }
    }

    private static String docSessionId(JWTClaimsSet claims) {
        try {
            return claims.getStringClaim(CLAIM_SID);
        } catch (ParseException loi) {
            throw AuthException.sessionKhongHopLe();
        }
    }

    private Instant bayGio() {
        return Instant.ofEpochMilli(clock.millis());
    }
}
