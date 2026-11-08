import { useEffect, useRef, useState } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { KhachHang, QuaTang, VoucherKhachHang } from '../types';
import { dinhDangNgay, dinhDangTien, hanVoucher, maQuaTang, thongDiepLoi } from '../utils';
import FlowFrame from './FlowFrame';

interface GDXacNhanDoiQuaProps {
  khachHang: KhachHang;
  quaTang: QuaTang;
  tongDiem: number;
  onDoiThanhCong: (voucher: VoucherKhachHang) => void;
  onQuayLai: () => void;
}

function homNayIso(): string {
  const homNay = new Date();
  const thang = String(homNay.getMonth() + 1).padStart(2, '0');
  const ngay = String(homNay.getDate()).padStart(2, '0');
  return `${homNay.getFullYear()}-${thang}-${ngay}`;
}

export default function GDXacNhanDoiQua({
  khachHang,
  quaTang,
  tongDiem,
  onDoiThanhCong,
  onQuayLai,
}: GDXacNhanDoiQuaProps) {
  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState('');

  const loiRef = useRef<HTMLParagraphElement>(null);
  const duDiem = tongDiem >= quaTang.soDiem;
  const conHang = quaTang.soLuong > 0;
  const diemConLai = tongDiem - quaTang.soDiem;

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  async function xacNhanDoi() {
    if (dangGui || !duDiem || !conHang) {
      return;
    }

    setDangGui(true);
    setLoi('');
    try {
      const voucher = await HTTPClient.fetch<VoucherKhachHang>('/api/doi-diem', {
        method: 'POST',
        body: JSON.stringify({ quaTangId: quaTang.id }),
      });
      onDoiThanhCong(voucher);
    } catch (error) {
      setLoi(
        thongDiepLoi(
          error,
          'Không thể đổi quà. Điểm hoặc tồn kho có thể vừa thay đổi; vui lòng quay lại chọn lại.',
        ),
      );
    } finally {
      setDangGui(false);
    }
  }

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Đổi điểm · Xác nhận"
      title="Xác nhận đổi quà"
      description="Kiểm tra lại quà và điểm trước khi xác nhận. Máy chủ sẽ kiểm tra lại điểm, tồn kho và ghi phiếu trong một giao dịch."
    >
      <section className="flow-card" aria-labelledby="tieuDeXacNhanDoiQua">
        <div className="section-head">
          <h2 id="tieuDeXacNhanDoiQua">Tóm tắt đổi điểm</h2>
          <p className="section-note">{quaTang.moTa || 'Quà tặng thành viên CineMan'}</p>
        </div>

        <p
          className="form-alert"
          id="outThongBao"
          role="alert"
          tabIndex={-1}
          ref={loiRef}
          aria-live="assertive"
        >
          {loi}
        </p>

        {!duDiem ? (
          <p className="form-alert form-alert--static" role="alert">
            Bạn còn thiếu {(quaTang.soDiem - tongDiem).toLocaleString('vi-VN')} điểm để đổi quà này.
          </p>
        ) : null}

        {!conHang ? (
          <p className="form-alert form-alert--static" role="alert">
            Quà tặng này đã hết hàng. Vui lòng chọn quà khác.
          </p>
        ) : null}

        <dl className="totals">
          <div>
            <dt>Quà tặng</dt>
            <dd>
              {quaTang.tenQua} ({maQuaTang(quaTang.id)})
            </dd>
          </div>
          <div>
            <dt>Điểm hiện có</dt>
            <dd>{tongDiem.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div>
            <dt>Điểm trừ</dt>
            <dd>−{quaTang.soDiem.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div className="total-line--grand">
            <dt>Điểm còn lại</dt>
            <dd>{Math.max(0, diemConLai).toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div>
            <dt>Hạn voucher dự kiến</dt>
            <dd>{hanVoucher(homNayIso())} (30 ngày kể từ ngày nhận)</dd>
          </div>
          {quaTang.loaiQua === 'GIAM_GIA' && quaTang.giaTriGiam ? (
            <div>
              <dt>Giá trị giảm</dt>
              <dd>{dinhDangTien(quaTang.giaTriGiam)}</dd>
            </div>
          ) : null}
          <div>
            <dt>Ngày nhận dự kiến</dt>
            <dd>{dinhDangNgay(homNayIso())}</dd>
          </div>
        </dl>

        <p className="section-note">
          Sau khi xác nhận, hệ thống tạo phiếu đổi điểm và voucher mới. Nếu bất kỳ bước ghi nào thất bại,
          điểm và tồn kho được hoàn nguyên.
        </p>

        <div className="flow-actions">
          <button
            className="btn btn--primary"
            id="btnXacNhan"
            type="button"
            disabled={dangGui || !duDiem || !conHang}
            aria-busy={dangGui}
            onClick={() => void xacNhanDoi()}
          >
            {dangGui ? 'Đang đổi quà…' : 'Xác nhận đổi quà'}
          </button>
          <button
            className="btn btn--ghost"
            id="btnQuayLai"
            type="button"
            disabled={dangGui}
            onClick={onQuayLai}
          >
            Chọn quà khác
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
