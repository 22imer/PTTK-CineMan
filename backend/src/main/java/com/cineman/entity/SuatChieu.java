package com.cineman.entity;

import java.time.LocalDate;

/**
 * Suất chiếu — tblSuatChieu.
 */
public class SuatChieu {

    private int id;
    private LocalDate ngayChieu;
    private int giaCoSo;
    private String trangThai;
    private Phim phim;
    private KhungGio khungGio;
    private PhongChieu phongChieu;

    public SuatChieu() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getNgayChieu() {
        return ngayChieu;
    }

    public void setNgayChieu(LocalDate ngayChieu) {
        this.ngayChieu = ngayChieu;
    }

    public int getGiaCoSo() {
        return giaCoSo;
    }

    public void setGiaCoSo(int giaCoSo) {
        this.giaCoSo = giaCoSo;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public Phim getPhim() {
        return phim;
    }

    public void setPhim(Phim phim) {
        this.phim = phim;
    }

    public KhungGio getKhungGio() {
        return khungGio;
    }

    public void setKhungGio(KhungGio khungGio) {
        this.khungGio = khungGio;
    }

    public PhongChieu getPhongChieu() {
        return phongChieu;
    }

    public void setPhongChieu(PhongChieu phongChieu) {
        this.phongChieu = phongChieu;
    }
}
