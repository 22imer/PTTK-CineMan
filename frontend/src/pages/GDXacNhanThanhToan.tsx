import { BookingMeta, DataTable, Notice, Screen, SeatLines, money } from '../ui/common';
import type { GheUI, HoaDonUI } from '../ui/models';

export interface GDXacNhanThanhToanProps {
  hoaDon: HoaDonUI;
  onXacNhan: () => void;
}

export interface GDKetQuaThanhToanProps {
  hoaDon: HoaDonUI;
  onVeTrangChu: () => void;
}

const NHAN_TRANG_THAI: Record<HoaDonUI['trangThai'], string> = {
  CHO_THANH_TOAN: 'Chờ thanh toán',
  DA_THANH_TOAN: 'Đã thanh toán',
  HET_HAN: 'Hết hạn',
  THAT_BAI: 'Thất bại',
  DA_HUY: 'Đã hủy',
};

const NHAN_TRANG_THAI_VE: Record<GheUI['trangThai'], string> = {
  TRONG: 'Trống',
  DANG_GIU: 'Đang giữ',
  DA_BAN: 'Đã bán',
};

/** Lý do chặn xác nhận thanh toán theo trạng thái hóa đơn do lớp điều khiển cung cấp. */
function lyDoChan(trangThai: HoaDonUI['trangThai']): string {
  switch (trangThai) {
    case 'HET_HAN':
      return 'Hóa đơn đã hết hạn giữ ghế nên không thể xác nhận thanh toán. Vui lòng chọn lại ghế.';
    case 'THAT_BAI':
      return 'Thanh toán trước đó không thành công nên hóa đơn không thể xác nhận thanh toán.';
    case 'DA_HUY':
      return 'Hóa đơn đã được hủy nên không thể xác nhận thanh toán.';
    case 'DA_THANH_TOAN':
      return 'Hóa đơn này đã được thanh toán trước đó.';
    default:
      return '';
  }
}

/**
 * VE-07 — Xác nhận trước khi sang cổng thanh toán.
 * Đây là hóa đơn chưa thanh toán, không phải thông báo đặt vé thành công.
 */
export default function GDXacNhanThanhToan({ hoaDon, onXacNhan }: GDXacNhanThanhToanProps) {
  const thongBao = lyDoChan(hoaDon.trangThai);
  const biKhoa = thongBao !== '';

  return (
    <Screen title="Xác nhận thanh toán">
      {biKhoa ? (
        <Notice tone={hoaDon.trangThai === 'THAT_BAI' ? 'error' : 'warning'}>{thongBao}</Notice>
      ) : (
        <Notice tone="info">
          Hóa đơn chưa được thanh toán. Xác nhận để chuyển sang cổng thanh toán.
        </Notice>
      )}

      <dl className="summary-list">
        <div>
          <dt>Hóa đơn</dt>
          <dd>{hoaDon.ma}</dd>
        </div>
        <div>
          <dt>Trạng thái</dt>
          <dd>
            <span className="status-pill">{NHAN_TRANG_THAI[hoaDon.trangThai]}</span>
          </dd>
        </div>
        <div>
          <dt>Khách hàng</dt>
          <dd>{hoaDon.khachHang}</dd>
        </div>
      </dl>

      <BookingMeta suatChieu={hoaDon.suatChieu} />

      <SeatLines seats={hoaDon.dsVe} />

      <dl className="summary-list">
        <div>
          <dt>Tổng tiền vé</dt>
          <dd className="numeric">{money(hoaDon.tongTien)}</dd>
        </div>
        {hoaDon.maVoucher ? (
          <div>
            <dt>Voucher</dt>
            <dd>
              {hoaDon.maVoucher} (−{money(hoaDon.giamGia)})
            </dd>
          </div>
        ) : null}
        <div className="total-row">
          <dt>Số tiền thanh toán</dt>
          <dd className="numeric">{money(hoaDon.thanhToan)}</dd>
        </div>
      </dl>

      <div className="actions">
        <button type="button" className="btn btn--primary" onClick={onXacNhan} disabled={biKhoa}>
          Xác nhận thanh toán
        </button>
      </div>
    </Screen>
  );
}

/**
 * VE-08 — Kết quả sau cổng thanh toán.
 * Chỉ hiển thị thành công khi hóa đơn đã ở trạng thái Đã thanh toán; mã QR chỉ lấy từ dữ liệu.
 */
export function GDKetQuaThanhToan({ hoaDon, onVeTrangChu }: GDKetQuaThanhToanProps) {
  if (hoaDon.trangThai !== 'DA_THANH_TOAN') {
    return (
      <Screen title="Kết quả thanh toán">
        <Notice tone="error">
          Chưa ghi nhận thanh toán thành công cho hóa đơn {hoaDon.ma}. Trạng thái hiện tại:{' '}
          {NHAN_TRANG_THAI[hoaDon.trangThai]}. Không có vé điện tử nào được phát hành.
        </Notice>

        <dl className="summary-list">
          <div>
            <dt>Hóa đơn</dt>
            <dd>{hoaDon.ma}</dd>
          </div>
          <div>
            <dt>Trạng thái</dt>
            <dd>
              <span className="status-pill">{NHAN_TRANG_THAI[hoaDon.trangThai]}</span>
            </dd>
          </div>
        </dl>

        {hoaDon.trangThai === 'HET_HAN' ? (
          <Notice tone="warning">Thời gian giữ ghế đã hết. Vui lòng quay lại chọn ghế.</Notice>
        ) : null}

        <div className="actions">
          <button type="button" className="btn btn--ghost" onClick={onVeTrangChu}>
            Về trang chủ
          </button>
        </div>
      </Screen>
    );
  }

  return (
    <Screen title="Đặt vé thành công!">
      <Notice tone="success">
        Hóa đơn {hoaDon.ma} đã được thanh toán. Vé điện tử đã được phát hành.
      </Notice>

      <dl className="summary-list">
        <div>
          <dt>Hóa đơn</dt>
          <dd>{hoaDon.ma}</dd>
        </div>
        <div>
          <dt>Ngày</dt>
          <dd>{hoaDon.thoiGian}</dd>
        </div>
        <div>
          <dt>Khách hàng</dt>
          <dd>{hoaDon.khachHang}</dd>
        </div>
        <div>
          <dt>Trạng thái</dt>
          <dd>
            <span className="status-pill">{NHAN_TRANG_THAI[hoaDon.trangThai]}</span>
          </dd>
        </div>
      </dl>

      <BookingMeta suatChieu={hoaDon.suatChieu} />

      <div id="tblListVe">
        <DataTable caption="Danh sách vé điện tử của hóa đơn">
          <thead>
            <tr>
              <th scope="col">TT</th>
              <th scope="col">Hàng</th>
              <th scope="col">Số ghế</th>
              <th scope="col">Loại ghế</th>
              <th scope="col">Trạng thái vé</th>
              <th scope="col" className="numeric">Đơn giá</th>
            </tr>
          </thead>
          <tbody>
            {hoaDon.dsVe.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty-state">
                  Hóa đơn chưa có vé nào.
                </td>
              </tr>
            ) : (
              hoaDon.dsVe.map((ve, index) => (
                <tr key={ve.id}>
                  <td>{index + 1}</td>
                  <th scope="row">{ve.hang}</th>
                  <td>{ve.so}</td>
                  <td>{ve.loai}</td>
                  <td>{NHAN_TRANG_THAI_VE[ve.trangThai]}</td>
                  <td className="numeric">{money(ve.donGia)}</td>
                </tr>
              ))
            )}
          </tbody>
        </DataTable>
      </div>

      <dl className="summary-list">
        <div>
          <dt>Tổng tiền vé</dt>
          <dd className="numeric">{money(hoaDon.tongTien)}</dd>
        </div>
        {hoaDon.maVoucher ? (
          <div>
            <dt>Voucher</dt>
            <dd>
              {hoaDon.maVoucher} (−{money(hoaDon.giamGia)})
            </dd>
          </div>
        ) : null}
        <div className="total-row">
          <dt>Đã thanh toán</dt>
          <dd className="numeric">{money(hoaDon.thanhToan)}</dd>
        </div>
      </dl>
      <p>
        Điểm tích lũy: <strong className="numeric">+{hoaDon.diemDuKien}</strong>
      </p>
      <p>
        Tổng điểm sau thanh toán: <strong className="numeric">{hoaDon.tongDiemSau} điểm</strong>
      </p>

      <figure className="ticket-qr">
        <figcaption>Vé điện tử</figcaption>
        <p className="muted">Vùng mã QR</p>
        {hoaDon.qrSrc ? (
          <img src={hoaDon.qrSrc} alt={`Mã QR vé điện tử của hóa đơn ${hoaDon.ma}`} />
        ) : (
          <p className="muted">Hóa đơn chưa có ảnh mã QR. Dùng mã vé bên dưới.</p>
        )}
        <div>
          <p>Mã vé điện tử</p>
          {hoaDon.maVe.length === 0 ? (
            <p className="muted">Chưa có mã vé.</p>
          ) : (
            <ul>
              {hoaDon.maVe.map((ma) => (
                <li key={ma}>{ma}</li>
              ))}
            </ul>
          )}
        </div>
      </figure>

      <p className="muted">Hệ thống gửi mã vé điện tử và quay về giao diện chính.</p>

      <div className="actions">
        <button type="button" className="btn btn--primary" onClick={onVeTrangChu}>
          Về trang chủ
        </button>
      </div>
    </Screen>
  );
}
