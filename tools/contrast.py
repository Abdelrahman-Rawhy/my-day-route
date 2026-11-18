"""Contrast audit: python3 tools/contrast.py cream|light|dark
Walks the qa.py states and lists visible text whose WCAG contrast against its
background is below 4.5 (3.0 for big/bold text). Backgrounds are composited
from ancestor background colors; gradients use their first and last stop
(the worse one counts). Needs /tmp/qrtest/qa.py (see handoff)."""
import asyncio,sys,importlib.util
spec=importlib.util.spec_from_file_location('qa','/tmp/qrtest/qa.py');qa=importlib.util.module_from_spec(spec);spec.loader.exec_module(qa)
from playwright.async_api import async_playwright
JS=r"""()=>{
const P=s=>{const m=s.match(/rgba?\(([^)]+)\)/);if(!m)return null;const a=m[1].split(/[ ,\/]+/).filter(Boolean).map(Number);return [a[0],a[1],a[2],a.length>3?a[3]:1]};
const over=(t,b)=>{const a=t[3];return [t[0]*a+b[0]*(1-a),t[1]*a+b[1]*(1-a),t[2]*a+b[2]*(1-a),1]};
const L=c=>{const f=v=>{v/=255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4)};return .2126*f(c[0])+.7152*f(c[1])+.0722*f(c[2])};
const CR=(a,b)=>{const x=L(a),y=L(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05)};
const root=P(getComputedStyle(document.documentElement).backgroundColor)||[255,255,255,1];
function bgs(e){let layers=[];for(let n=e;n&&n.nodeType===1;n=n.parentElement){const cs=getComputedStyle(n);const bi=cs.backgroundImage;
  if(bi&&bi.includes('gradient')){const cols=[...bi.matchAll(/rgba?\([^)]+\)/g)].map(m=>P(m[0]));if(cols.length)layers.push({g:[cols[0],cols[cols.length-1]]})}
  const c=P(cs.backgroundColor);if(c&&c[3]>0)layers.push({c});
  if(c&&c[3]>=1&&!(bi&&bi.includes('gradient')))break}
  let outs=[root];for(let i=layers.length-1;i>=0;i--){const l=layers[i];if(l.c)outs=outs.map(b=>over(l.c,b));else outs=outs.flatMap(b=>l.g.map(g=>over(g,b)))}return outs}
const out=[];document.querySelectorAll('.view.on *, .sheet.on *, #lifeR.on *, #chatScreen.on *, .adkr.on *, #celebrate.on *, #qReader.on *, #toast.on *, #pcPill *').forEach(e=>{
  if(![...e.childNodes].some(n=>n.nodeType===3&&n.textContent.trim()))return;
  const r=e.getBoundingClientRect();if(!r.width||!r.height||r.bottom<0||r.top>innerHeight)return;
  const cs=getComputedStyle(e);if(cs.visibility==='hidden'||+cs.opacity<.2)return;
  let op=1;for(let n=e;n&&n.nodeType===1;n=n.parentElement)op*=+getComputedStyle(n).opacity;if(op<.2)return;
  let fg=P(cs.color);if(!fg)return;fg=[fg[0],fg[1],fg[2],fg[3]*op];
  if(cs.webkitTextFillColor&&cs.webkitTextFillColor.includes('rgba(0, 0, 0, 0)'))return;
  const big=parseFloat(cs.fontSize)>=24||(parseFloat(cs.fontSize)>=18.6&&+cs.fontWeight>=700);const need=big?3:4.5;
  const worst=Math.min(...bgs(e).map(b=>CR(over(fg,b),b)));
  if(worst<need)out.push(worst.toFixed(2)+' '+(e.className||e.tagName).toString().slice(0,24)+' «'+[...e.childNodes].filter(n=>n.nodeType===3).map(n=>n.textContent.trim()).join(' ').slice(0,28)+'»')});
return [...new Set(out)]}"""
async def main():
  theme=sys.argv[1] if len(sys.argv)>1 else 'cream'
  async with async_playwright() as p:
    b=await p.chromium.launch();c=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    pg=await c.new_page()
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    await pg.evaluate(f"S.name='Boudy';S.theme='{theme}';save()");await pg.reload();await pg.wait_for_timeout(1500)
    seen={}
    for name,code in qa.STEPS:
      try: await pg.evaluate(code)
      except Exception: pass
      await pg.wait_for_timeout(400)
      for line in await pg.evaluate(JS):
        k=line.split(' ',1)[1]
        if k not in seen: seen[k]=(line,name)
    for k,(line,name) in sorted(seen.items(),key=lambda x:float(x[1][0].split()[0])): print(f'{line}   [{name}]')
    print('TOTAL',len(seen))
    await b.close()
asyncio.run(main())
