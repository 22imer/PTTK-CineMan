# Graph Report - Sua-Controller  (2026-11-11)

## Corpus Check
- 85 files · ~66,135 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 9, .puml 3, .properties 2)

## Summary
- 1083 nodes · 2514 edges · 42 communities (29 shown, 13 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 102 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3e852636`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App.tsx
- Backend Integration Tests
- RESTController.java
- Business Contracts and Vocabulary
- Member Authentication and Passwords
- Registration and HTTP Client
- Docker Service Configuration
- Frontend Dependencies and Scripts
- HoaDonDAO
- CineMan — Quản lý rạp chiếu phim
- Gift Catalog and Inventory
- Checkpoint Hook Test Harness
- Customer Voucher Lifecycle
- TypeScript Compiler Configuration
- Movie Entity Fields
- with
- KhachHangDAO
- gen-hall-scene.mjs
- GiaoDichDoiDiem
- Screen
- MySQL Tables and Constraints
- Invoice Entity Fields
- Cinema Rooms and Seating
- Time Slot Entity Fields
- Branch Flow Automation
- Preview.tsx
- Demo Ticket QR Payload
- common.tsx
- Scoped Checkpoint Shell Script
- SuatChieu
- Backend Maven Coordinate
- Product
- org.springframework.jdbc.core.JdbcTemplate
- Design
- main.tsx
- steps
- check-graphify.py

## God Nodes (most connected - your core abstractions)
1. `RESTController` - 47 edges
2. `AuthIntegrationTest` - 46 edges
3. `TaiKhoanApp()` - 44 edges
4. `M1IntegrationTest` - 43 edges
5. `VoucherKhachHang` - 34 edges
6. `SuatChieu` - 32 edges
7. `Ve` - 30 edges
8. `HoaDonDAO` - 30 edges
9. `Phim` - 29 edges
10. `KhachHang` - 28 edges

## Surprising Connections (you probably didn't know these)
- `VoucherKhachHang (Issued Customer Voucher)` --semantically_similar_to--> `VoucherKhachHang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/VoucherKhachHang.java
- `Phim (Movie)` --semantically_similar_to--> `Phim`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/Phim.java
- `KhachHang (Customer Profile)` --semantically_similar_to--> `KhachHang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/KhachHang.java
- `GiaoDichDoiDiem (Point Redemption Receipt)` --semantically_similar_to--> `GiaoDichDoiDiem`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/GiaoDichDoiDiem.java
- `QuaTang (Gift Catalog Item)` --semantically_similar_to--> `QuaTang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/QuaTang.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Canonical CineMan Business Vocabulary** — glossary_cineman_m1, glossary_thanhvien, glossary_khachhang, glossary_hoadon, glossary_ve, glossary_giaodichdoidiem, glossary_voucherkhachhang [EXTRACTED 1.00]
- **Colon-Delimited Ticket Payload Field Structure** — frontend_public_ticket_demo_payload, frontend_public_ticket_demo_brand_namespace, frontend_public_ticket_demo_invoice_ref, frontend_public_ticket_demo_ticket_refs, frontend_public_ticket_demo_invalid_marker [EXTRACTED 1.00]
- **Booking and Seat-Hold Flow** — specs_booking_scenario, glossary_hoadon, glossary_ve, glossary_ghe, glossary_suatchieu, architecture_seat_hold_300s [INFERRED 0.85]
- **Demo Ticket QR Rendering Path on Payment-Result Screen** — frontend_src_ui_preview, frontend_src_ui_models_hoadonui, frontend_public_ticket_demo, frontend_public_ticket_demo_qrcode [INFERRED 0.85]
- **Reward Point Redemption Flow** — specs_reward_redemption_scenario, glossary_giaodichdoidiem, glossary_quatang, glossary_voucherkhachhang, architecture_atomic_transactions [INFERRED 0.85]

## Communities (42 total, 13 thin omitted)

### Community 0 - "App.tsx"
Cohesion: 0.11
Nodes (35): doiDiemUI(), doiKhungGioUI(), doiPhimUI(), doiQuaTangUI(), doiSuatChieuUI(), ManHinh, params, TaiKhoanApp() (+27 more)

### Community 1 - "Backend Integration Tests"
Cohesion: 0.05
Nodes (6): ArchitectureInvariantTest, AuthIntegrationTest, HoSo, M1IntegrationTest, SchemaSeedTest, UnicodeRepairIntegrationTest

### Community 2 - "RESTController.java"
Cohesion: 0.08
Nodes (11): ApiResponse, DanhSachQuaTang, RESTController, SoDoGhe, TomTatHoaDon, YeuCauApDungVoucher, YeuCauDangKy, YeuCauDangNhap (+3 more)

### Community 3 - "Business Contracts and Vocabulary"
Cohesion: 0.07
Nodes (49): Agent Checkpoint Commit Protocol (issue #11), CineMan Agent Working Guidelines (AGENTS.md), M1 Scope: Three Functions Only, CineMan Architecture (ARCHITECTURE.md), DAO Layer with _fix Method Signatures, MySQL Schema: 13 Tables, 14 Foreign Keys, 12 React TypeScript Pages (GD*.tsx), REST API Envelope and Endpoint Groups (+41 more)

### Community 4 - "Member Authentication and Passwords"
Cohesion: 0.07
Nodes (3): CinemanApplication, ThanhVienDAO, ThanhVien

### Community 5 - "Registration and HTTP Client"
Cohesion: 0.08
Nodes (27): ApiEnvelope, HTTPClient, HTTPError, serverError(), AuthFrame(), AuthFrameProps, DangNhap(), DangNhapProps (+19 more)

### Community 6 - "Docker Service Configuration"
Cohesion: 0.06
Nodes (33): build, depends_on, environment, ports, mysql, API_PROXY_TARGET, DB_PASSWORD, DB_URL (+25 more)

### Community 7 - "Frontend Dependencies and Scripts"
Cohesion: 0.08
Nodes (30): dependencies, @fontsource/handjet@^5.3.1, react@^19.3.1, react-dom@^19.3.1, uqr@^0.1.3, devDependencies, @types/node@^24.19.2, @types/react@^19.3.0 (+22 more)

### Community 8 - "HoaDonDAO"
Cohesion: 0.05
Nodes (17): HoaDonDAO, HoaDonKhoa, KetQuaThaoTac, MaThaoTac, DA_HUY, DA_THANH_TOAN, HET_HAN, KHONG_PHAI_CHU (+9 more)

### Community 9 - "CineMan — Quản lý rạp chiếu phim"
Cohesion: 0.15
Nodes (13): 1. Chuẩn bị MySQL, 2. Chạy backend, 3. Chạy frontend, Chạy nhanh bằng Docker Compose, Chạy thử trên DB seed mới, Chạy từng thành phần trên máy, CineMan — Quản lý rạp chiếu phim, Cấu trúc và tài liệu (+5 more)

### Community 10 - "Gift Catalog and Inventory"
Cohesion: 0.09
Nodes (5): QuaTangDAO, QuaTang, VoucherGiamGia, VoucherVatPham, VoucherVeTang

### Community 11 - "Checkpoint Hook Test Harness"
Cohesion: 0.07
Nodes (33): children, cleanEnv(), commandContext(), CommandHandler, commitCount(), emitHook(), git(), GitResult (+25 more)

### Community 12 - "Customer Voucher Lifecycle"
Cohesion: 0.13
Nodes (3): TrangThaiVoucher, VoucherKhachHangDAO, VoucherKhachHang

### Community 13 - "TypeScript Compiler Configuration"
Cohesion: 0.09
Nodes (21): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, forceConsistentCasingInFileNames, isolatedModules, jsx, lib, module (+13 more)

### Community 15 - "with"
Cohesion: 0.10
Nodes (27): runs-on, steps, runs-on, steps, runs-on, steps, jobs, backend (+19 more)

### Community 19 - "gen-hall-scene.mjs"
Cohesion: 0.05
Nodes (38): buf, bulbAt, bulbs, bulbsBoard, bulbsBoardMirror, C, cbulbs, DIGITS (+30 more)

### Community 22 - "Screen"
Cohesion: 0.19
Nodes (27): ticket-demo.svg (Static QR Ticket Fixture), GDChonGhe(), GDChonPhim(), GDChonQua(), GDKhungGio(), GDSuatChieu(), GDThanhToan(), GDThanhToanProps (+19 more)

### Community 23 - "MySQL Tables and Constraints"
Cohesion: 0.28
Nodes (14): idx_tblVe_suatChieu_ghe, tblGhe, tblGiaoDichDoiDiem, tblHoaDon, tblKhachHang, tblKhungGio, tblLoaiGhe, tblPhim (+6 more)

### Community 27 - "Branch Flow Automation"
Cohesion: 0.18
Nodes (10): runs-on, steps, jobs, branch-flow, name, on, pull_request, branches (+2 more)

### Community 29 - "Preview.tsx"
Cohesion: 0.12
Nodes (23): GDChonQuaProps, GDThongBao(), GDThongBaoProps, GDXacNhanDoiQua(), GDXacNhanDoiQuaProps, doiDiemMau, gheDaChon, gheMau (+15 more)

### Community 30 - "Demo Ticket QR Payload"
Cohesion: 0.33
Nodes (6): CINEMAN-DEMO Namespace Prefix, KHONG-CO-GIA-TRI-SU-DUNG Invalid-Use Marker, Invoice Reference HD001, Decoded Ticket Payload (CINEMAN-DEMO:HD001:VE001:VE002:KHONG-CO-GIA-TRI-SU-DUNG), QRcode Group (29x29 Module Matrix, 3.92cm), Ticket Codes VE001 and VE002

### Community 31 - "common.tsx"
Cohesion: 0.21
Nodes (16): CumGhe, veNutGhe(), GDChonGheProps, HangGhe, nhanTrangThaiGhe(), nhomTheoCap(), nhomTheoHang(), GDChonPhimProps (+8 more)

### Community 33 - "Scoped Checkpoint Shell Script"
Cohesion: 0.83
Nodes (3): die(), agent-commit.sh script, usage()

### Community 38 - "Product"
Cohesion: 0.22
Nodes (8): Brand commitments, Constraints, Open decisions, Platform, Positioning, Product, Product Purpose, Users

### Community 40 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.15
Nodes (5): DAO, GiaoDichDoiDiemDAO, KhungGioDAO, PhimDAO, SuatChieuDAO

### Community 41 - "Design"
Cohesion: 0.29
Nodes (6): Color, Components, Design, Motion, Shape and depth, Typography

### Community 42 - "main.tsx"
Cohesion: 0.29
Nodes (3): App(), container, ScrollbarAutohide()

### Community 45 - "steps"
Cohesion: 0.17
Nodes (11): runs-on, steps, jobs, graph-fresh, name, on, pull_request, branches (+3 more)

### Community 46 - "check-graphify.py"
Cohesion: 0.24
Nodes (4): main(), md5(), main(), tracked()

## Knowledge Gaps
- **17 isolated node(s):** `DA_HUY`, `DA_THANH_TOAN`, `HET_HAN`, `KHONG_PHAI_CHU`, `KHONG_THE_XU_LY` (+12 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 360 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `KhachHang` connect `Invoice Entity Fields` to `RESTController.java`, `Business Contracts and Vocabulary`, `Member Authentication and Passwords`, `HoaDonDAO`, `Customer Voucher Lifecycle`, `KhachHangDAO`, `GiaoDichDoiDiem`, `Persistence Imports and Utilities`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `TaiKhoanApp()` (e.g. with `doiKhungGioUI()` and `doiPhimUI()`) actually correct?**
  _`TaiKhoanApp()` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `DA_HUY`, `DA_THANH_TOAN`, `HET_HAN` to the rest of the system?**
  _17 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `App.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.11498257839721254 - nodes in this community are weakly interconnected._
- **Why does `KhachHang (Customer Profile)` connect `Business Contracts and Vocabulary` to `Invoice Entity Fields`, `Registration and HTTP Client`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Should `Backend Integration Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.05366726296958855 - nodes in this community are weakly interconnected._
- **Why does `KhachHang` connect `Registration and HTTP Client` to `App.tsx`, `Business Contracts and Vocabulary`, `Preview.tsx`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._