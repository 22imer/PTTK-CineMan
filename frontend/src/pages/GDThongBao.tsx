import type { KhachHang, VoucherKhachHang } from '../types';
import {
  dinhDangNgay,
  dinhDangNgayGio,
  hanVoucher,
  maPhieuDoiDiem,
  maQuaTang,
  nhanTrangThai,
} from '../utils';
import FlowFrame from './FlowFrame';

interface GDThongBaoProps {
  khachHang: KhachHang;
  voucher: VoucherKhachHang;
  /** Quay lại catalogue để đổi tiếp; App tải lại điểm tươi. */
  onDoiTiep: () => void;
  onVeTrangChu: () => void;
}

export default function GDThongBao({
  khachHang,
  voucher,
  onDoiTiep,
  onVeTrangChu,
}: GDThongBaoProps) {
  const phieu = voucher.giaoDichDoiDiem;
  const maPhieu = maPhieuDoiDiem(phieu.id);

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Đổi điểm · Hoàn tất"
      title="Đổi quà thành công"
      description="Phiếu đổi điểm và voucher mới đã được ghi nhận. Voucher có hạn 30 ngày kể từ ngày nhận."
    >
      <section className="flow-card" aria-labelledby="tieuDePhieuDoiDiem">
        <div className="section-head section-head--split">
          <div>
            <h2 id="tieuDePhieuDoiDiem">Phiếu {maPhieu}</h2>
            <p className="section-note" id="outPDQ">
              Mã phiếu: <strong>{maPhieu}</strong> · Ghi nhận lúc {dinhDangNgayGio(phieu.thoiGian)}
            </p>
          </div>
          <p className="badge badge--ok">Đã ghi nhận</p>
        </div>

        <div className="voucher-highlight">
          <p className="voucher-highlight__label">Mã voucher của bạn</p>
          <p className="voucher-highlight__code">{voucher.maVoucher}</p>
          <p className="section-note">
            Trạng thái: {nhanTrangThai(voucher.trangThai)} · Ngày nhận {dinhDangNgay(voucher.ngayNhan)} ·
            Hạn dùng {hanVoucher(voucher.ngayNhan)}
          </p>
        </div>

        <dl className="totals">
          <div>
            <dt>Quà tặng</dt>
            <dd>
              {phieu.quaTang?.tenQua || 'Quà tặng CineMan'}
              {phieu.quaTang?.id ? ` (${maQuaTang(phieu.quaTang.id)})` : ''}
            </dd>
          </div>
          <div>
            <dt>Điểm đã trừ</dt>
            <dd>−{phieu.soDiemTru.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div className="total-line--grand">
            <dt>Điểm còn lại</dt>
            <dd>{phieu.diemConLai.toLocaleString('vi-VN')} điểm</dd>
          </div>
          <div>
            <dt>Khách hàng</dt>
            <dd>{phieu.khachHang?.hoTen || khachHang.hoTen || khachHang.username}</dd>
          </div>
        </dl>

        <p className="section-note">
          Điểm còn lại đã được cập nhật trên máy chủ và hiển thị tại trang chủ. Bạn có thể đổi tiếp hoặc
          dùng voucher này cho hóa đơn vé phim phù hợp.
        </p>

        <div className="flow-actions">
          <button className="btn btn--primary" id="btnXacNhan" type="button" onClick={onDoiTiep}>
            Đổi quà khác
          </button>
          <button className="btn btn--ghost" id="btnQuayLai" type="button" onClick={onVeTrangChu}>
            Về trang chủ
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
