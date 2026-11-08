import { useEffect, useRef, useState, type FormEvent } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { KhachHang, Phim } from '../types';
import { dinhDangNgay, maPhim, thongDiepLoi } from '../utils';
import FlowFrame from './FlowFrame';

interface GDChonPhimProps {
  khachHang: KhachHang;
  onChonPhim: (phim: Phim) => void;
  onQuayLai: () => void;
}

export default function GDChonPhim({ khachHang, onChonPhim, onQuayLai }: GDChonPhimProps) {
  const [dsPhim, setDsPhim] = useState<Phim[]>([]);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState('');
  const [tuKhoa, setTuKhoa] = useState('');
  const [tuKhoaApDung, setTuKhoaApDung] = useState('');

  const loiRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    let huy = false;

    async function taiDanhSach() {
      setDangTai(true);
      setLoi('');
      try {
        const danhSach = await HTTPClient.fetch<Phim[]>('/api/phim');
        if (!huy) {
          setDsPhim(Array.isArray(danhSach) ? danhSach : []);
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải danh sách phim. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTai(false);
        }
      }
    }

    void taiDanhSach();
    return () => {
      huy = true;
    };
  }, []);

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  const tuKhoaChuan = tuKhoaApDung.trim().toLowerCase();
  const dsHienThi = tuKhoaChuan
    ? dsPhim.filter((phim) =>
        [phim.tenPhim, phim.theLoai, phim.daoDien]
          .filter(Boolean)
          .some((giaTri) => giaTri.toLowerCase().includes(tuKhoaChuan)),
      )
    : dsPhim;

  function handleTimKiem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTuKhoaApDung(tuKhoa);
  }

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 1 · Chọn phim"
      title="Hôm nay bạn muốn xem phim gì?"
      description="Danh sách phim đang mở bán do rạp cập nhật. Chọn một phim để xem ngày chiếu và khung giờ."
    >
      <section className="flow-card" aria-labelledby="tieuDeDanhSachPhim">
        <div className="section-head">
          <h2 id="tieuDeDanhSachPhim">Phim đang mở bán</h2>
          <form className="search-form" onSubmit={handleTimKiem} role="search">
            <label className="field-label" htmlFor="inTuKhoaPhim">
              Tìm theo tên phim, thể loại hoặc đạo diễn
            </label>
            <div className="search-row">
              <input
                id="inTuKhoaPhim"
                type="search"
                value={tuKhoa}
                onChange={(event) => setTuKhoa(event.target.value)}
                placeholder="Ví dụ: Mai"
                autoComplete="off"
              />
              <button className="btn btn--primary" id="btnTimKiem" type="submit" disabled={dangTai}>
                Tìm kiếm
              </button>
            </div>
          </form>
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
            Đang tải danh sách phim…
          </p>
        ) : null}

        {!dangTai && !loi && dsPhim.length === 0 ? (
          <div className="empty-state">
            <p>Hiện chưa có phim nào đang mở bán.</p>
            <button className="btn btn--ghost" id="btnVeTrangChu" type="button" onClick={onQuayLai}>
              Về trang chủ
            </button>
          </div>
        ) : null}

        {!dangTai && dsPhim.length > 0 && dsHienThi.length === 0 ? (
          <div className="empty-state">
            <p>Không tìm thấy phim phù hợp với từ khóa “{tuKhoaApDung}”.</p>
            <button
              className="btn btn--ghost"
              type="button"
              onClick={() => {
                setTuKhoa('');
                setTuKhoaApDung('');
              }}
            >
              Xóa từ khóa
            </button>
          </div>
        ) : null}

        {dsHienThi.length > 0 ? (
          <div className="table-scroll">
            <table className="data-table" id="tblListPhim">
              <caption className="sr-only">Danh sách phim đang mở bán</caption>
              <thead>
                <tr>
                  <th scope="col">Phim</th>
                  <th scope="col">Thể loại</th>
                  <th scope="col">Thời lượng</th>
                  <th scope="col">Phân loại</th>
                  <th scope="col">Khởi chiếu</th>
                  <th scope="col">
                    <span className="sr-only">Chọn phim</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {dsHienThi.map((phim) => (
                  <tr key={phim.id}>
                    <th scope="row">
                      <span className="cell-title">{phim.tenPhim}</span>
                      <span className="cell-sub">
                        Mã phim {maPhim(phim.id)} · Đạo diễn: {phim.daoDien}
                        {phim.moTa ? ` · ${phim.moTa}` : ''}
                      </span>
                    </th>
                    <td>{phim.theLoai}</td>
                    <td>{phim.thoiLuong} phút</td>
                    <td>
                      <span className="badge">{phim.doTuoi}</span>
                    </td>
                    <td>{dinhDangNgay(phim.ngayKhoiChieu)}</td>
                    <td className="cell-action">
                      <button
                        className="btn btn--primary chonPhim"
                        id={`chonPhim-${phim.id}`}
                        type="button"
                        onClick={() => onChonPhim(phim)}
                        data-chon="chonPhim"
                        aria-label={`Chọn phim ${phim.tenPhim}`}
                      >
                        Chọn phim
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        <div className="flow-actions">
          <button className="btn btn--ghost" id="btnQuayLai" type="button" onClick={onQuayLai}>
            Về trang chủ
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
