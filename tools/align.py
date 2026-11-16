import asyncio,json,sys
from playwright.async_api import async_playwright
STEPS=open('/tmp/qrtest/qa.py').read().split('STEPS=[')[1].split('\n]\n')[0]
exec('STEPS=['+STEPS+'\n]')
STEPS+= [("new goal pick","closeSheets();openNewGoal()")]
CHK="""()=>{const o=[];document.querySelectorAll('.view.on *, .sheet.on *').forEach(e=>{const t=[...e.childNodes].filter(n=>n.nodeType===3).map(n=>n.nodeValue).join('').trim();if(!t||!/[\\u0600-\\u06FF]/.test(t))return;const cs=getComputedStyle(e);if(cs.display==='inline'||cs.display==='inline-flex'&&false)return;const r=e.getBoundingClientRect();if(!r.width)return;
 if(cs.textAlign==='left'||(cs.direction==='ltr'&&!e.closest('[dir=ltr],input,.adk-n,.tp-big'))){o.push((e.className||e.tagName)+' ['+cs.textAlign+'/'+cs.direction+'] '+t.slice(0,25))}});return [...new Set(o)].slice(0,12)}"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844})
    pg=await c.new_page()
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    allp={}
    for name,code in STEPS:
      try: await pg.evaluate(code)
      except: pass
      await pg.wait_for_timeout(250)
      await pg.evaluate("(()=>{const c=document.getElementById('celebrate');if(c)c.classList.remove('on')})()")
      r=await pg.evaluate(CHK)
      if r: allp[name]=r
    print(json.dumps(allp,ensure_ascii=False,indent=0));await b.close()
asyncio.run(main())
