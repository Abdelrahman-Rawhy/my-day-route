// Bridge between the web app and the Windows shell. The app sees window.yawmiDesk.LN with the same calls it uses
// for Capacitor LocalNotifications on the phone (schedule / cancel / getPending / addListener), so doSchedule() works unchanged.
const {contextBridge,ipcRenderer}=require('electron');
const subs=[];
ipcRenderer.on('ln-action',(e,a)=>subs.forEach(f=>{try{f(a)}catch(x){}}));
const toList=o=>((o&&o.notifications)||[]);
contextBridge.exposeInMainWorld('yawmiDesk',{
 platform:process.platform,
 LN:{
  checkPermissions:async()=>({display:'granted'}),
  requestPermissions:async()=>({display:'granted'}),
  createChannel:async()=>true,
  registerActionTypes:async()=>true,
  getPending:()=>ipcRenderer.invoke('ln','pending'),
  cancel:o=>ipcRenderer.invoke('ln','cancel',toList(o).map(n=>n.id)),
  schedule:o=>ipcRenderer.invoke('ln','schedule',toList(o).map(n=>({id:n.id,title:String(n.title||''),body:String(n.body||''),at:n.schedule&&n.schedule.at?new Date(n.schedule.at).getTime():Date.now()+800,extra:n.extra?JSON.parse(JSON.stringify(n.extra)):null}))),
  addListener:(ev,cb)=>{if(ev==='localNotificationActionPerformed'&&typeof cb==='function')subs.push(cb);return {remove:()=>{}}},
  removeAllListeners:async()=>{subs.length=0}
 },
 version:((process.argv||[]).find(a=>a.startsWith('--yawmi-ver='))||'').slice(12),
 widgetData:d=>{try{ipcRenderer.send('wdata',JSON.parse(JSON.stringify(d)))}catch(e){}},
 addWidget:t=>ipcRenderer.invoke('w-add',t),
 onWidgetAction:cb=>ipcRenderer.on('w-act',(e,x)=>{try{cb(x)}catch(err){}}),
 getLogin:()=>ipcRenderer.invoke('login','get'),
 setLogin:v=>ipcRenderer.invoke('login','set',!!v)
});
