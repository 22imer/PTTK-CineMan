import { useState } from 'react';
import type { KhachHang } from './types';
import DangNhap from './pages/DangNhap';
import GDDangKyTaiKhoan from './pages/GDDangKyTaiKhoan';
import GDKhachHangHome from './pages/GDKhachHangHome';
import SiteLayout from './ui/SiteLayout';
import ScrollbarAutohide from './ui/ScrollbarAutohide';
import { Screen } from './ui/common';
import Preview from './ui/Preview';

type ManHinh = 'khach' | 'dang-nhap' | 'dang-ky' | 'trang-chu';
const params = new URLSearchParams(window.location.search);

function TaiKhoanApp() {
  const requested = params.get('screen');
  const [manHinh, setManHinh] = useState<ManHinh>(requested === 'dang-ky' || requested === 'dang-nhap' ? requested : 'khach');
  const [khachHang, setKhachHang] = useState<KhachHang | null>(null);
  const [thongBaoDangKy, setThongBaoDangKy] = useState('');
  const headerActions = !khachHang && <>
    <button className="btn btn--ghost" type="button" onClick={() => setManHinh('dang-nhap')}>Đăng nhập</button>
    <button className="btn btn--primary" type="button" onClick={() => setManHinh('dang-ky')}>Đăng ký thành viên</button>
  </>;

  return <SiteLayout name={khachHang?.hoTen} actions={headerActions}>
    {manHinh === 'khach' && <Screen title="Trang chủ" subtitle="Chưa đăng nhập">{null}</Screen>}
    {manHinh === 'trang-chu' && khachHang && <GDKhachHangHome khachHang={khachHang} onDatVe={() => window.location.assign('?ui=VE-02')} onDoiDiem={() => window.location.assign('?ui=DT-01')} />}
    {manHinh === 'dang-ky' && <GDDangKyTaiKhoan onHuy={() => setManHinh('khach')} onDangKyThanhCong={(thongBao) => { setThongBaoDangKy(thongBao); setManHinh('dang-nhap'); }} />}
    {manHinh === 'dang-nhap' && <DangNhap thongBao={thongBaoDangKy} onDangNhapThanhCong={(kh) => { setThongBaoDangKy(''); setKhachHang(kh); setManHinh('trang-chu'); }} onMoDangKy={() => setManHinh('dang-ky')} />}
  </SiteLayout>;
}

export default function App() {
  return <><ScrollbarAutohide />{params.has('ui') ? <Preview /> : <TaiKhoanApp />}</>;
}
