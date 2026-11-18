/* Original extension layer v16; preserved in source order. */

/* =====================================================================
   v16 · 切页行为 / 整页划线 / 媒体条目菜单与导入拆分 / 快捷入口重做
   ===================================================================== */
(function(){

/* ---------- 1. 视频切到其他界面立即暂停；朗读切到其他界面继续读 ---------- */
ttsGuard=function(){if(TTS.on&&view==='today'&&readKey()!==TTS.key)ttsStop();};
function updateTtsPill(){if(window.updateFloat)updateFloat();}
var __rd2=render;
render=function(){
 var r=__rd2.apply(this,arguments);
 if(view!=='player'){
  document.querySelectorAll('video').forEach(function(v){try{if(!v.paused)v.pause();}catch(e){}});
 }
 updateTtsPill();
 return r;
};
['ttsStop','ttsToggle'].forEach(function(n){
 var f=window[n];
 window[n]=function(){var r=f.apply(this,arguments);try{updateTtsPill();}catch(e){}return r;};
});

/* ---------- 2. 划线：整个文字页（含表格、列表、说明等）都能选中划线 ---------- */
var BLOCKY=['TD','TH','TR','P','DIV','UL','OL','LI','TABLE','THEAD','TBODY','TR','H1','H2','H3','H4','H5','H6','SECTION','PRE','BLOCKQUOTE','DL','DT','DD','FIGURE'];
var SKIP=/^(SCRIPT|STYLE|BUTTON|SELECT|TEXTAREA|INPUT|SVG|IFRAME|VIDEO|AUDIO|CANVAS|NOSCRIPT|PRE)$/i;
function autoTag(root){
 if(!root)return;
 var chId=null,f=root.querySelector('[data-ch]');
 if(f)chId=f.dataset.ch;
 if(!chId){try{chId=String((S.pin&&chById(S.pin)&&S.pin)||cur().id);}catch(e){}}
 if(!chId)return;
 var n=0;
 (function walk(el){
  Array.prototype.forEach.call(el.children,function(c){
   if(SKIP.test(c.tagName)||c.hasAttribute('data-blk'))return;
   if(c.closest('.btns,.modeRow,.selbar,.no-annot,.media-ctl'))return;
   var blockChild=false;
   for(var i=0;i<c.children.length;i++){if(BLOCKY.indexOf(c.children[i].tagName)>=0){blockChild=true;break;}}
   if(blockChild||c.querySelector('[data-blk]')){walk(c);return;}
   if(!(c.textContent||'').replace(/\s+/g,'').length)return;
   if(c.querySelector('button,input,select,textarea'))return;
   c.dataset.ch=chId;c.dataset.blk='X'+(n++);
  });
 })(root);
}
var __aa=attachAnnotation;
attachAnnotation=function(){
 try{autoTag(document.getElementById('cardBox'));}catch(e){console.error(e);}
 return __aa.apply(this,arguments);
};

/* ---------- 3. 主题化的输入弹窗 ---------- */
window.uiPrompt=function(title,value,onOk,o){
 o=o||{};
 var old=document.querySelector('.dlg-mask');if(old)old.remove();
 var mask=h('div','dlg-mask'),dlg=h('div','dlg');
 dlg.appendChild(h('h4',null,title));
 var inp=document.createElement('input');inp.type='text';inp.className='dlg-in';inp.value=value||'';inp.placeholder=o.placeholder||'';inp.maxLength=o.max||120;
 dlg.appendChild(inp);
 var ft=h('div','dlg-ft'),c=h('button','btn','取消'),k=h('button','btn pri',o.ok||'保存');
 function close(){mask.classList.remove('on');setTimeout(function(){mask.remove();},200);}
 function go(){var v=inp.value.trim();if(!v){toast('名称不能为空');return;}close();onOk(v);}
 c.onclick=close;k.onclick=go;
 inp.onkeydown=function(e){if(e.key==='Enter'){e.preventDefault();go();}if(e.key==='Escape')close();};
 mask.onclick=function(e){if(e.target===mask)close();};
 ft.appendChild(c);ft.appendChild(k);dlg.appendChild(ft);mask.appendChild(dlg);document.body.appendChild(mask);
 requestAnimationFrame(function(){requestAnimationFrame(function(){mask.classList.add('on');inp.focus();inp.select();});});
};

/* ---------- 4. 视频 / 音频课程：条目的重命名 / 删除 ---------- */
function mediaPackCh(co,id){return packOf(co).ch.find(function(x){return x.id===id;});}
function renameMediaItem(co,c){
 var KN=co.media==='video'?'视频':'音频';
 uiPrompt('重命名'+KN,c.t,function(v){
  var pc=mediaPackCh(co,c.id)||c;pc.t=v;c.t=v;
  save();render();toast('已重命名');
 },{ok:'保存',placeholder:'名称'});
}
function deleteMediaItem(co,c){
 var p=packOf(co),KN=co.media==='video'?'视频':'音频';
 if(p.ch.length<=1){toast('至少保留一集；要删除整门课程，请在「编辑课程」里删除');return;}
 uiConfirm('删除「'+c.t+'」？不会删除手机里的文件。',function(){
  var id=c.id,isCur=co.id===S.cur,curId=null;
  if(isCur){try{curId=((S.pin&&chById(S.pin))||cur()||{}).id;}catch(e){}}
  var i=p.ch.findIndex(function(x){return x.id===id;});if(i>=0)p.ch.splice(i,1);
  Object.keys(p.routes).forEach(function(k){p.routes[k]=p.routes[k].filter(function(x){return x!==id;});});
  var used={};p.ch.forEach(function(x){used[x.m]=1;});
  var keep=p.mods.filter(function(m){return used[m.k];});if(keep.length)p.mods.splice(0,p.mods.length,...keep);
  delete MEDIA[co.id+':'+id];
  if(window.mediaPurge)mediaPurge(co.id,[id]);
  var bag=S.cp&&S.cp[co.id];
  if(bag){if(bag.status)delete bag.status[id];if(bag.learn)delete bag.learn[id];if(Array.isArray(bag.marks))bag.marks=bag.marks.filter(function(m){return m.ch!==id;});if(bag.pin===id)bag.pin='';}
  if(S.ui.mediaPos)delete S.ui.mediaPos[co.id+':'+id];
  if(isCur){
   loadPack(co.id);
   var q=seq(),ix=curId?q.indexOf(curId):-1;
   S.idx=ix>=0?ix:Math.min(Math.max(0,S.idx),Math.max(0,q.length-1));
  }
  save();render();toast('已删除这一'+(KN==='视频'?'集视频':'集音频'));
 },{ok:'删除',danger:true,title:'删除这一集'});
}
window.renameMediaItem=renameMediaItem;window.deleteMediaItem=deleteMediaItem;
var __ocm=openChapterMenu;
openChapterMenu=function(c){
 var co=curCourse();
 if(!co||!co.media)return __ocm.apply(this,arguments);
 openPicker(c.t,[['rename','重命名'],['del','删除','','danger']],null,function(v){
  setTimeout(function(){if(v==='rename')renameMediaItem(co,c);else deleteMediaItem(co,c);},120);
 });
};

/* ---------- 5. 视频 / 音频课程右上角三点：编辑 / 导入 ---------- */
function openMediaImportSheet(co){
 var KN=co.media==='video'?'视频':'音频',p=packOf(co);
 var picked=[],dirHandle=null;
 var body=h('div','mi');
 body.appendChild(h('p','mi-note','选择文件夹或单个文件：已有的集数会重新连接，新的文件追加为新的一集。导入的'+KN+'沿用原来的文件名，不会改名。'));
 var btns=h('div','mi-btns'),bf=h('button','btn pri','选择文件夹'),bm=h('button','btn','选择单个文件');
 btns.appendChild(bf);btns.appendChild(bm);body.appendChild(btns);
 var info=h('p','mi-info');body.appendChild(info);
 var lh=h('div','ml-hd');body.appendChild(lh);
 var list=h('div','ml-list');body.appendChild(list);
 var keepBox=null;
 function keyOf(it){return it.rel+'|'+it.fname+'|'+it.size;}
 function paint(){
  var have={},byRel={},byKey={};
  picked.forEach(function(it){byRel[it.rel]=1;byKey[it.fname+'|'+it.size]=1;});
  list.textContent='';
  var rows=0;
  p.ch.forEach(function(c,i){
   if(!c.media)return;rows++;
   have[c.media.rel]=1;have[c.media.fname+'|'+c.media.size]=1;
   var r=h('div','mi-row');r.appendChild(h('span','mi-n',String(i+1)));
   r.appendChild(h('span','mi-t',c.media.fname||c.t));
   var linked=!!MEDIA[co.id+':'+c.id],again=byRel[c.media.rel]||byKey[c.media.fname+'|'+c.media.size];
   var tag=h('small','ml-tag'+(again?' hot':linked?' ok':''),again?'将重新连接':(linked?'已连接':'未连接'));
   r.appendChild(tag);list.appendChild(r);
  });
  var fresh=picked.filter(function(it){return !(have[it.rel]||have[it.fname+'|'+it.size]);});
  fresh.forEach(function(it,j){
   var r=h('div','mi-row');r.appendChild(h('span','mi-n',String(rows+j+1)));
   r.appendChild(h('span','mi-t',it.fname));r.appendChild(h('small','ml-tag hot','新增 · '+fmtSize(it.size)));list.appendChild(r);
  });
  lh.textContent='课程里的'+KN+' · '+rows+' 个'+(fresh.length?' + 新增 '+fresh.length+' 个':'');
  info.textContent=picked.length?'本次选择了 '+picked.length+' 个文件，其中新增 '+fresh.length+' 个。':'';
  return fresh.length;
 }
 function got(res){
  if(!res)return;
  if(!res.items.length){toast('没有找到'+KN+'文件');return;}
  picked=res.items;dirHandle=res.dirHandle||null;paint();
  if(keepBox)keepBox.remove();keepBox=mediaKeepNote(picked);body.appendChild(keepBox);
 }
 bf.onclick=function(){pickFolder(co.media).then(got);};
 bm.onclick=function(){pickFiles(co.media).then(got);};
 paint();
 openSheet('导入'+KN,body,function(){
  if(!picked.length){toast('请先选择文件夹或文件');return false;}
  var out=addMediaItems(co,picked,dirHandle);
  mediaPersist(co,picked,keepBox?keepBox.get():S.ui.mediaKeep!==false,dirHandle);
  toast(out.added?('已追加 '+out.added+' 集'+(out.linked?'，重新连接 '+out.linked+' 集':'')):'已重新连接 '+out.linked+' 集');
  render();setTimeout(function(){probeDurations(co.id);},300);
 },'导入',{cls:'fixh'});
}
window.openMediaImportSheet=openMediaImportSheet;
function openCourseMenu(co){
 var KN=co.media==='video'?'视频':'音频',anchor=$('abEdit');
 var old=document.querySelector('.apop-mask');if(old)old.remove();
 var mask=h('div','apop-mask'),pop=h('div','apop');
 function close(){mask.classList.remove('on');setTimeout(function(){mask.remove();},160);}
 [['edit','wrench','编辑课程'],['import','importCourse','导入'+KN]].forEach(function(it){
  var b=h('button','apop-i');b.type='button';var i=h('span','ai');i.innerHTML=ic(it[1]);b.appendChild(i);b.appendChild(h('span',null,it[2]));
  b.onclick=function(){close();setTimeout(function(){if(it[0]==='edit')openCourseEditor(co);else openMediaImportSheet(co);},120);};
  pop.appendChild(b);
 });
 mask.appendChild(pop);mask.onclick=function(e){if(e.target===mask)close();};document.body.appendChild(mask);
 var r=anchor.getBoundingClientRect();
 pop.style.top=(r.bottom+6)+'px';pop.style.right=Math.max(8,window.innerWidth-r.right)+'px';
 requestAnimationFrame(function(){mask.classList.add('on');});
}
$('abEdit').onclick=function(){var co=curCourse();if(co&&co.media)openCourseMenu(co);else openCourseEditor(co);};
/* 新导入的视频 / 音频：章节名沿用原文件名 */
var __mc=mediaChapters;
mediaChapters=function(){
 var r=__mc.apply(this,arguments);
 r.chs.forEach(function(c){if(c.media&&c.media.fname)c.title=c.media.fname;});
 return r;
};

/* ---------- 6. 快捷入口：重做 ---------- */
var SC_W=[['auto','自适应','推荐'],['third','1/3 行'],['half','1/2 行'],['full','整行'],['px','自定义']];
var SC_H=[[48,'同搜索框'],[40,'紧凑'],[64,'标准'],[88,'大'],[120,'海报']];
var SC_R=[[0,'直角'],[8,'小'],[12,'默认'],[18,'大'],[99,'胶囊']];
var ROW_W=342;
function scWk(it){var w=it.w;if(typeof w==='number')return 'px';return (w==='third'||w==='half'||w==='full')?w:'auto';}
function scAutoW(it){
 var txt=(it.name||'').length*14.5,row=scH(it)<64;
 var w=txt+(it.img&&!it.name?0:(row?(it.img?0:27):0))+28;
 return Math.round(Math.max(72,Math.min(240,w)));
}
function scRealW(it){var k=scWk(it);return k==='px'?Math.max(48,Number(it.w)):k==='third'?107:k==='half'?166:k==='full'?ROW_W:scAutoW(it);}
function scH(it){return Math.max(32,Number(it.h)||48);}
function scR(it){return (it.r==null||it.r==='')?12:Number(it.r);}
function scFit(box,bg,im,it){
 function fit(){
  var W=box.clientWidth,H=box.clientHeight,iw=im.naturalWidth||it.iw,ih=im.naturalHeight||it.ih;
  if(!W||!H||!iw||!ih)return;
  var p=it.pos||{x:50,y:50,z:1},z=Math.max(1,Number(p.z)||1);
  var s=Math.max(W/iw,H/ih)*z,rw=iw*s,rh=ih*s;
  im.style.width=rw+'px';im.style.height=rh+'px';
  im.style.left=((W-rw)*(p.x==null?50:p.x)/100)+'px';
  im.style.top=((H-rh)*(p.y==null?50:p.y)/100)+'px';
  box.__dim={W:W,H:H,rw:rw,rh:rh};
 }
 box.__fit=fit;
 im.onload=fit;
 if(im.complete)fit();
 requestAnimationFrame(function(){fit();requestAnimationFrame(fit);});
 if(window.ResizeObserver&&!box.__ro){box.__ro=new ResizeObserver(fit);box.__ro.observe(box);}
}
scTile=function(it,mode){
 var h0=scH(it),r0=scR(it),tc=scTextColor(it),wk=scWk(it);
 var b=h('button','scard sc-tile'+(h0<64?' sc-h-row':' sc-h-col')+(it.img?' has-img':'')+(it.color&&!it.img?' has-col':'')+(tc==='#ffffff'?' tc-w':''));
 b.style.height=h0+'px';b.style.minHeight=h0+'px';b.style.borderRadius=(r0>=99?999:r0)+'px';
 b.style.flexShrink='0';b.dataset.fw=wk;
 if(mode==='pv'){b.style.width=scRealW(it)+'px';b.style.flex='none';}
 else if(wk==='third')b.style.flex='0 0 calc((100% - 20px)/3)';
 else if(wk==='half')b.style.flex='0 0 calc((100% - 10px)/2)';
 else if(wk==='full')b.style.flex='0 0 100%';
 else if(wk==='px'){b.style.flex='0 0 '+Math.max(48,Number(it.w))+'px';b.style.width=Math.max(48,Number(it.w))+'px';}
 else{b.style.flex='0 0 auto';b.style.minWidth='72px';}
 if(it.img){
  var bg=h('span','sc-bg'),im=document.createElement('img');im.alt='';im.draggable=false;im.src=it.img;bg.appendChild(im);b.appendChild(bg);
  scFit(b,bg,im,it);
 }else if(it.color)b.style.backgroundColor=it.color;
 if(tc)b.style.setProperty('--sc-tc',tc);
 if(!it.img){var ico=h('span','sc-ico');ico.innerHTML=ic(it.icon||SC_ICON[it.target]||'layers');b.appendChild(ico);}
 var lab=it.name||(it.img?'':'未命名');
 if(lab)b.appendChild(h('span','sc-lab',lab));else b.setAttribute('aria-label','图片入口');
 if(mode===true||mode==='pv')b.tabIndex=-1;else b.onclick=function(){goShortcut(it.target);};
 return b;
};
function scImageLoad(file,cb){
 var rd=new FileReader();
 rd.onload=function(){
  var img=new Image();
  img.onload=function(){
   var W=720,sc=Math.min(1,W/img.width),w=Math.round(img.width*sc),hh=Math.round(img.height*sc);
   var cv=document.createElement('canvas');cv.width=w;cv.height=hh;
   var cx=cv.getContext('2d');cx.drawImage(img,0,0,w,hh);
   var lum=0.2;
   try{var y0=Math.floor(hh*0.62),d=cx.getImageData(0,y0,w,hh-y0).data,s=0,n=0;for(var i=0;i<d.length;i+=16){s+=0.2126*d[i]/255+0.7152*d[i+1]/255+0.0722*d[i+2]/255;n++;}lum=n?s/n:0.2;}catch(e){}
   cb(cv.toDataURL('image/jpeg',0.84),lum>0.62?'#161a18':'#ffffff',w,hh);
  };
  img.onerror=function(){toast('图片无法读取，请换一张');};
  img.src=String(rd.result||'');
 };
 rd.readAsDataURL(file);
}
function scTargetLabel2(t){
 var tg=SC_TARGETS.find(function(x){return x[0]===t;});
 if(tg)return tg[1];
 if(String(t).indexOf('course:')===0){var c=COURSES.find(function(x){return 'course:'+x.id===t;});return c?'课程 · '+coMeta(c).title:'课程（已不存在）';}
 return t;
}
function scTargetItems2(){
 var items=SC_TARGETS.map(function(t){return [t[0],t[1]];});
 if(COURSES.length){items.push({g:'打开某一门课程'});COURSES.forEach(function(c){items.push(['course:'+c.id,coMeta(c).title]);});}
 return items;
}
function sizeText(it){var k=scWk(it);return (k==='auto'?'宽 自适应':k==='third'?'宽 1/3 行':k==='half'?'宽 1/2 行':k==='full'?'宽 整行':'宽 '+scRealW(it)+'px')+' · 高 '+scH(it)+'px';}

openScEditor=function(idx){
 var list=getSC(),isNew=idx<0;
 var it=isNew?{id:'sc_'+uid(),name:'',target:'course-manage',img:'',color:'',tc:'',icon:'',w:'auto',h:48,r:12,pos:{x:50,y:50,z:1}}
  :Object.assign({icon:'',w:'auto',h:48,r:12,pos:{x:50,y:50,z:1}},JSON.parse(JSON.stringify(list[idx])));
 if(!it.pos)it.pos={x:50,y:50,z:1};
 var tab=it.img?'img':(it.icon?'icon':'color');
 var body=h('div','sce2');
 /* 顶部：展示区（占 40%）+ 名称 / 跳转 */
 var top=h('div','sce2-top'),pvc=h('div','sce2-pvcol'),stage=h('div','sce2-stage'),cap=h('div','sce2-cap');
 pvc.appendChild(stage);pvc.appendChild(cap);
 var fs=h('div','sce2-fields');
 var nm=document.createElement('input');nm.type='text';nm.placeholder='名称（用图片时可留空）';nm.maxLength=8;nm.value=it.name;
 nm.oninput=function(){it.name=nm.value.trim();paintPv();};
 fs.appendChild(field('名称',nm));
 var tgBtn=h('button','pkbtn');tgBtn.type='button';
 var tgSp=h('span',null,scTargetLabel2(it.target));tgBtn.appendChild(tgSp);
 var tgCh=h('i');tgCh.style.display='contents';tgCh.innerHTML=ic('chevDown');tgBtn.appendChild(tgCh);
 tgBtn.onclick=function(){openPicker('点击后前往',scTargetItems2(),it.target,function(v){it.target=v;tgSp.textContent=scTargetLabel2(v);paintPv();});};
 fs.appendChild(field('点击后前往',tgBtn));
 top.appendChild(pvc);top.appendChild(fs);body.appendChild(top);
 var pvRaf=0;
 var stripFn=null;
 var paintPv=function(){
  if(pvRaf)return;
  pvRaf=requestAnimationFrame(function(){
   pvRaf=0;stage.textContent='';
   var rw=scRealW(it),rh=scH(it),avail=Math.max(60,stage.clientWidth-16),availH=stage.clientHeight-16;
   var K=Math.min(1,availH/rh);                       /* 只由高度决定缩放：改宽度不会让高度变化 */
   var box=h('div','sce2-pvbox'),tile=scTile(it,'pv');
   tile.style.transformOrigin='0 0';tile.style.transform='scale('+K+')';
   box.style.width=Math.min(rw*K,avail)+'px';box.style.height=(rh*K)+'px';box.appendChild(tile);stage.appendChild(box);
   stage.classList.toggle('clip',rw*K>avail+1);
   cap.textContent='首页效果 · '+(scWk(it)==='auto'?'自适应':rw+'px')+' × '+rh+'px';
   if(stripFn)stripFn();
  });
 };
 var tabs=h('div','sce2-tabs'),pane=h('div','sce2-pane');
 body.appendChild(tabs);body.appendChild(pane);
 function chips(def,cur,onPick){
  var row=h('div','sce2-chips');
  def.forEach(function(d){
   var b=h('button','sce2-chip'+(String(cur)===String(d[0])?' on':''));b.type='button';b.appendChild(document.createTextNode(d[1]));
   if(d[2])b.appendChild(h('i',null,d[2]));
   b.onclick=function(){onPick(d[0]);};row.appendChild(b);
  });
  return row;
 }
 function group(label,val,chipRow,slider){
  var g=h('div','sce2-grp'),hd=h('div','sce2-gh');hd.appendChild(h('b',null,label));hd.appendChild(h('span','gv',val));
  g.appendChild(hd);g.appendChild(chipRow);if(slider)g.appendChild(slider);return g;
 }
 function range(min,max,step,val,on){
  var r=document.createElement('input');r.type='range';r.min=min;r.max=max;r.step=step;r.value=val;
  r.oninput=function(){on(Number(r.value));};return r;
 }
 function paintPane(){
  tabs.textContent='';
  [['color','颜色'],['img','图片'],['icon','图标'],['size','尺寸']].forEach(function(t){
   var b=h('button',tab===t[0]?'on':'',t[1]);b.type='button';b.onclick=function(){tab=t[0];paintPane();};tabs.appendChild(b);
  });
  pane.textContent='';pane.scrollTop=0;stripFn=null;
  if(tab==='color'){
   var row=h('div','sc-colors');
   var none=h('button','sc-sw none'+(!it.color?' on':''));none.setAttribute('aria-label','默认');none.onclick=function(){it.color='';it.img='';paintPv();paintPane();};row.appendChild(none);
   SC_COLORS.forEach(function(c){var b=h('button','sc-sw'+(it.color===c&&!it.img?' on':''));b.style.background=c;b.setAttribute('aria-label',c);b.onclick=function(){it.color=c;it.img='';it.tc='';paintPv();paintPane();};row.appendChild(b);});
   var cp=document.createElement('input');cp.type='color';cp.className='sc-sw cpick';cp.value=/^#[0-9a-f]{6}$/i.test(it.color)?it.color:'#2f7d5f';
   cp.oninput=function(){it.color=cp.value;it.img='';it.tc='';paintPv();};cp.onchange=function(){paintPane();};row.appendChild(cp);
   row.style.flexWrap='wrap';pane.appendChild(row);
   
  }else if(tab==='img'){
   var ir=h('div','sce2-imgrow'),pick=h('button','btn sm',it.img?'更换图片':'选择图片');
   pick.onclick=function(){var fi=document.createElement('input');fi.type='file';fi.accept='image/*';fi.onchange=function(){var f=fi.files&&fi.files[0];if(!f)return;scImageLoad(f,function(url,tcx,iw,ih){it.img=url;it.tc=tcx;it.iw=iw;it.ih=ih;it.pos={x:50,y:50,z:1};paintPv();paintPane();});};fi.click();};
   ir.appendChild(pick);
   if(it.img){var clr=h('button','btn sm ghost','移除图片');clr.onclick=function(){it.img='';it.tc='';paintPv();paintPane();};ir.appendChild(clr);}
   pane.appendChild(ir);
   if(!it.img){return;}
   var rw=scRealW(it),rh=scH(it),ratio=rw/rh;
   var pw=Math.max(200,pane.clientWidth-6),stW=Math.min(pw,128*ratio),stH=stW/ratio;
   var st=h('div','sce2-crop view');st.style.width=stW+'px';st.style.height=stH+'px';
   var bg=h('span','sc-bg'),im=document.createElement('img');im.alt='';im.draggable=false;im.src=it.img;bg.appendChild(im);st.appendChild(bg);
   var badge=h('span','sce2-edit');badge.innerHTML=ic('crop')+'<i>调整</i>';st.appendChild(badge);
   pane.appendChild(st);
   scFit(st,bg,im,it);
   function openIt(){
    openCropModal({img:it.img,iw:it.iw,ih:it.ih,pos:it.pos,ratio:ratio,onDone:function(r){it.img=r.img;it.iw=r.iw;it.ih=r.ih;it.pos=r.pos;paintPv();paintPane();}});
   }
   st.onclick=openIt;
   var er=h('div','sce2-imgrow');er.style.marginTop='12px';
   var eb=h('button','btn pri sm','裁剪 / 调整图片');eb.onclick=openIt;er.appendChild(eb);pane.appendChild(er);
  }else if(tab==='icon'){
   pane.appendChild(iconGrid(it.icon||'',function(k){it.icon=k;it.img='';paintPv();},ENTRY_GLYPHS));
   var rb=h('button','btn sm ghost','恢复默认图标');rb.style.marginTop='10px';rb.onclick=function(){it.icon='';paintPv();paintPane();};pane.appendChild(rb);
   
  }else{
   var strip=h('div','sce2-strip'),sRow=h('div','sce2-srow');strip.appendChild(sRow);pane.appendChild(strip);
   function paintStrip(){
    sRow.textContent='';
    var rw=scRealW(it),rh=scH(it),pw=Math.max(200,pane.clientWidth-8),Ks=Math.min(pw/ROW_W,64/rh);
    var box=h('div','sce2-pvbox'),tile=scTile(it,'pv');
    tile.style.transformOrigin='0 0';tile.style.transform='scale('+Ks+')';
    box.style.width=Math.min(rw,ROW_W)*Ks+'px';box.style.height=rh*Ks+'px';box.appendChild(tile);sRow.appendChild(box);
   }
   stripFn=paintStrip;paintStrip();
   var wk=scWk(it),recW=(it.img&&scH(it)>=80)?'full':'auto',recH=it.img?88:48;
   var wdef=SC_W.map(function(d){return [d[0],d[1],d[0]===recW?'推荐':''];});
   var wRow=chips(wdef,wk,function(v){if(v==='px')it.w=scRealW(it);else it.w=v;paintPv();paintPane();});
   var wSl=range(64,400,2,scRealW(it),function(v){it.w=v;paintPv();pane.querySelectorAll('.gv')[0].textContent=v+'px';wRow.querySelectorAll('.sce2-chip').forEach(function(x,i){x.classList.toggle('on',i===4);});});
   var wv=h('span',null,wk==='auto'?'自适应':scRealW(it)+'px');
   pane.appendChild(group('宽度',wk==='auto'?'自适应':scRealW(it)+'px',wRow,wSl));
   var hdef=SC_H.map(function(d){return [d[0],d[1]+' '+d[0],d[0]===recH?'推荐':''];});
   var hRow=chips(hdef,scH(it),function(v){it.h=v;paintPv();paintPane();});
   var hSl=range(36,160,2,scH(it),function(v){it.h=v;paintPv();pane.querySelectorAll('.gv')[1].textContent=v+'px（首页搜索框高 48px）';hRow.querySelectorAll('.sce2-chip').forEach(function(x){x.classList.remove('on');});});
   pane.appendChild(group('高度',scH(it)+'px（首页搜索框高 48px）',hRow,hSl));
   var rdef=SC_R.map(function(d){return [d[0],d[1]];});
   var rRow=chips(rdef,scR(it)>=99?99:scR(it),function(v){it.r=v;paintPv();paintPane();});
   var rSl=range(0,30,1,Math.min(30,scR(it)),function(v){it.r=v;paintPv();pane.querySelectorAll('.gv')[2].textContent=v+'px';rRow.querySelectorAll('.sce2-chip').forEach(function(x){x.classList.remove('on');});});
   pane.appendChild(group('圆角',scR(it)>=99?'胶囊':scR(it)+'px',rRow,rSl));
  }
 }
 paintPane();paintPv();
 var opts={cls:'sc-sheet'};
 if(!isNew){opts.cancelText='移除';opts.danger=true;opts.onCancel=function(){
  var nmx=(list[idx]&&list[idx].name)||'这个入口';
  uiConfirm('移除「'+nmx+'」？',function(){
   var i=list.findIndex(function(x){return x.id===it.id;});
   if(i>=0)list.splice(i,1);
   save();closeSheet();render();toast('已移除入口');
  },{ok:'移除',danger:true,title:'移除入口'});
 };}
 openSheet(isNew?'添加入口':'编辑入口',body,function(){
  if(!it.name&&!it.img){toast('请填写名称，或选择一张图片');return false;}
  if(isNew)list.push(it);else list[idx]=it;
  save();render();
 },'保存',opts);
 paintPv();
};
var __rm=renderMine;
renderMine=function(){
 __rm.apply(this,arguments);
 
};
renderShortcuts=function(){
 var box=$('v-shortcuts');box.textContent='';
 var wrap=h('div','sc-page');box.appendChild(wrap);
 var list=getSC();
 var pv=stSection(ic('layers'),'预览');pv.classList.add('sc-pvp');
 var row=h('div','shortrow sc-row'+(list.length<=3?' sc-few':''));list.forEach(function(it){row.appendChild(scTile(it,true));});
 pv.appendChild(row);
 wrap.appendChild(pv);
 var lp=stSection(ic('compass'),'入口');
 var acts=h('div','acts');
 var add=iconBtn('plus','添加入口',function(){openScEditor(-1);});add.classList.add('sc-ib');
 var rst=iconBtn('resetArrow','恢复默认入口',function(){
  uiConfirm('恢复默认入口？当前自定义入口会被替换。',function(){S.ui.shortcuts=scDefaults();save();renderShortcuts();toast('已恢复默认入口');},{ok:'恢复',title:'恢复默认入口'});
 });rst.classList.add('sc-ib');
 acts.appendChild(add);acts.appendChild(rst);
 var hdEl=lp.querySelector('.st-head');if(hdEl){hdEl.appendChild(acts);hdEl.classList.add('sc-hd2');}else lp.insertBefore(acts,lp.firstChild);
 list.forEach(function(it,i){
  var r=h('div','sc-item');
  var sw=h('span','sc-mini');
  if(it.img)sw.style.backgroundImage='url("'+it.img+'")';
  else{if(it.color)sw.style.background=it.color;sw.innerHTML=ic(it.icon||SC_ICON[it.target]||'layers');sw.style.color=scTextColor(it)||'var(--pri)';}
  r.appendChild(sw);
  var mid=h('div','sc-mid');mid.appendChild(h('b',null,it.name||(it.img?'图片入口':'未命名')));
  var meta=h('div','sc-meta');meta.appendChild(h('span',null,scTargetLabel2(it.target)));
  var k=scWk(it);meta.appendChild(h('i',null,k==='auto'?'自适应':k==='third'?'1/3':k==='half'?'1/2':k==='full'?'整行':scRealW(it)+'px'));meta.appendChild(h('i',null,scH(it)+'px'));
  mid.appendChild(meta);r.appendChild(mid);
  var ops=h('div','sc-ops');
  var up=h('button',null,'↑');up.setAttribute('aria-label','上移');up.disabled=i===0;up.onclick=function(e){e.stopPropagation();var t=list[i];list[i]=list[i-1];list[i-1]=t;save();renderShortcuts();};
  var dn=h('button',null,'↓');dn.setAttribute('aria-label','下移');dn.disabled=i===list.length-1;dn.onclick=function(e){e.stopPropagation();var t=list[i];list[i]=list[i+1];list[i+1]=t;save();renderShortcuts();};
  var ed=h('button');ed.innerHTML=ic('pen');ed.setAttribute('aria-label','编辑');ed.onclick=function(e){e.stopPropagation();openScEditor(i);};
  ops.appendChild(up);ops.appendChild(dn);ops.appendChild(ed);r.appendChild(ops);
  r.onclick=function(){openScEditor(i);};
  lp.appendChild(r);
 });
 wrap.appendChild(lp);
};
render();
})();
