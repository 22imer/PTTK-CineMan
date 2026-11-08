package com.cineman.dao;

import com.cineman.entity.GiaoDichDoiDiem;
import com.cineman.entity.KhachHang;
import com.cineman.entity.VoucherKhachHang;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Voucher phát cho khách — tblVoucherKhachHang (ARCHITECTURE §5, R2/R3).
 *
 * <p>Chủ/quà suy ra qua phiếu {@code tblGiaoDichDoiDiem}; hạn = {@code ngayNhan + 30 ngày} (dẫn xuất,
 * không lưu cột). Áp voucher chỉ kiểm tra và gắn FK vào hóa đơn — voucher vẫn {@code CHUA_SU_DUNG}
 * cho tới khi thanh toán thành công; hủy/hết hạn hóa đơn không khóa voucher.
 *
 * <p>Khóa đọc dùng {@code SELECT id, trangThai, ngayNhan ... FOR UPDATE} trên đúng hàng voucher (không
 * join) để tránh khóa chéo với luồng đổi điểm (khách → quà) và để trạng thái đọc được là bản mới nhất.
 */
@Repository
public class VoucherKhachHangDAO extends DAO {

    public static final String CHUA_SU_DUNG = "CHUA_SU_DUNG";
    public static final String DA_SU_DUNG = "DA_SU_DUNG";

    /** Số ngày hiệu lực kể từ ngày nhận (R2). */
    private static final int HAN_NGAY = 30;

    private static final String COT_VOUCHER = """
            vc.id AS voucherId, vc.maVoucher, vc.ngayNhan, vc.ngaySuDung, vc.trangThai,
            gd.id AS giaoDichId, gd.thoiGian, gd.soDiemTru, gd.diemConLai,
            gd.tblKhachHangId AS giaoDichKhachHangId,
            qt.id AS id, qt.tenQua, qt.loaiQua, qt.soDiem, qt.soLuong, qt.moTa, qt.giaTriGiam""";

    private static final String TU_VOUCHER = """
            FROM tblVoucherKhachHang vc
            JOIN tblGiaoDichDoiDiem gd ON gd.id = vc.tblGiaoDichDoiDiemId
            JOIN tblQuaTang qt ON qt.id = gd.tblQuaTangId""";

    public VoucherKhachHangDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Voucher đầy đủ (kèm phiếu + quà) theo id, {@code null} nếu không có. */
    public VoucherKhachHang layTheoId(int idVoucher) {
        if (idVoucher <= 0) {
            return null;
        }
        List<VoucherKhachHang> ds = jdbcTemplate.query(
                "SELECT " + COT_VOUCHER + " " + TU_VOUCHER + " WHERE vc.id = ?",
                (rs, soThuTu) -> docVoucher(rs), idVoucher);
        return ds.isEmpty() ? null : ds.get(0);
    }

    private VoucherKhachHang layTheoMa(String maVoucher) {
        if (maVoucher == null || maVoucher.isBlank()) {
            return null;
        }
        List<VoucherKhachHang> ds = jdbcTemplate.query(
                "SELECT " + COT_VOUCHER + " " + TU_VOUCHER + " WHERE vc.maVoucher = ?",
                (rs, soThuTu) -> docVoucher(rs), maVoucher);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /**
     * Khóa hàng voucher và trả bản đầy đủ. Trạng thái/ngày nhận lấy từ chính locking read để không
     * phụ thuộc snapshot REPEATABLE READ khi phải chờ transaction khác nhả khóa.
     */
    public VoucherKhachHang khoaTheoId(int idVoucher) {
        if (idVoucher <= 0) {
            return null;
        }
        List<TrangThaiVoucher> ds = jdbcTemplate.query("""
                SELECT id, trangThai, ngayNhan FROM tblVoucherKhachHang WHERE id = ? FOR UPDATE""",
                (rs, soThuTu) -> new TrangThaiVoucher(rs.getString("trangThai"),
                        rs.getDate("ngayNhan") != null ? rs.getDate("ngayNhan").toLocalDate() : null),
                idVoucher);
        if (ds.isEmpty()) {
            return null;
        }
        VoucherKhachHang voucher = layTheoId(idVoucher);
        if (voucher != null) {
            voucher.setTrangThai(ds.get(0).trangThai());
            voucher.setNgayNhan(ds.get(0).ngayNhan());
        }
        return voucher;
    }

    private record TrangThaiVoucher(String trangThai, LocalDate ngayNhan) {
    }

    /**
     * Voucher thuộc đúng khách, còn {@code CHUA_SU_DUNG} và còn hạn? {@code null} nếu không hợp lệ.
     * Kiểm tra loại quà/khả năng áp hóa đơn do tầng gọi quyết định (R3).
     */
    public VoucherKhachHang kiemTraVoucher(String maVoucher, int idKhachHang) {
        if (idKhachHang <= 0) {
            return null;
        }
        VoucherKhachHang voucher = layTheoMa(maVoucher);
        if (voucher == null || !CHUA_SU_DUNG.equals(voucher.getTrangThai()) || !conHan(voucher)) {
            return null;
        }
        GiaoDichDoiDiem giaoDich = voucher.getGiaoDichDoiDiem();
        if (giaoDich == null || giaoDich.getKhachHang() == null
                || giaoDich.getKhachHang().getId() != idKhachHang) {
            return null;
        }
        return voucher;
    }

    /** Còn hạn dùng? Hết hạn khi {@code ngayNhan + 30 ngày} không còn sau hôm nay (R2). */
    public boolean conHan(VoucherKhachHang voucher) {
        return voucher != null && voucher.getNgayNhan() != null
                && voucher.getNgayNhan().plusDays(HAN_NGAY).isAfter(LocalDate.now());
    }

    /** Gắn voucher vào hóa đơn đang chờ thanh toán (không đổi trạng thái voucher). */
    public boolean apDungVoucher(int idHoaDon, int idVoucher) {
        if (idHoaDon <= 0 || idVoucher <= 0) {
            return false;
        }
        return jdbcTemplate.update("""
                UPDATE tblHoaDon SET tblVoucherKhachHangId = ?
                WHERE id = ? AND trangThai = 'CHO_THANH_TOAN'""", idVoucher, idHoaDon) > 0;
    }

    /** Đánh dấu voucher đã dùng khi thanh toán thành công; chỉ một transaction thắng. */
    public boolean danhDauDaSuDung(int idVoucher) {
        if (idVoucher <= 0) {
            return false;
        }
        return jdbcTemplate.update("""
                UPDATE tblVoucherKhachHang SET trangThai = 'DA_SU_DUNG', ngaySuDung = ?
                WHERE id = ? AND trangThai = 'CHUA_SU_DUNG'""",
                Date.valueOf(LocalDate.now()), idVoucher) > 0;
    }

    /**
     * Lưu voucher mới trong transaction của người gọi (đổi điểm gọi sau khi đã ghi phiếu), trả id sinh
     * ra. Mã do người gọi cấp; nếu thiếu thì sinh mã UUID mới {@code VC-...} (không tái dùng mã seed).
     */
    public int luuVoucher(VoucherKhachHang voucher) {
        Objects.requireNonNull(voucher, "voucher");
        GiaoDichDoiDiem giaoDich = Objects.requireNonNull(voucher.getGiaoDichDoiDiem(), "giaoDichDoiDiem");
        String maVoucher = voucher.getMaVoucher() == null || voucher.getMaVoucher().isBlank()
                ? "VC-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT)
                : voucher.getMaVoucher();
        LocalDate ngayNhan = voucher.getNgayNhan() != null ? voucher.getNgayNhan() : LocalDate.now();
        String trangThai = voucher.getTrangThai() != null ? voucher.getTrangThai() : CHUA_SU_DUNG;

        GeneratedKeyHolder khoa = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO tblVoucherKhachHang
                        (maVoucher, ngayNhan, ngaySuDung, trangThai, tblGiaoDichDoiDiemId)
                    VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, maVoucher);
            ps.setDate(2, Date.valueOf(ngayNhan));
            ps.setDate(3, voucher.getNgaySuDung() != null ? Date.valueOf(voucher.getNgaySuDung()) : null);
            ps.setString(4, trangThai);
            ps.setInt(5, giaoDich.getId());
            return ps;
        }, khoa);

        int id = Objects.requireNonNull(khoa.getKey()).intValue();
        voucher.setId(id);
        voucher.setMaVoucher(maVoucher);
        voucher.setNgayNhan(ngayNhan);
        voucher.setTrangThai(trangThai);
        return id;
    }

    static VoucherKhachHang docVoucher(ResultSet rs) throws SQLException {
        VoucherKhachHang voucher = new VoucherKhachHang();
        voucher.setId(rs.getInt("voucherId"));
        voucher.setMaVoucher(rs.getString("maVoucher"));
        Date ngayNhan = rs.getDate("ngayNhan");
        if (ngayNhan != null) {
            voucher.setNgayNhan(ngayNhan.toLocalDate());
        }
        Date ngaySuDung = rs.getDate("ngaySuDung");
        if (ngaySuDung != null) {
            voucher.setNgaySuDung(ngaySuDung.toLocalDate());
        }
        voucher.setTrangThai(rs.getString("trangThai"));

        GiaoDichDoiDiem giaoDich = new GiaoDichDoiDiem();
        giaoDich.setId(rs.getInt("giaoDichId"));
        LocalDateTime thoiGian = rs.getObject("thoiGian", LocalDateTime.class);
        if (thoiGian != null) {
            giaoDich.setThoiGian(thoiGian.withNano(0));
        }
        giaoDich.setSoDiemTru(rs.getInt("soDiemTru"));
        giaoDich.setDiemConLai(rs.getInt("diemConLai"));

        KhachHang khachHang = new KhachHang();
        khachHang.setId(rs.getInt("giaoDichKhachHangId"));
        giaoDich.setKhachHang(khachHang);
        giaoDich.setQuaTang(QuaTangDAO.docQuaTang(rs));
        voucher.setGiaoDichDoiDiem(giaoDich);
        return voucher;
    }
}
