const fs = require('fs');
const src = fs.readFileSync('/root/userdata/attachment/桌面运维_星球主题版261113_v6.html', 'utf8');
const SW = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">';
const out = {};

// 1) main ICONS literal (single-quoted values)
const i0 = src.indexOf('const ICONS={');
const j0 = src.indexOf('\n};', i0);
const body = src.slice(i0, j0);
const re1 = /^\s*([a-zA-Z0-9_]+):\s*'((?:[^'\\]|\\.)*)',?$/gm;
let m;
while ((m = re1.exec(body))) out[m[1]] = m[2];

// 2) ICONS.name = '<svg ...>'  |  ICONS.name = P('...')
const re2 = /ICONS\.([a-zA-Z0-9_]+)\s*=\s*(?:P\()?\s*'((?:[^'\\]|\\.)*)'/g;
while ((m = re2.exec(src))) {
  const v = m[2];
  out[m[1]] = v.trim().startsWith('<svg') ? v : (SW + v + '</svg>');
}

// 3) glyph pool objects:  g_xxx:'<path .../>'
const re3 = /^\s*(g_[a-z_]+|leaf|orbit|orbitFill|userFill|highlighter|database|route|queue|more|copy|check|checkTick|checklist|dot|dotst|speaker|quote|sort|sparkle|bookOpen|upload|download|filter|locate|note|tune|pause|play)\s*:\s*'((?:[^'\\]|\\.)*)',?\s*$/gm;
while ((m = re3.exec(src))) {
  if (out[m[1]]) continue;
  const v = m[2];
  out[m[1]] = v.trim().startsWith('<svg') ? v : (SW + v + '</svg>');
}

fs.writeFileSync('icons_raw.json', JSON.stringify(out, null, 1));
console.log('icons:', Object.keys(out).length);
console.log(Object.keys(out).sort().join(' '));
