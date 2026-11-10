package com.cineman.auth;

import com.cineman.entity.KhachHang;
import java.time.Instant;

/**
 * Dữ liệu đăng nhập/làm mới phiên trả trong envelope {@code data} (ARCHITECTURE §7.3).
 *
 * <p>{@code accessToken} chỉ nằm trong bộ nhớ frontend; {@code sessionExpiresAt} là hạn tuyệt đối
 * 7 ngày và không đổi khi refresh; {@code khachHang} luôn là hồ sơ đọc mới từ DB (không có mật khẩu).
 */
public record ThongTinDangNhap(String accessToken, Instant expiresAt, Instant sessionExpiresAt,
                               String sessionId, KhachHang khachHang) {
}
