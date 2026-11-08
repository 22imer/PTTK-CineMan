import { useEffect, useMemo, useRef, useState } from 'react';
import { HTTPClient } from '../api/httpClient';
import type { Ghe, KhachHang, SuatChieu, ThongTinGhe, TomTatHoaDon } from '../types';
import {
  dinhDangGio,
  dinhDangNgay,
  dinhDangTien,
  laSweetbox,
  maPhim,
  maSuatChieu,
  nhanGhe,
  thongDiepLoi,
} from '../utils';
import FlowFrame from './FlowFrame';

const SO_GHE_TOI_DA = 8;

interface GDChonGheProps {
  khachHang: KhachHang;
  suatChieu: SuatChieu;
  onTiepTuc: (tomTat: TomTatHoaDon) => void;
  onQuayLai: () => void;
}

export default function GDChonGhe({
  khachHang,
  suatChieu,
  onTiepTuc,
  onQuayLai,
}: GDChonGheProps) {
  const [dsGhe, setDsGhe] = useState<Ghe[]>([]);
  const [gheDangGiu, setGheDangGiu] = useState<Set<number>>(new Set());
  const [suatHienTai, setSuatHienTai] = useState<SuatChieu>(suatChieu);
  const [daChon, setDaChon] = useState<Set<number>>(new Set());
  const [dangTai, setDangTai] = useState(true);
  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState('');

  const loiRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (loi) {
      requestAnimationFrame(() => loiRef.current?.focus());
    }
  }, [loi]);

  async function taiSoDo(): Promise<void> {
    setDangTai(true);
    try {
      const thongTin = await HTTPClient.fetch<ThongTinGhe>(
        `/api/ghe?suatChieuId=${encodeURIComponent(String(suatChieu.id))}`,
      );
      const danhSachGhe = Array.isArray(thongTin.dsGhe) ? thongTin.dsGhe : [];
      const ban = new Set(
        (Array.isArray(thongTin.dsVe) ? thongTin.dsVe : [])
          .filter((ve) => ve.trangThaiVe === 'DANG_GIU' || ve.trangThaiVe === 'DA_BAN')
          .map((ve) => ve.ghe.id),
      );
      setDsGhe(danhSachGhe);
      setGheDangGiu(ban);
      if (thongTin.suatChieu) {
        setSuatHienTai(thongTin.suatChieu);
      }
      setDaChon((truoc) => {
        const conChon = Array.from(truoc).filter(
          (id) => !ban.has(id) && danhSachGhe.some((ghe) => ghe.id === id),
        );
        return new Set(conChon);
      });
    } catch (error) {
      setLoi(thongDiepLoi(error, 'Không thể tải sơ đồ ghế. Vui lòng thử lại.'));
    } finally {
      setDangTai(false);
    }
  }

  useEffect(() => {
    let huy = false;

    async function taiLanDau() {
      setDangTai(true);
      setLoi('');
      try {
        const thongTin = await HTTPClient.fetch<ThongTinGhe>(
          `/api/ghe?suatChieuId=${encodeURIComponent(String(suatChieu.id))}`,
        );
        if (!huy) {
          const ban = new Set(
            (Array.isArray(thongTin.dsVe) ? thongTin.dsVe : [])
              .filter((ve) => ve.trangThaiVe === 'DANG_GIU' || ve.trangThaiVe === 'DA_BAN')
              .map((ve) => ve.ghe.id),
          );
          setDsGhe(Array.isArray(thongTin.dsGhe) ? thongTin.dsGhe : []);
          setGheDangGiu(ban);
          if (thongTin.suatChieu) {
            setSuatHienTai(thongTin.suatChieu);
          }
        }
      } catch (error) {
        if (!huy) {
          setLoi(thongDiepLoi(error, 'Không thể tải sơ đồ ghế. Vui lòng thử lại.'));
        }
      } finally {
        if (!huy) {
          setDangTai(false);
        }
      }
    }

    void taiLanDau();
    return () => {
      huy = true;
    };
  }, [suatChieu.id]);

  const dsHang = useMemo(() => {
    const nhom = new Map<string, Ghe[]>();
    for (const ghe of dsGhe) {
      const hang = nhom.get(ghe.hangGhe);
      if (hang) {
        hang.push(ghe);
      } else {
        nhom.set(ghe.hangGhe, [ghe]);
      }
    }
    return Array.from(nhom.entries())
      .sort(([a], [b]) => a.localeCompare(b, 'vi'))
      .map(([hang, ghes]) => ({
        hang,
        ghes: [...ghes].sort((a, b) => Number(a.soGhe) - Number(b.soGhe)),
      }));
  }, [dsGhe]);

  const banCua = useMemo(() => {
    const tim: Record<number, Ghe | null> = {};
    for (const ghe of dsGhe) {
      if (!laSweetbox(ghe)) {
        tim[ghe.id] = null;
        continue;
      }
      const so = Number(ghe.soGhe);
      if (!Number.isFinite(so)) {
        tim[ghe.id] = null;
        continue;
      }
      const soBan = so % 2 === 1 ? so + 1 : so - 1;
      const ungVien = dsGhe.find(
        (khac) =>
          khac.id !== ghe.id &&
          khac.hangGhe === ghe.hangGhe &&
          khac.loaiGhe?.id === ghe.loaiGhe?.id &&
          Number(khac.soGhe) === soBan &&
          Math.min(so, soBan) % 2 === 1,
      );
      tim[ghe.id] = ungVien || null;
    }
    return tim;
  }, [dsGhe]);

  const dsGheDaChon = useMemo(
    () => dsGhe.filter((ghe) => daChon.has(ghe.id)),
    [dsGhe, daChon],
  );

  // Ghế vừa bị người khác giữ (hoặc cặp Sweetbox mất một nửa) phải rời khỏi lựa chọn,
  // tránh gửi lên máy chủ một cặp không còn hợp lệ.
  useEffect(() => {
    setDaChon((truoc) => {
      let thayDoi = false;
      const moi = new Set<number>();
      for (const id of truoc) {
        const ghe = dsGhe.find((ungVien) => ungVien.id === id);
        if (!ghe || gheDangGiu.has(id)) {
          thayDoi = true;
          continue;
        }
        if (laSweetbox(ghe)) {
          const ban = banCua[ghe.id];
          if (!ban || gheDangGiu.has(ban.id)) {
            thayDoi = true;
            continue;
          }
        }
        moi.add(id);
      }
      return thayDoi ? moi : truoc;
    });
  }, [dsGhe, gheDangGiu, banCua]);

  const maPhimHienTai = suatHienTai.phim?.id ? maPhim(suatHienTai.phim.id) : '—';

  const giaGhe = (ghe: Ghe): number => suatHienTai.giaCoSo + (ghe.loaiGhe?.phuThu || 0);
  const tamTinh = dsGheDaChon.reduce((tong, ghe) => tong + giaGhe(ghe), 0);

  function biKhoa(ghe: Ghe): boolean {
    if (gheDangGiu.has(ghe.id)) {
      return true;
    }
    if (laSweetbox(ghe)) {
      const ban = banCua[ghe.id];
      return !ban || gheDangGiu.has(ban.id);
    }
    return false;
  }

  function chonGhe(ghe: Ghe) {
    if (dangTai || dangGui || gheDangGiu.has(ghe.id)) {
      return;
    }

    const ban = laSweetbox(ghe) ? banCua[ghe.id] : null;
    if (laSweetbox(ghe) && (!ban || gheDangGiu.has(ban.id))) {
      setLoi(
        `Cặp Sweetbox của ghế ${nhanGhe(ghe)} không còn đủ hai ghế liền kề. Vui lòng chọn cặp khác.`,
      );
      return;
    }

    const nhom = ban ? [ghe, ban] : [ghe];
    if (nhom.every((item) => daChon.has(item.id))) {
      setDaChon((truoc) => {
        const moi = new Set(truoc);
        nhom.forEach((item) => moi.delete(item.id));
        return moi;
      });
      setLoi('');
      return;
    }

    const them = nhom.filter((item) => !daChon.has(item.id)).length;
    if (daChon.size + them > SO_GHE_TOI_DA) {
      setLoi(`Mỗi hóa đơn chỉ được chọn tối đa ${SO_GHE_TOI_DA} ghế.`);
      return;
    }

    setDaChon((truoc) => {
      const moi = new Set(truoc);
      nhom.forEach((item) => moi.add(item.id));
      return moi;
    });
    setLoi('');
  }

  async function tiepTuc() {
    if (dangGui || daChon.size === 0 || daChon.size > SO_GHE_TOI_DA) {
      return;
    }

    setDangGui(true);
    setLoi('');
    try {
      const tomTat = await HTTPClient.fetch<TomTatHoaDon>('/api/hoadon', {
        method: 'POST',
        body: JSON.stringify({
          suatChieuId: suatChieu.id,
          dsGhe: dsGheDaChon.map((ghe) => ghe.id),
        }),
      });
      onTiepTuc(tomTat);
    } catch (error) {
      setLoi(
        thongDiepLoi(
          error,
          'Không thể tạo hóa đơn giữ ghế. Vui lòng kiểm tra lại ghế và thử lại.',
        ),
      );
      // Ghế có thể vừa bị người khác giữ: tải lại sơ đồ để trạng thái khớp máy chủ.
      await taiSoDo();
    } finally {
      setDangGui(false);
    }
  }

  return (
    <FlowFrame
      khachHang={khachHang}
      kicker="Bước 4 · Chọn ghế"
      title="Chọn chỗ ngồi của bạn"
      description={`${maPhimHienTai} · ${maSuatChieu(suatHienTai.id)} · ${suatHienTai.phim?.tenPhim || 'Suất chiếu'} · ${dinhDangGio(suatHienTai.khungGio?.gioBatDau)} ngày ${dinhDangNgay(suatHienTai.ngayChieu)} · ${suatHienTai.phongChieu?.tenPhong || 'Phòng chiếu'} (${suatHienTai.phongChieu?.loaiPhong || '—'}).`}
    >
      <section className="flow-card" aria-labelledby="tieuDeSoDoGhe">
        <div className="section-head section-head--split">
          <div>
            <h2 id="tieuDeSoDoGhe">Sơ đồ ghế</h2>
            <p className="section-note">
              Ghế trống màu sáng, ghế đã bán/đang giữ màu xám. Sweetbox được chọn theo cặp liền kề.
            </p>
          </div>
          <p className="seat-limit">
            Đã chọn <strong>{daChon.size}</strong>/{SO_GHE_TOI_DA} ghế
          </p>
        </div>

        <p
          className="form-alert"
          role="alert"
          tabIndex={-1}
          ref={loiRef}
          aria-live="assertive"
        >
          {loi}
        </p>

        <ul className="seat-legend" aria-label="Chú thích sơ đồ ghế">
          <li>
            <span className="legend-swatch legend-swatch--free" aria-hidden="true" /> Ghế trống
          </li>
          <li>
            <span className="legend-swatch legend-swatch--selected" aria-hidden="true" /> Đang chọn
          </li>
          <li>
            <span className="legend-swatch legend-swatch--busy" aria-hidden="true" /> Đã bán / đang giữ
          </li>
          <li>
            <span className="legend-swatch legend-swatch--sweetbox" aria-hidden="true" /> Sweetbox (chọn theo cặp)
          </li>
        </ul>

        {dangTai ? (
          <p className="loading-state" role="status">
            Đang tải sơ đồ ghế…
          </p>
        ) : null}

        {!dangTai && dsGhe.length === 0 && !loi ? (
          <div className="empty-state">
            <p>Suất chiếu này chưa có sơ đồ ghế.</p>
            <button className="btn btn--ghost" type="button" onClick={onQuayLai}>
              Chọn suất khác
            </button>
          </div>
        ) : null}

        {dsGhe.length > 0 ? (
          <div className="seat-scroll">
            <p className="screen-label" aria-hidden="true">
              Màn hình
            </p>
            <table className="seat-table" id="tblListGhe">
              <caption className="sr-only">Sơ đồ ghế suất chiếu</caption>
              <tbody>
                {dsHang.map(({ hang, ghes }) => (
                  <tr key={hang}>
                    <th scope="row" className="seat-row-label">
                      {hang}
                    </th>
                    <td>
                      <div className="seat-row">
                        {ghes.map((ghe) => {
                          const khoa = biKhoa(ghe);
                          const dangChon = daChon.has(ghe.id);
                          const sweetbox = laSweetbox(ghe);
                          return (
                            <button
                              key={ghe.id}
                              className={[
                                'seat',
                                dangChon ? 'seat--selected' : '',
                                gheDangGiu.has(ghe.id) ? 'seat--busy' : '',
                                !gheDangGiu.has(ghe.id) && khoa ? 'seat--blocked' : '',
                                sweetbox ? 'seat--sweetbox' : '',
                                'chonGhe',
                              ]
                                .filter(Boolean)
                                .join(' ')}
                              id={`chonGhe-${ghe.id}`}
                              type="button"
                              data-chon="chonGhe"
                              aria-pressed={dangChon}
                              aria-label={`Ghế ${nhanGhe(ghe)} · ${ghe.loaiGhe?.tenLoai || 'Ghế'} · ${dinhDangTien(giaGhe(ghe))}${gheDangGiu.has(ghe.id) ? ' · không khả dụng' : ''}`}
                              disabled={dangTai || dangGui || khoa}
                              onClick={() => chonGhe(ghe)}
                            >
                              {nhanGhe(ghe)}
                            </button>
                          );
                        })}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        <div className="booking-checkout">
          <div className="chosen-seats">
            <h3 className="block-title">Ghế đã chọn</h3>
            {dsGheDaChon.length === 0 ? (
              <p className="section-note">Chưa chọn ghế nào.</p>
            ) : (
              <ul className="chosen-list">
                {dsGheDaChon.map((ghe) => (
                  <li key={ghe.id}>
                    <span>
                      <strong>{nhanGhe(ghe)}</strong> · {ghe.loaiGhe?.tenLoai || 'Ghế'}
                      {laSweetbox(ghe) ? ' (cặp)' : ''}
                    </span>
                    <span>{dinhDangTien(giaGhe(ghe))}</span>
                  </li>
                ))}
              </ul>
            )}
            <p className="section-note">
              Tạm tính {dinhDangTien(tamTinh)}. Giá và tổng tiền cuối cùng do máy chủ xác nhận khi tạo hóa đơn.
            </p>
          </div>

          <div className="flow-actions flow-actions--stack">
            <button
              className="btn btn--primary"
              id="btnTiepTuc"
              type="button"
              disabled={dangTai || dangGui || daChon.size === 0 || daChon.size > SO_GHE_TOI_DA}
              aria-busy={dangGui}
              onClick={() => void tiepTuc()}
            >
              {dangGui ? 'Đang tạo hóa đơn…' : 'Tiếp tục thanh toán'}
            </button>
            <button
              className="btn btn--ghost"
              id="btnQuayLai"
              type="button"
              disabled={dangGui}
              onClick={onQuayLai}
            >
              Chọn suất khác
            </button>
          </div>
        </div>
      </section>
    </FlowFrame>
  );
}
