import asyncio,json,sys
from playwright.async_api import async_playwright
RATE=int(sys.argv[1]) if len(sys.argv)>1 else 4
SEED="""S.ob1=1;S.name='Boudy';for(let i=0;i<6;i++)S.tasks.push({id:'u'+i,own:1,name:'خطوة '+i,time:(8+i)+':00',pts:10,icon:'book',prayer:null});
for(let i=0;i<30;i++)TD().items.push({id:'t'+i,t:'مهمة '+i,pri:i%4,due:dzK(i%5-2),tags:[],subs:[],status:'todo',created:1});
for(let i=0;i<6;i++)S.goals.push({id:'g'+i,type:'project',title:'هدف '+i,area:'study',icon:'graduation',created:1,steps:[{id:'a',name:'x',done:false}]});
for(let d=1;d<120;d++)S.history[dzK(-d)]={done:S.tasks.slice(0,d%7).map(t=>t.id),pts:20};save()"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},device_scale_factor=2.5,is_mobile=True,has_touch=True);pg=await c.new_page()
    cdp=await c.new_cdp_session(pg)
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate(SEED);
    await cdp.send('Emulation.setCPUThrottlingRate',{'rate':RATE})
    await pg.add_init_script("""window.__lt=[];new PerformanceObserver(l=>l.getEntries().forEach(e=>window.__lt.push([Math.round(e.startTime),Math.round(e.duration)]))).observe({type:'longtask',buffered:true});
    const _si=setInterval;window.__iv=[];window.setInterval=function(f,ms){window.__iv.push(ms);return _si.apply(this,arguments)}""")
    await pg.reload();await pg.wait_for_timeout(4000)
    nav=await pg.evaluate("(()=>{const n=performance.getEntriesByType('navigation')[0];const p=performance.getEntriesByType('paint');return {dcl:Math.round(n.domContentLoadedEventEnd),load:Math.round(n.loadEventEnd),paint:p.map(x=>[x.name,Math.round(x.startTime)])}})()")
    lt=await pg.evaluate("window.__lt");iv=await pg.evaluate("window.__iv")
    print('RATE',RATE,'nav',nav);print('long tasks at start',lt[:15],'total',sum(x[1] for x in lt));print('intervals',iv)
    await pg.evaluate("document.getElementById('celebrate').classList.remove('on');try{obFinish()}catch(e){}")
    res={}
    for v in ['faith','tasks','goals','journal','family','awards','settings','today']:
      t=await pg.evaluate(f"(()=>{{const t=performance.now();go('{v}');return performance.now()-t}})()")
      await pg.wait_for_timeout(400);res[v]=round(t)
    print('go() ms',res)
    r=await pg.evaluate("(()=>{const t=performance.now();render();return Math.round(performance.now()-t)})()");print('render() ms',r)
    for name,code in [('adhkar reader',"openAdhkar('morning')"),('close reader',"adkRExit&&adkRExit()"),('mushaf page',"openPage(50)"),('life',"lifeOpen('he')"),('sheet',"lifeClose();openQuick()")]:
      t=await pg.evaluate(f"(async()=>{{const t=performance.now();await {code};return Math.round(performance.now()-t)}})()");await pg.wait_for_timeout(800);print(name,t)
    await pg.evaluate("closeSheets();try{qrClose()}catch(e){};go('today');scrollTo(0,0)")
    await pg.wait_for_timeout(600)
    fr=await pg.evaluate("""new Promise(res=>{const ts=[];let y=0;function f(t){ts.push(t);y+=40;scrollTo(0,y);if(ts.length<120)requestAnimationFrame(f);else{const d=ts.slice(1).map((x,i)=>x-ts[i]);d.sort((a,b)=>a-b);res({avg:Math.round(d.reduce((a,b)=>a+b)/d.length*10)/10,p95:Math.round(d[Math.floor(d.length*.95)]),max:Math.round(d[d.length-1]),slow:d.filter(x=>x>34).length})}}requestAnimationFrame(f)})""")
    print('scroll frames ms',fr)
    lt2=await pg.evaluate("window.__lt.slice(-20)");print('recent long tasks',lt2)
    await b.close()
asyncio.run(main())
