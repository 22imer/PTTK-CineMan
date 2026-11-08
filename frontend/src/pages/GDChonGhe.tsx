import { BookingMeta, Notice, Screen, SeatLines, money } from '../ui/common';
import type { GheUI, SuatChieuUI } from '../ui/models';

export interface GDChonGheProps {
  suatChieu: SuatChieuUI;
  ghe: GheUI[];
  tongTien: number;
  diemDuKien: number;
  error?: string;
  canContinue?: boolean;
  onChon: (ghe: GheUI) => void;
  onXoa: (ghe: GheUI) => void;
  onTiepTuc: () => void;
}

interface HangGhe {
  hang: string;
  ds: GheUI[];
}

interface CumGhe {
  cap?: string;
  ds: GheUI[];
}

/** Nhóm ghế theo hàng, giữ nguyên thứ tự dữ liệu do lớp điều khiển cung cấp. */
function nhomTheoHang(ds: GheUI[]): HangGhe[] {
  const ketQua: HangGhe[] = [];
  for (const item of ds) {
    const cuoi = ketQua[ketQua.length - 1];
    if (cuoi && cuoi.hang === item.hang) {
      cuoi.ds.push(item);
    } else {
      ketQua.push({ hang: item.hang, ds: [item] });
    }
  }
  return ketQua;
}

/** Ghế Sweetbox được lớp điều khiển gắn cùng một `cap`; gom cạnh nhau để vẽ liền cặp. */
function nhomTheoCap(ds: GheUI[]): CumGhe[] {
  const ketQua: CumGhe[] = [];
  for (const item of ds) {
    const cuoi = ketQua[ketQua.length - 1];
    if (item.cap && cuoi && cuoi.cap === item.cap) {
      cuoi.ds.push(item);
    } else {
      ketQua.push({ cap: item.cap, ds: [item] });
    }
  }
  return ketQua;
}

function nhanTrangThaiGhe(ghe: GheUI): string {
  if (ghe.trangThai === 'DA_BAN') {
    return 'đã bán';
  }
  if (ghe.trangThai === 'DANG_GIU') {
    return 'đang được người khác giữ';
  }
  return 'còn trống';
}

/**
 * VE-05 — Chọn ghế.
 * Sơ đồ ghế vẽ toàn bộ dữ liệu phòng chiếu; trạng thái ghế và tiền do lớp điều khiển cung cấp.
 */
export default function GDChonGhe({
  suatChieu,
  ghe,
  tongTien,
  diemDuKien,
  error,
  canContinue,
  onChon,
  onXoa,
  onTiepTuc,
}: GDChonGheProps) {
  const dsHang = nhomTheoHang(ghe);
  const daChon = ghe.filter((item) => item.dangChon);
  const coTheTiepTuc = canContinue ?? (daChon.length > 0);

  function veNutGhe(item: GheUI) {
    const conTrong = item.trangThai === 'TRONG';
    const ten = `Ghế ${item.hang}${item.so} — ${item.loai} — ${nhanTrangThaiGhe(item)}`;
    const lop = ['seat'];
    if (item.dangChon) {
      lop.push('seat--selected');
    }
    if (!conTrong) {
      lop.push('seat--unavailable');
    }
    return (
      <button
        key={item.id}
        type="button"
        className={lop.join(' ')}
        aria-pressed={item.dangChon}
        aria-label={item.dangChon ? `${ten} — đang chọn` : ten}
        disabled={!conTrong}
        onClick={() => onChon(item)}
      >
        {item.hang}
        {item.so}
      </button>
    );
  }

  return (
    <Screen title="Chọn ghế">
      <BookingMeta suatChieu={suatChieu} />

      {error ? <Notice tone="error">{error}</Notice> : null}

      <div className="seat-map" id="tblListGhe">
        <p className="seat-map__screen">MÀN HÌNH</p>
        {dsHang.length === 0 ? (
          <p className="empty-state">Chưa có sơ đồ ghế cho suất chiếu này.</p>
        ) : (
          <div className="seat-map__rows">
            {dsHang.map((hangGhe) => (
              <div className="seat-map__row" key={hangGhe.hang} role="group" aria-label={`Hàng ${hangGhe.hang}`}>
                <span className="seat-map__row-label" aria-hidden="true">Hàng {hangGhe.hang}</span>
                <div className="seat-map__seats">
                  {nhomTheoCap(hangGhe.ds).map((cum, index) =>
                    cum.cap ? (
                      <div
                        className="seat-map__pair"
                        key={`${cum.cap}-${index}`}
                        role="group"
                        aria-label={`Cặp ghế Sweetbox ${cum.cap}`}
                      >
                        {cum.ds.map(veNutGhe)}
                      </div>
                    ) : (
                      cum.ds.map(veNutGhe)
                    ),
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <ul className="seat-legend">
        <li>
          <span className="seat-legend__swatch" aria-hidden="true" />
          Ghế trống
        </li>
        <li>
          <span className="seat-legend__swatch seat-legend__swatch--unavailable" aria-hidden="true" />
          Đã bán/đang được người khác giữ
        </li>
        <li>
          <span className="seat-legend__swatch seat-legend__swatch--selected" aria-hidden="true" />
          Đang chọn
        </li>
      </ul>

      <div id="dsVe">
        <SeatLines seats={daChon} onRemove={onXoa} />
      </div>

      <dl className="summary-list">
        <div className="total-row">
          <dt>Tổng tiền</dt>
          <dd className="numeric">{money(tongTien)}</dd>
        </div>
      </dl>
      <p className="muted">
        Điểm tích lũy dự kiến: <strong className="numeric">+{diemDuKien}</strong>
      </p>

      <div className="actions">
        <button type="button" className="btn btn--primary" onClick={onTiepTuc} disabled={!coTheTiepTuc}>
          Tiếp tục
        </button>
      </div>
    </Screen>
  );
}
