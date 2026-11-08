export type ViewState = 'ready' | 'loading' | 'empty' | 'error';

export interface PhimUI {
  id: number;
  ma: string;
  ten: string;
  theLoai: string;
  thoiLuong: number;
  doTuoi: string;
}

export interface KhungGioUI {
  id: number;
  batDau: string;
  ketThuc: string;
}

export interface SuatChieuUI {
  id: number;
  ma: string;
  phim: PhimUI;
  ngay: string;
  khungGio: KhungGioUI;
  phong: string;
  dinhDang: string;
  giaCoSo: number;
}

export interface GheUI {
  id: number;
  hang: string;
  so: number;
  loai: string;
  phuThu: number;
  donGia: number;
  trangThai: 'TRONG' | 'DANG_GIU' | 'DA_BAN';
  dangChon: boolean;
  cap?: string;
}

export interface HoaDonUI {
  ma: string;
  khachHang: string;
  suatChieu: SuatChieuUI;
  dsVe: GheUI[];
  trangThai: 'CHO_THANH_TOAN' | 'DA_THANH_TOAN' | 'HET_HAN' | 'THAT_BAI' | 'DA_HUY';
  thoiGian: string;
  conLai: string;
  tongTien: number;
  maVoucher?: string;
  moTaVoucher?: string;
  giamGia: number;
  thanhToan: number;
  diemDuKien: number;
  tongDiemSau: number;
  maVe: string[];
  qrSrc?: string;
}

export interface QuaTangUI {
  id: number;
  ma: string;
  ten: string;
  loai: string;
  diem: number;
  tonKho: number;
  moTa: string;
}

export interface DoiDiemUI {
  maPhieu: string;
  khachHang: string;
  thoiGian: string;
  qua: QuaTangUI;
  diemTruoc: number;
  diemTru: number;
  diemSau: number;
  maVoucher: string;
  hanSuDung: string;
  trangThaiVoucher: string;
  ngaySuDung: string | null;
}
