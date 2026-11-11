# scripts/

| File | Việc |
| :--- | :--- |
| `agent-commit.sh` (+ `agent-commit.test.ts`) | Checkpoint commit theo scope cho agent (issue #11). |
| `check-graphify.py` | CI `graph-fresh`: `graphify-out/manifest.json` khớp file thật. Sửa: `graphify update .` rồi commit `graphify-out/`. |
| `check-md-links.py` | CI `doc-links`: `*.md` không trỏ tới file không tồn tại. Tham chiếu cố ý ngoài repo ghi vào `EXTERNAL` trong script. |

## Checkpoint commit cho agent (issue #11)

- **Commit theo scope, đừng dồn change.** Xong một đơn vị mạch lạc (một issue/phase nhỏ) thì checkpoint ngay.
  Chỉ stage đúng file của mình, không `git add -A`, không stage file ngoài scope (ghi chú giao diện cục bộ,
  metadata IDE `.project`, `.classpath`, `.settings/`).
- **Kiểm chứng trước, commit sau.** Chạy gate liên quan (`mvn test`, `npm run build`, kịch bản smoke ở `AGENTS.md` §5)
  rồi mới checkpoint. Hook chỉ nhắc/chặn vì file còn dirty; nó **không chạy test**. Không được nói "test xanh" nếu chưa chạy.
- **Cây dùng chung:** subagent sửa xong thì bàn giao file + kết quả kiểm chứng cho coordinator; **chỉ coordinator**
  chạy integration commit để tránh tranh chấp index. Không tự commit file của agent/người dùng khác.
- **Cách gọi:**

  ```bash
  # Slash command: để message trần (không bọc quote); quote chỉ dành cho đường dẫn file.
  /checkpoint feat(auth): mô tả ngắn (#7) -- backend/src/main/java/com/cineman/controller/DangKyController.java
  # hoặc gọi trực tiếp (đây là shell nên message vẫn phải quote):
  bash scripts/agent-commit.sh "feat(auth): mô tả ngắn (#7)" -- backend/src/main/java/com/cineman/controller/DangKyController.java
  ```

  Message của `/checkpoint` là **toàn bộ text thô** trước dấu `--` độc lập **cuối cùng**, giữ nguyên khoảng trắng
  **và cả dấu quote** vì slash command không shell-tokenize. Đừng bọc message trong `"..."`/`'...'` — dấu quote sẽ
  lọt vào commit message; chỉ quote đường dẫn file sau `--` khi tên có khoảng trắng/ký tự đặc biệt, ví dụ
  `-- "backend/src/main/resources/application.properties"`. Khi gọi trực tiếp `bash scripts/agent-commit.sh`,
  message là đối số shell nên vẫn quote như bình thường.
  Mỗi token sau `--` phải là một file cụ thể trong repo: không dùng `.`, thư mục, wildcard, pathspec magic,
  path tuyệt đối/ngoài repo hoặc `.git`. Script từ chối index
  đang staged, merge/rebase/conflict; stage đúng file được liệt kê, `git diff --cached --check`, commit local;
  không push/reset/clean. Không có thay đổi thì exit 0 và không tạo commit rỗng. Lỗi commit giữ nguyên file đã stage.
- **Hook native** `.omp/hooks/post/commit-checkpoint.ts`: sau `edit`/`write` thành công, `tool_result` của hook nhắc
  checkpoint và nhớ file local trong phiên. Khi phiên chính kết thúc (`session_stop`), hook chặn nếu các file đó vẫn
  dirty; file không quan sát được thì không bị chặn. File do subagent `edit`/`write` **vẫn được `tool_result` ghi
  nhận**, nhưng `session_stop` không chạy cho subagent nên không có gì chặn — coordinator phải chạy integration
  commit. File do `eval`/`bash` ghi hoặc thay đổi ngoài phiên hoàn toàn không được quan sát; các trường hợp đó vẫn
  phải chạy `/checkpoint` thủ công. Hook không tự commit.
- **Reload:** OMP discover `.omp/hooks/pre|post/*.ts` và đăng ký `/checkpoint` lúc nạp phiên. Sau khi thêm/sửa hook,
  khởi động lại phiên omp (hoặc reload phiên) rồi kiểm tra `/checkpoint` có trong danh sách command. Không cần
  cấu hình global/settings.
