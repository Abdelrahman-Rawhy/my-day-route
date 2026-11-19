import asyncio,json,sys
from playwright.async_api import async_playwright
COLLECT=open('/tmp/qrtest/collect.py').read().split('COLLECT="""')[1].split('"""')[0]
LAYOUT="""()=>{const W=innerWidth,out=[];document.querySelectorAll('.view.on *, .sheet.on *').forEach(e=>{const r=e.getBoundingClientRect();if(!r.width||!r.height)return;const cs=getComputedStyle(e);if(cs.position==='fixed')return;if(r.right>W+2||r.left<-2){if(!e.closest('.tpl,.exch,.tabs2,.daychips,.fbar,.chips,.hscroll,[style*=overflow]')&&!(e.parentElement&&getComputedStyle(e.parentElement).overflowX!=='visible'))out.push((e.id||e.className||e.tagName).toString().slice(0,40)+' '+Math.round(r.left)+'..'+Math.round(r.right))}
 if(e.children.length===0&&e.scrollWidth>e.clientWidth+3&&cs.overflow!=='visible'&&cs.textOverflow!=='ellipsis'&&!['INPUT','TEXTAREA','SELECT'].includes(e.tagName)&&e.textContent.trim())out.push('clip:'+e.textContent.trim().slice(0,30))});return [...new Set(out)].slice(0,15)}"""
STEPS=[
 ("wizard","obOpen()"),("wiz name","obGo(1)"),("wiz city","obGo(1)"),("wiz theme","obGo(1)"),("wiz done","obGo(1)"),
 ("today","go('today')"),("checklist","obPaint();document.getElementById('obCk').scrollIntoView()"),("coach","window._tipShown=0;S.tips={};scrollTo(0,0);obTipToday()"),("coach close","tipClose()"),
 ("help","helpOpen()"),("help cat","closePick();helpCat(5)"),("help show","closePick();helpShow(11,5)"),("help close","closeAsk()"),
 ("tasks empty","go('tasks');tdMode='list';renderTasks()"),("day idea","go('today');S.ob.ck.pray=1;obPaint();document.getElementById('obDay').scrollIntoView()"),("add example","addExample(1);addExample(4)"),("tick step","(()=>{const t=S.tasks.find(x=>!x.prayer);toggle(t.id)})()"),
 ("open step","openStep(S.tasks.find(x=>!x.prayer).id)"),("add sub","document.getElementById('subIn')&&(document.getElementById('subIn').value='سورة الملك',addSub())"),
 ("opts step","closeSheets();openOpts(S.tasks.find(x=>!x.prayer).id)"),("opts prayer","closeSheets();openOpts(S.tasks.find(x=>x.prayer).id)"),
 ("setPQ","closeSheets();setPQ('fajr','jamaah')"),("quick add","openQuick();document.getElementById('qaIn').value='الجيم الساعة 7 مساء';qaPreview()"),("save quick","saveQuick()"),
 ("task sheet","closeSheets();openTask()"),("edit","closeSheets();openEdit()"),("rescue","closeSheets();typeof rescue==='function'&&rescue()"),
 ("life ib","lifeOpen('ib')"),("life he","lifeOpen('he')"),("life mo","lifeOpen('mo')"),("life le","lifeOpen('le')"),("life close","lifeClose()"),
 ("mushaf index","go('faith');fTab='mushaf';qView.s=null;qPos().bm=[{s:2,a:255,pg:42,at:today()}];renderFaith()"),("mushaf search","qSearch='يس';renderFaith()"),("mushaf clear","qSearch='';renderFaith()"),
 ("kh confirm","fTab='quran';renderFaith();khSlideSet(kh().page+50)"),("kh confirm close","closeAsk()"),("kh small","khSlideSet(kh().page+1)"),
 ("task route","go('tasks');TD().items.push({id:'tr1',t:'اشتري عيش',pri:1,due:today(),tags:[],subs:[],status:'todo',created:1});tdOpen('tr1')"),("task route add","tdToRoute('tr1')"),("task route close","tdClose();go('today')"),
 ("guide","guideOpen()"),("guide next","guideGo(1);guideGo(1)"),("guide close","guideClose()"),
 ("plan open","go('today');planOpen()"),("plan saved","dzPick(0);planSave()"),("discipline hub","closeSheets();dzOpen()"),("hub close","closeSheets()"),
 ("adhkar home","go('faith');fTab='adhkar';renderFaith()"),("adhkar reader","adkOpen('travel')"),("adhkar count","adkRCount()"),("adhkar search","adkRClose();adkQ='سفر';renderFaith()"),("adhkar fav","adkQ='';renderFaith();adkFavToggle('food',0);adkOpenFav()"),("adhkar close","adkRClose()"),
 ("faith tasbih","go('faith');fTab='tasbih';renderFaith()"),("faith quran","fTab='quran';renderFaith()"),("faith qibla","fTab='qibla';renderFaith()"),("faith prayers","fTab='prayers';renderFaith()"),
 ("goals","go('goals')"),("new project","openNewGoal('project')"),("new habit","closeSheets();openNewGoal('habit')"),("new number","closeSheets();openNewGoal('number')"),("new life","closeSheets();openNewGoal('life')"),
 ("make goals","closeSheets();S.goals.push({id:'g1',type:'project',title:'مشروع',area:'study',icon:'graduation',created:Date.now(),steps:[{id:'s1',name:'خطوة',done:false}]},{id:'g2',type:'habit',title:'قراءة',area:'faith',icon:'book',created:Date.now(),target:1,unit:'صفحات',period:'day',log:{}},{id:'g3',type:'number',title:'حفظ',area:'faith',icon:'book',created:Date.now(),start:0,target:30,unit:'جزء',current:2,history:[]});save();renderGoals()"),
 ("open goal proj","openGoal('g1')"),("open goal habit","closeSheets();openGoal('g2')"),("habit add","closeSheets();habitAdd('g2')"),("open goal num","openGoal('g3')"),
 ("timer start","closeSheets();go('goals');startPause()"),("timer reset","startPause();resetTimer()"),("distracted","startPause();dzDistract()"),("distract save","document.getElementById('dzDistT').value='رسالة الشغل';dzDistSave();resetTimer()"),
 ("tasks matrix","go('tasks');tdMode='mx';renderTasks()"),("mx menu","TD().items.push({id:'mq',t:'مهمة',pri:2,due:today(),tags:[],subs:[],status:'todo',created:1});renderTasks();tdMxTap('mq')"),("mx add","closeSheets();tdMxAdd(2)"),
 ("journal","closeSheets();go('journal')"),("j editor","openJEditor()"),("ideas","closeSheets();go('ideas')"),
 ("awards","go('awards')"),("prizes","go('prizes')"),("reward sheet","openReward()"),("review","closeSheets();go('review')"),
 ("settings","go('settings')"),("city","openCity()"),("backup","closeSheets();openBackup()"),("account","closeSheets();openAccount()"),("cal","closeSheets();openCalSet()"),
 ("family create","closeSheets();window.firebase=window.firebase||{};FB.ok=true;S.famCode=null;S.famId=null;famTab='create';go('family')"),("family join","famTab='join';renderFamily()"),
 ("family in","S.famCode='ABCD-EFGH-JK23';S.famId='f1';FB.uid='u1';FAM.family={name:'عائلة'};FAM.members=[{id:'u1',name:'أحمد',color:'#7C5CFF'},{id:'u2',name:'ماما',color:'#F472B6'}];FAM.tasks=[{id:'t1',title:'عيش',type:'shopping',from:'u2',to:'u1',status:'open',pts:10,items:[{t:'لبن',done:false}],fromName:'ماما'}];famView='tasks';famTab='mine';renderFamily()"),
 ("chat open","FAM.members=[{id:'u1',name:'أحمد',color:'#8A5A2B'},{id:'u2',name:'Baba',color:'#F472B6',online:true,pub:1}];openChat('u2')"),("chat close","closeChat()"),
 ("family sent","famTab='sent';renderFamily()"),("family chats","famView='chats';renderFamily()"),("send sheet","openSend(null,'home')"),("qr","closeSheets();showFamQR()"),
 ("update banner","closeSheets();go('today');showUpdate('تجربة')"),("back x3","handleBack();handleBack();handleBack()"),
]
async def main():
  theme=sys.argv[1] if len(sys.argv)>1 else 'dark'
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    pg=await c.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)[:160]))
    toasts=[]
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate(f"S.name='Boudy';S.theme='{theme}';save()");await pg.reload();await pg.wait_for_timeout(1500)
    await pg.evaluate("const _t=toast;window._toasts=[];toast=(m,i)=>{window._toasts.push(m);_t(m,i)}")
    eng=set();lay={};fails=[]
    for name,code in STEPS:
      n0=len(errs)
      try: await pg.evaluate(code)
      except Exception as e: fails.append(name+': '+str(e).split('\n')[0][:140])
      await pg.wait_for_timeout(350)
      await pg.evaluate("(()=>{const c=document.getElementById('celebrate');if(c)c.classList.remove('on')})()")
      for t in await pg.evaluate(COLLECT): eng.add(t)
      L=await pg.evaluate(LAYOUT)
      if L: lay[name]=L
      safe=name.replace(' ','_');await pg.screenshot(path=f'/tmp/qrtest/qa/{theme}_{safe}.png')
      if len(errs)>n0: fails.append(name+' -> pageerror '+errs[-1])
    t=await pg.evaluate("window._toasts||[]")
    for x in (t or []):
      x=str(x)
      if any(ch.isascii() and ch.isalpha() for ch in x): eng.add('TOAST::'+x)
    print('FAILS',json.dumps(fails,ensure_ascii=False,indent=0))
    print('LAYOUT',json.dumps(lay,ensure_ascii=False,indent=0))
    skip=('Boudy','ABCD','https','calendar.google','English','QR','iCal','PDF','Indeed','Firebase','AuthError')
    print('ENGLISH',json.dumps(sorted(x for x in eng if not any(k in x for k in skip)),ensure_ascii=False,indent=0))
    await b.close()
asyncio.run(main())
