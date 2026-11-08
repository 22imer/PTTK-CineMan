import { useEffect, useRef, useState, type FormEvent } from 'react';
import { HTTPClient, HTTPError } from '../api/httpClient';
import type { KhachHang, TomTatHoaDon } from '../types';
import {
  dinhDangDemNguoc,
  dinhDangGio,
  dinhDangNgay,
  dinhDangNgayGio,
  dinhDangTien,
  maHoaDon,
  maPhim,
  maSuatChieu,
  nhanGhe,
  nhanTrangThai,
  thongDiepLoi,
} from '../utils';
import FlowFrame from './FlowFrame';

interface GDThanhToanProps {
  khachHang: KhachHang;
  tomTatBanDau: TomTatHoaDon;
  onDaThanhToan: (tomTat: TomTatHoaDon) => void;
  /** Chỉ gọi sau khi DELETE hóa đơn thành công (hoặc hóa đơn đã hết hạn/hủy). */
  onQuayLai: () => void;
}

/**
 * Đếm ngược dựa trên serverNow + thời gian trôi đơn điệu (performance.now).
 * Không dùng đồng hồ hệ điều hành/trình duyệt làm nguồn quyết định và không tự hủy hóa đơn.
 */
function useDemNguoc(han: string, serverNow: string): number | null {
  const [conLai, setConLai] = useState<number | null>(null);

  useEffect(() => {
    const hanMs = Date.parse(han);
    const serverMs = Date.parse(serverNow);
    if (!Number.isFinite(hanMs) || !Number.isFinite(serverMs)) {
      setConLai(null);
      return;
    }

    const moc = performance.now();
    const tinh = () => {
      const daTroi = performance.now() - moc;
      setConLai(Math.max(0, (hanMs - serverMs - daTroi) / 1000));
    };
    tinh();
    const ma = window.setInterval(tinh, 250);
    return () => window.clearInterval(ma);
  }, [han, serverNow]);

  return conLai;
}

export default function GDThanhToan({
  khachHang,
  tomTatBanDau,
  onDaThanhToan,
  onQuayLai,
}: GDThanhToanProps) {
  const [tomTat, setTomTat] = useState<TomTatHoaDon>(tomTatBanDau);
  const [maVoucher, setMaVoucher] = useState('');
  const [thanhCong, setThanhCong] = useState(true);
  const [dangApDung, setDangApDung] = useState(false);
  const [dangThanhToan, setDangThanhToan] = useState(false);
  const [dangHuy, setDangHuy] = useState(false);
  const [loi, setLoi] = useState('');
  const [thongBao, setThongBao] = useState('');

  const loiRef = useRef<HTMLParagraphElement>(null);
  const voucherRef = useRef<HTMLInputElement>(null);
  const conLai = useDemNguoc(tomTat.hoaDon.hetHan, tomTat.serverNow);
  const daHetHan = conLai !== null && conLai <= 0;

  const hoaDon = tomTat.hoaDon;
  const dsVe = Array.isArray(hoaDon.dsVe) ? hoaDon.dsVe : [];
  const giamGia = Math.max(0, tomTat.tongTienTruocVoucher - tomTat.tongTien);
  const dangXuLy = dangApDung || dangThanhToan || dangHuy;
  const suatChieu = dsVe[0]?.suatChieu;
  const voucherDaApDung = hoaDon.voucher?.maVoucher || '';

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  async function apDungVoucher(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (dangXuLy) {
      return;
    }

    const ma = maVoucher.trim();
    if (!ma) {
      setLoi('Vui lòng nhập mã voucher trước khi áp dụng.');
      return;
    }

    setDangApDung(true);
    setLoi('');
    setThongBao('');
    try {
      const moi = await HTTPClient.fetch<TomTatHoaDon>('/api/hoadon/apdung-voucher', {
        method: 'POST',
        body: JSON.stringify({ hoaDonId: hoaDon.id, maVoucher: ma }),
      });
      setTomTat(moi);
      setMaVoucher(ma);
      setThongBao(
        `Đã áp dụng voucher ${ma}. Voucher chỉ được tính khi thanh toán thành công trong thời gian giữ ghế.`,
      );
    } catch (error) {
      // Không thay đổi tổng tiền đang hiển thị khi voucher không hợp lệ.
      setLoi(
        thongDiepLoi(
          error,
          'Không thể áp dụng voucher. Vui lòng kiểm tra mã và thử lại.',
        ),
      );
    } finally {
      setDangApDung(false);
    }
  }

  async function xacNhanThanhToan() {
    if (dangXuLy) {
      return;
    }

    setDangThanhToan(true);
    setLoi('');
    setThongBao('');
    try {
      const moi = await HTTPClient.fetch<TomTatHoaDon>(
        `/api/hoadon/${encodeURIComponent(String(hoaDon.id))}/thanh-toan`,
        {
          method: 'POST',
          body: JSON.stringify({ thanhCong }),
        },
      );
      onDaThanhToan(moi);
    } catch (error) {
      setLoi(
        thongDiepLoi(
          error,
          thanhCong
            ? 'Không thể xác nhận thanh toán. Hóa đơn vẫn được giữ trong thời hạn, bạn có thể thử lại.'
            : 'Thanh toán giả lập thất bại. Hóa đơn và ghế vẫn được giữ trong thời hạn, bạn có thể thử lại.',
        ),
      );
    } finally {
      setDangThanhToan(false);
    }
  }

  async function huyVaQuayLai() {
    if (dangXuLy) {
      return;
    }

    setDangHuy(true);
    setLoi('');
    setThongBao('');
    try {
      const ketQua = await HTTPClient.fetch<TomTatHoaDon>(
        `/api/hoadon/${encodeURIComponent(String(hoaDon.id))}`,
        { method: 'DELETE' },
      );
      if (ketQua.hoaDon.trangThai === 'DA_THANH_TOAN') {
        onDaThanhToan(ketQua);
        return;
      }
      onQuayLai();
    } catch (error) {
      // Hóa đơn không còn: máy chủ đã nhả ghế và trả 404. Vẫn rời trang để chọn lại.
      if (error instanceof HTTPError && error.httpStatus === 404) {
        onQuayLai();
        return;
      }
      setLoi(
        thongDiepLoi(
          error,
          'Không thể nhả ghế cho hóa đơn này. Vui lòng thử lại trước khi quay lại chọn ghế.',
        ),
      );
      setDangHuy(false);
    }
  }

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 5 · Thanh toán"
      title="Hoàn tất hóa đơn giữ ghế"
      description="Máy chủ đang giữ ghế cho bạn trong 5 phút. Giá, điểm và hạn giữ đều do máy chủ quyết định."
    >
      <div className="flow-grid">
        <section className="flow-card" aria-labelledby="tieuDeHoaDon">
          <div className="section-head">
            <h2 id="tieuDeHoaDon">Thông tin hóa đơn</h2>
            <p className="section-note">
              Mã hóa đơn {maHoaDon(hoaDon.id)} · Tạo lúc {dinhDangNgayGio(hoaDon.ngayTao)} · Trạng thái{' '}
              {nhanTrangThai(hoaDon.trangThai)}
            </p>
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

          <div className="table-scroll">
            <table className="data-table" id="tblListVeHoaDon">
              <caption className="sr-only">Ghế và giá vé trong hóa đơn</caption>
              <thead>
                <tr>
                  <th scope="col">Ghế</th>
                  <th scope="col">Loại ghế</th>
                  <th scope="col">Giá vé</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {dsVe.map((ve) => (
                  <tr key={ve.id}>
                    <th scope="row">{nhanGhe(ve.ghe)}</th>
                    <td>{ve.ghe.loaiGhe?.tenLoai || '—'}</td>
                    <td>{dinhDangTien(ve.giaVe)}</td>
                    <td>{nhanTrangThai(ve.trangThaiVe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <dl className="totals">
            <div>
              <dt>Tổng tiền trước voucher</dt>
              <dd>{dinhDangTien(tomTat.tongTienTruocVoucher)}</dd>
            </div>
            <div>
              <dt>Giảm giá voucher</dt>
              <dd>−{dinhDangTien(giamGia)}</dd>
            </div>
            <div className="total-line--grand">
              <dt>Tổng thanh toán</dt>
              <dd>{dinhDangTien(tomTat.tongTien)}</dd>
            </div>
            <div>
              <dt>Điểm cộng sau thanh toán</dt>
              <dd>+{tomTat.diemCong.toLocaleString('vi-VN')} điểm</dd>
            </div>
            <div>
              <dt>Điểm hiện có</dt>
              <dd>{tomTat.tongDiem.toLocaleString('vi-VN')} điểm</dd>
            </div>
          </dl>

          <p className="countdown" id="dongHo" aria-live="off">
            Thời gian giữ ghế còn lại (ước tính — máy chủ quyết định):{' '}
            <strong>
              {conLai === null ? 'Đang đồng bộ…' : dinhDangDemNguoc(conLai)}
            </strong>
          </p>
          {daHetHan ? (
            <p className="form-alert" role="status">
              Đồng hồ ước tính đã về 0. Bạn vẫn có thể thử thanh toán; máy chủ sẽ kiểm tra hạn giữ ghế
              và từ chối nếu hóa đơn đã hết hạn. Bấm “Quay lại sơ đồ ghế” để nhả ghế và chọn lại.
            </p>
          ) : null}
        </section>

        <section className="flow-card" aria-labelledby="tieuDeVoucher">
          <div className="section-head">
            <h2 id="tieuDeVoucher">Voucher và thanh toán</h2>
            <button
              className="link-button"
              id="btnVoucher"
              type="button"
              onClick={() => voucherRef.current?.focus()}
              disabled={dangXuLy}
            >
              Nhập mã voucher
            </button>
            <p className="section-note">
              Tối đa một voucher cho mỗi hóa đơn. Voucher chỉ bị tiêu khi thanh toán thành công.
            </p>
          </div>

          {voucherDaApDung ? (
            <p className="form-notice">
              Voucher đang gắn với hóa đơn: <strong>{voucherDaApDung}</strong>
            </p>
          ) : null}

          <form className="voucher-form" onSubmit={(event) => void apDungVoucher(event)}>
            <div className="field">
              <label htmlFor="inoutVoucher">Mã voucher</label>
              <input
                id="inoutVoucher"
                ref={voucherRef}
                type="text"
                value={maVoucher}
                onChange={(event) => setMaVoucher(event.target.value)}
                placeholder="Ví dụ: VC-VIDU-12345"
                autoComplete="off"
                spellCheck={false}
                disabled={dangXuLy}
              />
            </div>
            <button
              className="btn btn--primary"
              id="btnApDungVC"
              type="submit"
              disabled={dangXuLy}
              aria-busy={dangApDung}
            >
              {dangApDung ? 'Đang áp dụng…' : 'Áp dụng voucher'}
            </button>
          </form>

          <p
            className="form-alert"
            role="alert"
            tabIndex={-1}
            ref={loiRef}
            aria-live="assertive"
          >
            {loi}
          </p>

          {thongBao ? (
            <p className="form-notice" role="status">
              {thongBao}
            </p>
          ) : null}

          <fieldset className="mock-fieldset" disabled={dangXuLy}>
            <legend>Kết quả thanh toán (giả lập)</legend>
            <label className="radio-row">
              <input
                type="radio"
                name="ketQuaThanhToan"
                value="thanh-cong"
                checked={thanhCong}
                onChange={() => setThanhCong(true)}
              />
              Thanh toán thành công
            </label>
            <label className="radio-row">
              <input
                type="radio"
                name="ketQuaThanhToan"
                value="that-bai"
                checked={!thanhCong}
                onChange={() => setThanhCong(false)}
              />
              Thanh toán thất bại (giữ hóa đơn để thử lại)
            </label>
          </fieldset>

          <div className="flow-actions flow-actions--stack">
            <button
              className="btn btn--primary"
              id="btnThanhToan"
              type="button"
              disabled={dangXuLy}
              aria-busy={dangThanhToan}
              onClick={() => void xacNhanThanhToan()}
            >
              {dangThanhToan ? 'Đang xác nhận…' : `Thanh toán ${dinhDangTien(tomTat.tongTien)}`}
            </button>
            <button
              className="btn btn--ghost"
              id="btnQuayLai"
              type="button"
              disabled={dangXuLy}
              aria-busy={dangHuy}
              onClick={() => void huyVaQuayLai()}
            >
              {dangHuy ? 'Đang nhả ghế…' : 'Quay lại sơ đồ ghế'}
            </button>
          </div>
          <p className="section-note">
            Quay lại sẽ hủy hóa đơn và nhả ghế trên máy chủ. Thất bại giả lập không nhả ghế; bạn có thể
            chọn lại “Thanh toán thành công” để thử lại trong thời hạn.
          </p>
        </section>
      </div>
    </FlowFrame>
  );
}
