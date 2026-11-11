# AGENTS.md — CineMan

Hướng dẫn cho mọi agent/dev trong repo này. `../bc.docx` (§1–§11) và `../phase/*.md` (R1–R12, 8 quyết định _fix)
là nguồn gốc nhưng nằm **ngoài repo, không có sẵn** — coi `ARCHITECTURE.md` §4–§5 là chuẩn.
Kiến trúc triển khai: `ARCHITECTURE.md` (cùng thư mục) — **đọc trước khi code**.

## 1. Dự án là gì

Hệ thống quản lý rạp chiếu phim — **module M1 (3 chức năng, không hơn)**:

1. Khách hàng đăng ký thành viên
2. Đặt mua vé trực tuyến
3. Đổi điểm thưởng lấy quà tặng/vé

Frontend **React + Vite + TypeScript** (12 trang `GD*.tsx`), Backend **Java Spring Boot REST** (3 controller + DAO),
DB **MySQL** (13 bảng, `tblGiaoDichDoiDiem` lưu lịch sử đổi quà). Không sửa `../bc.docx`.

## 2. Quy tắc không đàm phán

- **Giữ nguyên tên**: 13 lớp thực thể + 3 lớp con `QuaTang` (`VoucherGiamGia`, `VoucherVeTang`, `VoucherVatPham`),
  13 bảng `tbl*`, `tblXId` cho khóa ngoại, các `*DAO` + `GiaoDichDoiDiemDAO`, 3 controller nghiệp vụ,
  `HTTPClient.fetch()`, 12 trang `GD*.tsx`, chữ ký DAO theo `ARCHITECTURE.md` §5 (bản _fix).
  Đổi tên = phá đối chiếu sơ đồ — hỏi trước.
- **Không tự bịa nghiệp vụ**: chỉ 3 chức năng trên. Cổng thanh toán là ngoài — ta chỉ lưu kết quả vào `tblHoaDon`.
  `tongTien`, `hanSuDung` là **dẫn xuất, không lưu cột**. Mã hiển thị = tiền tố + `id`, trừ `maVoucher`.
- **R1–R12 là luật bắt buộc** (`ARCHITECTURE.md` §4): điểm 1/20.000đ trước voucher, tối đa 1 voucher/hóa đơn,
  miễn 1 vé rẻ nhất 2D, hóa đơn + vé `DANG_GIU` tạo lúc Tiếp tục, tối đa 8 ghế, Sweetbox theo cặp, giữ ghế 300s,
  bỏ thanh toán đến muộn, `doTuoi` chỉ hiển thị, giữ `GDChonKhungGio`, mốc 27/09/2026.
- **Giao dịch nguyên tử**: đổi điểm `thucHienDoiDiem` (**ghi phiếu trước → tạo voucher sau**) và
  `taoHoaDon(hd, dsVe)` phải là **một transaction** — lỗi bất kỳ bước nào rollback toàn bộ.
  `hetHan` chỉ nằm ở server (`tblHoaDon`), đồng hồ FE chỉ hiển thị.
- **Ràng buộc trùng `(suất, ghế)`**: kiểm tra ở tầng ứng dụng trong giao dịch `taoHoaDon` bằng lọc
  `trangThaiVe IN ('DANG_GIU','DA_BAN')` — **không thêm UNIQUE trơn, không thêm cột `conHieuLuc`**.
- **Frontend không chạm DB**: mọi dữ liệu qua `fetch()` → 3 `@RestController` → DAO. Entity là POJO, DAO dùng JdbcTemplate.
- **Mật khẩu**: băm BCrypt, không bao giờ lưu/log thô.
- **Diagrams**: thay đổi bản _fix phải sửa cả `diagrams/puml/` gốc, không sửa ảnh `out/` bằng tay.

## 3. Điểm chưa chốt — không đoán

Chỉ còn `hangThanhVien` (ngưỡng đổi hạng), cổng thanh toán thật (Q10 dùng mock), actor Cổng thanh toán (không thêm).
Khi gặp: ghi rõ chỗ cần quyết, không hard-code ngầm.

## 4. Cách làm việc

1. **Làm từng bước một** — xong bước được yêu cầu thì dừng, báo kết quả, chờ bước tiếp theo.
2. Khi cần sửa spec: sửa `ARCHITECTURE.md` trước, code theo sau; không sửa `../bc.docx`.
3. Mọi thay đổi bảng/lớp/DAO phải cập nhật `ARCHITECTURE.md` §3–§5 trong cùng commit.
4. Test bằng seed `ARCHITECTURE.md` §8 (PH01, SC03 27/09/2026, PT02 100 điểm, `VC-VIDU-12345`, Nguyễn Văn A 250 điểm).
5. Quan hệ giữa class/DAO/trang (ai gọi ai, thay đổi này chạm đâu): `graphify query "<câu hỏi>"` trước (đồ thị ở `graphify-out/`), rồi xác nhận bằng `grep`/`read`. Sửa code xong chạy `graphify update .` và commit `graphify-out/` cùng thay đổi — hook git của graphify không chạy trong worktree, CI `graph-fresh` chặn nếu đồ thị lệch.

## 5. Kiểm chứng

Chạy và build: `README.md`. Render sơ đồ từ `diagrams/puml/`, lệnh riêng từng định dạng vì cờ thứ hai đè cờ thứ nhất:
`plantuml -tsvg -charset UTF-8 -o ../out <file>.puml` rồi `plantuml -tpng -charset UTF-8 -o ../out <file>.puml`.

Kiểm chứng tối thiểu trước khi báo xong: `mvn test` xanh + chạy thử 1 trong 3 kịch bản chính trên dữ liệu seed
(đăng ký → đặt vé A3+B3 → đổi điểm PT02 → `VC-VIDU-12345` → miễn A3 → +10 điểm), kèm kiểm tra:
quá 8 ghế bị chặn, trùng `(suất, ghế)` bị chặn, đổi điểm không đủ điểm rollback, hết 300s hóa đơn `HET_HAN`.

Commit: đọc `scripts/README.md` trước khi checkpoint (commit theo scope, chỉ stage file của mình, chỉ coordinator commit tích hợp).
