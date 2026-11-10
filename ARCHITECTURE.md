# CineMan — ARCHITECTURE

Hệ thống quản lý rạp chiếu phim — Module M1 (đăng ký thành viên, đặt vé trực tuyến, đổi điểm thưởng).
Nguồn đặc tả: `../bc.docx` (§1–§11) + quyết định nhóm theo `../phase/quy-uoc-phan-tich.md` (R1–R12)
+ `../phase/quy-uoc-thiet-ke.md` + `../phase/nhan-xet-thiet-ke.md` §3 (8 quyết định _fix, 28/10/2026).
File này là shared understanding đã chốt (Q1–Q11) — implement đúng như mô tả, không tự thêm/bớt nghiệp vụ.

## 1. Quyết định đã chốt (Q1–Q11)

| # | Quyết định | Hệ quả |
| :--- | :--- | :--- |
| Q1 | Frontend **React + Vite + TypeScript** (thay Thymeleaf) | 12 trang `GD*.tsx`; bỏ Thymeleaf/JSP; gọi API qua `HTTPClient.fetch()` |
| Q2 | Backend **Java Spring Boot REST + DAO** | 3 `@RestController` nghiệp vụ (`DangKy`/`DatVe`/`DoiDiemController`) + DAO (JdbcTemplate), entity là POJO (không JPA) |
| Q3 | **Thêm `GiaoDichDoiDiem`** (phiếu PDT-001) | `VoucherKhachHang` chứa `giaoDichDoiDiem`; bỏ `khachHang/quaTang` trên voucher; **ghi phiếu trước → tạo voucher sau**, 1 transaction `thucHienDoiDiem` |
| Q4 | Giữ **3 lớp con `QuaTang`** | 1 bảng `tblQuaTang` chứa `loaiQua` + `giaTriGiam` (NULL với vé/bắp nước); 3 lớp con `VoucherGiamGia`/`VoucherVeTang`/`VoucherVatPham` vẫn bắt buộc |
| Q5–Q6 | Chốt **R1–R12** | Xem §4; không còn mục "chưa chốt" |
| Q7 | **13 bảng nghiệp vụ + 1 bảng phiên**, **15 khóa ngoại** | Thêm `tblPhienDangNhap` theo thiết kế JWT được xác nhận 09/11/2026; giữ `tblXId` và kiểm tra trùng ghế ở app-level |
| Q8 | **12 trang `*.tsx`** | `DangNhap` tách riêng; giữ thuộc tính `in/out/sub`; session ẩn `kh, phim, sc, hoaDon…` |
| Q9 | Backend **Java Spring Boot** | Chữ ký DAO theo `_fix` (§5); mật khẩu băm BCrypt |
| Q10 | **Phiên JWT có trạng thái thu hồi + cổng thanh toán mock** | Access Bearer 15 phút, refresh/phiên tối đa 7 ngày; bỏ `HttpSession`; cổng thanh toán vẫn mock |
| Q11 | Diagrams `_fix` + seed + docker | Tên/chữ ký theo bản _fix; seed mốc 27/09/2026; `docker-compose.yml` ở gốc repo |

## 2. Tổng quan

```text
Browser (React TSX) ── fetch (HTTPClient) ──▶ Spring REST ──▶ DAO (JdbcTemplate) ──▶ MySQL
                                                (3 Controller)    (Connection chung)
```

| Tầng | Công nghệ | Vai trò |
| :--- | :--- | :--- |
| Frontend | TypeScript + React + Vite | 12 trang `GD*.tsx` (§6), gọi API qua `HTTPClient.fetch()` |
| Backend | Java Spring Boot (REST) | 3 controller nghiệp vụ → DAO |
| Database | MySQL | 13 bảng nghiệp vụ + `tblPhienDangNhap` kỹ thuật, tổng 14 bảng / 15 khóa ngoại |

## 3. Mô hình dữ liệu (MySQL, 14 bảng, 15 khóa ngoại)

| Bảng | Cột (không khóa ngoại) | Ghi chú |
| :--- | :--- | :--- |
| `tblThanhVien` | `id, username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro` | UNIQUE `username`, `email`, `sdt`; `password` băm BCrypt |
| `tblKhachHang` | `tblThanhVienId (PK=FK), ngayDangKy, tongDiem, hangThanhVien` | Kế thừa ThanhVien (1-1) |
| `tblPhim` | `id, tenPhim, theLoai, daoDien, thoiLuong, doTuoi, moTa, ngayKhoiChieu, ngayKetThuc` | `doTuoi` chỉ hiển thị (R10) |
| `tblKhungGio` | `id, gioBatDau, gioKetThuc` | |
| `tblPhongChieu` | `id, tenPhong, loaiPhong, sucChua` | `loaiPhong` chứa `2D`/`3D`… |
| `tblSuatChieu` | `id, ngayChieu, giaCoSo, trangThai, tblPhimId, tblKhungGioId, tblPhongChieuId` | |
| `tblLoaiGhe` | `id, tenLoai, phuThu` | Thường 0đ / VIP 30.000đ / Đôi-Sweetbox |
| `tblGhe` | `id, soGhe, hangGhe, tblPhongChieuId, tblLoaiGheId` | Sweetbox = 2 bản ghi liền kề (R6) |
| `tblVe` | `id, giaVe, trangThaiVe, tblHoaDonId, tblSuatChieuId, tblGheId` | `trangThaiVe ∈ {DANG_GIU, DA_BAN, DA_HUY}`; `tblHoaDonId` NOT NULL (`1..8` vé/hóa đơn, R7) |
| `tblHoaDon` | `id, ngayTao, hetHan, trangThai, tblKhachHangId, tblVoucherKhachHangId` | `hetHan = ngayTao + 300s` (R5); `trangThai ∈ {CHO_THANH_TOAN, DA_THANH_TOAN, THAT_BAI, DA_HUY, HET_HAN}`; tổng tiền **dẫn xuất, không lưu cột** |
| `tblQuaTang` | `id, tenQua, loaiQua, soDiem, soLuong, giaTriGiam, moTa` | `loaiQua ∈ {GIAM_GIA, VE_PHIM, BAP_NUOC}`; `giaTriGiam` chỉ dùng cho quà `GIAM_GIA` (> 0), NULL với vé/bắp nước; 3 lớp con `VoucherGiamGia`/`VoucherVeTang`/`VoucherVatPham` vẫn giữ, phân biệt bằng `loaiQua` |
| `tblGiaoDichDoiDiem` | `id, thoiGian, soDiemTru, diemConLai, tblKhachHangId, tblQuaTangId` | Phiếu PDT-001; **chỉ lưu phiếu thành công**; không có `trangThai` |
| `tblVoucherKhachHang` | `id, maVoucher, ngayNhan, ngaySuDung, trangThai, tblGiaoDichDoiDiemId` | UNIQUE `maVoucher`; `trangThai ∈ {CHUA_SU_DUNG, DA_SU_DUNG}`; hạn = `ngayNhan + 30 ngày` (**không lưu cột**); chủ/quà qua `giaoDichDoiDiem` |
| `tblPhienDangNhap` | `id` (UUID), `refreshTokenHash`, `previousTokenHash` (nullable), `xoayLuc` (nullable), `ngayTao`, `hetHan`, `thuHoiLuc` (nullable), `tblKhachHangId` (FK) | Bảng kỹ thuật xác thực: POJO `PhienDangNhap`, chỉ hash refresh token; khóa hàng khi xoay/thu hồi; không chứa trạng thái đặt vé |
| | | 13 bảng nghiệp vụ = 12 bảng bc + `tblGiaoDichDoiDiem`; bảng phiên kỹ thuật nằm ngoài mô hình nghiệp vụ M1 |

> Quyết định 08/11/2026 ([issue #3](https://github.com/22imer/PTTK-CineMan/issues/3)): lịch sử đổi quà dùng
> `tblGiaoDichDoiDiem` hiện có — không thêm bảng lịch sử trùng lặp; `giaTriGiam` nằm chung `tblQuaTang`,
> bỏ bảng tách `tblVoucherGiamGia` (bảng này từng được thêm chỉ để ép đủ "14 bảng" — con số đếm sai).
> 3 lớp con `QuaTang` vẫn bắt buộc. Từ 09/11/2026 giữ nguyên 13 bảng nghiệp vụ / 14 khóa ngoại và thêm đúng một bảng phiên kỹ thuật / một khóa ngoại, tổng 14 bảng / 15 khóa ngoại.

Quy ước kiểu (thiết kế): tiền/điểm/số lượng = `int`; chuỗi/loại/trạng thái = `String`; ngày/giờ = `Date`
(CSDL tách `DATE`/`DATETIME`/`TIME`; Java dùng `LocalDate`/`LocalDateTime`/`LocalTime` tương ứng); tên khóa ngoại `tblXId` (`tbl` + tên bảng camelCase + `Id`).
Mã hiển thị = tiền tố + đầu số `id` (`PH01`, `SC03`, `HD001`, `PT02`, `PDT-001`), **không lưu cột** (trừ `maVoucher`).

Ràng buộc trùng `(suất, ghế)`: không hai `Ve` `DANG_GIU`/`DA_BAN` cùng (`tblSuatChieuId`, `tblGheId`)
— kiểm tra ở tầng ứng dụng trong giao dịch `taoHoaDon` bằng lọc `trangThaiVe IN ('DANG_GIU','DA_BAN')`,
**không** UNIQUE trơn (vé `DA_HUY` giữ cặp đó). Quan hệ `HoaDon–VoucherKhachHang`: `0..1–0..1` (R2).

## 4. Quy tắc nghiệp vụ R1–R12 (đã chốt)

| # | Quy tắc |
| :--- | :--- |
| R1 | Điểm = 1 điểm / 20.000đ tiền vé **trước** voucher (làm tròn xuống); 210.000đ → +10; chỉ cộng khi thanh toán thành công |
| R2 | Voucher hợp lệ: đúng chủ đăng nhập, `CHUA_SU_DUNG`, còn hạn 30 ngày, tối đa 1/hóa đơn, giảm ≤ tổng tiền. Áp chỉ kiểm tra; `DA_SU_DUNG` khi thanh toán thành công; kiểm tra lại lúc ghi nhận |
| R3 | `VoucherGiamGia.giaTriGiam` trừ cố định; `VoucherVeTang` (PT02) miễn 1 vé rẻ nhất, chỉ suất 2D (VD `VC-VIDU-12345` miễn A3 90.000đ → trả 120.000đ); `VoucherVatPham` bắp nước nhận tại quầy, không áp hóa đơn |
| R4 | Hóa đơn (`CHO_THANH_TOAN`) + vé (`DANG_GIU`) tạo khi bấm **Tiếp tục** ở chọn ghế; thanh toán thành công → `DA_THANH_TOAN` + vé `DA_BAN`; mock thất bại giữ `CHO_THANH_TOAN` để thử lại trong hạn (issue #1, story 35), Quay lại → `DA_HUY`, quá hạn → `HET_HAN`, vé hủy/qua hạn → `DA_HUY`; không có `PhienDatVe`/`GiuCho` |
| R5 | Giữ ghế 300 giây từ lúc tạo hóa đơn; hạn `hetHan` do server lưu, FE chỉ hiển thị; không gia hạn |
| R6 | Ghế đôi Sweetbox = 2 `Ghe` liền kề cùng loại, mỗi ghế 1 `Ve`; chọn theo cặp ở lớp điều khiển |
| R7 | Tối đa 8 ghế/hóa đơn: `HoaDon 1 — 1..8 Ve` |
| R8 | Đăng ký: bắt buộc, email/sđt đúng định dạng, mật khẩu ≥8 ký tự, nhập lại khớp; không ràng buộc tuổi |
| R9 | Bỏ thanh toán đến muộn (`CHO_HOAN_TIEN`, hoàn tiền): quá 300s → `HET_HAN`, không cho xác nhận nữa |
| R10 | `Phim.doTuoi`, `dienVien` chỉ hiển thị, không chặn theo tuổi |
| R11 | Giữ `GDChonKhungGio`: phim → ngày, khung giờ → suất chiếu → ghế |
| R12 | Mốc ví dụ 27/09/2026 (SC03), hóa đơn 25/09/2026 (`HD001`, `PDT-001`) |

Điểm quy đổi (R1) đặt vào `application.properties` (`points.per-amount=20000`), không hard-code rải rác.
`hangThanhVien`, `vaiTro`, `gioiTinh`, `SuatChieu.trangThai` giữ nguyên giá trị String như bc (chưa có ngưỡng → không suy diễn).

## 5. Backend — Controller + DAO (chữ ký _fix)

Lớp `DAO` là lớp trừu tượng dùng chung kết nối Spring (JdbcTemplate); các `…DAO` kế thừa. Entity là POJO.
Controller là Spring `@RestController` (3 controller nghiệp vụ). Frontend không chạm DB.

| DAO | Phương thức (chữ ký đầy đủ) |
| :--- | :--- |
| `ThanhVienDAO` | `kiemTraThongTin(kh : KhachHang) : boolean`, `kiemTraEmailSDT(email : String, sdt : String) : boolean`, `kiemTraDangNhap(ThanhVien tv) : boolean` |
| `KhachHangDAO` | `taoHoSo(kh : KhachHang) : boolean`, `kiemTraDiem(maKhachHang : String, diem : int) : boolean`, `truDiem(maKhachHang : String, diem : int) : boolean`, `congDiem(maKhachHang : String, diem : int) : boolean`, `layTheoId(id : int) : KhachHang`, `khoaTheoId(id : int) : KhachHang` (đọc/khóa hồ sơ không mật khẩu) |
| `PhimDAO` / `KhungGioDAO` | `laydsPhim() : List<Phim>`, `laydsKhungGio() : List<KhungGio>` |
| `SuatChieuDAO` | `laydsSuatChieu(idPhim : int, ngayChieu : Date, idKhungGio : int) : List<SuatChieu>`, `layTheoId(id : int) : SuatChieu`, `khoaTheoId(id : int) : SuatChieu` |
| `GheDAO` | `laydsGhe(idSuatChieu : int) : List<Ghe>` |
| `VeDAO` | `laydsVeConHieuLuc(suatChieu) : List<Ve>`, `kiemTraVe(idSuatChieu : int, idGhe : int) : boolean`, `tinhTienVe(suatChieu : SuatChieu, ghe : Ghe) : Ve` (= `giaCoSo + phuThu`, tạo đối tượng `Ve`), `capNhatTrangThaiVe(idHoaDon : int, trangThai : String) : boolean`, `huyVe(idVe : int)` |
| `HoaDonDAO` | `taoHoaDon(hd : HoaDon, dsVe : List<Ve>) : HoaDon` (1 giao dịch: kiểm tra `(suất,ghế)` + lưu HoaDon + Ve), `tinhTongTien(dsVe : List<Ve>) : int`, `tinhTongTien(dsVe, voucher) : int` (1 hàm, voucher optional — **bỏ** `capNhatTongTien`), `kiemTraHetHan(idHoaDon : int)`, `xacNhanThanhToan(idHoaDon : int)`, `huyHoaDon(idHoaDon : int)` — **bỏ** `timHoaDonTheoIdVe`, bỏ `phatHanhVe` (vé tạo cùng hóa đơn) |
| `VoucherKhachHangDAO` | `kiemTraVoucher(maVoucher : String, idKhachHang : int) : VoucherKhachHang`, `apDungVoucher(idHoaDon : int, idVoucher : int) : boolean`, `luuVoucher(vc : VoucherKhachHang) : int`, `layTheoId(id : int) : VoucherKhachHang`, `khoaTheoId(id : int) : VoucherKhachHang` — **bỏ** `taoVoucher()` không tham số |
| `GiaoDichDoiDiemDAO` | `luuGiaoDich(gd : GiaoDichDoiDiem) : int`, `layLichSuTheoKhachHang(idKhachHang : int) : List<VoucherKhachHang>` |
| `QuaTangDAO` | `laydsQuaTang() : List<QuaTang>`, `kiemTraSoLuong(idQua : int) : boolean`, `capNhatSoLuong(idQua : int, soLuong : int) : boolean`, `khoaQuaTang(idQua : int) : QuaTang` |
| `PhienDangNhapDAO` | `taoPhien(id : String, khachHangId : int, refreshTokenHash : String, ngayTao : Instant, hetHan : Instant) : void`, `layTheoId(id : String) : PhienDangNhap`, `khoaTheoId(id : String) : PhienDangNhap`, `xoayToken(id : String, hashCu : String, hashMoi : String, xoayLuc : Instant) : boolean`, `thuHoi(id : String, thuHoiLuc : Instant) : boolean` — JdbcTemplate, giao dịch ở tầng xác thực, không đổi DAO nghiệp vụ |

`maKhachHang` trong DAO điểm là id nội bộ dạng chuỗi số; các phép cộng/trừ từ chối số âm.

Thử lại nút Quay lại trên hóa đơn đã thanh toán là thao tác không thay đổi: backend trả tóm tắt
`DA_THANH_TOAN`, giao diện chuyển tới trang xác nhận để khách không mất vé.
`QuaTangDAO.khoaQuaTang(id)` đọc tồn kho bằng khóa ghi; giao dịch đổi điểm kiểm tra trực tiếp số dư và
tồn kho từ các hàng vừa khóa, tránh đọc lại snapshot cũ khi có yêu cầu tranh chấp.

**Giao dịch nguyên tử (bắt buộc):**

- Đổi điểm `thucHienDoiDiem`: kiểm tra lại điểm + tồn kho → trừ điểm → giảm tồn kho → **ghi `GiaoDichDoiDiem` trước** (`luuGiaoDich`) → tạo `VoucherKhachHang(giaoDichDoiDiem, maVoucher, ngayNhan, CHUA_SU_DUNG)` → `luuVoucher`. Lỗi bất kỳ bước nào → rollback (không trừ điểm, không giảm kho, không voucher, không phiếu — Q3).
- Đặt vé `taoHoaDon(hd, dsVe)`: kiểm tra trùng `(suất, ghế)` + lưu hóa đơn + vé `DANG_GIU` trong **một giao dịch**. `xacNhanThanhToan` (1 giao dịch): hóa đơn `DA_THANH_TOAN`, vé → `DA_BAN`, voucher → `DA_SU_DUNG`, cộng điểm (R1).

### 5.1. Thiết kế DAO đặt vé theo sơ đồ

**Phạm vi:** bản thiết kế trình bày theo ảnh lớp DAO đã chốt, không thay thế hợp đồng triển khai ở đầu §5.
Chỉ hiển thị lớp cha `DAO` và 8 DAO con phục vụ đặt vé. Theo điều chỉnh sau khi chốt ảnh,
không có lớp `GheDAO` trong thiết kế; chuyển trách nhiệm `laydsGhe(idSuatChieu) : List<Ghe>` sang `VeDAO`.
Ngoài việc chuyển trách nhiệm này và bổ sung kiểu còn thiếu, giữ các phương thức trong ảnh.
Các DAO/phương thức kỹ thuật không xuất hiện trong hình vẫn tồn tại trong backend.
Không đổi code, tên lớp Java, API, thực thể, quy tắc R1–R12 hoặc sơ đồ đổi điểm.

Nguồn: [dao-dat-ve-theo-so-do.puml](diagrams/puml/dao-dat-ve-theo-so-do.puml).
Ảnh render: [dao-dat-ve-theo-so-do.svg](diagrams/out/dao-dat-ve-theo-so-do.svg).

`DAO` có `-con : Connection` và `+DAO()`. Cả 8 lớp dưới đây kế thừa `DAO` và có constructor công khai
không tham số tương ứng (`+PhimDAO()`, …). Constructor không được kế thừa.
`-con` là thuộc tính private của lớp cha; quan hệ kế thừa không cho lớp con truy cập trực tiếp thuộc tính này.
Đây là ký hiệu thiết kế khái niệm, không yêu cầu thay cách Spring quản lý kết nối.

| DAO trong hình | Phương thức công khai (ngoài constructor) | Trách nhiệm |
| :--- | :--- | :--- |
| `ThanhVienDAO` | `kiemTraDangNhap(username : String, password : String) : boolean` | Xác thực thành viên trước khi đặt vé; mật khẩu vẫn kiểm tra bằng BCrypt. |
| `KhachHangDAO` | `congDiem(maKhachHang : String, diem : int) : boolean` | Cộng điểm khi thanh toán thành công theo R1; không cộng lại khi nhận kết quả lặp. |
| `PhimDAO` | `laydsPhim() : List<Phim>` | Truy vấn danh sách phim đang chiếu. |
| `KhungGioDAO` | `laydsKhungGio() : List<KhungGio>` | Truy vấn danh sách khung giờ chiếu. |
| `SuatChieuDAO` | `laydsSuatChieu(idPhim : int, ngayChieu : Date, idKhungGio : int) : List<SuatChieu>` | Tìm suất chiếu theo phim, ngày và khung giờ. |
| `VeDAO` | `tinhtienVe(suatChieu : SuatChieu, ghe : Ghe) : int`; `laydsGhe(idSuatChieu : int) : List<Ghe>`; `laydsVe(idSuatChieu : int) : List<Ve>`; `kiemTraVe(idSuatChieu : int, idGhe : int) : boolean`; `phatHanhVe(idHoaDon : int) : boolean`; `huyVe(idVe : int) : boolean` | Tính giá cơ sở + phụ thu; truy vấn toàn bộ ghế trong phòng của suất chiếu, gồm cả ghế chưa có vé; đọc vé để hiển thị trạng thái ghế; kiểm tra ghế đã được giữ/bán; chuyển trạng thái bán hoặc hủy vé. |
| `HoaDonDAO` | `taoHoaDon() : HoaDon`; `xacNhanThanhToan(idHoaDon : int) : boolean`; `huyHoaDon() : boolean` | Tạo hóa đơn; cập nhật thanh toán thành công; hủy hóa đơn chưa thanh toán. |
| `VoucherDAO` | `kiemTraVoucher(maVoucher : String, idKhachHang : int) : VoucherKhachHang`; `apDungVoucher(idHoaDon : int, idVoucher : int) : boolean` | Kiểm tra tính hợp lệ, chủ sở hữu và gắn tối đa một voucher vào hóa đơn; chỉ tiêu thụ voucher khi thanh toán thành công. |

Kiểu `int`, `List<Ve>` và `boolean` được bổ sung cho các tham số/kết quả còn thiếu trong ảnh.
Giữ nguyên cách viết `tinhtienVe` và tên `VoucherDAO` của hình; không đổi tên symbol Java theo hình.
`kiemTraVe` trả `true` khi ghế đã có vé `DANG_GIU` hoặc `DA_BAN`; vé `DA_HUY` không chặn ghế.
`laydsGhe` cung cấp sơ đồ ghế đầy đủ; `laydsVe` cung cấp trạng thái vé để đánh dấu ghế đang giữ/đã bán.
Không suy ra toàn bộ sơ đồ ghế từ danh sách vé, vì ghế chưa có vé vẫn phải hiển thị.

**Đối chiếu với triển khai hiện tại:**

| Ký hiệu trong thiết kế theo ảnh | Triển khai Java / cách hiểu |
| :--- | :--- |
| `DAO.con`, constructor không tham số | `DAO` giữ `JdbcTemplate`, constructor nhận `JdbcTemplate`; kết nối được Spring quản lý theo transaction, không giữ một `Connection` chung cho mọi khách. |
| `ThanhVienDAO.kiemTraDangNhap(username, password)` | `kiemTraDangNhap(ThanhVien tv)` nhận đối tượng do controller dựng từ thông tin đăng nhập. |
| `SuatChieuDAO` dùng `Date` | Java dùng `LocalDate` cho ngày chiếu theo quy ước kiểu ở §3. |
| `VeDAO.tinhtienVe(...): int` | `tinhTienVe(...): Ve` tạo đối tượng vé chưa lưu; giá nằm ở `Ve.giaVe`. |
| `VeDAO.laydsGhe(idSuatChieu)` | Trách nhiệm ghế được chuyển sang `VeDAO` trong thiết kế; Java hiện vẫn dùng `GheDAO.laydsGhe(idSuatChieu)`, chưa chuyển code trong bước này. |
| `VeDAO.laydsVe(idSuatChieu)` | `laydsVeConHieuLuc(SuatChieu)` đọc vé đang giữ/đã bán; không thay thế truy vấn danh sách ghế đầy đủ. |
| `VeDAO.phatHanhVe(idHoaDon)` | `capNhatTrangThaiVe(idHoaDon, DA_BAN)` trong giao dịch xác nhận thanh toán; không gọi bán vé lần nữa sau khi giao dịch đã hoàn tất. |
| `HoaDonDAO.taoHoaDon()` | Chữ ký trình bày rút gọn của `taoHoaDon(hd, dsVe)`; dữ liệu khách hàng và vé phải được truyền tường minh khi triển khai. |
| `HoaDonDAO.huyHoaDon()` | Chữ ký trình bày rút gọn của `huyHoaDon(idHoaDon)`; luồng HTTP truyền thêm ID khách đăng nhập để kiểm tra quyền sở hữu. |
| `VoucherDAO` | Lớp Java vẫn tên `VoucherKhachHangDAO`; không tạo lớp thay thế hoặc alias `VoucherDAO`. |

`taoHoaDon()` và `huyHoaDon()` không có nghĩa là DAO tự đọc session hoặc giữ thuộc tính
“khách hàng/hóa đơn hiện tại”. `DatVeController` lấy danh tính từ phiên và truyền dữ liệu vào DAO.
Các phương thức khác giữ ý nghĩa ở §5; hình không phải danh mục đầy đủ của backend.

**Ranh giới giao dịch không đổi:** Tiếp tục → tạo hóa đơn + vé `DANG_GIU` trong một transaction.
Thanh toán thành công → cập nhật hóa đơn, bán vé, sử dụng voucher và cộng điểm trong một transaction.
Các phương thức tách trên hình chỉ thể hiện trách nhiệm, không phải các transaction hoặc HTTP request độc lập.
Hủy hoặc hết 300 giây → nhả ghế; không hủy hóa đơn đã thanh toán.
Mock thanh toán thất bại vẫn giữ hóa đơn chờ để thử lại trong hạn theo R4.


## 6. Frontend — 12 trang React

Theo sơ đồ trạng thái `bc.docx` §4 + `diagrams/puml/hinh-3-20/21/22_*_fix.puml`:

```text
DangNhap → GDDangKyTaiKhoan                       (đăng ký)
GDKhachHangHome → GDChonPhim → GDKhungGio → GDSuatChieu → GDChonGhe → GDThanhToan → GDXacNhanThanhToan   (đặt vé)
GDKhachHangHome → GDChonQua → GDXacNhanDoiQua → GDThongBao   (đổi điểm)
```

Issue #1 triển khai đủ 12 trang nối với REST API thật qua `App.tsx` và `HTTPClient.fetch()`.
Các màn `GD*.tsx` nhận dữ liệu hiển thị (`frontend/src/ui/models.ts`) và callback từ tầng điều phối;
ngoài luồng thật ở `/`, chế độ `?ui=VE-02`… (`Preview.tsx`) vẫn giữ để duyệt nhanh 14 wireframe tĩnh.

| Trang | Vai trò cốt lõi |
| :--- | :--- |
| `DangNhap.tsx` | Đăng nhập qua `POST /api/auth/login` (`kiemTraDangNhap`); nút Đăng ký → `GDDangKyTaiKhoan` |
| `GDDangKyTaiKhoan.tsx` | Form đăng ký `inHoTen…inxacnhanmatkhau` → `POST /api/auth/register`; `outThongBao` |
| `GDKhachHangHome.tsx` | Home khách hàng (`kh` từ React state); `btnDatVe`, `btnDoiDiemThuong` |
| `GDChonPhim.tsx` | `tblListPhim` |
| `GDKhungGio.tsx` | `ngayChieu` (Select) + `tblListKhungGio` (R11) |
| `GDSuatChieu.tsx` | `tblListSuatChieu` theo phim + ngày + khung giờ |
| `GDChonGhe.tsx` | Sơ đồ ghế `tblListGhe` (trắng trống / xám đã bán-đang giữ), `dsVe` đã chọn, tối đa 8 ghế, Sweetbox theo cặp |
| `GDThanhToan.tsx` | Đồng hồ 5 phút, tổng tiền, `inoutVoucher`, `subApDungVoucher`, `subThanhToan`, `subQuayLai` |
| `GDXacNhanThanhToan.tsx` | Hóa đơn đã thanh toán, mã vé/QR |
| `GDChonQua.tsx` | `outTongDiem` + `tblListQua` (hết hàng hiển thị "hết hàng") + `tblLichSuDoiQua` (lịch sử phiếu đổi quà, rỗng báo "Chưa có lịch sử đổi quà.") |
| `GDXacNhanDoiQua.tsx` | Bảng tóm tắt (điểm hiện có − điểm trừ = còn lại, hạn 30 ngày) |
| `GDThongBao.tsx` | Đổi điểm thành công (`outPDQ`, `vc`) |

Session ẩn: `kh`, `phim`, `ngayChieu`, `khungGio`, `sc`, `hoaDon`, `qt`, `vc`. Tên giữ prefix `in/out/sub` như bc.

## 7. REST API

`HTTPClient.fetch()` ↔ 3 `@RestController`. Đáp ứng JSON `{ success: boolean, data?: T, message?: string, code?: string }`;
lỗi trả `{ success: false, message, code? }` — không stacktrace, không password/hash.
Ba nhóm API dùng cùng envelope và phiên server; không tin id khách hàng, giá vé, điểm hay hạn giữ ghế do client gửi.

| Nhóm | Method & path | Vào | Ra |
| :--- | :--- | :--- | :--- |
| Auth (`DangKyController`) | `POST /api/auth/register` | `{ "kh": {…}, "xacNhanMatKhau": "…" }` | HTTP 200 `{ success: true, data: KhachHang, message }` |
| | `POST /api/auth/login` | `{ "username": "…", "password": "…" }` | `ThongTinDangNhap` (§7.3) + cookie refresh; không tạo `JSESSIONID` |
| Booking (`DatVeController`) | `GET /api/phim` | — | `List<Phim>` |
| | `GET /api/khunggio?phimId=&ngayChieu=` | phim, ngày | `List<KhungGio>` |
| | `GET /api/suatchieu?phimId=&ngayChieu=&khungGioId=` | ngày/khung giờ optional để liệt kê ngày mở bán | `List<SuatChieu>` |
| | `GET /api/ghe?suatChieuId=` | — | `{dsGhe, dsVe, suatChieu}`; nhả giữ ghế hết hạn trước khi đọc |
| | `POST /api/hoadon` | `{suatChieuId, dsGhe: number[]}` | tóm tắt hóa đơn (`taoHoaDon(hd, dsVe)`) |
| | `POST /api/hoadon/apdung-voucher` | `{hoaDonId, maVoucher}` | tóm tắt sau giảm, voucher chưa tiêu |
| | `POST /api/hoadon/{id}/thanh-toan` | `{thanhCong?: boolean}` (mock, mặc định true) | tóm tắt đã thanh toán; false cho thử lại trong hạn |
| | `DELETE /api/hoadon/{id}` | — | tóm tắt hủy + nhả ghế; không hủy hóa đơn đã thanh toán |
| Reward (`DoiDiemController`) | `GET /api/quatang` | — | `{dsQuaTang: List<QuaTang>, tongDiem, lichSuDoiQua: List<VoucherKhachHang>}` |
| | `GET /api/doi-diem/lich-su` | — | `List<VoucherKhachHang>` (lịch sử phiếu đổi quà của khách trong phiên) |
| | `POST /api/doi-diem` | `{quaTangId}` | `VoucherKhachHang` kèm phiếu `giaoDichDoiDiem` |

Tóm tắt hóa đơn: `{hoaDon, tongTienTruocVoucher, tongTien, diemCong, tongDiem, serverNow}`.
`hoaDon.dsVe` có ghế/suất chiếu; tiền và điểm tính server, không thêm cột dẫn xuất. Đồng hồ dùng
`hetHan - serverNow` và thời gian trôi để hiển thị, không gửi quyết định hết hạn từ trình duyệt.
Khóa hàng suất chiếu trước kiểm tra trùng ghế; khóa hóa đơn/voucher khi thanh toán; khóa khách/quà
khi đổi điểm. Xác nhận thanh toán lặp không cộng điểm/tiêu voucher lần nữa. Hết hạn phải được commit
`HET_HAN`/vé `DA_HUY` dù API trả lỗi. Voucher được kiểm tra lại lúc ghi nhận, không áp bắp nước vào vé.
Mã voucher mới sinh duy nhất; `VC-VIDU-12345` chỉ là seed, không cấp trùng cho lần đổi mới.

### 7.1. Hợp đồng đăng ký/đăng nhập (bước 2)

`POST /api/auth/register` — body:

```json
{
  "kh": {
    "username": "annguyen2002",
    "password": "Matkhau@123",
    "hoTen": "Nguyễn Văn A",
    "ngaySinh": "2002-08-15",
    "gioiTinh": "Nam",
    "diaChi": "96 Trần Phú, Hà Đông, Hà Nội",
    "email": "an.nguyen@gmail.com",
    "sdt": "0912345678"
  },
  "xacNhanMatKhau": "Matkhau@123"
}
```

- `ngaySinh` là ngày `YYYY-MM-DD`.
- 200: `{ success: true, data: KhachHang, message: "Đăng ký tài khoản thành viên thành công!" }`;
  `data` là hồ sơ mới — `tongDiem = 0`, `hangThanhVien = null`, `ngayDangKy` do server đặt, không có `password`.
- 400: R8 sai (thiếu trường bắt buộc, email/sđt sai định dạng, mật khẩu < 8 ký tự, nhập lại không khớp)
  hoặc JSON sai; 409: `username`/`email`/`sdt` đã tồn tại. Mọi lỗi là envelope `{ success: false, message }`.
- Không chặn tuổi; `ThanhVien` + `KhachHang` ghi trong **một transaction**; mật khẩu BCrypt, không lưu/log/trả thô.

`POST /api/auth/login`:

- Body `{ "username": "annguyen2002", "password": "Matkhau@123" }`. Server bỏ qua `id`/`vaiTro`/`tongDiem` client gửi.
- 200: `{ success: true, data: ThongTinDangNhap }` với hồ sơ **đọc từ server**, access JWT và cookie refresh (§7.3).
- 401: sai thông tin — envelope `{ success: false, message }`, không tạo/thu hồi phiên đang hợp lệ.

**Phiên & hiển thị:** JWT thay hoàn toàn `HttpSession`; danh tính đã xác thực được truyền nội bộ đến
controller sau bước kiểm tra Bearer và phiên DB. `HTTPClient.fetch` gọi `/api/...`, giữ
`credentials: 'include'` cho refresh cookie và thêm Bearer cho API được bảo vệ.
Không lưu token/hồ sơ vào `localStorage`/`sessionStorage`. Reload khôi phục đăng nhập, không khôi phục
tiến trình đặt vé. `HTTPClient.fetch<T>(path, init?)` vẫn trả `data` hoặc ném `HTTPError`.

### 7.2. Hợp đồng đọc lịch sử đổi quà (issue #13)

- **Đường dẫn (nhóm Reward — `DoiDiemController`)**:
  - `GET /api/doi-diem/lich-su` → `{ success: true, data: List<VoucherKhachHang> }`.
  - `GET /api/quatang` trả kèm `lichSuDoiQua: List<VoucherKhachHang>` cùng `{ dsQuaTang, tongDiem }` để màn `GDChonQua` tải danh mục + số dư + lịch sử đồng bộ trong một lượt.
- **Chủ sở hữu & bảo mật**: luôn lọc theo danh tính khách hàng đã xác thực bằng JWT và phiên DB; mọi tham số `khachHangId` client gửi trên query/body đều bị bỏ qua. Chưa đăng nhập → HTTP 401 `{ success: false, message }`.
- **Nguồn dữ liệu & thứ tự**: đọc từ `tblGiaoDichDoiDiem gd` (chỉ chứa phiếu đổi thành công) nối `tblQuaTang qt ON qt.id = gd.tblQuaTangId` và `tblVoucherKhachHang vc ON vc.tblGiaoDichDoiDiemId = gd.id` (liên kết gián tiếp Q3, không thêm bảng/cột). Sắp xếp mới nhất trước: `gd.thoiGian DESC, gd.id DESC`.
- **Trường trả về mỗi dòng (`VoucherKhachHang`)**:
  - `id`, `maVoucher`, `ngayNhan` (hạn suy ra = `ngayNhan + 30 ngày`, không lưu cột), `ngaySuDung` (nullable), `trangThai` (`CHUA_SU_DUNG` | `DA_SU_DUNG`).
  - `giaoDichDoiDiem`: `id` (mã phiếu hiển thị `PDT-00x`), `thoiGian`, `soDiemTru`, `diemConLai` (**ảnh chụp** số điểm còn lại tại thời điểm đổi, không đổi khi `tongDiem` biến động về sau), `khachHang` (`id`), `quaTang` (`id` → mã `PT0x`, `tenQua`, `loaiQua`, `soDiem`, `soLuong`, `moTa`, `giaTriGiam`).
- **Hiển thị trên `GDChonQua.tsx`**: bảng `#tblLichSuDoiQua` nằm ngay trong màn hình đổi điểm hiện có (không thêm trang thứ 13); khi danh sách rỗng hiển thị rõ `"Chưa có lịch sử đổi quà."`; sau khi đổi quà thành công và quay về danh mục, lịch sử tải lại có ngay phiếu mới.

### 7.3. Phiên JWT (grilling Q1–Q16, xác nhận 09/11/2026)

- Giữ ba controller nghiệp vụ; login/refresh/me/logout ở `DangKyController`. Bỏ hoàn toàn đường xác thực
  `HttpSession`, không hỗ trợ hai cơ chế song song. API danh mục vẫn yêu cầu đăng nhập như trước.
- `ThongTinDangNhap = { accessToken, expiresAt, sessionExpiresAt, sessionId, khachHang }`.
  Login và `POST /api/auth/refresh` trả kiểu này trong envelope hiện tại; refresh thay cookie mới.
  `GET /api/auth/me` dùng Bearer, trả `KhachHang` mới đọc từ DB.
  `POST /api/auth/logout` thu hồi phiên của cookie refresh và trả JSON thành công, không dùng 204.
  Logout gửi `X-Auth-Session: <sessionId đã chụp khi bấm nút>`; server đối chiếu với sid trong cookie
  đã kiểm tra chữ ký. Cookie đã chuyển sang phiên khác → 409 `SESSION_CHANGED`, không thu hồi phiên
  thay thế và không sửa cookie. Thông báo logout giữa các tab cũng phải gắn đúng sessionId.
- Access JWT HS256: hạn 15 phút (không vượt hạn phiên), kiểm tra chữ ký, thuật toán cố định, issuer,
  audience, loại token, subject và UUID phiên. Refresh token cũng được ký, có loại/audience riêng để
  nhận diện replay của token cũ mà không cho kẻ đoán UUID thu hồi phiên; DB chỉ lưu SHA-256 token.
  Mọi request được bảo vệ phải kiểm tra phiên DB chưa thu hồi/chưa hết hạn.
- Phiên hết hạn tuyệt đối sau 7 ngày từ login; refresh không kéo dài mốc đó. Mỗi trình duyệt/thiết bị
  độc lập; login thành công thay tài khoản sẽ thu hồi phiên cũ của cookie hiện tại, không đá thiết bị khác.
  `tblPhienDangNhap` tồn tại qua restart backend nếu DB và khóa ký giữ nguyên.
- Refresh khóa hàng phiên và xoay token nguyên tử. Token trước vừa dùng bị lặp trong 5 giây:
  HTTP 409 `REFRESH_CONFLICT`, không cấp token, không sửa/xóa cookie, không thu hồi phiên.
  Replay token ký hợp lệ ngoài cửa sổ này thu hồi phiên, kể cả access JWT chưa hết 15 phút.
  Token giả không được phép thu hồi phiên khác.
- Refresh cookie `HttpOnly`, `SameSite=Strict`, host-only, `Path=/api/auth`, hạn bằng thời gian còn lại
  của phiên; `Secure` bật khi HTTPS. Các POST login/refresh/logout yêu cầu `X-CSRF-Protection: 1`;
  kiểm tra Origin theo danh sách cấu hình khi có Origin và không bật CORS tùy tiện.
  Quyết định xác thực/CSRF không được dùng path thô khác cách MVC định tuyến; từ chối đường dẫn
  không canonical (mã hóa/path parameter) bằng HTTP 400 `INVALID_REQUEST_PATH` trước khi có thể bỏ qua filter hoặc chuyển tới controller.
- `ApiResponse` được bổ sung `code` tùy chọn: access hết hạn → 401 `ACCESS_TOKEN_EXPIRED`;
  phiên/token không hợp lệ → 401 `SESSION_INVALID`; refresh tranh chấp → 409 `REFRESH_CONFLICT`.
  Không đưa token, hash, password hoặc stacktrace vào log/lỗi; auth response dùng `Cache-Control: no-store`.
- Frontend access chỉ ở bộ nhớ; phối hợp refresh/login/logout giữa các tab, không truyền token qua storage.
  Các tab cùng origin chung tài khoản/phiên, đồng bộ đăng xuất/đổi tài khoản; không tự gửi lại giao dịch
  dưới tài khoản khác. Khi access hết hạn chỉ refresh và gửi lại request tối đa một lần sau 401 từ lớp
  xác thực (nghiệp vụ chưa chạy); không retry mạng/timeout/5xx, không replay sau đăng nhập mới.
- Reload/mở lại trình duyệt lấy access mới qua cookie rồi hồ sơ server; có trạng thái đang khôi phục
  và cho thử lại khi lỗi mạng, không coi lỗi mạng là mất phiên. Hết/thu hồi phiên xóa UI và về đăng nhập.
  Nếu mất phản hồi cookie sau rotation có thể phải đăng nhập lại, không khôi phục bằng token cũ.
- Logout chỉ báo thành công sau server xác nhận thu hồi cả access/refresh phiên hiện tại.
  Lỗi mạng báo chưa thể đăng xuất và cho thử lại. Hóa đơn vẫn theo R4–R5: không hủy/gia hạn do logout,
  hết phiên hoặc reload, không khôi phục màn thanh toán.
- `JWT_SECRET` bắt buộc (ít nhất 32 byte), không có khóa mặc định; cấu hình thiếu/sai phải fail startup.
  `AUTH_COOKIE_SECURE` mặc định false cho HTTP demo; HTTPS phải bật true.
  `AUTH_ALLOWED_ORIGINS` mặc định `http://localhost:5173,http://127.0.0.1:5173`; thêm origin triển khai
  qua cấu hình. Không thêm Redis, quản trị thiết bị, đổi mật khẩu, RBAC hay hạ tầng production.
- Web Locks chỉ tồn tại trong secure context: `localhost`/`127.0.0.1` và HTTPS. Demo HTTP qua IP LAN hoặc
  hostname Docker không có Web Locks nên UI báo không hỗ trợ và không cho đăng nhập; muốn demo từ máy khác
  phải dùng HTTPS (bật `AUTH_COOKIE_SECURE=true`). Không thêm fallback một tab: đồng bộ tài khoản giữa tab
  và chống xoay refresh trùng là yêu cầu đã chốt.


## 8. Seed data (mốc 27/09/2026, R12)

Dùng đúng số liệu xuyên suốt kịch bản: phim PH01 Mai / PH02 / PH03; suất SC03 27/09/2026 19:00–21:11 Phòng 1 (2D) 90.000đ;
ghế A3 Thường 90.000đ, B3 VIP 120.000đ (tổng 210.000đ); quà PT01–PT04 (PT02 100 điểm, tồn kho 45; PT04 tồn kho 0);
khách Nguyễn Văn A 250 điểm; voucher `VC-VIDU-12345` (vé phim, từ PT02, 30 ngày).
Nạp qua `backend/src/main/resources/schema.sql` (chứa `CREATE TABLE` + FK) + `data.sql`.

Quyết định seed (giá trị bc chưa nêu — chỉ là dữ liệu mẫu, đổi được không ảnh hưởng nghiệp vụ):

| Mục | Giá trị seed |
| :--- | :--- |
| Khách mẫu | `annguyen2002` / `Matkhau@123` (trùng dữ liệu kịch bản đăng ký → demo đăng ký dùng dữ liệu khác; nhập lại dữ liệu mẫu = ngoại lệ 6) |
| `vaiTro` / `hangThanhVien` | `KHACH_HANG` / `NULL` (ngưỡng hạng chưa chốt) |
| `SuatChieu.trangThai` | `MO_BAN`; SC01–SC06, SC04 ở Phòng 2 (3D) để thử voucher vé phim sai loại phòng |
| Sơ đồ ghế | Mỗi phòng 40 ghế: A Thường, B–C VIP, D Thường, E Sweetbox (cặp 1-2, 3-4, 5-6, 7-8); Phòng 1: A3 = id 3, B3 = id 11 |
| Phụ thu Sweetbox | 50.000đ |
| `PDT-001` + `VC-VIDU-12345` | `thoiGian`/`ngayNhan` = thời điểm nạp seed để voucher còn hạn 30 ngày (mốc 25/09/2026 đã quá hạn); `tongDiem` vẫn 250 để chạy kịch bản đổi điểm |

## 9. Cấu trúc thư mục (gốc repo, không thêm `deploy/`)

```text
.
├── ARCHITECTURE.md
├── AGENTS.md
├── GLOSSARY.md                   # thuật ngữ nghiệp vụ chuẩn (issue #1)
├── docker-compose.yml            # mysql + backend + frontend
├── frontend/                     # Vite + React + TS, dev server cổng 5173
│   ├── Dockerfile                # node:24-alpine, npm ci, npm run dev
│   ├── .dockerignore             # node_modules, dist
│   ├── index.html                # mount /src/main.tsx
│   ├── package.json              # + package-lock.json do orchestrator sinh
│   ├── tsconfig.json
│   ├── vite.config.ts            # proxy /api → API_PROXY_TARGET
│   └── src/
│       ├── main.tsx              # mount React
│       ├── App.tsx               # điều hướng theo React state (mục 6)
│       ├── types.ts              # kiểu hồ sơ, đặt vé và đổi điểm dùng chung
│       ├── styles.css
│       ├── ui/                   # primitives, data model cho view và preview mẫu
│       ├── api/                  # httpClient.ts (Bearer/retry), authSession.ts (phiên), transport.ts (fetch/envelope)
│       └── pages/                # DangNhap, GDDangKyTaiKhoan, GDKhachHangHome + 9 màn nghiệp vụ
├── backend/                      # Spring Boot (Maven)
│   ├── Dockerfile                # maven build → JRE 21
│   ├── .dockerignore             # target/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/cineman/
│       │   ├── controller/       # DangKyController, DatVeController, DoiDiemController
│       │   ├── auth/             # JWT, filter xác thực/CSRF, cookie và vòng đời phiên
│       │   ├── dao/              # DAO + ThanhVienDAO, KhachHangDAO, PhimDAO, KhungGioDAO,
│       │   │                     # SuatChieuDAO, GheDAO, VeDAO, HoaDonDAO,
│       │   │                     # VoucherKhachHangDAO, GiaoDichDoiDiemDAO, QuaTangDAO, PhienDangNhapDAO
│       │   └── entity/           # ThanhVien, KhachHang, Phim, KhungGio, PhongChieu, SuatChieu,
│       │                         # LoaiGhe, Ghe, Ve, HoaDon, QuaTang (+3 lớp con), VoucherKhachHang,
│       │                         # GiaoDichDoiDiem, PhienDangNhap (kỹ thuật)
│       └── resources/            # application.properties, schema.sql, data.sql, migrations/
```

> Tên file/lớp giữ tiếng Việt không dấu như bc để đối chiếu sơ đồ (VD `ThanhVienDAO`, `GDChonGhe.tsx`).

`frontend/src/ui/Preview.tsx` có bộ chọn dùng chung cho 14 cảnh `DK-01`–`DT-03`.
Mỗi cảnh chọn trạng thái mẫu của chính cảnh đó (có dữ liệu, đang tải, rỗng, lỗi và
 ca lỗi nghiệp vụ liên quan). Bộ chọn này không suy luận giao dịch từ trạng thái
 màn hình; nó chỉ để xem trạng thái tĩnh có nhãn rõ.

## 10. Diagrams

Tên/chữ ký triển khai bám **bản `_fix`** của `../phase/nhan-xet-thiet-ke.md`
(`VeDAO.kiemTraVe`, `taoHoaDon(hd, dsVe)`, ghi phiếu trước voucher, tách 3.24 a/b/c).
Nguồn sơ đồ đổi điểm ở `diagrams/puml/hinh-3-22-hoat-dong-doi-diem.puml` và
`diagrams/puml/hinh-3-25-tuan-tu-doi-diem.puml` đã bổ sung lịch sử theo issue #13;
ảnh SVG/PNG trong `diagrams/out/` được render từ nguồn, không sửa ảnh bằng tay.
`diagrams/puml/phien-jwt.puml` mô tả đầy đủ bảng phiên kỹ thuật và khóa ngoại đến khách hàng;
hai sơ đồ đổi điểm dùng danh tính JWT đã kiểm tra phiên DB thay cho `HttpSession`.
Khi đồng bộ với repo phân tích, cần cập nhật nhánh mock thất bại ở R4: giữ hóa đơn chờ
thanh toán để thử lại trong hạn theo story 35 của issue #1.

Sơ đồ [DAO đặt vé theo ảnh](diagrams/out/dao-dat-ve-theo-so-do.svg) có nguồn riêng
`diagrams/puml/dao-dat-ve-theo-so-do.puml`; đây là thiết kế khái niệm ở §5.1, không thay thế chữ ký `_fix`.
Render SVG từ nguồn bằng `plantuml -tsvg -charset UTF-8 -o ../out diagrams/puml/dao-dat-ve-theo-so-do.puml`.


## 11. Chạy & build

```bash
# Tạo một lần và lưu bí mật để dùng lại qua restart/recreate backend:
export JWT_SECRET="$(openssl rand -hex 32)"
docker compose up --build        # mysql:3306, backend:8080, frontend:5173 (DB seed mới mỗi lần down/up)
# frontend: build ./frontend, node:24-alpine, API_PROXY_TARGET=http://backend:8080, depends_on backend
#   → Vite dev server lắng nghe 0.0.0.0:5173, proxy /api sang backend nội bộ.
# hoặc chạy tay (DB cineman, utf8mb4):
mysql --default-character-set=utf8mb4 -u cineman -p cineman < backend/src/main/resources/schema.sql
mysql --default-character-set=utf8mb4 -u cineman -p cineman < backend/src/main/resources/data.sql
cd backend && mvn spring-boot:run   # DB_URL / DB_USER / DB_PASSWORD ghi đè kết nối mặc định
cd frontend && npm ci && npm run dev    # localhost:5173, /api → http://localhost:8080 (mặc định)
cd backend && mvn test              # H2 chế độ MySQL, tự nạp schema.sql + data.sql
cd frontend && npm run build        # tsc --noEmit && vite build → dist/
```

DB đang có dữ liệu chỉ chạy migration bổ sung bảng phiên
`backend/src/main/resources/migrations/20261109-jwt-session.sql`, không nạp lại `schema.sql`/`data.sql`.
`JWT_SECRET` phải được đặt cả khi chạy backend ngoài Compose; profile test dùng khóa riêng chỉ cho test.

Kiểm thử M1 có thể chạy lại trên **DB MySQL riêng, được phép xóa schema/seed**:

```bash
cd backend
M1_TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/cineman_test' \
M1_TEST_DB_USER=cineman M1_TEST_DB_PASSWORD=cineman \
mvn -Dtest=M1IntegrationTest test
```

Không trỏ lệnh này vào DB đang dùng: `spring.sql.init.mode=always` trong profile test nạp lại toàn bộ
schema/seed. Kiểm thử qua REST/JDBC kiểm tra giữ ghế tranh chấp, voucher/điểm đúng một lần, và rollback
cả hóa đơn/vé, phiếu/voucher, thanh toán/điểm khi một bước ghi bị lỗi.

Issue #13: `M1IntegrationTest` kiểm tra lịch sử theo chủ phiên, trạng thái rỗng, thứ tự
`thoiGian DESC, id DESC`, snapshot điểm sau thanh toán, và không thêm lịch sử khi đổi thất bại/rollback.
Smoke trên seed: đăng nhập → Đổi điểm → thấy PDT-001/PT02/100/150/VC-VIDU-12345 →
đổi PT02 → về danh mục thấy thêm đúng một phiếu mới → đặt SC03 ghế A3+B3, áp voucher seed,
thanh toán 120.000đ và cộng 10 điểm → mở lại lịch sử: điểm hiện có 160, phiếu cũ vẫn 150,
voucher seed đã dùng kèm ngày dùng. Khách đăng ký mới thấy “Chưa có lịch sử đổi quà.”

Issue #27: chuỗi tiếng Việt đi nguyên dạng UTF-8 từ seed → MySQL `utf8mb4` → JDBC → JSON → DOM.
`data.sql` đặt `SET NAMES utf8mb4` trước INSERT: charset của server không thay thế charset phiên import.
Kiểm chứng trên MySQL 8.4: import cùng file UTF-8 bằng client `latin1` và `utf8mb4`, đọc lại qua kết nối
`utf8mb4`; tên khách, địa chỉ, phim, quà, loại ghế và phòng phải giữ nguyên dấu tiếng Việt.
Không giải mã bù ở frontend. `backend/src/main/resources/repair-unicode.sql` sửa record seed cũ trong
một transaction: chỉ thay từng trường có ID và giá trị byte khớp chính xác seed bị import qua Latin-1.
Chữ đã đúng/chỉnh sửa riêng, NULL, ID, mật khẩu, điểm, tồn kho và giao dịch không đổi; chạy lại là no-op.
Sao lưu trước, chạy thủ công trên DB đang dùng (xem README); không nạp lại `schema.sql`/`data.sql`.
Script này không khôi phục dữ liệu nhập riêng hoặc dạng lỗi khác bằng cách đoán nội dung.
`UnicodeRepairIntegrationTest` chỉ bật với `UNICODE_TEST_DB_URL` trỏ tới MySQL riêng được phép xóa
schema/seed; kiểm chứng bảo toàn mọi cột/record của 13 bảng và tính idempotent của bản sửa.


Kiểm chứng JWT (trên cây đã merge `dev`, gồm cả `UnicodeRepairIntegrationTest` của #27):
`mvn test` 81 test, 0 lỗi, 1 skip (`UnicodeRepairIntegrationTest` chỉ chạy khi đặt `UNICODE_TEST_DB_URL`),
`M1IntegrationTest` trên MySQL 8.4 (23 test) và frontend production build. Smoke API trên MySQL 8.4 phủ seed (250 điểm → đổi PT02 còn 150), voucher `VC-VIDU-12345` (120.000đ, +10 điểm),
chặn quá 8 ghế và trùng ghế, rollback khi thiếu điểm, chặn hóa đơn của khách khác, logout thu hồi ngay,
refresh xoay token kèm 409 trong 5 giây và replay ngoài cửa sổ thu hồi phiên; chờ **300 giây thực** xác nhận
thanh toán muộn bị 409 và ghế được nhả. Phiên + refresh vẫn hợp lệ sau restart backend khi giữ DB/khóa ký.
Migration chạy trên schema **trước JWT** (`ee320b2^`): 13 bảng/14 FK → 14/15, khách hàng, 250 điểm, phiếu đổi
và vé giữ nguyên, khóa ngoại của bảng phiên chèn được. Đường dẫn không canonical (`..`, `;`, `%`, `//`) trả
400 `INVALID_REQUEST_PATH` qua HTTP thô; đường dẫn canonical không công khai như `/api/phim`, `/api/authX`
trả 401; cookie đã thu hồi kèm `X-Auth-Session` cũ trả 409 không xóa cookie, còn retry đúng phiên trả 200.
Node 26 với hai instance module sản xuất độc lập (Web Locks + BroadcastChannel thật, gọi API MySQL) kiểm tra
refresh single-flight, login đồng thời, logout chờ login mới với thứ tự khoá xác định qua `locks.query()`
(lặp 5 vòng), thông báo logout cũ, không replay giao dịch dưới tài khoản mới hoặc sau mất phản hồi.
DOM thật của React được chạy trong Lightpanda: form `DangNhap` → trang chủ hiện tên khách → reload khôi phục
phiên → đăng xuất → màn đăng nhập, lặp 5 lần; đăng xuất ở một trang cũng đưa trang thứ hai về đăng nhập.
Lightpanda không có `navigator.locks` và báo `Response.status` của 401 thành 407 (backend và Vite proxy đều
xác nhận trả 401 bằng curl), nên khi kiểm tra DOM chỉ thêm script sửa hai khiếm khuyết engine đó, không sửa
ứng dụng. Chưa kiểm chứng: tranh chấp Web Locks thật giữa nhiều tab (chỉ chạy trong harness Node), độ trễ
mạng thực và bố cục hiển thị; Lightpanda không tạo ảnh render.

`npm ci` cài đúng theo `package-lock.json` (Dockerfile dùng lệnh này); `npm install` chỉ khi cần cập nhật lockfile.
`API_PROXY_TARGET` mặc định `http://localhost:8080`, trong compose đặt `http://backend:8080`.
Image frontend `node:24-alpine` — Node 24 thỏa `engines` của Vite 8 và `@vitejs/plugin-react` (`^20.19.0 || >=22.12.0`).

## 12. Điểm còn mở (không chặn xây dựng, đã ghi nhận)

- `hangThanhVien` đổi theo `tongDiem` ở ngưỡng nào — bc chưa định nghĩa → giữ String, không suy diễn.
- Chi tiết cổng thanh toán thật (Q10 chỉ mock) — để pha Cài đặt.
- Actor Cổng thanh toán không thêm (theo `requirement.md`).

## 13. Shared understanding (tổng hợp từ grilling Q1–Q11)

| Q | Quyết định | Trạng thái |
| :--- | :--- | :--- |
| Q1 | Frontend React (thay Thymeleaf) | ✅ chốt |
| Q2 | Backend REST + DAO (3 controller nghiệp vụ) | ✅ chốt |
| Q3 | Thêm `GiaoDichDoiDiem`, ghi phiếu trước voucher | ✅ chốt |
| Q4 | Giữ 3 lớp con `QuaTang`; 1 bảng `tblQuaTang` (`loaiQua` + `giaTriGiam`), bỏ bảng tách `tblVoucherGiamGia` ([#3](https://github.com/22imer/PTTK-CineMan/issues/3), sửa 08/11/2026) | ✅ chốt |
| Q5–Q6 | Chốt R1–R12 | ✅ chốt |
| Q7 | 13 bảng nghiệp vụ / 14 FK + `tblPhienDangNhap` kỹ thuật / 1 FK; tổng 14 bảng / 15 FK (JWT, xác nhận 09/11/2026) | ✅ chốt |
| Q8 | 12 trang `GD*.tsx` | ✅ chốt |
| Q9 | Java Spring Boot, DAO chữ ký _fix | ✅ chốt |
| Q10 | Phiên JWT (§7.3) + cổng thanh toán mock | ✅ chốt |
| Q11 | Diagrams _fix + seed + docker | ✅ chốt |

Chi tiết mục 10–12 đã được gộp vào file này; nếu đổi quyết định, sửa mục liên quan trong cùng commit.
