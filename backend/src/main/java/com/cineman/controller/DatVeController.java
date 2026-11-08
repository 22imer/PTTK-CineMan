package com.cineman.controller;

import com.cineman.dao.GheDAO;
import com.cineman.dao.HoaDonDAO;
import com.cineman.dao.KhungGioDAO;
import com.cineman.dao.PhimDAO;
import com.cineman.dao.SuatChieuDAO;
import com.cineman.dao.VeDAO;
import com.cineman.entity.Ghe;
import com.cineman.entity.HoaDon;
import com.cineman.entity.KhachHang;
import com.cineman.entity.KhungGio;
import com.cineman.entity.Phim;
import com.cineman.entity.SuatChieu;
import com.cineman.entity.Ve;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Đặt vé trực tuyến (ARCHITECTURE §7, R1–R12): danh mục phim/khung giờ/suất chiếu, sơ đồ ghế, hóa đơn,
 * áp voucher, thanh toán mock và hủy.
 *
 * <p>Mọi endpoint chỉ tin phiên server ({@link DangKyController#KHOA_PHIEN_KHACH_HANG}); khách ẩn danh
 * nhận 401 và không bị tạo phiên. Id khách hàng, giá vé, điểm hay hạn giữ ghế client gửi lên đều bị bỏ
 * qua — {@link HoaDonDAO} đọc lại suất chiếu/ghế/giá và tự tính tiền, điểm, hạn.
 *
 * <p>Controller chỉ ánh xạ kết quả nghiệp vụ của DAO sang HTTP envelope; lỗi dữ liệu (SQL) được che
 * thành 500 an toàn, không lộ câu lệnh/stacktrace.
 */
@RestController
@RequestMapping("/api")
public class DatVeController {

    private static final String THONG_BAO_CHUA_DANG_NHAP = "Bạn cần đăng nhập để đặt vé.";

    private final PhimDAO phimDAO;
    private final KhungGioDAO khungGioDAO;
    private final SuatChieuDAO suatChieuDAO;
    private final GheDAO gheDAO;
    private final VeDAO veDAO;
    private final HoaDonDAO hoaDonDAO;

    public DatVeController(PhimDAO phimDAO, KhungGioDAO khungGioDAO, SuatChieuDAO suatChieuDAO,
                           GheDAO gheDAO, VeDAO veDAO, HoaDonDAO hoaDonDAO) {
        this.phimDAO = phimDAO;
        this.khungGioDAO = khungGioDAO;
        this.suatChieuDAO = suatChieuDAO;
        this.gheDAO = gheDAO;
        this.veDAO = veDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    /** Tóm tắt hóa đơn dùng chung cho tạo/áp voucher/thanh toán/hủy. */
    public record TomTatHoaDon(HoaDon hoaDon, int tongTienTruocVoucher, int tongTien,
                               int diemCong, int tongDiem, LocalDateTime serverNow) {
    }

    /** Sơ đồ ghế của một suất chiếu: toàn bộ ghế phòng + vé còn hiệu lực + suất chiếu. */
    public record SoDoGhe(List<Ghe> dsGhe, List<Ve> dsVe, SuatChieu suatChieu) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YeuCauTaoHoaDon(Integer suatChieuId, List<Integer> dsGhe) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YeuCauApDungVoucher(Integer hoaDonId, String maVoucher) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YeuCauThanhToan(Boolean thanhCong) {
    }

    // ------------------------------------------------------------------ danh mục

    @GetMapping("/phim")
    public ResponseEntity<ApiResponse<List<Phim>>> layDanhSachPhim(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        return ResponseEntity.ok(ApiResponse.ok(phimDAO.laydsPhim()));
    }

    @GetMapping("/suatchieu")
    public ResponseEntity<ApiResponse<List<SuatChieu>>> layDanhSachSuatChieu(
            @RequestParam(required = false) Integer phimId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChieu,
            @RequestParam(required = false) Integer khungGioId,
            HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (phimId == null || phimId <= 0) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai mã phim.");
        }
        int khungGio = khungGioId != null && khungGioId > 0 ? khungGioId : 0;
        return ResponseEntity.ok(ApiResponse.ok(
                suatChieuDAO.laydsSuatChieu(phimId, ngayChieu, khungGio)));
    }

    @GetMapping("/khunggio")
    public ResponseEntity<ApiResponse<List<KhungGio>>> layDanhSachKhungGio(
            @RequestParam(required = false) Integer phimId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChieu,
            HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (phimId == null || phimId <= 0) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai mã phim.");
        }
        return ResponseEntity.ok(ApiResponse.ok(khungGioDAO.laydsKhungGioTheoPhim(phimId, ngayChieu)));
    }

    /** Nhả giữ ghế quá hạn trước khi đọc sơ đồ ghế (R5), rồi trả ghế/vé/suất chiếu. */
    @GetMapping("/ghe")
    public ResponseEntity<ApiResponse<SoDoGhe>> laySoDoGhe(
            @RequestParam(required = false) Integer suatChieuId,
            HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (suatChieuId == null || suatChieuId <= 0) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai mã suất chiếu.");
        }
        SuatChieu suatChieu = suatChieuDAO.layTheoId(suatChieuId);
        if (suatChieu == null) {
            return loi(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại.");
        }
        hoaDonDAO.giaiPhongGiuHetHan(suatChieuId);
        return ResponseEntity.ok(ApiResponse.ok(new SoDoGhe(
                gheDAO.laydsGhe(suatChieuId),
                veDAO.laydsVeConHieuLuc(suatChieu),
                suatChieu)));
    }

    // ------------------------------------------------------------------ hóa đơn

    @PostMapping("/hoadon")
    public ResponseEntity<ApiResponse<TomTatHoaDon>> taoHoaDon(
            @RequestBody(required = false) YeuCauTaoHoaDon yeuCau, HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (yeuCau == null || yeuCau.suatChieuId() == null || yeuCau.suatChieuId() <= 0
                || yeuCau.dsGhe() == null) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu thông tin suất chiếu hoặc ghế.");
        }

        HoaDon hoaDon = new HoaDon();
        KhachHang khachHang = new KhachHang();
        khachHang.setId(khachHangId);
        hoaDon.setKhachHang(khachHang);

        List<Ve> dsVe = new ArrayList<>(yeuCau.dsGhe().size());
        for (Integer idGhe : yeuCau.dsGhe()) {
            if (idGhe == null || idGhe <= 0) {
                return loi(HttpStatus.BAD_REQUEST, "Danh sách ghế không hợp lệ.");
            }
            SuatChieu suatChieu = new SuatChieu();
            suatChieu.setId(yeuCau.suatChieuId());
            Ghe ghe = new Ghe();
            ghe.setId(idGhe);
            Ve ve = new Ve();
            ve.setSuatChieu(suatChieu);
            ve.setGhe(ghe);
            dsVe.add(ve);
        }

        HoaDon daTao = hoaDonDAO.taoHoaDon(hoaDon, dsVe);
        if (daTao == null) {
            return loi(HttpStatus.BAD_REQUEST,
                    "Lựa chọn ghế không hợp lệ hoặc ghế đã được giữ/bán.");
        }
        return ResponseEntity.ok(ApiResponse.ok(tomTat(daTao, khachHangId), "Đã tạo hóa đơn giữ ghế."));
    }

    @PostMapping("/hoadon/apdung-voucher")
    public ResponseEntity<ApiResponse<TomTatHoaDon>> apDungVoucher(
            @RequestBody(required = false) YeuCauApDungVoucher yeuCau, HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        if (yeuCau == null || yeuCau.hoaDonId() == null || yeuCau.hoaDonId() <= 0
                || yeuCau.maVoucher() == null || yeuCau.maVoucher().isBlank()) {
            return loi(HttpStatus.BAD_REQUEST, "Thiếu mã hóa đơn hoặc mã voucher.");
        }
        HoaDonDAO.KetQuaThaoTac ketQua = hoaDonDAO.apDungVoucherChoHoaDon(
                yeuCau.hoaDonId(), khachHangId, yeuCau.maVoucher().trim());
        return theoKetQua(ketQua, khachHangId, "Đã áp dụng voucher.");
    }

    @PostMapping("/hoadon/{id}/thanh-toan")
    public ResponseEntity<ApiResponse<TomTatHoaDon>> thanhToan(
            @PathVariable int id,
            @RequestBody(required = false) YeuCauThanhToan yeuCau,
            HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        boolean thanhCong = yeuCau == null || yeuCau.thanhCong() == null || yeuCau.thanhCong();
        HoaDonDAO.KetQuaThaoTac ketQua = hoaDonDAO.xacNhanThanhToan(id, khachHangId, thanhCong);
        if (ketQua.ma() == HoaDonDAO.MaThaoTac.THAT_BAI_MOCK) {
            return loi(HttpStatus.BAD_REQUEST,
                    "Thanh toán không thành công; hóa đơn vẫn giữ ghế để thử lại trong thời hạn.");
        }
        return theoKetQua(ketQua, khachHangId, "Thanh toán thành công.");
    }

    @DeleteMapping("/hoadon/{id}")
    public ResponseEntity<ApiResponse<TomTatHoaDon>> huyHoaDon(
            @PathVariable int id, HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP);
        }
        HoaDonDAO.KetQuaThaoTac ketQua = hoaDonDAO.huyHoaDon(id, khachHangId);
        return theoKetQua(ketQua, khachHangId, "Đã hủy hóa đơn và nhả ghế.");
    }

    // ------------------------------------------------------------------ nội bộ

    private TomTatHoaDon tomTat(HoaDon hoaDon, int khachHangId) {
        int tongTienTruocVoucher = hoaDonDAO.tinhTongTien(hoaDon.getDsVe());
        int tongTien = hoaDonDAO.tinhTongTien(hoaDon.getDsVe(), hoaDon.getVoucher());
        return new TomTatHoaDon(
                hoaDon,
                tongTienTruocVoucher,
                tongTien,
                hoaDonDAO.tinhDiemCong(tongTienTruocVoucher),
                hoaDonDAO.layTongDiem(khachHangId),
                LocalDateTime.now().withNano(0));
    }

    private ResponseEntity<ApiResponse<TomTatHoaDon>> theoKetQua(
            HoaDonDAO.KetQuaThaoTac ketQua, int khachHangId, String thongBaoThanhCong) {
        return switch (ketQua.ma()) {
            case THANH_CONG, DA_THANH_TOAN ->
                    ResponseEntity.ok(ApiResponse.ok(tomTat(ketQua.hoaDon(), khachHangId), thongBaoThanhCong));
            case KHONG_TIM_THAY -> loi(HttpStatus.NOT_FOUND, "Hóa đơn không tồn tại.");
            case KHONG_PHAI_CHU -> loi(HttpStatus.FORBIDDEN, "Hóa đơn không thuộc khách hàng đang đăng nhập.");
            case HET_HAN -> loi(HttpStatus.CONFLICT,
                    "Hóa đơn đã hết hạn giữ ghế; ghế đã được nhả.");
            case VOUCHER_KHONG_HOP_LE -> loi(HttpStatus.BAD_REQUEST,
                    "Voucher không hợp lệ, đã dùng, quá hạn hoặc không áp được cho hóa đơn này.");
            case THAT_BAI_MOCK -> loi(HttpStatus.BAD_REQUEST, "Thanh toán không thành công.");
            case KHONG_THE_XU_LY, DA_HUY -> loi(HttpStatus.CONFLICT,
                    "Hóa đơn không ở trạng thái có thể xử lý.");
        };
    }

    /** JSON hỏng/sai kiểu → envelope 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> duLieuKhongDocDuoc(HttpMessageNotReadableException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
    }

    /** Query/path sai định dạng (vd. ngày không phải YYYY-MM-DD) → envelope 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> thamSoSaiDinhDang(MethodArgumentTypeMismatchException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Tham số gửi lên không hợp lệ.");
    }

    /** Lỗi DB → envelope 500 an toàn; transaction đã rollback ở proxy. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> loiDuLieu(DataAccessException ngoaiLe) {
        return loi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu do lỗi dữ liệu.");
    }

    /** Lỗi transaction (commit/rollback) → envelope 500 an toàn, không lộ chi tiết kỹ thuật. */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> loiGiaoDich(TransactionSystemException ngoaiLe) {
        return loi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu do lỗi dữ liệu.");
    }

    /** Chỉ đọc phiên có sẵn; khách ẩn danh không bị tạo phiên mới. */
    private static Integer khachHangDangNhap(HttpServletRequest request) {
        HttpSession phien = request.getSession(false);
        if (phien == null) {
            return null;
        }
        Object giaTri = phien.getAttribute(DangKyController.KHOA_PHIEN_KHACH_HANG);
        return giaTri instanceof Integer id && id > 0 ? id : null;
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
