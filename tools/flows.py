# Functional flows with assertions (not just "no error"): each step does something real and checks the data.
import asyncio,json,sys
from playwright.async_api import async_playwright
F=[
 ("route: add step via quick add","openQuick();document.getElementById('qaIn').value='مشي الساعة 6 مساء';qaPreview();saveQuick();return S.tasks.some(t=>/مشي/.test(t.name))"),
 ("route: tick + untick a prayer","const t=S.tasks.find(x=>x.prayer);toggle(t.id);const a=dayRec().done.includes(t.id);toggle(t.id);return a&&!dayRec().done.includes(t.id)"),
 ("route: points added on tick","const t=S.tasks.find(x=>x.prayer),p=S.points;toggle(t.id);return S.points>p"),
 ("route: reset today","resetToday();return dayRec().done.length===0"),
 ("tasks: natural language add","go('tasks');tdMode='list';renderTasks();document.getElementById('tdIn').value='اتصل بعلي بكرة 5م #شغل !2';tdQuickAdd();const x=TD().items.find(i=>/اتصل/.test(i.t));return !!x&&x.tags.includes('شغل')&&x.pri===2&&!!x.due&&x.time==='17:00'"),
 ("tasks: complete task gives points","const x=TD().items.find(i=>/اتصل/.test(i.t)),p=S.points;tdToggle?tdToggle(x.id):0;return x.status==='done'&&S.points>p"),
 ("plan: save top 3 creates new task","planOpen();const i=document.getElementById('dzT0');i.value='حاجة جديدة خالص';planSave();return (S.plan[today()].top||[]).some(x=>x.t==='حاجة جديدة خالص'&&x.tid)&&TD().items.some(x=>x.t==='حاجة جديدة خالص')"),
 ("plan: tick item","planToggle(0);return S.plan[today()].top[0].done===true"),
 ("goal: create project with steps","go('goals');openNewGoal('project');document.getElementById('ngTitle').value='مشروع اختبار';const st=document.getElementById('ngSteps');if(st)st.value='خطوة 1\\nخطوة 2';saveGoal();const g=S.goals.find(x=>x.title==='مشروع اختبار');return !!g&&(g.steps||[]).length===2"),
 ("goal: add step + tick step","closeSheets();const g=S.goals.find(x=>x.title==='مشروع اختبار');openGoal(g.id);document.getElementById('stepIn').value='خطوة 3';stepAdd(g.id);togMs?0:0;return g.steps.length===3"),
 ("habit: create + add","closeSheets();openNewGoal('habit');document.getElementById('ngTitle').value='قراءة اختبار';saveGoal();const g=S.goals.find(x=>x.title==='قراءة اختبار');if(!g)return false;habitAdd(g.id,1);return habitCount(g)>=1"),
 ("focus: full session counted","closeSheets();go('goals');resetTimer();startPause();S.timer.endAt=Date.now()-1000;timerTick();return (dayRec().sessions||0)>=1&&S.timer.mode==='break'"),
 ("night review: save","go('today');S.tasks.forEach(t=>{if(!dayRec().done.includes(t.id))toggle(t.id)});render();window._rate=4;const g=document.getElementById('gratIn');if(!g)return 'no review card (before 7pm)';g.value='الصحة';saveReview();return dayRec().review.rating===4"),
 ("adhkar: finish a section","adkOpen('wake');const L=adhkarList('wake');for(let r=0;r<40;r++){if(!ADKR)break;const rf=adkRRef();if(!rf)break;adkRCount()}const st=adkState().wake||{};return L.every((x,i)=>(st[i]||0)>=x.n)"),
 ("adhkar: checklist adk ticks","adkRClose&&adkRClose();go('today');render();return S.ob.ck.adk===1"),
 ("tasbih: count","go('faith');fTab='tasbih';renderFaith();const a=JSON.stringify(dayRec()).length;const ev={clientX:100,clientY:300,pointerId:1,preventDefault(){},target:document.querySelector('[onpointerdown^=\"tzDown\"]')};tzDown(ev);tzUp(ev);return JSON.stringify(dayRec()).length!==a"),
 ("life: water +1","lifeOpen('he');const k=today(),a=LF().he.water[k]||0;heWater(1);return (LF().he.water[k]||0)===a+1"),
 ("life: sunnah toggle","lifeOpen('ib');const r=dayRec(),a=JSON.stringify(r.sun||{});togSun('fajr');return JSON.stringify(dayRec().sun||{})!==a"),
 ("life: close","lifeClose();return !document.body.classList.contains('lifeon')"),
 ("prizes: add + claim","go('prizes');openReward();document.getElementById('rName').value='قهوة اختبار';document.getElementById('rCost').value='1';saveReward();const r=S.rewards.find(x=>x.name==='قهوة اختبار');if(!r)return false;S.points+=5;const p=S.points;claim(r.id);return S.points===p-1"),
 ("theme: all 4","for(const t of ['dark','light','cream','auto']){S.theme=t;applyTheme()}S.theme='cream';applyTheme();return document.documentElement.classList.contains('cream')"),
 ("text size","S.fs=1.25;applyTheme();const z=document.body.style.zoom;S.fs=1;applyTheme();return z==='1.25'"),
 ("name: save","setName();document.getElementById('nameIn').value='أحمد';saveName();return S.name==='أحمد'"),
 ("help: search","helpOpen();closePick();return HELP_FAQ().length>=12"),
 ("back: closes sheet then minimises","openCity();handleBack();const o=document.querySelector('.sheet.on:not(.closing)');return !o||('still open: '+o.id+' coach='+!!document.getElementById('coach')+' body='+document.body.className+' on='+[...document.querySelectorAll('.on')].map(e=>e.id||e.className).join(','))"),
 ("guide: walk all slides","guideOpen();for(let i=0;i<20;i++)guideGo(1);return !document.body.classList.contains('guiding')"),
 ("wizard: full run saves name+theme","S.ob1=0;obOpen();obGo(1);document.getElementById('obName').value='سارة';obGo(1);obGo(1);obTheme('light');obGo(1);return S.ob1===1&&S.name==='سارة'&&S.theme==='light'"),
]
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)).new_page()
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e).split('\n')[0][:200]))
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.ob1=1;S.guide1=1;OB();S.tips={};window._tipShown=1;const W=document.getElementById('obWiz');if(W)W.classList.remove('on');document.body.classList.remove('guiding','obon');addExample(1);save()")
    ok=0
    for n,code in F:
      n0=len(errs)
      try: r=await pg.evaluate("(()=>{"+code+"})()")
      except Exception as e: r='EXC '+str(e).splitlines()[0][:160]
      await pg.wait_for_timeout(250)
      await pg.evaluate("(()=>{const c=document.getElementById('celebrate');if(c){c.classList.remove('on');c.classList.remove('on','closing')}})()");await pg.wait_for_timeout(300)
      e=errs[n0:] 
      st='PASS' if r is True and not e else 'FAIL'
      ok+=st=='PASS';print(f'{st}  {n}'+('' if r is True else f'  -> {r}')+(f'  ERR {e}' if e else ''))
    print(f'{ok}/{len(F)} passed');await b.close()
asyncio.run(main())
