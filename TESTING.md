# Test scope

This pre-release is not physical-tablet or Windows certification. Keep fixture data separate from personal photos.

## JVM and Android simulation

With JDK 17 and the Android SDK/NDK versions listed in README:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Robolectric uses Android 14 API 34. Its images are simulated renders, not screenshots of a physical device. Tests that require an SMB fixture skip when the environment variables below are absent. A green test run with skips is not equivalent to a connected-server run.

## Connected SMB fixture

Use a temporary SMB2 server that requires a named account, supports signing and grants read/list only. Bind it to loopback, not a public interface. Supply these environment variables only to the test process; do not commit or print them:

- PICVIEWER_TEST_PORT
- PICVIEWER_TEST_USER
- PICVIEWER_TEST_PASSWORD
- PICVIEWER_TEST_ROOT: local fixture directory, needed by tests that add/delete fixture files

The share name is `photos`. Its initial structure must contain four image candidates:

- 여행/하위/사진2.jpg: valid JPEG
- 여행/사진1.jpg: valid JPEG
- 일상/사진10.png: valid PNG
- raw/sample.dng: a placeholder may be used, but that is not a valid RAW decoder test

An empty directory and a non-image text file are useful negative fixtures. Run all tests in one process with these variables set. The tests modify only local fixture files, not a personal share. The UI integration tests use real SMBJ reads in the simulated Android activity. They do not run on Android hardware.

## Native decoder

```sh
cmake -S app/src/main/cpp -B build-host -DCMAKE_BUILD_TYPE=Release
cmake --build build-host -j2
python tests/native_codec_test.py build-host/piccodec
python tests/native_malformed_test.py build-host/piccodec
```

The first script needs Pillow. It checks generated TIFF variants, orientations, alpha and broken input. The second uses short-lived child processes with resource limits on Linux. Random malformed inputs are a bounded smoke test, not a security audit.

AddressSanitizer/UndefinedBehaviorSanitizer can be enabled with CMAKE_C_FLAGS, CMAKE_CXX_FLAGS and CMAKE_EXE_LINKER_FLAGS. The malformed-input script's virtual-address cap must be removed for ASan, which reserves a large shadow address range. Keep CPU and wall-clock limits. The LibRaw fixes and exact upstream version are recorded in third_party/LibRaw/PICVIEWER_PATCHES.md.

RAW fixture source URLs, camera modes and SHA-256 values are in tests/raw-sample-sources.json. Those selected entries were labeled CC0. No RAW fixture image binaries are bundled in the repository or APK. Fetch only the selected entries and verify hashes before use.

## Device-folder provider fixture

`LocalSourceTest` supplies a synthetic ContentProvider and non-personal photo files under Android 14 Robolectric. It covers recursive Unicode paths, read-only file opens, size/version checks, byte limits with unknown size, partial/loading/error listings, duplicate names, additions/deletions, stale document IDs, explicit read-only tree picker intent, persistable grant result handling, local config persistence without overwriting PC fields, activity restart, playback, folder selection and access revocation. Unknown metadata receives a new cache version each scan.

This exercises the application and Android simulation, not a real OEM DocumentsUI picker, removable card or reboot grant persistence. Test those on the target device before normal use. No READ_MEDIA_IMAGES, WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permission is requested.

## Actual Android runtime

The separate Android JNI probe is documented in tests/android_probe/README.md. It calls the APK's native library through Android's runtime and Bitmap API. It does not test the entire activity, SMB workflow or ARM ABI. The larger NEF's complete RAW render is not counted as a bounded emulator smoke-test pass.

Android instrumentation test source is included under app/src/androidTest. Including or compiling a test is not evidence that its runner completed. Record the runner result explicitly before counting it.

## Before normal use

A physical device still needs installation/update, Keystore save/reload, actual Windows authentication, small-folder playback, add/delete rescan, connection interruption/recovery, actual camera files, rotation, background/resume and an extended thermal/memory test. Do not lower SMB security or expose port 445 to get a pass. Keep original-photo backups.
