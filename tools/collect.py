import asyncio,json
from playwright.async_api import async_playwright
SHEETS="recSheet ideaSheet editSheet taskSheet calSheet chSheet optSheet stepSheet qaSheet rewardSheet nameSheet projSheet reportSheet qrSheet scanSheet sendSheet memberSheet jEditSheet jViewSheet pinSheet backupSheet acctSheet goalNewSheet goalSheet citySheet".split()
COLLECT="""()=>{const out=new Set();const skip='.bub,.idea .t,textarea,#recTxt,.code,.ar,.ar2,#jBody .jtext,script,style';const w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let n;while(n=w.nextNode()){const p=n.parentElement;if(!p||p.closest(skip))continue;const t=n.nodeValue.trim();if(t&&/[A-Za-z]{2,}/.test(t))out.add(t)}document.querySelectorAll('[placeholder]').forEach(e=>{if(/[A-Za-z]{2,}/.test(e.placeholder))out.add('PH::'+e.placeholder)});return [...out]}"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844})
    pg=await c.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)[:150]))
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.lang='ar';S.tasks.push({id:'x1',icon:'book',name:'Read Quran',time:'05:25',pts:20,prayer:null});save()");await pg.reload();await pg.wait_for_timeout(1500)
    allt=set()
    async def grab():
      for t in await pg.evaluate(COLLECT): allt.add(t)
    await grab()
    for v in ['today','goals','journal','awards','prizes','review','settings','faith','ideas']:
      try: await pg.evaluate(f"go('{v}')");await pg.wait_for_timeout(500);await grab()
      except Exception as e: pass
    for ft in ['tasbih','quran','qibla','prayers']:
      try: await pg.evaluate(f"go('faith');fTab='{ft}';renderFaith()");await pg.wait_for_timeout(300);await grab()
      except: pass
    # family mocks
    fam=["window.firebase=window.firebase||{};FB.ok=true;S.famCode=null;S.famId=null;famTab='create';go('family');renderFamily()",
         "famTab='join';renderFamily()",
         "S.famCode='ABCD-EFGH-JK23';S.famId='f1';FB.uid='u1';FAM.family={name:'Rawhy'};FAM.members=[{id:'u1',name:'Ahmed',color:'#7C5CFF'},{id:'u2',name:'Mom',color:'#F472B6'}];FAM.tasks=[{id:'t1',title:'Bread',type:'task',from:'u2',to:'u1',status:'open',pts:10,items:[{t:'Milk',done:false}],fromName:'Mom'}];famView='tasks';famTab='mine';renderFamily()",
         "famTab='sent';renderFamily()","famView='chats';renderFamily()"]
    for f in fam:
      try: await pg.evaluate(f);await pg.wait_for_timeout(400);await grab()
      except Exception as e: errs.append('fam '+str(e)[:100])
    opens=["openTask()","openEdit()","openQuick()","openReward()","setName()","openCity()","showFamQR()","openScan()","openSend(null,'task')","openStep(S.tasks.find(t=>!t.prayer).id)","openOpts(S.tasks.find(t=>!t.prayer).id)","openOpts(S.tasks.find(t=>t.prayer).id)","openNewGoal()","startRec()","openNewGoal('project')","openNewGoal('habit')","openNewGoal('number')","openNewGoal('life')","openNewGoal()","openGoal(S.goals[0].id)","openProj&&openProj()","openReport&&openReport()","openBackup&&openBackup()","openAcct&&openAcct()"]
    for o in opens:
      try: await pg.evaluate("closeSheets();"+o);await pg.wait_for_timeout(500);await grab()
      except Exception as e: errs.append(o+' '+str(e)[:80])
    for sh in SHEETS:
      try: await pg.evaluate(f"closeSheets();document.getElementById('{sh}').classList.add('on')");await pg.wait_for_timeout(150);await grab()
      except: pass
    json.dump(sorted(allt),open('/tmp/qrtest/untr.json','w'),ensure_ascii=False,indent=0)
    print(len(allt));print(errs[:10]);await b.close()
asyncio.run(main())
