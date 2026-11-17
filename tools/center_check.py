import asyncio,json,sys,re
from playwright.async_api import async_playwright
src=open('/home/claude/MyDayRoute/tools/qa.py').read()
STEPS=eval('['+src.split('STEPS=[')[1].split('\n]\n')[0]+']')
STEPS=[("faith adhkar","go('faith');fTab='adhkar';renderFaith()"),("faith khatma","fTab='khatma';renderFaith()"),("tasks","go('tasks');tdMode='list';renderTasks()"),("tasks mx","tdMode='mx';renderTasks()"),("tasks board","tdMode='board';renderTasks()"),("tasks cal","tdMode='cal';renderTasks()"),("widget set","go('settings');openWidgetSet()"),("timer menu","closeAsk();go('goals');timerMenu()"),("path","closeSheets();S.routeView='path';go('today')")]+STEPS
CHECK=r"""()=>{const out=[];const els=document.querySelectorAll('.view.on button,.view.on .btn,.view.on .iconbtn,.sheet.on button,#nav button,.view.on .pill,.view.on .chip,.view.on [class*=ring] ,.view.on .adk-n,.view.on .dp-n,.view.on .badge .bi,.view.on .ico,.sheet.on .ico,.upd button,#pcPill button,.view.on .td-cb,.view.on .hab .plus,.view.on .hring');
 for(const e of els){const r=e.getBoundingClientRect();if(r.width<8||r.height<8||r.bottom<0||r.top>innerHeight*3)continue;const cs=getComputedStyle(e);if(cs.visibility==='hidden'||cs.display==='none'||+cs.opacity===0)continue;
  const centered=(/flex/.test(cs.display)&&cs.justifyContent==='center'&&cs.alignItems==='center')||(cs.display==='grid'&&/center/.test(cs.placeItems||cs.alignItems)&&/center/.test(cs.justifyItems+cs.justifyContent))||cs.textAlign==='center'&&/inline-block|block|table-cell|flex/.test(cs.display);
  if(!centered)continue;
  // content box union: text nodes + svg/img/i/span leaf boxes
  let L=1e9,T=1e9,R=-1e9,B=-1e9,n=0;const w=document.createTreeWalker(e,NodeFilter.SHOW_TEXT|NodeFilter.SHOW_ELEMENT);let x;
  while(x=w.nextNode()){let rr;if(x.nodeType===3){if(!x.textContent.trim())continue;const rg=document.createRange();rg.selectNodeContents(x);rr=rg.getBoundingClientRect()}else if(['svg','IMG','img','CANVAS'].includes(x.tagName)){rr=x.getBoundingClientRect()}else continue;
   if(!rr||!rr.width)continue;const pc=getComputedStyle(x.nodeType===3?x.parentElement:x);if(pc.position==='absolute'||pc.position==='fixed')continue;L=Math.min(L,rr.left);T=Math.min(T,rr.top);R=Math.max(R,rr.right);B=Math.max(B,rr.bottom);n++}
  if(!n)continue;const pl=parseFloat(cs.paddingLeft),pr=parseFloat(cs.paddingRight),pt=parseFloat(cs.paddingTop),pb=parseFloat(cs.paddingBottom),bl=parseFloat(cs.borderLeftWidth),bt=parseFloat(cs.borderTopWidth);
  const cx=(r.left+bl+pl+r.right-bl-pr)/2,cy=(r.top+bt+pt+r.bottom-bt-pb)/2,dx=(L+R)/2-cx,dy=(T+B)/2-cy;
  if(Math.abs(dx)>2.5||Math.abs(dy)>2.5){const sig=(e.id?'#'+e.id:'')+'.'+[...e.classList].join('.')+'<'+e.tagName.toLowerCase()+'>';out.push(sig+' dx='+dx.toFixed(1)+' dy='+dy.toFixed(1)+' "'+(e.textContent||'').trim().slice(0,18)+'" '+Math.round(r.width)+'x'+Math.round(r.height))}}
 return out}"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    pg=await c.new_page();await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.name='Boudy';save();pcShow=()=>{}");found={}
    for name,code in STEPS:
      try: await pg.evaluate(code)
      except Exception as e: pass
      await pg.wait_for_timeout(450)
      await pg.evaluate("(()=>{const c=document.getElementById('celebrate');if(c)c.classList.remove('on')})()")
      for o in await pg.evaluate(CHECK):
        k=re.sub(r' dx=.*','',o);found.setdefault(k,(name,o))
    for k,(n,o) in sorted(found.items()): print(n,'|',o)
asyncio.run(main())
