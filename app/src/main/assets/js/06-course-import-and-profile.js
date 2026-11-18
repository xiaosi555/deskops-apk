/* Original extension layer v15; preserved in source order. */

/* =====================================================================
   v15 · 补丁层：课程导入页 / 课程删除 / 主题化弹窗 / 快捷入口 / 数据折叠 / 个人数据 / 朗读续读
   ===================================================================== */
(function(){
var NS='http://www.w3.org/2000/svg';
function P(d){return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+d+'</svg>';}

/* ---------- 图标 ---------- */
ICONS.importCourse=P('<path d="M12 3.5v10.5"/><path d="M8 10.5l4 4 4-4"/><path d="M4.5 14.5V18a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-3.5"/>');
ICONS.resetArrow=P('<path d="M4.5 12a7.5 7.5 0 1 0 2.3-5.4"/><path d="M4.5 4.5v4.2h4.2"/>');
ICONS.chevDown=P('<path d="M6 9.5l6 6 6-6"/>');
ICONS.checkTick=P('<path d="M5 12.5l4.5 4.5L19 7.5"/>');
var G={
 g_code:'<path d="M8.5 7.5L4 12l4.5 4.5"/><path d="M15.5 7.5L20 12l-4.5 4.5"/><path d="M13.5 5l-3 14"/>',
 g_terminal:'<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><path d="M7.5 9.5l3 2.5-3 2.5M12.5 15h4"/>',
 g_cloud:'<path d="M7.5 18.5h9.2a4 4 0 0 0 .6-7.95 5.5 5.5 0 0 0-10.6 1.2A3.4 3.4 0 0 0 7.5 18.5z"/>',
 g_shield:'<path d="M12 3.5l7 2.8v5.4c0 4.2-2.9 7.3-7 8.8-4.1-1.5-7-4.6-7-8.8V6.3z"/><path d="M9 12l2.2 2.2L15.2 10"/>',
 g_cpu:'<rect x="7" y="7" width="10" height="10" rx="2"/><path d="M9.5 3.5V7M14.5 3.5V7M9.5 17v3.5M14.5 17v3.5M3.5 9.5H7M3.5 14.5H7M17 9.5h3.5M17 14.5h3.5"/>',
 g_globe:'<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17"/><path d="M12 3.5c2.5 2.4 3.7 5.2 3.7 8.5s-1.2 6.100-3.700 8.500C9.500 18.100 8.300 15.300 8.300 12S9.500 5.900 12 3.500z"/>',
 g_bulb:'<path d="M9.5 17.5h5M10 20.5h4"/><path d="M12 3.5a5.500 5.500 0 0 0-3.200 10c.5.400.700.900.700 1.500v.5h5V15c0-.6.2-1.100.7-1.500A5.500 5.500 0 0 0 12 3.500z"/>',
 g_flask:'<path d="M9.500 3.500h5M10.500 3.500V9L5.500 18a1.800 1.800 0 0 0 1.600 2.500h9.800a1.800 1.800 0 0 0 1.600-2.500L13.500 9V3.500"/><path d="M8 14.500h8"/>',
 g_music:'<path d="M9 18V6.500l10-2V16"/><circle cx="6.500" cy="18" r="2.500"/><circle cx="16.500" cy="16" r="2.500"/>',
 g_video:'<rect x="3.500" y="6" width="12" height="12" rx="2.500"/><path d="M15.500 10.500l5-3v9l-5-3z"/>',
 g_camera:'<path d="M4 8.500h3l1.500-2.500h7L17 8.500h3v10H4z"/><circle cx="12" cy="13.500" r="3.200"/>',
 g_rocket:'<path d="M14 4.500c3 .3 5 2.300 5.500 5.500L14 15.500 8.500 10z"/><path d="M8.500 10L5 11l-1.500 3 4 .5M14 15.500L13 19l-3 1.500-.5-4"/><circle cx="14.500" cy="9.500" r="1.300"/>',
 g_heart:'<path d="M12 19.500S4.500 15 4.500 9.800A3.800 3.800 0 0 1 12 8a3.800 3.800 0 0 1 7.500 1.800C19.500 15 12 19.500 12 19.500z"/>',
 g_bolt:'<path d="M13 3.500L5.500 13.500H11l-1 7 7.500-10H12z"/>',
 g_target:'<circle cx="12" cy="12" r="8.500"/><circle cx="12" cy="12" r="4.500"/><circle cx="12" cy="12" r="1" fill="currentColor"/>',
 g_puzzle:'<path d="M9.500 4.500h5v3a1.800 1.800 0 1 1 0 3.600v.4H18v4h-3a1.800 1.800 0 1 0-3.600 0V19.500h-5v-4h3A1.800 1.800 0 1 0 6.500 12H4.500V8h5z"/>',
 g_server:'<rect x="4" y="4.500" width="16" height="6" rx="2"/><rect x="4" y="13.500" width="16" height="6" rx="2"/><path d="M7.500 7.500h.01M7.500 16.500h.01"/>',
 g_wifi:'<path d="M3.500 9.500a12 12 0 0 1 17 0"/><path d="M6.500 12.800a7.800 7.800 0 0 1 11 0"/><path d="M9.500 16a3.500 3.500 0 0 1 5 0"/><path d="M12 19.300h.01"/>',
 g_mic:'<rect x="9" y="3.500" width="6" height="10.500" rx="3"/><path d="M5.500 11.500a6.500 6.500 0 0 0 13 0M12 18v2.500"/>',
 g_headset:'<path d="M4.500 14v-2a7.500 7.500 0 0 1 15 0v2"/><rect x="3.500" y="13.500" width="4" height="6" rx="1.800"/><rect x="16.500" y="13.500" width="4" height="6" rx="1.800"/>',
 g_clock:'<circle cx="12" cy="12" r="8.500"/><path d="M12 7v5l3.200 2"/>',
 g_mail:'<rect x="3.500" y="5.500" width="17" height="13" rx="2.500"/><path d="M4 7.500l8 5.500 8-5.500"/>'
};
Object.keys(G).forEach(function(k){ICONS[k]=P(G[k]);});
var COURSE_GLYPHS=['monitor','book','g_code','g_terminal','g_server','g_cpu','g_wifi','g_shield','g_cloud','g_globe','g_bulb','g_flask','g_rocket','g_target','g_puzzle','g_bolt','g_heart','star','g_video','g_music','g_mic','g_headset','g_camera','folder','wrench','chart','layers','compass'];
var ENTRY_GLYPHS=['layers','book','highlighter','calendar','play','refresh','aA','folder','wrench','chart','share','settings','database','compass','star','g_bolt','g_heart','g_rocket','g_target','g_bulb','g_flask','g_code','g_terminal','g_globe','g_cloud','g_music','g_video','g_mic','g_headset','g_camera','g_puzzle','g_shield','g_cpu','g_server','g_wifi','g_clock'];
window.ENTRY_GLYPHS=ENTRY_GLYPHS;window.COURSE_GLYPHS=COURSE_GLYPHS;
function iconGrid(cur,onPick,list){
 var g=h('div','ig');
 list.forEach(function(k){
  var b=h('button',cur===k?'on':'');b.type='button';b.innerHTML=ic(k);b.setAttribute('aria-label',k);
  b.onclick=function(){cur=k;g.querySelectorAll('button').forEach(function(x){x.classList.remove('on');});b.classList.add('on');onPick(k);};
  g.appendChild(b);
 });
 return g;
}

window.iconGrid=iconGrid;
/* ---------- 主题化确认弹窗（替代原生 confirm） ---------- */
window.uiConfirm=function(msg,onOk,o){
 o=o||{};
 var old=document.querySelector('.dlg-mask');if(old)old.remove();
 var mask=h('div','dlg-mask'),dlg=h('div','dlg');
 dlg.setAttribute('role','alertdialog');dlg.setAttribute('aria-modal','true');
 dlg.appendChild(h('h4',null,o.title||(o.danger?'确认操作':'请确认')));
 dlg.appendChild(h('p',null,msg));
 var ft=h('div','dlg-ft'),c=h('button','btn',o.cancel||'取消'),k=h('button','btn '+(o.danger?'danger':'pri'),o.ok||'确定');
 function kd(e){if(e.key==='Escape')close();}
 function close(){document.removeEventListener('keydown',kd);mask.classList.remove('on');setTimeout(function(){mask.remove();},200);}
 c.onclick=function(){close();if(o.onCancel)o.onCancel();};
 k.onclick=function(){close();try{if(onOk)onOk();}catch(e){console.error(e);}};
 mask.onclick=function(e){if(e.target===mask)close();};
 document.addEventListener('keydown',kd);
 ft.appendChild(c);ft.appendChild(k);dlg.appendChild(ft);mask.appendChild(dlg);document.body.appendChild(mask);
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');});});
 return mask;
};

/* ---------- 主题化选项弹层（替代原生下拉） ---------- */
window.openPicker=function(title,items,value,onPick){
 var old=document.querySelector('.pk-mask');if(old)old.remove();
 var mask=h('div','pk-mask'),box=h('div','pk');
 box.appendChild(h('div','pk-hd',title));
 function close(){mask.classList.remove('on');setTimeout(function(){mask.remove();},260);}
 items.forEach(function(it){
  if(it.g){box.appendChild(h('div','pk-grp',it.g));return;}
  var b=h('button','pk-opt'+(String(it[0])===String(value)?' on':'')+(it[3]?' '+it[3]:''));b.type='button';
  var t=h('span','pk-t');t.appendChild(document.createTextNode(it[1]));if(it[2])t.appendChild(h('small',null,it[2]));b.appendChild(t);
  var ck=h('span','ck');ck.innerHTML=ic('checkTick');b.appendChild(ck);
  b.onclick=function(){close();onPick(it[0],it[1]);};
  box.appendChild(b);
 });
 mask.appendChild(box);mask.onclick=function(e){if(e.target===mask)close();};
 document.body.appendChild(mask);
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');var on=box.querySelector('.pk-opt.on');if(on)on.scrollIntoView({block:'center'});});});
};
function pkField(label,getText,items,getVal,onPick){
 var b=h('button','pkbtn');b.type='button';
 var sp=h('span',null,getText());b.appendChild(sp);
 var ch=h('i');ch.style.display='contents';ch.innerHTML=ic('chevDown');b.appendChild(ch);
 b.onclick=function(){
  openPicker(label,typeof items==='function'?items():items,getVal(),function(v,t){onPick(v,t);sp.textContent=getText();});
 };
 return field(label,b);
}

/* ---------- openSheet：支持自定义左侧按钮（如「删除」「移除」） ---------- */
openSheet=function(title,body,onSave,saveText,opts){
 opts=opts||{};
 closeSheet();
 var mask=h('div','sheetmask'),sh=h('div','sheet'+(opts.cls?' '+opts.cls:''));
 var hd=h('div','shhd');hd.appendChild(h('b',null,title));
 var x=h('button','icbtn');x.innerHTML=ic('close');x.setAttribute('aria-label','关闭');x.onclick=closeSheet;hd.appendChild(x);
 sh.appendChild(hd);sh.appendChild(body);
 if(onSave){
  var ft=h('div','shft');
  var c=h('button','btn'+(opts.danger?' danger':''),opts.cancelText||'取消');
  c.onclick=function(){if(opts.onCancel)opts.onCancel();else closeSheet();};
  var s=h('button','btn pri',saveText||'保存');s.onclick=function(){if(onSave()!==false)closeSheet();};
  ft.appendChild(c);ft.appendChild(s);sh.appendChild(ft);
 }
 mask.appendChild(sh);document.body.appendChild(mask);
 mask.onclick=function(e){if(e.target===mask)closeSheet();};
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');});});
};

/* ---------- 课程：图标 / 说明同步 / 内置课程可删除 ---------- */
var BUILTIN=COURSES.filter(function(c){return !c.custom;});
function applyCoOverrides(){
 COURSES.forEach(function(co){
  if(co.desc0==null)co.desc0=co.desc;
  var m=(S.ui&&S.ui.co&&S.ui.co[co.id])||{};
  if(m.glyph)co.glyph=m.glyph;
  if(m.desc)co.desc=m.desc;
 });
}
syncCourses=function(){
 var del=(S.ui&&S.ui.deletedCourses)||[];
 COURSES.length=0;
 BUILTIN.forEach(function(c){if(del.indexOf(c.id)<0)COURSES.push(c);});
 ((S.ui&&S.ui.userCourses)||[]).forEach(function(c){
  if(!c||!c.id||COURSES.some(function(x){return x.id===c.id;}))return;
  if(!c.pack){c.pack=normalizePack(c);delete c.chapters;}
  c.custom=true;COURSES.push(c);
 });
 applyCoOverrides();
};
function setGlyph(co,g){
 if(!co||!g)return;
 co.glyph=g;S.ui.co=S.ui.co||{};(S.ui.co[co.id]=S.ui.co[co.id]||{}).glyph=g;
}
function deleteCourse(id){
 var co=COURSES.find(function(c){return c.id===id;});if(!co)return false;
 var next=COURSES.find(function(c){return c.id!==id;});if(!next)return false;
 S.ui=S.ui||{};
 if(co.custom)S.ui.userCourses=(S.ui.userCourses||[]).filter(function(x){return x.id!==id;});
 else{S.ui.deletedCourses=S.ui.deletedCourses||[];if(S.ui.deletedCourses.indexOf(id)<0)S.ui.deletedCourses.push(id);}
 if(S.ui.co)delete S.ui.co[id];
 if(S.ui.star)delete S.ui.star[id];
 if(Array.isArray(S.ui.shortcuts))S.ui.shortcuts=S.ui.shortcuts.filter(function(it){return it.target!=='course:'+id;});
 try{if(co.media&&window.mediaPurge)mediaPurge(id,packOf(co).ch.map(function(c){return c.id;}));idbDel('dir:'+id).catch(function(){});}catch(e){}
 Object.keys(MEDIA).forEach(function(k){if(k.indexOf(id+':')===0)delete MEDIA[k];});
 if(S.cur===id)S.cur=next.id;
 if(S.cp)delete S.cp[id];
 syncCourses();
 curBag();loadPack(S.cur);
 miniShown=false;
 save();
 if(view==='course'||view==='today'||view==='player'||view==='know'||view==='review'||view==='case'||view==='gloss'||view==='play'||view==='prog'||view==='io'){navStack=[];view='mine';}
 render();
 return true;
}
function confirmDeleteCourse(co){
 var m=coMeta(co);
 if(COURSES.length<=1){toast('至少需要保留一门课程');return;}
 uiConfirm('删除「'+m.title+'」及它的进度、划线与案例？无法恢复。',function(){
  closeSheet();
  if(deleteCourse(co.id))toast('课程已删除');
 },{ok:'删除',danger:true,title:'删除课程'});
}

/* 给已有的媒体课程追加 / 重新连接文件 */
function addMediaItems(co,items,dirHandle){
 var p=packOf(co),have={};
 p.ch.forEach(function(c){if(c.media){have[c.media.rel]=1;have[c.media.fname+'|'+c.media.size]=1;}});
 var linked=attachFiles(co.id,items);
 var fresh=items.filter(function(it){return !(have[it.rel]||have[it.fname+'|'+it.size]);});
 var by=(co.sort&&co.sort.by)||'name',dir=(co.sort&&co.sort.dir)||'asc';
 sortMediaItems(fresh,by,dir);
 var nums=p.ch.map(function(c){return parseInt(c.id,10);}).filter(function(n){return isFinite(n);});
 var n=nums.length?Math.max.apply(null,nums):p.ch.length;
 var wd=Math.max(2,String(p.ch.length+fresh.length).length);
 var rk=p.routes.all?'all':Object.keys(p.routes)[0];
 fresh.forEach(function(it){
  var mn=it.mod||'全部',mod=p.mods.find(function(m){return m.n===mn;});
  if(!mod){var used={};p.mods.forEach(function(m){used[m.k]=1;});var k,i=0;do{k=i<26?String.fromCharCode(65+i):'M'+(i+1);i++;}while(used[k]);mod={k:k,n:mn,tag:''};p.mods.push(mod);}
  var id;do{n++;id=String(n).padStart(wd,'0');}while(p.ch.some(function(c){return c.id===id;}));
  p.ch.push({id:id,m:mod.k,t:it.fname,s:fmtSize(it.size),terms:'',plain:[],cmds:[],pits:[],gate:'能用自己的话讲清本章内容',quiz:{q:'用自己的话复述本章最重要的三点。',a:'对照章节内容自查：能讲出来才算过关。',hint:''},media:{kind:co.media,fname:it.fname,rel:it.rel,size:it.size,mtime:it.mtime,dur:0}});
  if(rk){p.routes[rk]=p.routes[rk]||[];p.routes[rk].push(id);}
  MEDIA[co.id+':'+id]={file:it.file};
 });
 if(dirHandle)idbSet('dir:'+co.id,dirHandle).catch(function(){});
 if(co.id===S.cur)loadPack(co.id);
 save();
 return {linked:linked,added:fresh.length};
}

window.addMediaItems=addMediaItems;
openCourseEditor=function(co){
 var m=coMeta(co),cover=m.cover,glyph=co.glyph||'book';
 var body=h('div'),row=h('div','covedit'),prev=h('div');
 function paint(){
  var nw=h('div','cover '+(cover?'img':co.cover));
  if(cover)nw.style.backgroundImage='url("'+cover+'")';else nw.innerHTML=ic(glyph);
  prev.replaceChildren(nw);
 }
 paint();row.appendChild(prev);
 var cb=h('div','cbt');
 var up=h('button','btn');up.innerHTML=ic('image')+' 上传封面';
 up.querySelector('svg').style.cssText='width:16px;height:16px;vertical-align:-3px;margin-right:2px';
 up.onclick=function(){pickImage(480,function(d){cover=d;paint();});};
 var rm=h('button','btn ghost','恢复默认封面');rm.onclick=function(){cover='';paint();};
 cb.appendChild(up);
 if(co.media==='video'){var fr=h('button','btn');fr.innerHTML=ic('g_camera')+' 截取视频画面';fr.querySelector('svg').style.cssText='width:16px;height:16px;vertical-align:-3px;margin-right:2px';fr.onclick=function(){openFrameModal(co,function(d){cover=d;paint();});};cb.appendChild(fr);}
 cb.appendChild(rm);row.appendChild(cb);body.appendChild(row);
 var ti=document.createElement('input');ti.type='text';ti.value=m.title;ti.maxLength=40;ti.placeholder='课程名称';
 var su=document.createElement('textarea');su.value=m.sub;su.maxLength=200;su.placeholder='一句话介绍这门课程';
 body.appendChild(field('课程名称',ti));
 var fs=field('课程简介',su);body.appendChild(fs);
 var fg=stSection(ic('layers'),'课程图标');fg.appendChild(iconGrid(glyph,function(k){glyph=k;paint();},COURSE_GLYPHS));body.appendChild(fg);
 if(co.media==='video'){var pr=h('button','io-card ce-poster');pr.type='button';var pi=h('span','ico');pi.innerHTML=ic('g_camera');pr.appendChild(pi);var px=h('span','tx');px.appendChild(h('b',null,'视频封面'));px.appendChild(h('small',null,'取视频画面 / 默认图标'));pr.appendChild(px);var pc=h('span','chv');pc.innerHTML=ic('chevDown');pr.appendChild(pc);pr.onclick=function(){setTimeout(function(){openPosterSheet(co);},80);};body.appendChild(pr);}
 openSheet('编辑课程',body,function(){
  var t=ti.value.trim();
  if(!t){toast('课程名称不能为空');return false;}
  var sub=su.value.trim();
  S.ui.co=S.ui.co||{};
  var rec=S.ui.co[co.id]||{};
  if(co.desc0==null)co.desc0=co.desc;
  rec.title=t===co.title?'':t;
  rec.sub=sub===co.sub?'':sub;
  rec.cover=cover;rec.glyph=glyph;
  rec.desc=(sub&&sub!==co.sub)?sub:'';
  co.glyph=glyph;co.desc=rec.desc||co.desc0;
  S.ui.co[co.id]=rec;
  save();render();toast('课程信息已保存');
 },'保存',{cancelText:'删除',danger:true,cls:'fixh',onCancel:function(){confirmDeleteCourse(co);}});
};

/* ---------- 课程管理：只放课程（文字 / 视频 / 语音），右上角只有导入 ---------- */
function cmRow(co){
 var m=coMeta(co),r=h('div','course-manage-row');
 r.appendChild(coverEl(co,'sm'));
 var ci=h('div','ci');ci.appendChild(h('b',null,m.title));ci.appendChild(h('small',null,m.sub||'课程简介尚未填写'));
 var pg=progOf(co);ci.appendChild(h('small',null,pg.chs+' 章 · '+pg.done+'/'+pg.total+' 已过关'));
 r.appendChild(ci);
 var act=h('div','cmrow-act');act.appendChild(iconBtn('pen','编辑课程',function(){openCourseEditor(co);}));r.appendChild(act);
 r.onclick=function(e){if(e.target.closest('button'))return;activeCourse=co.id;cdSeg='chs';nav('course');};
 return r;
}
renderCourseManage=function(){
 var box=$('v-course-manage');box.textContent='';
 var cnt={text:0,video:0,audio:0};COURSES.forEach(function(c){cnt[courseType(c)]++;});
 var tab=S.ui.cmTab;
 if(!tab||(!cnt[tab]&&(cnt.text||cnt.video||cnt.audio)&&tab==='text'))tab=cnt.text?'text':(cnt.video?'video':(cnt.audio?'audio':'text'));
 var bar=h('div','cm-tabs'),list=h('div','stagger'),empty=h('p','cm-empty');
 box.appendChild(bar);box.appendChild(list);box.appendChild(empty);
 var EMPTY={text:'还没有文字课程。点右上角的导入图标，粘贴或选择课程文件即可导入。',video:'还没有视频课程。点右上角的导入图标，切到「视频」读取手机里的视频文件夹。',audio:'还没有语音课程。点右上角的导入图标，切到「语音」读取手机里的音频文件夹。'};
 function apply(){
  S.ui.cmTab=tab;bar.textContent='';
  [['text','文字'],['video','视频'],['audio','语音']].forEach(function(t){
   var b=h('button',tab===t[0]?'on':'');b.appendChild(document.createTextNode(t[1]));b.appendChild(h('i',null,String(cnt[t[0]])));
   b.onclick=function(){tab=t[0];apply();};bar.appendChild(b);
  });
  list.textContent='';
  COURSES.filter(function(c){return courseType(c)===tab;}).forEach(function(c){list.appendChild(cmRow(c));});
  empty.textContent=cnt[tab]?'':EMPTY[tab];empty.style.display=cnt[tab]?'none':'';
 }
 apply();
};
tidyCourseManage=function(){};
var __ra=renderAppbar;
renderAppbar=function(){
 __ra.apply(this,arguments);
 var ac=$('abActions');
 if(view==='course-manage'&&ac){ac.textContent='';ac.appendChild(iconBtn('importCourse','导入课程',function(){nav('course-import');}));}
};

/* ---------- 新页面：导入课程 / 个人数据 ---------- */
var stage=document.querySelector('main.stage');
['v-course-import','v-personal-data'].forEach(function(id){if(!document.getElementById(id)){var s=document.createElement('section');s.id=id;s.className='hide';stage.appendChild(s);}});
PAGES.push('course-import','personal-data');
TITLES['course-import']='导入课程';TITLES['personal-data']='个人数据';
var ci={tab:'text',text:'',glyph:'book',m:{}};
var __nav=nav;
nav=function(v){if(v==='course-import'&&view!=='course-import'){ci={tab:'text',text:'',glyph:'book',m:{}};}return __nav.apply(this,arguments);};

function importDone(kind,msg){
 S.ui.cmTab=kind;save();
 popCheck(msg);
 navBack();
}
function renderCourseImport(){
 var box=$('v-course-import');box.textContent='';
 var wrap=h('div','ci-page');box.appendChild(wrap);
 var bar=h('div','cm-tabs'),pane=h('div','ci-pane');wrap.appendChild(bar);wrap.appendChild(pane);
 function tabs(){
  bar.textContent='';
  [['text','文字'],['video','视频'],['audio','语音']].forEach(function(t){
   var b=h('button',ci.tab===t[0]?'on':'',t[1]);
   b.onclick=function(){ci.tab=t[0];tabs();paintPane();};bar.appendChild(b);
  });
 }
 function paintPane(){pane.textContent='';if(ci.tab==='text')textPane();else mediaPane(ci.tab);}
 function glyphField(get,set,note){
  var f=stSection(ic('layers'),'课程图标');f.appendChild(iconGrid(get(),set,COURSE_GLYPHS));return f;
 }
 function textPane(){
  var ta=document.createElement('textarea');ta.className='io-ta';ta.placeholder='粘贴课程档案（JSON / Markdown / CSV），或选择文件';ta.value=ci.text;
  ta.oninput=function(){ci.text=ta.value;};
  pane.appendChild(ta);
  var row=h('div','ci-row'),pick=h('button','btn','选择文件');
  pick.onclick=function(){var f=document.createElement('input');f.type='file';f.accept='.json,.md,.csv,.js,.txt,application/json,text/markdown,text/csv,text/javascript,application/javascript,text/plain';
   f.onchange=function(){var file=f.files&&f.files[0];if(!file)return;var rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');ci.text=ta.value;};rd.readAsText(file);};f.click();};
  row.appendChild(pick);pane.appendChild(row);
  pane.appendChild(glyphField(function(){return ci.glyph;},function(k){ci.glyph=k;}));
  var fold=guideFold({scope:'course',scopes:false});fold.style.marginTop='14px';pane.appendChild(fold);
  var go=h('button','btn pri ci-go','导入课程');
  go.onclick=function(){
   var res=parseArchiveText(ta.value);
   if(res.err){toast(res.err.length>26?res.err.slice(0,26)+'…':res.err);return;}
   if(!isFullScope(res.obj)){toast('这是局部档案（划线 / 进度 / 案例），请到「我的 → 数据 → 导入档案」导入');return;}
   var r=applyArchive(res.obj,'merge');
   if(!r.created.length){toast('没有可导入的课程（需要 title）');return;}
   r.created.forEach(function(id){setGlyph(COURSES.find(function(c){return c.id===id;}),ci.glyph);});
   importDone('text','已导入 '+r.created.length+' 门课程');
  };
  pane.appendChild(go);
 }
 function mediaPane(kind){
  var KN=kind==='video'?'视频':'音频';
  var st=ci.m[kind]||(ci.m[kind]={items:[],by:'name',dir:'asc',title:'',dirHandle:null,glyph:kind==='video'?'monitor':'speaker'});
  var body=h('div','mi');pane.appendChild(body);
  var btns=h('div','mi-btns'),bf=h('button','btn pri','选择文件夹'),bm=h('button','mi-alt','文件夹读不到？改选单个文件');
  btns.appendChild(bf);body.appendChild(btns);body.appendChild(bm);
  var info=h('p','mi-info');body.appendChild(info);
  var nameInp=document.createElement('input');nameInp.type='text';nameInp.placeholder='课程名称';nameInp.value=st.title;nameInp.oninput=function(){st.title=nameInp.value.trim();};
  var nameF=field('课程名称',nameInp);body.appendChild(nameF);
  var sortBar=h('div','mi-sort');body.appendChild(sortBar);
  var list=h('div','mi-list');body.appendChild(list);
  if(false)body.appendChild(h('p','mi-note',kind==='video'?'选择后会按文件名自然排序（1、2、10 排在正确位置）。文件只在本机读取，不会上传。mkv / avi 等格式部分手机浏览器无法播放，推荐 mp4。':'选择后会按文件名自然排序（1、2、10 排在正确位置）。文件只在本机读取，不会上传。推荐 mp3 / m4a。'));
  body.appendChild(glyphField(function(){return st.glyph;},function(k){st.glyph=k;}));
  var keepHolder=h('div');body.appendChild(keepHolder);
  var go=h('button','btn pri ci-go','导入'+KN+'课程');body.appendChild(go);
  var keepBox=null;
  function paint(){
   var has=st.items.length>0;
   info.textContent=has?('已读取 '+st.items.length+' 个'+KN+'文件 · 共 '+fmtSize(st.items.reduce(function(s,x){return s+x.size;},0))):'';
   nameF.style.display=sortBar.style.display=has?'':'none';
   sortBar.textContent='';
   [['name','按名称'],['time','按时间']].forEach(function(o){var b=h('button',st.by===o[0]?'on':'',o[1]);b.onclick=function(){st.by=o[0];paint();};sortBar.appendChild(b);});
   sortBar.appendChild(h('span','mi-gap'));
   [['asc','升序'],['desc','降序']].forEach(function(o){var b=h('button',st.dir===o[0]?'on':'',o[1]);b.onclick=function(){st.dir=o[0];paint();};sortBar.appendChild(b);});
   list.textContent='';
   var sorted=sortMediaItems(st.items.slice(),st.by,st.dir);
   sorted.forEach(function(it,i){
    var r=h('div','mi-row');r.appendChild(h('span','mi-n',String(i+1)));r.appendChild(h('span','mi-t',(it.mod?it.mod+' / ':'')+it.fname));
    r.appendChild(h('small',null,st.by==='time'?fmtDate(it.mtime):fmtSize(it.size)));list.appendChild(r);
   });
  }
  function got(res){
   if(!res)return;
   if(!res.items.length){toast('没有在这个文件夹里找到'+KN+'文件；可点下方「改选单个文件」');bm.classList.add('hot');return;}
   st.items=res.items;st.dirHandle=res.dirHandle||null;
   if(!st.title)st.title=res.name||(KN+'课程');
   nameInp.value=st.title;paint();
   keepHolder.textContent='';keepBox=mediaKeepNote(st.items);keepHolder.appendChild(keepBox);
  }
  bf.onclick=function(){pickFolder(kind).then(got);};
  bm.onclick=function(){pickFiles(kind).then(got);};
  go.onclick=function(){
   if(!st.items.length){toast('请先选择文件夹或文件');return;}
   var id=createMediaCourse(kind,st.title||(KN+'课程'),st.items,st.by,st.dir,st.dirHandle);
   var newCo=COURSES.find(function(c){return c.id===id;});
   setGlyph(newCo,st.glyph);
   mediaPersist(newCo,st.items,keepBox?keepBox.get():S.ui.mediaKeep!==false,st.dirHandle);
   delete ci.m[kind];
   setTimeout(function(){probeDurations(id);},300);
   importDone(kind,'已导入'+KN+'课程');
  };
  paint();
 }
 tabs();paintPane();
}

/* 个人数据：全部个人设置与学习数据的导入 / 导出 */
function personalPayload(){
 return {app:AR_APP,kind:'personal-data',ver:1,exportedAt:new Date().toISOString(),
  note:'个人数据：学习进度、划线与备注、案例与术语、自建与导入的课程、头像与昵称、主题与主题色、快捷入口、朗读设置等。视频 / 音频文件本身不在其中，导入后需要重新连接。',
  state:JSON.parse(JSON.stringify(S))};
}
function personalSummary(st){
 var cp=st.cp||{},marks=0,done=0;
 Object.keys(cp).forEach(function(k){var b=cp[k]||{};marks+=(b.marks||[]).length;done+=Object.keys(b.status||{}).filter(function(i){return b.status[i]==='green';}).length;});
 var ui=st.ui||{},uc=(ui.userCourses||[]).length;
 return [['课程',uc+'（自建 / 导入）'],['划线',marks],['已过关章节',done],['昵称',(ui.profile&&ui.profile.name)||'未设置'],['主题',(ui.style==='minimal'?'极简版':'星球版')+' · '+(st.theme==='dark'?'暗色':'亮色')],['快捷入口',(ui.shortcuts||[]).length||'默认']];
}
function exportPersonal(){
 var p=personalPayload();
 openExport({kind:'json',title:'个人数据 · 导出预览',filename:'桌面运维-个人数据-'+today()+'.json',mime:'application/json',text:JSON.stringify(p,null,2),
  hint:'包含全部个人学习数据与个人设置。可在「我的 → 个人数据 → 导入」里恢复。视频 / 音频文件本身不在其中。'});
}
function applyPersonal(obj){
 var st=obj.state;
 try{if(typeof ttsStop==='function')ttsStop();}catch(e){}
 S=Object.assign({theme:'light',ui:{}},st);
 S.ui=S.ui||{};
 initCourseState();
 syncCourses();
 if(!COURSES.some(function(c){return c.id===S.cur;}))S.cur=COURSES[0].id;
 curBag();loadPack(S.cur);
 try{migrateMarks();}catch(e){}
 try{syncThemeMode(false);}catch(e){}
 applyTheme();applySat();
 miniGone=false;miniShown=false;
 save();
 navStack=[];view='profile';
 render();
}
function openPersonalImport(){
 var body=h('div'),ta=document.createElement('textarea');ta.className='io-ta';ta.placeholder='粘贴个人数据 JSON，或选择之前导出的 .json 文件';
 var info=h('p','ci-hint');info.style.margin='10px 2px 0';
 body.appendChild(h('p','ci-hint','选择之前导出的「个人数据」文件。导入会覆盖这台设备上现有的学习数据与个人设置，请先确认。'));
 body.appendChild(ta);
 var row=h('div','ci-row'),pick=h('button','btn','选择文件');
 pick.onclick=function(){var f=document.createElement('input');f.type='file';f.accept='.json,application/json,text/plain';
  f.onchange=function(){var file=f.files&&f.files[0];if(!file)return;var rd=new FileReader();rd.onload=function(){ta.value=String(rd.result||'');parse();};rd.readAsText(file);};f.click();};
 row.appendChild(pick);body.appendChild(row);body.appendChild(info);
 function parse(){
  var r=parsePersonal(ta.value);
  info.textContent=r.err?r.err:'已识别：'+personalSummary(r.obj.state).map(function(x){return x[0]+' '+x[1];}).join(' · ');
  return r;
 }
 ta.oninput=function(){if(ta.value.trim())parse();else info.textContent='';};
 openSheet('导入个人数据',body,function(){
  var r=parse();
  if(r.err){toast(r.err.length>24?r.err.slice(0,24)+'…':r.err);return false;}
  uiConfirm('导入会用文件里的内容覆盖这台设备上现有的学习数据、头像昵称、主题和主题色等全部个人设置，无法撤销。确定导入吗？',function(){
   closeSheet();applyPersonal(r.obj);popCheck('个人数据已导入');
  },{ok:'覆盖导入',danger:true,title:'导入个人数据'});
  return false;
 },'导入');
}
function parsePersonal(raw){
 var t=String(raw||'').replace(/^\uFEFF/,'').trim();
 if(!t)return {err:'内容为空，请选择文件或粘贴内容'};
 var o;try{o=JSON.parse(t);}catch(e){return {err:'JSON 格式有误：'+e.message};}
 if(!o||typeof o!=='object')return {err:'这不是个人数据文件'};
 if(o.kind!=='personal-data'){
  if(o.kind==='portable-archive'||Array.isArray(o.courses))return {err:'这是课程档案，请到「数据 → 导入档案」导入'};
  return {err:'这不是「个人数据」文件'};
 }
 if(!o.state||typeof o.state!=='object'||(!o.state.ui&&!o.state.cp))return {err:'文件里没有可导入的个人数据'};
 return {obj:o};
}
function renderPersonalData(){
 var box=$('v-personal-data');box.textContent='';
 var wrap=h('div','io-page stagger');box.appendChild(wrap);
 var s1=stSection(ic('database'),'个人数据'),o=h('div','st-opts two');
 o.appendChild(stOption('导出',false,exportPersonal,ic('share'),'保存为文件'));
 o.appendChild(stOption('导入',false,openPersonalImport,ic('upload'),'从文件恢复'));
 s1.appendChild(o);wrap.appendChild(s1);
 var s2=stSection(ic('layers'),'包含'),g=h('div','pd-grid');
 [['book','学习进度'],['highlighter','划线备注'],['folder','案例术语'],['user','头像昵称'],['sun','主题配色'],['layers','快捷入口'],['speaker','朗读设置'],['monitor','自建课程']].forEach(function(x){
  var c=h('div','pd-chip'),i=h('span','pi');i.innerHTML=ic(x[0]);c.appendChild(i);c.appendChild(h('span',null,x[1]));g.appendChild(c);
 });
 s2.appendChild(g);wrap.appendChild(s2);
 var s3=stSection(ic('chart'),'当前设备'),chips=h('div','io-chips');
 personalSummary(personalPayload().state).forEach(function(r){chips.appendChild(h('span','pill',r[0]+' '+r[1]));});
 s3.appendChild(chips);wrap.appendChild(s3);
}

var __rd=render;
render=function(){
 var r=__rd.apply(this,arguments);
 if(view==='course-import')renderCourseImport();
 if(view==='personal-data')renderPersonalData();
 return r;
};
var __rt=renderTabbar;
renderTabbar=function(){
 __rt.apply(this,arguments);
 if(view==='course-import'||view==='personal-data'){
  var keys=Object.keys(TABPAGES),orig=TABPAGES[navStack[0]]?navStack[0]:'profile',btns=document.querySelectorAll('#tbin .tbi');
  keys.forEach(function(k,i){if(btns[i])btns[i].classList.toggle('on',k===orig);});
 }
};

/* ---------- 我的：快捷入口下增加「个人数据」 ---------- */
var __rpp=renderProfilePlanet;
renderProfilePlanet=function(){
 __rpp.apply(this,arguments);
 var gs=$('profileBox').querySelectorAll('.stagger>div'),g2=gs[gs.length-1];
 if(g2)pfRow(g2,'database','个人数据','',function(){nav('personal-data');});
};

/* ---------- 数据页：导出只保留「整门课程 · 完整档案」，其余折叠 ---------- */
var ioMore=false;
var __io=renderIo;
renderIo=function(){
 var box=$('v-io');if(!box)return;
 if(ioDetail)return __io.apply(this,arguments);
 box.textContent='';
 var wrap=h('div','stagger');box.appendChild(wrap);
 var s1=stSection(ic('share'),'导出');
 function open(e){ioDetail=e.id;ioFormat='json';render();}
 var big=IO_ENTRIES[0],card=h('button','io-card');card.type='button';
 var ci_=h('span','ico');ci_.innerHTML=ic(big.icon);card.appendChild(ci_);
 var tx=h('span','tx');tx.appendChild(h('b',null,big.title));tx.appendChild(h('small',null,'课程 · 进度 · 划线 · 设置'));card.appendChild(tx);
 var ch=h('span','chv');ch.innerHTML=ic('chevDown');card.appendChild(ch);
 card.onclick=function(){open(big);};s1.appendChild(card);
 var rest=IO_ENTRIES.slice(1);
 var fold=h('button','io-fold'+(ioMore?' on':''));fold.type='button';
 fold.appendChild(h('b',null,'更多'));fold.appendChild(h('small',null,String(rest.length)));
 var chv=h('i');chv.style.display='contents';chv.innerHTML=ic('chevDown');fold.appendChild(chv);
 var more=h('div','st-opts two io-more'+(ioMore?' on':''));
 rest.forEach(function(e){more.appendChild(stOption(e.title,false,function(){open(e);},ic(e.icon)));});
 fold.onclick=function(){ioMore=!ioMore;fold.classList.toggle('on',ioMore);more.classList.toggle('on',ioMore);};
 s1.appendChild(fold);s1.appendChild(more);wrap.appendChild(s1);
 var s2=stSection(ic('upload'),'导入');
 var imp=h('button','io-card');imp.type='button';
 var ii=h('span','ico');ii.innerHTML=ic('upload');imp.appendChild(ii);
 var it=h('span','tx');it.appendChild(h('b',null,'导入档案'));it.appendChild(h('small',null,'JSON · Markdown · CSV'));imp.appendChild(it);
 var c2=h('span','chv');c2.innerHTML=ic('chevDown');imp.appendChild(c2);
 imp.onclick=function(){impState={text:'',res:null,mode:'merge',fname:''};ioDetail='import';render();};
 s2.appendChild(imp);wrap.appendChild(s2);
};

/* =====================================================================
   朗读：在线来源 / 音色弹窗主题化、在线语音多重兜底、断点续读、选中后从此处朗读
   ===================================================================== */
TTS.dead={};TTS.forceEng='';
/* 在线来源：新增两个备用源，并按「优先、可用」排序 */
(function(){
 var enc=encodeURIComponent;
 var extra=[
  {id:'baidufy',name:'百度翻译',url:function(t,c,spd){return 'https://fanyi.baidu.com/gettts?lan=zh&text='+enc(t)+'&spd='+Math.max(1,Math.min(9,spd))+'&source=web';}},
  {id:'youdao2',name:'有道（备用）',url:function(t){return 'https://tts.youdao.com/fanyivoice?word='+enc(t)+'&le=zh&keyfrom=speaker-target';}}
 ];
 var by={};TTS_PROVIDERS.forEach(function(p){by[p.id]=p;});extra.forEach(function(p){by[p.id]=p;});
 var order=['baidu','baidufy','youdao','youdao2','sogou','baidu2','google'];
 TTS_PROVIDERS.splice(0,TTS_PROVIDERS.length);
 order.forEach(function(k){if(by[k])TTS_PROVIDERS.push(by[k]);});
})();
/* 单段文字的在线播放：逐个来源尝试；连续失败的来源本次先跳过，避免每段都白等 */
playOnlineText=function(text,tok,hooks,fail){
 var cfg=ttsCfg(),spd=5,rate=Math.max(0.6,Math.min(1.6,Number(cfg.rate)||1)),list=provList().filter(function(p){return (TTS.dead[p.id]||0)<2;});
 if(!list.length){TTS.dead={};list=provList();}
 var k=0;
 function tryNext(){
  if(tok!==TTS.tok)return;
  if(k>=list.length){fail('all');return;}
  var p=list[k++],a=ttsAudio(),settled=false,started=false,timer=0;
  function clean(){a.onplaying=a.onended=a.onerror=null;clearTimeout(timer);}
  function bad(){if(settled)return;settled=true;clean();try{a.pause();}catch(e){}TTS.dead[p.id]=(TTS.dead[p.id]||0)+1;tryNext();}
  a.onplaying=function(){if(tok!==TTS.tok)return;started=true;clearTimeout(timer);TTS.dead[p.id]=0;if(hooks.onstart)hooks.onstart();};
  a.onended=function(){if(settled)return;settled=true;clean();cfg.provOk=p.id;TTS.dead[p.id]=0;if(tok===TTS.tok)hooks.onend();};
  a.onerror=function(){if(tok===TTS.tok)bad();};
  try{a.defaultPlaybackRate=rate;a.src=p.url(text,cfg,spd);a.playbackRate=rate;try{a.preservesPitch=true;a.webkitPreservesPitch=true;}catch(x){}var pr=a.play();if(pr&&pr.catch)pr.catch(function(){if(!started&&!settled&&tok===TTS.tok)bad();});}catch(e){bad();return;}
  timer=setTimeout(function(){if(tok===TTS.tok&&!started&&!settled)bad();},7000);
 }
 tryNext();
};
/* 在线不可用时：先退回系统朗读，再退回专注阅读 */
var __eng=ttsEngineNow;
ttsEngineNow=function(){if(TTS.on&&TTS.forceEng)return TTS.forceEng;return __eng.apply(this,arguments);};
var __fb=ttsFallback;
ttsFallback=function(from){
 if(from==='online'&&TTS.on&&ttsSupported()&&!TTS.fail.system&&TTS.forceEng!=='system'){
  TTS.tok++;ttsHaltAudio();
  var cur=TTS.q[TTS.i];TTS.fail.online=true;TTS.forceEng='system';
  toast('在线语音连不上，已改用系统朗读');
  if(cur){var q=ttsBuildQueue(cur.b,'system');var drop=0;while(drop<q.length&&q[drop].off+q[drop].text.length<=cur.off)drop++;if(drop<q.length&&q[drop].off<cur.off){q[drop].text=q[drop].text.slice(cur.off-q[drop].off);q[drop].off=cur.off;}TTS.q=q.slice(drop);TTS.i=0;}
  ttsSpeak();return;
 }
 TTS.forceEng='';
 return __fb.apply(this,arguments);
};
/* 给每个朗读分段记下它在所属段落里的字符位置，用来断点续读 / 从选中处开始 */
var __bq=ttsBuildQueue;
ttsBuildQueue=function(fromBlock,eng){
 var q=__bq.apply(this,arguments),cum=0,lastB=-1;
 q.forEach(function(it){if(it.b!==lastB){lastB=it.b;cum=0;}it.off=cum;cum+=it.text.length;});
 return q;
};
function buildQueueAt(b,off,eng){
 var q=ttsBuildQueue(b,eng);
 if(off>0){
  var drop=0;
  while(drop<q.length&&q[drop].b===b&&q[drop].off+q[drop].text.length<=off)drop++;
  if(drop<q.length&&q[drop].b===b&&q[drop].off<off){q[drop].text=q[drop].text.slice(off-q[drop].off);q[drop].off=off;}
  q=q.slice(drop);
 }
 return q.length?q:ttsBuildQueue(b,eng);
}
var posSaved=0;
function savePos(item,force){
 if(!item||!TTS.key)return;
 S.ui.ttsPos=S.ui.ttsPos||{};
 S.ui.ttsPos[TTS.key]={b:item.b,o:item.off||0,s:String(item.text).slice(0,16)};
 var now=Date.now();if(force||now-posSaved>5000){posSaved=now;save();}
}
function getPos(blocks){
 var pos=(S.ui&&S.ui.ttsPos&&S.ui.ttsPos[readKey()])||null;if(!pos)return null;
 var bi=pos.b,o=pos.o||0;
 if(!(blocks[bi]&&blocks[bi].text.substr(o,pos.s.length)===pos.s)){
  bi=-1;for(var i=0;i<blocks.length;i++){var j=blocks[i].text.indexOf(pos.s);if(j>=0){bi=i;o=j;break;}}
 }
 return bi>=0?{b:bi,o:o,s:pos.s}:null;
}
var __sp=ttsSpeak;
ttsSpeak=function(){
 if(TTS.on&&!TTS.paused){
  if(TTS.i>=TTS.q.length){if(S.ui.ttsPos&&TTS.key){delete S.ui.ttsPos[TTS.key];save();}TTS.forceEng='';}
  else savePos(TTS.q[TTS.i]);
 }
 return __sp.apply(this,arguments);
};
var __stop=ttsStop;
ttsStop=function(){
 if(TTS.on&&TTS.q&&TTS.q[TTS.i])savePos(TTS.q[TTS.i],true);
 TTS.forceEng='';
 return __stop.apply(this,arguments);
};
var __tg=ttsToggle;
ttsToggle=function(){
 if(TTS.on&&!TTS.paused&&TTS.q[TTS.i])savePos(TTS.q[TTS.i],true);
 return __tg.apply(this,arguments);
};
ttsStart=function(o){
 if(o&&(o.target||o.preventDefault))o=null;
 o=o||{};
 var blocks=ttsBlocks();if(!blocks.length){toast('这一章没有可朗读的文字');return;}
 var from=-1,off=0,resumed=false;
 if(o.fromSel){from=o.b;off=o.off||0;}
 else if(!o.fresh){var ps=getPos(blocks);if(ps){from=ps.b;off=ps.o;resumed=true;}}
 if(from<0){from=blocks.findIndex(function(b){return b.el.getBoundingClientRect().bottom>120;});if(from<0)from=0;off=0;}
 TTS.blocks=blocks;TTS.fail={system:false,online:false};TTS.forceEng='';
 if(ttsEngineNow()==='reader'){openFocusReader(blocks,from);return;}
 var eng=ttsEngineNow();
 if(typeof ttsUnlock==='function')ttsUnlock();
 try{if(ttsSupported())speechSynthesis.cancel();}catch(e){}
 TTS.on=true;TTS.paused=false;TTS.key=readKey();TTS.q=buildQueueAt(from,off,eng);TTS.i=0;TTS.tok++;
 renderAppbar();setTimeout(ttsSpeak,60);
 if(resumed)toast('从上次读到的位置继续');
 else if(o.fromSel)toast('从选中的位置开始朗读');
};
/* 选中一段文字 → 点「听当前」 */
var lastSel=null;
function selInfo(){
 var sel=window.getSelection();
 if(!sel||!sel.rangeCount||sel.isCollapsed)return null;
 var r=sel.getRangeAt(0),cb=$('cardBox');
 if(!cb||!cb.contains(r.startContainer))return null;
 var blocks=ttsBlocks(),bi=-1,best=1e9;
 blocks.forEach(function(b,i){if(b.el.contains(r.startContainer)&&b.text.length<best){best=b.text.length;bi=i;}});
 if(bi<0){
  for(var i=0;i<blocks.length;i++){var pos=r.startContainer.compareDocumentPosition(blocks[i].el);if(pos&Node.DOCUMENT_POSITION_FOLLOWING){bi=i;break;}}
  if(bi<0)return null;
  return {b:bi,off:0};
 }
 var pre=document.createRange();pre.selectNodeContents(blocks[bi].el);pre.setEnd(r.startContainer,r.startOffset);
 var off=pre.toString().replace(/\s+/g,' ').replace(/^\s+/,'').length;
 return {b:bi,off:Math.min(off,Math.max(0,blocks[bi].text.length-1))};
}
function listenSelection(){
 var info=selInfo()||lastSel;lastSel=null;
 if(!info){toast('先在正文里选中一段文字');return;}
 try{hideBar();}catch(e){}
 try{var s=window.getSelection();if(s)s.removeAllRanges();}catch(e){}
 try{pendRec=null;}catch(e){}
 if(TTS.on){var was=TTS.on;TTS.on=false;TTS.tok++;ttsHaltAudio();TTS.on=was;ttsStop();}
 ttsStart({fromSel:true,b:info.b,off:info.off});
}
var __eb=ensureBar;
ensureBar=function(){
 var b=__eb.apply(this,arguments);
 var t=b.querySelector('.x.tts');
 if(!t){
  t=h('button','x tts','听当前');t.type='button';t.title='从选中的位置开始朗读';
  var cap=function(){lastSel=selInfo();};
  t.addEventListener('pointerdown',cap);t.addEventListener('touchstart',cap,{passive:true});t.addEventListener('mousedown',function(e){cap();e.preventDefault();});
  t.onclick=listenSelection;
  var first=b.querySelector('.x');b.insertBefore(t,first||null);
 }
 t.style.display=(view==='today')?'':'none';
 return b;
};
/* 朗读设置：来源 / 音色改成主题化弹层 */
openTtsSettings=function(){
 var cfg=ttsCfg(),body=h('div','tts-set');
 var eRow=h('div','st-opts two');
 [['auto','自动','系统不可用时自动换'],['system','系统语音','手机自带 TTS'],['online','在线语音','联网，多来源自动切换'],['reader','专注阅读','不发声，逐段阅读']].forEach(function(o){
  var b=stOption(o[1],cfg.engine===o[0],function(){cfg.engine=o[0];save();eRow.querySelectorAll('.st-opt').forEach(function(x){x.classList.remove('on');});b.classList.add('on');paintVoices();},null,o[2]);
  eRow.appendChild(b);
 });
 body.appendChild(field('朗读方式',eRow));
 var vBox=h('div','tts-voices');body.appendChild(vBox);
 function paintVoices(){
  vBox.textContent='';
  var e=cfg.engine;
  if(e==='auto'||e==='system'){
   var vs=[];try{vs=ttsSupported()?speechSynthesis.getVoices():[];}catch(x){}
   var zh=vs.filter(function(v){return /zh|cmn|yue/i.test(v.lang);});
   var o0=zh.length?'自动选择中文音色':(ttsSupported()?'（暂未读到音色，使用系统默认）':'（此浏览器不支持系统朗读）');
   var sysItems=function(){return [['',o0]].concat(zh.map(function(v){return [v.voiceURI||v.name,v.name+' · '+v.lang];}));};
   vBox.appendChild(pkField('系统音色',function(){var it=sysItems().find(function(x){return x[0]===(cfg.voice||'');});return it?it[1]:o0;},sysItems,function(){return cfg.voice||'';},function(v){cfg.voice=v;save();}));
  }
  if(e==='auto'||e==='online'){
   var provItems=function(){return [['auto','自动（哪个能用用哪个）']].concat(TTS_PROVIDERS.map(function(p){return [p.id,p.name];}));};
   vBox.appendChild(pkField('在线来源',function(){var it=provItems().find(function(x){return x[0]===(cfg.provider||'auto');});return it?it[1]:'自动';},provItems,function(){return cfg.provider||'auto';},function(v){cfg.provider=v;TTS.dead={};save();}));
   var vcItems=function(){return ONLINE_VOICES.map(function(v){return [String(v[0]),v[1]];});};
   vBox.appendChild(pkField('在线音色（仅百度来源可选）',function(){var it=vcItems().find(function(x){return x[0]===String(cfg.per||0);});return it?it[1]:vcItems()[0][1];},vcItems,function(){return String(cfg.per||0);},function(v){cfg.per=Number(v);save();}));
   var pr=h('div','tp-list'),pb=h('button','btn sm','检测在线语音');pb.onclick=function(){ttsUnlock();TTS.dead={};ttsProbe(pr,pb);};
   vBox.appendChild(pb);vBox.appendChild(pr);
  }
  if(e!=='reader'){
   var rg=document.createElement('input');rg.type='range';rg.min='0.6';rg.max='1.6';rg.step='0.1';rg.value=String(cfg.rate||1);
   var lab=h('span','tts-rate',(cfg.rate||1).toFixed(1)+'×');
   rg.oninput=function(){cfg.rate=Number(rg.value);lab.textContent=cfg.rate.toFixed(1)+'×';};rg.onchange=function(){save();if(TTS.on&&!TTS.paused){TTS.tok++;ttsHaltAudio();setTimeout(ttsSpeak,220);}};
   var rw=h('div','tts-rw');rw.appendChild(rg);rw.appendChild(lab);vBox.appendChild(field('语速',rw));
   var pv=h('button','btn sm','试听');pv.onclick=ttsPreview;vBox.appendChild(pv);
  }
  vBox.appendChild(h('p','muted',(e==='online'||e==='auto')?'在线语音会把朗读的文字发送到第三方语音服务（百度 / 有道 / 搜狗 / 谷歌），需要联网；在线都连不上时先改用系统朗读，仍不行再改为专注阅读。':'系统语音不会联网。'));
 }
 paintVoices();
 /* 朗读位置：仅在章节学习页显示 */
 if(view==='today'){
  var ps=null;try{ps=getPos(ttsBlocks());}catch(e){}
  var pos=h('div','tts-pos');
  pos.appendChild(h('div','tp-t',ps?'上次读到：「'+ps.s+'…」，点朗读按钮会从这里接着读。':'点朗读按钮会从当前屏幕开始读；中途停下后，下次会从停下的位置接着读。'));
  var row=h('div','row'),fr=h('button','btn sm','从头朗读本章');
  fr.onclick=function(){closeSheet();ttsStop();setTimeout(function(){if(S.ui.ttsPos&&readKey())delete S.ui.ttsPos[readKey()];ttsStart({fresh:true});},60);};
  row.appendChild(fr);pos.appendChild(row);body.appendChild(pos);
 }
 try{if(ttsSupported()){speechSynthesis.onvoiceschanged=function(){if(vBox.isConnected)paintVoices();};}}catch(e){}
 openSheet('朗读设置',body,function(){ttsStop();},'完成');
};
/* 有断点时，朗读按钮提示「继续朗读」 */
var __ta=ttsAppbar;
ttsAppbar=function(){
 __ta.apply(this,arguments);
 if(view==='today'&&!TTS.on){
  var bt=document.querySelector('#abActions button[aria-label="朗读本章"]');
  try{if(bt&&getPos(ttsBlocks())){bt.setAttribute('aria-label','继续朗读');bt.title='从上次读到的位置继续';}}catch(e){}
 }
};

/* ---------- 启动：应用「已删除的内置课程」与课程图标 / 说明 ---------- */
syncCourses();
if(!COURSES.some(function(c){return c.id===S.cur;}))S.cur=COURSES[0].id;
curBag();loadPack(S.cur);
render();
})();
