import {
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
} from 'react';
import { renderSVG } from 'uqr';
import {
  dangXuat as dangXuatPhien,
  getAuthSnapshot,
  khoiPhucPhien,
  subscribeAuth,
} from './api/authSession';
import { HTTPClient, HTTPError } from './api/httpClient';
import type {
  DanhSachQuaTang,
  Ghe,
  KhachHang,
  KhungGio,
  Phim,
  QuaTang,
  SuatChieu,
  ThongTinDangNhap,
  ThongTinGhe,
  TomTatHoaDon,
  VoucherKhachHang,
} from './types';
import {
  dinhDangDemNguoc,
  dinhDangGio,
  dinhDangNgay,
  dinhDangNgayGio,
  hanVoucher,
  laSweetbox,
  maHoaDon,
  maPhieuDoiDiem,
  maPhim,
  maQuaTang,
  maSuatChieu,
  maVe,
  thongDiepLoi,
} from './utils';
import DangNhap from './pages/DangNhap';
import GDDangKyTaiKhoan from './pages/GDDangKyTaiKhoan';
import GDKhachHangHome from './pages/GDKhachHangHome';
import GDChonPhim from './pages/GDChonPhim';
import GDKhungGio from './pages/GDKhungGio';
import GDSuatChieu from './pages/GDSuatChieu';
import GDChonGhe from './pages/GDChonGhe';
import GDThanhToan from './pages/GDThanhToan';
import GDXacNhanThanhToan, { GDKetQuaThanhToan } from './pages/GDXacNhanThanhToan';
import GDChonQua from './pages/GDChonQua';
import GDXacNhanDoiQua from './pages/GDXacNhanDoiQua';
import GDThongBao from './pages/GDThongBao';
import SiteLayout from './ui/SiteLayout';
import ScrollbarAutohide from './ui/ScrollbarAutohide';
import Preview from './ui/Preview';
import type {
  DoiDiemUI,
  GheUI,
  HoaDonUI,
  KhungGioUI,
  PhimUI,
  QuaTangUI,
  SuatChieuUI,
  ViewState,
} from './ui/models';

type ManHinh =
  | 'trang-chu'
  | 'chon-phim'
  | 'khung-gio'
  | 'suat-chieu'
  | 'chon-ghe'
  | 'thanh-toan'
  | 'xac-nhan-thanh-toan'
  | 'ket-qua-thanh-toan'
  | 'chon-qua'
  | 'thong-bao';

const params = new URLSearchParams(window.location.search);

function doiPhimUI(p: Phim): PhimUI {
  return {
    id: p.id,
    ma: maPhim(p.id),
    ten: p.tenPhim,
    theLoai: p.theLoai,
    thoiLuong: p.thoiLuong,
    doTuoi: p.doTuoi,
  };
}

function doiKhungGioUI(kg: KhungGio): KhungGioUI {
  return {
    id: kg.id,
    batDau: dinhDangGio(kg.gioBatDau),
    ketThuc: dinhDangGio(kg.gioKetThuc),
  };
}

function doiSuatChieuUI(sc: SuatChieu): SuatChieuUI {
  return {
    id: sc.id,
    ma: maSuatChieu(sc.id),
    phim: doiPhimUI(sc.phim),
    ngay: dinhDangNgay(sc.ngayChieu),
    khungGio: doiKhungGioUI(sc.khungGio),
    phong: sc.phongChieu.tenPhong,
    dinhDang: sc.phongChieu.loaiPhong,
    giaCoSo: sc.giaCoSo,
  };
}

function doiQuaTangUI(q: QuaTang): QuaTangUI {
  return {
    id: q.id,
    ma: maQuaTang(q.id),
    ten: q.tenQua,
    loai: q.loaiQua,
    diem: q.soDiem,
    tonKho: q.soLuong,
    moTa: q.moTa || '',
  };
}

function doiDiemUI(vc: VoucherKhachHang, tenKhachHang: string): DoiDiemUI {
  const gd = vc.giaoDichDoiDiem;
  return {
    maPhieu: maPhieuDoiDiem(gd.id),
    khachHang: tenKhachHang,
    thoiGian: dinhDangNgayGio(gd.thoiGian),
    qua: doiQuaTangUI(gd.quaTang),
    diemTruoc: gd.diemConLai + gd.soDiemTru,
    diemTru: gd.soDiemTru,
    diemSau: gd.diemConLai,
    maVoucher: vc.maVoucher,
    hanSuDung: hanVoucher(vc.ngayNhan),
    trangThaiVoucher: vc.trangThai,
    ngaySuDung: vc.ngaySuDung ? dinhDangNgay(vc.ngaySuDung) : null,
  };
}

function useDemNguoc(han: string | undefined, serverNow: string | undefined): string {
  const [conLai, setConLai] = useState('05:00');

  useEffect(() => {
    if (!han || !serverNow) {
      setConLai('05:00');
      return;
    }
    const hanMs = Date.parse(han);
    const serverMs = Date.parse(serverNow);
    if (!Number.isFinite(hanMs) || !Number.isFinite(serverMs)) {
      setConLai('05:00');
      return;
    }
    const moc = performance.now();
    const tinh = () => {
      const daTroi = performance.now() - moc;
      const giay = Math.max(0, (hanMs - serverMs - daTroi) / 1000);
      setConLai(dinhDangDemNguoc(giay));
    };
    tinh();
    const id = window.setInterval(tinh, 250);
    return () => window.clearInterval(id);
  }, [han, serverNow]);

  return conLai;
}

function PhienApp({ phien }: { phien: ThongTinDangNhap }) {
  const [manHinh, setManHinh] = useState<ManHinh>('trang-chu');
  const [khachHang, setKhachHang] = useState<KhachHang>(phien.khachHang);
  const [dangXuat, setDangXuat] = useState(false);
  const [loiDangXuat, setLoiDangXuat] = useState<string | undefined>();

  // Cùng phiên nhưng hồ sơ có thể mới sau refresh; đổi phiên/tài khoản sẽ remount cả cây này.
  useEffect(() => {
    setKhachHang(phien.khachHang);
  }, [phien.khachHang]);

  // State đặt vé
  const [dsPhim, setDsPhim] = useState<Phim[]>([]);
  const [ttPhim, setTtPhim] = useState<ViewState>('ready');
  const [loiPhim, setLoiPhim] = useState<string | undefined>();
  const [phimChon, setPhimChon] = useState<Phim | null>(null);

  const [ngayIsoMap, setNgayIsoMap] = useState<Record<string, string>>({});
  const [ngayHienThi, setNgayHienThi] = useState('');
  const [dsKhungGio, setDsKhungGio] = useState<KhungGio[]>([]);
  const [ttKhungGio, setTtKhungGio] = useState<ViewState>('ready');
  const [loiKhungGio, setLoiKhungGio] = useState<string | undefined>();

  const [dsSuatChieu, setDsSuatChieu] = useState<SuatChieu[]>([]);
  const [ttSuatChieu, setTtSuatChieu] = useState<ViewState>('ready');
  const [loiSuatChieu, setLoiSuatChieu] = useState<string | undefined>();
  const [suatChieuChon, setSuatChieuChon] = useState<SuatChieu | null>(null);

  const [dsGheRaw, setDsGheRaw] = useState<Ghe[]>([]);
  const [gheBanSet, setGheBanSet] = useState<Set<number>>(new Set());
  const [gheChonSet, setGheChonSet] = useState<Set<number>>(new Set());
  const [loiGhe, setLoiGhe] = useState<string | undefined>();

  const [tomTatHoaDon, setTomTatHoaDon] = useState<TomTatHoaDon | null>(null);
  const [maVoucherNhap, setMaVoucherNhap] = useState('');
  const [loiVoucher, setLoiVoucher] = useState<string | undefined>();

  // State đổi điểm
  const [dsQuaTang, setDsQuaTang] = useState<QuaTang[]>([]);
  const [lichSuDoiQua, setLichSuDoiQua] = useState<VoucherKhachHang[]>([]);
  const [ttQuaTang, setTtQuaTang] = useState<ViewState>('ready');
  const [loiQuaTang, setLoiQuaTang] = useState<string | undefined>();
  const [quaChon, setQuaChon] = useState<QuaTang | null>(null);
  const [dangDoiQua, setDangDoiQua] = useState(false);
  const [loiXacNhanQua, setLoiXacNhanQua] = useState<string | undefined>();
  const [voucherMoi, setVoucherMoi] = useState<VoucherKhachHang | null>(null);

  const conLai = useDemNguoc(tomTatHoaDon?.hoaDon.hetHan, tomTatHoaDon?.serverNow);

  async function xuLyDangXuat() {
    if (dangXuat) return;
    setDangXuat(true);
    setLoiDangXuat(undefined);
    try {
      await dangXuatPhien();
    } catch (error) {
      setLoiDangXuat(thongDiepLoi(error, 'Không thể đăng xuất. Vui lòng thử lại.'));
    } finally {
      setDangXuat(false);
    }
  }

  async function moChonPhim() {
    setManHinh('chon-phim');
    setTtPhim('loading');
    setLoiPhim(undefined);
    try {
      const ds = await HTTPClient.fetch<Phim[]>('/api/phim');
      setDsPhim(ds);
      setTtPhim(ds.length === 0 ? 'empty' : 'ready');
    } catch (err) {
      setLoiPhim(thongDiepLoi(err, 'Không tải được danh sách phim.'));
      setTtPhim('error');
    }
  }

  async function chonPhim(phimUi: PhimUI) {
    const found = dsPhim.find((p) => p.id === phimUi.id);
    if (!found) return;
    setPhimChon(found);
    setManHinh('khung-gio');
    setTtKhungGio('loading');
    setLoiKhungGio(undefined);
    try {
      const dsSuat = await HTTPClient.fetch<SuatChieu[]>(
        `/api/suatchieu?phimId=${encodeURIComponent(String(found.id))}`,
      );
      const map: Record<string, string> = {};
      for (const s of dsSuat) {
        if (s.trangThai === 'MO_BAN' && s.ngayChieu) {
          map[dinhDangNgay(s.ngayChieu)] = s.ngayChieu;
        }
      }
      setNgayIsoMap(map);
      const danhSachNgay = Object.keys(map);
      const ngayDau = danhSachNgay[0] || '';
      setNgayHienThi(ngayDau);
      if (ngayDau) {
        await taiKhungGio(found.id, map[ngayDau]);
      } else {
        setDsKhungGio([]);
        setTtKhungGio('empty');
      }
    } catch (err) {
      setLoiKhungGio(thongDiepLoi(err, 'Không tải được lịch chiếu.'));
      setTtKhungGio('error');
    }
  }

  async function taiKhungGio(phimId: number, ngayIso: string) {
    setTtKhungGio('loading');
    setLoiKhungGio(undefined);
    try {
      const ds = await HTTPClient.fetch<KhungGio[]>(
        `/api/khunggio?phimId=${encodeURIComponent(String(phimId))}&ngayChieu=${encodeURIComponent(ngayIso)}`,
      );
      setDsKhungGio(ds);
      setTtKhungGio(ds.length === 0 ? 'empty' : 'ready');
    } catch (err) {
      setLoiKhungGio(thongDiepLoi(err, 'Không tải được khung giờ.'));
      setTtKhungGio('error');
    }
  }

  async function chonKhungGio(kgUi: KhungGioUI) {
    const found = dsKhungGio.find((k) => k.id === kgUi.id);
    if (!found || !phimChon) return;
    const ngayIso = ngayIsoMap[ngayHienThi];
    setManHinh('suat-chieu');
    setTtSuatChieu('loading');
    setLoiSuatChieu(undefined);
    try {
      const ds = await HTTPClient.fetch<SuatChieu[]>(
        `/api/suatchieu?phimId=${encodeURIComponent(String(phimChon.id))}&ngayChieu=${encodeURIComponent(ngayIso)}&khungGioId=${encodeURIComponent(String(found.id))}`,
      );
      setDsSuatChieu(ds);
      setTtSuatChieu(ds.length === 0 ? 'empty' : 'ready');
    } catch (err) {
      setLoiSuatChieu(thongDiepLoi(err, 'Không tải được suất chiếu.'));
      setTtSuatChieu('error');
    }
  }

  async function taiSoDoGhe(suatId: number) {
    const thongTin = await HTTPClient.fetch<ThongTinGhe>(
      `/api/ghe?suatChieuId=${encodeURIComponent(String(suatId))}`,
    );
    setDsGheRaw(thongTin.dsGhe);
    const ban = new Set<number>();
    for (const v of thongTin.dsVe) {
      if (v.trangThaiVe === 'DANG_GIU' || v.trangThaiVe === 'DA_BAN') {
        ban.add(v.ghe.id);
      }
    }
    setGheBanSet(ban);
    if (thongTin.suatChieu) {
      setSuatChieuChon(thongTin.suatChieu);
    }
  }

  async function chonSuatChieu(scUi: SuatChieuUI) {
    const found = dsSuatChieu.find((s) => s.id === scUi.id);
    if (!found) return;
    setSuatChieuChon(found);
    setGheChonSet(new Set());
    setLoiGhe(undefined);
    setManHinh('chon-ghe');
    try {
      await taiSoDoGhe(found.id);
    } catch (err) {
      setLoiGhe(thongDiepLoi(err, 'Không tải được sơ đồ ghế.'));
    }
  }

  const gheUiList: GheUI[] = useMemo(() => {
    if (!suatChieuChon) return [];
    return dsGheRaw.map((g) => {
      const so = Number(g.soGhe);
      const cap = laSweetbox(g) ? `${g.hangGhe}${Math.ceil(so / 2)}` : undefined;
      const phuThu = g.loaiGhe?.phuThu || 0;
      const trangThai = gheBanSet.has(g.id) ? 'DANG_GIU' : 'TRONG';
      return {
        id: g.id,
        hang: g.hangGhe,
        so,
        loai: g.loaiGhe?.tenLoai || 'Ghế thường',
        phuThu,
        donGia: suatChieuChon.giaCoSo + phuThu,
        trangThai,
        dangChon: gheChonSet.has(g.id),
        cap,
      };
    });
  }, [dsGheRaw, gheBanSet, gheChonSet, suatChieuChon]);

  function batTatGhe(ghe: GheUI) {
    const nhom = ghe.cap
      ? gheUiList.filter((item) => item.cap === ghe.cap && item.trangThai === 'TRONG')
      : [ghe];
    if (ghe.cap && nhom.length < 2) {
      setLoiGhe('Cặp ghế Sweetbox không còn đủ hai ghế trống liền kề.');
      return;
    }
    const dangChon = nhom.every((item) => gheChonSet.has(item.id));
    if (dangChon) {
      const next = new Set(gheChonSet);
      nhom.forEach((item) => next.delete(item.id));
      setGheChonSet(next);
      setLoiGhe(undefined);
      return;
    }
    const them = nhom.filter((item) => !gheChonSet.has(item.id)).length;
    if (gheChonSet.size + them > 8) {
      setLoiGhe('Chỉ được chọn tối đa 8 ghế cho một hóa đơn.');
      return;
    }
    const next = new Set(gheChonSet);
    nhom.forEach((item) => next.add(item.id));
    setGheChonSet(next);
    setLoiGhe(undefined);
  }

  async function tiepTucGiuGhe() {
    if (!suatChieuChon || gheChonSet.size === 0) return;
    setLoiGhe(undefined);
    try {
      const tomTat = await HTTPClient.fetch<TomTatHoaDon>('/api/hoadon', {
        method: 'POST',
        body: JSON.stringify({
          suatChieuId: suatChieuChon.id,
          dsGhe: Array.from(gheChonSet),
        }),
      });
      setTomTatHoaDon(tomTat);
      setMaVoucherNhap('');
      setLoiVoucher(undefined);
      setManHinh('thanh-toan');
    } catch (err) {
      setLoiGhe(thongDiepLoi(err, 'Không thể giữ ghế đã chọn.'));
      await taiSoDoGhe(suatChieuChon.id).catch(() => {});
    }
  }

  async function apDungVoucher() {
    if (!tomTatHoaDon) return;
    const ma = maVoucherNhap.trim();
    if (!ma) {
      setLoiVoucher('Vui lòng nhập mã voucher.');
      return;
    }
    setLoiVoucher(undefined);
    try {
      const capNhat = await HTTPClient.fetch<TomTatHoaDon>('/api/hoadon/apdung-voucher', {
        method: 'POST',
        body: JSON.stringify({ hoaDonId: tomTatHoaDon.hoaDon.id, maVoucher: ma }),
      });
      setTomTatHoaDon(capNhat);
    } catch (err) {
      setLoiVoucher(thongDiepLoi(err, 'Voucher không hợp lệ.'));
    }
  }

  async function huyHoaDonQuayLai() {
    if (!tomTatHoaDon || !suatChieuChon) return;
    try {
      const ketQua = await HTTPClient.fetch<TomTatHoaDon>(
        `/api/hoadon/${encodeURIComponent(String(tomTatHoaDon.hoaDon.id))}`,
        { method: 'DELETE' },
      );
      if (ketQua.hoaDon.trangThai === 'DA_THANH_TOAN') {
        setTomTatHoaDon(ketQua);
        setKhachHang((kh) => ({ ...kh, tongDiem: ketQua.tongDiem }));
        setManHinh('ket-qua-thanh-toan');
        return;
      }
    } catch (err) {
      if (!(err instanceof HTTPError && err.httpStatus === 404)) {
        setLoiVoucher(thongDiepLoi(err, 'Không thể hủy hóa đơn.'));
        return;
      }
    }
    setTomTatHoaDon(null);
    setGheChonSet(new Set());
    await taiSoDoGhe(suatChieuChon.id).catch(() => {});
    setManHinh('chon-ghe');
  }

  async function xacNhanThanhToan() {
    if (!tomTatHoaDon) return;
    try {
      const daTra = await HTTPClient.fetch<TomTatHoaDon>(
        `/api/hoadon/${encodeURIComponent(String(tomTatHoaDon.hoaDon.id))}/thanh-toan`,
        {
          method: 'POST',
          body: JSON.stringify({ thanhCong: true }),
        },
      );
      setTomTatHoaDon(daTra);
      setKhachHang((kh) => ({ ...kh, tongDiem: daTra.tongDiem }));
      setManHinh('ket-qua-thanh-toan');
    } catch (err) {
      setLoiVoucher(thongDiepLoi(err, 'Thanh toán không thành công.'));
      setManHinh('thanh-toan');
    }
  }

  async function moDoiDiem() {
    setManHinh('chon-qua');
    setTtQuaTang('loading');
    setLoiQuaTang(undefined);
    setQuaChon(null);
    try {
      const duLieu = await HTTPClient.fetch<DanhSachQuaTang>('/api/quatang');
      setDsQuaTang(duLieu.dsQuaTang);
      setLichSuDoiQua(duLieu.lichSuDoiQua);
      setKhachHang((kh) => ({ ...kh, tongDiem: duLieu.tongDiem }));
      setTtQuaTang(duLieu.dsQuaTang.length === 0 ? 'empty' : 'ready');
    } catch (err) {
      setLoiQuaTang(thongDiepLoi(err, 'Không tải được danh mục quà tặng.'));
      setTtQuaTang('error');
    }
  }

  function chonQua(quaUi: QuaTangUI) {
    const found = dsQuaTang.find((q) => q.id === quaUi.id);
    if (!found) return;
    if (found.soLuong <= 0) {
      setLoiQuaTang('Phần quà đã hết hàng.');
      return;
    }
    if (khachHang.tongDiem < found.soDiem) {
      setLoiQuaTang('Không đủ điểm để đổi phần thưởng này.');
      return;
    }
    setLoiQuaTang(undefined);
    setLoiXacNhanQua(undefined);
    setQuaChon(found);
  }

  async function xacNhanDoiQua() {
    if (!quaChon) return;
    setDangDoiQua(true);
    setLoiXacNhanQua(undefined);
    try {
      const vc = await HTTPClient.fetch<VoucherKhachHang>('/api/doi-diem', {
        method: 'POST',
        body: JSON.stringify({ quaTangId: quaChon.id }),
      });
      setVoucherMoi(vc);
      setKhachHang((kh) => ({
        ...kh,
        tongDiem: vc.giaoDichDoiDiem.diemConLai,
      }));
      setQuaChon(null);
      setManHinh('thong-bao');
    } catch (err) {
      setLoiXacNhanQua(thongDiepLoi(err, 'Đổi quà không thành công.'));
    } finally {
      setDangDoiQua(false);
    }
  }

  const hoaDonUi: HoaDonUI | null = useMemo(() => {
    if (!tomTatHoaDon) return null;
    const hd = tomTatHoaDon.hoaDon;
    const sc = hd.dsVe[0]?.suatChieu || suatChieuChon;
    if (!sc) return null;
    const giamGia = Math.max(0, tomTatHoaDon.tongTienTruocVoucher - tomTatHoaDon.tongTien);
    const maVeList = hd.dsVe.map((v) => maVe(v));
    const svg = renderSVG(maVeList[0] || maHoaDon(hd.id), { ecc: 'M', border: 4 });
    return {
      ma: maHoaDon(hd.id),
      khachHang: khachHang.hoTen,
      suatChieu: doiSuatChieuUI(sc),
      dsVe: hd.dsVe.map((v) => ({
        id: v.ghe.id,
        hang: v.ghe.hangGhe,
        so: Number(v.ghe.soGhe),
        loai: v.ghe.loaiGhe?.tenLoai || 'Ghế',
        phuThu: v.ghe.loaiGhe?.phuThu || 0,
        donGia: v.giaVe,
        trangThai: v.trangThaiVe === 'DA_BAN' ? 'DA_BAN' : 'DANG_GIU',
        dangChon: true,
      })),
      trangThai: hd.trangThai as HoaDonUI['trangThai'],
      thoiGian: dinhDangNgayGio(hd.ngayTao),
      conLai,
      tongTien: tomTatHoaDon.tongTienTruocVoucher,
      maVoucher: hd.voucher?.maVoucher,
      moTaVoucher: hd.voucher?.giaoDichDoiDiem?.quaTang?.tenQua,
      giamGia,
      thanhToan: tomTatHoaDon.tongTien,
      diemDuKien: tomTatHoaDon.diemCong,
      tongDiemSau:
        hd.trangThai === 'DA_THANH_TOAN'
          ? tomTatHoaDon.tongDiem
          : tomTatHoaDon.tongDiem + tomTatHoaDon.diemCong,
      maVe: maVeList,
      qrSrc: `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`,
    };
  }, [tomTatHoaDon, khachHang, suatChieuChon, conLai]);

  const headerActions = (
    <>
      {manHinh !== 'trang-chu' && (
        <button className="btn btn--ghost" type="button" onClick={() => setManHinh('trang-chu')}>
          Trang chủ
        </button>
      )}
      <button
        className="btn btn--ghost"
        type="button"
        onClick={() => void xuLyDangXuat()}
        disabled={dangXuat}
      >
        {dangXuat ? 'Đang đăng xuất…' : 'Đăng xuất'}
      </button>
    </>
  );

  const tongTienChonGhe = gheUiList
    .filter((g) => g.dangChon)
    .reduce((sum, g) => sum + g.donGia, 0);
  const diemDuKienChonGhe = Math.floor(tongTienChonGhe / 20000);

  return (
    <SiteLayout name={khachHang.hoTen} actions={headerActions}>
      {loiDangXuat && (
        <div className="form-alert" role="alert">
          {loiDangXuat}{' '}
          <button
            className="link-button"
            type="button"
            onClick={() => void xuLyDangXuat()}
            disabled={dangXuat}
          >
            Thử lại
          </button>
        </div>
      )}
      {manHinh === 'trang-chu' && (
        <GDKhachHangHome
          khachHang={khachHang}
          preview
          onDatVe={() => void moChonPhim()}
          onDoiDiem={() => void moDoiDiem()}
        />
      )}
      {manHinh === 'chon-phim' && (
        <GDChonPhim
          phim={dsPhim.map(doiPhimUI)}
          state={ttPhim}
          error={loiPhim}
          onChon={(p) => void chonPhim(p)}
        />
      )}
      {manHinh === 'khung-gio' && phimChon && (
        <GDKhungGio
          phim={doiPhimUI(phimChon)}
          ngay={ngayHienThi}
          ngayKhaDung={Object.keys(ngayIsoMap)}
          khungGio={dsKhungGio.map(doiKhungGioUI)}
          state={ttKhungGio}
          error={loiKhungGio}
          onNgayChange={(ngayMoi) => {
            setNgayHienThi(ngayMoi);
            const iso = ngayIsoMap[ngayMoi];
            if (iso) void taiKhungGio(phimChon.id, iso);
          }}
          onChon={(kg) => void chonKhungGio(kg)}
        />
      )}
      {manHinh === 'suat-chieu' && (
        <GDSuatChieu
          suatChieu={dsSuatChieu.map(doiSuatChieuUI)}
          state={ttSuatChieu}
          error={loiSuatChieu}
          onChon={(sc) => void chonSuatChieu(sc)}
        />
      )}
      {manHinh === 'chon-ghe' && suatChieuChon && (
        <GDChonGhe
          suatChieu={doiSuatChieuUI(suatChieuChon)}
          ghe={gheUiList}
          tongTien={tongTienChonGhe}
          diemDuKien={diemDuKienChonGhe}
          error={loiGhe}
          canContinue={gheChonSet.size > 0 && gheChonSet.size <= 8}
          onChon={batTatGhe}
          onXoa={batTatGhe}
          onTiepTuc={() => void tiepTucGiuGhe()}
        />
      )}
      {manHinh === 'thanh-toan' && hoaDonUi && (
        <GDThanhToan
          hoaDon={hoaDonUi}
          voucher={maVoucherNhap}
          onVoucherChange={setMaVoucherNhap}
          voucherError={loiVoucher}
          onApDung={() => void apDungVoucher()}
          onQuayLai={() => void huyHoaDonQuayLai()}
          onThanhToan={() => setManHinh('xac-nhan-thanh-toan')}
        />
      )}
      {manHinh === 'xac-nhan-thanh-toan' && hoaDonUi && (
        <GDXacNhanThanhToan
          hoaDon={hoaDonUi}
          onXacNhan={() => void xacNhanThanhToan()}
        />
      )}
      {manHinh === 'ket-qua-thanh-toan' && hoaDonUi && (
        <GDKetQuaThanhToan
          hoaDon={hoaDonUi}
          onVeTrangChu={() => setManHinh('trang-chu')}
        />
      )}
      {manHinh === 'chon-qua' && (
        <>
          <GDChonQua
            khachHang={khachHang.hoTen}
            diem={khachHang.tongDiem}
            qua={dsQuaTang.map(doiQuaTangUI)}
            lichSu={lichSuDoiQua.map((vc) => doiDiemUI(vc, khachHang.hoTen))}
            state={ttQuaTang}
            error={loiQuaTang}
            onChon={chonQua}
          />
          {quaChon && (
            <GDXacNhanDoiQua
              open={Boolean(quaChon)}
              qua={doiQuaTangUI(quaChon)}
              diemTruoc={khachHang.tongDiem}
              diemSau={khachHang.tongDiem - quaChon.soDiem}
              onHuy={() => setQuaChon(null)}
              onXacNhan={() => void xacNhanDoiQua()}
              busy={dangDoiQua}
              error={loiXacNhanQua}
            />
          )}
        </>
      )}
      {manHinh === 'thong-bao' && voucherMoi && (
        <GDThongBao
          giaoDich={doiDiemUI(voucherMoi, khachHang.hoTen)}
          onVeDanhMuc={() => void moDoiDiem()}
        />
      )}
    </SiteLayout>
  );
}

function AuthGate() {
  const auth = useSyncExternalStore(subscribeAuth, getAuthSnapshot, getAuthSnapshot);
  const requested = params.get('screen');
  const [manHinh, setManHinh] = useState<'dang-nhap' | 'dang-ky'>(
    requested === 'dang-ky' ? 'dang-ky' : 'dang-nhap',
  );
  const [thongBaoDangKy, setThongBaoDangKy] = useState('');
  const daKhoiPhuc = useRef(false);

  useEffect(() => {
    if (daKhoiPhuc.current) return;
    daKhoiPhuc.current = true;
    void khoiPhucPhien();
  }, []);

  const trangThai = auth.status;
  useEffect(() => {
    if (trangThai === 'chua-dang-nhap') {
      setManHinh('dang-nhap');
      setThongBaoDangKy('');
    }
  }, [trangThai]);

  if (auth.status === 'dang-khoi-phuc') {
    return (
      <SiteLayout>
        <section className="ui-page" role="status" aria-live="polite">
          <header className="page-heading">
            <div>
              <h1>Đang khôi phục phiên đăng nhập…</h1>
              <p className="page-subtitle">
                Đang kiểm tra cookie phiên và tải hồ sơ mới nhất từ máy chủ.
              </p>
            </div>
          </header>
        </section>
      </SiteLayout>
    );
  }

  if (auth.status === 'loi-khoi-phuc') {
    return (
      <SiteLayout>
        <section className="ui-page">
          <header className="page-heading">
            <div>
              <h1>
                {auth.loi.unsupported
                  ? 'Không thể đồng bộ phiên đăng nhập'
                  : 'Không thể khôi phục phiên đăng nhập'}
              </h1>
              <p className="page-subtitle">
                {auth.loi.unsupported
                  ? 'Trình duyệt cần hỗ trợ Web Locks và BroadcastChannel trên origin này.'
                  : 'Lỗi mạng hoặc máy chủ; phiên của bạn chưa bị coi là hết hạn.'}
              </p>
            </div>
          </header>
          <p className="form-alert" role="alert">
            {auth.loi.message}
          </p>
          {!auth.loi.unsupported && (
            <div className="form-actions">
              <button
                className="btn btn--primary"
                type="button"
                onClick={() => void khoiPhucPhien()}
              >
                Thử lại
              </button>
            </div>
          )}
        </section>
      </SiteLayout>
    );
  }

  if (auth.status === 'da-dang-nhap') {
    // Đổi phiên/tài khoản → key mới để remount và xóa toàn bộ state nghiệp vụ.
    // Refresh cùng phiên giữ nguyên sessionId nên không làm gián đoạn thao tác đang dở.
    return (
      <PhienApp
        key={`${auth.phien.sessionId}:${auth.phien.khachHang.id}`}
        phien={auth.phien}
      />
    );
  }

  return (
    <SiteLayout
      actions={
        <>
          <button
            className="btn btn--ghost"
            type="button"
            onClick={() => setManHinh('dang-nhap')}
          >
            Đăng nhập
          </button>
          <button
            className="btn btn--primary"
            type="button"
            onClick={() => setManHinh('dang-ky')}
          >
            Đăng ký thành viên
          </button>
        </>
      }
    >
      {manHinh === 'dang-ky' ? (
        <GDDangKyTaiKhoan
          onHuy={() => setManHinh('dang-nhap')}
          onDangKyThanhCong={(thongBao) => {
            setThongBaoDangKy(thongBao);
            setManHinh('dang-nhap');
          }}
        />
      ) : (
        <DangNhap
          thongBao={thongBaoDangKy}
          onMoDangKy={() => setManHinh('dang-ky')}
        />
      )}
    </SiteLayout>
  );
}

export default function App() {
  return (
    <>
      <ScrollbarAutohide />
      {params.has('ui') ? <Preview /> : <AuthGate />}
    </>
  );
}
