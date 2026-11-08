-- CineMan M1 — seed theo ARCHITECTURE.md §8 (mốc 27/09/2026, R12). Chạy ngay sau schema.sql (bảng rỗng).
-- Không ghi id: AUTO_INCREMENT cấp 1..n theo thứ tự INSERT; chú thích ghi id + mã hiển thị để đối chiếu.

-- id 1 — Nguyễn Văn A, đăng nhập annguyen2002 / Matkhau@123 (BCrypt)
INSERT INTO tblThanhVien (username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro) VALUES
    ('annguyen2002', '$2a$10$IWSpxMat9ys9tW9nZIQijeO0jmdAA0Jj9oIXHnIoadQGXuoYRM4BW', 'Nguyễn Văn A', '2002-08-15', 'Nam',
     '96 Trần Phú, Hà Đông, Hà Nội', 'an.nguyen@gmail.com', '0912345678', 'KHACH_HANG');

-- 250 điểm: trạng thái đầu kịch bản đổi điểm (250 → 150) rồi đặt vé (150 → 160). hangThanhVien chưa chốt → NULL.
INSERT INTO tblKhachHang (tblThanhVienId, ngayDangKy, tongDiem, hangThanhVien) VALUES
    (1, '2026-01-15', 250, NULL);

-- PH01..PH03
INSERT INTO tblPhim (tenPhim, theLoai, daoDien, thoiLuong, doTuoi, moTa, ngayKhoiChieu, ngayKetThuc) VALUES
    ('Mai', 'Tâm lý, Tình cảm', 'Trấn Thành', 131, 'C18', 'Câu chuyện về Mai, người phụ nữ làm nghề mát-xa', '2026-09-01', '2026-10-31'),
    ('Đào, Phở và Piano', 'Lịch sử, Chiến tranh', 'Phi Tiến Sơn', 100, 'C13', 'Hà Nội những ngày cuối năm 1946', '2026-09-01', '2026-10-31'),
    ('Kung Fu Panda 4', 'Hoạt hình, Hài', 'Mike Mitchell', 94, 'P', 'Po tìm người kế vị Thần Long Đại Hiệp', '2026-09-01', '2026-10-31');

-- id 1..3
INSERT INTO tblKhungGio (gioBatDau, gioKetThuc) VALUES
    ('09:00:00', '11:11:00'),
    ('14:00:00', '16:11:00'),
    ('19:00:00', '21:11:00');

-- id 1 Phòng 1 (2D), id 2 Phòng 2 (3D — để kiểm tra voucher vé phim chỉ áp suất 2D, R3)
INSERT INTO tblPhongChieu (tenPhong, loaiPhong, sucChua) VALUES
    ('Phòng 1', '2D', 40),
    ('Phòng 2', '3D', 40);

-- id 1 Thường 0đ, id 2 VIP 30.000đ, id 3 Đôi-Sweetbox (phụ thu mẫu, bc chưa nêu số)
INSERT INTO tblLoaiGhe (tenLoai, phuThu) VALUES
    ('Ghế Thường', 0),
    ('Ghế VIP', 30000),
    ('Ghế Đôi Sweetbox', 50000);

-- SC01..SC06; SC03 = Mai 27/09/2026 19:00–21:11 Phòng 1 (2D) 90.000đ
INSERT INTO tblSuatChieu (ngayChieu, giaCoSo, trangThai, tblPhimId, tblKhungGioId, tblPhongChieuId) VALUES
    ('2026-09-27', 90000, 'MO_BAN', 1, 1, 1),
    ('2026-09-27', 90000, 'MO_BAN', 1, 2, 1),
    ('2026-09-27', 90000, 'MO_BAN', 1, 3, 1),
    ('2026-09-27', 110000, 'MO_BAN', 3, 2, 2),
    ('2026-09-28', 85000, 'MO_BAN', 2, 3, 1),
    ('2026-09-28', 90000, 'MO_BAN', 1, 1, 1);

-- Mỗi phòng 40 ghế: A Thường, B–C VIP, D Thường, E Sweetbox (cặp 1-2, 3-4, 5-6, 7-8; R6).
-- Phòng 1: id 1..40 (A3 = id 3 → 90.000đ ở SC03, B3 = id 11 → 120.000đ); Phòng 2: id 41..80.
INSERT INTO tblGhe (soGhe, hangGhe, tblPhongChieuId, tblLoaiGheId) VALUES
    ('1', 'A', 1, 1),
    ('2', 'A', 1, 1),
    ('3', 'A', 1, 1),
    ('4', 'A', 1, 1),
    ('5', 'A', 1, 1),
    ('6', 'A', 1, 1),
    ('7', 'A', 1, 1),
    ('8', 'A', 1, 1),
    ('1', 'B', 1, 2),
    ('2', 'B', 1, 2),
    ('3', 'B', 1, 2),
    ('4', 'B', 1, 2),
    ('5', 'B', 1, 2),
    ('6', 'B', 1, 2),
    ('7', 'B', 1, 2),
    ('8', 'B', 1, 2),
    ('1', 'C', 1, 2),
    ('2', 'C', 1, 2),
    ('3', 'C', 1, 2),
    ('4', 'C', 1, 2),
    ('5', 'C', 1, 2),
    ('6', 'C', 1, 2),
    ('7', 'C', 1, 2),
    ('8', 'C', 1, 2),
    ('1', 'D', 1, 1),
    ('2', 'D', 1, 1),
    ('3', 'D', 1, 1),
    ('4', 'D', 1, 1),
    ('5', 'D', 1, 1),
    ('6', 'D', 1, 1),
    ('7', 'D', 1, 1),
    ('8', 'D', 1, 1),
    ('1', 'E', 1, 3),
    ('2', 'E', 1, 3),
    ('3', 'E', 1, 3),
    ('4', 'E', 1, 3),
    ('5', 'E', 1, 3),
    ('6', 'E', 1, 3),
    ('7', 'E', 1, 3),
    ('8', 'E', 1, 3),
    ('1', 'A', 2, 1),
    ('2', 'A', 2, 1),
    ('3', 'A', 2, 1),
    ('4', 'A', 2, 1),
    ('5', 'A', 2, 1),
    ('6', 'A', 2, 1),
    ('7', 'A', 2, 1),
    ('8', 'A', 2, 1),
    ('1', 'B', 2, 2),
    ('2', 'B', 2, 2),
    ('3', 'B', 2, 2),
    ('4', 'B', 2, 2),
    ('5', 'B', 2, 2),
    ('6', 'B', 2, 2),
    ('7', 'B', 2, 2),
    ('8', 'B', 2, 2),
    ('1', 'C', 2, 2),
    ('2', 'C', 2, 2),
    ('3', 'C', 2, 2),
    ('4', 'C', 2, 2),
    ('5', 'C', 2, 2),
    ('6', 'C', 2, 2),
    ('7', 'C', 2, 2),
    ('8', 'C', 2, 2),
    ('1', 'D', 2, 1),
    ('2', 'D', 2, 1),
    ('3', 'D', 2, 1),
    ('4', 'D', 2, 1),
    ('5', 'D', 2, 1),
    ('6', 'D', 2, 1),
    ('7', 'D', 2, 1),
    ('8', 'D', 2, 1),
    ('1', 'E', 2, 3),
    ('2', 'E', 2, 3),
    ('3', 'E', 2, 3),
    ('4', 'E', 2, 3),
    ('5', 'E', 2, 3),
    ('6', 'E', 2, 3),
    ('7', 'E', 2, 3),
    ('8', 'E', 2, 3);

-- PT01..PT04; PT04 hết hàng (tồn kho 0)
INSERT INTO tblQuaTang (tenQua, loaiQua, soDiem, soLuong, moTa, giaTriGiam) VALUES
    ('Bắp rang bơ phô mai', 'BAP_NUOC', 50, 120, 'Bắp ngọt cỡ vừa phủ phô mai', NULL),
    ('Vé xem phim 2D Standard', 'VE_PHIM', 100, 45, 'Áp dụng xem phim 2D tất cả các ngày', NULL),
    ('Combo (1 Bắp + 1 Nước)', 'BAP_NUOC', 80, 60, '1 bắp vừa + 1 nước ngọt 22oz', NULL),
    ('Voucher giảm giá 50.000đ', 'GIAM_GIA', 60, 0, 'Giảm trực tiếp trên hóa đơn vé', 50000);

-- PDT-001 + VC-VIDU-12345 (vé phim, từ PT02): voucher cố định cho kịch bản đặt vé (miễn A3).
-- Ngày = lúc nạp seed để voucher còn hạn 30 ngày (R2); mốc 25/09/2026 đã quá hạn so với hiện tại.
INSERT INTO tblGiaoDichDoiDiem (thoiGian, soDiemTru, diemConLai, tblKhachHangId, tblQuaTangId) VALUES
    (CURRENT_TIMESTAMP, 100, 150, 1, 2);

INSERT INTO tblVoucherKhachHang (maVoucher, ngayNhan, ngaySuDung, trangThai, tblGiaoDichDoiDiemId) VALUES
    ('VC-VIDU-12345', CURRENT_DATE, NULL, 'CHUA_SU_DUNG', 1);
