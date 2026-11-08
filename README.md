# CineMan — Quản lý rạp chiếu phim

Đồ án Phân tích và thiết kế hệ thống, triển khai **module M1** với ba chức năng:

1. Khách hàng đăng ký thành viên.
2. Đặt mua vé trực tuyến.
3. Đổi điểm thưởng lấy quà tặng hoặc vé phim.

Thanh toán hiện dùng **mock**, chưa kết nối cổng thanh toán thật. Các màn quản trị, quản lý nhân viên và nghiệp vụ ngoài M1 không thuộc phạm vi triển khai.

## Công nghệ và kiến trúc

| Thành phần | Công nghệ | Vai trò |
| --- | --- | --- |
| Frontend | React, TypeScript, Vite | Giao diện và gọi API qua `HTTPClient.fetch()` |
| Backend | Java 21, Spring Boot, JdbcTemplate | REST API, xử lý nghiệp vụ và giao dịch |
| Cơ sở dữ liệu | MySQL 8.4 | 13 bảng nghiệp vụ, 14 khóa ngoại |
| Kiểm thử backend | JUnit, MockMvc, H2 chế độ MySQL | Kiểm thử schema, tài khoản, đặt vé và đổi điểm |

Frontend chỉ truy cập dữ liệu qua API; entity là POJO, không dùng JPA. Phiên đăng nhập do server quản lý bằng `HttpSession`; mật khẩu được băm BCrypt.

Chi tiết hợp đồng API, mô hình dữ liệu và quy tắc R1–R12 nằm trong [ARCHITECTURE.md](ARCHITECTURE.md).

## Chạy nhanh bằng Docker Compose

**Yêu cầu:** Git, Docker Engine hoặc Docker Desktop đang chạy, Docker Compose; các cổng bên dưới chưa bị chiếm. Không cần cài Java, Node.js hoặc MySQL trên máy nếu chạy toàn bộ bằng Docker.

```bash
git clone https://github.com/22imer/PTTK-CineMan.git
cd PTTK-CineMan
docker compose up --build
```

Chờ MySQL khởi tạo schema/seed và backend khởi động, sau đó mở ứng dụng:

| Dịch vụ | Địa chỉ |
| --- | --- |
| Giao diện | http://localhost:5173 |
| Backend REST | http://localhost:8080/api/phim |
| MySQL | `localhost:3306`, database `cineman` |

Frontend chạy Vite dev server; `/api` được proxy tới backend. Đây là cấu hình phát triển/demo, không phải cấu hình triển khai production.

```bash
# Xem log khi ứng dụng chưa sẵn sàng
docker compose logs -f backend frontend

# Dừng container nhưng giữ nguyên container/dữ liệu hiện có
docker compose stop

# Khởi động lại các container đã dừng
docker compose start
```

> **Dữ liệu demo:** Compose không cấu hình volume dữ liệu bền vững cho MySQL. `docker compose down` rồi `docker compose up --build` tạo DB mới và nạp lại seed. Không dùng cấu hình này để lưu dữ liệu quan trọng. Tài khoản DB và tài khoản mẫu bên dưới chỉ dành cho môi trường local/demo.

## Tài khoản và kịch bản mẫu

| Dữ liệu | Giá trị |
| --- | --- |
| Tên đăng nhập | `annguyen2002` |
| Mật khẩu demo | `Matkhau@123` |
| Khách hàng | Nguyễn Văn A, 250 điểm |
| Phim / suất chiếu | PH01 — Mai / SC03, ngày 27/09/2026, 19:00–21:11, Phòng 1, 2D |
| Ghế mẫu | A3: 90.000đ; B3: 120.000đ |
| Quà đổi điểm | PT02: vé phim, 100 điểm |
| Voucher có sẵn | `VC-VIDU-12345`: miễn một vé rẻ nhất của suất 2D |

Mốc suất chiếu được cố định theo báo cáo, không phải lịch chiếu cập nhật theo ngày hiện tại. Voucher seed dùng ngày nạp dữ liệu và có hạn 30 ngày; voucher phát sinh khi đổi điểm có mã riêng, không dùng lại mã seed.

### Chạy thử trên DB seed mới

1. Đăng nhập bằng tài khoản mẫu.
2. Vào đổi điểm thưởng, chọn PT02 và xác nhận: số điểm còn lại là **150**.
3. Về trang khách hàng, đặt vé phim Mai, ngày 27/09/2026, khung giờ tương ứng SC03; chọn A3 và B3 rồi bấm **Tiếp tục**.
4. Kiểm tra tổng tiền trước giảm là **210.000đ**; áp voucher seed `VC-VIDU-12345` để miễn A3, còn **120.000đ**.
5. Hoàn tất thanh toán mock thành công: hai vé được bán, cộng **10 điểm** tính trên tiền vé trước voucher; tài khoản còn **160 điểm**.

Để thử đăng ký thành viên, dùng username, email và số điện thoại chưa tồn tại. Tài khoản đăng ký mới bắt đầu với **0 điểm**, không phải 250 điểm như tài khoản seed.

### Quy tắc cần lưu ý

- Tối đa 8 ghế mỗi hóa đơn; Sweetbox phải chọn theo cặp.
- Hóa đơn và vé giữ chỗ được tạo khi bấm **Tiếp tục** ở màn chọn ghế; hạn giữ chỗ là 300 giây, do server quyết định.
- Không bán/giữ trùng một ghế trong cùng suất chiếu; thanh toán đến sau hạn không được ghi nhận.
- Tối đa một voucher mỗi hóa đơn. Voucher vé phim chỉ áp suất 2D; voucher vật phẩm nhận tại quầy, không trừ tiền vé.
- Thanh toán mock thất bại cho phép thử lại trong hạn giữ chỗ; không gia hạn đồng hồ.
- Đổi điểm và tạo hóa đơn/vé là các giao dịch nguyên tử: lỗi một bước phải rollback toàn bộ.

### Xem catalogue giao diện

Mở http://localhost:5173/?ui=VE-01 để chọn màn hình và trạng thái minh họa. Catalogue không tạo hóa đơn, giữ ghế, thanh toán hoặc đổi điểm; QR mẫu không có giá trị sử dụng. **Riêng biểu mẫu đăng ký/đăng nhập trong catalogue vẫn gọi API tài khoản thật.**

## Chạy từng thành phần trên máy

**Yêu cầu:** JDK 21, Maven 3.9+, MySQL 8.4, Node.js thỏa `^20.19.0 || >=22.12.0` và npm. Dockerfile frontend dùng Node 24; nên dùng cùng phiên bản để thống nhất môi trường.

### 1. Chuẩn bị MySQL

Tạo database `cineman` dùng `utf8mb4` và tài khoản `cineman` có quyền trên database đó. Cấu hình local mặc định dùng mật khẩu DB `cineman`; nếu dùng thông tin khác, đặt biến môi trường ở bước tiếp theo.

**Cảnh báo: `schema.sql` xóa rồi tạo lại các bảng. Chỉ chạy trên database phát triển được phép mất dữ liệu.** Từ gốc repo:

```bash
mysql -h 127.0.0.1 -u cineman -p cineman < backend/src/main/resources/schema.sql
mysql -h 127.0.0.1 -u cineman -p cineman < backend/src/main/resources/data.sql
```

Backend không tự nạp schema/seed khi chạy bình thường (`spring.sql.init.mode=never`).

### 2. Chạy backend

Trong một terminal riêng:

```bash
cd backend
mvn spring-boot:run
```

| Biến môi trường | Mặc định / mục đích |
| --- | --- |
| `DB_URL` | JDBC MySQL tới `localhost:3306/cineman`; xem chuỗi đầy đủ trong [application.properties](backend/src/main/resources/application.properties) |
| `DB_USER` | `cineman` |
| `DB_PASSWORD` | `cineman` |

### 3. Chạy frontend

Trong terminal khác, bắt đầu từ gốc repo:

```bash
cd frontend
npm ci
npm run dev
```

Mở http://localhost:5173. Proxy API mặc định trỏ tới `http://localhost:8080`; đặt `API_PROXY_TARGET` khi backend chạy ở địa chỉ khác. Nên truy cập ứng dụng qua frontend để `/api` và cookie phiên cùng origin.

## Kiểm thử và build

Chạy từ gốc repo:

```bash
# Backend: H2 chế độ MySQL, tự nạp schema và seed; không cần MySQL đang chạy
(cd backend && mvn test)

# Frontend: kiểm tra kiểu TypeScript và build production vào frontend/dist/
(cd frontend && npm ci && npm run build)
```

Bộ test backend kiểm tra đăng ký/đăng nhập, ràng buộc schema/seed, đặt vé và đổi điểm; bao gồm tranh chấp giữ ghế, hơn 8 ghế, Sweetbox, voucher, rollback, hết hạn và thanh toán lặp không cộng điểm hai lần. H2 không thay thế việc kiểm chứng trên MySQL; cách chạy `M1IntegrationTest` với **DB MySQL test riêng, được phép xóa dữ liệu** nằm trong [ARCHITECTURE.md §11](ARCHITECTURE.md#11-chạy--build).

## Cấu trúc và tài liệu

| Đường dẫn | Nội dung |
| --- | --- |
| [frontend/](frontend/) | Giao diện React, API client, kiểu dữ liệu và catalogue giao diện |
| [backend/](backend/) | Spring Boot REST, DAO, entity, schema/seed và kiểm thử |
| [docker-compose.yml](docker-compose.yml) | Khởi chạy MySQL, backend và frontend cho demo |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Nguồn kiến trúc, API, DAO, R1–R12 và hướng dẫn chạy chi tiết |
| [specs.md](specs.md) | Đặc tả và kịch bản phân tích/thiết kế M1 |
| [GLOSSARY.md](GLOSSARY.md) | Thuật ngữ nghiệp vụ chuẩn |
| [AGENTS.md](AGENTS.md) | Quy ước làm việc và checkpoint commit |
| [graphify-out/GRAPH_REPORT.md](graphify-out/GRAPH_REPORT.md) | Báo cáo knowledge graph và giới hạn trích xuất |
| [graphify-out/graph.json](graphify-out/graph.json) | Dữ liệu graph để truy vấn |
| [graphify-out/graph.html](graphify-out/graph.html) | Graph tương tác; tải về và mở bằng trình duyệt, cần mạng để tải thư viện hiển thị |

Knowledge graph là bản đồ hỗ trợ điều hướng, không phải call graph đã xác minh đầy đủ. Xem cảnh báo về liên kết suy luận, endpoint chưa phân giải và giới hạn kiểm chứng hiển thị trong báo cáo. Thay đổi mã nguồn/tài liệu có thể làm graph cũ; cần dựng lại khi dùng để phân tích phiên bản mới.

## Phạm vi còn mở và đóng góp

- Chưa chốt ngưỡng chuyển `hangThanhVien`; không tự suy diễn từ tổng điểm.
- Cổng thanh toán thật chưa được tích hợp; các luồng thanh toán hiện là mock.
- Đọc [ARCHITECTURE.md](ARCHITECTURE.md) và [AGENTS.md](AGENTS.md) trước khi sửa; giữ tên lớp, bảng và DAO để đối chiếu thiết kế.
- PR tính năng/tài liệu nhắm vào `dev`; chỉ nhánh `dev` được mở PR vào `main`, theo [workflow branch-flow](.github/workflows/branch-flow.yml).
