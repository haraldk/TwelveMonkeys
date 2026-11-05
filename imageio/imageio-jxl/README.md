# TwelveMonkeys ImageIO » JPEG XL plugin

ImageIO plugin for reading [JPEG XL](https://jpeg.org/jpegxl/) (JXL) images. It wraps
[JXLatte](https://github.com/Traneptora/jxlatte), a pure Java JPEG XL decoder.

**Status:** Work in progress, read only. JXLatte can only decode, so there's no writer.

This file tracks what the plugin supports, its limits and its quirks. Keep it up to date when the plugin changes.

## Requirements

| Requirement           | Notes |
|-----------------------|-------|
| Java 21+ at runtime   | JXLatte is compiled for Java 21. The plugin itself is compiled for Java 8 like the rest of TwelveMonkeys, so it can sit on an older JVM's class path without breaking anything (see below). |
| JXLatte on class path | `com.traneptora:jxlatte` is a `provided` dependency, so it has to be added explicitly. It must have `JXLDecoder.readHeader()`, which no JXLatte release has yet. For now use the patched build in `lib/` (see "Known issues"). |
| Brotli (optional)     | `org.brotli:dec` (pure Java, MIT License) is an optional `provided` dependency. With it on the class path, Brotli-compressed (`brob`) Exif and XMP boxes are read; without it, they are skipped with a warning. |
| JDK 21+ to build      | The module is only part of the build when the `jdk21+` profile in `imageio/pom.xml` is active, which happens automatically on JDK 21 or later. |

If JXLatte is missing, too old (no `JXLDecoder.readHeader()`) or can't be loaded (for example on Java older than 21),
`JXLImageReaderSpi` removes itself from the ImageIO registry when it is registered. In that case ImageIO finds no JPEG XL reader, and nothing fails.

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-jxl</artifactId>
    <version>${twelvemonkeys.version}</version>
</dependency>
<dependency>
    <groupId>com.traneptora</groupId>
    <artifactId>jxlatte</artifactId>
    <version>0.1.0-tm.4</version> <!-- Patched build, from lib/ in this module, see "Known issues" -->
</dependency>
<!-- Optional, for Brotli compressed Exif/XMP boxes -->
<dependency>
    <groupId>org.brotli</groupId>
    <artifactId>dec</artifactId>
    <version>0.1.2</version>
</dependency>
```

## Format registration

| Property     | Value                                                    |
|--------------|----------------------------------------------------------|
| Format names | `jxl`, `JXL`, `jpeg-xl`, `JPEG-XL`                       |
| Suffixes     | `jxl`                                                    |
| MIME types   | `image/jxl`                                              |
| Input types  | `ImageInputStream`                                       |
| Detection    | Raw codestream (`FF 0A`) and ISO BMFF container (`00 00 00 0C 'JXL ' 0D 0A 87 0A`) |

## Capabilities

✔ supported, ◐ partial, ✘ not supported, ☐ planned

| Feature                                   | Status | Notes |
|-------------------------------------------|:------:|-------|
| Raw codestream and container files        | ✔ | Both are tested. |
| Lossless (Modular) images                 | ✔ | |
| Lossy (VarDCT, XYB) images                | ✔ | |
| Losslessly recompressed JPEGs             | ✔ | Decoded to pixels. The original JPEG bytes are not reconstructed. |
| Orientation                               | ✔ | JXLatte applies the orientation from the header, so width and height are those of the rotated image. |
| Alpha channel                             | ✔ | Premultiplied alpha is converted to straight alpha. |
| Embedded ICC profile                      | ✔ | Used as the image's color space if Java can load it and it has the right number of components. Otherwise the image is converted as if it had no profile. The samples are not converted. Exception: XYB (lossy) images, see quirks. |
| Color conversion to sRGB                  | ✔ | Done by JXLatte for color images without a usable ICC profile, the same way as its own PNG output. Colors outside sRGB (wide gamut) are clipped (not tested). |
| 1–8 bits per sample                       | ✔ | Returned as 8 bits per sample (`TYPE_BYTE`), scaled to the full 0–255 range. |
| 9–16 bits per sample                      | ✔ | Returned as 16 bits per sample (`TYPE_USHORT`), scaled to the full 0–65535 range. The sample count decides: if color or alpha needs more than 8 bits, all bands use 16 bits. |
| Float samples                             | ◐ | Returned as 16 bits per sample, clipped to 0–1. Lossy and lossless float work (tested lossless: 16-bit float (f16), 24-bit and 32-bit float, f16 alpha), lossless float needs the JXLatte fix (see quirks). |
| HDR (PQ, HLG)                             | ◐ | Converted to SDR sRGB by JXLatte (16 bits per sample): linear light is scaled so that the brightest pixel becomes 1.0 (if it's below 1.0, i.e. 10000 nits), then gamut mapped and clipped. That isn't real tone mapping, highlights clip. PQ needs the JXLatte fixes, HLG the JXLatte HLG change (see quirks). HLG is converted from scene light without the OOTF (system gamma) and isn't scaled, so it comes out darker than intended (HLG reference white, 75 %, becomes about 55 % in sRGB). |
| Grayscale                                 | ✔ | `TYPE_BYTE_GRAY` / `TYPE_USHORT_GRAY` (or gray + alpha) in `CS_GRAY`, with samples using the sRGB transfer function (gamma). That's the same convention as the JDK's PNG and JPEG readers. A gray ICC profile is used if embedded (not tested). |
| Premultiplied alpha                       | ◐ | Converted to straight alpha before any color conversion. Not tested (no sample). |
| Extra channels other than the first alpha | ✘ | Ignored (depth, spot colors, more alpha channels, ...). |
| Source region and subsampling             | ✔ | Applied when copying from the cached full decoded image, so they don't make decoding faster. |
| Source and destination bands              | ◐ | Implemented but not tested yet (those tests are disabled in `ImageReaderAbstractTest`). |
| Destination image and offset              | ✔ | Only the type that `getImageTypes` returns is accepted (one type per image right now). |
| Java2D compatibility                      | ◐ | 16-bit RGB(A) is `TYPE_CUSTOM`, and RGB(A) with an ICC profile has a custom color space. `AffineTransformOp` can't handle either of them (Java2D limitation). |
| Size and image type without decoding      | ✔ | From the image header, see "Limits". The decoded image is checked against the header (`IIOException` if they differ). |
| Progress listener                         | ◐ | Decoding happens in one step without callbacks, before `imageStarted`. Progress events only cover the copy to the destination (a default `read` reports 100% at once). |
| Abort                                     | ◐ | Checked while copying to the destination only. Decoding can't be aborted. |
| Image metadata                            | ✔ | Standard metadata format (`javax_imageio_1.0`) only, no native format. Chroma, data, dimension and transparency describe the decoded image. `SignificantBitsPerSample` is set when the original integer samples have fewer bits (for example 12 bits decoded to 16). CompressionTypeName is `JPEG XL`. `Lossless` is `FALSE` for XYB images, and left out otherwise (see quirks). Needs only the header and the container boxes, not decoding. |
| Exif / XMP                                | ✔ | Read by the plugin from the container boxes (JXLatte skips them). Exif goes into the standard Text node like in the TIFF plugin (ImageDescription, Make, Model, Software, Artist, Copyright, ...), and DateTime becomes Document/ImageCreationTime. XMP becomes a TextEntry with the keyword `XML:com.adobe.xmp`, as the JDK PNG reader does. Only the first box of each type (compressed or not) is used. Brotli-compressed (`brob`) boxes are read if `org.brotli:dec` is on the class path, otherwise they are skipped with a warning. Corrupt boxes are skipped with a warning. |
| Stream metadata                           | ✘ | `getStreamMetadata` returns `null`. |
| Thumbnails / preview frame                | ✘ | |
| Animation / multiple images               | ✘ | JXLatte doesn't support animation. `getNumImages` always returns 1. What JXLatte returns for an animated file hasn't been checked. |
| Progressive / partial decoding            | ✘ | JXLatte doesn't support it. |
| `readRaster`                              | ✘ | `canReadRaster` returns `false`. |
| Writing                                   | ✘ | JXLatte can only decode. |

## Limits

- **`getWidth`, `getHeight`, `getRawImageType` and `getImageTypes` only read the header.** They use JXLatte's
  `JXLDecoder.readHeader()` (not in a JXLatte release yet, see "Known issues"). The header includes the
  ICC profile, which is decompressed to find the color space. This takes milliseconds.
- **`read` always decodes the whole image.** JXLatte can't decode only a region. After decoding, the plugin drops the
  JXLatte decoder, which still holds full-size buffers.
  - A `read` without parameters (or with default ones) returns the decoded image itself, without copying it. As the
    caller may modify that image, it isn't kept, so a further `read` decodes again.
  - A `read` with a source region, subsampling, bands or a destination copies from the decoded image, which is kept
    for further reads of the same input.
- **Size limit:** before decoding, the plugin estimates JXLatte's memory use from the header (width × height ×
  all channels, including extra channels × 4 bytes) and refuses larger images with an `IIOException`. The limit is
  the one `ImageReaderBase` uses for input of unknown length: by default half the heap, at most 512 MB and at least
  64 MB; change it with `-Dcom.twelvemonkeys.imageio.maxImageBytes=<bytes>`. An expansion ratio based on the file size,
  as other plugins use, doesn't work for JPEG XL: valid files compress extremely well (JXLatte's `quilt.jxl` is 27
  bytes for 1024x1024 RGB, more than 100,000:1).
- **Memory:** JXLatte keeps samples in `int`/`float` arrays (4 bytes per sample per channel), and color conversion can
  make full copies. On top of that comes the decoded image (and the destination, when copying). Expect memory use to
  peak at a few times the estimate above.
- **Out of memory:** an `OutOfMemoryError` while JXLatte reads or decodes (typically a single, too large allocation
  from corrupt data) is reported as `IIOException`, as `ImageReaderBase.getDestination` does.
- **CPU:** JXLatte runs parts of decoding with `CompletableFuture.runAsync`, which uses the JVM's **common
  `ForkJoinPool`**, normally one thread per core. Set `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`
  to limit it. Note that this affects everything using the common pool. `JXLOptions.threads` exists but JXLatte
  never uses it.

## Quirks and known issues

- **Truncated input hangs in JXLatte 0.1.0.** In `Frame.decodeFrame`, a background task reads the parts of the image
  and hands them to the decoder. If it hits end of file it fails, but the parts the decoder is still waiting for never
  arrive. The decoder waits for them with `join()`, which ignores interrupts, so the reading thread blocks forever.
  Status: fixed in the patched JXLatte in `lib/`, reported as [#41](https://github.com/Traneptora/jxlatte/issues/41), PR [#42](https://github.com/Traneptora/jxlatte/pull/42). Until a JXLatte
  release has the fix, `JXLImageReaderTest.testReadTruncated` only passes against a patched JXLatte.
- **JXLatte's color transforms can drop extra channels.** `JXLImage.transform(...)` (when it has to linearize or convert
  primaries) creates new buffers and only fills the color channels, leaving the extra channels empty. Before
  transforming, the plugin therefore takes the alpha buffer from the image as decoded.
- **`JXLImage.transform(...)` fails on gray images:** it can expand gray to RGB (`fillColor()`), but doesn't extend the
  bit depth array, so `toneMapLinear` throws `ArrayIndexOutOfBoundsException` (for example on a linear 16-bit gray
  image, also with the JXLatte CLI). For gray images the plugin only uses `JXLImage.transfer(...)`.
- **`JXLImage.transferInPlace` scales int samples wrong:** it calls `castToFloatWithMax(bitDepth)` with the bit depth
  where the maximum value belongs. That breaks integer images tagged as linear light. Before converting, the plugin
  turns the color samples of linear images into floats itself.
- **JXLatte's PQ transfer function is wrong:** `TransferFunction.TF_PQ.toLinear` uses `c2 + c3·d` where SMPTE ST 2084
  has `c2 - c3·d`, and both directions use `m1 = 2612/16384` instead of `2610/16384`. PQ samples that need
  linearizing (anything not XYB-encoded, for example lossless PQ) end up around 1e-15, so the image is black.
  XYB-encoded HDR images are decoded straight to linear light and aren't affected by the sign error.
  Status: fixed in the patched JXLatte in `lib/`, reported as [#43](https://github.com/Traneptora/jxlatte/issues/43), PR [#44](https://github.com/Traneptora/jxlatte/pull/44).
- **`MathHelper.max(float...)` returns the minimum** (reversed comparison). This breaks peak detection when converting
  PQ to SDR (the peak becomes the largest row minimum, so images are scaled far too bright), and the maximum color
  when rendering splines. Status: fixed in the patched JXLatte in `lib/`,
  reported as [#45](https://github.com/Traneptora/jxlatte/issues/45), PR [#46](https://github.com/Traneptora/jxlatte/pull/46). `JXLImageReaderTest.testReadPQToSRGB` only passes with both PQ fixes.
- **HLG isn't implemented in JXLatte** (`UnsupportedOperationException`, reported as `IIOException`), unless the image
  is XYB-encoded. Status: implemented in the patched JXLatte in `lib/` (BT.2100 HLG OETF and inverse, without the OOTF),
  reported as [#50](https://github.com/Traneptora/jxlatte/issues/50), PR [#51](https://github.com/Traneptora/jxlatte/pull/51). `JXLImageReaderTest.testReadHLGToSRGB` only passes with it. Converting HLG to SDR or PQ still lacks
  the OOTF (system gamma) and any scaling of reference white: HLG scene light 1.0 becomes sRGB 1.0 (SDR is too dark),
  or 10000 nits in the JXLatte CLI's PQ output (instead of the nominal 1000 nits).
- **Lossless 32-bit float fails in JXLatte:** a valid lossless `grayf32` file (decodes fine with libjxl) fails with
  "Invalid input bitstream" while decoding modular data. JXLatte computes predictions with 32-bit `int`s, which overflow
  for 32-bit samples (libjxl uses 64 bits), and converts modular float samples numerically instead of reinterpreting
  their bits. Lossless float images in other formats (f16, 24-bit float) decode, but with wrong values (the bits read
  as an integer). Lossy float images decode. Status: fixed in the patched JXLatte in `lib/`, previously reported as
  [#37](https://github.com/Traneptora/jxlatte/issues/37).
  The `JXLImageReaderTest.testRead*FloatLossless*` tests only pass with the fix.
- **XYB images ignore their ICC profile:** for XYB-encoded (lossy) images, JXLatte doesn't return the ICC profile and
  converts from the color encoding in the header instead. Not verified with a sample.
- **`ImageTypeSpecifier.createFromRenderedImage` loses ICC color spaces:** Java2D classifies the decoded image's layout
  as a standard type (such as `TYPE_3BYTE_BGR`), and `createFromRenderedImage` then returns that standard type in sRGB.
  So the plugin creates its image type from the decoded image's own color model and sample model.
- **Logging:** by default JXLatte writes to `System.err`. The plugin turns this off by setting
  `JXLOptions.verbosity = Loggers.LOG_BASE - 1`.
- **Errors:** JXLatte reports invalid data as `InvalidBitstreamException`, but corrupt data can also cause other
  runtime exceptions, possibly wrapped in `CompletionException`. The plugin turns all of these into `IIOException`.
  End of file and other I/O errors are passed on as they are (for example `EOFException`).
  `JXLImageReaderTest.testReadCorruptedFuzz` checks this with 200 deterministic random corruptions of small samples
  (results are only success or `IOException`, no other exceptions, errors or hangs). A larger run of 1,350 cases
  (256 MB heap) found nothing else after the fix below.
- **JXLatte allocated buffers from untrusted lengths:** extension payloads, TOC entries and the encoded ICC profile
  were allocated with their (64-bit) length from the bitstream before reading the data. A few corrupt bits in a
  43-byte file caused allocations of up to 2 GB (`OutOfMemoryError`, found by fuzzing, sample
  `art-corrupt-extensions.jxl`). Status: fixed in the patched JXLatte in `lib/`,
  reported as [#47](https://github.com/Traneptora/jxlatte/issues/47), PR [#48](https://github.com/Traneptora/jxlatte/pull/48). Remaining in JXLatte (not fixed): the decoded ICC profile size and frame sizes from frame
  headers (frames can be larger than the image) are still trusted. The size limit above only covers the image header,
  and the out-of-memory handling is the safety net for these.
- **Lossless or not is unknown for non-XYB images:** that depends on the frame encoding (VarDCT or Modular, and Modular
  can be lossy too), which only the frame headers tell. Even JXLatte's `--info` only says "Possibly". So the standard
  metadata only reports `Lossless` = `FALSE` for XYB images, and leaves it out otherwise.
- **Metadata boxes can come after the codestream:** to find them, `getImageMetadata` scans all container boxes, which
  reads through (or seeks over) the whole file. It saves and restores the stream position, as the JXLatte decoder may
  still continue after the header.
- **Exif orientation is ignored:** the JPEG XL spec makes the codestream orientation the one that counts, and JXLatte
  applies it while decoding. So the metadata always reports orientation `Normal`, since the decoded image is already
  rotated.
- **Brotli is optional:** neither the JDK nor JXLatte has a Brotli decoder, so compressed metadata boxes (`brob`) need
  `org.brotli:dec` on the class path. Only `BrotliDecompressor` references it, and only if the library is available
  (checked once, in `JXLImageReader.BROTLI_AVAILABLE`), so the plugin loads and works without it. The decompressed
  size has the same 64 MB limit as uncompressed boxes. `org.brotli:dec` 0.1.2 (2017) is the latest release on
  Maven Central. JXLatte skips `brob` boxes without decompressing them, which is fine, as the codestream can't be
  Brotli-compressed.
- **Two JXLatte samples have embedded ICC profiles:** `patches-lossless.jxl` ("Colour LCD Calibrated") and
  `bench.jxl` ("BRG").
- **JXLatte's sample size info:** `--parse-only` in the JXLatte CLI shows the size before orientation is applied (for
  example `bench.jxl` is listed as 500x606 but decodes as 606x500).
- **JXLatte has no public API to read only the header.** Added `JXLDecoder.readHeader()` (and
  `JXLCodestreamDecoder.readHeader()`) in the patched JXLatte in `lib/` (previously requested as [#38](https://github.com/Traneptora/jxlatte/issues/38), PR [#49](https://github.com/Traneptora/jxlatte/pull/49)). It moves
  the existing header parsing out of `decode()`. A following `decode()` continues after the header, and `JXLDecoder`
  doesn't reset the demuxer in that case (the reset would make the container demuxer look for the signature in the
  middle of the data). The plugin needs this API.
- **Patched JXLatte in `lib/`:** JXLatte `main` with the fixes and the header API described above (each submitted
  upstream as a separate pull request), shipped with this module as `com.traneptora:jxlatte:0.1.0-tm.4`, in the file
  based Maven repository `lib/`, which the module `pom.xml` declares. `lib/README.md` describes how to rebuild. Remove `lib/` and the `<repository>` once JXLatte has a
  release with these changes.
- **Not released yet:** the module isn't part of releases for now, and isn't in the BOM (`bom/pom.xml` has a TODO).
  The release job builds on Java 8, so it doesn't build or deploy this module anyway. To release it:
  - JXLatte with the changes above should be on Maven Central (upstream release). Then switch `jxlatte.version` to
    it, and remove `lib/` and the `<repository>` from the module `pom.xml`.
  - The release job must build on JDK 21+. As all other modules still target Java 8, the build then needs
    `--release 8` for JDK 9+ (not only `-source`/`-target 8`), otherwise Java 8 users can hit `NoSuchMethodError`s
    (for example covariant `ByteBuffer` methods). Alternatively, deploy this module in a separate JDK 21 step.
  - Add the module back to the BOM.

## Development

No setup needed: the patched JXLatte is resolved from `lib/`. NOTE: Maven's offline mode (`-o`) also blocks
file repositories, so the first build after a JXLatte update in `lib/` must not be offline (no network access is
needed for it, though). To test with other JXLatte changes, rebuild and add a
new version to `lib/` as described in `lib/README.md`.

Run the module tests with decoding limited to one thread:

```sh
cd imageio/imageio-jxl
mvn test "-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=1"
```

Test resources (`src/test/resources/jxl`) are JXLatte samples (MIT License, see `README.txt` there), plus reference
PNGs decoded by the JXLatte CLI. Most tests in `ImageReaderAbstractTest` only use the first test sample, so keep that
one small.

## Roadmap

1. ✔ Module skeleton, SPI, format detection
2. ✔ Minimal reader (8-bit RGB/RGBA, ICC or sRGB)
3. ✔ Color and bit depth: 16-bit output, gray without RGB expansion, premultiplied alpha, working ICC profiles, PQ and HLG
   (with JXLatte fixes). Possible later: float output (`TYPE_FLOAT`) and/or HDR output without conversion to SDR
   (custom `ImageReadParam`?)
4. ✔ Read size, bit depth, alpha and color info from the header without decoding the image (JXLatte header API)
5. ✔ Metadata: standard metadata format, Exif/XMP from the container. Possible later: native metadata format (raw
   Exif/XMP, for example a TIFF native tree as the TIFF and JPEG plugins have), and `brob` boxes (✔ with optional `org.brotli:dec`)
6. ✔ Robustness: fuzz regression test, size limit before decoding, out of memory as `IIOException`, no copy for
   default reads, JXLatte allocation fix
7. ◐ Integration: ✔ project README format table and build note. ✔ CI: the existing JDK 21 and 25 jobs (Linux,
   Windows, macOS) build and test the module automatically (`jdk21+` profile), the JDK 8/11/17 jobs skip it; the
   JDK 25 javadoc job includes it. ☐ Release: postponed until JXLatte has an upstream release with the changes; not
   in the BOM until then (see "Known issues")
