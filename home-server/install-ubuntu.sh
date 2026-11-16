#!/usr/bin/env bash
# My Day Route - Home Server installer for Ubuntu
# Run from inside the home-server folder:   sudo bash install-ubuntu.sh
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then echo "Please run with sudo:  sudo bash install-ubuntu.sh"; exit 1; fi
HERE="$(cd "$(dirname "$0")" && pwd)"
DEST=/opt/mdr-home-server
DATA=/srv/mydayroute-backups
PORT=8765

echo
echo "== My Day Route home server setup =="
read -rp "Choose a username for the app: " MDR_USER
while true; do
  read -rsp "Choose a password (at least 6 characters): " MDR_PASS; echo
  [ ${#MDR_PASS} -ge 6 ] && break || echo "Too short, try again."
done
read -rp "Backups folder [$DATA]: " IN; DATA=${IN:-$DATA}

echo "-> Installing files"
apt-get update -qq && apt-get install -y -qq python3 >/dev/null
id mdr >/dev/null 2>&1 || useradd --system --home "$DEST" --shell /usr/sbin/nologin mdr
mkdir -p "$DEST" "$DATA"
install -m 0755 "$HERE/mdr_home_server.py" "$DEST/mdr_home_server.py"
python3 - "$DEST/mdr_server_config.json" "$MDR_USER" "$MDR_PASS" <<'PY'
import json, sys
json.dump({"user": sys.argv[2], "password": sys.argv[3]}, open(sys.argv[1], "w"))
PY
chown -R mdr:mdr "$DEST" "$DATA"
chmod 600 "$DEST/mdr_server_config.json"

echo "-> Starting automatically on every boot"
cat > /etc/systemd/system/mdr-home-server.service <<EOF
[Unit]
Description=My Day Route home backup server
After=network-online.target
Wants=network-online.target

[Service]
User=mdr
ExecStart=/usr/bin/python3 $DEST/mdr_home_server.py --port $PORT --folder $DATA
Restart=always
RestartSec=5
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=$DATA $DEST
ProtectHome=true

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable --now mdr-home-server >/dev/null

echo "-> Keeping the PC awake (no sleep or suspend)"
systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target >/dev/null 2>&1 || true

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  echo "-> Allowing the app through the firewall (home network and Tailscale only)"
  ufw allow from 192.168.0.0/16 to any port $PORT proto tcp >/dev/null
  ufw allow from 10.0.0.0/8 to any port $PORT proto tcp >/dev/null
  ufw allow from 100.64.0.0/10 to any port $PORT proto tcp >/dev/null
fi

read -rp "Install Tailscale to reach the server from anywhere (Maadi, work, travel)? [Y/n] " TS
if [[ ! "$TS" =~ ^[Nn] ]]; then
  if ! command -v tailscale >/dev/null 2>&1; then curl -fsSL https://tailscale.com/install.sh | sh; fi
  echo "-> A link will appear: open it and sign in with the same account you use in the Tailscale phone app."
  tailscale up || true
fi

LAN_IP=$(hostname -I | awk '{print $1}')
TS_IP=$(tailscale ip -4 2>/dev/null | head -1 || true)
echo
echo "=============================================="
echo " Done. The server is running and starts by itself after every reboot."
echo
echo " In the app: Settings -> Backup & restore -> Home server"
echo "   At home:        http://$LAN_IP:$PORT/"
[ -n "$TS_IP" ] && echo "   From anywhere:  http://$TS_IP:$PORT/   (with Tailscale on the phone)"
echo "   Username:       $MDR_USER"
echo "   Backups folder: $DATA"
echo
echo " Check status:  sudo systemctl status mdr-home-server"
echo "=============================================="
