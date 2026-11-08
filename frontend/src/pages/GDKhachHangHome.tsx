import { useEffect, useRef } from 'react';
import type { KhachHang } from '../types';
import { dinhDangNgay } from '../utils';

interface GDKhachHangHomeProps {
  khachHang: KhachHang;
  onDatVe: () => void;
  onDoiDiemThuong: () => void;
}

export default function GDKhachHangHome({
  khachHang,
  onDatVe,
  onDoiDiemThuong,
}: GDKhachHangHomeProps) {
  const hoTen = khachHang.hoTen?.trim() || khachHang.username;
  const diem = Number.isFinite(khachHang.tongDiem) ? khachHang.tongDiem : 0;
  const hang = khachHang.hangThanhVien?.trim() || 'Chưa xếp hạng';

  const tieuDeRef = useRef<HTMLHeadingElement>(null);

  // Điều hướng bằng React state: đưa focus tới tiêu đề để trình đọc màn hình biết trang đã đổi.
  useEffect(() => {
    tieuDeRef.current?.focus();
  }, []);

  return (
    <main className="home-shell">
      <header className="home-header">
        <p className="wordmark">CineMan</p>
        <p className="home-user">Xin chào, {hoTen}</p>
      </header>

      <section className="home-hero" aria-labelledby="tieuDeTrangChu">
        <p className="home-kicker">Thẻ thành viên</p>
        <h1 className="home-title" id="tieuDeTrangChu" tabIndex={-1} ref={tieuDeRef}>
          Chào {hoTen}, suất chiếu đang chờ bạn.
        </h1>
        <p className="home-copy">
          Đây là hồ sơ thành viên của bạn tại CineMan. Điểm và hạng hiển thị bên dưới được
          lấy từ tài khoản đăng nhập.
        </p>
      </section>

      <section className="home-membership" aria-labelledby="tieuDeHoSo">
        <h2 id="tieuDeHoSo">Hồ sơ thành viên</h2>
        <dl className="member-grid">
          <div>
            <dt>Họ tên</dt>
            <dd>{hoTen}</dd>
          </div>
          <div>
            <dt>Tên đăng nhập</dt>
            <dd>{khachHang.username}</dd>
          </div>
          <div>
            <dt>Điểm tích lũy</dt>
            <dd>{diem.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div>
            <dt>Hạng thành viên</dt>
            <dd>{hang}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{khachHang.email}</dd>
          </div>
          <div>
            <dt>Số điện thoại</dt>
            <dd>{khachHang.sdt}</dd>
          </div>
          <div>
            <dt>Thành viên từ</dt>
            <dd>{dinhDangNgay(khachHang.ngayDangKy)}</dd>
          </div>
        </dl>
        {diem === 0 && !khachHang.hangThanhVien?.trim() ? (
          <p className="member-note">
            Khách hàng mới bắt đầu với 0 điểm và chưa được xếp hạng thành viên.
          </p>
        ) : null}
      </section>

      <section className="home-actions" aria-labelledby="tieuDeChucNang">
        <h2 id="tieuDeChucNang">Chức năng dành cho khách hàng</h2>
        <div className="action-list">
          <article className="action-card">
            <div>
              <h3>Đặt vé trực tuyến</h3>
              <p>Chọn phim, ngày chiếu, khung giờ, suất chiếu và ghế ngồi cho buổi xem của bạn.</p>
              <p className="action-status">Giữ ghế 5 phút và thanh toán ngay trên web.</p>
            </div>
            <button
              className="btn btn--ghost"
              id="btnDatVe"
              type="button"
              onClick={onDatVe}
            >
              Đặt vé
            </button>
          </article>

          <article className="action-card">
            <div>
              <h3>Đổi điểm thưởng</h3>
              <p>Dùng điểm tích lũy để nhận voucher giảm giá, vé phim hoặc bắp nước tại rạp.</p>
              <p className="action-status">Điểm hiện có: {diem.toLocaleString('vi-VN')} điểm.</p>
            </div>
            <button
              className="btn btn--ghost"
              id="btnDoiDiemThuong"
              type="button"
              onClick={onDoiDiemThuong}
            >
              Đổi điểm thưởng
            </button>
          </article>
        </div>
      </section>
    </main>
  );
}
