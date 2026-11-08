import { DataTable, ListState, Notice, Screen } from '../ui/common';
import type { DoiDiemUI, QuaTangUI, ViewState } from '../ui/models';

export interface GDChonQuaProps {
  khachHang: string;
  diem: number;
  qua: QuaTangUI[];
  lichSu: DoiDiemUI[];
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
  lichSu,
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
      <section aria-labelledby="tieuDeLichSu">
        <h2 id="tieuDeLichSu">Lịch sử đổi quà</h2>
        <p className="muted">Mới nhất trước. Điểm còn lại là số dư tại thời điểm đổi.</p>
        <div id="tblLichSuDoiQua">
          <ListState state={state === 'empty' ? 'ready' : state} error={error}>
            {lichSu.length === 0 ? (
              <p className="empty-state">Chưa có lịch sử đổi quà.</p>
            ) : (
              <DataTable caption="Lịch sử phiếu đổi quà">
                <thead>
                  <tr>
                    <th scope="col">Mã phiếu</th>
                    <th scope="col">Ngày đổi</th>
                    <th scope="col">Phần thưởng</th>
                    <th scope="col" className="numeric">Điểm đã trừ</th>
                    <th scope="col" className="numeric">Điểm còn lại lúc đổi</th>
                    <th scope="col">Mã voucher</th>
                    <th scope="col">Hạn sử dụng</th>
                    <th scope="col">Trạng thái voucher</th>
                  </tr>
                </thead>
                <tbody>
                  {lichSu.map((phieu) => (
                    <tr key={phieu.maPhieu}>
                      <th scope="row">{phieu.maPhieu}</th>
                      <td>{phieu.thoiGian}</td>
                      <td>{phieu.qua.ma} · {phieu.qua.ten}</td>
                      <td className="numeric">{phieu.diemTru}</td>
                      <td className="numeric">{phieu.diemSau}</td>
                      <td>{phieu.maVoucher}</td>
                      <td>{phieu.hanSuDung}</td>
                      <td>
                        <span className="status-pill">
                          {phieu.trangThaiVoucher === 'DA_SU_DUNG' ? 'Đã sử dụng' : 'Chưa sử dụng'}
                        </span>
                        {phieu.ngaySuDung && <p>Ngày dùng: {phieu.ngaySuDung}</p>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </DataTable>
            )}
          </ListState>
        </div>
      </section>
    </Screen>
  );
}
