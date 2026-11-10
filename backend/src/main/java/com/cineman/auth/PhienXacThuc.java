package com.cineman.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Danh tính đã xác thực của request hiện tại, do {@link AuthFilter} gắn vào request attribute sau khi
 * kiểm tra Bearer JWT + phiên DB còn hiệu lực. Controller chỉ đọc giá trị này, không bao giờ tin id
 * khách hàng client gửi kèm.
 */
public record PhienXacThuc(int khachHangId, String sessionId, Instant hetHan) {

    /** Khóa request attribute; private theo quy ước servlet nhưng cần cho filter và controller. */
    public static final String KHOA_REQUEST = PhienXacThuc.class.getName();

    public static PhienXacThuc tu(HttpServletRequest request) {
        return request.getAttribute(KHOA_REQUEST) instanceof PhienXacThuc phien ? phien : null;
    }
}
