# Android JNI probe

This is a command-line check using Android `app_process`, not part of the shipped APK.
Compile `NativeCodec.java` against android.jar, convert to DEX with d8, and package classes.dex as probe.jar. Push it and the built libpiccodec.so to /data/local/tmp. Set CLASSPATH=/data/local/tmp/probe.jar and run `app_process /system/bin com.picviewer.app.NativeCodec` on a development emulator. It intentionally uses the JNI class/method name from the application to exercise the same native entry point and Android Bitmap allocation.

Generate a 48x32 RGB TIFF with pixel (100,80,50), using LZW compression, as /data/local/tmp/picviewer-test.tif. Put an invalid TIFF at /data/local/tmp/picviewer-broken.tif. Supply the CC0 Nikon D7000 NEF and small Canon EOS 5D Mark III-derived DNG as /data/local/tmp/picviewer-test.NEF and picviewer-test.dng. Source and SHA-256 references are in tests/raw-sample-sources.json; no sample image binaries are bundled in this source tree. The probe prints JNI_ALL_PASS only after all assertions pass.

The larger NEF's complete RAW render is intentionally not in this bounded smoke test. It can hit the decoder's processing deadline on a software-emulated CPU. This test does not check the application's Java loader, lifecycle, physical ARM hardware, Windows or Wi-Fi performance.

## Keystore smoke probe

CryptoProbe.java is a separate development-only command-line check. Compile it with the unchanged application's Config.java and core Paths.java into a DEX/JAR and invoke it through `run-as com.picviewer.app`. Its reflection setup supplies the Application/Context that a normal app process gets automatically. That setup is not shipped in the APK.

It tests the real Android Keystore provider with a synthetic, non-account value: encrypt/decrypt round trips, different IVs for repeated encryption and rejection of modified ciphertext. It prints only a pass marker, not plaintext or ciphertext. It does not prove hardware-backed storage on a physical tablet, actual connection-form persistence, or recovery across Android upgrades.
