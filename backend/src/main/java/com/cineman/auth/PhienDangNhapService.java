package com.cineman.auth;

import com.cineman.dao.KhachHangDAO;
import com.cineman.dao.PhienDangNhapDAO;
import com.cineman.entity.KhachHang;
import com.cineman.entity.PhienDangNhap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vòng đời phiên JWT (ARCHITECTURE §7.3): login tạo phiên, refresh xoay token nguyên tử, logout thu hồi,
 * mỗi request được bảo vệ kiểm tra lại phiên trong DB.
 *
 * <p>Quy ước thời gian: mọi mốc lấy từ {@link Clock} và cắt về mili giây (khớp {@code DATETIME(3)}).
 * Access token sống 15 phút nhưng không vượt hạn tuyệt đối 7 ngày của phiên; refresh không kéo dài hạn
 * phiên. Refresh token vừa bị xoay mà xuất hiện lại trong 5 giây → {@code REFRESH_CONFLICT} (không đổi
 * gì); ngoài cửa sổ đó (hoặc là thế hệ cũ hơn nữa) → thu hồi phiên rồi báo {@code SESSION_INVALID}.
 * Hủy phiên phải commit dù request trả lỗi, nên service trả {@link KetQuaPhien} thay vì ném ngoại lệ
 * bên trong transaction.
 */
@Service
public class PhienDangNhapService {

    public static final Duration HAN_ACCESS = Duration.ofMinutes(15);
    public static final Duration HAN_PHIEN = Duration.ofDays(7);
    public static final Duration CUA_SO_REFRESH_LAP = Duration.ofSeconds(5);

    /** Kết quả nghiệp vụ để controller ánh xạ HTTP (không ném lỗi → mọi thay đổi đã commit). */
    public enum MaPhien {
        THANH_CONG,
        PHIEN_KHONG_HOP_LE,
        XUNG_DOT_REFRESH
    }

    /**
     * Đăng xuất: {@code THANH_CONG} (đã thu hồi hoặc không còn gì để thu hồi) vs
     * {@code PHIEN_DA_THAY_DOI} (cookie hợp lệ nhưng không khớp phiên client mong đợi — trình duyệt đã
     * đăng nhập lại/đổi tài khoản ở tab khác, nên không được thu hồi phiên mới).
     */
    public enum MaDangXuat {
        THANH_CONG,
        PHIEN_DA_THAY_DOI
    }

    /**
     * {@code thongTin}/{@code refreshToken} chỉ khác {@code null} khi {@link MaPhien#THANH_CONG};
     * {@code refreshToken} là token thô duy nhất còn tồn tại ngoài DB (đi vào cookie HttpOnly).
     */
    public record KetQuaPhien(MaPhien ma, ThongTinDangNhap thongTin, String refreshToken) {

        static KetQuaPhien loi(MaPhien ma) {
            return new KetQuaPhien(ma, null, null);
        }
    }

    private final JwtTokenService jwt;
    private final PhienDangNhapDAO phienDAO;
    private final KhachHangDAO khachHangDAO;
    private final Clock clock;

    public PhienDangNhapService(JwtTokenService jwt, PhienDangNhapDAO phienDAO,
                                KhachHangDAO khachHangDAO, Clock clock) {
        this.jwt = jwt;
        this.phienDAO = phienDAO;
        this.khachHangDAO = khachHangDAO;
        this.clock = clock;
    }

    /**
     * Tạo phiên mới sau khi mật khẩu đã được xác thực. Nếu trình duyệt gửi kèm refresh cookie của phiên
     * cũ (cùng máy, có thể đổi tài khoản), phiên đó bị thu hồi — không ảnh hưởng thiết bị/phiên khác.
     * Cookie giả mạo bị bỏ qua vì {@link JwtTokenService#docRefreshTokenKhongXetHan} kiểm tra chữ ký.
     */
    @Transactional
    public KetQuaPhien dangNhap(KhachHang khachHang, String refreshTokenCu) {
        Instant bayGio = bayGio();
        if (refreshTokenCu != null) {
            jwt.docRefreshTokenKhongXetHan(refreshTokenCu)
                    .ifPresent(cu -> phienDAO.thuHoi(cu.sessionId(), bayGio));
        }
        String sessionId = UUID.randomUUID().toString();
        Instant hetHan = bayGio.plus(HAN_PHIEN);
        String refreshToken = jwt.taoRefreshToken(sessionId, khachHang.getId(), bayGio, hetHan);
        phienDAO.taoPhien(sessionId, khachHang.getId(), jwt.bam(refreshToken), bayGio, hetHan);
        Instant accessHetHan = accessHetHan(bayGio, hetHan);
        String accessToken = jwt.taoAccessToken(sessionId, khachHang.getId(), bayGio, accessHetHan);
        return thanhCong(accessToken, accessHetHan, hetHan, sessionId, khachHang, refreshToken);
    }

    /**
     * Xoay refresh token trong một transaction có khóa hàng phiên. Trả {@code THANH_CONG} kèm access mới
     * và refresh mới; {@code XUNG_DOT_REFRESH} khi token trước vừa dùng lặp trong 5 giây (không ghi gì);
     * {@code PHIEN_KHONG_HOP_LE} khi phiên hết hạn/đã thu hồi, token sai chủ, hoặc replay thế hệ cũ —
     * trường hợp replay thật đã thu hồi phiên trước khi trả lỗi. Refresh token hết hạn tuyệt đối cũng
     * thuộc nhóm sau, không bao giờ gia hạn phiên.
     */
    @Transactional
    public KetQuaPhien lamMoi(String refreshToken) {
        JwtTokenService.RefreshTokenClaims claims;
        try {
            claims = jwt.docRefreshToken(refreshToken);
        } catch (AuthException khongHopLe) {
            return KetQuaPhien.loi(MaPhien.PHIEN_KHONG_HOP_LE);
        }

        Instant bayGio = bayGio();
        PhienDangNhap phien = phienDAO.khoaTheoId(claims.sessionId());
        if (phien == null
                || phien.getThuHoiLuc() != null
                || !phien.getHetHan().isAfter(bayGio)
                || phien.getKhachHangId() != claims.khachHangId()) {
            return KetQuaPhien.loi(MaPhien.PHIEN_KHONG_HOP_LE);
        }

        String hashGuiLen = jwt.bam(refreshToken);
        if (JwtTokenService.soKhopHash(hashGuiLen, phien.getRefreshTokenHash())) {
            return xoay(phien, bayGio);
        }
        if (phien.getPreviousTokenHash() != null
                && JwtTokenService.soKhopHash(hashGuiLen, phien.getPreviousTokenHash())) {
            // Token vừa bị thay thế. Trong cửa sổ 5 giây: client/browser gửi trùng (mất phản hồi, hai tab)
            // → 409, không thu hồi. Ngoài cửa sổ: replay thật → thu hồi phiên.
            if (phien.getXoayLuc() != null && !bayGio.minus(CUA_SO_REFRESH_LAP).isAfter(phien.getXoayLuc())) {
                return KetQuaPhien.loi(MaPhien.XUNG_DOT_REFRESH);
            }
            phienDAO.thuHoi(phien.getId(), bayGio);
            return KetQuaPhien.loi(MaPhien.PHIEN_KHONG_HOP_LE);
        }
        // Token ký hợp lệ nhưng không phải thế hệ hiện hành lẫn thế hệ trước → replay của thế hệ cũ hơn.
        phienDAO.thuHoi(phien.getId(), bayGio);
        return KetQuaPhien.loi(MaPhien.PHIEN_KHONG_HOP_LE);
    }

    /**
     * Thu hồi phiên của refresh cookie, có ràng buộc phiên mong đợi từ client ({@code X-Auth-Session}) để
     * tab cũ không thu hồi nhầm phiên mới sau khi tab khác đăng nhập lại/đổi tài khoản.
     *
     * <ul>
     *   <li>Không có cookie / cookie giả mạo (không xác thực được chữ ký) → thành công (no-op, idempotent;
     *       token giả không thu hồi được phiên nào).</li>
     *   <li>Cookie ký hợp lệ nhưng {@code sessionMongDoi} thiếu/lệch {@code sid} ĐÃ KÝ — kể cả khi phiên
     *       trong cookie đã thu hồi hoặc hết hạn — → {@link MaDangXuat#PHIEN_DA_THAY_DOI} (409, không thu
     *       hồi, không xóa cookie). So sánh làm ngay sau khi xác thực chữ ký, trước mọi nhánh trạng thái
     *       DB, để cookie cũ không bị coi là "đã đăng xuất xong" cho một phiên khác.</li>
     *   <li>Cookie ký hợp lệ, {@code sessionMongDoi} trùng {@code sid}, phiên còn sống → thu hồi.</li>
     *   <li>Cookie ký hợp lệ, {@code sessionMongDoi} trùng {@code sid}, phiên đã thu hồi/hết hạn → thành
     *       công (200, idempotent khi client thử lại sau khi mất phản hồi).</li>
     * </ul>
     */
    @Transactional
    public MaDangXuat dangXuat(String refreshToken, String sessionMongDoi) {
        if (refreshToken == null) {
            return MaDangXuat.THANH_CONG;
        }
        Optional<JwtTokenService.RefreshTokenClaims> claims = jwt.docRefreshTokenKhongXetHan(refreshToken);
        if (claims.isEmpty()) {
            return MaDangXuat.THANH_CONG;
        }
        JwtTokenService.RefreshTokenClaims duLieu = claims.get();
        if (sessionMongDoi == null || sessionMongDoi.isBlank() || !sessionMongDoi.equals(duLieu.sessionId())) {
            return MaDangXuat.PHIEN_DA_THAY_DOI;
        }
        Instant bayGio = bayGio();
        PhienDangNhap phien = phienDAO.khoaTheoId(duLieu.sessionId());
        if (phien == null || phien.getThuHoiLuc() != null || !phien.getHetHan().isAfter(bayGio)) {
            return MaDangXuat.THANH_CONG;
        }
        phienDAO.thuHoi(phien.getId(), bayGio);
        return MaDangXuat.THANH_CONG;
    }

    /**
     * Xác thực access token cho từng request được bảo vệ: chữ ký/loại/issuer/audience/sid, rồi phiên DB
     * phải tồn tại, chưa thu hồi, chưa quá hạn tuyệt đối và đúng chủ (subject khớp cột khách hàng).
     * Phiên bị thu hồi/hết hạn → {@code SESSION_INVALID} kể cả khi access JWT còn hạn; chỉ khi phiên còn
     * sống mới xét hạn access để trả {@code ACCESS_TOKEN_EXPIRED}.
     */
    public PhienXacThuc xacThucAccessToken(String accessToken) {
        JwtTokenService.AccessTokenClaims claims = jwt.docAccessToken(accessToken);
        Instant bayGio = bayGio();
        PhienDangNhap phien = phienDAO.layTheoId(claims.sessionId());
        if (phien == null
                || phien.getThuHoiLuc() != null
                || !phien.getHetHan().isAfter(bayGio)
                || phien.getKhachHangId() != claims.khachHangId()) {
            throw AuthException.sessionKhongHopLe();
        }
        if (!claims.hetHan().isAfter(bayGio)) {
            throw AuthException.accessTokenHetHan();
        }
        return new PhienXacThuc(phien.getKhachHangId(), phien.getId(), phien.getHetHan());
    }

    private KetQuaPhien xoay(PhienDangNhap phien, Instant bayGio) {
        KhachHang khachHang = khachHangDAO.layTheoId(phien.getKhachHangId());
        if (khachHang == null) {
            phienDAO.thuHoi(phien.getId(), bayGio);
            return KetQuaPhien.loi(MaPhien.PHIEN_KHONG_HOP_LE);
        }
        String refreshTokenMoi = jwt.taoRefreshToken(
                phien.getId(), phien.getKhachHangId(), bayGio, phien.getHetHan());
        boolean daXoay = phienDAO.xoayToken(
                phien.getId(), phien.getRefreshTokenHash(), jwt.bam(refreshTokenMoi), bayGio);
        if (!daXoay) {
            // Hàng đã bị request khác xoay giữa lúc khóa và lúc ghi: đọc lại trạng thái mới nhất để
            // trả đúng REFRESH_CONFLICT (trùng trong 5 giây) thay vì 401, không thu hồi phiên.
            PhienDangNhap moiNhat = phienDAO.layTheoId(phien.getId());
            boolean trungTrongCuaSo = moiNhat != null
                    && moiNhat.getThuHoiLuc() == null
                    && moiNhat.getXoayLuc() != null
                    && JwtTokenService.soKhopHash(phien.getRefreshTokenHash(), moiNhat.getPreviousTokenHash())
                    && !bayGio.minus(CUA_SO_REFRESH_LAP).isAfter(moiNhat.getXoayLuc());
            return KetQuaPhien.loi(trungTrongCuaSo ? MaPhien.XUNG_DOT_REFRESH : MaPhien.PHIEN_KHONG_HOP_LE);
        }
        Instant accessHetHan = accessHetHan(bayGio, phien.getHetHan());
        String accessToken = jwt.taoAccessToken(
                phien.getId(), phien.getKhachHangId(), bayGio, accessHetHan);
        return thanhCong(accessToken, accessHetHan, phien.getHetHan(), phien.getId(), khachHang, refreshTokenMoi);
    }

    private static KetQuaPhien thanhCong(String accessToken, Instant accessHetHan, Instant sessionHetHan,
                                         String sessionId, KhachHang khachHang, String refreshToken) {
        return new KetQuaPhien(MaPhien.THANH_CONG,
                new ThongTinDangNhap(accessToken, accessHetHan, sessionHetHan, sessionId, khachHang),
                refreshToken);
    }

    /** Access token không bao giờ sống quá hạn tuyệt đối của phiên. */
    private static Instant accessHetHan(Instant bayGio, Instant sessionHetHan) {
        Instant duKien = bayGio.plus(HAN_ACCESS);
        return duKien.isAfter(sessionHetHan) ? sessionHetHan : duKien;
    }

    private Instant bayGio() {
        return Instant.ofEpochMilli(clock.millis());
    }
}
