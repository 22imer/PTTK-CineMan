# AGENTS.md — CineMan (deploy/)

Hướng dẫn cho mọi agent/dev trong `deploy/`. Nguồn nghiệp vụ: `../bc.docx` (§1–§11), `../phase/quy-uoc-phan-tich.md`
(R1–R12), `../phase/quy-uoc-thiet-ke.md`, `../phase/nhan-xet-thiet-ke.md` §3 (8 quyết định _fix).
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

## 5. Lệnh làm việc

```bash
# Cài đặt toàn bộ
docker compose up --build          # mysql:3306 / backend:8080 / frontend:5173

# Backend (từ deploy/backend)
mvn spring-boot:run                # hoặc: mvn test

# Frontend (từ deploy/frontend)
npm install && npm run dev         # npm run build để build production

# Render lại sơ đồ (lệnh riêng từng định dạng vì cờ thứ hai đè cờ thứ nhất)
cd deploy/diagrams/puml
plantuml -tsvg -charset UTF-8 -o ../out <file>.puml
plantuml -tpng -charset UTF-8 -o ../out <file>.puml
```

Kiểm chứng tối thiểu trước khi báo xong: `mvn test` xanh + chạy thử 1 trong 3 kịch bản chính trên dữ liệu seed
(đăng ký → đặt vé A3+B3 → đổi điểm PT02 → `VC-VIDU-12345` → miễn A3 → +10 điểm), kèm kiểm tra:
quá 8 ghế bị chặn, trùng `(suất, ghế)` bị chặn, đổi điểm không đủ điểm rollback, hết 300s hóa đơn `HET_HAN`.

## 6. Checkpoint commit cho agent (issue #11)

- **Commit theo scope, đừng dồn change.** Xong một đơn vị mạch lạc (một issue/phase nhỏ) thì checkpoint ngay.
  Chỉ stage đúng file của mình, không `git add -A`, không stage `UI.md` hay metadata IDE (`.project`,
  `.classpath`, `.settings/`) khi không thuộc thay đổi của mình.
- **Kiểm chứng trước, commit sau.** Chạy gate liên quan (`mvn test`, `npm run build`, kịch bản smoke §5) rồi mới
  checkpoint. Hook chỉ nhắc/chặn vì file còn dirty; nó **không chạy test**. Không được nói "test xanh" nếu chưa chạy.
- **Cây dùng chung:** subagent sửa xong thì bàn giao file + kết quả kiểm chứng cho coordinator; **chỉ coordinator**
  chạy integration commit để tránh tranh chấp index. Không tự commit file của agent/người dùng khác.
- **Cách gọi:**

  ```bash
  # Slash command: để message trần (không bọc quote); quote chỉ dành cho đường dẫn file.
  /checkpoint feat(auth): mô tả ngắn (#7) -- backend/src/main/java/com/cineman/controller/DangKyController.java
  # hoặc gọi trực tiếp (đây là shell nên message vẫn phải quote):
  bash scripts/agent-commit.sh "feat(auth): mô tả ngắn (#7)" -- backend/src/main/java/com/cineman/controller/DangKyController.java
  ```

  Message của `/checkpoint` là **toàn bộ text thô** trước dấu `--` độc lập **cuối cùng**, giữ nguyên khoảng trắng
  **và cả dấu quote** vì slash command không shell-tokenize. Đừng bọc message trong `"..."`/`'...'` — dấu quote sẽ
  lọt vào commit message; chỉ quote đường dẫn file sau `--` khi tên có khoảng trắng/ký tự đặc biệt, ví dụ
  `-- "backend/src/main/resources/application.properties"`. Khi gọi trực tiếp `bash scripts/agent-commit.sh`,
  message là đối số shell nên vẫn quote như bình thường.
  Mỗi token sau `--` phải là một file cụ thể trong repo: không dùng `.`, thư mục, wildcard, pathspec magic,
  path tuyệt đối/ngoài repo hoặc `.git`. Script từ chối index
  đang staged, merge/rebase/conflict; stage đúng file được liệt kê, `git diff --cached --check`, commit local;
  không push/reset/clean. Không có thay đổi thì exit 0 và không tạo commit rỗng. Lỗi commit giữ nguyên file đã stage.
- **Hook native** `.omp/hooks/post/commit-checkpoint.ts`: sau `edit`/`write` thành công, `tool_result` của hook nhắc
  checkpoint và nhớ file local trong phiên. Khi phiên chính kết thúc (`session_stop`), hook chặn nếu các file đó vẫn
  dirty; file không quan sát được thì không bị chặn. File do subagent `edit`/`write` **vẫn được `tool_result` ghi
  nhận**, nhưng `session_stop` không chạy cho subagent nên không có gì chặn — coordinator phải chạy integration
  commit. File do `eval`/`bash` ghi hoặc thay đổi ngoài phiên hoàn toàn không được quan sát; các trường hợp đó vẫn
  phải chạy `/checkpoint` thủ công. Hook không tự commit.
- **Reload:** OMP discover `.omp/hooks/pre|post/*.ts` và đăng ký `/checkpoint` lúc nạp phiên. Sau khi thêm/sửa hook,
  khởi động lại phiên omp (hoặc reload phiên) rồi kiểm tra `/checkpoint` có trong danh sách command. Không cần
  cấu hình global/settings.
