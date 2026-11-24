// Bridge for the desktop widget windows (desktop/widgets/widget.html).
const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('yawmiW',{
 ready:()=>ipcRenderer.send('w-ready'),
 onData:cb=>ipcRenderer.on('w-data',(e,d)=>cb(d)),
 onCfg:cb=>ipcRenderer.on('w-cfg',(e,c)=>cb(c)),
 act:x=>ipcRenderer.send('w-act',x),
 menu:()=>ipcRenderer.send('w-menu'),
 setView:v=>ipcRenderer.send('w-view',v)
});
