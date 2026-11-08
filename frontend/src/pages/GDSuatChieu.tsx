import { DataTable, ListState, Screen, money } from '../ui/common';
import type { SuatChieuUI, ViewState } from '../ui/models';

export interface GDSuatChieuProps {
  suatChieu: SuatChieuUI[];
  state?: ViewState;
  error?: string;
  onChon: (suatChieu: SuatChieuUI) => void;
}

/**
 * VE-04 — Chọn suất chiếu.
 * Suất chiếu theo phim + ngày + khung giờ do lớp điều khiển tải sẵn; màn hình chỉ hiển thị.
 */
export default function GDSuatChieu({ suatChieu, state, error, onChon }: GDSuatChieuProps) {
  const trangThai: ViewState = state ?? (suatChieu.length === 0 ? 'empty' : 'ready');
  const dauTien = suatChieu[0];
  const phuDe = dauTien
    ? `${dauTien.phim.ten} — ${dauTien.ngay} — ${dauTien.khungGio.batDau} – ${dauTien.khungGio.ketThuc}`
    : undefined;

  return (
    <Screen title="Chọn suất chiếu" subtitle={phuDe}>
      <ListState state={trangThai} error={error}>
        <div id="tblListSuatChieu">
          <DataTable caption="Danh sách suất chiếu theo phim, ngày và khung giờ đã chọn">
            <thead>
              <tr>
                <th scope="col">TT</th>
                <th scope="col">Mã</th>
                <th scope="col">Ngày chiếu</th>
                <th scope="col">Khung giờ</th>
                <th scope="col">Phòng chiếu</th>
                <th scope="col" className="numeric">Giá cơ sở</th>
                <th scope="col">Chọn</th>
              </tr>
            </thead>
            <tbody>
              {suatChieu.map((item, index) => (
                <tr key={item.id}>
                  <td>{index + 1}</td>
                  <th scope="row">{item.ma}</th>
                  <td>{item.ngay}</td>
                  <td>{item.khungGio.batDau} – {item.khungGio.ketThuc}</td>
                  <td>{item.phong} ({item.dinhDang})</td>
                  <td className="numeric">{money(item.giaCoSo)}</td>
                  <td>
                    <button type="button" className="btn btn--ghost" onClick={() => onChon(item)}>
                      Chọn
                      <span className="sr-only"> suất chiếu {item.ma}</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        </div>
      </ListState>
    </Screen>
  );
}
