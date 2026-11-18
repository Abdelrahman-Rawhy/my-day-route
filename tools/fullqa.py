# Full button crawler: opens every screen / sheet / overlay, clicks EVERY visible button in it
# (state restored before each click) and reports JS errors and buttons that seem to do nothing.
# usage: python3 tools/fullqa.py [theme] [contextFilter]
import asyncio,json,sys,re,time
from playwright.async_api import async_playwright
src=open('/home/claude/MyDayRoute/tools/qa.py').read().split('async def main')[0]
ns={};exec(src.replace('from playwright.async_api import async_playwright',''),ns)
STEPS=dict(ns['STEPS'])
SEED="""(()=>{%s;closeSheets();
%s;closeSheets();
TD().items.push({id:'mq',t:'مهمة تجربة',pri:2,due:today(),tags:['شغل'],subs:[{id:'x1',t:'فرعية',done:false}],status:'todo',created:1},{id:'mq2',t:'مهمة خلصت',pri:0,due:today(),tags:[],subs:[],status:'done',doneAt:Date.now(),created:1});
S.plan=S.plan||{};S.plan[today()]={top:[{t:'مذاكرة',gid:'g1',done:false},{t:'مهمة تجربة',tid:'mq',done:false}],set:Date.now()};
LF();S.life.mo.tx=[{id:'t1',d:today(),amt:100,type:'out',cat:'food',note:'غدا'}];S.life.le.books=[{id:'b1',t:'كتاب',pages:200,at:20}];S.life.le.cards=[{id:'c1',q:'سؤال',a:'جواب',due:today(),iv:0,ef:2.5}];S.life.he.meds=[{id:'m1',name:'فيتامين',times:['09:00']}];
S.ob1=1;S.guide1=1;OB();S.tips={a:1};window._tipShown=1;save()})()"""%(STEPS['add example'],STEPS['make goals'])
FAMIN=STEPS['family in']
C=[ # name, opener js
 ('today',"go('today')"),('today list',"S.routeView='list';go('today')"),
 ('tasks list',"go('tasks');tdMode='list';renderTasks()"),('tasks board',"go('tasks');tdMode='board';renderTasks()"),('tasks cal',"go('tasks');tdMode='cal';renderTasks()"),('tasks matrix',"go('tasks');tdMode='mx';renderTasks()"),
 ('goals',"go('goals')"),('faith adhkar',"go('faith');fTab='adhkar';renderFaith()"),('faith quran',"go('faith');fTab='quran';renderFaith()"),('faith khatma',"go('faith');fTab='khatma';renderFaith()"),('faith tasbih',"go('faith');fTab='tasbih';renderFaith()"),('faith prayers',"go('faith');fTab='prayers';renderFaith()"),('faith qibla',"go('faith');fTab='qibla';renderFaith()"),
 ('journal',"go('journal')"),('ideas',"go('ideas')"),('awards',"go('awards')"),('prizes',"go('prizes')"),('review',"go('review')"),('settings',"go('settings')"),
 ('family create',"window.firebase=window.firebase||{};FB.ok=true;S.famCode=null;S.famId=null;famTab='create';go('family')"),('family join',"FB.ok=true;S.famCode=null;S.famId=null;famTab='join';go('family')"),
 ('family in',"go('family');"+FAMIN),('family sent',"go('family');"+FAMIN+";famTab='sent';renderFamily()"),('family chats',"go('family');"+FAMIN+";famView='chats';renderFamily()"),
 ('chat',"go('family');"+FAMIN+";FAM.members=[{id:'u1',name:'أحمد',color:'#8A5A2B'},{id:'u2',name:'Baba',color:'#F472B6',online:true,pub:1}];openChat('u2')"),
 ('sheet step',"go('today');openStep(S.tasks.find(x=>!x.prayer).id)"),('sheet opts',"go('today');openOpts(S.tasks.find(x=>!x.prayer).id)"),('sheet opts prayer',"go('today');openOpts(S.tasks.find(x=>x.prayer).id)"),
 ('sheet quick',"go('today');openQuick()"),('sheet edit',"go('today');openEdit()"),('sheet task',"go('today');openTask()"),('sheet plan',"go('today');planOpen()"),('sheet discipline',"go('today');dzOpen()"),
 ('sheet path',"go('today');pathTap&&pathTap(S.tasks.find(x=>!x.prayer).id)"),
 ('sheet new project',"go('goals');openNewGoal('project')"),('sheet new habit',"go('goals');openNewGoal('habit')"),('sheet new number',"go('goals');openNewGoal('number')"),('sheet new life',"go('goals');openNewGoal('life')"),
 ('sheet goal proj',"go('goals');openGoal('g1')"),('sheet goal habit',"go('goals');openGoal('g2')"),('sheet goal num',"go('goals');openGoal('g3')"),
 ('sheet todo',"go('tasks');tdOpen('mq')"),('sheet city',"go('settings');openCity()"),('sheet backup',"go('settings');openBackup()"),('sheet account',"go('settings');openAccount()"),('sheet cal',"go('settings');openCalSet()"),
 ('sheet reward',"go('prizes');openReward()"),('sheet widgets',"go('settings');openWidgetSet()"),('sheet name',"go('settings');setName()"),('sheet jeditor',"go('journal');openJEditor()"),
 ('sheet send',"go('family');"+FAMIN+";openSend(null,'home')"),('sheet qr',"go('family');"+FAMIN+";showFamQR()"),
 ('adhkar reader',"go('faith');adkOpen('morning')"),('mushaf',"go('faith');typeof qOpenPage==='function'?qOpenPage(1):qContinue()"),
 ('life ib',"go('today');lifeOpen('ib')"),('life he',"go('today');lifeOpen('he')"),('life mo',"go('today');lifeOpen('mo')"),('life le',"go('today');lifeOpen('le')"),
 ('guide',"guideOpen()"),('wizard',"S.ob1=0;obOpen()"),('help menu',"go('settings');helpOpen()"),('checklist',"go('today');S.ob={};OB();obPaint()"),
]
SKIP_TXT=re.compile(r'(Sign out|تسجيل الخروج|Delete account)')
ENUM="""(sel)=>{const ov=[...document.querySelectorAll('.sheet.on:not(.closing)')].pop();let root=ov?ov.querySelector('.panel')||ov:null;
 if(!root)for(const id of ['obWiz','appGuide','adkR','lifeR','qReader','chatScreen']){const e=document.getElementById(id);if(e&&e.classList.contains('on')){root=e;break}}
 if(!root)root=document.querySelector('.view.on');const out=[];
 root.querySelectorAll('button,[onclick],.setting,a[href],input[type=checkbox],label.sw').forEach(e=>{if(e.closest('[disabled]'))return;const r=e.getBoundingClientRect();if(!r.width||!r.height)return;if(e.parentElement&&e.parentElement.closest('button,[onclick]')&&!e.matches('button'))return;
  out.push({t:(e.innerText||e.getAttribute('aria-label')||e.className||e.tagName).trim().replace(/\\s+/g,' ').slice(0,40),oc:(e.getAttribute('onclick')||'').slice(0,90)})});
 window._qaEls=null;return out}"""
CLICK="""(i)=>{const ov=[...document.querySelectorAll('.sheet.on:not(.closing)')].pop();let root=ov?ov.querySelector('.panel')||ov:null;
 if(!root)for(const id of ['obWiz','appGuide','adkR','lifeR','qReader','chatScreen']){const e=document.getElementById(id);if(e&&e.classList.contains('on')){root=e;break}}
 if(!root)root=document.querySelector('.view.on');const L=[];root.querySelectorAll('button,[onclick],.setting,a[href],input[type=checkbox],label.sw').forEach(e=>{if(e.closest('[disabled]'))return;const r=e.getBoundingClientRect();if(!r.width||!r.height)return;if(e.parentElement&&e.parentElement.closest('button,[onclick]')&&!e.matches('button'))return;L.push(e)});
 const e=L[i];if(!e)return 'MISSING';window._sig0=sig();window._lastPointer=Date.now();e.click();return 'ok'}"""
SIG="""window.sig=()=>{const t=document.getElementById('toast');return JSON.stringify(S).length+'|'+document.body.innerHTML.length+'|'+[...document.querySelectorAll('.sheet.on,.view.on,.on')].length+'|'+(t?t.textContent:'')+'|'+(window._qaHit||0)+'|'+scrollY+'|'+[...document.querySelectorAll('.on')].map(e=>e.id||e.className).join(',')+'|'+document.body.className}"""
async def main():
  theme=sys.argv[1] if len(sys.argv)>1 else 'cream';flt=sys.argv[2] if len(sys.argv)>2 and sys.argv[2]!='-' else '';rng=sys.argv[3] if len(sys.argv)>3 else '0:999';a0,a1=map(int,rng.split(':'))
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    await c.route(re.compile(r'^(?!file:|data:|blob:).*'),lambda r:asyncio.ensure_future(r.abort()))
    pg=await c.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e).split('\n')[0][:200]));pg.on('dialog',lambda d:asyncio.ensure_future(d.dismiss()))
    async def boot():
      await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1300)
      await pg.evaluate(f"S.name='Boudy';S.theme='{theme}';S.ob1=1;S.guide1=1;save()");await pg.reload();await pg.wait_for_timeout(1300)
      await pg.evaluate(SEED);await pg.evaluate(SIG)
      await pg.evaluate("(()=>{window.open=()=>{window._qaHit=(window._qaHit||0)+1;return null};try{navigator.share=async()=>{window._qaHit=(window._qaHit||0)+1}}catch(e){};try{navigator.clipboard.writeText=async()=>{window._qaHit=(window._qaHit||0)+1}}catch(e){};HTMLInputElement.prototype.showPicker=function(){window._qaHit=(window._qaHit||0)+1};const _ic=HTMLElement.prototype.click;HTMLElement.prototype.click=function(){if(this instanceof HTMLInputElement&&this.type==='file'){window._qaHit=(window._qaHit||0)+1;return}return _ic.call(this)}})()")
      return await pg.evaluate("localStorage.getItem('dayRoute.v1')")
    snap=await boot()
    async def reset():
      await pg.evaluate("""(sn)=>{try{document.querySelectorAll('.sheet').forEach(s=>s.classList.remove('on','closing'));['obWiz','appGuide','adkR','lifeR','qReader','chatScreen','celebrate','viewer','coach'].forEach(id=>{const e=document.getElementById(id);if(e)e.classList.remove('on','closing')});document.body.classList.remove('guiding','obon','adkreading','lifeon','reading','sheet-open');if(typeof closeChat==='function'&&document.getElementById('chatScreen').classList.contains('on'))closeChat()}catch(e){}
       localStorage.setItem('dayRoute.v1',sn);S=JSON.parse(sn);window._tipShown=1;if(S.timer)S.timer.running=false}""",snap)
    report={'errors':[],'dead':[],'counts':{}}
    for ci,(name,op) in enumerate(C):
      if flt and flt not in name: continue
      if not a0<=ci<a1: continue
      await reset()
      try: await pg.evaluate(op)
      except Exception as e: report['errors'].append(f'[{name}] OPEN FAILED: {str(e).splitlines()[0][:160]}');continue
      await pg.wait_for_timeout(450)
      els=await pg.evaluate(ENUM,None)
      await pg.screenshot(path=f'/tmp/qrtest/full/{theme}_{name.replace(" ","_")}.png')
      report['counts'][name]=len(els)
      for i,el in enumerate(els):
        if SKIP_TXT.search(el['t']): continue
        await reset()
        try: await pg.evaluate(op)
        except Exception: pass
        await pg.wait_for_timeout(300)
        n0=len(errs)
        try: r=await pg.evaluate(CLICK,i)
        except Exception as e: r='EXC '+str(e).splitlines()[0][:150]
        if 'sig is not defined' in r:
          snap=await boot();await reset()
          try: await pg.evaluate(op)
          except Exception: pass
          await pg.wait_for_timeout(300);n0=len(errs)
          try: r=await pg.evaluate(CLICK,i)
          except Exception as e: r='EXC '+str(e).splitlines()[0][:150]
        await pg.wait_for_timeout(330)
        try: ch=await pg.evaluate("typeof sig==='function'?sig()!==window._sig0:'reload'")
        except Exception: ch=True
        if ch=='reload': snap=await boot();ch=True
        if r!='ok' and r!='MISSING': report['errors'].append(f'[{name}] #{i} «{el["t"]}» {r}')
        if len(errs)>n0: report['errors'].append(f'[{name}] #{i} «{el["t"]}» onclick={el["oc"]} -> {errs[-1]}')
        elif r=='ok' and not ch: report['dead'].append(f'[{name}] #{i} «{el["t"]}» {el["oc"]}')
        if len(errs)>n0 and 'Target closed' in errs[-1]: snap=await boot()
      print(f'{name}: {len(els)} buttons, errors so far {len(report["errors"])}',flush=True)
    json.dump(report,open(f'/tmp/qrtest/full/report_{theme}_{a0}.json','w'),ensure_ascii=False,indent=1)
    print('TOTAL buttons',sum(report['counts'].values()),'errors',len(report['errors']),'no-effect',len(report['dead']))
    await b.close()
asyncio.run(main())
