package com.cineman.entity;

import java.time.Instant;

/**
 * Phiên đăng nhập — tblPhienDangNhap (ARCHITECTURE §7.3).
 *
 * <p>Không chứa token thô: DB chỉ giữ SHA-256 hex của refresh token hiện hành
 * ({@code refreshTokenHash}) và của token vừa bị xoay ({@code previousTokenHash} + {@code xoayLuc}).
 * {@code hetHan} là hạn tuyệt đối của phiên (7 ngày từ login); {@code thuHoiLuc} khác {@code null}
 * nghĩa là phiên đã bị thu hồi và mọi access token của nó phải bị từ chối.
 */
public class PhienDangNhap {

    private String id;
    private int khachHangId;
    private String refreshTokenHash;
    private String previousTokenHash;
    private Instant xoayLuc;
    private Instant ngayTao;
    private Instant hetHan;
    private Instant thuHoiLuc;

    public PhienDangNhap() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getKhachHangId() {
        return khachHangId;
    }

    public void setKhachHangId(int khachHangId) {
        this.khachHangId = khachHangId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getPreviousTokenHash() {
        return previousTokenHash;
    }

    public void setPreviousTokenHash(String previousTokenHash) {
        this.previousTokenHash = previousTokenHash;
    }

    public Instant getXoayLuc() {
        return xoayLuc;
    }

    public void setXoayLuc(Instant xoayLuc) {
        this.xoayLuc = xoayLuc;
    }

    public Instant getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Instant ngayTao) {
        this.ngayTao = ngayTao;
    }

    public Instant getHetHan() {
        return hetHan;
    }

    public void setHetHan(Instant hetHan) {
        this.hetHan = hetHan;
    }

    public Instant getThuHoiLuc() {
        return thuHoiLuc;
    }

    public void setThuHoiLuc(Instant thuHoiLuc) {
        this.thuHoiLuc = thuHoiLuc;
    }
}
