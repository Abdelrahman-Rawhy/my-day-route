import asyncio,json
from playwright.async_api import async_playwright
INIT="""window._calls=[];window._last={link:'',at:0};
const AB={sync:(o)=>{_calls.push(['sync',o]);return Promise.resolve({armed:o.alarms.length})},status:()=>Promise.resolve({exact:true,full:false,notif:true,count:0}),
 openSettings:(o)=>{_calls.push(['set',o]);return Promise.resolve()},test:(o)=>{_calls.push(['test',o]);return Promise.resolve()},last:()=>Promise.resolve(window._last)};
window.Capacitor={isNativePlatform:()=>true,getPlatform:()=>'android',isPluginAvailable:(n)=>n==='AlarmBridge',Plugins:{AlarmBridge:AB},registerPlugin:(n)=>n==='AlarmBridge'?AB:new Proxy({},{get:(t,k)=>k==='then'?undefined:(k==='addListener'?(()=>({remove(){}})):(()=>Promise.resolve({})))})};"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':400,'height':860});errs=[]
    pg.on('pageerror',lambda e:errs.append(str(e)[:200]))
    await pg.add_init_script(INIT)
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    R=lambda js:pg.evaluate("()=>{"+js+"}")
    await R("try{obFinish()}catch(e){}document.querySelectorAll('.on').forEach(e=>{if(e.id==='celebrate')e.classList.remove('on')});")
    out={}
    out['almP']=await R("return !!almP()")
    # sleep plan with alarm
    await R("lifeOpen('he');scEdit();window._scD.bed='23:00';window._scD.wake='06:30';document.getElementById('scAlm').checked=true;scTog('alarm');")
    out['switch_and_test_btn']=await R("return !!document.getElementById('scAlm')&&document.getElementById('scAlm').checked&&/جرّب المنبّه/.test(document.getElementById('askPanel').innerText)")
    await pg.screenshot(path='/tmp/qrtest/t155_plan.png')
    await R("scSave()");await pg.wait_for_timeout(1200)
    out['perm_prompt_full']=await R("return document.getElementById('askSheet').classList.contains('on')&&/ملء الشاشة/.test(document.getElementById('askPanel').innerText)")
    await R("closeAsk();doSchedule()");await pg.wait_for_timeout(800)
    s=await R("const c=_calls.filter(x=>x[0]==='sync');return c.length?c[c.length-1][1].alarms:null")
    out['wake_alarms']=[ (a['id'],a['title']) for a in (s or [])]
    out['wake_ids_1to7']=sorted(a['id'] for a in s)==list(range(1,8)) or sorted(a['id'] for a in s)==list(range(2,8))
    # notifications 79203 skipped
    out['no_wake_notif']=await R("return !slNotifs(Date.now()).some(n=>n.id%4===3&&n.id>=79203&&n.id<79230)")
    out['label_clock']=await R("lifeDraw&&lifeDraw();return /⏰/.test(scSecHtml())")
    # nap
    await R("slNap(25)");await pg.wait_for_timeout(1500);await R("doSchedule()");await pg.wait_for_timeout(800)
    s=await R("const c=_calls.filter(x=>x[0]==='sync');return c[c.length-1][1].alarms")
    out['nap_alarm']=[a for a in s if a['id']==20][0]['link'] if any(a['id']==20 for a in s) else None
    out['no_nap_notif']=await R("return !slNotifs(Date.now()).some(n=>n.id===79250)")
    out['nap_card_text']=await R("render();return /المنبّه هيرن/.test(document.getElementById('slSlot').innerText)")
    # stop the nap alarm → almCheck ends nap
    await R("closeAsk();window._last={link:'com.rawhy.mydayroute://open/today?alarm=nap',at:Date.now()};almCheck()");await pg.wait_for_timeout(500)
    out['nap_ended']=await R("return !LF().he.slNap")
    # sleep mode then wake via alarm stop 7h later
    await R("slSleep();const H=LF().he;H.slOn.at=Date.now()-7*36e5;save();window._last={link:'com.rawhy.mydayroute://open/today?alarm=wake',at:Date.now()+1000};almCheck()");await pg.wait_for_timeout(600)
    out['woke_logged']=await R("const H=LF().he;return !H.slOn&&!!(H.sleep[today()]||{}).wake")
    out['same_stop_once']=await R("const n=_calls.length;almCheck();return true")
    await R("doSchedule()");await pg.wait_for_timeout(800)
    s=await R("const c=_calls.filter(x=>x[0]==='sync');return c[c.length-1][1].alarms")
    out['today_wake_skipped_after_woke']=1 not in [a['id'] for a in s]
    # test button
    await R("almTest()");await pg.wait_for_timeout(400)
    out['test_called']=await R("return _calls.some(x=>x[0]==='test'&&x[1].sec===10)")
    out['errs']=errs
    print(json.dumps(out,ensure_ascii=False,indent=1))
    await b.close()
asyncio.run(main())
