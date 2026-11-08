import { DataTable, ListState, Screen } from '../ui/common';
import type { KhungGioUI, PhimUI, ViewState } from '../ui/models';

export interface GDKhungGioProps {
  phim: PhimUI;
  ngay: string;
  ngayKhaDung: string[];
  khungGio: KhungGioUI[];
  state?: ViewState;
  error?: string;
  onNgayChange: (ngay: string) => void;
  onChon: (khungGio: KhungGioUI) => void;
}

/**
 * VE-03 — Chọn ngày chiếu và khung giờ.
 * Ngày là chuỗi hiển thị dd/mm/yyyy do lớp điều khiển cung cấp; màn hình không tự dựng lịch.
 */
export default function GDKhungGio({
  phim,
  ngay,
  ngayKhaDung,
  khungGio,
  state,
  error,
  onNgayChange,
  onChon,
}: GDKhungGioProps) {
  const trangThai: ViewState = state ?? (khungGio.length === 0 ? 'empty' : 'ready');
  const ngayDangChon = ngayKhaDung.includes(ngay) ? ngay : '';

  return (
    <Screen title={`Chọn ngày và khung giờ — Phim ${phim.ten}`}>
      <div className="field booking-date">
        <label htmlFor="ngayChieu">Ngày chiếu</label>
        <select
          id="ngayChieu"
          value={ngayDangChon}
          onChange={(event) => onNgayChange(event.target.value)}
          disabled={ngayKhaDung.length === 0}
        >
          {ngayDangChon === '' ? <option value="">Chọn ngày chiếu</option> : null}
          {ngayKhaDung.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
      </div>

      <ListState state={trangThai} error={error}>
        <div id="tblListKhungGio">
          <DataTable caption={`Khung giờ khả dụng của phim ${phim.ten}`}>
            <thead>
              <tr>
                <th scope="col">TT</th>
                <th scope="col">Giờ bắt đầu</th>
                <th scope="col">Giờ kết thúc</th>
                <th scope="col">Chọn</th>
              </tr>
            </thead>
            <tbody>
              {khungGio.map((item, index) => (
                <tr key={item.id}>
                  <td>{index + 1}</td>
                  <th scope="row">{item.batDau}</th>
                  <td>{item.ketThuc}</td>
                  <td>
                    <button type="button" className="btn btn--ghost" onClick={() => onChon(item)}>
                      Chọn
                      <span className="sr-only"> khung giờ {item.batDau} – {item.ketThuc}</span>
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
