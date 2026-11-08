package com.cineman.entity;

/**
 * Quà GIAM_GIA — lưu giaTriGiam trong tblQuaTang; trừ cố định trên hóa đơn (R3).
 */
public class VoucherGiamGia extends QuaTang {

    private int giaTriGiam;

    public VoucherGiamGia() {
    }

    public int getGiaTriGiam() {
        return giaTriGiam;
    }

    public void setGiaTriGiam(int giaTriGiam) {
        this.giaTriGiam = giaTriGiam;
    }
}
