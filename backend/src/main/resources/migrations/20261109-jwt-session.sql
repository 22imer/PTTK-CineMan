-- Migration KHÔNG phá dữ liệu cho phiên JWT (ARCHITECTURE §7.3, 09/11/2026).
-- Chỉ thêm bảng phiên đăng nhập + khóa ngoại; không DROP/ALTER bảng nghiệp vụ nào.
-- Chạy một lần trên DB đang có dữ liệu: mysql cineman < migrations/20261109-jwt-session.sql
--
-- DB chỉ lưu SHA-256 của refresh token (CHAR(64) hex). previousTokenHash + xoayLuc phục vụ phát hiện
-- replay trong cửa sổ 5 giây; hetHan là hạn tuyệt đối 7 ngày, không gia hạn khi refresh.

CREATE TABLE IF NOT EXISTS tblPhienDangNhap (
    id                VARCHAR(36) PRIMARY KEY,
    tblKhachHangId    INT          NOT NULL,
    refreshTokenHash  CHAR(64)     NOT NULL,
    previousTokenHash CHAR(64)     NULL,
    xoayLuc           DATETIME(3)  NULL,
    ngayTao           DATETIME(3)  NOT NULL,
    hetHan            DATETIME(3)  NOT NULL,
    thuHoiLuc         DATETIME(3)  NULL,
    CONSTRAINT fk_tblPhienDangNhap_tblKhachHang FOREIGN KEY (tblKhachHangId) REFERENCES tblKhachHang (tblThanhVienId)
);
