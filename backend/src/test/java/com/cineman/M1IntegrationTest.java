package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** REST boundary with real JDBC persistence, isolated from auth tests. No DAO doubles. */
@SpringBootTest(properties = {
        "spring.datasource.url=${M1_TEST_DB_URL:jdbc:h2:mem:m1-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${M1_TEST_DB_USER:sa}",
        "spring.datasource.password=${M1_TEST_DB_PASSWORD:}"
})
@AutoConfigureMockMvc
class M1IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    private MockHttpSession session;

    @BeforeEach
    void reset() throws Exception {
        jdbc.update("DELETE FROM tblVe");
        jdbc.update("DELETE FROM tblHoaDon");
        jdbc.update("DELETE FROM tblVoucherKhachHang WHERE id <> 1");
        jdbc.update("DELETE FROM tblGiaoDichDoiDiem WHERE id <> 1");
        jdbc.update("DELETE FROM tblKhachHang WHERE tblThanhVienId=999");
        jdbc.update("DELETE FROM tblThanhVien WHERE id=999");
        jdbc.update("UPDATE tblKhachHang SET tongDiem = 250");
        jdbc.update("UPDATE tblQuaTang SET soLuong = CASE WHEN id = 4 THEN 0 ELSE 45 END");
        jdbc.update("UPDATE tblVoucherKhachHang SET trangThai='CHUA_SU_DUNG', ngayNhan=CURRENT_DATE, ngaySuDung=NULL WHERE id=1");
        jdbc.update("UPDATE tblSuatChieu SET giaCoSo=90000 WHERE id=3");
        MvcResult login = call(post("/api/auth/login"), null,
                Map.of("username", "annguyen2002", "password", "Matkhau@123"));
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        session = (MockHttpSession) login.getRequest().getSession(false);
    }

    @Test
    void goldenExchangeAndPaymentAreAtomicAndConfirmationIsIdempotent() throws Exception {
        JsonNode invoice = create(List.of(3, 11));
        int id = invoice.path("hoaDon").path("id").asInt();
        assertThat(invoice.path("tongTien").asInt()).isEqualTo(210000);
        assertThat(invoice.path("diemCong").asInt()).isEqualTo(10);
        assertThat(invoice.path("hoaDon").path("trangThai").asText()).isEqualTo("CHO_THANH_TOAN");
        LocalDateTime start = LocalDateTime.parse(invoice.path("hoaDon").path("ngayTao").asText());
        assertThat(LocalDateTime.parse(invoice.path("hoaDon").path("hetHan").asText())).isEqualTo(start.plusSeconds(300));
        JsonNode voucher = ok(post("/api/doi-diem"), Map.of("quaTangId", 2, "khachHangId", 999999));
        assertThat(voucher.path("giaoDichDoiDiem").path("diemConLai").asInt()).isEqualTo(150);
        assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(2);
        assertThat(count("tblVoucherKhachHang")).isEqualTo(2);
        assertThat(number("SELECT soLuong FROM tblQuaTang WHERE id=2")).isEqualTo(44);
        JsonNode applied = ok(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId", id, "maVoucher", "VC-VIDU-12345"));
        assertThat(applied.path("tongTien").asInt()).isEqualTo(120000);
        assertThat(applied.path("diemCong").asInt()).isEqualTo(10);
        assertThat(text("SELECT trangThai FROM tblVoucherKhachHang WHERE id=1")).isEqualTo("CHUA_SU_DUNG");
        JsonNode paid = ok(post("/api/hoadon/" + id + "/thanh-toan"), null);
        assertThat(paid.path("hoaDon").path("trangThai").asText()).isEqualTo("DA_THANH_TOAN");
        assertThat(paid.path("tongTien").asInt()).isEqualTo(120000);
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(160);
        assertThat(number("SELECT COUNT(*) FROM tblVe WHERE trangThaiVe='DA_BAN'")).isEqualTo(2);
        assertThat(text("SELECT trangThai FROM tblVoucherKhachHang WHERE id=1")).isEqualTo("DA_SU_DUNG");
        ok(post("/api/hoadon/" + id + "/thanh-toan"), null);
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(160);
    }

    @Test
    void catalogueFiltersAndSeatPricesComeFromServer() throws Exception {
        JsonNode phim = ok(get("/api/phim"), null);
        assertThat(phim.get(0).path("tenPhim").asText()).isEqualTo("Mai");
        JsonNode shows = ok(get("/api/suatchieu?phimId=1&ngayChieu=2026-09-27&khungGioId=3"), null);
        assertThat(shows.get(0).path("id").asInt()).isEqualTo(3);
        JsonNode seats = ok(get("/api/ghe?suatChieuId=3"), null);
        assertThat(seats.path("dsGhe").get(2).path("hangGhe").asText()).isEqualTo("A");
        assertThat(seats.path("dsGhe").get(10).path("hangGhe").asText()).isEqualTo("B");
        assertThat(seats.path("dsGhe").get(32).path("hangGhe").asText()).isEqualTo("E");
        MvcResult result = call(post("/api/hoadon"), session,
                Map.of("suatChieuId", 3, "dsGhe", List.of(3, 11), "khachHangId", 999, "giaVe", 1));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(data(result).path("tongTien").asInt()).isEqualTo(210000);
    }

    @Test
    void invalidSeatSelectionsNeverCreatePartialInvoice() throws Exception {
        for (List<Integer> seats : List.of(List.<Integer>of(), List.of(1,2,3,4,5,6,7,8,9),
                List.of(3,3), List.of(41), List.of(33), List.of(34,35))) {
            fails(post("/api/hoadon"), Map.of("suatChieuId", 3, "dsGhe", seats));
            assertThat(count("tblHoaDon")).isZero();
            assertThat(count("tblVe")).isZero();
        }
        assertThat(create(List.of(33,34)).path("hoaDon").path("dsVe").size()).isEqualTo(2);
    }

    @Test
    void cancelAndExpiryReleaseSeatsButLatePaymentNeverSucceeds() throws Exception {
        int cancelled = create(List.of(3)).path("hoaDon").path("id").asInt();
        fails(post("/api/hoadon"), Map.of("suatChieuId",3,"dsGhe",List.of(3)));
        ok(delete("/api/hoadon/"+cancelled), null);
        int expired = create(List.of(3)).path("hoaDon").path("id").asInt();
        jdbc.update("UPDATE tblHoaDon SET ngayTao=?,hetHan=? WHERE id=?", LocalDateTime.now().minusSeconds(301), LocalDateTime.now().minusSeconds(1), expired);
        fails(post("/api/hoadon/"+expired+"/thanh-toan"), null);
        assertThat(text("SELECT trangThai FROM tblHoaDon WHERE id="+expired)).isEqualTo("HET_HAN");
        assertThat(text("SELECT trangThaiVe FROM tblVe WHERE tblHoaDonId="+expired)).isEqualTo("DA_HUY");
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(250);
        create(List.of(3));
    }

    @Test
    void deletingPaidInvoiceReturnsPaidSummaryWithoutChangingState() throws Exception {
        int id=create(List.of(3)).path("hoaDon").path("id").asInt();
        ok(post("/api/hoadon/"+id+"/thanh-toan"), null);
        JsonNode before=ok(delete("/api/hoadon/"+id), null);
        assertThat(before.path("hoaDon").path("trangThai").asText()).isEqualTo("DA_THANH_TOAN");
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(254);
        assertThat(text("SELECT trangThaiVe FROM tblVe WHERE tblHoaDonId="+id)).isEqualTo("DA_BAN");
    }

    @Test
    void failedMockCanRetryAndPointsUseFloorBeforeDiscount() throws Exception {
        jdbc.update("UPDATE tblSuatChieu SET giaCoSo=199999 WHERE id=3");
        JsonNode invoice = create(List.of(3));
        int id = invoice.path("hoaDon").path("id").asInt();
        assertThat(invoice.path("diemCong").asInt()).isEqualTo(9);
        fails(post("/api/hoadon/"+id+"/thanh-toan"), Map.of("thanhCong",false));
        assertThat(text("SELECT trangThai FROM tblHoaDon WHERE id="+id)).isEqualTo("CHO_THANH_TOAN");
        ok(post("/api/hoadon/"+id+"/thanh-toan"), Map.of("thanhCong",true));
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(259);
    }

    @Test
    void insufficientPointsAndStockLeaveAllExchangeStateUnchanged() throws Exception {
        jdbc.update("UPDATE tblKhachHang SET tongDiem=99 WHERE tblThanhVienId=1");
        fails(post("/api/doi-diem"), Map.of("quaTangId",2));
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(99);
        assertThat(number("SELECT soLuong FROM tblQuaTang WHERE id=2")).isEqualTo(45);
        jdbc.update("UPDATE tblKhachHang SET tongDiem=250 WHERE tblThanhVienId=1");
        fails(post("/api/doi-diem"), Map.of("quaTangId",4));
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(250);
        assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(1);
        assertThat(count("tblVoucherKhachHang")).isEqualTo(1);
    }

    @Test
    void expiredUsedForeignAndPhysicalVouchersCannotChangeInvoice() throws Exception {
        int id = create(List.of(3,11)).path("hoaDon").path("id").asInt();
        jdbc.update("UPDATE tblVoucherKhachHang SET ngayNhan=? WHERE id=1", java.time.LocalDate.now().minusDays(30));
        fails(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId",id,"maVoucher","VC-VIDU-12345"));
        jdbc.update("UPDATE tblVoucherKhachHang SET ngayNhan=CURRENT_DATE,trangThai='DA_SU_DUNG' WHERE id=1");
        fails(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId",id,"maVoucher","VC-VIDU-12345"));
        jdbc.update("UPDATE tblVoucherKhachHang SET trangThai='CHUA_SU_DUNG' WHERE id=1");
        // A distinct customer cannot borrow the seed customer's voucher or invoice.
        MockHttpSession original = session;
        session = new MockHttpSession();
        jdbc.update("INSERT INTO tblThanhVien (id,username,password,hoTen,ngaySinh,gioiTinh,diaChi,email,sdt,vaiTro) VALUES (999,'other','unused','Other','2000-01-01','Nam','X','other@example.com','0999999999','KHACH_HANG')");
        jdbc.update("INSERT INTO tblKhachHang (tblThanhVienId,ngayDangKy,tongDiem) VALUES (999,CURRENT_DATE,250)");
        session.setAttribute("khachHangId",999);
        fails(post("/api/hoadon/"+id+"/thanh-toan"), null);
        int foreign = create(List.of(4)).path("hoaDon").path("id").asInt();
        fails(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId",foreign,"maVoucher","VC-VIDU-12345"));
        session = original;
        String physical = ok(post("/api/doi-diem"), Map.of("quaTangId",3)).path("maVoucher").asText();
        fails(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId",id,"maVoucher",physical));
        assertThat(jdbc.queryForObject("SELECT tblVoucherKhachHangId FROM tblHoaDon WHERE id=?",Integer.class,id)).isNull();
    }

    @Test
    void competingSeatHoldsHaveExactlyOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var action = (java.util.concurrent.Callable<Integer>) () -> {
                ready.countDown(); start.await();
                return call(post("/api/hoadon"),session,Map.of("suatChieuId",3,"dsGhe",List.of(3))).getResponse().getStatus();
            };
            var a=pool.submit(action); var b=pool.submit(action);
            ready.await(); start.countDown();
            assertThat(List.of(a.get(),b.get())).contains(200).filteredOn(s -> s==200).hasSize(1);
        }
        assertThat(count("tblHoaDon")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM tblVe WHERE trangThaiVe='DANG_GIU'")).isEqualTo(1);
    }

    @Test
    void racingExchangesCannotOverspendOrOversell() throws Exception {
        ok(get("/api/quatang"), null);
        jdbc.update("UPDATE tblKhachHang SET tongDiem=100 WHERE tblThanhVienId=1");
        jdbc.update("UPDATE tblQuaTang SET soLuong=1 WHERE id=2");
        List<Integer> statuses = race(() -> call(post("/api/doi-diem"), session,
                Map.of("quaTangId",2)).getResponse().getStatus());
        assertThat(statuses).contains(200).filteredOn(s -> s==200).hasSize(1);
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isZero();
        assertThat(number("SELECT soLuong FROM tblQuaTang WHERE id=2")).isZero();
        assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(2);
        assertThat(count("tblVoucherKhachHang")).isEqualTo(2);
    }

    @Test
    void voucherCanBeConsumedByOnlyOneOfTwoPendingInvoices() throws Exception {
        int a=create(List.of(3)).path("hoaDon").path("id").asInt();
        int b=create(List.of(4,5)).path("hoaDon").path("id").asInt();
        for (int id : List.of(a,b)) {
            ok(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",id,"maVoucher","VC-VIDU-12345"));
        }
        CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var one=pool.submit(() -> {ready.countDown();start.await();return call(post("/api/hoadon/"+a+"/thanh-toan"),session,null).getResponse().getStatus();});
            var two=pool.submit(() -> {ready.countDown();start.await();return call(post("/api/hoadon/"+b+"/thanh-toan"),session,null).getResponse().getStatus();});
            ready.await();start.countDown();
            assertThat(List.of(one.get(),two.get())).contains(200).filteredOn(s -> s==200).hasSize(1);
            assertThat(number("SELECT COUNT(*) FROM tblHoaDon WHERE id IN ("+a+","+b+") AND trangThai='CHO_THANH_TOAN'")).isEqualTo(1);
        }
        assertThat(number("SELECT COUNT(*) FROM tblHoaDon WHERE trangThai='DA_THANH_TOAN'")).isEqualTo(1);
        String thang = text("SELECT CASE WHEN id=" + a + " THEN 'A' ELSE 'B' END FROM tblHoaDon WHERE trangThai='DA_THANH_TOAN'");
        assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo("A".equals(thang) ? 254 : 259);
    }

    @Test
    void exchangeFailureAfterReceiptRollsBackPointsStockAndReceipt() throws Exception {
        jdbc.execute("ALTER TABLE tblVoucherKhachHang ADD CONSTRAINT test_reject_new CHECK (tblGiaoDichDoiDiemId=1)");
        try {
            MvcResult result=call(post("/api/doi-diem"),session,Map.of("quaTangId",2));
            assertThat(result.getResponse().getStatus()).isBetween(400,599);
            assertThat(root(result).path("success").asBoolean()).isFalse();
            assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(250);
            assertThat(number("SELECT soLuong FROM tblQuaTang WHERE id=2")).isEqualTo(45);
            assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(1);
            assertThat(count("tblVoucherKhachHang")).isEqualTo(1);
        } finally {
            jdbc.execute("ALTER TABLE tblVoucherKhachHang DROP CONSTRAINT test_reject_new");
        }
    }

    @Test
    void secondTicketInsertFailureRollsBackInvoiceAndFirstTicket() throws Exception {
        jdbc.execute("ALTER TABLE tblVe ADD CONSTRAINT test_reject_b3 CHECK (tblGheId<>11)");
        try {
            MvcResult result=call(post("/api/hoadon"),session,Map.of("suatChieuId",3,"dsGhe",List.of(3,11)));
            assertThat(result.getResponse().getStatus()).isBetween(400,599);
            assertThat(root(result).path("success").asBoolean()).isFalse();
            assertThat(count("tblHoaDon")).isZero();
            assertThat(count("tblVe")).isZero();
        } finally {
            jdbc.execute("ALTER TABLE tblVe DROP CONSTRAINT test_reject_b3");
        }
    }

    @Test
    void pointCreditFailureRollsBackPaymentTicketsAndVoucher() throws Exception {
        int id=create(List.of(3,11)).path("hoaDon").path("id").asInt();
        ok(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",id,"maVoucher","VC-VIDU-12345"));
        jdbc.execute("ALTER TABLE tblKhachHang ADD CONSTRAINT test_reject_credit CHECK (tongDiem<=250)");
        try {
            MvcResult result=call(post("/api/hoadon/"+id+"/thanh-toan"),session,null);
            assertThat(result.getResponse().getStatus()).isBetween(400,599);
            assertThat(root(result).path("success").asBoolean()).isFalse();
            assertThat(text("SELECT trangThai FROM tblHoaDon WHERE id="+id)).isEqualTo("CHO_THANH_TOAN");
            assertThat(number("SELECT COUNT(*) FROM tblVe WHERE trangThaiVe='DANG_GIU'")).isEqualTo(2);
            assertThat(text("SELECT trangThai FROM tblVoucherKhachHang WHERE id=1")).isEqualTo("CHUA_SU_DUNG");
            assertThat(number("SELECT tongDiem FROM tblKhachHang WHERE tblThanhVienId=1")).isEqualTo(250);
        } finally {
            jdbc.execute("ALTER TABLE tblKhachHang DROP CONSTRAINT test_reject_credit");
        }
    }

    @Test
    void voucherEligibilityIsRecheckedAtPaymentAndRejects3D() throws Exception {
        int id=create(List.of(3)).path("hoaDon").path("id").asInt();
        ok(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",id,"maVoucher","VC-VIDU-12345"));
        jdbc.update("UPDATE tblVoucherKhachHang SET ngayNhan=? WHERE id=1",java.time.LocalDate.now().minusDays(30));
        fails(post("/api/hoadon/"+id+"/thanh-toan"),null);
        assertThat(text("SELECT trangThai FROM tblHoaDon WHERE id="+id)).isEqualTo("CHO_THANH_TOAN");
        jdbc.update("UPDATE tblVoucherKhachHang SET ngayNhan=CURRENT_DATE WHERE id=1");
        int threeD=ok(post("/api/hoadon"),Map.of("suatChieuId",4,"dsGhe",List.of(41))).path("hoaDon").path("id").asInt();
        fails(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",threeD,"maVoucher","VC-VIDU-12345"));
    }

    @Test
    void fixedDiscountIsCappedWithoutReducingPoints() throws Exception {
        jdbc.update("UPDATE tblQuaTang SET soLuong=1 WHERE id=4");
        String code=ok(post("/api/doi-diem"),Map.of("quaTangId",4)).path("maVoucher").asText();
        int id=create(List.of(3,11)).path("hoaDon").path("id").asInt();
        JsonNode applied=ok(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",id,"maVoucher",code));
        assertThat(applied.path("tongTien").asInt()).isEqualTo(160000);
        assertThat(applied.path("diemCong").asInt()).isEqualTo(10);
        int small=ok(post("/api/hoadon"),Map.of("suatChieuId",5,"dsGhe",List.of(1))).path("hoaDon").path("id").asInt();
        jdbc.update("UPDATE tblQuaTang SET giaTriGiam=100000 WHERE id=4");
        try {
            JsonNode capped=ok(post("/api/hoadon/apdung-voucher"),Map.of("hoaDonId",small,"maVoucher",code));
            assertThat(capped.path("tongTien").asInt()).isZero();
            assertThat(capped.path("diemCong").asInt()).isEqualTo(4);
        } finally {
            jdbc.update("UPDATE tblQuaTang SET giaTriGiam=50000 WHERE id=4");
        }
    }

    @Test
    void differentCustomersCannotBothTakeLastGift() throws Exception {
        jdbc.update("INSERT INTO tblThanhVien (id,username,password,hoTen,ngaySinh,gioiTinh,diaChi,email,sdt,vaiTro) VALUES (999,'last_stock','unused','Other','2000-01-01','Nam','X','stock@example.com','0999999998','KHACH_HANG')");
        jdbc.update("INSERT INTO tblKhachHang (tblThanhVienId,ngayDangKy,tongDiem) VALUES (999,CURRENT_DATE,250)");
        jdbc.update("UPDATE tblQuaTang SET soLuong=1 WHERE id=2");
        MockHttpSession other=new MockHttpSession();
        other.setAttribute("khachHangId",999);
        CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> {ready.countDown();start.await();return call(post("/api/doi-diem"),session,Map.of("quaTangId",2)).getResponse().getStatus();});
            var b=pool.submit(() -> {ready.countDown();start.await();return call(post("/api/doi-diem"),other,Map.of("quaTangId",2)).getResponse().getStatus();});
            ready.await();start.countDown();
            assertThat(List.of(a.get(),b.get())).containsExactlyInAnyOrder(200,409);
        }
        assertThat(number("SELECT SUM(tongDiem) FROM tblKhachHang WHERE tblThanhVienId IN (1,999)")).isEqualTo(400);
        assertThat(number("SELECT soLuong FROM tblQuaTang WHERE id=2")).isZero();
        assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(2);
    }

    private List<Integer> race(java.util.concurrent.Callable<Integer> action) throws Exception {
        CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> gated=() -> {ready.countDown();start.await();return action.call();};
            var a=pool.submit(gated);var b=pool.submit(gated);
            ready.await();start.countDown();
            return List.of(a.get(),b.get());
        }
    }

    @Test
    void anonymousCannotReadOrMutateBusinessData() throws Exception {
        for (String path : List.of("/api/phim","/api/quatang","/api/ghe?suatChieuId=3")) {
            MvcResult result=call(get(path),null,null);
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    @Test
    void malformedAndMissingInputsReturnSafeEnvelopeWithoutWrites() throws Exception {
        fails(post("/api/hoadon"), Map.of());
        fails(post("/api/doi-diem"), Map.of("quaTangId",-1));
        fails(post("/api/hoadon/apdung-voucher"), Map.of("hoaDonId",0,"maVoucher",""));
        fails(get("/api/ghe"), null);
        fails(get("/api/suatchieu?phimId=1&ngayChieu=not-a-date"), null);
        fails(post("/api/hoadon/not-an-id/thanh-toan"), null);
        MvcResult broken=call(post("/api/doi-diem").contentType(MediaType.APPLICATION_JSON).content("{"),session,null);
        assertThat(broken.getResponse().getStatus()).isEqualTo(400);
        assertThat(root(broken).path("success").asBoolean()).isFalse();
        assertThat(count("tblHoaDon")).isZero();
        assertThat(count("tblGiaoDichDoiDiem")).isEqualTo(1);
    }

    private JsonNode create(List<Integer> seats) throws Exception {
        return ok(post("/api/hoadon"),Map.of("suatChieuId",3,"dsGhe",seats));
    }
    private MvcResult call(MockHttpServletRequestBuilder request, MockHttpSession who,Object body) throws Exception {
        if(who!=null) request.session(who);
        if(body!=null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        return mvc.perform(request).andReturn();
    }
    private JsonNode ok(MockHttpServletRequestBuilder request,Object body) throws Exception {
        MvcResult result=call(request,session,body);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(root(result).path("success").asBoolean()).isTrue();
        return data(result);
    }
    private void fails(MockHttpServletRequestBuilder request,Object body) throws Exception {
        MvcResult result=call(request,session,body);
        assertThat(result.getResponse().getStatus()).isBetween(400,499);
        assertThat(root(result).path("success").asBoolean()).isFalse();
        assertThat(root(result).path("message").asText()).isNotBlank();
    }
    private JsonNode root(MvcResult r) throws Exception {return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));}
    private JsonNode data(MvcResult r) throws Exception {return root(r).path("data");}
    private int count(String table){return number("SELECT COUNT(*) FROM "+table);}
    private int number(String sql){return jdbc.queryForObject(sql,Integer.class);}
    private String text(String sql){return jdbc.queryForObject(sql,String.class);}
}
