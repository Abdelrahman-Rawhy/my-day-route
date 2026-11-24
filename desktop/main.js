// Yawmi for Windows: a small Electron shell around the same web app the phone runs (www/).
// The app is served from 127.0.0.1 on a fixed port so its saved data (localStorage / IndexedDB) always has the same origin.
// The web layer updates itself from GitHub like on the phone; this shell only changes when desktop/ changes.
const {app,BrowserWindow,Tray,Menu,Notification,ipcMain,nativeImage,shell,dialog,session}=require('electron');
const http=require('http'),path=require('path'),fs=require('fs');

const PORT=47219,HOST='127.0.0.1',ORIGIN=`http://${HOST}:${PORT}`;
const ROOT=path.join(__dirname,'www'),WROOT=path.join(__dirname,'widgets');
const MIME={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png','.jpg':'image/jpeg','.svg':'image/svg+xml','.woff2':'font/woff2','.woff':'font/woff','.ttf':'font/ttf','.otf':'font/otf','.wav':'audio/wav','.mp3':'audio/mpeg','.ico':'image/x-icon'};

let win=null,tray=null,quitting=false;
const startHidden=process.argv.includes('--hidden');

app.setAppUserModelId('com.rawhy.yawmi');
const gotLock=app.requestSingleInstanceLock();if(!gotLock)app.quit();
app.on('second-instance',()=>showWin());

function serve(){return new Promise((resolve,reject)=>{
 const srv=http.createServer((req,res)=>{
  if(req.headers.host!==`${HOST}:${PORT}`){res.writeHead(403);return res.end()}
  let p;try{p=decodeURIComponent(new URL(req.url,ORIGIN).pathname)}catch(e){res.writeHead(400);return res.end()}
  if(p==='/')p='/index.html';
  const base=p.startsWith('/w/')?WROOT:ROOT;const f=path.normalize(path.join(base,p.startsWith('/w/')?p.slice(2):p));
  if(!f.startsWith(base+path.sep)){res.writeHead(403);return res.end()}
  fs.readFile(f,(err,data)=>{if(err){res.writeHead(404);return res.end()}
   res.writeHead(200,{'Content-Type':MIME[path.extname(f).toLowerCase()]||'application/octet-stream','Cache-Control':'no-cache'});res.end(data)})});
 srv.on('error',reject);srv.listen(PORT,HOST,resolve)})}

function showWin(){if(!win)return;if(win.isMinimized())win.restore();win.show();win.focus()}

function createWindow(){
 win=new BrowserWindow({width:1320,height:860,minWidth:420,minHeight:560,show:false,autoHideMenuBar:true,backgroundColor:'#F6EFE2',title:'Yawmi',
  icon:path.join(__dirname,'build','icon.png'),
  webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true,spellcheck:false,backgroundThrottling:false,additionalArguments:['--yawmi-ver='+app.getVersion()]}});
 win.loadURL(ORIGIN+'/index.html');
 win.once('ready-to-show',()=>{if(!startHidden)win.show()});
 // Links: keep the app inside the window, open everything else (web, tel:, mailto:, whatsapp) outside.
 const ext=u=>{if(/^(https?|mailto|tel|whatsapp):/i.test(u))shell.openExternal(u)};
 win.webContents.setWindowOpenHandler(({url})=>{if(!url.startsWith(ORIGIN))ext(url);return {action:'deny'}});
 win.webContents.on('will-navigate',(e,url)=>{if(!url.startsWith(ORIGIN)){e.preventDefault();ext(url)}});
 win.on('close',e=>{if(!quitting){e.preventDefault();win.hide();hintTray()}});
}

function hintTray(){const f=path.join(app.getPath('userData'),'.tray-hint');if(fs.existsSync(f)||!Notification.isSupported())return;try{fs.writeFileSync(f,'1')}catch(e){}new Notification({title:'يومي لسه شغّال · Yawmi is still running',body:'هيفضل يفكّرك بالمواعيد من شريط المهام. للخروج: كليك يمين على الأيقونة ← خروج. · Right-click the tray icon to quit.',icon:path.join(__dirname,'build','icon.png')}).show()}
function loginOn(){try{return app.getLoginItemSettings({args:['--hidden']}).openAtLogin}catch(e){return false}}
function setLogin(v){app.setLoginItemSettings({openAtLogin:!!v,args:['--hidden']})}
function trayMenu(){return Menu.buildFromTemplate([
 {label:'فتح يومي  ·  Open Yawmi',click:showWin},
 {label:'ويدجت  ·  Widgets',submenu:[...WTYPES.map(([k,a,e])=>({label:a+'  ·  '+e,click:()=>wAdd(k)})),{type:'separator'},{label:'إظهار الكل  ·  Show all',click:()=>wwins.forEach(b=>b.showInactive())},{label:'إخفاء الكل  ·  Hide all',click:()=>wwins.forEach(b=>b.hide())}]},
 {label:'يشتغل مع ويندوز  ·  Start with Windows',type:'checkbox',checked:loginOn(),click:i=>{setLogin(i.checked)}},
 {type:'separator'},
 {label:'خروج  ·  Quit',click:()=>{quitting=true;app.quit()}}])}
function createTray(){tray=new Tray(nativeImage.createFromPath(path.join(__dirname,'tray.png')));tray.setToolTip('Yawmi · يومي');tray.setContextMenu(trayMenu());tray.on('click',showWin)}


// ---- Desktop widgets (TickTick-style): frameless cards on the desktop, fed by the app (pushWidget → 'wdata') ----
const WTYPES=[['tasks','المهام (زي TickTick)','Tasks (TickTick style)'],['tbar','الصلاة على شريط المهام','Prayer on the taskbar'],['today','مسار النهارده',"Today's route"],['prayers','مواعيد الصلاة','Prayer times'],['habits','العادات','Habits'],['cds','عدّاد الأيام','Countdown'],['faith','إيمانيات النهارده','Faith today']];
const WSIZE={today:[310,380],prayers:[290,360],tasks:[420,640],tbar:[170,48],habits:[300,320],cds:[290,300],faith:[290,250]};
let WCFG=[],wData=null;const wwins=new Map();
// pin to desktop: pinhelper.exe keeps 'desk' widgets at the bottom of the window stack (see native/pin.cs)
let pinP=null;function pinBottom(b){if(process.platform!=='win32'||!b||b.isDestroyed())return;try{
 if(!pinP){const exe=app.isPackaged?path.join(process.resourcesPath,'pinhelper.exe'):path.join(__dirname,'bin','pinhelper.exe');if(!fs.existsSync(exe))return;
  pinP=require('child_process').spawn(exe,[],{windowsHide:true,stdio:['pipe','ignore','ignore']});pinP.on('exit',()=>{pinP=null});pinP.stdin.on('error',()=>{pinP=null})}
 const h=b.getNativeWindowHandle();const v=h.length>=8?h.readBigUInt64LE(0):BigInt(h.readUInt32LE(0));pinP.stdin.write('bottom '+v.toString()+'\n')}catch(e){}}
function wLayer(c){if(c.layer)return c.layer;return c.top===false?'normal':(c.top===true&&c.type!=='tasks'?'top':'desk')}
function tbarPos(c){const {screen}=require('electron');const d=screen.getPrimaryDisplay(),B=d.bounds,W=d.workArea;const th=B.height-W.height;
 if(th>20&&W.y===B.y){c.h=Math.min(th,60);c.y=W.y+W.height;if(c.x==null)c.x=B.x+180}else{c.h=48;if(c.y==null)c.y=B.y+B.height-48;if(c.x==null)c.x=B.x+180}c.w=c.w||170}
const wFile=()=>path.join(app.getPath('userData'),'widgets.json');
function wLoad(){try{WCFG=JSON.parse(fs.readFileSync(wFile(),'utf8'))||[]}catch(e){WCFG=[]}}
let wST=null;function wSave(){clearTimeout(wST);wST=setTimeout(()=>{try{fs.writeFileSync(wFile(),JSON.stringify(WCFG))}catch(e){}},400)}
function wCfgOf(b){for(const [id,w] of wwins)if(w===b)return WCFG.find(c=>c.id===id);return null}
function wOpen(c){const {screen}=require('electron');const wa=screen.getPrimaryDisplay().workArea;const [dw,dh]=WSIZE[c.type]||[300,340];
 if(c.type==='tbar')tbarPos(c);
 if(c.w==null){c.w=dw;c.h=dh}if(c.x==null){const n=WCFG.indexOf(c);c.x=c.type==='tasks'?wa.x+16:wa.x+wa.width-c.w-24-(n%3)*30;c.y=c.type==='tasks'?wa.y+wa.height-c.h-16:wa.y+24+(n%3)*30}
const TB=c.type==='tbar';const vis=TB||screen.getAllDisplays().some(d=>{const a=d.workArea;return c.x<a.x+a.width-40&&c.x+c.w>a.x+40&&c.y<a.y+a.height-40&&c.y+30>a.y});if(!vis){c.x=wa.x+wa.width-c.w-24;c.y=wa.y+24}
 const b=new BrowserWindow({x:c.x,y:c.y,width:c.w,height:c.h,minWidth:TB?120:220,minHeight:TB?30:160,frame:false,transparent:true,resizable:!c.lock&&!TB,movable:!c.lock,skipTaskbar:true,alwaysOnTop:TB||wLayer(c)==='top',minimizable:false,maximizable:false,fullscreenable:false,hasShadow:false,show:false,title:'Yawmi widget',
  webPreferences:{preload:path.join(__dirname,'wpreload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true,spellcheck:false}});
 b.loadURL(ORIGIN+'/w/widget.html?type='+encodeURIComponent(c.type));
 b.once('ready-to-show',()=>{b.showInactive();if(TB)b.setAlwaysOnTop(true,'screen-saver');else if(wLayer(c)==='desk')setTimeout(()=>pinBottom(b),300)});
 b.on('blur',()=>{if(!TB&&wLayer(c)==='desk')pinBottom(b)});
 if(TB){const iv=setInterval(()=>{if(b.isDestroyed())return clearInterval(iv);if(b.isVisible()){b.setAlwaysOnTop(true,'screen-saver');b.moveTop()}},4000)}
 const keep=()=>{const r=b.getBounds();Object.assign(c,{x:r.x,y:r.y,w:r.width,h:r.height});wSave()};b.on('moved',keep);b.on('resized',keep);
 b.on('closed',()=>wwins.delete(c.id));b.webContents.setWindowOpenHandler(()=>({action:'deny'}));b.webContents.on('will-navigate',e=>e.preventDefault());
 wwins.set(c.id,b)}
function wAdd(type){if(type==='tbar'){const ex=WCFG.find(x=>x.type==='tbar');if(ex){const b=wwins.get(ex.id);if(b)b.showInactive();return}}const c={id:'w'+Date.now().toString(36),type,layer:type==='tbar'?'top':'desk',op:type==='tbar'?1:.86,lock:false,view:'inbox'};WCFG.push(c);wSave();wOpen(c)}
function wApply(c){const b=wwins.get(c.id);if(!b)return;const L=wLayer(c);if(c.type!=='tbar'){b.setAlwaysOnTop(L==='top');if(L==='desk')pinBottom(b)}b.setResizable(!c.lock);b.setMovable(!c.lock);b.webContents.send('w-cfg',c);wSave()}
ipcMain.on('wdata',(e,d)=>{wData=d;wwins.forEach(b=>{try{b.webContents.send('w-data',d)}catch(x){}})});
ipcMain.handle('w-add',(e,type)=>{if(WTYPES.some(t=>t[0]===type))wAdd(type);return WCFG.length});
ipcMain.on('w-ready',e=>{const b=BrowserWindow.fromWebContents(e.sender),c=b&&wCfgOf(b);if(c)e.sender.send('w-cfg',c);if(wData)e.sender.send('w-data',wData)});
ipcMain.on('w-view',(e,v)=>{const b=BrowserWindow.fromWebContents(e.sender),c=b&&wCfgOf(b);if(c&&typeof v==='string'&&v.length<200){c.view=v;wSave()}});
ipcMain.on('w-act',(e,x)=>{if(!x)return;if(x.k==='open')showWin();if(win)win.webContents.send('w-act',x)});
ipcMain.on('w-menu',e=>{const b=BrowserWindow.fromWebContents(e.sender),c=b&&wCfgOf(b);if(!c)return;const t=WTYPES.find(x=>x[0]===c.type)||WTYPES[0];
 Menu.buildFromTemplate([{label:t[1]+'  ·  '+t[2],enabled:false},{type:'separator'},
  ...(c.type==='tbar'?[]:[{label:'ثبّت على سطح المكتب (ورا البرامج)  ·  Pin to desktop',type:'radio',checked:wLayer(c)==='desk',click:()=>{c.layer='desk';wApply(c)}},{label:'فوق كل البرامج  ·  Always on top',type:'radio',checked:wLayer(c)==='top',click:()=>{c.layer='top';wApply(c)}},{label:'نافذة عادية  ·  Normal window',type:'radio',checked:wLayer(c)==='normal',click:()=>{c.layer='normal';wApply(c)}},{type:'separator'}]),
  {label:'الشفافية  ·  Background',submenu:[[1,'100%'],[.86,'85%'],[.7,'70%'],[.5,'50%'],[.3,'30%'],[0,'0%']].map(([v,n])=>({label:n,type:'radio',checked:Math.abs((c.op==null?.86:c.op)-v)<.01,click:()=>{c.op=v;wApply(c)}}))},
  {label:'ثبّت المكان والحجم  ·  Lock position',type:'checkbox',checked:!!c.lock,click:i=>{c.lock=i.checked;wApply(c)}},
  {type:'separator'},{label:'افتح يومي  ·  Open Yawmi',click:showWin},
  {label:'اقفل الويدجت ده  ·  Remove widget',click:()=>{WCFG=WCFG.filter(x=>x.id!==c.id);wSave();b.destroy()}}]).popup({window:b})});

// ---- Reminders: the app hands its list here; they fire even when the window is closed to the tray ----
const timers=new Map();
function clearOne(id){const t=timers.get(id);if(t){clearTimeout(t.h);timers.delete(id)}}
function fire(n){timers.delete(n.id);if(!Notification.isSupported())return;
 const no=new Notification({title:n.title||'Yawmi',body:n.body||'',icon:path.join(__dirname,'build','icon.png'),silent:false});
 no.on('click',()=>{showWin();if(win)win.webContents.send('ln-action',{actionId:'tap',notification:{id:n.id,title:n.title,body:n.body,extra:n.extra||{}}})});
 no.show()}
ipcMain.handle('ln',(e,op,arg)=>{
 if(op==='pending')return {notifications:[...timers.values()].map(t=>({id:t.n.id}))};
 if(op==='cancel'){(arg||[]).forEach(clearOne);return true}
 if(op==='schedule'){(arg||[]).forEach(n=>{if(!n||n.id==null)return;clearOne(n.id);const d=(+n.at||0)-Date.now();
  if(d<-60000)return;if(d>20*864e5)return;timers.set(n.id,{n,h:setTimeout(()=>fire(n),Math.max(0,d))})});return true}
 return null});
ipcMain.handle('login',(e,op,v)=>{if(op==='set')setLogin(v);return loginOn()});

app.whenReady().then(async()=>{if(!gotLock)return;
 session.defaultSession.setPermissionRequestHandler((wc,perm,cb)=>cb(['notifications','media','clipboard-sanitized-write','clipboard-read','fullscreen'].includes(perm)));
 try{await serve()}catch(e){dialog.showErrorBox('Yawmi','Yawmi could not start because port '+PORT+' is in use. Close the other program or restart the PC.\n\n'+e.message);app.quit();return}
 createWindow();wLoad();createTray();WCFG.forEach(wOpen);
 if(!app.isPackaged)return;
 // First launch after install: start with Windows (can be turned off from the tray menu).
 const flag=path.join(app.getPath('userData'),'.login-set');if(!fs.existsSync(flag)){setLogin(true);try{fs.writeFileSync(flag,'1')}catch(e){}tray.setContextMenu(trayMenu())}
});
app.on('before-quit',()=>{quitting=true});
app.on('window-all-closed',e=>{/* stay in the tray */});
