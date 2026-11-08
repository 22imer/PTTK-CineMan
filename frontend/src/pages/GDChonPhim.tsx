import { DataTable, ListState, Screen } from '../ui/common';
import type { PhimUI, ViewState } from '../ui/models';

export interface GDChonPhimProps {
  phim: PhimUI[];
  state?: ViewState;
  error?: string;
  onChon: (phim: PhimUI) => void;
}

/**
 * VE-02 — Chọn phim.
 * Chỉ hiển thị dữ liệu do lớp điều khiển cung cấp; không tự lọc phim hay suất chiếu.
 */
export default function GDChonPhim({ phim, state, error, onChon }: GDChonPhimProps) {
  const trangThai: ViewState = state ?? (phim.length === 0 ? 'empty' : 'ready');

  return (
    <Screen title="Đặt vé trực tuyến — Chọn phim">
      <ListState state={trangThai} error={error}>
        <div id="tblListPhim">
          <DataTable caption="Danh sách phim đang mở bán">
            <thead>
              <tr>
                <th scope="col">TT</th>
                <th scope="col">Mã</th>
                <th scope="col">Tên phim</th>
                <th scope="col">Thể loại</th>
                <th scope="col" className="numeric">Thời lượng</th>
                <th scope="col">Độ tuổi</th>
                <th scope="col">Chọn</th>
              </tr>
            </thead>
            <tbody>
              {phim.map((item, index) => (
                <tr key={item.id}>
                  <td>{index + 1}</td>
                  <th scope="row">{item.ma}</th>
                  <td>{item.ten}</td>
                  <td>{item.theLoai}</td>
                  <td className="numeric">{item.thoiLuong} phút</td>
                  <td>{item.doTuoi}</td>
                  <td>
                    <button type="button" className="btn btn--ghost" onClick={() => onChon(item)}>
                      Chọn
                      <span className="sr-only"> phim {item.ten}</span>
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
