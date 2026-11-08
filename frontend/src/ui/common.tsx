import { useEffect, useRef, useState, type ReactNode } from 'react';
import type { GheUI, SuatChieuUI, ViewState } from './models';

const dong = new Intl.NumberFormat('vi-VN');
export function money(value: number) { return `${dong.format(value)}đ`; }

export function Screen({ title, subtitle, children, action }: {
  title: string; subtitle?: string; children: ReactNode; action?: ReactNode;
}) {
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => { heading.current?.focus({ preventScroll: true }); }, [title]);
  return <section className="ui-page">
    <header className="page-heading">
      <div><h1 ref={heading} tabIndex={-1}>{title}</h1>{subtitle && <p className="page-subtitle">{subtitle}</p>}</div>
      {action}
    </header>
    {children}
  </section>;
}

export function Notice({ children, tone = 'info', id }: {
  children: ReactNode; tone?: 'info' | 'error' | 'success' | 'warning'; id?: string;
}) {
  return <div id={id} className={`notice notice--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function DataTable({ caption, children }: { caption: string; children: ReactNode }) {
  const scroller = useRef<HTMLDivElement>(null);
  const [overflow, setOverflow] = useState(false);
  useEffect(() => {
    const element = scroller.current;
    if (!element) return;
    const observer = new ResizeObserver(() => setOverflow(element.scrollWidth > element.clientWidth));
    observer.observe(element);
    if (element.firstElementChild) observer.observe(element.firstElementChild);
    return () => observer.disconnect();
  }, []);
  return <div className="table-container">
    <div className="table-scroll" ref={scroller} role="region" aria-label={caption} tabIndex={0}>
      <table className="data-table"><caption className="sr-only">{caption}</caption>{children}</table>
    </div>
    <div className="table-navigation" hidden={!overflow}>
      <span>Cuộn ngang để xem đủ thông tin</span>
      <div><button type="button" className="scroll-button" aria-label="Cuộn bảng sang trái" onClick={() => scroller.current?.scrollBy({ left: -240, behavior: 'smooth' })}>←</button>
      <button type="button" className="scroll-button" aria-label="Cuộn bảng sang phải" onClick={() => scroller.current?.scrollBy({ left: 240, behavior: 'smooth' })}>→</button></div>
    </div>
  </div>;
}

export function ListState({ state = 'ready', error, children }: {
  state?: ViewState; error?: string; children: ReactNode;
}) {
  if (state === 'error') return <Notice tone="error">{error || 'Không tải được dữ liệu. Vui lòng thử lại.'}</Notice>;
  if (state === 'empty') return <p className="empty-state">Chưa có dữ liệu khả dụng.</p>;
  if (state === 'loading') return <div className="loading-rows" aria-busy="true" role="status"><span className="sr-only">Đang tải danh sách…</span>{[1, 2, 3].map((row) => <div className="skeleton-row" key={row}><span /><span /><span /></div>)}</div>;
  return <>{children}</>;
}

export function BookingMeta({ suatChieu }: { suatChieu: SuatChieuUI }) {
  return <p className="booking-meta">{suatChieu.phim.ten} <span>·</span> {suatChieu.ma} <span>·</span> {suatChieu.ngay} <span>·</span> {suatChieu.khungGio.batDau} – {suatChieu.khungGio.ketThuc} <span>·</span> {suatChieu.phong} ({suatChieu.dinhDang})</p>;
}

export function SeatLines({ seats, onRemove }: { seats: GheUI[]; onRemove?: (ghe: GheUI) => void }) {
  return <DataTable caption="Danh sách vé theo ghế đã chọn">
    <thead><tr><th scope="col">TT</th><th scope="col">Hàng</th><th scope="col">Số ghế</th><th scope="col">Loại ghế</th><th scope="col" className="numeric">Phụ thu</th><th scope="col" className="numeric">Đơn giá</th>{onRemove && <th scope="col">Xóa</th>}</tr></thead>
    <tbody>{seats.length ? seats.map((ghe, index) => <tr key={ghe.id}><td>{index + 1}</td><th scope="row">{ghe.hang}</th><td>{ghe.so}</td><td>{ghe.loai}</td><td className="numeric">{money(ghe.phuThu)}</td><td className="numeric">{money(ghe.donGia)}</td>{onRemove && <td><button type="button" className="btn btn--ghost" aria-label={`Xóa ghế ${ghe.hang}${ghe.so}`} onClick={() => onRemove(ghe)}>Xóa</button></td>}</tr>) : <tr><td colSpan={onRemove ? 7 : 6} className="empty-state">Chưa chọn ghế nào. Chọn ghế trống trên sơ đồ.</td></tr>}</tbody>
  </DataTable>;
}
