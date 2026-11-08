package com.cineman.dao;

import com.cineman.entity.GiaoDichDoiDiem;
import com.cineman.entity.KhachHang;
import com.cineman.entity.QuaTang;
import com.cineman.entity.VoucherKhachHang;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Phiếu đổi điểm PDT-xxx — tblGiaoDichDoiDiem. Chỉ lưu phiếu thành công, ghi trước voucher (Q3).
 *
 * <p>Không tự mở transaction: người gọi đặt {@code luuGiaoDich} trong transaction đổi điểm để lỗi
 * bước sau (tạo voucher) cuốn theo phiếu này.
 */
@Repository
public class GiaoDichDoiDiemDAO extends DAO {

    public GiaoDichDoiDiemDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Phiếu của đúng khách trong phiên, mới nhất trước; số dư lấy từ ảnh chụp trên phiếu. */
    public List<VoucherKhachHang> layLichSuTheoKhachHang(int idKhachHang) {
        return jdbcTemplate.query("""
                SELECT vc.id AS voucherId, vc.maVoucher, vc.ngayNhan, vc.ngaySuDung, vc.trangThai,
                       gd.id AS giaoDichId, gd.thoiGian, gd.soDiemTru, gd.diemConLai,
                       gd.tblKhachHangId AS giaoDichKhachHangId,
                       qt.id, qt.tenQua, qt.loaiQua, qt.soDiem, qt.soLuong, qt.moTa, qt.giaTriGiam
                FROM tblGiaoDichDoiDiem gd
                JOIN tblQuaTang qt ON qt.id = gd.tblQuaTangId
                JOIN tblVoucherKhachHang vc ON vc.tblGiaoDichDoiDiemId = gd.id
                WHERE gd.tblKhachHangId = ?
                ORDER BY gd.thoiGian DESC, gd.id DESC
                """, (rs, row) -> VoucherKhachHangDAO.docVoucher(rs), idKhachHang);
    }

    /** Lưu phiếu, gán lại id/thời gian vào {@code giaoDich} và trả id sinh ra. */
    public int luuGiaoDich(GiaoDichDoiDiem giaoDich) {
        Objects.requireNonNull(giaoDich, "giaoDich");
        KhachHang khachHang = Objects.requireNonNull(giaoDich.getKhachHang(), "khachHang");
        QuaTang quaTang = Objects.requireNonNull(giaoDich.getQuaTang(), "quaTang");
        LocalDateTime thoiGian = giaoDich.getThoiGian() != null ? giaoDich.getThoiGian() : LocalDateTime.now();

        GeneratedKeyHolder khoa = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO tblGiaoDichDoiDiem (thoiGian, soDiemTru, diemConLai, tblKhachHangId, tblQuaTangId)
                    VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, Timestamp.valueOf(thoiGian));
            ps.setInt(2, giaoDich.getSoDiemTru());
            ps.setInt(3, giaoDich.getDiemConLai());
            ps.setInt(4, khachHang.getId());
            ps.setInt(5, quaTang.getId());
            return ps;
        }, khoa);

        int id = Objects.requireNonNull(khoa.getKey()).intValue();
        giaoDich.setId(id);
        giaoDich.setThoiGian(thoiGian);
        return id;
    }
}
