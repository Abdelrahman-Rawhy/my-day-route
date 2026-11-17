import asyncio
from playwright.async_api import async_playwright
exec(open('/home/claude/MyDayRoute/tools/center_check.py').read().split('CHECK=')[0].split('async def')[0].replace('from playwright.async_api import async_playwright',''))
BAR=r"""()=>{const out=[];const sel='.mini-bar>i,.pbar>i,.adk-pb>i,.gq-bar>i,.g-xp>i,.kh-fill,.kh-today>i,i.bar,.apk-bar>i,.adk-top .pbar i,progress,[class*=bar]>i,[class*=prog]>i';
 document.querySelectorAll(sel).forEach(e=>{const r=e.getBoundingClientRect(),p=e.parentElement.getBoundingClientRect();if(!r.width||!p.width||r.top>innerHeight*4)return;const v=e.closest('.view,.sheet,#updBox');if(v&&!(v.classList.contains('on')||v.id==='updBox'))return;
  const w=parseFloat(e.style.width);const tr=getComputedStyle(e).transitionProperty+' '+getComputedStyle(e).transitionDuration;const rtl=getComputedStyle(e.parentElement).direction==='rtl';
  const sig=(e.parentElement.className||e.parentElement.tagName)+'>'+(e.className||e.tagName);
  if(isNaN(w)&&e.style.width)out.push('BADWIDTH '+sig+' '+e.style.width);if(w>100.5||w<0)out.push('RANGE '+sig+' '+w);
  if(r.width>p.width+1)out.push('OVERFLOW '+sig);
  if(rtl&&r.width<p.width-2&&Math.abs(r.right-p.right)>3&&Math.abs(r.left-p.left)<3)out.push('LTR-FILL-in-RTL '+sig);
  if(!/width|all/.test(tr)||/^\S+ 0s$/.test(tr))out.push('NO-TRANSITION '+sig+' ['+tr+']')});return out}"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    pg=await c.new_page();await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.name='Boudy';save();pcShow=()=>{};addExample(1);addExample(4);toggle(S.tasks.find(x=>!x.prayer).id)");found={}
    extra=[("today","go('today')"),("awards","go('awards')"),("adhkar","go('faith');fTab='adhkar';renderFaith();adkTap(1)"),("khatma","fTab='khatma';renderFaith()"),("quran","fTab='quran';renderFaith()"),("apk","showApk({apk:200},{code:1});document.getElementById('apkProg').hidden=false")]
    for name,code in extra+STEPS:
      try: await pg.evaluate(code)
      except Exception: pass
      await pg.wait_for_timeout(400)
      for o in await pg.evaluate(BAR): found.setdefault(o,name)
    for k,v in sorted(found.items()): print(v,'|',k)
asyncio.run(main())
