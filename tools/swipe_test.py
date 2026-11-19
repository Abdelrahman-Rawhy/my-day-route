import asyncio
from playwright.async_api import async_playwright
async def drag(cdp,x,y0,y1,steps=10):
  await cdp.send('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[{'x':x,'y':y0}]})
  for i in range(1,steps+1):
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchMove','touchPoints':[{'x':x,'y':y0+(y1-y0)*i/steps}]});await asyncio.sleep(0.016)
  await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]})
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':700},has_touch=True,is_mobile=True);pg=await c.new_page();cdp=await c.new_cdp_session(pg)
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.ob1=1;document.getElementById('celebrate').classList.remove('on');openTask()");await pg.wait_for_timeout(700)
    await pg.evaluate("document.querySelector('#taskSheet .panel').scrollTop=250");await pg.wait_for_timeout(200)
    await drag(cdp,200,300,500);await pg.wait_for_timeout(600)
    print('scrolled-start drag: open=',await pg.evaluate("document.getElementById('taskSheet').classList.contains('on')"),'scrollTop',await pg.evaluate("document.querySelector('#taskSheet .panel').scrollTop"))
    await pg.evaluate("document.querySelector('#taskSheet .panel').scrollTop=0");await pg.wait_for_timeout(200)
    bb=await pg.evaluate("(()=>{const r=document.querySelector('#taskSheet .panel h3').getBoundingClientRect();return [r.x+r.width/2,r.y+r.height/2]})()");print(bb)
    await drag(cdp,bb[0],bb[1],bb[1]+250);await pg.wait_for_timeout(900)
    print('top drag: open=',await pg.evaluate("document.getElementById('taskSheet').classList.contains('on')"))
    await b.close()
asyncio.run(main())
