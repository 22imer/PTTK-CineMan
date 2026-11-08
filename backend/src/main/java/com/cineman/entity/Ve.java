package com.cineman.entity;

/**
 * Vé — tblVe; {@code trangThaiVe} ∈ {DANG_GIU, DA_BAN, DA_HUY}; {@code giaVe} = giaCoSo + phuThu.
 */
public class Ve {

    private int id;
    private int giaVe;
    private String trangThaiVe;
    private SuatChieu suatChieu;
    private Ghe ghe;

    public Ve() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGiaVe() {
        return giaVe;
    }

    public void setGiaVe(int giaVe) {
        this.giaVe = giaVe;
    }

    public String getTrangThaiVe() {
        return trangThaiVe;
    }

    public void setTrangThaiVe(String trangThaiVe) {
        this.trangThaiVe = trangThaiVe;
    }

    public SuatChieu getSuatChieu() {
        return suatChieu;
    }

    public void setSuatChieu(SuatChieu suatChieu) {
        this.suatChieu = suatChieu;
    }

    public Ghe getGhe() {
        return ghe;
    }

    public void setGhe(Ghe ghe) {
        this.ghe = ghe;
    }
}
