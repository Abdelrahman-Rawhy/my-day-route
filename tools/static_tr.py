import re,json,asyncio
s=open('/home/claude/MyDayRoute/www/app.html',encoding='utf8').read()
js=s[s.index('<script>'):]
html=s[:s.index('<script>')]
cands=set()
# text between tags (html + templates)
for m in re.finditer(r'>([^<>{}`$]*[A-Za-z][^<>{}`$]*)<',s):
    t=m.group(1).strip()
    if t: cands.add(t)
# string literals in JS
for m in re.finditer(r"'((?:[^'\\\n]|\\.){3,200})'|\"((?:[^\"\\\n]|\\.){3,200})\"",js):
    t=(m.group(1) or m.group(2) or '').replace("\\'","'").strip()
    if t: cands.add(t)
# placeholders/titles
for m in re.finditer(r'(?:placeholder|title|aria-label)="([^"]+)"',s): cands.add(m.group(1))
def ok(t):
    if not re.search(r'[A-Za-z]{3,}',t): return False
    if re.search(r'[=;{}<>\\]|=>|\(\)|^\.|^#|https?:|www\.|\.(js|png|wav|ics|html|css)\b|^[a-z]+[A-Z]|^[a-z_]+$|^[A-Z_]+$|^[a-z-]+:|px\b|rgba|var\(|calc\(',t): return False
    if re.match(r'^[a-z][a-z0-9]*([-_][a-z0-9]+)+$',t): return False
    if len(t)>220: return False
    words=re.findall(r'[A-Za-z]+',t)
    if len(words)==1 and t[0].islower(): return False
    if re.search(r'[؀-ۿ]',t): return False
    return True
C=sorted(c for c in cands if ok(c))
print('candidates',len(C))
json.dump(C,open('/tmp/qrtest/cands.json','w'),ensure_ascii=False)
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context()).new_page()
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1200)
    res=await pg.evaluate("C=>{S.lang='ar';return C.filter(x=>{const r=trS(x);return r===x})}",C)
    json.dump(res,open('/tmp/qrtest/untr_static.json','w'),ensure_ascii=False,indent=0)
    print('untranslated',len(res))
    await b.close()
asyncio.run(main())
