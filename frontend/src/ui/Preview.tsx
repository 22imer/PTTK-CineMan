import { useState } from 'react';
import DangNhap from '../pages/DangNhap';
import GDDangKyTaiKhoan from '../pages/GDDangKyTaiKhoan';
import GDKhachHangHome from '../pages/GDKhachHangHome';
import GDChonPhim from '../pages/GDChonPhim';
import GDKhungGio from '../pages/GDKhungGio';
import GDSuatChieu from '../pages/GDSuatChieu';
import GDChonGhe from '../pages/GDChonGhe';
import GDThanhToan from '../pages/GDThanhToan';
import GDXacNhanThanhToan, { GDKetQuaThanhToan } from '../pages/GDXacNhanThanhToan';
import GDChonQua from '../pages/GDChonQua';
import GDXacNhanDoiQua from '../pages/GDXacNhanDoiQua';
import GDThongBao from '../pages/GDThongBao';
import SiteLayout from './SiteLayout';
import { Screen } from './common';
import { doiDiemMau, gheDaChon, gheMau, hoaDonMau, hoaDonVoucher, khachMau, khungGioMau, phimMau, quaMau, suatMau } from './fixtures';
import type { HoaDonUI, ViewState } from './models';

const screens = [
  ['DK-01', 'Trang chủ chưa đăng nhập'], ['DK-02', 'Đăng ký thành viên'], ['DK-03', 'Đăng ký thành công / Đăng nhập'],
  ['VE-01', 'Trang chủ khách hàng'], ['VE-02', 'Chọn phim'], ['VE-03', 'Chọn ngày và khung giờ'],
  ['VE-04', 'Chọn suất chiếu'], ['VE-05', 'Chọn ghế'], ['VE-06', 'Thanh toán và voucher'],
  ['VE-07', 'Xác nhận trước thanh toán'], ['VE-08', 'Vé điện tử'],
  ['DT-01', 'Danh mục phần thưởng'], ['DT-02', 'Xác nhận đổi điểm'], ['DT-03', 'Kết quả đổi điểm'],
];
const lists = [['ready', 'Có dữ liệu'], ['loading', 'Đang tải'], ['empty', 'Rỗng'], ['error', 'Lỗi tải']];
const states: Record<string, string[][]> = {
  'VE-02': [...lists, ['stress', 'Tên phim dài']], 'VE-03': lists, 'VE-04': lists,
  'VE-05': [['ready', 'Chưa chọn ghế'], ['selected', 'A3 + B3'], ['sweetbox', 'Cặp Sweetbox'], ['limit', 'Quá 8 ghế'], ['conflict', 'Ghế vừa được giữ'], ['loading', 'Đang tải'], ['error', 'Lỗi tải']],
  'VE-06': [['ready', 'Chưa dùng voucher'], ['applied', 'Miễn vé A3'], ['owner', 'Voucher sai chủ'], ['used', 'Voucher đã sử dụng'], ['expired-voucher', 'Voucher hết hạn'], ['value', 'Giảm vượt tổng'], ['non2d', 'Suất không phải 2D'], ['physical', 'Voucher bắp nước'], ['expired', 'Hết 300 giây'], ['failed', 'Thanh toán thất bại'], ['cancelled', 'Đã hủy']],
  'VE-07': [['ready', 'Chờ thanh toán'], ['expired', 'Hết hạn'], ['failed', 'Thất bại']],
  'VE-08': [['ready', 'Đã thanh toán'], ['unpaid', 'Chưa thanh toán']],
  'DT-01': [...lists, ['insufficient', 'Không đủ điểm'], ['unavailable', 'Quà không khả dụng'], ['updated', 'Sau khi đổi PT02'], ['stress', 'Tên dài / không mô tả']],
  'DT-02': [['ready', 'Xác nhận'], ['busy', 'Đang ghi nhận'], ['error', 'Ghi nhận thất bại']],
};
const voucherErrors: Record<string, string> = {
  owner: 'Voucher không thuộc khách hàng đang đăng nhập.', used: 'Voucher đã được sử dụng.',
  'expired-voucher': 'Voucher đã hết hạn sử dụng.', value: 'Giá trị giảm vượt tổng tiền vé.',
  non2d: 'Voucher vé phim chỉ áp dụng cho suất chiếu 2D.', physical: 'Voucher bắp nước nhận tại quầy, không dùng để giảm tiền vé.',
};
function go(screen: string, state = 'ready') { window.location.assign(`?ui=${screen}&state=${state}`); }

/** Mỗi lựa chọn là một ví dụ tĩnh, không mô phỏng giao dịch hay gọi API nghiệp vụ. */
export default function Preview() {
  const params = new URLSearchParams(window.location.search);
  const requested = params.get('ui') || 'VE-01';
  const scene = screens.some(([code]) => code === requested) ? requested : 'VE-01';
  const availableStates = states[scene] || [['ready', 'Mặc định']];
  const requestedState = params.get('state') || 'ready';
  const state = availableStates.some(([code]) => code === requestedState) ? requestedState : 'ready';
  const listState: ViewState = state === 'loading' || state === 'empty' || state === 'error' ? state : 'ready';
  const [message, setMessage] = useState('');
  const [voucher, setVoucher] = useState(state === 'applied' || voucherErrors[state] ? 'VC-VIDU-12345' : '');
  const [dialogOpen, setDialogOpen] = useState(true);
  const action = () => setMessage('Đây là bản xem giao diện. Thao tác nghiệp vụ chưa được kết nối; dữ liệu, điểm, tồn kho và hóa đơn không thay đổi. Chọn màn hoặc trạng thái phía trên để xem ví dụ khác.');
  const hoaDon: HoaDonUI = {
    ...(state === 'applied' || scene === 'VE-07' || scene === 'VE-08' ? hoaDonVoucher : hoaDonMau),
    trangThai: state === 'expired' ? 'HET_HAN' : state === 'failed' ? 'THAT_BAI' : state === 'cancelled' ? 'DA_HUY' : 'CHO_THANH_TOAN',
    conLai: state === 'expired' ? '00:00' : '04:32',
  };
  let view;
  switch (scene) {
    case 'DK-01': view = <Screen title="Trang chủ" subtitle="Chưa đăng nhập"><a className="btn btn--primary" href="?screen=dang-ky">Đăng ký thành viên</a></Screen>; break;
    case 'DK-02': view = <GDDangKyTaiKhoan onHuy={() => go('DK-01')} onDangKyThanhCong={() => go('DK-03')} />; break;
    case 'DK-03': view = <DangNhap thongBao="Đăng ký tài khoản thành viên thành công!" onMoDangKy={() => go('DK-02')} onDangNhapThanhCong={() => window.location.assign('/')} />; break;
    case 'VE-01': view = <GDKhachHangHome khachHang={khachMau} preview onDatVe={() => go('VE-02')} onDoiDiem={() => go('DT-01')} />; break;
    case 'VE-02': view = <GDChonPhim phim={state === 'stress' ? [{ ...phimMau[0], ten: 'Những người viết huyền thoại: Hành trình trở về giữa mùa mưa tháng Chín' }, ...phimMau.slice(1)] : phimMau} state={listState} onChon={action} />; break;
    case 'VE-03': view = <GDKhungGio phim={phimMau[0]} ngay="27/09/2026" ngayKhaDung={['27/09/2026']} khungGio={khungGioMau} state={listState} onNgayChange={action} onChon={action} />; break;
    case 'VE-04': view = <GDSuatChieu suatChieu={[suatMau]} state={listState} onChon={action} />; break;
    case 'VE-05': {
      const ghe = state === 'selected' ? gheDaChon : state === 'sweetbox' ? gheMau.map((g) => ({ ...g, dangChon: g.hang === 'E' && g.so <= 2 })) : state === 'limit' ? gheMau.map((g, i) => ({ ...g, dangChon: i < 9 })) : gheMau;
      view = state === 'loading' ? <Screen title="Chọn ghế"><div className="loading-rows" role="status" aria-busy="true"><p className="empty-state">Đang tải sơ đồ ghế…</p></div></Screen> : <GDChonGhe suatChieu={suatMau} ghe={ghe} tongTien={state === 'selected' ? 210000 : state === 'sweetbox' ? 280000 : state === 'limit' ? 840000 : 0} diemDuKien={state === 'selected' ? 10 : state === 'sweetbox' ? 14 : state === 'limit' ? 42 : 0} error={state === 'limit' ? 'Chỉ được chọn tối đa 8 ghế cho một hóa đơn.' : state === 'conflict' ? 'Ghế A3 vừa được người khác giữ. Vui lòng chọn ghế khác.' : state === 'error' ? 'Không tải được trạng thái ghế. Vui lòng thử lại.' : undefined} canContinue={state === 'selected' || state === 'sweetbox'} onChon={action} onXoa={action} onTiepTuc={action} />; break;
    }
    case 'VE-06': view = <GDThanhToan hoaDon={hoaDon} voucher={voucher} onVoucherChange={setVoucher} voucherError={voucherErrors[state]} onApDung={action} onQuayLai={action} onThanhToan={action} />; break;
    case 'VE-07': view = <GDXacNhanThanhToan hoaDon={hoaDon} onXacNhan={action} />; break;
    case 'VE-08': view = <GDKetQuaThanhToan hoaDon={{ ...hoaDonVoucher, trangThai: state === 'unpaid' ? 'CHO_THANH_TOAN' : 'DA_THANH_TOAN', qrSrc: '/ticket-demo.svg' }} onVeTrangChu={() => go('VE-01')} />; break;
    case 'DT-01':
    case 'DT-02': {
      const qua = state === 'updated' ? quaMau.map((q) => q.id === 2 ? { ...q, tonKho: 44 } : q) : state === 'stress' ? [{ ...quaMau[0], ten: 'Combo bắp rang bơ phô mai và nước ngọt dành cho buổi chiếu cuối tuần', moTa: '' }, ...quaMau.slice(1)] : quaMau;
      view = <><GDChonQua khachHang={state === 'stress' ? 'Nguyễn Thị Phương Thảo' : khachMau.hoTen} diem={state === 'insufficient' ? 40 : state === 'updated' ? 150 : 250} qua={qua} lichSu={state === 'empty' ? [] : [doiDiemMau]} state={scene === 'DT-02' ? 'ready' : listState} error={state === 'insufficient' ? 'Không đủ điểm để đổi phần thưởng.' : state === 'unavailable' ? 'Phần quà không khả dụng.' : undefined} onChon={scene === 'DT-02' ? () => setDialogOpen(true) : action} />{scene === 'DT-02' && <GDXacNhanDoiQua open={dialogOpen} qua={quaMau[1]} diemTruoc={250} diemSau={150} onHuy={() => setDialogOpen(false)} onXacNhan={action} busy={state === 'busy'} error={state === 'error' ? 'Đổi quà không thành công. Điểm và tồn kho chưa thay đổi.' : undefined} />}</>; break;
    }
    case 'DT-03': view = <GDThongBao giaoDich={doiDiemMau} onVeDanhMuc={() => go('DT-01', 'updated')} />; break;
  }
  const auth = scene.startsWith('DK-');
  return <SiteLayout name={auth ? undefined : khachMau.hoTen} actions={<a className="btn btn--ghost" href="/">Thoát bản xem</a>}>
    <aside className="preview-controls" aria-label="Catalogue giao diện">
      <p className="preview-title">Bản xem giao diện · Module M1</p>
      <div className="preview-selector">
        <div className="field"><label htmlFor="ui-screen">Màn hình theo UI.md</label><select id="ui-screen" value={scene} onChange={(e) => go(e.target.value)}>{screens.map(([code, title]) => <option key={code} value={code}>{code} · {title}</option>)}</select></div>
        <div className="field"><label htmlFor="ui-state">Trạng thái mẫu</label><select id="ui-state" value={state} onChange={(e) => go(scene, e.target.value)}>{availableStates.map(([code, title]) => <option key={code} value={code}>{title}</option>)}</select></div>
      </div>
      <p className="preview-disclosure">Dữ liệu minh họa theo báo cáo, mốc 27/09/2026. Không tạo hóa đơn, giữ ghế, thanh toán hoặc đổi điểm. QR mẫu không có giá trị sử dụng.{auth && ' Riêng biểu mẫu đăng ký/đăng nhập vẫn gọi API tài khoản thật.'}</p>
      {message && <p role="status" className="preview-message">{message}</p>}
    </aside>
    {view}
  </SiteLayout>;
}
