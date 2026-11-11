-- Issue #27: sửa seed UTF-8 từng bị import qua phiên latin1, không nạp lại schema/data.
-- Sao lưu DB trước. Chỉ sửa khi ID và từng giá trị cũ khớp chính xác dạng lỗi của seed;
-- giữ nguyên chữ đã đúng/chỉnh sửa riêng, NULL, mật khẩu, điểm, tồn kho và giao dịch.
SET NAMES utf8mb4;
START TRANSACTION;

-- UTF-8 bytes → latin1 → utf8mb4 tái tạo chính xác giá trị lỗi để đối chiếu byte.
UPDATE tblThanhVien AS t
JOIN (
    SELECT 1 AS id, 'annguyen2002' AS username, 'Nguyễn Văn A' AS hoTen,
           '96 Trần Phú, Hà Đông, Hà Nội' AS diaChi
) AS s ON t.id = s.id AND t.username = s.username
SET t.hoTen = IF(CAST(t.hoTen AS BINARY) = CONVERT(CONVERT(CAST(s.hoTen AS BINARY) USING latin1) USING utf8mb4), s.hoTen, t.hoTen),
    t.diaChi = IF(CAST(t.diaChi AS BINARY) = CONVERT(CONVERT(CAST(s.diaChi AS BINARY) USING latin1) USING utf8mb4), s.diaChi, t.diaChi);

UPDATE tblPhim AS t
JOIN (
    SELECT 1 AS id, 'Mai' AS tenPhim, 'Tâm lý, Tình cảm' AS theLoai, 'Trấn Thành' AS daoDien,
           'Câu chuyện về Mai, người phụ nữ làm nghề mát-xa' AS moTa
    UNION ALL
    SELECT 2, 'Đào, Phở và Piano', 'Lịch sử, Chiến tranh', 'Phi Tiến Sơn',
           'Hà Nội những ngày cuối năm 1946'
    UNION ALL
    SELECT 3, 'Kung Fu Panda 4', 'Hoạt hình, Hài', 'Mike Mitchell',
           'Po tìm người kế vị Thần Long Đại Hiệp'
) AS s ON t.id = s.id
SET t.tenPhim = IF(CAST(t.tenPhim AS BINARY) = CONVERT(CONVERT(CAST(s.tenPhim AS BINARY) USING latin1) USING utf8mb4), s.tenPhim, t.tenPhim),
    t.theLoai = IF(CAST(t.theLoai AS BINARY) = CONVERT(CONVERT(CAST(s.theLoai AS BINARY) USING latin1) USING utf8mb4), s.theLoai, t.theLoai),
    t.daoDien = IF(CAST(t.daoDien AS BINARY) = CONVERT(CONVERT(CAST(s.daoDien AS BINARY) USING latin1) USING utf8mb4), s.daoDien, t.daoDien),
    t.moTa = IF(CAST(t.moTa AS BINARY) = CONVERT(CONVERT(CAST(s.moTa AS BINARY) USING latin1) USING utf8mb4), s.moTa, t.moTa);

UPDATE tblQuaTang AS t
JOIN (
    SELECT 1 AS id, 'Bắp rang bơ phô mai' AS tenQua, 'Bắp ngọt cỡ vừa phủ phô mai' AS moTa
    UNION ALL
    SELECT 2, 'Vé xem phim 2D Standard', 'Áp dụng xem phim 2D tất cả các ngày'
    UNION ALL
    SELECT 3, 'Combo (1 Bắp + 1 Nước)', '1 bắp vừa + 1 nước ngọt 22oz'
    UNION ALL
    SELECT 4, 'Voucher giảm giá 50.000đ', 'Giảm trực tiếp trên hóa đơn vé'
) AS s ON t.id = s.id
SET t.tenQua = IF(CAST(t.tenQua AS BINARY) = CONVERT(CONVERT(CAST(s.tenQua AS BINARY) USING latin1) USING utf8mb4), s.tenQua, t.tenQua),
    t.moTa = IF(CAST(t.moTa AS BINARY) = CONVERT(CONVERT(CAST(s.moTa AS BINARY) USING latin1) USING utf8mb4), s.moTa, t.moTa);

UPDATE tblPhongChieu AS t
JOIN (
    SELECT 1 AS id, 'Phòng 1' AS tenPhong
    UNION ALL
    SELECT 2, 'Phòng 2'
) AS s ON t.id = s.id
SET t.tenPhong = IF(CAST(t.tenPhong AS BINARY) = CONVERT(CONVERT(CAST(s.tenPhong AS BINARY) USING latin1) USING utf8mb4), s.tenPhong, t.tenPhong);

UPDATE tblLoaiGhe AS t
JOIN (
    SELECT 1 AS id, 'Ghế Thường' AS tenLoai
    UNION ALL
    SELECT 2, 'Ghế VIP'
    UNION ALL
    SELECT 3, 'Ghế Đôi Sweetbox'
) AS s ON t.id = s.id
SET t.tenLoai = IF(CAST(t.tenLoai AS BINARY) = CONVERT(CONVERT(CAST(s.tenLoai AS BINARY) USING latin1) USING utf8mb4), s.tenLoai, t.tenLoai);

COMMIT;
