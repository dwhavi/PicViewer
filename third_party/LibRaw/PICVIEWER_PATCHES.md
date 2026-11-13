# PicViewer changes to LibRaw 0.22.3

Upstream tag: https://github.com/LibRaw/LibRaw/releases/tag/0.22.3 . Selected license: CDDL 1.0. Original copyright and license notices remain in place.

Modified 2026-11-14 for PicViewer:

- src/decoders/decoders_dcraw.cpp, ljpeg_diff: return zero for a zero-bit difference before testing the sign bit. UndefinedBehaviorSanitizer found a negative shift exponent on valid DNG, PEF and CR2 test samples.
- src/decoders/crx.cpp, crxDecodeSymbolL1: replace a left shift of a signed difference by multiplication by two. UndefinedBehaviorSanitizer found a negative left-shift operand on the EOS R CR3 sample.

These patches do not claim a full security review of LibRaw. Test sample sources and hashes are recorded in ../../tests/raw-sample-sources.json.
