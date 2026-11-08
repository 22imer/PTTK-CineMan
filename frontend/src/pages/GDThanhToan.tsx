import { BookingMeta, Notice, Screen, SeatLines, money } from '../ui/common';
import type { HoaDonUI } from '../ui/models';

export interface GDThanhToanProps {
  hoaDon: HoaDonUI;
  voucher: string;
  onVoucherChange: (voucher: string) => void;
  voucherError?: string;
  onApDung: () => void;
  onQuayLai: () => void;
  onThanhToan: () => void;
}

const NHAN_TRANG_THAI: Record<HoaDonUI['trangThai'], string> = {
  CHO_THANH_TOAN: 'Chờ thanh toán',
  DA_THANH_TOAN: 'Đã thanh toán',
  HET_HAN: 'Hết hạn',
  THAT_BAI: 'Thất bại',
  DA_HUY: 'Đã hủy',
};

/** Lý do chặn thanh toán theo trạng thái hóa đơn do lớp điều khiển cung cấp. */
function thongBaoChan(trangThai: HoaDonUI['trangThai']): string {
  switch (trangThai) {
    case 'HET_HAN':
      return 'Hóa đơn đã hết hạn giữ ghế. Ghế đã được nhả; vui lòng chọn lại ghế.';
    case 'THAT_BAI':
      return 'Thanh toán trước đó không thành công nên hóa đơn không thể thanh toán tiếp.';
    case 'DA_HUY':
      return 'Hóa đơn đã được hủy nên không thể thanh toán.';
    case 'DA_THANH_TOAN':
      return 'Hóa đơn này đã được thanh toán.';
    default:
      return '';
  }
}

/**
 * VE-06 — Thanh toán và áp dụng voucher.
 * Số tiền, điểm và trạng thái hóa đơn đều là dữ liệu hiển thị; màn hình không tự tính toán.
 */
export default function GDThanhToan({
  hoaDon,
  voucher,
  onVoucherChange,
  voucherError,
  onApDung,
  onQuayLai,
  onThanhToan,
}: GDThanhToanProps) {
  const thongBao = thongBaoChan(hoaDon.trangThai);
  const biKhoa = thongBao !== '';

  return (
    <Screen
      title="Thanh toán"
      action={
        hoaDon.trangThai === 'CHO_THANH_TOAN' ? (
          <p className="booking-timer">
            Giữ ghế còn lại: <strong className="numeric">{hoaDon.conLai}</strong>
          </p>
        ) : undefined
      }
    >
      <p>
        Hóa đơn: <strong>{hoaDon.ma}</strong> —{' '}
        <span className="status-pill">{NHAN_TRANG_THAI[hoaDon.trangThai]}</span>
      </p>

      {biKhoa ? (
        <Notice tone={hoaDon.trangThai === 'THAT_BAI' ? 'error' : 'warning'}>{thongBao}</Notice>
      ) : null}

      <BookingMeta suatChieu={hoaDon.suatChieu} />

      <p className="muted">
        Giá cơ sở: <span className="numeric">{money(hoaDon.suatChieu.giaCoSo)}</span>
      </p>

      <SeatLines seats={hoaDon.dsVe} />

      <form
        className="inline-form"
        onSubmit={(event) => {
          event.preventDefault();
          onApDung();
        }}
        noValidate
      >
        <div className="field">
          <label htmlFor="inoutVoucher">Mã voucher</label>
          <input
            id="inoutVoucher"
            name="maVoucher"
            type="text"
            value={voucher}
            onChange={(event) => onVoucherChange(event.target.value)}
            aria-invalid={voucherError ? true : undefined}
            aria-describedby={voucherError ? 'loiVoucher' : undefined}
            autoComplete="off"
            spellCheck={false}
            disabled={biKhoa}
          />
          {voucherError ? (
            <p className="field-error" id="loiVoucher" role="alert">
              {voucherError}
            </p>
          ) : null}
        </div>
        <button id="subApDungVoucher" type="submit" className="btn btn--ghost" disabled={biKhoa}>
          Áp dụng
        </button>
      </form>

      <dl className="summary-list">
        <div>
          <dt>Tổng tiền vé trước voucher</dt>
          <dd className="numeric">{money(hoaDon.tongTien)}</dd>
        </div>
        {hoaDon.maVoucher ? (
          <div>
            <dt>
              Voucher {hoaDon.maVoucher}
              {hoaDon.moTaVoucher ? ` — ${hoaDon.moTaVoucher}` : ''}
            </dt>
            <dd className="numeric">−{money(hoaDon.giamGia)}</dd>
          </div>
        ) : null}
        <div className="total-row">
          <dt>Thanh toán</dt>
          <dd className="numeric">{money(hoaDon.thanhToan)}</dd>
        </div>
      </dl>

      <p className="muted">
        Điểm tích lũy dự kiến: <strong className="numeric">+{hoaDon.diemDuKien}</strong>
      </p>

      <div className="actions">
        <button id="subQuayLai" type="button" className="btn btn--ghost" onClick={onQuayLai}>
          Quay lại
        </button>
        <button id="subThanhToan" type="button" className="btn btn--primary" onClick={onThanhToan} disabled={biKhoa}>
          Thanh toán
        </button>
      </div>
    </Screen>
  );
}
