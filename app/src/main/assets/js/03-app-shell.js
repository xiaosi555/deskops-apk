/* Extracted original app layer; classic-script scope and order are intentional. */
/* SECTION: app router · 图标 / 课程 / 导航 */
const ICONS={
 back:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"/></svg>',
 moon:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5z"/></svg>',
 sun:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5V5M12 19v2.5M2.5 12H5M19 12h2.5M5.2 5.2l1.8 1.8M17 17l1.8 1.8M18.8 5.2L17 7M7 17l-1.8 1.8"/></svg>',
 search:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2"/></svg>',
 compass:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2.1 4.9-4.9 2.1 2.1-4.9z"/></svg>',
 layers:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.5l8.5 4.7L12 12.9 3.5 8.2z"/><path d="M3.5 12.6l8.5 4.7 8.5-4.7"/><path d="M3.5 16.6l8.5 4.7 8.5-4.7"/></svg>',
 grid:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="7" height="7" rx="1.8"/><rect x="13" y="4" width="7" height="7" rx="1.8"/><rect x="4" y="13" width="7" height="7" rx="1.8"/><rect x="13" y="13" width="7" height="7" rx="1.8"/></svg>',
 user:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8.2" r="3.8"/><path d="M4.8 20c1.4-3.4 4-5 7.2-5s5.8 1.6 7.2 5"/></svg>',
 play:'<svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M8.5 5.8v12.4c0 .8.9 1.3 1.6.9l9.6-6.2c.6-.4.6-1.4 0-1.8L10.1 4.9c-.7-.4-1.6.1-1.6.9z"/></svg>',
 refresh:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4.5 12a7.5 7.5 0 0 1 12.9-5.2L20 9.3"/><path d="M20 4.5v4.8h-4.8"/><path d="M19.5 12a7.5 7.5 0 0 1-12.9 5.2L4 14.7"/><path d="M4 19.5v-4.8h4.8"/></svg>',
 pen:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5l1-4L15.5 5a2.1 2.1 0 0 1 3 3L8 18.5z"/><path d="M13.5 7l3 3"/><path d="M4 19.5h7"/></svg>',
 book:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z"/><path d="M4 20.5V5.5"/><path d="M9 8h7M9 11.5h5"/></svg>',
 aA:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 17.5L8.5 6l5 11.5"/><path d="M5.5 13.5h6"/><path d="M16.5 17.5v-5.3"/><path d="M16.5 12.2c1.9 0 3.2 1 3.2 2.7v2.6"/></svg>',
 folder:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 7A2.5 2.5 0 0 1 6 4.5h3.4l2 2.4H18A2.5 2.5 0 0 1 20.5 9.3v7.2A2.5 2.5 0 0 1 18 19H6a2.5 2.5 0 0 1-2.5-2.5z"/><path d="M9.5 13.2h5"/></svg>',
 wrench:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a4.2 4.2 0 0 0-5.6 5.2L4 16.6V20h3.4l5.1-5.1a4.2 4.2 0 0 0 5.2-5.6L14.9 12l-2.9-2.9z"/></svg>',
 chart:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h16"/><path d="M7 20v-6"/><path d="M12 20V9"/><path d="M17 20V4.5"/></svg>',
 share:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 14.5V4"/><path d="M8 7.5L12 3.5l4 4"/><path d="M5 12.5v5.5A2 2 0 0 0 7 20h10a2 2 0 0 0 2-2v-5.5"/></svg>',
 chev:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9.5 5.5l6.5 6.5-6.5 6.5"/></svg>',
 star:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.8l2.5 5 5.6.9-4.1 3.9 1 5.6-5-2.7-5 2.7 1-5.6L3.9 9.7l5.6-.9z"/></svg>',
 starFill:'<svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M12 3.8l2.5 5 5.6.9-4.1 3.9 1 5.6-5-2.7-5 2.7 1-5.6L3.9 9.7l5.6-.9z"/></svg>',
 monitor:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="4.5" width="17" height="12" rx="2"/><path d="M9.5 20h5M12 16.5V20"/></svg>',
 trash:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4.5 6.5h15"/><path d="M9 6.5V5a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 5v1.5"/><path d="M6.5 6.5l.8 12a2 2 0 0 0 2 1.9h5.4a2 2 0 0 0 2-1.9l.8-12"/><path d="M10 10.5v5.5M14 10.5v5.5"/></svg>'
};
function ic(name){return ICONS[name]||'';}
/* 额外图标 */
ICONS.calendar='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="5" width="17" height="16" rx="2"/><path d="M7.5 3.5v3M16.5 3.5v3M3.5 9.5h17"/></svg>';
ICONS.settings='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.7 2.9-.2-.1a1.7 1.7 0 0 0-1.8.1l-.2.1h-3.4l-.1-.2a1.7 1.7 0 0 0-1.5-1l-.2-.1-1.7-2.9.1-.2a1.7 1.7 0 0 0-.3-1.8l-.1-.2V9.8l.2-.1a1.7 1.7 0 0 0 .2-1.8l-.1-.2 1.7-2.9.2.1a1.7 1.7 0 0 0 1.8-.1l.2-.1h3.4l.1.2a1.7 1.7 0 0 0 1.5 1l.2.1 1.7 2.9-.1.2a1.7 1.7 0 0 0 .3 1.8l.1.2v3.4z"/></svg>';
ICONS.close='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6l12 12M18 6L6 18"/></svg>';
ICONS.image='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><circle cx="9" cy="10" r="1.6"/><path d="M4 17l5-4.5 3.5 3 2.5-2L20 17"/></svg>';
ICONS.info='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.2M12 7.8v.2"/></svg>';

/* 课程库：后期在这里追加新课程即可，「课程」页自动多出一张卡 */
const COURSES=[
 {id:'ops',title:'桌面运维全能学习导师',sub:'零基础到独当一面：Windows、AD 域与策略、网络、终端外设、排障方法论一条龙。',cover:'cv-a',glyph:'monitor',
  desc:'本课程把桌面运维拆成 7 个模块、30 章：先打地基（Windows / 网络 / 硬件），再攻 AD 域与策略主线，穿插 Helpdesk 高频场景与分层排障方法论。每章都配双轨对照、命令与预期输出、避坑清单和互动题，学完直接沉淀进案例库。'}
];
let DEPTEXT='A2 网络基础 ──┬──> C4 DNS/DHCP ──> C2 认证 ──> C5 GPO ──> C6 GPO 场景\n              │                              ↑\nA1 Windows ───┘──> C1 域概念 ────────────────┘\n   C1 ──> C3 ADUC ──> D3 文件共享权限\n   A3 硬件 ──> B1/B2/B3（可并行插队）\n   F2 排障方法论 需要 C4 + A2 ／ F6 PowerShell 需要 C3';
/* 工具箱：属于每一门课程，只在课程页内出现 */
const TOOLGROUPS=[
 {g:'学习节奏',items:[['today','今日学习','play'],['review','复习队列','refresh'],['anno','我的划线','highlighter']]},
 {g:'知识资料库',items:[['know','知识详解','book'],['gloss','术语手册','aA'],['case','案例库','folder']]},
 {g:'方法与进度',items:[['play','排障手册','wrench'],['prog','学习进度','chart'],['io','数据','share']]}
];
const PAGES=['mine','profile','course','today','know','anno','io','review','case','gloss','play','prog','calendar','my-marks','course-manage','settings','shortcuts','player'];
const TABPAGES={mine:'课程',profile:'我的'};
const TABICON={mine:'layers',profile:'user'};
const TITLES={know:'知识详解',anno:'我的划线',io:'数据',review:'复习队列',case:'案例库',gloss:'术语手册',play:'排障手册',prog:'学习进度',calendar:'学习日历', 'my-marks':'我的划线', 'course-manage':'课程管理',settings:'设置',shortcuts:'快捷入口',player:''};
let view='mine',navStack=[],mineKw='',cdSeg='chs',selectedMarkCourse='';
let miniGone=false,miniShown=false,miniMoved=false;

function curCourse(){return COURSES.find(function(c){return c.id===activeCourse;})||COURSES[0];}
/* 课程的自定义信息（名称 / 简介 / 封面）覆盖在默认值之上 */
function coMeta(co){
 const m=(S.ui.co&&S.ui.co[co.id])||{};
 return {title:(m.title&&m.title.trim())||co.title,sub:(m.sub&&m.sub.trim())||co.sub,cover:m.cover||''};
}
function coverEl(co,cls){
 const m=coMeta(co);
 const el=h('div','cover '+(cls||'')+' '+(m.cover?'img':co.cover));
 if(m.cover)el.style.backgroundImage='url("'+m.cover+'")';else el.innerHTML=ic(co.glyph);
 return el;
}
function navCore(v){
 hideBar();pendRec=null;
 try{const s0=window.getSelection();if(s0)s0.removeAllRanges();}catch(e){}
 if(v===view){render();return;}
 if(TABPAGES[v])navStack=[];else navStack.push(view);
 view=v;render();
}
function navBack(){const p=navStack.pop();view=p||'mine';render();}
function pageTitle(){
 if(view==='today'){const c=(S.pin&&chById(S.pin))||cur();return c?c.id+' '+c.t:'';}
 if(view==='course')return coMeta(curCourse()).title;
 return TITLES[view]||'';
}
function courseProgress(){
 const q=seq();
 const done=q.filter(function(id){return S.status[id]==='green';}).length;
 return {done:done,total:q.length};
}
function applyTheme(){
 document.documentElement.setAttribute('data-theme',S.theme==='dark'?'dark':'light');
 const colors=['#628b72','#6689a5','#8b7ca8','#b47e68','#718b91','#87916a','#a58b70','#78817c','#b98591'];applyAccent(colors[(S.ui&&S.ui.accent)||0]||colors[0]);applyStyleMode();
 document.querySelectorAll('[data-theme-toggle]').forEach(function(b){b.innerHTML=ic(S.theme==='dark'?'sun':'moon');});
 save();
}
function renderTabbar(){
 const box=document.getElementById('tbin');if(!box)return;
 box.textContent='';
 Object.keys(TABPAGES).forEach(function(k){
  const b=document.createElement('button');
  b.className='tbi'+((view===k||(k==='profile'&&['calendar','my-marks','course-manage','settings'].includes(view)))?' on':'');
  b.innerHTML=ic(TABICON[k])+'<span>'+TABPAGES[k]+'</span>';
  b.onclick=function(){nav(k);};
  box.appendChild(b);
 });
}
function renderAppbar(){
 const ab=document.getElementById('appbar');if(!ab)return;
 const onTab=!!TABPAGES[view];
 ab.classList.toggle('on',!onTab);
 document.getElementById('abTitle').textContent=pageTitle();
 document.getElementById('backBtn').classList.toggle('hide',onTab);
 document.getElementById('abEdit').classList.toggle('hide',view!=='course');
}

/* ===== 迷你「继续学习」条：左右滑动可关闭，重新进入 APP 再次出现 ===== */
function renderMini(){
 const m=document.getElementById('mini');if(!m)return;
 const inner=document.getElementById('miniInner');
 if(view==='today'||view==='course'||miniGone){m.classList.remove('on');miniShown=false;return;}
 const c=(S.pin&&chById(S.pin))||cur();
 if(!c){m.classList.remove('on');miniShown=false;return;}
 const q=seq();const i=Math.min(S.idx,q.length-1);
 inner.innerHTML='';
 inner.appendChild(coverEl(curCourse(),'sm'));
 const mi=h('div','mi');mi.appendChild(h('b',null,c.id+' '+c.t));mi.appendChild(h('span',null,'第 '+(i+1)+' / '+q.length+' 站 · '+ROUTENAME[S.route]));
 inner.appendChild(mi);
 const pb=h('span','pbtn');pb.innerHTML=ic('play');inner.appendChild(pb);
 m.classList.add('on');
 if(!miniShown){
  miniShown=true;
  inner.style.transform='';inner.style.opacity='';inner.style.transition='';
  inner.classList.remove('enter');void inner.offsetWidth;inner.classList.add('enter');
 }
}
function bindMini(){
 const m=document.getElementById('mini'),el=document.getElementById('miniInner');
 let sx=0,sy=0,dx=0,t0=0,down=false,locked=false;
 function springBack(){
  el.style.transition='transform 320ms cubic-bezier(.32,1.34,.5,1),opacity 200ms ease';
  el.style.transform='translateX(0)';el.style.opacity='1';
 }
 function dismiss(dir){
  el.style.transition='transform 260ms cubic-bezier(.4,0,.8,.6),opacity 240ms ease';
  el.style.transform='translateX('+(dir*(window.innerWidth||400))+'px)';
  el.style.opacity='0';
  setTimeout(function(){
   miniGone=true;miniShown=false;
   m.classList.remove('on');
   el.style.transition='none';el.style.transform='';el.style.opacity='';
  },270);
 }
 el.addEventListener('pointerdown',function(e){
  if(e.pointerType==='mouse'&&e.button!==0)return;
  down=true;locked=false;dx=0;sx=e.clientX;sy=e.clientY;t0=Date.now();miniMoved=false;
  el.classList.remove('enter');
  el.style.transition='none';
 });
 el.addEventListener('pointermove',function(e){
  if(!down)return;
  const mx=e.clientX-sx,my=e.clientY-sy;
  if(!locked){
   if(Math.abs(mx)>8&&Math.abs(mx)>Math.abs(my)*1.2){locked=true;try{el.setPointerCapture(e.pointerId);}catch(_){}}
   else if(Math.abs(my)>12){down=false;return;}
   else return;
  }
  dx=mx;miniMoved=true;
  el.style.transform='translateX('+dx+'px)';
  el.style.opacity=String(Math.max(.12,1-Math.abs(dx)/(el.offsetWidth*.9)));
 });
 function end(){
  if(!down)return;down=false;
  if(!locked)return;
  const v=Math.abs(dx)/Math.max(1,Date.now()-t0);
  if(Math.abs(dx)>el.offsetWidth*.3||(v>.55&&Math.abs(dx)>28))dismiss(dx>0?1:-1);
  else springBack();
  setTimeout(function(){miniMoved=false;},60);
 }
 el.addEventListener('pointerup',end);
 el.addEventListener('pointercancel',function(){if(down&&locked)springBack();down=false;});
 el.addEventListener('click',function(){if(miniMoved){miniMoved=false;return;}nav('today');});
 /* 重新进入 APP（回到前台）时再次显示 */
 document.addEventListener('visibilitychange',function(){
  if(!document.hidden&&miniGone){miniGone=false;miniShown=false;renderMini();}
 });
 window.addEventListener('pageshow',function(e){if(e.persisted&&miniGone){miniGone=false;miniShown=false;renderMini();}});
}

/* ===== 通用：图片压缩（裁成正方形）、底部弹层 ===== */
function pickImage(size,cb){
 const inp=document.createElement('input');inp.type='file';inp.accept='image/*';
 inp.onchange=function(){
  const f=inp.files&&inp.files[0];if(!f)return;
  const fr=new FileReader();
  fr.onerror=function(){toast('图片读取失败');};
  fr.onload=function(){
   const im=new Image();
   im.onerror=function(){toast('这张图片无法读取');};
   im.onload=function(){
    const s=Math.min(im.width,im.height),sx=(im.width-s)/2,sy=(im.height-s)/2,o=Math.min(size,s);
    const cv=document.createElement('canvas');cv.width=o;cv.height=o;
    const cx=cv.getContext('2d');cx.fillStyle='#fff';cx.fillRect(0,0,o,o);
    cx.drawImage(im,sx,sy,s,s,0,0,o,o);
    cb(cv.toDataURL('image/jpeg',.84));
   };
   im.src=fr.result;
  };
  fr.readAsDataURL(f);
 };
 inp.click();
}
function closeSheet(){
 const m=document.querySelector('.sheetmask');if(!m)return;
 m.classList.remove('on');setTimeout(function(){m.remove();},260);
}
function openSheet(title,body,onSave,saveText){
 closeSheet();
 const mask=h('div','sheetmask'),sh=h('div','sheet');
 const hd=h('div','shhd');hd.appendChild(h('b',null,title));
 const x=h('button','icbtn');x.innerHTML=ic('close');x.setAttribute('aria-label','关闭');x.onclick=closeSheet;hd.appendChild(x);
 sh.appendChild(hd);sh.appendChild(body);
 if(onSave){
  const ft=h('div','shft');
  const c=h('button','btn','取消');c.onclick=closeSheet;
  const s=h('button','btn pri',saveText||'保存');s.onclick=function(){if(onSave()!==false)closeSheet();};
  ft.appendChild(c);ft.appendChild(s);sh.appendChild(ft);
 }
 mask.appendChild(sh);document.body.appendChild(mask);
 mask.onclick=function(e){if(e.target===mask)closeSheet();};
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');});});
}
function field(label,el){const f=h('div','field');f.appendChild(h('label',null,label));f.appendChild(el);return f;}

/* 课程：自定义封面 / 名称 / 简介 */
function openCourseEditor(co){
 const m=coMeta(co);
 let cover=m.cover;
 const body=h('div');
 const row=h('div','covedit');
 const prev=h('div');
 function paint(){
  const nw=h('div','cover '+(cover?'img':co.cover));
  if(cover)nw.style.backgroundImage='url("'+cover+'")';else nw.innerHTML=ic(co.glyph);
  prev.replaceChildren(nw);
 }
 paint();row.appendChild(prev);
 const cb=h('div','cbt');
 const up=h('button','btn');up.innerHTML=ic('image')+' 上传封面';
 up.querySelector('svg').style.cssText='width:16px;height:16px;vertical-align:-3px;margin-right:2px';
 up.onclick=function(){pickImage(480,function(d){cover=d;paint();});};
 const rm=h('button','btn ghost','恢复默认封面');rm.onclick=function(){cover='';paint();};
 cb.appendChild(up);cb.appendChild(rm);row.appendChild(cb);body.appendChild(row);
 const ti=document.createElement('input');ti.type='text';ti.value=m.title;ti.maxLength=40;ti.placeholder='课程名称';
 const su=document.createElement('textarea');su.value=m.sub;su.maxLength=200;su.placeholder='一句话介绍这门课程';
 body.appendChild(field('课程名称',ti));body.appendChild(field('课程简介',su));
 openSheet('编辑课程',body,function(){
  const t=ti.value.trim();
  if(!t){toast('课程名称不能为空');return false;}
  S.ui.co=S.ui.co||{};
  S.ui.co[co.id]={title:t===co.title?'':t,sub:su.value.trim()===co.sub?'':su.value.trim(),cover:cover};
  save();render();toast('课程信息已保存');
 });
}
/* 个人信息：头像 / 昵称 / 签名 */
function profileInfo(){const p=S.ui.profile||{};return {name:(p.name&&p.name.trim())||'桌面运维学习者',bio:p.bio||'',avatar:p.avatar||''};}
function avatarEl(cls){
 const p=profileInfo();
 const a=h('div','avatar2'+(p.avatar?' img':'')+(cls?' '+cls:''));
 if(p.avatar)a.style.backgroundImage='url("'+p.avatar+'")';else a.textContent=Array.from(p.name)[0]||'我';
 return a;
}
function openProfileEditor(){
 const p=profileInfo();let avatar=p.avatar;
 const body=h('div');
 const row=h('div','covedit');
 const prev=h('div');
 const ti=document.createElement('input');ti.type='text';ti.value=p.name;ti.maxLength=16;ti.placeholder='你的名字';
 function paint(){
  const a=h('div','avatar2'+(avatar?' img':''));
  if(avatar)a.style.backgroundImage='url("'+avatar+'")';else a.textContent=Array.from(ti.value.trim()||p.name)[0]||'我';
  prev.replaceChildren(a);
 }
 paint();row.appendChild(prev);
 const cb=h('div','cbt');
 const up=h('button','btn');up.innerHTML=ic('image')+' 更换头像';
 up.querySelector('svg').style.cssText='width:16px;height:16px;vertical-align:-3px;margin-right:2px';
 up.onclick=function(){pickImage(256,function(d){avatar=d;paint();});};
 const rm=h('button','btn ghost','移除头像');rm.onclick=function(){avatar='';paint();};
 cb.appendChild(up);cb.appendChild(rm);row.appendChild(cb);body.appendChild(row);
 ti.oninput=function(){if(!avatar)paint();};
 const bi=document.createElement('input');bi.type='text';bi.value=p.bio;bi.maxLength=40;bi.placeholder='个性签名 / 学习目标（选填）';
 body.appendChild(field('昵称',ti));body.appendChild(field('个性签名',bi));
 openSheet('编辑个人信息',body,function(){
  if(!ti.value.trim()){toast('昵称不能为空');return false;}
  S.ui.profile={name:ti.value.trim(),bio:bi.value.trim(),avatar:avatar};
  save();render();toast('个人信息已保存');
 });
}

function toolTile(v,label,ico,badge){
 const t=h('div','tile');
 const c=h('span','tic');c.innerHTML=ic(ico);t.appendChild(c);
 t.appendChild(h('span',null,label));
 if(badge)t.appendChild(h('i','bdg',String(badge)));
 t.onclick=function(){nav(v);};
 return t;
}
function chRow(c){
 const curId=(S.pin&&chById(S.pin))?S.pin:cur().id;
 const el=h('div','chrow'+(c.id===curId?' cur':''));
 el.appendChild(h('span','cid',c.id));
 const ci=h('div','ci');
 ci.appendChild(h('b',null,c.t));
 ci.appendChild(h('p',null,c.s));
 el.appendChild(ci);
 const st=S.status[c.id];
 el.appendChild(h('span','st mk '+(st==='green'?'g':st==='yellow'?'y':'r'),st==='green'?'🟢':st==='yellow'?'🟡':''));
 const g=h('span','chev');g.innerHTML=ic('chev');el.appendChild(g);
 el.onclick=function(){openChapter(c.id);};
 return el;
}
function note2(t,d){const n=h('div','note');n.appendChild(h('b',null,t));n.appendChild(document.createTextNode(d));return n;}

/* ===== 课程（底部导航第一项）：课程卡 + 搜索 ===== */
function courseCard(co){
 const m=coMeta(co),pr=courseProgress();
 const started=Object.keys(S.learn||{}).length>0;
 const starred=S.ui.star&&S.ui.star[co.id];
 const el=h('div','ccard');
 el.appendChild(coverEl(co,''));
 const ci=h('div','ci');
 ci.appendChild(h('b',null,m.title));
 ci.appendChild(h('p','cs',m.sub));
 if(started){
  const bar=h('div','bar');const fi=h('i');fi.style.width=(pr.total?Math.round(pr.done/pr.total*100):0)+'%';bar.appendChild(fi);ci.appendChild(bar);
 }
 ci.appendChild(h('div','cmeta',(started?'已过关 '+pr.done+' / '+pr.total+' 章 · ':'')+MODS.length+' 模块 · '+CH.length+' 章'));
 el.appendChild(ci);
 const st=h('button','star'+(starred?' on':''));
 st.innerHTML=ic(starred?'starFill':'star');st.setAttribute('aria-label',starred?'取消星标':'星标课程');
 st.onclick=function(e){
  e.stopPropagation();
  S.ui.star=S.ui.star||{};
  if(S.ui.star[co.id])delete S.ui.star[co.id];else S.ui.star[co.id]=1;
  save();renderMine();
 };
 el.appendChild(st);
 el.onclick=function(){activeCourse=co.id;if(co.custom){openCourseEditor(co);}else{cdSeg='chs';nav('course');}};
 return el;
}
function searchAll(kw){
 const out={courses:[],chapters:[],know:[]};
 COURSES.forEach(function(co){
  const m=coMeta(co);
  if((m.title+' '+m.sub+' '+co.title+' '+co.desc).toLowerCase().indexOf(kw)>=0)out.courses.push(co);
 });
 CH.forEach(function(c){
  const hay=(c.id+' '+c.t+' '+c.s+' '+(c.terms||'')).toLowerCase();
  if(hay.indexOf(kw)>=0)out.chapters.push(c);
 });
 CH.forEach(function(c){
  const d=DEEP[c.id];if(!d||!d.deep)return;
  d.deep.forEach(function(row){
   if(out.know.length>=30)return;
   if((row[0]+' '+row[1]).toLowerCase().indexOf(kw)>=0)out.know.push([c,row]);
  });
 });
 return out;
}
function renderMine(){
 const box=document.getElementById('mineBox');if(!box)return;
 box.textContent='';
 const kw=mineKw.trim().toLowerCase();
 if(!kw){
  const list=COURSES.slice().sort(function(a,b){return (S.ui.star&&S.ui.star[b.id]?1:0)-(S.ui.star&&S.ui.star[a.id]?1:0);});
  list.forEach(function(co){box.appendChild(courseCard(co));});
  return;
 }
 const r=searchAll(kw);
 const n=r.courses.length+r.chapters.length+r.know.length;
 if(!n){box.appendChild(h('div','srempty','没有找到「'+mineKw.trim()+'」，试试 DNS、GPO、打印机、4625'));return;}
 if(r.courses.length){
  box.appendChild(h('div','srcnt','课程 · '+r.courses.length));
  r.courses.forEach(function(co){box.appendChild(courseCard(co));});
 }
 if(r.chapters.length){
  box.appendChild(h('div','srcnt','章节 · '+r.chapters.length));
  r.chapters.slice(0,30).forEach(function(c){box.appendChild(chRow(c));});
 }
 if(r.know.length){
  box.appendChild(h('div','srcnt','知识点 · '+r.know.length+(r.know.length>=30?'+':'')));
  r.know.forEach(function(it){
   const c=it[0],row=it[1];
   const el=h('div','chrow');
   el.appendChild(h('span','cid',c.id));
   const ci=h('div','ci');ci.appendChild(h('b',null,row[0]));ci.appendChild(h('p',null,row[1]));el.appendChild(ci);
   const g=h('span','chev');g.innerHTML=ic('chev');el.appendChild(g);
   el.onclick=function(){openChapter(c.id);};
   box.appendChild(el);
  });
 }
}

/* ===== 我的：个人数据总结 ===== */
function lrow(parent,ico,label,fn,tail,cls){
 const r=h('div','lrow'+(cls?' '+cls:''));
 const c=h('span','lic');c.innerHTML=ic(ico);r.appendChild(c);
 r.appendChild(h('span',null,label));
 if(tail)r.appendChild(h('span','tail',tail));
 const g=h('span','chev');g.innerHTML=ic('chev');r.appendChild(g);
 r.onclick=fn;parent.appendChild(r);return r;
}
function ymd(d){return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');}
function myItem(parent,ico,title,sub,tail,fn){
 const b=h('button','my-item');const icn=h('span','mi-ico');icn.innerHTML=ic(ico);b.appendChild(icn);
 const c=h('span','mi-copy');c.appendChild(h('b',null,title));if(sub)c.appendChild(h('small',null,sub));b.appendChild(c);
 if(tail)b.appendChild(h('span','mi-tail',tail));b.appendChild(h('span','mi-arrow','›'));b.onclick=fn;parent.appendChild(b);return b;
}
function groupTitle(box,t){const g=h('div','my-group');g.appendChild(h('div','my-group-title',t));const l=h('div','my-list');g.appendChild(l);box.appendChild(g);return l;}
function renderProfileMin(){
 const box=document.getElementById('profileBox');if(!box)return;box.textContent='';
 const pr=courseProgress(),p=profileInfo(),pct=pr.total?Math.round(pr.done/pr.total*100):0;
 const card=h('div','pcard2');const av=avatarEl();av.onclick=openProfileEditor;av.style.cursor='pointer';card.appendChild(av);
 const pi=h('div','pi');pi.appendChild(h('b',null,p.name));pi.appendChild(h('p',null,p.bio||'把每一次学习，留成自己的进度'));card.appendChild(pi);
 const ed=h('button','pedit','编辑  ›');ed.onclick=openProfileEditor;card.appendChild(ed);box.appendChild(card);
 const c1=groupTitle(box,'学习空间');
 myItem(c1,'book','课程','管理课程、封面与课程资料',String(COURSES.length),()=>nav('course-manage'));
 myItem(c1,'highlighter','划线','按课程查看与整理已保存的重点',String((S.marks||[]).length),()=>nav('my-marks'));
 myItem(c1,'calendar','学习日历','查看每天的学习记录与课程进度','',()=>nav('calendar'));
 const c2=groupTitle(box,'偏好设置');
 myItem(c2,'settings','设置','主题色、外观与夜间模式','',()=>nav('settings'));
 const c3=groupTitle(box,'数据与隐私');
 myItem(c3,'info','数据说明','学习数据仅保存在当前浏览器','',()=>{const b=h('div','shtext');b.appendChild(h('p',null,'本应用是纯本地单文件应用：进度、划线、案例库、术语手册与个人信息保存在当前浏览器的 localStorage 中，不联网、不上传。'));b.appendChild(h('p',null,'更换设备或清理浏览器数据前，请先在课程页「工具箱 → 数据」中备份。'));openSheet('数据说明',b);});
 const c4=groupTitle(box,'学习概况');
 const stats=h('div','clean-panel');const line=h('div','bigpct');line.appendChild(h('b',null,pct+'%'));line.appendChild(h('span',null,'课程章节完成度 · '+pr.done+'/'+pr.total));stats.appendChild(line);const bar=h('div','bar');const fill=h('i');fill.style.width=pct+'%';bar.appendChild(fill);stats.appendChild(bar);box.appendChild(stats);
}
function renderCourseManage(){
 const box=document.getElementById('v-course-manage');box.textContent='';
 const head=h('div','phead');head.appendChild(h('h1',null,'课程'));box.appendChild(head);
 box.appendChild(h('p','page-intro','整理你的学习内容。你可以新增课程、调整课程封面与简介，也可以导入课程资料文件。'));
 const panel=h('div','clean-panel');const hd=h('div','hd');hd.appendChild(h('h2',null,'课程列表'));hd.appendChild(h('span','pill',COURSES.length+' 门'));panel.appendChild(hd);
 COURSES.forEach(function(co){const m=coMeta(co),r=h('div','course-manage-row');r.appendChild(coverEl(co,'sm'));const ci=h('div','ci');ci.appendChild(h('b',null,m.title));ci.appendChild(h('small',null,m.sub||'暂无简介'));r.appendChild(ci);const e=h('button','btn sm','编辑');e.onclick=function(){openCourseEditor(co);};r.appendChild(e);panel.appendChild(r);});
 const actions=h('div','action-line');const add=h('button','btn pri','＋ 新增课程');add.onclick=function(){openNewCourseEditor();};actions.appendChild(add);
 const imp=h('button','btn','导入课程数据');imp.onclick=function(){openCourseImport();};actions.appendChild(imp);panel.appendChild(actions);box.appendChild(panel);
}
function openNewCourseEditor(){
 let cover='';const body=h('div');const ti=document.createElement('input');ti.type='text';ti.placeholder='课程名称';const su=document.createElement('textarea');su.placeholder='课程简介';
 const up=h('button','btn','上传封面');up.onclick=function(){pickImage(480,function(d){cover=d;up.textContent='封面已选择 ✓';});};body.appendChild(up);body.appendChild(field('课程名称',ti));body.appendChild(field('课程简介',su));
 openSheet('新增课程',body,function(){const title=ti.value.trim();if(!title){toast('请填写课程名称');return false;}S.ui=S.ui||{};S.ui.userCourses=S.ui.userCourses||[];const id='custom_'+Date.now();const obj={id,title,sub:su.value.trim(),cover:'cv-a',glyph:'book',desc:su.value.trim(),custom:true};S.ui.userCourses.push(obj);if(cover)S.ui.co=S.ui.co||{},S.ui.co[id]={cover:cover};COURSES.push(obj);save();renderCourseManage();toast('课程已新增');});
}
function openCourseImport(){
 const body=h('div');const tip=h('p','muted','支持导入本应用课程 JSON：可包含 title、description / desc、chapters。原始数据会保存在本机课程资料中，不会覆盖现有学习进度。');body.appendChild(tip);
 const ta=document.createElement('textarea');ta.rows=7;ta.placeholder='粘贴课程 JSON，或选择 .json 文件';body.appendChild(field('课程数据 JSON',ta));
 const pick=h('button','btn','选择 JSON 文件');pick.onclick=function(){const f=document.createElement('input');f.type='file';f.accept='.json,application/json';f.onchange=function(){const file=f.files&&f.files[0];if(!file)return;const rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');};rd.readAsText(file);};f.click();};body.appendChild(pick);
 openSheet('导入课程数据',body,function(){let d;try{d=JSON.parse(ta.value);}catch(e){toast('JSON 格式不正确');return false;}const arr=Array.isArray(d)?d:(Array.isArray(d.courses)?d.courses:[d]);if(!arr.length){toast('没有可导入的课程');return false;}S.ui=S.ui||{};S.ui.userCourses=S.ui.userCourses||[];arr.forEach(function(x,i){if(!x||!String(x.title||x.name||'').trim())return;const id='import_'+Date.now()+'_'+i;const co={id,title:String(x.title||x.name),sub:String(x.description||x.desc||''),desc:String(x.description||x.desc||''),cover:'cv-a',glyph:'book',custom:true,importedData:x};S.ui.userCourses.push(co);COURSES.push(co);});save();renderCourseManage();toast('课程资料已导入');});
}
function renderMyMarksOrig(){
 const box=document.getElementById('v-my-marks');box.textContent='';const head=h('div','phead');head.appendChild(h('h1',null,selectedMarkCourse?'课程划线':'我的划线'));if(selectedMarkCourse){const back=h('button','btn ghost','‹ 全部课程');back.onclick=function(){selectedMarkCourse='';renderMyMarks();};head.appendChild(back);}box.appendChild(head);
 const marks=allMarks();const groups={};marks.forEach(function(m){const key=m.courseId||S.cur;(groups[key]=groups[key]||[]).push(m);});
 if(!marks.length){box.appendChild(h('div','clean-panel','还没有划线。阅读课程时选中一段文字即可添加划线。'));return;}
 if(!selectedMarkCourse){box.appendChild(h('p','page-intro','按课程整理你保存的重点。选择课程进入独立的划线详情。'));Object.keys(groups).forEach(function(k){const co=COURSES.find(c=>c.id===k)||COURSES[0],p=h('button','clean-panel');p.style.cssText='width:100%;text-align:left;cursor:pointer';const row=h('div','course-manage-row');row.appendChild(coverEl(co,'sm'));const ci=h('div','ci');ci.appendChild(h('b',null,coMeta(co).title));ci.appendChild(h('small',null,groups[k].length+' 处划线 · 最近 '+new Date(Math.max.apply(null,groups[k].map(m=>m.at||0))).toLocaleDateString('zh-CN')));row.appendChild(ci);row.appendChild(h('span','mi-arrow','›'));p.appendChild(row);p.onclick=function(){selectedMarkCourse=k;renderMyMarks();};box.appendChild(p);});return;}
 const co=COURSES.find(c=>c.id===selectedMarkCourse)||COURSES[0],list=(groups[selectedMarkCourse]||[]).slice().sort((a,b)=>(b.at||0)-(a.at||0));const intro=h('div','clean-panel');intro.appendChild(h('h3',null,coMeta(co).title));intro.appendChild(h('p','muted',coMeta(co).sub||'课程划线与阅读备注'));intro.appendChild(h('span','pill',list.length+' 处重点'));box.appendChild(intro);
 list.forEach(function(m){const p=h('div','clean-panel'),meta=h('div','hd');meta.appendChild(h('span','pill',m.ch||'课程内容'));meta.appendChild(h('span','muted',new Date(m.at||Date.now()).toLocaleDateString('zh-CN')));p.appendChild(meta);{const q=h('p','mk-q'),sp=h('span','hlx ty-'+(m.ty||'solid')+' co-'+(m.co||'b'));sp.textContent=m.t||'（无文本）';q.appendChild(sp);p.appendChild(q);}if(m.n){const note=h('p','muted');note.style.marginTop='9px';note.textContent='备注：'+m.n;p.appendChild(note);}const actions=h('div','action-line');const go=h('button','btn sm','回到原文');go.onclick=function(){if(chById(m.ch))openMark(m.id);else toast('该课程内容尚未关联可跳转章节');};actions.appendChild(go);const del=h('button','btn sm ghost','删除');del.onclick=function(){if(!confirm('删除这条划线？'))return;S.marks=S.marks.filter(x=>x.id!==m.id);save();renderMyMarks();};actions.appendChild(del);p.appendChild(actions);box.appendChild(p);});
}

function renderCalendarOrig(){
 const box=document.getElementById('v-calendar');box.textContent='';const head=h('div','phead');head.appendChild(h('h1',null,'学习日历'));box.appendChild(head);box.appendChild(h('p','page-intro','以日期回看学习轨迹。当前版本按已学习章节记录日期；学习时长可在日期详情中补充。'));
 const now=new Date(),year=now.getFullYear(),month=now.getMonth(),first=new Date(year,month,1),last=new Date(year,month+1,0);const counts={};Object.keys(S.learn||{}).forEach(id=>{const a=S.learn[id]&&S.learn[id].at;if(a)counts[a]=(counts[a]||0)+1;});
 const panel=h('div','clean-panel'),mh=h('div','month-head');mh.appendChild(h('b',null,year+'年 '+(month+1)+'月'));mh.appendChild(h('span','muted','本月 '+Object.keys(counts).filter(k=>k.slice(0,7)===year+'-'+String(month+1).padStart(2,'0')).length+' 天有学习'));panel.appendChild(mh);
 const grid=h('div','month-grid');['日','一','二','三','四','五','六'].forEach(d=>grid.appendChild(h('div','dow',d)));for(let i=0;i<first.getDay();i++)grid.appendChild(h('div','month-day muted',''));
 for(let n=1;n<=last.getDate();n++){const date=new Date(year,month,n),key=ymd(date),num=counts[key]||0,d=h('button','month-day'+(key===today()?' active':''));d.appendChild(h('span',null,String(n)));if(num)d.appendChild(h('i'));d.onclick=function(){renderCalendarDay(key,num);};grid.appendChild(d);}panel.appendChild(grid);box.appendChild(panel);
 const log=h('div','clean-panel');log.appendChild(h('h3',null,'最近学习记录'));const entries=Object.keys(counts).sort().reverse().slice(0,10);if(!entries.length)log.appendChild(h('p','muted','完成章节学习后，这里会自动出现记录。'));entries.forEach(k=>{const r=h('div','log-row');const m=h('div','lr-main');m.appendChild(h('b',null,k));m.appendChild(h('small',null,counts[k]+' 个章节有学习记录'));r.appendChild(m);const b=h('button','btn sm','补充时长');b.onclick=function(){const inp=document.createElement('input');inp.type='text';inp.placeholder='例如 35（分钟）';openSheet('学习时长 · '+k,field('分钟',inp),function(){const v=Number(inp.value);if(!Number.isFinite(v)||v<0){toast('请输入有效分钟数');return false;}S.ui.studyLog=S.ui.studyLog||{};S.ui.studyLog[k]=v;save();renderCalendar();toast('时长已保存');});};r.appendChild(b);log.appendChild(r);});box.appendChild(log);
}
function renderCalendarDay(key,num){const v=(S.ui.studyLog||{})[key]||0;const body=h('div');body.appendChild(h('p',null,'学习日期：'+key));body.appendChild(h('p',null,'学习章节：'+(num||0)+' 个'));body.appendChild(h('p',null,'学习时长：'+v+' 分钟'));const ids=Object.keys(S.learn||{}).filter(id=>S.learn[id]&&S.learn[id].at===key);const ul=h('div','clean-panel');ul.appendChild(h('h3',null,'当日学习课程与进度'));if(ids.length){ul.appendChild(h('p',null,coMeta(COURSES.find(c=>c.id==='ops')||COURSES[0]).title+' · '+ids.length+' 个章节有学习记录'));ul.appendChild(h('p','muted','当前课程整体进度：'+courseProgress().done+' / '+courseProgress().total+' 章'));}else ul.appendChild(h('p','muted','当天暂无章节学习记录'));body.appendChild(ul);openSheet('当日学习',body);}
function renderSettings(){
 const box=document.getElementById('v-settings');box.textContent='';const head=h('div','phead');head.appendChild(h('h1',null,'设置'));box.appendChild(head);
 const pal=[['苔绿','#628b72'],['海盐蓝','#6689a5'],['雾紫','#8b7ca8'],['陶土','#b47e68'],['灰蓝','#718b91'],['橄榄','#87916a'],['浅棕','#a58b70'],['石墨','#78817c'],['樱粉','#b98591']];
 const p=h('div','clean-panel');p.appendChild(h('h3',null,'主题色'));const sw=h('div','theme-swatches');pal.forEach(([n,c],i)=>{const b=h('button','theme-swatch'+((S.ui.accent||0)===i?' on':''));b.style.setProperty('--sw',c);b.appendChild(h('i'));b.appendChild(h('span',null,n));b.onclick=function(){S.ui.accent=i;applyAccent(c);save();renderSettings();};sw.appendChild(b);});p.appendChild(sw);box.appendChild(p);
 const ap=h('div','clean-panel');ap.appendChild(h('h3',null,'外观'));const dark=h('div','my-inline');dark.appendChild(h('span','label','夜间模式'));const toggle=h('span','swt'+(S.theme==='dark'?' on':''));dark.appendChild(toggle);dark.onclick=function(){S.theme=S.theme==='dark'?'light':'dark';applyTheme();renderSettings();};ap.appendChild(dark);
 const theme=h('div','my-inline');theme.appendChild(h('span','label','主题风格'));const select=document.createElement('select');[['minimal','极简版'],['planet','星球版（预留）']].forEach(([v,n])=>{const o=h('option',null,n);o.value=v;o.selected=(S.ui.style||'minimal')===v;select.appendChild(o);});select.onchange=function(){S.ui.style=select.value;save();applyStyleMode();toast(select.value==='planet'?'已切换星球版基础样式':'已切换极简版');};theme.appendChild(select);ap.appendChild(theme);box.appendChild(ap);
}
function applyAccent(c){document.documentElement.style.setProperty('--pri',c);document.documentElement.style.setProperty('--pri-soft','color-mix(in srgb,'+c+' 12%,var(--l2))');}
function applyStyleMode(){document.documentElement.setAttribute('data-style',S.ui&&S.ui.style||'minimal');}

/* ===== 课程详情：章节 / 课程说明 / 工具箱 ===== */
function renderCourseMin(){
 const sec=document.getElementById('v-course');if(!sec)return;
 sec.textContent='';
 const co=curCourse();const m=coMeta(co);const pr=courseProgress();
 const due=dueList().length;
 const hero=h('div','cdhero');
 hero.appendChild(coverEl(co,''));
 const ci=h('div','ci');
 ci.appendChild(h('h1',null,m.title));
 ci.appendChild(h('p','sub',m.sub));
 const by=h('div','muted');by.style.marginTop='9px';
 by.textContent=MODS.length+' 模块 · '+CH.length+' 章 · '+Object.keys(ROUTES).length+' 条学习路线';
 ci.appendChild(by);
 hero.appendChild(ci);sec.appendChild(hero);
 const stt=h('div','cdstat');
 stt.appendChild(h('b',null,String(pr.done)));
 stt.appendChild(h('span',null,'章已过关 / 共 '+pr.total+' 章 · 累计学习 Day '+S.day));
 sec.appendChild(stt);
 const acts=h('div','cdacts');
 const started=Object.keys(S.learn).length>0;
 const go1=h('button','btn pri',started?'继续学习':'开始学习');
 go1.onclick=function(){nav('today');};
 acts.appendChild(go1);
 const rwrap=h('div','cdsel');
 const sel=document.createElement('select');
 Object.keys(ROUTES).forEach(function(k){
  const o=h('option',null,ROUTENAME[k]);o.value=k;
  if(k===S.route)o.selected=true;
  sel.appendChild(o);
 });
 sel.onchange=function(){S.route=this.value;S.idx=0;save();render();};
 rwrap.appendChild(sel);acts.appendChild(rwrap);
 sec.appendChild(acts);
 const seg=h('div','cdseg');
 [['chs','章节'],['about','课程说明'],['tools','工具箱']].forEach(function(s){
  const b=h('button',cdSeg===s[0]?'on':'',s[1]);
  if(s[0]==='tools'&&due)b.appendChild(h('i','nb',String(due)));
  b.onclick=function(){cdSeg=s[0];renderCourse();};
  seg.appendChild(b);
 });
 sec.appendChild(seg);
 if(cdSeg==='chs'){
  MODS.forEach(function(mo){
   const mh=h('div','modhead');
   mh.appendChild(h('span','mk2',mo.k));
   mh.appendChild(h('b',null,mo.n.replace('模块 '+mo.k+' · ','')));
   mh.appendChild(h('span',null,mo.tag));
   sec.appendChild(mh);
   CH.filter(function(c){return c.m===mo.k;}).forEach(function(c){sec.appendChild(chRow(c));});
  });
 }else if(cdSeg==='about'){
  const cd=h('div','card');
  const t=h('p','pp');t.textContent=co.desc;t.style.fontWeight='var(--fw-m)';t.style.lineHeight='1.8';cd.appendChild(t);
  const lb=h('p','muted','模块依赖关系：什么能跳，什么绝对不能');lb.style.marginTop='16px';lb.style.marginBottom='8px';cd.appendChild(lb);
  cd.appendChild(h('pre',null,DEPTEXT));
  const g=h('div','grid2');g.style.marginTop='14px';
  DEPNOTES.forEach(function(n){g.appendChild(note2(n[0],n[1]));});
  
  cd.appendChild(g);
  sec.appendChild(cd);
 }else{
  TOOLGROUPS.forEach(function(gr){
   sec.appendChild(h('div','gtitle',gr.g));
   const g=h('div','tgrid');
   gr.items.forEach(function(t){g.appendChild(toolTile(t[0],t[1],t[2],(t[0]==='review'&&due)?due:0));});
   sec.appendChild(g);
  });
 }
}

