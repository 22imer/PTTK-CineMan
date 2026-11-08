package com.cineman.controller;

import com.cineman.dao.KhachHangDAO;
import com.cineman.dao.ThanhVienDAO;
import com.cineman.entity.KhachHang;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng ký thành viên + đăng nhập (ARCHITECTURE §7).
 *
 * <p>Đăng ký: kiểm tra R8 ở {@link ThanhVienDAO#kiemTraThongTin}, xác nhận mật khẩu tại boundary,
 * kiểm tra trùng email/sđt trước rồi để UNIQUE của DB chặn nốt username và trường hợp tranh chấp
 * (bắt {@link DuplicateKeyException} → 409). Việc lưu tài khoản + hồ sơ nằm trong một transaction
 * của {@link KhachHangDAO#taoHoSo}.
 *
 * <p>Đăng nhập: dựng {@link KhachHang} mới chỉ từ username/password client gửi; mọi trường hồ sơ
 * (id, vaiTro, điểm, hạng…) do {@link ThanhVienDAO#kiemTraDangNhap} nạp từ DB, không phản chiếu giá trị
 * client. Sai thông tin → 401 và không tạo phiên; đúng → hủy phiên cũ, tạo phiên mới chứa id khách hàng.
 */
@RestController
@RequestMapping("/api/auth")
public class DangKyController {

    /** Khóa HttpSession chứa id (số nguyên) khách hàng đang đăng nhập do server xác thực. */
    public static final String KHOA_PHIEN_KHACH_HANG = "khachHangId";

    public static final String THONG_BAO_DANG_KY_THANH_CONG = "Đăng ký tài khoản thành viên thành công!";

    private final ThanhVienDAO thanhVienDAO;
    private final KhachHangDAO khachHangDAO;

    public DangKyController(ThanhVienDAO thanhVienDAO, KhachHangDAO khachHangDAO) {
        this.thanhVienDAO = thanhVienDAO;
        this.khachHangDAO = khachHangDAO;
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
    public ResponseEntity<ApiResponse<KhachHang>> dangNhap(@RequestBody(required = false) YeuCauDangNhap yeuCau,
                                                           HttpServletRequest request) {
        if (yeuCau == null || !coGiaTri(yeuCau.username()) || !coGiaTri(yeuCau.password())) {
            return loi(HttpStatus.BAD_REQUEST, "Tên đăng nhập và mật khẩu không được để trống.");
        }
        KhachHang kh = new KhachHang();
        kh.setUsername(yeuCau.username());
        kh.setPassword(yeuCau.password());
        if (!thanhVienDAO.kiemTraDangNhap(kh)) {
            return loi(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng.");
        }
        moPhienMoi(request, kh.getId());
        return ResponseEntity.ok(ApiResponse.ok(kh));
    }

    /** JSON hỏng hoặc sai kiểu (kể cả ngaySinh sai định dạng) → envelope 400, không lộ stacktrace. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> duLieuKhongDocDuoc(HttpMessageNotReadableException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
    }

    /** Chống session fixation: hủy phiên cũ (nếu có) rồi tạo phiên mới chỉ chứa id khách hàng từ DB. */
    private static void moPhienMoi(HttpServletRequest request, int khachHangId) {
        HttpSession phienCu = request.getSession(false);
        if (phienCu != null) {
            phienCu.invalidate();
        }
        request.getSession(true).setAttribute(KHOA_PHIEN_KHACH_HANG, khachHangId);
    }

    private static boolean coGiaTri(String s) {
        return s != null && !s.isBlank();
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
