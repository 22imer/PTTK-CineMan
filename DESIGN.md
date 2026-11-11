# Design

Sảnh rạp chiếu phim pixel art 16-bit, thập niên 90. Khung ngoài là gỗ tối và nhung burgundy; nội dung nằm trên panel bơ-kem; cyan neon chỉ dùng cho hành động chính, ghế đang chọn và focus. Mọi token nằm ở `:root` trong `frontend/src/styles.css`.

## Color

| Token | Hex | Vai trò |
|---|---|---|
| `--wood-0` | `#160b08` | nền trang, viền ngoài, bóng cứng |
| `--wood-1` / `--wood-2` / `--wood-3` | `#2a170f` / `#4a2b1a` / `#7a4a2a` | gỗ: đầu bảng, ghế không khả dụng, viền panel |
| `--velvet` / `--velvet-hi` | `#6b1426` / `#962339` | bảng hiệu, tiêu đề, ghế trống |
| `--gold` / `--gold-deep` | `#f2b84b` / `#8c5a14` | viền marquee, bóng chữ |
| `--butter` | `#ffd966` | chữ trên nền tối (wordmark, đầu bảng) |
| `--cream` / `--cream-2` | `#fff1cf` / `#f3d99b` | nền panel, nút phụ |
| `--ink` / `--muted` | `#2a1710` / `#6a4a36` | chữ trên panel (15,2:1 và 7,1:1) |
| `--cyan` / `--cyan-deep` | `#29f0ff` / `#0b6674` | neon: nút chính, ghế chọn, focus |
| `--tt-1…6` | đỏ, teal, vàng, xanh lá, cam, tím | ô số cột TT, quay vòng theo hàng |

Chiến lược màu: Full palette. Cyan không xuất hiện ngoài hành động chính, lựa chọn hiện tại và focus.

## Typography

- Display: Handjet 700 (tự host `@fontsource/handjet`, có subset `vietnamese`) cho wordmark, `h1`–`h3`, nút, đầu bảng, nhãn ghế, đồng hồ giữ ghế, mã voucher. Cỡ 19–48px.
- Body: stack hệ thống (`Segoe UI`, `system-ui`), 15px/1.6, cho form, bảng, đoạn văn.
- Không in hoa toàn bộ, không số thứ tự trang trí; cột TT là số thật.

## Shape and depth

- Không bo góc. Viền bậc thang tạo bằng bốn `box-shadow` 3px lệch trục (`--px: 3px`), bóng đổ là khối cứng 6–10px.
- Nút nhấn xuống 3px khi `:active`. Dithering bằng `repeating-conic-gradient` 6–8px (nền trang, dải nhung, ghế khóa, backdrop dialog).
- Focus: vòng cyan 3px trong, vòng gỗ tối 3px ngoài.

## Components

- **Bảng hiệu (`.site-header`)**: nền nhung dither, viền vàng, hai dãy bóng đèn chạy xen kẽ (CSS, chỉ khi không `prefers-reduced-motion`).
- **Panel (`.ui-page`, `.auth-frame`)**: nền kem, viền gỗ hai lớp, dải nhung 12px mép trên, đường chấm vàng dưới tiêu đề.
- **Nút**: `.btn` kem, `.btn--primary` cyan, `.btn--ghost` viền nhung. Trong header nút phụ viền vàng.
- **Bảng suất chiếu (`.data-table`)**: đầu bảng gỗ tối chữ bơ; cột TT là ô số màu `--tt-*`; hàng chẵn nhạt, hover vàng nhạt.
- **Khán phòng (`.seat-map`)**: nền gỗ dither, thanh MÀN HÌNH viền cyan, ghế nhung burgundy, ghế chọn cyan, ghế khóa dither nâu; cặp Sweetbox nằm trong khung vàng.
- **Vé và voucher**: viền gạch 3px, nền bơ nhạt (`.ticket-qr`, `.voucher-card`).
- **Tranh sảnh (`HallScene`)**: SVG 192×108 đối xứng, vòm neon cyan bậc thang, marquee CINEMAN, hai bảng số phòng, quầy bắp nước với máy bắp rang. Chỉ ở trang chủ (đã và chưa đăng nhập) và khung đăng nhập. Sinh bằng `frontend/scripts/gen-hall-scene.mjs`.

## Motion

Một khoảnh khắc: bóng đèn marquee nhấp nháy hai pha (header và tranh sảnh). Dialog đổi quà hiện theo bước `steps(4)`. `prefers-reduced-motion` tắt toàn bộ animation.
