package com.cineman.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hóa đơn — tblHoaDon; 1..8 vé (R7); {@code hetHan} = ngayTao + 300s do server lưu (R5).
 * {@code trangThai} ∈ {CHO_THANH_TOAN, DA_THANH_TOAN, THAT_BAI, DA_HUY, HET_HAN}.
 * Tổng tiền là dẫn xuất (HoaDonDAO.tinhTongTien), không lưu cột.
 */
public class HoaDon {

    private int id;
    private LocalDateTime ngayTao;
    private LocalDateTime hetHan;
    private String trangThai;
    private KhachHang khachHang;
    private List<Ve> dsVe = new ArrayList<>();
    private VoucherKhachHang voucher;

    public HoaDon() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getHetHan() {
        return hetHan;
    }

    public void setHetHan(LocalDateTime hetHan) {
        this.hetHan = hetHan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public List<Ve> getDsVe() {
        return dsVe;
    }

    public void setDsVe(List<Ve> dsVe) {
        this.dsVe = dsVe;
    }

    public VoucherKhachHang getVoucher() {
        return voucher;
    }

    public void setVoucher(VoucherKhachHang voucher) {
        this.voucher = voucher;
    }
}
