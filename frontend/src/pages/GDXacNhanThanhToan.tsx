import { useMemo } from 'react';
import { renderSVG } from 'uqr';
import type { KhachHang, TomTatHoaDon } from '../types';
import {
  dinhDangGio,
  dinhDangNgay,
  dinhDangNgayGio,
  dinhDangTien,
  maHoaDon,
  maPhim,
  maSuatChieu,
  maVe,
  nhanGhe,
  nhanTrangThai,
} from '../utils';
import FlowFrame from './FlowFrame';

interface GDXacNhanThanhToanProps {
  khachHang: KhachHang;
  tomTat: TomTatHoaDon;
  onVeTrangChu: () => void;
}

/** QR thật (SVG mã hóa nội dung vé), không phải hình trang trí. */
function MaQrVe({ noiDung, moTa }: { noiDung: string; moTa: string }) {
  const src = useMemo(() => {
    const svg = renderSVG(noiDung, { ecc: 'M', border: 4 });
    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }, [noiDung]);

  return <img className="qr-image" src={src} alt={moTa} width={148} height={148} />;
}

export default function GDXacNhanThanhToan({
  khachHang,
  tomTat,
  onVeTrangChu,
}: GDXacNhanThanhToanProps) {
  const hoaDon = tomTat.hoaDon;
  const dsVe = Array.isArray(hoaDon.dsVe) ? hoaDon.dsVe : [];
  const suatChieu = dsVe[0]?.suatChieu;
  const giamGia = Math.max(0, tomTat.tongTienTruocVoucher - tomTat.tongTien);
  const daThanhToan = hoaDon.trangThai === 'DA_THANH_TOAN';

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 6 · Xác nhận thanh toán"
      title="Đặt vé thành công"
      description="Mỗi vé có mã và mã QR riêng. Vui lòng xuất trình mã QR tại quầy hoặc cổng soát vé."
    >
      <section className="flow-card" aria-labelledby="tieuDeHoaDonDaThanhToan">
        <div className="section-head section-head--split">
          <div>
            <h2 id="tieuDeHoaDonDaThanhToan">Hóa đơn {maHoaDon(hoaDon.id)}</h2>
            <p className="section-note">
              {dinhDangNgayGio(hoaDon.ngayTao)} · Trạng thái{' '}
              <strong>{nhanTrangThai(hoaDon.trangThai)}</strong>
            </p>
          </div>
          <p className="badge badge--ok">{daThanhToan ? 'Đã thanh toán' : nhanTrangThai(hoaDon.trangThai)}</p>
        </div>

        {suatChieu ? (
          <div className="invoice-film">
            <p className="cell-title">{suatChieu.phim?.tenPhim || 'Suất chiếu'}</p>
            <p className="section-note">
              {maPhim(suatChieu.phim.id)} · {maSuatChieu(suatChieu.id)} ·{' '}
              {dinhDangGio(suatChieu.khungGio?.gioBatDau)} – {dinhDangGio(suatChieu.khungGio?.gioKetThuc)} ·{' '}
              {dinhDangNgay(suatChieu.ngayChieu)} · {suatChieu.phongChieu?.tenPhong || 'Phòng chiếu'} ·{' '}
              {suatChieu.phongChieu?.loaiPhong || '—'}
            </p>
          </div>
        ) : null}

        <dl className="totals">
          <div>
            <dt>Tổng tiền trước voucher</dt>
            <dd>{dinhDangTien(tomTat.tongTienTruocVoucher)}</dd>
          </div>
          <div>
            <dt>Giảm giá voucher</dt>
            <dd>−{dinhDangTien(giamGia)}</dd>
          </div>
          {hoaDon.voucher?.maVoucher ? (
            <div>
              <dt>Voucher</dt>
              <dd>{hoaDon.voucher.maVoucher}</dd>
            </div>
          ) : null}
          <div className="total-line--grand">
            <dt>Đã thanh toán</dt>
            <dd>{dinhDangTien(tomTat.tongTien)}</dd>
          </div>
          <div>
            <dt>Điểm được cộng</dt>
            <dd>+{tomTat.diemCong.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div>
            <dt>Tổng điểm hiện tại</dt>
            <dd>{tomTat.tongDiem.toLocaleString('vi-VN')} điểm</dd>
          </div>
        </dl>

        <h3 className="block-title">Vé của bạn</h3>
        <ul className="ticket-list" id="tblListVeDaThanhToan">
          {dsVe.map((ve) => {
            const ma = maVe(ve);
            return (
              <li className="ticket-stub" key={ve.id}>
                <div className="ticket-stub__info">
                  <p className="ticket-stub__seat">Ghế {nhanGhe(ve.ghe)}</p>
                  <p className="section-note">
                    {ve.ghe.loaiGhe?.tenLoai || 'Ghế'} · {dinhDangTien(ve.giaVe)} ·{' '}
                    {nhanTrangThai(ve.trangThaiVe)}
                  </p>
                  <p className="ticket-stub__code">
                    Mã vé: <strong id={`maVe-${ve.id}`}>{ma}</strong>
                  </p>
                </div>
                <div className="ticket-stub__qr">
                  <MaQrVe noiDung={ma} moTa={`Mã QR cho vé ${ma} ghế ${nhanGhe(ve.ghe)}`} />
                  <span className="section-note">Quét mã tại rạp</span>
                </div>
              </li>
            );
          })}
        </ul>

        <div className="flow-actions">
          <button className="btn btn--primary" id="btnQuayLai" type="button" onClick={onVeTrangChu}>
            Về trang chủ
          </button>
        </div>
        <p className="section-note">
          Điểm thành viên mới đã được máy chủ cập nhật. Trang chủ sẽ hiển thị số điểm này.
        </p>
      </section>
    </FlowFrame>
  );
}
