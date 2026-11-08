package com.cineman.entity;

import java.time.LocalDate;

/**
 * Khách hàng — tblKhachHang (PK = FK tblThanhVienId, kế thừa ThanhVien 1-1).
 *
 * {@code hangThanhVien} giữ String, ngưỡng đổi hạng chưa chốt (ARCHITECTURE §12).
 */
public class KhachHang extends ThanhVien {

    private LocalDate ngayDangKy;
    private int tongDiem;
    private String hangThanhVien;

    public KhachHang() {
    }

    public LocalDate getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDate ngayDangKy) {
        this.ngayDangKy = ngayDangKy;
    }

    public int getTongDiem() {
        return tongDiem;
    }

    public void setTongDiem(int tongDiem) {
        this.tongDiem = tongDiem;
    }

    public String getHangThanhVien() {
        return hangThanhVien;
    }

    public void setHangThanhVien(String hangThanhVien) {
        this.hangThanhVien = hangThanhVien;
    }
}
