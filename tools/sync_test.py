import asyncio,json
from playwright.async_api import async_playwright
JS=r"""
async()=>{const out={};
 // mock cloud
 const CLOUD={};window.CLOUD=CLOUD;
 const ref=(path)=>({path,collection:n=>({doc:id=>ref(path+'/'+n+'/'+id),onSnapshot:()=>()=>{}}),get:async()=>({exists:path in CLOUD,data:()=>CLOUD[path]}),set:async d=>{CLOUD[path]=JSON.parse(JSON.stringify(d))}});
 FB.ok=true;FB.uid='u1';FB.db={collection:n=>({doc:id=>ref(n+'/'+id)}),runTransaction:async fn=>{const writes=[];const tx={get:r=>r.get(),set:(r,d)=>writes.push([r,d])};const res=await fn(tx);for(const [r,d] of writes)await r.set(d);return res}};
 window.firebase=Object.assign(window.firebase||{},{auth:()=>({currentUser:{isAnonymous:false}})});
 const k=await crypto.subtle.generateKey({name:'AES-GCM',length:256},true,['encrypt','decrypt']);const raw=new Uint8Array(await crypto.subtle.exportKey('raw',k));const MKB=b64(raw);
 const pull=async()=>{SYR={};MK=MK||await impKey(S.mk);for(const p in CLOUD){const m=p.match(/^users\/u1\/sync\/(.+)$/);if(m)SYR[m[1]]=await openJ(MK,CLOUD[p].e)}};
 const t=today();
 // ---- device A (phone) ----
 S.mk=MKB;S.theme='cream';S.todo.items=[{id:'x1',t:'Call Ali',status:'todo'},{id:'x2',t:'Buy milk',status:'todo'}];S.goals=[{id:'g1',title:'Read'}];dayRec().done=['t1'];S.name='Ahmed';delete S._mt;
 syReady=true;syStamp(0);await syRun();
 out.docs=Object.keys(CLOUD);
 const A=JSON.parse(JSON.stringify(S));
 // ---- device B (desktop, fresh) ----
 const fresh=JSON.parse(localStorage.getItem('freshB'));S=fresh;S.mk=MKB;S.theme='dark';MK=null;await pull();syStamp(1);await syRun();
 out.B1=[S.name,S.todo.items.map(x=>x.id+':'+x.t).join(','),S.goals.map(g=>g.id).join(','),JSON.stringify((S.history[t]||{}).done),S.theme];
 // concurrent edits
 const B=S;B.todo.items.find(x=>x.id==='x1').t='Call Ali at 5';B.history[t].done.push('t3');B.goals=[];
 S=A;MK=null;await pull();syStamp(0); // A sees nothing new yet; edits:
 S.todo.items.push({id:'x3',t:'Gym',status:'todo'});S.history[t].done.push('t2');
 const SYR_A_stale=JSON.parse(JSON.stringify(SYR));await syRun();const A2=S;
 S=B;MK=null;SYR=JSON.parse(JSON.stringify(SYR_A_stale));// B has stale remote view
 await new Promise(r=>setTimeout(r,5));await syRun(); // transaction merges A's push
 out.dbg=[Object.keys(S._mt).filter(p=>p.startsWith('goals')).map(p=>p+'='+S._mt[p].join('|')).join(' ; '),Object.keys(SYR.m.t).filter(p=>p.startsWith('goals')).map(p=>p+'='+SYR.m.t[p].join('|')).join(' ; '),typeof S.goals, Object.keys(S).length];
 try{out.B2=[JSON.stringify(S.todo),JSON.stringify(S.history[t]),JSON.stringify(S.goals)]}catch(e){out.B2=String(e)}
 S=A2;MK=null;await pull();await syRun();
 try{out.A3=[S.todo.items.map(x=>x.id+':'+x.t).join(','),[...S.history[t].done].sort().join(','),JSON.stringify(S.goals),S.theme]}catch(e){out.A3=String(e)+' '+JSON.stringify(Object.keys(S))}
 // no-op run writes nothing
 const mt0=JSON.parse(JSON.stringify(S._mt));syStamp();out.chg=Object.keys(S._mt).filter(p=>JSON.stringify(S._mt[p])!==JSON.stringify(mt0[p])).map(p=>p+' '+JSON.stringify(mt0[p])+'→'+JSON.stringify(S._mt[p])).slice(0,12);
 const before=JSON.stringify(CLOUD);await syRun();const b2=JSON.stringify(CLOUD);await syRun();out.noop=[before===b2,b2===JSON.stringify(CLOUD)];
 out.size=Object.fromEntries(Object.entries(CLOUD).map(([k,v])=>[k.split('/').pop(),JSON.stringify(v).length]));
 out.units=Object.keys(S._mt).length;
 return out}
"""
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch();pg=await b.new_page()
        errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
        await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
        await pg.evaluate("try{obFinish()}catch(e){};localStorage.setItem('freshB',JSON.stringify(S))")
        print(json.dumps(await pg.evaluate(JS),ensure_ascii=False,indent=0))
        print('errs',errs[:5])
        await b.close()
asyncio.run(main())
