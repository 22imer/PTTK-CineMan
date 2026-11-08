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
| Q7 | Schema **13 bảng**, **14 khóa ngoại** | `tblXId` cho khóa ngoại; kiểm tra trùng `(suatChieu,ghe)` ở app-level |
| Q8 | **12 trang `*.tsx`** | `DangNhap` tách riêng; giữ thuộc tính `in/out/sub`; session ẩn `kh, phim, sc, hoaDon…` |
| Q9 | Backend **Java Spring Boot** | Chữ ký DAO theo `_fix` (§5); mật khẩu băm BCrypt |
| Q10 | Session mock + cổng thanh toán mock | `HttpSession`; cổng trả redirect mock; không thêm actor Cổng thanh toán |
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
| Database | MySQL | 13 bảng `tbl*` (§3), 14 khóa ngoại `tblXId` |

## 3. Mô hình dữ liệu (MySQL, 13 bảng, 14 khóa ngoại)

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
| | | 13 bảng = 12 bảng bc + `tblGiaoDichDoiDiem` (lịch sử phiếu đổi quà, canonical) |

> Quyết định 08/11/2026 ([issue #3](https://github.com/22imer/PTTK-CineMan/issues/3)): lịch sử đổi quà dùng
> `tblGiaoDichDoiDiem` hiện có — không thêm bảng lịch sử trùng lặp; `giaTriGiam` nằm chung `tblQuaTang`,
> bỏ bảng tách `tblVoucherGiamGia` (bảng này từng được thêm chỉ để ép đủ "14 bảng" — con số đếm sai).
> 3 lớp con `QuaTang` vẫn bắt buộc; schema đúng là 13 bảng / 14 khóa ngoại. Bước 2 không đổi API hay lịch sử.

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
| `GiaoDichDoiDiemDAO` | `luuGiaoDich(gd : GiaoDichDoiDiem) : int` |
| `QuaTangDAO` | `laydsQuaTang() : List<QuaTang>`, `kiemTraSoLuong(idQua : int) : boolean`, `capNhatSoLuong(idQua : int, soLuong : int) : boolean`, `khoaQuaTang(idQua : int) : QuaTang` |

`maKhachHang` trong DAO điểm là id nội bộ dạng chuỗi số; các phép cộng/trừ từ chối số âm.

Thử lại nút Quay lại trên hóa đơn đã thanh toán là thao tác không thay đổi: backend trả tóm tắt
`DA_THANH_TOAN`, giao diện chuyển tới trang xác nhận để khách không mất vé.
`QuaTangDAO.khoaQuaTang(id)` đọc tồn kho bằng khóa ghi; giao dịch đổi điểm kiểm tra trực tiếp số dư và
tồn kho từ các hàng vừa khóa, tránh đọc lại snapshot cũ khi có yêu cầu tranh chấp.

**Giao dịch nguyên tử (bắt buộc):**

- Đổi điểm `thucHienDoiDiem`: kiểm tra lại điểm + tồn kho → trừ điểm → giảm tồn kho → **ghi `GiaoDichDoiDiem` trước** (`luuGiaoDich`) → tạo `VoucherKhachHang(giaoDichDoiDiem, maVoucher, ngayNhan, CHUA_SU_DUNG)` → `luuVoucher`. Lỗi bất kỳ bước nào → rollback (không trừ điểm, không giảm kho, không voucher, không phiếu — Q3).
- Đặt vé `taoHoaDon(hd, dsVe)`: kiểm tra trùng `(suất, ghế)` + lưu hóa đơn + vé `DANG_GIU` trong **một giao dịch**. `xacNhanThanhToan` (1 giao dịch): hóa đơn `DA_THANH_TOAN`, vé → `DA_BAN`, voucher → `DA_SU_DUNG`, cộng điểm (R1).

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
| `GDChonQua.tsx` | `outTongDiem` + `tblListQua` (hết hàng hiển thị "hết hàng") |
| `GDXacNhanDoiQua.tsx` | Bảng tóm tắt (điểm hiện có − điểm trừ = còn lại, hạn 30 ngày) |
| `GDThongBao.tsx` | Đổi điểm thành công (`outPDQ`, `vc`) |

Session ẩn: `kh`, `phim`, `ngayChieu`, `khungGio`, `sc`, `hoaDon`, `qt`, `vc`. Tên giữ prefix `in/out/sub` như bc.

## 7. REST API

`HTTPClient.fetch()` ↔ 3 `@RestController`. Đáp ứng JSON `{ success: boolean, data?: T, message?: string }`;
lỗi trả `{ success: false, message }` — không stacktrace, không password/hash.
Ba nhóm API dùng cùng envelope và phiên server; không tin id khách hàng, giá vé, điểm hay hạn giữ ghế do client gửi.

| Nhóm | Method & path | Vào | Ra |
| :--- | :--- | :--- | :--- |
| Auth (`DangKyController`) | `POST /api/auth/register` | `{ "kh": {…}, "xacNhanMatKhau": "…" }` | HTTP 200 `{ success: true, data: KhachHang, message }` |
| | `POST /api/auth/login` | `{ "username": "…", "password": "…" }` | HTTP 200 `{ success: true, data: KhachHang }` + `HttpSession.khachHangId` |
| Booking (`DatVeController`) | `GET /api/phim` | — | `List<Phim>` |
| | `GET /api/khunggio?phimId=&ngayChieu=` | phim, ngày | `List<KhungGio>` |
| | `GET /api/suatchieu?phimId=&ngayChieu=&khungGioId=` | ngày/khung giờ optional để liệt kê ngày mở bán | `List<SuatChieu>` |
| | `GET /api/ghe?suatChieuId=` | — | `{dsGhe, dsVe, suatChieu}`; nhả giữ ghế hết hạn trước khi đọc |
| | `POST /api/hoadon` | `{suatChieuId, dsGhe: number[]}` | tóm tắt hóa đơn (`taoHoaDon(hd, dsVe)`) |
| | `POST /api/hoadon/apdung-voucher` | `{hoaDonId, maVoucher}` | tóm tắt sau giảm, voucher chưa tiêu |
| | `POST /api/hoadon/{id}/thanh-toan` | `{thanhCong?: boolean}` (mock, mặc định true) | tóm tắt đã thanh toán; false cho thử lại trong hạn |
| | `DELETE /api/hoadon/{id}` | — | tóm tắt hủy + nhả ghế; không hủy hóa đơn đã thanh toán |
| Reward (`DoiDiemController`) | `GET /api/quatang` | — | `{dsQuaTang: List<QuaTang>, tongDiem}` |
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
- 200: `{ success: true, data: KhachHang }` với hồ sơ **đọc từ server**; ghi `khachHangId` vào `HttpSession`
  (đổi id phiên khi đăng nhập thành công để chống session fixation) và trả cookie phiên.
- 401: sai thông tin — envelope `{ success: false, message }`, không tạo phiên.

**Phiên & hiển thị:** backend là nguồn sự thật của phiên (`HttpSession.khachHangId`). `HTTPClient.fetch` đặt
`credentials: 'include'` và luôn gọi path tương đối `/api/...`, nên qua proxy Vite cùng origin cookie phiên
(`JSESSIONID`) đi kèm mọi request; `KhachHang` chỉ nằm trong **React state** để hiển thị. Không lưu mật khẩu hay
object hồ sơ vào `localStorage`/`sessionStorage`; reload trang quay lại `DangNhap` (state mất) dù phiên phía
server còn hiệu lực. `HTTPClient.fetch<T>(path, init?)` trả `data` và `throw new Error(message)`
(kể cả lỗi mạng) cho UI hiển thị.

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
│       ├── api/httpClient.ts     # HTTPClient.fetch()
│       └── pages/                # DangNhap, GDDangKyTaiKhoan, GDKhachHangHome + 9 màn nghiệp vụ
├── backend/                      # Spring Boot (Maven)
│   ├── Dockerfile                # maven build → JRE 21
│   ├── .dockerignore             # target/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/cineman/
│       │   ├── controller/       # DangKyController, DatVeController, DoiDiemController
│       │   ├── dao/              # DAO + ThanhVienDAO, KhachHangDAO, PhimDAO, KhungGioDAO,
│       │   │                     # SuatChieuDAO, GheDAO, VeDAO, HoaDonDAO,
│       │   │                     # VoucherKhachHangDAO, GiaoDichDoiDiemDAO, QuaTangDAO
│       │   └── entity/           # ThanhVien, KhachHang, Phim, KhungGio, PhongChieu, SuatChieu,
│       │                         # LoaiGhe, Ghe, Ve, HoaDon, QuaTang (+3 lớp con), VoucherKhachHang,
│       │                         # GiaoDichDoiDiem
│       └── resources/            # application.properties, schema.sql, data.sql
```

> Tên file/lớp giữ tiếng Việt không dấu như bc để đối chiếu sơ đồ (VD `ThanhVienDAO`, `GDChonGhe.tsx`).

`frontend/src/ui/Preview.tsx` có bộ chọn dùng chung cho 14 cảnh `DK-01`–`DT-03`.
Mỗi cảnh chọn trạng thái mẫu của chính cảnh đó (có dữ liệu, đang tải, rỗng, lỗi và
 ca lỗi nghiệp vụ liên quan). Bộ chọn này không suy luận giao dịch từ trạng thái
 màn hình; nó chỉ để xem trạng thái tĩnh có nhãn rõ.

## 10. Diagrams

Tên/chữ ký triển khai bám **bản `_fix`** của `../phase/nhan-xet-thiet-ke.md`
(`VeDAO.kiemTraVe`, `taoHoaDon(hd, dsVe)`, ghi phiếu trước voucher, tách 3.24 a/b/c).
Checkout triển khai này không chứa `diagrams/puml/` hay ảnh `out/`; không sinh hoặc sửa ảnh thay cho
nguồn gốc. Khi đồng bộ với repo phân tích, cần cập nhật nhánh mock thất bại ở R4: giữ hóa đơn chờ
thanh toán để thử lại trong hạn theo story 35 của issue #1.

## 11. Chạy & build

```bash
docker compose up --build        # mysql:3306, backend:8080, frontend:5173 (DB seed mới mỗi lần down/up)
# frontend: build ./frontend, node:24-alpine, API_PROXY_TARGET=http://backend:8080, depends_on backend
#   → Vite dev server lắng nghe 0.0.0.0:5173, proxy /api sang backend nội bộ.
# hoặc chạy tay (DB cineman, utf8mb4):
mysql -u cineman -p cineman < backend/src/main/resources/schema.sql
mysql -u cineman -p cineman < backend/src/main/resources/data.sql
cd backend && mvn spring-boot:run   # DB_URL / DB_USER / DB_PASSWORD ghi đè kết nối mặc định
cd frontend && npm ci && npm run dev    # localhost:5173, /api → http://localhost:8080 (mặc định)
cd backend && mvn test              # H2 chế độ MySQL, tự nạp schema.sql + data.sql
cd frontend && npm run build        # tsc --noEmit && vite build → dist/
```

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

UI hiện chỉ được kiểm chứng hoàn toàn về cấu trúc qua DOM render trong Lightpanda;
 Lightpanda không tạo ảnh render và không thay cho kiểm tra bằng mắt ở khổ hẹp.

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
| Q7 | Schema 13 bảng / 14 khóa ngoại, FK `tblXId` ([#3](https://github.com/22imer/PTTK-CineMan/issues/3), sửa 08/11/2026) | ✅ chốt |
| Q8 | 12 trang `GD*.tsx` | ✅ chốt |
| Q9 | Java Spring Boot, DAO chữ ký _fix | ✅ chốt |
| Q10 | Session mock + cổng mock | ✅ chốt |
| Q11 | Diagrams _fix + seed + docker | ✅ chốt |

Chi tiết mục 10–12 đã được gộp vào file này; nếu đổi quyết định, sửa mục liên quan trong cùng commit.
