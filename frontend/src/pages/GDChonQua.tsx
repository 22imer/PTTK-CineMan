import { useEffect, useRef, useState, type FormEvent } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { DanhSachQuaTang, KhachHang, QuaTang } from '../types';
import { dinhDangTien, maQuaTang, thongDiepLoi } from '../utils';
import FlowFrame from './FlowFrame';

const NHAN_LOAI_QUA: Record<string, string> = {
  GIAM_GIA: 'Voucher giảm giá',
  VE_PHIM: 'Vé xem phim',
  BAP_NUOC: 'Bắp nước',
};

interface GDChonQuaProps {
  khachHang: KhachHang;
  onChonQua: (quaTang: QuaTang) => void;
  /** Điểm tươi từ GET /api/quatang, cập nhật hồ sơ trong React state. */
  onCapNhatDiem: (tongDiem: number) => void;
  onQuayLai: () => void;
}

export default function GDChonQua({
  khachHang,
  onChonQua,
  onCapNhatDiem,
  onQuayLai,
}: GDChonQuaProps) {
  const [dsQuaTang, setDsQuaTang] = useState<QuaTang[]>([]);
  const [tongDiem, setTongDiem] = useState<number | null>(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState('');
  const [tuKhoa, setTuKhoa] = useState('');
  const [tuKhoaApDung, setTuKhoaApDung] = useState('');
  const [lanTai, setLanTai] = useState(0);

  const loiRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  useEffect(() => {
    let huy = false;

    async function taiCatalogue() {
      setDangTai(true);
      setLoi('');
      try {
        const duLieu = await HTTPClient.fetch<DanhSachQuaTang>('/api/quatang');
        if (!huy) {
          setDsQuaTang(Array.isArray(duLieu.dsQuaTang) ? duLieu.dsQuaTang : []);
          const diem = Number.isFinite(duLieu.tongDiem) ? duLieu.tongDiem : 0;
          setTongDiem(diem);
          onCapNhatDiem(diem);
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải danh sách quà tặng. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTai(false);
        }
      }
    }

    void taiCatalogue();
    return () => {
      huy = true;
    };
  }, [lanTai, onCapNhatDiem]);

  const tuKhoaChuan = tuKhoaApDung.trim().toLowerCase();
  const dsHienThi = tuKhoaChuan
    ? dsQuaTang.filter((qua) =>
        [qua.tenQua, qua.moTa, NHAN_LOAI_QUA[qua.loaiQua]]
          .filter((giaTri): giaTri is string => Boolean(giaTri))
          .some((giaTri) => giaTri.toLowerCase().includes(tuKhoaChuan)),
      )
    : dsQuaTang;

  function handleTimKiem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTuKhoaApDung(tuKhoa);
  }

  function lyDoKhongChon(qua: QuaTang): string {
    if (qua.soLuong <= 0) {
      return 'Hết hàng';
    }
    if (tongDiem !== null && tongDiem < qua.soDiem) {
      return 'Không đủ điểm';
    }
    return '';
  }

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Đổi điểm · Chọn quà"
      title="Đổi điểm thưởng thành quà tặng"
      description="Catalogue hiển thị tồn kho và điểm tươi từ máy chủ. Quà còn hàng và đủ điểm mới có thể chọn."
    >
      <section className="flow-card" aria-labelledby="tieuDeCatalogue">
        <div className="section-head section-head--split">
          <div>
            <h2 id="tieuDeCatalogue">Quà tặng khả dụng</h2>
            <p className="section-note">
              Mỗi lần đổi tạo một phiếu đổi điểm và một voucher mới có hạn 30 ngày.
            </p>
          </div>
          <p className="points-balance" id="outTongDiem">
            Điểm hiện có
            <strong>
              {tongDiem === null ? '—' : tongDiem.toLocaleString('vi-VN')} điểm
            </strong>
          </p>
        </div>

        <form className="search-form" onSubmit={handleTimKiem} role="search">
          <label className="field-label" htmlFor="inTuKhoaQua">
            Tìm quà theo tên hoặc mô tả
          </label>
          <div className="search-row">
            <input
              id="inTuKhoaQua"
              type="search"
              value={tuKhoa}
              onChange={(event) => setTuKhoa(event.target.value)}
              placeholder="Ví dụ: vé phim"
              autoComplete="off"
            />
            <button className="btn btn--primary" id="btnTimKiem" type="submit" disabled={dangTai}>
              Tìm kiếm
            </button>
          </div>
        </form>

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
            Đang tải danh sách quà…
          </p>
        ) : null}

        {!dangTai && !loi && dsQuaTang.length === 0 ? (
          <div className="empty-state">
            <p>Hiện chưa có quà tặng nào trong catalogue.</p>
            <div className="empty-actions">
              <button className="btn btn--ghost" type="button" onClick={() => setLanTai((n) => n + 1)}>
                Tải lại
              </button>
              <button className="btn btn--ghost" type="button" onClick={onQuayLai}>
                Về trang chủ
              </button>
            </div>
          </div>
        ) : null}

        {!dangTai && dsQuaTang.length > 0 && dsHienThi.length === 0 ? (
          <div className="empty-state">
            <p>Không tìm thấy quà phù hợp với từ khóa “{tuKhoaApDung}”.</p>
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
            <table className="data-table" id="tblListQua">
              <caption className="sr-only">Danh sách quà tặng đổi bằng điểm</caption>
              <thead>
                <tr>
                  <th scope="col">Quà tặng</th>
                  <th scope="col">Loại</th>
                  <th scope="col">Điểm cần</th>
                  <th scope="col">Tồn kho</th>
                  <th scope="col">Giá trị</th>
                  <th scope="col">
                    <span className="sr-only">Chọn quà</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {dsHienThi.map((qua) => {
                  const lyDo = lyDoKhongChon(qua);
                  const conHang = qua.soLuong > 0;
                  return (
                    <tr key={qua.id}>
                      <th scope="row">
                        <span className="cell-title">{qua.tenQua}</span>
                        <span className="cell-sub">
                          Mã quà {maQuaTang(qua.id)} · {qua.moTa || '—'}
                        </span>
                      </th>
                      <td>{NHAN_LOAI_QUA[qua.loaiQua] || qua.loaiQua}</td>
                      <td>
                        <strong>{qua.soDiem.toLocaleString('vi-VN')} điểm</strong>
                      </td>
                      <td>
                        {conHang ? (
                          `Còn ${qua.soLuong}`
                        ) : (
                          <span className="badge badge--muted">Hết hàng</span>
                        )}
                      </td>
                      <td>
                        {qua.loaiQua === 'GIAM_GIA' && qua.giaTriGiam
                          ? dinhDangTien(qua.giaTriGiam)
                          : '—'}
                      </td>
                      <td className="cell-action">
                        <button
                          className="btn btn--primary chonQua"
                          id={`chonQua-${qua.id}`}
                          type="button"
                          data-chon="chonQua"
                          disabled={Boolean(lyDo)}
                          aria-describedby={lyDo ? `lyDoQua-${qua.id}` : undefined}
                          aria-label={`Chọn quà ${qua.tenQua} với ${qua.soDiem} điểm`}
                          onClick={() => onChonQua(qua)}
                        >
                          {lyDo || 'Chọn quà'}
                        </button>
                        {lyDo ? (
                          <span className="sr-only" id={`lyDoQua-${qua.id}`}>
                            {lyDo}
                          </span>
                        ) : null}
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
            Về trang chủ
          </button>
        </div>
      </section>
    </FlowFrame>
  );
}
