#!/usr/bin/env python3
"""
My Day Route - Home Server
==========================
A tiny, private backup server for the My Day Route app. One file, no installs:
it only needs Python 3 (Windows, Mac, Linux, Raspberry Pi).

The app encrypts every backup on the phone BEFORE sending it, so this server only
ever stores locked files. It speaks a small part of WebDAV (the same standard used
by Nextcloud and Synology), with a username and password.

Start it:
    python mdr_home_server.py
Then in the app: Settings -> Backup & restore -> Home server, and enter the
address this program prints (for example http://192.168.1.20:8765/).

Options:
    --port 8765              port to listen on
    --folder ./backups       where backup files are kept
    --user ahmed             username (asked on first run if missing)
    --password ...           password (asked on first run if missing)
    --cert cert.pem --key key.pem   serve HTTPS instead of HTTP (optional)
"""
import argparse
import base64
import getpass
import hmac
import json
import os
import re
import socket
import ssl
import sys
from email.utils import formatdate
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from xml.sax.saxutils import escape

SAFE_NAME = re.compile(r"^[A-Za-z0-9._-]{1,120}\.mdr$")
CONFIG_FILE = "mdr_server_config.json"
MAX_UPLOAD = 8 * 1024 ** 3  # 8 GB per file


def lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("10.255.255.255", 1))
        return s.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        s.close()


def make_handler(folder: Path, user: str, password: str):
    expected = "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()

    class Handler(BaseHTTPRequestHandler):
        server_version = "MyDayRouteHome/1.0"

        # ---------- helpers ----------
        def log_message(self, fmt, *args):
            sys.stdout.write("%s  %s\n" % (self.log_date_time_string(), fmt % args))

        def cors(self):
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, PUT, DELETE, PROPFIND, MKCOL, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type, Depth")
            self.send_header("Access-Control-Expose-Headers", "Content-Length")

        def reply(self, code, body=b"", ctype="text/plain; charset=utf-8", extra=None):
            self.send_response(code)
            self.cors()
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            for k, v in (extra or {}).items():
                self.send_header(k, v)
            self.end_headers()
            if self.command != "HEAD" and body:
                self.wfile.write(body)

        def authorized(self):
            got = self.headers.get("Authorization", "")
            if hmac.compare_digest(got.encode(), expected.encode()):
                return True
            self.reply(401, b"Login required", extra={"WWW-Authenticate": 'Basic realm="My Day Route"'})
            return False

        def target(self):
            """Return (name, path) for the requested file, or (None, None) for the folder itself."""
            name = self.path.split("?", 1)[0].rstrip("/").split("/")[-1]
            if not name:
                return None, None
            if not SAFE_NAME.match(name):
                return name, False
            return name, folder / name

        # ---------- methods ----------
        def do_OPTIONS(self):
            self.send_response(204)
            self.cors()
            self.send_header("DAV", "1")
            self.send_header("Content-Length", "0")
            self.end_headers()

        def do_PROPFIND(self):
            if not self.authorized():
                return
            length = int(self.headers.get("Content-Length") or 0)
            if length:
                self.rfile.read(length)
            name, path = self.target()
            depth = self.headers.get("Depth", "1")
            items = []
            if path is None:
                items.append(("/", True, 0, os.path.getmtime(folder)))
                if depth != "0":
                    for f in sorted(folder.glob("*.mdr")):
                        st = f.stat()
                        items.append(("/" + f.name, False, st.st_size, st.st_mtime))
            elif path and path.exists():
                st = path.stat()
                items.append(("/" + name, False, st.st_size, st.st_mtime))
            else:
                return self.reply(404, b"Not found")
            parts = ['<?xml version="1.0" encoding="utf-8"?>\n<d:multistatus xmlns:d="DAV:">']
            for href, is_dir, size, mtime in items:
                parts.append(
                    "<d:response><d:href>%s</d:href><d:propstat><d:prop>"
                    "<d:resourcetype>%s</d:resourcetype>"
                    "<d:getcontentlength>%d</d:getcontentlength>"
                    "<d:getlastmodified>%s</d:getlastmodified>"
                    "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>"
                    % (escape(href), "<d:collection/>" if is_dir else "", size, formatdate(mtime, usegmt=True))
                )
            parts.append("</d:multistatus>")
            self.reply(207, "".join(parts).encode(), "application/xml; charset=utf-8")

        def do_GET(self):
            if not self.authorized():
                return
            name, path = self.target()
            if path is None:
                return self.reply(200, b"My Day Route home server is running.\n")
            if not path or not path.exists():
                return self.reply(404, b"Not found")
            size = path.stat().st_size
            self.send_response(200)
            self.cors()
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Length", str(size))
            self.end_headers()
            with open(path, "rb") as fh:
                while True:
                    chunk = fh.read(1024 * 1024)
                    if not chunk:
                        break
                    self.wfile.write(chunk)

        def do_PUT(self):
            if not self.authorized():
                return
            name, path = self.target()
            if not path:
                return self.reply(400, b"Only .mdr backup files can be stored")
            length = int(self.headers.get("Content-Length") or 0)
            if length <= 0 or length > MAX_UPLOAD:
                return self.reply(413, b"Bad size")
            tmp = path.with_suffix(".part")
            remaining = length
            with open(tmp, "wb") as fh:
                while remaining > 0:
                    chunk = self.rfile.read(min(1024 * 1024, remaining))
                    if not chunk:
                        break
                    fh.write(chunk)
                    remaining -= len(chunk)
            if remaining:
                tmp.unlink(missing_ok=True)
                return self.reply(400, b"Upload interrupted")
            os.replace(tmp, path)
            self.reply(201, b"Saved")

        def do_DELETE(self):
            if not self.authorized():
                return
            name, path = self.target()
            if not path or not path.exists():
                return self.reply(404, b"Not found")
            path.unlink()
            self.reply(204)

        def do_MKCOL(self):
            if not self.authorized():
                return
            self.reply(405, b"Folder already exists")

    return Handler


def main():
    ap = argparse.ArgumentParser(description="My Day Route home backup server")
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--folder", default="backups")
    ap.add_argument("--user")
    ap.add_argument("--password")
    ap.add_argument("--cert")
    ap.add_argument("--key")
    a = ap.parse_args()

    here = Path(__file__).resolve().parent
    cfg_path = here / CONFIG_FILE
    cfg = json.loads(cfg_path.read_text()) if cfg_path.exists() else {}
    user = a.user or cfg.get("user") or input("Choose a username: ").strip()
    password = a.password or cfg.get("password") or getpass.getpass("Choose a password: ")
    if not user or len(password) < 6:
        sys.exit("Username required and password must be at least 6 characters.")
    cfg.update(user=user, password=password)
    cfg_path.write_text(json.dumps(cfg))
    try:
        os.chmod(cfg_path, 0o600)
    except OSError:
        pass

    folder = Path(a.folder)
    if not folder.is_absolute():
        folder = here / folder
    folder.mkdir(parents=True, exist_ok=True)

    httpd = ThreadingHTTPServer(("0.0.0.0", a.port), make_handler(folder, user, password))
    scheme = "http"
    if a.cert and a.key:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.load_cert_chain(a.cert, a.key)
        httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
        scheme = "https"

    print("\n  My Day Route home server is running")
    print("  ------------------------------------")
    print(f"  Address for the app:  {scheme}://{lan_ip()}:{a.port}/")
    print(f"  Username:             {user}")
    print(f"  Backups folder:       {folder}")
    print("  Keep this window open. Press Ctrl+C to stop.\n")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
