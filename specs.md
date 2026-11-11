# CineMan — SPECS theo kịch bản bc.docx

Nguồn: `../bc.docx` §1–§11 (ngoài repo, không có sẵn; báo cáo Module M1: đăng ký, đặt vé trực tuyến, đổi điểm).
Tài liệu này trích nguyên văn kịch bản chuẩn + ngoại lệ của 3 chức năng thành spec triển khai.
Kiến trúc triển khai xem `ARCHITECTURE.md`; quy tắc chốt R1–R12 xem `ARCHITECTURE.md` §4;
trong `diagrams/` chỉ có sơ đồ DAO đặt vé và 2 sơ đồ đổi điểm (§10 của `ARCHITECTURE.md`).

---

## 1. Use case (§1 bc.docx)

| Mảng | Use case | Mô tả |
| :--- | :--- | :--- |
| Đăng ký | Đăng ký thành viên | Khách chưa tài khoản cung cấp/xác nhận thông tin → tạo tài khoản + hồ sơ |
| Đặt vé | Đăng nhập | Xác thực tài khoản để truy cập hệ thống |
| Đặt vé | Chọn phim | Chọn phim muốn xem |
| Đặt vé | Chọn suất chiếu | Chọn suất chiếu khả dụng |
| Đặt vé | Chọn ghế | Chọn các ghế khả dụng |
| Đặt vé | Xác nhận đặt vé | Xác nhận suất chiếu muốn đặt |
| Đặt vé | Áp mã giảm giá | Dùng phần thưởng/quà tặng |
| Đổi điểm | Đăng nhập | Xác thực tài khoản để truy cập hệ thống |
| Đổi điểm | Chọn quà/vé thưởng | Dùng điểm đổi quà tặng/vé |
| Đổi điểm | Tìm phần thưởng | Tìm phần thưởng trong danh mục quà tặng/vé |
| Đổi điểm | Xác nhận đổi thưởng | Tiêu dùng điểm để hoàn tất giao dịch đổi thưởng |

Actor: *Khách hàng chưa có tài khoản* (đăng ký), *Khách hàng* (đặt vé, đổi điểm).
Cổng thanh toán là hệ thống bên ngoài — hệ thống chỉ lưu kết quả vào hóa đơn.

---

## 2. Kịch bản đăng ký thành viên (§2.1.1)

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Use case** | Đăng ký thành viên |
| **Actor** | Khách hàng chưa có tài khoản |
| **Tiền điều kiện** | Đã truy cập trang chủ CineMan, chưa đăng nhập |
| **Hậu điều kiện** | Tài khoản được lưu, hồ sơ KH khởi tạo với `tongDiem=0`, KH dùng tài khoản để đăng nhập được |
| **Qui tắc áp dụng** | R8 |

### 2.1. Kịch bản chính (8 bước)

1. Tại giao diện chính (trang chủ, chưa đăng nhập), KH click liên kết **Đăng ký thành viên**.
2. Giao diện đăng ký hiện lên, gồm ô nhập: Họ tên, Tên đăng nhập, Mật khẩu, Nhập lại mật khẩu, Ngày sinh, Giới tính, Địa chỉ, Email, Số điện thoại; nút Đăng ký và nút Hủy.
3. KH nhập đầy đủ thông tin:

| Trường | Giá trị mẫu |
| :--- | :--- |
| Họ tên | Nguyễn Văn A |
| Tên đăng nhập | annguyen2002 |
| Mật khẩu | Matkhau@123 |
| Nhập lại mật khẩu | Matkhau@123 |
| Ngày sinh | 15/08/2002 |
| Giới tính | Nam |
| Địa chỉ | 96 Trần Phú, Hà Đông, Hà Nội |
| Email | an.nguyen@gmail.com |
| Số điện thoại | 0912345678 |

4. KH click nút **Đăng ký**.
5. Hệ thống kiểm tra hợp lệ: trường bắt buộc không trống; email và SDT đúng định dạng; mật khẩu ≥ 8 ký tự; mật khẩu nhập lại khớp (R8).
6. Hệ thống kiểm tra duy nhất: tên đăng nhập, email và SDT chưa từng tồn tại trên hệ thống.
7. Hệ thống lưu thông tin thành viên và tạo hồ sơ KH: cấp mã khách hàng, điểm tích lũy ban đầu = 0.
8. Hệ thống thông báo **"Đăng ký tài khoản thành viên thành công!"** và tự động chuyển về giao diện Đăng nhập.

### 2.2. Ngoại lệ

| # | Điều kiện | Hành vi |
| :--- | :--- | :--- |
| 4 | Click nút Hủy | Quay về giao diện chính, không lưu thông tin |
| 5 | Trường bắt buộc trống / email hoặc SDT sai định dạng / mật khẩu < 8 ký tự / hai lần mật khẩu không khớp | Thông báo lỗi; KH sửa lại thông tin và quay lại bước 4 |
| 6 | Tên đăng nhập, email hoặc SDT đã tồn tại | Thông báo thông tin đã được sử dụng, đề nghị dùng thông tin khác; KH sửa lại và quay lại bước 4 |

---

## 3. Kịch bản đặt mua vé trực tuyến (§2.1.2, 19 bước)

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Use case** | Đặt mua vé trực tuyến |
| **Actor** | Khách hàng |
| **Tiền điều kiện** | Đã đăng nhập bằng tài khoản thành viên; có suất chiếu đang trong thời gian mở bán vé |
| **Hậu điều kiện** | Cổng thanh toán xác nhận thành công trong thời gian giữ ghế; hóa đơn → Đã thanh toán; từng vé → Đã bán; voucher đã dùng → Đã sử dụng; cộng điểm tích lũy; gửi mã vé điện tử |
| **Qui tắc áp dụng** | R1, R2, R3, R4, R5, R6, R7, R11, R12 |

### 3.1. Kịch bản chính

**Bước 1–2.** Sau đăng nhập, KH chọn chức năng Đặt vé trực tuyến. Giao diện hiện danh sách phim có suất chiếu khả dụng:

| TT | Mã | Tên phim | Thể loại | Thời lượng | Độ tuổi | Chọn |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | PH01 | Mai | Tâm lý, Tình cảm | 131 phút | C18 | chọn |
| 2 | PH02 | Đào, Phở và Piano | Lịch sử, Chiến tranh | 100 phút | C13 | chọn |
| 3 | PH03 | Kung Fu Panda 4 | Hoạt hình, Hài | 94 phút | P | chọn |

**Bước 3.** KH click chọn phim **Mai**.

**Bước 4.** Giao diện chọn ngày chiếu và khung giờ. KH chọn ngày chiếu **27/09/2026**; hiển thị:

| TT | Giờ bắt đầu | Giờ kết thúc | Chọn |
| :--- | :--- | :--- | :--- |
| 1 | 09:00 | 11:11 | chọn |
| 2 | 14:00 | 16:11 | chọn |
| 3 | 19:00 | 21:11 | chọn |

**Bước 5.** KH chọn khung giờ **19:00–21:11**.

**Bước 6.** Danh sách suất chiếu của phim Mai ngày 27/09/2026, khung 19:00–21:11:

| TT | Mã | Ngày chiếu | Khung giờ | Phòng chiếu | Giá cơ sở | Chọn |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | SC03 | 27/09/2026 | 19:00–21:11 | Phòng 1 (2D) | 90.000đ | chọn |

**Bước 7.** KH chọn suất chiếu **SC03 (Phòng 1)**.

**Bước 8.** Giao diện sơ đồ ghế: vị trí màn hình chiếu, các hàng ghế màu trạng thái [Trắng] ghế trống, [Xám] ghế đã bán/đang giữ. Bảng ghế đã chọn đang rỗng.

**Bước 9.** KH chọn các ghế trống trên sơ đồ. Hệ thống kiểm tra ghế còn trống và tính đơn giá = giá cơ sở suất chiếu + phụ thu loại ghế. Bảng ghế đã chọn:

| TT | Hàng | Số ghế | Loại ghế | Phụ thu | Đơn giá | Xóa |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | A | 3 | Ghế Thường | 0đ | 90.000đ | xóa |
| 2 | B | 3 | Ghế VIP | 30.000đ | 120.000đ | xóa |

> Tổng tiền: **210.000đ**. Điểm tích lũy dự kiến: **+10** (R1: 210.000 ÷ 20.000 = 10, làm tròn xuống, tính trước voucher).

**Bước 10.** KH click nút **Tiếp tục**.

**Bước 11.** Hệ thống kiểm tra lại trạng thái các ghế, tạo hóa đơn **HD001** trạng thái **Chờ thanh toán** cùng các vé **A3, B3 trạng thái Đang giữ**, giữ ghế trong **300 giây** tính từ thời điểm này; hạn giữ ghế do hệ thống lưu trên hóa đơn, đồng hồ trên giao diện chỉ để hiển thị (R5). Chuyển sang giao diện thanh toán.

**Bước 12.** Giao diện thanh toán: tóm tắt thông tin vé, tổng tiền, điểm dự kiến, đồng hồ đếm ngược giữ ghế, ô nhập voucher và các nút Áp dụng, Thanh toán, Quay lại.

| **Phim** | **Suất chiếu** | **Ngày chiếu** | **Khung giờ** | **Phòng chiếu** | **Giá cơ sở** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Mai | SC03 | 27/09/2026 | 19:00–21:11 | Phòng 1 (2D) | 90.000đ |

| TT | Hàng | Số ghế | Loại ghế | Phụ thu | Đơn giá |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | A | 3 | Ghế Thường | 0đ | 90.000đ |
| 2 | B | 3 | Ghế VIP | 30.000đ | 120.000đ |

> Tổng tiền: 210.000đ. Điểm dự kiến: +10.

**Bước 13.** KH nhập mã voucher **VC-VIDU-12345** (Vé xem phim 2D Standard, nhận được ở kịch bản đổi điểm 2.1.3) và click **Áp dụng**.

**Bước 14.** Hệ thống kiểm tra voucher: thuộc đúng KH đang đăng nhập, chưa sử dụng, còn hạn (HSD 30 ngày), hóa đơn chưa có voucher khác, suất chiếu SC03 phòng 2D nên dùng được voucher vé phim. Voucher hợp lệ → hệ thống miễn một vé có đơn giá thấp nhất (**A3, 90.000đ**) và hiển thị: `Voucher VC-VIDU-12345 (−90.000đ), Thanh toán: 120.000đ`. Điểm dự kiến vẫn +10 vì điểm tính trên tổng tiền trước voucher (R1). Voucher vẫn ở trạng thái Chưa sử dụng (R2).

**Bước 15.** KH click nút **Thanh toán**.

**Bước 16.** Hệ thống ghi voucher VC-VIDU-12345 và số tiền thanh toán 120.000đ vào hóa đơn HD001 (vẫn Chờ thanh toán) và hiển thị giao diện xác nhận thanh toán: thông tin hóa đơn, danh sách vé và số tiền 120.000đ.

**Bước 17.** KH click **Xác nhận thanh toán** và thực hiện thanh toán trên cổng thanh toán.

**Bước 18.** Cổng thanh toán báo thành công. Hệ thống cập nhật hóa đơn → Đã thanh toán, chuyển các vé A3, B3 → Đã bán, chuyển voucher VC-VIDU-12345 → Đã sử dụng, cộng **+10 điểm** cho KH (từ 150 lên 160 điểm).

| **Hóa đơn** | HD001 |
| :--- | :--- |
| **Ngày** | 12:03 25/09/2026 |
| **Khách hàng** | Nguyễn Văn A |
| **Phim** | Mai – SC03 – 27/09/2026 – 19:00–21:11 – Phòng 1 (2D) |
| **Vé** | A3 Ghế Thường 90.000đ; B3 Ghế VIP 120.000đ |
| **Tổng tiền** | 210.000đ |
| **Voucher** | VC-VIDU-12345 (−90.000đ) |
| **Thanh toán** | 120.000đ |
| **Điểm tích lũy** | +10 |
| **Trạng thái** | Đã thanh toán |

**Bước 19.** Hệ thống thông báo **"Đặt vé thành công!"**, hiển thị mã hóa đơn, thông tin vé điện tử có mã QR và quay về giao diện chính.

### 3.2. Ngoại lệ

| # | Điều kiện | Hành vi |
| :--- | :--- | :--- |
| 2 | Không có phim nào đang mở bán suất chiếu | Thông báo không có phim mở bán, quay về giao diện chính |
| 11 | Quá 300 giây kể từ bước 11 mà chưa thanh toán thành công | Hóa đơn → Hết hạn, hủy các vé đang giữ (ghế về trạng thái trống), thông báo hết hạn giữ ghế; KH chọn lại ghế (R5) |
| 12 | KH click Quay lại tại giao diện thanh toán | Hóa đơn → Đã hủy, hủy các vé đang giữ (nhả ghế), quay về giao diện chọn ghế |
| 14 | Voucher không hợp lệ (không thuộc KH, đã sử dụng, hết hạn, giá trị giảm vượt tổng tiền, voucher vé phim cho suất không phải 2D, hoặc quà bắp nước) | Thông báo lý do, giữ nguyên tổng tiền; KH nhập mã khác hoặc thanh toán không dùng voucher (R2, R3) |

---

## 4. Kịch bản đổi điểm thưởng lấy quà tặng/vé (§2.1.3, 9 bước)

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Use case** | Đổi điểm thưởng lấy quà tặng/vé |
| **Actor** | Khách hàng |
| **Tiền điều kiện** | Đã đăng nhập thành công vào tài khoản thành viên |
| **Hậu điều kiện** | Điểm bị trừ đúng định mức; tồn kho giảm đi; mã voucher được cấp cho KH; phiếu đổi điểm được lưu |
| **Qui tắc** | Giao dịch nguyên tử: lỗi ở bất kỳ bước nào → rollback toàn bộ |

### 4.1. Kịch bản chính

**Bước 1–2.** Sau đăng nhập, KH chọn chức năng Đổi điểm thưởng. Giao diện hiện thông tin thành viên, số điểm hiện tại (**250 điểm**) và danh mục quà/vé:

| TT | Mã | Tên phần thưởng | Loại | Điểm đổi | Tồn kho | Mô tả | Chọn |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | PT01 | Bắp rang bơ phô mai | Bắp nước | 50 | 120 | Bắp ngọt cỡ vừa phủ phô mai | đổi |
| 2 | PT02 | Vé xem phim 2D Standard | Vé phim | 100 | 45 | Áp dụng xem phim 2D tất cả các ngày | đổi |
| 3 | PT03 | Combo (1 Bắp + 1 Nước) | Bắp nước | 80 | 60 | 1 bắp vừa + 1 nước ngọt 22oz | đổi |
| 4 | PT04 | Voucher giảm giá 50.000đ | Voucher | 60 | 0 | Giảm trực tiếp trên hóa đơn vé | hết hàng |

**Bước 3.** KH chọn phần quà **Vé xem phim 2D Standard (PT02, cần 100 điểm)**. Hệ thống kiểm tra còn hàng và KH đủ điểm.

**Bước 4.** Giao diện xác nhận đổi điểm (modal popup), hiển thị bảng tóm tắt:

| **Phần thưởng đổi** | Vé xem phim 2D Standard (Mã PT02) |
| :--- | :--- |
| **Điểm tích lũy hiện có** | 250 điểm |
| **Số điểm quy đổi trừ vào tài khoản** | −100 điểm |
| **Số điểm tích lũy còn lại sau đổi** | 150 điểm |
| **Thời hạn sử dụng voucher** | 30 ngày kể từ ngày đổi điểm |

Nút Xác nhận / Hủy.

**Bước 5.** KH click **Xác nhận đổi**.

**Bước 6.** Hệ thống kiểm tra lại tại đúng thời điểm ghi nhận: điểm tích lũy hiện tại và tồn kho của phần thưởng.

**Bước 7.** Hệ thống ghi nhận giao dịch trong một lần (lỗi bất kỳ bước nào thì hoàn tác toàn bộ): trừ 100 điểm, giảm tồn kho PT02 **45 → 44**, sinh mã voucher điện tử ngẫu nhiên có hạn sử dụng 30 ngày và lập phiếu đổi điểm:

| **Phiếu** | PDT-001 |
| :--- | :--- |
| **Khách hàng** | Nguyễn Văn A |
| **Ngày** | 11:03 25/09/2026 |
| **Phần thưởng đổi** | Vé xem phim 2D Standard (Mã PT02) |
| **Điểm tích lũy hiện có** | 250 điểm |
| **Số điểm quy đổi trừ vào tài khoản** | −100 điểm |
| **Số điểm tích lũy còn lại sau đổi** | 150 điểm |
| **Thời hạn sử dụng voucher** | 30 ngày kể từ ngày đổi điểm |

**Bước 8.** Hệ thống thông báo **"Đổi điểm thành công!"** và hiển thị mã quà tặng điện tử: **VC-VIDU-12345**.

**Bước 9.** Hệ thống quay lại giao diện đổi điểm ở bước 2; số điểm KH cập nhật thành **150 điểm**.

### 4.2. Ngoại lệ

| # | Điều kiện | Hành vi |
| :--- | :--- | :--- |
| 3.a | Phần quà đã hết hàng (VD PT04, tồn kho 0) | Thông báo phần quà không khả dụng, KH ở lại bước 2 chọn quà khác |
| 3.b | Số điểm KH < số điểm đổi của phần quà | Thông báo không đủ điểm, không mở giao diện xác nhận; KH ở lại bước 2 |
| 6 | Tại thời điểm ghi nhận, điểm không còn đủ hoặc phần quà vừa hết | Không trừ điểm, không giảm tồn kho, không tạo voucher, không lưu phiếu; thông báo đổi quà không thành công, quay lại bước 2 |

---

## 5. Trích lớp thực thể (§3 bc.docx)

### 5.1. Mô tả hệ thống
Hệ thống quản lý rạp chiếu phim cho phép khách chưa có tài khoản đăng ký thành viên, KH đặt vé trực tuyến và đổi điểm thưởng. Khi đăng ký thành công, hệ thống tạo tài khoản và hồ sơ khách hàng. Khi đặt vé, khách hàng chọn phim, suất chiếu theo ngày, khung giờ và phòng chiếu, sau đó chọn ghế theo loại ghế; hệ thống lập hóa đơn và vé cho từng ghế đã chọn, giữ các ghế trong 300 giây. KH có thể áp dụng một voucher rồi chuyển sang cổng thanh toán; khi thanh toán thành công trong thời gian giữ ghế, hệ thống ghi nhận vé đã bán, cập nhật voucher và cộng điểm. Khi đổi điểm, khách hàng chọn quà tặng; hệ thống kiểm tra điểm trên khách hàng và số lượng tồn kho; nếu hợp lệ thì trừ điểm, giảm tồn kho và phát hành voucher khách hàng.

### 5.2. Lớp thực thể

| Lớp | Thuộc tính | Ghi chú |
| :--- | :--- | :--- |
| `ThanhVien` | username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro | |
| `KhachHang` ◁ `ThanhViet` | maKhachHang, tongDiem, hangThanhVien, ngayDangKy | `tongDiem` = điểm tích lũy/thưởng |
| `Phim` | tenPhim, theLoai, daoDien, thoiLuong, doTuoi, moTa, ngayKhoiChieu, ngayKetThuc | |
| `KhungGio` | gioBatDau, gioKetThuc | |
| `PhongChieu` | maPhong, tenPhong, loaiPhong, sucChua | |
| `SuatChieu` | ngayChieu, giaCoSo, trangThai | |
| `Ghe` | soGhe, hangGhe | |
| `LoaiGhe` | tenLoai, phuThu | Loại ghế: Thường, VIP, Đôi-Sweetbox |
| `Ve` | giaVe, trangThaiVe | |
| `HoaDon` | ngayTao, tongTien, trangThai | `tongTien` dẫn xuất, không lưu cột ở thiết kế |
| `QuaTang` | tenQua, loaiQua, moTa, soDiem, soLuong, giaTriGiam | 3 lớp con: `VoucherGiamGia`/`VoucherVeTang`/`VoucherVatPham` |
| `VoucherKhachHang` | maVoucher, ngayNhan, ngaySuDung, trangThai | Quà/voucher thực tế phát cho KH sau đổi điểm; hạn = ngayNhan + 30 ngày, không lưu cột |

> Bỏ qua: Hệ thống, giao diện, chức năng, thông tin (chung chung); KH chưa có tài khoản (chưa có dữ liệu lưu); Cổng thanh toán (hệ thống bên ngoài). Phiếu đổi điểm = thông tin `VoucherKhachHang`.
> Sơ đồ `hinh-2-04-lop-thuc-the` thuộc bc.docx, không có trong repo.

### 5.3. Quan hệ (phân tích)

- `KhachHang` kế thừa `ThanhVien`
- `Phim 1—n SuatChieu`; `KhungGio 1—n SuatChieu`; `PhongChieu 1—n SuatChieu`; `PhongChieu 1◆—n Ghe`; `LoaiGhe 1—n Ghe`
- `SuatChieu` + `Ghe` là n—n → trung gian `Ve`
- `KhachHang 1—n HoaDon`; `HoaDon 1—n Ve`; `HoaDon 0..1—0..1 VoucherKhachHang`; `KhachHang` + `QuaTang` n—n → trung gian `VoucherKhachHang`

---

## 6. Biểu đồ trạng thái (§4 bc.docx)

Trạng thái = mỗi giao diện chính. Sơ đồ: `diagrams/puml/hinh-2-05*` (đăng ký), `hinh-2-06*` (đặt vé), `hinh-2-07*` (đổi điểm).

### 6.1. Đăng ký
Trạng thái: Giao diện chính → Giao diện đăng ký tài khoản → Thông báo nhập lại thông tin → Thông báo thông tin đã tồn tại → Thông báo đăng ký thành công.
Chuyển đổi: click Đăng ký → trang đăng ký; Quay lại → về trang chính; Đăng ký → kiểm tra: không hợp lệ → báo nhập lại → về trang đăng ký; hợp lệ nhưng đã tồn tại → báo đã tồn tại → về trang đăng ký; chưa tồn tại → báo thành công → kết thúc.

### 6.2. Đặt vé
Trạng thái: Trang chính KH → Chọn phim (+ thông báo phim không khả dụng) → Chọn khung giờ → Chọn suất chiếu → Chọn ghế (+ thông báo ghế không khả dụng) → Thanh toán (+ thêm voucher, thông báo voucher không hợp lệ) → Kiểm tra thời gian thanh toán → Xác nhận thanh toán (+ thông báo thất bại) → Thông báo đặt vé thành công.
Chuyển đổi: chọn phim → khung giờ → suất chiếu → ghế (ghế không khả dụng → báo → quay lại chọn ghế); Tiếp tục → thanh toán; dùng voucher → thêm voucher (không hợp lệ → báo → quay lại thêm voucher); kiểm tra thời gian: hết hạn → quay lại thanh toán, hợp lệ → xác nhận; xác nhận: thất bại → báo thất bại → quay lại xác nhận, thành công → báo đặt vé thành công → kết thúc.

### 6.3. Đổi điểm
Trạng thái: Trang chính KH → Chọn quà (+ thông báo quà không khả dụng) → Xác nhận đổi quà (+ thông báo không đủ điểm) → Thông báo voucher đổi thành công.
Chuyển đổi: chọn quà → không khả dụng → báo → quay lại chọn quà; khả dụng → xác nhận đổi; kiểm tra điểm: không đủ → báo không đủ điểm → quay lại xác nhận; đủ → báo đổi thành công → kết thúc.

---

## 7. Biểu đồ lớp đầy đủ pha phân tích (§5 bc.docx) — phương thức nghiệp vụ

Sơ đồ lớp phân tích cho từng module. Tên thông điệp phải trùng bảng này.

### 7.1. Đăng ký

- `ThanhVien.kiemTraThongTin()` — Input: thông tin đăng ký; Output: boolean
- `ThanhVien.kiemTraEmailSDT(email, sdt)` — Output: boolean
- `KhachHang.taoHoSo(thongTinThanhVien)` — Output: `KhachHang` mới với điểm ban đầu = 0

### 7.2. Đặt vé

- `ThanhVien.kiemTraDangNhap(username, password) : boolean`
- `Phim.laydsPhim() : List<Phim>`; `KhungGio.laydsKhungGio(phim, ngay) : List<KhungGio>`; `SuatChieu.laydsSuatChieu(phim, ngay, khungGio)`
- `Ve.laydsVe(suatChieu)`; `Ve.kiemTraVe(suatChieu, ghe)`; `Ve.tinhTienVe(suatChieu, ghe)` = giaCoSo + phuThu
- `HoaDon.taoHoaDon(khachHang, suatChieu, dsGhe)` → HoaDon CHO_THANH_TOAN + các Ve DANG_GIU
- `HoaDon.tinhTongTien(dsVe)`; `VoucherKhachHang.kiemTraVoucher(maVoucher, khachHang)`; `HoaDon.capNhatTongTien(voucher)`
- `HoaDon.kiemTraHetHan(hd) : boolean`
- `HoaDon.xacNhanThanhToan(hd)`; `Ve.phatHanhVe(hd)`; `KhachHang.congDiem(kh, soDiem)`; `VoucherKhachHang.apDungVoucher(voucher, hd)`
- `HoaDon.huyHoaDon()` + `Ve.huyVe()` khi Quay lại hoặc hết hạn

### 7.3. Đổi điểm

- `QuaTang.laydsQuaTang()`; `KhachHang.kiemTraDiem(kh, soDiem)`; `QuaTang.kiemTraSoLuong(qua)`
- `KhachHang.truDiem(kh, soDiem)`; `QuaTang.capNhatSoLuong(qua)`; `VoucherKhachHang.taoVoucher(kh, qua)`

> Ghi chú triển khai (ARCHITECTURE.md): chữ ký DAO đầy đủ theo bản `_fix` — `taoHoaDon(hd, dsVe)`, `tinhTongTien(dsVe, voucher?)`, `kiemTraHetHan(idHoaDon)`, ghi `GiaoDichDoiDiem` trước rồi `luuVoucher(vc)`, `VeDAO.kiemTraVe/laydsVeConHieuLuc/capNhatTrangThaiVe`, `QuaTangDAO.capNhatSoLuong(idQua, soLuong)`. Bỏ `phatHanhVe`, `capNhatTongTien`, `timHoaDonTheoIdVe`.

---

## 8. Biểu đồ giao tiếp pha phân tích — Kịch bản V2 (§6 bc.docx)

Mỗi kịch bản V2 là danh sách thông điệp tuần tự (lớp biên → điều khiển → thực thể). Sơ đồ: `hinh-2-11*`, `hinh-2-12*`, `hinh-2-13*`.

### 8.1. Đăng ký (V2, 16 thông điệp)

1. KH chọn chức năng đăng ký trên `GDChinhKhachHang`. 2. `GDChinhKhachHang` gọi `GDDangKyTaiKhoan`. 3. `GDDangKyTaiKhoan` hiển thị giao diện. 4. KH nhập thông tin, click Đăng ký. 5. `GDDangKyTaiKhoan` gọi `ThanhVien.kiemTraThongTin()`. 6–7. kiểm tra, trả kết quả. 8. gọi `ThanhVien.kiemTraEmailSDT()`. 9–10. kiểm tra trùng lặp, trả kết quả. 11. gọi `ThanhVien` tạo tài khoản. 12. `ThanhVien` gọi `KhachHang` tạo hồ sơ. 13. `KhachHang.taoHoSo()`. 14–16. trả kết quả, thông báo thành công.

### 8.2. Đặt vé (V2, 64 thông điệp)

Đường chính: chọn chức năng đặt vé → `laydsPhim()` → hiển thị → chọn phim → `laydsKhungGio(phim, ngay)` → chọn ngày/giờ → `laydsSuatChieu(phim, ngay, khungGio)` → chọn suất → `laydsVe(suatChieu)` → hiển thị sơ đồ → chọn ghế → `kiemTraVe` → `tinhTienVe` → hiển thị ghế đã chọn/giá → chọn thanh toán → `taoHoaDon()` → `tinhTongTien()` → hiển thị → nhập voucher → `kiemTraVoucher()` → `capNhatTongTien()` → hiển thị số tiền sau giảm → xác nhận → `kiemtraHetHan()` → `xacNhanThanhToan()` → `phatHanhVe()` → `congDiem()` → `apDungVoucher()` → trả kết quả → thông báo đặt vé thành công. (Chi tiết từng dòng 1–64 như bc.docx §6, mục Đặt vé.)

### 8.3. Đổi điểm (V2, 26 thông điệp)

Chọn chức năng đổi quà → `laydsQuaTang()` → hiển thị danh sách → chọn quà → `kiemTraDiem()` → `kiemTraSoLuong()` → `GDXacNhanDoiQua` → hiển thị thông tin → xác nhận → `truDiem()` → `capNhatSoLuong()` → `taoVoucher()` → thông báo đổi thưởng thành công.

---

## 9. Thiết kế lớp thực thể (§7 bc.docx)

- **Bước 1:** thêm `id` vào mọi lớp không kế thừa (tất cả trừ `KhachHang`).
- **Bước 2:** kiểu dữ liệu — định danh, số lượng, điểm, tiền (VNĐ): `int`; chuỗi, loại, trạng thái: `String`; ngày, giờ: `Date`.
- **Bước 3:** chuyển association thành aggregation/composition: `SuatChieu`+`Ghe` → `Ve`; `KhachHang`+`QuaTang` → `VoucherKhachHang`; `Phim`+`KhungGio`+`PhongChieu` → `SuatChieu`.
- **Bước 4:** thuộc tính đối tượng theo quan hệ thành phần: `SuatChieu` có 1 `Phim`, 1 `KhungGio`, 1 `PhongChieu`; `PhongChieu` có `ArrayList<Ghe>`; `Ghe` có 1 `LoaiGhe`; `Ve` có 1 `SuatChieu`, 1 `Ghe`; `HoaDon` có `ArrayList<Ve>`, 1 `KhachHang`, 1 `VoucherKhachHang` (0..1); `VoucherKhachHang` có 1 `KhachHang`, 1 `QuaTang`; giữ `KhachHang ◁ ThanhVien`.
- Sơ đồ: `hinh-2-04` (pha phân tích) + ảnh gốc `hinh-3-01-*` (thiết kế); cả hai thuộc bc.docx, không có trong repo.

> Ghi chú triển khai: ở pha thiết kế, `VoucherKhachHang` chứa `giaoDichDoiDiem` (bỏ `khachHang`/`quaTang` trực tiếp) và `GiaoDichDoiDiem` là 1 lớp riêng (phiếu PDT-001).

---

## 10. Thiết kế CSDL (§8 bc.docx)

- **Bước 1:** mỗi lớp thực thể → 1 bảng `tbl<Tên lớp>`: `tblThanhVien`, `tblKhachHang`, `tblPhim`, `tblKhungGio`, `tblPhongChieu`, `tblSuatChieu`, `tblLoaiGhe`, `tblGhe`, `tblVe`, `tblHoaDon`, `tblQuaTang`, `tblVoucherKhachHang` (+ `tblGiaoDichDoiDiem` ở triển khai, xem ARCHITECTURE.md §3).
- **Bước 2:** thuộc tính không phải đối tượng → cột của bảng tương ứng (vd `tblThanhVien: id, username, password, hoTen, ngaySinh, gioiTinh, diaChi, email, sdt, vaiTro`; `tblHoaDon: id, ngayTao, hetHan, trangThai`; `tblVoucherKhachHang: id, maVoucher, ngayNhan, ngaySuDung, trangThai`).
- **Bước 3:** quan hệ 1–n giữa các bảng theo quan hệ lớp (1 `tblThanhVien` – 1 `tblKhachHang`; 1 `tblKhachHang` – n `tblHoaDon`/`tblVoucherKhachHang`; 1 `tblQuaTang` – n `tblVoucherKhachHang`; 1 `tblVoucherKhachHang` – 1 `tblHoaDon`; 1 `tblHoaDon` – n `tblVe`; 1 `tblSuatChieu`/`tblGhe` – n `tblVe`; 1 `tblPhim`/`tblKhungGio`/`tblPhongChieu` – n `tblSuatChieu`; 1 `tblPhongChieu` – n `tblGhe`; 1 `tblLoaiGhe` – n `tblGhe`).
- **Bước 4:** khóa chính = `id` (trừ `tblKhachHang` dùng khóa ngoại `tblThanhVienid` làm khóa chính); khóa ngoại theo quan hệ số lượng (vd `tblHoaDon` có FK `tblKhachHang…id` + `tblVoucherKhachHangid`; `tblVe` có FK `tblHoaDonid`, `tblSuatChieuid`, `tblGheid`…).
- **Bước 5:** loại thuộc tính dẫn xuất: tổng tiền hóa đơn (tính từ giá vé + giảm voucher), hạn sử dụng voucher (ngayNhan + 30 ngày) → **không lưu thành cột**.
- Sơ đồ: ảnh gốc `diagrams/chuong-3/hinh-3-02-csdl.jpeg`.

---

## 11. Thiết kế giao diện + biểu đồ lớp chi tiết (§9 bc.docx)

Công nghệ: Frontend **TypeScript (ts) → React `GD*.tsx`** (chi tiết ARCHITECTURE.md §6); Backend **Java Spring Boot**, DB **MySQL**.

### 11.1. Danh sách trang (đã chốt — 12 trang React)

| Trang | Vai trò / lớp biên |
| :--- | :--- |
| `DangNhap.tsx` | `btnDangKy`; đăng nhập qua `POST /api/auth/login`; click → `GDDangKyTaiKhoan` |
| `GDDangKyTaiKhoan.tsx` | `inHoTen, inNgaySinh, inGioiTinh, inSodienthoai, inemail, inDiaChi, inusername, inpassword, inxacnhanmatkhau`; `outThongBao`; `btnDangKy` |
| `GDKhachHangHome.tsx` | `btnDatVe`, `btnDoiDiemThuong` (`kh` ẩn) |
| `GDChonPhim.tsx` | `btnTimKiem`, `tblListPhim`, `chonPhim` |
| `GDKhungGio.tsx` | `ngayChieu` (Select), `tblListKhungGio`, `chonKhungGio` (`phim`, `kh` ẩn) |
| `GDSuatChieu.tsx` | `tblListSuatChieu`, `chonSuatChieu` (`kh`, `phim`, `kg` ẩn) |
| `GDChonGhe.tsx` | `tblListGhe`, `chonGhe`, `btnTiepTuc`, `btnQuayLai` (`kh`, `sc` ẩn) |
| `GDThanhToan.tsx` | `btnVoucher`, `btnApDungVC`, `btnThanhToan`, `btnQuayLai`; đồng hồ 5 phút (`kh`, `sc`, `ghe[]` ẩn) |
| `GDXacNhanThanhToan.tsx` | `btnQuayLai` (`hd`, `kh` ẩn) |
| `GDChonQua.tsx` | `btnTimKiem`, `tblListQua`, `chonQua`, `tblLichSuDoiQua` (lịch sử phiếu đổi quà của khách đang đăng nhập) |
| `GDXacNhanDoiQua.tsx` | `btnXacNhan`, `btnQuayLai`, `outThongBao` (`kh`, `qt` ẩn) |
| `GDThongBao.tsx` | `outPDQ`, `btnXacNhan` (`vc` ẩn) |

Thuộc tính tường minh/ẩn của từng trang (in/out/sub) liệt kê đầy đủ ở bc.docx §9 cho từng module.

**Bước 2 (đã triển khai):** chỉ 3 trang `DangNhap.tsx`, `GDDangKyTaiKhoan.tsx`, `GDKhachHangHome.tsx`.
Đăng ký gọi `POST /api/auth/register` với body
`{ "kh": { "username", "password", "hoTen", "ngaySinh": "YYYY-MM-DD", "gioiTinh", "diaChi", "email", "sdt" }, "xacNhanMatKhau" }`;
đăng nhập gọi `POST /api/auth/login` với `{ "username", "password" }`. Phiên do backend giữ
(`HttpSession.khachHangId`); FE chỉ giữ `KhachHang` trong React state, không lưu storage, reload quay lại `DangNhap`.
Mã HTTP/thông điệp lỗi chi tiết: ARCHITECTURE.md §7.1. 9 trang còn lại thuộc bước đặt vé/đổi điểm.

### 11.2. Tầng DAO triển khai — hợp đồng ở ARCHITECTURE.md §5

- `DAO`: cung cấp `JdbcTemplate` dùng chung cấu hình `DataSource` của Spring; kết nối quản lý theo transaction.
- `ThanhVienDAO`: `kiemTraThongTin`, `kiemTraEmailSDT`, `kiemTraDangNhap(ThanhVien tv)`.
- `KhachHangDAO`: `taoHoSo`, `kiemTraDiem`, `truDiem`, `congDiem`.
- `PhimDAO`: `laydsPhim` · `KhungGioDAO`: `laydsKhungGio` · `SuatChieuDAO`: `laydsSuatChieu`.
- `VeDAO`: `laydsGhe(idSuatChieu)`, `tinhTienVe(...): Ve`, `kiemTraVe`, `laydsVeConHieuLuc`, `capNhatTrangThaiVe`, `huyVe`.
- `HoaDonDAO`: `taoHoaDon(hd, dsVe)`, `tinhTongTien(dsVe, voucher?)`, `kiemTraHetHan(idHoaDon)`, `xacNhanThanhToan`, `huyHoaDon(idHoaDon)` · `VoucherKhachHangDAO`: `kiemTraVoucher`, `apDungVoucher`, `luuVoucher(vc)` · `GiaoDichDoiDiemDAO`: `luuGiaoDich(gd)`, `layLichSuTheoKhachHang(idKhachHang)` · `QuaTangDAO`: `laydsQuaTang`, `kiemTraSoLuong`, `capNhatSoLuong(idQua, soLuong)`.

#### 11.2.1. Thiết kế DAO đặt vé theo sơ đồ

Bản trình bày theo ảnh chỉ gồm `DAO` (`-con : Connection`, `+DAO()`) và 8 DAO con:
`ThanhVienDAO`, `KhachHangDAO`, `PhimDAO`, `KhungGioDAO`, `SuatChieuDAO`, `VeDAO`, `HoaDonDAO`, `VoucherDAO`.
Mỗi DAO con có constructor công khai tương ứng; giữ các phương thức trong ảnh và bổ sung kiểu còn thiếu.
`VeDAO.laydsGhe(idSuatChieu : int) : List<Ghe>` đảm nhiệm truy vấn toàn bộ ghế trong phòng của suất chiếu; không có lớp `GheDAO`.
Kết hợp với `VeDAO.laydsVe(idSuatChieu : int) : List<Ve>` để hiển thị cả ghế trống và ghế đang giữ/đã bán.
Không thêm `themVe`, `luuVe`, `capNhatTrangThaiVe`, `tinhTongTien` hoặc `kiemTraHetHan` vào hình.

Danh sách chữ ký, trách nhiệm và bảng đối chiếu với Java nằm ở [ARCHITECTURE.md §5.1](ARCHITECTURE.md#51-thiết-kế-dao-đặt-vé-theo-sơ-đồ).
Nguồn [PlantUML](diagrams/puml/dao-dat-ve-theo-so-do.puml) và ảnh [SVG](diagrams/out/dao-dat-ve-theo-so-do.svg)
chỉ mô tả phần DAO đặt vé, không phải sơ đồ đầy đủ của giao diện/thực thể hoặc của chức năng đổi điểm.

`taoHoaDon()` và `huyHoaDon()` là chữ ký trình bày rút gọn; không cho phép DAO tự đọc session hoặc lưu
“hóa đơn hiện tại”. Hợp đồng triển khai ở §11.2 và `ARCHITECTURE.md §5` giữ nguyên.
Tạo hóa đơn cùng vé phải nguyên tử; xác nhận thanh toán, bán vé, sử dụng voucher và cộng điểm cũng phải
trong một transaction, dù trên hình là các phương thức riêng.

### 11.3. Tầng thực thể & phụ trợ

- Thực thể: đăng ký dùng `ThanhVien, KhachHang`; đặt vé dùng `ThanhVien, KhachHang, Phim, KhungGio, SuatChieu, PhongChieu, Ghe, LoaiGhe, Ve, HoaDon, VoucherKhachHang, QuaTang`; đổi điểm dùng `ThanhVien, KhachHang, QuaTang, VoucherKhachHang` (+ `GiaoDichDoiDiem`).
- Phụ trợ: `HTTPClient.fetch()` (FE → BE), `RESTController.requestMapping()` (BE → DAO).

---

## 12. Thiết kế biểu đồ hoạt động (§10 bc.docx)

Mỗi lớp biên → 1 activity; mỗi action → 1 phương thức DAO; gọi qua `HTTPClient.fetch()` + `RESTController.requestMapping()`. Sơ đồ: `hinh-3-20`, `hinh-3-21`, `hinh-3-22` (bản `_fix` trong `diagrams/`).

### 12.1. Đăng ký
`DangNhap.tsx` hiển thị → click Đăng ký → `GDDangKyTaiKhoan.tsx` → nhận thông tin đăng ký → `ThanhVienDAO.kiemTraThongTin()` (+ `kiemTraEmailSDT(email, sdt)`) → nếu hợp lệ → `KhachHangDAO.taoHoSo()` → báo thành công → Xác nhận → `DangNhap.tsx`. (Không hợp lệ / đã tồn tại → báo tại `outThongBao`, quay lại nhập.)

### 12.2. Đặt vé
`GDKhachHangHome.tsx` (session `kh`) → click Đặt vé → `GDChonPhim.tsx` (`PhimDAO.laydsPhim()`, hiển thị, chọn phim lưu session) → `GDKhungGio.tsx` (`KhungGioDAO.laydsKhungGio()`, chọn ngày + khung giờ lưu session) → `GDSuatChieu.tsx` (`SuatChieuDAO.laydsSuatChieu(idPhim, ngayChieu, idKhungGio)`, chọn suất lưu session) → `GDChonGhe.tsx` (`VeDAO.laydsVe/laydsVeConHieuLuc(idSuatChieu)`, chọn ghế → `VeDAO.kiemTraVe` → `VeDAO.tinhtienVe(suatChieu, ghe)`, Tiếp tục → `HoaDonDAO.taoHoaDon()`, lưu `hoaDon` vào session) → `GDThanhToan.tsx` (`HoaDonDAO.tinhTongTien(dsVe)`, đồng hồ 5 phút; áp voucher → `VoucherDAO.kiemTraVoucher` → `apDungVoucher`, không hợp lệ → báo; Thanh toán → `HoaDonDAO.kiemtraHetHan(hd)`: hết hạn → báo + `huyHoaDon()`, còn hạn → `xacNhanThanhToan(idHoaDon)`) → `GDXacNhanThanhToan.tsx` (hiển thị hóa đơn đã thanh toán) → Quay lại.

### 12.3. Đổi điểm
`GDKhachHangHome.tsx` → click Đổi điểm → `GDChonQua.tsx` (`QuaTangDAO.laydsQuaTang()` + `GiaoDichDoiDiemDAO.layLichSuTheoKhachHang(idKhachHang)`, hiển thị điểm + quà + lịch sử phiếu đổi quà; chọn quà → `QuaTangDAO.kiemTraSoLuong(idQua)` + `KhachHangDAO.kiemTraDiem(maKhachHang, diem)`: còn hàng + đủ điểm → `GDXacNhanDoiQua.tsx`, không → báo không khả dụng/không đủ điểm) → `GDXacNhanDoiQua.tsx` (hiển thị giao dịch; Quay lại → `GDChonQua.tsx`; Xác nhận đổi → kiểm tra lại điểm + tồn kho, không hợp lệ → báo tại `outThongBao` → quay lại; hợp lệ → `truDiem` → `capNhatSoLuong` → `luuGiaoDich(gd)` → `luuVoucher(vc)` — một giao dịch) → `GDThongBao` (báo thành công, mã phiếu + voucher) → Xác nhận → `GDChonQua.tsx` (tải lại điểm, tồn kho và lịch sử phiếu đổi quà).

Hợp đồng đọc lịch sử (issue #13, chi tiết ARCHITECTURE.md §7.2): `GET /api/doi-diem/lich-su`
trả `{success: true, data: VoucherKhachHang[]}`; `GET /api/quatang` trả thêm `lichSuDoiQua`
cùng danh mục và số dư. Lọc duy nhất theo `HttpSession.khachHangId`, bỏ qua id khách do client gửi;
chưa đăng nhập trả HTTP 401 với `{success: false, message}`. Nối phiếu → quà và voucher → phiếu,
xếp `thoiGian DESC, id DESC`; mỗi dòng hiện mã phiếu, ngày đổi, mã/tên quà, `soDiemTru`,
`diemConLai` tại lúc đổi, mã/trạng thái/ngày dùng voucher và hạn `ngayNhan + 30 ngày`.
Phiếu rollback không xuất hiện; chưa đổi quà hiện “Chưa có lịch sử đổi quà.” ngay trong `GDChonQua`.


---

## 13. Thiết kế biểu đồ tuần tự — Kịch bản V3 (§11 bc.docx)

Sơ đồ: `hinh-3-23` (đăng ký), `hinh-3-24a/b/c` (đặt vé tách 3 phần), `hinh-3-25` (đổi điểm). Mỗi thông điệp là một bước gọi trong kịch bản V3 (xem bc.docx §11 cho danh sách từng bước).

### 13.1. Đăng ký (20 bước)
Khách click Đăng ký trên `DangNhap.tsx` → `DangNhap.ts → GDDangKyTaiKhoan.ts` → hiển thị → KH nhập thông tin, click Đăng ký → `GDDangKyTaiKhoan.ts → KhachHang()` khởi tạo đối tượng → trả `kh` → `HTTPClient.fetch(kh)` → `RESTController.requestMapping(kh)` → `ThanhVienDAO.kiemTraThongTin(kh)` → `kiemTraEmailSDT(email, sdt)` → `KhachHangDAO.taoHoSo(kh)` → `KHD → KHE : lưu(kh)` → trả kết quả → `GDDangKyTaiKhoan.tsx` thông báo thành công.

### 13.2. Đặt vé (chia `hinh-3-24a/b/c`)
- **24a** (chọn phim/khung giờ/suất chiếu): `GDChonPhim.ts → HTTPClient.fetch() → RESTController.requestMapping() → PhimDAO.laydsPhim()` → trả `List<Phim>` → hiển thị → chọn phim → `KhungGioDAO.laydsKhungGio()` → chọn ngày/khung giờ → `laydsSuatChieu(idPhim, ngayChieu, idKhungGio)` → hiển thị suất chiếu.
- **24b** (chọn ghế, tạo HoaDon/Ve): `VeDAO.laydsGhe(idSuatChieu)` + `VeDAO.laydsVeConHieuLuc(suatChieu)` → hiển thị sơ đồ ghế → loop mỗi ghế: `VeDAO.kiemTraVe(idSuatChieu, idGhe)` → `VeDAO.tinhtienVe(suatChieu, ghe)` → `new Ve(...)` → thêm vào `dsVe` → click Tiếp tục → `new HoaDon(kh, dsVe, CHO_THANH_TOAN, hetHan)` → `HoaDonDAO.taoHoaDon(hd, dsVe)` (một giao dịch: kiểm tra `(suất, ghế)`, lưu HoaDon + Ve) → chuyển `GDThanhToan.tsx`.
- **24c** (thanh toán/voucher/xác nhận): `HoaDonDAO.tinhTongTien(dsVe)` → hiển thị → nhập mã voucher, click Áp dụng → `VoucherDAO.kiemTraVoucher` → `apDungVoucher` → `tinhTongTien(dsVe, voucher)` → hiển thị số tiền sau giảm → click Xác nhận thanh toán → `kiemTraHetHan(idHoaDon)` → `xacNhanThanhToan(idHoaDon)` → `VeDAO.capNhatTrangThaiVe(idHoaDon, DA_BAN)` → `KhachHangDAO.congDiem` → `GDXacNhanThanhToan.tsx` thông báo đặt vé thành công.

### 13.3. Đổi điểm (73 bước, `hinh-3-25`)
`DangNhap.tsx` đăng nhập (`ThanhVienDAO.kiemTraDangNhap()`) → lưu session KhachHang → `GDKhachHangHome.tsx` → click Đổi điểm → `GDChonQua.tsx` → `QuaTangDAO.laydsQuaTang()` → hiển thị điểm + quà → chọn quà → `kiemTraDiem` + `kiemTraSoLuong` → `GDXacNhanDoiQua.tsx` → hiển thị giao dịch → click Xác nhận → kiểm tra lại → `truDiem` → `capNhatSoLuong(idQua, soLuong)` → **`new GiaoDichDoiDiem(kh, qt, soDiemTru, diemConLai, thoiGian)` → `GiaoDichDoiDiemDAO.luuGiaoDich(gd)` (ghi phiếu trước)** → `new VoucherKhachHang(gd, maVoucher, ngayNhan, trangThai)` → `VoucherKhachHangDAO.luuVoucher(vc)` → trả kết quả → `GDThongBao.tsx` thông báo đổi điểm thành công.

---

## 14. Điểm khác so với bc.docx gốc (triển khai theo ARCHITECTURE.md)

| Nội dung bc.docx | Triển khai (đã chốt) |
| :--- | :--- |
| Frontend `*.ts` | React `GD*.tsx` (12 trang) |
| Backend "RESTController" chung + `HTTPClient.fetch()` | 1 `RESTController` (`@RestController` duy nhất, gộp Auth/Booking/Reward) ở backend; `HTTPClient.fetch()` chỉ ở frontend |
| 12 bảng, không có `GiaoDichDoiDiem` | 13 bảng: thêm `tblGiaoDichDoiDiem` (lịch sử đổi quà) |
| 3 lớp con ghi gộp nhưng mô tả ít | 1 bảng `tblQuaTang` giữ `loaiQua` + `giaTriGiam`; vẫn đủ 3 lớp con `VoucherGiamGia`/`VoucherVeTang`/`VoucherVatPham` |
| Chữ ký DAO rỗng (`taoHoSo()`…) | Chữ ký `_fix` đầy đủ (xem §7.2, ARCHITECTURE.md §5) |
| `phatHanhVe`, `capNhatTongTien` | Bỏ; dùng `capNhatTrangThaiVe`, `tinhTongTien(dsVe, voucher?)` |
| Điểm quy đổi chỉ có ví dụ | R1: 1 điểm / 20.000đ trước voucher |
| Hình 3.24 quá mờ | Tách 3.24a/b/c |
| Mật khẩu thô | Băm BCrypt (lưu) |
| `DangNhap` chỉ chuyển trang (không gọi backend) | Gọi `POST /api/auth/login` thật, nhận `KhachHang` + `HttpSession.khachHangId` |

Lệnh chạy/kiểm thử: xem ARCHITECTURE.md §11 — `docker compose up --build` (mysql/backend/frontend, FE 5173),
`cd backend && mvn test`, `cd frontend && npm run build`.

Quyết định 08/11/2026 ([issue #3](https://github.com/22imer/PTTK-CineMan/issues/3)): lịch sử đổi quà dùng
`tblGiaoDichDoiDiem` hiện có (không thêm bảng lịch sử trùng lặp); `giaTriGiam` nằm chung `tblQuaTang`,
bỏ bảng tách `tblVoucherGiamGia`; schema đúng là **13 bảng / 14 khóa ngoại**; vẫn giữ 3 lớp con `QuaTang`.
