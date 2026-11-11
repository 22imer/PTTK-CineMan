# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Khách hàng của một rạp chiếu phim, dùng trình duyệt máy tính hoặc điện thoại để đăng ký thành viên, đặt vé trực tuyến và đổi điểm thưởng. Họ làm việc theo từng bước ngắn, có thời hạn: giữ ghế 300 giây, nên cần nhìn rõ ghế, giá và đồng hồ.

## Product Purpose

CineMan M1 có ba chức năng và không hơn:

1. Khách hàng đăng ký thành viên.
2. Đặt mua vé trực tuyến (chọn phim, ngày, khung giờ, suất chiếu, ghế, voucher, thanh toán, vé điện tử).
3. Đổi điểm thưởng lấy quà tặng hoặc vé.

Thành công là khách hoàn tất một trong ba việc mà không nhầm ghế, không nhầm số tiền, không mất điểm khi có lỗi.

## Positioning

Giao diện là một sảnh rạp chiếu phim: mỗi bước trong luồng đặt vé nằm ở một khu của sảnh (bảng suất chiếu, khán phòng, quầy bắp nước). Mốc dữ liệu minh họa là 27/09/2026.

## Constraints

- Giữ nguyên tên lớp, `id`, props, logic và mọi chữ tiếng Việt (`AGENTS.md` §2). Đổi chữ cần được duyệt trước.
- Quy tắc nghiệp vụ R1–R12 nằm trong `ARCHITECTURE.md` §4; giao diện chỉ hiển thị kết quả server trả về.
- Tiếng Việt có dấu phải đọc rõ ở mọi cỡ chữ.
- Hạn chuyển động: tôn trọng `prefers-reduced-motion`; focus bàn phím luôn thấy.
- Font tự host qua npm, chạy được trong Docker offline.

## Brand commitments

Thế giới thị giác do người dùng chỉ định nguyên văn: pixel art 16-bit của sảnh rạp phim cổ điển, đối xứng chính diện, vòm neon art deco bậc thang màu cyan, bảng hiệu marquee khối pixel ánh vàng, bảng màu ấm (vàng bơ bắp rang, nhung burgundy, gỗ tối), bảng suất chiếu có ô số nhiều màu, quầy bắp nước có máy bắp rang, dithering pixel, đường nét pixel sắc, không khí lo-fi thập niên 90.

## Open decisions

- `hangThanhVien` (ngưỡng đổi hạng) và cổng thanh toán thật: chưa chốt (`AGENTS.md` §3), giao diện không hiển thị.
