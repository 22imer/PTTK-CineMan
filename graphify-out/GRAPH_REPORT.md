# Graph Report - Tao-README  (2026-11-09)

## Corpus Check
- Corpus is ~43,735 words - fits in a single context window. You may not need a graph.

## Summary
- 928 nodes · 2352 edges · 37 communities (23 shown, 14 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 95 edges (avg confidence: 0.88)
- Token cost: unavailable — the agent tool exposed no measured input/output usage; this was not a zero-token run.

## Community Hubs (Navigation)
- Application Screens and Preview
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
- DAO Mapping and Repositories
- JDBC Persistence and Injection
- Ticket Creation and Storage
- Screenings and Schedule Data
- Agent Checkpoint Hook
- Point Redemption Receipts
- Gift Voucher Variants
- MySQL Tables and Constraints
- Invoice Entity Fields
- Cinema Rooms and Seating
- Time Slot Entity Fields
- Branch Flow Automation
- Invoice Operation Statuses
- Checkpoint Git Test Helpers
- Demo Ticket QR Payload
- Git State Test Helpers
- Time Slot Query DAO
- Scoped Checkpoint Shell Script
- Checkpoint Stop Event Tests
- Backend Maven Coordinate

## God Nodes (most connected - your core abstractions)
1. `AuthIntegrationTest` - 46 edges
2. `TaiKhoanApp()` - 44 edges
3. `M1IntegrationTest` - 37 edges
4. `SuatChieu` - 36 edges
5. `KhachHang` - 35 edges
6. `HoaDonDAO` - 34 edges
7. `VoucherKhachHang` - 34 edges
8. `Ve` - 33 edges
9. `DatVeController` - 31 edges
10. `Phim` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Ghe (Seat)` --semantically_similar_to--> `Ghe`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/Ghe.java
- `GiaoDichDoiDiem (Point Redemption Receipt)` --semantically_similar_to--> `GiaoDichDoiDiem`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/GiaoDichDoiDiem.java
- `HoaDon (Invoice)` --semantically_similar_to--> `HoaDon`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/HoaDon.java
- `KhachHang (Customer Profile)` --semantically_similar_to--> `KhachHang`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/KhachHang.java
- `KhungGio (Time Slot)` --semantically_similar_to--> `KhungGio`  [INFERRED] [semantically similar]
  GLOSSARY.md → backend/src/main/java/com/cineman/entity/KhungGio.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Canonical CineMan Business Vocabulary** — glossary_cineman_m1, glossary_thanhvien, glossary_khachhang, glossary_hoadon, glossary_ve, glossary_giaodichdoidiem, glossary_voucherkhachhang [EXTRACTED 1.00]
- **Colon-Delimited Ticket Payload Field Structure** — frontend_public_ticket_demo_payload, frontend_public_ticket_demo_brand_namespace, frontend_public_ticket_demo_invoice_ref, frontend_public_ticket_demo_ticket_refs, frontend_public_ticket_demo_invalid_marker [EXTRACTED 1.00]
- **Booking and Seat-Hold Flow** — specs_booking_scenario, glossary_hoadon, glossary_ve, glossary_ghe, glossary_suatchieu, architecture_seat_hold_300s [INFERRED 0.85]
- **Demo Ticket QR Rendering Path on Payment-Result Screen** — frontend_src_ui_preview, frontend_src_ui_models_hoadonui, frontend_public_ticket_demo, frontend_public_ticket_demo_qrcode [INFERRED 0.85]
- **Reward Point Redemption Flow** — specs_reward_redemption_scenario, glossary_giaodichdoidiem, glossary_quatang, glossary_voucherkhachhang, architecture_atomic_transactions [INFERRED 0.85]

## Communities (37 total, 14 thin omitted)

### Community 0 - "Application Screens and Preview"
Cohesion: 0.050381679389312976
Nodes (105): ticket-demo.svg (Static QR Ticket Fixture), App(), doiKhungGioUI(), doiPhimUI(), doiQuaTangUI(), doiSuatChieuUI(), ManHinh, params (+97 more)

### Community 1 - "Backend Integration Tests"
Cohesion: 0.0676704742125303
Nodes (4): AuthIntegrationTest, HoSo, M1IntegrationTest, SchemaSeedTest

### Community 2 - "Booking Controllers and Customers"
Cohesion: 0.07448107448107448
Nodes (15): ApiResponse, DangKyController, YeuCauDangKy, YeuCauDangNhap, DatVeController, SoDoGhe, TomTatHoaDon, YeuCauApDungVoucher (+7 more)

### Community 3 - "Business Contracts and Vocabulary"
Cohesion: 0.07256894049346879
Nodes (49): Agent Checkpoint Commit Protocol (issue #11), CineMan Agent Working Guidelines (AGENTS.md), M1 Scope: Three Functions Only, CineMan Architecture (ARCHITECTURE.md), DAO Layer with _fix Method Signatures, MySQL Schema: 13 Tables, 14 Foreign Keys, 12 React TypeScript Pages (GD*.tsx), REST API Envelope and Endpoint Groups (+41 more)

### Community 4 - "Member Authentication and Passwords"
Cohesion: 0.06659619450317125
Nodes (3): CinemanApplication, ThanhVienDAO, ThanhVien

### Community 5 - "Registration and HTTP Client"
Cohesion: 0.08739495798319327
Nodes (23): ApiEnvelope, HTTPClient, HTTPError, serverError(), AuthFrame(), AuthFrameProps, DangNhap(), DangNhapProps (+15 more)

### Community 6 - "Docker Service Configuration"
Cohesion: 0.06060606060606061
Nodes (33): build, depends_on, environment, ports, mysql, API_PROXY_TARGET, DB_PASSWORD, DB_URL (+25 more)

### Community 7 - "Frontend Dependencies and Scripts"
Cohesion: 0.08505747126436781
Nodes (28): dependencies, react@^19.3.1, react-dom@^19.3.1, uqr@^0.1.3, devDependencies, @types/node@^24.19.2, @types/react@^19.3.0, @types/react-dom@^19.3.0 (+20 more)

### Community 9 - "Invoice Transactions and Payments"
Cohesion: 0.1976284584980237
Nodes (3): HoaDonDAO, HoaDonKhoa, KetQuaThaoTac

### Community 11 - "Checkpoint Hook Test Harness"
Cohesion: 0.09881422924901186
Nodes (14): children, commandContext(), CommandHandler, GitResult, HOOK_FILE, hookContext(), HookHandler, HookHarness (+6 more)

### Community 12 - "Customer Voucher Lifecycle"
Cohesion: 0.14285714285714285
Nodes (3): TrangThaiVoucher, VoucherKhachHangDAO, VoucherKhachHang

### Community 13 - "TypeScript Compiler Configuration"
Cohesion: 0.09090909090909091
Nodes (21): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, forceConsistentCasingInFileNames, isolatedModules, jsx, lib, module (+13 more)

### Community 16 - "JDBC Persistence and Injection"
Cohesion: 0.1830065359477124
Nodes (5): DAO, GheDAO, GiaoDichDoiDiemDAO, SuatChieuDAO, VeDAO

### Community 19 - "Agent Checkpoint Hook"
Cohesion: 0.13970588235294118
Nodes (9): collectObservedPaths(), Exec, findSeparator(), OBSERVED, parseCheckpointArgs(), pathsFromText(), SCRIPT_PATH, SessionContextLike (+1 more)

### Community 22 - "Gift Voucher Variants"
Cohesion: 0.20952380952380953
Nodes (3): VoucherGiamGia, VoucherVatPham, VoucherVeTang

### Community 23 - "MySQL Tables and Constraints"
Cohesion: 0.2761904761904762
Nodes (14): idx_tblVe_suatChieu_ghe, tblGhe, tblGiaoDichDoiDiem, tblHoaDon, tblKhachHang, tblKhungGio, tblLoaiGhe, tblPhim (+6 more)

### Community 27 - "Branch Flow Automation"
Cohesion: 0.18181818181818182
Nodes (10): runs-on, steps, jobs, branch-flow, name, on, pull_request, branches (+2 more)

### Community 28 - "Invoice Operation Statuses"
Cohesion: 0.2
Nodes (10): MaThaoTac, DA_HUY, DA_THANH_TOAN, HET_HAN, KHONG_PHAI_CHU, KHONG_THE_XU_LY, KHONG_TIM_THAY, THANH_CONG (+2 more)

### Community 29 - "Checkpoint Git Test Helpers"
Cohesion: 0.2857142857142857
Nodes (7): cleanEnv(), commitCount(), git(), holdCheckpointLock(), initRepo(), runCheckpoint(), write()

### Community 30 - "Demo Ticket QR Payload"
Cohesion: 0.3333333333333333
Nodes (6): CINEMAN-DEMO Namespace Prefix, KHONG-CO-GIA-TRI-SU-DUNG Invalid-Use Marker, Invoice Reference HD001, Decoded Ticket Payload (CINEMAN-DEMO:HD001:VE001:VE002:KHONG-CO-GIA-TRI-SU-DUNG), QRcode Group (29x29 Module Matrix, 3.92cm), Ticket Codes VE001 and VE002

### Community 31 - "Git State Test Helpers"
Cohesion: 0.3333333333333333
Nodes (6): gitText(), headTree(), indexEntries(), indexTree(), nulTerms(), statusEntries()

### Community 33 - "Scoped Checkpoint Shell Script"
Cohesion: 0.8333333333333334
Nodes (3): die(), agent-commit.sh script, usage()

### Community 34 - "Checkpoint Stop Event Tests"
Cohesion: 0.5
Nodes (4): emitHook(), stopBlocks(), stopEvent(), stopNow()

## Knowledge Gaps
- **18 isolated node(s):** `com.cineman:cineman-backend`, `THANH_CONG`, `DA_THANH_TOAN`, `DA_HUY`, `HET_HAN` (+13 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 290 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `KhachHang` connect `Booking Controllers and Customers` to `Business Contracts and Vocabulary`, `Member Authentication and Passwords`, `Customer Voucher Lifecycle`, `Ticket Creation and Storage`, `Point Redemption Receipts`, `Persistence Imports and Utilities`, `Gift Voucher Variants`, `Invoice Entity Fields`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `TaiKhoanApp()` (e.g. with `doiKhungGioUI()` and `doiPhimUI()`) actually correct?**
  _`TaiKhoanApp()` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.cineman:cineman-backend`, `THANH_CONG`, `DA_THANH_TOAN` to the rest of the system?**
  _18 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Application Screens and Preview` be split into smaller, more focused modules?**
  _Cohesion score 0.050381679389312976 - nodes in this community are weakly interconnected._
- **Why does `KhachHang (Customer Profile)` connect `Business Contracts and Vocabulary` to `Booking Controllers and Customers`, `Registration and HTTP Client`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **Should `Backend Integration Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.0676704742125303 - nodes in this community are weakly interconnected._
- **Why does `KhachHang` connect `Registration and HTTP Client` to `Application Screens and Preview`, `Business Contracts and Vocabulary`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._

## Integrity and Coverage Audit

- All 6 semantic sources report complete coverage; no partial or missing source reports.
- Extraction integrity: 142 dangling-endpoint edges; 0 missing-endpoint edges; 4 self-loop edges. Default undirected construction collapses 133 same-endpoint edges (directed diagnostic: 131). These are extraction/build limitations, not verified application defects.
- External references: 9 edges. The builder materializes unresolved references; the graph is a navigation aid, not a complete verified call graph.
- No modeled symbols for `frontend/vite.config.ts` and SQL INSERT data in `backend/src/main/resources/data.sql`; `backend/pom.xml` contributes its Maven coordinate only.
- Added 21 exact-name glossary-to-Java/TypeScript links as INFERRED (0.95), not verified runtime dependencies. QR payload interpretation does not establish ticket-gate enforcement.
- `graphify-out/` was written into the working tree and is not ignored. Commit only team-shareable outputs or add it to `.gitignore`; see https://docs.graphify.com/guides/team-workflows.
- Retrieval smoke: `graphify query "customer voucher invoice" --budget 1000` completed, finding 210 nodes with source anchors and named communities; output was truncated to the requested budget.
- Browser smoke: Lightpanda loaded the HTML, 928 node records, 2,352 edge records, and 37 community records. Canvas rendering stops at `TypeError: I.ctx.measureText is not a function`; visual layout, search, filtering, and graph interactions remain unverified. No Chromium fallback was used.
- CLI benchmark estimate: 46,400 words / ~61,866 naive tokens versus ~1,628 tokens per sampled query (38.0x). This is an estimate, not measured model usage.
