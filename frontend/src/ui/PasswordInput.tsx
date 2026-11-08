import { useState, type ComponentProps } from 'react';

export default function PasswordInput(props: ComponentProps<'input'>) {
  const [visible, setVisible] = useState(false);
  return <div className="password-field">
    <input {...props} type={visible ? 'text' : 'password'} />
    <button className="password-toggle" type="button" disabled={props.disabled} aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? 'Ẩn' : 'Hiện'}</button>
  </div>;
}
