# PicViewer

A quiet Android photo frame for photos in a device folder or on an authenticated SMB2/3 share.

**Pre-release.** The app is being tested. It has not been validated on a physical tablet or a real Windows share yet. Installation and the welcome screen have been checked on an Android 9 x86 software emulator. Do not weaken your server security settings to make it connect.

## What it does

- Read-only device-folder access through the Android system folder picker (Storage Access Framework). No all-files or broad media permission.
- Read-only SMB2/3 access with signing required and a named account.
- Recursive photo index, multiple folder checkboxes, select all, and optional subfolder inclusion.
- Sequential natural filename order or shuffle without repetition within a cycle.
- Full-screen fit/optional crop, interval controls, pause, previous/next, and foreground screen-on.
- Distinct connection, authentication, share-permission and cached-offline states.
- Periodic rescans. Successfully confirmed deletions leave the queue and disk cache; failed listings do not erase the index.
- Password encryption using an Android Keystore AES-GCM key. Backups are disabled. Photos and credentials are not uploaded to a cloud service.
- Basic image formats through Android decoders. RAW embedded previews/basic rendering through LibRaw and TIFF through LibTIFF are included as best-effort paths.

This is not a universal decoder guarantee. Unsupported encodings, camera variants, damaged files and resource limits can prevent display. GIF and other animated containers are shown as still images. HDR/professional RAW colour matching is not promised. RAW processing is limited to one native operation at a time across decode workers and uses constrained buffers. A 256MB file limit and 50MP full-RAW limit protect low-memory devices; embedded previews may work for larger sensors.

## Build

Requirements: JDK 17, Android SDK 35 / build-tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1. Android 8.0+ (API 26) is the current build minimum, with Android 14 the primary validation target. The package contains arm64-v8a, armeabi-v7a x86_64 and x86 native libraries.

```sh
# Set ANDROID_HOME or sdk.dir in a local.properties file.
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is a development build, not a Play Store release. Never add a signing key or real connection details to the repository.

## Device folders (0.2.0)

Choose **기기 폴더 선택** on the welcome screen or in Settings. In Android's picker, choose a photo folder and allow access. The app requests persistent read permission only. It does not request write access or change original files. Internal storage and SD-card folders work when the device's document provider exposes them. The picker is asked to show local locations; provider behavior still depends on the installed Android ROM.

One source is active at a time. Device folders and SMB are not mixed into one list. Changing location rebuilds the index, folder choices, playback queue and cache, but keeps saved PC credentials. Use **저장한 PC 공유로 전환** in Settings to return, or **PC 연결 설정** to edit them. A local source restores on app restart while its grant remains valid.

Subfolder checkboxes, select all, sequential/shuffled playback and manual/periodic rescans work on the chosen tree. Android 11+ can disallow choosing storage roots, the whole Download folder, and Android/data or Android/obb. Choose Pictures, DCIM or a permitted subfolder instead; this app does not bypass Android restrictions. A disconnected SD card or revoked grant requires checking/reselecting the folder. If the provider omits version metadata, files are fetched again after each scan rather than trusting an old cached version. Providers can still change a file during a read without useful metadata, so exact snapshot guarantees are not possible.

Android platform guidance: https://developer.android.com/training/data-storage/shared/documents-files

The local-source checks use a fixture DocumentsProvider under Android 14 Robolectric, including reads, restart, grant result handling, revocation, add/delete scans and playback. They are not physical-tablet or real picker/SD-card tests.

## Connect

Use the PC hostname/IP, share name, account name and the account's password. The Windows login PIN is not the SMB account password. Grant the account only read/list access in both share permissions and folder security permissions. Do not expose TCP 445 to the internet. Guest shares and SMB1 are not supported.

No response means the PC/share is unreachable; it does not prove that the PC's power is off. A firewall, sleeping PC or a different network can cause the same result.

The demo button works without a server and is clearly labeled. It contains a licensed example landscape and an original generated colour chart, not personal photos.

## Tests

- Pure Java domain tests for natural sorting, selection inheritance, deduplication, random cycles, queue restoration, partial scan failures, 2,000-folder rescans and a 100,000-photo queue. These are local synthetic tests, not network throughput claims.
- Android 14 Robolectric tests for screens, image decoding, SQLite persistence and cache pruning. Captures from these tests are simulated Android renders, not physical-device screenshots.
- Optional live SMB fixture tests use environment variables `PICVIEWER_TEST_PORT`, `PICVIEWER_TEST_USER`, `PICVIEWER_TEST_PASSWORD`, `PICVIEWER_TEST_ROOT`. They are skipped when the fixture is absent. Only ephemeral test accounts belong there.

- Portable native decoder checks: `cmake -S app/src/main/cpp -B build-host && cmake --build build-host -j2`, then `python tests/native_codec_test.py build-host/piccodec` (Pillow required). These exercise the same decode functions on the host; they do not prove Android JNI compatibility or tablet performance.

See `THIRD_PARTY_NOTICES.md` for component and photo licenses. Native dependency versions and the two documented LibRaw decoder patches are listed there. Test data and build output are not a claim of compatibility with every RAW camera.

`tests/android_probe/NativeCodec.java` is a small Android-runtime JNI probe, not an app feature. It was run with the APK's x86 library against a generated LZW TIFF, a CC0 Nikon D7000 NEF preview, a small Canon-derived DNG render and a broken TIFF. It is not a substitute for arm64/physical-device tests. Android instrumentation test source is also included but is not counted as executed unless its runner completed.
