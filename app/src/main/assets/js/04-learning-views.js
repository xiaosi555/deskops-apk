/* Extracted original app layer; classic-script scope and order are intentional. */
/* SECTION: today view */
function dueList(){
 const out=[];
 Object.keys(S.learn).forEach(id=>{
  const rec=S.learn[id];const diff=dayDiff(rec.at);
  INTERVALS.forEach(iv=>{ if(rec.done&&rec.done.indexOf(iv)>=0)return; if(diff>=iv){const ch=chById(id);if(ch)out.push({id,ch,iv,diff});} });
 });
 return out.sort((a,b)=>a.iv-b.iv);
}
function renderToday(){
 const c=(S.pin&&chById(S.pin))||cur();const q=seq();
 const cb=document.getElementById('cardBox');cb.textContent='';
 const hd2=h('div','hd');
 hd2.appendChild(h('span','pill pri',c.id+' ｜ 模块 '+c.m));
 const th=h('h2',null,c.t);typeset(th,26);hd2.appendChild(th);
 cb.appendChild(hd2);
 cb.appendChild(h('p','muted','Day '+S.day+' · 第 '+(Math.min(S.idx,q.length-1)+1)+' / '+q.length+' 站 ｜ 本节要解决的问题：'+c.s));
 if(S.pin){
  const pb=h('div','note','当前是从课程地图直接展开的详解视图（'+S.pin+'），不改变你的路线进度。看完可以按下面的「回到路线进度」继续原来的学习顺序。');
  pb.style.marginTop='10px';cb.appendChild(pb);
 }
 renderLong(cb,c);
 const plainSec=h('div','sec');plainSec.appendChild(h('h3',null,'核心概念 · 双轨表达'));
 plainSec.appendChild(h('p','muted','下面这张表左边是大白话，右边是面试和文档里要说的术语。角色一一对应，不要只记一边。'));
 const sc=h('div','scroller');const tb=h('table','plain');
 const thead=h('thead');const trh=h('tr');['大白话','专业术语'].forEach(x=>trh.appendChild(h('th',null,x)));thead.appendChild(trh);tb.appendChild(thead);
 const tbody=h('tbody');
 c.plain.forEach(([term,say])=>{const tr=h('tr');tr.appendChild(h('td',null,term));tr.appendChild(h('td',null,say));tbody.appendChild(tr);});
 tb.appendChild(tbody);sc.appendChild(tb);plainSec.appendChild(sc);if(c.plain&&c.plain.length)cb.appendChild(plainSec);
 const termsSec=h('div','sec');termsSec.appendChild(h('h3',null,'本章专业清单'));
 termsSec.appendChild(h('p',null,String(c.terms||'').split('｜').join(' ／ ')));if(c.terms)cb.appendChild(termsSec);
 const gs=h('div','sec');gs.appendChild(h('h3',null,'过关标准'));
 gs.appendChild(h('p',null,c.gate));
 const st=S.status[c.id];
 const stm=h('div','st');stm.appendChild(h('span','mk '+(st==='green'?'g':st==='yellow'?'y':'r'),st==='green'?'🟢 过关':st==='yellow'?'🟡 半过':'🔴 未过关'));
 gs.appendChild(stm);cb.appendChild(gs);
 const navb=h('div','btns');
 const half=h('button','btn','🟡 能说出但不清楚用在哪');
 half.onclick=()=>{S.status[c.id]='yellow';if(!S.learn[c.id])S.learn[c.id]={at:today(),done:[]};save();toast('已标记 🟡，建议补一次场景提问');render();};
 const nxt=h('button','btn pri','下一站 →');
 nxt.onclick=()=>{window.__readTop=1;const ch=cur();if(!S.learn[ch.id])S.learn[ch.id]={at:today(),done:[]};delete S.pin;S.idx=Math.min(S.idx+1,seq().length-1);S.day++;save();toast('已进入 '+(seq()[S.idx]||'')+'');render();};
 if(S.pin){
  S.idx=Math.max(0,seq().indexOf(S.pin));
  if(seq().indexOf(S.pin)<0){const back=h('button','btn','回到路线进度');back.onclick=()=>{delete S.pin;save();render();};navb.appendChild(back);}
 }
 const go=h('button','btn ghost','跳到指定章节');
 go.onclick=()=>{nav('course');};
 navb.appendChild(half);navb.appendChild(go);navb.appendChild(nxt);
 cb.appendChild(navb);
 attachAnnotation(cb,c.id);
 const tip=h('div','note','今天只需带走的一句话：'+c.s+'。这一章的过关标准就是"能不能不看资料把这个类比和术语对上"。');
 cb.appendChild(tip);
}

/* SECTION: map view */
function openChapter(id){
 const c=chById(id);if(!c)return;
 S.pin=id;save();nav('today');
 typeset(document.querySelector('#cardBox .hd h2'),26);
 toast('已展开 '+id+' 的完整详解');
}

/* SECTION: knowledge view */
let knowKw='',knowMod='';
function renderKnow(){
 const box=document.getElementById('knowBox');box.textContent='';
 const fbox=document.getElementById('knowFilter');
 if(!fbox.childElementCount){
  MODS.forEach(m=>{
   const b=h('button','btn sm',m.n.replace('模块 ','').replace(' · ',' '));
   b.onclick=()=>{knowMod=(knowMod===m.k?'':m.k);renderKnow();fbox.querySelectorAll('.btn').forEach(x=>x.classList.remove('pri'));if(knowMod)b.classList.add('pri');};
   fbox.appendChild(b);
  });
 }
 let total=0;
 MODS.forEach(m=>{
  if(knowMod&&knowMod!==m.k)return;
  const list=CH.filter(c=>c.m===m.k&&(DEEP[c.id]&&DEEP[c.id].deep.length));
  const items=[];
  list.forEach(c=>DEEP[c.id].deep.forEach(row=>{
   const hay=(c.id+' '+c.t+' '+row[0]+' '+row[1]).toLowerCase();
   if(!knowKw||hay.indexOf(knowKw)>=0)items.push([c,row]);
  }));
  if(!items.length)return;
  total+=items.length;
  const card=h('div','card');
  const hd=h('div','hd');hd.appendChild(h('h2',null,m.n));hd.appendChild(h('span','pill',items.length+' 条'));
  card.appendChild(hd);
  items.forEach(([c,row],ii)=>{
   const w=h('div','dp');
   const tag=h('div',null,'');
   const pill=h('span','pill pri',c.id+' '+c.t);w.appendChild(pill);
   const bt=h('b',null,row[0]);bt.style.marginTop='6px';w.appendChild(bt);
   const kp=h('p',null,row[1]);hl(kp,c.id,'DK'+ii);w.appendChild(kp);
   const bb=h('div','btns');
   const cp=h('button','btn sm','复制这条');
   cp.onclick=()=>copyText('['+c.id+' '+c.t+'] '+row[0]+'\n\n'+row[1],'知识点已复制');
   bb.appendChild(cp);
   const go=h('button','btn sm','学到这章');
   go.onclick=()=>openChapter(c.id);
   bb.appendChild(go);
   w.appendChild(bb);card.appendChild(w);
  });
  box.appendChild(card);
 });
 document.getElementById('knowCount').textContent=(total||0)+' 条';
 if(!total)box.appendChild(h('div','card',null)).appendChild(h('p','muted','没有匹配的知识点。换个关键词试试，比如只搜「DNS」「端口」「默认」「报错」。'));
}
document.getElementById('knowSearch').oninput=function(){knowKw=this.value.trim().toLowerCase();renderKnow();};

/* SECTION: review view */
function renderReview(){
 const box=document.getElementById('rvBox');box.textContent='';
 const q=document.getElementById('rvAll')._on?allLearned():dueList();
 if(!q.length){box.appendChild(h('p','muted','当前没有内容。先在"今日卡片"推进几章，复习队列会自动建立。'));return;}
 q.forEach(d=>{
  const rv=h('div','rv');
  const hd=h('div','hd');
  hd.appendChild(h('b',null,d.ch.id+' '+d.ch.t));
  hd.appendChild(h('span','pill',d.iv?('第 '+d.iv+' 天 · 已过 '+d.diff+' 天'):'已学 '+d.diff+' 天'));
  rv.appendChild(hd);
  rv.appendChild(h('div','qtext',d.ch.quiz.q));
  const kp=(DEEP[d.id]&&DEEP[d.id].deep)||[];
  if(kp.length){
   const pw=h('div','dp');
   pw.appendChild(h('b',null,'细节回忆：只准说出这三条里的一条，说得出具体数值/路径/端口才算过'));
   const ul=h('p',null,kp.slice(0,3).map(x=>x[0]).join('　·　'));
   pw.appendChild(ul);
   rv.appendChild(pw);
  }
  const ans=h('div','ans','参考答案：'+d.ch.quiz.a+'\n\n细节补一条：'+((kp[0]||['',''])[0]+' —— '+(kp[0]||['',''])[1]).slice(0,400));
  const b=h('div','btns');
  const s=h('button','btn sm','看答案与细节');s.onclick=()=>ans.classList.toggle('on');
  b.appendChild(s);
  const dcp=h('button','btn sm','复制本章要点');
  dcp.onclick=()=>copyText(chapterDigest(d.ch),'本章要点已复制');
  b.appendChild(dcp);
  if(d.iv){
   const ok=h('button','btn sm pri','还记得');
   ok.onclick=()=>{markReviewed(d.id,d.iv);save();toast('记录完成');render();};
   b.appendChild(ok);
  }
  rv.appendChild(b);rv.appendChild(ans);box.appendChild(rv);
 });
}
function allLearned(){
 return Object.keys(S.learn).map(id=>{const ch=chById(id);const diff=dayDiff(S.learn[id].at);return{id,ch,iv:0,diff};}).sort((a,b)=>b.diff-a.diff);
}
function markReviewed(id,iv){
 const rec=S.learn[id]||{at:today(),done:[]};
 if(rec.done.indexOf(iv)<0)rec.done.push(iv);
 if(iv===INTERVALS[INTERVALS.length-1])rec.at=rec.at;else rec.done=rec.done;
 S.learn[id]=rec;
 if(iv>=INTERVALS[INTERVALS.length-1]){S.learn[id]={at:today(),done:[]};}
 save();
}
document.getElementById('rvAll').onclick=function(){this._on=!this._on;this.textContent=this._on?'只看今日到期':'查看全部条目';renderReview();};
document.getElementById('rvNow').onclick=function(){nav('today');window.scrollTo({top:0,behavior:'smooth'});};

/* SECTION: cases */
const MODULECODES=[['AD','AD 域与策略'],['NET','网络'],['SYS','Windows 系统'],['PRN','打印机'],['HW','硬件'],['O365','Office 365'],['MAIL','邮件'],['FILE','文件共享'],['SEC','终端防护'],['CONF','会议外设'],['MAC','macOS'],['LINUX','Linux'],['TICKET','工单流程'],['OTHER','其他']];
function caseCode(){
 const used=S.cases.map(c=>c.no||'').filter(x=>x.indexOf('-')>0).map(x=>x.split('-')[0]);
 const pick=MODULECODES.find(m=>!used.some(u=>u===m[0]))||MODULECODES[MODULECODES.length-1];
 const n=S.cases.filter(c=>(c.no||'').indexOf(pick[0]+'-')===0).length+1;
 return pick[0]+'-'+String(n).padStart(3,'0');
}
function renderCases(){
 const kw=(document.getElementById('caseSearch').value||'').trim().toLowerCase();
 document.getElementById('caseCount').textContent=S.cases.length+' 条';
 const box=document.getElementById('caseList');box.textContent='';
 const list=S.cases.filter(c=>!kw||[c.no,c.sym,c.path,c.cause,c.lesson,c.ch].join(' ').toLowerCase().indexOf(kw)>=0);
 if(!S.cases.length){box.appendChild(h('p','muted','还没有案例。学完一个故障场景就记一条——五栏由页面给你脚手架，别从空白开始写。'));return;}
 if(!list.length){box.appendChild(h('p','muted','没有匹配 "'+kw+'" 的案例。'));return;}
 list.forEach(c=>{
  const rv=h('div','rv');
  const hd=h('div','hd');hd.appendChild(h('b',null,c.no));hd.appendChild(h('span','pill',c.ch||'未关联章节'));
  const del=h('button','btn sm','删除');del.style.marginLeft='auto';
  del.onclick=()=>{uiConfirm('删除案例 '+c.no+'？',()=>{S.cases=S.cases.filter(x=>x!==c);save();renderCases();},{ok:'删除',danger:true});};
  hd.appendChild(del);rv.appendChild(hd);
  [['现象',c.sym],['排查路径',c.path],['根因',c.cause],['下次第一步查什么',c.lesson]].forEach(([k,v])=>{
   const p=h('div');p.appendChild(h('span','muted',k+'：'));p.appendChild(h('span',null,v||'—'));rv.appendChild(p);
  });
  const bar=h('div','btns');
  const ed=h('button','btn sm withic');ed.innerHTML=ic('pen')+'<span>编辑</span>';ed.onclick=()=>openCase(c);
  const cp=h('button','btn sm','复制这条');
  cp.onclick=()=>copyText(['编号：'+c.no,'现象：'+c.sym,'排查路径：'+c.path,'根因：'+c.cause,'结论：'+c.lesson,'关联章节：'+c.ch].join('\n'),'案例已复制');
  bar.appendChild(ed);bar.appendChild(cp);rv.appendChild(bar);
  box.appendChild(rv);
 });
}
let editing=null;
function openCase(c){
 editing=c||{no:caseCode(),sym:'',path:'',cause:'',lesson:'',ch:cur().id};
 const m=h('div');
 const card=h('div','card');
 const hd0=h('div','hd');
 hd0.appendChild(h('h2',null,editing?'编辑案例':'新建故障案例'));
 hd0.appendChild(h('span','pill','五栏固定'));
 card.appendChild(hd0);
 const mk=(lab,key,ph,ta)=>{
  const f=h('div','field');f.appendChild(h('label',null,lab));
  const el=ta?h('textarea'):h('input');if(!ta)el.type='text';
  if(ta)el.placeholder=ph;else el.placeholder=ph;
  el.value=editing[key]||'';
  el.oninput=()=>{editing[key]=el.value;};
  f.appendChild(el);card.appendChild(f);return el;
 };
 mk('编号（自动生成，可改）','no','如 AD-001');
 mk('现象：用户原话 + 具体报错文字/代码','sym','例如：提示"与域控制器的连接不可用，请使用以前的凭据登录"');
 mk('排查路径：按时间顺序记每一步查了什么、看到什么','path','1. ipconfig /all 发现 DNS 是 8.8.8.8\n2. 改回 DC 地址后 nslookup 能列出域控\n3. 复验登录正常',true);
 mk('根因（一句话）','cause','家用路由器 DHCP 下发了公共 DNS，导致解析不到 SRV 记录');
 mk('结论：下次遇到同类问题第一步查什么','lesson','先看 DNS，不要先怀疑账号被锁');
 mk('关联章节','ch','C4');
 const btns=h('div','btns');
 const closeForm=()=>{const f=document.getElementById('v-case').querySelector('.case-form');if(f)f.remove();};
 const sv=h('button','btn pri','保存案例');
 sv.onclick=()=>{
  if(!editing.sym){toast('至少把"现象"写清楚');return;}
  if(S.cases.indexOf(editing)<0)S.cases.push(editing);
  save();editing=null;closeForm();renderCases();toast('已保存。案例库是你最值钱的产出物');
 };
 const cn=h('button','btn ghost','取消');cn.onclick=()=>{editing=null;closeForm();renderCases();};
 btns.appendChild(sv);btns.appendChild(cn);card.appendChild(btns);
 document.getElementById('caseList').textContent='';
 const host=document.getElementById('v-case');
 const prev=host.querySelector('.case-form');if(prev)prev.remove();
 card.classList.add('case-form');host.appendChild(card);
 card.scrollIntoView({behavior:'smooth',block:'start'});
}
document.getElementById('newCase').onclick=()=>openCase(null);
document.getElementById('caseSearch').oninput=renderCases;
document.getElementById('expCase').onclick=function(){
 if(!S.cases.length){toast('案例库还是空的');return;}
 openExport({kind:'txt',title:'故障案例库 · 全部 '+S.cases.length+' 条',filename:'故障案例库-'+today()+'.txt',mime:'text/plain',text:casesText()});
};

/* SECTION: glossary */
function buildBaseGloss(){
 const out=[];
 CH.forEach(c=>{
  c.plain.forEach(([term,say])=>out.push({term,full:c.id+' '+c.t,say,src:'auto'}));
 });
 return out;
}
function renderGloss(){
 const kw=(document.getElementById('glSearch').value||'').trim().toLowerCase();
 const body=document.getElementById('glBody');body.textContent='';
 const auto=buildBaseGloss();
 const mine=S.gloss;
 const all=mine.concat(auto);
 const rows=all.filter(r=>!kw||(r.term+' '+r.full+' '+r.say).toLowerCase().indexOf(kw)>=0);
 document.getElementById('glCount').textContent='自填 '+mine.length+' 条 ｜ 自动生成 '+auto.length+' 条';
 rows.slice(0,400).forEach(r=>{
  const tr=h('tr');
  tr.appendChild(h('td',null,r.term));tr.appendChild(h('td',null,r.full));
  tr.appendChild(h('td',null,r.say));tr.appendChild(h('td',null,r.src==='auto'?'章节导入':'我填的'));
  const td=h('td');
  if(r.src==='user'){const del=h('button','btn sm','删');del.onclick=()=>{S.gloss=S.gloss.filter(x=>x!==r);save();renderGloss();};td.appendChild(del);}
  tr.appendChild(td);body.appendChild(tr);
 });
 if(rows.length>400)body.appendChild(h('tr')).appendChild(h('td',null,'仅显示前 400 条，请用检索过滤'));
}
document.getElementById('glSearch').oninput=renderGloss;
document.getElementById('addGl').onclick=()=>{
 const term=prompt('术语名（如 SRV 记录）');if(!term)return;
 const full=prompt('全称或说明（如 Service Resource Record，服务定位记录）')||'';
 const say=prompt('一句话大白话（如：写着"物业办公室往哪走"的专用指路牌）')||'';
 S.gloss.push({term,full,say,src:'user'});save();renderGloss();toast('已加入你的术语手册');
};
document.getElementById('syncGl').onclick=()=>{renderGloss();toast('已学章节的双轨对照已全部展示在表中');};

/* SECTION: playbook */
function renderPlay(){
 const lb=document.getElementById('layerBox');lb.textContent='';
 LAYERS.forEach(([t,d])=>{const li=h('li');li.appendChild(h('b',null,t));li.appendChild(h('span',null,d));lb.appendChild(li);});
 const cb=document.getElementById('cmdBody');cb.textContent='';
 const kw=(document.getElementById('cmdSearch').value||'').trim().toLowerCase();
 CMDS.filter(x=>!kw||x.join(' ').toLowerCase().indexOf(kw)>=0).forEach(x=>{
  const tr=h('tr');tr.appendChild(h('td',null,x[0]));tr.appendChild(h('td',null,x[1]));tr.appendChild(h('td',null,x[2]));cb.appendChild(tr);
 });
 const eb=document.getElementById('evBody');eb.textContent='';
 const ekw=(document.getElementById('evSearch').value||'').trim().toLowerCase();
 EVENTS.filter(x=>!ekw||x.join(' ').toLowerCase().indexOf(ekw)>=0).forEach(x=>{
  const tr=h('tr');x.forEach((v,i)=>tr.appendChild(h(i===0?'td':'td',null,v)));eb.appendChild(tr);
 });
 const sb=document.getElementById('scenarioBox');sb.textContent='';
 SCEN.forEach(s=>{
  const acc=h('div','acc');
  const hd=h('h4');hd.appendChild(h('span',null,s.t));hd.appendChild(h('span','arrow','▶'));
  const body=h('div','body');
  /* Grid 0fr → 1fr：内层必须有 overflow:hidden 与 min-height:0 */
  const inner=h('div','inner');body.appendChild(inner);
  inner.appendChild(h('p','muted','现象：'+s.p));
  inner.appendChild(h('h4',null,'标准排障链路'));
  const ol=h('ul','tl');s.steps.forEach(x=>{const li=h('li');li.appendChild(h('span',null,x));ol.appendChild(li);});
  inner.appendChild(ol);
  inner.appendChild(h('p',null,'高频根因：'+s.c));
  inner.appendChild(h('div','note','处理：'+s.f));
  const sbn=h('div','btns');
  const scp=h('button','btn sm','复制这条排障链路');
  scp.onclick=()=>copyText(s.t+'\n现象：'+s.p+'\n\n'+s.steps.map((x,i)=>(i+1)+'. '+x).join('\n')+'\n\n高频根因：'+s.c+'\n处理：'+s.f,'链路已复制');
  sbn.appendChild(scp);inner.appendChild(sbn);
  hd.onclick=()=>acc.classList.toggle('open');
  acc.appendChild(hd);acc.appendChild(body);sb.appendChild(acc);
 });
}
document.getElementById('cmdSearch').oninput=renderPlay;
document.getElementById('evSearch').oninput=renderPlay;

/* SECTION: progress */
function renderProg(){
 const q=seq();const done=q.filter(id=>S.status[id]==='green').length;
 const knowTotal=Object.keys(DEEP).reduce((n,k)=>n+(DEEP[k].deep||[]).length,0);
 const outTotal=Object.keys(DEEP).reduce((n,k)=>n+(DEEP[k].out||[]).length,0);
 const stats=[['已过关章节',done+' / '+q.length],['累计学习 Day',String(S.day)],['知识点详解',String(knowTotal)],['命令输出样例',String(outTotal)],['案例库',S.cases.length+' 条'],['自填术语',S.gloss.length+' 条'],['待复习',String(dueList().length)]];
 const sb=document.getElementById('statBox');sb.textContent='';
 stats.forEach(([k,v])=>{const s=h('div','stat');s.appendChild(h('b',null,v));s.appendChild(h('span',null,k));sb.appendChild(s);});
 const gb=document.getElementById('gateBody');gb.textContent='';
 q.forEach(id=>{
  const c=chById(id);if(!c)return;
  const st=S.status[id];
  const tr=h('tr');
  tr.appendChild(h('td',null,c.id+' '+c.t));
  tr.appendChild(h('td',null,c.gate));
  tr.appendChild(h('td','mk '+(st==='green'?'g':st==='yellow'?'y':'r'),st==='green'?'🟢 过关':st==='yellow'?'🟡 半过':'🔴 未过关'));
  const td=h('td');const sel=h('select');
  [['','请选择'],['green','🟢 过关'],['yellow','🟡 半过'],['red','🔴 未过关']].forEach(([v,n])=>{const o=h('option',null,n);o.value=v;if(st===v)o.selected=true;sel.appendChild(o);});
  sel.onchange=()=>{if(sel.value)S.status[id]=sel.value;else delete S.status[id];if(!S.learn[id])S.learn[id]={at:today(),done:[]};save();render();};
  td.appendChild(sel);tr.appendChild(td);gb.appendChild(tr);
 });
 const lab=document.getElementById('labBox');lab.textContent='';
 LAB.forEach(([t,d])=>{const li=h('li');li.appendChild(h('b',null,t));li.appendChild(h('span',null,d));lab.appendChild(li);});
}

document.getElementById('resetAll').onclick=()=>{
 uiConfirm('确认清空本机保存的全部进度、案例库与自填术语？此操作不可撤销。',function(){
 localStorage.removeItem(KEY);S={theme:'light',ui:{}};initCourseState();syncCourses();loadPack('ops');save();render();toast('已清空');
 },{ok:'清空',danger:true});
};
document.addEventListener('click',function(e){
 const tb=e.target.closest?e.target.closest('[data-theme-toggle]'):null;
 if(tb){S.theme=S.theme==='dark'?'light':'dark';applyTheme();}
});

