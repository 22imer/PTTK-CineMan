# CineMan M1

Một ngữ cảnh quản lý khách hàng thành viên, đặt vé trực tuyến và đổi điểm thưởng của rạp chiếu phim.

## Ngôn ngữ

**ThanhVien**: Tài khoản dùng để đăng nhập CineMan.
_Tránh_: dùng ThanhVien thay cho hồ sơ KhachHang.

**KhachHang**: Thành viên có hồ sơ khách hàng, số điểm tích lũy và hạng thành viên.
_Tránh_: coi tài khoản và hồ sơ khách hàng là hai từ đồng nghĩa.

**Phim**: Tác phẩm điện ảnh được rạp trình chiếu; phân loại độ tuổi là thông tin hiển thị.

**KhungGio**: Khoảng giờ bắt đầu và kết thúc dùng để chọn suất chiếu.

**SuatChieu**: Một lần chiếu phim cụ thể vào một ngày, khung giờ và phòng chiếu.

**Ghe**: Một vị trí ngồi trong phòng chiếu, thuộc một loại ghế có phụ thu riêng. Ghế Sweetbox được chọn thành cặp liền kề.

**HoaDon**: Chứng từ đặt mua từ một đến tám vé; hóa đơn chờ thanh toán đồng thời là lần giữ ghế có hạn.
_Tránh_: PhienDatVe, GiuCho như nghiệp vụ tách biệt.

**Ve**: Quyền xem một suất chiếu tại một ghế; vé đang giữ chưa phải vé đã bán.

**QuaTang**: Quà trong danh mục có giá đổi bằng điểm và số lượng tồn; gồm giảm giá, vé tặng hoặc vật phẩm nhận tại quầy.
_Tránh_: gọi quà trong danh mục là voucher đã cấp.

**GiaoDichDoiDiem**: Phiếu ghi nhận một lần đổi điểm thành công của khách hàng cho một quà tặng.
_Tránh_: thêm lịch sử đổi quà riêng cho cùng sự kiện.

**VoucherKhachHang**: Voucher cụ thể được cấp cho khách hàng từ một phiếu đổi điểm, có mã và hạn sử dụng riêng.
_Tránh_: đồng nhất với QuaTang.

**Điểm tích lũy**: Số dư dùng đổi quà; điểm từ mua vé được ghi nhận khi thanh toán thành công, tính trên tiền vé trước voucher.
