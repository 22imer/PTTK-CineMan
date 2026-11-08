import { Screen } from '../ui/common';
import type { DoiDiemUI } from '../ui/models';

export interface GDThongBaoProps {
  giaoDich: DoiDiemUI;
  onVeDanhMuc: () => void;
}

/**
 * DT-03 — Đổi điểm thành công.
 * Phiếu và voucher do lớp điều khiển truyền vào; màn này không tạo giao dịch,
 * không cấp voucher và không tự tính điểm.
 */
export default function GDThongBao({ giaoDich, onVeDanhMuc }: GDThongBaoProps) {
  return (
    <Screen title="Đổi điểm thành công!">
      <section className="reward-receipt" id="outPDQ" aria-label="Phiếu đổi điểm">
        <p className="receipt-meta">
          <span>
            Phiếu: <strong>{giaoDich.maPhieu}</strong>
          </span>
          <span>Ngày: {giaoDich.thoiGian}</span>
        </p>

        <dl className="summary-list">
          <div>
            <dt>Khách hàng</dt>
            <dd>{giaoDich.khachHang}</dd>
          </div>
          <div>
            <dt>Phần thưởng đổi</dt>
            <dd>
              {giaoDich.qua.ten}{' '}
              <span className="muted">(Mã {giaoDich.qua.ma})</span>
            </dd>
          </div>
          <div>
            <dt>Điểm tích lũy trước khi đổi</dt>
            <dd>{giaoDich.diemTruoc} điểm</dd>
          </div>
          <div>
            <dt>Số điểm quy đổi bị trừ</dt>
            <dd>−{giaoDich.diemTru} điểm</dd>
          </div>
          <div className="total-row">
            <dt>Số điểm còn lại</dt>
            <dd>
              <strong>{giaoDich.diemSau} điểm</strong>
            </dd>
          </div>
        </dl>
      </section>

      <section className="voucher-card" aria-label="Voucher điện tử">
        <p className="muted">Mã voucher điện tử</p>
        <p className="voucher-code" id="vc">
          {giaoDich.maVoucher}
        </p>
      </section>

      <p className="reward-validity">
        Thời hạn sử dụng voucher: <strong>{giaoDich.hanSuDung}</strong>
      </p>
      <p className="muted">
        Hệ thống quay lại danh mục phần thưởng với số điểm đã cập nhật.
      </p>

      <div className="actions">
        <button type="button" className="btn btn--primary" id="btnXacNhan" onClick={onVeDanhMuc}>
          Về danh mục phần thưởng
        </button>
      </div>
    </Screen>
  );
}
