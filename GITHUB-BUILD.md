# Build APK tanpa Android Studio

1. Login di https://github.com dan buat repositori baru bernama `PovaDesktopLauncher` (private boleh).
2. Unggah **isi folder PovaDesktopLauncher** ke ROOT repositori, termasuk `.github/workflows/build-apk.yml`.
   Pastikan file `app/build.gradle` dan `settings.gradle` terlihat langsung di root repositori.
3. Buka tab **Actions** -> **Build Pova Desktop APK** -> **Run workflow** -> **Run workflow**.
4. Setelah workflow SUKSES, buka run yang sukses -> **Artifacts** -> `PovaDesktopLauncher-APK`.
5. Unduh ZIP artifact, ekstrak, dan instal `app-debug.apk` pada Android 16.

APK debug ditandatangani otomatis oleh Android build system untuk uji coba, bukan rilis Play Store.
Jangan memasang APK yang belum menghasilkan status workflow sukses.
Tidak perlu root / custom ROM untuk memasang launcher ini. Dukungan freeform aplikasi lain bergantung HiOS.
