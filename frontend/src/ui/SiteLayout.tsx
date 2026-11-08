import type { ReactNode } from 'react';

export default function SiteLayout({ children, name, actions }: { children: ReactNode; name?: string; actions?: ReactNode }) {
  return <div className="site-shell">
    <a className="skip-link" href="#noiDung">Đến nội dung chính</a>
    <header className="site-header">
      <a className="wordmark" href="/" aria-label="CineMan — Trang chủ">CineMan</a>
      <div className="header-actions">{name && <span className="header-user">{name}</span>}{actions}</div>
    </header>
    <main className="site-content" id="noiDung">{children}</main>
  </div>;
}
