// 从原 HTML 抽取内置课程数据 + 图标定义，产出 Android 工程资源
const fs = require('fs');
const path = require('path');
const HTML = '/root/userdata/attachment/桌面运维_星球主题版261113_v6.html';
const lines = fs.readFileSync(HTML, 'utf8').split('\n');
const OUT = path.resolve(__dirname, '..');

/* ---------- 1. 数据 ---------- */
const dataCode = lines.slice(2729, 3925).join('\n'); // SECTION: data ... 至 state 前
const d = new Function(dataCode + '\nreturn {MODS,CH,DEEP,LONG,LAYERS,CMDS,EVENTS,SCEN,LAB,ROUTES,ROUTENAME,INTERVALS};')();
const pack = {
  id: 'ops',
  title: '桌面运维全能学习导师',
  sub: '零基础到独当一面：Windows、AD 域与策略、网络、终端外设、排障方法论一条龙。',
  desc: '本课程把桌面运维拆成 7 个模块、30 章：先打地基（Windows / 网络 / 硬件），再攻 AD 域与策略主线，穿插 Helpdesk 高频场景与分层排障方法论。每章都配双轨对照、命令与预期输出、避坑清单和互动题，学完直接沉淀进案例库。',
  glyph: 'monitor',
  coverClass: 'cv-a',
  mods: d.MODS, ch: d.CH, deep: d.DEEP, long: d.LONG,
  layers: d.LAYERS, cmds: d.CMDS, events: d.EVENTS, scen: d.SCEN, lab: d.LAB,
  routes: d.ROUTES, routeNames: d.ROUTENAME, intervals: d.INTERVALS, glbase: {},
  depText: 'A2 网络基础 ──┬──> C4 DNS/DHCP ──> C2 认证 ──> C5 GPO ──> C6 GPO 场景\n              │                              ↑\nA1 Windows ───┘──> C1 域概念 ────────────────┘\n   C1 ──> C3 ADUC ──> D3 文件共享权限\n   A3 硬件 ──> B1/B2/B3（可并行插队）\n   F2 排障方法论 需要 C4 + A2 ／ F6 PowerShell 需要 C3',
  depNotes: [['允许插队：', 'B 模块、E 模块、D1 / D2，随时可提前学。'], ['禁止跳过：', 'A2 → C4 → C5 这条链。跳过 A2 或 C4 去学 GPO，是 AD 学习失败的主要模式。']]
};
const assetDir = path.join(OUT, 'app/src/main/assets');
fs.mkdirSync(assetDir, { recursive: true });
fs.writeFileSync(path.join(assetDir, 'course_ops.json'), JSON.stringify(pack));
console.log('assets/course_ops.json', fs.statSync(path.join(assetDir, 'course_ops.json')).size, 'bytes');

/* ---------- 2. 图标 ---------- */
const SW = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">';
const src = lines.join('\n');
const icons = {};
const i0 = src.indexOf('const ICONS={');
const j0 = src.indexOf('\n};', i0);
let m;
const re1 = /^\s*([a-zA-Z0-9_]+):\s*'((?:[^'\\]|\\.)*)',?$/gm;
while ((m = re1.exec(src.slice(i0, j0)))) icons[m[1]] = m[2];
const re2 = /ICONS\.([a-zA-Z0-9_]+)\s*=\s*(?:P\(\s*)?'((?:[^'\\]|\\.)*)'/g;
while ((m = re2.exec(src))) icons[m[1]] = m[2].trim().startsWith('<svg') ? m[2] : SW + m[2] + '</svg>';
const re3 = /^\s*(g_[a-z_]+|leaf|orbit|orbitFill|userFill|highlighter|database|route|queue|more|copy|check|checkTick|checklist|speaker|quote|sort|sparkle|bookOpen|upload|download|filter|locate|note|tune|pause|json|deco|crop|rotateCw|zoomIn|zoomOut|importCourse|resetArrow|fullscreenX|image|info|settings|calendar|aA|folder|wrench|grid|user|star|starFill|pen|share|play)\s*:\s*'((?:[^'\\]|\\.)*)',?\s*$/gm;
while ((m = re3.exec(src))) {
  if (icons[m[1]]) continue;
  icons[m[1]] = m[2].trim().startsWith('<svg') ? m[2] : SW + m[2] + '</svg>';
}

// 补充缺失的常用图标（与原文案风格一致的 24x24 线性图标）
const extra = {
  highlighter: SW + '<path d="M14 4.5l5.5 5.5L10 19.5H4.5V14z"/><path d="M9 19.5h11"/></svg>',
  database: SW + '<ellipse cx="12" cy="6" rx="7.5" ry="2.8"/><path d="M4.5 6v11c0 1.6 3.4 2.8 7.5 2.8s7.5-1.2 7.5-2.8V6"/><path d="M4.5 11.5c0 1.6 3.4 2.8 7.5 2.8s7.5-1.2 7.5-2.8"/></svg>',
  route: SW + '<circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/><path d="M9 6.5h5a3 3 0 0 1 0 6H10a3 3 0 0 0 0 6h5"/></svg>',
  queue: SW + '<path d="M4 7h11M4 12h11M4 17h7"/><path d="M18.5 14.5v6l4.5-3z"/></svg>',
  more: SW + '<circle cx="12" cy="5.5" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="18.5" r="1.4" fill="currentColor" stroke="none"/></svg>',
  copy: SW + '<rect x="8.5" y="8.5" width="11" height="11" rx="2.2"/><path d="M15.5 5.5H6.5a2 2 0 0 0-2 2v9"/></svg>',
  check: SW + '<path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  checkTick: SW + '<path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  checklist: SW + '<path d="M4 6.5l2 2 3-3.5M4 13l2 2 3-3.5M4 19.5l2 2 3-3.5"/><path d="M13 7h7M13 14h7M13 21h5"/></svg>',
  quote: '<svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M9.5 5.5C6.5 7 5 9.5 5 12.5V18.5h6v-6H8c0-2 .8-3.4 2.6-4.4zM19.5 5.5C16.5 7 15 9.5 15 12.5V18.5h6v-6h-3c0-2 .8-3.4 2.6-4.4z"/></svg>',
  sort: SW + '<path d="M7 4.5v15"/><path d="M3.5 8L7 4.5 10.5 8"/><path d="M17 19.5v-15"/><path d="M13.5 16l3.5 3.5 3.5-3.5"/></svg>',
  sparkle: SW + '<path d="M12 3l1.8 4.8L18.6 9.6l-4.8 1.8L12 16.2l-1.8-4.8L5.4 9.6l4.8-1.8z"/><path d="M18.5 15.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z"/></svg>',
  leaf: SW + '<path d="M5 19c0-7 5-12 14-12 0 9-5 12-11 12-2 0-3-1-3 0z"/><path d="M5 19c3-4 6-6 10-7"/></svg>',
  orbit: SW + '<circle cx="12" cy="12" r="4"/><ellipse cx="12" cy="12" rx="10" ry="4.6" transform="rotate(-25 12 12)"/></svg>',
  orbitFill: SW + '<circle cx="12" cy="12" r="4" fill="currentColor" stroke="none"/><ellipse cx="12" cy="12" rx="10" ry="4.6" transform="rotate(-25 12 12)"/></svg>',
  userFill: SW + '<circle cx="12" cy="8.2" r="3.8" fill="currentColor" stroke="none"/><path d="M4.8 20c1.4-3.4 4-5 7.2-5s5.8 1.6 7.2 5z" fill="currentColor" stroke="none"/></svg>',
  speaker: SW + '<path d="M5 9.5h3l4.5-3.5v12L8 14.5H5z"/><path d="M16 9a4.5 4.5 0 0 1 0 6"/><path d="M18.5 6.5a8 8 0 0 1 0 11"/></svg>',
  bookOpen: SW + '<path d="M12 6.5C10 4.8 7 4.3 4 4.8v12.4c3-.5 6 0 8 1.7 2-1.7 5-2.2 8-1.7V4.8c-3-.5-6 0-8 1.7z"/><path d="M12 6.5v12.4"/></svg>',
  upload: SW + '<path d="M12 19V8"/><path d="M8 11.5L12 7.5l4 4"/><path d="M5 19h14"/></svg>',
  download: SW + '<path d="M12 4.5V16"/><path d="M8 12.5l4 4 4-4"/><path d="M5 19.5h14"/></svg>',
  filter: SW + '<path d="M4 6h16l-6 7v5l-4 2v-7z"/></svg>',
  locate: SW + '<circle cx="12" cy="12" r="3.2"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3"/><circle cx="12" cy="12" r="8.5"/></svg>',
  note: SW + '<path d="M6 3.5h12v17H6z"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
  tune: SW + '<path d="M4 8h9M17 8h3M4 16h3M11 16h9"/><circle cx="15" cy="8" r="2.2"/><circle cx="8" cy="16" r="2.2"/></svg>',
  pause: SW + '<path d="M9.5 5.5v13M14.5 5.5v13"/></svg>',
  json: SW + '<path d="M8.5 4.5C6.5 4.5 6.5 7 6.5 8.5S5 10.5 4 10.5c1 0 2.5.5 2.5 2s0 4 2 4"/><path d="M15.5 4.5c2 0 2 2.5 2 4s1.5 2 2.5 2c-1 0-2.5.5-2.5 2s0 4-2 4"/></svg>',
  dot: SW + '<circle cx="12" cy="12" r="3" fill="currentColor" stroke="none"/></svg>',
  note: SW + '<path d="M6 3.5h12v17H6z"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
  bookmark: SW + '<path d="M6.5 3.5h11v17l-5.5-4-5.5 4z"/></svg>',
  clock: SW + '<circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3.2 2"/></svg>'
};
Object.keys(extra).forEach(k => { if (!icons[k]) icons[k] = extra[k]; });

/* ---------- 3. SVG -> VectorDrawable ---------- */
function num(s) { return String(Math.round(parseFloat(s) * 100) / 100); }
function rectPath(x, y, w, h, r) {
  r = Math.min(r || 0, w / 2, h / 2);
  if (!r) return `M${num(x)},${num(y)}h${num(w)}v${num(h)}h${-num(w)}z`;
  return `M${num(x + r)},${num(y)}h${num(w - 2 * r)}a${num(r)},${num(r)} 0 0 1 ${num(r)},${num(r)}v${num(h - 2 * r)}a${num(r)},${num(r)} 0 0 1 ${-num(r)},${num(r)}h${num(-(w - 2 * r))}a${num(r)},${num(r)} 0 0 1 ${-num(r)},${-num(r)}v${num(-(h - 2 * r))}a${num(r)},${num(r)} 0 0 1 ${num(r)},${-num(r)}z`;
}
function circlePath(cx, cy, rx, ry) {
  ry = ry == null ? rx : ry;
  return `M${num(cx - rx)},${num(cy)}a${num(rx)},${num(ry)} 0 1 0 ${num(2 * rx)},0a${num(rx)},${num(ry)} 0 1 0 ${num(-2 * rx)},0z`;
}
function toVector(svg) {
  const vb = (svg.match(/viewBox="([^"]+)"/) || ['', '0 0 24 24'])[1].split(/\s+/).map(Number);
  const rootFill = /fill="currentColor"/.test(svg);
  const rootStroke = /stroke="currentColor"/.test(svg);
  const sw = (svg.match(/stroke-width="([\d.]+)"/) || [, '1.8'])[1];
  let body = svg.replace(/^[\s\S]*?<svg[^>]*>/, '').replace(/<\/svg>[\s\S]*$/, '');
  const parts = [];
  const tagRe = /<(path|circle|rect|ellipse)\b([^>]*)\/?>/g;
  let t;
  while ((t = tagRe.exec(body))) {
    const tag = t[1], attrs = t[2];
    const a = {};
    attrs.replace(/([a-zA-Z-]+)="([^"]*)"/g, (_, k, v) => { a[k] = v; return ''; });
    let d2 = '';
    if (tag === 'path') d2 = a.d || '';
    else if (tag === 'circle') d2 = circlePath(+a.cx, +a.cy, +a.r);
    else if (tag === 'ellipse') d2 = circlePath(+a.cx, +a.cy, +a.rx, +a.ry);
    else if (tag === 'rect') d2 = rectPath(+a.x, +a.y, +a.width, +a.height, +(a.rx || 0));
    if (!d2) continue;
    const fillAttr = a.fill || (rootFill ? 'currentColor' : 'none');
    const strokeAttr = a.stroke || (fillAttr === 'currentColor' ? 'none' : (rootStroke ? 'currentColor' : 'none'));
    const fillOpaque = fillAttr !== 'none' && fillAttr !== '';
    let xml = `        <path\n            android:pathData="${d2.replace(/"/g, '')}"`;
    xml += `\n            android:fillColor="${fillOpaque ? '#FF000000' : '#00000000'}"`;
    if (strokeAttr !== 'none') {
      xml += `\n            android:strokeColor="#FF000000"\n            android:strokeWidth="${a['stroke-width'] || sw}"`;
      xml += `\n            android:strokeLineCap="round"\n            android:strokeLineJoin="round"`;
    }
    if (a.transform) xml += `\n            android:trimPathStart="0"`;
    xml += ' />';
    parts.push(xml);
  }
  if (!parts.length) return null;
  return `<?xml version="1.0" encoding="utf-8"?>\n<!-- auto-generated from source app icon set -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp" android:height="24dp"\n    android:viewportWidth="${vb[2]}" android:viewportHeight="${vb[3]}">\n${parts.join('\n')}\n</vector>\n`;
}

const dr = path.join(OUT, 'app/src/main/res/drawable');
fs.mkdirSync(dr, { recursive: true });
let made = 0, missed = [];
Object.keys(icons).forEach(name => {
  const xml = toVector(icons[name]);
  if (xml) { fs.writeFileSync(path.join(dr, 'ic_' + name.toLowerCase().replace(/[^a-z0-9_]/g, '_') + '.xml'), xml); made++; }
  else missed.push(name);
});
console.log('drawables:', made, 'missed:', missed.join(' '));
fs.writeFileSync(path.join(OUT, 'tools/icons_raw.json'), JSON.stringify(icons, null, 1));
