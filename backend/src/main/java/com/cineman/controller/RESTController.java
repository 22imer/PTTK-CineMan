package com.cineman.controller;

import com.cineman.dao.GiaoDichDoiDiemDAO;
import com.cineman.dao.HoaDonDAO;
import com.cineman.dao.KhachHangDAO;
import com.cineman.dao.KhungGioDAO;
import com.cineman.dao.PhimDAO;
import com.cineman.dao.QuaTangDAO;
import com.cineman.dao.SuatChieuDAO;
import com.cineman.dao.ThanhVienDAO;
import com.cineman.dao.VeDAO;
import com.cineman.dao.VoucherKhachHangDAO;
import com.cineman.entity.Ghe;
import com.cineman.entity.GiaoDichDoiDiem;
import com.cineman.entity.HoaDon;
import com.cineman.entity.KhachHang;
import com.cineman.entity.KhungGio;
import com.cineman.entity.Phim;
import com.cineman.entity.QuaTang;
import com.cineman.entity.SuatChieu;
import com.cineman.entity.Ve;
import com.cineman.entity.VoucherKhachHang;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.server.ResponseStatusException;

/**
 * Điểm vào REST duy nhất của backend (ARCHITECTURE §5, §7): {@code HTTPClient.fetch()} của frontend
 * chỉ gọi lớp này, lớp này gọi DAO. Ba nhóm API nằm cạnh nhau, chung envelope {@link ApiResponse},
 * chung phiên server và chung bộ {@code @ExceptionHandler}:
 *
 * <ul>
 *   <li><b>Auth</b> — đăng ký thành viên + đăng nhập ({@code /api/auth/*}).
 *   <li><b>Booking</b> — đặt vé trực tuyến (R1–R12): danh mục phim/khung giờ/suất chiếu, sơ đồ ghế,
 *       hóa đơn, áp voucher, thanh toán mock và hủy.
 *   <li><b>Reward</b> — đổi điểm thưởng: danh mục quà, lịch sử, đổi điểm thành voucher.
 * </ul>
 *
 * <p>Mọi endpoint ngoài Auth chỉ tin phiên server ({@link #KHOA_PHIEN_KHACH_HANG}); khách ẩn danh nhận
 * 401 và không bị tạo phiên. Id khách hàng, giá vé, điểm hay hạn giữ ghế client gửi lên đều bị bỏ qua —
 * {@link HoaDonDAO} đọc lại suất chiếu/ghế/giá và tự tính tiền, điểm, hạn.
 *
 * <p>Đăng ký: kiểm tra R8 ở {@link ThanhVienDAO#kiemTraThongTin}, xác nhận mật khẩu tại boundary,
 * kiểm tra trùng email/sđt trước rồi để UNIQUE của DB chặn nốt username và trường hợp tranh chấp
 * (bắt {@link DuplicateKeyException} → 409). Việc lưu tài khoản + hồ sơ nằm trong một transaction
 * của {@link KhachHangDAO#taoHoSo}.
 *
 * <p>Đăng nhập: dựng {@link KhachHang} mới chỉ từ username/password client gửi; mọi trường hồ sơ
 * (id, vaiTro, điểm, hạng…) do {@link ThanhVienDAO#kiemTraDangNhap} nạp từ DB, không phản chiếu giá trị
 * client. Sai thông tin → 401 và không tạo phiên; đúng → hủy phiên cũ, tạo phiên mới chứa id khách hàng.
 *
 * <p>Danh mục quà luôn đọc lại số điểm hiện tại từ DB, không lấy từ client; {@link #danhSachQuaTang} là
 * {@code @Transactional(readOnly = true)} để danh mục, số dư và lịch sử đọc cùng một snapshot DB.
 * {@link #thucHienDoiDiem} là biên ghi {@code @Transactional} công khai duy nhất của nhóm Reward: khóa
 * khách rồi khóa quà, kiểm tra lại điểm/tồn kho, trừ điểm, giảm kho, ghi phiếu
 * ({@link GiaoDichDoiDiemDAO}) trước rồi mới tạo voucher ({@link VoucherKhachHangDAO#luuVoucher}).
 * Ngoại lệ thoát khỏi phương thức (không bắt bên trong) để proxy transaction thực sự rollback cả điểm,
 * kho, phiếu và voucher. Xử lý lỗi chỉ nằm ở các {@code @ExceptionHandler} chạy sau proxy; lỗi dữ liệu
 * (SQL) được che thành 500 an toàn, không lộ câu lệnh/stacktrace.
 */
@RestController
@RequestMapping("/api")
public class RESTController {

    /** Khóa HttpSession chứa id (số nguyên) khách hàng đang đăng nhập do server xác thực. */
    public static final String KHOA_PHIEN_KHACH_HANG = "khachHangId";

    public static final String THONG_BAO_DANG_KY_THANH_CONG = "Đăng ký tài khoản thành viên thành công!";

    public static final String TRANG_THAI_CHUA_SU_DUNG = "CHUA_SU_DUNG";

    private static final String THONG_BAO_CHUA_DANG_NHAP_DAT_VE = "Bạn cần đăng nhập để đặt vé.";

    private static final String THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM = "Bạn cần đăng nhập để đổi điểm.";

    private final ThanhVienDAO thanhVienDAO;
    private final KhachHangDAO khachHangDAO;
    private final PhimDAO phimDAO;
    private final KhungGioDAO khungGioDAO;
    private final SuatChieuDAO suatChieuDAO;
    private final VeDAO veDAO;
    private final HoaDonDAO hoaDonDAO;
    private final QuaTangDAO quaTangDAO;
    private final GiaoDichDoiDiemDAO giaoDichDoiDiemDAO;
    private final VoucherKhachHangDAO voucherKhachHangDAO;

    public RESTController(ThanhVienDAO thanhVienDAO, KhachHangDAO khachHangDAO,
                          PhimDAO phimDAO, KhungGioDAO khungGioDAO, SuatChieuDAO suatChieuDAO,
                          VeDAO veDAO, HoaDonDAO hoaDonDAO,
                          QuaTangDAO quaTangDAO, GiaoDichDoiDiemDAO giaoDichDoiDiemDAO,
                          VoucherKhachHangDAO voucherKhachHangDAO) {
        this.thanhVienDAO = thanhVienDAO;
        this.khachHangDAO = khachHangDAO;
        this.phimDAO = phimDAO;
        this.khungGioDAO = khungGioDAO;
        this.suatChieuDAO = suatChieuDAO;
        this.veDAO = veDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.quaTangDAO = quaTangDAO;
        this.giaoDichDoiDiemDAO = giaoDichDoiDiemDAO;
        this.voucherKhachHangDAO = voucherKhachHangDAO;
    }

    // ------------------------------------------------------------------ envelope + request/response

    /** Đáp ứng JSON chung {@code { success, data?, message? }} (ARCHITECTURE §7). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiResponse<T>(boolean success, T data, String message) {

        public static <T> ApiResponse<T> ok(T data, String message) {
            return new ApiResponse<>(true, data, message);
        }

        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, data, null);
        }

        public static <T> ApiResponse<T> fail(String message) {
            return new ApiResponse<>(false, null, message);
        }
    }

    /** Body của {@code POST /api/auth/register}: {@code {"kh": {...}, "xacNhanMatKhau": "..."}}. */
    public record YeuCauDangKy(KhachHang kh, String xacNhanMatKhau) {
    }

    /** Body của {@code POST /api/auth/login}; trường lạ của client bị bỏ qua, không bao giờ được tin. */
    public record YeuCauDangNhap(String username, String password) {
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

    /** Body của {@code POST /api/doi-diem}; trường lạ (vd. {@code khachHangId}) bị bỏ qua. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YeuCauDoiDiem(Integer quaTangId) {
    }

    /** Danh mục, số dư và lịch sử của khách trong phiên, đọc cùng snapshot DB. */
    public record DanhSachQuaTang(List<QuaTang> dsQuaTang, int tongDiem,
                                  List<VoucherKhachHang> lichSuDoiQua) {
    }

    // ------------------------------------------------------------------ Auth

    @PostMapping("/auth/register")
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

    @PostMapping("/auth/login")
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

    // ------------------------------------------------------------------ Booking: danh mục

    @GetMapping("/phim")
    public ResponseEntity<ApiResponse<List<Phim>>> layDanhSachPhim(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
                veDAO.laydsGhe(suatChieuId),
                veDAO.laydsVeConHieuLuc(suatChieu),
                suatChieu)));
    }

    // ------------------------------------------------------------------ Booking: hóa đơn

    @PostMapping("/hoadon")
    public ResponseEntity<ApiResponse<TomTatHoaDon>> taoHoaDon(
            @RequestBody(required = false) YeuCauTaoHoaDon yeuCau, HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DAT_VE);
        }
        HoaDonDAO.KetQuaThaoTac ketQua = hoaDonDAO.huyHoaDon(id, khachHangId);
        return theoKetQua(ketQua, khachHangId, "Đã hủy hóa đơn và nhả ghế.");
    }

    // ------------------------------------------------------------------ Reward

    @Transactional(readOnly = true)
    @GetMapping("/quatang")
    public ResponseEntity<ApiResponse<DanhSachQuaTang>> danhSachQuaTang(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM);
        }
        KhachHang khachHang = khachHangDAO.layTheoId(khachHangId);
        if (khachHang == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                new DanhSachQuaTang(quaTangDAO.laydsQuaTang(), khachHang.getTongDiem(),
                        giaoDichDoiDiemDAO.layLichSuTheoKhachHang(khachHangId))));
    }

    @GetMapping("/doi-diem/lich-su")
    public ResponseEntity<ApiResponse<List<VoucherKhachHang>>> lichSuDoiQua(HttpServletRequest request) {
        Integer khachHangId = khachHangDangNhap(request);
        if (khachHangId == null || khachHangDAO.layTheoId(khachHangId) == null) {
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM);
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
            return loi(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM);
        }
        if (yeuCau == null || yeuCau.quaTangId() == null || yeuCau.quaTangId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai mã quà tặng.");
        }

        KhachHang khachHang = khachHangDAO.khoaTheoId(khachHangId);
        if (khachHang == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, THONG_BAO_CHUA_DANG_NHAP_DOI_DIEM);
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

    // ------------------------------------------------------------------ xử lý lỗi (mọi endpoint)

    /** JSON hỏng hoặc sai kiểu (kể cả ngaySinh sai định dạng) → envelope 400, không lộ stacktrace. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> duLieuKhongDocDuoc(HttpMessageNotReadableException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
    }

    /** Query/path sai định dạng (vd. ngày không phải YYYY-MM-DD) → envelope 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> thamSoSaiDinhDang(MethodArgumentTypeMismatchException ngoaiLe) {
        return loi(HttpStatus.BAD_REQUEST, "Tham số gửi lên không hợp lệ.");
    }

    /** Lỗi nghiệp vụ có chủ đích (ResponseStatusException) → envelope đúng mã, message an toàn. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> loiNghiepVu(ResponseStatusException ngoaiLe) {
        String message = ngoaiLe.getReason() != null ? ngoaiLe.getReason() : "Không thể xử lý yêu cầu.";
        return loi(ngoaiLe.getStatusCode(), message);
    }

    /** Lỗi DB (đã rollback ở proxy) → envelope 500 an toàn, không lộ SQL/stacktrace. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> loiDuLieu(DataAccessException ngoaiLe) {
        return loi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu do lỗi dữ liệu.");
    }

    /** Lỗi transaction (commit/rollback) → envelope 500 an toàn, không lộ chi tiết kỹ thuật. */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> loiGiaoDich(TransactionSystemException ngoaiLe) {
        return loi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu do lỗi dữ liệu.");
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

    /** Chống session fixation: hủy phiên cũ (nếu có) rồi tạo phiên mới chỉ chứa id khách hàng từ DB. */
    private static void moPhienMoi(HttpServletRequest request, int khachHangId) {
        HttpSession phienCu = request.getSession(false);
        if (phienCu != null) {
            phienCu.invalidate();
        }
        request.getSession(true).setAttribute(KHOA_PHIEN_KHACH_HANG, khachHangId);
    }

    /** Chỉ đọc phiên có sẵn; khách ẩn danh không bị tạo phiên mới. */
    private static Integer khachHangDangNhap(HttpServletRequest request) {
        HttpSession phien = request.getSession(false);
        if (phien == null) {
            return null;
        }
        Object giaTri = phien.getAttribute(KHOA_PHIEN_KHACH_HANG);
        return giaTri instanceof Integer id && id > 0 ? id : null;
    }

    private static boolean coGiaTri(String s) {
        return s != null && !s.isBlank();
    }

    private static <T> ResponseEntity<ApiResponse<T>> loi(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
