# Yawmi (يومي) — formerly My Day Route

A daily route app: prayers (auto times), Quran, work, graduation project, family, points, badges and prizes, with phone notifications for every step.

## Install on Android
Download the latest APK:
**https://github.com/Abdelrahman-Rawhy/my-day-route/releases/latest/download/MyDayRoute.apk**

Open it on your phone and allow "Install unknown apps" when Android asks. When the app opens, allow notifications, then tap **Settings → Exact-time alarms → Allow** so reminders ring on the minute.

## Updates
- Small updates (design, features): the app downloads them by itself and shows **Update ready → Restart**. No reinstall needed.
- Bigger updates: GitHub builds a new APK automatically on every push to `main`. Install it over the old one; your progress is kept.

## Folders
- `www/` the app (`app.html`) and the boot loader (`index.html`)
- `update/` the version the installed apps download (`version.json` + `app.html`)
- `android/` the Android project (Capacitor)
- `.github/workflows/build-apk.yml` builds and publishes the APK

## Build on your own computer (optional)
Install Node.js and Android Studio, then:
```
npm ci
npx cap sync android
npx cap open android
```
In Android Studio: Build → Build APK(s).

Note: the signing key (`android/app/mydayroute.keystore`) is included so every build can update the installed app. Keep this repository for personal/family use.
