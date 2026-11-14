# PicViewer

A quiet Android photo frame for photos in a device folder or on an authenticated SMB2/3 share.

**Pre-release.** The app is being tested. This build is not certified on a physical tablet or a real Windows share. Installation and the welcome screen have been checked on an Android 9 x86 software emulator. Do not weaken your server security settings to make it connect.

## What it does

- Read-only device-folder access through the Android system folder picker (Storage Access Framework). No all-files or broad media permission.
- Read-only SMB2/3 access with signing required and a named account.
- Recursive photo index, multiple folder checkboxes, select all, and optional subfolder inclusion.
- Sequential natural filename order or shuffle without repetition within a cycle.
- Full-screen fit/optional crop, interval controls, pause, previous/next, and foreground screen-on.
- Distinct connection, authentication, share-permission and cached-offline states.
- Periodic rescans. Successfully confirmed deletions leave the queue and disk cache; failed listings do not erase the index.
- Active connection and named saved-connection profiles encrypted using an Android Keystore AES-GCM key. Backups are disabled. Photos and credentials are not uploaded to a cloud service.
- Basic image formats through Android decoders. RAW embedded previews/basic rendering through LibRaw and TIFF through LibTIFF are included as best-effort paths.

This is not a universal decoder guarantee. Unsupported encodings, camera variants, damaged files and resource limits can prevent display. GIF and other animated containers are shown as still images. HDR/professional RAW colour matching is not promised. RAW processing is limited to one native operation at a time across decode workers and uses constrained buffers. A 256MB file limit and 50MP full-RAW limit protect low-memory devices; embedded previews may work for larger sensors.

## Build

Requirements: JDK 17, Android SDK 35 / build-tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1. Android 8.0+ (API 26) is the current build minimum, with Android 14 the primary validation target. The package contains arm64-v8a, armeabi-v7a x86_64 and x86 native libraries.

```sh
# Set ANDROID_HOME or sdk.dir in a local.properties file.
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is a development build, not a Play Store release. Never add a signing key or real connection details to the repository.

## Stability and playback update (0.4.0)

- Removed an 8-second socket idle timeout that could close the SMB session between slides. Individual SMB requests still have a 30-second timeout. A transport failure gets one read-only reconnect/retry; authentication, permission, changed-file and disk-space errors do not trigger login loops.
- Successful folder listings survive a later transport interruption. The portrait folder area is larger, with a counted, scrollable **전체 목록** multi-select dialog. Reparse/symlink entries are still not followed; their skipped count is shown. Do not treat a junction or cloud-placeholder tree as an ordinary local folder.
- **저장된 연결 불러오기 / 삭제** and **현재 입력을 연결 목록에 저장** keep up to 20 named profiles. The full profile, including credentials, is AES-GCM encrypted on the device. Loading fills the form without logging in. The previous single-connection format remains readable and is replaced by the encrypted-record format when saved again. Removing a profile leaves the separately saved active connection intact.
- Tap a playing photo, then **간격 ...초 · 변경** to set 1-3600 seconds without leaving playback. The value persists locally.
- During playback, prepare the next frames and retain a previous frame when memory allows. Up to four forward targets and one previous target are considered. The decoded-frame cache is limited to 6 entries and the smallest of 48MiB, one-eighth of the app heap, and conservative heap/native/system-headroom allowances. Each decoded frame is at most 1920x1200 / 10MiB. Background/low-memory notifications clear it. RAW prefetch uses previews only, not full RAW rendering. Fetch/decode operations are serialized and queued prefetch steps yield between images so manual navigation can run.
- The previous photo stays on screen while a requested frame is prepared. There is no centered loading spinner. Prefetch failures stay quiet; actual display failures remain visible. No hard latency guarantee is possible on slow networks or unsupported images.

Regression checks reproduce failure with the old 8-second socket timeout after a 12-second idle, and pass with the new setting. They also cover a disconnected session, a 12MiB read, 400 long Unicode folder names, adaptive cache limits, ownership/recycling, forward/backward frame reuse, and settings persistence. The saved-record/profile code was separately exercised with Android Keystore under an Android 9 emulator app UID. This is component evidence, not latest whole-app installation or target-hardware certification.

## PC discovery and share picker (0.3.0)

Open **PC 공유 연결**. Type a PC IP/name, or use **같은 Wi-Fi에서 PC 찾기**. Discovery sends two WS-Discovery probes during an approximately 8-second foreground search, limited to the active IPv4 Wi-Fi subnet. It does not scan every IP, fetch metadata URLs, or send credentials to discovered computers. Replies are only candidates, not authenticated identities. Confirm the IP belongs to your PC before continuing. IPv6-only, non-Wi-Fi, blocked multicast, disabled Windows network discovery, sleeping PCs and guest-network isolation can prevent discovery. Direct IP entry is always available. No SMB1 or guest access is enabled.

Enter the named Windows account and password, then press **이 PC의 공유 폴더 찾기**. The app authenticates to that PC using SMB2/3 with signing required and reads its SRVSVC share list. Choose a normal disk share, then **저장하고 연결** to validate actual folder reading and activate it. Listing a share does not prove read permission. Share enumeration alone does not save credentials or replace your current photo source. Wrong credentials are not retried automatically.

If listing is blocked or unsupported, use **고급: 공유 이름 직접 입력 / 하위 경로 / 포트**. Hidden/administrative/IPC/printer shares are omitted from the normal picker. A known hidden disk share can still be typed explicitly if its account has access. The app implements only DCE/RPC v5 + NDR32 SRVSVC level-1 share enumeration, with bounded pages/fragments/strings; it is not a general RPC implementation. No extra RPC dependency was bundled.

Tests include authenticated share enumeration against a loopback SMB2/SRVSVC fixture, RPC parsing/reassembly and malformed messages, discovery-response parsing/subnet checks, and Android 14 simulated setup flows. They do **not** prove WS-Discovery multicast, OEM Wi-Fi behavior or Windows 11 interoperability on a real LAN. Manual setup remains the fallback until target-device testing is done.

Protocol references: [MS-SRVS NetrShareEnum](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-srvs/c4a98e7b-d416-439c-97bd-4d9f52f8ba52), [Windows Probe messages](https://learn.microsoft.com/en-us/windows/win32/wsdapi/probe-message), [modern Explorer discovery](https://learn.microsoft.com/en-us/windows-server/storage/file-server/troubleshoot/smbv1-not-installed-by-default-in-windows).

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

### 0.3.2 입력 편의 수정
- PC 연결/저장 연결 선택·삭제/PC·공유 선택/슬라이드 간격 창은 바깥을 눌러도 닫히지 않습니다. 취소·뒤로가기는 유지합니다.
- IP를 끝에서 한 글자씩 입력하면 유효한 3자리 구간 뒤 점을 자동 입력합니다. 1~2자리 구간은 점을 직접 입력하세요. 붙여넣기·중간 수정·삭제는 자동 재작성하지 않습니다. PC 이름도 사용할 수 있습니다.

### 0.4.0 감상 기능
- 재생 중 `감상 설정` 또는 사진함 `설정 → 감상 설정`에서 선택합니다. 모두 기기 안 설정입니다.
- 야간 모드: 기본 꺼짐, 켜면 22:00~07:00/앱 밝기10%/야간 일시정지. 시간·밝기와 충전 해제 시 화면 켜두기 해제를 설정할 수 있습니다. 실제 화면 끄기/아침 깨우기/충전 제한이 아니며 기기가 잠들면 직접 깨워야 합니다.
- 짧은320ms 디졸브 선택, 기본 즉시 전환. 메모리 여유 없으면 즉시 전환합니다.
- 두 번 탭2배/원래 배율, 핀치1~4배와 이동. 확대하면 자동 일시정지하며 계속 보기는 직접 누릅니다. 화면용 샘플 이미지 확대이며 원본 해상도 보장이 아닙니다.
- 자르지 않기/채우기/흐린 배경과 가로·세로 고정. 흐린 배경은24×24 축소 영상으로 비용을 제한합니다.
- 순차 촬영일 정렬: 읽은 EXIF 원촬영일 우선, 없는 사진은 뒤에서 파일명 순. 전체 정렬은 사진함에서 선택한 사진 촬영일 읽기를 실행하세요. SMB 파일을 읽는 시간이 들고 재생/백그라운드 전환 때 중지합니다. 원본 사진에 쓰지 않고 버전 키별 날짜만 로컬 SQLite에 기록합니다. 날짜에 시간대가 없으면 카메라 현지시각 기준입니다.
- 시계/촬영일/파일명을 각각 선택, 모두 기본 꺼짐. 네 모서리와 불투명도20~100%. 파일명 노출과 고정 정보 장기 표시를 주의하세요.
- 별도 오프라인 보관 앨범은 추가하지 않았습니다. 기존 임시 캐시와 예시/기기 폴더 기능은 유지합니다.
