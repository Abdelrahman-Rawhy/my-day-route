# My Day Route – Home Server

Keep encrypted backups of My Day Route (journal, photos, **videos**, history) on a computer at home. Unlimited space, free, and nothing leaves your house.

## Ubuntu (recommended for an old PC)
Copy this folder to the PC, open a terminal inside it and run:
```
sudo bash install-ubuntu.sh
```
It asks for a username and password, starts the server automatically on every boot, keeps the PC awake, and can install Tailscale so you can reach it from anywhere. At the end it prints the address to type in the app.

## 1. Start it (Windows / Mac, manual)
- **Windows:** install Python from python.org (tick *Add Python to PATH*), then double-click `Start-Windows.bat`.
- **Mac / Linux / Raspberry Pi:** run `./start-mac-linux.sh`.

The first time, choose a username and password. The window then shows the address, e.g. `http://192.168.1.20:8765/`. Keep the window open.

If Windows asks about the firewall, allow **Private networks**.

## 2. Connect the app
App → Settings → **Backup & restore → Home server**
- Server address: the address shown in the window
- Username / password: the ones you chose
- Backup password: a separate password that encrypts the files (you need it to restore)
- Tap **Test & save**, then **Back up now**. Daily automatic backups happen on Wi‑Fi.

The app keeps the last 14 daily backups in the `backups` folder.

## Outside your home (optional)
Install **Tailscale** (free) on the computer and the phone, sign in with the same account, then use the computer's Tailscale address (`http://100.x.x.x:8765/`). It's a private network, so you don't open your router to the internet.

## Using Nextcloud or Synology instead
Use your WebDAV address, for example:
- Nextcloud: `https://your-cloud/remote.php/dav/files/USERNAME/MyDayRoute/` (create the `MyDayRoute` folder first)
- Synology: enable *WebDAV Server* in Package Center, then `http://NAS-IP:5005/MyDayRoute/`

## Security
Backups are encrypted on the phone (AES‑256) with your backup password before they are sent. The server only stores locked `.mdr` files; without the password they can't be opened.
