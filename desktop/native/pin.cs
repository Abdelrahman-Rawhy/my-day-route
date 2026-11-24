// pinhelper.exe: keeps Yawmi desktop widgets at the bottom of the window stack (on the wallpaper, under every other window),
// like TickTick's "pin to desktop". Electron has no API for this, so the shell runs this tiny helper and writes one command per line:
//   bottom <hwnd>   -> SetWindowPos(hwnd, HWND_BOTTOM) without moving, resizing or activating it
//   quit
// Built on the GitHub Windows runner with the .NET Framework csc.exe (see .github/workflows/build-desktop.yml).
using System;
using System.Runtime.InteropServices;

static class PinHelper {
  [DllImport("user32.dll", SetLastError = true)]
  static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);
  [DllImport("user32.dll")]
  static extern bool IsWindow(IntPtr hWnd);

  static readonly IntPtr HWND_BOTTOM = new IntPtr(1);
  const uint SWP_NOSIZE = 0x0001, SWP_NOMOVE = 0x0002, SWP_NOACTIVATE = 0x0010, SWP_NOOWNERZORDER = 0x0200;

  static void Main() {
    string line;
    while ((line = Console.ReadLine()) != null) {
      line = line.Trim();
      if (line == "quit") break;
      string[] p = line.Split(' ');
      if (p.Length != 2 || p[0] != "bottom") continue;
      long h;
      if (!long.TryParse(p[1], out h)) continue;
      IntPtr hwnd = new IntPtr(h);
      if (!IsWindow(hwnd)) continue;
      SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOOWNERZORDER);
    }
  }
}
