/* Original extension layer v19; preserved in source order. */

/* =====================================================================
   v19 · 图片编辑器 / 视频画面截取 / 导出页 / 工具页风格 / 顶部装饰 / 标签适配 / 互斥播放
   ===================================================================== */
(function(){
function P(d,f){return '<svg viewBox="0 0 24 24" fill="'+(f||'none')+'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+d+'</svg>';}
ICONS.crop=P('<path d="M6 3v13a2 2 0 0 0 2 2h13"/><path d="M3 6h13a2 2 0 0 1 2 2v13"/>');
ICONS.rotateCw=P('<path d="M20 12a8 8 0 1 1-2.7-6"/><path d="M20 4v5h-5"/>');
ICONS.zoomIn=P('<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2M11 8v6M8 11h6"/>');
ICONS.zoomOut=P('<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2M8 11h6"/>');
ICONS.deco=P('<path d="M12 3l1.8 4.6L18.5 9l-4.7 1.4L12 15l-1.8-4.6L5.5 9l4.7-1.4z"/><path d="M19 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z"/>');
ICONS.fullscreenX=P('<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>');

/* ---------- 1. 图片编辑器（裁剪 / 拖动 / 双指缩放 / 旋转） ---------- */
window.openCropModal=function(o){
 var old=document.querySelector('.crop-mask');if(old)old.remove();
 var iw=o.iw||1,ih=o.ih||1,img=o.img;
 var mask=h('div','crop-mask'),top=h('div','cm-top'),stage=h('div','cm-stage'),bar=h('div','cm-bar');
 var bx=h('button','cm-ic');bx.type='button';bx.innerHTML=ic('close');bx.setAttribute('aria-label','取消');
 var ok=h('button','cm-done');ok.type='button';ok.innerHTML=ic('checkTick')+'<span>完成</span>';
 top.appendChild(bx);top.appendChild(h('b',null,'调整图片'));top.appendChild(ok);
 var im=document.createElement('img');im.alt='';im.draggable=false;im.src=img;
 var frame=h('div','cm-frame');['a','b','c','d'].forEach(function(k){frame.appendChild(h('i','g '+k));});
 ['v1','v2','h1','h2'].forEach(function(k){frame.appendChild(h('i','l '+k));});
 stage.appendChild(im);stage.appendChild(frame);
 function tbtn(icon,label,fn){var b=h('button','cm-t');b.type='button';b.innerHTML=ic(icon)+'<span>'+label+'</span>';b.onclick=fn;return b;}
 var fw=0,fh=0,fx=0,fy=0,s=1,s0=1,tx=0,ty=0;
 function layout(){
  var W=stage.clientWidth,H=stage.clientHeight,maxW=Math.min(W-32,380),maxH=Math.min(H-28,440),r=o.ratio||1;
  if(r>=maxW/maxH){fw=maxW;fh=fw/r;}else{fh=maxH;fw=fh*r;}
  fx=(W-fw)/2;fy=(H-fh)/2;
  frame.style.left=fx+'px';frame.style.top=fy+'px';frame.style.width=fw+'px';frame.style.height=fh+'px';
  s0=Math.max(fw/iw,fh/ih);
 }
 function clamp(){s=Math.max(s0,Math.min(s0*6,s));tx=Math.min(0,Math.max(fw-iw*s,tx));ty=Math.min(0,Math.max(fh-ih*s,ty));}
 function paint(){im.style.width=iw*s+'px';im.style.height=ih*s+'px';im.style.left=(fx+tx)+'px';im.style.top=(fy+ty)+'px';}
 function center(){s=s0;tx=(fw-iw*s)/2;ty=(fh-ih*s)/2;clamp();paint();}
 function zoomAt(px,py,ns){ns=Math.max(s0,Math.min(s0*6,ns));var k=ns/s;tx=px-(px-tx)*k;ty=py-(py-ty)*k;s=ns;clamp();paint();}
 function fromPos(){
  var p=o.pos||{x:50,y:50,z:1};s=s0*Math.max(1,p.z||1);
  tx=(fw-iw*s)*(p.x==null?50:p.x)/100;ty=(fh-ih*s)*(p.y==null?50:p.y)/100;clamp();paint();
 }
 function toPos(){
  var dx=fw-iw*s,dy=fh-ih*s;
  return {x:Math.abs(dx)>0.5?Math.max(0,Math.min(100,tx*100/dx)):50,y:Math.abs(dy)>0.5?Math.max(0,Math.min(100,ty*100/dy)):50,z:s/s0};
 }
 /* 手势：单指拖动、双指缩放、滚轮缩放 */
 var ptrs={},last=null;
 function pts(){return Object.keys(ptrs).map(function(k){return ptrs[k];});}
 stage.addEventListener('pointerdown',function(e){ptrs[e.pointerId]={x:e.clientX,y:e.clientY};try{stage.setPointerCapture(e.pointerId);}catch(x){}last=null;});
 stage.addEventListener('pointermove',function(e){
  if(!ptrs[e.pointerId])return;
  var p=ptrs[e.pointerId],list=pts();
  if(list.length===1){tx+=e.clientX-p.x;ty+=e.clientY-p.y;p.x=e.clientX;p.y=e.clientY;clamp();paint();}
  else if(list.length>=2){
   var a=list[0],b=list[1],d0=Math.hypot(a.x-b.x,a.y-b.y),cx0=(a.x+b.x)/2,cy0=(a.y+b.y)/2;
   p.x=e.clientX;p.y=e.clientY;
   var a2=list[0],b2=list[1],d1=Math.hypot(a2.x-b2.x,a2.y-b2.y),cx1=(a2.x+b2.x)/2,cy1=(a2.y+b2.y)/2;
   var sr=stage.getBoundingClientRect();
   tx+=cx1-cx0;ty+=cy1-cy0;
   if(d0>8)zoomAt(cx1-sr.left-fx,cy1-sr.top-fy,s*d1/d0);else{clamp();paint();}
  }
 });
 function up(e){delete ptrs[e.pointerId];}
 stage.addEventListener('pointerup',up);stage.addEventListener('pointercancel',up);
 stage.addEventListener('wheel',function(e){e.preventDefault();var sr=stage.getBoundingClientRect();zoomAt(e.clientX-sr.left-fx,e.clientY-sr.top-fy,s*(e.deltaY<0?1.12:1/1.12));},{passive:false});
 bar.appendChild(tbtn('rotateCw','旋转',function(){rotate();}));
 bar.appendChild(tbtn('zoomOut','缩小',function(){zoomAt(fw/2,fh/2,s/1.2);}));
 bar.appendChild(tbtn('zoomIn','放大',function(){zoomAt(fw/2,fh/2,s*1.2);}));
 bar.appendChild(tbtn('resetArrow','重置',function(){center();}));
 function rotate(){
  var src=new Image();
  src.onload=function(){
   var cv=document.createElement('canvas');cv.width=src.height;cv.height=src.width;
   var cx=cv.getContext('2d');cx.translate(src.height,0);cx.rotate(Math.PI/2);cx.drawImage(src,0,0);
   img=cv.toDataURL('image/jpeg',0.88);var t=iw;iw=ih;ih=t;
   im.onload=function(){layout();center();};im.src=img;
  };
  src.src=img;
 }
 function close(){document.removeEventListener('keydown',kd);mask.classList.remove('on');setTimeout(function(){mask.remove();},180);}
 function kd(e){if(e.key==='Escape')close();}
 bx.onclick=close;
 ok.onclick=function(){var pos=toPos();close();o.onDone({img:img,iw:iw,ih:ih,pos:pos});};
 document.addEventListener('keydown',kd);
 mask.appendChild(top);mask.appendChild(stage);
 var hint=h('div','cm-hint','拖动图片 · 双指缩放');mask.appendChild(hint);mask.appendChild(bar);
 document.body.appendChild(mask);
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');layout();fromPos();});});
};

/* ---------- 2. 视频画面截取（用作课程封面） ---------- */
window.openFrameModal=function(co,cb){
 var key=function(c){return co.id+':'+c.id;};
 var chs=packOf(co).ch.filter(function(c){var r=MEDIA[key(c)];return c.media&&r&&r.file;});
 if(!chs.length){toast('请先连接视频文件');return;}
 var old=document.querySelector('.crop-mask');if(old)old.remove();
 var mask=h('div','crop-mask fm'),top=h('div','cm-top'),stage=h('div','cm-stage fm-stage');
 var bx=h('button','cm-ic');bx.type='button';bx.innerHTML=ic('close');
 var ok=h('button','cm-done');ok.type='button';ok.innerHTML=ic('checkTick')+'<span>完成</span>';
 top.appendChild(bx);top.appendChild(h('b',null,'截取封面'));top.appendChild(ok);
 var v=document.createElement('video');v.muted=true;v.setAttribute('playsinline','');v.preload='auto';stage.appendChild(v);
 var url='',curC=chs[0],t=1;
 var ctl=h('div','fm-ctl'),rg=document.createElement('input');rg.type='range';rg.min=0;rg.max=60;rg.step=0.1;rg.value=1;
 var tl=h('span','fm-t','0:01');ctl.appendChild(rg);ctl.appendChild(tl);
 var list=h('div','fm-list');
 function load(c){
  curC=c;try{if(url)URL.revokeObjectURL(url);}catch(e){}
  url=URL.createObjectURL(MEDIA[key(c)].file);v.src=url;
  v.onloadedmetadata=function(){rg.max=Math.max(1,Math.floor((v.duration||60)*10)/10);t=Math.min(t,v.duration||t);rg.value=t;try{v.currentTime=t;}catch(e){}};
  list.querySelectorAll('.fm-chip').forEach(function(x){x.classList.toggle('on',x.dataset.id===c.id);});
 }
 chs.forEach(function(c){var b=h('button','fm-chip',c.t);b.type='button';b.dataset.id=c.id;b.onclick=function(){t=1;load(c);};list.appendChild(b);});
 var tm=0;
 rg.oninput=function(){t=Number(rg.value);tl.textContent=Math.floor(t/60)+':'+('0'+Math.floor(t%60)).slice(-2);clearTimeout(tm);tm=setTimeout(function(){try{v.currentTime=t;}catch(e){}},50);};
 function close(){try{if(url)URL.revokeObjectURL(url);}catch(e){}mask.classList.remove('on');setTimeout(function(){mask.remove();},180);}
 bx.onclick=close;
 ok.onclick=function(){
  try{
   var W=480,r=(v.videoWidth&&v.videoHeight)?v.videoWidth/v.videoHeight:16/9,hh=Math.round(W/r);
   var cv=document.createElement('canvas');cv.width=W;cv.height=hh;cv.getContext('2d').drawImage(v,0,0,W,hh);
   var d=cv.toDataURL('image/jpeg',0.82);close();cb(d);
  }catch(e){toast('截取失败，请换一帧');}
 };
 mask.appendChild(top);mask.appendChild(stage);mask.appendChild(ctl);mask.appendChild(list);
 document.body.appendChild(mask);load(chs[0]);
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');});});
};

/* ---------- 3. 快捷入口文字：不省略，必要时缩小字号 ---------- */
function fitScLabels(){}
window.addEventListener('resize',function(){requestAnimationFrame(fitScLabels);});

/* ---------- 4. 导出详情页：设置页风格 ---------- */
renderExportPage=function(box){
 var meta=IO_ENTRIES.find(function(e){return e.id===ioDetail;});if(!meta){ioDetail=null;renderIo();return;}
 var wrap=h('div','io-page stagger');box.appendChild(wrap);
 var head=h('div','io-card static'),hi=h('span','ico');hi.innerHTML=ic(meta.icon);head.appendChild(hi);
 var tx=h('span','tx');tx.appendChild(h('b',null,meta.title));tx.appendChild(h('small',null,coMeta(curCourse()).title));head.appendChild(tx);wrap.appendChild(head);
 var s1=stSection(ic('layers'),'格式'),opts=h('div','st-opts three');
 var FM=[['json','database','JSON'],['md','book','MD'],['csv','chart','CSV']];
 FM.forEach(function(k){
  var f=FMT_META[k[0]],b=stOption(k[2],ioFormat===k[0],function(){
   ioFormat=k[0];opts.querySelectorAll('.st-opt').forEach(function(x){x.classList.remove('on');});b.classList.add('on');upd();
  },ic(k[1]),f.ext);
  opts.appendChild(b);
 });
 s1.appendChild(opts);wrap.appendChild(s1);
 var go=h('button','btn pri ex-go');go.type='button';wrap.appendChild(go);
 var s2=stSection(ic('chart'),'包含'),grid=h('div','ex-grid');s2.appendChild(grid);wrap.appendChild(s2);
 var gw=h('div','exp-guide');wrap.appendChild(gw);
 var gscope=(ioDetail==='all'||ioDetail==='course')?'course':ioDetail;
 function upd(){
  grid.textContent='';
  archiveSummary(makeArchive(ioDetail)).forEach(function(r){
   var c=h('div','ex-stat'+(String(r[1]).length>6?' wide':''));c.appendChild(h('b',null,String(r[1])));c.appendChild(h('small',null,String(r[0])));grid.appendChild(c);
  });
  if(!grid.childElementCount)grid.appendChild(h('div','ex-empty','暂无数据'));
  go.innerHTML=ic('share')+'<span>预览并导出 '+FMT_META[ioFormat].name+'</span>';
  gw.textContent='';
  gw.appendChild(guideFold({fmt:ioFormat,scope:gscope,variant:'full',fixedFmt:true,scopes:false},'格式说明'));
 }
 upd();
 go.onclick=function(){
  var d=archiveText(ioDetail,ioFormat),F=FMT_META[ioFormat],name=coMeta(curCourse()).title+'-'+meta.name+'-'+today()+'.'+d.ext;
  openExport({kind:ioFormat,title:meta.title+' · '+F.name,filename:name,mime:d.mime,text:d.text,
   hint:'文件格式：'+F.ext+' · UTF-8'+(ioFormat==='csv'?'（带 BOM）':'')+'。可在「数据 → 导入档案」重新导入。'});
 };
};

/* ---------- 5. 工具页：标题图标 + 精简说明，随主题变化 ---------- */
var TP={gloss:'aA',case:'folder',play:'wrench',prog:'chart',review:'refresh',know:'book'};
function styleToolPage(){
 var root=document.getElementById('v-'+view);
 document.querySelectorAll('.tp-page').forEach(function(x){if(x!==root)x.classList.remove('tp-page');});
 if(!root||!TP[view])return;
 root.classList.add('tp-page');
 root.querySelectorAll('.card').forEach(function(p,i){
  var hd=p.querySelector(':scope>.hd');if(!hd)return;
  if(!hd.querySelector('.tp-ic')){
   var c=h('span','tp-ic');c.innerHTML=ic(i===0?TP[view]:'layers');hd.insertBefore(c,hd.firstChild);hd.classList.add('tp-hd');
  }
  var nx=hd.nextElementSibling;
  if(nx&&nx.tagName==='P'&&(nx.textContent||'').length>28)nx.classList.add('tp-hide');
 });
}

/* ---------- 6. 顶部装饰 ---------- */
var DECOS=[['lines','竖线','静态'],['hair','细光','静态'],['grid','网格','静态'],['dots','点阵','静态'],['contour','等高线','静态'],['ink','水墨','静态'],['aurora','极光','动态'],['stars','星空','动态'],['ripple','涟漪','动态'],['scan','扫描','动态'],['beam','光束','动态'],['none','无','']];
function curDeco(){return (S.ui&&S.ui.deco)||(document.documentElement.getAttribute('data-style')==='planet'?'lines':'none');}
function applyDeco(){document.documentElement.setAttribute('data-deco',curDeco());}
var __rs=renderSettings;
renderSettings=function(){
 __rs.apply(this,arguments);
 var root=document.getElementById('v-settings');if(!root)return;
 var secs=root.querySelectorAll('.st-sec'),sec=stSection(ic('deco'),'顶部装饰');
 var grid=h('div','dc-grid');
 DECOS.forEach(function(d){
  var b=h('button','dc-card'+(curDeco()===d[0]?' on':''));b.type='button';
  var pv=h('div','dpv');pv.dataset.d=d[0];b.appendChild(pv);
  var lb=h('div','dc-lb');lb.appendChild(h('b',null,d[1]));if(d[2])lb.appendChild(h('small',d[2]==='动态'?'mv':'',d[2]));b.appendChild(lb);
  b.onclick=function(){S.ui.deco=d[0];save();applyDeco();grid.querySelectorAll('.dc-card').forEach(function(x){x.classList.remove('on');});b.classList.add('on');};
  grid.appendChild(b);
 });
 sec.appendChild(grid);
 var anchor=secs[2]||secs[secs.length-1];
 if(anchor&&anchor.parentNode)anchor.parentNode.insertBefore(sec,anchor.nextSibling);else root.appendChild(sec);
};

/* ---------- 7. 滑块：统一样式（带进度填充） ---------- */
function setP(r){var min=+r.min||0,max=+r.max||100,v=+r.value;r.style.setProperty('--p',((v-min)/((max-min)||1)*100)+'%');}
document.addEventListener('input',function(e){if(e.target&&e.target.type==='range')setP(e.target);},true);
new MutationObserver(function(ms){
 ms.forEach(function(m){m.addedNodes.forEach(function(n){
  if(n.nodeType!==1)return;
  if(n.matches&&n.matches('input[type=range]'))setP(n);
  if(n.querySelectorAll)n.querySelectorAll('input[type=range]').forEach(setP);
 });});
}).observe(document.body,{childList:true,subtree:true});

/* ---------- 8. 朗读与音视频互斥 ---------- */
document.addEventListener('play',function(e){
 var t=e.target;
 if(t&&(t.tagName==='AUDIO'||t.tagName==='VIDEO')&&TTS.on){
  if(t.id==='ttsAudioEl'||t===TTS.el)return;     /* 在线朗读自己用的音频元素不算 */
  ttsStop();toast('已切换为播放');
 }
},true);
var __ts5=ttsStart;
ttsStart=function(){
 document.querySelectorAll('audio,video').forEach(function(m){if(m===TTS.el)return;try{if(!m.paused)m.pause();}catch(e){}});
 return __ts5.apply(this,arguments);
};

/* ---------- 8b. 首页快捷入口：极简版也显示 ---------- */
var __rm5=renderMine;
renderMine=function(){
 __rm5.apply(this,arguments);
 var sh=$('mineShort');
 if(sh&&!isPlanet()&&!String(mineKw||'').trim()){
  sh.textContent='';sh.classList.add('sc-row');
  getSC().forEach(function(it){sh.appendChild(scTile(it));});
 }
};

/* ---------- 8c. 空状态：图标 + 一句话 ---------- */
function emptyState(root,icon,title,sub){
 var d=h('div','empty-st'),i=h('span','es-ic');i.innerHTML=ic(icon);d.appendChild(i);d.appendChild(h('b',null,title));if(sub)d.appendChild(h('small',null,sub));
 root.appendChild(d);
}
function decorateEmpty(){
 var root=document.getElementById('v-my-marks');
 if(root&&view==='my-marks'&&!root.querySelector('.empty-st')){
  var ps=Array.from(root.querySelectorAll(':scope>p')).filter(function(p){return /还没有划线/.test(p.textContent||'');});
  if(ps.length){ps[0].remove();emptyState(root,'highlighter','还没有划线','阅读时选中文字即可');}
 }
 var cal=document.getElementById('v-calendar');
 if(cal&&view==='calendar'){
  Array.from(cal.querySelectorAll('p')).forEach(function(p){var t=p.textContent||'';if(t.length>40&&!p.classList.contains('tp-hide'))p.classList.add('tp-hide');});
 }
}

/* ---------- 9. 渲染后处理 ---------- */
var __rd5=render;
render=function(){
 var r=__rd5.apply(this,arguments);
 try{applyDeco();styleToolPage();decorateEmpty();}catch(e){console.error(e);}
 return r;
};
applyDeco();
render();
}());
