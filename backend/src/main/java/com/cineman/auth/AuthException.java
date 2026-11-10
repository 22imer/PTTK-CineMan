package com.cineman.auth;

import org.springframework.http.HttpStatus;

/**
 * Lỗi xác thực phiên có mã nghiệp vụ để client phân biệt (ARCHITECTURE §7.3):
 * access hết hạn → {@code ACCESS_TOKEN_EXPIRED} (được phép refresh), phiên/token không hợp lệ →
 * {@code SESSION_INVALID} (về màn đăng nhập), refresh trùng trong cửa sổ ngắn → {@code REFRESH_CONFLICT}.
 *
 * <p>Thông điệp không chứa token, hash, mật khẩu hay chi tiết kỹ thuật.
 */
public class AuthException extends RuntimeException {

    public static final String MA_ACCESS_TOKEN_EXPIRED = "ACCESS_TOKEN_EXPIRED";
    public static final String MA_SESSION_INVALID = "SESSION_INVALID";
    public static final String MA_REFRESH_CONFLICT = "REFRESH_CONFLICT";
    public static final String MA_CSRF_REQUIRED = "CSRF_REQUIRED";
    public static final String MA_ORIGIN_NOT_ALLOWED = "ORIGIN_NOT_ALLOWED";
    public static final String MA_SESSION_CHANGED = "SESSION_CHANGED";
    public static final String MA_INVALID_PATH = "INVALID_REQUEST_PATH";

    public static final String THONG_BAO_ACCESS_TOKEN_EXPIRED =
            "Phiên đăng nhập đã hết hạn. Vui lòng làm mới phiên.";
    public static final String THONG_BAO_SESSION_INVALID =
            "Phiên đăng nhập không hợp lệ hoặc đã bị thu hồi. Vui lòng đăng nhập lại.";
    public static final String THONG_BAO_REFRESH_CONFLICT =
            "Yêu cầu làm mới phiên bị trùng. Vui lòng thử lại sau.";
    public static final String THONG_BAO_CSRF_REQUIRED =
            "Yêu cầu thiếu hoặc sai mã bảo vệ CSRF.";
    public static final String THONG_BAO_ORIGIN_NOT_ALLOWED =
            "Nguồn gửi yêu cầu không nằm trong danh sách cho phép.";
    public static final String THONG_BAO_SESSION_CHANGED =
            "Phiên trong cookie không còn là phiên hiện tại của trình duyệt; không thu hồi phiên mới.";
    public static final String THONG_BAO_INVALID_PATH =
            "Đường dẫn yêu cầu không hợp lệ.";

    private final HttpStatus status;
    private final String ma;

    private AuthException(HttpStatus status, String ma, String thongBao) {
        super(thongBao);
        this.status = status;
        this.ma = ma;
    }

    public static AuthException accessTokenHetHan() {
        return new AuthException(HttpStatus.UNAUTHORIZED, MA_ACCESS_TOKEN_EXPIRED, THONG_BAO_ACCESS_TOKEN_EXPIRED);
    }

    public static AuthException sessionKhongHopLe() {
        return new AuthException(HttpStatus.UNAUTHORIZED, MA_SESSION_INVALID, THONG_BAO_SESSION_INVALID);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMa() {
        return ma;
    }
}
