-- CineMan M1 — schema MySQL (ARCHITECTURE.md §3). Chạy lại được: xóa bảng theo thứ tự phụ thuộc rồi tạo mới.
-- Dẫn xuất KHÔNG lưu cột: tổng tiền hóa đơn, hạn voucher (ngayNhan + 30 ngày), mã hiển thị (tiền tố + id, trừ maVoucher).
-- Trùng (suất, ghế) kiểm tra ở tầng ứng dụng trong taoHoaDon — không UNIQUE trơn trên tblVe.

DROP TABLE IF EXISTS tblPhienDangNhap;
DROP TABLE IF EXISTS tblVe;
DROP TABLE IF EXISTS tblHoaDon;
DROP TABLE IF EXISTS tblVoucherKhachHang;
DROP TABLE IF EXISTS tblGiaoDichDoiDiem;
DROP TABLE IF EXISTS tblQuaTang;
DROP TABLE IF EXISTS tblGhe;
DROP TABLE IF EXISTS tblLoaiGhe;
DROP TABLE IF EXISTS tblSuatChieu;
DROP TABLE IF EXISTS tblPhongChieu;
DROP TABLE IF EXISTS tblKhungGio;
DROP TABLE IF EXISTS tblPhim;
DROP TABLE IF EXISTS tblKhachHang;
DROP TABLE IF EXISTS tblThanhVien;

CREATE TABLE tblThanhVien (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL,
    password  VARCHAR(100) NOT NULL, -- BCrypt
    hoTen     VARCHAR(100) NOT NULL,
    ngaySinh  DATE         NOT NULL,
    gioiTinh  VARCHAR(100) NOT NULL,
    diaChi    VARCHAR(100) NOT NULL,
    email     VARCHAR(100) NOT NULL,
    sdt       VARCHAR(100) NOT NULL,
    vaiTro    VARCHAR(100) NOT NULL,
    CONSTRAINT uq_tblThanhVien_username UNIQUE (username),
    CONSTRAINT uq_tblThanhVien_email UNIQUE (email),
    CONSTRAINT uq_tblThanhVien_sdt UNIQUE (sdt)
);

CREATE TABLE tblKhachHang (
    tblThanhVienId INT PRIMARY KEY,
    ngayDangKy     DATE         NOT NULL,
    tongDiem       INT          NOT NULL DEFAULT 0,
    hangThanhVien  VARCHAR(100) NULL, -- ngưỡng đổi hạng chưa chốt (ARCHITECTURE §12)
    CONSTRAINT ck_tblKhachHang_tongDiem CHECK (tongDiem >= 0),
    CONSTRAINT fk_tblKhachHang_tblThanhVien FOREIGN KEY (tblThanhVienId) REFERENCES tblThanhVien (id)
);

-- Phiên đăng nhập JWT (ARCHITECTURE §7.3): chỉ lưu SHA-256 refresh token, không lưu token thô.
-- previousTokenHash + xoayLuc phát hiện replay trong 5 giây; hetHan là hạn tuyệt đối, refresh không kéo dài.
CREATE TABLE tblPhienDangNhap (
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

CREATE TABLE tblPhim (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    tenPhim       VARCHAR(100) NOT NULL,
    theLoai       VARCHAR(100) NOT NULL,
    daoDien       VARCHAR(100) NOT NULL,
    thoiLuong     INT          NOT NULL,
    doTuoi        VARCHAR(100) NOT NULL, -- chỉ hiển thị (R10)
    moTa          VARCHAR(255) NULL,
    ngayKhoiChieu DATE         NOT NULL,
    ngayKetThuc   DATE         NOT NULL
);

CREATE TABLE tblKhungGio (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    gioBatDau  TIME NOT NULL,
    gioKetThuc TIME NOT NULL
);

CREATE TABLE tblPhongChieu (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    tenPhong  VARCHAR(100) NOT NULL,
    loaiPhong VARCHAR(100) NOT NULL, -- 2D / 3D …
    sucChua   INT          NOT NULL
);

CREATE TABLE tblSuatChieu (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    ngayChieu        DATE         NOT NULL,
    giaCoSo          INT          NOT NULL,
    trangThai        VARCHAR(100) NOT NULL,
    tblPhimId        INT          NOT NULL,
    tblKhungGioId    INT          NOT NULL,
    tblPhongChieuId  INT          NOT NULL,
    CONSTRAINT fk_tblSuatChieu_tblPhim FOREIGN KEY (tblPhimId) REFERENCES tblPhim (id),
    CONSTRAINT fk_tblSuatChieu_tblKhungGio FOREIGN KEY (tblKhungGioId) REFERENCES tblKhungGio (id),
    CONSTRAINT fk_tblSuatChieu_tblPhongChieu FOREIGN KEY (tblPhongChieuId) REFERENCES tblPhongChieu (id)
);

CREATE TABLE tblLoaiGhe (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    tenLoai VARCHAR(100) NOT NULL,
    phuThu  INT          NOT NULL
);

CREATE TABLE tblGhe (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    soGhe            VARCHAR(100) NOT NULL,
    hangGhe          VARCHAR(100) NOT NULL,
    tblPhongChieuId  INT          NOT NULL,
    tblLoaiGheId     INT          NOT NULL,
    CONSTRAINT uq_tblGhe_vitri UNIQUE (tblPhongChieuId, hangGhe, soGhe),
    CONSTRAINT fk_tblGhe_tblPhongChieu FOREIGN KEY (tblPhongChieuId) REFERENCES tblPhongChieu (id),
    CONSTRAINT fk_tblGhe_tblLoaiGhe FOREIGN KEY (tblLoaiGheId) REFERENCES tblLoaiGhe (id)
);

-- 3 lớp con QuaTang gộp vào tblQuaTang, phân biệt bằng loaiQua; giaTriGiam chỉ dùng cho GIAM_GIA.
CREATE TABLE tblQuaTang (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    tenQua     VARCHAR(100) NOT NULL,
    loaiQua    VARCHAR(100) NOT NULL,
    soDiem     INT          NOT NULL,
    soLuong    INT          NOT NULL,
    moTa       VARCHAR(100) NULL,
    giaTriGiam INT          NULL,
    CONSTRAINT ck_tblQuaTang_loaiQua CHECK (loaiQua IN ('GIAM_GIA', 'VE_PHIM', 'BAP_NUOC')),
    CONSTRAINT ck_tblQuaTang_soLuong CHECK (soLuong >= 0)
);

-- Phiếu đổi điểm PDT-xxx: chỉ lưu phiếu thành công, không có trangThai
CREATE TABLE tblGiaoDichDoiDiem (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    thoiGian       DATETIME NOT NULL,
    soDiemTru      INT      NOT NULL,
    diemConLai     INT      NOT NULL,
    tblKhachHangId INT      NOT NULL,
    tblQuaTangId   INT      NOT NULL,
    CONSTRAINT fk_tblGiaoDichDoiDiem_tblKhachHang FOREIGN KEY (tblKhachHangId) REFERENCES tblKhachHang (tblThanhVienId),
    CONSTRAINT fk_tblGiaoDichDoiDiem_tblQuaTang FOREIGN KEY (tblQuaTangId) REFERENCES tblQuaTang (id)
);

CREATE TABLE tblVoucherKhachHang (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    maVoucher            VARCHAR(100) NOT NULL,
    ngayNhan             DATE         NOT NULL,
    ngaySuDung           DATE         NULL,
    trangThai            VARCHAR(100) NOT NULL,
    tblGiaoDichDoiDiemId INT          NOT NULL,
    CONSTRAINT uq_tblVoucherKhachHang_maVoucher UNIQUE (maVoucher),
    CONSTRAINT uq_tblVoucherKhachHang_giaoDich UNIQUE (tblGiaoDichDoiDiemId),
    CONSTRAINT ck_tblVoucherKhachHang_trangThai CHECK (trangThai IN ('CHUA_SU_DUNG', 'DA_SU_DUNG')),
    CONSTRAINT fk_tblVoucherKhachHang_tblGiaoDichDoiDiem FOREIGN KEY (tblGiaoDichDoiDiemId) REFERENCES tblGiaoDichDoiDiem (id)
);

-- hetHan = ngayTao + 300s (R5), do server ghi
CREATE TABLE tblHoaDon (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    ngayTao               DATETIME     NOT NULL,
    hetHan                DATETIME     NOT NULL,
    trangThai             VARCHAR(100) NOT NULL,
    tblKhachHangId        INT          NOT NULL,
    tblVoucherKhachHangId INT          NULL,
    CONSTRAINT ck_tblHoaDon_trangThai CHECK (trangThai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'THAT_BAI', 'DA_HUY', 'HET_HAN')),
    CONSTRAINT fk_tblHoaDon_tblKhachHang FOREIGN KEY (tblKhachHangId) REFERENCES tblKhachHang (tblThanhVienId),
    CONSTRAINT fk_tblHoaDon_tblVoucherKhachHang FOREIGN KEY (tblVoucherKhachHangId) REFERENCES tblVoucherKhachHang (id)
);

CREATE TABLE tblVe (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    giaVe          INT          NOT NULL,
    trangThaiVe    VARCHAR(100) NOT NULL,
    tblHoaDonId    INT          NOT NULL,
    tblSuatChieuId INT          NOT NULL,
    tblGheId       INT          NOT NULL,
    CONSTRAINT ck_tblVe_trangThaiVe CHECK (trangThaiVe IN ('DANG_GIU', 'DA_BAN', 'DA_HUY')),
    CONSTRAINT fk_tblVe_tblHoaDon FOREIGN KEY (tblHoaDonId) REFERENCES tblHoaDon (id),
    CONSTRAINT fk_tblVe_tblSuatChieu FOREIGN KEY (tblSuatChieuId) REFERENCES tblSuatChieu (id),
    CONSTRAINT fk_tblVe_tblGhe FOREIGN KEY (tblGheId) REFERENCES tblGhe (id)
);

CREATE INDEX idx_tblVe_suatChieu_ghe ON tblVe (tblSuatChieuId, tblGheId);
