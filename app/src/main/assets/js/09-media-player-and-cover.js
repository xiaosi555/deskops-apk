/* Original extension layer v18; preserved in source order. */

/* =====================================================================
   v18 · 视频封面 / 稳定全屏 / 音频紧凑播放条 / 工具箱适配课程内容
   ===================================================================== */
(function(){

/* ---------- 1. 视频封面（取视频某一帧） ---------- */
var POSTERS={},pq=Promise.resolve(),ensuring={};
function pkey(co,c){return co.id+':'+c.id;}
window.posterOn=function(co){return !!(co&&co.media==='video'&&(!co.poster||co.poster.on!==false));};
function posterT(co,c){var m=c.media||{};if(m.pt!=null)return Number(m.pt)||0;return Number(co.poster&&co.poster.t!=null?co.poster.t:1)||0;}
function frameBlob(file,t){
 return new Promise(function(res){
  var url=URL.createObjectURL(file),v=document.createElement('video'),done=false,tm=0;
  v.muted=true;v.preload='auto';v.setAttribute('playsinline','');
  function fin(b){if(done)return;done=true;clearTimeout(tm);try{v.removeAttribute('src');v.load();}catch(e){}URL.revokeObjectURL(url);res(b);}
  tm=setTimeout(function(){fin(null);},9000);
  v.onerror=function(){fin(null);};
  v.onloadedmetadata=function(){var d=v.duration||0,tt=d?Math.min(Math.max(0.05,t),Math.max(0.05,d-0.2)):Math.max(0.05,t);try{v.currentTime=tt;}catch(e){fin(null);}};
  v.onseeked=function(){
   try{
    var W=360,r=(v.videoWidth&&v.videoHeight)?v.videoWidth/v.videoHeight:16/9,hh=Math.max(1,Math.round(W/r));
    var cv=document.createElement('canvas');cv.width=W;cv.height=hh;cv.getContext('2d').drawImage(v,0,0,W,hh);
    cv.toBlob(function(b){fin(b);},'image/jpeg',0.72);
   }catch(e){fin(null);}
  };
  v.src=url;
 });
}
function makePoster(co,c,t){
 var key=pkey(co,c),reg=MEDIA[key];if(!reg||!reg.file)return Promise.resolve(false);
 pq=pq.then(async function(){
  var b=await frameBlob(reg.file,t);if(!b)return false;
  try{await idbSet('poster:'+key,b);}catch(e){}
  if(POSTERS[key])URL.revokeObjectURL(POSTERS[key]);
  POSTERS[key]=URL.createObjectURL(b);decoratePosters();return true;
 }).catch(function(){return false;});
 return pq;
}
async function ensurePosters(co){
 if(!posterOn(co)||ensuring[co.id])return;
 ensuring[co.id]=true;
 try{
  var chs=packOf(co).ch.filter(function(c){return c.media;});
  for(var i=0;i<chs.length;i++){
   var c=chs[i],key=pkey(co,c);if(POSTERS[key])continue;
   try{var b=await idbGet('poster:'+key);if(b){POSTERS[key]=URL.createObjectURL(b);decoratePosters();continue;}}catch(e){}
   if(MEDIA[key])await makePoster(co,c,posterT(co,c));
  }
 }finally{ensuring[co.id]=false;}
}
function clearPosters(co,onlyCh){
 packOf(co).ch.forEach(function(c){
  if(onlyCh&&c.id!==onlyCh)return;
  var key=pkey(co,c);if(POSTERS[key]){URL.revokeObjectURL(POSTERS[key]);delete POSTERS[key];}
  idbDel('poster:'+key).catch(function(){});
 });
}
var __epc=epChapter;
epChapter=function(c){var el=__epc.apply(this,arguments);el.dataset.cid=c.id;return el;};
function decoratePosters(){
 var co;try{co=curCourse();}catch(e){}
 if(!co||co.media!=='video')return;
 var on=posterOn(co),pk=function(id){return on&&POSTERS[co.id+':'+id];};
 document.querySelectorAll('#v-course .ep[data-cid]').forEach(function(el){
  var th=el.querySelector('.ep-th');if(!th)return;
  var url=pk(el.dataset.cid),d=th.querySelector('.vp-img');
  if(url){th.classList.add('vp');if(!d){d=h('div','vp-img');th.appendChild(d);}d.style.backgroundImage='url("'+url+'")';}
  else{th.classList.remove('vp');if(d)d.remove();}
 });
 var v=document.querySelector('#v-player video.media-el'),c=null;try{c=curChapter();}catch(e){}
 if(v&&c){var pu=pk(c.id);if(pu)v.poster=pu;else v.removeAttribute('poster');}
}
window.decoratePosters=decoratePosters;

/* 预览：从某个视频里取帧，显示在对话框里 */
function mountPreview(file,t,box,onDur){
 var url=URL.createObjectURL(file),v=document.createElement('video');
 v.muted=true;v.preload='auto';v.setAttribute('playsinline','');v.src=url;box.textContent='';box.appendChild(v);
 v.onloadedmetadata=function(){if(onDur)onDur(v.duration||0);try{v.currentTime=Math.min(t,Math.max(0.05,(v.duration||t)-0.2));}catch(e){}};
 box.__v=v;box.__url=url;
 return v;
}
function unmount(box){try{if(box.__url)URL.revokeObjectURL(box.__url);}catch(e){}}
function firstFile(co){
 var chs=packOf(co).ch;
 for(var i=0;i<chs.length;i++){var r=MEDIA[pkey(co,chs[i])];if(r&&r.file)return {c:chs[i],file:r.file};}
 return null;
}
function fmtT(s){s=Math.max(0,Math.round(s));return Math.floor(s/60)+':'+('0'+(s%60)).slice(-2);}

/* 整门课程的封面设置 */
window.openPosterSheet=function(co){
 co.poster=co.poster||{on:true,t:1};
 var st={on:co.poster.on!==false,t:Number(co.poster.t!=null?co.poster.t:1)};
 var body=h('div','ps');
 var s1=stSection(ic('g_camera'),'视频封面');
 var opts=h('div','st-opts two');
 var bFrame=stOption('视频画面',st.on,function(){st.on=true;paint();},ic('g_video'));
 var bIcon=stOption('默认图标',!st.on,function(){st.on=false;paint();},ic(co.glyph||'monitor'));
 opts.appendChild(bFrame);opts.appendChild(bIcon);s1.appendChild(opts);body.appendChild(s1);
 var s2=stSection(ic('g_clock'),'截取位置');
 var stage=h('div','ps-stage');s2.appendChild(stage);
 var row=h('div','ps-row'),lab=h('span','ps-t',fmtT(st.t));
 var rg=document.createElement('input');rg.type='range';rg.min=0;rg.max=120;rg.step=1;rg.value=Math.min(120,st.t);
 row.appendChild(rg);row.appendChild(lab);s2.appendChild(row);body.appendChild(s2);
 var ff=firstFile(co),pv=null,timer=0;
 if(ff){
  pv=mountPreview(ff.file,st.t,stage,function(d){if(d){rg.max=Math.max(5,Math.floor(d));rg.value=Math.min(st.t,rg.max);}});
 }else{stage.appendChild(h('div','ps-empty','连接视频文件后可预览'));}
 rg.oninput=function(){st.t=Number(rg.value);lab.textContent=fmtT(st.t);if(pv){clearTimeout(timer);timer=setTimeout(function(){try{pv.currentTime=st.t;}catch(e){}},60);}};
 function paint(){
  bFrame.classList.toggle('on',st.on);bIcon.classList.toggle('on',!st.on);
  s2.style.opacity=st.on?'':'.4';s2.style.pointerEvents=st.on?'':'none';
 }
 paint();
 openSheet('视频封面',body,function(){
  var changedT=st.t!==Number(co.poster.t!=null?co.poster.t:1);
  co.poster={on:st.on,t:st.t};
  if(changedT){clearPosters(co);packOf(co).ch.forEach(function(c){if(c.media)delete c.media.pt;});}
  save();unmount(stage);render();
  if(st.on)setTimeout(function(){ensurePosters(co);},200);
  toast(st.on?'已更新视频封面':'已隐藏视频封面');
 },'应用',{cls:'fixh',onCancel:function(){unmount(stage);closeSheet();}});
};
/* 单个视频的封面 */
window.openVideoPosterSheet=function(co,c){
 var reg=MEDIA[pkey(co,c)];
 if(!reg||!reg.file){toast('请先连接这个视频文件');return;}
 var st={t:posterT(co,c)};
 var body=h('div','ps');
 var s=stSection(ic('g_camera'),'选择封面画面');
 var stage=h('div','ps-stage');s.appendChild(stage);
 var row=h('div','ps-row'),lab=h('span','ps-t',fmtT(st.t));
 var rg=document.createElement('input');rg.type='range';rg.min=0;rg.max=600;rg.step=0.5;rg.value=st.t;
 row.appendChild(rg);row.appendChild(lab);s.appendChild(row);body.appendChild(s);
 var pv=mountPreview(reg.file,st.t,stage,function(d){if(d){rg.max=Math.max(1,Math.floor(d*10)/10);rg.value=Math.min(st.t,rg.max);}}),timer=0;
 rg.oninput=function(){st.t=Number(rg.value);lab.textContent=fmtT(st.t);clearTimeout(timer);timer=setTimeout(function(){try{pv.currentTime=st.t;}catch(e){}},50);};
 var rs=h('button','btn ghost ps-reset');rs.type='button';rs.innerHTML=ic('resetArrow')+'<span>用整体设置</span>';
 rs.onclick=function(){if(c.media)delete c.media.pt;var pc=packOf(co).ch.find(function(x){return x.id===c.id;});if(pc&&pc.media)delete pc.media.pt;clearPosters(co,c.id);save();unmount(stage);closeSheet();ensurePosters(co);toast('已恢复整体设置');};
 body.appendChild(rs);
 openSheet('视频封面',body,function(){
  var pc=packOf(co).ch.find(function(x){return x.id===c.id;})||c;
  pc.media.pt=st.t;clearPosters(co,c.id);save();unmount(stage);
  makePoster(co,pc,st.t).then(function(){render();});
  toast('已设为封面');
 },'设为封面',{cls:'fixh',onCancel:function(){unmount(stage);closeSheet();}});
};
/* 条目菜单里加「设置封面」 */
var __ocm4=openChapterMenu;
openChapterMenu=function(c){
 var co=curCourse();
 if(!co||co.media!=='video')return __ocm4.apply(this,arguments);
 openPicker(c.t,[['rename','重命名'],['poster','设置封面'],['del','删除','','danger']],null,function(v){
  setTimeout(function(){
   if(v==='poster')openVideoPosterSheet(co,c);
   else if(v==='rename')window.renameMediaItem&&window.renameMediaItem(co,c);
   else window.deleteMediaItem&&window.deleteMediaItem(co,c);
  },120);
 });
};

/* ---------- 2. 播放页：稳定全屏 / 音频紧凑播放条 ---------- */
function enterFS(v){
 try{
  var rq=v.requestFullscreen||v.webkitRequestFullscreen||v.msRequestFullscreen;
  if(rq){var pr=rq.call(v);if(pr&&pr.catch)pr.catch(function(){if(v.webkitEnterFullscreen)v.webkitEnterFullscreen();});}
  else if(v.webkitEnterFullscreen)v.webkitEnterFullscreen();
 }catch(e){}
}
function onFsChange(){
 var fs=document.fullscreenElement||document.webkitFullscreenElement;
 try{
  if(fs&&fs.tagName==='VIDEO'){
   var land=(fs.videoWidth||16)>=(fs.videoHeight||9);
   if(screen.orientation&&screen.orientation.lock)screen.orientation.lock(land?'landscape':'portrait').catch(function(){});
  }else if(screen.orientation&&screen.orientation.unlock)screen.orientation.unlock();
 }catch(e){}
}
document.addEventListener('fullscreenchange',onFsChange);document.addEventListener('webkitfullscreenchange',onFsChange);
function fmtClock(s){if(!isFinite(s)||s<0)s=0;s=Math.floor(s);var m=Math.floor(s/60),x=s%60;return m+':'+('0'+x).slice(-2);}
var __rp4=renderPlayer;
renderPlayer=function(){
 __rp4.apply(this,arguments);
 var co=curCourse(),el=document.querySelector('#v-player .media-el');
 if(!el||!co)return;
 if(co.media==='video'){
  /* 用自己的全屏按钮：系统全屏会随画面比例自动转屏，容易横竖来回跳；这里固定锁定方向 */
  el.setAttribute('controlsList','nofullscreen noremoteplayback');
  el.setAttribute('disablePictureInPicture','');
  var ctl=document.querySelector('#v-player .media-ctl');
  if(ctl&&!ctl.querySelector('.mc-fs')){
   var fb=h('button','mc-fs');fb.type='button';fb.innerHTML=ic('g_video')+'<span>全屏</span>';fb.setAttribute('aria-label','全屏播放');
   fb.onclick=function(){enterFS(el);};
   var sp=ctl.querySelector('.mc-speed');ctl.insertBefore(fb,sp||null);
  }
  decoratePosters();
 }else if(co.media==='audio'){
  var wrap=document.querySelector('#v-player .media-wrap.audio');
  if(!wrap||wrap.querySelector('.ap'))return;
  el.controls=false;el.style.display='none';
  var art=wrap.querySelector('.media-art');
  var bar=h('div','ap');
  var a=h('span','ap-art');a.innerHTML=ic(co.glyph&&co.glyph!=='monitor'?co.glyph:'g_headset');bar.appendChild(a);
  var pb=h('button','ap-play');pb.type='button';bar.appendChild(pb);
  var mid=h('div','ap-mid'),rg=document.createElement('input');rg.type='range';rg.min=0;rg.max=1000;rg.value=0;rg.className='ap-rg';mid.appendChild(rg);
  var tm=h('div','ap-tm'),cur=h('span',null,'0:00'),tot=h('span',null,'0:00');tm.appendChild(cur);tm.appendChild(tot);mid.appendChild(tm);bar.appendChild(mid);
  if(art)art.remove();wrap.appendChild(bar);
  var drag=false;
  function paintPlay(){pb.innerHTML=ic(el.paused?'play':'pauseI');pb.setAttribute('aria-label',el.paused?'播放':'暂停');}
  function upd(){var d=el.duration||0;tot.textContent=fmtClock(d);if(!drag){cur.textContent=fmtClock(el.currentTime);rg.value=d?Math.round(el.currentTime/d*1000):0;rg.style.setProperty('--p',(d?el.currentTime/d*100:0)+'%');}}
  pb.onclick=function(){if(el.paused){var p=el.play();if(p&&p.catch)p.catch(function(){});}else el.pause();};
  rg.oninput=function(){drag=true;var d=el.duration||0;cur.textContent=fmtClock(d*rg.value/1000);rg.style.setProperty('--p',(rg.value/10)+'%');};
  rg.onchange=function(){var d=el.duration||0;if(d)el.currentTime=d*rg.value/1000;drag=false;};
  ['play','pause','ended'].forEach(function(e){el.addEventListener(e,paintPlay);});
  ['timeupdate','loadedmetadata','durationchange'].forEach(function(e){el.addEventListener(e,upd);});
  paintPlay();upd();
 }
};

/* ---------- 3. 工具箱：功能项按课程内容适配 ---------- */
function hasLong(){try{return Object.keys(LONG||{}).length>0;}catch(e){return false;}}
function hasTerms(){try{return CH.some(function(c){return c.terms&&String(c.terms).trim();})||(GLBASE&&Object.keys(GLBASE).length>0);}catch(e){return false;}}
function hasPits(){try{return CH.some(function(c){return c.pits&&c.pits.length;})||(SCEN&&Object.keys(SCEN).length>0)||(EVENTS&&EVENTS.length>0);}catch(e){return false;}}
function setToolGroups(){
 var co;try{co=curCourse();}catch(e){return;}
 var g=[];
 if(co.media){
  var KN=co.media==='video'?'视频':'音频';
  g.push({g:'播放',items:[['today','继续播放','play'],['act:import','导入'+KN,'importCourse'],co.media==='video'?['act:cover','视频封面','g_camera']:['act:speed','播放倍速','g_clock']]});
  g.push({g:'学习',items:[['review','复习队列','refresh'],['prog','学习进度','chart'],['calendar','学习日历','calendar']]});
  g.push({g:'管理',items:[['act:edit','课程设置','wrench'],['io','数据','share']]});
 }else if(co.custom){
  g.push({g:'学习节奏',items:[['today','今日学习','play'],['review','复习队列','refresh'],['anno','我的划线','highlighter']]});
  var lib=[];if(hasLong())lib.push(['know','知识详解','book']);if(hasTerms())lib.push(['gloss','术语手册','aA']);lib.push(['case','案例库','folder']);
  g.push({g:'知识资料库',items:lib});
  var mt=[];if(hasPits())mt.push(['play','排障手册','wrench']);mt.push(['prog','学习进度','chart']);mt.push(['io','数据','share']);
  g.push({g:'方法与进度',items:mt});
 }else return;     /* 内置课程保持完整工具箱 */
 TOOLGROUPS.splice(0,TOOLGROUPS.length);g.forEach(function(x){TOOLGROUPS.push(x);});
}
var BASE_TOOLGROUPS=TOOLGROUPS.map(function(x){return {g:x.g,items:x.items.slice()};});
var __rc4=renderCourse;
renderCourse=function(){
 var co;try{co=curCourse();}catch(e){}
 if(co&&(co.media||co.custom))setToolGroups();
 else{TOOLGROUPS.splice(0,TOOLGROUPS.length);BASE_TOOLGROUPS.forEach(function(x){TOOLGROUPS.push({g:x.g,items:x.items.slice()});});}
 return __rc4.apply(this,arguments);
};
var __tt=toolTile;
toolTile=function(v,label,ico,badge){
 if(String(v).indexOf('act:')!==0)return __tt.apply(this,arguments);
 var t=h('div','tile'),c=h('span','tic');c.innerHTML=ic(ico);t.appendChild(c);t.appendChild(h('span',null,label));
 t.onclick=function(){
  var co=curCourse(),k=v.slice(4);
  if(k==='import')window.openMediaImportSheet&&window.openMediaImportSheet(co);
  else if(k==='cover')openPosterSheet(co);
  else if(k==='edit')openCourseEditor(co);
  else if(k==='speed'){var RT=[0.5,0.75,1,1.25,1.5,1.75,2,2.5,3],cur=Number(S.ui.mediaRate)||1;openPicker('播放倍速',RT.map(function(x){return [String(x),x+'×'+(x===1?'（正常）':'')];}),String(cur),function(x){S.ui.mediaRate=Number(x);save();toast('倍速 '+x+'×');});}
 };
 return t;
};

/* ---------- 4. 渲染后：封面装饰 / 生成封面 ---------- */
var __rd4=render;
render=function(){
 var r=__rd4.apply(this,arguments);
 try{
  var co=curCourse();
  if(co&&co.media==='video'&&(view==='course'||view==='player')){decoratePosters();ensurePosters(co);}
 }catch(e){}
 return r;
};

/* 兜底：重新连接成功后不再来回重绘（避免循环刷新播放器导致退出全屏） */
var __rl4=relinkMedia;
relinkMedia=async function(courseId,interactive,onlyCh){
 var before=Object.keys(MEDIA).filter(function(k){return k.indexOf(courseId+':')===0;}).length;
 await __rl4.apply(this,arguments);
 var after=Object.keys(MEDIA).filter(function(k){return k.indexOf(courseId+':')===0;}).length;
 return after>before;
};
}());
