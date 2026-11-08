import { DataTable, ListState, Notice, Screen } from '../ui/common';
import type { QuaTangUI, ViewState } from '../ui/models';

export interface GDChonQuaProps {
  khachHang: string;
  diem: number;
  qua: QuaTangUI[];
  state?: ViewState;
  error?: string;
  onChon: (qua: QuaTangUI) => void;
}

/**
 * DT-01 — Danh mục phần thưởng.
 * Chỉ hiển thị dữ liệu do lớp điều khiển truyền vào; không tự kiểm tra điểm,
 * không tự trừ điểm/tồn kho và không gọi API đổi thưởng.
 */
export default function GDChonQua({
  khachHang,
  diem,
  qua,
  state = 'ready',
  error,
  onChon,
}: GDChonQuaProps) {
  return (
    <Screen
      title="Đổi điểm thưởng lấy quà tặng/vé"
      subtitle={`Thành viên: ${khachHang}`}
      action={
        <p className="reward-balance">
          <span className="muted">Điểm hiện có:</span>{' '}
          <strong id="outTongDiem">{diem} điểm</strong>
        </p>
      }
    >
      {error && state !== 'error' && (
        <Notice tone="error" id="thongBaoChonQua">
          {error}
        </Notice>
      )}

      <div id="tblListQua">
        <ListState state={state} error={error}>
          <DataTable caption="Danh mục phần thưởng đổi điểm">
            <thead>
              <tr>
                <th scope="col">TT</th>
                <th scope="col">Mã</th>
                <th scope="col">Tên phần thưởng</th>
                <th scope="col">Loại</th>
                <th scope="col" className="numeric">Điểm đổi</th>
                <th scope="col" className="numeric">Tồn kho</th>
                <th scope="col">Mô tả</th>
                <th scope="col">Chọn</th>
              </tr>
            </thead>
            <tbody>
              {qua.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty-state">
                    Chưa có phần thưởng khả dụng.
                  </td>
                </tr>
              ) : (
                qua.map((phanThuong, index) => {
                  const hetHang = phanThuong.tonKho <= 0;
                  return (
                    <tr key={phanThuong.id}>
                      <td>{index + 1}</td>
                      <td>{phanThuong.ma}</td>
                      <th scope="row">{phanThuong.ten}</th>
                      <td>{phanThuong.loai}</td>
                      <td className="numeric">{phanThuong.diem}</td>
                      <td className="numeric">{phanThuong.tonKho}</td>
                      <td>{phanThuong.moTa}</td>
                      <td>
                        {hetHang ? (
                          <span className="status-pill">Hết hàng</span>
                        ) : (
                          <button
                            type="button"
                            className="btn btn--ghost"
                            aria-label={`Đổi phần thưởng ${phanThuong.ten}`}
                            onClick={() => onChon(phanThuong)}
                          >
                            Đổi
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </DataTable>
        </ListState>
      </div>
    </Screen>
  );
}
