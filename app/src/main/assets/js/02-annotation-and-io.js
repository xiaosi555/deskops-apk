/* Extracted original app layer; classic-script scope and order are intentional. */
/* SECTION: state */
const KEY='***';
let S={route:'r1',idx:0,status:{},learn:{},cases:[],gloss:[],theme:'light',day:1,marks:[],mode:'long'};
try{const raw=localStorage.getItem(KEY);if(raw)S=Object.assign(S,JSON.parse(raw));}catch(e){}
S.marks=S.marks||[];S.gloss=S.gloss||[];S.status=S.status||{};S.learn=S.learn||{};S.cases=S.cases||[];S.ui=S.ui||{}; initCourseState();
function save(){try{localStorage.setItem(KEY,JSON.stringify(S));}catch(e){}}
function today(){const d=new Date();return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');}
function dayDiff(s){if(!s)return 0;const a=new Date(s+'T00:00:00'),b=new Date(today()+'T00:00:00');return Math.round((b-a)/86400000);}
function chById(id){return CH.find(c=>c.id===id);}
function seq(){return ROUTES[S.route]||ROUTES.r1;}
function cur(){const q=seq();return chById(q[Math.min(S.idx,q.length-1)])||CH[0];}
function toast(t){const el=document.getElementById('toast');el.textContent=t;el.classList.add('on');setTimeout(()=>el.classList.remove('on'),1700);}
function h(tag,cls,txt){const e=document.createElement(tag);if(cls)e.className=cls;if(txt!=null)e.textContent=txt;return e;}
/* SECTION: motion & clipboard */
// 流式打字：每字 blur(6px) 渐显，duration 120ms，字间错开 30ms
function typeset(el,maxChars){
 if(!el)return;
 const raw=el.textContent||'';
 if(!raw||raw.length>(maxChars||72)){el.style.animation='fadeSoft 240ms var(--snap) both';return;}
 el.textContent='';
 el.setAttribute('aria-label',raw);
 const frag=document.createDocumentFragment();
 Array.from(raw).forEach((ch,i)=>{
  const s=document.createElement('span');
  s.textContent=ch;
  s.style.display='inline-block';
  s.style.whiteSpace='pre';
  s.style.animation='typeIn 120ms var(--snap) both';
  s.style.animationDelay=(i*30)+'ms';
  frag.appendChild(s);
 });
 el.appendChild(frag);
}
// 分性格动效：正反馈（点赞/复制）用 380/18 的 Q 弹微回弹，2.5s 自动隐身
let checkTimer=null;
function popCheck(msg){
 let el=document.getElementById('checkPop');
 if(!el){
  el=document.createElement('div');el.id='checkPop';el.className='check';
  const t=h('span','tick','✓');const s=h('span',null,'');
  el.appendChild(t);el.appendChild(s);document.body.appendChild(el);
 }
 el.querySelector('span:last-child').textContent=msg||'已复制';
 el.classList.remove('on');void el.offsetWidth;el.classList.add('on');
 clearTimeout(checkTimer);
 checkTimer=setTimeout(()=>{el.classList.remove('on');},2500);
}
// 复制：成功给 Q 弹对勾并自动隐身，失败退回普通提示
function copyText(txt,okMsg){
 const done=()=>popCheck(okMsg||'已复制到剪贴板');
 const fail=()=>toast('浏览器不允许直接复制，已改为文本框，手动全选即可');
 if(navigator.clipboard&&window.isSecureContext){
  navigator.clipboard.writeText(txt).then(done).catch(()=>legacyCopy(txt)?done():fail());
 }else{ legacyCopy(txt)?done():fail(); }
}
function legacyCopy(txt){
 try{
  const ta=document.createElement('textarea');
  ta.value=txt;ta.setAttribute('readonly','');ta.style.position='fixed';ta.style.left='-9999px';
  document.body.appendChild(ta);ta.select();
  const ok=document.execCommand('copy');ta.remove();return ok;
 }catch(e){return false;}
}
function chapterDigest(c){
 const d=DEEP[c.id]||{deep:[],out:[],flow:[]};
 const L=['','【'+c.id+' '+c.t+'】','本节要解决：'+c.s,'','■ 双轨对照'];
 c.plain.forEach(([t,s])=>L.push('· '+t+' = '+s));
 L.push('','■ 专业清单',c.terms.split('｜').join(' / '));
 (d.deep||[]).forEach(([t,b])=>{L.push('','■ '+t,b);});
 L.push('','■ 命令与样例');

 (c.cmds||[]).forEach(x=>L.push('$ '+x.c+'  ('+x.n+')'));
 (d.out||[]).forEach(([cmd,rep])=>{L.push('$ '+cmd,'预期输出：',rep);});
 (d.flow||[]).forEach(([t,steps])=>L.push('','■ '+t,steps));
 L.push('','■ 避坑');
 (c.pits||[]).forEach(p=>L.push('错误：'+p.w,'  后果：'+p.c,'  正确：'+p.r));
 L.push('','■ 过关标准',c.gate,'','■ 练习题',c.quiz.q,'','参考答案',c.quiz.a);
 return L.join('\n');
}

/* SECTION: annotation v4 */
const LINE_TYPES=[['solid','横线'],['wavy','波浪线'],['dash','虚线']];
const LINE_COLORS=[['b','#2f6fed','蓝'],['g','#188a5c','绿'],['a','#b8770f','琥珀'],['r','#c4483f','红']];
const TAG_PRESET=['易错','面试','命令','流程'];
function uid(){return 'm'+Date.now().toString(36)+Math.random().toString(36).slice(2,7);}
function normText(t){return String(t==null?'':t).replace(/\uFFFC/g,'').replace(/\s+/g,' ');}
function sigOf(t){const s=normText(t);let h=2166136261;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)>>>0;}return 's'+(h>>>0).toString(36)+'_'+s.length;}
function blockText(el){
 if(el.dataset.sig===undefined||el.dataset.orig===undefined){
  const r=document.createRange();r.selectNodeContents(el);
  el.dataset.orig=r.toString();el.dataset.sig=sigOf(el.dataset.orig);
 }
 return el.dataset.orig;
}
function allBlocks(){
 return Array.prototype.slice.call(document.querySelectorAll('[data-blk]')).filter(function(el){
  return el.dataset.ch&&el.dataset.blk!==undefined;
 });
}
function ownerBlock(node){
 let n=node&&(node.nodeType===3?node.parentNode:node);
 return n&&n.closest?n.closest('[data-blk]'):null;
}
function markById(id){const l=(S.marks||[]).filter(function(x){return x.id===id;});return l[0];}
function migrateMarkList(list){
 (list||[]).forEach(function(m){
  if(!m||typeof m!=='object')return;
  if(!m.id)m.id=uid();
  if(!m.segs){m.segs=[{sig:'@'+(m.blk||''),s:m.a||0,e:m.b||0,txt:m.t||''}];}
  m.segs.forEach(function(sg){if(sg.txt==null)sg.txt='';if(sg.sig==null)sg.sig='';});
  if(!m.tags)m.tags=[];
  if(!m.up)m.up=m.at||Date.now();
  const joined=m.segs.map(function(s){return s.txt;}).join('');
  m.t=joined||m.t||'';
  delete m.a;delete m.b;delete m.blk;
 });
}
function migrateMarks(){migrateMarkList(S.marks||[]);}
function fingerprint(m){return (m.ch||'')+'|'+(m.segs||[]).map(function(s){return sigOf(s.txt);}).join(',');}
function commonEnd(a,b){
 a=String(a||'');b=String(b||'');let n=0;
 while(n<a.length&&n<b.length&&a[a.length-1-n]===b[b.length-1-n])n++;
 return n;
}
function commonStart(a,b){
 a=String(a||'');b=String(b||'');let n=0;
 while(n<a.length&&n<b.length&&a[n]===b[n])n++;
 return n;
}
/* 同一句话在正文里重复出现时，用前后文挑出正确的那一处，避免划线跑到别处 */
function pickByContext(text,sg){
 const t=sg.txt;if(!t)return null;
 const cands=[];let i=-1;
 while((i=text.indexOf(t,i+1))>=0){cands.push(i);if(cands.length>60)break;}
 if(!cands.length)return null;
 let best=cands[0],bestScore=-1e9;
 cands.forEach(function(a){
  let sc=0;
  if(sg.pre)sc+=commonEnd(text.slice(Math.max(0,a-18),a),sg.pre)*2;
  if(sg.post)sc+=commonStart(text.slice(a+t.length,a+t.length+18),sg.post)*2;
  sc-=Math.min(6,Math.abs(a-(sg.s||0))/120);
  if(sc>bestScore){bestScore=sc;best=a;}
 });
 return {a:best,b:best+t.length};
}
function resolveSeg(el){
 const text=blockText(el),sig=el.dataset.sig,out=[];
 (S.marks||[]).forEach(function(m){
  if(m.ch!==el.dataset.ch)return;
  (m.segs||[]).forEach(function(sg){
   let hit=null;
   if(sg.sig===sig&&sg.sig&&sg.txt){
    const a=Math.max(0,Math.min(sg.s||0,text.length)),b=Math.max(a,Math.min(sg.e||0,text.length));
    if(b>a&&text.slice(a,b)===sg.txt)hit={a:a,b:b};
   }
   if(!hit&&sg.txt&&sg.txt.length>1)hit=pickByContext(text,sg);
   if(hit){sg.sig=sig;sg.s=hit.a;sg.e=hit.b;sg.txt=text.slice(hit.a,hit.b);out.push({m:m,a:hit.a,b:hit.b});}
  });
 });
 out.sort(function(x,y){return (x.m.at||0)-(y.m.at||0)||x.a-y.a;});
 return out;
}
function paintBlock(el){
 const text=blockText(el),hits=resolveSeg(el);
 el.textContent='';
 if(!hits.length){el.appendChild(document.createTextNode(text));return;}
 const pts={};pts[0]=1;pts[text.length]=1;
 hits.forEach(function(x){pts[x.a]=1;pts[x.b]=1;});
 const b=Object.keys(pts).map(Number).sort(function(p,q){return p-q;});
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
  el.appendChild(node);
 }
}
function attachAnnotation(root){
 if(!root)return;
 Array.prototype.forEach.call(root.querySelectorAll('[data-blk]'),function(el){blockText(el);paintBlock(el);});
}
function rerenderMarks(){
 allBlocks().forEach(paintBlock);
 if(view==='anno')renderAnno();
 if(view==='io'){bindIoOnce();renderIo();}
 if(view==='io')renderIo();
}
function offOf(el,node,off){
 try{
  const r=document.createRange();r.setStart(el,0);r.setEnd(node,off);
  return r.toString().length;
 }catch(e){return -1;}
}
function collectSel(){
 const sel=window.getSelection();
 if(!sel||!sel.rangeCount||sel.isCollapsed)return null;
 const r=sel.getRangeAt(0);
 const list=allBlocks();
 const A=ownerBlock(r.startContainer),B=ownerBlock(r.endContainer);
 if(!A||!B)return null;
 let i=list.indexOf(A),j=list.indexOf(B);
 if(i<0||j<0)return null;
 if(i>j){const t=i;i=j;j=t;}
 const out=[];
 const push=function(el,a,b){
  const tx=blockText(el);
  a=Math.max(0,Math.min(tx.length,a));b=Math.max(a,Math.min(tx.length,b));
  if(b>a)out.push({el:el,a:a,b:b,txt:tx.slice(a,b)});
 };
 let a0=0,a1=blockText(A).length,b0=0,b1=blockText(B).length;
 const sa=offOf(A,r.startContainer,r.startOffset),sb=offOf(B,r.endContainer,r.endOffset);
 if(i===j){if(sa>=0&&sb>=0&&sb>sa)push(A,sa,sb);}
 else{
  if(sa>=0)push(A,sa,a1);
  for(let q=i+1;q<j;q++){push(list[q],0,blockText(list[q]).length);}
  if(sb>=0)push(B,b0,sb);
 }
 const total=out.reduce(function(n,x){return n+x.txt.replace(/\s/g,'').length;},0);
 if(!out.length||total<1)return null;
 return out;
}
let curCo='b',pendRec=null,barPos=null,lastRect=null,selTimer=null,rafQ=false;
const IS_COARSE=!!(window.matchMedia&&window.matchMedia('(pointer:coarse)').matches);
try{if(S.ui&&S.ui.bar&&typeof S.ui.bar.x==='number')barPos={x:S.ui.bar.x,y:S.ui.bar.y};}catch(e){}
/* 视口：手机键盘弹出时 visualViewport 才是"真正看得见"的区域 */
function vpSize(){
 const vv=window.visualViewport;
 return {w:Math.round(vv?vv.width:window.innerWidth),h:Math.round(vv?vv.height:window.innerHeight),top:Math.round(vv?vv.offsetTop:0),left:Math.round(vv?vv.offsetLeft:0)};
}
function clampBar(x,y,bar){
 const v=vpSize(),w=bar.offsetWidth||280,hh=bar.offsetHeight||44;
 const maxX=Math.max(v.left+6,v.left+v.w-w-6),maxY=Math.max(v.top+6,v.top+v.h-hh-6);
 return {x:Math.min(Math.max(v.left+6,x),maxX),y:Math.min(Math.max(v.top+6,y),maxY)};
}
function setBarPos(x,y){
 const bar=document.getElementById('selBar');if(!bar)return;
 const p=clampBar(x,y,bar);
 bar.style.left=p.x+'px';bar.style.top=p.y+'px';
 return p;
}
function hideBar(){const b=document.getElementById('selBar');if(b)b.classList.remove('on');}
function syncBarSel(){
 const b=document.getElementById('selBar');if(!b)return;
 const rec=pendRec||null;
 b.querySelectorAll('.sw').forEach(function(el){el.classList.remove('sel');});
 LINE_TYPES.forEach(function(t){const el=b.querySelector('.sw.t-'+t[0]);if(el)el.classList.toggle('sel',!!rec&&t[0]===(rec.ty||'solid'));});
 LINE_COLORS.forEach(function(c){const el=b.querySelector('.sw.c-'+c[0]);if(el)el.classList.toggle('sel',c[0]===(rec?rec.co:curCo));});
}
function ensureBar(){
 let b=document.getElementById('selBar');if(b)return b;
 b=document.createElement('div');b.id='selBar';b.className='selbar';b.setAttribute('role','toolbar');b.setAttribute('aria-label','划线工具条');
 const grab=h('button','grab','⠿');grab.type='button';grab.title='按住拖动，把工具条放到不挡字的地方';grab.setAttribute('aria-label','拖动工具条');
 b.appendChild(grab);
 const lab=function(t){const e=h('span','lb',t);b.appendChild(e);return e;};
 lab('线型');
 LINE_TYPES.forEach(function(t){
  const s=h('button','sw t-'+t[0]);s.type='button';s.title=t[1]+'（点一下即划线）';
  s.appendChild(document.createElement('i'));
  s.onclick=function(){addMark(t[0],null);};b.appendChild(s);
 });
 const sep=h('span','sep');b.appendChild(sep);
 lab('颜色');
 LINE_COLORS.forEach(function(c){
  const s=h('button','sw c-'+c[0]);s.type='button';s.title=c[2];
  s.appendChild(document.createElement('i'));
  s.onclick=function(){addMark(null,c[0]);};b.appendChild(s);
 });
 const fin=h('button','x','加备注');
 fin.type='button';
 fin.onclick=function(){
  if(!pendRec){hideBar();return;}
  const id=pendRec.id;hideBar();
  const s=window.getSelection();if(s)s.removeAllRanges();
  openEditor(id);
 };
 b.appendChild(fin);
 const ok=h('button','x done','完成');ok.type='button';
 ok.onclick=function(){pendRec=null;hideBar();const s=window.getSelection();if(s)s.removeAllRanges();toast('划线已存进「我的划线」');};
 b.appendChild(ok);
 b.appendChild(h('span','hint','拖动中 · 松手即固定'));
 document.body.appendChild(b);
 bindBarDrag(b);
 return b;
}
/* 拖动：Pointer Events 同时覆盖鼠标、手指、触控笔；松手后记住位置 */
function bindBarDrag(bar){
 const grab=bar.querySelector('.grab');if(!grab)return;
 let st=null;
 grab.addEventListener('pointerdown',function(e){
  if(e.button!=null&&e.button!==0)return;
  const r=bar.getBoundingClientRect();
  st={dx:e.clientX-r.left,dy:e.clientY-r.top,moved:false,x:r.left,y:r.top};
  bar.classList.add('dragging');
  try{grab.setPointerCapture(e.pointerId);}catch(_){}
  e.preventDefault();e.stopPropagation();
 });
 grab.addEventListener('pointermove',function(e){
  if(!st)return;
  const p=setBarPos(e.clientX-st.dx,e.clientY-st.dy);
  st.moved=true;st.x=p.x;st.y=p.y;
  e.preventDefault();
 });
 const finish=function(e){
  if(!st)return;
  const s=st;st=null;
  bar.classList.remove('dragging');
  try{if(e&&e.pointerId!=null)grab.releasePointerCapture(e.pointerId);}catch(_){}
  if(s.moved){
   barPos={x:s.x,y:s.y};
   S.ui=S.ui||{};S.ui.bar=barPos;save();
   popCheck('工具条位置已记住');
  }
 };
 grab.addEventListener('pointerup',finish);
 grab.addEventListener('pointercancel',finish);
 grab.addEventListener('lostpointercapture',finish);
 /* 双击复位：下次回到跟随选区的默认位置 */
 grab.addEventListener('dblclick',function(){
  barPos=null;S.ui=S.ui||{};delete S.ui.bar;save();
  const segs=collectSel();showBarAtSelection(segs);
  toast('已恢复跟随选区');
 });
}
function selectionRect(){
 try{
  const s=window.getSelection();if(!s||!s.rangeCount)return null;
  const r=s.getRangeAt(0);let rect=r.getBoundingClientRect();
  if(!rect||(!rect.width&&!rect.height)){
   const rs=r.getClientRects();
   if(rs&&rs.length)rect=rs[rs.length-1];
  }
  return (rect&&(rect.width||rect.height))?rect:null;
 }catch(e){return null;}
}
function showBarAtSelection(segs){
 const bar=ensureBar();
 bar.classList.add('on');
 syncBarSel();
 const r=selectionRect()||(segs&&segs[0]?segs[0].el.getBoundingClientRect():null);
 lastRect=r;
 if(barPos){setBarPos(barPos.x,barPos.y);return;}
 const v=vpSize(),bw=bar.offsetWidth,bh=bar.offsetHeight;
 let x,y;
 if(v.w<640||IS_COARSE){
  /* 小屏/触屏：贴底居中，但抬到底部导航和迷你条之上，不挡任何入口 */
  x=(v.w-bw)/2;
  y=v.top+v.h-bh-76;
 }else if(r){
  x=r.left+r.width/2-bw/2;
  y=r.top-bh-10;
  if(y<v.top+8)y=Math.min(v.top+v.h-bh-8,r.bottom+10);
 }else{
  x=(v.w-bw)/2;y=v.top+80;
 }
 setBarPos(x,y);
}
function repositionBar(){
 if(rafQ)return;rafQ=true;
 requestAnimationFrame(function(){
  rafQ=false;
  const bar=document.getElementById('selBar');
  if(!bar||!bar.classList.contains('on'))return;
  if(barPos){setBarPos(barPos.x,barPos.y);return;}
  const r=selectionRect()||lastRect;if(!r)return;
  const v=vpSize(),bw=bar.offsetWidth,bh=bar.offsetHeight;
  let x=r.left+r.width/2-bw/2,y=r.top-bh-10;
  if(y<v.top+8)y=Math.min(v.top+v.h-bh-8,r.bottom+10);
  setBarPos(x,y);
 });
}
window.addEventListener('scroll',repositionBar,{passive:true});
window.addEventListener('resize',repositionBar);
if(window.visualViewport){
 window.visualViewport.addEventListener('resize',repositionBar);
 window.visualViewport.addEventListener('scroll',repositionBar);
}
function mkRec(segs){
 const rec={id:uid(),courseId:activeCourse||'ops',ch:segs[0].el.dataset.ch,
  segs:segs.map(function(s){
   const tx=blockText(s.el);
   return {sig:s.el.dataset.sig,s:s.a,e:s.b,txt:s.txt,
    pre:tx.slice(Math.max(0,s.a-14),s.a),
    post:tx.slice(s.b,Math.min(tx.length,s.b+14))};
  }),
  ty:'solid',co:curCo,n:'',tags:[],t:segs.map(function(s){return s.txt;}).join(''),at:Date.now(),up:Date.now()};
 S.marks=S.marks||[];S.marks.push(rec);pendRec=rec;return rec;
}
function addMark(ty,co){
 if(!pendRec){const segs=collectSel();if(!segs)return;pendRec=mkRec(segs);}
 if(ty)pendRec.ty=ty;
 if(co){curCo=co;pendRec.co=co;}
 pendRec.up=Date.now();
 pendRec.t=(pendRec.segs||[]).map(function(s){return s.txt;}).join('');
 save();rerenderMarks();
 const bar=ensureBar();bar.classList.add('on');
 syncBarSel();
 popCheck(ty?'已划线 · '+((LINE_TYPES.find(function(x){return x[0]===ty;})||['','横线'])[1]):'已改颜色 · 可继续叠别的颜色');
}
function checkSelection(){
 const p=document.getElementById('notePanel');
 if(p&&p.classList.contains('on'))return;
 const segs=collectSel();
 if(!segs){if(!pendRec)hideBar();return;}
 if(pendRec)return;
 pendRec=null;
 showBarAtSelection(segs);
}
function scheduleCheck(ms){clearTimeout(selTimer);selTimer=setTimeout(checkSelection,ms==null?120:ms);}
/* 三路触发：鼠标抬起、手指选择（selectionchange 去抖）、触屏抬起后再等把手落位 */
document.addEventListener('selectionchange',function(){scheduleCheck(IS_COARSE?420:140);});
document.addEventListener('pointerup',function(e){if(e.button!=null&&e.button>0)return;scheduleCheck(IS_COARSE?260:0);});
document.addEventListener('touchend',function(){scheduleCheck(320);},{passive:true});
document.addEventListener('keyup',function(e){
 if(e.key==='Escape'){hideBar();closeEditor();}
 else if(e.shiftKey)scheduleCheck(0);
});
document.addEventListener('pointerdown',function(e){
 const bar=document.getElementById('selBar');
 if(bar&&bar.classList.contains('on')&&!bar.contains(e.target)){
  /* 点到工具条和正文以外：清空选区并彻底收起，不再挡住底部导航 */
  const onText=e.target.closest&&e.target.closest('[data-blk]');
  if(!onText){
   pendRec=null;
   const s0=window.getSelection();if(s0)s0.removeAllRanges();
  }
  hideBar();
 }
 const p=document.getElementById('notePanel');
 if(p&&p.classList.contains('on')&&!p.contains(e.target)&&!(e.target.closest&&e.target.closest('[data-mid]')))closeEditor();
},{passive:true});
document.addEventListener('click',function(e){
 const sp=e.target.closest?e.target.closest('[data-mid]'):null;
 if(!sp)return;
 const s=window.getSelection();
 if(s&&!s.isCollapsed&&String(s).trim().length>0)return;
 e.preventDefault();e.stopPropagation();openEditor(sp.dataset.mid);
});
document.addEventListener('copy',function(e){
 const s=window.getSelection();
 if(!s||s.isCollapsed)return;
 if(!ownerBlock(s.anchorNode))return;
 e.preventDefault();
 toast('正文已关闭复制：选中后用上方按钮划线，点划线写备注');
});
document.addEventListener('contextmenu',function(e){
 const el=e.target.closest?e.target.closest('[data-blk]'):null;
 if(!el)return;
 /* 触屏上放行系统菜单，否则会挡住系统自带的选择把手 */
 if(IS_COARSE)return;
 e.preventDefault();
 toast('请用划线标记：横线 / 波浪线 / 虚线 × 四色，支持跨段落与多条并存');
});
/* SECTION: mark editor panel */
function marksAtPoint(el,point){
 const out=[];
 (S.marks||[]).forEach(function(m){
  if(m.ch!==el.dataset.ch)return;
  (m.segs||[]).forEach(function(sg){
   if(sg.sig&&sg.sig!==el.dataset.sig)return;
   const a=sg.s||0,b=(sg.e||0);
   if(b>a&&a<=point&&point<=b&&out.indexOf(m)<0)out.push(m);
  });
 });
 out.sort(function(a,b){return (a.at||0)-(b.at||0);});
 return out;
}
let annEdit=null;
function blocksBySig(ch,sig){
 return allBlocks().filter(function(el){return el.dataset.ch===ch&&el.dataset.sig===sig;})[0]||null;
}
function segBlock(m,sg){
 const exact=blocksBySig(m.ch,sg.sig);
 if(exact)return exact;
 /* 换视图后原位块的 sig 变了：用原文在该章里找回唯一匹配的块 */
 if(!sg.txt)return null;
 const cands=allBlocks().filter(function(el){
  return el.dataset.ch===m.ch&&blockText(el).indexOf(sg.txt)>=0;
 });
 if(cands.length===1)return cands[0];
 return cands.filter(function(el){return blockText(el).indexOf(sg.pre||'\u0000')>=0;})[0]||null;
}
function adjustMark(edge,delta){
 const m=markById(annEdit);if(!m)return;
 const segs=m.segs||[];if(!segs.length)return;
 const idx=edge==='s'?0:segs.length-1;
 const sg=segs[idx];
 const el=segBlock(m,sg);
 if(!el){toast('这条划线不在当前页面，先点「定位到原文」再微调');return;}
 const tx=blockText(el);
 let a=sg.s||0,b=sg.e||0;
 if(edge==='s'){
  a=Math.max(0,Math.min(a+delta,b-1));
  if(idx===0&&segs.length>1&&a>=b)return;
 }else{
  b=Math.min(tx.length,Math.max(b+delta,a+1));
 }
 if(b-a<1)return;
 sg.sig=el.dataset.sig;sg.s=a;sg.e=b;sg.txt=tx.slice(a,b);
 sg.pre=tx.slice(Math.max(0,a-14),a);sg.post=tx.slice(b,Math.min(tx.length,b+14));
 m.t=segs.map(function(x){return x.txt;}).join('');
 m.up=Date.now();save();rerenderMarks();syncEditor();
}
function expandMark(){
 const m=markById(annEdit);if(!m)return;
 const segs=m.segs||[];if(!segs.length)return;
 const first=segs[0],last=segs[segs.length-1];
 const e1=segBlock(m,first),e2=segBlock(m,last);
 if(!e1||!e2){toast('这条划线不在当前页面，先点「定位到原文」再微调');return;}
 const t1=blockText(e1),t2=blockText(e2);
 first.sig=e1.dataset.sig;first.s=0;first.e=t1.length;first.txt=t1;
 first.pre='';first.post='';
 if(segs.length>1){last.sig=e2.dataset.sig;last.s=0;last.e=t2.length;last.txt=t2;last.pre='';last.post='';}
 m.t=segs.map(function(x){return x.txt;}).join('');
 m.up=Date.now();save();rerenderMarks();syncEditor();toast('已扩展到整段');
}
function bindPanelDrag(p){
 const hd=p.querySelector('.nh');if(!hd)return;
 let st=null;
 hd.addEventListener('pointerdown',function(e){
  if(e.target.closest('button'))return;
  const r=p.getBoundingClientRect();
  st={dx:e.clientX-r.left,dy:e.clientY-r.top};
  p.style.left=r.left+'px';p.style.top=r.top+'px';p.style.right='auto';p.style.bottom='auto';
  p.classList.add('dragging');
  try{hd.setPointerCapture(e.pointerId);}catch(_){}
  e.preventDefault();
 });
 hd.addEventListener('pointermove',function(e){
  if(!st)return;
  const v=vpSize();
  const x=Math.min(Math.max(4,e.clientX-st.dx),v.w-p.offsetWidth-4);
  const y=Math.min(Math.max(4,e.clientY-st.dy),v.top+v.h-p.offsetHeight-4);
  p.style.left=x+'px';p.style.top=y+'px';
 });
 const end=function(e){
  if(!st)return;st=null;p.classList.remove('dragging');
  try{if(e&&e.pointerId!=null)hd.releasePointerCapture(e.pointerId);}catch(_){}
 };
 hd.addEventListener('pointerup',end);
 hd.addEventListener('pointercancel',end);
}
function openEditor(mid){
 const m=markById(mid);if(!m)return;
 annEdit=mid;
 let p=document.getElementById('notePanel');
 if(!p){
  p=document.createElement('div');p.id='notePanel';p.className='notepanel';
  const hd=document.createElement('div');hd.className='nh';
  hd.appendChild(h('span','grip','⠿'));
  const bt=document.createElement('b');bt.textContent='划线备注';
  const hm=document.createElement('span');hm.id='npMeta';hm.className='muted';
  const hx=document.createElement('button');hx.className='btn ghost sm';hx.textContent='收起';hx.onclick=closeEditor;
  hd.appendChild(bt);hd.appendChild(hm);hd.appendChild(hx);p.appendChild(hd);
  const body=document.createElement('div');body.className='nb';
  const ta=document.createElement('textarea');ta.id='noteArea';ta.rows=4;
  ta.placeholder='写下你的理解、下次排障第一步、或者面试怎么讲（自动保存）';
  body.appendChild(ta);
  const r1=document.createElement('div');r1.className='r1';
  const l1=document.createElement('span');l1.className='lb';l1.textContent='线型';r1.appendChild(l1);
  LINE_TYPES.forEach(function(t){
   const s=document.createElement('button');s.className='sw t-'+t[0];s.title=t[1];
   s.appendChild(document.createElement('i'));
   s.onclick=function(){const mm=markById(annEdit);if(!mm)return;mm.ty=t[0];mm.up=Date.now();save();rerenderMarks();syncEditor();popCheck('已改为'+t[1]);};
   r1.appendChild(s);
  });
  const l2=document.createElement('span');l2.className='lb';l2.textContent='颜色';r1.appendChild(l2);
  LINE_COLORS.forEach(function(c){
   const s=document.createElement('button');s.className='sw c-'+c[0];s.title=c[2];
   s.appendChild(document.createElement('i'));
   s.onclick=function(){const mm=markById(annEdit);if(!mm)return;mm.co=c[0];curCo=c[0];mm.up=Date.now();save();rerenderMarks();syncEditor();popCheck('已改为'+c[2]);};
   r1.appendChild(s);
  });
  body.appendChild(r1);
  const r2=document.createElement('div');r2.className='r1';
  const l3=document.createElement('span');l3.className='lb';l3.textContent='标签';r2.appendChild(l3);
  const tw=document.createElement('span');tw.id='npTags';tw.className='tw';r2.appendChild(tw);
  const add=document.createElement('button');add.className='btn sm';add.textContent='＋ 自定义';
  add.onclick=function(){const v=prompt('输入标签名（如：厂商差异）','');if(!v)return;const mm=markById(annEdit);if(!mm)return;mm.tags=mm.tags||[];if(mm.tags.indexOf(v)<0)mm.tags.push(v);mm.up=Date.now();save();rerenderMarks();syncEditor();};
  r2.appendChild(add);body.appendChild(r2);
  /* 范围微调：手指选字不准时，按一下就能挪一个字符 */
  const r3=h('div','adj');
  r3.appendChild(h('span','lb','范围'));
  const nb=function(t,title,fn){
   const b=document.createElement('button');b.className='btn';b.textContent=t;b.title=title;b.onclick=fn;r3.appendChild(b);return b;
  };
  nb('起 −','划线起点往前一个字',function(){adjustMark('s',-1);});
  nb('起 +','划线起点往后一个字',function(){adjustMark('s',1);});
  nb('终 −','划线终点往前一个字',function(){adjustMark('e',-1);});
  nb('终 +','划线终点往后一个字',function(){adjustMark('e',1);});
  nb('整段','直接划满整段',expandMark);
  body.appendChild(r3);
  p.appendChild(body);
  const ft=document.createElement('div');ft.className='ft';
  const btn=function(txt,cls,fn){const b=document.createElement('button');b.className=cls;b.textContent=txt;b.onclick=fn;ft.appendChild(b);return b;};
  btn('定位到原文','btn sm',function(){locateMark(annEdit);});
  btn('复制这条原文','btn sm',function(){const mm=markById(annEdit);if(mm)copyText(mm.t+(mm.n?'\n备注：'+mm.n:'')+(mm.tags&&mm.tags.length?'\n标签：'+mm.tags.join('/'):''),'这条原文已复制');});
  btn('删除这条划线','btn sm',function(){if(!annEdit)return;S.marks=(S.marks||[]).filter(function(x){return x.id!==annEdit;});save();closeEditor();rerenderMarks();toast('已删除这条划线');});
  p.appendChild(ft);
  document.body.appendChild(p);
  ta.oninput=function(){const mm=markById(annEdit);if(!mm)return;mm.n=ta.value;mm.up=Date.now();save();};
  ta.onkeydown=function(e){if(e.key==='Escape')closeEditor();};
  bindPanelDrag(p);
 }
 p.classList.add('on');
 const inp=document.getElementById('noteArea');inp.value=m.n||'';
 syncEditor();
 /* 触屏不自动聚焦：否则键盘弹起会挡住刚刚划的那句话 */
 if(!IS_COARSE)setTimeout(function(){try{inp.focus();}catch(e){}},60);
}
function syncEditor(){
 const m=markById(annEdit);if(!m)return;
 const p=document.getElementById('notePanel');if(!p)return;
 const c0=chById(m.ch);
 const segs=m.segs||[];
 const total=segs.reduce(function(n,s){return n+(s.txt||'').length;},0);
 p.querySelector('#npMeta').textContent=(m.ch||'')+' '+(c0?c0.t:'')+' · '+segs.length+' 段 · '+total+' 字 · 更新 '+new Date(m.up||m.at||Date.now()).toLocaleDateString('zh-CN');
 const r1=p.querySelectorAll('.r1')[0];
 r1.querySelectorAll('.sw').forEach(function(el){
  const ty=LINE_TYPES.filter(function(t){return el.className.indexOf('t-'+t[0])>=0;})[0];
  const co=LINE_COLORS.filter(function(c){return el.className.indexOf('c-'+c[0])>=0;})[0];
  if(ty)el.classList.toggle('sel',ty[0]===(m.ty||'solid'));
  if(co)el.classList.toggle('sel',co[0]===(m.co||'b'));
 });
 const tw=p.querySelector('#npTags');tw.textContent='';
 const pool=TAG_PRESET.concat(m.tags||[]).filter(function(v,i,arr){return arr.indexOf(v)===i;});
 pool.forEach(function(tg){
  const b=document.createElement('button');
  b.className='tg'+(((m.tags||[]).indexOf(tg)>=0)?' on':'');
  b.textContent=tg;
  b.onclick=function(){m.tags=m.tags||[];const ix=m.tags.indexOf(tg);if(ix>=0)m.tags.splice(ix,1);else m.tags.push(tg);m.up=Date.now();save();rerenderMarks();syncEditor();};
  tw.appendChild(b);
 });
}
function closeEditor(){const p=document.getElementById('notePanel');if(p)p.classList.remove('on');annEdit=null;}
function locateMark(id){
 const m=markById(id);if(!m)return;
 S.pin=m.ch;S.mode='long';closeEditor();hideBar();save();
 nav('today');
 setTimeout(function(){
  const sp=document.querySelector('[data-mid="'+id+'"]');
  if(!sp){toast('这条划线在当前视图没找到，原文可能已被更新');return;}
  const blk=sp.closest('[data-blk]');
  (blk||sp).scrollIntoView({behavior:'smooth',block:'center'});
  const t=blk||sp;t.classList.add('flash');
  setTimeout(function(){t.classList.remove('flash');},1700);
 },160);
}
function markStats(){
 const ms=S.marks||[];
 return {total:ms.length,notes:ms.filter(function(x){return x.n;}).length,tagged:ms.filter(function(x){return (x.tags||[]).length;}).length,multi:ms.filter(function(x){return (x.segs||[]).length>1;}).length};
}
/* SECTION: anno list with filters */
const AF={ch:'',ty:'',co:'',has:'',tag:'',q:'',sort:'new',sel:{}};
function annoFiltered(){
 let list=(S.marks||[]).slice();
 if(AF.ch)list=list.filter(function(m){return m.ch===AF.ch;});
 if(AF.ty)list=list.filter(function(m){return (m.ty||'solid')===AF.ty;});
 if(AF.co)list=list.filter(function(m){return (m.co||'b')===AF.co;});
 if(AF.has==='yes')list=list.filter(function(m){return !!m.n;});
 if(AF.has==='no')list=list.filter(function(m){return !m.n;});
 if(AF.tag)list=list.filter(function(m){return (m.tags||[]).indexOf(AF.tag)>=0;});
 if(AF.q){const q=AF.q.toLowerCase();list=list.filter(function(m){return ((m.t||'')+' '+(m.n||'')+' '+(m.tags||[]).join(' ')+' '+(m.ch||'')).toLowerCase().indexOf(q)>=0;});}
 const map={new:function(a,b){return (b.at||0)-(a.at||0);},old:function(a,b){return (a.at||0)-(b.at||0);},ch:function(a,b){return (a.ch||'').localeCompare(b.ch||'')||(a.segs||[]).length-(b.segs||[]).length;},note:function(a,b){return ((b.n||'').length-(a.n||'').length)||(b.at||0)-(a.at||0);}};
 return list.sort(map[AF.sort]||map.new);
}
function allTags(){
 const t=[];(S.marks||[]).forEach(function(m){(m.tags||[]).forEach(function(x){if(t.indexOf(x)<0)t.push(x);});});
 return t;
}
function buildAnnoTools(){
 const box=document.getElementById('annoTools');if(!box||box.childElementCount)return;
 const mk=function(label,opts,key){
  const w=document.createElement('label');w.className='fl';
  const sp=document.createElement('span');sp.textContent=label;w.appendChild(sp);
  const s=document.createElement('select');
  opts.forEach(function(o){const op=document.createElement('option');op.value=o[0];op.textContent=o[1];s.appendChild(op);});
  s.value=AF[key];s.onchange=function(){AF[key]=s.value;AF.sel={};renderAnno();};
  w.appendChild(s);box.appendChild(w);return s;
 };
 mk('章节',[['','全部章节']].concat(CH.map(function(c){return [c.id,c.id+' '+c.t];})),'ch');
 mk('线型',[['','全部线型']].concat(LINE_TYPES.map(function(t){return [t[0],t[1]];})),'ty');
 mk('颜色',[['','全部颜色']].concat(LINE_COLORS.map(function(c){return [c[0],c[2]];})),'co');
 mk('备注',[['','不限备注'],['yes','只看有备注'],['no','只看无备注']],'has');
 mk('标签',[['','全部标签']].concat(allTags().map(function(t){return [t,t];})),'tag');
 mk('排序',[['new','新记录在前'],['old','旧记录在前'],['ch','按章节'],['note','备注长的在前']],'sort');
 const sw=document.createElement('div');sw.className='fl grow';
 const inp=document.createElement('span');inp.textContent='搜索';sw.appendChild(inp);
 const it=document.createElement('input');it.type='text';it.id='annoSearch';it.placeholder='划线原文 / 备注 / 标签，例如 SRV、42 天、9100';
 it.oninput=function(){AF.q=it.value.trim();renderAnno();};
 sw.appendChild(it);box.appendChild(sw);
}
function annoItemText(m){
 return [(m.segs||[{txt:m.t}]).map(function(s){return s.txt;}).join(' ／ '),'线型：'+((LINE_TYPES.find(function(x){return x[0]===(m.ty||'solid');})||['','横线'])[1])+'　颜色：'+((LINE_COLORS.find(function(x){return x[0]===(m.co||'b');})||['','','蓝'])[2])+(m.tags&&m.tags.length?'　标签：'+m.tags.join('/'):''),'备注：'+(m.n||'（无）'),'时间：'+new Date(m.at||Date.now()).toLocaleString('zh-CN')].join('\n');
}
function renderAnno(){
 buildAnnoTools();
 const box=document.getElementById('annoList');if(!box)return;
 box.textContent='';
 const st=markStats();
 const cnt=document.getElementById('annoCount');
 if(cnt)cnt.textContent=st.total+' 处 · 有备注 '+st.notes+' · 跨段 '+st.multi;
 if(!st.total){
  const c=h('div','card');
  c.appendChild(h('p','muted','还没有划线。去「今日卡片」的正文里选中一段话（可以跨段落连续选），松手后浮出面板：三种线型 × 四种颜色；同一段落允许不同颜色并存，删除只删自己那条。点任意划线可以写多行备注、改线型颜色、打标签。'));
  box.appendChild(c);return;
 }
 const list=annoFiltered();
 const bar=h('div','card');
 const rowB=h('div','btns');rowB.style.marginTop='0';
 const mkB=function(t,cls,fn){const b=document.createElement('button');b.className=cls;b.textContent=t;b.onclick=fn;rowB.appendChild(b);return b;};
 mkB('全选当前筛选','btn sm',function(){list.forEach(function(m){AF.sel[m.id]=1;});renderAnno();});
 mkB('取消选择','btn sm',function(){AF.sel={};renderAnno();});
 mkB('删除所选','btn sm',function(){
  const n=Object.keys(AF.sel).length;if(!n){toast('还没勾选任何划线');return;}
  uiConfirm('确认删除选中的 '+n+' 条划线与备注？',function(){
  S.marks=(S.marks||[]).filter(function(m){return !AF.sel[m.id];});AF.sel={};save();rerenderMarks();toast('已删除 '+n+' 条');
  },{ok:'删除',danger:true});
 });
 mkB('导出所选为文本','btn sm pri',function(){
  const chosen=list.filter(function(m){return AF.sel[m.id];});
  const use=chosen.length?chosen:list;
  openExport({kind:'txt',title:'划线与备注摘录 · '+use.length+' 条',filename:'桌面运维-划线摘录-'+today()+'.txt',mime:'text/plain',text:marksText(use)});
 });
 rowB.appendChild(h('span','muted','筛选出 '+list.length+' / 共 '+st.total+' 处'));
 bar.appendChild(rowB);box.appendChild(bar);
 if(!list.length){box.appendChild(h('div','card')).appendChild(h('p','muted','没有符合筛选条件的划线，放宽一个条件试试。'));return;}
 const byCh={};
 list.forEach(function(m){(byCh[m.ch]=byCh[m.ch]||[]).push(m);});
 Object.keys(byCh).forEach(function(chk){
  const c0=chById(chk);
  const card=h('div','card');
  const hd=h('div','hd');hd.appendChild(h('h2',null,chk+' '+(c0?c0.t:'')));
  hd.appendChild(h('span','pill',byCh[chk].length+' 处'));card.appendChild(hd);
  byCh[chk].forEach(function(m){
   const w=h('div','annoItem'+(AF.sel[m.id]?' picked':''));
   const head=h('div','hd');
   const pick=document.createElement('input');pick.type='checkbox';pick.checked=!!AF.sel[m.id];
   pick.onchange=function(){if(pick.checked)AF.sel[m.id]=1;else delete AF.sel[m.id];renderAnno();};
   head.appendChild(pick);
   const qt=h('div','qt');
   (m.segs||[{txt:m.t}]).forEach(function(sg,ii,arr){
    const sp=document.createElement('span');
    sp.className='hlx ty-'+(m.ty||'solid')+' co-'+(m.co||'b');
    sp.style.textUnderlineOffset='0.16em';
    sp.textContent=sg.txt;qt.appendChild(sp);
    if(ii<arr.length-1)qt.appendChild(h('span','sepm',' ／ '));
   });
   head.appendChild(qt);w.appendChild(head);
   if(m.n){const nt=h('div','nt');nt.appendChild(h('b',null,'备注：'));nt.appendChild(h('span',null,m.n));w.appendChild(nt);}
   const meta=h('div','meta');
   const chip=h('span','markChip',((LINE_TYPES.find(function(x){return x[0]===(m.ty||'solid');})||['','横线'])[1])+' · '+((LINE_COLORS.find(function(x){return x[0]===(m.co||'b');})||['','','蓝'])[2])+((m.segs||[]).length>1?' · 跨 '+(m.segs||[]).length+' 段':''));
   meta.appendChild(chip);
   (m.tags||[]).forEach(function(tg){
    const t=h('span','tg on',tg);t.title='按这个标签筛选';
    t.onclick=function(){AF.tag=tg;const sels=document.querySelectorAll('#annoTools select');if(sels[4])sels[4].value=tg;renderAnno();};
    meta.appendChild(t);
   });
   meta.appendChild(h('span',null,new Date(m.at||Date.now()).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'})));
   const e1=h('button','btn sm withic');e1.innerHTML=ic('pen')+'<span>编辑备注</span>';e1.onclick=function(){openEditor(m.id);};
   const e2=h('button','btn sm','定位原文');e2.onclick=function(){locateMark(m.id);};
   const e3=h('button','btn sm','复制');e3.onclick=function(){copyText(annoItemText(m),'这条已复制');};
   meta.appendChild(e1);meta.appendChild(e2);meta.appendChild(e3);
   w.appendChild(meta);card.appendChild(w);
  });
  box.appendChild(card);
 });
}
function marksText(list){
 const out=['桌面运维学习 · 我的划线与备注　'+new Date().toLocaleString('zh-CN'),'共 '+list.length+' 处','','════════════════'];
 const byCh={};
 list.forEach(function(m){(byCh[m.ch]=byCh[m.ch]||[]).push(m);});
 Object.keys(byCh).forEach(function(chk){
  const c0=chById(chk);
  out.push('','【'+chk+' '+(c0?c0.t:'')+'】');
  byCh[chk].forEach(function(m,i){
   out.push((i+1)+'. 线型：'+((LINE_TYPES.find(function(x){return x[0]===(m.ty||'solid');})||['','横线'])[1])+'　颜色：'+((LINE_COLORS.find(function(x){return x[0]===(m.co||'b');})||['','','蓝'])[2])+(m.tags&&m.tags.length?'　标签：'+m.tags.join('/'):'')+(m.segs&&m.segs.length>1?'　跨'+m.segs.length+'段':''));
   out.push('   原文：'+(m.segs||[{txt:m.t}]).map(function(s){return s.txt;}).join(' ／ '));
   out.push('   备注：'+(m.n||'（无）'));
   out.push('   时间：'+new Date(m.at||Date.now()).toLocaleString('zh-CN'));
   out.push('');
  });
 });
 return out.join('\n');
}

/* SECTION: io center · 导出预览（先看后存，绝不自动下载） */
function fmtSize(n){
 if(n<1024)return n+' B';
 if(n<1048576)return (n/1024).toFixed(1)+' KB';
 return (n/1048576).toFixed(2)+' MB';
}
/* 只在用户明确点了「保存到本机」时才写入文件 */
function saveBlob(name,mime,text,withBom){
 try{
  const blob=new Blob([(withBom?'\uFEFF':'')+text],{type:mime+';charset=utf-8'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a');
  a.href=url;a.download=name;
  a.style.display='none';
  document.body.appendChild(a);a.click();a.remove();
  setTimeout(function(){try{URL.revokeObjectURL(url);}catch(e){}},4000);
  return true;
 }catch(e){toast('当前浏览器不允许保存文件，请用「复制全文」');return false;}
}
let xState=null,xView='preview';
function ensureXmod(){
 let root=document.getElementById('xmod');
 if(root)return root;
 root=document.createElement('div');root.id='xmod';root.className='xmod';
 root.innerHTML='<div class="mask"></div><div class="sheet" role="dialog" aria-modal="true" aria-label="导出预览">'
  +'<div class="xh"><div class="tt"><b></b><div class="xm"></div></div><button class="xclose" aria-label="关闭">✕</button></div>'
  +'<div class="xtabs"></div><div class="xb"></div><div class="xf"></div></div>';
 document.body.appendChild(root);
 root.querySelector('.mask').onclick=closeExport;
 root.querySelector('.xclose').onclick=closeExport;
 return root;
}
function closeExport(){
 const root=document.getElementById('xmod');if(!root)return;
 root.classList.remove('on');
 document.body.style.overflow='';
 xState=null;
}
function exportBody(){
 const root=ensureXmod(),o=xState,body=root.querySelector('.xb');
 body.textContent='';
 if(!o)return;
 if(o.kind==='print'&&xView!=='source'){
  const f=document.createElement('iframe');f.className='xframe';f.setAttribute('title','打印版预览');
  f.srcdoc=o.html;body.appendChild(f);return;
 }
 if(o.kind==='md'&&xView!=='source'){
  const d=h('div','xmd');d.innerHTML=mdToHtml(o.text);body.appendChild(d);return;
 }
 if(o.kind==='json'){
  let obj=null;try{obj=JSON.parse(o.text);}catch(e){}
  if(obj&&obj.counts){
   const sum=h('div','xsum');
   [['划线',obj.counts.marks],['案例',obj.counts.cases],['术语',obj.counts.gloss],['已学章节',obj.counts.learned]].forEach(function(r){
    sum.appendChild(h('span','pill',r[0]+' '+r[1]));
   });
   body.appendChild(sum);
  }
 }
 body.appendChild(h('pre','xpre',o.text));
}
function openExport(o){
 const root=ensureXmod();
 xState=o;xView='preview';
 root.querySelector('.xh b').textContent=o.title||'导出预览';
 const bytes=new Blob([o.text||o.html||'']).size;
 root.querySelector('.xh .xm').textContent=(o.filename||'')+' · '+fmtSize(bytes)+' · 仅在本机生成，未上传';
 /* 预览 / 源码切换 */
 const tabs=root.querySelector('.xtabs');tabs.textContent='';
 if(o.kind==='md'||o.kind==='print'){
  [['preview',o.kind==='print'?'打印版预览':'渲染效果'],['source','源码']].forEach(function(t){
   const b=h('button','xtab'+(xView===t[0]?' on':''),t[1]);
   b.onclick=function(){
    xView=t[0];
    for(const el of tabs.children)el.classList.remove('on');
    b.classList.add('on');
    exportBody();
   };
   tabs.appendChild(b);
  });
 }
 if(o.kind==='txt'||o.kind==='csv'||o.kind==='json')tabs.appendChild(h('span','pill',o.kind==='json'?'JSON 存档':o.kind.toUpperCase()+' 文本'));
 exportBody();
 /* 底部动作：复制最轻，保存必须再点一次 */
 const ft=root.querySelector('.xf');ft.textContent='';
 const mk=function(t,cls,fn){
  const b=document.createElement('button');b.className=cls||'btn';b.textContent=t;
  if(fn)b.onclick=fn;
  ft.appendChild(b);return b;
 };
 mk('复制全文','btn pri',function(){copyText(o.text||o.html||'','已复制全文，可直接粘进笔记');});
 if(o.kind==='print'){
  mk('打印 / 存为 PDF','btn',function(){
   const f=root.querySelector('.xframe');
   if(!f){toast('切到预览后再打印');return;}
   try{f.contentWindow.focus();f.contentWindow.print();}
   catch(e){toast('当前浏览器拦截了打印，请先「保存到本机」再打开打印');}
  });
 }
 mk('保存到本机','btn',function(){
  const name=o.filename||('导出-'+today()+'.txt');
  const mime=o.mime||'text/plain';
  const bom=o.kind==='csv'||o.kind==='txt';
  if(saveBlob(name,mime,(o.kind==='print'?(o.html||''):(o.text||'')),bom))popCheck('已保存 '+name);
 });
 if(navigator.share){
  mk('分享…','btn',async function(){
   try{await navigator.share({title:o.title||'桌面运维学习资料',text:(o.text||'').slice(0,20000)});}
   catch(e){copyText(o.text||'','已改为复制');}
  });
 }
 ft.appendChild(h('span','spacer'));
 mk('关闭','btn ghost',closeExport);
 ft.appendChild(h('div','note','这里只是预览：点「保存到本机」才会生成文件，直接关闭不会触发任何下载。'));
  if(o.hint)ft.appendChild(h('div','note',o.hint));
 root.classList.add('on');
 document.body.style.overflow='hidden';
 root.querySelector('.sheet').scrollTop=0;
}
document.addEventListener('keydown',function(e){
 if(e.key==='Escape'&&xState)closeExport();
});
function buildPrintHtml(){
 const cssP='body{font:15px/1.8 -apple-system,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif;max-width:840px;margin:0 auto;padding:34px;color:#1b2027}'
  +'h1{font-size:25px;border-bottom:2px solid #dbe3ee;padding-bottom:12px}'
  +'h2{font-size:20px;margin-top:34px;background:#f2f5f9;padding:9px 11px;border-radius:9px;break-after:avoid}'
  +'h3{font-size:18px;margin-top:28px;border-left:4px solid #27AE60;padding-left:10px;break-after:avoid}'
  +'h4{font-size:14px;margin:20px 0 6px;color:#27AE60;letter-spacing:.04em;break-after:avoid}'
  +'p{margin:9px 0}ul{margin:9px 0 9px 22px;padding:0}'
  +'pre{background:#f5f7fa;border-radius:9px;padding:11px;overflow-x:auto;font-size:12.5px;white-space:pre-wrap;word-break:break-all}'
  +'code{background:#eef2f7;padding:1px 5px;border-radius:5px;font-size:.9em}'
  +'table{border-collapse:collapse;width:100%;font-size:13.5px;margin:10px 0}'
  +'th,td{border:1px solid #dde3ea;padding:8px 10px;text-align:left;vertical-align:top}'
  +'th{background:#f3f6fa}'
  +'blockquote{margin:10px 0;padding:9px 13px;background:#f2f5f9;border-left:4px solid #b9cbe4;color:#44505f;border-radius:0 9px 9px 0}'
  +'@media print{body{padding:0;max-width:none}h2{break-before:page}}';
 return '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">'
  +'<meta name="viewport" content="width=device-width, initial-scale=1">'
  +'<title>桌面运维学习手册（打印版）</title><style>'+cssP+'</style></head><body>'
  +mdToHtml(manualMd())+'</body></html>';
}
function buildBackup(){
 return JSON.stringify({app:'desktop-it-ops-mentor',ver:4,kind:'backup',exportedAt:new Date().toISOString(),
  counts:{marks:(S.marks||[]).length,cases:(S.cases||[]).length,gloss:(S.gloss||[]).length,learned:Object.keys(S.learn||{}).length},
  state:{route:S.route,idx:S.idx,day:S.day,mode:S.mode,status:S.status,learn:S.learn,cases:S.cases,gloss:S.gloss,marks:S.marks}},null,1);
}
function manualMd(){
 const out=['# '+coMeta(curCourse()).title+' · 学习手册　'+CH.length+' 章 · 概念 / 实操 / 指南 / 互动题','',
  '> 导出时间：'+new Date().toLocaleString('zh-CN')+'　来源：'+coMeta(curCourse()).title,'',
  '## 目录'];
 CH.forEach(function(c){out.push('- **'+c.id+'** '+c.t+'　—　'+c.s);});
 out.push('','---');
 MODS.forEach(function(m){
  out.push('','## '+m.n+'　（'+m.tag+'）');
  CH.filter(function(c){return c.m===m.k;}).forEach(function(c){
   const d=LONG[c.id]||synth(c);
   out.push('','### '+c.id+'　'+c.t,'','> 本节解决：'+c.s);
   if(c.plain&&c.plain.length){
    out.push('','**双轨对照**','| 专业术语 | 大白话 |','| --- | --- |');
    c.plain.forEach(function(p){out.push('| '+p[0]+' | '+p[1]+' |');});
   }
   out.push('','#### 一、概念');
   if(d.title)out.push('','**'+d.title+'**');
   d.concept.forEach(function(row){
    if(row.p)out.push('',row.p);
    if(row.ul)row.ul.forEach(function(x){out.push('- '+x);});
    if(row.tbl){
     out.push('','| '+row.tbl.head.join(' | ')+' |','| '+row.tbl.head.map(function(){return '---';}).join(' | ')+' |');
     row.tbl.rows.forEach(function(r){out.push('| '+r.join(' | ')+' |');});
    }
   });
   out.push('','#### 二、实操');
   d.practice.forEach(function(st,i){
    out.push('','**'+(i+1)+'. '+st.t+'**');
    if(st.d)out.push('',st.d);
    out.push('','```','  '+st.cmd,'```');
    if(st.out)out.push('','预期输出','','```','  '+st.out,'```');
    if(st.read)out.push('','怎么读：'+st.read);
   });
   out.push('','#### 三、指南');
   d.guide.forEach(function(g,i){out.push('','**'+(i+1)+'. 错误做法：'+g.w+'**','- 后果：'+g.c,'- 正确：'+g.r);});
   out.push('','#### 四、互动题','',d.quiz.q,'');
   if(d.quiz.hint)out.push('> 提示：'+d.quiz.hint,'');
   out.push('参考答案：'+d.quiz.a,'','**过关标准**：'+c.gate);
  });
 });
 return out.join('\n');
}
function casesText(){
 const out=['故障案例库（五栏）　导出 '+new Date().toLocaleString('zh-CN'),''];
 (S.cases||[]).forEach(function(c){
  out.push('编号：'+c.no,'现象：'+c.sym,'排查路径：'+c.path,'根因：'+c.cause,'结论（下次第一步查什么）：'+c.lesson,'关联章节：'+c.ch,'','────────','');
 });
 return out.join('\n');
}
function csvOf(rows){
 const esc=function(v){return '"'+String(v==null?'':v).replace(/"/g,'""')+'"';};
 return rows.map(function(r){return r.map(esc).join(',');}).join('\r\n');
}
function casesCsv(){
 const rows=[['编号','现象','排查路径','根因','结论（下次第一步）','关联章节']];
 (S.cases||[]).forEach(function(c){rows.push([c.no,c.sym,String(c.path||'').replace(/\n/g,' / '),c.cause,c.lesson,c.ch]);});
 return csvOf(rows);
}
function glossCsv(){
 const rows=[['术语','全称','大白话','来源']];
 (S.gloss||[]).forEach(function(g){rows.push([g.term,g.full,g.say,'自填']);});
 buildBaseGloss().forEach(function(g){rows.push([g.term,g.full,g.say,'章节自动']);});
 return csvOf(rows);
}
function mdInline(s){
 return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
  .replace(/\*\*([^*]+)\*\*/g,'<b>$1</b>').replace(/`([^`]+)`/g,'<code>$1</code>');
}
function mdToHtml(md){
 const lines=md.split('\n');let out='',inCode=false,buf=[],inTbl=false,tb=[],inUl=false;
 const closeUl=function(){if(inUl){out+='</ul>';inUl=false;}};
 const closeTbl=function(){
  if(!inTbl)return;
  let h='<table>';
  tb.forEach(function(ln,i){
   if(/^[\s|:-]+$/.test(ln))return;
   const cells=ln.split('|').slice(1,-1).map(function(x){return x.trim();});
   const tag=i===0?'th':'td';
   h+='<tr>'+cells.map(function(c){return '<'+tag+'>'+mdInline(c)+'</'+tag+'>';}).join('')+'</tr>';
  });
  out+=h+'</table>';inTbl=false;tb=[];
 };
 lines.forEach(function(raw){
  const line=raw.replace(/\s+$/,'');
  if(/^```/.test(line.trim())){
   if(inCode){out+='<pre>'+buf.join('\n').replace(/&/g,'&amp;').replace(/</g,'&lt;')+'</pre>';buf=[];inCode=false;}
   else{closeTbl();closeUl();inCode=true;}
   return;
  }
  if(inCode){buf.push(raw);return;}
  if(/^\|/.test(line)){closeUl();inTbl=true;tb.push(line);return;}
  closeTbl();
  if(/^#### /.test(line)){closeUl();out+='<h4>'+mdInline(line.slice(6))+'</h4>';return;}
  if(/^### /.test(line)){closeUl();out+='<h3>'+mdInline(line.slice(4))+'</h3>';return;}
  if(/^## /.test(line)){closeUl();out+='<h2>'+mdInline(line.slice(3))+'</h2>';return;}
  if(/^# /.test(line)){closeUl();out+='<h1>'+mdInline(line.slice(2))+'</h1>';return;}
  if(/^- /.test(line)){if(!inUl){out+='<ul>';inUl=true;}out+='<li>'+mdInline(line.slice(2))+'</li>';return;}
  if(/^> /.test(line)){closeUl();out+='<blockquote>'+mdInline(line.slice(2))+'</blockquote>';return;}
  if(!line){closeUl();return;}
  closeUl();out+='<p>'+mdInline(line)+'</p>';
 });
 closeTbl();closeUl();
 if(inCode)out+='<pre>'+buf.join('\n')+'</pre>';
 return out;
}
let pendingImp=null;
function importCounts(st){
 return {marks:(st.marks||[]).length,cases:(st.cases||[]).length,gloss:(st.gloss||[]).length,learned:Object.keys(st.learn||{}).length,pass:Object.keys(st.status||{}).filter(function(k){return st.status[k]==='green';}).length};
}
function handleImportFile(file){
 const msg=document.getElementById('ioMsg');
 msg.textContent='正在读取文件…';
 const rd=new FileReader();
 rd.onload=function(){
  let obj=null;
  try{obj=JSON.parse(String(rd.result||'').replace(/^\uFEFF/,''));}catch(e){obj=null;}
  if(!obj||!obj.state||!(obj.app==='desktop-it-ops-mentor'||obj.kind==='backup')){
   pendingImp=null;renderImportPreview();
   msg.textContent='解析失败：这不是本应用导出的 JSON 存档。请确认选择的是「全量备份」导出的 .json 文件（不是手册 .md 或案例 .csv）。';
   return;
  }
  pendingImp={obj:obj,mode:'merge'};
  renderImportPreview();
  msg.textContent='已读取存档，请选择合并或覆盖后确认导入。';
 };
 rd.onerror=function(){msg.textContent='读取文件失败，请重试或换一个文件。';};
 rd.readAsText(file);
}
function renderImportPreview(){
 const box=document.getElementById('impPreview');if(!box)return;
 box.textContent='';
 if(!pendingImp){box.appendChild(h('p','muted','还没有待导入的存档。导入只影响你自己的学习数据，不会改动内置的 30 章内容。'));return;}
 const o=pendingImp.obj,st=o.state||{},ic=importCounts(st);
 box.appendChild(h('h3',null,'待导入存档 · '+new Date(o.exportedAt||Date.now()).toLocaleString('zh-CN')+'　（v'+(o.ver||3)+'）'));
 const ul=h('ul','pl');
 [[(S.marks||[]).length,ic.marks,'划线'],[(S.cases||[]).length,ic.cases,'案例'],[(S.gloss||[]).length,ic.gloss,'自填术语'],[Object.keys(S.learn||{}).length,ic.learned,'已学章节'],[Object.keys(S.status||{}).filter(function(k){return S.status[k]==='green';}).length,ic.pass,'已过关']]
  .forEach(function(r){ul.appendChild(h('li',null,r[2]+'：存档 '+r[1]+'　当前 '+r[0]));});
 box.appendChild(ul);
 const btns=h('div','btns');
 const m1=h('button','btn sm'+(pendingImp.mode==='merge'?' pri':''),'合并（保留两边，重复跳过）');
 const m2=h('button','btn sm'+(pendingImp.mode==='cover'?' pri':''),'覆盖（以存档为准）');
 m1.onclick=function(){pendingImp.mode='merge';renderImportPreview();};
 m2.onclick=function(){pendingImp.mode='cover';renderImportPreview();};
 const go=h('button','btn pri','确认导入');go.onclick=applyImport;
 const cancel=h('button','btn ghost','取消');cancel.onclick=function(){pendingImp=null;renderImportPreview();document.getElementById('ioMsg').textContent='已取消导入。';};
 btns.appendChild(m1);btns.appendChild(m2);btns.appendChild(h('span','spacer'));btns.appendChild(cancel);btns.appendChild(go);
 box.appendChild(btns);
}
function applyImport(){
 if(!pendingImp){toast('请先选择存档文件');return;}
 const st=pendingImp.obj.state||{},msg=document.getElementById('ioMsg');
 if(pendingImp.mode==='cover'){
  S.marks=st.marks||[];S.cases=st.cases||[];S.gloss=st.gloss||[];S.learn=st.learn||{};S.status=st.status||{};
  if(st.route&&ROUTES[st.route])S.route=st.route;
  if(typeof st.idx==='number')S.idx=st.idx;
  if(st.mode)S.mode=st.mode;
  if(typeof st.day==='number')S.day=st.day;
  migrateMarks();save();closeEditor();rerenderMarks();render();
  msg.textContent='覆盖导入完成：划线 '+S.marks.length+' 处、案例 '+S.cases.length+' 条、已学章节 '+Object.keys(S.learn).length+' 章。';
  pendingImp=null;renderImportPreview();popCheck('导入完成');return;
 }
 migrateMarks();
 const seen={};(S.marks||[]).forEach(function(m){seen[fingerprint(m)]=1;});
 let aM=0,aC=0,aG=0;
 (st.marks||[]).forEach(function(m){
  const k=fingerprint(m);if(seen[k])return;seen[k]=1;
  S.marks=S.marks||[];S.marks.push(m);aM++;
 });
 const cn={};(S.cases||[]).forEach(function(c){cn[c.no]=1;});
 (st.cases||[]).forEach(function(c){
  if(!c||!c.no||cn[c.no])return;S.cases=S.cases||[];S.cases.push(c);cn[c.no]=1;aC++;
 });
 const gt={};(S.gloss||[]).forEach(function(g){gt[String(g.term||'').toLowerCase()]=1;});
 (st.gloss||[]).forEach(function(g){
  if(!g||!g.term||gt[String(g.term).toLowerCase()])return;S.gloss=S.gloss||[];S.gloss.push(g);gt[String(g.term).toLowerCase()]=1;aG++;
 });
 Object.keys(st.status||{}).forEach(function(k){if(!S.status[k])S.status[k]=st.status[k];});
 Object.keys(st.learn||{}).forEach(function(k){if(!S.learn[k])S.learn[k]=st.learn[k];});
 if(typeof st.day==='number')S.day=Math.max(S.day||1,st.day);
 save();rerenderMarks();render();
 msg.textContent='合并导入完成：新增划线 '+aM+' 处、案例 '+aC+' 条、术语 '+aG+' 条（重复项已自动跳过，现有记录全部保留）。';
 pendingImp=null;renderImportPreview();popCheck('导入完成');
}
function renderIo(){
 const st=markStats();
 const set=function(id,v){const e=document.getElementById(id);if(e)e.textContent=v;};
 set('ioMarks',st.total+' 处');set('ioNotes',st.notes+' 条');set('ioCases',(S.cases||[]).length+' 条');
 set('ioGloss',(S.gloss||[]).length+' 条');set('ioDone',Object.keys(S.status||{}).filter(function(k){return S.status[k]==='green';}).length+' 章');
 set('ioKnow',String(KNOW_TOTAL));
 const file=document.getElementById('impFile');
 if(file&&!file._b){file._b=1;file.onchange=function(){const f=file.files&&file.files[0];if(f)handleImportFile(f);file.value='';};}
 const drop=document.getElementById('ioDrop');
 if(drop&&!drop._b){
  drop._b=1;
  drop.addEventListener('dragover',function(e){e.preventDefault();drop.classList.add('hot');});
  drop.addEventListener('dragleave',function(){drop.classList.remove('hot');});
  drop.addEventListener('drop',function(e){
   e.preventDefault();drop.classList.remove('hot');
   const f=e.dataTransfer&&e.dataTransfer.files&&e.dataTransfer.files[0];
   if(f)handleImportFile(f);
   else document.getElementById('ioMsg').textContent='没读到拖进来的文件，请点「选择存档文件」。';
  });
 }
 renderImportPreview();
}

/* 四段结构：概念 / 实操 / 指南 / 互动题 */
function hl(el,ch,blk){el.dataset.ch=ch;el.dataset.blk=blk;el.classList.add('blk');return el;}
function buildTbl(t){
 const wrap=h('div','scroller');const tb=h('table','plain');
 const th=h('thead');const tr=h('tr');t.head.forEach(x=>tr.appendChild(h('th',null,x)));th.appendChild(tr);tb.appendChild(th);
 const body=h('tbody');
 t.rows.forEach(row=>{const tr2=h('tr');row.forEach(cell=>tr2.appendChild(h('td',null,cell)));body.appendChild(tr2);});
 tb.appendChild(body);wrap.appendChild(tb);return wrap;
}
function renderLong(cb,c){
 const L=LONG[c.id];
 const d=L||synth(c);
 const chId=c.id;
 const mk=(cls)=>h('div',cls);
 /* ① 概念 */
 const s1=h('div','lsec');
 const t1=h('div','lt');t1.appendChild(h('span','no','概'));t1.appendChild(h3s('概念','讲透它到底解决什么问题'));s1.appendChild(t1);
 if(L&&L.title){const ti=h('p','pp');ti.appendChild(document.createTextNode(L.title));ti.style.fontWeight='var(--fw-h)';s1.appendChild(hl(ti,chId,'T'));}
 d.concept.forEach((row,i)=>{
  const w=h('div','cseg'+(i>=2?' deep':''));
  if(row.p){const p=h('p','pp');p.textContent=row.p;w.appendChild(hl(p,chId,'C'+i));}
  if(row.ul){const ul=h('ul','pl');row.ul.forEach(x=>{const li=h('li');li.textContent=x;ul.appendChild(li);});w.appendChild(ul);}
  if(row.tbl){w.appendChild(buildTbl(row.tbl));}
  s1.appendChild(w);
 });
 cb.appendChild(s1);
 /* ② 实操 */
 const s2=h('div','lsec');
 const t2=h('div','lt');t2.appendChild(h('span','no','操'));t2.appendChild(h3s('实操','命令、图形界面路径与预期输出'));s2.appendChild(t2);
 d.practice.forEach((st,i)=>{
  const w=h('div','step');
  const hd=h('div','st');hd.appendChild(h('em',null,String(i+1)));
  const bt=h('span',null,st.t);hd.appendChild(bt);w.appendChild(hd);
  if(st.d){const p=h('p');p.textContent=st.d;w.appendChild(hl(p,chId,'P'+i+'d'));}
  const code=h('pre',null,st.cmd);w.appendChild(code);
  if(st.out){const lab=h('div','lab det','输出样例');lab.style.fontSize='12px';lab.style.color='var(--sub)';lab.style.marginTop='8px';w.appendChild(lab);w.appendChild(h('pre','det',st.out));}
  if(st.read){const rd=h('div','rd det','怎么读：'+st.read);w.appendChild(hl(rd,chId,'P'+i+'r'));}
  const bb=h('div','btns');
  const cp=h('button','btn sm','复制这条命令');cp.onclick=()=>copyText(st.cmd,'命令已复制');bb.appendChild(cp);
  const cp2=h('button','btn sm','复制本节要点');
  cp2.onclick=()=>copyText(['■ '+st.t,st.d||'','$ '+st.cmd,st.out?('→ '+st.out):'',st.read?('读法：'+st.read):''].filter(Boolean).join('\n'),'要点已复制');
  bb.appendChild(cp2);w.appendChild(bb);
  s2.appendChild(w);
 });
 cb.appendChild(s2);
 /* ③ 指南 */
 const s3=h('div','lsec');
 const t3=h('div','lt');t3.appendChild(h('span','no','指'));t3.appendChild(h3s('指南','避坑与运维习惯'));s3.appendChild(t3);
 d.guide.forEach((g,i)=>{
  const row=h('div','gline');row.appendChild(h('i',null,String(i+1)));
  const box=h('div');
  box.appendChild(hl(h('p','gw','错误做法：'+g.w),chId,'G'+i+'w'));
  box.appendChild(hl(h('p','gc','后果：'+g.c),chId,'G'+i+'c'));
  box.appendChild(hl(h('p','gr','正确：'+g.r),chId,'G'+i+'r'));
  row.appendChild(box);s3.appendChild(row);
 });
 cb.appendChild(s3);
 /* ④ 互动题 */
 const s4=h('div','lsec');
 const t4=h('div','lt');t4.appendChild(h('span','no','题'));t4.appendChild(h3s('互动题','答完再看解析'));s4.appendChild(t4);
 const qe=h('div','qtext',d.quiz.q);s4.appendChild(hl(qe,chId,'QZ'));
 if(d.quiz.hint)s4.appendChild(h('p','muted','提示：'+d.quiz.hint));
 const an=h('div','ans','参考答案：'+d.quiz.a);
 const bb2=h('div','btns');
 const sb=h('button','btn sm','看参考答案');sb.onclick=()=>an.classList.toggle('on');bb2.appendChild(sb);
 const cs=h('button','btn sm','这题我答对了 · 标记 🟢 过关');
 cs.onclick=()=>{if(!S.learn[chId])S.learn[chId]={at:today(),done:[]};S.status[chId]='green';save();toast('已标记过关并加入复习队列');render();};
 bb2.appendChild(cs);
 s4.appendChild(bb2);s4.appendChild(an);
 cb.appendChild(s4);
 /* 模式切换 */
 const mr=h('div','modeRow');
 const b1=h('button','btn sm'+(S.mode==='long'?' on':''),'完整讲义');
 const b2=h('button','btn sm'+(S.mode==='lite'?' on':''),'速览（折叠细节）');
 b1.onclick=()=>{S.mode='long';save();render();};
 b2.onclick=()=>{S.mode='lite';save();render();};
 const b3=h('button','btn sm','我的划线 '+((S.marks||[]).length));
 b3.onclick=()=>{nav('anno');};
 const b4=h('button','btn sm','数据');
 b4.onclick=()=>{nav('io');};
 mr.appendChild(b1);mr.appendChild(b2);
 mr.appendChild(h('span','muted','选中文本即可划线（可跨段落）；同一段落能叠多条不同颜色，删除只删自己那条。点划线写多行备注、改线型、打标签。'));
 mr.appendChild(b3);mr.appendChild(b4);
 cb.appendChild(mr);
 cb.classList.add('card-'+S.mode);
 if(S.mode==='long')cb.classList.add('longMode');else cb.classList.remove('longMode');
}
function h3(t){const e=document.createElement('h3');e.textContent=t;return e;}
function h3s(main,sub){const e=document.createElement('h3');e.appendChild(document.createTextNode(main));if(sub){const s=document.createElement('span');s.className='lsub';s.textContent=sub;e.appendChild(s);}return e;}
function synth(c){
 const dp=DEEP[c.id]||{deep:[],out:[],flow:[]};
 const concept=[];
 concept.push({p:'本章要解决的问题：'+c.s+'。先用下面的双轨对照把术语钉住，再看后面的细节条目。'});
 concept.push({tbl:{head:['专业术语','大白话（面试和文档里怎么翻译给别人）'],rows:c.plain.map(x=>[x[0],x[1]])}});
 (dp.deep||[]).forEach(r=>concept.push({p:r[0]+'。'+r[1]}));
 const practice=(c.cmds||[]).map(x=>({t:x.n,cmd:x.c,d:'档位：'+(x.lv==='w'?'会改动本机配置，先征得同意或只在实验环境做':x.lv==='s'?'需要管理员或服务器权限，仅限实验环境，生产走变更审批':'只读查看，可在自己办公机上运行'),read:x.lv==='r'?'只读命令，可在自己办公机上跑；结论写进工单。':x.lv==='w'?'会改动本机配置：先征得同意，或在实验虚拟机里做。':'需要管理员或服务器权限：只在实验环境跑，生产走变更审批。'}));
 (dp.out||[]).forEach(o=>practice.push({t:'看输出样例：'+o[0],cmd:o[0],out:o[1],d:'先看清正常输出长什么样，现场才有对照物。'}));
 (dp.flow||[]).forEach(f=>practice.push({t:f[0],cmd:'顺序：'+String(f[1]).split('→').map(x=>x.trim()).filter(Boolean).join(' → '),d:'把这条链路当成操作清单，逐项做完再进下一项。'}));
 return {title:c.t,concept,practice:practice.length?practice:[{t:'本章无命令行操作',cmd:'（图形界面为主）参考指南条目的操作路径',d:'用图形界面完成，重点是流程与留痕。'}],guide:c.pits||[],quiz:c.quiz};
}

