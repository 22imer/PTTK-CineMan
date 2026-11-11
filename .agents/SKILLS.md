# SKILLS.md — Skills đã dùng trong dự án CineMan (PTTK-Project)

Tổng hợp từ transcript phiên omp của các worktree `PTTK-Project/*` và thư mục gốc `PTTK2`, `PTTK3`, `PTTK-Project`
(49 phiên, 21/09/2026 → 11/11/2026). Chỉ tính **skill thực sự được agent đọc** (`read skill://<tên>` hoặc `SKILL.md`),
không tính skill chỉ xuất hiện trong danh sách có sẵn. Cột "Phiên" = số phiên đã đọc skill đó (30/49 phiên có dùng skill).

Quy ước dự án vẫn lấy từ `AGENTS.md` và `ARCHITECTURE.md`; file này chỉ là chỉ mục để biết **khi nào gọi skill nào**.

## 1. Quy trình lõi (dùng ở hầu hết phiên)

| Skill | Phiên | Dùng để làm gì trong dự án |
| --- | --- | --- |
| `brainstorming` | 13 | Trước mỗi tính năng/thay đổi mới (đặt vé, JWT, UI, DAO): làm rõ yêu cầu và thiết kế trước khi code |
| `verification-before-completion` | 11 | Chạy `mvn test`, `npm run build`, smoke seed §8 trước khi báo xong; không nói "xanh" khi chưa chạy |
| `contributing` | 11 | Quy ước sửa repo: nhánh `dev`→`main`, PR, commit scope, checkpoint |
| `implement` | 8 | Viết code tính năng/sửa lỗi theo `ARCHITECTURE.md` |
| `test-driven-development` | 8 | Test trước cho DAO/controller (R1–R12, giao dịch `taoHoaDon`, `thucHienDoiDiem`) |
| `writing-plans` | 7 | Lập kế hoạch nhiều bước trước khi code (DAO redesign, JWT, UI, issue #1) |
| `ponytail` | 7 | Giữ code đơn giản, tránh over-engineering, đúng 3 chức năng M1 |
| `systematic-debugging` | 6 | Lỗi Unicode seed MySQL, JWT, DAO, lỗi PR |
| `finishing-a-development-branch` | 5 | Chốt nhánh: merge/PR vào `dev`, dọn nhánh |
| `grilling` | 10 | Chất vấn kế hoạch/thiết kế trước khi chốt (DAO, session, pixel UI, issue còn tồn) |

## 2. Phân tích & thiết kế (báo cáo PTTK, `bc.docx`, sơ đồ)

| Skill | Phiên | Dùng để làm gì |
| --- | --- | --- |
| `domain-modeling` | 7 | `GLOSSARY.md`, thuật ngữ nghiệp vụ, thực thể, ADR |
| `archify` | 5 | Vẽ kiến trúc/sequence/workflow, cập nhật sơ đồ lớp, DAO |
| `graphify` | 4 | Sinh knowledge graph `graphify-out/` (`GRAPH_REPORT.md`, `graph.json`) |
| `doc-coauthoring` | 3 | Soạn README, ARCHITECTURE, tài liệu chương 2/3 |
| `convert-documents-to-markdown` | 3 | Chuyển `bc.docx`/PDF nhận xét sang Markdown để đối chiếu |
| `preview` | 3 | Xem Markdown/PDF render, xuất báo cáo |
| `grill-with-docs` | 3 | Chất vấn thiết kế có đối chiếu tài liệu (đã gỡ khỏi danh sách skill hiện tại) |
| `ooad-expert` | 5 (đều ở PTTK2) | Phân tích hướng đối tượng (use case, lớp thực thể, hoạt động) — đã gỡ khỏi danh sách hiện tại |
| `plantuml-ascii` | 2 | Phác sơ đồ PlantUML dạng ASCII — đã gỡ khỏi danh sách hiện tại |
| `docx` | 1 | Sửa file Word báo cáo |
| `to-spec` | 1 | Viết spec từ yêu cầu — đã gỡ khỏi danh sách hiện tại |

## 3. Frontend / UI

| Skill | Phiên | Dùng để làm gì |
| --- | --- | --- |
| `ui-ux` | 5 | Dựng 12 trang `GD*.tsx` từ `UI.md`, kiểm tra theo checklist |
| `agent-browser` | 8 | Chạy thử giao diện thật (Docker `:5173`), chụp màn hình, kiểm tra kịch bản |
| `frontend-design` | 2 | Giao diện pixel-art 16-bit sảnh rạp (PR #32) |
| `impeccable` | 1 | Được yêu cầu cho redesign pixel-art nhưng không có trong danh sách khi đó; thay bằng `frontend-design` + `ui-ux` |
| `grill-me` | 1 | Tương tự, thay bằng `grilling` |

## 4. Git, review, vận hành

| Skill | Phiên | Dùng để làm gì |
| --- | --- | --- |
| `dispatching-parallel-agents` | 6 | Chia việc song song cho subagent (issue #1, UI, README) |
| `writing-for-agents` | 5 | Viết/sửa `AGENTS.md`, skill, hook (issue #11, retro) |
| `lsp-verify` | 3 | Chẩn đoán LSP sau khi sửa Java/TS |
| `find-docs` | 3 | Tra tài liệu thư viện/framework hiện hành (phiên PTTK2, JWT) |
| `requesting-code-review` | 2 | Yêu cầu review PR |
| `receiving-code-review` | 2 | Xử lý nhận xét review trước khi sửa (phiên JWT, `PTTK-Project`) |
| `release-skills` | 2 | Phát hành / ghi release |
| `docker` | 2 | Chạy stack `docker compose` |
| `customize` | 2 | Cấu hình agent/skill/connector |
| `product-self-knowledge` | 2 | Hỏi về hành vi/cấu hình của chính agent |
| `code-review` | 1 | Review nhánh/PR ở worktree `Xu-ly-PR` |
| `resolving-merge-conflicts` | 1 | Xử lý xung đột khi merge `dev`↔`main` |
| `lsp-impact` | 1 | Đo tác động trước khi bỏ `GheDAO` → `VeDAO` |
| `subagent-driven-development` | 1 | Thực thi kế hoạch JWT bằng subagent |
| `using-git-worktrees` | 1 | Tách worktree theo issue/tính năng (`Orca`) |
| `find-security-vulnerabilities-in-code` | 1 | Rà lỗ hổng cho cơ chế session/JWT |
| `orchestration` | 1 | Điều phối worker khác |
| `self-awareness` | 1 | Xem lại lịch sử phiên/file đã ghi |

## 5. Dùng một lần ở giai đoạn đầu (PTTK2), ngoài phạm vi code hiện tại

`autoresearch`, `memory-discipline`, `pi-lens-lsp-navigation`, `steel-browser`, `remember`, `typesafe-ai`, `jevris`.
Phục vụ việc cài môi trường/công cụ (jdt.ls, Steel MCP, Jevris advisor), không ảnh hưởng đến M1.

## 6. Gợi ý chọn skill theo việc

| Việc | Skill nên đọc |
| --- | --- |
| Tính năng mới | `brainstorming` → `writing-plans` → `test-driven-development` → `implement` → `verification-before-completion` |
| Lỗi/test đỏ | `systematic-debugging` → `test-driven-development` |
| Sửa spec/sơ đồ | `domain-modeling`, `archify`, `doc-coauthoring` |
| Sửa UI | `ui-ux`, `frontend-design`, kiểm bằng `agent-browser` |
| Kết thúc nhánh/PR | `verification-before-completion` → `finishing-a-development-branch`; review: `requesting-code-review`/`receiving-code-review` |
| Sửa `AGENTS.md`/hook | `writing-for-agents` |

## 7. Ghi chú

- Các skill đánh dấu "đã gỡ" (`ooad-expert`, `plantuml-ascii`, `grill-with-docs`, `to-spec`, `impeccable`, `grill-me`,
  `memory-discipline`, `pi-lens-lsp-navigation`, `steel-browser`, `typesafe-ai`, `jevris`, `remember`) không còn trong
  danh sách skill của phiên hiện tại; dùng skill thay thế cùng cột ở trên.
- Số phiên lấy từ `~/.omp/agent/sessions/*PTTK*`; đếm lại khi có phiên mới.
