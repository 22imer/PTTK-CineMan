package com.cineman.controller;

import com.cineman.auth.AuthCookie;
import com.cineman.auth.AuthException;
import com.cineman.auth.AuthFilter;
import com.cineman.auth.PhienDangNhapService;
import com.cineman.auth.PhienXacThuc;
import com.cineman.auth.ThongTinDangNhap;
import com.cineman.dao.KhachHangDAO;
import com.cineman.dao.ThanhVienDAO;
import com.cineman.entity.KhachHang;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng ký thành viên + vòng đời phiên JWT (ARCHITECTURE §7.3).
 *
 * <p>Đăng ký: kiểm tra R8 ở {@link ThanhVienDAO#kiemTraThongTin}, xác nhận mật khẩu tại boundary,
 * kiểm tra trùng email/sđt trước rồi để UNIQUE của DB chặn nốt username và trường hợp tranh chấp
 * (bắt {@link DuplicateKeyException} → 409). Việc lưu tài khoản + hồ sơ nằm trong một transaction
 * của {@link KhachHangDAO#taoHoSo}.
 *
 * <p>Đăng nhập: dựng {@link KhachHang} mới chỉ từ username/password client gửi; mọi trường hồ sơ
 * (id, vaiTro, điểm, hạng…) do {@link ThanhVienDAO#kiemTraDangNhap} nạp từ DB, không phản chiếu giá trị
 * client. Sai thông tin → 401 và không tạo/thu hồi phiên. Đúng → {@link PhienDangNhapService} tạo phiên
 * mới (thu hồi phiên của refresh cookie cũ nếu có), trả access token ngắn hạn + cookie refresh
 * HttpOnly; không tạo {@code JSESSIONID}.
 *
 * <p>Refresh/me/logout nằm cùng controller này: refresh xoay token bằng cookie (409 khi trùng trong
 * 5 giây, không đổi cookie), me đọc hồ sơ mới từ DB theo danh tính Bearer đã xác thực, logout thu hồi
 * phiên idempotent rồi xóa cookie.
 */
@RestController
@RequestMapping("/api/auth")
public class DangKyController {

    public static final String THONG_BAO_DANG_KY_THANH_CONG = "Đăng ký tài khoản thành viên thành công!";
    public static final String THONG_BAO_DANG_XUAT_THANH_CONG = "Đã đăng xuất.";

    private final ThanhVienDAO thanhVienDAO;
    private final KhachHangDAO khachHangDAO;
    private final PhienDangNhapService phienDangNhapService;
    private final AuthCookie authCookie;

    public DangKyController(ThanhVienDAO thanhVienDAO, KhachHangDAO khachHangDAO,
                            PhienDangNhapService phienDangNhapService, AuthCookie authCookie) {
        this.thanhVienDAO = thanhVienDAO;
        this.khachHangDAO = khachHangDAO;
        this.phienDangNhapService = phienDangNhapService;
        this.authCookie = authCookie;
    }

    /** Body của {@code POST /api/auth/register}: {@code {"kh": {...}, "xacNhanMatKhau": "..."}}. */
    public record YeuCauDangKy(KhachHang kh, String xacNhanMatKhau) {
    }

    /** Body của {@code POST /api/auth/login}; trường lạ của client bị bỏ qua, không bao giờ được tin. */
    public record YeuCauDangNhap(String username, String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<KhachHang>> dangKy(@RequestBody(required = false) YeuCauDangKy yeuCau) {
        if (yeuCau == null || yeuCau.kh() == null) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu thông tin đăng ký.");
        }
        KhachHang kh = yeuCau.kh();
        if (!thanhVienDAO.kiemTraThongTin(kh)) {
            return loi(HttpStatus.BAD_REQUEST,
                    "Thông tin đăng ký không hợp lệ: thiếu/sai trường bắt buộc hoặc mật khẩu chưa đạt yêu cầu.");
        }
        if (!Objects.equals(kh.getPassword(), yeuCau.xacNhanMatKhau())) {
            return loi(HttpStatus.BAD_REQUEST, "Mật khẩu nhập lại không khớp.");
        }
        if (!thanhVienDAO.kiemTraEmailSDT(kh.getEmail(), kh.getSdt())) {
            return loi(HttpStatus.CONFLICT, "Email hoặc số điện thoại đã được sử dụng.");
        }
        try {
            if (!khachHangDAO.taoHoSo(kh)) {
                return loi(HttpStatus.BAD_REQUEST, "Không thể tạo hồ sơ khách hàng.");
            }
        } catch (DuplicateKeyException trung) {
            return loi(HttpStatus.CONFLICT, "Tên đăng nhập, email hoặc số điện thoại đã được sử dụng.");
        }
        return ResponseEntity.ok(ApiResponse.ok(kh, THONG_BAO_DANG_KY_THANH_CONG));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ThongTinDangNhap>> dangNhap(
            @RequestBody(required = false) YeuCauDangNhap yeuCau, HttpServletRequest request) {
        if (yeuCau == null || !coGiaTri(yeuCau.username()) || !coGiaTri(yeuCau.password())) {
            return loi(HttpStatus.BAD_REQUEST, "Tên đăng nhập và mật khẩu không được để trống.");
        }
        KhachHang kh = new KhachHang();
        kh.setUsername(yeuCau.username());
        kh.setPassword(yeuCau.password());
        if (!thanhVienDAO.kiemTraDangNhap(kh)) {
            return loi(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng.");
        }
        return traKetQua(phienDangNhapService.dangNhap(kh, AuthCookie.doc(request)));
    }

    /** Làm mới phiên bằng refresh cookie; không nhận body và không cần access token còn hạn. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<ThongTinDangNhap>> lamMoi(HttpServletRequest request) {
        String refreshToken = AuthCookie.doc(request);
        if (refreshToken == null) {
            return loi(HttpStatus.UNAUTHORIZED,
                    AuthException.THONG_BAO_SESSION_INVALID, AuthException.MA_SESSION_INVALID);
        }
        return traKetQua(phienDangNhapService.lamMoi(refreshToken));
    }

    /** Hồ sơ của khách đang đăng nhập, đọc mới từ DB theo danh tính Bearer đã được filter xác thực. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<KhachHang>> toi(HttpServletRequest request) {
        PhienXacThuc phien = PhienXacThuc.tu(request);
        KhachHang khachHang = phien == null ? null : khachHangDAO.layTheoId(phien.khachHangId());
        if (khachHang == null) {
            return loi(HttpStatus.UNAUTHORIZED,
                    AuthException.THONG_BAO_SESSION_INVALID, AuthException.MA_SESSION_INVALID);
        }
        return ResponseEntity.ok(ApiResponse.ok(khachHang));
    }

    /**
     * Thu hồi phiên của refresh cookie rồi xóa cookie, nhưng chỉ khi cookie vẫn trỏ đúng phiên mà client
     * đang giữ ({@code X-Auth-Session}). Lệch phiên (tab khác đã đăng nhập lại/đổi tài khoản) → 409
     * {@code SESSION_CHANGED}, không thu hồi và không xóa cookie của phiên mới. Idempotent: gọi lại với
     * đúng cặp cookie+session đã thu hồi vẫn thành công; cookie thiếu/giả mạo chỉ là no-op an toàn.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> dangXuat(
            HttpServletRequest request,
            @RequestHeader(name = AuthFilter.HEADER_PHIEN, required = false) String sessionMongDoi) {
        PhienDangNhapService.MaDangXuat ma =
                phienDangNhapService.dangXuat(AuthCookie.doc(request), sessionMongDoi);
        if (ma == PhienDangNhapService.MaDangXuat.PHIEN_DA_THAY_DOI) {
            return loi(HttpStatus.CONFLICT,
                    AuthException.THONG_BAO_SESSION_CHANGED, AuthException.MA_SESSION_CHANGED);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.xoa().toString())
                .body(ApiResponse.<Void>ok(null, THONG_BAO_DANG_XUAT_THANH_CONG));
    }

    /** JSON hỏng hoặc sai kiểu (kể cả ngaySinh sai định dạng) → envelope 400, không lộ stacktrace. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> duLieuKhongDocDuoc(HttpMessageNotReadableException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
    }

    private ResponseEntity<ApiResponse<ThongTinDangNhap>> traKetQua(PhienDangNhapService.KetQuaPhien ketQua) {
        return switch (ketQua.ma()) {
            case THANH_CONG -> ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookie
                            .tao(ketQua.refreshToken(), ketQua.thongTin().sessionExpiresAt())
                            .toString())
                    .body(ApiResponse.<ThongTinDangNhap>ok(ketQua.thongTin()));
            case XUNG_DOT_REFRESH -> DangKyController.<ThongTinDangNhap>loi(HttpStatus.CONFLICT,
                    AuthException.THONG_BAO_REFRESH_CONFLICT, AuthException.MA_REFRESH_CONFLICT);
            case PHIEN_KHONG_HOP_LE -> DangKyController.<ThongTinDangNhap>loi(HttpStatus.UNAUTHORIZED,
                    AuthException.THONG_BAO_SESSION_INVALID, AuthException.MA_SESSION_INVALID);
        };
    }

    private static boolean coGiaTri(String s) {
        return s != null && !s.isBlank();
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatusCode status, String message, String code) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message, code));
    }
}
