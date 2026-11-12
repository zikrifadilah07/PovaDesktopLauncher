# Pova Desktop Launcher 0.1 — prototipe Android

Target: Tecno Pova 7 4G (LJ6), Android 16 / HiOS 16. Java, Android Studio, tanpa root.

## Fitur yang sudah dikodekan
- Desktop UI pada layar utama, START/taskbar/shortcut.
- Jendela internal (Notes / daftar aplikasi / demo Browser) dapat digeser, ditumpuk, diubah ukurannya, ditutup.
- Daftar aplikasi terpasang dan pembukaan aplikasi eksternal melalui intent.
- ActivityOptions `setLaunchBounds` untuk meminta ukuran jendela (OEM boleh mengabaikan).
- Presentation untuk desktop terpisah di layar eksternal yang dideteksi Android.
- Dapat ditawarkan sebagai HOME launcher, tetapi tidak otomatis mengganti launcher bawaan.

## Batasan
- APK belum di-build/diuji pada HP nyata. Catatan belum persisten, browser internal adalah demo offline, minimize belum bisa restore, tidak ada ikon aplikasi.
- Aplikasi eksternal **tidak** ditanam ke jendela internal; floating behavior tergantung fitur freeform OEM.
- Layar eksternal memerlukan output DisplayPort/HDMI/Wi-Fi Display yang benar-benar mendukung secondary presentation; mirroring bukan independent display.
- Resolusi beberapa perangkat kecil mungkin memerlukan penyesuaian ukuran jendela.

## Build
1. Instal Android Studio dan Android SDK Platform 35 + JDK 17/21.
2. File > Open > pilih folder `PovaDesktopLauncher`.
3. Izinkan Gradle sync. Bila diminta Gradle distribution, pilih Gradle 8.11.1 (kompatibel AGP 8.9.2).
4. Build > Build APK(s), atau `gradle assembleDebug` bila Gradle 8.11.1 tersedia.
5. APK hasil build berada di `app/build/outputs/apk/debug/app-debug.apk`.
6. Aktifkan USB debugging HP, gunakan `adb install -r app-debug.apk`.
7. Buka aplikasi Pova Desktop; jangan set sebagai HOME default sebelum menguji tombol navigasi dan aplikasi lain.

## Verifikasi mode freeform dan display
`adb shell pm list features | grep freeform`
`adb shell dumpsys display`
`adb shell settings get global enable_freeform_support`
Perintah hanya baca; tidak mengaktifkan freeform. Pada Windows pakai `findstr` menggantikan `grep`.

## Rujukan
https://developer.android.com/reference/android/app/ActivityOptions#setLaunchBounds(android.graphics.Rect)
https://developer.android.com/develop/adaptive-apps/guides/support-connected-displays
https://source.android.com/docs/core/display/multi-window
