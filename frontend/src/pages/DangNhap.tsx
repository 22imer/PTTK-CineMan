import { useEffect, useRef, useState, type FormEvent } from 'react';
import { dangNhap } from '../api/authSession';
import type { KhachHang } from '../types';
import AuthFrame from './AuthFrame';
import PasswordInput from '../ui/PasswordInput';

interface DangNhapProps {
  /** Thông báo chuyển từ trang đăng ký sau khi tạo tài khoản thành công. */
  thongBao?: string;
  onDangNhapThanhCong?: (khachHang: KhachHang) => void;
  onMoDangKy: () => void;
}

export default function DangNhap({
  thongBao,
  onDangNhapThanhCong,
  onMoDangKy,
}: DangNhapProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loi, setLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);

  const usernameRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const loiRef = useRef<HTMLParagraphElement>(null);
  const thongBaoRef = useRef<HTMLParagraphElement>(null);

  // Trang đăng nhập được mở lại sau khi đăng ký: đưa focus tới thông báo thành công,
  // còn lần đầu vào trang thì focus sẵn ô tên đăng nhập.
  useEffect(() => {
    if (thongBao) {
      thongBaoRef.current?.focus();
      return;
    }
    usernameRef.current?.focus();
  }, [thongBao]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (dangGui) {
      return;
    }

    const tenDangNhap = username.trim();
    if (!tenDangNhap) {
      setLoi('Vui lòng nhập tên đăng nhập.');
      requestAnimationFrame(() => usernameRef.current?.focus());
      return;
    }
    if (!password) {
      setLoi('Vui lòng nhập mật khẩu.');
      requestAnimationFrame(() => passwordRef.current?.focus());
      return;
    }

    setDangGui(true);
    setLoi('');

    try {
      const phien = await dangNhap(tenDangNhap, password);
      setPassword('');
      onDangNhapThanhCong?.(phien.khachHang);
    } catch (error) {
      setLoi(
        error instanceof Error && error.message
          ? error.message
          : 'Không thể đăng nhập. Vui lòng kiểm tra kết nối và thử lại.',
      );
      requestAnimationFrame(() => loiRef.current?.focus());
    } finally {
      setDangGui(false);
    }
  }

  return (
    <AuthFrame
      heading="Đăng nhập"
      description="Dùng tài khoản thành viên để tiếp tục."
    >
      {thongBao ? (
        <p
          className="form-notice"
          id="outThongBao"
          role="status"
          tabIndex={-1}
          ref={thongBaoRef}
        >
          {thongBao}
        </p>
      ) : null}

      {loi ? (
        <p className="form-alert" id="loiDangNhap" role="alert" tabIndex={-1} ref={loiRef}>
          {loi}
        </p>
      ) : null}

      <form
        className="form"
        onSubmit={handleSubmit}
        noValidate
        aria-busy={dangGui}
      >
        <div className="field">
          <label htmlFor="inusername">Tên đăng nhập</label>
          <input
            id="inusername"
            name="username"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            value={username}
            onChange={(event) => {
              setUsername(event.target.value);
              setLoi('');
            }}
            ref={usernameRef}
            disabled={dangGui}
            aria-required={true}
            aria-invalid={loi ? true : undefined}
            aria-describedby={loi ? 'loiDangNhap' : undefined}
          />
        </div>

        <div className="field">
          <label htmlFor="inpassword">Mật khẩu</label>
          <PasswordInput
            id="inpassword"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => {
              setPassword(event.target.value);
              setLoi('');
            }}
            ref={passwordRef}
            disabled={dangGui}
            aria-required={true}
            aria-invalid={loi ? true : undefined}
            aria-describedby={loi ? 'loiDangNhap' : undefined}
          />
        </div>

        <div className="form-actions">
          <button className="btn btn--primary" id="btnDangNhap" type="submit" disabled={dangGui}>
            {dangGui ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </div>
      </form>

      <p className="auth-switch">
        Chưa có tài khoản?{' '}
        <button
          className="link-button"
          id="btnDangKy"
          type="button"
          onClick={onMoDangKy}
          disabled={dangGui}
        >
          Đăng ký thành viên
        </button>
      </p>
    </AuthFrame>
  );
}
