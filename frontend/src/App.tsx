import { useCallback, useState } from 'react';
import type {
  KhachHang,
  KhungGio,
  Phim,
  QuaTang,
  SuatChieu,
  TomTatHoaDon,
  VoucherKhachHang,
} from './types';
import DangNhap from './pages/DangNhap';
import GDDangKyTaiKhoan from './pages/GDDangKyTaiKhoan';
import GDKhachHangHome from './pages/GDKhachHangHome';
import GDChonPhim from './pages/GDChonPhim';
import GDKhungGio from './pages/GDKhungGio';
import GDSuatChieu from './pages/GDSuatChieu';
import GDChonGhe from './pages/GDChonGhe';
import GDThanhToan from './pages/GDThanhToan';
import GDXacNhanThanhToan from './pages/GDXacNhanThanhToan';
import GDChonQua from './pages/GDChonQua';
import GDXacNhanDoiQua from './pages/GDXacNhanDoiQua';
import GDThongBao from './pages/GDThongBao';

type ManHinh =
  | 'dang-nhap'
  | 'dang-ky'
  | 'trang-chu'
  | 'chon-phim'
  | 'khung-gio'
  | 'suat-chieu'
  | 'chon-ghe'
  | 'thanh-toan'
  | 'xac-nhan-thanh-toan'
  | 'chon-qua'
  | 'xac-nhan-doi-qua'
  | 'thong-bao';

/**
 * Điều hướng bằng React state (không cần router).
 * Phiên đặt vé/đổi điểm chỉ nằm trong bộ nhớ React: tải lại trang sẽ quay về đăng nhập.
 */
export default function App() {
  const [manHinh, setManHinh] = useState<ManHinh>('dang-nhap');
  const [khachHang, setKhachHang] = useState<KhachHang | null>(null);
  const [thongBaoDangKy, setThongBaoDangKy] = useState('');

  const [phim, setPhim] = useState<Phim | null>(null);
  const [ngayChieu, setNgayChieu] = useState('');
  const [khungGio, setKhungGio] = useState<KhungGio | null>(null);
  const [suatChieu, setSuatChieu] = useState<SuatChieu | null>(null);
  const [hoaDon, setHoaDon] = useState<TomTatHoaDon | null>(null);
  const [quaTang, setQuaTang] = useState<QuaTang | null>(null);
  const [voucher, setVoucher] = useState<VoucherKhachHang | null>(null);

  const capNhatDiem = useCallback((tongDiem: number) => {
    setKhachHang((truoc) => (truoc ? { ...truoc, tongDiem } : truoc));
  }, []);

  function veTrangChu() {
    setManHinh('trang-chu');
  }

  function batDauDatVe() {
    setPhim(null);
    setNgayChieu('');
    setKhungGio(null);
    setSuatChieu(null);
    setHoaDon(null);
    setManHinh('chon-phim');
  }

  function batDauDoiDiem() {
    setQuaTang(null);
    setVoucher(null);
    setManHinh('chon-qua');
  }

  if (!khachHang) {
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

  if (manHinh === 'trang-chu') {
    return (
      <GDKhachHangHome
        khachHang={khachHang}
        onDatVe={batDauDatVe}
        onDoiDiemThuong={batDauDoiDiem}
      />
    );
  }

  if (manHinh === 'chon-phim') {
    return (
      <GDChonPhim
        khachHang={khachHang}
        onChonPhim={(duocChon) => {
          setPhim(duocChon);
          setNgayChieu('');
          setKhungGio(null);
          setSuatChieu(null);
          setHoaDon(null);
          setManHinh('khung-gio');
        }}
        onQuayLai={veTrangChu}
      />
    );
  }

  if (manHinh === 'khung-gio' && phim) {
    return (
      <GDKhungGio
        khachHang={khachHang}
        phim={phim}
        ngayChieuBanDau={ngayChieu}
        onChonKhungGio={(ngay, gio) => {
          setNgayChieu(ngay);
          setKhungGio(gio);
          setSuatChieu(null);
          setManHinh('suat-chieu');
        }}
        onQuayLai={() => setManHinh('chon-phim')}
      />
    );
  }

  if (manHinh === 'suat-chieu' && phim && ngayChieu && khungGio) {
    return (
      <GDSuatChieu
        khachHang={khachHang}
        phim={phim}
        ngayChieu={ngayChieu}
        khungGio={khungGio}
        onChonSuatChieu={(duocChon) => {
          setSuatChieu(duocChon);
          setHoaDon(null);
          setManHinh('chon-ghe');
        }}
        onQuayLai={() => setManHinh('khung-gio')}
      />
    );
  }

  if (manHinh === 'chon-ghe' && suatChieu) {
    return (
      <GDChonGhe
        khachHang={khachHang}
        suatChieu={suatChieu}
        onTiepTuc={(tomTat) => {
          setHoaDon(tomTat);
          setManHinh('thanh-toan');
        }}
        onQuayLai={() => {
          setHoaDon(null);
          setManHinh('suat-chieu');
        }}
      />
    );
  }

  if (manHinh === 'thanh-toan' && hoaDon) {
    return (
      <GDThanhToan
        khachHang={khachHang}
        tomTatBanDau={hoaDon}
        onDaThanhToan={(tomTat) => {
          setHoaDon(tomTat);
          capNhatDiem(tomTat.tongDiem);
          setManHinh('xac-nhan-thanh-toan');
        }}
        onQuayLai={() => {
          setHoaDon(null);
          setManHinh('chon-ghe');
        }}
      />
    );
  }

  if (manHinh === 'xac-nhan-thanh-toan' && hoaDon) {
    return (
      <GDXacNhanThanhToan
        khachHang={khachHang}
        tomTat={hoaDon}
        onVeTrangChu={() => {
          setHoaDon(null);
          setManHinh('trang-chu');
        }}
      />
    );
  }

  if (manHinh === 'chon-qua') {
    return (
      <GDChonQua
        khachHang={khachHang}
        onChonQua={(duocChon) => {
          setQuaTang(duocChon);
          setManHinh('xac-nhan-doi-qua');
        }}
        onCapNhatDiem={capNhatDiem}
        onQuayLai={veTrangChu}
      />
    );
  }

  if (manHinh === 'xac-nhan-doi-qua' && quaTang) {
    return (
      <GDXacNhanDoiQua
        khachHang={khachHang}
        quaTang={quaTang}
        tongDiem={khachHang.tongDiem}
        onDoiThanhCong={(voucherMoi) => {
          setVoucher(voucherMoi);
          capNhatDiem(voucherMoi.giaoDichDoiDiem.diemConLai);
          setManHinh('thong-bao');
        }}
        onQuayLai={() => setManHinh('chon-qua')}
      />
    );
  }

  if (manHinh === 'thong-bao' && voucher) {
    return (
      <GDThongBao
        khachHang={khachHang}
        voucher={voucher}
        onDoiTiep={() => {
          setQuaTang(null);
          setVoucher(null);
          setManHinh('chon-qua');
        }}
        onVeTrangChu={veTrangChu}
      />
    );
  }

  return (
    <GDKhachHangHome
      khachHang={khachHang}
      onDatVe={batDauDatVe}
      onDoiDiemThuong={batDauDoiDiem}
    />
  );
}
