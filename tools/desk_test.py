import asyncio
from playwright.async_api import async_playwright
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch();pg=await b.new_page(viewport={'width':1366,'height':800})
        errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
        await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
        await pg.evaluate("try{obFinish()}catch(e){};document.getElementById('celebrate')&&document.getElementById('celebrate').classList.remove('on');0")
        await pg.evaluate("S.todo.items.push({id:'a1',t:'كلم دكتور الأسنان',list:null,pri:2,due:today(),time:'17:00',rem:null,rep:null,tags:[],notes:'',subs:[],status:'todo',created:Date.now()});S.goals.push({id:'g1',title:'مشروع التخرج',type:'project',steps:[{t:'1',d:false}]});save();render()")
        for th in ['cream','dark','light']:
            await pg.evaluate(f"S.theme='{th}';applyTheme&&applyTheme();go('today');window.scrollTo(0,0)");await pg.wait_for_timeout(500)
            await pg.screenshot(path=f'/tmp/qrtest/d152_{th}_today.png')
        await pg.evaluate("S.theme='cream';applyTheme();go('tasks')");await pg.wait_for_timeout(500);await pg.screenshot(path='/tmp/qrtest/d152_tasks.png')
        await pg.evaluate("tdOpen('a1')");await pg.wait_for_timeout(500);await pg.screenshot(path='/tmp/qrtest/d152_sheet.png')
        await pg.keyboard.press('Escape');await pg.wait_for_timeout(400)
        r=await pg.evaluate("document.querySelector('#tdSheet').classList.contains('on')");print('sheet open after esc',r)
        await pg.keyboard.press('2');await pg.wait_for_timeout(400);print('view',await pg.evaluate("document.querySelector('.view.on').id"))
        await pg.keyboard.press('n');await pg.wait_for_timeout(400);print('focus',await pg.evaluate("document.activeElement.id"))
        # narrow again -> back to phone layout order
        await pg.set_viewport_size({'width':400,'height':800});await pg.evaluate("go('today')");await pg.wait_for_timeout(500)
        print('cols gone',await pg.evaluate("!document.getElementById('dkCols')"),await pg.evaluate("[...document.getElementById('v-today').children].map(e=>e.id||e.className).join(',')"))
        await pg.screenshot(path='/tmp/qrtest/d152_narrow.png')
        print('errs',errs)
        await b.close()
asyncio.run(main())
