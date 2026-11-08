package com.cineman.entity;

/**
 * Quà tặng — tblQuaTang (single-table), phân lớp theo {@code loaiQua}.
 * Lớp con: {@link VoucherGiamGia} (GIAM_GIA), {@link VoucherVeTang} (VE_PHIM),
 * {@link VoucherVatPham} (BAP_NUOC); không có bảng riêng.
 */
public abstract class QuaTang {

    private int id;
    private String tenQua;
    private String loaiQua;
    private int soDiem;
    private int soLuong;
    private String moTa;

    public QuaTang() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTenQua() {
        return tenQua;
    }

    public void setTenQua(String tenQua) {
        this.tenQua = tenQua;
    }

    public String getLoaiQua() {
        return loaiQua;
    }

    public void setLoaiQua(String loaiQua) {
        this.loaiQua = loaiQua;
    }

    public int getSoDiem() {
        return soDiem;
    }

    public void setSoDiem(int soDiem) {
        this.soDiem = soDiem;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
}
