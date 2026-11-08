import { useState } from 'react';
import type { KhachHang } from './types';
import DangNhap from './pages/DangNhap';
import GDDangKyTaiKhoan from './pages/GDDangKyTaiKhoan';
import GDKhachHangHome from './pages/GDKhachHangHome';

type ManHinh = 'dang-nhap' | 'dang-ky' | 'trang-chu';

/**
 * Điều hướng bằng React state (không cần router).
 * Hồ sơ khách hàng chỉ nằm trong bộ nhớ React: tải lại trang sẽ quay về đăng nhập.
 */
export default function App() {
  const [manHinh, setManHinh] = useState<ManHinh>('dang-nhap');
  const [khachHang, setKhachHang] = useState<KhachHang | null>(null);
  const [thongBaoDangKy, setThongBaoDangKy] = useState('');

  if (manHinh === 'trang-chu' && khachHang) {
    return <GDKhachHangHome khachHang={khachHang} />;
  }

  if (manHinh === 'dang-ky') {
    return (
      <GDDangKyTaiKhoan
        onHuy={() => setManHinh('dang-nhap')}
        onDangKyThanhCong={(thongBao) => {
          setThongBaoDangKy(thongBao);
          setManHinh('dang-nhap');
        }}
      />
    );
  }

  return (
    <DangNhap
      thongBao={thongBaoDangKy}
      onDangNhapThanhCong={(kh) => {
        setThongBaoDangKy('');
        setKhachHang(kh);
        setManHinh('trang-chu');
      }}
      onMoDangKy={() => setManHinh('dang-ky')}
    />
  );
}
