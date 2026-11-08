package com.cineman.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Phòng chiếu — tblPhongChieu; {@code loaiPhong} chứa 2D/3D…; hợp thành danh sách ghế.
 */
public class PhongChieu {

    private int id;
    private String tenPhong;
    private String loaiPhong;
    private int sucChua;
    private List<Ghe> dsGhe = new ArrayList<>();

    public PhongChieu() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTenPhong() {
        return tenPhong;
    }

    public void setTenPhong(String tenPhong) {
        this.tenPhong = tenPhong;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(String loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public int getSucChua() {
        return sucChua;
    }

    public void setSucChua(int sucChua) {
        this.sucChua = sucChua;
    }

    public List<Ghe> getDsGhe() {
        return dsGhe;
    }

    public void setDsGhe(List<Ghe> dsGhe) {
        this.dsGhe = dsGhe;
    }
}
