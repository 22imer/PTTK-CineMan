package com.cineman.entity;

import java.time.LocalTime;

/**
 * Khung giờ chiếu — tblKhungGio.
 */
public class KhungGio {

    private int id;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;

    public KhungGio() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalTime getGioBatDau() {
        return gioBatDau;
    }

    public void setGioBatDau(LocalTime gioBatDau) {
        this.gioBatDau = gioBatDau;
    }

    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }

    public void setGioKetThuc(LocalTime gioKetThuc) {
        this.gioKetThuc = gioKetThuc;
    }
}
