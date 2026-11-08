import { useEffect, useRef, useState } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { KhachHang, KhungGio, Phim, SuatChieu } from '../types';
import { dinhDangGio, dinhDangNgay, dinhDangTien, maSuatChieu, thongDiepLoi } from '../utils';
import FlowFrame from './FlowFrame';

interface GDSuatChieuProps {
  khachHang: KhachHang;
  phim: Phim;
  ngayChieu: string;
  khungGio: KhungGio;
  onChonSuatChieu: (suatChieu: SuatChieu) => void;
  onQuayLai: () => void;
}

export default function GDSuatChieu({
  khachHang,
  phim,
  ngayChieu,
  khungGio,
  onChonSuatChieu,
  onQuayLai,
}: GDSuatChieuProps) {
  const [dsSuatChieu, setDsSuatChieu] = useState<SuatChieu[]>([]);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState('');
  const [lanTai, setLanTai] = useState(0);

  const loiRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  useEffect(() => {
    let huy = false;
    const thamSo = new URLSearchParams({
      phimId: String(phim.id),
      ngayChieu,
      khungGioId: String(khungGio.id),
    });

    async function taiSuatChieu() {
      setDangTai(true);
      setLoi('');
      try {
        const danhSach = await HTTPClient.fetch<SuatChieu[]>(
          `/api/suatchieu?${thamSo.toString()}`,
        );
        if (!huy) {
          setDsSuatChieu(Array.isArray(danhSach) ? danhSach : []);
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải suất chiếu. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTai(false);
        }
      }
    }

    void taiSuatChieu();
    return () => {
      huy = true;
    };
  }, [phim.id, ngayChieu, khungGio.id, lanTai]);

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 3 · Chọn suất chiếu"
      title={`Suất chiếu ${dinhDangGio(khungGio.gioBatDau)} ngày ${dinhDangNgay(ngayChieu)}`}
      description={`Phim ${phim.tenPhim}. Chọn một suất còn mở bán để xem sơ đồ ghế.`}
    >
      <section className="flow-card" aria-labelledby="tieuDeSuatChieu">
        <div className="section-head">
          <h2 id="tieuDeSuatChieu">Suất chiếu khả dụng</h2>
          <p className="section-note">
            Khung giờ {dinhDangGio(khungGio.gioBatDau)} – {dinhDangGio(khungGio.gioKetThuc)} · Ngày{' '}
            {dinhDangNgay(ngayChieu)}
          </p>
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

        {dangTai ? (
          <p className="loading-state" role="status">
            Đang tải suất chiếu…
          </p>
        ) : null}

        {!dangTai && !loi && dsSuatChieu.length === 0 ? (
          <div className="empty-state">
            <p>Không còn suất chiếu phù hợp với lựa chọn của bạn.</p>
            <div className="empty-actions">
              <button className="btn btn--ghost" type="button" onClick={onQuayLai}>
                Chọn khung giờ khác
              </button>
              <button className="btn btn--ghost" type="button" onClick={() => setLanTai((n) => n + 1)}>
                Tải lại
              </button>
            </div>
          </div>
        ) : null}

        {dsSuatChieu.length > 0 ? (
          <div className="table-scroll">
            <table className="data-table" id="tblListSuatChieu">
              <caption className="sr-only">Suất chiếu ngày {dinhDangNgay(ngayChieu)}</caption>
              <thead>
                <tr>
                  <th scope="col">Giờ chiếu</th>
                  <th scope="col">Phòng</th>
                  <th scope="col">Định dạng</th>
                  <th scope="col">Giá cơ sở</th>
                  <th scope="col">Trạng thái</th>
                  <th scope="col">
                    <span className="sr-only">Chọn suất chiếu</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {dsSuatChieu.map((suat) => {
                  const conMoBan = suat.trangThai === 'MO_BAN';
                  return (
                    <tr key={suat.id}>
                      <th scope="row">
                        <span className="cell-title">
                          {dinhDangGio(suat.khungGio?.gioBatDau)} – {dinhDangGio(suat.khungGio?.gioKetThuc)}
                        </span>
                        <span className="cell-sub">
                          Mã suất {maSuatChieu(suat.id)} · {dinhDangNgay(suat.ngayChieu)}
                        </span>
                      </th>
                      <td>{suat.phongChieu?.tenPhong || '—'}</td>
                      <td>
                        <span className="badge">{suat.phongChieu?.loaiPhong || '—'}</span>
                      </td>
                      <td>{dinhDangTien(suat.giaCoSo)}</td>
                      <td>
                        <span className={conMoBan ? 'badge badge--ok' : 'badge badge--muted'}>
                          {conMoBan ? 'Mở bán' : suat.trangThai}
                        </span>
                      </td>
                      <td className="cell-action">
                        <button
                          className="btn btn--primary chonSuatChieu"
                          id={`chonSuatChieu-${suat.id}`}
                          type="button"
                          data-chon="chonSuatChieu"
                          disabled={!conMoBan}
                          onClick={() => onChonSuatChieu(suat)}
                          aria-label={`Chọn suất ${dinhDangGio(suat.khungGio?.gioBatDau)} tại ${suat.phongChieu?.tenPhong || 'phòng chiếu'}`}
                        >
                          Chọn suất
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}

        <div className="flow-actions">
          <button className="btn btn--ghost" id="btnQuayLai" type="button" onClick={onQuayLai}>
            Chọn khung giờ khác
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
