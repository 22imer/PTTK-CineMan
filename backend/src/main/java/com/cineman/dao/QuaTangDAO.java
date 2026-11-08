package com.cineman.dao;

import com.cineman.entity.QuaTang;
import com.cineman.entity.VoucherGiamGia;
import com.cineman.entity.VoucherVatPham;
import com.cineman.entity.VoucherVeTang;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Danh mục quà tặng đổi điểm — tblQuaTang (single-table, phân lớp theo {@code loaiQua}).
 *
 * <p>Ánh xạ dùng chung {@link #docQuaTang(ResultSet)} để cả danh mục và trình đọc voucher của
 * luồng đặt vé nhận đúng lớp con {@link VoucherGiamGia}/{@link VoucherVeTang}/{@link VoucherVatPham}.
 */
@Repository
public class QuaTangDAO extends DAO {

    private static final String CAC_COT = "id, tenQua, loaiQua, soDiem, soLuong, moTa, giaTriGiam";

    public QuaTangDAO(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /** Toàn bộ quà tặng kèm tồn kho hiện tại, sắp theo id. */
    public List<QuaTang> laydsQuaTang() {
        return jdbcTemplate.query("SELECT " + CAC_COT + " FROM tblQuaTang ORDER BY id",
                (rs, soThuTu) -> docQuaTang(rs));
    }

    /**
     * Khóa hàng quà tặng ({@code SELECT ... FOR UPDATE}) để kiểm tra tồn kho và trừ kho tuần tự
     * trong transaction của người gọi. Trả {@code null} nếu không tồn tại.
     */
    public QuaTang khoaQuaTang(int idQua) {
        if (idQua <= 0) {
            return null;
        }
        List<QuaTang> ds = jdbcTemplate.query(
                "SELECT " + CAC_COT + " FROM tblQuaTang WHERE id = ? FOR UPDATE",
                (rs, soThuTu) -> docQuaTang(rs), idQua);
        return ds.isEmpty() ? null : ds.get(0);
    }

    /** Còn ít nhất một phần quà trong kho. */
    public boolean kiemTraSoLuong(int idQua) {
        if (idQua <= 0) {
            return false;
        }
        List<Integer> ds = jdbcTemplate.query("SELECT soLuong FROM tblQuaTang WHERE id = ?",
                (rs, soThuTu) -> rs.getInt(1), idQua);
        return !ds.isEmpty() && ds.get(0) > 0;
    }

    /** Ghi tồn kho tuyệt đối; từ chối số âm. Người gọi phải giữ khóa hàng trước khi ghi. */
    public boolean capNhatSoLuong(int idQua, int soLuong) {
        if (idQua <= 0 || soLuong < 0) {
            return false;
        }
        return jdbcTemplate.update("UPDATE tblQuaTang SET soLuong = ? WHERE id = ?", soLuong, idQua) > 0;
    }

    /**
     * Đọc một hàng tblQuaTang thành đúng lớp con theo {@code loaiQua}.
     * Cột bắt buộc: id, tenQua, loaiQua, soDiem, soLuong, moTa, giaTriGiam.
     */
    public static QuaTang docQuaTang(ResultSet rs) throws SQLException {
        String loaiQua = rs.getString("loaiQua");
        QuaTang qua;
        if ("GIAM_GIA".equals(loaiQua)) {
            VoucherGiamGia giamGia = new VoucherGiamGia();
            giamGia.setGiaTriGiam(rs.getInt("giaTriGiam"));
            qua = giamGia;
        } else if ("VE_PHIM".equals(loaiQua)) {
            qua = new VoucherVeTang();
        } else if ("BAP_NUOC".equals(loaiQua)) {
            qua = new VoucherVatPham();
        } else {
            throw new SQLException("loaiQua không hợp lệ: " + loaiQua);
        }
        qua.setId(rs.getInt("id"));
        qua.setTenQua(rs.getString("tenQua"));
        qua.setLoaiQua(loaiQua);
        qua.setSoDiem(rs.getInt("soDiem"));
        qua.setSoLuong(rs.getInt("soLuong"));
        qua.setMoTa(rs.getString("moTa"));
        return qua;
    }
}
