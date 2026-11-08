import { useEffect, useRef, useState } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { KhachHang, KhungGio, Phim, SuatChieu } from '../types';
import { dinhDangGio, dinhDangNgay, maPhim, thongDiepLoi } from '../utils';
import FlowFrame from './FlowFrame';

interface GDKhungGioProps {
  khachHang: KhachHang;
  phim: Phim;
  /** Ngày đã chọn trước đó khi quay lại từ trang suất chiếu. */
  ngayChieuBanDau?: string;
  onChonKhungGio: (ngayChieu: string, khungGio: KhungGio) => void;
  onQuayLai: () => void;
}

export default function GDKhungGio({
  khachHang,
  phim,
  ngayChieuBanDau,
  onChonKhungGio,
  onQuayLai,
}: GDKhungGioProps) {
  const [dsNgay, setDsNgay] = useState<string[]>([]);
  const [ngayChieu, setNgayChieu] = useState('');
  const [dsKhungGio, setDsKhungGio] = useState<KhungGio[]>([]);
  const [dangTaiNgay, setDangTaiNgay] = useState(true);
  const [dangTaiKhungGio, setDangTaiKhungGio] = useState(false);
  const [loi, setLoi] = useState('');

  const loiRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  // Ngày chiếu lấy từ chính các suất đang mở bán của phim.
  useEffect(() => {
    let huy = false;

    async function taiNgay() {
      setDangTaiNgay(true);
      setLoi('');
      setDsNgay([]);
      setNgayChieu('');
      setDsKhungGio([]);
      try {
        const danhSach = await HTTPClient.fetch<SuatChieu[]>(
          `/api/suatchieu?phimId=${encodeURIComponent(String(phim.id))}`,
        );
        if (!huy) {
          const ngay = Array.from(
            new Set(
              (Array.isArray(danhSach) ? danhSach : [])
                .filter((suat) => suat.trangThai === 'MO_BAN' && suat.ngayChieu)
                .map((suat) => suat.ngayChieu),
            ),
          ).sort();
          setDsNgay(ngay);
          setNgayChieu(ngay.includes(ngayChieuBanDau || '') ? ngayChieuBanDau || '' : ngay[0] || '');
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải ngày chiếu. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTaiNgay(false);
        }
      }
    }

    void taiNgay();
    return () => {
      huy = true;
    };
  }, [phim.id]);

  useEffect(() => {
    if (!ngayChieu) {
      setDsKhungGio([]);
      return;
    }

    let huy = false;
    const thamSo = new URLSearchParams({
      phimId: String(phim.id),
      ngayChieu,
    });

    async function taiKhungGio() {
      setDangTaiKhungGio(true);
      setLoi('');
      setDsKhungGio([]);
      try {
        const danhSach = await HTTPClient.fetch<KhungGio[]>(`/api/khunggio?${thamSo.toString()}`);
        if (!huy) {
          setDsKhungGio(Array.isArray(danhSach) ? danhSach : []);
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải khung giờ. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTaiKhungGio(false);
        }
      }
    }

    void taiKhungGio();
    return () => {
      huy = true;
    };
  }, [phim.id, ngayChieu]);

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 2 · Chọn ngày và khung giờ"
      title={`Lịch chiếu phim ${phim.tenPhim}`}
      description="Chọn ngày chiếu rồi chọn khung giờ bắt đầu. Các khung giờ dưới đây chỉ gồm suất đang mở bán."
    >
      <section className="flow-card" aria-labelledby="tieuDeKhungGio">
        <div className="section-head section-head--split">
          <div>
            <h2 id="tieuDeKhungGio">Ngày chiếu</h2>
            <p className="section-note">
              {maPhim(phim.id)} · {phim.theLoai} · {phim.thoiLuong} phút · Phân loại {phim.doTuoi} (chỉ hiển thị)
            </p>
          </div>
          {!dangTaiNgay && dsNgay.length > 0 ? (
            <div className="field field--compact">
              <label htmlFor="ngayChieu">Chọn ngày chiếu</label>
              <select
                id="ngayChieu"
                value={ngayChieu}
                onChange={(event) => setNgayChieu(event.target.value)}
              >
                {dsNgay.map((ngay) => (
                  <option key={ngay} value={ngay}>
                    {dinhDangNgay(ngay)}
                  </option>
                ))}
              </select>
            </div>
          ) : null}
        </div>

        <p
          className="form-alert"
          role="alert"
          tabIndex={-1}
          ref={loiRef}
          aria-live="assertive"
        >
          {loi}
        </p>

        {dangTaiNgay ? (
          <p className="loading-state" role="status">
            Đang tải ngày chiếu…
          </p>
        ) : null}

        {!dangTaiNgay && !loi && dsNgay.length === 0 ? (
          <div className="empty-state">
            <p>Phim này hiện chưa có ngày chiếu nào đang mở bán.</p>
            <button className="btn btn--ghost" type="button" onClick={onQuayLai}>
              Chọn phim khác
            </button>
          </div>
        ) : null}

        {ngayChieu && !loi ? (
          <div className="timeframe-block">
            <h3 className="block-title">Khung giờ ngày {dinhDangNgay(ngayChieu)}</h3>
            {dangTaiKhungGio ? (
              <p className="loading-state" role="status">
                Đang tải khung giờ…
              </p>
            ) : null}

            {!dangTaiKhungGio && dsKhungGio.length === 0 ? (
              <p className="empty-state empty-state--inline">
                Ngày này chưa có khung giờ nào đang mở bán. Vui lòng chọn ngày khác.
              </p>
            ) : null}

            {dsKhungGio.length > 0 ? (
              <div className="table-scroll">
                <table className="data-table" id="tblListKhungGio">
                  <caption className="sr-only">Khung giờ chiếu ngày {dinhDangNgay(ngayChieu)}</caption>
                  <thead>
                    <tr>
                      <th scope="col">Khung giờ</th>
                      <th scope="col">Bắt đầu</th>
                      <th scope="col">Kết thúc</th>
                      <th scope="col">
                        <span className="sr-only">Chọn khung giờ</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {dsKhungGio.map((khungGio) => (
                      <tr key={khungGio.id}>
                        <th scope="row">
                          <span className="cell-title">
                            {dinhDangGio(khungGio.gioBatDau)} – {dinhDangGio(khungGio.gioKetThuc)}
                          </span>
                          <span className="cell-sub">Ngày {dinhDangNgay(ngayChieu)}</span>
                        </th>
                        <td>{dinhDangGio(khungGio.gioBatDau)}</td>
                        <td>{dinhDangGio(khungGio.gioKetThuc)}</td>
                        <td className="cell-action">
                          <button
                            className="btn btn--primary chonKhungGio"
                            id={`chonKhungGio-${khungGio.id}`}
                            type="button"
                            data-chon="chonKhungGio"
                            onClick={() => onChonKhungGio(ngayChieu, khungGio)}
                            aria-label={`Chọn khung giờ ${dinhDangGio(khungGio.gioBatDau)} ngày ${dinhDangNgay(ngayChieu)}`}
                          >
                            Chọn khung giờ
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </div>
        ) : null}

        <div className="flow-actions">
          <button className="btn btn--ghost" id="btnQuayLai" type="button" onClick={onQuayLai}>
            Chọn phim khác
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
