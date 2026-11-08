import { useEffect, useRef } from 'react';
import { Notice } from '../ui/common';
import type { QuaTangUI } from '../ui/models';

export interface GDXacNhanDoiQuaProps {
  open: boolean;
  qua: QuaTangUI;
  diemTruoc: number;
  diemSau: number;
  onHuy: () => void;
  onXacNhan: () => void;
  busy?: boolean;
  error?: string;
}

/**
 * DT-02 — Popup xác nhận đổi điểm.
 * Dùng <dialog> gốc để có sẵn focus trap; tiêu điểm mở đầu rơi vào Hủy, không
 * vào nút xác nhận. Mọi thay đổi điểm/tồn kho thuộc lớp điều khiển.
 */
export default function GDXacNhanDoiQua({
  open,
  qua,
  diemTruoc,
  diemSau,
  onHuy,
  onXacNhan,
  busy = false,
  error,
}: GDXacNhanDoiQuaProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const triggerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }

    if (!open) {
      if (dialog.open) {
        dialog.close();
      }
      return;
    }

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onHuy();
      }
    };
    dialog.addEventListener('keydown', onEscape, true);

    if (!dialog.open) {
      const active = document.activeElement;
      triggerRef.current =
        active instanceof HTMLElement && active !== document.body ? active : null;
      dialog.showModal();
    }
    cancelRef.current?.focus({ preventScroll: true });

    return () => {
      dialog.removeEventListener('keydown', onEscape, true);
      if (dialog.open) {
        dialog.close();
      }
      const trigger = triggerRef.current;
      triggerRef.current = null;
      if (trigger?.isConnected) {
        trigger.focus({ preventScroll: true });
      }
    };
  }, [open, onHuy]);

  const xacNhan = () => {
    if (busy) {
      return;
    }
    onXacNhan();
  };

  return (
    <dialog
      ref={dialogRef}
      id="hopXacNhanDoiQua"
      className="reward-dialog"
      aria-labelledby="tieuDeXacNhanDoiQua"
    >
      <header className="reward-dialog-header">
        <h2 id="tieuDeXacNhanDoiQua">Xác nhận đổi điểm</h2>
      </header>

      <div className="reward-dialog-body">
        {error && (
          <Notice tone="error" id="outThongBao">
            {error}
          </Notice>
        )}

        <dl className="summary-list">
          <div>
            <dt>Phần thưởng đổi</dt>
            <dd>
              {qua.ten}
              <br />
              <span className="muted">Mã {qua.ma}</span>
            </dd>
          </div>
          <div>
            <dt>Điểm tích lũy hiện có</dt>
            <dd>{diemTruoc} điểm</dd>
          </div>
          <div>
            <dt>Số điểm quy đổi bị trừ</dt>
            <dd>−{qua.diem} điểm</dd>
          </div>
          <div>
            <dt>Số điểm còn lại sau đổi</dt>
            <dd>
              <strong>{diemSau} điểm</strong>
            </dd>
          </div>
          <div>
            <dt>Thời hạn sử dụng voucher</dt>
            <dd>30 ngày kể từ ngày đổi điểm</dd>
          </div>
        </dl>
      </div>

      <footer className="reward-dialog-footer">
        <button
          ref={cancelRef}
          type="button"
          id="btnQuayLai"
          className="btn btn--ghost"
          onClick={onHuy}
        >
          Hủy
        </button>
        <button
          type="button"
          id="btnXacNhan"
          className="btn btn--primary"
          aria-disabled={busy || undefined}
          aria-busy={busy || undefined}
          onClick={xacNhan}
        >
          Xác nhận đổi
        </button>
      </footer>
    </dialog>
  );
}
