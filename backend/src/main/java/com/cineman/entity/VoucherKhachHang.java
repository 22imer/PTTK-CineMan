package com.cineman.entity;

import java.time.LocalDate;

/**
 * Voucher phát cho khách — tblVoucherKhachHang; {@code trangThai} ∈ {CHUA_SU_DUNG, DA_SU_DUNG}.
 * Hạn = ngayNhan + 30 ngày (dẫn xuất); chủ và quà lấy qua {@code giaoDichDoiDiem}.
 */
public class VoucherKhachHang {

    private int id;
    private String maVoucher;
    private LocalDate ngayNhan;
    private LocalDate ngaySuDung;
    private String trangThai;
    private GiaoDichDoiDiem giaoDichDoiDiem;

    public VoucherKhachHang() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMaVoucher() {
        return maVoucher;
    }

    public void setMaVoucher(String maVoucher) {
        this.maVoucher = maVoucher;
    }

    public LocalDate getNgayNhan() {
        return ngayNhan;
    }

    public void setNgayNhan(LocalDate ngayNhan) {
        this.ngayNhan = ngayNhan;
    }

    public LocalDate getNgaySuDung() {
        return ngaySuDung;
    }

    public void setNgaySuDung(LocalDate ngaySuDung) {
        this.ngaySuDung = ngaySuDung;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public GiaoDichDoiDiem getGiaoDichDoiDiem() {
        return giaoDichDoiDiem;
    }

    public void setGiaoDichDoiDiem(GiaoDichDoiDiem giaoDichDoiDiem) {
        this.giaoDichDoiDiem = giaoDichDoiDiem;
    }
}
