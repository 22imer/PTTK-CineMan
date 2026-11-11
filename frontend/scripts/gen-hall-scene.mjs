// Sinh frontend/src/assets/hall-scene.svg: sảnh rạp pixel art 192x108, đối xứng qua cột 95.5.
// Chạy: node scripts/gen-hall-scene.mjs  (không phải bước build; chỉ chạy lại khi sửa tranh).
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const W = 192, H = 108, CX = 95.5;
const buf = Array.from({ length: H }, () => Array(W).fill(null));
const px = (x, y, c) => { if (x >= 0 && x < W && y >= 0 && y < H) buf[y][x] = c; };
const rect = (x0, y0, x1, y1, c) => { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) px(x, y, c); };
const checker = (x, y, n = 1) => ((Math.floor(x / n) + Math.floor(y / n)) & 1) === 0;
const mirror = (x) => W - 1 - x;
const sym = (x, y, c) => { px(x, y, c); px(mirror(x), y, c); };
const symRect = (x0, y0, x1, y1, c) => { rect(x0, y0, x1, y1, c); rect(mirror(x1), y0, mirror(x0), y1, c); };

const C = {
  ink: '#120a10', wood0: '#1c0f0a', wood1: '#3a2216', wood2: '#5e3a22', wood3: '#8a5a34',
  wine0: '#2b0d14', wine1: '#3a1019', wine2: '#5c1220', wine3: '#7c1a2e', wine4: '#a22a42',
  gold0: '#8c5a14', gold1: '#c98a2a', gold2: '#f2b84b', butter: '#ffe08a', cream: '#fff3d1',
  cy0: '#0a3d47', cy1: '#0e6f7a', cy2: '#17c4d6', cy3: '#29f0ff',
  red: '#d9344a', teal: '#29c2d6', yellow: '#ffd24a', green: '#4cc27a', orange: '#ff8a3d', violet: '#b36bd6',
};

// 1. Tường nhung: sọc dọc 4px, tối dần lên trần.
for (let y = 0; y < 92; y++) for (let x = 0; x < W; x++) {
  const stripe = Math.floor(x / 4) & 1;
  let c = stripe ? C.wine1 : C.wine0;
  if (y < 14 && checker(x, y)) c = C.ink;
  px(x, y, c);
}
// 2. Trần: xà gỗ, dentil và diềm màn.
rect(0, 0, W - 1, 3, C.wood0); rect(0, 4, W - 1, 5, C.wood1); rect(0, 6, W - 1, 6, C.wood3);
for (let x = 2; x < W; x += 6) rect(x, 7, x + 2, 8, C.wood2);
// 3. Màn nhung hai bên, nếp gấp chu kỳ 6px, tua vàng.
const fold = [C.wine3, C.wine4, C.wine3, C.wine2, C.wine2, C.wine1];
for (let x = 0; x < 9; x++) for (let y = 9; y < 84; y++) { px(x, y, fold[x % 6]); px(mirror(x), y, fold[x % 6]); }
for (let x = 0; x < 9; x++) { sym(x, 84, C.gold2); sym(x, 85, x % 2 ? C.gold1 : C.gold0); sym(x, 86, x % 2 ? C.gold0 : C.gold1); }
rect(0, 9, W - 1, 9, C.gold2);
for (let x = 0; x < W; x++) {
  const drop = 2 + Math.round(2 * Math.abs(Math.sin((x * Math.PI) / 12)));
  for (let y = 10; y < 10 + drop; y++) px(x, y, fold[x % 6]);
  px(x, 10 + drop, C.gold1);
}
// 4. Sàn caro 8x8, vệt sáng ấm dưới vòm.
for (let y = 92; y < H; y++) for (let x = 0; x < W; x++) {
  const tile = (Math.floor(x / 8) + Math.floor((y - 92) / 8)) & 1;
  let c = tile ? C.wood2 : C.wood1;
  const dx = Math.abs(x - CX) / 46, dy = (y - 92) / 14;
  if (dx * dx + dy * dy < 1 && checker(x, y)) c = tile ? C.wood3 : C.wood2;
  px(x, y, c);
}
rect(0, 92, W - 1, 92, C.gold1); rect(0, 93, W - 1, 93, C.wood0);

// 5. Vòm neon bậc thang.
const STEP_DX = [12, 22, 32, 42], STEP_Y = [14, 18, 23, 29, 36];
const topY = (x) => { const dx = Math.abs(x - CX); let i = 0; while (i < STEP_DX.length && dx >= STEP_DX[i]) i++; return STEP_Y[i]; };
const ARCH_X0 = 44, ARCH_X1 = 147, ARCH_BOTTOM = 76;
const inside0 = (x, y) => x >= ARCH_X0 && x <= ARCH_X1 && y >= topY(x) && y <= ARCH_BOTTOM + 40;
const inErode = (x, y, e) => {
  for (let j = -e; j <= e; j++) for (let i = -e; i <= e; i++) if (!inside0(x + i, y + j) && y + j <= ARCH_BOTTOM) return false;
  return true;
};
const inDilate = (x, y, d) => {
  for (let j = -d; j <= d; j++) for (let i = -d; i <= d; i++) if (inside0(x + i, y + j)) return true;
  return false;
};
for (let y = 0; y <= ARCH_BOTTOM; y++) for (let x = 0; x < W; x++) {
  if (inside0(x, y)) continue;
  if (inDilate(x, y, 1)) px(x, y, C.cy1);
  else if (inDilate(x, y, 4) && checker(x, y)) px(x, y, C.cy0);
  else if (inDilate(x, y, 7) && checker(x, y, 2) && checker(x, y)) px(x, y, C.cy0);
}
const rings = [[0, C.cy3], [4, C.cy2], [8, C.cy1]];
for (let y = 0; y <= ARCH_BOTTOM; y++) for (let x = ARCH_X0; x <= ARCH_X1; x++) {
  if (!inside0(x, y)) continue;
  let c = null;
  for (const [e, col] of rings) if (inErode(x, y, e) && !inErode(x, y, e + 2)) { c = col; break; }
  if (c) px(x, y, c);
  else if (inErode(x, y, 10)) px(x, y, checker(x, y) && y < 50 ? C.wine0 : C.ink);
  else px(x, y, C.ink);
}
// ánh sáng cyan lan trong vòm (dither nhẹ cạnh vòng trong).
for (let y = 0; y <= ARCH_BOTTOM; y++) for (let x = ARCH_X0; x <= ARCH_X1; x++) {
  if (inErode(x, y, 10) && !inErode(x, y, 13) && checker(x, y)) px(x, y, C.cy0);
}

// 6. Cửa đôi gỗ, ô tròn sáng, thanh đẩy vàng, ánh sáng ấm tràn ra.
const DX0 = 78, DX1 = 113, DY0 = 50, DY1 = ARCH_BOTTOM;
rect(DX0 - 2, DY0 - 2, DX1 + 2, DY1, C.wood0);
rect(DX0, DY0, DX1, DY1, C.wood2);
for (const [a, b] of [[DX0, 95], [96, DX1]]) {
  rect(a, DY0, b, DY1, C.wood2);
  rect(a, DY0, b, DY0, C.wood3); rect(a, DY0, a, DY1, C.wood3);
  rect(a + 3, DY0 + 4, b - 3, DY0 + 11, C.wood1); rect(a + 4, DY0 + 5, b - 4, DY0 + 10, C.butter);
  rect(a + 6, DY0 + 7, b - 6, DY0 + 8, C.cream);
  rect(a + 3, DY0 + 15, b - 3, DY1 - 3, C.wood1); rect(a + 4, DY0 + 16, b - 4, DY1 - 4, C.wood2);
}
rect(95, DY0, 96, DY1, C.wood0);
rect(88, DY0 + 17, 93, DY0 + 18, C.gold2); rect(98, DY0 + 17, 103, DY0 + 18, C.gold2);
rect(DX0 - 3, DY0 - 3, DX1 + 3, DY0 - 3, C.gold2);
for (let y = DY0 - 8; y < DY0 - 3; y++) for (let x = DX0 - 2; x <= DX1 + 2; x++) if (checker(x, y)) px(x, y, C.gold0);

// 7. Bảng suất chiếu hai bên: 3x2 ô số nhiều màu.
const DIGITS = {
  1: ['.X.', 'XX.', '.X.', '.X.', 'XXX'], 2: ['XXX', '..X', 'XXX', 'X..', 'XXX'], 3: ['XXX', '..X', 'XXX', '..X', 'XXX'],
  4: ['X.X', 'X.X', 'XXX', '..X', '..X'], 5: ['XXX', 'X..', 'XXX', '..X', 'XXX'], 6: ['XXX', 'X..', 'XXX', 'X.X', 'XXX'],
};
const glyph = (rows, x, y, c) => rows.forEach((r, j) => [...r].forEach((ch, i) => { if (ch === 'X') px(x + i, y + j, c); }));
const PANEL = [C.red, C.teal, C.yellow, C.green, C.orange, C.violet];
const BX0 = 11, BY0 = 38;
rect(BX0, BY0, BX0 + 31, BY0 + 28, C.wood0); rect(BX0 + 1, BY0 + 1, BX0 + 30, BY0 + 27, C.gold1);
rect(BX0 + 2, BY0 + 2, BX0 + 29, BY0 + 26, C.wood0);
rect(BX0 + 2, BY0 + 2, BX0 + 29, BY0 + 4, C.wine2);
const bulbsBoard = [];
for (let i = 0; i < 7; i++) bulbsBoard.push([BX0 + 3 + i * 4, BY0 + 3, i & 1 ? 'B' : 'A']);
for (let n = 0; n < 6; n++) {
  const col = n % 3, row = Math.floor(n / 3);
  const x = BX0 + 3 + col * 9, y = BY0 + 7 + row * 9;
  rect(x, y, x + 7, y + 7, PANEL[n]);
  rect(x, y + 7, x + 7, y + 7, C.wood0); rect(x + 7, y, x + 7, y + 7, C.wood0);
  glyph(DIGITS[n + 1], x + 2, y + 1, C.wood0);
}
rect(BX0 + 4, BY0 + 29, BX0 + 27, BY0 + 31, C.wood1); // chân bảng
// bảng phải: nhân đôi trái → phải bằng cách sao chép cột (phản chiếu), rồi vẽ lại chữ số không bị lật.
for (let y = BY0; y <= BY0 + 31; y++) for (let x = BX0; x <= BX0 + 31; x++) px(mirror(x), y, buf[y][x]);
const bulbsBoardMirror = bulbsBoard.map(([x, y, g]) => [mirror(x), y, g === 'A' ? 'B' : 'A']);
for (let n = 0; n < 6; n++) {
  const col = n % 3, row = Math.floor(n / 3);
  const x = BX0 + 3 + col * 9, y = BY0 + 7 + row * 9;
  const mx = mirror(x + 7);
  rect(mx, y, mx + 7, y + 7, PANEL[(n + 3) % 6]);
  rect(mx, y + 7, mx + 7, y + 7, C.wood0); rect(mx + 7, y, mx + 7, y + 7, C.wood0);
  glyph(DIGITS[((n + 3) % 6) + 1], mx + 2, y + 1, C.wood0);
}

// 8. Bảng hiệu marquee CINEMAN, bóng đèn nhấp nháy hai pha.
const GL = {
  C: ['.XXX.', 'X...X', 'X....', 'X....', 'X....', 'X...X', '.XXX.'], I: ['XXXXX', '..X..', '..X..', '..X..', '..X..', '..X..', 'XXXXX'],
  N: ['X...X', 'XX..X', 'X.X.X', 'X..XX', 'X...X', 'X...X', 'X...X'], E: ['XXXXX', 'X....', 'X....', 'XXXX.', 'X....', 'X....', 'XXXXX'],
  M: ['X...X', 'XX.XX', 'X.X.X', 'X.X.X', 'X...X', 'X...X', 'X...X'], A: ['.XXX.', 'X...X', 'X...X', 'XXXXX', 'X...X', 'X...X', 'X...X'],
};
const SX0 = 61, SX1 = 130, SY0 = 27, SY1 = 42;
rect(SX0 - 1, SY0 - 1, SX1 + 1, SY1 + 1, C.wood0);
rect(SX0, SY0, SX1, SY1, C.wine2);
for (let y = SY0 + 1; y < SY1; y++) for (let x = SX0 + 1; x < SX1; x++) if (checker(x, y)) px(x, y, C.wine3);
rect(SX0 + 3, SY0 + 3, SX1 - 3, SY1 - 3, C.wine0);
const word = 'CINEMAN';
const tx0 = Math.round(CX - (word.length * 6 - 1) / 2);
[...word].forEach((ch, i) => { glyph(GL[ch], tx0 + i * 6 + 1, SY0 + 6, C.gold0); glyph(GL[ch], tx0 + i * 6, SY0 + 5, C.butter); });
const bulbs = [...bulbsBoard, ...bulbsBoardMirror];
let k = 0;
for (let x = SX0 + 1; x <= SX1 - 1; x += 3) { bulbs.push([x, SY0 + 1, k & 1 ? 'B' : 'A'], [x, SY1 - 1, k & 1 ? 'A' : 'B']); k++; }
for (let y = SY0 + 4; y <= SY1 - 4; y += 3) { bulbs.push([SX0 + 1, y, k & 1 ? 'B' : 'A'], [SX1 - 1, y, k & 1 ? 'A' : 'B']); k++; }
// hai dây treo bảng hiệu
for (const x of [SX0 + 6, SX1 - 6]) for (let y = 14; y < SY0 - 1; y++) px(x, y, C.gold1);

// 9. Quầy bắp nước: mặt quầy, thanh gỗ, máy bắp rang trái, máy nước phải, máy tính tiền giữa.
rect(60, 76, 131, 77, C.gold2); rect(60, 78, 131, 79, C.gold1);
for (let x = 60; x <= 131; x++) rect(x, 80, x, 91, (Math.floor((x - 60) / 4) & 1) ? C.wood1 : C.wood2);
rect(60, 80, 131, 80, C.wood3); rect(60, 90, 131, 91, C.wood0);
const cbulbs = [];
for (let x = 66; x <= 126; x += 6) { rect(x - 1, 83, x + 1, 85, C.wood0); cbulbs.push([x, 84, (x - 66) / 6 & 1 ? 'B' : 'A']); }
// máy bắp rang
const PM = 62;
rect(PM, 70, PM + 21, 75, C.red); rect(PM, 70, PM + 21, 70, C.gold2); rect(PM + 2, 72, PM + 19, 73, C.wine3);
rect(PM, 56, PM + 21, 69, C.cy2); rect(PM + 1, 57, PM + 20, 69, C.cy0);
for (let y = 63; y < 70; y++) for (let x = PM + 2; x < PM + 20; x++) {
  const h = (x * 7 + y * 13) % 5;
  px(x, y, h === 0 ? C.cream : h < 3 ? C.butter : C.gold2);
}
rect(PM + 1, 57, PM + 20, 62, C.wine0);
[...'POP'].forEach((ch, i) => {
  const g = { P: ['XX.', 'X.X', 'XX.', 'X..', 'X..'], O: ['.X.', 'X.X', 'X.X', 'X.X', '.X.'] }[ch];
  glyph(g, PM + 5 + i * 4, 57, C.butter);
});
rect(PM + 1, 57, PM + 1, 69, C.cy3);
rect(PM - 1, 52, PM + 22, 55, C.red); rect(PM + 2, 49, PM + 19, 51, C.red); rect(PM + 7, 47, PM + 14, 48, C.red);
rect(PM - 1, 55, PM + 22, 55, C.gold2); rect(PM + 2, 51, PM + 19, 51, C.gold2); rect(PM + 9, 45, PM + 12, 46, C.gold2);
// máy nước (đối xứng)
const DM = mirror(PM + 21);
rect(DM, 70, DM + 21, 75, C.cy1); rect(DM, 70, DM + 21, 70, C.gold2);
rect(DM, 52, DM + 21, 69, C.wood0); rect(DM + 1, 53, DM + 20, 68, C.cy1);
rect(DM + 3, 55, DM + 18, 62, C.cy3); rect(DM + 4, 56, DM + 17, 61, C.cy2);
for (let y = 56; y < 62; y++) for (let x = DM + 4; x < DM + 18; x++) if (checker(x, y)) px(x, y, C.cy3);
rect(DM + 5, 64, DM + 7, 67, C.red); rect(DM + 9, 64, DM + 11, 67, C.yellow); rect(DM + 13, 64, DM + 15, 67, C.green);
rect(DM + 2, 48, DM + 19, 51, C.wine2); rect(DM + 2, 51, DM + 19, 51, C.gold2); rect(DM + 5, 49, DM + 16, 50, C.butter);
// cốc trên quầy và máy tính tiền
for (const [cx, col] of [[85, C.red], [102, C.teal]]) { rect(cx, 70, cx + 4, 75, col); rect(cx, 70, cx + 4, 70, C.cream); rect(cx + 1, 66, cx + 3, 69, col); }
rect(91, 70, 100, 75, C.wood1); rect(92, 66, 99, 70, C.wood0); rect(93, 67, 98, 69, C.green);

// ---- xuất SVG: gộp pixel thành đoạn ngang, rồi gộp theo màu thành path.
const runs = new Map();
const bulbAt = new Map();
for (const [x, y, g] of [...bulbs, ...cbulbs]) bulbAt.set(`${x},${y}`, g);
for (let y = 0; y < H; y++) {
  let x = 0;
  while (x < W) {
    const c = buf[y][x];
    const isBulb = bulbAt.has(`${x},${y}`);
    if (!c || isBulb) { x++; continue; }
    let e = x;
    while (e + 1 < W && buf[y][e + 1] === c && !bulbAt.has(`${e + 1},${y}`)) e++;
    if (!runs.has(c)) runs.set(c, []);
    runs.get(c).push(`M${x} ${y}h${e - x + 1}v1h-${e - x + 1}z`);
    x = e + 1;
  }
}
const paths = [...runs].map(([c, d]) => `<path fill="${c}" d="${d.join('')}"/>`).join('\n');
const bulbPath = (g) => [...bulbAt].filter(([, v]) => v === g).map(([k]) => { const [x, y] = k.split(','); return `M${x} ${y}h1v1h-1z`; }).join('');
const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}" shape-rendering="crispEdges">
<style>
.bulb{fill:#fff3d1}
@media (prefers-reduced-motion: no-preference){.bulb--a{animation:blink 1.4s steps(1) infinite}.bulb--b{animation:blink 1.4s steps(1) infinite .7s}}
@keyframes blink{50%{fill:#8c5a14}}
</style>
${paths}
<path class="bulb bulb--a" d="${bulbPath('A')}"/>
<path class="bulb bulb--b" d="${bulbPath('B')}"/>
</svg>
`;
const out = resolve(dirname(fileURLToPath(import.meta.url)), '../src/assets/hall-scene.svg');
mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, svg);
console.log('wrote', out, svg.length, 'bytes');
