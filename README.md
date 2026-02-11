# Master Duel Android ADB User Changer

>**[WARNING] DISCLAIMER: USE AT YOUR OWN RISK.** This software is provided "as is" without warranty of any kind. The developer is not responsible for any damages, including data loss or account suspensions, caused by using this tool.

Desktop GUI to manage multiple Yu-Gi-Oh! Master Duel profiles on Android by renaming the app's persistent data folder via ADB (root required).

This app is for:
- Regular users who want a simple button-driven workflow
- Developers who want to extend or automate the tool

## What It Does
- Activate a selected profile
- Deactivate the current profile so a fresh one is created on next app launch
- Rename a profile alias for easier identification
- Inspect device info and logs for ADB operations

## Requirements (End Users)
- ADB installed and reachable in PATH (or set in the ADB tab)
- Android device or emulator with root access (`su`)
- Master Duel installed (package: `jp.konami.masterduel`)

## Quick Start (End Users)
1. Open the app.
2. Go to the ADB tab and set the ADB path if needed, then click `Check ADB`.
3. On the Devices tab, click `Refresh Devices` and select your device.
4. On the Users tab:
   - `Activate Selected` makes the selected profile active.
   - `Deactivate Current User` archives the current profile so the next app launch creates a new one.
   - `Set Alias` changes the display name of a profile.
5. Check the Logs tab if anything fails.

## Important Notes
- This tool renames folders under:
  `/data/data/jp.konami.masterduel/files`
- Profile metadata is stored at:
  `persistent*/master-duel-android-adb-user-changer-metadata.properties`
- Root access is required for all operations.
- Back up your device if you are unsure. File operations are safe but still destructive if interrupted.

## Troubleshooting
- `Root access not available`:
  Ensure `adb shell su -c id` returns `uid=0`.
- `Permission denied` or `No such file or directory`:
  Make sure the game is installed and the device is rooted.
- If switching fails:
  Use the Logs tab to copy the full command output.

## Developer Notes
### Prerequisites
- Java 25 (Temurin) via `mise` (recommended)

### Setup (mise)
```bash
mise install
mise use -g java@temurin-25
```

### Run
```bash
./gradlew run
```

### Build Runnable JAR
```bash
./gradlew shadowJar
```

Output: `build/libs/master-duel-android-adb-user-changer-<version>-all.jar`

### Design Notes
- All ADB commands are executed via `adb shell su -c`.
- Write operations use stdin piping to avoid shell escaping issues.
