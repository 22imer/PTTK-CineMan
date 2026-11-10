package com.cineman;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Đồng hồ điều khiển được cho test biên thời gian phiên (ARCHITECTURE §7.3): thời điểm cố định, chỉ đổi
 * khi test gọi {@link #dat(Instant)}/{@link #tien(Duration)} để kiểm tra hạn access, hạn tuyệt đối 7 ngày
 * và cửa sổ replay 5 giây mà không phải ngủ thật.
 */
public final class TestClock extends Clock {

    private volatile Instant hienTai;

    public TestClock(Instant batDau) {
        this.hienTai = batDau;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return hienTai;
    }

    public void dat(Instant thoiDiem) {
        this.hienTai = thoiDiem;
    }

    public void tien(Duration khoang) {
        this.hienTai = this.hienTai.plus(khoang);
    }
}
