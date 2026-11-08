import { useEffect, useRef, useState, type FormEvent } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { KhachHang } from '../types';
import AuthFrame from './AuthFrame';

const THONG_BAO_DANG_KY_THANH_CONG = 'Đăng ký tài khoản thành viên thành công!';
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SDT_PATTERN = /^0\d{9}$/;
// Giới hạn khớp ThanhVienDAO/schema để lỗi hiện ngay tại trường thay vì 400 từ máy chủ.
const MAT_KHAU_TOI_THIEU = 8;
const MAT_KHAU_TOI_DA_BYTE = 72;
const USERNAME_TOI_DA = 50;
const HO_TEN_TOI_DA = 100;
const DIA_CHI_TOI_DA = 100;
const EMAIL_TOI_DA = 100;

type FieldKey =
  | 'hoTen'
  | 'ngaySinh'
  | 'gioiTinh'
  | 'sdt'
  | 'diaChi'
  | 'email'
  | 'username'
  | 'password'
  | 'xacNhanMatKhau';

interface FormValues {
  hoTen: string;
  ngaySinh: string;
  gioiTinh: string;
  sdt: string;
  diaChi: string;
  email: string;
  username: string;
  password: string;
  xacNhanMatKhau: string;
}

type FormErrors = Partial<Record<FieldKey, string>>;

const GIA_TRI_BAN_DAU: FormValues = {
  hoTen: '',
  ngaySinh: '',
  gioiTinh: '',
  sdt: '',
  diaChi: '',
  email: '',
  username: '',
  password: '',
  xacNhanMatKhau: '',
};

const THU_TU_KIEM_TRA: FieldKey[] = [
  'hoTen',
  'ngaySinh',
  'gioiTinh',
  'sdt',
  'email',
  'diaChi',
  'username',
  'password',
  'xacNhanMatKhau',
];

const ID_THEO_TRUONG: Record<FieldKey, string> = {
  hoTen: 'inHoTen',
  ngaySinh: 'inNgaySinh',
  gioiTinh: 'inGioiTinh',
  sdt: 'inSodienthoai',
  diaChi: 'inDiaChi',
  email: 'inemail',
  username: 'inusername',
  password: 'inpassword',
  xacNhanMatKhau: 'inxacnhanmatkhau',
};

type FieldElement = HTMLInputElement | HTMLSelectElement;

function kiemTra(values: FormValues): FormErrors {
  const errors: FormErrors = {};

  if (!values.hoTen.trim()) {
    errors.hoTen = 'Vui lòng nhập họ tên.';
  } else if (values.hoTen.trim().length > HO_TEN_TOI_DA) {
    errors.hoTen = `Họ tên không được vượt quá ${HO_TEN_TOI_DA} ký tự.`;
  }
  if (!values.ngaySinh) {
    errors.ngaySinh = 'Vui lòng chọn ngày sinh.';
  }
  if (!values.gioiTinh) {
    errors.gioiTinh = 'Vui lòng chọn giới tính.';
  }
  if (!values.sdt.trim()) {
    errors.sdt = 'Vui lòng nhập số điện thoại.';
  } else if (!SDT_PATTERN.test(values.sdt.trim())) {
    errors.sdt = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.';
  }
  if (!values.diaChi.trim()) {
    errors.diaChi = 'Vui lòng nhập địa chỉ.';
  } else if (values.diaChi.trim().length > DIA_CHI_TOI_DA) {
    errors.diaChi = `Địa chỉ không được vượt quá ${DIA_CHI_TOI_DA} ký tự.`;
  }
  if (!values.email.trim()) {
    errors.email = 'Vui lòng nhập email.';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Email không hợp lệ.';
  } else if (values.email.trim().length > EMAIL_TOI_DA) {
    errors.email = `Email không được vượt quá ${EMAIL_TOI_DA} ký tự.`;
  }
  if (!values.username.trim()) {
    errors.username = 'Vui lòng nhập tên đăng nhập.';
  } else if (values.username.trim().length > USERNAME_TOI_DA) {
    errors.username = `Tên đăng nhập không được vượt quá ${USERNAME_TOI_DA} ký tự.`;
  }
  if (!values.password.trim()) {
    errors.password = 'Vui lòng nhập mật khẩu.';
  } else if (values.password.length < MAT_KHAU_TOI_THIEU) {
    errors.password = `Mật khẩu phải có ít nhất ${MAT_KHAU_TOI_THIEU} ký tự.`;
  } else if (new TextEncoder().encode(values.password).length > MAT_KHAU_TOI_DA_BYTE) {
    errors.password = 'Mật khẩu quá dài, vui lòng chọn mật khẩu ngắn hơn.';
  }
  if (!values.xacNhanMatKhau) {
    errors.xacNhanMatKhau = 'Vui lòng nhập lại mật khẩu.';
  } else if (values.xacNhanMatKhau !== values.password) {
    errors.xacNhanMatKhau = 'Mật khẩu nhập lại không khớp.';
  }

  return errors;
}

interface GDDangKyTaiKhoanProps {
  onHuy: () => void;
  onDangKyThanhCong: (thongBao: string) => void;
}

export default function GDDangKyTaiKhoan({
  onHuy,
  onDangKyThanhCong,
}: GDDangKyTaiKhoanProps) {
  const [values, setValues] = useState<FormValues>(GIA_TRI_BAN_DAU);
  const [errors, setErrors] = useState<FormErrors>({});
  const [loiMayChu, setLoiMayChu] = useState('');
  const [dangGui, setDangGui] = useState(false);

  const fieldRefs = useRef<Partial<Record<FieldKey, FieldElement | null>>>({});
  const loiMayChuRef = useRef<HTMLParagraphElement>(null);

  // Trang đăng ký bắt đầu bằng hồ sơ: focus vào trường đầu tiên.
  useEffect(() => {
    fieldRefs.current.hoTen?.focus();
  }, []);

  function capNhat<K extends FieldKey>(field: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }));
    setLoiMayChu('');
    setErrors((current) => {
      if (!current[field]) {
        return current;
      }
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  function focusTruongDauTien(errorsMoi: FormErrors) {
    const field = THU_TU_KIEM_TRA.find((key) => errorsMoi[key]);
    if (field) {
      requestAnimationFrame(() => fieldRefs.current[field]?.focus());
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (dangGui) {
      return;
    }

    const errorsMoi = kiemTra(values);
    setErrors(errorsMoi);
    if (Object.keys(errorsMoi).length > 0) {
      setLoiMayChu('');
      focusTruongDauTien(errorsMoi);
      return;
    }

    setDangGui(true);
    setLoiMayChu('');

    try {
      await HTTPClient.fetch<KhachHang>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          kh: {
            username: values.username.trim(),
            password: values.password,
            hoTen: values.hoTen.trim(),
            ngaySinh: values.ngaySinh,
            gioiTinh: values.gioiTinh,
            diaChi: values.diaChi.trim(),
            email: values.email.trim(),
            sdt: values.sdt.trim(),
          },
          xacNhanMatKhau: values.xacNhanMatKhau,
        }),
      });
      setValues((current) => ({
        ...current,
        password: '',
        xacNhanMatKhau: '',
      }));
      onDangKyThanhCong(THONG_BAO_DANG_KY_THANH_CONG);
    } catch (error) {
      setLoiMayChu(
        error instanceof Error && error.message
          ? error.message
          : 'Không thể đăng ký. Vui lòng kiểm tra kết nối và thử lại.',
      );
      requestAnimationFrame(() => loiMayChuRef.current?.focus());
    } finally {
      setDangGui(false);
    }
  }

  function fieldProps(field: FieldKey) {
    const error = errors[field];
    return {
      id: ID_THEO_TRUONG[field],
      'aria-required': true,
      'aria-invalid': error ? true : undefined,
      'aria-describedby': error ? `loi-${field}` : undefined,
      disabled: dangGui,
    } as const;
  }

  function renderFieldError(field: FieldKey) {
    return errors[field] ? (
      <p className="field-error" id={`loi-${field}`}>
        {errors[field]}
      </p>
    ) : null;
  }

  function setRef(field: FieldKey) {
    return (element: FieldElement | null) => {
      fieldRefs.current[field] = element;
    };
  }

  return (
    <AuthFrame
      heading="Đăng ký thành viên"
      description="Điền đầy đủ thông tin để tạo tài khoản CineMan."
    >
      <p
        className="form-alert"
        id="outThongBao"
        role="alert"
        tabIndex={-1}
        ref={loiMayChuRef}
      >
        {loiMayChu}
      </p>

      <form className="form" onSubmit={handleSubmit} noValidate aria-busy={dangGui}>
        <div className="register-grid">
          <div className="field field--wide">
            <label htmlFor="inHoTen">Họ tên</label>
            <input
              {...fieldProps('hoTen')}
              name="hoTen"
              type="text"
              autoComplete="name"
              value={values.hoTen}
              onChange={(event) => capNhat('hoTen', event.target.value)}
              ref={setRef('hoTen')}
            />
            {renderFieldError('hoTen')}
          </div>

          <div className="field">
            <label htmlFor="inNgaySinh">Ngày sinh</label>
            <input
              {...fieldProps('ngaySinh')}
              name="ngaySinh"
              type="date"
              autoComplete="bday"
              value={values.ngaySinh}
              onChange={(event) => capNhat('ngaySinh', event.target.value)}
              ref={setRef('ngaySinh')}
            />
            {renderFieldError('ngaySinh')}
          </div>

          <div className="field">
            <label htmlFor="inGioiTinh">Giới tính</label>
            <select
              {...fieldProps('gioiTinh')}
              name="gioiTinh"
              value={values.gioiTinh}
              onChange={(event) => capNhat('gioiTinh', event.target.value)}
              ref={setRef('gioiTinh')}
            >
              <option value="">Chọn giới tính</option>
              <option value="Nam">Nam</option>
              <option value="Nữ">Nữ</option>
            </select>
            {renderFieldError('gioiTinh')}
          </div>

          <div className="field">
            <label htmlFor="inSodienthoai">Số điện thoại</label>
            <input
              {...fieldProps('sdt')}
              name="sdt"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              placeholder="0912345678"
              value={values.sdt}
              onChange={(event) => capNhat('sdt', event.target.value)}
              ref={setRef('sdt')}
            />
            {renderFieldError('sdt')}
          </div>

          <div className="field">
            <label htmlFor="inemail">Email</label>
            <input
              {...fieldProps('email')}
              name="email"
              type="email"
              inputMode="email"
              autoComplete="email"
              autoCapitalize="none"
              spellCheck={false}
              placeholder="ban@example.com"
              value={values.email}
              onChange={(event) => capNhat('email', event.target.value)}
              ref={setRef('email')}
            />
            {renderFieldError('email')}
          </div>

          <div className="field field--wide">
            <label htmlFor="inDiaChi">Địa chỉ</label>
            <input
              {...fieldProps('diaChi')}
              name="diaChi"
              type="text"
              autoComplete="street-address"
              value={values.diaChi}
              onChange={(event) => capNhat('diaChi', event.target.value)}
              ref={setRef('diaChi')}
            />
            {renderFieldError('diaChi')}
          </div>

          <div className="field field--wide">
            <label htmlFor="inusername">Tên đăng nhập</label>
            <input
              {...fieldProps('username')}
              name="username"
              type="text"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              value={values.username}
              onChange={(event) => capNhat('username', event.target.value)}
              ref={setRef('username')}
            />
            {renderFieldError('username')}
          </div>

          <div className="field">
            <label htmlFor="inpassword">Mật khẩu</label>
            <input
              {...fieldProps('password')}
              name="password"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={values.password}
              onChange={(event) => capNhat('password', event.target.value)}
              ref={setRef('password')}
            />
            {renderFieldError('password')}
          </div>

          <div className="field">
            <label htmlFor="inxacnhanmatkhau">Nhập lại mật khẩu</label>
            <input
              {...fieldProps('xacNhanMatKhau')}
              name="xacNhanMatKhau"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={values.xacNhanMatKhau}
              onChange={(event) => capNhat('xacNhanMatKhau', event.target.value)}
              ref={setRef('xacNhanMatKhau')}
            />
            {renderFieldError('xacNhanMatKhau')}
          </div>
        </div>

        <div className="form-actions form-actions--end">
          <button
            className="btn btn--ghost"
            id="btnHuy"
            type="button"
            onClick={onHuy}
            disabled={dangGui}
          >
            Hủy
          </button>
          <button
            className="btn btn--primary"
            id="btnDangKy"
            type="submit"
            disabled={dangGui}
          >
            {dangGui ? 'Đang đăng ký…' : 'Đăng ký'}
          </button>
        </div>
      </form>
    </AuthFrame>
  );
}
