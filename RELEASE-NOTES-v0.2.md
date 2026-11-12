# Pova Desktop Launcher v0.2

Changes:
- Force landscape while Pova Desktop Activity is open (does not force other apps into landscape).
- Windows-inspired desktop, pinned Chrome/WhatsApp launch buttons, START app list.
- Attempts to launch external apps with ActivityOptions launch bounds.
- Adds restore for minimized internal windows.
- Uses working CI workflow without failing android-actions/setup-android step.

Limitations:
- Not compiled/verified on LJ6 at preparation time.
- Android system may ignore launch bounds from a regular APK; adb shell windowingMode=5 is stronger and does not transfer its privileges to this app.
- No root, custom ROM, Shizuku, or elevated system permissions included.
- External monitor requires Android to expose a presentation display.
- No native Windows .exe support.

Build: Upload these files to root of GitHub repository and run GitHub Actions. Install APK artifact only if build succeeds.
