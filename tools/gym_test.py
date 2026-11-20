import asyncio
from playwright.async_api import async_playwright
from PIL import Image
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844})
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1200)
    await pg.evaluate("S.ob1=1;S.theme='dark';save()");await pg.reload();await pg.wait_for_timeout(1200)
    await pg.evaluate("document.getElementById('celebrate').classList.remove('on');S.tips=Object.assign(S.tips||{},{he:1,ib:1,mo:1,le:1});lifeOpen('gy')");await pg.wait_for_timeout(500)
    await pg.screenshot(path='/tmp/qrtest/g1.png')
    # routine + workout
    r=await pg.evaluate("""async()=>{const w=ms=>new Promise(r=>setTimeout(r,ms));const G=GY();G.rt.push({id:'r1',n:'Push',ex:['bp','ohp','tpd']});gyStart('r1');await w(200);
      gySetV(0,0,'kg',60);gySetV(0,0,'r',10);gySetOk(0,0);gySetV(0,1,'kg',62.5);gySetV(0,1,'r',8);gySetOk(0,1);await w(300);const rest=!!document.querySelector('.gy-rest');return {rest,sets:G.cur.ex[0].sets.filter(t=>t.ok).length}}""")
    print(r);await pg.wait_for_timeout(300);await pg.screenshot(path='/tmp/qrtest/g2.png')
    r=await pg.evaluate("""async()=>{const w=ms=>new Promise(r=>setTimeout(r,ms));gyRestStop();const p0=S.points;gyFinish();await w(900);document.getElementById('celebrate').classList.remove('on');const G=GY();
      // second session beats record
      gyStart('r1');await w(100);gySetV(0,0,'kg',65);gySetV(0,0,'r',6);gySetOk(0,0);gyRestStop();gyFinish();await w(900);document.getElementById('celebrate').classList.remove('on');
      return {ses:G.ses.length,best:gyBest('bp'),work:LF().he.work[today()],pts:S.points-p0,cur:G.cur}}""")
    print(r);await pg.wait_for_timeout(300);await pg.screenshot(path='/tmp/qrtest/g3.png')
    await pg.evaluate("gyChart('bp')");await pg.wait_for_timeout(400);await pg.screenshot(path='/tmp/qrtest/g4.png')
    # sleep mode
    r=await pg.evaluate("""async()=>{closeAsk();lifeOpen('he');slSleep();const on=!!LF().he.slOn;LF().he.slOn.at-=7.5*36e5;slWake();return {on,slept:JSON.stringify(LF().he.sleep[today()]),off:!LF().he.slOn}}""")
    print(r)
    ims=[Image.open(f'/tmp/qrtest/g{i}.png') for i in (1,2,3,4)];out=Image.new('RGB',(1560,844));[out.paste(im,(i*390,0)) for i,im in enumerate(ims)];out.save('/tmp/qrtest/gym.png');print(errs)
    await b.close()
asyncio.run(main())
