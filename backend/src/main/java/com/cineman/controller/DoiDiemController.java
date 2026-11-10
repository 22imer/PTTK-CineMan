package com.cineman.controller;

import com.cineman.auth.PhienXacThuc;
import com.cineman.dao.GiaoDichDoiDiemDAO;
import com.cineman.dao.KhachHangDAO;
import com.cineman.dao.QuaTangDAO;
import com.cineman.dao.VoucherKhachHangDAO;
import com.cineman.entity.GiaoDichDoiDiem;
import com.cineman.entity.KhachHang;
import com.cineman.entity.QuaTang;
import com.cineman.entity.VoucherKhachHang;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Đổi điểm thưởng (ARCHITECTURE §7): xem danh mục quà + đổi điểm thành voucher.
 *
 * <p>Danh mục luôn đọc lại số điểm hiện tại từ DB, không lấy từ client. Đổi điểm chỉ tin danh tính đã
 * xác thực bởi {@link com.cineman.auth.AuthFilter} (Bearer JWT + phiên DB, đọc qua {@link PhienXacThuc});
 * mọi id khách hàng client gửi kèm đều bị bỏ qua.
 *
 * <p>{@link #thucHienDoiDiem} là một biên {@code @Transactional} công khai duy nhất: khóa khách rồi
 * khóa quà, kiểm tra lại điểm/tồn kho, trừ điểm, giảm kho, ghi phiếu ({@link GiaoDichDoiDiemDAO})
 * trước rồi mới tạo voucher ({@link VoucherKhachHangDAO#luuVoucher}). Ngoại lệ thoát khỏi phương thức
 * (không bắt bên trong) để proxy transaction thực sự rollback cả điểm, kho, phiếu và voucher.
 * Xử lý lỗi chỉ nằm ở các {@code @ExceptionHandler} chạy sau proxy.
 */
@RestController
@RequestMapping("/api")
public class DoiDiemController {

    public static final String TRANG_THAI_CHUA_SU_DUNG = "CHUA_SU_DUNG";

    private static final String THONG_BAO_CHUA_DANG_NHAP = "Bạn cần đăng nhập để đổi điểm.";

    private final KhachHangDAO khachHangDAO;
    private final QuaTangDAO quaTangDAO;
    private final GiaoDichDoiDiemDAO giaoDichDoiDiemDAO;
    private final VoucherKhachHangDAO voucherKhachHangDAO;

    public DoiDiemController(KhachHangDAO khachHangDAO, QuaTangDAO quaTangDAO,
                             GiaoDichDoiDiemDAO giaoDichDoiDiemDAO,
                             VoucherKhachHangDAO voucherKhachHangDAO) {
        this.khachHangDAO = khachHangDAO;
        this.quaTangDAO = quaTangDAO;
        this.giaoDichDoiDiemDAO = giaoDichDoiDiemDAO;
        this.voucherKhachHangDAO = voucherKhachHangDAO;
    }

    /** Body của {@code POST /api/doi-diem}; trường lạ (vd. {@code khachHangId}) bị bỏ qua. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YeuCauDoiDiem(Integer quaTangId) {
    }

    /** Danh mục, số dư và lịch sử của khách trong phiên, đọc cùng snapshot DB. */
    public record DanhSachQuaTang(List<QuaTang> dsQuaTang, int tongDiem,
                                  List<VoucherKhachHang> lichSuDoiQua) {
    }

    @Transactional(readOnly = true)
    @GetMapping("/quatang")
    public ResponseEntity<ApiResponse<DanhSachQuaTang>> danhSachQuaTang(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        KhachHang khachHang = khachHangDAO.layTheoId(khachHangId);
        if (khachHang == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                new DanhSachQuaTang(quaTangDAO.laydsQuaTang(), khachHang.getTongDiem(),
                        giaoDichDoiDiemDAO.layLichSuTheoKhachHang(khachHangId))));
    }

    @GetMapping("/doi-diem/lich-su")
    public ResponseEntity<ApiResponse<List<VoucherKhachHang>>> lichSuDoiQua(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null || khachHangDAO.layTheoId(khachHangId) == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                giaoDichDoiDiemDAO.layLichSuTheoKhachHang(khachHangId)));
    }

    /**
     * Đổi điểm thành voucher trong đúng một transaction. Lỗi nghiệp vụ hoặc lỗi ghi bất kỳ bước nào
     * đều ném ra ngoài để rollback toàn bộ (điểm, tồn kho, phiếu, voucher).
     */
    @Transactional
    @PostMapping("/doi-diem")
    public ResponseEntity<ApiResponse<VoucherKhachHang>> thucHienDoiDiem(
            @RequestBody(required = false) YeuCauDoiDiem yeuCau, HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (yeuCau == null || yeuCau.quaTangId() == null || yeuCau.quaTangId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai mã quà tặng.");
        }

        KhachHang khachHang = khachHangDAO.khoaTheoId(khachHangId);
        if (khachHang == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        QuaTang quaTang = quaTangDAO.khoaQuaTang(yeuCau.quaTangId());
        if (quaTang == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quà tặng không tồn tại.");
        }

        int soDiem = quaTang.getSoDiem();
        if (soDiem < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quà tặng không hợp lệ.");
        }
        String maKhachHang = String.valueOf(khachHangId);
        if (khachHang.getTongDiem() < soDiem) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ điểm để đổi quà.");
        }
        if (quaTang.getSoLuong() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quà tặng đã hết.");
        }
        if (!khachHangDAO.truDiem(maKhachHang, soDiem)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ điểm để đổi quà.");
        }
        if (!quaTangDAO.capNhatSoLuong(quaTang.getId(), quaTang.getSoLuong() - 1)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quà tặng đã hết.");
        }

        int diemConLai = khachHang.getTongDiem() - soDiem;
        khachHang.setTongDiem(diemConLai);

        GiaoDichDoiDiem giaoDich = new GiaoDichDoiDiem();
        giaoDich.setThoiGian(LocalDateTime.now());
        giaoDich.setSoDiemTru(soDiem);
        giaoDich.setDiemConLai(diemConLai);
        giaoDich.setKhachHang(khachHang);
        giaoDich.setQuaTang(quaTang);
        giaoDichDoiDiemDAO.luuGiaoDich(giaoDich);

        VoucherKhachHang voucher = new VoucherKhachHang();
        voucher.setMaVoucher("VC-" + UUID.randomUUID());
        voucher.setNgayNhan(LocalDate.now());
        voucher.setTrangThai(TRANG_THAI_CHUA_SU_DUNG);
        voucher.setGiaoDichDoiDiem(giaoDich);
        voucher.setId(voucherKhachHangDAO.luuVoucher(voucher));

        return ResponseEntity.ok(ApiResponse.ok(voucher, "Đổi điểm thành công!"));
    }

    /** JSON hỏng/sai kiểu → envelope 400, không lộ stacktrace. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> duLieuKhongDocDuoc(HttpMessageNotReadableException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
    }

    /** Lỗi nghiệp vụ có chủ đích (ResponseStatusException) → envelope đúng mã, message an toàn. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> loiNghiepVu(ResponseStatusException ngoaiLe) {
        String message = ngoaiLe.getReason() != null ? ngoaiLe.getReason() : "Không thể xử lý yêu cầu.";
        return loi(ngoaiLe.getStatusCode(), message);
    }

    /** Lỗi DB (đã rollback ở proxy) → envelope 500, không lộ SQL/stacktrace. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> loiDuLieu(DataAccessException ngoaiLe) {
        return loi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu do lỗi dữ liệu.");
    }

    /** Danh tính do filter xác thực; không đọc/khởi tạo HttpSession nên khách ẩn danh không có phiên. */
    private static Integer khachHangDangNhap(HttpServletRequest request) {
        PhienXacThuc phien = PhienXacThuc.tu(request);
        return phien == null ? null : phien.khachHangId();
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
