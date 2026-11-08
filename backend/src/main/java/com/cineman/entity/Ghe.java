package com.cineman.entity;

/**
 * Ghế — tblGhe. Sweetbox = 2 ghế liền kề cùng loại, mỗi ghế 1 vé (R6).
 */
public class Ghe {

    private int id;
    private String soGhe;
    private String hangGhe;
    private LoaiGhe loaiGhe;

    public Ghe() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSoGhe() {
        return soGhe;
    }

    public void setSoGhe(String soGhe) {
        this.soGhe = soGhe;
    }

    public String getHangGhe() {
        return hangGhe;
    }

    public void setHangGhe(String hangGhe) {
        this.hangGhe = hangGhe;
    }

    public LoaiGhe getLoaiGhe() {
        return loaiGhe;
    }

    public void setLoaiGhe(LoaiGhe loaiGhe) {
        this.loaiGhe = loaiGhe;
    }
}
