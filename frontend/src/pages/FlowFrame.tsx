import { useEffect, useRef, type ReactNode } from 'react';
import type { KhachHang } from '../types';

interface FlowFrameProps {
  khachHang: KhachHang;
  /** Nhãn bước, ví dụ "Bước 1 · Chọn phim". */
  kicker: string;
  title: string;
  description?: ReactNode;
  children: ReactNode;
}

/**
 * Khung chung cho chín trang đặt vé/đổi điểm: giữ tông sảnh tối + tấm vé giấy
 * của AuthFrame/Home, đồng thời đưa focus tới tiêu đề sau mỗi lần chuyển trang.
 */
export default function FlowFrame({
  khachHang,
  kicker,
  title,
  description,
  children,
}: FlowFrameProps) {
  const tieuDeRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    tieuDeRef.current?.focus();
  }, []);

  const hoTen = khachHang.hoTen?.trim() || khachHang.username;

  return (
    <main className="flow-shell">
      <header className="home-header">
        <p className="wordmark">CineMan</p>
        <p className="home-user">Xin chào, {hoTen}</p>
      </header>

      <section className="flow-intro">
        <p className="home-kicker">{kicker}</p>
        <h1 className="home-title" id="tieuDeTrang" tabIndex={-1} ref={tieuDeRef}>
          {title}
        </h1>
        {description ? <p className="home-copy">{description}</p> : null}
      </section>

      {children}

      <p className="ticket-footnote">CineMan · Hệ thống quản lý rạp chiếu</p>
    </main>
  );
}
