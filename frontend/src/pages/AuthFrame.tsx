import type { ReactNode } from 'react';

interface AuthFrameProps {
  heading: string;
  description: string;
  children: ReactNode;
}

/** Khung biểu mẫu theo DK-02/DK-03, dùng chung token với các màn nghiệp vụ. */
export default function AuthFrame({ heading, description, children }: AuthFrameProps) {
  return (
    <section className={`auth-frame ${heading === 'Đăng ký thành viên' ? 'auth-frame--register' : ''}`}>
      <header className="page-heading">
        <div>
          <h1>{heading}</h1>
          <p className="page-subtitle">{description}</p>
        </div>
      </header>
      {children}
    </section>
  );
}
