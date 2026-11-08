package com.cineman.entity;

import java.time.LocalDateTime;

/**
 * Phiếu đổi điểm (PDT-xxx) — tblGiaoDichDoiDiem; chỉ lưu phiếu thành công, ghi trước voucher (Q3).
 */
public class GiaoDichDoiDiem {

    private int id;
    private LocalDateTime thoiGian;
    private int soDiemTru;
    private int diemConLai;
    private KhachHang khachHang;
    private QuaTang quaTang;

    public GiaoDichDoiDiem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(LocalDateTime thoiGian) {
        this.thoiGian = thoiGian;
    }

    public int getSoDiemTru() {
        return soDiemTru;
    }

    public void setSoDiemTru(int soDiemTru) {
        this.soDiemTru = soDiemTru;
    }

    public int getDiemConLai() {
        return diemConLai;
    }

    public void setDiemConLai(int diemConLai) {
        this.diemConLai = diemConLai;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public QuaTang getQuaTang() {
        return quaTang;
    }

    public void setQuaTang(QuaTang quaTang) {
        this.quaTang = quaTang;
    }
}
