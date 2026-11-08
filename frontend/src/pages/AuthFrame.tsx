import type { ReactNode } from 'react';

interface AuthFrameProps {
  heading: string;
  description: string;
  children: ReactNode;
}

/**
 * Khung chung cho hai trang xác thực: cột thương hiệu + tấm vé giấy chứa biểu mẫu.
 */
export default function AuthFrame({ heading, description, children }: AuthFrameProps) {
  return (
    <main className="auth-shell">
      <aside className="brand-rail">
        <p className="wordmark">CineMan</p>
        <p className="brand-kicker">Rạp chiếu phim · Phòng vé trực tuyến</p>
        <h1 className="brand-heading">
          Một tấm vé,
          <br />
          một buổi tối trọn vẹn.
        </h1>
        <p className="brand-copy">
          Đăng nhập để giữ ghế, tích điểm thành viên và đổi quà ngay tại rạp.
        </p>
        <div className="filmstrip" aria-hidden="true" />
      </aside>

      <section className="ticket-column" aria-label="Biểu mẫu tài khoản">
        <div className="ticket">
          <header className="ticket-header">
            <p className="ticket-brand">CineMan</p>
            <h2 className="ticket-title">{heading}</h2>
            <p className="ticket-description">{description}</p>
          </header>
          {children}
        </div>
        <p className="ticket-footnote">CineMan · Hệ thống quản lý rạp chiếu</p>
      </section>
    </main>
  );
}
