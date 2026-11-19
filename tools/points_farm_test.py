import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844})
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('file:///home/claude/MyDayRoute/www/app.html');await pg.wait_for_timeout(1500)
    r=await pg.evaluate("""async()=>{S.ob1=1;const o={};const P=()=>S.points;const w=ms=>new Promise(r=>setTimeout(r,ms));
     // perfect day farm
     S.tasks=S.tasks.filter(t=>t.prayer);const ids=S.tasks.filter(t=>onDay(t)).map(t=>t.id);ids.forEach(id=>{if(!dayRec().done.includes(id))toggle(id)});const a=P();const last=ids[ids.length-1];
     for(let i=0;i<3;i++){toggle(last);toggle(last)}o.perfect=P()-a;
     // task farm
     TD().items.push({id:'f1',t:'x',pri:0,tags:[],subs:[],status:'todo',created:1});let b=P();for(let i=0;i<5;i++){tdToggle('f1');tdToggle('f1')}o.task=P()-b;
     // goal create/delete farm
     b=P();for(let i=0;i<5;i++){S.goals.push({id:'gg'+i,type:'number',title:'g'+i,start:0,target:10,current:0,unit:'u',created:1});const g=S.goals[S.goals.length-1];if(awCap('gc',3)){g.cpa=today();addPts(5,'x','x')}const btn=document.createElement('button');btn.dataset.sure=1;delGoal(g.id,btn)}o.goalCreate=P()-b;
     // number goal set/reset farm
     S.goals.push({id:'n1',type:'number',title:'n',start:0,target:10,current:0,unit:'u',created:1});b=P();for(let i=0;i<5;i++){numSet('n1',1);numSet('n1',0)}closeSheets();o.numGoal=P()-b;
     // project done/undone farm
     S.goals.push({id:'p1',type:'project',title:'p',area:'study',created:1,steps:[{id:'s1',name:'a',done:false}]});b=P();for(let i=0;i<3;i++){stepToggle('p1','s1');stepToggle('p1','s1')}closeSheets();o.project=P()-b;stepToggle('p1','s1');closeSheets();o.projectOnce=P()-b;
     // khatma +/- farm
     const K=kh();K.page=100;b=P();for(let i=0;i<10;i++){khAdd(1);khAdd(-1)}o.khatma=P()-b;
     // khatma complete without reading
     K.page=600;K.start=today();b=P();khAdd(4);document.getElementById('celebrate').classList.remove('on');o.khComplete=P()-b;
     // ideas cap
     b=P();for(let i=0;i<15;i++)addIdeaText&&0;o.ideaCap=awCap('idea',10)?'ok':'capped';
     // water
     lifeOpen('he');const H=LF().he;H.wg=3;H.water[today()]=2;b=P();for(let i=0;i<3;i++){heWater(1);heWater(-1)}o.water=P()-b;lifeClose();
     // tasbih cap
     b=P();for(let i=0;i<1600;i++)try{tzTap()}catch(e){};o.tasbih=P()-b;
     // reward reserve
     S.points=200;S.rewards=[{id:'r1',name:'x',cost:100,icon:'gift'}];gm().shields=0;claim('r1');o.reserveAsk=document.getElementById('askSheet').classList.contains('on');closeAsk();
     return o}""")
    print(r);print(errs);await b.close()
asyncio.run(main())
