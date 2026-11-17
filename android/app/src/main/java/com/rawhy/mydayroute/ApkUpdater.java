package com.rawhy.mydayroute;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads a new APK from the app's GitHub release and opens Android's installer (the user still taps Update). */
@CapacitorPlugin(name = "ApkUpdater")
public class ApkUpdater extends Plugin {
    private File apkFile() {
        File dir = new File(getContext().getCacheDir(), "apk");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "Yawmi.apk");
    }

    @PluginMethod
    public void getInfo(PluginCall call) {
        JSObject r = new JSObject();
        try {
            Context c = getContext();
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
            r.put("code", code);
            r.put("name", p.versionName);
            r.put("canInstall", Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls());
        } catch (Exception e) {
            r.put("code", 0);
        }
        call.resolve(r);
    }

    @PluginMethod
    public void download(PluginCall call) {
        final String url = call.getString("url");
        if (url == null || !url.startsWith("https://github.com/Abdelrahman-Rawhy/")) {
            call.reject("bad url");
            return;
        }
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                String u = url;
                // follow redirects by hand (GitHub sends the file from another host)
                for (int hop = 0; hop < 6; hop++) {
                    con = (HttpURLConnection) new URL(u).openConnection();
                    con.setInstanceFollowRedirects(false);
                    con.setConnectTimeout(20000);
                    con.setReadTimeout(60000);
                    int st = con.getResponseCode();
                    if (st >= 300 && st < 400 && con.getHeaderField("Location") != null) {
                        u = new URL(new URL(u), con.getHeaderField("Location")).toString();
                        con.disconnect();
                        continue;
                    }
                    if (st != 200) throw new Exception("HTTP " + st);
                    break;
                }
                long total = con.getContentLengthLong();
                File out = apkFile();
                File tmp = new File(out.getPath() + ".part");
                long got = 0;
                int lastPct = -1;
                try (InputStream in = con.getInputStream(); FileOutputStream fo = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        got += n;
                        int pct = total > 0 ? (int) (got * 100 / total) : -1;
                        if (pct != lastPct) {
                            lastPct = pct;
                            JSObject ev = new JSObject();
                            ev.put("pct", pct);
                            ev.put("got", got);
                            notifyListeners("progress", ev);
                        }
                    }
                }
                if (got < 1000000 || (total > 0 && got != total)) throw new Exception("incomplete download");
                if (out.exists()) out.delete();
                if (!tmp.renameTo(out)) throw new Exception("could not save");
                JSObject r = new JSObject();
                r.put("size", got);
                call.resolve(r);
            } catch (Exception e) {
                call.reject(e.getMessage() == null ? "download failed" : e.getMessage());
            } finally {
                if (con != null) con.disconnect();
            }
        }).start();
    }

    /** Installs the downloaded APK through PackageInstaller (no prompt on Android 12+ once this app is the installer). */
    @PluginMethod
    public void install(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
            Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()));
            s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(s);
            r.put("needPermission", true);
            call.resolve(r);
            return;
        }
        new Thread(() -> {
            try {
                SelfUpdate.install(c);
                r.put("started", true);
                call.resolve(r);
            } catch (Exception e) {
                // fall back to the classic installer screen
                try {
                    Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", apkFile());
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                    r.put("started", true);
                    call.resolve(r);
                } catch (Exception e2) {
                    call.reject(e2.getMessage() == null ? "install failed" : e2.getMessage());
                }
            }
        }).start();
    }

    /** Background updates on/off (default on). */
    @PluginMethod
    public void setAuto(PluginCall call) {
        boolean on = Boolean.TRUE.equals(call.getBoolean("on", true));
        SelfUpdate.prefs(getContext()).edit().putBoolean("auto", on).apply();
        SelfUpdate.schedule(getContext());
        call.resolve();
    }

    /** Run the background check now (used when the app opens). */
    @PluginMethod
    public void checkNow(PluginCall call) {
        final Context c = getContext().getApplicationContext();
        new Thread(() -> SelfUpdate.runCheck(c)).start();
        call.resolve();
    }
}
