# Pova Desktop v0.3 — Shizuku Freeform experiment

This experimental revision adds explicit Shizuku permission and a Shizuku UserService that invokes `/system/bin/am start --windowingMode 5 -n <component>` with the Shizuku shell identity. The existing launcher remains landscape.

**Not yet built or tested on LJ6.** Shizuku may prompt for permission; tap `Shizuku` on the desktop to connect. Tapping an installed app in START requests freeform. If a process is already running it may reuse an existing task, and OEM restrictions may still apply.

Security: the service accepts only a validated Android component name and does not expose a generic shell interface. No root is requested.

Upgrade note: debug signing keys produced on different GitHub runners may differ, so if `adb install -r` returns `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, preserve any notes you need and uninstall the old test APK before installing the new one.

To build: upload/replace the files in your GitHub repository and inspect Actions results. Shizuku API/provider are loaded from Maven Central. If a build fails, use its exact log to adjust source.
