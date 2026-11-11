package com.cineman.dao;

import com.cineman.entity.Ghe;
import com.cineman.entity.GiaoDichDoiDiem;
import com.cineman.entity.HoaDon;
import com.cineman.entity.KhachHang;
import com.cineman.entity.QuaTang;
import com.cineman.entity.SuatChieu;
import com.cineman.entity.Ve;
import com.cineman.entity.VoucherGiamGia;
import com.cineman.entity.VoucherKhachHang;
import com.cineman.entity.VoucherVatPham;
import com.cineman.entity.VoucherVeTang;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hóa đơn — tblHoaDon (ARCHITECTURE §5, R1–R7).
 *
 * <p><b>Thứ tự khóa toàn cục:</b> suất chiếu → hóa đơn → voucher. Đặt vé khóa hàng suất chiếu trước
 * (tuần tự hóa mọi lần chọn ghế của suất đó), nhả giữ ghế hết hạn rồi mới kiểm tra trùng ghế bằng
 * locking read và tạo hóa đơn + vé trong cùng transaction. Thanh toán/hủy/áp voucher khóa đúng hàng
 * hóa đơn; thanh toán khóa tiếp hàng voucher (không join) — không luồng nào lấy suất chiếu sau hóa đơn
 * hay voucher trước hóa đơn nên không thể tạo chu trình khóa.
 *
 * <p>Mọi kiểm tra giá/phòng/tối đa 8 ghế/cặp Sweetbox và giá vé đều tính lại phía server trong
 * transaction, không tin client. Lỗi nghiệp vụ trả mã kết quả để controller đổi thành HTTP; ngoại lệ
 * kỹ thuật (DB) được ném ra ngoài để transaction rollback sạch.
 */
@Repository
public class HoaDonDAO extends DAO {

    public static final String CHO_THANH_TOAN = "CHO_THANH_TOAN";
    public static final String DA_THANH_TOAN = "DA_THANH_TOAN";
    public static final String THAT_BAI = "THAT_BAI";
    public static final String DA_HUY = "DA_HUY";
    public static final String HET_HAN = "HET_HAN";
    public static final String MO_BAN = "MO_BAN";

    private static final int SO_GHE_TOI_DA = 8;
    private static final int HAN_GIU_GIAY = 300;

    /** Mã kết quả nghiệp vụ để controller ánh xạ HTTP (không ném lỗi → transaction vẫn commit). */
    public enum MaThaoTac {
        THANH_CONG,
        DA_THANH_TOAN,
        DA_HUY,
        HET_HAN,
        KHONG_TIM_THAY,
        KHONG_PHAI_CHU,
        KHONG_THE_XU_LY,
        VOUCHER_KHONG_HOP_LE,
        THAT_BAI_MOCK
    }

    /** Kết quả kèm ảnh chụp hóa đơn (nếu còn đọc được) để controller dựng tóm tắt/envelope. */
    public record KetQuaThaoTac(MaThaoTac ma, HoaDon hoaDon) {
    }

    private final SuatChieuDAO suatChieuDAO;
    private final VeDAO veDAO;
    private final VoucherKhachHangDAO voucherKhachHangDAO;
    private final KhachHangDAO khachHangDAO;
    private final int diemMoiMuc;

    public HoaDonDAO(JdbcTemplate jdbcTemplate,
                     SuatChieuDAO suatChieuDAO,
                     VeDAO veDAO,
                     VoucherKhachHangDAO voucherKhachHangDAO,
                     KhachHangDAO khachHangDAO,
                     @Value("${points.per-amount:20000}") int diemMoiMuc) {
        super(jdbcTemplate);
        this.suatChieuDAO = suatChieuDAO;
        this.veDAO = veDAO;
        this.voucherKhachHangDAO = voucherKhachHangDAO;
        this.khachHangDAO = khachHangDAO;
        this.diemMoiMuc = diemMoiMuc > 0 ? diemMoiMuc : 20000;
    }

    // ------------------------------------------------------------------ đặt vé

    /**
     * Tạo hóa đơn + vé {@code DANG_GIU} trong một transaction (R4/R5/R6/R7).
     *
     * <p>{@code dsVe} chỉ cần mang id suất chiếu + id ghế; giá, phòng, cặp Sweetbox và giới hạn 8 ghế
     * được xác minh lại từ DB. Trả {@code null} khi lựa chọn không hợp lệ/ghế đã bị giữ (transaction
     * vẫn commit phần nhả ghế hết hạn, không có dữ liệu rác).
     */
    @Transactional
    public HoaDon taoHoaDon(HoaDon hoaDon, List<Ve> dsVe) {
        if (hoaDon == null || hoaDon.getKhachHang() == null || hoaDon.getKhachHang().getId() <= 0) {
            return null;
        }
        if (dsVe == null || dsVe.isEmpty() || dsVe.size() > SO_GHE_TOI_DA) {
            return null;
        }

        Integer idSuatChieu = null;
        Set<Integer> idGhe = new LinkedHashSet<>();
        for (Ve ve : dsVe) {
            if (ve == null || ve.getSuatChieu() == null || ve.getGhe() == null
                    || ve.getSuatChieu().getId() <= 0 || ve.getGhe().getId() <= 0) {
                return null;
            }
            if (idSuatChieu == null) {
                idSuatChieu = ve.getSuatChieu().getId();
            } else if (idSuatChieu != ve.getSuatChieu().getId()) {
                return null;
            }
            if (!idGhe.add(ve.getGhe().getId())) {
                return null;
            }
        }

        int khachHangId = hoaDon.getKhachHang().getId();
        SuatChieu suatChieu = suatChieuDAO.khoaTheoId(idSuatChieu);
        KhachHang khachHang = khachHangDAO.layTheoId(khachHangId);
        if (suatChieu == null || !MO_BAN.equals(suatChieu.getTrangThai()) || khachHang == null) {
            return null;
        }

        List<Ghe> gheCaPhong = veDAO.laydsGhe(idSuatChieu);
        List<Ghe> gheDaChon = new ArrayList<>(dsVe.size());
        for (int id : idGhe) {
            Ghe ghe = null;
            for (Ghe ungVien : gheCaPhong) {
                if (ungVien.getId() == id) {
                    ghe = ungVien;
                    break;
                }
            }
            if (ghe == null) {
                return null;
            }
            gheDaChon.add(ghe);
        }
        if (!capSweetboxHopLe(gheDaChon, gheCaPhong)) {
            return null;
        }

        LocalDateTime ngayTao = LocalDateTime.now().withNano(0);
        giaiPhongGiuHetHanTrongGiaoDich(idSuatChieu, ngayTao);
        for (Ghe ghe : gheDaChon) {
            if (veDAO.kiemTraVe(idSuatChieu, ghe.getId())) {
                return null;
            }
        }

        LocalDateTime hetHan = ngayTao.plusSeconds(HAN_GIU_GIAY);
        GeneratedKeyHolder khoa = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO tblHoaDon (ngayTao, hetHan, trangThai, tblKhachHangId, tblVoucherKhachHangId)
                    VALUES (?, ?, ?, ?, NULL)""", Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, ngayTao);
            ps.setObject(2, hetHan);
            ps.setString(3, CHO_THANH_TOAN);
            ps.setInt(4, khachHangId);
            return ps;
        }, khoa);
        int idHoaDon = Objects.requireNonNull(khoa.getKey()).intValue();

        List<Ve> veDaTao = new ArrayList<>(gheDaChon.size());
        for (Ghe ghe : gheDaChon) {
            Ve ve = veDAO.tinhTienVe(suatChieu, ghe);
            veDAO.luuVe(ve, idHoaDon);
            veDaTao.add(ve);
        }

        hoaDon.setId(idHoaDon);
        hoaDon.setNgayTao(ngayTao);
        hoaDon.setHetHan(hetHan);
        hoaDon.setTrangThai(CHO_THANH_TOAN);
        hoaDon.setVoucher(null);
        hoaDon.setDsVe(veDaTao);
        hoaDon.setKhachHang(khachHang);
        return hoaDon;
    }

    private static boolean capSweetboxHopLe(List<Ghe> daChon, List<Ghe> caPhong) {
        Set<Integer> daChonIds = new LinkedHashSet<>();
        for (Ghe ghe : daChon) {
            daChonIds.add(ghe.getId());
        }
        for (Ghe ghe : daChon) {
            if (!laSweetbox(ghe)) {
                continue;
            }
            int soGhe;
            try {
                soGhe = Integer.parseInt(ghe.getSoGhe());
            } catch (NumberFormatException | NullPointerException ngoaiLe) {
                return false;
            }
            int soBan = soGhe % 2 == 1 ? soGhe + 1 : soGhe - 1;
            Ghe ban = null;
            for (Ghe ungVien : caPhong) {
                if (ungVien.getId() != ghe.getId()
                        && Objects.equals(ungVien.getHangGhe(), ghe.getHangGhe())
                        && cungLoaiGhe(ungVien, ghe)
                        && Objects.equals(ungVien.getSoGhe(), String.valueOf(soBan))) {
                    ban = ungVien;
                    break;
                }
            }
            if (ban == null || !daChonIds.contains(ban.getId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean laSweetbox(Ghe ghe) {
        if (ghe == null || ghe.getLoaiGhe() == null || ghe.getLoaiGhe().getTenLoai() == null) {
            return false;
        }
        String ten = ghe.getLoaiGhe().getTenLoai().toLowerCase(Locale.ROOT);
        return ten.contains("sweetbox") || ten.contains("đôi") || ten.contains("doi");
    }

    private static boolean cungLoaiGhe(Ghe a, Ghe b) {
        return a.getLoaiGhe() != null && b.getLoaiGhe() != null
                && a.getLoaiGhe().getId() == b.getLoaiGhe().getId();
    }

    // ------------------------------------------------------------------ tóm tắt / giá

    /** Tổng tiền vé trước voucher. */
    public int tinhTongTien(List<Ve> dsVe) {
        int tong = 0;
        if (dsVe == null) {
            return 0;
        }
        for (Ve ve : dsVe) {
            if (ve != null) {
                tong += ve.getGiaVe();
            }
        }
        return tong;
    }

    /**
     * Tổng tiền sau voucher (R3): GIAM_GIA trừ cố định (không âm); VE_PHIM miễn vé rẻ nhất và chỉ áp
     * cho suất 2D; BAP_NUOC không áp hóa đơn.
     */
    public int tinhTongTien(List<Ve> dsVe, VoucherKhachHang voucher) {
        int tong = tinhTongTien(dsVe);
        if (voucher == null || dsVe == null || dsVe.isEmpty()) {
            return tong;
        }
        QuaTang quaTang = voucher.getGiaoDichDoiDiem() != null
                ? voucher.getGiaoDichDoiDiem().getQuaTang() : null;
        if (quaTang instanceof VoucherGiamGia giamGia) {
            return Math.max(0, tong - giamGia.getGiaTriGiam());
        }
        if (quaTang instanceof VoucherVeTang) {
            if (!tatCaSuat2D(dsVe)) {
                return tong;
            }
            int giaReNhat = Integer.MAX_VALUE;
            for (Ve ve : dsVe) {
                if (ve != null && ve.getGiaVe() < giaReNhat) {
                    giaReNhat = ve.getGiaVe();
                }
            }
            return giaReNhat == Integer.MAX_VALUE ? tong : Math.max(0, tong - giaReNhat);
        }
        return tong;
    }

    /** R1: 1 điểm / {@code points.per-amount}đ tiền vé trước voucher, làm tròn xuống. */
    public int tinhDiemCong(int tongTienTruocVoucher) {
        if (tongTienTruocVoucher <= 0 || diemMoiMuc <= 0) {
            return 0;
        }
        return tongTienTruocVoucher / diemMoiMuc;
    }

    /** Số dư điểm mới nhất của khách để trả trong tóm tắt. */
    public int layTongDiem(int khachHangId) {
        KhachHang khachHang = khachHangDAO.layTheoId(khachHangId);
        return khachHang != null ? khachHang.getTongDiem() : 0;
    }

    private static boolean tatCaSuat2D(List<Ve> dsVe) {
        if (dsVe == null || dsVe.isEmpty()) {
            return false;
        }
        for (Ve ve : dsVe) {
            if (ve == null || ve.getSuatChieu() == null || ve.getSuatChieu().getPhongChieu() == null) {
                return false;
            }
            String loaiPhong = ve.getSuatChieu().getPhongChieu().getLoaiPhong();
            if (loaiPhong == null || !loaiPhong.trim().toUpperCase(Locale.ROOT).contains("2D")) {
                return false;
            }
        }
        return true;
    }

    private static boolean voucherApDungDuoc(VoucherKhachHang voucher, List<Ve> dsVe) {
        if (voucher == null || voucher.getGiaoDichDoiDiem() == null) {
            return false;
        }
        QuaTang quaTang = voucher.getGiaoDichDoiDiem().getQuaTang();
        if (quaTang instanceof VoucherVatPham) {
            return false;
        }
        if (quaTang instanceof VoucherVeTang) {
            return tatCaSuat2D(dsVe);
        }
        if (quaTang instanceof VoucherGiamGia giamGia) {
            return giamGia.getGiaTriGiam() > 0;
        }
        return false;
    }

    // ------------------------------------------------------------------ đọc hóa đơn

    /** Hóa đơn đầy đủ (vé + voucher) theo id; {@code null} nếu không có. */
    public HoaDon layTheoId(int idHoaDon) {
        if (idHoaDon <= 0) {
            return null;
        }
        List<HoaDon> ds = jdbcTemplate.query("""
                SELECT id, ngayTao, hetHan, trangThai, tblKhachHangId, tblVoucherKhachHangId
                FROM tblHoaDon WHERE id = ?""", (rs, soThuTu) -> {
                    HoaDon hoaDon = new HoaDon();
                    hoaDon.setId(rs.getInt("id"));
                    LocalDateTime ngayTao = rs.getObject("ngayTao", LocalDateTime.class);
                    if (ngayTao != null) {
                        hoaDon.setNgayTao(ngayTao.withNano(0));
                    }
                    LocalDateTime hetHan = rs.getObject("hetHan", LocalDateTime.class);
                    if (hetHan != null) {
                        hoaDon.setHetHan(hetHan.withNano(0));
                    }
                    hoaDon.setTrangThai(rs.getString("trangThai"));
                    KhachHang khachHang = new KhachHang();
                    khachHang.setId(rs.getInt("tblKhachHangId"));
                    hoaDon.setKhachHang(khachHang);
                    int idVoucher = rs.getInt("tblVoucherKhachHangId");
                    if (!rs.wasNull()) {
                        VoucherKhachHang voucher = new VoucherKhachHang();
                        voucher.setId(idVoucher);
                        hoaDon.setVoucher(voucher);
                    }
                    return hoaDon;
                }, idHoaDon);
        if (ds.isEmpty()) {
            return null;
        }
        HoaDon hoaDon = ds.get(0);
        hoaDon.setDsVe(veDAO.laydsVeTheoHoaDon(idHoaDon));
        if (hoaDon.getVoucher() != null) {
            hoaDon.setVoucher(voucherKhachHangDAO.layTheoId(hoaDon.getVoucher().getId()));
        }
        KhachHang khachHang = khachHangDAO.layTheoId(hoaDon.getKhachHang().getId());
        if (khachHang != null) {
            hoaDon.setKhachHang(khachHang);
        }
        return hoaDon;
    }

    private record HoaDonKhoa(int id, LocalDateTime ngayTao, LocalDateTime hetHan,
                              String trangThai, int khachHangId, Integer voucherId) {
    }

    /** Khóa hàng hóa đơn (locking read) — bắt buộc gọi trong transaction. */
    private HoaDonKhoa khoaHoaDon(int idHoaDon) {
        List<HoaDonKhoa> ds = jdbcTemplate.query("""
                SELECT id, ngayTao, hetHan, trangThai, tblKhachHangId, tblVoucherKhachHangId
                FROM tblHoaDon WHERE id = ? FOR UPDATE""", (rs, soThuTu) -> {
                    LocalDateTime ngayTao = rs.getObject("ngayTao", LocalDateTime.class);
                    LocalDateTime hetHan = rs.getObject("hetHan", LocalDateTime.class);
                    int idVoucher = rs.getInt("tblVoucherKhachHangId");
                    Integer voucherId = rs.wasNull() ? null : idVoucher;
                    return new HoaDonKhoa(
                            rs.getInt("id"),
                            ngayTao != null ? ngayTao.withNano(0) : null,
                            hetHan != null ? hetHan.withNano(0) : null,
                            rs.getString("trangThai"),
                            rs.getInt("tblKhachHangId"),
                            voucherId);
                }, idHoaDon);
        return ds.isEmpty() ? null : ds.get(0);
    }

    // ------------------------------------------------------------------ áp voucher

    /**
     * Áp voucher vào hóa đơn đang chờ thanh toán: khóa hóa đơn, kiểm tra chủ/trạng thái/hạn, rồi kiểm
     * tra voucher đúng chủ, {@code CHUA_SU_DUNG}, còn hạn, loại áp được (không bắp nước, vé phim chỉ 2D)
     * và tối đa 1 voucher. Chỉ gắn FK — voucher vẫn chưa bị tiêu.
     */
    @Transactional
    public KetQuaThaoTac apDungVoucherChoHoaDon(int idHoaDon, int idKhachHang, String maVoucher) {
        HoaDonKhoa hoaDon = khoaHoaDon(idHoaDon);
        if (hoaDon == null) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_TIM_THAY, null);
        }
        if (idKhachHang > 0 && hoaDon.khachHangId() != idKhachHang) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_PHAI_CHU, null);
        }
        if (HET_HAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.HET_HAN, layTheoId(idHoaDon));
        }
        if (!CHO_THANH_TOAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_THE_XU_LY, layTheoId(idHoaDon));
        }
        if (!hoaDon.hetHan().isAfter(LocalDateTime.now())) {
            hetHanHoaDon(idHoaDon);
            return new KetQuaThaoTac(MaThaoTac.HET_HAN, layTheoId(idHoaDon));
        }

        VoucherKhachHang voucher = voucherKhachHangDAO.kiemTraVoucher(maVoucher, idKhachHang);
        if (voucher == null) {
            return new KetQuaThaoTac(MaThaoTac.VOUCHER_KHONG_HOP_LE, layTheoId(idHoaDon));
        }
        if (hoaDon.voucherId() != null && hoaDon.voucherId().intValue() != voucher.getId()) {
            return new KetQuaThaoTac(MaThaoTac.VOUCHER_KHONG_HOP_LE, layTheoId(idHoaDon));
        }

        HoaDon dayDu = layTheoId(idHoaDon);
        if (!voucherApDungDuoc(voucher, dayDu.getDsVe())) {
            return new KetQuaThaoTac(MaThaoTac.VOUCHER_KHONG_HOP_LE, dayDu);
        }
        if (hoaDon.voucherId() == null
                && !voucherKhachHangDAO.apDungVoucher(idHoaDon, voucher.getId())) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_THE_XU_LY, layTheoId(idHoaDon));
        }
        return new KetQuaThaoTac(MaThaoTac.THANH_CONG, layTheoId(idHoaDon));
    }

    // ------------------------------------------------------------------ thanh toán

    /** Dạng rút gọn giữ chữ ký ARCHITECTURE; không kiểm tra chủ sở hữu. */
    @Transactional
    public boolean xacNhanThanhToan(int idHoaDon) {
        MaThaoTac ma = xacNhanThanhToan(idHoaDon, 0, true).ma();
        return ma == MaThaoTac.THANH_CONG || ma == MaThaoTac.DA_THANH_TOAN;
    }

    /**
     * Thanh toán (mock) trong một transaction. Khóa hóa đơn, rồi voucher nếu có; <b>kiểm tra hạn sau
     * khi đã lấy đủ khóa</b>. Hóa đơn quá hạn được ghi {@code HET_HAN} + vé {@code DA_HUY} rồi trả mã
     * lỗi — không ném ngoại lệ nên phần hết hạn vẫn commit dù HTTP trả 4xx. Xác nhận lặp trên hóa đơn
     * đã trả tiền trả lại tóm tắt và không cộng điểm/tiêu voucher lần nữa.
     */
    @Transactional
    public KetQuaThaoTac xacNhanThanhToan(int idHoaDon, int idKhachHang, boolean thanhCong) {
        HoaDonKhoa hoaDon = khoaHoaDon(idHoaDon);
        if (hoaDon == null) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_TIM_THAY, null);
        }
        if (idKhachHang > 0 && hoaDon.khachHangId() != idKhachHang) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_PHAI_CHU, null);
        }
        if (DA_THANH_TOAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.DA_THANH_TOAN, layTheoId(idHoaDon));
        }
        if (!CHO_THANH_TOAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_THE_XU_LY, layTheoId(idHoaDon));
        }

        VoucherKhachHang voucher = null;
        if (thanhCong && hoaDon.voucherId() != null) {
            voucher = voucherKhachHangDAO.khoaTheoId(hoaDon.voucherId());
        }
        if (!hoaDon.hetHan().isAfter(LocalDateTime.now())) {
            hetHanHoaDon(idHoaDon);
            return new KetQuaThaoTac(MaThaoTac.HET_HAN, layTheoId(idHoaDon));
        }
        if (!thanhCong) {
            return new KetQuaThaoTac(MaThaoTac.THAT_BAI_MOCK, layTheoId(idHoaDon));
        }

        HoaDon dayDu = layTheoId(idHoaDon);
        if (hoaDon.voucherId() != null
                && !voucherHopLeLucThanhToan(voucher, idKhachHang, dayDu.getDsVe())) {
            return new KetQuaThaoTac(MaThaoTac.VOUCHER_KHONG_HOP_LE, dayDu);
        }

        int capNhat = jdbcTemplate.update("""
                UPDATE tblHoaDon SET trangThai = ? WHERE id = ? AND trangThai = ?""",
                DA_THANH_TOAN, idHoaDon, CHO_THANH_TOAN);
        if (capNhat == 0) {
            throw new DataIntegrityViolationException("Hóa đơn đã đổi trạng thái khi thanh toán.");
        }
        veDAO.capNhatTrangThaiVe(idHoaDon, VeDAO.DA_BAN);
        if (voucher != null && !voucherKhachHangDAO.danhDauDaSuDung(voucher.getId())) {
            throw new DataIntegrityViolationException("Voucher đã được sử dụng bởi hóa đơn khác.");
        }

        int diemCong = tinhDiemCong(tinhTongTien(dayDu.getDsVe()));
        if (diemCong > 0
                && !khachHangDAO.congDiem(String.valueOf(hoaDon.khachHangId()), diemCong)) {
            throw new DataIntegrityViolationException("Không cộng được điểm cho khách hàng.");
        }
        return new KetQuaThaoTac(MaThaoTac.THANH_CONG, layTheoId(idHoaDon));
    }

    private boolean voucherHopLeLucThanhToan(VoucherKhachHang voucher, int idKhachHang, List<Ve> dsVe) {
        if (voucher == null || !VoucherKhachHangDAO.CHUA_SU_DUNG.equals(voucher.getTrangThai())
                || !voucherKhachHangDAO.conHan(voucher)) {
            return false;
        }
        GiaoDichDoiDiem giaoDich = voucher.getGiaoDichDoiDiem();
        if (giaoDich == null || giaoDich.getKhachHang() == null
                || (idKhachHang > 0 && giaoDich.getKhachHang().getId() != idKhachHang)) {
            return false;
        }
        return voucherApDungDuoc(voucher, dsVe);
    }

    // ------------------------------------------------------------------ hủy / hết hạn

    /** Dạng rút gọn giữ chữ ký ARCHITECTURE; không kiểm tra chủ sở hữu. */
    @Transactional
    public boolean huyHoaDon(int idHoaDon) {
        MaThaoTac ma = huyHoaDon(idHoaDon, 0).ma();
        return ma == MaThaoTac.THANH_CONG || ma == MaThaoTac.DA_HUY;
    }

    /**
     * Hủy hóa đơn đang chờ thanh toán và nhả ghế. Không hủy hóa đơn đã thanh toán; hóa đơn quá hạn
     * được ghi {@code HET_HAN} (không phải {@code DA_HUY}) và trả mã tương ứng.
     */
    @Transactional
    public KetQuaThaoTac huyHoaDon(int idHoaDon, int idKhachHang) {
        HoaDonKhoa hoaDon = khoaHoaDon(idHoaDon);
        if (hoaDon == null) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_TIM_THAY, null);
        }
        if (idKhachHang > 0 && hoaDon.khachHangId() != idKhachHang) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_PHAI_CHU, null);
        }
        if (DA_HUY.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.THANH_CONG, layTheoId(idHoaDon));
        }
        if (HET_HAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.HET_HAN, layTheoId(idHoaDon));
        }
        if (DA_THANH_TOAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.DA_THANH_TOAN, layTheoId(idHoaDon));
        }
        if (!CHO_THANH_TOAN.equals(hoaDon.trangThai())) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_THE_XU_LY, layTheoId(idHoaDon));
        }
        if (!hoaDon.hetHan().isAfter(LocalDateTime.now())) {
            hetHanHoaDon(idHoaDon);
            return new KetQuaThaoTac(MaThaoTac.HET_HAN, layTheoId(idHoaDon));
        }
        int capNhat = jdbcTemplate.update("""
                UPDATE tblHoaDon SET trangThai = ? WHERE id = ? AND trangThai = ?""",
                DA_HUY, idHoaDon, CHO_THANH_TOAN);
        if (capNhat == 0) {
            return new KetQuaThaoTac(MaThaoTac.KHONG_THE_XU_LY, layTheoId(idHoaDon));
        }
        veDAO.capNhatTrangThaiVe(idHoaDon, VeDAO.DA_HUY);
        return new KetQuaThaoTac(MaThaoTac.THANH_CONG, layTheoId(idHoaDon));
    }

    /** Kiểm tra hạn và ghi {@code HET_HAN}/vé {@code DA_HUY} nếu đã quá 300s (R5/R9). */
    @Transactional
    public boolean kiemTraHetHan(int idHoaDon) {
        HoaDonKhoa hoaDon = khoaHoaDon(idHoaDon);
        if (hoaDon == null) {
            return false;
        }
        if (HET_HAN.equals(hoaDon.trangThai())) {
            return true;
        }
        if (!CHO_THANH_TOAN.equals(hoaDon.trangThai()) || hoaDon.hetHan().isAfter(LocalDateTime.now())) {
            return false;
        }
        hetHanHoaDon(idHoaDon);
        return true;
    }

    /**
     * Nhả mọi giữ ghế đã quá hạn của một suất chiếu (chạy trước khi đọc sơ đồ ghế và trong transaction
     * đặt vé). Hóa đơn quá hạn → {@code HET_HAN}, vé {@code DANG_GIU} → {@code DA_HUY}.
     */
    @Transactional
    public void giaiPhongGiuHetHan(int idSuatChieu) {
        if (idSuatChieu <= 0) {
            return;
        }
        giaiPhongGiuHetHanTrongGiaoDich(idSuatChieu, LocalDateTime.now());
    }

    private void giaiPhongGiuHetHanTrongGiaoDich(int idSuatChieu, LocalDateTime bayGio) {
        jdbcTemplate.update("""
                UPDATE tblHoaDon SET trangThai = 'HET_HAN'
                WHERE trangThai = 'CHO_THANH_TOAN' AND hetHan <= ?""",
                bayGio);
        jdbcTemplate.update("""
                UPDATE tblVe SET trangThaiVe = 'DA_HUY'
                WHERE trangThaiVe = 'DANG_GIU' AND tblSuatChieuId = ?
                  AND tblHoaDonId IN (SELECT id FROM tblHoaDon WHERE trangThai = 'HET_HAN')""",
                idSuatChieu);
    }

    private void hetHanHoaDon(int idHoaDon) {
        jdbcTemplate.update("""
                UPDATE tblHoaDon SET trangThai = 'HET_HAN'
                WHERE id = ? AND trangThai = 'CHO_THANH_TOAN'""", idHoaDon);
        veDAO.capNhatTrangThaiVe(idHoaDon, VeDAO.DA_HUY);
    }
}
