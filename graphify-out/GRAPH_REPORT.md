# Graph Report - Improvement  (2026-11-11)

## Corpus Check
- 88 files · ~66,247 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 9, .puml 3, .properties 2)

## Summary
- 1076 nodes · 2594 edges · 41 communities (30 shown, 11 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 102 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9eb8d754`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App.tsx
- Backend Integration Tests
- Booking Controllers and Customers
- Business Contracts and Vocabulary
- Member Authentication and Passwords
- Registration and HTTP Client
- Docker Service Configuration
- Frontend Dependencies and Scripts
- Seats and Seat Types
- Invoice Transactions and Payments
- Gift Catalog and Inventory
- Checkpoint Hook Test Harness
- Customer Voucher Lifecycle
- TypeScript Compiler Configuration
- Movie Entity Fields
- with
- models.ts
- gen-hall-scene.mjs
- QuaTang
- Screen
- MySQL Tables and Constraints
- Invoice Entity Fields
- Cinema Rooms and Seating
- Time Slot Entity Fields
- Branch Flow Automation
- Invoice Operation Statuses
- Preview.tsx
- Demo Ticket QR Payload
- GDChonGhe.tsx
- Scoped Checkpoint Shell Script
- VeDAO
- Backend Maven Coordinate
- Product
- org.springframework.jdbc.core.JdbcTemplate
- Design
- main.tsx
- steps
- check-graphify.py

## God Nodes (most connected - your core abstractions)
1. `AuthIntegrationTest` - 46 edges
2. `TaiKhoanApp()` - 44 edges
3. `M1IntegrationTest` - 43 edges
4. `VoucherKhachHang` - 38 edges
5. `SuatChieu` - 36 edges
6. `KhachHang` - 35 edges
7. `Ve` - 33 edges
8. `HoaDonDAO` - 33 edges
9. `Phim` - 31 edges
10. `DatVeController` - 30 edges

## Surprising Connections (you probably didn't know these)
- `QuaTang (Gift Catalog Item)` --semantically_similar_to--> `QuaTang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/QuaTang.java
- `VoucherKhachHang (Issued Customer Voucher)` --semantically_similar_to--> `VoucherKhachHang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/VoucherKhachHang.java
- `Phim (Movie)` --semantically_similar_to--> `Phim`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/Phim.java
- `KhachHang (Customer Profile)` --semantically_similar_to--> `KhachHang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/KhachHang.java
- `Ve (Ticket)` --semantically_similar_to--> `Ve`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/Ve.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Canonical CineMan Business Vocabulary** — glossary_cineman_m1, glossary_thanhvien, glossary_khachhang, glossary_hoadon, glossary_ve, glossary_giaodichdoidiem, glossary_voucherkhachhang [EXTRACTED 1.00]
- **Colon-Delimited Ticket Payload Field Structure** — frontend_public_ticket_demo_payload, frontend_public_ticket_demo_brand_namespace, frontend_public_ticket_demo_invoice_ref, frontend_public_ticket_demo_ticket_refs, frontend_public_ticket_demo_invalid_marker [EXTRACTED 1.00]
- **Booking and Seat-Hold Flow** — specs_booking_scenario, glossary_hoadon, glossary_ve, glossary_ghe, glossary_suatchieu, architecture_seat_hold_300s [INFERRED 0.85]
- **Demo Ticket QR Rendering Path on Payment-Result Screen** — frontend_src_ui_preview, frontend_src_ui_models_hoadonui, frontend_public_ticket_demo, frontend_public_ticket_demo_qrcode [INFERRED 0.85]
- **Reward Point Redemption Flow** — specs_reward_redemption_scenario, glossary_giaodichdoidiem, glossary_quatang, glossary_voucherkhachhang, architecture_atomic_transactions [INFERRED 0.85]

## Communities (41 total, 11 thin omitted)

### Community 0 - "App.tsx"
Cohesion: 0.11
Nodes (35): doiDiemUI(), doiKhungGioUI(), doiPhimUI(), doiQuaTangUI(), doiSuatChieuUI(), ManHinh, params, TaiKhoanApp() (+27 more)

### Community 1 - "Backend Integration Tests"
Cohesion: 0.05
Nodes (6): ArchitectureInvariantTest, AuthIntegrationTest, HoSo, M1IntegrationTest, SchemaSeedTest, UnicodeRepairIntegrationTest

### Community 2 - "Booking Controllers and Customers"
Cohesion: 0.07
Nodes (16): ApiResponse, DangKyController, YeuCauDangKy, YeuCauDangNhap, DatVeController, SoDoGhe, TomTatHoaDon, YeuCauApDungVoucher (+8 more)

### Community 3 - "Business Contracts and Vocabulary"
Cohesion: 0.05
Nodes (62): Agent Checkpoint Commit Protocol (issue #11), CineMan Agent Working Guidelines (AGENTS.md), M1 Scope: Three Functions Only, CineMan Architecture (ARCHITECTURE.md), DAO Layer with _fix Method Signatures, MySQL Schema: 13 Tables, 14 Foreign Keys, 12 React TypeScript Pages (GD*.tsx), REST API Envelope and Endpoint Groups (+54 more)

### Community 4 - "Member Authentication and Passwords"
Cohesion: 0.07
Nodes (3): CinemanApplication, ThanhVienDAO, ThanhVien

### Community 5 - "Registration and HTTP Client"
Cohesion: 0.08
Nodes (26): ApiEnvelope, HTTPClient, HTTPError, serverError(), AuthFrame(), AuthFrameProps, DangNhap(), DangNhapProps (+18 more)

### Community 6 - "Docker Service Configuration"
Cohesion: 0.06
Nodes (33): build, depends_on, environment, ports, mysql, API_PROXY_TARGET, DB_PASSWORD, DB_URL (+25 more)

### Community 7 - "Frontend Dependencies and Scripts"
Cohesion: 0.08
Nodes (30): dependencies, @fontsource/handjet@^5.3.1, react@^19.3.1, react-dom@^19.3.1, uqr@^0.1.3, devDependencies, @types/node@^24.19.2, @types/react@^19.3.0 (+22 more)

### Community 9 - "Invoice Transactions and Payments"
Cohesion: 0.27
Nodes (3): HoaDonDAO, HoaDonKhoa, KetQuaThaoTac

### Community 10 - "Gift Catalog and Inventory"
Cohesion: 0.13
Nodes (4): QuaTangDAO, VoucherGiamGia, VoucherVatPham, VoucherVeTang

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

### Community 16 - "models.ts"
Cohesion: 0.29
Nodes (10): GDChonPhimProps, GDChonQuaProps, GDKhungGioProps, GDThongBaoProps, GDXacNhanDoiQuaProps, DoiDiemUI, KhungGioUI, PhimUI (+2 more)

### Community 19 - "gen-hall-scene.mjs"
Cohesion: 0.05
Nodes (38): buf, bulbAt, bulbs, bulbsBoard, bulbsBoardMirror, C, cbulbs, DIGITS (+30 more)

### Community 22 - "Screen"
Cohesion: 0.17
Nodes (30): ticket-demo.svg (Static QR Ticket Fixture), GDChonPhim(), GDChonQua(), GDKhachHangHome(), GDKhungGio(), GDSuatChieu(), GDThanhToan(), GDThanhToanProps (+22 more)

### Community 23 - "MySQL Tables and Constraints"
Cohesion: 0.28
Nodes (14): idx_tblVe_suatChieu_ghe, tblGhe, tblGiaoDichDoiDiem, tblHoaDon, tblKhachHang, tblKhungGio, tblLoaiGhe, tblPhim (+6 more)

### Community 27 - "Branch Flow Automation"
Cohesion: 0.18
Nodes (10): runs-on, steps, jobs, branch-flow, name, on, pull_request, branches (+2 more)

### Community 28 - "Invoice Operation Statuses"
Cohesion: 0.20
Nodes (10): MaThaoTac, DA_HUY, DA_THANH_TOAN, HET_HAN, KHONG_PHAI_CHU, KHONG_THE_XU_LY, KHONG_TIM_THAY, THANH_CONG (+2 more)

### Community 29 - "Preview.tsx"
Cohesion: 0.17
Nodes (16): doiDiemMau, gheDaChon, gheMau, hoaDonMau, hoaDonVoucher, khachMau, khungGioMau, phimMau (+8 more)

### Community 30 - "Demo Ticket QR Payload"
Cohesion: 0.33
Nodes (6): CINEMAN-DEMO Namespace Prefix, KHONG-CO-GIA-TRI-SU-DUNG Invalid-Use Marker, Invoice Reference HD001, Decoded Ticket Payload (CINEMAN-DEMO:HD001:VE001:VE002:KHONG-CO-GIA-TRI-SU-DUNG), QRcode Group (29x29 Module Matrix, 3.92cm), Ticket Codes VE001 and VE002

### Community 31 - "GDChonGhe.tsx"
Cohesion: 0.27
Nodes (11): CumGhe, GDChonGhe(), veNutGhe(), GDChonGheProps, HangGhe, nhanTrangThaiGhe(), nhomTheoCap(), nhomTheoHang() (+3 more)

### Community 33 - "Scoped Checkpoint Shell Script"
Cohesion: 0.83
Nodes (3): die(), agent-commit.sh script, usage()

### Community 38 - "Product"
Cohesion: 0.22
Nodes (8): Brand commitments, Constraints, Open decisions, Platform, Positioning, Product, Product Purpose, Users

### Community 40 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.18
Nodes (3): DAO, PhimDAO, SuatChieuDAO

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
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 356 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `KhachHang` connect `Booking Controllers and Customers` to `Business Contracts and Vocabulary`, `Member Authentication and Passwords`, `Invoice Transactions and Payments`, `Gift Catalog and Inventory`, `Customer Voucher Lifecycle`, `QuaTang`, `Persistence Imports and Utilities`, `Invoice Entity Fields`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `TaiKhoanApp()` (e.g. with `doiKhungGioUI()` and `doiPhimUI()`) actually correct?**
  _`TaiKhoanApp()` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `DA_HUY`, `DA_THANH_TOAN`, `HET_HAN` to the rest of the system?**
  _17 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `App.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.11498257839721254 - nodes in this community are weakly interconnected._
- **Why does `KhachHang (Customer Profile)` connect `Business Contracts and Vocabulary` to `Booking Controllers and Customers`, `Registration and HTTP Client`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Should `Backend Integration Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.05366726296958855 - nodes in this community are weakly interconnected._
- **Why does `KhachHang` connect `Registration and HTTP Client` to `App.tsx`, `Business Contracts and Vocabulary`, `Preview.tsx`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._