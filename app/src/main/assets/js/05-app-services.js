/* Extracted original app layer; classic-script scope and order are intentional. */
/* SECTION: io bindings */
let KNOW_TOTAL=Object.keys(DEEP).reduce(function(n,k){return n+(DEEP[k].deep||[]).length;},0);
function bindIoOnce(){
 if(window.__ioB)return;window.__ioB=1;
 const on=function(id,fn){const e=document.getElementById(id);if(e)e.onclick=fn;};
 on('expBackup',function(){
  openExport({kind:'json',title:'全量备份 · JSON 存档',filename:'桌面运维-学习存档-'+today()+'.json',mime:'application/json',text:buildBackup()});
 });
 on('expManual',function(){
  openExport({kind:'md',title:'学习手册 · Markdown',filename:'桌面运维-学习手册-'+today()+'.md',mime:'text/markdown',text:manualMd()});
 });
 on('expPrint',function(){
  openExport({kind:'print',title:'学习手册 · 可打印版',filename:'桌面运维-学习手册-打印版-'+today()+'.html',mime:'text/html',html:buildPrintHtml()});
 });
 on('expMarks',function(){
  if(!(S.marks||[]).length){toast('还没有划线可导出');return;}
  openExport({kind:'txt',title:'划线与备注摘录',filename:'桌面运维-划线摘录-'+today()+'.txt',mime:'text/plain',text:marksText(S.marks)});
 });
 on('expCases',function(){
  if(!(S.cases||[]).length){toast('案例库还是空的');return;}
  openExport({kind:'txt',title:'故障案例库 · TXT',filename:'桌面运维-故障案例库-'+today()+'.txt',mime:'text/plain',text:casesText()});
 });
 on('expCasesCsv',function(){
  if(!(S.cases||[]).length){toast('案例库还是空的');return;}
  openExport({kind:'csv',title:'故障案例库 · CSV',filename:'桌面运维-故障案例库-'+today()+'.csv',mime:'text/csv',text:casesCsv()});
 });
 on('expGloss',function(){
  openExport({kind:'csv',title:'术语手册 · CSV',filename:'桌面运维-术语手册-'+today()+'.csv',mime:'text/csv',text:glossCsv()});
 });
 on('pickFile',function(){const f=document.getElementById('impFile');if(f)f.click();});
}
let ioDetail=null,ioFormat='json';
/* =====================================================================
   v7 · 单主题色引擎 · 星球风格 · 统一数据格式 · 动效
   （这一段整体替换了旧的 render 路由与上一轮的临时覆盖函数）
   ===================================================================== */
const $=function(id){return document.getElementById(id);};
function isPlanet(){return ((S.ui&&S.ui.style)||'planet')==='planet';}

/* ---------- 图标补全（上一轮引用了未定义的图标，这里一并补上） ---------- */
(function(){
 const s=function(d){return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+d+'</svg>';};
 const f=function(d){return '<svg viewBox="0 0 24 24" fill="currentColor" stroke="none">'+d+'</svg>';};
 ICONS.orbit=s('<circle cx="12" cy="12" r="5"/><ellipse cx="12" cy="12" rx="10.5" ry="4" transform="rotate(-24 12 12)"/>');
 ICONS.orbitFill='<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="6.2" fill="currentColor"/><ellipse cx="12" cy="12" rx="10.6" ry="4.1" transform="rotate(-24 12 12)" fill="none" stroke="currentColor" stroke-width="1.7"/></svg>';
 ICONS.userFill=f('<circle cx="12" cy="8" r="4.3"/><path d="M3.8 20.5c.3-4 3.7-6.3 8.2-6.3s7.9 2.3 8.2 6.3z"/>');
 ICONS.leaf=s('<path d="M5 19c0-8 5-13 14-14 0 9-5 14-13 14"/><path d="M5 19c2-4 5-7 9-9"/>');
 ICONS.database=s('<ellipse cx="12" cy="6" rx="7.5" ry="3"/><path d="M4.5 6v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3V6"/><path d="M4.5 12v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3v-6"/>');
 ICONS['book-open']=s('<path d="M12 6.5C10 5 7 4.5 3.5 5v13c3.5-.5 6.5 0 8.5 1.5 2-1.5 5-2 8.5-1.5V5C17 4.5 14 5 12 6.5z"/><path d="M12 6.5v13"/>');
 ICONS.highlighter=s('<path d="M13.5 5.5l5 5-8 8H5.5v-5z"/><path d="M4 21h9"/>');
 ICONS['chart-no-axes-combined']=ICONS.chart;
 ICONS.pause=f('<rect x="6.5" y="5" width="3.8" height="14" rx="1.2"/><rect x="13.7" y="5" width="3.8" height="14" rx="1.2"/>');
 ICONS.more=f('<circle cx="12" cy="5.5" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="12" cy="18.5" r="1.8"/>');
 ICONS.queue=s('<path d="M4 6.5h11M4 12h11M4 17.5h6"/><path d="M16.5 14.5v6l5-3z" fill="currentColor"/>');
 ICONS.sort=s('<path d="M4 6.5h9M4 12h6M4 17.5h3"/><path d="M17 5v14M14 16l3 3 3-3"/>');
 ICONS.bell=s('<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>');
 ICONS.quote=f('<path d="M5 18c-1.1 0-2-.9-2-2 0-2.8 1.6-5.1 4.4-6l.6 1.3c-1.4.6-2.1 1.5-2.3 2.7H7c1.1 0 2 .9 2 2s-.9 2-2 2zm9 0c-1.1 0-2-.9-2-2 0-2.8 1.6-5.1 4.4-6l.6 1.3c-1.4.6-2.1 1.5-2.3 2.7H16c1.1 0 2 .9 2 2s-.9 2-2 2z"/>');
 ICONS.sparkle=s('<path d="M12 3c.6 4.6 2.4 6.4 7 7-4.6.6-6.4 2.4-7 7-.6-4.6-2.4-6.4-7-7 4.6-.6 6.4-2.4 7-7z"/>');
 ICONS.json=s('<path d="M8.5 4C6.5 4 6 5 6 6.5v2C6 10 5 11.5 3.5 12 5 12.5 6 14 6 15.5v2C6 19 6.5 20 8.5 20"/><path d="M15.5 4c2 0 2.5 1 2.5 2.5v2c0 1.5 1 3 2.5 3.5-1.5.5-2.5 2-2.5 3.5v2c0 1.5-.5 2.5-2.5 2.5"/>');
 ICONS.download=s('<path d="M12 4v11"/><path d="M7.5 10.5L12 15l4.5-4.5"/><path d="M5 19.5h14"/>');
 ICONS.upload=s('<path d="M12 16V5"/><path d="M7.5 9.5L12 5l4.5 4.5"/><path d="M5 19.5h14"/>');
 ICONS.check=s('<path d="M5 12.5l4.5 4.5L19 7.5"/>');
 ICONS.plus=s('<path d="M12 5v14M5 12h14"/>');
 ICONS.route=s('<circle cx="6" cy="18" r="2.2"/><circle cx="18" cy="6" r="2.2"/><path d="M8 18h7a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h7"/>');
 ICONS.note=s('<path d="M6 4h12v16H6z"/><path d="M9 9h6M9 13h6M9 17h3"/>');
})();

/* ---------- 单主题色引擎 ---------- */
const DEFAULT_ACCENT=4;
const ACCENT_PALETTE=[
 ['珊瑚红','#f5304a'],['海洋蓝','#1a6dff'],['电光紫','#7a3cff'],
 ['活力橙','#ff7a00'],['翡翠绿','#00b56a'],['湖水青','#00a6c7'],
 ['玫瑰粉','#f23d97'],['柠檬黄','#f5b800'],['靛蓝','#3b4bff']
];
function hex2rgb(x){x=String(x||'').replace('#','');if(x.length===3)x=x.replace(/(.)/g,'$1$1');const n=parseInt(x,16);return [(n>>16)&255,(n>>8)&255,n&255];}
function rgb2hex(a){return '#'+a.map(function(v){return Math.max(0,Math.min(255,Math.round(v))).toString(16).padStart(2,'0');}).join('');}
function mixHex(a,b,t){const x=hex2rgb(a),y=hex2rgb(b);return rgb2hex([x[0]+(y[0]-x[0])*t,x[1]+(y[1]-x[1])*t,x[2]+(y[2]-x[2])*t]);}
function lumOf(hex){const c=hex2rgb(hex).map(function(v){v/=255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4);});return .2126*c[0]+.7152*c[1]+.0722*c[2];}
function accentHex(){
 const u=S.ui||{};
 if(u.accent===-1&&/^#[0-9a-f]{6}$/i.test(u.accentCustom||''))return u.accentCustom;
 return (ACCENT_PALETTE[u.accent==null?DEFAULT_ACCENT:u.accent]||ACCENT_PALETTE[DEFAULT_ACCENT])[1];
}
function applyAccent(c){
 const root=document.documentElement,dark=S.theme==='dark';
 const base=/^#[0-9a-f]{6}$/i.test(c||'')?c:ACCENT_PALETTE[0][1];
 const use=dark?mixHex(base,'#ffffff',.14):base;
 const onPri=(1.05/(lumOf(use)+.05))<2.2?'#1b1d22':'#ffffff';
 root.style.setProperty('--pri',use);
 root.style.setProperty('--on-pri',onPri);
 const meta=document.querySelector('meta[name=theme-color]');if(meta)meta.setAttribute('content',S.theme==='dark'?'#0e1011':'#ffffff');
}
function applyStyleMode(){document.documentElement.setAttribute('data-style',(S.ui&&S.ui.style)||'planet');}
function applyTheme(){
 const root=document.documentElement;
 root.setAttribute('data-theme',S.theme==='dark'?'dark':'light');
 applyAccent(accentHex());applyStyleMode();
 document.querySelectorAll('[data-theme-toggle]').forEach(function(b){b.innerHTML=ic(S.theme==='dark'?'sun':'moon');});
 save();
}
function themeFlash(){const r=document.documentElement;r.classList.add('theming');clearTimeout(themeFlash.t);themeFlash.t=setTimeout(function(){r.classList.remove('theming');},520);}

/* ---------- 小组件 ---------- */
function ringBtn(p,icon,cls){
 const b=h('button','rbtn '+(cls||'')),C=2*Math.PI*21;
 p=Math.max(0,Math.min(1,p||0));
 b.innerHTML='<svg class="rg" viewBox="0 0 48 48"><circle class="rg-bg" cx="24" cy="24" r="21"/><circle class="rg-fg" cx="24" cy="24" r="21" style="--C:'+C.toFixed(2)+';stroke-dasharray:'+C.toFixed(2)+';stroke-dashoffset:'+(C*(1-p)).toFixed(2)+'"/></svg><span class="ri">'+ic(icon)+'</span>';
 return b;
}
function countUp(el,to,suffix){
 to=Number(to)||0;suffix=suffix||'';
 const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion:reduce)').matches;
 if(!to||reduce){el.textContent=to+suffix;return;}
 el.textContent='0'+suffix;
 const ms=850;let t0=null;
 requestAnimationFrame(function f(t){if(t0==null)t0=t;const p=Math.min(1,(t-t0)/ms),e=1-Math.pow(1-p,3);el.textContent=Math.round(to*e)+suffix;if(p<1)requestAnimationFrame(f);});
}
function iconBtn(icon,label,fn){const b=h('button','icbtn');b.innerHTML=ic(icon);b.setAttribute('aria-label',label);b.title=label;b.onclick=fn;return b;}
/* 点击后才展开的 JSON 示例：小图标 + 折叠框 */
function sampleToggle(getText,note){
 const btn=h('button','smpbtn');btn.innerHTML=ic('json');btn.setAttribute('aria-label','查看 JSON 示例');btn.title='查看 JSON 示例';
 const box=h('div','smpbox'),inner=h('div','smpin'),pre=h('pre',null,'');
 inner.appendChild(pre);if(note)inner.appendChild(h('p','smpnote',note));box.appendChild(inner);
 btn.onclick=function(e){e.stopPropagation();const on=!box.classList.contains('on');if(on)pre.textContent=getText();box.classList.toggle('on',on);btn.classList.toggle('on',on);};
 return {btn:btn,box:box};
}
/* 与「搜索结果」同款的条目行：极简 = 圆角卡片行；星球 = 播客列表行 */
function listRow(o){
 if(isPlanet()){
  const el=h('div','ep sm'+(o.static?' static':'')),th=h('div','ep-th');th.innerHTML=ic(o.icon);el.appendChild(th);
  const mn=h('div','ep-main');mn.appendChild(h('div','ep-t',o.title));if(o.desc)mn.appendChild(h('div','ep-s',o.desc));el.appendChild(mn);
  if(!o.static){const a=h('div','ep-act'),c=h('span','morebtn');c.innerHTML=ic('chev');a.appendChild(c);el.appendChild(a);el.onclick=o.onClick;}
  return el;
 }
 const el=h('div','chrow io-row'+(o.static?' static':'')),cid=h('span','cid ico');cid.innerHTML=ic(o.icon);el.appendChild(cid);
 const ci=h('div','ci');ci.appendChild(h('b',null,o.title));if(o.desc)ci.appendChild(h('p',null,o.desc));el.appendChild(ci);
 if(!o.static){const g=h('span','chev');g.innerHTML=ic('chev');el.appendChild(g);el.onclick=o.onClick;}
 return el;
}

/* ---------- 课程数据同步（见 course.js 段） ---------- */
function chapterMarks(id){return (S.marks||[]).filter(function(m){return m.ch===id;}).length;}
function setChapterStatus(id,val){
 if(val)S.status[id]=val;else delete S.status[id];
 if(val&&!S.learn[id])S.learn[id]={at:today(),done:[]};
 save();
}

/* ---------- 路由 ---------- */
const IO_ENTRIES=[
 {id:'all',title:'整门课程 · 完整档案',desc:'课程资料、学习手册、划线、进度、案例、术语与个人设置，一次带走',icon:'database',name:'完整档案'},
 {id:'course',title:'课程资料',desc:'课程名称、简介、封面与章节目录（含自建、导入的课程）',icon:'book',name:'课程资料'},
 {id:'handbook',title:'学习手册',desc:'全部章节的概念、实操、指南与互动题',icon:'book-open',name:'学习手册'},
 {id:'marks',title:'划线与备注',desc:'划线原文、线型颜色、标签、备注与时间',icon:'highlighter',name:'划线与备注'},
 {id:'progress',title:'学习进度',desc:'章节过关状态、学习日期、当前路线与学习时长',icon:'chart-no-axes-combined',name:'学习进度'},
 {id:'cases',title:'案例与术语',desc:'故障案例库（五栏）与自填术语手册',icon:'folder',name:'案例与术语'}
];
const IO_FORMATS=[['json','JSON','完整结构'],['md','Markdown','可读文档'],['csv','CSV','一行一条']];
let cdFilter='all',cdDesc=false,recOff=0,lastView=null,navTimer=null;
let impState={text:'',res:null,mode:'merge',fname:''};

function navBack(){
 if(view==='my-marks'&&selectedMarkCourse){selectedMarkCourse='';render();return;}
 if(view==='io'&&ioDetail){ioDetail=null;render();return;}
 const p=navStack.pop();view=p||'mine';render();
}
function pageTitle(){
 if(view==='today'){const c=(S.pin&&chById(S.pin))||cur();return c?c.id+' '+c.t:'';}
 if(view==='course')return coMeta(curCourse()).title;
 if(view==='my-marks')return selectedMarkCourse?'课程划线':'我的划线';
 if(view==='io'){
  if(ioDetail==='import')return '导入档案';
  const e=IO_ENTRIES.find(function(x){return x.id===ioDetail;});
  return e?e.title:'数据';
 }
 return TITLES[view]||'';
}
function renderAppbar(){
 const ab=$('appbar');if(!ab)return;
 const onTab=!!TABPAGES[view];
 ab.classList.toggle('on',!onTab);
 document.body.classList.toggle('has-ab',!onTab);
 ab.classList.toggle('ghost',isPlanet()&&view==='course');
 $('abTitle').textContent=pageTitle();
 $('backBtn').classList.toggle('hide',onTab);
 $('abEdit').classList.toggle('hide',view!=='course');
 const ac=$('abActions');ac.textContent='';
 if(view==='course-manage'){
  ac.appendChild(iconBtn('plus','新增课程',function(){openNewCourseEditor();}));
  ac.appendChild(iconBtn('download','导入课程',function(){openCourseImport();}));
 }
 syncScrollState();
}
function syncScrollState(){
 const ab=$('appbar');if(!ab)return;
 const th=(isPlanet()&&view==='course')?150:6;
 ab.classList.toggle('scrolled',(window.scrollY||0)>th);
}
function renderTabbar(){
 const box=$('tbin');if(!box)return;box.textContent='';
 const planet=isPlanet();
 Object.keys(TABPAGES).forEach(function(k){
  const sub=['calendar','my-marks','course-manage','settings','shortcuts','player','io'].includes(view),orig=TABPAGES[navStack[0]]?navStack[0]:'profile';
   const on=(view===k||(sub&&k===orig));
  const b=document.createElement('button');b.className='tbi'+(on?' on':'');
  const ico=planet?(k==='mine'?(on?'orbitFill':'orbit'):(on?'userFill':'user')):TABICON[k];
  b.innerHTML=ic(ico)+'<span>'+TABPAGES[k]+'</span>';
  b.onclick=function(){nav(k);};box.appendChild(b);
 });
}
function renderMini(){
 const m=$('mini');if(!m)return;
 const inner=$('miniInner');
 const planet=isPlanet();
 if(view==='today'||(!planet&&view==='course')||miniGone){m.classList.remove('on');miniShown=false;return;}
 const c=(S.pin&&chById(S.pin))||cur();
 if(!c){m.classList.remove('on');miniShown=false;return;}
 const q=seq(),i=Math.min(S.idx,q.length-1),pr=courseProgress();
 inner.innerHTML='';
 const ops=curCourse();
 inner.appendChild(coverEl(ops,'sm'));
 const mi=h('div','mi');mi.appendChild(h('b',null,c.id+' '+c.t));
 mi.appendChild(h('span',null,planet?('第 '+(i+1)+' / '+q.length+' 站 · 已过关 '+pr.done+'/'+pr.total):('第 '+(i+1)+' / '+q.length+' 站 · '+ROUTENAME[S.route])));
 inner.appendChild(mi);
 if(planet){
  const rb=ringBtn(pr.total?pr.done/pr.total:0,'play','mini-ring');rb.setAttribute('aria-label','继续学习');
  inner.appendChild(rb);
  const qb=h('button','qbtn');qb.innerHTML=ic('queue');qb.setAttribute('aria-label','课程章节');
  qb.onclick=function(e){e.stopPropagation();cdSeg='chs';nav('course');};
  qb.addEventListener('pointerdown',function(e){e.stopPropagation();});
  inner.appendChild(qb);
 }else{
  const pb=h('span','pbtn');pb.innerHTML=ic('play');inner.appendChild(pb);
 }
 m.classList.add('on');
 if(!miniShown){
  miniShown=true;
  inner.style.transform='';inner.style.opacity='';inner.style.transition='';
  inner.classList.remove('enter');void inner.offsetWidth;inner.classList.add('enter');
 }
}

/* ---------- 课程 Tab ---------- */
function ensureSearchUi(){
 const inp=$('mineSearch');if(!inp)return;
 const clr=$('mineClear');if(clr)clr.classList.toggle('on',!!inp.value);
}
function courseCard(co){
 const m=coMeta(co),pr=progOf(co),started=pr.started;
 const starred=S.ui.star&&S.ui.star[co.id];
 const el=h('div','ccard');
 el.appendChild(coverEl(co,''));
 const ci=h('div','ci');
 ci.appendChild(h('b',null,m.title));
 ci.appendChild(h('p','cs',m.sub));
 if(started){const bar=h('div','bar'),fi=h('i');fi.style.width=(pr.total?Math.round(pr.done/pr.total*100):0)+'%';bar.appendChild(fi);ci.appendChild(bar);}
 ci.appendChild(h('div','cmeta',(started?'已过关 '+pr.done+' / '+pr.total+' 章 · ':'')+pr.mods+' 模块 · '+pr.chs+' 章'));
 el.appendChild(ci);
 const st=h('button','star'+(starred?' on':''));
 st.innerHTML=ic(starred?'starFill':'star');st.setAttribute('aria-label',starred?'取消星标':'星标课程');
 st.onclick=function(e){e.stopPropagation();S.ui.star=S.ui.star||{};if(S.ui.star[co.id])delete S.ui.star[co.id];else S.ui.star[co.id]=1;save();renderMine();};
 el.appendChild(st);
 el.onclick=function(){activeCourse=co.id;cdSeg='chs';nav('course');};
 return el;
}
function scard(icon,text,cls,fn,badge){
 const b=h('button','scard'+(cls?' '+cls:''));b.innerHTML=ic(icon);b.appendChild(h('span',null,text));
 if(badge)b.appendChild(h('i','nb',String(badge)));b.onclick=fn;return b;
}
function playBtn(done,fn,label){
 const b=h('button','pbt'+(done?' done':''));b.innerHTML=ic(done?'check':'play');b.setAttribute('aria-label',label||'开始');
 b.onclick=function(e){e.stopPropagation();fn();};return b;
}
function epCourse(co){
 const m=coMeta(co),pr=progOf(co),started=pr.started;
 const el=h('div','ep');
 const th=h('div','ep-th');th.appendChild(coverEl(co,''));el.appendChild(th);
 const mn=h('div','ep-main'),k=h('div','ep-k');
 k.appendChild(h('span',null,pr.mods+' 模块 · '+pr.chs+' 章'));
 const starred=S.ui.star&&S.ui.star[co.id];
 const sb=h('button','stbtn'+(starred?' on':''));sb.innerHTML=ic(starred?'starFill':'star');sb.setAttribute('aria-label','星标');
 sb.onclick=function(e){e.stopPropagation();S.ui.star=S.ui.star||{};if(S.ui.star[co.id])delete S.ui.star[co.id];else S.ui.star[co.id]=1;save();renderMine();};
 k.appendChild(sb);mn.appendChild(k);
 mn.appendChild(h('div','ep-t',m.title));
 if(m.sub){const q=h('div','ep-q');q.innerHTML=ic('quote');q.appendChild(h('span',null,m.sub));mn.appendChild(q);}
 if(started){const mt=h('div','ep-meta'),a=h('span','mi');a.innerHTML=ic('check');a.appendChild(document.createTextNode('已过关 '+pr.done+' / '+pr.total+' 章'));mt.appendChild(a);mn.appendChild(mt);}
 el.appendChild(mn);
 const act=h('div','ep-act');
 if(started){const rb=ringBtn(pr.total?pr.done/pr.total:0,'play');rb.onclick=function(e){e.stopPropagation();activeCourse=co.id;nav('today');};act.appendChild(rb);}
 else act.appendChild(playBtn(false,function(){activeCourse=co.id;nav('today');},'开始学习'));
 el.appendChild(act);
 el.onclick=function(){activeCourse=co.id;cdSeg='chs';nav('course');};
 return el;
}
function epChapter(c,mode){
 const curId=(S.pin&&chById(S.pin))?S.pin:cur().id;
 const st=S.status[c.id],isCur=c.id===curId,ops=curCourse();
 const el=h('div','ep'+(mode==='disc'?'':' sm')+(isCur&&mode!=='disc'?' cur':''));
 const th=h('div','ep-th');th.appendChild(coverEl(ops,''));el.appendChild(th);
 const mn=h('div','ep-main');
 if(mode==='disc'){const k=h('div','ep-k');k.appendChild(h('span',null,coMeta(ops).title));mn.appendChild(k);}
 mn.appendChild(h('div','ep-t',c.id+' '+c.t));
 if(mode==='disc'){const q=h('div','ep-q');q.innerHTML=ic('quote');q.appendChild(h('span',null,c.s));mn.appendChild(q);}
 else{
  mn.appendChild(h('div','ep-s',c.s));
  const mt=h('div','ep-meta'),a=h('span','mi');
  if(st==='green'){const d=h('span','dotst');d.innerHTML=ic('check');a.appendChild(d);a.appendChild(document.createTextNode('已过关'));}
  else a.appendChild(document.createTextNode(st==='yellow'?'半过关':(S.learn[c.id]?'学习中':'未开始')));
  mt.appendChild(a);
  const mod=MODS.find(function(x){return x.k===c.m;});
  mt.appendChild(h('span','mi',mod?mod.n.replace('模块 '+mod.k+' · ',''):'模块 '+c.m));
  const nm=chapterMarks(c.id);
  if(nm){const x=h('span','mi');x.innerHTML=ic('highlighter');x.appendChild(document.createTextNode(String(nm)));mt.appendChild(x);}
  mn.appendChild(mt);
 }
 el.appendChild(mn);
 const act=h('div','ep-act');
 if(mode!=='disc'&&isCur&&st!=='green'){const pr=courseProgress(),rb=ringBtn(pr.total?pr.done/pr.total:0,'play');rb.onclick=function(e){e.stopPropagation();openChapter(c.id);};act.appendChild(rb);}
 else act.appendChild(playBtn(st==='green'&&mode!=='disc',function(){openChapter(c.id);},'学习 '+c.id));
 if(mode!=='disc'){const mb=h('button','morebtn');mb.innerHTML=ic('more');mb.setAttribute('aria-label','更多');mb.onclick=function(e){e.stopPropagation();openChapterMenu(c);};act.appendChild(mb);}
 el.appendChild(act);
 el.onclick=function(){openChapter(c.id);};
 return el;
}
function openChapterMenu(c){
 const body=h('div');
 const items=[['play','开始学习本章',function(){closeSheet();openChapter(c.id);}],
  ['check','标记为已过关',function(){setChapterStatus(c.id,'green');closeSheet();render();toast(c.id+' 已过关');}],
  ['refresh','标记为半过关',function(){setChapterStatus(c.id,'yellow');closeSheet();render();toast(c.id+' 标记为半过关');}],
  ['close','标记为未过关',function(){setChapterStatus(c.id,'red');closeSheet();render();toast(c.id+' 标记为未过关');}],
  ['trash','清除状态',function(){setChapterStatus(c.id,'');closeSheet();render();toast('已清除状态');}]];
 items.forEach(function(it){const r=h('button','pf-row');const p=h('span','pi');p.innerHTML=ic(it[0]);r.appendChild(p);r.appendChild(h('span','tx',it[1]));r.onclick=it[2];body.appendChild(r);});
 openSheet(c.id+' '+c.t,body);
}
function renderMineMinSearch(box,kw){
 const r=searchAll(kw);
 const n=r.courses.length+r.chapters.length+r.know.length;
 if(!n){box.appendChild(h('div','srempty','没有找到「'+mineKw.trim()+'」，试试 DNS、GPO、打印机、4625'));return;}
 if(r.courses.length){box.appendChild(h('div','srcnt','课程 · '+r.courses.length));r.courses.forEach(function(co){box.appendChild(courseCard(co));});}
 if(r.chapters.length){box.appendChild(h('div','srcnt','章节 · '+r.chapters.length));r.chapters.slice(0,30).forEach(function(c){box.appendChild(chRow(c));});}
 if(r.know.length){
  box.appendChild(h('div','srcnt','知识点 · '+r.know.length+(r.know.length>=30?'+':'')));
  r.know.forEach(function(it){
   const c=it[0],row=it[1],el=h('div','chrow');
   el.appendChild(h('span','cid',c.id));
   const ci=h('div','ci');ci.appendChild(h('b',null,row[0]));ci.appendChild(h('p',null,row[1]));el.appendChild(ci);
   const g=h('span','chev');g.innerHTML=ic('chev');el.appendChild(g);
   el.onclick=function(){openChapter(c.id);};box.appendChild(el);
  });
 }
}
function renderMine(){
 const box=$('mineBox');if(!box)return;
 const sh=$('mineShort');if(sh)sh.textContent='';
 box.textContent='';ensureSearchUi();
 const kw=mineKw.trim().toLowerCase();
 if(!isPlanet()){
  if(!kw){
   const list=COURSES.slice().sort(function(a,b){return (S.ui.star&&S.ui.star[b.id]?1:0)-(S.ui.star&&S.ui.star[a.id]?1:0);});
   list.forEach(function(co){box.appendChild(courseCard(co));});
  }else renderMineMinSearch(box,kw);
  box.className='stagger';return;
 }
 box.className='';
 if(!kw){
  const due=dueList().length,started=Object.keys(S.learn||{}).length>0;
  sh.classList.add('sc-row');sh.classList.toggle('sc-few',getSC().length<=3);getSC().forEach(function(it){sh.appendChild(scTile(it));});
  const ops=curCourse();
  const pool=seq().map(chById).filter(function(c){return c&&S.status[c.id]!=='green';});
  box.appendChild(h('div','pl-sec','为你推荐'));
  const rec=h('div','stagger');box.appendChild(rec);
  if(!pool.length)rec.appendChild(h('div','pl-empty','全部章节都已过关，去「复习队列」巩固一下吧。'));
  else{
   const n=Math.min(3,pool.length);
   for(let i=0;i<n;i++)rec.appendChild(epChapter(pool[(recOff+i)%pool.length],'disc'));
   if(pool.length>3){
    const mw=h('div','pl-more'),mb=h('button');mb.innerHTML=ic('refresh');mb.appendChild(document.createTextNode('换一换'));
    mb.onclick=function(){recOff=(recOff+3)%pool.length;mb.querySelector('svg').classList.add('spin');
     rec.textContent='';for(let i=0;i<n;i++){const e=epChapter(pool[(recOff+i)%pool.length],'disc');e.style.animation='itemIn .42s var(--snap) both';e.style.animationDelay=(i*60)+'ms';rec.appendChild(e);}
     setTimeout(function(){const s=mb.querySelector('svg');if(s)s.classList.remove('spin');},620);};
    mw.appendChild(mb);box.appendChild(mw);
   }
  }
  box.appendChild(h('div','pl-sec','我的课程'));
  const lst=h('div','stagger');box.appendChild(lst);
  COURSES.slice().sort(function(a,b){return (S.ui.star&&S.ui.star[b.id]?1:0)-(S.ui.star&&S.ui.star[a.id]?1:0);}).forEach(function(co){lst.appendChild(epCourse(co));});
  return;
 }
 const r=searchAll(kw),n=r.courses.length+r.chapters.length+r.know.length;
 if(!n){box.appendChild(h('div','pl-empty','没有找到「'+mineKw.trim()+'」，试试 DNS、GPO、打印机、4625'));return;}
 const ops=curCourse();
 if(r.courses.length){const t=h('div','pl-sec','课程');t.appendChild(h('small',null,String(r.courses.length)));box.appendChild(t);const g=h('div','swap');r.courses.forEach(function(co){g.appendChild(epCourse(co));});box.appendChild(g);}
 if(r.chapters.length){const t=h('div','pl-sec','章节');t.appendChild(h('small',null,String(r.chapters.length)));box.appendChild(t);const g=h('div','swap');r.chapters.slice(0,30).forEach(function(c){g.appendChild(epChapter(c,'list'));});box.appendChild(g);}
 if(r.know.length){
  const t=h('div','pl-sec','知识点');t.appendChild(h('small',null,r.know.length+(r.know.length>=30?'+':'')));box.appendChild(t);
  const g=h('div','swap');
  r.know.forEach(function(it){
   const c=it[0],row=it[1],el=h('div','ep sm'),th=h('div','ep-th');th.innerHTML=ic('book');el.appendChild(th);
   const mn=h('div','ep-main');const k=h('div','ep-k');k.appendChild(h('span',null,c.id+' '+c.t));mn.appendChild(k);mn.appendChild(h('div','ep-t',row[0]));mn.appendChild(h('div','ep-s',row[1]));el.appendChild(mn);
   el.onclick=function(){openChapter(c.id);};g.appendChild(el);
  });
  box.appendChild(g);
 }
}

/* ---------- 课程详情 ---------- */
function toolTile(v,label,ico,badge){
 const t=h('div','tile'),c=h('span','tic');c.innerHTML=ic(ico);t.appendChild(c);
 t.appendChild(h('span',null,label));
 if(badge)t.appendChild(h('i','bdg',String(badge)));
 t.onclick=function(){nav(v);};return t;
}
function planetHead(co,m,metaText){
 const head=h('div','pl-head'),left=h('div','pl-hl');
 left.appendChild(h('h1',null,m.title));
 if(m.sub)left.appendChild(h('p','pl-sub',m.sub));
 const by=h('div','pl-by'),av=h('span','pl-av');av.innerHTML=ic(co.glyph||'book');by.appendChild(av);by.appendChild(h('span',null,metaText));left.appendChild(by);
 head.appendChild(left);head.appendChild(coverEl(co,'pl-cover'));return head;
}
function openRouteSheet(){
 const body=h('div');
 Object.keys(ROUTES).forEach(function(k){
  const r=h('button','pf-row'),p=h('span','pi');p.innerHTML=ic(k===S.route?'check':'route');r.appendChild(p);
  r.appendChild(h('span','tx',ROUTENAME[k]));r.appendChild(h('span','tail',ROUTES[k].length+' 章'));
  r.onclick=function(){S.route=k;S.idx=0;save();closeSheet();render();toast('已切换到'+ROUTENAME[k]);};body.appendChild(r);
 });
 openSheet('学习路线',body);
}
function renderCoursePlanet(sec,co){
 const m=coMeta(co),pr=courseProgress(),due=dueList().length,started=Object.keys(S.learn).length>0;
 sec.appendChild(planetHead(co,m,MODS.length+' 模块 · '+CH.length+' 章 · '+Object.keys(ROUTES).length+' 条学习路线'));
 const stat=h('div','pl-stat'),big=h('div','pl-big'),num=h('b',null,'0');
 big.appendChild(num);big.appendChild(h('span',null,'章已过关 · 共 '+pr.total+' 章'));stat.appendChild(big);
 const acts=h('div','pl-acts');
 const rb=h('button','sbtn ic');rb.innerHTML=ic('route');rb.setAttribute('aria-label','学习路线');rb.title=ROUTENAME[S.route];rb.onclick=openRouteSheet;acts.appendChild(rb);
 const go=h('button','sbtn solid');go.appendChild(document.createTextNode(started?'继续学习':'开始学习'));go.onclick=function(){nav('today');};acts.appendChild(go);
 stat.appendChild(acts);sec.appendChild(stat);countUp(num,pr.done);
 const tabs=h('div','pl-tabs');
 [['chs','章节'],['about','课程说明'],['tools','工具箱']].forEach(function(s){
  const b=h('button',cdSeg===s[0]?'on':'',s[1]);
  if(s[0]==='tools'&&due)b.appendChild(h('i','nb',String(due)));
  b.onclick=function(){cdSeg=s[0];renderCourse();};tabs.appendChild(b);
 });
 sec.appendChild(tabs);
 const body=h('div','swap');sec.appendChild(body);
 if(cdSeg==='chs'){
  const doneN=CH.filter(function(c){return S.status[c.id]==='green';}).length;
  let list=CH.filter(function(c){return cdFilter==='all'||(cdFilter==='done'?S.status[c.id]==='green':S.status[c.id]!=='green');});
  if(cdDesc)list=list.slice().reverse();
  const cnt=h('div','pl-count');cnt.appendChild(h('span',null,list.length+' 章'));
  const tools=h('div','tools'),sb=h('button','icbtn'+(cdDesc?' flip':''));sb.innerHTML=ic('sort');sb.setAttribute('aria-label','切换排序');sb.onclick=function(){cdDesc=!cdDesc;renderCourse();};tools.appendChild(sb);
  cnt.appendChild(tools);body.appendChild(cnt);
  const chips=h('div','pl-chips');
  [['all','全部'],['todo','未过关 '+(CH.length-doneN)],['done','已过关 '+doneN]].forEach(function(f){
   const b=h('button','chip2'+(cdFilter===f[0]?' on':''),f[1]);b.onclick=function(){cdFilter=f[0];renderCourse();};chips.appendChild(b);
  });
  body.appendChild(chips);
  const lst=h('div');list.forEach(function(c){lst.appendChild(epChapter(c,'list'));});
  if(!list.length)lst.appendChild(h('div','pl-empty','这个分类下还没有章节。'));
  body.appendChild(lst);
 }else if(cdSeg==='about'){
  const cd=h('div','card');
  const t=h('p','pp');t.textContent=co.desc;t.style.fontWeight='var(--fw-m)';t.style.lineHeight='1.8';cd.appendChild(t);
  if(DEPTEXT){const lb=h('p','muted','模块依赖关系：什么能跳，什么绝对不能');lb.style.margin='16px 0 8px';cd.appendChild(lb);cd.appendChild(h('pre',null,DEPTEXT));}
  if(DEPNOTES.length){const g=h('div','grid2');g.style.marginTop='14px';DEPNOTES.forEach(function(n){g.appendChild(note2(n[0],n[1]));});cd.appendChild(g);}
  body.appendChild(cd);
 }else{
  TOOLGROUPS.forEach(function(gr){
   body.appendChild(h('div','gtitle',gr.g));
   const g=h('div','tgrid');gr.items.forEach(function(t){g.appendChild(toolTile(t[0],t[1],t[2],(t[0]==='review'&&due)?due:0));});body.appendChild(g);
  });
 }
}
function renderCustomCourse(sec,co){
 const m=coMeta(co),chs=co.chapters||[];
 if(isPlanet())sec.appendChild(planetHead(co,m,chs.length+' 章 · 自建课程'));
 else{
  const hero=h('div','cdhero');hero.appendChild(coverEl(co,''));
  const ci=h('div','ci');ci.appendChild(h('h1',null,m.title));if(m.sub)ci.appendChild(h('p','sub',m.sub));
  const by=h('div','muted',chs.length+' 章 · 自建课程');by.style.marginTop='9px';ci.appendChild(by);hero.appendChild(ci);sec.appendChild(hero);
 }
 const body=h('div','swap');sec.appendChild(body);
 const lab=h('div',isPlanet()?'pl-count':'gtitle',isPlanet()?(chs.length+' 章'):'章节目录');body.appendChild(lab);
 if(!chs.length){
  body.appendChild(h('div','pl-empty','这门课程还没有章节内容。可在「我的 → 课程 → 导入」中导入带 chapters 的 JSON，格式见导入窗口里的示例。'));
  return;
 }
 chs.forEach(function(c,i){
  body.appendChild(listRow({icon:'book',title:(c.id!=null&&String(c.id)!==String(i+1)?c.id+' ':(i+1)+' ')+c.title,desc:c.summary||String(c.content||'').slice(0,60),onClick:function(){
   const b=h('div','shtext');(String(c.content||c.summary||'这一章还没有正文。')).split(/\n{2,}/).forEach(function(p){b.appendChild(h('p',null,p));});
   openSheet(c.title,b);
  }}));
 });
}
function renderCourse(){
 const sec=$('v-course');if(!sec)return;
 const co=curCourse();
 if(isPlanet()){sec.textContent='';renderCoursePlanet(sec,co);return;}
 renderCourseMin();
 const cdseg=sec.querySelector('.cdseg');
 if(cdseg){let n=cdseg.nextElementSibling;const wrap=h('div','swap');while(n){const nx=n.nextElementSibling;wrap.appendChild(n);n=nx;}sec.appendChild(wrap);}
}

/* ---------- 我的 ---------- */
function pfRow(box,icon,text,tail,fn){
 const r=h('button','pf-row'),p=h('span','pi');p.innerHTML=ic(icon);r.appendChild(p);r.appendChild(h('span','tx',text));
 if(tail)r.appendChild(h('span','tail',tail));
 const c=h('span','chv');c.innerHTML=ic('chev');r.appendChild(c);r.onclick=fn;box.appendChild(r);return r;
}
function renderProfilePlanet(){
 const box=$('profileBox');box.textContent='';
 const pr=courseProgress(),p=profileInfo(),pct=pr.total?Math.round(pr.done/pr.total*100):0;
 const wrap=h('div','stagger');box.appendChild(wrap);
 const head=h('div','pf-head'),l=h('div');
 const nm=h('div','pf-name');nm.appendChild(h('span',null,p.name));const cv=h('span');cv.innerHTML=ic('chev');nm.appendChild(cv.firstChild);l.appendChild(nm);
 l.appendChild(h('div','pf-bio',p.bio||'还没有设置签名'));head.appendChild(l);head.appendChild(avatarEl());head.onclick=openProfileEditor;wrap.appendChild(head);
 const ban=h('div','pf-banner'),bl=h('div'),bt=h('div','pf-bt');bt.innerHTML=ic('sparkle');
 bt.appendChild(h('span',null,coMeta(curCourse()).title));
 bl.appendChild(bt);bl.appendChild(h('div','pf-bs','已过关 '+pr.done+' / '+pr.total+' 章 · 累计学习 Day '+S.day));ban.appendChild(bl);
 const bg=h('div','pf-bg');bg.appendChild(document.createTextNode(Object.keys(S.learn||{}).length?'继续学习':'开始学习'));const bc=h('span');bc.innerHTML=ic('chev');bg.appendChild(bc.firstChild);ban.appendChild(bg);
 const bb=h('div','pf-bbar'),bi=h('i');bi.style.width=pct+'%';bb.appendChild(bi);ban.appendChild(bb);
 ban.onclick=function(){nav('today');};wrap.appendChild(ban);
 const g1=h('div');wrap.appendChild(g1);
 pfRow(g1,'book','课程',String(COURSES.length),function(){nav('course-manage');});
 pfRow(g1,'highlighter','划线',String((S.marks||[]).length),function(){selectedMarkCourse='';nav('my-marks');});
 pfRow(g1,'calendar','学习日历','',function(){nav('calendar');});
 wrap.appendChild(h('div','pf-div'));
 const g2=h('div');wrap.appendChild(g2);
 pfRow(g2,'settings','设置','',function(){nav('settings');});
 pfRow(g2,'layers','快捷入口','',function(){nav('shortcuts');});
}
function renderProfile(){
 renderProfilePlanet();return;
 renderProfileMin();
 const box=$('profileBox'),extra=h('div','my-group'),l=h('div','my-list');
 extra.appendChild(h('div','my-group-title','数据'));extra.appendChild(l);
 myItem(l,'database','数据','课程、学习手册、划线、进度、案例与术语','',function(){ioDetail=null;nav('io');});
 const first=box.querySelectorAll('.my-group')[2];if(first)box.insertBefore(extra,first);else box.appendChild(extra);
 box.className='stagger';
}

/* ---------- 课程管理 / 设置 / 日历 / 划线 ---------- */
function renderCourseManage(){
 const box=$('v-course-manage');box.textContent='';
 const list=h('div','stagger');
 COURSES.forEach(function(co){
  const m=coMeta(co),r=h('div','course-manage-row');
  r.appendChild(coverEl(co,'sm'));
  const ci=h('div','ci');ci.appendChild(h('b',null,m.title));ci.appendChild(h('small',null,m.sub||'课程简介尚未填写'));
  const pg=progOf(co);ci.appendChild(h('small',null,pg.chs+' 章 · '+pg.done+'/'+pg.total+' 已过关'));
  r.appendChild(ci);
  const act=h('div','cmrow-act');
  act.appendChild(iconBtn('pen','编辑课程',function(){openCourseEditor(co);}));
  if(co.custom)act.appendChild(iconBtn('trash','删除课程',function(){
   if(!confirm('删除「'+m.title+'」？该课程的章节、进度、划线、案例都会一并移除（建议先在「数据」里导出）。'))return;
   S.ui.userCourses=(S.ui.userCourses||[]).filter(function(x){return x.id!==co.id;});if(S.ui.co)delete S.ui.co[co.id];
   if(S.cp)delete S.cp[co.id];
   syncCourses();if(S.cur===co.id){S.cur='ops';loadPack('ops');}
   save();renderCourseManage();toast('课程已删除');
  }));
  r.appendChild(act);
  r.onclick=function(e){if(e.target.closest('button'))return;activeCourse=co.id;cdSeg='chs';nav('course');};
  list.appendChild(r);
 });
 box.appendChild(list);
}
function stripHead(id){const el=$(id);if(!el)return;el.querySelectorAll(':scope>.phead,:scope>.page-intro').forEach(function(n){n.remove();});}
function renderCalendar(){renderCalendarOrig();stripHead('v-calendar');}
function renderMyMarks(){renderMyMarksOrig();stripHead('v-my-marks');}
function renderSettings(){
 const box=$('v-settings');box.textContent='';
 const wrap=h('div','stagger');box.appendChild(wrap);
 const pal=h('div','clean-panel');pal.appendChild(h('h3',null,'主题色'));
 const sw=h('div','theme-swatches');
 ACCENT_PALETTE.forEach(function(it,i){
  const b=h('button','theme-swatch'+(((S.ui.accent==null?DEFAULT_ACCENT:S.ui.accent)===i)?' on':''));
  b.style.setProperty('--sw',it[1]);b.appendChild(h('i'));b.appendChild(h('span',null,it[0]));
  b.onclick=function(){S.ui.accent=i;themeFlash();applyAccent(it[1]);save();renderSettings();toast('主题色：'+it[0]);};sw.appendChild(b);
 });
 const cu=h('label','theme-swatch'+(S.ui.accent===-1?' on':''));
 cu.style.setProperty('--sw',S.ui.accent===-1&&S.ui.accentCustom?S.ui.accentCustom:'#9aa3ad');cu.style.cursor='pointer';
 cu.appendChild(h('i'));cu.appendChild(h('span',null,'自定义'));
 const ci=document.createElement('input');ci.type='color';ci.value=S.ui.accentCustom||accentHex();ci.style.cssText='position:absolute;opacity:0;width:0;height:0;pointer-events:none';
 ci.oninput=function(){S.ui.accent=-1;S.ui.accentCustom=ci.value;applyAccent(ci.value);cu.style.setProperty('--sw',ci.value);};
 ci.onchange=function(){S.ui.accent=-1;S.ui.accentCustom=ci.value;save();renderSettings();};
 cu.appendChild(ci);sw.appendChild(cu);pal.appendChild(sw);wrap.appendChild(pal);
 const ap=h('div','clean-panel');ap.appendChild(h('h3',null,'外观'));
 const dark=h('div','my-inline');dark.appendChild(h('span','label','夜间模式'));dark.appendChild(h('span','swt'+(S.theme==='dark'?' on':'')));
 dark.style.cursor='pointer';dark.onclick=function(){S.theme=S.theme==='dark'?'light':'dark';themeFlash();applyTheme();renderSettings();};ap.appendChild(dark);
 [['minimal','极简版','保持克制、留白与轻量层级','leaf'],['planet','星球版','播客式内容流：大标题、封面列表、进度播放条','orbit']].forEach(function(it){
  const on=(S.ui.style||'planet')===it[0];
  const row=listRow({icon:it[3],title:it[1]+(on?'  ·  使用中':''),desc:it[2],onClick:function(){
   if(on)return;S.ui.style=it[0];themeFlash();save();applyStyleMode();miniShown=false;render();toast('已切换为'+it[1]);
  }});
  row.style.marginTop='8px';ap.appendChild(row);
 });
 wrap.appendChild(ap);
 wrap.appendChild(satPanel());
  const io=h('div','clean-panel');io.appendChild(h('h3',null,'数据'));
 io.appendChild(listRow({icon:'database',title:'数据',desc:'课程、学习手册、划线、进度、案例与术语',onClick:function(){ioDetail=null;nav('io');}}));
 wrap.appendChild(io);
}

/* =====================================================================
   v8 · 多课程引擎：每门课程 = 独立的课程内容（章节/详解/长文/路线…）+ 独立的学习数据
   （进度、划线、案例、术语、路线位置）。切换课程 = 切换这一整套。
   ===================================================================== */
function bagKeys(){return ['route','idx','day','mode','status','learn','cases','gloss','marks','pin'];}
function newBag(route){return {route:route||'r1',idx:0,day:1,mode:'long',status:{},learn:{},cases:[],gloss:[],marks:[],pin:''};}
function curBag(){S.cp=S.cp||{};return S.cp[S.cur]||(S.cp[S.cur]=newBag());}
/* 读档后调用：把旧版单课程存档迁移成「按课程分开」的结构，并让 S.status 等指向当前课程 */
function initCourseState(){
 S.cp=(S.cp&&typeof S.cp==='object')?S.cp:{};
 if(!S.cur)S.cur='ops';
 if(!S.cp.ops){
  const b=newBag('r1');
  bagKeys().forEach(function(k){if(Object.prototype.hasOwnProperty.call(S,k)&&S[k]!==undefined)b[k]=S[k];});
  (b.marks||[]).forEach(function(m){if(m&&!m.courseId)m.courseId='ops';});
  S.cp.ops=b;
 }
 bagKeys().forEach(function(k){
  try{delete S[k];}catch(e){}
  Object.defineProperty(S,k,{configurable:true,enumerable:false,get:function(){return curBag()[k];},set:function(v){curBag()[k]=v;}});
 });
}
function allMarks(){
 const out=[];
 Object.keys(S.cp||{}).forEach(function(id){((S.cp[id]&&S.cp[id].marks)||[]).forEach(function(m){if(m&&!m.courseId)m.courseId=id;out.push(m);});});
 return out;
}

/* ---------- 课程内容包（pack） ---------- */
let OPS_PACK=null,LOADED_COURSE='',DEPNOTES=[];
function opsPack(){
 if(!OPS_PACK)OPS_PACK={mods:MODS,ch:CH,deep:DEEP,long:LONG,layers:LAYERS,cmds:CMDS,events:EVENTS,scen:SCEN,lab:LAB,routes:ROUTES,routeNames:ROUTENAME,glbase:GLBASE,depText:DEPTEXT,
  depNotes:[['允许插队：','B 模块、E 模块、D1 / D2，随时可提前学。'],['禁止跳过：','A2 → C4 → C5 这条链。跳过 A2 或 C4 去学 GPO，是 AD 学习失败的主要模式。']]};
 return OPS_PACK;
}
function packOf(co){return co&&co.custom&&co.pack?co.pack:opsPack();}
function normalizePack(x){
 x=x||{};
 const arr=function(v){return Array.isArray(v)?v:[];};
 const chIn=arr(x.chapters||x.ch);
 const mods=arr(x.modules||x.mods).filter(function(m){return m&&m.k!=null;}).map(function(m){return {k:String(m.k),n:String(m.n||('模块 '+m.k)),tag:String(m.tag||'')};});
 const ch=chIn.map(function(c,i){
  c=c||{};
  const q=c.quiz&&c.quiz.q?c.quiz:{q:'用自己的话复述本章最重要的三点。',a:'对照章节内容自查：能讲出来才算过关。',hint:''};
  return {id:String(c.id!=null?c.id:i+1),m:String(c.m||c.module||(mods[0]&&mods[0].k)||'A'),t:String(c.t||c.title||c.name||('第 '+(i+1)+' 章')),s:String(c.s||c.summary||''),
   terms:String(c.terms||''),plain:arr(c.plain).map(function(p){return [String(p[0]),String(p[1])];}),cmds:arr(c.cmds),pits:arr(c.pits),
   gate:String(c.gate||'能用自己的话讲清本章内容'),quiz:q,media:(c.media&&typeof c.media==='object')?c.media:null};
 });
 const have={};mods.forEach(function(m){have[m.k]=1;});
 ch.forEach(function(c){if(!have[c.m]){mods.push({k:c.m,n:'模块 '+c.m,tag:''});have[c.m]=1;}});
 const deep=(x.deep&&typeof x.deep==='object')?x.deep:{},long=(x.long&&typeof x.long==='object')?x.long:{};
 chIn.forEach(function(c,i){
  if(c&&c.content&&!long[ch[i].id]){
   const ps=String(c.content).split(/\n{2,}/).map(function(s){return s.trim();}).filter(Boolean);
   long[ch[i].id]={title:ch[i].t,concept:(looksMd(c.content)?[{md:String(c.content)}]:ps.map(function(p){return {p:p};})),practice:[{t:'本章无命令行操作',cmd:'参考正文中的步骤',d:'按正文逐项完成并留痕。'}],guide:[],quiz:ch[i].quiz};
  }
 });
 const ids={};ch.forEach(function(c){ids[c.id]=1;});
 const routes={};
 if(x.routes&&typeof x.routes==='object')Object.keys(x.routes).forEach(function(k){if(Array.isArray(x.routes[k]))routes[k]=x.routes[k].map(String).filter(function(id){return ids[id];});});
 if(!Object.keys(routes).length)routes.all=ch.map(function(c){return c.id;});
 const rn={};Object.keys(routes).forEach(function(k){rn[k]=(x.routeNames&&x.routeNames[k])||(k==='all'?'全章节顺序':'路线 '+k);});
 return {mods:mods,ch:ch,deep:deep,long:long,layers:arr(x.layers),cmds:arr(x.cmds),events:arr(x.events),scen:arr(x.scen),lab:arr(x.lab),routes:routes,routeNames:rn,
  glbase:(x.glbase&&typeof x.glbase==='object')?x.glbase:{},depText:String(x.depText||''),depNotes:arr(x.depNotes)};
}
function loadPack(id){
 const co=COURSES.find(function(c){return c.id===id;})||COURSES[0],p=packOf(co);
 MODS=p.mods;CH=p.ch;DEEP=p.deep;LONG=p.long;LAYERS=p.layers;CMDS=p.cmds;EVENTS=p.events;SCEN=p.scen;LAB=p.lab;
 ROUTES=p.routes;ROUTENAME=p.routeNames;GLBASE=p.glbase;DEPTEXT=p.depText;DEPNOTES=p.depNotes||[];
 KNOW_TOTAL=Object.keys(DEEP).reduce(function(n,k){return n+((DEEP[k]&&DEEP[k].deep)||[]).length;},0);
 LOADED_COURSE=co.id;
 const b=curBag();
 if(!ROUTES[b.route]){b.route=Object.keys(ROUTES)[0]||'all';b.idx=0;}
}
function setCourse(id){
 if(!COURSES.some(function(c){return c.id===id;}))return;
 if(S.cur===id&&LOADED_COURSE===id)return;
 S.cur=id;curBag();loadPack(id);miniShown=false;save();
}
try{Object.defineProperty(window,'activeCourse',{configurable:true,get:function(){return S.cur;},set:function(v){setCourse(v);}});}catch(e){}
function syncCourses(){
 for(let i=COURSES.length-1;i>=0;i--)if(COURSES[i].custom)COURSES.splice(i,1);
 ((S.ui&&S.ui.userCourses)||[]).forEach(function(c){
  if(!c||!c.id||COURSES.some(function(x){return x.id===c.id;}))return;
  if(!c.pack){c.pack=normalizePack(c);delete c.chapters;}
  c.custom=true;COURSES.push(c);
 });
}
function progOf(co){
 const b=S.cp&&S.cp[co.id],p=packOf(co);
 const q=(b&&p.routes[b.route])||p.routes[Object.keys(p.routes)[0]]||[];
 return {done:b?q.filter(function(id){return b.status[id]==='green';}).length:0,total:q.length,started:!!(b&&Object.keys(b.learn||{}).length),mods:p.mods.length,chs:p.ch.length};
}
function seq(){const k=ROUTES[S.route]?S.route:Object.keys(ROUTES)[0];return ROUTES[k]||[];}
function nav(v){
 if(['today','know','review','prog','play'].indexOf(v)>=0&&!CH.length){toast('这门课程还没有章节内容，可通过「课程 → 导入」补充');return;}
 navCore(v);
}
function save(){try{localStorage.setItem(KEY,JSON.stringify(S));}catch(e){try{toast('存储空间不足：请先导出备份，再删除不用的课程');}catch(_){}}}

/* =====================================================================
   数据：一份统一结构，JSON / Markdown / CSV 内容完全一致
   整门课程档案 = 课程信息 + 全部章节内容 + 学习手册 + 该课程的进度 / 划线 / 案例 / 术语
   ===================================================================== */
const AR_APP='desktop-it-ops-mentor',AR_VER=7;
function courseObj(co,scope){
 const m=coMeta(co),p=packOf(co),o={id:co.id,title:m.title,subtitle:m.sub||'',description:co.desc||m.sub||'',cover:m.cover||''};
 if(scope==='all'||scope==='course'){
  o.modules=p.mods;o.chapters=p.ch;o.deep=p.deep;o.long=p.long;o.layers=p.layers;o.cmds=p.cmds;o.events=p.events;o.scen=p.scen;o.lab=p.lab;
  o.routes=p.routes;o.routeNames=p.routeNames;o.depText=p.depText;o.depNotes=p.depNotes||[];o.glbase=p.glbase||{};
 }
 if(scope==='all'||scope==='course'||scope==='handbook')o.handbook=manualMd();
 const st=archiveState(scope);if(Object.keys(st).length)o.state=st;
 return o;
}
function archiveState(scope){
 const log=(S.ui||{}).studyLog||{};
 if(scope==='marks')return {marks:S.marks||[]};
 if(scope==='progress')return {route:S.route,idx:S.idx,day:S.day,mode:S.mode,pin:S.pin||'',status:S.status||{},learn:S.learn||{},studyLog:log};
 if(scope==='cases')return {cases:S.cases||[],gloss:S.gloss||[]};
 if(scope==='all')return {route:S.route,idx:S.idx,day:S.day,mode:S.mode,pin:S.pin||'',status:S.status||{},learn:S.learn||{},cases:S.cases||[],gloss:S.gloss||[],marks:S.marks||[],studyLog:log};
 return {};
}
function makeArchive(scope){
 const c=courseObj(curCourse(),scope),st=c.state||{};
 return {app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:scope,exportedAt:new Date().toISOString(),
  counts:{marks:(st.marks||[]).length,cases:(st.cases||[]).length,gloss:(st.gloss||[]).length,learned:Object.keys(st.learn||{}).length},courses:[c]};
}
function chTitle(id){const c=chById(id);return c?id+' '+c.t:(id||'');}
function chunk(s,n){const a=[];s=String(s||'');for(let i=0;i<s.length;i+=n)a.push(s.slice(i,i+n));return a;}
function archiveText(scope,format){return renderArchive(makeArchive(scope),scope,format);}
function renderArchive(o,scope,format){
 const c=o.courses[0],st=c.state||{};
 const meta=IO_ENTRIES.find(function(e){return e.id===scope;})||{name:'学习档案'};
 if(format==='json')return {text:JSON.stringify(o,null,2),ext:'json',mime:'application/json'};
 if(format==='md'){
  const data=JSON.parse(JSON.stringify(o));delete data.courses[0].handbook;
  let md='# '+c.title+' · '+meta.name+'\n\n导出时间：'+new Date().toLocaleString('zh-CN')+'\n\n> 本文件可在「我的 → 数据 → 导入档案」中直接重新导入。请保留文末「数据存档」代码块，其余章节仅用于阅读。\n\n';
  if(c.description)md+='## 课程简介\n\n'+c.description+'\n\n';
  if(c.chapters&&c.chapters.length)md+='## 章节目录\n\n'+c.chapters.map(function(x){return '- **'+x.id+'** '+x.t+(x.s?'　—　'+x.s:'');}).join('\n')+'\n\n';
  if(st.marks&&st.marks.length)md+='## 划线与备注\n\n'+st.marks.map(function(m,i){return (i+1)+'. '+(m.t||'')+(m.n?'\n   - 备注：'+m.n:'')+(m.ch?'\n   - 章节：'+chTitle(m.ch):'');}).join('\n\n')+'\n\n';
  if(st.cases&&st.cases.length)md+='## 故障案例\n\n'+st.cases.map(function(x){return '### '+(x.no||'案例')+'\n\n- 现象：'+(x.sym||'')+'\n- 排查路径：'+String(x.path||'').replace(/\n/g,' / ')+'\n- 根因：'+(x.cause||'')+'\n- 结论：'+(x.lesson||'');}).join('\n\n')+'\n\n';
  if(st.gloss&&st.gloss.length)md+='## 自填术语\n\n'+st.gloss.map(function(g){return '- **'+g.term+'**（'+(g.full||'')+'）：'+(g.say||'');}).join('\n')+'\n\n';
  if(st.status||st.learn){const ids=Object.keys(Object.assign({},st.status||{},st.learn||{}));md+='## 学习进度\n\n- 有记录的章节：'+ids.length+'\n- 已过关：'+ids.filter(function(k){return (st.status||{})[k]==='green';}).length+'\n\n';}
  if(c.handbook)md+='## 学习手册\n\n<!--handbook-->\n'+c.handbook+'\n<!--/handbook-->\n\n';
  md+='## 数据存档\n\n```json\n'+JSON.stringify(data,null,2)+'\n```\n';
  return {text:md,ext:'md',mime:'text/markdown'};
 }
 /* CSV：一行一条记录；超长内容拆行，保证单元格不超过表格软件上限 */
 const J=JSON.stringify,rows=[['type','course','id','title','content','json']],k=c.id;
 rows.push(['meta','','archive',c.title+' · '+meta.name,'导出时间 '+new Date().toLocaleString('zh-CN'),J({app:o.app,kind:o.kind,ver:o.ver,scope:o.scope,exportedAt:o.exportedAt})]);
 rows.push(['course',k,k,c.title,c.description||'',J({id:c.id,title:c.title,subtitle:c.subtitle,description:c.description,cover:c.cover})]);
 (c.modules||[]).forEach(function(m){rows.push(['module',k,m.k,m.n,m.tag||'',J(m)]);});
 (c.chapters||[]).forEach(function(x){rows.push(['chapter',k,x.id,x.t,x.s||'',J(x)]);});
 Object.keys(c.deep||{}).forEach(function(id){rows.push(['deep',k,id,chTitle(id),'',J(c.deep[id])]);});
 Object.keys(c.long||{}).forEach(function(id){rows.push(['long',k,id,(c.long[id]&&c.long[id].title)||chTitle(id),'',J(c.long[id])]);});
 Object.keys(c.routes||{}).forEach(function(id){rows.push(['route',k,id,(c.routeNames||{})[id]||id,(c.routes[id]||[]).length+' 章',J({name:(c.routeNames||{})[id]||id,ids:c.routes[id]})]);});
 ['layers','cmds','events','scen','lab','glbase','depText','depNotes'].forEach(function(f){if(c[f]!==undefined)rows.push(['resource',k,f,f,'',J({v:c[f]})]);});
 if(c.handbook)chunk(c.handbook,20000).forEach(function(part,i){rows.push(['handbook',k,String(i),'学习手册 第 '+(i+1)+' 段',part.slice(0,60).replace(/\s+/g,' ')+'…',J({part:part})]);});
 const sj={};['route','idx','day','mode','pin'].forEach(function(f){if(st[f]!==undefined)sj[f]=st[f];});
 if(Object.keys(sj).length)rows.push(['state',k,'session','学习位置','路线 '+(sj.route||'')+' · 第 '+((sj.idx||0)+1)+' 站 · Day '+(sj.day||1),J(sj)]);
 Object.keys(Object.assign({},st.status||{},st.learn||{})).forEach(function(id){
  const s=(st.status||{})[id],l=(st.learn||{})[id];
  rows.push(['progress',k,id,chTitle(id),({green:'已过关',yellow:'半过关',red:'未过关'})[s]||'学习中',J({status:s||null,learn:l||null})]);
 });
 (st.marks||[]).forEach(function(m){rows.push(['mark',k,m.id,chTitle(m.ch),(m.t||'')+(m.n?' ｜ 备注：'+m.n:''),J(m)]);});
 (st.cases||[]).forEach(function(x){rows.push(['case',k,x.no,x.sym,x.cause,J(x)]);});
 (st.gloss||[]).forEach(function(g){rows.push(['gloss',k,g.term,g.full,g.say,J(g)]);});
 if(st.studyLog&&Object.keys(st.studyLog).length)rows.push(['studylog',k,'studylog','学习日历记录','',J(st.studyLog)]);
 return {text:csvOf(rows),ext:'csv',mime:'text/csv'};
}
function archiveSample(scope){
 const course={id:'my-course',title:'示例课程',subtitle:'一句话简介',description:'课程介绍',cover:'',
  modules:[{k:'A',n:'模块 A · 基础',tag:'必修'}],
  chapters:[{id:'A1',m:'A',t:'第一章',s:'本章摘要',terms:'',plain:[],cmds:[],pits:[],gate:'过关标准',quiz:{q:'练习题',a:'参考答案',hint:''}}],
  long:{A1:{title:'第一章',concept:[{p:'正文第一段'}],practice:[{t:'练习',cmd:'ipconfig /all',d:'说明'}],guide:[],quiz:{q:'练习题',a:'参考答案'}}},
  routes:{all:['A1']},routeNames:{all:'全章节顺序'}};
 const state={route:'all',idx:0,day:1,status:{A1:'green'},learn:{A1:{at:'2026-11-07',done:[]}},
  marks:[{id:'m1',ch:'A1',t:'划线原文',n:'我的备注',ty:'solid',co:'b',tags:[]}],
  cases:[{no:'NET-001',sym:'现象',path:'1. 排查步骤',cause:'根因',lesson:'下次第一步',ch:'A1'}],
  gloss:[{term:'DNS',full:'Domain Name System',say:'把名字翻译成 IP 的电话簿'}]};
 const o={app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:scope};
 if(scope==='all'){course.state=state;o.courses=[course];}
 else if(scope==='course'){o.courses=[course];}
 else if(scope==='handbook'){o.courses=[{title:'示例课程',handbook:'# 学习手册\n\n正文…'}];}
 else if(scope==='marks')o.courses=[{title:'示例课程',state:{marks:state.marks}}];
 else if(scope==='progress')o.courses=[{title:'示例课程',state:{route:'all',idx:0,status:state.status,learn:state.learn}}];
 else o.courses=[{title:'示例课程',state:{cases:state.cases,gloss:state.gloss}}];
 return JSON.stringify(o,null,2);
}
function parseCsv(text){
 const rows=[];let row=[],f='',q=false;
 for(let i=0;i<text.length;i++){
  const c=text[i];
  if(q){if(c==='"'){if(text[i+1]==='"'){f+='"';i++;}else q=false;}else f+=c;}
  else if(c==='"')q=true;
  else if(c===','){row.push(f);f='';}
  else if(c==='\n'||c==='\r'){if(c==='\r'&&text[i+1]==='\n')i++;row.push(f);f='';rows.push(row);row=[];}
  else f+=c;
 }
 if(f!==''||row.length){row.push(f);rows.push(row);}
 return rows.filter(function(r){return r.length>1||(r[0]||'')!=='';});
}
function csvToArchive(text){
 const rows=parseCsv(text);if(!rows.length)return null;
 const head=rows[0].map(function(x){return String(x).trim().toLowerCase();}),ti=head.indexOf('type'),ji=head.indexOf('json'),ci=head.indexOf('course'),ii=head.indexOf('id');
 if(ti<0||ji<0)return null;
 const o={app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:'all',courses:[]},map={},hb={};
 function get(key){key=key||'c1';if(!map[key]){map[key]={state:{}};o.courses.push(map[key]);}return map[key];}
 rows.slice(1).forEach(function(r){
  const type=String(r[ti]||'').trim(),key=ci>=0?String(r[ci]||''):'c1',id=ii>=0?String(r[ii]||''):'';let j=null;
  if(r[ji]){try{j=JSON.parse(r[ji]);}catch(e){return;}}
  if(!j)return;
  if(type==='meta'){o.app=j.app||o.app;o.kind=j.kind||o.kind;o.ver=j.ver||o.ver;o.scope=j.scope||o.scope;o.exportedAt=j.exportedAt;return;}
  const c=get(key),st=c.state;
  if(type==='course')Object.assign(c,j);
  else if(type==='module')(c.modules=c.modules||[]).push(j);
  else if(type==='chapter')(c.chapters=c.chapters||[]).push(j);
  else if(type==='deep')(c.deep=c.deep||{})[id]=j;
  else if(type==='long')(c.long=c.long||{})[id]=j;
  else if(type==='route'){(c.routes=c.routes||{})[id]=j.ids||[];(c.routeNames=c.routeNames||{})[id]=j.name||id;}
  else if(type==='resource')c[id]=j.v;
  else if(type==='handbook'){(hb[key]=hb[key]||[])[Number(id)||0]=j.part||'';}
  else if(type==='state')Object.assign(st,j);
  else if(type==='progress'){if(j.status)(st.status=st.status||{})[id]=j.status;if(j.learn)(st.learn=st.learn||{})[id]=j.learn;}
  else if(type==='mark')(st.marks=st.marks||[]).push(j);
  else if(type==='case')(st.cases=st.cases||[]).push(j);
  else if(type==='gloss')(st.gloss=st.gloss||[]).push(j);
  else if(type==='studylog')st.studyLog=j;
 });
 Object.keys(hb).forEach(function(k){get(k).handbook=hb[k].join('');});
 o.courses.forEach(function(c){if(!Object.keys(c.state).length)delete c.state;});
 return o.courses.length?o:null;
}
function mdToArchive(text){
 const i=text.lastIndexOf('```json');if(i<0)return null;
 const rest=text.slice(i+7),j=rest.indexOf('\n```');if(j<0)return null;
 let o;try{o=JSON.parse(rest.slice(0,j));}catch(e){return null;}
 const hb=text.match(/<!--handbook-->\n([\s\S]*?)\n<!--\/handbook-->/);
 if(hb&&o&&Array.isArray(o.courses)&&o.courses[0]&&!o.courses[0].handbook)o.courses[0].handbook=hb[1];
 return o;
}
/* ---------- 旧版档案（v6 及更早）修复：正文只在顶层 handbook 里 ----------
   按 manualMd 的章节结构解析出每章的：双轨对照、概念（含表格）、实操步骤、避坑指南、互动题、过关标准，
   重建 long / plain / gate / quiz / deep，使导入的课程与内置课程一样内容完整。 */
function parseHandbookChapter(body){
 const res={plain:[],concept:[],practice:[],guide:[],quiz:{q:'',a:'',hint:''},gate:''};
 const cut=function(from,to){
  const i=body.indexOf(from);if(i<0)return '';
  const j=to>from?body.indexOf(to,i+from.length):body.length;
  return body.slice(i+from.length,j<0?body.length:j);
 };
 /* 双轨对照：正文里第一张「专业术语 | 大白话」表 */
 const tb=body.match(/\*\*双轨对照\*\*\s*\n((?:\|[^\n]*\n)+)/);
 if(tb){
  tb[1].split('\n').forEach(function(ln){
   const cells=ln.split('|').slice(1,-1).map(function(x){return x.trim();});
   if(cells.length>=2&&cells[0]&&cells[0]!=='专业术语'&&!/^[-:\s]*$/.test(cells[0]))res.plain.push([cells[0],cells.slice(1).join('｜')]);
  });
 }
 /* 概念：段落到段落的块解析（表格 / 列表 / 段落），首行 **标题** 提出来 */
 const concept=cut('#### 一、概念','#### 二、实操');
 if(concept){
  const blocks=concept.split(/\n{2,}/).map(function(b){return b.replace(/\s+$/,'');}).filter(Boolean);
  blocks.forEach(function(bk){
   const lines=bk.split('\n');
   if(lines.every(function(l){return l.trim().indexOf('|')===0;})){
    const rows=lines.filter(function(l){return !/^[\s|:-]+$/.test(l);}).map(function(l){return l.split('|').slice(1,-1).map(function(x){return x.trim();});});
    if(rows.length>=2)res.concept.push({tbl:{head:rows[0],rows:rows.slice(1)}});
    return;
   }
   if(lines.every(function(l){return /^-\s+/.test(l.trim());})){
    res.concept.push({ul:lines.map(function(l){return l.trim().slice(2);})});return;
   }
   lines.forEach(function(l){
    const t=l.replace(/^\*\*|\*\*$/g,'').trim();
    if(!t||/^本章要解决的问题：/.test(t))return;
    res.concept.push({p:t});
   });
  });
 }
 /* 实操：**N. 步骤名** + 说明 + 命令代码块 + 预期输出 + 怎么读 */
 const practice=cut('#### 二、实操','#### 三、指南');
 if(practice){
  const parts=practice.split(/\n(?=\*\*\d+\.\s)/);
  parts.forEach(function(pt){
   const mt=pt.match(/^\*\*(\d+)\.\s+(.+?)\*\*/);if(!mt)return;
   const st={t:mt[2].trim(),cmd:'',d:'',out:'',read:''};
   const rest=pt.slice(mt[0].length);
   const codes=[];const cm=rest.match(/```[^\n]*\n([\s\S]*?)```/g)||[];
   cm.forEach(function(cb){codes.push(cb.replace(/^```[^\n]*\n/,'').replace(/```\s*$/,'').split('\n').map(function(l){return l.replace(/^  /,'');}).join('\n').trim());});
   if(codes.length)st.cmd=codes[0];
   if(codes.length>1)st.out=codes[1];
   const rd=rest.match(/怎么读：([^\n]*)/);if(rd)st.read=rd[1].trim();
   const dv=rest.split('```')[0].split('\n').map(function(l){return l.trim();}).filter(function(l){return l&&!/^预期输出/.test(l);});
   if(dv.length)st.d=dv.join(' ');
   res.practice.push(st);
  });
 }
 /* 指南：**N. 错误做法：w** + 后果 + 正确 */
 const guide=cut('#### 三、指南','#### 四、互动题');
 if(guide){
  const gd=guide.match(/\*\*\d+\.\s*错误做法：(.+?)\*\*\s*\n-\s*后果：([^\n]*)\s*\n-\s*正确：([^\n]*)/g)||[];
  gd.forEach(function(g){
   const m=g.match(/\*\*\d+\.\s*错误做法：(.+?)\*\*\s*\n-\s*后果：([^\n]*)\s*\n-\s*正确：([^\n]*)/);
   if(m)res.guide.push({w:m[1].trim(),c:m[2].trim(),r:m[3].trim()});
  });
 }
 /* 互动题 + 过关标准 */
 const quiz=cut('#### 四、互动题','**过关标准**');
 if(quiz){
  const ai=quiz.indexOf('参考答案：');
  let q=quiz,hint='';
  const hi=quiz.match(/> 提示：([^\n]*)/);if(hi)hint=hi[1].trim();
  q=q.split('\n').filter(function(l){return l.trim()&&!/^> /.test(l.trim())&&l.indexOf('参考答案：')<0&&l.indexOf('#### ')<0;}).join('\n').trim();
  res.quiz.q=q;res.quiz.hint=hint;
  if(ai>=0)res.quiz.a=quiz.slice(ai+5).split('\n').filter(function(l){return l.trim()&&l.indexOf('**过关标准**')<0;}).join('\n').trim();
 }
 const gt=body.match(/\*\*过关标准\*\*：?\s*([\s\S]*)$/);
 if(gt)res.gate=gt[1].split('\n').filter(function(l){return l.trim();}).join(' ').trim();
 return res;
}
function repairFromHandbook(c){
 if(!c||typeof c!=='object'||!Array.isArray(c.chapters)||!c.chapters.length)return;
 const hb=typeof c.handbook==='string'?c.handbook:'';
 if(!hb)return;
 const hasContent=c.chapters.some(function(x){return x&&((x.plain&&x.plain.length)||(x.content&&String(x.content).trim()));})||(c.long&&Object.keys(c.long).length);
 if(hasContent)return;
 /* 按「### 章号　标题」切分章节 */
 const secs=[];const re=/^###\s+(\S+)[\s\u3000]+([^\n]*)$/gm;let m;
 while((m=re.exec(hb)))secs.push({id:m[1],title:m[2].trim(),start:m.index});
 secs.forEach(function(s,i){s.end=i+1<secs.length?secs[i+1].start:hb.length;});
 if(!secs.length)return;
 c.long=c.long||{};c.deep=c.deep||{};
 c.chapters.forEach(function(x){
  if(!x||!x.id)return;
  const sec=secs.filter(function(s){return s.id===String(x.id);})[0];if(!sec)return;
  const P=parseHandbookChapter(hb.slice(sec.start,sec.end));
  if((!x.plain||!x.plain.length)&&P.plain.length)x.plain=P.plain;
  if(!x.gate&&P.gate)x.gate=P.gate;
  if((!x.quiz||!x.quiz.q)&&P.quiz.q)x.quiz=P.quiz;
  if(!c.long[x.id]){
   c.long[x.id]={title:x.title||sec.title,concept:P.concept,practice:P.practice,guide:P.guide,
    quiz:(x.quiz&&x.quiz.q&&x.quiz.a)?x.quiz:P.quiz};
  }
  /* 知识详解：概念段落拆成「标题 + 正文」，段落以第一句为标题 */
  if(!c.deep[x.id]&&P.concept.length){
   const rows=[];
   P.concept.forEach(function(r){
    if(r.p&&r.p.length>24){
     const i=r.p.indexOf('。');
     if(i>0&&i<60)rows.push([r.p.slice(0,i),r.p.slice(i+1)]);
     else rows.push([r.p.slice(0,15),r.p]);
    }
   });
   if(rows.length)c.deep[x.id]={deep:rows,out:[],flow:[]};
  }
 });
}
function normalizeArchive(o){
 if(Array.isArray(o))o={courses:o};
 if(!o||typeof o!=='object')return null;
 if(!(o.kind==='portable-archive'||o.app===AR_APP||(o.kind==='backup'&&o.state))){
  if(Array.isArray(o.courses)||o.title||o.name)o={app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:'course',courses:Array.isArray(o.courses)?o.courses:[o]};
  else return null;
 }
 if(!Array.isArray(o.courses))o.courses=[];
 o.scope=o.scope||'course';
 /* 旧版全量备份（v4 及更早：kind='backup'，只有 state 没有 courses）：转换成一门完整课程导入 */
 if(o.kind==='backup'&&o.state&&!o.courses.length){
  o.scope='all';
  const src=(typeof opsPack==='function')?opsPack():null;
  const body=src?JSON.parse(JSON.stringify({modules:src.mods,chapters:src.ch,deep:src.deep,long:src.long,layers:src.layers,cmds:src.cmds,events:src.events,scen:src.scen,lab:src.lab,routes:src.routes,routeNames:src.routeNames,glbase:src.glbase,depText:src.depText,depNotes:src.depNotes})):{};
  const base=(typeof COURSES!=='undefined'&&COURSES[0])||{};
  o.courses=[Object.assign({title:base.title||'桌面运维课程',description:base.desc||''},body,{state:o.state})];
  delete o.state;
 }
 const clean=function(st){
  if(!st||typeof st!=='object')return undefined;
  ['marks','cases','gloss'].forEach(function(k){if(st[k]!==undefined&&!Array.isArray(st[k]))delete st[k];});
  ['status','learn','studyLog'].forEach(function(k){if(st[k]!==undefined&&(!st[k]||typeof st[k]!=='object'||Array.isArray(st[k])))delete st[k];});
  return st;
 };
 o.courses=o.courses.filter(function(c){return c&&typeof c==='object';});
 o.courses.forEach(function(c){c.state=clean(c.state);if(!c.state)delete c.state;});
 if(o.state){o.state=clean(o.state);}
 /* v6 及更早的导出：state 与 handbook 在顶层，挂回（唯一的）课程 */
 if(o.state&&o.courses.length===1&&!o.courses[0].state)o.courses[0].state=clean(o.state);
 delete o.state;
 if(typeof o.handbook==='string'&&o.handbook)o.courses.forEach(function(c){if(!c.handbook)c.handbook=o.handbook;});
 delete o.handbook;
 o.courses.forEach(repairFromHandbook);
 return o;
}
function parseArchiveText(raw){
 const t=String(raw||'').replace(/^\uFEFF/,'').trim();
 if(!t)return {err:'内容为空，请选择文件或粘贴档案内容。'};
 let o=null,fmt='';
 if(t[0]==='{'||t[0]==='['){try{o=JSON.parse(t);fmt='JSON';}catch(e){return {err:'JSON 格式有误：'+e.message};}}
 else if(t.indexOf('```json')>=0){o=mdToArchive(t);fmt='Markdown';if(!o)return {err:'Markdown 里没有找到完整的「数据存档」代码块，请确认文件末尾未被删改。'};}
 else if(/^"?type"?\s*,/i.test(t.split(/\r?\n/)[0])){o=csvToArchive(t);fmt='CSV';if(!o)return {err:'CSV 表头应为 type,course,id,title,content,json。'};}
 else return {err:'无法识别格式：请使用本应用导出的 JSON、Markdown 或 CSV。'};
 const n=normalizeArchive(o);
 if(!n)return {err:'这不是本应用的档案：缺少 app / kind 字段，也没有 courses 课程列表。可点上方 { } 图标查看示例。'};
 return {obj:n,fmt:fmt};
}
function isFullScope(o){return o.scope==='all'||o.scope==='course';}
function archiveSummary(o){
 const cs=o.courses||[],c=cs[0]||{},st=c.state||o.state||{},a=[];
 if(isFullScope(o)&&cs.length)a.push(['课程',cs.length>1?cs.length+' 门':(c.title||'')]);
 const chs=cs.reduce(function(n,x){return n+((x&&x.chapters)||[]).length;},0);if(chs)a.push(['章节',chs]);
 if(c.long&&Object.keys(c.long).length)a.push(['长文详解',Object.keys(c.long).length]);
 if(c.deep&&Object.keys(c.deep).length)a.push(['知识点章节',Object.keys(c.deep).length]);
 if(c.handbook)a.push(['学习手册',Math.max(1,Math.round(String(c.handbook).length/1000))+'k 字']);
 if(st.marks)a.push(['划线',st.marks.length]);
 if(st.status||st.learn){a.push(['有记录章节',Object.keys(Object.assign({},st.status||{},st.learn||{})).length]);a.push(['已过关',Object.keys(st.status||{}).filter(function(k){return st.status[k]==='green';}).length]);}
 if(st.cases)a.push(['案例',st.cases.length]);
 if(st.gloss)a.push(['术语',st.gloss.length]);
 return a;
}
/* 课程标题去重：同名自动加「副本」 */
function uniqueCourseId(base){
 let id=base,k=2;
 while(COURSES.some(function(c){return c.id===id;})||(S.cp&&S.cp[id]))id=base+'_'+(k++);
 return id;
}
function uniqueCourseTitle(t){
 const names=COURSES.map(function(c){return coMeta(c).title;});
 if(names.indexOf(t)<0)return t;
 let k=2,x=t+' 副本';while(names.indexOf(x)>=0)x=t+' 副本 '+(k++);return x;
}
function fillBag(bag,st,id,pack){
 if(!st)return;
 if(Array.isArray(st.marks))bag.marks=st.marks.filter(function(m){return m&&typeof m==='object';}).map(function(m){return Object.assign({},m,{courseId:id});});
 if(st.status&&typeof st.status==='object')bag.status=Object.assign({},st.status);
 if(st.learn&&typeof st.learn==='object')bag.learn=Object.assign({},st.learn);
 if(Array.isArray(st.cases))bag.cases=st.cases.slice();
 if(Array.isArray(st.gloss))bag.gloss=st.gloss.slice();
 if(st.route&&pack.routes[st.route]){bag.route=st.route;if(typeof st.idx==='number')bag.idx=Math.max(0,st.idx);}
 if(typeof st.day==='number')bag.day=st.day;
 if(typeof st.mode==='string')bag.mode=st.mode;
 if(typeof st.pin==='string'&&pack.ch.some(function(c){return c.id===st.pin;}))bag.pin=st.pin;
 mergeLog(st.studyLog);
}
function mergeLog(log){
 if(!log||typeof log!=='object')return;
 S.ui.studyLog=S.ui.studyLog||{};Object.keys(log).forEach(function(k){S.ui.studyLog[k]=Math.max(S.ui.studyLog[k]||0,Number(log[k])||0);});
}
/* 导入课程：永远新建一门独立课程（含全部内容与该课程自己的学习数据）；重名则加「副本」
   （旧版导出会带 builtin:true 标记，那只是导出时的痕迹，不影响导入——导出的课程同样要能重新导入） */
function importCourse(x,i){
 if(!x||typeof x!=='object')return null;
 const title=String(x.title||x.name||'').trim();if(!title)return null;
 S.ui=S.ui||{};S.ui.co=S.ui.co||{};S.ui.userCourses=S.ui.userCourses||[];S.cp=S.cp||{};
 const pack=normalizePack(x),id=uniqueCourseId('c_'+Date.now().toString(36)+'_'+i);
 const desc=String(x.description||x.desc||''),sub=String(x.subtitle||x.sub||desc);
 const rec={id:id,title:uniqueCourseTitle(title),sub:sub,desc:desc||sub,cover:'cv-a',glyph:'book',custom:true,pack:pack};
 if(typeof x.handbook==='string')rec.handbook=x.handbook;
  if(x.media)rec.media=String(x.media);
 S.ui.userCourses.push(rec);
 if(typeof x.cover==='string'&&x.cover.indexOf('data:image')===0)S.ui.co[id]={cover:x.cover};
 const bag=newBag(Object.keys(pack.routes)[0]);S.cp[id]=bag;
 const st=x.state||{};
 fillBag(bag,st,id,pack);
 /* 旧版档案的 state.ui 里带着个人信息：只补空缺，不覆盖现有资料 */
 if(st.ui&&st.ui.profile&&typeof st.ui.profile==='object'){
  S.ui.profile=S.ui.profile||{};
  ['name','bio','avatar'].forEach(function(k){if(!S.ui.profile[k]&&st.ui.profile[k])S.ui.profile[k]=st.ui.profile[k];});
  delete st.ui;
 }
 migrateMarkList(bag.marks);
 syncCourses();
 return id;
}
/* 局部档案（划线 / 进度 / 案例 / 手册）：合并或覆盖到「当前课程」 */
function applyPartial(st,mode){
 const rank={green:3,yellow:2,red:1};
 if(mode==='cover'){
  ['marks','cases','gloss'].forEach(function(k){if(Array.isArray(st[k]))S[k]=st[k].slice();});
  if(Array.isArray(st.marks))S.marks.forEach(function(m){m.courseId=S.cur;});
  if(st.status)S.status=Object.assign({},st.status);
  if(st.learn)S.learn=Object.assign({},st.learn);
  if(st.route&&ROUTES[st.route]){S.route=st.route;if(typeof st.idx==='number')S.idx=st.idx;}
  if(typeof st.day==='number')S.day=st.day;
  if(typeof st.mode==='string')S.mode=st.mode;
  if(typeof st.pin==='string'&&chById(st.pin))S.pin=st.pin;
 }else{
  [['marks',function(v){return v.id||JSON.stringify(v);}],['cases',function(v){return v.no||JSON.stringify(v);}],['gloss',function(v){return v.term||JSON.stringify(v);}]].forEach(function(p){
   if(!Array.isArray(st[p[0]]))return;
   const have={};S[p[0]].forEach(function(v){have[p[1](v)]=1;});
   st[p[0]].forEach(function(v){if(!v||typeof v!=='object')return;const k=p[1](v);if(!have[k]){if(p[0]==='marks')v=Object.assign({},v,{courseId:S.cur});S[p[0]].push(v);have[k]=1;}});
  });
  if(st.status)Object.keys(st.status).forEach(function(k){if(!S.status[k]||(rank[st.status[k]]||0)>(rank[S.status[k]]||0))S.status[k]=st.status[k];});
  if(st.learn)Object.keys(st.learn).forEach(function(k){if(!S.learn[k])S.learn[k]=st.learn[k];});
 }
 mergeLog(st.studyLog);
}
function applyArchive(obj,mode,only){
 S.ui=S.ui||{};
 const created=[];
 if(isFullScope(obj)||only==='courses'){
  obj.courses.forEach(function(x,i){const id=importCourse(x,i);if(id)created.push(id);});
 }else{
  const x=obj.courses[0]||{},st=x.state||obj.state||{};
  applyPartial(st,mode);
  if(typeof x.handbook==='string'){const co=curCourse();if(co.custom)co.handbook=x.handbook;else S.ui.handbook=x.handbook;}
  migrateMarks();
 }
 syncCourses();save();render();
 return {created:created,partial:!(isFullScope(obj)||only==='courses')};
}

/* ---------- 数据页面 ---------- */
function renderIo(){
 const box=$('v-io');if(!box)return;box.textContent='';
 if(ioDetail==='import'){renderImportPage(box);return;}
 if(ioDetail){renderExportPage(box);return;}
 const wrap=h('div','stagger');box.appendChild(wrap);
 wrap.appendChild(h('div','io-cur','当前课程 · '+coMeta(curCourse()).title));
 wrap.appendChild(h('div','gtitle','导出'));
 IO_ENTRIES.forEach(function(e){wrap.appendChild(listRow({icon:e.icon,title:e.title,desc:e.desc,onClick:function(){ioDetail=e.id;ioFormat='json';render();}}));});
 wrap.appendChild(h('div','gtitle','导入'));
 wrap.appendChild(listRow({icon:'upload',title:'导入档案',desc:'整门课程会新建为一门独立课程；划线、进度、案例等合并到当前课程',onClick:function(){impState={text:'',res:null,mode:'merge',fname:''};ioDetail='import';render();}}));
}
function renderExportPage(box){
 const meta=IO_ENTRIES.find(function(e){return e.id===ioDetail;});if(!meta){ioDetail=null;renderIo();return;}
 const wrap=h('div','io-page stagger');box.appendChild(wrap);
 wrap.appendChild(listRow({icon:meta.icon,title:meta.title,desc:meta.desc,static:true}));
 wrap.appendChild(h('div','io-cur','当前课程 · '+coMeta(curCourse()).title));
 const lab=h('div','io-lab');lab.appendChild(h('span',null,'选择文件格式'));
 const sm=sampleToggle(function(){return archiveSample(ioDetail);},'Markdown 与 CSV 内含同一份数据：Markdown 文末的「数据存档」代码块、CSV 每行 json 列，结构与 JSON 完全一致，三种格式都能重新导入。');
 lab.appendChild(sm.btn);wrap.appendChild(lab);wrap.appendChild(sm.box);
 const fm=h('div','io-format');
 IO_FORMATS.forEach(function(f){
  const b=h('button',ioFormat===f[0]?'on':'');b.appendChild(h('b',null,f[1]));b.appendChild(h('small',null,f[2]));
  b.onclick=function(){ioFormat=f[0];fm.querySelectorAll('button').forEach(function(x){x.classList.remove('on');});b.classList.add('on');upd();};fm.appendChild(b);
 });
 wrap.appendChild(fm);
 wrap.appendChild(h('div','io-lab',''));
 const chips=h('div','io-chips');wrap.appendChild(chips);
 const acts=h('div','io-actions'),exp=h('button','btn pri');acts.appendChild(exp);wrap.appendChild(acts);
 function upd(){
  chips.textContent='';
  archiveSummary(makeArchive(ioDetail)).forEach(function(r){chips.appendChild(h('span','pill',r[0]+' '+r[1]));});
  exp.textContent='预览并导出 '+(ioFormat==='md'?'Markdown':ioFormat.toUpperCase());
 }
 upd();
 exp.onclick=function(){
  const d=archiveText(ioDetail,ioFormat),name=coMeta(curCourse()).title+'-'+meta.name+'-'+today()+'.'+d.ext;
  openExport({kind:ioFormat,title:meta.title+' · '+(ioFormat==='md'?'Markdown':ioFormat.toUpperCase()),filename:name,mime:d.mime,text:d.text});
 };
}
function previewCard(res,onGo,seg){
 const o=res.obj,full=isFullScope(o);
 const card=h('div','io-prev');card.appendChild(h('h4',null,full?'将新建课程':'将导入到当前课程'));
 if(full){
  const names=o.courses.map(function(c){return String(c.title||c.name||'').trim();}).filter(Boolean);
  names.forEach(function(nm){const ex=COURSES.some(function(c){return coMeta(c).title===nm;});card.appendChild(h('p','io-msg',nm+(ex?'　→ 已有同名课程，将命名为「'+uniqueCourseTitle(nm)+'」':'　→ 作为新课程添加')));});
 }else card.appendChild(h('p','io-msg','当前课程：'+coMeta(curCourse()).title));
 const chips=h('div','io-chips');archiveSummary(o).forEach(function(r){chips.appendChild(h('span','pill',r[0]+' '+r[1]));});
 if(!chips.childElementCount)chips.appendChild(h('span','pill','没有可导入的数据'));
 card.appendChild(chips);
 if(!full&&seg)card.appendChild(seg);
 const go=h('button','btn pri',full?'导入为新课程':'导入');go.style.cssText='width:100%;margin-top:14px;padding:12px';go.onclick=onGo;card.appendChild(go);
 return card;
}
function renderImportPage(box){
 const wrap=h('div','io-page stagger');box.appendChild(wrap);
 wrap.appendChild(listRow({icon:'upload',title:'导入档案',desc:'JSON、Markdown、CSV 都可以；导入前先预览',static:true}));
 const hint=h('p','io-hint');hint.innerHTML='支持 <b>.json · .md · .csv</b>，文本编码 UTF-8。不确定怎么写？拉到下面看「格式要求与示例」，照着模板改就能导入。';wrap.appendChild(hint);
 const file=document.createElement('input');file.type='file';file.accept='.json,.md,.csv,.js,.txt,application/json,text/markdown,text/csv,text/javascript,application/javascript,text/plain';file.style.display='none';
 const drop=h('div','io-drop');drop.innerHTML=ic('upload');drop.appendChild(h('b',null,'选择文件，或拖到这里'));drop.appendChild(h('span',null,'.json · .md · .csv'));drop.appendChild(file);
 drop.style.cursor='pointer';drop.onclick=function(e){if(e.target!==file)file.click();};
 ['dragenter','dragover'].forEach(function(ev){drop.addEventListener(ev,function(e){e.preventDefault();drop.classList.add('hot');});});
 ['dragleave','drop'].forEach(function(ev){drop.addEventListener(ev,function(e){e.preventDefault();drop.classList.remove('hot');});});
 drop.addEventListener('drop',function(e){const f=e.dataTransfer&&e.dataTransfer.files&&e.dataTransfer.files[0];if(f)readFile(f);});
 wrap.appendChild(drop);
 const ta=document.createElement('textarea');ta.className='io-ta';ta.placeholder='或直接粘贴档案内容…';ta.style.marginTop='12px';ta.value=impState.text;wrap.appendChild(ta);
 const msg=h('p','io-msg','');wrap.appendChild(msg);
 const prev=h('div');wrap.appendChild(prev);
 let to=null;
 function readFile(f){const rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');impState.fname=f.name;parse();};rd.onerror=function(){setMsg('文件读取失败，请重试。',true);};rd.readAsText(f);}
 function setMsg(t,err){msg.textContent=t;msg.classList.toggle('err',!!err);}
 function parse(){
  impState.text=ta.value;prev.textContent='';
  if(!ta.value.trim()){impState.res=null;setMsg('');return;}
  const res=parseArchiveText(ta.value);impState.res=res;
  if(res.err){setMsg(res.err,true);return;}
  setMsg('已识别为 '+res.fmt+' 档案'+(impState.fname?'（'+impState.fname+'）':'')+'，确认内容后点导入。'+(res.warn?' ⚠ '+res.warn:''));
  const seg=h('div','io-seg');
  [['merge','合并 · 保留现有数据'],['cover','覆盖 · 以档案为准']].forEach(function(m){
   const b=h('button',impState.mode===m[0]?'on':'',m[1]);b.onclick=function(){impState.mode=m[0];seg.querySelectorAll('button').forEach(function(x){x.classList.remove('on');});b.classList.add('on');};seg.appendChild(b);
  });
  prev.appendChild(previewCard(res,function(){
   const full=isFullScope(res.obj);
   if(full&&!res.obj.courses.some(function(c){return String(c.title||c.name||'').trim();})){toast('课程缺少 title，无法导入');return;}
    const goImp=function(){
   const r=applyArchive(res.obj,impState.mode);
   if(full&&!r.created.length){toast('没有可导入的课程');return;}
   popCheck(full?'课程已导入':'导入完成');
   toast(full?'已新建课程：'+coMeta(COURSES.find(function(c){return c.id===r.created[0];})).title:'已导入到当前课程');
   impState={text:'',res:null,mode:'merge',fname:''};ioDetail=null;render();
    };
    if(!full&&impState.mode==='cover')uiConfirm('覆盖会用档案中的内容替换当前课程的对应数据，确定继续吗？',goImp,{ok:'覆盖'});else goImp();
  },seg));
  if(res.fmt==='Markdown')prev.appendChild(mdPreviewBlock(res.obj));
 }
 file.onchange=function(){const f=file.files&&file.files[0];if(f)readFile(f);};
 ta.oninput=function(){impState.fname='';clearTimeout(to);to=setTimeout(parse,280);};
 if(impState.text)parse();
 wrap.appendChild(guideFold({scope:'course'}));
}
function openCourseImport(){
 const body=h('div'),ta=document.createElement('textarea');ta.className='io-ta';ta.placeholder='粘贴课程档案（JSON / Markdown / CSV），或选择文件';
 const lab=h('div','io-lab');lab.style.margin='0 2px 10px';lab.appendChild(h('span',null,'导入新课程'));
 const sm=sampleToggle(function(){return archiveSample('all');},'与「数据 → 导出 → 整门课程」是同一格式：导入后新建一门独立课程，带全部章节内容和它自己的进度、划线、案例；名称与现有课程相同时自动加「副本」。');
 lab.appendChild(sm.btn);body.appendChild(lab);body.appendChild(sm.box);body.appendChild(ta);
 const row=h('div','io-actions');row.style.margin='10px 0 0';
 const pick=h('button','btn','选择文件');pick.onclick=function(){const f=document.createElement('input');f.type='file';f.accept='.json,.md,.csv,.js,.txt,application/json,text/markdown,text/csv,text/javascript,application/javascript,text/plain';f.onchange=function(){const file=f.files&&f.files[0];if(!file)return;const rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');};rd.readAsText(file);};f.click();};
 row.appendChild(pick);body.appendChild(row);
 openSheet('导入课程',body,function(){
  const res=parseArchiveText(ta.value);
  if(res.err){toast(res.err.length>26?res.err.slice(0,26)+'…':res.err);return false;}
  if(!isFullScope(res.obj)){toast('这是局部档案（划线 / 进度 / 案例），请到「我的 → 数据 → 导入档案」导入');return false;}
  const r=applyArchive(res.obj,'merge');
  if(!r.created.length){toast('没有可导入的课程（需要 title）');return false;}
  render();popCheck('已导入 '+r.created.length+' 门课程');
 },'导入');
}

/* ---------- 路由 ---------- */
function render(){
 PAGES.forEach(function(k){const el=$('v-'+k);if(el)el.classList.toggle('hide',view!==k);});
 const changed=lastView!==view;
 if(changed){document.body.classList.add('navanim');clearTimeout(navTimer);navTimer=setTimeout(function(){document.body.classList.remove('navanim');},950);}
 if(changed){AF.batch=false;AF.sel={};AF.panel=false;}
 lastView=view;
 renderAppbar();
 if(view==='mine')renderMine();
 if(view==='profile')renderProfile();
 if(view==='calendar')renderCalendar();
 if(view==='my-marks')renderMyMarks();
 if(view==='course-manage')renderCourseManage();
 if(view==='settings')renderSettings();
  if(view==='shortcuts')renderShortcuts();
  if(view==='player')renderPlayer();
 if(view==='course')renderCourse();
 if(view==='today')renderToday();
 if(view==='know')renderKnow();
 if(view==='anno')renderAnno();
 if(view==='io')renderIo();
 if(view==='review')renderReview();
 if(view==='case')renderCases();
 if(view==='gloss')renderGloss();
 if(view==='play')renderPlay();
 if(view==='prog')renderProg();
 renderTabbar();renderMini();syncAnnoBar();
 try{attachAnnotation(document);}catch(e){}
 if(changed)window.scrollTo({top:0});
 syncScrollState();
}

/* ---------- 工具箱 · 我的划线（重构） ---------- */
(function(){
 const s=function(d){return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+d+'</svg>';};
 ICONS.copy=s('<rect x="9" y="9" width="11" height="11" rx="2.2"/><path d="M5 15V6.5A2.5 2.5 0 0 1 7.5 4H15"/>');
 ICONS.locate=s('<circle cx="12" cy="12" r="6.5"/><circle cx="12" cy="12" r="2"/><path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3"/>');
 ICONS.filter=s('<path d="M4 6.5h16M7 12h10M10 17.5h4"/>');
 ICONS.checklist=s('<rect x="4" y="4" width="16" height="16" rx="4.5"/><path d="M8.5 12.3l2.5 2.5 4.6-5"/>');
})();
const AN_SORTS=[['new','新记录在前'],['old','旧记录在前'],['ch','按章节'],['note','备注长的在前']];
function anLabel(list,k){const r=list.find(function(x){return x[0]===k;});return r?r[r.length-1]:k;}
function anCaps(){
 const c=[];
 if(AF.q)c.push(['搜索：'+AF.q,function(){AF.q='';const i=$('annoSearch');if(i)i.value='';}]);
 if(AF.ch)c.push(['章节 '+AF.ch,function(){AF.ch='';}]);
 if(AF.ty)c.push([anLabel(LINE_TYPES,AF.ty),function(){AF.ty='';}]);
 if(AF.co)c.push([anLabel(LINE_COLORS,AF.co)+'色',function(){AF.co='';}]);
 if(AF.has)c.push([AF.has==='yes'?'有备注':'无备注',function(){AF.has='';}]);
 if(AF.tag)c.push(['# '+AF.tag,function(){AF.tag='';}]);
 return c;
}
function anFilterCount(){return anCaps().filter(function(x){return x[0].indexOf('搜索')!==0;}).length;}
function openAnnoSort(){
 const body=h('div');
 AN_SORTS.forEach(function(o){
  const r=h('button','pf-row'),p=h('span','pi');p.innerHTML=ic(AF.sort===o[0]?'check':'sort');r.appendChild(p);r.appendChild(h('span','tx',o[1]));
  r.onclick=function(){AF.sort=o[0];closeSheet();renderAnno();};body.appendChild(r);
 });
 openSheet('排序',body);
}
function anSelList(){return annoFiltered().filter(function(m){return AF.sel[m.id];});}
function annoExport(){
 const list=annoFiltered(),chosen=list.filter(function(m){return AF.sel[m.id];}),use=chosen.length?chosen:list;
 if(!use.length){toast('没有可导出的划线');return;}
 openExport({kind:'txt',title:'划线与备注摘录 · '+use.length+' 条',filename:'桌面运维-划线摘录-'+today()+'.txt',mime:'text/plain',text:marksText(use)});
}
function annoDelete(){
 const n=Object.keys(AF.sel).length;
 if(!n){toast('还没有选择划线');return;}
 uiConfirm('确认删除选中的 '+n+' 条划线与备注？',function(){
 S.marks=(S.marks||[]).filter(function(m){return !AF.sel[m.id];});
 AF.sel={};AF.batch=false;save();rerenderMarks();toast('已删除 '+n+' 条');
 if(view==='anno')renderAnno();
 },{ok:'删除',danger:true});
}
function annoExitBatch(){AF.batch=false;AF.sel={};refreshAnno();}
function syncAnnoBar(){
 let bar=$('anBar');
 if(view!=='anno'||!AF.batch){if(bar)bar.remove();document.body.classList.remove('has-anbar');return;}
 document.body.classList.add('has-anbar');
 if(!bar){bar=h('div','anbar');bar.id='anBar';document.body.appendChild(bar);}
 bar.textContent='';
 const inn=h('div','in'),n=Object.keys(AF.sel).length,total=annoFiltered().length;
 const sa=h('button','sa'+(n&&n===total?' on':''));sa.innerHTML=ic('checklist');sa.setAttribute('aria-label','全选');
 sa.onclick=function(){const all=annoFiltered();if(n===all.length)AF.sel={};else all.forEach(function(m){AF.sel[m.id]=1;});refreshAnno();};
 inn.appendChild(sa);inn.appendChild(h('span','sel',n?'已选 '+n+' 条':'未选择'));
 const del=h('button','del');del.innerHTML=ic('trash');del.setAttribute('aria-label','删除所选');del.onclick=annoDelete;
 const exp=h('button','exp');exp.innerHTML=ic('download');exp.appendChild(document.createTextNode(n?'导出 '+n+' 条':'导出全部'));exp.onclick=annoExport;
 inn.appendChild(del);inn.appendChild(exp);bar.appendChild(inn);
}
function annoCard(m){
 const w=h('div','an-card'+(AF.sel[m.id]?' picked':''));
 if(AF.batch){const ck=h('span','an-ck');ck.innerHTML=ic('check');w.appendChild(ck);}
 const bd=h('div','an-body'),qt=h('div','an-qt');
 (m.segs||[{txt:m.t}]).forEach(function(sg,ii,arr){
  const sp=document.createElement('span');sp.className='hlx ty-'+(m.ty||'solid')+' co-'+(m.co||'b');sp.style.textUnderlineOffset='0.16em';sp.textContent=sg.txt;qt.appendChild(sp);
  if(ii<arr.length-1)qt.appendChild(h('span','sepm',' ／ '));
 });
 bd.appendChild(qt);
 if(m.n)bd.appendChild(h('div','an-nt',m.n));
 const ft=h('div','an-foot');
 ft.appendChild(h('span',null,new Date(m.at||Date.now()).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'})));
 if(!AF.batch){
  const ops=h('div','an-ops');
  [['pen','编辑备注',function(){openEditor(m.id);}],['locate','定位原文',function(){locateMark(m.id);}],['copy','复制',function(){copyText(annoItemText(m),'这条已复制');}]].forEach(function(o){
   const b=h('button');b.innerHTML=ic(o[0]);b.setAttribute('aria-label',o[1]);b.title=o[1];b.onclick=function(e){e.stopPropagation();o[2]();};ops.appendChild(b);
  });
  ft.appendChild(ops);
 }
 bd.appendChild(ft);w.appendChild(bd);
 /* 长按进入批量模式；批量模式下点按卡片即勾选 */
 let t=null,sx=0,sy=0,fired=false;
 w.addEventListener('pointerdown',function(e){if(AF.batch||e.target.closest('button'))return;fired=false;sx=e.clientX;sy=e.clientY;
  t=setTimeout(function(){fired=true;AF.batch=true;AF.sel={};AF.sel[m.id]=1;try{if(navigator.vibrate)navigator.vibrate(12);}catch(_){}refreshAnno();},560);});
 ['pointerup','pointercancel','pointerleave'].forEach(function(ev){w.addEventListener(ev,function(){clearTimeout(t);});});
 w.addEventListener('pointermove',function(e){if(Math.abs(e.clientX-sx)>8||Math.abs(e.clientY-sy)>8)clearTimeout(t);});
 w.addEventListener('contextmenu',function(e){e.preventDefault();});
 w.addEventListener('click',function(e){
  if(fired){fired=false;return;}
  if(!AF.batch)return;
  if(AF.sel[m.id])delete AF.sel[m.id];else AF.sel[m.id]=1;
  w.classList.toggle('picked',!!AF.sel[m.id]);anCountLine();syncAnnoBar();
 });
 return w;
}
function anCountLine(){
 const row=$('anCount');if(!row)return;row.textContent='';
 const all=(S.marks||[]).length,list=annoFiltered();
 if(AF.batch){
  row.appendChild(h('span',null,'批量选择 · 当前 '+list.length+' 处'));
  const b=h('button','lnk','完成');b.onclick=annoExitBatch;row.appendChild(b);
 }else row.appendChild(h('span',null,list.length===all?all+' 处划线':'筛选出 '+list.length+' / 共 '+all+' 处'));
}
function refreshAnno(){
 const caps=$('anCaps');if(caps){
  caps.textContent='';const cs=anCaps();
  cs.forEach(function(c){const b=h('button','cap');b.appendChild(document.createTextNode(c[0]));const x=h('span');x.innerHTML=ic('close');b.appendChild(x.firstChild);b.setAttribute('aria-label','移除 '+c[0]);b.onclick=function(){c[1]();AF.sel={};renderAnno();};caps.appendChild(b);});
  if(cs.length>1){const b=h('button','cap clr','清除全部');b.onclick=function(){AF.ch=AF.ty=AF.co=AF.has=AF.tag=AF.q='';AF.sel={};renderAnno();};caps.appendChild(b);}
 }
 const fb=$('anFilterBtn');if(fb){fb.classList.toggle('act',anFilterCount()>0||!!AF.panel);const d=fb.querySelector('.dot');if(anFilterCount()&&!d)fb.appendChild(h('i','dot'));if(!anFilterCount()&&d)d.remove();}
 anCountLine();
 const box=$('annoList');if(box){
  box.textContent='';
  const list=annoFiltered();
  if(!list.length){box.appendChild(h('div','pl-empty','没有符合条件的划线，去掉一个筛选条件试试。'));}
  else{
   const by={};list.forEach(function(m){(by[m.ch]=by[m.ch]||[]).push(m);});
   Object.keys(by).forEach(function(k){
    const c0=chById(k);box.appendChild(h('div','an-gh',k+' '+(c0?c0.t:'')));
    by[k].forEach(function(m){box.appendChild(annoCard(m));});
   });
  }
  box.classList.remove('swap');void box.offsetWidth;box.classList.add('swap');
 }
 syncAnnoBar();
}
function renderAnno(){
 const sec=$('v-anno');if(!sec)return;
 sec.textContent='';
 if(!(S.marks||[]).length){
  AF.batch=false;AF.sel={};syncAnnoBar();
  sec.appendChild(h('div','pl-empty','还没有划线。在「今日学习」正文里选中一段话，就能划线并写备注。'));return;
 }
 const top=h('div','an-top');
 const sw=h('div','searchwrap');sw.innerHTML=ic('search');
 const inp=document.createElement('input');inp.type='text';inp.id='annoSearch';inp.placeholder='搜索划线、备注、标签';inp.value=AF.q||'';inp.autocomplete='off';
 sw.appendChild(inp);
 const clr=h('button','sclear'+(AF.q?' on':''));clr.innerHTML=ic('close');clr.onclick=function(){inp.value='';AF.q='';clr.classList.remove('on');refreshAnno();inp.focus();};sw.appendChild(clr);
 inp.oninput=function(){AF.q=inp.value.trim();clr.classList.toggle('on',!!inp.value);AF.sel={};refreshAnno();};
 top.appendChild(sw);
 top.appendChild(iconBtn('sort','排序：'+anLabel(AN_SORTS,AF.sort),openAnnoSort));
 const fb=iconBtn('filter','筛选',function(){AF.panel=!AF.panel;panel.classList.toggle('on',AF.panel);fb.classList.toggle('act',AF.panel||anFilterCount()>0);});fb.id='anFilterBtn';top.appendChild(fb);
 top.appendChild(iconBtn('checklist','批量选择',function(){AF.batch=true;AF.sel={};refreshAnno();}));
 sec.appendChild(top);
 const panel=h('div','smpbox'+(AF.panel?' on':'')),pin=h('div','smpin'),grid=h('div','an-filters');
 const mk=function(label,opts,key){
  const w=document.createElement('label'),sp=document.createElement('span');sp.textContent=label;w.appendChild(sp);
  const sel=document.createElement('select');opts.forEach(function(o){const op=document.createElement('option');op.value=o[0];op.textContent=o[1];sel.appendChild(op);});
  sel.value=AF[key];sel.onchange=function(){AF[key]=sel.value;AF.sel={};refreshAnno();};w.appendChild(sel);grid.appendChild(w);
 };
 mk('章节',[['','全部章节']].concat(CH.map(function(c){return [c.id,c.id+' '+c.t];})),'ch');
 mk('线型',[['','全部线型']].concat(LINE_TYPES.map(function(t){return [t[0],t[1]];})),'ty');
 mk('颜色',[['','全部颜色']].concat(LINE_COLORS.map(function(c){return [c[0],c[2]];})),'co');
 mk('备注',[['','不限'],['yes','只看有备注'],['no','只看无备注']],'has');
 mk('标签',[['','全部标签']].concat(allTags().map(function(t){return [t,t];})),'tag');
 pin.appendChild(grid);panel.appendChild(pin);sec.appendChild(panel);
 const caps=h('div','an-caps');caps.id='anCaps';sec.appendChild(caps);
 const cnt=h('div','an-count');cnt.id='anCount';sec.appendChild(cnt);
 const list=h('div');list.id='annoList';sec.appendChild(list);
 refreshAnno();
}

/* ---------- 初始化 ---------- */
/* =====================================================================
   v9 · 补丁层（A）：编辑图标 / 竖线背景 / 全面屏状态栏 / 表格等分 / 设置
   ===================================================================== */

/* 编辑图标：与参考图右上角同款——圆角方框缺右上角 + 斜向实心箭头。所有「编辑」入口共用这一个 */
ICONS.pen='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"><path d="M10.6 5H6.2A2.2 2.2 0 0 0 4 7.2v10.6A2.2 2.2 0 0 0 6.2 20h10.6a2.2 2.2 0 0 0 2.2-2.2v-4.4"/><path d="M10.6 13.4L19.3 4.7"/><path d="M14.6 3.4h6v6z" fill="currentColor"/></svg>';

/* ---------- 顶部状态栏留白（全面屏） ---------- */
const SAT_OPTS=[['auto','自动'],['0','不留白'],['24','紧凑'],['32','标准'],['44','宽松']];
function applySat(){
 /* 手动留白只对「我的」「设置」两个界面生效，其子界面与其他页面一律用系统安全区 */
 const root=document.documentElement,v=(S.ui&&S.ui.sat!=null)?String(S.ui.sat):'auto';
 const scoped=(typeof view!=='undefined')&&(view==='profile'||view==='settings');
 if(v==='auto'||!scoped)root.style.removeProperty('--sat-min');
 else root.style.setProperty('--sat-min',(Number(v)||0)+'px');
}
function detectImmersive(){
 let full=false;
 try{
  const coarse=window.matchMedia&&window.matchMedia('(pointer:coarse)').matches;
  /* 触屏设备上，可视高度 == 屏幕高度，说明状态栏浮在页面之上（沉浸式 / 全屏） */
  full=!!coarse&&Math.abs(window.innerHeight-screen.height)<=2&&Math.min(window.innerWidth,screen.width)<=900;
 }catch(e){}
 document.documentElement.classList.toggle('immersive',full);
}

/* ---------- 星球版竖线背景 ---------- */
function buildPlanetLines(){
 const host=document.getElementById('plbg');if(!host)return;
 const W=Math.round(Math.min(document.documentElement.clientWidth||window.innerWidth||390,640));
 let seed=20261108;
 const rnd=function(){seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed/4294967296;};
 let x=1,out='';
 while(x<W){
  x+=2.6+rnd()*2.4;                       /* 间距 2.6~5px */
  const r=rnd();let hf,op;
  if(r<.32){hf=.84+rnd()*.16;op=.05+rnd()*.04;}          /* 长而淡：托底 */
  else if(r<.7){hf=.46+rnd()*.32;op=.08+rnd()*.05;}       /* 中长：主体 */
  else{hf=.18+rnd()*.26;op=.12+rnd()*.07;}               /* 短而深：靠近顶部，形成层次 */
  out+='<rect x="'+x.toFixed(1)+'" y="0" width="'+(.9+rnd()*.7).toFixed(2)+'" height="'+(200*hf).toFixed(1)+'" fill-opacity="'+op.toFixed(3)+'"/>';
 }
 host.innerHTML='<svg width="'+W+'" viewBox="0 0 '+W+' 200" preserveAspectRatio="none" shape-rendering="crispEdges" xmlns="http://www.w3.org/2000/svg">'+out+'</svg>';
}
function ensureShell(){
 if(!document.getElementById('statusfill')){const s=document.createElement('div');s.className='statusfill';s.id='statusfill';s.setAttribute('aria-hidden','true');document.body.insertBefore(s,document.body.firstChild);}
 const app=document.querySelector('.app');
 if(app&&!document.getElementById('plbg')){const p=document.createElement('div');p.className='plbg';p.id='plbg';p.setAttribute('aria-hidden','true');app.insertBefore(p,app.firstChild);}
}
function syncSatSolid(){document.documentElement.classList.toggle('sat-solid',(window.scrollY||0)>4);}

/* ---------- 表格：按内容长度给每一列分配宽度，限制最宽 / 最窄 ---------- */
function cellLen(t){
 t=String(t||'').replace(/\s+/g,' ').trim();let n=0;
 for(const ch of t)n+=/[\u2e80-\u9fff\uff00-\uffef]/.test(ch)?1:0.82;
 return n;
}
function clampNorm(w,lo,hi){
 const n=w.length,fixed=new Array(n).fill(false);let p=w.slice();
 for(let it=0;it<10;it++){
  const fixedSum=p.reduce(function(s,x,i){return fixed[i]?s+x:s;},0);
  const freeW=w.reduce(function(s,x,i){return fixed[i]?s:s+x;},0)||1;
  let changed=false;
  p=w.map(function(x,i){return fixed[i]?p[i]:x/freeW*(100-fixedSum);});
  p.forEach(function(x,i){
   if(fixed[i])return;
   if(x<lo){p[i]=lo;fixed[i]=true;changed=true;}
   else if(x>hi){p[i]=hi;fixed[i]=true;changed=true;}
  });
  if(!changed)break;
 }
 const sum=p.reduce(function(s,x){return s+x;},0)||1;
 return p.map(function(x){return x/sum*100;});
}
function fitTable(tb){
 const rows=Array.from(tb.rows);if(!rows.length)return;
 const PRESET={gateBody:[25,36,17,22],cmdBody:[38,46,16],evBody:[11,19,30,40],glBody:[17,23,38,13,9]};
 let fixedCols=tb.dataset.cols?tb.dataset.cols.split(',').map(Number):null;
 Object.keys(PRESET).forEach(function(k){if(tb.querySelector('#'+k))fixedCols=PRESET[k];});
 const head=(tb.tHead&&tb.tHead.rows[0])||null;
 const n=Math.max.apply(null,rows.map(function(r){return r.cells.length;}));
 if(n<2){tb.style.minWidth='';return;}
 const body=rows.filter(function(r){return !(tb.tHead&&r.parentNode===tb.tHead);}).slice(0,80);
 const w=[];
 for(let j=0;j<n;j++){
  let sum=0,cnt=0,mx=0,ctl=false;
  body.forEach(function(r){
   const c=r.cells[j];if(!c)return;
   if(c.querySelector('select,button,input'))ctl=true;
   const L=Math.min(cellLen(c.textContent),46);sum+=L;cnt++;mx=Math.max(mx,L);
  });
  const hl=head&&head.cells[j]?cellLen(head.cells[j].textContent):0;
  const avg=cnt?sum/cnt:0;
  w[j]=ctl?Math.max(hl+4,9):Math.max(hl+3,avg*0.7+mx*0.3,5);
 }
 if(fixedCols&&fixedCols.length===n)w.splice(0,n,...fixedCols);
 const lo=n>=5?15:Math.max(13,52/n),hi=Math.min(62,190/n);
 const p=clampNorm(w,lo,hi);
 let cg=tb.querySelector(':scope>colgroup');
 if(cg)cg.remove();
 cg=document.createElement('colgroup');
 p.forEach(function(x){const c=document.createElement('col');c.style.width=x.toFixed(2)+'%';cg.appendChild(c);});
 tb.insertBefore(cg,tb.firstChild);
 const MINW={gateBody:560,evBody:500,glBody:460,cmdBody:0};
 let mw=n*60;Object.keys(MINW).forEach(function(k){if(tb.querySelector('#'+k))mw=MINW[k]||mw;});
 tb.style.minWidth=mw+'px';
}
let fitQ=0;
function fitAll(){document.querySelectorAll('table').forEach(function(t){try{fitTable(t);}catch(e){}});}
function scheduleFit(){if(fitQ)return;fitQ=requestAnimationFrame(function(){fitQ=0;fitAll();});}
function fitRelevant(m){
 if(m.type!=='childList')return false;
 const t=m.target,tag=t.tagName;
 if(tag==='COLGROUP'||tag==='COL')return false;
 const added=Array.from(m.addedNodes);
 if(tag==='TABLE'&&added.every(function(n){return n.tagName==='COLGROUP';}))return false;
 if(tag==='TBODY'||tag==='THEAD'||tag==='TABLE')return true;
 return added.some(function(n){return n.nodeType===1&&(n.tagName==='TABLE'||(n.querySelector&&n.querySelector('table')));});
}

/* ---------- 设置：新增「顶部状态栏留白」 ---------- */
function satPanel(){
 const p=h('div','clean-panel sat-row');
 const top=h('div','sat-top');top.appendChild(h('span','sat-lab','顶部留白'));
 const seg=h('div','io-seg sat'),cur=(S.ui&&S.ui.sat!=null)?String(S.ui.sat):'auto';
 SAT_OPTS.forEach(function(o){
  const b=h('button',cur===o[0]?'on':'',o[1]);
  b.onclick=function(){S.ui.sat=o[0];save();applySat();seg.querySelectorAll('button').forEach(function(x){x.classList.remove('on');});b.classList.add('on');};
  seg.appendChild(b);
 });
 top.appendChild(seg);p.appendChild(top);
 p.appendChild(h('p','muted','仅「我的」和「设置」页生效'));
 return p;
}

/* ---------- v10：导入页精简（粘贴框折叠、去掉重复标题与说明） ---------- */
function tidyImport(){
 const w=document.querySelector('#v-io .io-page');
 if(!w)return;
 const isImp=(typeof ioDetail!=='undefined'&&ioDetail==='import');
 w.classList.toggle('imp',isImp);
 if(!isImp)return;
 const ta=w.querySelector('textarea.io-ta');
 if(ta&&!w.querySelector('details.imp-paste')){
  const d=document.createElement('details');d.className='imp-paste';
  const s=document.createElement('summary');s.textContent='或粘贴内容';d.appendChild(s);
  ta.parentNode.insertBefore(d,ta);d.appendChild(ta);
  if(ta.value.trim())d.open=true;
 }
}

/* =====================================================================
   v11 · 补丁层（A）：划线可多次唤起 / Markdown 章节渲染 / 讲义全量显示 / 导入修正
   ===================================================================== */

/* ---------- 划线工具条：同一界面可以反复唤起 ----------
   原逻辑在完成第一次划线后一直保留 pendRec，导致后续选区不再弹出工具条。
   现在：只要选中的是另一段文字，就开始一条新划线并再次显示工具条。 */
function sameSel(rec,segs){
 if(!rec||!rec.segs||!segs||rec.segs.length!==segs.length)return false;
 for(let i=0;i<segs.length;i++){
  const a=rec.segs[i],b=segs[i];
  if(a.sig!==b.el.dataset.sig||a.s!==b.a||a.e!==b.b)return false;
 }
 return true;
}
function checkSelection(){
 const p=document.getElementById('notePanel');
 if(p&&p.classList.contains('on'))return;
 const segs=collectSel();
 if(!segs){if(!pendRec)hideBar();return;}
 if(pendRec&&sameSel(pendRec,segs))return;
 pendRec=null;
 showBarAtSelection(segs);
}

/* ---------- 行内格式：把 **粗体** `代码` *斜体* ~~删除~~ [链接](url) 解析成「纯文本 + 格式区间」 ---------- */
function mdInline(src){
 let text='';const fmt=[];
 function run(str){
  const re=/\*\*([^*]+?)\*\*|__([^_]+?)__|`([^`]+)`|~~([^~]+?)~~|\[([^\]]+)\]\((https?:[^)\s]+)\)|\*([^*\s][^*]*?)\*/g;
  let last=0,m;
  while((m=re.exec(str))){
   text+=str.slice(last,m.index);
   const s=text.length;
   if(m[1]!=null||m[2]!=null){run(m[1]!=null?m[1]:m[2]);fmt.push({s:s,e:text.length,t:'strong'});}
   else if(m[3]!=null){text+=m[3];fmt.push({s:s,e:text.length,t:'code'});}
   else if(m[4]!=null){run(m[4]);fmt.push({s:s,e:text.length,t:'del'});}
   else if(m[5]!=null){run(m[5]);fmt.push({s:s,e:text.length,t:'a',h:m[6]});}
   else{run(m[7]);fmt.push({s:s,e:text.length,t:'em'});}
   last=re.lastIndex;
  }
  text+=str.slice(last);
 }
 run(String(src==null?'':src));
 return {text:text,fmt:fmt.filter(function(f){return f.e>f.s;})};
}
function fmtWrap(node,s,e,fmt){
 const cov=fmt.filter(function(f){return f.s<=s&&f.e>=e;}).sort(function(a,b){return (a.e-a.s)-(b.e-b.s);});
 cov.forEach(function(f){
  const el=document.createElement(f.t);
  if(f.t==='a'){el.href=f.h;el.target='_blank';el.rel='noopener noreferrer';}
  el.appendChild(node);node=el;
 });
 return node;
}
function paintFmt(el,text,fmt){
 el.textContent='';
 if(!fmt.length){el.appendChild(document.createTextNode(text));return;}
 const pts={0:1};pts[text.length]=1;fmt.forEach(function(f){pts[f.s]=1;pts[f.e]=1;});
 const b=Object.keys(pts).map(Number).filter(function(n){return n>=0&&n<=text.length;}).sort(function(p,q){return p-q;});
 for(let i=0;i<b.length-1;i++){
  if(b[i+1]<=b[i])continue;
  el.appendChild(fmtWrap(document.createTextNode(text.slice(b[i],b[i+1])),b[i],b[i+1],fmt));
 }
}
/* 重写 paintBlock：划线高亮的同时保留粗体 / 代码等行内格式 */
function paintBlock(el){
 const text=blockText(el),hits=resolveSeg(el);
 let fmt=[];try{fmt=JSON.parse(el.dataset.fmt||'[]');}catch(e){}
 el.textContent='';
 if(!hits.length&&!fmt.length){el.appendChild(document.createTextNode(text));return;}
 const pts={0:1};pts[text.length]=1;
 hits.forEach(function(x){pts[x.a]=1;pts[x.b]=1;});fmt.forEach(function(f){pts[f.s]=1;pts[f.e]=1;});
 const b=Object.keys(pts).map(Number).filter(function(n){return n>=0&&n<=text.length;}).sort(function(p,q){return p-q;});
 for(let i=0;i<b.length-1;i++){
  const s=b[i],e=b[i+1];if(e<=s)continue;
  const cov=hits.filter(function(x){return x.a<=s&&x.b>=e;});
  let node=document.createTextNode(text.slice(s,e));
  for(let k=cov.length-1;k>=0;k--){
   const m=cov[k].m,sp=document.createElement('span');
   sp.className='hlx ty-'+(m.ty||'solid')+' co-'+(m.co||'b')+((k===0&&m.n)?' has-note':'');
   sp.dataset.mid=m.id;
   sp.style.textUnderlineOffset=(0.14+0.24*(cov.length-1-k)).toFixed(2)+'em';
   sp.title=((LINE_TYPES.find(function(x){return x[0]===(m.ty||'solid');})||['','横线'])[1])+' · '+((LINE_COLORS.find(function(x){return x[0]===(m.co||'b');})||['','','蓝'])[2])+(m.tags&&m.tags.length?' · '+m.tags.join('/'):'')+(m.n?' · 有备注 · 点击编辑':' · 点击加备注');
   sp.appendChild(node);node=sp;
  }
  el.appendChild(fmtWrap(node,s,e,fmt));
 }
}

/* ---------- Markdown → DOM（不使用 innerHTML，天然安全；可划线块带 data-blk） ---------- */
function looksMd(s){
 s=String(s||'');
 return /^#{1,6}\s+\S/m.test(s)||/^\s*\|.+\|\s*$/m.test(s)&&/^\s*\|?\s*:?-{3,}/m.test(s)||/\*\*[^*\n]+\*\*/.test(s)||/^\s*[-*+]\s+\S/m.test(s)||/^```/m.test(s)||/^\s*>\s?\S/m.test(s)||/^\s*\d+[.)]\s+\S/m.test(s);
}
function mdSplitRow(line){
 let s=line.trim();if(s[0]==='|')s=s.slice(1);if(s[s.length-1]==='|'&&s[s.length-2]!=='\\')s=s.slice(0,-1);
 const out=[];let cur='';
 for(let i=0;i<s.length;i++){
  if(s[i]==='\\'&&s[i+1]==='|'){cur+='|';i++;}
  else if(s[i]==='|'){out.push(cur.trim());cur='';}
  else cur+=s[i];
 }
 out.push(cur.trim());return out;
}
function mdBlocks(src,chId,kp){
 const frag=document.createDocumentFragment(),lines=String(src||'').replace(/\r/g,'').split('\n');
 let i=0,n=0;
 function mkBlock(tag,cls,text){
  const inl=mdInline(text),el=h(tag,cls);
  el.textContent=inl.text;
  if(inl.fmt.length)el.dataset.fmt=JSON.stringify(inl.fmt);
  if(chId)hl(el,chId,kp+(n++));
  return {el:el,inl:inl};
 }
 const isHead=function(l){return /^#{1,6}\s+\S/.test(l);};
 const isFence=function(l){return /^\s*```/.test(l);};
 const isHr=function(l){return /^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(l);};
 const isList=function(l){return /^\s*([-*+]|\d+[.)])\s+\S/.test(l);};
 const isTbl=function(k){return k+1<lines.length&&lines[k].trim()[0]==='|'&&/^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/.test(lines[k+1]);};
 while(i<lines.length){
  const line=lines[i],t=line.trim();
  if(!t){i++;continue;}
  if(isFence(line)){
   const buf=[];i++;
   while(i<lines.length&&!isFence(lines[i])){buf.push(lines[i]);i++;}
   i++;frag.appendChild(h('pre','md-code',buf.join('\n')));continue;
  }
  let m=line.match(/^(#{1,6})\s+(.+?)\s*#*\s*$/);
  if(m){
   const lv=m[1].length,e=h(lv<=2?'h3':lv===3?'h4':'h5','md-h'+Math.min(lv,5));
   e.textContent=mdInline(m[2]).text;frag.appendChild(e);i++;continue;
  }
  if(isHr(line)){frag.appendChild(h('hr','md-hr'));i++;continue;}
  if(isTbl(i)){
   const head=mdSplitRow(lines[i]);i+=2;
   const rows=[];
   while(i<lines.length&&lines[i].trim()[0]==='|'){rows.push(mdSplitRow(lines[i]));i++;}
   const wrap=h('div','scroller'),tb=h('table','plain md-table'),th=h('thead'),tr=h('tr');
   head.forEach(function(x){const c=h('th');const inl=mdInline(x);paintFmt(c,inl.text,inl.fmt);tr.appendChild(c);});
   th.appendChild(tr);tb.appendChild(th);
   const body=h('tbody');
   rows.forEach(function(r){
    const t2=h('tr');
    for(let j=0;j<head.length;j++){const c=h('td');const inl=mdInline(r[j]||'');paintFmt(c,inl.text,inl.fmt);t2.appendChild(c);}
    body.appendChild(t2);
   });
   tb.appendChild(body);wrap.appendChild(tb);frag.appendChild(wrap);continue;
  }
  if(t[0]==='>'){
   const buf=[];
   while(i<lines.length&&lines[i].trim()[0]==='>'){buf.push(lines[i].trim().replace(/^>\s?/,''));i++;}
   const text=buf.join('\n').trim();
   const b=mkBlock('div','md-quote'+(/^[\u{1F4A1}\u26A0\u2757\u2705\u{1F4CC}\u{1F511}\u2B50\u{1F6A8}]/u.test(text)?' md-callout':''),text);
   frag.appendChild(b.el);continue;
  }
  if(isList(line)){
   const ordered=/^\s*\d+[.)]/.test(line),box=h('div','md-list '+(ordered?'md-ol':'md-ul'));
   let item=null;
   while(i<lines.length){
    const l=lines[i];
    if(isList(l)){
     const mm=l.match(/^(\s*)([-*+]|\d+[.)])\s+(.*)$/),lvl=Math.min(2,Math.floor(mm[1].replace(/\t/g,'  ').length/2));
     if(item)box.appendChild(mkBlock('div','md-li lv'+item.lvl,item.text).el);
     item={lvl:lvl,text:mm[3]};i++;
    }else if(l.trim()&&/^\s{2,}\S/.test(l)&&!isFence(l)&&item){item.text+='\n'+l.trim();i++;}
    else if(!l.trim()&&i+1<lines.length&&isList(lines[i+1])){i++;}
    else break;
   }
   if(item)box.appendChild(mkBlock('div','md-li lv'+item.lvl,item.text).el);
   frag.appendChild(box);continue;
  }
  const buf=[];
  while(i<lines.length&&lines[i].trim()&&!isHead(lines[i])&&!isFence(lines[i])&&!isHr(lines[i])&&!isList(lines[i])&&lines[i].trim()[0]!=='>'&&!isTbl(i)){buf.push(lines[i].trim());i++;}
  if(!buf.length){i++;continue;}
  const b=mkBlock('p','pp md-p',buf.join('\n'));
  const f=b.inl.fmt;
  if(f.length===1&&f[0].t==='strong'&&f[0].s===0&&f[0].e===b.inl.text.length)b.el.classList.add('md-sub');
  frag.appendChild(b.el);
 }
 return frag;
}
/* 导入预览里的「排版预览」：放在预览最后，默认折叠 */
function mdPreviewBlock(obj){
 const co=obj&&obj.courses&&obj.courses[0],frag=document.createDocumentFragment();
 if(!co)return frag;
 const ch=(co.chapters||[]).find(function(c){return c&&c.content&&looksMd(c.content);});
 if(!ch)return frag;
 const d=h('details','md-prev'),s=h('summary');
 s.appendChild(h('span',null,'排版预览'));s.appendChild(h('small',null,String(ch.id||'')+' '+String(ch.title||ch.t||'')));d.appendChild(s);
 const body=h('div','md-prev-body mdroot');
 body.appendChild(mdBlocks(String(ch.content).slice(0,5000),null,'V'));
 d.appendChild(body);frag.appendChild(d);return frag;
}

/* ---------- 讲义渲染：兼容多种章节数据结构，保证内容完整显示 ----------
   支持：concept 行 {p} {ul} {tbl} {md} {t,parts[]}；practice 项 {t,cmd,d,out,read} 或 {t,parts[]}；guide 项 {w,c,r} 或 {t,d} */
function partsInto(w,parts,chId,kp){
 let stepN=0;
 (parts||[]).forEach(function(pt,j){
  if(pt==null)return;
  const k=kp+'_'+j;
  if(typeof pt==='string'){stepN=0;const p=h('p','pp');p.textContent=pt;w.appendChild(hl(p,chId,k));return;}
  if(pt.p!=null){stepN=0;const p=h('p','pp');p.textContent=String(pt.p);w.appendChild(hl(p,chId,k));}
  else if(pt.step!=null){stepN++;const r=h('div','pstep');r.appendChild(h('em',null,String(stepN)));const sp=h('span','pstext');sp.textContent=String(pt.step);hl(sp,chId,k);r.appendChild(sp);w.appendChild(r);}
  else if(pt.table&&pt.table.length){stepN=0;w.appendChild(buildTbl({head:pt.table[0]||[],rows:pt.table.slice(1)}));}
  else if(pt.tbl){stepN=0;w.appendChild(buildTbl(pt.tbl));}
  else if(pt.ul){stepN=0;const ul=h('ul','pl');pt.ul.forEach(function(x){const li=h('li');li.textContent=String(x);ul.appendChild(li);});w.appendChild(ul);}
  else if(pt.code!=null){stepN=0;w.appendChild(h('pre','md-code',String(pt.code)));}
  else if(pt.md!=null){stepN=0;w.appendChild(mdBlocks(String(pt.md),chId,k+'m'));}
 });
}
function renderLong(cb,c){
 if(S.mode!=='long'&&S.mode!=='lite'){S.mode='long';save();}   /* 导入档案里的未知显示模式（如 list）会把讲义全部隐藏 */
 const L=LONG[c.id],d=L||synth(c),chId=c.id;
 const concept=Array.isArray(d.concept)?d.concept:[],practice=Array.isArray(d.practice)?d.practice:[],guide=Array.isArray(d.guide)?d.guide:[];
 const quiz=(d.quiz&&typeof d.quiz==='object')?d.quiz:{q:'',a:''};
 const isMd=concept.length>0&&concept.every(function(r){return r&&r.md!=null;});
 if(isMd){
  const s=h('div','lsec mdsec mdroot');
  concept.forEach(function(r,ri){s.appendChild(mdBlocks(r.md,chId,'M'+ri+'_'));});
  cb.appendChild(s);
 }else{
  /* ① 概念 */
  const s1=h('div','lsec');
  const t1=h('div','lt');t1.appendChild(h('span','no','概'));t1.appendChild(h3s('概念','讲透它到底解决什么问题'));s1.appendChild(t1);
  if(L&&L.title){const ti=h('p','pp');ti.appendChild(document.createTextNode(L.title));ti.style.fontWeight='var(--fw-h)';s1.appendChild(hl(ti,chId,'T'));}
  concept.forEach(function(row,i){
   if(!row)return;
   const w=h('div','cseg'+(i>=2?' deep':''));
   if(row.t&&row.parts){const ct=h('h4','ctitle');ct.textContent=String(row.t);w.appendChild(ct);}
   if(row.p){const p=h('p','pp');p.textContent=row.p;w.appendChild(hl(p,chId,'C'+i));}
   if(row.ul){const ul=h('ul','pl');row.ul.forEach(function(x){const li=h('li');li.textContent=x;ul.appendChild(li);});w.appendChild(ul);}
   if(row.tbl){w.appendChild(buildTbl(row.tbl));}
   if(row.md!=null){w.appendChild(mdBlocks(String(row.md),chId,'C'+i+'m'));}
   if(row.parts)partsInto(w,row.parts,chId,'C'+i);
   s1.appendChild(w);
  });
  cb.appendChild(s1);
  /* ② 实操 */
  if(practice.length){
   const s2=h('div','lsec');
   const t2=h('div','lt');t2.appendChild(h('span','no','操'));t2.appendChild(h3s('实操','命令、图形界面路径与预期输出'));s2.appendChild(t2);
   practice.forEach(function(st,i){
    st=st||{};
    const w=h('div','step');
    const hd=h('div','st');hd.appendChild(h('em',null,String(i+1)));
    hd.appendChild(h('span',null,st.t||''));w.appendChild(hd);
    if(st.parts){partsInto(w,st.parts,chId,'P'+i);}
    else{
     if(st.d){const p=h('p');p.textContent=st.d;w.appendChild(hl(p,chId,'P'+i+'d'));}
     if(st.cmd!=null)w.appendChild(h('pre',null,st.cmd));
     if(st.out){const lab=h('div','lab det','输出样例');lab.style.fontSize='12px';lab.style.color='var(--sub)';lab.style.marginTop='8px';w.appendChild(lab);w.appendChild(h('pre','det',st.out));}
     if(st.read){const rd=h('div','rd det','怎么读：'+st.read);w.appendChild(hl(rd,chId,'P'+i+'r'));}
     if(st.cmd!=null){
      const bb=h('div','btns');
      const cp=h('button','btn sm','复制这条命令');cp.onclick=function(){copyText(st.cmd,'命令已复制');};bb.appendChild(cp);
      const cp2=h('button','btn sm','复制本节要点');
      cp2.onclick=function(){copyText(['■ '+st.t,st.d||'','$ '+st.cmd,st.out?('→ '+st.out):'',st.read?('读法：'+st.read):''].filter(Boolean).join('\n'),'要点已复制');};
      bb.appendChild(cp2);w.appendChild(bb);
     }
    }
    s2.appendChild(w);
   });
   cb.appendChild(s2);
  }
  /* ③ 指南 */
  if(guide.length){
   const s3=h('div','lsec');
   const t3=h('div','lt');t3.appendChild(h('span','no','指'));t3.appendChild(h3s('指南','避坑与运维习惯'));s3.appendChild(t3);
   guide.forEach(function(g,i){
    g=g||{};
    const row=h('div','gline');row.appendChild(h('i',null,String(i+1)));
    const box=h('div');
    if(g.w!=null||g.c!=null||g.r!=null){
     box.appendChild(hl(h('p','gw','错误做法：'+(g.w||'')),chId,'G'+i+'w'));
     box.appendChild(hl(h('p','gc','后果：'+(g.c||'')),chId,'G'+i+'c'));
     box.appendChild(hl(h('p','gr','正确：'+(g.r||'')),chId,'G'+i+'r'));
    }else{
     if(g.t)box.appendChild(hl(h('p','gw gt',String(g.t)),chId,'G'+i+'t'));
     String(g.d||'').split(/(?=后果：|正确：|错误做法：)/).map(function(x){return x.trim();}).filter(Boolean).forEach(function(seg,j){
      const cls=/^后果：/.test(seg)?'gc':/^正确：/.test(seg)?'gr':'gw';
      box.appendChild(hl(h('p',cls,seg),chId,'G'+i+'d'+j));
     });
    }
    row.appendChild(box);s3.appendChild(row);
   });
   cb.appendChild(s3);
  }
  /* ④ 互动题 */
  if(quiz.q){
   const s4=h('div','lsec');
   const t4=h('div','lt');t4.appendChild(h('span','no','题'));t4.appendChild(h3s('互动题','答完再看解析'));s4.appendChild(t4);
   const qe=h('div','qtext',quiz.q);s4.appendChild(hl(qe,chId,'QZ'));
   if(quiz.hint)s4.appendChild(h('p','muted','提示：'+quiz.hint));
   const an=h('div','ans','参考答案：'+(quiz.a||''));
   const bb2=h('div','btns');
   const sb=h('button','btn sm','看参考答案');sb.onclick=function(){an.classList.toggle('on');};bb2.appendChild(sb);
   const cs=h('button','btn sm','这题我答对了 · 标记 🟢 过关');
   cs.onclick=function(){if(!S.learn[chId])S.learn[chId]={at:today(),done:[]};S.status[chId]='green';save();toast('已标记过关并加入复习队列');render();};
   bb2.appendChild(cs);
   s4.appendChild(bb2);s4.appendChild(an);
   cb.appendChild(s4);
  }
 }
 /* 模式切换 */
 const mr=h('div','modeRow');
 const b1=h('button','btn sm'+(S.mode==='long'?' on':''),'完整讲义');
 const b2=h('button','btn sm'+(S.mode==='lite'?' on':''),'速览（折叠细节）');
 b1.onclick=function(){S.mode='long';save();render();};
 b2.onclick=function(){S.mode='lite';save();render();};
 const b3=h('button','btn sm','我的划线 '+((S.marks||[]).length));
 b3.onclick=function(){nav('anno');};
 const b4=h('button','btn sm','数据');
 b4.onclick=function(){nav('io');};
 mr.appendChild(b1);mr.appendChild(b2);
 mr.appendChild(h('span','muted','选中文本即可划线（可跨段落）；同一段落能叠多条不同颜色，删除只删自己那条。点划线写多行备注、改线型、打标签。'));
 mr.appendChild(b3);mr.appendChild(b4);
 cb.appendChild(mr);
 cb.classList.add('card-'+S.mode);
 if(S.mode==='long')cb.classList.add('longMode');else cb.classList.remove('longMode');
}

/* =====================================================================
   v11 · 补丁层（B）：阅读位置 / 学习时长 / 学习日历 / 快捷入口
   ===================================================================== */

/* ---------- 章节阅读位置：下一站回到顶部；再次进入回到上次位置 ---------- */
let rdKey=null,rdRestoring=false,rdTimer=0;
function readKey(){
 try{const c=(S.pin&&chById(S.pin))||cur();return c?String(S.cur)+':'+c.id:null;}catch(e){return null;}
}
function jumpTo(y){window.scrollTo({top:y,left:0,behavior:'instant'});}
function afterRender(y0){
 if(view!=='today'){rdKey=null;return;}
 const k=readKey();if(!k)return;
 const top=!!window.__readTop;window.__readTop=0;
 if(k===rdKey&&!top){if(y0>0)jumpTo(y0);return;}   /* 同一章内重绘：保持原位置 */
 rdKey=k;
 S.ui.readPos=S.ui.readPos||{};
 let y=top?0:(Number(S.ui.readPos[k])||0);
 if(top){S.ui.readPos[k]=0;}
 rdRestoring=true;
 const go=function(){jumpTo(y);};
 go();requestAnimationFrame(function(){go();setTimeout(function(){go();setTimeout(function(){rdRestoring=false;},120);},60);});
}
window.addEventListener('scroll',function(){
 if(view!=='today'||rdRestoring||!rdKey)return;
 clearTimeout(rdTimer);
 rdTimer=setTimeout(function(){S.ui.readPos=S.ui.readPos||{};S.ui.readPos[rdKey]=Math.round(window.scrollY||0);save();},250);
},{passive:true});

/* ---------- 学习时长：在学习相关页面、页面可见且有操作时自动累计 ---------- */
const STUDY_VIEWS={today:1,course:1,know:1,anno:1,review:1,case:1,gloss:1,play:1,prog:1};
let stLast=Date.now(),stAcc=0;
['pointerdown','touchstart','keydown','wheel','scroll','click'].forEach(function(ev){window.addEventListener(ev,function(){stLast=Date.now();},{passive:true,capture:true});});
setInterval(function(){
 if(document.hidden||!STUDY_VIEWS[view]||Date.now()-stLast>120000)return;
 const k=today();S.ui.studyLog=S.ui.studyLog||{};
 S.ui.studyLog[k]=Math.round(((Number(S.ui.studyLog[k])||0)+5/60)*1000)/1000;
 stAcc+=5;if(stAcc>=30){stAcc=0;save();}
},5000);
window.addEventListener('pagehide',function(){save();});
document.addEventListener('visibilitychange',function(){if(document.hidden)save();});

/* ---------- 学习日历：日 / 周 / 月 / 年 ---------- */
let calP='week',calOff=0;
function logMin(k){return Number((S.ui.studyLog||{})[k])||0;}
function fmtDur(m){
 if(!(m>0))return '0 分钟';
 if(m<1)return '不足 1 分钟';
 const t=Math.round(m),hh=Math.floor(t/60),mm=t%60;
 return hh?(hh+' 小时'+(mm?' '+mm+' 分':'')):(t+' 分钟');
}
function shortDur(m){if(!(m>0))return '';if(m<1)return '<1分';const t=Math.round(m);return t>=60?(Math.floor(t/60)+'h'+(t%60?String(t%60).padStart(2,'0'):'')):(t+'分');}
function addDays(d,n){const x=new Date(d.getFullYear(),d.getMonth(),d.getDate()+n);return x;}
function learnedOn(key){return Object.keys(S.learn||{}).filter(function(id){return S.learn[id]&&S.learn[id].at===key;});}
function calRange(){
 const now=new Date(),base=new Date(now.getFullYear(),now.getMonth(),now.getDate());
 if(calP==='day'){const d=addDays(base,calOff);return {from:d,to:d,label:d.getFullYear()+'年'+(d.getMonth()+1)+'月'+d.getDate()+'日'};}
 if(calP==='week'){const dow=(base.getDay()+6)%7,s=addDays(base,-dow+7*calOff),e=addDays(s,6);return {from:s,to:e,label:(s.getMonth()+1)+'月'+s.getDate()+'日 – '+(e.getMonth()+1)+'月'+e.getDate()+'日'};}
 if(calP==='month'){const m=new Date(now.getFullYear(),now.getMonth()+calOff,1),e=new Date(m.getFullYear(),m.getMonth()+1,0);return {from:m,to:e,label:m.getFullYear()+'年 '+(m.getMonth()+1)+'月'};}
 const y=now.getFullYear()+calOff;return {from:new Date(y,0,1),to:new Date(y,11,31),label:y+'年'};
}
function calSum(from,to){
 let tot=0,days=0,chs=0;
 for(let d=new Date(from);d<=to;d=addDays(d,1)){const k=ymd(d),m=logMin(k),c=learnedOn(k).length;tot+=m;if(m>0||c>0)days++;chs+=c;}
 return {tot:tot,days:days,chs:chs};
}
function renderCalendar(){
 const box=$('v-calendar');box.textContent='';
 const wrap=h('div','cal');box.appendChild(wrap);
 const tabs=h('div','cal-tabs');
 [['day','日'],['week','周'],['month','月'],['year','年']].forEach(function(t){
  const b=h('button',calP===t[0]?'on':'',t[1]);b.onclick=function(){calP=t[0];calOff=0;renderCalendar();};tabs.appendChild(b);
 });
 wrap.appendChild(tabs);
 const rg=calRange(),sm=calSum(rg.from,rg.to);
 const nv=h('div','cal-nav');
 const prev=h('button','cal-arr','‹');prev.setAttribute('aria-label','上一段');prev.onclick=function(){calOff--;renderCalendar();};
 const next=h('button','cal-arr','›');next.setAttribute('aria-label','下一段');next.disabled=calOff>=0;next.onclick=function(){if(calOff<0){calOff++;renderCalendar();}};
 const lab=h('button','cal-lab',rg.label);lab.onclick=function(){if(calOff!==0){calOff=0;renderCalendar();}};
 nv.appendChild(prev);nv.appendChild(lab);nv.appendChild(next);wrap.appendChild(nv);
 /* 汇总 */
 const tot=h('div','cal-total');tot.appendChild(h('div','cal-big',fmtDur(sm.tot)));
 const nd=Math.round((rg.to-rg.from)/86400000)+1;
 tot.appendChild(h('div','cal-sub',calP==='day'?('学习章节 '+sm.chs+' 个'):(sm.days+' 天有学习 · 日均 '+fmtDur(sm.days?sm.tot/sm.days:0)+' · 章节 '+sm.chs+' 个')));
 wrap.appendChild(tot);
 /* 图表 */
 if(calP==='week'||calP==='year'){
  const items=[];
  if(calP==='week'){for(let i=0;i<7;i++){const d=addDays(rg.from,i);items.push({l:'一二三四五六日'[i],v:logMin(ymd(d)),on:ymd(d)===today(),k:ymd(d)});}}
  else{for(let mth=0;mth<12;mth++){let v=0;const e=new Date(rg.from.getFullYear(),mth+1,0);for(let d=new Date(rg.from.getFullYear(),mth,1);d<=e;d=addDays(d,1))v+=logMin(ymd(d));items.push({l:String(mth+1),v:v,on:rg.from.getFullYear()===new Date().getFullYear()&&mth===new Date().getMonth()});}}
  const mx=Math.max.apply(null,items.map(function(x){return x.v;}).concat([1]));
  const bars=h('div','cal-bars');
  items.forEach(function(it){
   const col=h('button','cal-col'+(it.on?' now':'')),val=h('span','cal-val',shortDur(it.v)),bar=h('span','cal-bar'),fill=h('i');
   fill.style.height=(it.v>0?Math.max(4,Math.round(it.v/mx*100)):0)+'%';bar.appendChild(fill);
   col.appendChild(val);col.appendChild(bar);col.appendChild(h('span','cal-xl',it.l));
   if(it.k)col.onclick=function(){calP='day';calOff=Math.round((new Date(it.k+'T00:00:00')-new Date(new Date().getFullYear(),new Date().getMonth(),new Date().getDate()))/86400000);renderCalendar();};
   bars.appendChild(col);
  });
  wrap.appendChild(bars);
 }
 if(calP==='month'){
  const grid=h('div','month-grid');
  ['日','一','二','三','四','五','六'].forEach(function(d){grid.appendChild(h('div','dow',d));});
  for(let i=0;i<rg.from.getDay();i++)grid.appendChild(h('div','month-day muted',''));
  for(let n=1;n<=rg.to.getDate();n++){
   const date=new Date(rg.from.getFullYear(),rg.from.getMonth(),n),key=ymd(date),v=logMin(key),c=learnedOn(key).length;
   const d=h('button','month-day'+(key===today()?' active':'')+(v>0||c?' has':''));
   d.appendChild(h('span',null,String(n)));d.appendChild(h('small',null,shortDur(v)));
   d.onclick=function(){calP='day';calOff=Math.round((date-new Date(new Date().getFullYear(),new Date().getMonth(),new Date().getDate()))/86400000);renderCalendar();};
   grid.appendChild(d);
  }
  wrap.appendChild(grid);
 }
 /* 明细 */
 const log=h('div','cal-log');
 if(calP==='day'){
  const key=ymd(rg.from),ids=learnedOn(key);
  log.appendChild(h('h3',null,'当日学习'));
  if(ids.length){ids.forEach(function(id){const c=chById(id);const r=h('div','log-row');r.appendChild(h('b',null,id+' '+(c?c.t:'')));log.appendChild(r);});}
  else log.appendChild(h('p','muted','当天没有完成章节学习的记录。'));
  const fix=h('button','btn sm','校正时长');
  fix.onclick=function(){const inp=document.createElement('input');inp.type='text';inp.placeholder='例如 35（分钟）';inp.value=logMin(key)?String(Math.round(logMin(key))):'';
   openSheet('学习时长 · '+key,field('分钟',inp),function(){const v=Number(inp.value);if(!Number.isFinite(v)||v<0){toast('请输入有效分钟数');return false;}S.ui.studyLog=S.ui.studyLog||{};S.ui.studyLog[key]=v;save();renderCalendar();toast('时长已保存');});};
  log.appendChild(fix);
 }else{
  log.appendChild(h('h3',null,'这段时间的学习记录'));
  const keys=[];for(let d=new Date(rg.from);d<=rg.to;d=addDays(d,1)){const k=ymd(d);if(logMin(k)>0||learnedOn(k).length)keys.push(k);}
  keys.reverse();
  if(!keys.length)log.appendChild(h('p','muted','还没有记录。读章节时会自动计时，完成章节学习后也会出现在这里。'));
  keys.slice(0,12).forEach(function(k){
   const r=h('button','log-row');r.appendChild(h('b',null,k.slice(5).replace('-','月')+'日'));
   r.appendChild(h('span','muted',fmtDur(logMin(k))+(learnedOn(k).length?' · '+learnedOn(k).length+' 章':'')));
   r.onclick=function(){const dt=new Date(k+'T00:00:00'),now=new Date();calP='day';calOff=Math.round((dt-new Date(now.getFullYear(),now.getMonth(),now.getDate()))/86400000);renderCalendar();};
   log.appendChild(r);
  });
 }
 wrap.appendChild(log);
 wrap.appendChild(h('p','cal-note','时长在阅读章节、复习、划线等学习页面自动记录；离开页面或 2 分钟无操作时暂停。'));
}

/* ---------- 快捷入口（课程页搜索栏下的功能块） ---------- */
const SC_TARGETS=[['course-manage','课程管理'],['my-marks','我的划线'],['calendar','学习日历'],['today','今日学习'],['review','复习队列'],['know','知识详解'],['gloss','术语手册'],['case','案例库'],['play','排障手册'],['prog','学习进度'],['io','数据'],['settings','设置']];
const SC_ICON={'course-manage':'book','my-marks':'highlighter',calendar:'calendar',today:'play',review:'refresh',know:'book',gloss:'aA',case:'folder',play:'wrench',prog:'chart',io:'share',settings:'settings'};
const SC_COLORS=['#2f7d5f','#3b6ea5','#7a5ca8','#c4673d','#d9a521','#e8e2d4','#f3c6c0','#bfe3d2','#22302b','#ffffff'];
function scDefaults(){return [{id:'sc_course',name:'课程',target:'course-manage',img:'',color:'',tc:''},{id:'sc_marks',name:'划线',target:'my-marks',img:'',color:'',tc:''},{id:'sc_cal',name:'学习日历',target:'calendar',img:'',color:'',tc:''}];}
function getSC(){if(!Array.isArray(S.ui.shortcuts))S.ui.shortcuts=scDefaults();return S.ui.shortcuts;}
function lumOf(hex){
 const m=/^#?([0-9a-f]{6})$/i.exec(String(hex||'').trim());if(!m)return 0.5;
 const v=[0,2,4].map(function(i){const c=parseInt(m[1].substr(i,2),16)/255;return c<=0.03928?c/12.92:Math.pow((c+0.055)/1.055,2.4);});
 return 0.2126*v[0]+0.7152*v[1]+0.0722*v[2];
}
/* 文字颜色：底纹亮 → 黑字；底纹暗 → 白字 */
function scTextColor(it){
 if(it.img)return it.tc||'#ffffff';
 if(it.color)return lumOf(it.color)>0.36?'#161a18':'#ffffff';
 return '';
}
function goShortcut(t){
 if(String(t).indexOf('course:')===0){const id=String(t).slice(7);if(COURSES.some(function(c){return c.id===id;})){activeCourse=id;cdSeg='chs';nav('course');}else toast('这门课程已不存在');return;}
 if(t==='my-marks')selectedMarkCourse='';
 nav(t);
}
function scTile(it,preview){
 const tc=scTextColor(it);
 const b=h('button','scard sc-tile'+(it.img?' has-img':'')+(it.color&&!it.img?' has-col':'')+(tc==='#ffffff'?' tc-w':''));
 if(it.img)b.style.backgroundImage='url("'+it.img+'")';
 else if(it.color)b.style.backgroundColor=it.color;
 if(tc)b.style.setProperty('--sc-tc',tc);
 if(!it.img){const ico=h('span','sc-ico');ico.innerHTML=ic(SC_ICON[it.target]||'layers');b.appendChild(ico);}
 b.appendChild(h('span','sc-lab',it.name||'未命名'));
 if(!preview)b.onclick=function(){goShortcut(it.target);};
 else b.tabIndex=-1;
 return b;
}
function fileToCover(file,cb){
 const rd=new FileReader();
 rd.onload=function(){
  const img=new Image();
  img.onload=function(){
   const W=360,sc=Math.min(1,W/img.width),w=Math.round(img.width*sc),hh=Math.round(img.height*sc);
   const cv=document.createElement('canvas');cv.width=w;cv.height=hh;
   const cx=cv.getContext('2d');cx.drawImage(img,0,0,w,hh);
   let lum=0.2;
   try{
    const y0=Math.floor(hh*0.62),d=cx.getImageData(0,y0,w,hh-y0).data;let s=0,n=0;
    for(let i=0;i<d.length;i+=16){const r=d[i]/255,g=d[i+1]/255,b=d[i+2]/255;s+=0.2126*r+0.7152*g+0.0722*b;n++;}
    lum=n?s/n:0.2;
   }catch(e){}
   cb(cv.toDataURL('image/jpeg',0.82),lum>0.62?'#161a18':'#ffffff');
  };
  img.onerror=function(){toast('图片无法读取，请换一张');};
  img.src=String(rd.result||'');
 };
 rd.readAsDataURL(file);
}
function openScEditor(idx){
 const list=getSC(),isNew=idx<0;
 const it=isNew?{id:'sc_'+uid(),name:'',target:'course-manage',img:'',color:'',tc:''}:Object.assign({},list[idx]);
 const body=h('div','sc-edit');
 const pv=h('div','sc-pv');body.appendChild(pv);
 function paintPv(){pv.textContent='';const row=h('div','shortrow sc-row sc-few');row.appendChild(scTile(it,true));pv.appendChild(row);}
 const nm=document.createElement('input');nm.type='text';nm.placeholder='例如：复习';nm.maxLength=8;nm.value=it.name;nm.oninput=function(){it.name=nm.value.trim();paintPv();};
 body.appendChild(field('名称',nm));
 const sel=document.createElement('select');
 SC_TARGETS.forEach(function(t){const o=document.createElement('option');o.value=t[0];o.textContent=t[1];sel.appendChild(o);});
 COURSES.forEach(function(c){const o=document.createElement('option');o.value='course:'+c.id;o.textContent='打开课程：'+coMeta(c).title;sel.appendChild(o);});
 sel.value=it.target;sel.onchange=function(){it.target=sel.value;paintPv();};
 body.appendChild(field('点击后前往',sel));
 const imgRow=h('div','sc-imgrow');
 const pick=h('button','btn sm','选择图片');
 pick.onclick=function(){const f=document.createElement('input');f.type='file';f.accept='image/*';f.onchange=function(){const file=f.files&&f.files[0];if(!file)return;fileToCover(file,function(url,tc){it.img=url;it.tc=tc;paintPv();paintImg();});};f.click();};
 const clr=h('button','btn sm ghost','移除图片');
 clr.onclick=function(){it.img='';it.tc='';paintPv();paintImg();};
 imgRow.appendChild(pick);imgRow.appendChild(clr);
 const imgNote=h('p','muted');
 function paintImg(){clr.style.display=it.img?'':'none';imgNote.textContent=it.img?'已设置图片；文字颜色按图片底部明暗自动选择。':'可选。设置图片后，图片会铺满整个功能块。';}
 const fImg=field('背景图片',imgRow);fImg.appendChild(imgNote);body.appendChild(fImg);paintImg();
 const colRow=h('div','sc-colors');
 function paintCols(){
  colRow.textContent='';
  const none=h('button','sc-sw none'+(!it.color?' on':''));none.setAttribute('aria-label','默认');none.title='默认';none.onclick=function(){it.color='';paintPv();paintCols();};colRow.appendChild(none);
  SC_COLORS.forEach(function(c){const b=h('button','sc-sw'+(it.color===c?' on':''));b.style.background=c;b.setAttribute('aria-label',c);b.onclick=function(){it.color=c;paintPv();paintCols();};colRow.appendChild(b);});
  const cp=document.createElement('input');cp.type='color';cp.className='sc-sw cpick';cp.value=/^#[0-9a-f]{6}$/i.test(it.color)?it.color:'#2f7d5f';cp.title='自定义颜色';cp.oninput=function(){it.color=cp.value;paintPv();colRow.querySelectorAll('.sc-sw.on').forEach(function(x){x.classList.remove('on');});};colRow.appendChild(cp);
 }
 paintCols();
 const fCol=field('底纹颜色',colRow);fCol.appendChild(h('p','muted','文字颜色自动适配：底纹偏亮用黑字，偏暗用白字。有图片时以图片为准。'));body.appendChild(fCol);
 paintPv();
 if(!isNew){
  const del=h('button','btn ghost','删除这个入口');del.style.marginTop='8px';
  del.onclick=function(){if(!confirm('删除「'+(list[idx].name||'未命名')+'」？'))return;list.splice(idx,1);save();closeSheet();render();};
  body.appendChild(del);
 }
 openSheet(isNew?'添加入口':'编辑入口',body,function(){
  if(!it.name){toast('请先填写名称');return false;}
  if(isNew)list.push(it);else list[idx]=it;
  save();render();
 });
}
function renderShortcuts(){
 const box=$('v-shortcuts');box.textContent='';
 const wrap=h('div','sc-page');box.appendChild(wrap);
 const list=getSC();
 const pv=h('div','clean-panel');pv.appendChild(h('h3',null,'预览'));
 const row=h('div','shortrow sc-row'+(list.length<=3?' sc-few':''));list.forEach(function(it){row.appendChild(scTile(it,true));});
 pv.appendChild(row);
 pv.appendChild(h('p','muted',list.length>3?'入口超过 3 个时，在课程页可左右滑动。':'显示在「课程」页搜索栏下方，超过 3 个时可左右滑动。'));
 wrap.appendChild(pv);
 const lp=h('div','clean-panel');lp.appendChild(h('h3',null,'入口 · '+list.length+' 个'));
 list.forEach(function(it,i){
  const r=h('div','sc-item');
  const sw=h('span','sc-mini');if(it.img)sw.style.backgroundImage='url("'+it.img+'")';else if(it.color)sw.style.background=it.color;r.appendChild(sw);
  const mid=h('div','sc-mid');mid.appendChild(h('b',null,it.name||'未命名'));
  const tg=SC_TARGETS.find(function(t){return t[0]===it.target;});
  mid.appendChild(h('small',null,tg?tg[1]:(String(it.target).indexOf('course:')===0?'打开课程':it.target)));r.appendChild(mid);
  const ops=h('div','sc-ops');
  const up=h('button',null,'↑');up.setAttribute('aria-label','上移');up.disabled=i===0;up.onclick=function(e){e.stopPropagation();const t=list[i];list[i]=list[i-1];list[i-1]=t;save();renderShortcuts();};
  const dn=h('button',null,'↓');dn.setAttribute('aria-label','下移');dn.disabled=i===list.length-1;dn.onclick=function(e){e.stopPropagation();const t=list[i];list[i]=list[i+1];list[i+1]=t;save();renderShortcuts();};
  const ed=h('button');ed.innerHTML=ic('pen');ed.setAttribute('aria-label','编辑');ed.onclick=function(e){e.stopPropagation();openScEditor(i);};
  ops.appendChild(up);ops.appendChild(dn);ops.appendChild(ed);r.appendChild(ops);
  r.onclick=function(){openScEditor(i);};
  lp.appendChild(r);
 });
 const add=h('button','btn sc-add','＋ 添加入口');add.onclick=function(){openScEditor(-1);};lp.appendChild(add);
 wrap.appendChild(lp);
 const rs=h('div','clean-panel');const rb=h('button','btn ghost','恢复默认入口');rb.onclick=function(){if(!confirm('恢复为默认的「课程 / 划线 / 学习日历」？'))return;S.ui.shortcuts=scDefaults();save();renderShortcuts();};rs.appendChild(rb);wrap.appendChild(rs);
}

/* =====================================================================
   v12 · 补丁层：竖三点编辑图标 / 章节朗读 / 设置页重排 / 划线页重做 / 快捷入口编辑
   ===================================================================== */

/* 编辑图标 → 竖向三个点 */
ICONS.pen='<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="4.8" r="2.1"/><circle cx="12" cy="12" r="2.1"/><circle cx="12" cy="19.2" r="2.1"/></svg>';
ICONS.speaker='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 9.5v5h3.6L12.5 19V5L7.6 9.5z"/><path d="M16 9a4 4 0 0 1 0 6"/><path d="M18.6 6.5a7.6 7.6 0 0 1 0 11"/></svg>';
ICONS.pauseI='<svg viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="5" width="4.2" height="14" rx="1.2"/><rect x="13.8" y="5" width="4.2" height="14" rx="1.2"/></svg>';

/* ---------- 章节朗读：使用系统自带的 TextToSpeech（Web Speech API） ---------- */
const TTS={on:false,paused:false,q:[],i:0,tok:0,key:null,mark:null};
function ttsSupported(){return typeof window!=='undefined'&&'speechSynthesis' in window&&typeof SpeechSynthesisUtterance!=='undefined';}
function ttsBlocks(){
 const cb=document.getElementById('cardBox');if(!cb)return [];
 const sel='.hd h2,.lsec h3,.lsec h4,.lsec h5,.lsec p,.lsec .md-li,.lsec .md-quote,.lsec .pstext,.lsec .qtext,.lsec li,.lsec td,.lsec th,.lsec .rd,.sec h3,.sec p,.sec li';
 const out=[];
 cb.querySelectorAll(sel).forEach(function(el){
  if(el.closest('pre,.modeRow,.btns'))return;
  if(el.closest('.det')&&document.body.classList.contains('x'))return;
  const t=(el.textContent||'').replace(/\s+/g,' ').trim();
  if(t.length<2||!/[\u4e00-\u9fffA-Za-z0-9]/.test(t))return;
  out.push({el:el,text:t});
 });
 return out;
}
function ttsChunks(text){
 const parts=text.match(/[^。！？；.!?;\n]+[。！？；.!?;]?/g)||[text],out=[];let cur='';
 parts.forEach(function(p){
  if((cur+p).length>110&&cur){out.push(cur);cur=p;}else cur+=p;
  while(cur.length>160){out.push(cur.slice(0,150));cur=cur.slice(150);}
 });
 if(cur.trim())out.push(cur);
 return out;
}
function ttsMarkNow(el){
 if(TTS.mark&&TTS.mark!==el)TTS.mark.classList.remove('tts-now');
 TTS.mark=el||null;
 if(el&&el.isConnected){
  el.classList.add('tts-now');
  const r=el.getBoundingClientRect();
  if(r.top<120||r.bottom>window.innerHeight-140)el.scrollIntoView({behavior:'smooth',block:'center'});
 }
}
function ttsSpeak(){
 if(!TTS.on||TTS.paused)return;
 if(TTS.i>=TTS.q.length){ttsStop();toast('朗读完成');return;}
 const item=TTS.q[TTS.i],tok=++TTS.tok;
 const u=new SpeechSynthesisUtterance(item.text);
 u.lang='zh-CN';u.rate=1;u.pitch=1;
 try{const v=speechSynthesis.getVoices().find(function(x){return /zh/i.test(x.lang);});if(v)u.voice=v;}catch(e){}
 u.onstart=function(){if(tok===TTS.tok)ttsMarkNow(item.el);};
 u.onend=function(){if(tok!==TTS.tok)return;TTS.i++;ttsSpeak();};
 u.onerror=function(e){if(tok!==TTS.tok)return;if(e&&(e.error==='canceled'||e.error==='interrupted'))return;TTS.i++;ttsSpeak();};
 try{speechSynthesis.speak(u);}catch(e){ttsStop();toast('朗读失败');}
}
function ttsStart(){
 if(!ttsSupported()){toast('当前浏览器不支持系统朗读');return;}
 const blocks=ttsBlocks();if(!blocks.length){toast('这一章没有可朗读的文字');return;}
 /* 从当前屏幕里第一段开始读 */
 let from=blocks.findIndex(function(b){return b.el.getBoundingClientRect().bottom>120;});if(from<0)from=0;
 const q=[];blocks.slice(from).forEach(function(b){ttsChunks(b.text).forEach(function(c){q.push({el:b.el,text:c});});});
 try{speechSynthesis.cancel();}catch(e){}
 TTS.on=true;TTS.paused=false;TTS.q=q;TTS.i=0;TTS.key=readKey();TTS.tok++;
 renderAppbar();setTimeout(ttsSpeak,80);
}
function ttsToggle(){
 if(!TTS.on)return;
 if(TTS.paused){TTS.paused=false;renderAppbar();ttsSpeak();}
 else{TTS.paused=true;TTS.tok++;try{speechSynthesis.cancel();}catch(e){}renderAppbar();}
}
function ttsStop(){
 const was=TTS.on;
 TTS.on=false;TTS.paused=false;TTS.tok++;TTS.q=[];TTS.i=0;
 try{if(ttsSupported())speechSynthesis.cancel();}catch(e){}
 if(TTS.mark){TTS.mark.classList.remove('tts-now');TTS.mark=null;}
 if(was)renderAppbar();
}
function ttsGuard(){if(TTS.on&&(view!=='today'||readKey()!==TTS.key))ttsStop();}
function ttsAppbar(){
 const ac=$('abActions');if(!ac||view!=='today')return;
 if(!TTS.on){ac.appendChild(iconBtn('speaker','朗读本章',ttsStart));return;}
 ac.appendChild(iconBtn(TTS.paused?'speaker':'pauseI',TTS.paused?'继续朗读':'暂停朗读',ttsToggle));
 ac.appendChild(iconBtn('close','停止朗读',ttsStop));
}
(function(){const __ra=renderAppbar;renderAppbar=function(){__ra.apply(this,arguments);ttsAppbar();};})();
window.addEventListener('pagehide',function(){if(TTS.on)ttsStop();});

/* ---------- 导航：划线两层、返回 ---------- */
(function(){
 const __nav=nav;
 nav=function(v){
  if(v==='anno'){selectedMarkCourse=S.cur;window.__marksDirect=true;v='my-marks';}
  else if(v==='my-marks'){window.__marksDirect=false;}
  return __nav.call(this,v);
 };
})();
function navBack(){
 if(view==='my-marks'&&selectedMarkCourse){
  if(!window.__marksDirect){selectedMarkCourse='';render();return;}
  window.__marksDirect=false;selectedMarkCourse='';
 }
 if(view==='io'&&ioDetail){ioDetail=null;render();return;}
 const p=navStack.pop();view=p||'mine';render();
}
function pageTitle(){
 if(view==='today'){const c=(S.pin&&chById(S.pin))||cur();return c?c.id+' '+c.t:'';}
 if(view==='player'){const pc=curChapter();return pc?pc.t:'';}
 if(view==='course')return coMeta(curCourse()).title;
 if(view==='my-marks')return selectedMarkCourse?'我的划线':'划线';
 if(view==='io'){
  if(ioDetail==='import')return '导入档案';
  const e=IO_ENTRIES.find(function(x){return x.id===ioDetail;});
  return e?e.title:'数据';
 }
 return TITLES[view]||'';
}

/* ---------- 划线：第一层与课程管理同版式；第二层按章节归类的条目列表 ---------- */
function jumpMark(m){
 const cid=m.courseId||S.cur;
 if(cid!==S.cur)setCourse(cid);
 if(!chById(m.ch)){toast('这条划线所在的章节已不存在');return;}
 locateMark(m.id);
}
function chOrderKey(ch,courseId){
 if(courseId===S.cur){const i=CH.findIndex(function(c){return c.id===ch;});return i<0?9999:i;}
 const n=parseFloat(String(ch).replace(/[^0-9.]/g,''));return Number.isFinite(n)?n:9999;
}
function renderMyMarks(){
 const box=$('v-my-marks');box.textContent='';
 const marks=allMarks(),groups={};
 marks.forEach(function(m){const k=m.courseId||S.cur;(groups[k]=groups[k]||[]).push(m);});
 if(!marks.length){box.appendChild(h('p','mk-empty','还没有划线。阅读章节时选中文字，即可添加划线与备注。'));return;}
 if(!selectedMarkCourse||!groups[selectedMarkCourse]){
  selectedMarkCourse='';
  const list=h('div','stagger');
  Object.keys(groups).forEach(function(k){
   const co=COURSES.find(function(c){return c.id===k;})||COURSES[0],r=h('div','course-manage-row');
   r.appendChild(coverEl(co,'sm'));
   const ci=h('div','ci');ci.appendChild(h('b',null,coMeta(co).title));
   ci.appendChild(h('small',null,groups[k].length+' 处划线'));
   ci.appendChild(h('small',null,'最近 '+new Date(Math.max.apply(null,groups[k].map(function(m){return m.at||0;}))).toLocaleDateString('zh-CN')));
   r.appendChild(ci);
   const act=h('div','cmrow-act');act.appendChild(h('span','mi-arrow','›'));r.appendChild(act);
   r.onclick=function(){selectedMarkCourse=k;render();};
   list.appendChild(r);
  });
  box.appendChild(list);return;
 }
 const k=selectedMarkCourse,co=COURSES.find(function(c){return c.id===k;})||COURSES[0],list=groups[k];
 box.appendChild(h('p','mk-sub',coMeta(co).title+' · '+list.length+' 处划线'));
 const by={};list.forEach(function(m){(by[m.ch||'']=by[m.ch||'']||[]).push(m);});
 Object.keys(by).sort(function(a,b){return chOrderKey(a,k)-chOrderKey(b,k);}).forEach(function(ch){
  const sec=h('section','mk-group'),hd=h('div','mk-gh');
  const cinfo=(k===S.cur)?chById(ch):null;
  hd.appendChild(h('span','mk-gt',(ch||'其他')+(cinfo?'  '+cinfo.t:'')));
  hd.appendChild(h('span','mk-gc',String(by[ch].length)));
  sec.appendChild(hd);
  by[ch].slice().sort(function(a,b){return (a.at||0)-(b.at||0);}).forEach(function(m){
   const e=h('div','mk-entry');e.setAttribute('role','button');e.tabIndex=0;
   const body=h('div','mk-body'),q=h('div','mk-qt'),sp=h('span','hlx ty-'+(m.ty||'solid')+' co-'+(m.co||'b'));
   sp.textContent=m.t||'（无文本）';q.appendChild(sp);body.appendChild(q);
   if(m.n){const n=h('div','mk-note');n.textContent=m.n;body.appendChild(n);}
   e.appendChild(body);
   e.onclick=function(){jumpMark(m);};
   e.onkeydown=function(ev){if(ev.key==='Enter')jumpMark(m);};
   sec.appendChild(e);
  });
  box.appendChild(sec);
 });
}

/* ---------- 外观模式：跟随系统 / 亮色 / 暗色 ---------- */
function themeMode(){const m=S.ui&&S.ui.themeMode;return (m==='system'||m==='light'||m==='dark')?m:(S.theme==='dark'?'dark':'light');}
function syncThemeMode(flash){
 const m=themeMode();let dark;
 if(m==='system')dark=!!(window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches);
 else dark=(m==='dark');
 const t=dark?'dark':'light';
 if(S.theme!==t||flash){if(flash)themeFlash();S.theme=t;applyTheme();}
}
(function(){
 try{
  const mq=window.matchMedia('(prefers-color-scheme: dark)');
  const fn=function(){if(themeMode()==='system')syncThemeMode(true);};
  if(mq.addEventListener)mq.addEventListener('change',fn);else if(mq.addListener)mq.addListener(fn);
 }catch(e){}
})();

/* ---------- 设置页：主题色 / 外观 / 主题 / 顶部留白（仿「外观设置」版式） ---------- */
const ST_IC={
 palette:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a9 9 0 1 0 0 18c1.1 0 1.7-.8 1.7-1.7 0-1.4-1.2-1.7-1.2-2.8 0-.9.7-1.6 1.6-1.6H16a5 5 0 0 0 5-5c0-3.9-4-7-9-7z"/><circle cx="7.8" cy="11" r="1" fill="currentColor"/><circle cx="10.6" cy="7.3" r="1" fill="currentColor"/><circle cx="15.2" cy="7.3" r="1" fill="currentColor"/></svg>',
 half:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.6"/><path d="M12 3.4v17.2a8.6 8.6 0 0 0 0-17.2z" fill="currentColor"/></svg>',
 sat:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v16"/><path d="M8 8l4-4 4 4"/><path d="M8 16l4 4 4-4"/></svg>',
 spark:'<svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.7" stroke-linejoin="round"><path d="M12 3.5c.6 4.6 3.9 7.9 8.5 8.5-4.6.6-7.9 3.9-8.5 8.5-.6-4.6-3.9-7.9-8.5-8.5 4.6-.6 7.9-3.9 8.5-8.5z"/></svg>'
};
let stSwScroll=0;
function stSection(icon,title){
 const s=h('section','st-sec'),hd=h('div','st-head'),ico=h('span','st-ic');
 ico.innerHTML=icon;hd.appendChild(ico);hd.appendChild(h('span','st-t',title));s.appendChild(hd);
 return s;
}
function stOption(label,on,fn,iconHtml,sub){
 const b=h('button','st-opt'+(on?' on':''));
 if(iconHtml){const i=h('span','oi');i.innerHTML=iconHtml;b.appendChild(i);}
 const t=h('span','ot');t.appendChild(h('b',null,label));if(sub)t.appendChild(h('small',null,sub));b.appendChild(t);
 b.onclick=fn;return b;
}
function renderSettings(){
 const box=$('v-settings');box.textContent='';
 const wrap=h('div','st-page');box.appendChild(wrap);
 /* 主题色 */
 const s1=stSection(ST_IC.palette,'主题色');
 const accIdx=S.ui.accent==null?DEFAULT_ACCENT:S.ui.accent;
 const mock=h('div','st-mock');
 mock.innerHTML='<div class="st-phone"><div class="st-card"><i class="sq"></i><span class="ln"><b></b><b></b><b class="s"></b></span><em class="pl"></em></div><div class="st-card"><i class="sq"></i><span class="ln"><b></b><b></b><b class="s"></b></span><em class="pl"></em></div><div class="st-card off"><i class="sq"></i><span class="ln"><b></b><b></b><b class="s"></b></span><em class="pl"></em></div></div>';
 s1.appendChild(mock);
 const row=h('div','st-sw-row');
 const order=[DEFAULT_ACCENT].concat(ACCENT_PALETTE.map(function(_,i){return i;}).filter(function(i){return i!==DEFAULT_ACCENT;}));
 order.forEach(function(i){
  const it=ACCENT_PALETTE[i],b=h('button','st-sw'+(accIdx===i?' on':'')),sq=h('span','sq');
  sq.style.setProperty('--c',it[1]);if(i!==DEFAULT_ACCENT)sq.innerHTML=ST_IC.spark;
  b.appendChild(sq);b.appendChild(h('span','nm',i===DEFAULT_ACCENT?'默认':it[0]));
  b.onclick=function(){S.ui.accent=i;themeFlash();applyAccent(it[1]);save();renderSettings();};
  row.appendChild(b);
 });
 const cu=h('label','st-sw'+(S.ui.accent===-1?' on':'')),csq=h('span','sq custom');
 if(S.ui.accent===-1&&S.ui.accentCustom)csq.style.setProperty('--c',S.ui.accentCustom);
 cu.appendChild(csq);cu.appendChild(h('span','nm','自定义'));
 const ci=document.createElement('input');ci.type='color';ci.value=S.ui.accentCustom||accentHex();ci.style.cssText='position:absolute;opacity:0;width:0;height:0;pointer-events:none';
 ci.oninput=function(){S.ui.accent=-1;S.ui.accentCustom=ci.value;applyAccent(ci.value);csq.style.setProperty('--c',ci.value);};
 ci.onchange=function(){S.ui.accent=-1;S.ui.accentCustom=ci.value;save();renderSettings();};
 cu.appendChild(ci);row.appendChild(cu);
 row.addEventListener('scroll',function(){stSwScroll=row.scrollLeft;},{passive:true});
 s1.appendChild(row);wrap.appendChild(s1);
 requestAnimationFrame(function(){row.scrollLeft=stSwScroll;});
 /* 外观 */
 const s2=stSection(ST_IC.half,'外观'),m=themeMode(),o2=h('div','st-opts three');
 [['system','跟随系统',null],['light','亮色',ic('sun')],['dark','暗色',ic('moon')]].forEach(function(x){
  o2.appendChild(stOption(x[1],m===x[0],function(){S.ui.themeMode=x[0];save();syncThemeMode(true);renderSettings();},x[2]));
 });
 s2.appendChild(o2);
 s2.appendChild(h('p','st-hint',m==='system'?'随手机系统的深色模式自动切换亮色 / 暗色。':''));
 wrap.appendChild(s2);
 /* 主题 */
 const s3=stSection(ic('layers'),'主题'),o3=h('div','st-opts two'),cs=(S.ui.style||'planet');
 [['minimal','极简版','克制、留白、轻量层级','leaf'],['planet','星球版','播客式大标题与封面列表','orbit']].forEach(function(x){
  o3.appendChild(stOption(x[1],cs===x[0],function(){if(cs===x[0])return;S.ui.style=x[0];themeFlash();save();applyStyleMode();miniShown=false;render();},ic(x[3]),x[2]));
 });
 s3.appendChild(o3);wrap.appendChild(s3);
 /* 顶部留白 */
 const s4=stSection(ST_IC.sat,'顶部留白'),o4=h('div','st-opts five'),cur_=(S.ui&&S.ui.sat!=null)?String(S.ui.sat):'auto';
 SAT_OPTS.forEach(function(x){o4.appendChild(stOption(x[1],cur_===x[0],function(){S.ui.sat=x[0];save();applySat();renderSettings();}));});
 s4.appendChild(o4);s4.appendChild(h('p','st-hint','全屏时避开顶部时间、电量一行；仅「我的」与本页生效。'));
 wrap.appendChild(s4);
}

/* ---------- 快捷入口：添加 / 编辑（精简版） ---------- */
function openScEditor(idx){
 const list=getSC(),isNew=idx<0;
 const it=isNew?{id:'sc_'+uid(),name:'',target:'course-manage',img:'',color:'',tc:''}:Object.assign({},list[idx]);
 let tab=it.img?'img':'color';
 const body=h('div','sc-edit');
 const top=h('div','sce-top'),pv=h('div','sce-pv'),f=h('div','sce-fields');
 top.appendChild(pv);top.appendChild(f);body.appendChild(top);
 function paintPv(){pv.textContent='';const row=h('div','shortrow sc-row sc-few');row.appendChild(scTile(it,true));pv.appendChild(row);}
 const nm=document.createElement('input');nm.type='text';nm.placeholder='名称';nm.maxLength=8;nm.value=it.name;nm.oninput=function(){it.name=nm.value.trim();paintPv();};
 f.appendChild(nm);
 const sel=document.createElement('select');
 SC_TARGETS.forEach(function(t){const o=document.createElement('option');o.value=t[0];o.textContent=t[1];sel.appendChild(o);});
 COURSES.forEach(function(c){const o=document.createElement('option');o.value='course:'+c.id;o.textContent='课程 · '+coMeta(c).title;sel.appendChild(o);});
 sel.value=it.target;sel.onchange=function(){it.target=sel.value;paintPv();};
 f.appendChild(sel);
 const tabs=h('div','sce-tabs'),pane=h('div','sce-pane');
 body.appendChild(tabs);body.appendChild(pane);
 function paintPane(){
  tabs.textContent='';
  [['color','颜色'],['img','图片']].forEach(function(t){const b=h('button',tab===t[0]?'on':'',t[1]);b.onclick=function(){tab=t[0];paintPane();};tabs.appendChild(b);});
  pane.textContent='';
  if(tab==='color'){
   const row=h('div','sc-colors');
   const none=h('button','sc-sw none'+(!it.color?' on':''));none.setAttribute('aria-label','默认');none.onclick=function(){it.color='';paintPv();paintPane();};row.appendChild(none);
   SC_COLORS.forEach(function(c){const b=h('button','sc-sw'+(it.color===c?' on':''));b.style.background=c;b.setAttribute('aria-label',c);b.onclick=function(){it.color=c;it.img='';it.tc='';paintPv();paintPane();};row.appendChild(b);});
   const cp=document.createElement('input');cp.type='color';cp.className='sc-sw cpick';cp.value=/^#[0-9a-f]{6}$/i.test(it.color)?it.color:'#2f7d5f';cp.oninput=function(){it.color=cp.value;it.img='';it.tc='';paintPv();};cp.onchange=function(){paintPane();};row.appendChild(cp);
   pane.appendChild(row);
  }else{
   const row=h('div','sc-imgrow');
   const pick=h('button','btn sm',it.img?'更换图片':'选择图片');
   pick.onclick=function(){const fi=document.createElement('input');fi.type='file';fi.accept='image/*';fi.onchange=function(){const file=fi.files&&fi.files[0];if(!file)return;fileToCover(file,function(url,tc){it.img=url;it.tc=tc;paintPv();paintPane();});};fi.click();};
   row.appendChild(pick);
   if(it.img){const clr=h('button','btn sm ghost','移除');clr.onclick=function(){it.img='';it.tc='';paintPv();paintPane();};row.appendChild(clr);}
   pane.appendChild(row);
  }
 }
 paintPane();paintPv();
 if(!isNew){
  const del=h('button','sce-del','删除入口');
  del.onclick=function(){if(!confirm('删除「'+(list[idx].name||'未命名')+'」？'))return;list.splice(idx,1);save();closeSheet();render();};
  body.appendChild(del);
 }
 openSheet(isNew?'添加入口':'编辑入口',body,function(){
  if(!it.name){toast('请先填写名称');return false;}
  if(isNew)list.push(it);else list[idx]=it;
  save();render();
 });
}

/* =====================================================================
   v13 · 视频 / 音频课程（读取文件夹） + 朗读多引擎与兜底
   ===================================================================== */

/* ---------- 工具 ---------- */
const MEDIA_EXT={video:['mp4','m4v','mov','webm','mkv','avi','ogv','3gp','flv','wmv','mpg','mpeg'],audio:['mp3','m4a','aac','wav','ogg','oga','flac','opus','amr','wma','m4b','aiff','mka']};
const MEDIA={};            /* 当前会话里已连接的文件：courseId:chapterId → {file} */
let MEDIA_URL={key:'',url:''};
function mediaKindOf(file){
 const n=String(file.name||'').toLowerCase(),ext=n.slice(n.lastIndexOf('.')+1),t=String(file.type||'');
 if(t.indexOf('video/')===0||MEDIA_EXT.video.indexOf(ext)>=0)return 'video';
 if(t.indexOf('audio/')===0||MEDIA_EXT.audio.indexOf(ext)>=0)return 'audio';
 return '';
}
function natCmp(a,b){return String(a).localeCompare(String(b),'zh-Hans-CN',{numeric:true,sensitivity:'base'});}
function sortMediaItems(items,by,dir){
 const s=dir==='desc'?-1:1;
 items.sort(function(x,y){let r=by==='time'?((x.mtime||0)-(y.mtime||0)):natCmp(x.name,y.name);if(r===0)r=natCmp(x.name,y.name);return r*s;});
 return items;
}
function fmtSize(n){n=Number(n)||0;if(n>=1073741824)return (n/1073741824).toFixed(1)+' GB';if(n>=1048576)return Math.round(n/1048576)+' MB';return Math.max(1,Math.round(n/1024))+' KB';}
function fmtClock(sec){sec=Math.max(0,Math.round(Number(sec)||0));const hh=Math.floor(sec/3600),mm=Math.floor(sec%3600/60),ss=sec%60;return (hh?hh+':'+String(mm).padStart(2,'0'):mm)+':'+String(ss).padStart(2,'0');}
function fmtDate(ms){const d=new Date(ms||0);return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');}
function stripExt(n){return String(n).replace(/\.[^.]+$/,'');}

/* ---------- IndexedDB：保存文件夹句柄（支持的浏览器上，下次可免重选） ---------- */
function idbOpen(){return new Promise(function(res,rej){try{const r=indexedDB.open('dim-media',1);r.onupgradeneeded=function(){r.result.createObjectStore('h');};r.onsuccess=function(){res(r.result);};r.onerror=function(){rej(r.error);};}catch(e){rej(e);}});}
function idbSet(k,v){return idbOpen().then(function(d){return new Promise(function(res,rej){const t=d.transaction('h','readwrite');t.objectStore('h').put(v,k);t.oncomplete=function(){res();};t.onerror=function(){rej(t.error);};});});}
function idbGet(k){return idbOpen().then(function(d){return new Promise(function(res,rej){const t=d.transaction('h','readonly'),q=t.objectStore('h').get(k);q.onsuccess=function(){res(q.result);};q.onerror=function(){rej(q.error);};});});}

/* ---------- 选择文件夹 / 文件 ---------- */
function toItem(file,mod,rel){
 return {file:file,fname:file.name,name:stripExt(file.name),rel:rel||file.webkitRelativePath||file.name,size:file.size,mtime:file.lastModified,mod:mod||''};
}
async function walkDir(dh,prefix,out,depth,kind){
 for await(const entry of dh.entries()){
  const name=entry[0],hd=entry[1];
  if(hd.kind==='file'){
   const f=await hd.getFile();
   if(!kind||mediaKindOf(f)===kind){const parts=(prefix+name).split('/');out.push(toItem(f,parts.length>1?parts[0]:'',prefix+name));}
  }else if(depth<3&&name[0]!=='.'){await walkDir(hd,prefix+name+'/',out,depth+1,kind);}
 }
}
/* 必须在点击事件里直接调用（浏览器要求用户手势） */
function pickFolder(kind){
 return new Promise(function(resolve){
  if(window.showDirectoryPicker){
   window.showDirectoryPicker({mode:'read'}).then(function(dh){
    const items=[];return walkDir(dh,'',items,0,kind).then(function(){resolve({items:items,dirHandle:dh,name:dh.name});});
   }).catch(function(){resolve(null);});
   return;
  }
  const inp=document.createElement('input');inp.type='file';inp.multiple=true;
  inp.setAttribute('webkitdirectory','');inp.setAttribute('directory','');
  inp.onchange=function(){
   const fl=Array.from(inp.files||[]).filter(function(f){return mediaKindOf(f)===kind;});
   if(!fl.length){resolve({items:[],name:''});return;}
   const root=String(fl[0].webkitRelativePath||'').split('/')[0];
   resolve({items:fl.map(function(f){const p=String(f.webkitRelativePath||f.name).split('/');return toItem(f,p.length>2?p[1]:'');}),name:root});
  };
  inp.oncancel=function(){resolve(null);};
  inp.click();
 });
}
function pickFiles(kind){
 return new Promise(function(resolve){
  const inp=document.createElement('input');inp.type='file';inp.multiple=true;inp.accept=kind==='video'?'video/*':'audio/*';
  inp.onchange=function(){
   const fl=Array.from(inp.files||[]).filter(function(f){return mediaKindOf(f)===kind;});
   resolve({items:fl.map(function(f){return toItem(f,'');}),name:''});
  };
  inp.oncancel=function(){resolve(null);};
  inp.click();
 });
}

/* ---------- 导入弹层 ---------- */
function openMediaImport(kind){
 const KN=kind==='video'?'视频':'音频';
 const st={items:[],by:'name',dir:'asc',title:'',dirHandle:null};
 const body=h('div','mi');
 const btns=h('div','mi-btns');
 const bf=h('button','btn pri','选择文件夹'),bm=h('button','mi-alt','文件夹读不到？改选单个文件');
 btns.appendChild(bf);body.appendChild(btns);body.appendChild(bm);
 const info=h('p','mi-info');body.appendChild(info);
 const nameInp=document.createElement('input');nameInp.type='text';nameInp.placeholder='课程名称';nameInp.oninput=function(){st.title=nameInp.value.trim();};
 const nameF=field('课程名称',nameInp);nameF.style.display='none';body.appendChild(nameF);
 const sortBar=h('div','mi-sort');sortBar.style.display='none';body.appendChild(sortBar);
 const list=h('div','mi-list');body.appendChild(list);
 body.appendChild(h('p','mi-note',kind==='video'?'选择后会按文件名自然排序（1、2、10 排在正确位置）。文件只在本机读取，不会上传。mkv / avi 等格式部分手机浏览器无法播放，推荐 mp4。':'选择后会按文件名自然排序（1、2、10 排在正确位置）。文件只在本机读取，不会上传。推荐 mp3 / m4a。'));
 function paint(){
  info.textContent=st.items.length?('已读取 '+st.items.length+' 个'+KN+'文件 · 共 '+fmtSize(st.items.reduce(function(s,x){return s+x.size;},0))):'';
  nameF.style.display=sortBar.style.display=st.items.length?'':'none';
  sortBar.textContent='';
  [['name','按名称'],['time','按时间']].forEach(function(o){const b=h('button',st.by===o[0]?'on':'',o[1]);b.onclick=function(){st.by=o[0];paint();};sortBar.appendChild(b);});
  sortBar.appendChild(h('span','mi-gap'));
  [['asc','升序'],['desc','降序']].forEach(function(o){const b=h('button',st.dir===o[0]?'on':'',o[1]);b.onclick=function(){st.dir=o[0];paint();};sortBar.appendChild(b);});
  list.textContent='';
  const sorted=sortMediaItems(st.items.slice(),st.by,st.dir);
  sorted.slice(0,8).forEach(function(it,i){
   const r=h('div','mi-row');r.appendChild(h('span','mi-n',String(i+1)));r.appendChild(h('span','mi-t',(it.mod?it.mod+' / ':'')+it.name));
   r.appendChild(h('small',null,st.by==='time'?fmtDate(it.mtime):fmtSize(it.size)));list.appendChild(r);
  });
  if(sorted.length>8)list.appendChild(h('p','mi-more','… 共 '+sorted.length+' 个'));
 }
 function got(res){
  if(!res)return;
  if(!res.items.length){toast('没有在这个文件夹里找到'+KN+'文件；可点下方「改选单个文件」');bm.classList.add('hot');return;}
  st.items=res.items;st.dirHandle=res.dirHandle||null;
  if(!st.title){st.title=res.name||(KN+'课程');}
  nameInp.value=st.title;paint();
 }
 bf.onclick=function(){pickFolder(kind).then(got);};
 bm.onclick=function(){pickFiles(kind).then(got);};
 openSheet('导入'+KN+'课程',body,function(){
  if(!st.items.length){toast('请先选择文件夹或文件');return false;}
  const id=createMediaCourse(kind,st.title||(KN+'课程'),st.items,st.by,st.dir,st.dirHandle);
  render();toast('已导入「'+coMeta(COURSES.find(function(c){return c.id===id;})).title+'」');
  setTimeout(function(){probeDurations(id);},300);
 },'导入');
}

/* ---------- 建课 ---------- */
function mediaChapters(items,by,dir){
 /* 子文件夹 → 模块；根目录文件 → 默认模块 */
 const groups={},order=[];
 items.forEach(function(it){const k=it.mod||'';if(!groups[k]){groups[k]=[];order.push(k);}groups[k].push(it);});
 order.sort(function(a,b){
  if(!a)return -1;if(!b)return 1;
  if(by==='time'){const ma=Math.min.apply(null,groups[a].map(function(x){return x.mtime||0;})),mb=Math.min.apply(null,groups[b].map(function(x){return x.mtime||0;}));return (ma-mb)*(dir==='desc'?-1:1);}
  return natCmp(a,b)*(dir==='desc'?-1:1);
 });
 const mods=[],chs=[];let n=0;const wd=Math.max(2,String(items.length).length);
 order.forEach(function(k,mi){
  const letter=mi<26?String.fromCharCode(65+mi):'M'+(mi+1);
  mods.push({k:letter,n:k||'全部',tag:''});
  sortMediaItems(groups[k],by,dir).forEach(function(it){
   n++;chs.push({id:String(n).padStart(wd,'0'),m:letter,title:it.name,summary:fmtSize(it.size),media:{kind:'',fname:it.fname,rel:it.rel,size:it.size,mtime:it.mtime,dur:0},_it:it});
  });
 });
 return {mods:mods,chs:chs};
}
function createMediaCourse(kind,title,items,by,dir,dirHandle){
 const mc=mediaChapters(items,by,dir);
 mc.chs.forEach(function(c){c.media.kind=kind;});
 const KN=kind==='video'?'视频':'音频';
 const x={title:title,subtitle:mc.chs.length+' 个'+KN+' · 读取自本机文件夹',description:'本课程由本机的'+KN+'文件导入，每个文件是一章。文件不会上传；刷新或重开后需要重新连接文件夹。',
  modules:mc.mods,chapters:mc.chs.map(function(c){return {id:c.id,m:c.m,title:c.title,summary:c.summary,media:c.media};}),media:kind};
 const id=importCourse(x,0);
 const rec=(S.ui.userCourses||[]).find(function(c){return c.id===id;});
 if(rec){rec.media=kind;rec.sort={by:by,dir:dir};rec.glyph=kind==='video'?'monitor':'speaker';}
 mc.chs.forEach(function(c){MEDIA[id+':'+c.id]={file:c._it.file};});
 if(dirHandle){idbSet('dir:'+id,dirHandle).catch(function(){});}
 syncCourses();save();return id;
}

/* ---------- 重新连接（刷新后文件需重新授权 / 选择） ---------- */
function attachFiles(courseId,items){
 const co=COURSES.find(function(c){return c.id===courseId;});if(!co)return 0;
 const byRel={},byKey={};
 items.forEach(function(it){byRel[it.rel]=it;byKey[it.fname+'|'+it.size]=it;});
 let n=0;
 packOf(co).ch.forEach(function(c){
  if(!c.media)return;
  const it=byRel[c.media.rel]||byKey[c.media.fname+'|'+c.media.size];
  if(it){MEDIA[courseId+':'+c.id]={file:it.file};n++;}
 });
 return n;
}
async function relinkMedia(courseId,interactive,kindHint){
 const co=COURSES.find(function(c){return c.id===courseId;});if(!co||!co.media)return false;
 try{
  const dh=await idbGet('dir:'+courseId);
  if(dh&&dh.queryPermission){
   let perm=await dh.queryPermission({mode:'read'});
   if(perm!=='granted'&&interactive)perm=await dh.requestPermission({mode:'read'});
   if(perm==='granted'){const items=[];await walkDir(dh,'',items,0,co.media);if(attachFiles(courseId,items)){return true;}}
  }
 }catch(e){}
 return false;
}
function relinkByPicker(courseId){
 const co=COURSES.find(function(c){return c.id===courseId;});if(!co)return;
 pickFolder(co.media).then(function(res){
  if(!res||!res.items.length){toast('没有选到文件');return;}
  const n=attachFiles(courseId,res.items);
  if(!n){toast('文件夹里没有匹配的文件，请选择当初导入的那个文件夹');return;}
  if(res.dirHandle)idbSet('dir:'+courseId,res.dirHandle).catch(function(){});
  toast('已连接 '+n+' 个文件');render();probeDurations(courseId);
 });
}

/* ---------- 时长探测（后台，逐个读取） ---------- */
let probing=false;
async function probeDurations(courseId){
 if(probing)return;probing=true;
 try{
  const co=COURSES.find(function(c){return c.id===courseId;});if(!co||!co.media)return;
  const p=packOf(co);
  for(const c of p.ch){
   if(!c.media||c.media.dur)continue;
   const reg=MEDIA[courseId+':'+c.id];if(!reg)continue;
   const dur=await new Promise(function(res){
    const el=document.createElement(co.media==='video'?'video':'audio'),url=URL.createObjectURL(reg.file);
    let done=false;const fin=function(v){if(done)return;done=true;try{URL.revokeObjectURL(url);}catch(e){}res(v);};
    el.preload='metadata';el.onloadedmetadata=function(){fin(isFinite(el.duration)?el.duration:0);};el.onerror=function(){fin(0);};
    setTimeout(function(){fin(0);},5000);el.src=url;
   });
   if(dur){c.media.dur=Math.round(dur);c.s=fmtClock(dur)+' · '+fmtSize(c.media.size);}
  }
  save();if(view==='course'||view==='today')render();
 }finally{probing=false;}
}

/* ---------- 章节页：播放器 ---------- */
function mediaNext(){
 const q=seq(),c=(S.pin&&chById(S.pin))||cur(),i=q.indexOf(c.id);
 if(i>=0&&i+1<q.length){S.idx=i+1;S.pin='';S.ui.mediaAutoplay=true;window.__readTop=1;save();render();}
 else toast('已经是最后一集');
}
function renderMediaChapter(cb,c){
 if(S.mode!=='long'&&S.mode!=='lite'){S.mode='long';save();}
 cb.classList.add('is-media');cb.classList.remove('card-list');
 const pm=cb.querySelector(':scope>p.muted');if(pm)pm.textContent=pm.textContent.replace(/\s*[｜|].*$/,'');
 const co=curCourse(),kind=co.media||(c.media&&c.media.kind)||'video',key=co.id+':'+c.id,reg=MEDIA[key];
 const box=h('div','lsec mediabox');
 const meta=c.media||{};
 if(!reg){
  const un=h('div','media-un');
  un.appendChild(h('p','mu-t','媒体文件还没有连接'));
  un.appendChild(h('p','mu-s','「'+(meta.fname||c.t)+'」'+(meta.size?' · '+fmtSize(meta.size):'')+' 保存在你的手机里。刷新或重新打开页面后，需要重新选择当初导入的文件夹。'));
  const b=h('button','btn pri','重新选择文件夹');b.onclick=function(){relinkMedia(co.id,true).then(function(ok){if(ok){toast('已连接');render();}else relinkByPicker(co.id);});};
  un.appendChild(b);
  box.appendChild(un);cb.appendChild(box);
  relinkMedia(co.id,false).then(function(ok){if(ok&&view==='today'&&readKey()===co.id+':'+c.id)render();});
  return;
 }
 if(MEDIA_URL.key!==key){if(MEDIA_URL.url){try{URL.revokeObjectURL(MEDIA_URL.url);}catch(e){}}MEDIA_URL={key:key,url:URL.createObjectURL(reg.file)};}
 const wrap=h('div','media-wrap '+kind);
 const el=document.createElement(kind==='video'?'video':'audio');
 el.className='media-el';el.controls=true;el.preload='metadata';el.setAttribute('playsinline','');el.setAttribute('webkit-playsinline','');el.src=MEDIA_URL.url;
 el.playbackRate=Number(S.ui.mediaRate)||1;
 if(kind==='audio'){const art=h('div','media-art');art.innerHTML=ic('speaker');wrap.appendChild(art);}
 wrap.appendChild(el);box.appendChild(wrap);
 S.ui.mediaPos=S.ui.mediaPos||{};
 const pos=Number(S.ui.mediaPos[key])||0;let lastSave=0;
 el.addEventListener('loadedmetadata',function(){
  el.playbackRate=Number(S.ui.mediaRate)||1;
  if(pos>2&&isFinite(el.duration)&&pos<el.duration-3){try{el.currentTime=pos;}catch(e){}}
  if(S.ui.mediaAutoplay){S.ui.mediaAutoplay=false;const pr=el.play();if(pr&&pr.catch)pr.catch(function(){});}
  if(isFinite(el.duration)&&!meta.dur){meta.dur=Math.round(el.duration);c.s=fmtClock(el.duration)+' · '+fmtSize(meta.size);save();}
 });
 el.addEventListener('timeupdate',function(){
  stLast=Date.now();
  const t=Date.now();if(t-lastSave>3000){lastSave=t;S.ui.mediaPos[key]=Math.floor(el.currentTime);save();}
 });
 el.addEventListener('ended',function(){
  S.ui.mediaPos[key]=0;
  if(!S.learn[c.id])S.learn[c.id]={at:today(),done:[]};
  save();
  if(S.ui.mediaAuto!==false)mediaNext();
 });
 /* 播放设置 */
 const ctl=h('div','media-ctl');
 const sp=document.createElement('select');sp.setAttribute('aria-label','播放速度');
 [0.75,1,1.25,1.5,2].forEach(function(v){const o=document.createElement('option');o.value=String(v);o.textContent=v+'×';sp.appendChild(o);});
 sp.value=String(Number(S.ui.mediaRate)||1);sp.onchange=function(){S.ui.mediaRate=Number(sp.value);el.playbackRate=S.ui.mediaRate;save();};
 ctl.appendChild(h('span','mc-l','倍速'));ctl.appendChild(sp);
 const au=h('button','mc-auto'+(S.ui.mediaAuto!==false?' on':''),'播完自动下一集');
 au.onclick=function(){S.ui.mediaAuto=!(S.ui.mediaAuto!==false);au.classList.toggle('on',S.ui.mediaAuto);save();};
 ctl.appendChild(au);box.appendChild(ctl);
 const mt=h('div','media-meta');
 mt.appendChild(h('div',null,meta.fname||c.t));
 mt.appendChild(h('small',null,[kind==='video'?'视频':'音频',fmtSize(meta.size),meta.dur?fmtClock(meta.dur):'',meta.mtime?('修改于 '+fmtDate(meta.mtime)):''].filter(Boolean).join(' · ')));
 box.appendChild(mt);
 cb.appendChild(box);
}
(function(){const __rl=renderLong;renderLong=function(cb,c){if(c&&c.media){renderMediaChapter(cb,c);return;}return __rl.apply(this,arguments);};})();

/* ---------- 媒体课程：章节页内重新排序 ---------- */
function resortMedia(courseId,by,dir){
 const co=COURSES.find(function(c){return c.id===courseId;});if(!co||!co.media)return;
 const p=packOf(co),bag=S.cp[courseId]||curBag();
 const curId=(co.id===S.cur)?((S.pin&&chById(S.pin))||cur()||{}).id:null;
 const byMod={};p.ch.forEach(function(c){(byMod[c.m]=byMod[c.m]||[]).push(c);});
 const out=[];
 p.mods.forEach(function(m){
  const arr=(byMod[m.k]||[]).map(function(c){return {c:c,name:(c.media&&stripExt(c.media.fname))||c.t,mtime:(c.media&&c.media.mtime)||0};});
  sortMediaItems(arr,by,dir).forEach(function(x){out.push(x.c);});
 });
 p.ch.splice(0,p.ch.length,...out);
 p.routes.all=out.map(function(c){return c.id;});
 co.sort={by:by,dir:dir};
 if(co.id===S.cur){loadPack(co.id);if(curId){const i=p.routes.all.indexOf(curId);if(i>=0)S.idx=i;}}
 save();
}
function tidyMediaCourse(){
 if(view!=='course')return;
 const co=curCourse();if(!co||!co.media||cdSeg!=='chs')return;
 const sw=document.querySelector('#v-course .swap');if(!sw||sw.querySelector('.media-sort'))return;
 const row=h('div','media-sort'),s=co.sort||{by:'name',dir:'asc'};
 row.appendChild(h('span','ms-l','排序'));
 [['name','名称'],['time','时间']].forEach(function(o){const b=h('button',s.by===o[0]?'on':'',o[1]);b.onclick=function(){resortMedia(co.id,o[0],s.dir);render();};row.appendChild(b);});
 row.appendChild(h('span','ms-gap'));
 [['asc','升序'],['desc','降序']].forEach(function(o){const b=h('button',s.dir===o[0]?'on':'',o[1]);b.onclick=function(){resortMedia(co.id,s.by,o[0]);render();};row.appendChild(b);});
 const chips=sw.querySelector('.pl-chips');
 if(chips&&chips.parentNode===sw)chips.insertAdjacentElement('afterend',row);else sw.insertBefore(row,sw.children[1]||null);
}

/* ---------- 课程管理页：导入媒体课程入口 ---------- */
function tidyCourseManage(){
 if(view!=='course-manage')return;
 const box=$('v-course-manage');if(!box||box.querySelector('.cm-import'))return;
 const p=h('div','cm-import');p.appendChild(h('h3',null,'导入媒体课程'));
 [['video','导入视频课程','读取文件夹里的视频，每个文件一章','monitor'],['audio','导入音频课程','读取文件夹里的音频，每个文件一章','speaker']].forEach(function(x){
  const r=h('button','cm-imp-row');
  const i=h('span','ci-ic');i.innerHTML=ic(x[3]);r.appendChild(i);
  const m=h('span','ci-mid');m.appendChild(h('b',null,x[1]));m.appendChild(h('small',null,x[2]));r.appendChild(m);
  r.appendChild(h('span','ci-arr','›'));
  r.onclick=function(){openMediaImport(x[0]);};
  p.appendChild(r);
 });
 box.appendChild(p);
}

/* =====================================================================
   朗读：系统 TTS → 在线语音 → 专注阅读，三级兜底；可选音色
   ===================================================================== */
TTS.fail={system:false,online:false};TTS.audio=null;TTS.blocks=[];
const ONLINE_VOICES=[[0,'度小美 · 女声'],[1,'度小宇 · 男声'],[3,'度逍遥 · 男声（磁性）'],[4,'度丫丫 · 童声']];
function ttsCfg(){S.ui.tts=S.ui.tts||{engine:'auto',voice:'',per:0,rate:1};return S.ui.tts;}
function ttsEngineNow(){
 const e=ttsCfg().engine;
 if(e==='system'||e==='online'||e==='reader')return e;
 if(ttsSupported()&&!TTS.fail.system)return 'system';
 if(!TTS.fail.online)return 'online';
 return 'reader';
}
function ttsChunks(text,max){
 max=max||110;
 const parts=text.match(/[^。！？；.!?;\n]+[。！？；.!?;]?/g)||[text],out=[];let cur='';
 parts.forEach(function(p){
  if((cur+p).length>max&&cur){out.push(cur);cur=p;}else cur+=p;
  while(cur.length>max+40){out.push(cur.slice(0,max+30));cur=cur.slice(max+30);}
 });
 if(cur.trim())out.push(cur);
 return out;
}
function ttsBuildQueue(fromBlock,eng){
 const q=[],max=eng==='online'?55:110;
 TTS.blocks.slice(fromBlock).forEach(function(b,bi){ttsChunks(b.text,max).forEach(function(c){q.push({el:b.el,text:c,b:fromBlock+bi});});});
 return q;
}
function ttsOnlineUrl(text){
 const cfg=ttsCfg(),spd=Math.max(0,Math.min(9,Math.round(5*(cfg.rate||1))));
 return 'https://tts.baidu.com/text2audio?tex='+encodeURIComponent(text)+'&cuid=dim-reader&lan=zh&ctp=1&pdt=301&vol=9&rate=32&per='+(cfg.per||0)+'&spd='+spd;
}
function speakSystem(item,tok,next,fail){
 if(!ttsSupported()){fail('unsupported');return;}
 const cfg=ttsCfg(),u=new SpeechSynthesisUtterance(item.text);
 u.lang='zh-CN';u.rate=cfg.rate||1;
 let started=false;
 try{
  const vs=speechSynthesis.getVoices();let v=null;
  if(cfg.voice)v=vs.find(function(x){return x.voiceURI===cfg.voice||x.name===cfg.voice;});
  if(!v)v=vs.find(function(x){return /zh/i.test(x.lang);});
  if(v){u.voice=v;u.lang=v.lang||'zh-CN';}
 }catch(e){}
 u.onstart=function(){started=true;if(tok===TTS.tok)ttsMarkNow(item.el);};
 u.onend=function(){started=true;next();};
 u.onerror=function(e){if(tok!==TTS.tok)return;if(e&&(e.error==='canceled'||e.error==='interrupted'))return;fail((e&&e.error)||'error');};
 try{speechSynthesis.speak(u);}catch(e){fail('throw');return;}
 setTimeout(function(){if(tok===TTS.tok&&!started){try{speechSynthesis.cancel();}catch(e){}fail('timeout');}},5000);
}
function speakOnline(item,tok,next,fail){
 const a=new Audio();TTS.audio=a;a.preload='auto';a.src=ttsOnlineUrl(item.text);
 let started=false;
 a.onplaying=function(){started=true;if(tok===TTS.tok)ttsMarkNow(item.el);};
 a.onended=function(){next();};
 a.onerror=function(){if(tok===TTS.tok)fail('audio');};
 const pr=a.play();if(pr&&pr.catch)pr.catch(function(){if(tok===TTS.tok&&!started)fail('play');});
 setTimeout(function(){if(tok===TTS.tok&&!started){try{a.pause();}catch(e){}fail('timeout');}},9000);
}
function ttsHaltAudio(){
 try{if(ttsSupported())speechSynthesis.cancel();}catch(e){}
 if(TTS.audio){try{TTS.audio.pause();TTS.audio.src='';}catch(e){}TTS.audio=null;}
}
function ttsSpeak(){
 if(!TTS.on||TTS.paused)return;
 if(TTS.i>=TTS.q.length){ttsStop();toast('朗读完成');return;}
 const eng=ttsEngineNow(),item=TTS.q[TTS.i],tok=++TTS.tok;
 const next=function(){if(tok!==TTS.tok)return;TTS.i++;ttsSpeak();};
 const fail=function(){if(tok!==TTS.tok)return;ttsFallback(eng);};
 if(eng==='system')speakSystem(item,tok,next,fail);
 else if(eng==='online')speakOnline(item,tok,next,fail);
 else ttsFallback('online');
}
function ttsFallback(from){
 TTS.tok++;ttsHaltAudio();
 const fromBlock=TTS.q[TTS.i]?TTS.q[TTS.i].b:0,fixed=ttsCfg().engine!=='auto';
 if(from==='system')TTS.fail.system=true;else TTS.fail.online=true;
 const nxt=fixed?'reader':ttsEngineNow();
 if(nxt==='reader'){
  ttsStop();
  toast((from==='system'?'系统朗读':'在线语音')+'不可用，已切换为专注阅读');
  openFocusReader(TTS.blocks,fromBlock);return;
 }
 toast('系统朗读不可用，已切换为在线语音');
 TTS.q=ttsBuildQueue(fromBlock,nxt);TTS.i=0;ttsSpeak();
}
function ttsStart(){
 const blocks=ttsBlocks();if(!blocks.length){toast('这一章没有可朗读的文字');return;}
 let from=blocks.findIndex(function(b){return b.el.getBoundingClientRect().bottom>120;});if(from<0)from=0;
 TTS.blocks=blocks;TTS.fail={system:false,online:false};
 const eng=ttsEngineNow();
 if(eng==='reader'){openFocusReader(blocks,from);return;}
 ttsHaltAudio();
 TTS.on=true;TTS.paused=false;TTS.q=ttsBuildQueue(from,eng);TTS.i=0;TTS.key=readKey();TTS.tok++;
 renderAppbar();setTimeout(ttsSpeak,80);
}
function ttsToggle(){
 if(!TTS.on)return;
 if(TTS.paused){TTS.paused=false;renderAppbar();ttsSpeak();}
 else{TTS.paused=true;TTS.tok++;ttsHaltAudio();renderAppbar();}
}
function ttsStop(){
 const was=TTS.on;
 TTS.on=false;TTS.paused=false;TTS.tok++;TTS.q=[];TTS.i=0;
 ttsHaltAudio();
 if(TTS.mark){TTS.mark.classList.remove('tts-now');TTS.mark=null;}
 if(was)renderAppbar();
}
function ttsAppbar(){
 const ac=$('abActions');if(!ac||view!=='today')return;
 const c=(S.pin&&chById(S.pin))||cur();if(c&&c.media)return;
 if(!TTS.on){ac.appendChild(iconBtn('tune','朗读设置（音色 / 引擎）',openTtsSettings));ac.appendChild(iconBtn('speaker','朗读本章',ttsStart));return;}
 ac.appendChild(iconBtn(TTS.paused?'speaker':'pauseI',TTS.paused?'继续朗读':'暂停朗读',ttsToggle));
 ac.appendChild(iconBtn('close','停止朗读',ttsStop));
}
ICONS.tune='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/></svg>';

/* ---------- 专注阅读：不依赖任何语音，一段一屏，可自动翻页 ---------- */
let FR=null;
function closeFocusReader(){
 if(FR){clearTimeout(FR.t);if(FR.el&&FR.el.parentNode)FR.el.parentNode.removeChild(FR.el);}
 FR=null;if(TTS.mark){TTS.mark.classList.remove('tts-now');TTS.mark=null;}
}
function openFocusReader(blocks,start){
 closeFocusReader();
 if(!blocks||!blocks.length)return;
 const ov=h('div','fr');ov.setAttribute('role','dialog');
 const top=h('div','fr-top'),cnt=h('span','fr-cnt'),cl=h('button','fr-x');cl.innerHTML=ic('close');cl.setAttribute('aria-label','关闭');
 top.appendChild(cnt);top.appendChild(cl);
 const body=h('div','fr-body'),txt=h('p','fr-txt');body.appendChild(txt);
 const bar=h('div','fr-bar'),prev=h('button',null,'‹ 上一段'),auto=h('button','fr-auto','自动翻页'),nx=h('button',null,'下一段 ›');
 const sp=document.createElement('select');sp.setAttribute('aria-label','阅读速度');
 [['慢',0.7],['中',1],['快',1.5]].forEach(function(o){const op=document.createElement('option');op.value=String(o[1]);op.textContent=o[0];sp.appendChild(op);});
 sp.value=String(ttsCfg().rate>=1.4?1.5:ttsCfg().rate<=0.8?0.7:1);
 bar.appendChild(prev);bar.appendChild(auto);bar.appendChild(sp);bar.appendChild(nx);
 ov.appendChild(top);ov.appendChild(body);ov.appendChild(bar);document.body.appendChild(ov);
 FR={el:ov,i:Math.max(0,Math.min(start||0,blocks.length-1)),auto:false,t:0};
 function paint(){
  const b=blocks[FR.i];txt.textContent=b.text;body.scrollTop=0;cnt.textContent=(FR.i+1)+' / '+blocks.length;
  prev.disabled=FR.i===0;nx.disabled=FR.i===blocks.length-1;
  ttsMarkNow(b.el);schedule();
 }
 function schedule(){
  clearTimeout(FR.t);if(!FR.auto)return;
  const b=blocks[FR.i],ms=Math.max(2600,b.text.length*230/Number(sp.value||1));
  FR.t=setTimeout(function(){if(FR&&FR.i<blocks.length-1){FR.i++;paint();}else{FR.auto=false;auto.classList.remove('on');}},ms);
 }
 prev.onclick=function(){if(FR.i>0){FR.i--;paint();}};
 nx.onclick=function(){if(FR.i<blocks.length-1){FR.i++;paint();}};
 auto.onclick=function(){FR.auto=!FR.auto;auto.classList.toggle('on',FR.auto);schedule();};
 sp.onchange=function(){const v=Number(sp.value);ttsCfg().rate=v;save();schedule();};
 cl.onclick=closeFocusReader;
 body.addEventListener('click',function(e){const r=body.getBoundingClientRect();if(e.clientX>r.left+r.width/2)nx.onclick();else prev.onclick();});
 paint();
}

/* ---------- 朗读设置：引擎 / 音色 / 语速 / 试听 ---------- */
function ttsPreview(){
 ttsStop();ttsHaltAudio();
 const item={el:null,text:'这是试听：门牌号、电话簿，还有出小区的大门。'},eng=ttsEngineNow(),tok=++TTS.tok,noop=function(){};
 if(eng==='online')speakOnline(item,tok,noop,function(){toast('在线语音不可用，请检查网络');});
 else if(eng==='system')speakSystem(item,tok,noop,function(){toast('系统朗读不可用，可改用在线语音或专注阅读');});
 else toast('当前是「专注阅读」，无需试听');
}
function openTtsSettings(){
 const cfg=ttsCfg(),body=h('div','tts-set');
 const eRow=h('div','st-opts two');
 [['auto','自动','系统不可用时自动换'],['system','系统语音','手机自带 TTS'],['online','在线语音','联网，音色更多'],['reader','专注阅读','不发声，逐段阅读']].forEach(function(o){
  const b=stOption(o[1],cfg.engine===o[0],function(){cfg.engine=o[0];save();eRow.querySelectorAll('.st-opt').forEach(function(x){x.classList.remove('on');});b.classList.add('on');paintVoices();},null,o[2]);
  eRow.appendChild(b);
 });
 body.appendChild(field('朗读方式',eRow));
 const vBox=h('div','tts-voices');body.appendChild(vBox);
 function paintVoices(){
  vBox.textContent='';
  const e=cfg.engine;
  if(e==='auto'||e==='system'){
   const sel=document.createElement('select');
   let vs=[];try{vs=ttsSupported()?speechSynthesis.getVoices():[];}catch(x){}
   const zh=vs.filter(function(v){return /zh|cmn|yue/i.test(v.lang);});
   const o0=document.createElement('option');o0.value='';o0.textContent=zh.length?'自动选择中文音色':(ttsSupported()?'（暂未读到音色，使用系统默认）':'（此浏览器不支持系统朗读）');sel.appendChild(o0);
   zh.forEach(function(v){const o=document.createElement('option');o.value=v.voiceURI||v.name;o.textContent=v.name+' · '+v.lang;sel.appendChild(o);});
   sel.value=cfg.voice||'';sel.onchange=function(){cfg.voice=sel.value;save();};
   vBox.appendChild(field('系统音色',sel));
  }
  if(e==='auto'||e==='online'){
   const sel2=document.createElement('select');
   ONLINE_VOICES.forEach(function(v){const o=document.createElement('option');o.value=String(v[0]);o.textContent=v[1];sel2.appendChild(o);});
   sel2.value=String(cfg.per||0);sel2.onchange=function(){cfg.per=Number(sel2.value);save();};
   vBox.appendChild(field('在线音色',sel2));
  }
  if(e!=='reader'){
   const rg=document.createElement('input');rg.type='range';rg.min='0.6';rg.max='1.6';rg.step='0.1';rg.value=String(cfg.rate||1);
   const lab=h('span','tts-rate',(cfg.rate||1).toFixed(1)+'×');
   rg.oninput=function(){cfg.rate=Number(rg.value);lab.textContent=cfg.rate.toFixed(1)+'×';};rg.onchange=function(){save();};
   const rw=h('div','tts-rw');rw.appendChild(rg);rw.appendChild(lab);vBox.appendChild(field('语速',rw));
   const pv=h('button','btn sm','试听');pv.onclick=ttsPreview;vBox.appendChild(pv);
  }
  const note=h('p','muted',(e==='online'||e==='auto')?'在线语音由百度语音合成提供：需要联网，朗读的文字会发送到该服务；服务不可用时会自动改为专注阅读。':'系统语音不会联网。');
  vBox.appendChild(note);
 }
 paintVoices();
 try{if(ttsSupported()){speechSynthesis.onvoiceschanged=function(){if(vBox.isConnected)paintVoices();};}}catch(e){}
 openSheet('朗读设置',body,function(){ttsStop();},'完成');
}

/* =====================================================================
   v14 · 独立的视频 / 音频播放页 + 课程管理分类导航 + 在线语音多源
   ===================================================================== */

/* ---------- 播放页：点击单个视频 / 音频章节进入（不再是文章详情） ---------- */
function curChapter(){return (S.pin&&chById(S.pin))||cur();}
function mediaSeqIndex(c){return seq().indexOf(c.id);}
function playerGoto(id){
 const q=seq(),i=q.indexOf(id);if(i<0)return;
 S.idx=i;S.pin='';S.ui.mediaAutoplay=true;save();render();window.scrollTo({top:0,behavior:'instant'});
}
function buildMediaPlayer(parent,c,co,kind){
 const key=co.id+':'+c.id,reg=MEDIA[key],meta=c.media||{};
 if(!reg){
  const un=h('div','media-un');
  un.appendChild(h('p','mu-t','媒体文件还没有连接'));
  un.appendChild(h('p','mu-s','「'+(meta.fname||c.t)+'」'+(meta.size?' · '+fmtSize(meta.size):'')+' 保存在你的手机里。刷新或重新打开页面后，需要重新选择当初导入的文件夹。'));
  const b=h('button','btn pri','重新选择文件夹');
  b.onclick=function(){relinkMedia(co.id,true).then(function(ok){if(ok){toast('已连接');render();}else relinkByPicker(co.id);});};
  un.appendChild(b);parent.appendChild(un);
  relinkMedia(co.id,false).then(function(ok){if(ok&&view==='player'&&curChapter()&&curChapter().id===c.id)render();});
  return;
 }
 if(MEDIA_URL.key!==key){if(MEDIA_URL.url){try{URL.revokeObjectURL(MEDIA_URL.url);}catch(e){}}MEDIA_URL={key:key,url:URL.createObjectURL(reg.file)};}
 const wrap=h('div','media-wrap '+kind),el=document.createElement(kind==='video'?'video':'audio');
 el.className='media-el';el.controls=true;el.preload='metadata';el.setAttribute('playsinline','');el.setAttribute('webkit-playsinline','');el.src=MEDIA_URL.url;
 el.playbackRate=Number(S.ui.mediaRate)||1;
 if(kind==='audio'){const art=h('div','media-art');art.innerHTML=ic('speaker');wrap.appendChild(art);}
 wrap.appendChild(el);parent.appendChild(wrap);
 S.ui.mediaPos=S.ui.mediaPos||{};
 const pos=Number(S.ui.mediaPos[key])||0;let lastSave=0;
 el.addEventListener('loadedmetadata',function(){
  el.playbackRate=Number(S.ui.mediaRate)||1;
  if(pos>2&&isFinite(el.duration)&&pos<el.duration-3){try{el.currentTime=pos;}catch(e){}}
  if(S.ui.mediaAutoplay){S.ui.mediaAutoplay=false;const pr=el.play();if(pr&&pr.catch)pr.catch(function(){});}
  if(isFinite(el.duration)&&!meta.dur){meta.dur=Math.round(el.duration);c.s=fmtClock(el.duration)+' · '+fmtSize(meta.size);save();}
 });
 el.addEventListener('timeupdate',function(){stLast=Date.now();const t=Date.now();if(t-lastSave>3000){lastSave=t;S.ui.mediaPos[key]=Math.floor(el.currentTime);save();}});
 el.addEventListener('pause',function(){S.ui.mediaPos[key]=Math.floor(el.currentTime);save();});
 el.addEventListener('ended',function(){
  S.ui.mediaPos[key]=0;
  if(!S.learn[c.id])S.learn[c.id]={at:today(),done:[]};
  save();
  if(S.ui.mediaAuto!==false)mediaNext();else renderPlayer();
 });
}
function renderPlayer(){
 const box=$('v-player');box.textContent='';
 const c=curChapter();
 if(!c||!c.media){view='today';render();return;}
 const co=curCourse(),kind=co.media||c.media.kind||'video',q=seq(),idx=q.indexOf(c.id);
 const wrap=h('div','pl-page');box.appendChild(wrap);
 buildMediaPlayer(wrap,c,co,kind);
 /* 标题 */
 const hd=h('div','pl-hd');
 hd.appendChild(h('div','pl-course',coMeta(co).title));
 hd.appendChild(h('h2','pl-title',c.t));
 const meta=c.media||{};
 hd.appendChild(h('div','pl-meta',[(idx+1)+' / '+q.length,meta.dur?fmtClock(meta.dur):'',meta.size?fmtSize(meta.size):'',S.learn[c.id]?'已学习':''].filter(Boolean).join(' · ')));
 wrap.appendChild(hd);
 /* 控制 */
 const ctl=h('div','media-ctl');
 const prev=h('button','mc-nav','‹ 上一集'),next=h('button','mc-nav','下一集 ›');
 prev.disabled=idx<=0;next.disabled=idx<0||idx>=q.length-1;
 prev.onclick=function(){playerGoto(q[idx-1]);};next.onclick=function(){playerGoto(q[idx+1]);};
 ctl.appendChild(prev);ctl.appendChild(next);
 const sp=document.createElement('select');sp.setAttribute('aria-label','播放速度');
 [0.75,1,1.25,1.5,2].forEach(function(v){const o=document.createElement('option');o.value=String(v);o.textContent=v+'×';sp.appendChild(o);});
 sp.value=String(Number(S.ui.mediaRate)||1);
 sp.onchange=function(){S.ui.mediaRate=Number(sp.value);const m=box.querySelector('.media-el');if(m)m.playbackRate=S.ui.mediaRate;save();};
 ctl.appendChild(sp);
 const au=h('button','mc-auto'+(S.ui.mediaAuto!==false?' on':''),'自动下一集');
 au.onclick=function(){S.ui.mediaAuto=!(S.ui.mediaAuto!==false);au.classList.toggle('on',S.ui.mediaAuto);save();};
 ctl.appendChild(au);wrap.appendChild(ctl);
 /* 选集列表 */
 const lh=h('div','pl-lh');lh.appendChild(h('span',null,'选集'));lh.appendChild(h('small',null,q.length+' 集'));wrap.appendChild(lh);
 const list=h('div','pl-list');
 q.forEach(function(id,i){
  const ch=chById(id);if(!ch)return;
  const r=h('button','pl-row'+(id===c.id?' on':''));
  r.appendChild(h('span','pl-n',id===c.id?'▶':String(i+1)));
  const t=h('span','pl-t');t.appendChild(h('b',null,ch.t));
  const mm=ch.media||{};t.appendChild(h('small',null,[mm.dur?fmtClock(mm.dur):'',mm.size?fmtSize(mm.size):''].filter(Boolean).join(' · ')));
  r.appendChild(t);
  if(S.learn[id])r.appendChild(h('span','pl-ok','✓'));
  r.onclick=function(){if(id!==c.id)playerGoto(id);};
  list.appendChild(r);
 });
 wrap.appendChild(list);
 requestAnimationFrame(function(){const on=list.querySelector('.pl-row.on');if(on&&list.children.length>6)on.scrollIntoView({block:'nearest'});});
}
/* 媒体章节不再走文章详情 */
function mediaNext(){
 const q=seq(),c=curChapter(),i=q.indexOf(c.id);
 if(i>=0&&i+1<q.length){S.idx=i+1;S.pin='';S.ui.mediaAutoplay=true;save();render();window.scrollTo({top:0,behavior:'instant'});}
 else{toast('已经是最后一集');renderPlayer();}
}
STUDY_VIEWS.player=1;

/* ---------- 课程管理：顶部分类导航（文字 / 视频 / 语音） ---------- */
function courseType(co){return co.media==='video'?'video':co.media==='audio'?'audio':'text';}
function tidyCourseManage(){
 if(view!=='course-manage')return;
 const box=$('v-course-manage');if(!box||box.querySelector('.cm-tabs'))return;
 const rows=Array.from(box.querySelectorAll('.course-manage-row')),types=COURSES.map(courseType);
 const cnt={text:0,video:0,audio:0};types.forEach(function(t){cnt[t]++;});
 let tab=S.ui.cmTab;if(!tab||(tab==='text'&&!cnt.text&&(cnt.video||cnt.audio)))tab=cnt.text?'text':(cnt.video?'video':(cnt.audio?'audio':'text'));
 const bar=h('div','cm-tabs'),empty=h('p','cm-empty'),imp=h('div','cm-import');
 box.insertBefore(bar,box.firstChild);box.appendChild(empty);box.appendChild(imp);
 function apply(){
  S.ui.cmTab=tab;
  bar.textContent='';
  [['text','文字'],['video','视频'],['audio','语音']].forEach(function(t){
   const b=h('button',tab===t[0]?'on':'');b.appendChild(document.createTextNode(t[1]));b.appendChild(h('i',null,String(cnt[t[0]])));
   b.onclick=function(){tab=t[0];apply();};bar.appendChild(b);
  });
  rows.forEach(function(r,i){r.style.display=(types[i]===tab)?'':'none';});
  empty.textContent=cnt[tab]?'':(tab==='text'?'还没有文字课程。点右上角新建，或到「数据」导入档案。':tab==='video'?'还没有视频课程。读取手机里的视频文件夹，每个视频是一集。':'还没有语音课程。读取手机里的音频文件夹，每个音频是一集。');
  empty.style.display=cnt[tab]?'none':'';
  imp.textContent='';
  if(tab!=='text'){
   const KN=tab==='video'?'视频':'音频';
   const r=h('button','cm-imp-row');
   const i=h('span','ci-ic');i.innerHTML=ic(tab==='video'?'monitor':'speaker');r.appendChild(i);
   const m=h('span','ci-mid');m.appendChild(h('b',null,'导入'+KN+'课程'));m.appendChild(h('small',null,'读取文件夹里的'+KN+'，每个文件一集'));r.appendChild(m);
   r.appendChild(h('span','ci-arr','›'));r.onclick=function(){openMediaImport(tab);};imp.appendChild(r);
  }
 }
 apply();
}

/* ---------- 在线语音：多个来源依次尝试，复用同一个 Audio（兼容 iOS / 微信等对自动播放的限制） ---------- */
const TTS_PROVIDERS=[
 {id:'baidu',name:'百度',url:function(t,c,spd){return 'https://tts.baidu.com/text2audio?tex='+encodeURIComponent(t)+'&cuid=dim-reader&lan=zh&ctp=1&pdt=301&vol=9&rate=32&per='+(c.per||0)+'&spd='+spd;}},
 {id:'youdao',name:'有道',url:function(t){return 'https://dict.youdao.com/dictvoice?audio='+encodeURIComponent(t)+'&le=zh';}},
 {id:'sogou',name:'搜狗',url:function(t,c,spd){return 'https://fanyi.sogou.com/reventondc/synthesis?text='+encodeURIComponent(t)+'&speed='+Math.max(0.5,(spd/5)).toFixed(1)+'&lang=zh-CHS&from=translateweb&speaker=6';}},
 {id:'baidu2',name:'百度（备用）',url:function(t,c,spd){return 'https://tts.baidu.com/text2audio?lan=zh&ie=UTF-8&spd='+spd+'&text='+encodeURIComponent(t);}},
 {id:'google',name:'谷歌',url:function(t){return 'https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=zh-CN&q='+encodeURIComponent(t);}}
];
const SILENT_WAV='data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAAA';
function ttsAudio(){
 if(!TTS.el){TTS.el=new Audio();TTS.el.preload='auto';TTS.el.setAttribute('playsinline','');}
 return TTS.el;
}
function ttsUnlock(){
 try{const a=ttsAudio();a.src=SILENT_WAV;const p=a.play();if(p&&p.catch)p.catch(function(){});}catch(e){}
}
function provList(){
 const cfg=ttsCfg(),all=TTS_PROVIDERS.slice();
 if(cfg.provider&&cfg.provider!=='auto'){const p=all.find(function(x){return x.id===cfg.provider;});return p?[p]:all;}
 if(cfg.provOk){const i=all.findIndex(function(x){return x.id===cfg.provOk;});if(i>0){const p=all.splice(i,1)[0];all.unshift(p);}}
 return all;
}
/* 依次尝试每个来源，成功后记住；全部失败才回调 fail */
function playOnlineText(text,tok,hooks,fail){
 const cfg=ttsCfg(),spd=Math.max(1,Math.min(9,Math.round(5*(cfg.rate||1)))),list=provList();
 let k=0;
 function tryNext(){
  if(tok!==TTS.tok)return;
  if(k>=list.length){fail('all');return;}
  const p=list[k++],a=ttsAudio();
  let settled=false,started=false;
  const clean=function(){a.onplaying=a.onended=a.onerror=null;};
  const bad=function(){if(settled)return;settled=true;clean();try{a.pause();}catch(e){}tryNext();};
  a.onplaying=function(){started=true;if(hooks.onstart)hooks.onstart();};
  a.onended=function(){if(settled)return;settled=true;clean();if(!started)started=true;cfg.provOk=p.id;hooks.onend();};
  a.onerror=bad;
  try{a.src=p.url(text,cfg,spd);a.playbackRate=1;const pr=a.play();if(pr&&pr.catch)pr.catch(function(){if(!started)bad();});}catch(e){bad();return;}
  setTimeout(function(){if(tok===TTS.tok&&!started&&!settled)bad();},6500);
 }
 tryNext();
}
function speakOnline(item,tok,next,fail){
 playOnlineText(item.text,tok,{onstart:function(){if(tok===TTS.tok)ttsMarkNow(item.el);},onend:next},fail);
}
function ttsHaltAudio(){
 try{if(ttsSupported())speechSynthesis.cancel();}catch(e){}
 if(TTS.el){try{TTS.el.onplaying=TTS.el.onended=TTS.el.onerror=null;TTS.el.pause();}catch(e){}}
}
function ttsStart(){
 const blocks=ttsBlocks();if(!blocks.length){toast('这一章没有可朗读的文字');return;}
 let from=blocks.findIndex(function(b){return b.el.getBoundingClientRect().bottom>120;});if(from<0)from=0;
 TTS.blocks=blocks;TTS.fail={system:false,online:false};
 const eng=ttsEngineNow();
 if(eng==='reader'){openFocusReader(blocks,from);return;}
 ttsUnlock();                       /* 在点击手势里解锁音频，之后各段才允许自动续播 */
 ttsHaltAudioKeep();
 TTS.on=true;TTS.paused=false;TTS.q=ttsBuildQueue(from,eng);TTS.i=0;TTS.key=readKey();TTS.tok++;
 renderAppbar();setTimeout(ttsSpeak,60);
}
function ttsHaltAudioKeep(){try{if(ttsSupported())speechSynthesis.cancel();}catch(e){}}
function ttsPreview(){
 ttsStop();
 const item={el:null,text:'这是试听：门牌号、电话簿，还有出小区的大门。'},eng=ttsEngineNow(),tok=++TTS.tok,noop=function(){};
 if(eng==='online'){ttsUnlock();speakOnline(item,tok,noop,function(){toast('在线语音不可用：请点「检测在线语音」查看各来源');});}
 else if(eng==='system')speakSystem(item,tok,noop,function(){toast('系统朗读不可用，可改用在线语音或专注阅读');});
 else toast('当前是「专注阅读」，无需试听');
}
/* 检测各来源是否可用（需要点击触发） */
function ttsProbe(listEl,btn){
 btn.disabled=true;btn.textContent='检测中…';listEl.textContent='';
 const rows={};
 TTS_PROVIDERS.forEach(function(p){const r=h('div','tp-row');r.appendChild(h('span','tp-n',p.name));const s=h('span','tp-s','等待');r.appendChild(s);listEl.appendChild(r);rows[p.id]=s;});
 let i=0,firstOk=null;
 function one(){
  if(i>=TTS_PROVIDERS.length){
   btn.disabled=false;btn.textContent='重新检测';
   if(firstOk){const cfg=ttsCfg();cfg.provOk=firstOk;save();toast('可用来源：'+TTS_PROVIDERS.find(function(x){return x.id===firstOk;}).name);}
   else toast('所有在线来源都连不上，请检查网络；也可以用系统语音或专注阅读');
   return;
  }
  const p=TTS_PROVIDERS[i++],s=rows[p.id];s.textContent='检测中…';
  const a=new Audio();let done=false;
  const fin=function(ok){if(done)return;done=true;try{a.pause();a.src='';}catch(e){}s.textContent=ok?'✓ 可用':'✗ 连不上';s.className='tp-s '+(ok?'ok':'no');if(ok&&!firstOk)firstOk=p.id;setTimeout(one,50);};
  a.onplaying=function(){fin(true);};a.oncanplaythrough=function(){fin(true);};a.onerror=function(){fin(false);};
  a.volume=0.01;
  try{a.src=p.url('你好',ttsCfg(),5);const pr=a.play();if(pr&&pr.catch)pr.catch(function(){/* 自动播放被拦不等于连不上，等 canplay */});}catch(e){fin(false);}
  setTimeout(function(){fin(false);},6000);
 }
 one();
}
function openTtsSettings(){
 const cfg=ttsCfg(),body=h('div','tts-set');
 const eRow=h('div','st-opts two');
 [['auto','自动','系统不可用时自动换'],['system','系统语音','手机自带 TTS'],['online','在线语音','联网，多来源自动切换'],['reader','专注阅读','不发声，逐段阅读']].forEach(function(o){
  const b=stOption(o[1],cfg.engine===o[0],function(){cfg.engine=o[0];save();eRow.querySelectorAll('.st-opt').forEach(function(x){x.classList.remove('on');});b.classList.add('on');paintVoices();},null,o[2]);
  eRow.appendChild(b);
 });
 body.appendChild(field('朗读方式',eRow));
 const vBox=h('div','tts-voices');body.appendChild(vBox);
 function paintVoices(){
  vBox.textContent='';
  const e=cfg.engine;
  if(e==='auto'||e==='system'){
   const sel=document.createElement('select');
   let vs=[];try{vs=ttsSupported()?speechSynthesis.getVoices():[];}catch(x){}
   const zh=vs.filter(function(v){return /zh|cmn|yue/i.test(v.lang);});
   const o0=document.createElement('option');o0.value='';o0.textContent=zh.length?'自动选择中文音色':(ttsSupported()?'（暂未读到音色，使用系统默认）':'（此浏览器不支持系统朗读）');sel.appendChild(o0);
   zh.forEach(function(v){const o=document.createElement('option');o.value=v.voiceURI||v.name;o.textContent=v.name+' · '+v.lang;sel.appendChild(o);});
   sel.value=cfg.voice||'';sel.onchange=function(){cfg.voice=sel.value;save();};
   vBox.appendChild(field('系统音色',sel));
  }
  if(e==='auto'||e==='online'){
   const sp=document.createElement('select');
   [['auto','自动（哪个能用用哪个）']].concat(TTS_PROVIDERS.map(function(p){return [p.id,p.name];})).forEach(function(v){const o=document.createElement('option');o.value=v[0];o.textContent=v[1];sp.appendChild(o);});
   sp.value=cfg.provider||'auto';sp.onchange=function(){cfg.provider=sp.value;save();};
   vBox.appendChild(field('在线来源',sp));
   const sel2=document.createElement('select');
   ONLINE_VOICES.forEach(function(v){const o=document.createElement('option');o.value=String(v[0]);o.textContent=v[1];sel2.appendChild(o);});
   sel2.value=String(cfg.per||0);sel2.onchange=function(){cfg.per=Number(sel2.value);save();};
   vBox.appendChild(field('在线音色（仅百度来源可选）',sel2));
   const pr=h('div','tp-list'),pb=h('button','btn sm','检测在线语音');pb.onclick=function(){ttsUnlock();ttsProbe(pr,pb);};
   vBox.appendChild(pb);vBox.appendChild(pr);
  }
  if(e!=='reader'){
   const rg=document.createElement('input');rg.type='range';rg.min='0.6';rg.max='1.6';rg.step='0.1';rg.value=String(cfg.rate||1);
   const lab=h('span','tts-rate',(cfg.rate||1).toFixed(1)+'×');
   rg.oninput=function(){cfg.rate=Number(rg.value);lab.textContent=cfg.rate.toFixed(1)+'×';};rg.onchange=function(){save();};
   const rw=h('div','tts-rw');rw.appendChild(rg);rw.appendChild(lab);vBox.appendChild(field('语速',rw));
   const pv=h('button','btn sm','试听');pv.onclick=ttsPreview;vBox.appendChild(pv);
  }
  vBox.appendChild(h('p','muted',(e==='online'||e==='auto')?'在线语音会把朗读的文字发送到第三方语音服务（百度 / 有道 / 搜狗 / 谷歌），需要联网；全部不可用时自动改为专注阅读。':'系统语音不会联网。'));
 }
 paintVoices();
 try{if(ttsSupported()){speechSynthesis.onvoiceschanged=function(){if(vBox.isConnected)paintVoices();};}}catch(e){}
 openSheet('朗读设置',body,function(){ttsStop();},'完成');
}

/* =====================================================================
   v9 · 补丁层（B）：导入导出的格式说明 / 模板 / 解析
   ===================================================================== */

/* ---------- 解析：Markdown 简易课程（只用标题层级，无需写 JSON） ---------- */
function mdSimpleToArchive(text){
 const lines=String(text||'').replace(/\r/g,'').split('\n');
 const courses=[];let co=null,ch=null,mod=null,fence=false,modN=0,skipSec=false,usedMods={};
 const letter=function(n){return n<26?String.fromCharCode(65+n):'M'+(n+1);};
 function finishChapter(){
  if(!ch||!co)return;
  const raw=ch.body.slice();
  while(raw.length&&!raw[0].trim())raw.shift();
  const sum=[];
  while(raw.length&&/^\s*>/.test(raw[0])){sum.push(raw.shift().replace(/^\s*>\s?/,'').trim());}
  const c={id:ch.id,m:ch.m,title:ch.title,summary:sum.join(' ').trim(),content:raw.join('\n').trim()};
  co.chapters.push(c);ch=null;
 }
 function finishCourse(){
  if(!co)return;finishChapter();
  const intro=co._intro.slice();
  while(intro.length&&!intro[0].trim())intro.shift();
  const sub=[];
  while(intro.length&&/^\s*>/.test(intro[0])){sub.push(intro.shift().replace(/^\s*>\s?/,'').trim());}
  co.subtitle=sub.join(' ').trim();
  co.description=intro.join('\n').trim();
  co.modules=co.modules.filter(function(m){return co.chapters.some(function(c){return c.m===m.k;});});
  delete co._intro;delete co._ids;
  courses.push(co);co=null;mod=null;modN=0;skipSec=false;usedMods={};
 }
 lines.forEach(function(line){
  const t=line.trim();
  if(/^```/.test(t)){fence=!fence;}
  let m;
  if(!fence&&(m=line.match(/^#\s+(.+?)\s*#*\s*$/))){finishCourse();co={title:m[1].trim(),subtitle:'',description:'',modules:[],chapters:[],_intro:[],_ids:{}};return;}
  if(!co)return;
  if(!fence&&(m=line.match(/^##\s+(.+?)\s*#*\s*$/))){
   finishChapter();
   const ttl=m[1].trim();
   if(/^(目录|contents?|toc|table of contents)$/i.test(ttl)){mod=null;skipSec=true;return;}
   skipSec=false;
   let k=null;const mk=ttl.match(/模块\s*([A-Za-z0-9]{1,3})/);
   if(mk)k=mk[1];
   if(!k||usedMods[k]){do{k=letter(modN++);}while(usedMods[k]);}
   let nm=ttl,tag='';const mt=ttl.match(/^(.*?)[\s\u3000]*[（(]([^）)]+)[）)]\s*$/);
   if(mt){nm=mt[1].trim();tag=mt[2].trim();}
   mod={k:k,n:nm,tag:tag};usedMods[k]=1;co.modules.push(mod);return;
  }
  if(!fence&&(m=line.match(/^###\s+(.+?)\s*#*\s*$/))){
   finishChapter();skipSec=false;
   if(!mod){mod={k:letter(modN++),n:'章节',tag:''};usedMods[mod.k]=1;co.modules.push(mod);}
   let id='',title=m[1].trim();
   const mm=title.match(/^([A-Za-z]{0,3}\d+(?:[.\-]\d+)*)[\s:：、.．]+(.+)$/);
   if(mm){id=mm[1];title=mm[2].trim();}
   if(!id||co._ids[id]){let n=co.chapters.length+1;id=mod.k+n;while(co._ids[id])id=mod.k+(++n);}
   co._ids[id]=1;
   ch={id:id,m:mod.k,title:title,body:[]};return;
  }
  if(ch)ch.body.push(line);else if(!skipSec&&!mod)co._intro.push(line);
 });
 finishCourse();
 if(!courses.length)return null;
 return {app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:'course',courses:courses};
}

/* ---------- 解析：CSV（完整写法：json 列；简易写法：只填 id / title / content） ---------- */
let csvSkipped=0;
function csvToArchive(text){
 const rows=parseCsv(text);if(!rows.length)return null;
 const head=rows[0].map(function(x){return String(x).replace(/^\uFEFF/,'').trim().toLowerCase();});
 const ti=head.indexOf('type'),ji=head.indexOf('json'),ci=head.indexOf('course'),ii=head.indexOf('id'),tt=head.indexOf('title'),cc=head.indexOf('content'),si=head.indexOf('summary');
 if(ti<0)return null;
 const o={app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:'all',courses:[]},map={},hb={},lastMod={};
 csvSkipped=0;
 function get(key){key=key||'c1';if(!map[key]){map[key]={state:{}};o.courses.push(map[key]);}return map[key];}
 rows.slice(1).forEach(function(r){
  const cell=function(i){return i>=0&&r[i]!=null?String(r[i]):'';};
  const type=cell(ti).trim().toLowerCase(),key=ci>=0?cell(ci).trim():'c1',id=cell(ii).trim();
  if(!type)return;
  let j=null;const raw=cell(ji).trim();
  if(raw){try{j=JSON.parse(raw);}catch(e){csvSkipped++;return;}}
  if(!j){
   /* 简易写法 */
   const t=cell(tt).trim(),ct=cell(cc),c0=get(key);
   if(type==='course'){if(!t)return;j={title:t,description:ct.trim(),subtitle:ct.trim().split(/[。\n]/)[0].slice(0,60)};}
   else if(type==='module'){if(!id&&!t)return;j={k:id||String.fromCharCode(65+(c0.modules||[]).length),n:t||('模块 '+id),tag:ct.trim()};}
   else if(type==='chapter'){
    if(!id&&!t)return;
    const sm=si>=0?cell(si).trim():ct.trim().split('\n')[0].replace(/\s+/g,' ').slice(0,40);
    j={id:id||String((c0.chapters||[]).length+1),m:lastMod[key]||'',title:t||id,summary:sm,content:ct.trim()};
   }
   else if(type==='gloss'){if(!id)return;j={term:id,full:t,say:ct.trim()};}
   else return;
  }
  if(type==='meta'){o.app=j.app||o.app;o.kind=j.kind||o.kind;o.ver=j.ver||o.ver;o.scope=j.scope||o.scope;o.exportedAt=j.exportedAt;return;}
  const c=get(key),st=c.state;
  if(type==='course')Object.assign(c,j);
  else if(type==='module'){(c.modules=c.modules||[]).push(j);lastMod[key]=j.k;}
  else if(type==='chapter'){if(!j.m&&lastMod[key])j.m=lastMod[key];(c.chapters=c.chapters||[]).push(j);}
  else if(type==='deep')(c.deep=c.deep||{})[id]=j;
  else if(type==='long')(c.long=c.long||{})[id]=j;
  else if(type==='route'){(c.routes=c.routes||{})[id]=j.ids||[];(c.routeNames=c.routeNames||{})[id]=j.name||id;}
  else if(type==='resource')c[id]=j.v;
  else if(type==='handbook'){(hb[key]=hb[key]||[])[Number(id)||0]=j.part||'';}
  else if(type==='state')Object.assign(st,j);
  else if(type==='progress'){if(j.status)(st.status=st.status||{})[id]=j.status;if(j.learn)(st.learn=st.learn||{})[id]=j.learn;}
  else if(type==='mark')(st.marks=st.marks||[]).push(j);
  else if(type==='case')(st.cases=st.cases||[]).push(j);
  else if(type==='gloss')(st.gloss=st.gloss||[]).push(j);
  else if(type==='studylog')st.studyLog=j;
 });
 Object.keys(hb).forEach(function(k){get(k).handbook=hb[k].join('');});
 o.courses.forEach(function(c){if(!Object.keys(c.state).length)delete c.state;});
 return o.courses.length?o:null;
}

/* ---------- 解析入口：识别 JSON / Markdown / CSV ---------- */
function unwrapJs(t){
 if(t[0]==='{'||t[0]==='[')return t;
 const m=t.match(/^\s*(?:export\s+default|module\.exports\s*=|(?:const|let|var)\s+[\w$]+\s*=|window\.[\w$]+\s*=|globalThis\.[\w$]+\s*=)\s*/);
 if(m){const r=t.slice(m[0].length).trim().replace(/;\s*$/,'');if(r[0]==='{'||r[0]==='[')return r;}
 return t;
}
function parseArchiveText(raw){
 const t=unwrapJs(String(raw||'').replace(/^\uFEFF/,'').trim());
 if(!t)return {err:'内容为空，请选择文件或粘贴档案内容。'};
 let o=null,fmt='',warn='';
 if(t[0]==='{'||t[0]==='['){
  try{o=JSON.parse(t);fmt='JSON';}
  catch(e){return {err:'JSON 格式有误：'+e.message+'。常见原因：多 / 少了逗号、用了中文引号“ ”、字符串里直接换行（应写成 \\n）。'};}
 }
 else if(t.indexOf('```json')>=0||/^#\s+\S/.test(t)){
  fmt='Markdown';
  o=t.indexOf('```json')>=0?mdToArchive(t):null;
  if(o&&!(Array.isArray(o)||o.courses||o.app||o.kind))o=null;
  if(!o)o=mdSimpleToArchive(t);
  if(!o)return {err:'Markdown 无法识别：手写课程请以「# 课程名称」开头、用「### 章节标题」划分章节；完整档案请保留文末 ```json 数据存档代码块。'};
 }
 else if(/^"?type"?\s*,/i.test(t.split(/\r?\n/)[0])){
  o=csvToArchive(t);fmt='CSV';
  if(!o)return {err:'CSV 里没有可导入的行：请确认表头以 type 开头（type,course,id,title,content,json），且至少有一行 course 与 chapter。'};
  if(csvSkipped)warn='有 '+csvSkipped+' 行的 json 列不是合法 JSON，已跳过。';
 }
 else return {err:'无法识别格式：请使用 .json、.md 或 .csv 文件（UTF-8 编码）。格式要求见下方「格式要求与示例」。'};
 const n=normalizeArchive(o);
 if(!n)return {err:'这不是可导入的档案：缺少 courses 课程列表（或课程缺少 title）。格式要求见下方「格式要求与示例」。'};
 return {obj:n,fmt:fmt,warn:warn};
}

/* ---------- 导出渲染：由「档案对象」生成 JSON / Markdown / CSV（导出与模板共用） ---------- */
/* chTitle 支持示例数据：模板里的章节编号能显示示例标题 */
function chTitle(id){const c=(window.__chSample&&window.__chSample[id])||chById(id);return c?id+' '+(c.t||c.title||''):(id||'');}

/* ---------- 模板数据 ---------- */
function sampleCourse(){
 return {id:'net-basic',title:'网络基础入门',subtitle:'从 IP 地址到 DNS，一周搞懂桌面运维必备的网络知识',
  description:'面向桌面运维新人的网络入门课：先理解地址与解析，再学会用命令定位「上不了网」的原因。',cover:'',
  modules:[{k:'A',n:'模块 A · 网络基础',tag:'必修'},{k:'B',n:'模块 B · 排障',tag:'进阶'}],
  chapters:[
   {id:'A1',m:'A',t:'IP 地址与子网掩码',s:'判断两台电脑在不在同一个网段',terms:'IP 地址｜子网掩码｜默认网关',plain:[['门牌号','IP 地址'],['小区范围','子网掩码']],cmds:[],pits:[],
    gate:'能说清为什么两台电脑互相 ping 不通',quiz:{q:'192.168.1.20/24 和 192.168.2.20/24 在同一网段吗？',a:'不在。/24 表示前三段相同才算同一网段。',hint:'先看前三段'}},
   {id:'A2',m:'A',t:'DNS 解析',s:'域名怎么变成 IP',terms:'DNS｜A 记录',plain:[['电话簿','DNS']],cmds:[],pits:[],
    gate:'能解释「能 ping 通 IP 却打不开网页」',quiz:{q:'能 ping 通 8.8.8.8 却打不开网页，先查什么？',a:'先查 DNS：nslookup 域名。',hint:''}},
   {id:'B1',m:'B',t:'用 ping 与 nslookup 排障',s:'三条命令定位断点',terms:'ping｜nslookup｜tracert',plain:[['敲门','ping']],cmds:[],pits:[],
    gate:'能按顺序用三条命令定位断点',quiz:{q:'先 ping 网关还是先 ping 外网？',a:'先网关，由近及远。',hint:''}}
  ],
  long:{
   A1:{title:'IP 地址与子网掩码',concept:[{p:'IP 地址相当于电脑的门牌号。'},{p:'子网掩码决定哪些门牌号属于同一个小区。'}],practice:[{t:'查看本机 IP',cmd:'ipconfig /all',d:'记下 IPv4 地址、子网掩码、默认网关。'}],guide:[],quiz:{q:'192.168.1.20/24 和 192.168.2.20/24 在同一网段吗？',a:'不在。'}},
   A2:{title:'DNS 解析',concept:[{p:'DNS 像一本电话簿，把域名翻译成 IP 地址。'}],practice:[{t:'解析一个域名',cmd:'nslookup www.example.com',d:'看返回的 Address 是否正常。'}],guide:[],quiz:{q:'能 ping 通 IP 却打不开网页，先查什么？',a:'先查 DNS。'}}
  },
  routes:{all:['A1','A2','B1']},routeNames:{all:'全章节顺序'}};
}
function sampleState(){
 return {route:'all',idx:1,day:3,mode:'long',pin:'',status:{A1:'green',A2:'yellow'},learn:{A1:{at:'2026-11-06',done:[]},A2:{at:'2026-11-07',done:[]}},
  marks:[{id:'m1',ch:'A1',t:'子网掩码决定哪些门牌号属于同一个小区',n:'考试重点',ty:'solid',co:'b',tags:['重点'],at:1793000000000}],
  cases:[{no:'NET-001',sym:'能 ping 通 IP 却打不开网页',path:'1. nslookup 域名\n2. 检查 DNS 服务器地址',cause:'DNS 服务器地址填错',lesson:'先 nslookup，再查 DNS 设置',ch:'A2'}],
  gloss:[{term:'DNS',full:'Domain Name System',say:'把域名翻译成 IP 的电话簿'}],
  studyLog:{'2026-11-06':30,'2026-11-07':45}};
}
function sampleObj(scope){
 const c=sampleCourse(),st=sampleState();
 const meta={id:c.id,title:c.title,subtitle:c.subtitle,description:c.description,cover:''};
 let courses,counts;
 if(scope==='all'){const x=Object.assign({},c);x.state={route:st.route,idx:st.idx,day:st.day,mode:st.mode,pin:st.pin,status:st.status,learn:st.learn,cases:st.cases,gloss:st.gloss,marks:st.marks,studyLog:st.studyLog};courses=[x];}
 else if(scope==='course'){courses=[c];}
 else if(scope==='handbook'){courses=[Object.assign({},meta,{handbook:'# 网络基础入门 · 学习手册\n\n## A1 IP 地址与子网掩码\n\nIP 地址相当于电脑的门牌号。\n\n## A2 DNS 解析\n\nDNS 像一本电话簿。'})];}
 else if(scope==='marks'){courses=[Object.assign({},meta,{state:{marks:st.marks}})];}
 else if(scope==='progress'){courses=[Object.assign({},meta,{state:{route:st.route,idx:st.idx,day:st.day,mode:st.mode,pin:st.pin,status:st.status,learn:st.learn,studyLog:st.studyLog}})];}
 else{courses=[Object.assign({},meta,{state:{cases:st.cases,gloss:st.gloss}})];}
 const sx=courses[0].state||{};
 counts={marks:(sx.marks||[]).length,cases:(sx.cases||[]).length,gloss:(sx.gloss||[]).length,learned:Object.keys(sx.learn||{}).length};
 return {app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:scope,exportedAt:'2026-11-08T08:00:00.000Z',counts:counts,courses:courses};
}
function sampleRender(fmt,scope){
 const o=sampleObj(scope);
 window.__chSample={A1:{t:'IP 地址与子网掩码'},A2:{t:'DNS 解析'},B1:{t:'用 ping 与 nslookup 排障'}};
 let txt='';
 try{txt=renderArchive(o,scope,fmt).text;}finally{window.__chSample=null;}
 return txt;
}
/* 最简写法：JSON / Markdown / CSV 三份内容等价（同一门课程） */
function sampleMinJson(){
 return JSON.stringify({app:AR_APP,kind:'portable-archive',ver:AR_VER,scope:'course',courses:[{
  title:'网络基础入门',subtitle:'从 IP 地址到 DNS，一周搞懂桌面运维必备的网络知识',
  description:'面向桌面运维新人的网络入门课：先理解地址与解析，再学会用命令定位「上不了网」的原因。',
  chapters:[
   {id:'A1',title:'IP 地址与子网掩码',summary:'判断两台电脑在不在同一个网段',content:'IP 地址相当于电脑的门牌号。\n\n子网掩码决定哪些门牌号属于同一个小区。'},
   {id:'A2',title:'DNS 解析',summary:'域名怎么变成 IP',content:'DNS 像一本电话簿，把域名翻译成 IP 地址。\n\n能 ping 通 IP 却打不开网页时，先怀疑 DNS。'}
  ]}]},null,2);
}
const SAMPLE_MIN_MD=[
'# 网络基础入门','',
'> 从 IP 地址到 DNS，一周搞懂桌面运维必备的网络知识','',
'面向桌面运维新人的网络入门课：先理解地址与解析，再学会用命令定位「上不了网」的原因。','',
'## 模块 A · 网络基础','',
'### A1 IP 地址与子网掩码','',
'> 判断两台电脑在不在同一个网段','',
'IP 地址相当于电脑的门牌号。','',
'子网掩码决定哪些门牌号属于同一个小区。','',
'### A2 DNS 解析','',
'> 域名怎么变成 IP','',
'DNS 像一本电话簿，把域名翻译成 IP 地址。','',
'能 ping 通 IP 却打不开网页时，先怀疑 DNS。',''].join('\n');
const SAMPLE_MIN_CSV=[
'type,course,id,title,content,json',
'course,c1,,网络基础入门,"面向桌面运维新人的网络入门课：先理解地址与解析，再学会用命令定位「上不了网」的原因。",',
'module,c1,A,模块 A · 网络基础,必修,',
'chapter,c1,A1,IP 地址与子网掩码,"IP 地址相当于电脑的门牌号。','','子网掩码决定哪些门牌号属于同一个小区。",',
'chapter,c1,A2,DNS 解析,"DNS 像一本电话簿，把域名翻译成 IP 地址。','','能 ping 通 IP 却打不开网页时，先怀疑 DNS。",',
'gloss,c1,DNS,Domain Name System,把域名翻译成 IP 的电话簿,',''].join('\n');
/* 旧入口保留：返回完整 JSON 示例 */
function archiveSample(scope){return JSON.stringify(sampleObj(scope),null,2);}
function sampleText(fmt,scope,variant){
 if(scope==='course'&&variant==='min')return fmt==='json'?sampleMinJson():(fmt==='md'?SAMPLE_MIN_MD:SAMPLE_MIN_CSV);
 return sampleRender(fmt,scope==='course'?'all':scope);
}

/* ---------- 格式说明文案 ---------- */
const FMT_META={
 json:{name:'JSON',ext:'.json',mime:'application/json',kind:'json',short:'结构最完整'},
 md:{name:'Markdown',ext:'.md',mime:'text/markdown',kind:'md',short:'可读文档'},
 csv:{name:'CSV',ext:'.csv',mime:'text/csv',kind:'csv',short:'表格软件'}
};
const GUIDE_SCOPES=[['course','课程'],['marks','划线与备注'],['progress','学习进度'],['cases','案例与术语'],['handbook','学习手册']];
const SCOPE_INFO={
 course:{label:'整门课程',apply:'导入后会新建为一门独立课程（带全部章节与该课程自己的学习数据）；与现有课程重名时自动加「副本」，不会覆盖已有课程。'},
 marks:{label:'划线与备注',apply:'导入后合并 / 覆盖到「当前课程」的划线。合并：按 id 去重，只补充没有的；覆盖：以档案为准替换。'},
 progress:{label:'学习进度',apply:'导入后合并 / 覆盖到「当前课程」的进度。合并：章节状态取更高等级（过关 > 半过关 > 未过关），学习日期取较早的记录；覆盖：以档案为准。'},
 cases:{label:'案例与术语',apply:'导入后合并 / 覆盖到「当前课程」的故障案例（按 no 去重）与自填术语（按 term 去重）。'},
 handbook:{label:'学习手册',apply:'导入后写入「当前课程」的学习手册文本，不改动章节与进度。'}
};
function req(t){return '<span class="req'+(t==='选填'?' opt':'')+'">'+t+'</span>';}
function guideSpec(fmt,scope,variant){
 const o={lead:'',rules:[],tblTitle:'字段说明',th:['字段','要求','说明'],rows:[],variants:[],notes:{},faq:[]};
 const course=scope==='course',simple=course&&variant==='min';
 const apply=SCOPE_INFO[scope].apply;
 o.variants=course?[{k:'min',label:'最简写法'},{k:'full',label:'完整写法'}]:[{k:'full',label:'完整示例'}];
 /* ---------- JSON ---------- */
 if(fmt==='json'){
  o.lead=course?'JSON 结构最完整，整门课程、多门课程一次导入都用它；让 AI 按下面的模板生成也最稳。':'局部档案同样是 JSON 对象，数据放在 <code>courses[0].state</code> 里（手册放在 <code>courses[0].handbook</code>）。';
  o.rules=['文件扩展名 <code>.json</code>，文本编码 <code>UTF-8</code>（内容为 JSON 的 <code>.js</code> 文件也能导入）。',
   '顶层是一个对象 <code>{ }</code>，必须含 <code>courses</code> 数组，每个元素是一门课程。顶层的 <code>app</code>、<code>kind</code>、<code>ver</code>、<code>scope</code> 照抄示例即可（缺了也能识别）。',
   course?'每门课程只有 <code>title</code> 是必填，其余都可省略；章节写在课程的 <code>chapters</code> 数组里。':'<code>scope</code> 必须写成 <code>'+scope+'</code>；数据写在 <code>courses[0].state</code>（手册写在 <code>courses[0].handbook</code>）。<code>title</code> 只用来标注，数据会进入「当前课程」。',
   '必须是严格 JSON：英文半角双引号、无注释、最后一项后面不能有逗号；字符串里的换行写成 <code>\\n</code>。'];
  if(course)o.rules.push('章节：建议每章写 <code>id</code>（课程内不重复，省略则按顺序编号 1、2、3…）、<code>title</code>、<code>summary</code>，正文写 <code>content</code>（空行分段，纯文本显示）。','<code>modules</code>、<code>routes</code> 可省略：省略时自动创建「模块 A」和「全章节顺序」。写了的话，章节的 <code>m</code> 要等于模块的 <code>k</code>，路线里只能放已有章节的 id。');
  o.rules.push(apply);
  if(course){
   o.tblTitle='字段说明 · 课程';
   o.rows=[['courses',req('必填'),'课程数组，可放多门，每门都会新建为独立课程'],['courses[].title',req('必填'),'课程名称（也可写 <code>name</code>）'],
    ['courses[].subtitle',req('选填'),'一句话简介，显示在课程列表里'],['courses[].description',req('选填'),'课程说明'],
    ['courses[].chapters',req('建议'),'章节数组；没有章节的课程只能看简介，不能开始学习'],
    ['chapters[].id',req('建议'),'章节编号（字符串），课程内唯一，如 <code>A1</code>'],['chapters[].title',req('建议'),'章节标题（<code>t</code>、<code>name</code> 同义）'],
    ['chapters[].summary',req('选填'),'列表里的一句话摘要（<code>s</code> 同义）'],['chapters[].content',req('选填'),'正文，空行分段'],
    ['chapters[].m',req('选填'),'所属模块编号，对应 <code>modules[].k</code>'],['chapters[].gate / quiz',req('选填'),'过关标准；互动题 <code>{"q":题目,"a":答案,"hint":提示}</code>'],
    ['chapters[].plain / terms',req('选填'),'双轨对照 <code>[["大白话","专业术语"]]</code>；本章术语清单（用 ｜ 分隔）'],
    ['modules[]',req('选填'),'<code>{"k":"A","n":"模块 A · 基础","tag":"必修"}</code>'],['routes / routeNames',req('选填'),'学习路线，如 <code>{"all":["A1","A2"]}</code>'],
    ['state',req('选填'),'这门课自己的学习数据（见「完整写法」示例）']];
  }
 }
 /* ---------- Markdown ---------- */
 if(fmt==='md'){
  if(simple){
   o.lead='手写课程最省事：只用 Markdown 标题层级，不用写 JSON。AI 生成课程时也推荐这种写法。';
   o.rules=['文件扩展名 <code>.md</code>，文本编码 <code>UTF-8</code>。',
    '第一行写 <code># 课程名称</code>（必填）。每个一级标题 = 一门课程，所以一个文件里写多门课也可以。',
    '标题后紧跟的 <code>&gt; 引用</code> = 一句话简介；到第一个 <code>##</code> 之前的普通文字 = 课程说明。',
    '<code>## 模块名</code> = 模块（可省略）；<code>### 编号 章节标题</code> = 章节，编号可省略（如 <code>### DNS 解析</code>），省略时自动编号。',
    '章节下第一个 <code>&gt; 引用</code> = 章节摘要；其余文字 = 正文，用空行分段。',
    '代码块 <code>```</code> 里的 <code>#</code> 不会被当成标题。正文按纯文本显示，不渲染 Markdown 样式。',
    '这种写法不含学习数据；要带上进度、划线、案例，请用「完整写法」。',apply];
   o.tblTitle='结构说明';o.th=['写法','含义','要求'];
   o.rows=[['<code># 标题</code>','课程名称',req('必填')],['<code>&gt; 引用</code>（紧跟标题）','一句话简介',req('选填')],['标题后的普通段落','课程说明（到第一个 <code>##</code> 为止）',req('选填')],
    ['<code>## 模块名</code>','模块',req('选填')],['<code>### A1 章节标题</code>','章节（编号可省略）',req('建议')],['章节内第一个 <code>&gt; 引用</code>','章节摘要',req('选填')],['其余文字','章节正文，空行分段',req('选填')]];
  }else{
   o.lead=course?'导出的 Markdown 就是这种：上半部分是方便阅读的内容，文末的 <code>```json</code> 代码块才是真正被导入的数据。':'局部档案的 Markdown：可读内容加文末 <code>```json</code> 数据存档，导入只读取这个代码块（手册另读 <code>&lt;!--handbook--&gt;</code> 区块）。';
   o.rules=['文件扩展名 <code>.md</code>，文本编码 <code>UTF-8</code>。',
    '文末必须有一个 <code>```json</code> 代码块，里面是完整的 JSON 档案（字段与「JSON」页一致）。导入时只读取<b>最后一个</b> <code>```json</code> 代码块。',
    '代码块开头写 <code>```json</code>，结尾的 <code>```</code> 单独占一行；三个反引号必须是英文半角。',
    '代码块以上的标题、列表、表格仅供阅读，改了不会影响导入结果；要改数据请改代码块里的 JSON。',
    '想手写一门新课程，用「最简写法」更省事；已有的完整 .md 想修改，建议直接改 JSON 代码块。',apply];
   o.tblTitle='文件结构';o.th=['部分','位置','要求'];
   o.rows=[['标题与导出时间','文件开头',req('选填')],['课程简介 / 章节目录 / 划线 / 案例 / 术语 / 进度','正文中部',req('选填')],['<code>&lt;!--handbook--&gt; … &lt;!--/handbook--&gt;</code>','学习手册区块',req('选填')],['<code>## 数据存档</code> + <code>```json</code> 代码块','文件末尾',req('必填')]];
  }
 }
 /* ---------- CSV ---------- */
 if(fmt==='csv'){
  o.lead=simple?'用 Excel / WPS 就能填：每行一条记录，只需要写 <code>type</code>、<code>id</code>、<code>title</code>、<code>content</code> 四列，<code>json</code> 列留空。':'导出的 CSV 就是这种：一行一条记录，<code>json</code> 列装着该条记录的完整数据，导入时以 <code>json</code> 列为准。';
  o.rules=['文件扩展名 <code>.csv</code>，文本编码 <code>UTF-8</code>（Excel / WPS 另存为时选「CSV UTF-8（逗号分隔）」）。',
   '第一行是表头，必须以 <code>type</code> 开头：<code>type,course,id,title,content,json</code>，列名不能改。',
   '一行一条记录；单元格里含逗号、换行或双引号时，用英文双引号把整格包起来，格内的双引号写成两个 <code>""</code>（Excel / WPS 会自动处理）。',
   '<code>course</code> 列填课程键：同一门课的所有行填一样的（如 <code>c1</code>）；也可以全部留空，视为同一门课。',
   simple?'简易写法：只填 <code>type / id / title / content</code>，<code>json</code> 留空。章节依次归入它上面最近的 <code>module</code> 行。':'完整写法：<code>json</code> 列放该行的完整 JSON，且必须是合法 JSON；不合法的行会被跳过（导入预览会提示行数）。',
   '单元格上限约 3 万字（Excel 限制）。超长正文请拆成多章，或改用 JSON。',
   course?apply:'局部档案必须保留第一行之后的 <code>meta</code> 行（它写明 <code>scope</code>）。'+apply];
  if(simple){
   o.tblTitle='type 取值（简易写法）';o.th=['type','含义','id / title / content 填什么'];
   o.rows=[['course','课程（必填一行）','title＝课程名称，content＝课程简介'],['module','模块（选填）','id＝模块编号（如 A），title＝模块名，content＝标签'],
    ['chapter','章节','id＝章节编号，title＝章节标题，content＝正文（空行分段）；可选 <code>summary</code> 列写摘要'],['gloss','术语（选填）','id＝术语，title＝全称，content＝大白话']];
  }else{
   o.tblTitle='type 取值（完整写法）';o.th=['type','含义','说明'];
   o.rows=[['meta','档案信息','第一行数据，<code>json</code> 里写 app / kind / ver / scope'],['course','课程信息','json＝{id,title,subtitle,description,cover}'],['module / chapter','模块 / 章节','json＝整个模块 / 章节对象'],
    ['deep / long','知识详解 / 长文','id＝章节编号，json＝该章内容'],['route','学习路线','id＝路线名，json＝{name,ids}'],['resource','其他资源','id＝字段名，json＝{v:…}'],['handbook','学习手册（分段）','id＝段号，json＝{part:…}'],
    ['state','学习位置','json＝{route,idx,day,mode,pin}'],['progress','章节进度','id＝章节编号，json＝{status,learn}'],['mark','划线','json＝一条划线'],['case','故障案例','json＝一条案例'],['gloss','自填术语','json＝一条术语'],['studylog','学习日历','json＝{日期:分钟}']];
  }
 }
 /* ---------- 局部档案字段表（JSON 与嵌入在 MD / CSV 里的内容一致） ---------- */
 if(!course){
  const T={
   marks:['state.marks[]','划线数组：每条一个对象',[['id',req('必填'),'划线编号，唯一，如 <code>m1</code>'],['ch',req('必填'),'所在章节编号'],['t',req('必填'),'划线原文'],['n',req('选填'),'备注'],['ty',req('选填'),'线型：<code>solid</code> 横线 / <code>wavy</code> 波浪 / <code>dash</code> 虚线'],['co',req('选填'),'颜色：<code>b</code> 蓝 / <code>g</code> 绿 / <code>a</code> 琥珀 / <code>r</code> 红'],['tags',req('选填'),'标签数组，如 <code>["重点"]</code>'],['at',req('选填'),'创建时间（毫秒时间戳）']]],
   progress:['state','学习进度',[['status',req('选填'),'<code>{"章节编号":"green"|"yellow"|"red"}</code>，过关 / 半过关 / 未过关'],['learn',req('选填'),'<code>{"章节编号":{"at":"2026-11-06","done":[]}}</code>，学习日期'],['route / idx / day / mode / pin',req('选填'),'当前路线、位置、累计天数、模式、置顶章节'],['studyLog',req('选填'),'学习日历 <code>{"2026-11-06":30}</code>（日期：分钟）']]],
   cases:['state','案例与术语',[['cases[].no',req('必填'),'案例编号，唯一，如 <code>NET-001</code>'],['cases[].sym / path / cause / lesson',req('选填'),'现象 / 排查路径 / 根因 / 下次第一步'],['cases[].ch',req('选填'),'关联章节编号'],['gloss[].term',req('必填'),'术语名，唯一'],['gloss[].full / say',req('选填'),'全称 / 一句话大白话']]],
   handbook:['courses[0].handbook','学习手册',[['handbook',req('必填'),'字符串，手册全文（Markdown 文本）；换行写成 <code>\\n</code>']]]
  }[scope];
  if(fmt==='json'){o.tblTitle='字段说明 · '+SCOPE_INFO[scope].label;o.rows=T[2];}
  else{
   o.tblTitle='数据字段 · '+SCOPE_INFO[scope].label;o.rows=T[2];
   o.notes.fields=fmt==='md'?'这些字段写在 <code>```json</code> 代码块的 <code>courses[0].state</code> 里。':'这些字段写在对应行的 <code>json</code> 列里（如 mark / progress / case / gloss 行）。';
  }
 }
 /* ---------- 示例说明 ---------- */
 o.notes.min=fmt==='json'?'只要课程名和章节就能导入；导入后新建一门课程。':fmt==='md'?'三种最简写法内容等价：同一门课，随便选一种。':'三种最简写法内容等价；表格软件里每格一个字段即可。';
 o.notes.full=course?'与「导出 → 整门课程」生成的文件同结构：含模块、路线、知识详解、划线、进度、案例等，可整包迁移到另一台设备。示例数据为演示用。':'与「导出 → '+SCOPE_INFO[scope].label+'」生成的文件同结构。示例数据为演示用。';
 /* ---------- 常见问题 ---------- */
 if(fmt==='json')o.faq=[['提示「JSON 格式有误」','多半是行尾多了 / 漏了逗号、用了中文引号“ ”、字符串里直接换行。把内容贴到任意 JSON 校验工具里能定位到出错的位置。'],['导入后章节是空的','检查 <code>chapters</code> 是不是写在课程对象里（<code>courses[0].chapters</code>），而不是顶层。'],['想一次导入多门课','在 <code>courses</code> 数组里放多个课程对象，每门都会新建为独立课程。']];
 if(fmt==='md')o.faq=[['提示「Markdown 无法识别」','手写课程请以 <code># 课程名称</code> 开头；完整档案请保留文末的 <code>```json</code> 数据存档。'],['章节没有拆开','章节标题要用三个 <code>###</code>，且 <code>#</code> 后面有一个空格。'],['正文里的 # 被当成标题','把含 <code>#</code> 的内容放进 <code>```</code> 代码块里（命令、脚本注释都适用）。']];
 if(fmt==='csv')o.faq=[['中文显示乱码','用 UTF-8 保存：Excel / WPS 另存为 → 「CSV UTF-8（逗号分隔）」。'],['提示表头不对','第一列必须叫 <code>type</code>，整行表头为 <code>type,course,id,title,content,json</code>。'],['内容里有逗号，列错位了','整格用英文双引号括起来；用 Excel / WPS 保存会自动处理。'],['有几行没导入','<code>json</code> 列填了但不是合法 JSON 的行会被跳过，导入预览会提示行数。']];
 return o;
}

/* ---------- 格式说明组件 ---------- */
function formatGuide(opt){
 opt=opt||{};
 const st={fmt:opt.fmt||'json',scope:opt.scope||'course',variant:opt.variant||'',open:Object.assign({req:true,fields:false,demo:true,faq:false},opt.open||{})};
 if(!st.variant)st.variant=st.scope==='course'?'min':'full';
 const root=h('div','fg');
 function det(key,title,fill){
  const d=h('details');if(st.open[key])d.open=true;
  const s=h('summary');s.appendChild(h('span',null,title));d.appendChild(s);
  const b=h('div','fg-body');fill(b);d.appendChild(b);
  d.addEventListener('toggle',function(){st.open[key]=d.open;});
  return d;
 }
 function paint(){
  root.textContent='';
  const sp=guideSpec(st.fmt,st.scope,st.variant),F=FMT_META[st.fmt];
  if(!opt.fixedFmt){
   const tabs=h('div','fg-tabs');
   ['json','md','csv'].forEach(function(k){
    const b=h('button',st.fmt===k?'on':'');b.appendChild(document.createTextNode(FMT_META[k].name));b.appendChild(h('small',null,FMT_META[k].ext+' · '+FMT_META[k].short));
    b.onclick=function(){st.fmt=k;paint();};tabs.appendChild(b);
   });
   root.appendChild(tabs);
  }
  if(opt.scopes!==false){
   const sc=h('div','fg-scopes');
   GUIDE_SCOPES.forEach(function(s){
    const b=h('button','chip2'+(st.scope===s[0]?' on':''),s[1]);
    b.onclick=function(){st.scope=s[0];st.variant=s[0]==='course'?'min':'full';paint();};sc.appendChild(b);
   });
   root.appendChild(sc);
  }
  const lead=h('p','fg-lead');lead.innerHTML='<b>'+F.name+'（'+F.ext+'）</b>　'+sp.lead;root.appendChild(lead);
  root.appendChild(det('req','文件要求',function(b){const ul=h('ul','fg-rules');sp.rules.forEach(function(r){const li=h('li');li.innerHTML=r;ul.appendChild(li);});b.appendChild(ul);}));
  root.appendChild(det('fields',sp.tblTitle,function(b){
   const w=h('div','fg-tbl'),tb=h('table'),th=h('thead'),tr=h('tr');if(sp.th[1]==='要求')tb.dataset.cols='48,12,40';
   sp.th.forEach(function(x){tr.appendChild(h('th',null,x));});th.appendChild(tr);tb.appendChild(th);
   const tbd=h('tbody');sp.rows.forEach(function(r){const t=h('tr');r.forEach(function(c){const td=h('td');td.innerHTML=c;t.appendChild(td);});tbd.appendChild(t);});
   tb.appendChild(tbd);w.appendChild(tb);b.appendChild(w);
   if(sp.notes.fields){const n=h('p','fg-note');n.innerHTML=sp.notes.fields;b.appendChild(n);}
  }));
  root.appendChild(det('demo','示例 · 可复制、可保存为模板',function(b){
   if(sp.variants.length>1){
    const vr=h('div','fg-var');
    sp.variants.forEach(function(v){const x=h('button',st.variant===v.k?'on':'',v.label);x.onclick=function(){st.variant=v.k;paint();};vr.appendChild(x);});
    b.appendChild(vr);
   }
   const text=sampleText(st.fmt,st.scope,st.variant);
   b.appendChild(h('pre','fg-code',text));
   const n=h('p','fg-note');n.innerHTML=sp.notes[st.variant==='min'?'min':'full'];b.appendChild(n);
   const act=h('div','fg-act');
   const cp=h('button','btn withic');cp.innerHTML=ic('copy')+'<span>复制示例</span>';cp.onclick=function(){copyText(text,'示例已复制，可粘贴进文件后修改');};
   const sv=h('button','btn withic');sv.innerHTML=ic('download')+'<span>预览并保存模板</span>';
   sv.onclick=function(){
    const nm='课程导入模板-'+SCOPE_INFO[st.scope].label+(st.scope==='course'?(st.variant==='min'?'-最简':'-完整'):'')+F.ext;
    openExport({kind:F.kind,title:'导入模板 · '+F.name,filename:nm,mime:F.mime,text:text,hint:'这是格式模板（示例数据）：改成自己的内容后，在「我的 → 数据 → 导入档案」选择该文件即可导入。文件为 UTF-8 编码的 '+F.ext+'。'});
   };
   act.appendChild(cp);act.appendChild(sv);b.appendChild(act);
  }));
  root.appendChild(det('faq','常见问题',function(b){
   const dl=h('dl','fg-faq');sp.faq.forEach(function(q){dl.appendChild(h('dt',null,q[0]));const dd=h('dd');dd.innerHTML=q[1];dl.appendChild(dd);});b.appendChild(dl);
  }));
 }
 paint();return root;
}

/* 折叠容器：默认收起，避免页面杂乱 */
function guideFold(opt){
 const d=h('details','fg-main');const s=h('summary');s.appendChild(h('span',null,'格式要求与示例'));s.appendChild(h('small',null,'JSON · Markdown · CSV'));d.appendChild(s);
 d.appendChild(formatGuide(Object.assign({open:{req:true,demo:false}},opt||{})));return d;
}
/* 折叠外壳：默认收起，避免一进页面就是一大片说明 */
function guideFold(opt,title){
 const d=h('details','fg-fold');const s=h('summary');s.appendChild(h('span',null,title||'格式要求与示例'));s.appendChild(h('small',null,'JSON · Markdown · CSV'));d.appendChild(s);
 let built=false;d.addEventListener('toggle',function(){if(d.open&&!built){built=true;d.appendChild(formatGuide(opt));}});return d;
}
/* ---------- 导出页：格式按钮 + 摘要 + 文件格式说明 ---------- */
function renderExportPage(box){
 const meta=IO_ENTRIES.find(function(e){return e.id===ioDetail;});if(!meta){ioDetail=null;renderIo();return;}
 const wrap=h('div','io-page stagger');box.appendChild(wrap);
 wrap.appendChild(listRow({icon:meta.icon,title:meta.title,desc:meta.desc,static:true}));
 wrap.appendChild(h('div','io-cur','当前课程 · '+coMeta(curCourse()).title));
 wrap.appendChild(h('div','io-lab',null)).appendChild(h('span',null,'选择文件格式'));
 const fm=h('div','io-format');
 ['json','md','csv'].forEach(function(k){
  const f=FMT_META[k],b=h('button',ioFormat===k?'on':'');b.appendChild(h('b',null,f.name));b.appendChild(h('small',null,f.ext+' · '+f.short));
  b.onclick=function(){ioFormat=k;fm.querySelectorAll('button').forEach(function(x){x.classList.remove('on');});b.classList.add('on');upd();};fm.appendChild(b);
 });
 wrap.appendChild(fm);
 wrap.appendChild(h('div','io-lab',''));
 const chips=h('div','io-chips');wrap.appendChild(chips);
 const acts=h('div','io-actions'),exp=h('button','btn pri');acts.appendChild(exp);wrap.appendChild(acts);
 const gw=h('div','exp-guide');wrap.appendChild(gw);
 const gscope=(ioDetail==='all'||ioDetail==='course')?'course':ioDetail;
 function upd(){
  chips.textContent='';
  archiveSummary(makeArchive(ioDetail)).forEach(function(r){chips.appendChild(h('span','pill',r[0]+' '+r[1]));});
  exp.textContent='预览并导出 '+FMT_META[ioFormat].name;
  gw.textContent='';
  gw.appendChild(guideFold({fmt:ioFormat,scope:gscope,variant:'full',fixedFmt:true,scopes:false}));
 }
 upd();
 exp.onclick=function(){
  const d=archiveText(ioDetail,ioFormat),F=FMT_META[ioFormat],name=coMeta(curCourse()).title+'-'+meta.name+'-'+today()+'.'+d.ext;
  openExport({kind:ioFormat,title:meta.title+' · '+F.name,filename:name,mime:d.mime,text:d.text,
   hint:'文件格式：'+F.ext+' · UTF-8'+(ioFormat==='csv'?'（带 BOM，Excel / WPS 直接打开不乱码）':'')+'。可在「我的 → 数据 → 导入档案」重新导入；'+((ioDetail==='all'||ioDetail==='course')?'整门课程会新建为一门独立课程。':'会合并 / 覆盖到当前课程。')});
 };
}

/* ---------- 课程页「导入新课程」弹层：格式要求 + 示例 ---------- */
function openCourseImport(){
 const body=h('div'),ta=document.createElement('textarea');ta.className='io-ta';ta.placeholder='粘贴课程档案（JSON / Markdown / CSV），或选择文件';
 const hint=h('p','io-hint','支持 .json · .md · .csv（UTF-8）。导入后新建一门独立课程，同名自动加「副本」。');
 hint.style.margin='0 2px 10px';body.appendChild(hint);body.appendChild(ta);
 const row=h('div','io-actions');row.style.margin='10px 0 0';
 const pick=h('button','btn','选择文件');pick.onclick=function(){const f=document.createElement('input');f.type='file';f.accept='.json,.md,.csv,.js,.txt,application/json,text/markdown,text/csv,text/javascript,application/javascript,text/plain';f.onchange=function(){const file=f.files&&f.files[0];if(!file)return;const rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');};rd.readAsText(file);};f.click();};
 row.appendChild(pick);body.appendChild(row);
 body.appendChild(guideFold({scope:'course',scopes:false}));
 openSheet('导入课程',body,function(){
  const res=parseArchiveText(ta.value);
  if(res.err){toast(res.err.length>26?res.err.slice(0,26)+'…':res.err);return false;}
  if(!isFullScope(res.obj)){toast('这是局部档案（划线 / 进度 / 案例），请到「我的 → 数据 → 导入档案」导入');return false;}
  const r=applyArchive(res.obj,'merge');
  if(!r.created.length){toast('没有可导入的课程（需要 title）');return false;}
  render();popCheck('已导入 '+r.created.length+' 门课程');
 },'导入');
}

opsPack();syncCourses();
if(!COURSES.some(function(c){return c.id===S.cur;}))S.cur='ops';
loadPack(S.cur);
applyTheme();
$('backBtn').innerHTML=ic('back');
$('backBtn').onclick=navBack;
$('abEdit').innerHTML=ic('pen');
$('abEdit').onclick=function(){openCourseEditor(curCourse());};
bindMini();
(function(){
 const inp=$('mineSearch'),clr=$('mineClear'),orb=$('mineOrbit');
 inp.oninput=function(){mineKw=this.value;renderMine();};
 inp.onkeydown=function(e){if(e.key==='Enter')this.blur();};
 if(clr){clr.innerHTML=ic('close');clr.onclick=function(){inp.value='';mineKw='';renderMine();inp.focus();};}
 if(orb){orb.innerHTML=ic('orbit');orb.setAttribute('aria-label','课程管理');orb.onclick=function(){nav('course-manage');};}
 window.addEventListener('scroll',syncScrollState,{passive:true});
 /* 按压水波：委托在 document 上，所有可点元素共用 */
 document.addEventListener('pointerdown',function(e){
  const t=e.target.closest&&e.target.closest('.btn,.chrow,.ep,.tile,.my-item,.chip2,.scard,.tbi,.icbtn,.pf-row,.pbt,.rbtn,.qbtn,.sbtn,.io-format button,.io-seg button,.smpbtn,.theme-swatch');
  if(!t||t.disabled||(e.pointerType==='mouse'&&e.button!==0))return;
  const r=t.getBoundingClientRect(),d=Math.max(r.width,r.height)*2,s=document.createElement('span');
  s.className='ripple';s.style.cssText='width:'+d+'px;height:'+d+'px;left:'+(e.clientX-r.left-d/2)+'px;top:'+(e.clientY-r.top-d/2)+'px';
  t.appendChild(s);setTimeout(function(){s.remove();},650);
 },{passive:true});
})();
migrateMarks();
ensureShell();applySat();detectImmersive();buildPlanetLines();syncSatSolid();
window.addEventListener('scroll',syncSatSolid,{passive:true});
window.addEventListener('resize',function(){clearTimeout(window.__rz);window.__rz=setTimeout(function(){detectImmersive();buildPlanetLines();},150);});
new MutationObserver(function(ms){if(ms.some(fitRelevant))scheduleFit();}).observe(document.body,{childList:true,subtree:true});
{const __r=render;render=function(){const r=__r.apply(this,arguments);applySat();return r;};}
(function(){const __r=render;render=function(){const y0=window.scrollY||0;try{if(view==='today'){const pc=curChapter();if(pc&&pc.media)view='player';}}catch(e){}__r.apply(this,arguments);applySat();tidyImport();afterRender(y0);ttsGuard();tidyMediaCourse();tidyCourseManage();};})();
render();fitAll();

