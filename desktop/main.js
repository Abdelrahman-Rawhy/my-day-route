// Yawmi for Windows: a small Electron shell around the same web app the phone runs (www/).
// The app is served from 127.0.0.1 on a fixed port so its saved data (localStorage / IndexedDB) always has the same origin.
// The web layer updates itself from GitHub like on the phone; this shell only changes when desktop/ changes.
const {app,BrowserWindow,Tray,Menu,Notification,ipcMain,nativeImage,shell,dialog,session}=require('electron');
const http=require('http'),path=require('path'),fs=require('fs');

const PORT=47219,HOST='127.0.0.1',ORIGIN=`http://${HOST}:${PORT}`;
const ROOT=path.join(__dirname,'www');
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
  const f=path.normalize(path.join(ROOT,p));
  if(!f.startsWith(ROOT+path.sep)){res.writeHead(403);return res.end()}
  fs.readFile(f,(err,data)=>{if(err){res.writeHead(404);return res.end()}
   res.writeHead(200,{'Content-Type':MIME[path.extname(f).toLowerCase()]||'application/octet-stream','Cache-Control':'no-cache'});res.end(data)})});
 srv.on('error',reject);srv.listen(PORT,HOST,resolve)})}

function showWin(){if(!win)return;if(win.isMinimized())win.restore();win.show();win.focus()}

function createWindow(){
 win=new BrowserWindow({width:1320,height:860,minWidth:420,minHeight:560,show:false,autoHideMenuBar:true,backgroundColor:'#F6EFE2',title:'Yawmi',
  icon:path.join(__dirname,'build','icon.png'),
  webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true,spellcheck:false}});
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
 {label:'يشتغل مع ويندوز  ·  Start with Windows',type:'checkbox',checked:loginOn(),click:i=>{setLogin(i.checked)}},
 {type:'separator'},
 {label:'خروج  ·  Quit',click:()=>{quitting=true;app.quit()}}])}
function createTray(){tray=new Tray(nativeImage.createFromPath(path.join(__dirname,'tray.png')));tray.setToolTip('Yawmi · يومي');tray.setContextMenu(trayMenu());tray.on('click',showWin)}

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
 createWindow();createTray();
 if(!app.isPackaged)return;
 // First launch after install: start with Windows (can be turned off from the tray menu).
 const flag=path.join(app.getPath('userData'),'.login-set');if(!fs.existsSync(flag)){setLogin(true);try{fs.writeFileSync(flag,'1')}catch(e){}tray.setContextMenu(trayMenu())}
});
app.on('before-quit',()=>{quitting=true});
app.on('window-all-closed',e=>{/* stay in the tray */});
