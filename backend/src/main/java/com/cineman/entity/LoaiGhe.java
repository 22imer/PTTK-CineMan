package com.cineman.entity;

/**
 * Loại ghế — tblLoaiGhe (Thường / VIP / Đôi-Sweetbox).
 */
public class LoaiGhe {

    private int id;
    private String tenLoai;
    private int phuThu;

    public LoaiGhe() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }

    public int getPhuThu() {
        return phuThu;
    }

    public void setPhuThu(int phuThu) {
        this.phuThu = phuThu;
    }
}
