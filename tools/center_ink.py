import asyncio,re,io
from PIL import Image
from playwright.async_api import async_playwright
exec(open('/home/claude/MyDayRoute/tools/center_check.py').read().split('CHECK=')[0].split('from playwright')[0])
src=open('/home/claude/MyDayRoute/tools/qa.py').read()
STEPS=eval('['+src.split('STEPS=[')[1].split('\n]\n')[0]+']')
STEPS=[("today","go('today')"),("path","S.routeView='path';go('today')"),("faith adhkar","go('faith');fTab='adhkar';renderFaith()"),("tasbih","fTab='tasbih';renderFaith()"),("khatma","fTab='khatma';renderFaith()"),("tasks","go('tasks');tdMode='list';renderTasks()"),("timer","go('goals')"),("awards","go('awards')"),("settings","go('settings')"),("widget set","openWidgetSet()"),("journal","closeAsk();go('journal')"),("family","go('family')")]
MARK=r"""()=>{const sel='.view.on button,.view.on .iconbtn,.view.on .pill,.view.on .adk-n,.view.on .dp-n,.view.on .td-cb,.view.on .hab .plus,.view.on .hring,.view.on .ico,.view.on .badge,.view.on .tz-c,.view.on .gearbtn,#nav button,.sheet.on button,.sheet.on .ico';const L=[];let i=0;
 document.querySelectorAll('[data-ink],[data-noink]').forEach(e=>{e.removeAttribute('data-ink');e.removeAttribute('data-noink')});
 for(const e of document.querySelectorAll(sel)){const r=e.getBoundingClientRect();if(r.width<14||r.height<14||r.width>200||r.top<0||r.bottom>innerHeight)continue;const cs=getComputedStyle(e);if(cs.display==='none'||cs.visibility==='hidden'||+cs.opacity<.2)continue;
  const centered=(/flex/.test(cs.display)&&cs.justifyContent==='center'&&cs.alignItems==='center')||(cs.display==='grid'&&/center/.test(cs.placeItems+cs.alignItems))||cs.textAlign==='center';if(!centered)continue;
  if(!e.textContent.trim()&&!e.querySelector('svg'))continue;if(e.closest('[data-ink]'))continue;const top=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);if(!top||!(e===top||e.contains(top)))continue;e.dataset.ink=++i;e.querySelectorAll('*').forEach(k=>{const p=getComputedStyle(k).position;if(p==='absolute'||p==='fixed')k.dataset.noink=1});
  const pl=parseFloat(cs.paddingLeft),pr=parseFloat(cs.paddingRight),pt=parseFloat(cs.paddingTop),pb=parseFloat(cs.paddingBottom),bw=parseFloat(cs.borderLeftWidth)||0;
  L.push({i,x:r.left,y:r.top,w:r.width,h:r.height,cx:(r.left+bw+pl+r.right-bw-pr)/2,cy:(r.top+bw+pt+r.bottom-bw-pb)/2,sig:(e.id?'#'+e.id:'')+'.'+[...e.classList].join('.')+'<'+e.tagName.toLowerCase()+'>',t:e.textContent.trim().slice(0,16)})}
 return L}"""
RED="""()=>{const st=document.createElement('style');st.id='inkst';st.textContent='*{transition:none!important;animation:none!important;text-shadow:none!important}[data-ink],[data-ink] *{color:#ff0000!important;-webkit-text-fill-color:#ff0000!important}[data-ink] svg,[data-ink] svg *{stroke:#ff0000!important}[data-ink] svg [fill]:not([fill=none]){fill:#ff0000!important}[data-noink]{visibility:hidden!important}';document.head.appendChild(st)}"""
UNRED="()=>{const s=document.getElementById('inkst');if(s)s.remove()}"
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},device_scale_factor=2,has_touch=True,is_mobile=True)
    pg=await c.new_page();await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate("S.name='Boudy';save();pcShow=()=>{};document.getElementById('pcPill')&&document.getElementById('pcPill').remove()");seen={}
    for name,code in STEPS:
      try: await pg.evaluate(code)
      except Exception as e: print('ERR',name,e)
      await pg.wait_for_timeout(600)
      for scroll in [0,700,1400]:
        await pg.evaluate(f"scrollTo(0,{scroll})");await pg.wait_for_timeout(250)
        L=await pg.evaluate(MARK)
        if not L: continue
        await pg.evaluate(RED);await pg.wait_for_timeout(80)
        img=Image.open(io.BytesIO(await pg.screenshot())).convert('RGB');px=img.load()
        await pg.evaluate(UNRED)
        for e in L:
          x0,y0,x1,y1=int(e['x']*2)+3,int(e['y']*2)+3,int((e['x']+e['w'])*2)-3,int((e['y']+e['h'])*2)-3
          xs=[];ys=[]
          for yy in range(max(0,y0),min(img.height,y1)):
            for xx in range(max(0,x0),min(img.width,x1)):
              r,g,bb=px[xx,yy]
              if r>170 and g<90 and bb<90: xs.append(xx);ys.append(yy)
          if len(xs)<12: continue
          ix=(min(xs)+max(xs))/4;iy=(min(ys)+max(ys))/4;dx=ix-e['cx'];dy=iy-e['cy']
          if abs(dx)>2 or abs(dy)>2:
            k=e['sig']+' "'+e['t']+'"'
            if k not in seen: seen[k]=(name,round(dx,1),round(dy,1),round(e['w']),round(e['h']))
    for k,v in sorted(seen.items(),key=lambda z:-abs(z[1][2])): print(v,k)
asyncio.run(main())
