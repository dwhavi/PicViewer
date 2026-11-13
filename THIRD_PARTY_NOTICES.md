# Third-party notices

## App source

No general public license has been selected for original PicViewer application code. Bundled third-party code keeps its own license. Public repository visibility does not change those terms.

## Bundled native source

- LibRaw 0.21.4, https://github.com/LibRaw/LibRaw (tag 0.21.4). Used under CDDL 1.0, selected from the upstream LGPL 2.1 / CDDL 1.0 dual license. The complete source and notices are in `third_party/LibRaw`. No changes to upstream source. The app's adapter is `app/src/main/cpp/piccodec.cpp`. Optional RawSpeed, DNG SDK, Jasper, LCMS and OpenMP components are not enabled. Support is limited to the compiled version and features.
- LibTIFF 4.7.2, https://download.osgeo.org/libtiff/tiff-4.7.2.tar.gz . Full source and permission notice in `third_party/libtiff/LICENSE.md`. No upstream changes. JPEG-compressed TIFF and optional ZSTD/WebP/LZMA/JBIG/LERC codecs are disabled in the present native build; this does not affect the separate Android JPEG decoder.
- libjpeg-turbo 3.1.0, https://github.com/libjpeg-turbo/libjpeg-turbo . Full source, README.ijg and LICENSE.md are in `third_party/libjpeg-turbo`. No upstream changes. Static JPEG8-compatible build; SIMD is disabled in this initial portable build. The external build wrapper is original app build configuration.

## Maven dependencies

SMBJ 0.13.0 is Apache 2.0. AndroidX ExifInterface 1.3.7 is Apache 2.0. SLF4J is MIT. SMBJ also brings Bouncy Castle (MIT-style license), ASN.1 and event-bus dependencies. The APK includes license texts under assets/licenses, accessible from Settings. Full resolved dependency versions can be inspected with `./gradlew :app:dependencies --configuration debugRuntimeClasspath`. Redistribution must preserve applicable notices. Test-only JUnit, Robolectric and Android test libraries are not app functionality.

## Demo photos

- `01-mountain.jpg`: Daniel Seßler, "The sun is setting over a mountain with a lake in the background", published 2024-07-28. https://unsplash.com/photos/the-sun-is-setting-over-a-mountain-with-a-lake-in-the-background-4--L_I3xug8 . Unsplash License. https://unsplash.com/license . Downloaded from the photographer's observed image URL.
- `03-portrait.jpg` and `04-details.jpg` are crops of the same photo. The demo is illustrative; the folder names are fictional.
- `02-colour.png` is an original simple colour/shape test chart, original project material.

No personal photos, real server addresses, account passwords or signing keys belong in this repository.
