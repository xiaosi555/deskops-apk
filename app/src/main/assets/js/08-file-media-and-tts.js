/* Original extension layer v17; preserved in source order. */

/* =====================================================================
   v17 · 文件选择 / 持久保存 / 播放页固定布局 / 倍速弹层 / 悬浮小球 / 朗读稳定性
   ===================================================================== */
(function(){

/* ---------- 1. IndexedDB：句柄与文件副本 ---------- */
window.idbDel=function(k){return idbOpen().then(function(d){return new Promise(function(res,rej){var t=d.transaction('h','readwrite');t.objectStore('h').delete(k);t.oncomplete=function(){res();};t.onerror=function(){rej(t.error);};});});};
window.mediaPurge=function(courseId,chIds){
 (chIds||[]).forEach(function(id){idbDel('blob:'+courseId+':'+id).catch(function(){});idbDel('fh:'+courseId+':'+id).catch(function(){});});
};

/* ---------- 2. 选择文件：用系统文件管理器，保留原文件名 ---------- */
function acceptList(kind){return MEDIA_EXT[kind].map(function(e){return '.'+e;}).join(',');}
pickFiles=function(kind){
 return new Promise(function(resolve){
  function viaInput(){
   var inp=document.createElement('input');inp.type='file';inp.multiple=true;
   inp.style.cssText='position:fixed;left:-9999px;top:0;opacity:0;width:1px;height:1px';document.body.appendChild(inp);
   var rm=function(){setTimeout(function(){inp.remove();},4000);};
   inp.accept=acceptList(kind);   /* 只给扩展名、不给 video/*：手机会打开「文件」管理器而不是相册选择器，名称也保持原样 */
   inp.onchange=function(){
    var fl=Array.from(inp.files||[]).filter(function(f){return mediaKindOf(f)===kind;});
    rm();resolve({items:fl.map(function(f){return toItem(f,'');}),name:''});
   };
   inp.oncancel=function(){rm();resolve(null);};
   inp.click();
  }
  if(window.showOpenFilePicker){
   var acc={};acc[kind+'/*']=MEDIA_EXT[kind].map(function(e){return '.'+e;});
   window.showOpenFilePicker({multiple:true,types:[{description:kind==='video'?'视频文件':'音频文件',accept:acc}]}).then(async function(hs){
    var items=[];
    for(var i=0;i<hs.length;i++){
     var f=await hs[i].getFile();
     if(mediaKindOf(f)===kind){var it=toItem(f,'');it.handle=hs[i];items.push(it);}
    }
    resolve({items:items,name:''});
   }).catch(function(e){if(e&&e.name==='AbortError')resolve(null);else viaInput();});
   return;
  }
  viaInput();
 });
};

/* ---------- 3. 导入后记住文件：句柄 + 应用内副本 ---------- */
function chapterFor(co,it){
 var p=packOf(co);
 return p.ch.find(function(c){return c.media&&(c.media.rel===it.rel||(c.media.fname===it.fname&&c.media.size===it.size));});
}
window.mediaPersist=async function(co,items,keep,dirHandle){
 if(!co)return;
 var list=items.map(function(it){return {it:it,c:chapterFor(co,it)};}).filter(function(x){return x.c;});
 if(dirHandle)idbSet('dir:'+co.id,dirHandle).catch(function(){});
 var done=0,fail=0;
 for(var i=0;i<list.length;i++){
  var x=list[i],key=co.id+':'+x.c.id;
  try{if(x.it.handle)await idbSet('fh:'+key,x.it.handle);}catch(e){}
  if(keep){
   try{await idbSet('blob:'+key,x.it.file);done++;}catch(e){fail++;}
   if(list.length>1&&(i%2===1||i===list.length-1))toast('正在保存到应用内 '+(i+1)+' / '+list.length);
  }
 }
 if(keep){toast(fail?('已保存 '+done+' 个，'+fail+' 个因存储空间不足未保存'):'已保存到应用内，下次打开可直接播放');}
 try{await navigator.storage.persist();}catch(e){}
};
window.mediaKeepNote=function(items,cb){
 var total=items.reduce(function(s,x){return s+(x.size||0);},0);
 var box=h('button','mc-auto on mkeep');box.type='button';
 var txt=h('span',null,'保存到应用内（推荐）· 约 '+fmtSize(total));box.appendChild(txt);
 var st={keep:S.ui.mediaKeep!==false};
 function paint(){box.classList.toggle('on',st.keep);}
 box.onclick=function(){st.keep=!st.keep;S.ui.mediaKeep=st.keep;paint();if(cb)cb(st.keep);};
 paint();
 var p=h('p','ig-note','开启后会复制一份到应用内存储，之后每次打开都能直接播放；关闭则不占空间，但重新打开应用后需要重新选择文件。');
 var wrap=h('div','mkeep-wrap');wrap.appendChild(box);wrap.appendChild(p);
 try{if(navigator.storage&&navigator.storage.estimate){navigator.storage.estimate().then(function(e){var free=(e.quota||0)-(e.usage||0);if(free&&total>free*0.9){p.textContent='可用存储约 '+fmtSize(free)+'，可能放不下这批文件；建议关闭此项，改为记录位置。';p.style.color='var(--danger)';}});}}catch(e){}
 wrap.get=function(){return st.keep;};
 return wrap;
};

/* ---------- 4. 重新连接：应用内副本 → 文件句柄 → 文件夹句柄 ---------- */
var __relinkOrig=relinkMedia;
relinkMedia=async function(courseId,interactive,onlyCh){
 var co=COURSES.find(function(c){return c.id===courseId;});if(!co||!co.media)return false;
 var n=0,p=packOf(co);
 var order=p.ch.filter(function(c){return c.media;});
 if(onlyCh)order.sort(function(a,b){return (a.id===onlyCh?-1:0)-(b.id===onlyCh?-1:0);});
 for(var i=0;i<order.length;i++){
  var c=order[i],key=courseId+':'+c.id;
  if(MEDIA[key])continue;
  try{
   var b=await idbGet('blob:'+key);
   if(b){MEDIA[key]={file:b};n++;continue;}
   var fh=await idbGet('fh:'+key);
   if(fh&&fh.getFile){
    var perm=fh.queryPermission?await fh.queryPermission({mode:'read'}):'granted';
    if(perm!=='granted'&&interactive&&(!onlyCh||c.id===onlyCh))perm=await fh.requestPermission({mode:'read'});
    if(perm==='granted'){MEDIA[key]={file:await fh.getFile()};n++;}
   }
  }catch(e){}
 }
 var missing=order.some(function(c){return !MEDIA[courseId+':'+c.id];});
 if(missing){try{if(await __relinkOrig(courseId,interactive))n++;}catch(e){}}
 return n>0;
};
window.mediaConnectClick=function(co,c){
 relinkMedia(co.id,true,c.id).then(function(ok){
  if(ok&&MEDIA[co.id+':'+c.id]){toast('已连接');render();return;}
  toast('请选择当初导入的文件');
  pickFiles(co.media).then(function(res){
   if(!res||!res.items.length)return;
   var n=attachFiles(co.id,res.items);
   if(!n){toast('没有匹配的文件，请选择当初导入的那个文件');return;}
   mediaPersist(co,res.items,S.ui.mediaKeep!==false,null);
   toast('已连接 '+n+' 个文件');render();probeDurations(co.id);
  });
 });
};
/* 启动时静默连接当前媒体课程，避免进入播放页时先闪一下「未连接」 */
setTimeout(function(){var co=curCourse();if(co&&co.media)relinkMedia(co.id,false).then(function(ok){if(ok&&view==='player')render();});},300);

/* ---------- 5. 播放页：固定高度的播放块 + 单行信息 + 主题化倍速弹层 ---------- */
var RATES=[0.5,0.75,1,1.25,1.5,1.75,2,2.5,3];
var __rp=renderPlayer;
renderPlayer=function(){
 __rp.apply(this,arguments);
 var wrap=document.querySelector('#v-player .pl-page');if(!wrap)return;
 var fix=h('div','pl-fix');
 var media=wrap.querySelector('.media-wrap,.media-un'),hd=wrap.querySelector('.pl-hd'),ctl=wrap.querySelector('.media-ctl');
 if(!media||!hd||!ctl)return;
 /* 信息行：左课程名，右视频详细（去掉单独的视频名称行） */
 var title=hd.querySelector('.pl-title');if(title)title.remove();
 hd.classList.add('pl-row1');
 var kind=(curCourse()&&curCourse().media)||'video';
 if(media.classList.contains('media-wrap'))media.classList.add('pl-fixed');
 /* 倍速：select → 主题化弹层 */
 var sel=ctl.querySelector('select');
 if(sel){
  var b=h('button','mc-speed');b.type='button';
  function lab(){var v=Number(S.ui.mediaRate)||1;b.textContent='倍速 '+v+'×';}
  lab();
  b.onclick=function(){
   var cur=Number(S.ui.mediaRate)||1;
   openPicker('播放倍速',RATES.map(function(v){return [String(v),v+'×'+(v===1?'（正常）':'')];}),String(cur),function(v){
    S.ui.mediaRate=Number(v);var m=document.querySelector('#v-player .media-el');if(m)m.playbackRate=S.ui.mediaRate;save();lab();
   });
  };
  sel.replaceWith(b);
 }
 /* 未连接：按钮走新的连接流程 */
 var un=media.classList&&media.classList.contains('media-un')?media:null;
 if(un){
  var co=curCourse(),c=curChapter();
  var old=un.querySelector('.btn');
  if(old){var nb=old.cloneNode(true);nb.textContent='选择文件 / 恢复连接';nb.onclick=function(){mediaConnectClick(co,c);};old.replaceWith(nb);}
  var s=un.querySelector('.mu-s');
  if(s)s.textContent='「'+((c.media&&c.media.fname)||c.t)+'」'+(c.media&&c.media.size?' · '+fmtSize(c.media.size):'')+' 保存在你的手机里。点下方按钮恢复连接；导入时开启「保存到应用内」后，下次就不需要再选择。';
 }
 fix.appendChild(media);fix.appendChild(hd);fix.appendChild(ctl);
 wrap.insertBefore(fix,wrap.firstChild);
};

/* ---------- 6. 悬浮小球（朗读 / 音频） ---------- */
var FB=null,audDismissed=false;
function audioEl(){return document.querySelector('#v-player audio.media-el');}
function mkBall(){
 var el=h('div','fball');el.id='fb';
 var btn=h('button','fb-main');btn.type='button';btn.setAttribute('aria-label','展开控制');
 var pan=h('div','fb-pan');
 el.appendChild(pan);el.appendChild(btn);document.body.appendChild(el);
 var st={el:el,btn:btn,pan:pan,moved:false,mode:''};
 var drag=null;
 btn.addEventListener('pointerdown',function(e){var r=el.getBoundingClientRect();drag={x:e.clientX,y:e.clientY,l:r.left,t:r.top};st.moved=false;try{btn.setPointerCapture(e.pointerId);}catch(x){}});
 btn.addEventListener('pointermove',function(e){
  if(!drag)return;var dx=e.clientX-drag.x,dy=e.clientY-drag.y;
  if(!st.moved&&Math.abs(dx)+Math.abs(dy)<6)return;
  st.moved=true;el.classList.remove('open');
  var W=window.innerWidth,H=window.innerHeight;
  el.style.left=Math.max(4,Math.min(W-52,drag.l+dx))+'px';el.style.top=Math.max(60,Math.min(H-120,drag.t+dy))+'px';el.style.right='auto';
 });
 function end(){
  if(!drag)return;drag=null;
  if(st.moved){
   var r=el.getBoundingClientRect(),W=window.innerWidth,left=r.left+r.width/2<W/2;
   el.style.left=left?'8px':(W-8-r.width)+'px';el.classList.toggle('left',left);
   S.ui.ballPos={l:left,t:r.top};save();
  }
 }
 btn.addEventListener('pointerup',end);btn.addEventListener('pointercancel',end);
 btn.addEventListener('click',function(){if(st.moved){st.moved=false;return;}el.classList.toggle('open');});
 document.addEventListener('pointerdown',function(e){if(!el.contains(e.target))el.classList.remove('open');},true);
 var p=S.ui.ballPos;
 if(p){el.style.top=p.t+'px';el.style.right='auto';el.style.left=p.l?'8px':'auto';if(!p.l)el.style.right='8px';el.classList.toggle('left',!!p.l);}
 else el.style.top='42%';
 return st;
}
function ballBtn(label,iconKey,fn){var b=h('button','fb-b');b.type='button';b.innerHTML=ic(iconKey);b.setAttribute('aria-label',label);b.title=label;b.onclick=function(e){e.stopPropagation();fn();};return b;}
/* 文字朗读与音频只用一个悬浮小球；两者互斥，不会同时发声 */
window.updateFloat=function(){
 var a=audioEl();
 var tts=!!(TTS.on&&view!=='today');
 var aud=!!(a&&view!=='player'&&!audDismissed&&!a.ended&&(a.currentTime>0||!a.paused||S.ui.mediaAutoplay));
 var mode=tts?'tts':(aud?'aud':'');
 if(!mode){if(FB){FB.el.classList.remove('on','open','playing');FB.mode='';}return;}
 if(!FB)FB=mkBall();
 var playing=mode==='tts'?!TTS.paused:!a.paused;
 FB.el.classList.add('on');FB.el.classList.toggle('playing',playing);
 if(FB.mode!==mode||FB.pl!==playing||mode==='aud'){
  FB.mode=mode;FB.pl=playing;
  FB.btn.innerHTML=ic(mode==='tts'?'speaker':'g_headset');
  FB.pan.textContent='';
  if(mode==='tts'){
   FB.pan.appendChild(h('span','fb-t',TTS.paused?'朗读已暂停':'朗读中'));
   FB.pan.appendChild(ballBtn(TTS.paused?'继续朗读':'暂停朗读',TTS.paused?'play':'pauseI',function(){ttsToggle();}));
   FB.pan.appendChild(ballBtn('回到文章','book',function(){FB.el.classList.remove('open');nav('today');}));
   FB.pan.appendChild(ballBtn('停止朗读','close',function(){ttsStop();}));
  }else{
   var c=null;try{c=curChapter();}catch(e){}
   FB.pan.appendChild(h('span','fb-t',(c&&c.t)||'音频播放中'));
   FB.pan.appendChild(ballBtn(a.paused?'继续播放':'暂停播放',a.paused?'play':'pauseI',function(){if(a.paused){var pr=a.play();if(pr&&pr.catch)pr.catch(function(){});}else a.pause();}));
   FB.pan.appendChild(ballBtn('回到播放页','g_music',function(){FB.el.classList.remove('open');nav('today');}));
   FB.pan.appendChild(ballBtn('关闭','close',function(){a.pause();audDismissed=true;updateFloat();}));
  }
 }
};
['play','pause','ended','emptied','loadedmetadata'].forEach(function(ev){
 document.addEventListener(ev,function(e){
  if(e.target&&e.target.tagName==='AUDIO'){if(ev==='play')audDismissed=false;setTimeout(updateFloat,0);}
 },true);
});
/* 离开播放页时，音频连续播放：播完自动接下一集（后台也继续） */
var __mn=mediaNext;
mediaNext=function(){
 __mn.apply(this,arguments);
 try{if(view!=='player'){var c=curChapter();if(c&&c.media&&(curCourse().media==='audio'))renderPlayer();}}catch(e){}
};
/* 媒体课程：条目名称沿用原文件名，去掉界面里多余的「01 」序号前缀 */
function stripMediaIds(){
 var titles={},any=false;
 COURSES.forEach(function(co){if(co.media&&co.pack){any=true;co.pack.ch.forEach(function(c){titles[c.id+' '+c.t]=c.t;});}});
 if(!any)return;
 ['v-mine','v-course','mini','v-my-marks'].forEach(function(id){
  var root=document.getElementById(id);if(!root)return;
  var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),n;
  while((n=w.nextNode())){var t=n.nodeValue.trim();if(titles[t])n.nodeValue=titles[t];}
 });
}
var __rd3=render;
render=function(){var r=__rd3.apply(this,arguments);try{updateFloat();stripMediaIds();}catch(e){}return r;};

/* ---------- 7. 朗读：系统引擎稳健化、语速立即生效 ---------- */
speakSystem=function(item,tok,next,fail){
 if(!ttsSupported()){fail('unsupported');return;}
 var cfg=ttsCfg(),synth=speechSynthesis,tries=0,finished=false;
 function build(){
  var u=new SpeechSynthesisUtterance(item.text);TTS.utt=u;     /* 持有引用，避免被回收后不触发回调 */
  u.lang='zh-CN';u.rate=Math.max(0.5,Math.min(2,Number(cfg.rate)||1));u.volume=1;
  try{
   var vs=synth.getVoices(),v=null;
   if(cfg.voice)v=vs.find(function(x){return x.voiceURI===cfg.voice||x.name===cfg.voice;});
   if(!v)v=vs.find(function(x){return /zh/i.test(x.lang);});
   if(v){u.voice=v;u.lang=v.lang||'zh-CN';}
  }catch(e){}
  var started=false;
  u.onstart=function(){started=true;if(tok===TTS.tok)ttsMarkNow(item.el);};
  u.onend=function(){if(finished)return;finished=true;started=true;if(tok===TTS.tok)next();};
  u.onerror=function(e){if(tok!==TTS.tok)return;if(e&&(e.error==='canceled'||e.error==='interrupted'))return;fail((e&&e.error)||'error');};
  return {u:u,started:function(){return started;}};
 }
 function go(){
  if(tok!==TTS.tok)return;
  var b=build(),delay=TTS.justStarted?180:0;TTS.justStarted=false;
  try{if(synth.speaking||synth.pending){synth.cancel();delay=Math.max(delay,220);}synth.resume();}catch(e){}
  setTimeout(function(){
   if(tok!==TTS.tok)return;
   try{synth.speak(b.u);}catch(e){fail('throw');return;}
   setTimeout(function(){
    if(tok!==TTS.tok||b.started()||finished)return;
    if(tries++<1){try{synth.cancel();}catch(e){}setTimeout(go,260);}   /* 取消后马上再说话常会静默失败：隔一会儿重试一次 */
    else{try{synth.cancel();}catch(e){}fail('timeout');}
   },2600);
  },delay);
 }
 go();
};
var __ts3=ttsStart;
ttsStart=function(){
 TTS.justStarted=true;
 /* 系统朗读：取消上一次后留出间隔再开始 */
 try{if(ttsSupported()&&(speechSynthesis.speaking||speechSynthesis.pending)){speechSynthesis.cancel();}}catch(e){}
 return __ts3.apply(this,arguments);
};
}());
