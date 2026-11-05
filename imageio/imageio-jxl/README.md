# TwelveMonkeys ImageIO » JPEG XL plugin

ImageIO plugin for reading [JPEG XL](https://jpeg.org/jpegxl/) (JXL) images. It wraps
[JXLatte](https://github.com/Traneptora/jxlatte), a pure Java JPEG XL decoder.

**Status:** Work in progress, read only. JXLatte can only decode, so there's no writer.

This file tracks what the plugin supports, its limits and its quirks. Keep it up to date when the plugin changes.

## Requirements

| Requirement           | Notes |
|-----------------------|-------|
| Java 21+ at runtime   | JXLatte is compiled for Java 21. The plugin itself is compiled for Java 8 like the rest of TwelveMonkeys, so it can sit on an older JVM's class path without breaking anything (see below). |
| JXLatte on class path | `com.traneptora:jxlatte` is a `provided` dependency, so it has to be added explicitly. |
| JDK 21+ to build      | The module is only part of the build when the `jdk21+` profile in `imageio/pom.xml` is active, which happens automatically on JDK 21 or later. |

If JXLatte is missing or can't be loaded (for example on Java older than 21), `JXLImageReaderSpi` removes itself from the
ImageIO registry when it is registered. In that case ImageIO finds no JPEG XL reader, and nothing fails.

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-jxl</artifactId>
    <version>${twelvemonkeys.version}</version>
</dependency>
<dependency>
    <groupId>com.traneptora</groupId>
    <artifactId>jxlatte</artifactId>
    <version>0.1.0</version> <!-- See "Known issues" about the version -->
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
| Embedded ICC profile                      | ◐ | Used as the image's color space if Java can load it and it has the right number of components. Otherwise the image is converted to sRGB. |
| Color conversion to sRGB                  | ✔ | Done for images without a usable ICC profile, the same way as JXLatte's own PNG output. |
| Bit depth > 8 / HDR / float samples       | ◐ | Decoded, but reduced to 8 bits per sample. HDR (PQ/HLG) is tone mapped to sRGB. ☐ Step 3 |
| Grayscale                                 | ◐ | Without an ICC profile, gray is expanded to RGB (sRGB). ☐ Step 3. No test samples yet. |
| Extra channels other than the first alpha | ✘ | Ignored (depth, spot colors, more alpha channels, ...). |
| Source region and subsampling             | ✔ | Applied when copying from the cached full decoded image, so they don't make decoding faster. |
| Source and destination bands              | ◐ | Implemented but not tested yet (those tests are disabled in `ImageReaderAbstractTest`). |
| Destination image and offset              | ✔ | Only the type that `getImageTypes` returns is accepted (one type per image right now). |
| Progress listener                         | ◐ | Decoding happens in one step without callbacks, so progress events only cover the copy to the destination. |
| Abort                                     | ◐ | Checked while copying to the destination only. Decoding can't be aborted. |
| Image metadata                            | ✘ | `getImageMetadata` returns `null`. ☐ Step 5 (standard metadata format) |
| Exif / XMP                                | ✘ | JXLatte doesn't expose these boxes. ☐ Step 5 |
| Stream metadata                           | ✘ | `getStreamMetadata` returns `null`. |
| Thumbnails / preview frame                | ✘ | |
| Animation / multiple images               | ✘ | JXLatte doesn't support animation. `getNumImages` always returns 1. What JXLatte returns for an animated file hasn't been checked. |
| Progressive / partial decoding            | ✘ | JXLatte doesn't support it. |
| `readRaster`                              | ✘ | `canReadRaster` returns `false`. |
| Writing                                   | ✘ | JXLatte can only decode. |

## Limits

- **The whole image is always decoded.** JXLatte can't decode only a region, so even `getWidth`/`getHeight` decode the
  complete image (☐ Step 4: read the header without decoding). The result is cached for the current input, so later calls
  and further `read`s don't decode again.
- **Memory:** JXLatte keeps samples in `int`/`float` arrays (4 bytes per sample per channel), and color conversion can
  make full copies. On top of that, the plugin caches the decoded 8-bit image and allocates the destination image.
  Expect memory use to peak at several times the size of the decoded image. There's no size or memory limit yet
  (☐ Step 6).
- **CPU:** JXLatte runs parts of decoding with `CompletableFuture.runAsync`, which uses the JVM's **common
  `ForkJoinPool`**, normally one thread per core. Set `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`
  to limit it. Note that this affects everything using the common pool. `JXLOptions.threads` exists but JXLatte
  never uses it.

## Quirks and known issues

- **Truncated input hangs in JXLatte 0.1.0.** In `Frame.decodeFrame`, a background task reads the parts of the image
  and hands them to the decoder. If it hits end of file it fails, but the parts the decoder is still waiting for never
  arrive. The decoder waits for them with `join()`, which ignores interrupts, so the reading thread blocks forever.
  Status: fixed on the local JXLatte branch `fix-truncated-input-hang`, to be submitted upstream. Until a JXLatte
  release has the fix, `JXLImageReaderTest.testReadTruncated` only passes against a patched JXLatte.
- **JXLatte's color transforms can drop extra channels.** `JXLImage.transform(...)` (when it has to linearize or convert
  primaries) creates new buffers and only fills the color channels, leaving the extra channels empty. Before
  transforming, the plugin therefore takes the alpha buffer from the image as decoded.
- **After `JXLImage.fillColor()`, bit depths are wrong:** gray-to-RGB expansion adds two buffers but doesn't extend the
  bit depth array. The plugin doesn't use `fillColor()` and expands gray itself.
- **Logging:** by default JXLatte writes to `System.err`. The plugin turns this off by setting
  `JXLOptions.verbosity = Loggers.LOG_BASE - 1`.
- **Errors:** JXLatte reports invalid data as `InvalidBitstreamException`, but corrupt data can also cause other
  runtime exceptions, possibly wrapped in `CompletionException`. The plugin turns all of these into `IIOException`.
  End of file and other I/O errors are passed on as they are (for example `EOFException`).
- **JXLatte's sample size info:** `--parse-only` in the JXLatte CLI shows the size before orientation is applied (for
  example `bench.jxl` is listed as 500x606 but decodes as 606x500).
- **No JXLatte on Maven Central:** `com.traneptora:jxlatte:0.1.0` currently has to be installed locally (see below).
  To release this module, JXLatte has to be published to Maven Central, since Central artifacts can't depend on
  JitPack.

## Development

Install JXLatte locally, either built with Maven (`mvn install` in the JXLatte checkout), or from an existing jar:

```sh
mvn install:install-file -Dfile=../jxlatte/build/src/jxlatte.jar -DpomFile=../jxlatte/pom.xml
```

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
2. ✔ Minimal reader (8-bit RGB/RGBA/gray, ICC or sRGB)
3. ☐ Color and bit depth: 16-bit and float output, HDR handling, gray without RGB expansion, premultiplied alpha
   handling
4. ☐ Read size, bit depth, alpha and color info from the header without decoding the image
5. ☐ Metadata: standard metadata format, Exif/XMP from the container
6. ☐ Robustness: malformed input tests, memory/size limits, avoid caching a copy when possible
7. ☐ Integration: project README format table, CI on JDK 21+, release (JXLatte availability on Maven Central)
