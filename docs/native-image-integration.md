# GraalVM Native Integration Guide (JavaFX)

This project ships a native executable using GraalVM Native Image.
This document explains what is required, why it is required, and how to maintain it.

## Goal and Scope

Goal:
- Build a native launcher for faster startup and easier distribution.

Important scope note:
- Single-file vs multi-file distribution is primarily a packaging-strategy choice, not an OS-only rule.
- `jpackage` creates an app image/installer bundle (directory layout), not a pure single standalone binary.
- In this project we use `nativeCompile` and place required JavaFX native libraries next to the executable.

## Native Integration Components

### 1) Build plugin and native target (`build.gradle`)

What:
- `org.graalvm.buildtools.native` plugin is enabled.
- `graalvmNative.binaries.main` sets image name and main class.

Why:
- Without this plugin/configuration, Gradle cannot produce a native image.

### 2) JavaFX launcher entry point (`Launcher`)

What:
- Native image entry point is `opensource.master_duel_android_adb_user_changer.Launcher`.
- `Launcher` calls `Application.launch(MainApp.class, args)`.

Why:
- Keeping a dedicated launcher class is the stable JavaFX entry pattern for native image.
- It avoids class initialization/entrypoint edge cases from launching JavaFX directly in complex app classes.

### 3) Reflection metadata (`reflect-config.json`)

What:
- `src/main/resources/META-INF/native-image/opensource.master_duel_android_adb_user_changer/master-duel-android-adb-user-changer/reflect-config.json`

Why:
- Native image closes the world at build time.
- JavaFX Windows internals use runtime lookups (reflection/JNI), including classes like `com.sun.glass.ui.win.WinDnDClipboard`.
- If these classes are not reachable in metadata, runtime errors occur (for example `ClassNotFoundException` during tab interaction).

### 4) Resource metadata (`resource-config.json`)

What:
- Includes CSS, JavaFX skin resources, message bundles, and Prism shader objects.

Why:
- Native image only packs resources that are explicitly reachable.
- Missing resources can cause runtime rendering/theme/style failures.

### 5) Reachability metadata (`reachability-metadata.json`)

What:
- `src/main/resources/META-INF/native-image/reachability-metadata.json`

Why:
- Captures additional JNI/reflection/resource requirements discovered by the tracing agent.
- JavaFX needs broad metadata coverage; this file reduces manual guesswork.

Maintenance rule:
- Treat this file as generated/derived metadata.
- Do not hand-edit unless you know exactly what you are changing.

### 6) JavaFX Windows DLL copy task (`copyJavafxWindowsNativeDlls`)

What:
- After `nativeCompile`, Gradle copies required JavaFX DLLs into `build/native/nativeCompile`.

Why:
- JavaFX native libraries are loaded at runtime.
- If DLLs are missing, executable may start but crash/fail on UI operations.

## Build Prerequisites

- GraalVM JDK 25 installed and selected.
- Native Image component installed (`native-image` command available).
- On Windows, run build in a shell where MSVC environment is loaded.
  - Example: Native Build Tools for Visual Studio

Quick checks:

```powershell
java -version
native-image --version
```

## Build Commands

PowerShell:

```powershell
.\gradlew.bat clean nativeCompile
```

bash:

```bash
./gradlew clean nativeCompile
```

Output directory:
- `build/native/nativeCompile/`

Main executable:
- Windows: `master-duel-android-adb-user-changer.exe`

## Runtime Metadata Sweep (for agent collection and validation)

The app has test-only runtime flags:
- `native.metadata.tabSweep=true`
- `native.metadata.tabSweepIntervalMillis=500`
- `native.metadata.autoExitSeconds=7`

These are used to quickly visit tabs and exit automatically while collecting metadata.

PowerShell (recommended):

```powershell
.\build\native\nativeCompile\master-duel-android-adb-user-changer.exe --% -Dnative.metadata.tabSweep=true -Dnative.metadata.tabSweepIntervalMillis=500 -Dnative.metadata.autoExitSeconds=7
```

PowerShell (alternative):

```powershell
& ".\build\native\nativeCompile\master-duel-android-adb-user-changer.exe" "-Dnative.metadata.tabSweep=true" "-Dnative.metadata.tabSweepIntervalMillis=500" "-Dnative.metadata.autoExitSeconds=7"
```

bash:

```bash
./build/native/nativeCompile/master-duel-android-adb-user-changer -Dnative.metadata.tabSweep=true -Dnative.metadata.tabSweepIntervalMillis=500 -Dnative.metadata.autoExitSeconds=7
```

Why this PowerShell detail matters:
- Plain `.\exe -D...` may be parsed unexpectedly depending on shell argument handling.
- `--%` or `&` with explicitly quoted arguments keeps argument boundaries predictable.

## Regenerating Native Metadata (Recommended Flow)

1. Run JVM mode with Graal tracing agent enabled and automatic tab sweep:

PowerShell:

```powershell
.\gradlew.bat -Pagent run metadataCopy -PnativeMetadataTabSweep=true -PnativeMetadataTabSweepIntervalMillis=500 -PnativeMetadataAutoExitSeconds=7
```

bash:

```bash
./gradlew -Pagent run metadataCopy -PnativeMetadataTabSweep=true -PnativeMetadataTabSweepIntervalMillis=500 -PnativeMetadataAutoExitSeconds=7
```

2. Review generated changes under:
- `src/main/resources/META-INF/native-image/**`

3. Rebuild native executable:
- `gradlew nativeCompile`

4. Re-test UI interactions (especially tab switching, clipboard, drag/drop paths).

## Troubleshooting

`ClassNotFoundException: com.sun.glass.ui.win.WinDnDClipboard`
- Cause: Missing Windows Glass class metadata in native image.
- Action: Update reflection/reachability metadata and rebuild.

Crash after switching tabs in native exe
- Cause: Incomplete JavaFX metadata for a code path reached at runtime.
- Action: Re-run agent collection with tab sweep, merge metadata, rebuild.

Styles/theme/resources missing
- Cause: Resource not included in native image.
- Action: Add/verify include patterns in `resource-config.json`.

Executable runs in one shell invocation but not another
- Cause: Shell argument parsing differences (especially PowerShell).
- Action: Use `--%` or `&` argument form shown above.

`--console=plain` question
- `--console=plain` is optional.
- It only changes Gradle output formatting; build behavior/artifacts remain the same.
