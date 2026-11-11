import type { KhachHang } from '../types';
import { Notice, Screen } from '../ui/common';
import HallScene from '../ui/HallScene';

interface GDKhachHangHomeProps {
  khachHang: KhachHang;
  onDatVe: () => void;
  onDoiDiem: () => void;
  preview?: boolean;
}

export default function GDKhachHangHome({ khachHang, onDatVe, onDoiDiem, preview = false }: GDKhachHangHomeProps) {
  return <Screen title="Trang chủ" subtitle={khachHang.hoTen}>
    <HallScene />
    <div className="home-actions">
      <button className="btn btn--primary" id="btnDatVe" type="button" onClick={onDatVe}>Đặt vé trực tuyến</button>
      <button className="btn" id="btnDoiDiemThuong" type="button" onClick={onDoiDiem}>Đổi điểm thưởng</button>
    </div>
    {!preview && <Notice>Đặt vé và đổi điểm hiện mở bản xem giao diện với dữ liệu minh họa; chưa thực hiện giao dịch.</Notice>}
  </Screen>;
}
