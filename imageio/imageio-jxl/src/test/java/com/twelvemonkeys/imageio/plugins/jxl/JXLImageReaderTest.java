/*
 * Copyright (c) 2026, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jxl;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.w3c.dom.NodeList;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class JXLImageReaderTest extends ImageReaderAbstractTest<JXLImageReader> {

    @Override
    protected List<TestData> getTestData() {
        // NOTE: Most tests only use the first sample, keep it small, as decoding is CPU intensive
        return asList(
                // Modular, codestream
                new TestData(getClassLoaderResource("/jxl/art.jxl"), new Dimension(128, 128)),
                // VarDCT, XYB, codestream
                new TestData(getClassLoaderResource("/jxl/white.jxl"), new Dimension(320, 240)),
                new TestData(getClassLoaderResource("/jxl/lenna.jxl"), new Dimension(512, 512)),
                // VarDCT (JPEG reconstruction), container, orientation (stored as 500x606, rotated by decoder)
                new TestData(getClassLoaderResource("/jxl/bench.jxl"), new Dimension(606, 500)),
                // Modular with alpha and patches, container
                new TestData(getClassLoaderResource("/jxl/patches-lossless.jxl"), new Dimension(1600, 1096)),
                // Synthetic, see make-test-images.py
                new TestData(getClassLoaderResource("/jxl/gray8.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/gray16.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/gray16-linear.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/graya8.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/grayf32-lossy.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/grayf32-lossless.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/grayf16-lossless.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/grayf24-lossless.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgbaf16-lossless.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgb16.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgba16.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgb8-icc.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgb16-pq.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgb16-pq1000.jxl"), new Dimension(64, 48)),
                new TestData(getClassLoaderResource("/jxl/rgb16-hlg.jxl"), new Dimension(64, 48)),
                // Container with Exif, brob, split codestream (jxlp) and XMP boxes, see make-test-container.py
                new TestData(getClassLoaderResource("/jxl/gray8-meta.jxl"), new Dimension(64, 48)),
                // Container with Brotli compressed Exif and XMP boxes
                new TestData(getClassLoaderResource("/jxl/gray8-brob.jxl"), new Dimension(64, 48))
        );
    }

    @Override
    protected List<TestData> getTestDataForAffineTransformOpCompatibility() {
        // AffineTransformOp can't handle 16 bit interleaved RGB(A) (TYPE_CUSTOM),
        // or RGBA with a non-sRGB ICC color space (patches-lossless has a "Colour LCD Calibrated" profile)
        List<String> excluded = asList("/rgb16.jxl", "/rgba16.jxl", "/rgbaf16-lossless.jxl", "/rgb16-pq.jxl", "/rgb16-pq1000.jxl", "/rgb16-hlg.jxl", "/patches-lossless.jxl");

        List<TestData> testData = new ArrayList<>(getTestData());
        testData.removeIf(data -> excluded.stream().anyMatch(data.toString()::endsWith));
        return testData;
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new JXLImageReaderSpi();
    }

    @Override
    protected List<String> getFormatNames() {
        return asList("jxl", "JXL", "jpeg-xl", "JPEG-XL");
    }

    @Override
    protected List<String> getSuffixes() {
        return asList("jxl");
    }

    @Override
    protected List<String> getMIMETypes() {
        return asList("image/jxl");
    }

    private BufferedImage read(final String resource, final ImageReadParam param) throws IOException {
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource(resource))) {
            reader.setInput(stream);
            return reader.read(0, param);
        }
        finally {
            reader.dispose();
        }
    }

    private void assertSameAsReference(final String name) throws IOException {
        BufferedImage expected = ImageIO.read(getClassLoaderResource("/jxl/" + name + "-jxlatte.png"));
        BufferedImage actual = read("/jxl/" + name + ".jxl", null);

        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());

        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertRGBEquals(String.format("Pixel differs @%d,%d", x, y), expected.getRGB(x, y), actual.getRGB(x, y), 0);
            }
        }
    }

    @Test
    void testReadModularSameAsReference() throws IOException {
        assertSameAsReference("art");
    }

    @Test
    void testReadVarDCTSameAsReference() throws IOException {
        assertSameAsReference("white");
    }

    @Test
    void testReadRGBRawType() throws IOException {
        BufferedImage image = read("/jxl/lenna.jxl", null);

        assertEquals(BufferedImage.TYPE_3BYTE_BGR, image.getType());
        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
    }

    @Test
    void testReadAlpha() throws IOException {
        // Read only a small region, to save some time
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new java.awt.Rectangle(0, 0, 64, 64));

        BufferedImage image = read("/jxl/patches-lossless.jxl", param);

        assertEquals(BufferedImage.TYPE_4BYTE_ABGR, image.getType());
        assertEquals(Transparency.TRANSLUCENT, image.getColorModel().getTransparency());
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
    }

    @Test
    @Timeout(30)
    void testReadTruncated() throws IOException {
        // NOTE: Unpatched JXLatte 0.1.0 blocks forever (not interruptible) on truncated input,
        // requires the fix in Frame.decodeFrame that fails pending TOC entry readers
        byte[] data = readResource("/jxl/lenna.jxl");

        JXLImageReader reader = createReader();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(Arrays.copyOf(data, 1024)))) {
            reader.setInput(stream);
            assertThrows(IOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    @Timeout(30)
    void testReadCorrupt() throws IOException {
        // Valid signature, followed by garbage
        byte[] data = new byte[256];
        for (int i = 2; i < data.length; i++) {
            data[i] = (byte) (i * 31);
        }
        data[0] = (byte) 0xFF;
        data[1] = 0x0A;

        JXLImageReader reader = createReader();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            reader.setInput(stream);
            assertThrows(IIOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    // Pixel formulas, mirroring make-test-images.py

    private static int gray8(final int x, final int y) {
        return (x * 4 + y) & 0xFF;
    }

    private static int gray16(final int x, final int y) {
        return (x * 1024 + y * 21 + 7) & 0xFFFF;
    }

    private static int alpha8(final int y) {
        return (y * 5) & 0xFF;
    }

    private static int[] rgb16(final int x, final int y) {
        return new int[] {(x * 1024 + y) & 0xFFFF, (y * 1365 ^ x) & 0xFFFF, 65535 - x * 1000};
    }

    private static int alpha16(final int x, final int y) {
        return ((x + y) * 700) & 0xFFFF;
    }

    private static int linearToSRGB16(final int value) {
        return linearToSRGB16(value / 65535.0);
    }

    private static int linearToSRGB16(final double linear) {
        double srgb = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
        return (int) Math.round(srgb * 65535);
    }

    private static double float16(final int x, final int y) {
        // Values exactly representable as f16, first row f16 subnormals, see make-libjxl-images.py
        return y == 0 ? x * 8 * 0x1p-24 : (x * 16 + y % 16) / 1024.0;
    }

    private static int pq16(final int x, final int y) {
        return x * 1000 + y * 20;
    }

    private static double pqToLinear(final double value) {
        // SMPTE ST 2084 EOTF, normalized to 10000 cd/m2
        double m1 = 2610 / 16384.0;
        double m2 = 2523 / 4096.0 * 128;
        double c1 = 3424 / 4096.0;
        double c2 = 2413 / 4096.0 * 32;
        double c3 = 2392 / 4096.0 * 32;

        double d = Math.pow(value, 1 / m2);
        return Math.pow(Math.max(d - c1, 0) / (c2 - c3 * d), 1 / m1);
    }

    private static double hlgToLinear(final double value) {
        // Rec. ITU-R BT.2100 HLG inverse OETF, normalized scene light (without the OOTF)
        double a = 0.17883277;
        double b = 1 - 4 * a;
        double c = 0.5 - a * Math.log(4 * a);

        return value <= 0.5 ? value * value / 3 : (Math.exp((value - c) / a) + b) / 12;
    }

    private interface PixelFunction {
        int[] expected(int x, int y);
    }

    private static void assertSamples(final BufferedImage image, final PixelFunction pixels, final int tolerance) {
        Raster raster = image.getRaster();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int[] expected = pixels.expected(x, y);
                assertEquals(expected.length, raster.getNumBands());

                for (int b = 0; b < expected.length; b++) {
                    int actual = raster.getSample(x, y, b);
                    if (Math.abs(expected[b] - actual) > tolerance) {
                        fail(String.format("Sample differs @%d,%d band %d: expected %d, actual %d (tolerance %d)", x, y, b, expected[b], actual, tolerance));
                    }
                }
            }
        }
    }

    @Test
    void testReadGray8() throws IOException {
        BufferedImage image = read("/jxl/gray8.jxl", null);

        assertEquals(BufferedImage.TYPE_BYTE_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {gray8(x, y)}, 0);
    }

    @Test
    void testReadGray16() throws IOException {
        BufferedImage image = read("/jxl/gray16.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {gray16(x, y)}, 0);
    }

    @Test
    void testReadGrayAlpha8() throws IOException {
        BufferedImage image = read("/jxl/graya8.jxl", null);

        assertEquals(ColorSpace.TYPE_GRAY, image.getColorModel().getColorSpace().getType());
        assertTrue(image.getColorModel().hasAlpha());
        assertFalse(image.getColorModel().isAlphaPremultiplied());
        assertEquals(DataBuffer.TYPE_BYTE, image.getRaster().getTransferType());
        assertSamples(image, (x, y) -> new int[] {gray8(x, y), alpha8(y)}, 0);
    }

    @Test
    void testReadGray16LinearToSRGB() throws IOException {
        // Same samples as gray16, but tagged as linear light (int samples, see JXLatte's transferInPlace bug)
        BufferedImage image = read("/jxl/gray16-linear.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {linearToSRGB16(gray16(x, y))}, 2);
    }

    @Test
    void testReadGrayFloatLossyToSRGB() throws IOException {
        // Lossy, float samples tagged as linear light, generated from gray16
        BufferedImage image = read("/jxl/grayf32-lossy.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {linearToSRGB16(gray16(x, y))}, 1000);
    }

    @Test
    void testReadGrayFloatLosslessToSRGB() throws IOException {
        // Lossless, float samples tagged as linear light, generated from gray16
        // NOTE: Requires a JXLatte with fixed lossless float decoding, otherwise reading fails
        BufferedImage image = read("/jxl/grayf32-lossless.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {linearToSRGB16(gray16(x, y))}, 2);
    }

    @Test
    void testReadRGB16() throws IOException {
        BufferedImage image = read("/jxl/rgb16.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertFalse(image.getColorModel().hasAlpha());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());
        assertSamples(image, JXLImageReaderTest::rgb16, 0);
    }

    @Test
    void testReadRGBA16() throws IOException {
        BufferedImage image = read("/jxl/rgba16.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertTrue(image.getColorModel().hasAlpha());
        assertFalse(image.getColorModel().isAlphaPremultiplied());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());
        assertSamples(image, (x, y) -> {
            int[] rgb = rgb16(x, y);
            return new int[] {rgb[0], rgb[1], rgb[2], alpha16(x, y)};
        }, 0);
    }

    @Test
    void testReadPQToSRGB() throws IOException {
        // Neutral gray PQ code values (BT.2100 PQ, BT.2020 primaries), so gamut mapping doesn't change the values.
        // JXLatte's peak detection (auto) scales the linear values so that the brightest pixel becomes 1.0.
        // NOTE: Requires a JXLatte with fixed PQ transfer function, otherwise the result is black
        BufferedImage image = read("/jxl/rgb16-pq.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());

        double peak = pqToLinear(pq16(image.getWidth() - 1, image.getHeight() - 1) / 65535.0);
        assertSamples(image, (x, y) -> {
            int expected = linearToSRGB16(pqToLinear(pq16(x, y) / 65535.0) / peak);
            return new int[] {expected, expected, expected};
        }, 64);
    }

    @Test
    void testReadGrayFloat16LosslessToSRGB() throws IOException {
        // Lossless, f16 samples (5 exponent bits) tagged as linear light, including subnormals
        // NOTE: Requires a JXLatte with fixed lossless float decoding, otherwise the samples are wrong
        BufferedImage image = read("/jxl/grayf16-lossless.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {linearToSRGB16(float16(x, y))}, 2);
    }

    @Test
    void testReadGrayFloat24LosslessToSRGB() throws IOException {
        // Lossless, 24-bit float samples (8 exponent bits) tagged as linear light
        // NOTE: Requires a JXLatte with fixed lossless float decoding, otherwise the samples are wrong
        BufferedImage image = read("/jxl/grayf24-lossless.jxl", null);

        assertEquals(BufferedImage.TYPE_USHORT_GRAY, image.getType());
        assertSamples(image, (x, y) -> new int[] {linearToSRGB16(float16(x, y))}, 2);
    }

    @Test
    void testReadRGBAFloat16LosslessToSRGB() throws IOException {
        // Lossless, f16 color and alpha (extra channel) samples, linear sRGB
        // NOTE: Requires a JXLatte with fixed lossless float decoding, otherwise the samples are wrong
        BufferedImage image = read("/jxl/rgbaf16-lossless.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertTrue(image.getColorModel().hasAlpha());
        assertFalse(image.getColorModel().isAlphaPremultiplied());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());
        assertSamples(image, (x, y) -> new int[] {
                linearToSRGB16(float16(x, y)),
                linearToSRGB16(float16(image.getWidth() - 1 - x, y)),
                linearToSRGB16(y * 21 / 1024.0),
                (int) Math.round((y * 16 + x % 16) / 1024.0 * 65535)
        }, 2);
    }

    @Test
    void testReadPQIntensityTargetToSRGB() throws IOException {
        // Same PQ code values as rgb16-pq.jxl, but with an intensity target of 1000 nits (not 10000).
        // Linear light is relative to the intensity target (1.0 is 1000 nits, like in libjxl), so the brightest pixel
        // is above 1.0, peak detection doesn't scale down, and everything from 1000 nits on clips.
        // NOTE: Requires a JXLatte with the PQ intensity target fix, otherwise the peak is scaled to 1.0 like for rgb16-pq
        BufferedImage image = read("/jxl/rgb16-pq1000.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());

        assertSamples(image, (x, y) -> {
            int expected = linearToSRGB16(Math.min(1.0, pqToLinear(pq16(x, y) / 65535.0) * 10000 / 1000));
            return new int[] {expected, expected, expected};
        }, 64);
    }

    @Test
    void testReadHLGToSRGB() throws IOException {
        // Neutral gray HLG code values (BT.2100 HLG, BT.2020 primaries), so gamut mapping doesn't change the values.
        // JXLatte maps scene light 1.0 to sRGB 1.0 (no OOTF, no peak detection for HLG), so the result is rather dark.
        // NOTE: Requires a JXLatte with the HLG transfer function, otherwise reading fails
        BufferedImage image = read("/jxl/rgb16-hlg.jxl", null);

        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getTransferType());

        assertSamples(image, (x, y) -> {
            int expected = linearToSRGB16(hlgToLinear(pq16(x, y) / 65535.0));
            return new int[] {expected, expected, expected};
        }, 4);
    }

    @Test
    void testReadICCProfile() throws IOException {
        // Pure red, green and blue thirds, with a profile that swaps red and green
        BufferedImage image = read("/jxl/rgb8-icc.jxl", null);

        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        assertInstanceOf(ICC_ColorSpace.class, colorSpace);
        assertFalse(colorSpace.isCS_sRGB());

        // Samples are not converted
        assertSamples(image, (x, y) -> {
            int third = Math.min(x * 3 / image.getWidth(), 2);
            return new int[] {third == 0 ? 255 : 0, third == 1 ? 255 : 0, third == 2 ? 255 : 0};
        }, 0);

        // ...but colors are, through the profile
        int red = image.getRGB(5, 10);
        int green = image.getRGB(32, 10);
        assertTrue(((red >> 8) & 0xFF) > ((red >> 16) & 0xFF) + 100, String.format("Expected green: %08x", red));
        assertTrue(((green >> 16) & 0xFF) > ((green >> 8) & 0xFF) + 100, String.format("Expected red: %08x", green));
    }

    private JXLImageReader readerForTruncated(final String resource, final int length) throws IOException {
        JXLImageReader reader = createReader();
        reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(Arrays.copyOf(readResource(resource), length))));
        return reader;
    }

    @Test
    @Timeout(30)
    void testHeaderOnlyCodestream() throws IOException {
        // The header is intact, but the image data is truncated: Size and type must be available without decoding
        JXLImageReader reader = readerForTruncated("/jxl/lenna.jxl", 1024);

        try {
            assertEquals(512, reader.getWidth(0));
            assertEquals(512, reader.getHeight(0));

            ImageTypeSpecifier rawType = reader.getRawImageType(0);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, rawType.getBufferedImageType());
            assertTrue(rawType.getColorModel().getColorSpace().isCS_sRGB());

            assertThrows(IOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    @Timeout(30)
    void testHeaderOnlyContainerOrientationICC() throws IOException {
        // Container, orientation (stored as 500x606) and embedded ICC profile, image data truncated
        JXLImageReader reader = readerForTruncated("/jxl/bench.jxl", 8192);

        try {
            assertEquals(606, reader.getWidth(0));
            assertEquals(500, reader.getHeight(0));

            ColorSpace colorSpace = reader.getRawImageType(0).getColorModel().getColorSpace();
            assertInstanceOf(ICC_ColorSpace.class, colorSpace);
            assertFalse(colorSpace.isCS_sRGB());

            assertThrows(IOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    void testRawImageTypeMatchesDecoded() throws IOException {
        JXLImageReader reader = createReader();

        try {
            for (TestData data : getTestData()) {
                try (ImageInputStream stream = data.getInputStream()) {
                    reader.setInput(stream);

                    ImageTypeSpecifier rawType = reader.getRawImageType(0);
                    BufferedImage image = reader.read(0);

                    String message = data.toString();
                    assertSame(rawType.getColorModel().getColorSpace(), image.getColorModel().getColorSpace(), message);
                    assertEquals(rawType.getColorModel(), image.getColorModel(), message);
                    assertEquals(rawType.getSampleModel().getDataType(), image.getSampleModel().getDataType(), message);
                    assertEquals(rawType.getNumBands(), image.getSampleModel().getNumBands(), message);
                    assertEquals(reader.getWidth(0), image.getWidth(), message);
                    assertEquals(reader.getHeight(0), image.getHeight(), message);
                }
            }
        }
        finally {
            reader.dispose();
        }
    }

    private static IIOMetadataNode standardTree(final IIOMetadata metadata) {
        assertTrue(metadata.isStandardMetadataFormatSupported());
        return (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
    }

    private static IIOMetadataNode findNode(final IIOMetadataNode root, final String name) {
        NodeList nodes = root.getElementsByTagName(name);
        return nodes.getLength() > 0 ? (IIOMetadataNode) nodes.item(0) : null;
    }

    private static Map<String, String> textEntries(final IIOMetadataNode root) {
        Map<String, String> entries = new HashMap<>();
        NodeList nodes = root.getElementsByTagName("TextEntry");

        for (int i = 0; i < nodes.getLength(); i++) {
            IIOMetadataNode entry = (IIOMetadataNode) nodes.item(i);
            entries.put(entry.getAttribute("keyword"), entry.getAttribute("value"));
        }

        return entries;
    }

    private List<String> readMetadataAndImage(final String resource, final boolean brotliAvailable,
                                              final boolean expectExifXMP) throws IOException {
        JXLImageReader reader = createReader();
        reader.brotliAvailable = brotliAvailable;

        List<String> warnings = new ArrayList<>();
        reader.addIIOReadWarningListener((source, warning) -> warnings.add(warning));

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource(resource))) {
            reader.setInput(stream);

            // Read header, then metadata (seeks around in the stream), then decode
            assertEquals(64, reader.getWidth(0));
            IIOMetadataNode root = standardTree(reader.getImageMetadata(0));

            assertEquals("GRAY", findNode(root, "ColorSpaceType").getAttribute("name"));
            assertEquals("1", findNode(root, "NumChannels").getAttribute("value"));
            assertEquals("JPEG XL", findNode(root, "CompressionTypeName").getAttribute("value"));
            assertNull(findNode(root, "Lossless"), "Lossless can't be known for non-XYB images");

            Map<String, String> text = textEntries(root);
            IIOMetadataNode creationTime = findNode(root, "ImageCreationTime");

            if (expectExifXMP) {
                // From Exif
                assertEquals("JXL test image", text.get("ImageDescription"));
                assertEquals("TwelveMonkeys", text.get("Make"));
                assertEquals("ImageIO JXL", text.get("Model"));
                assertEquals("make-test-container.py", text.get("Software"));
                assertEquals("CC0", text.get("Copyright"));

                assertNotNull(creationTime);
                assertEquals("2026", creationTime.getAttribute("year"));
                assertEquals("11", creationTime.getAttribute("month"));
                assertEquals("5", creationTime.getAttribute("day"));
                assertEquals("19", creationTime.getAttribute("hour"));
                assertEquals("30", creationTime.getAttribute("minute"));
                assertEquals("42", creationTime.getAttribute("second"));

                // From XMP (after the codestream)
                String xmp = text.get(JXLImageMetadata.XMP_KEYWORD);
                assertNotNull(xmp);
                assertTrue(xmp.startsWith("<?xpacket begin="), xmp);
                assertTrue(xmp.contains("JXL XMP test"), xmp);
            }
            else {
                assertTrue(text.isEmpty(), text.toString());
                assertNull(creationTime);
            }

            // Decoding still works after reading the metadata
            assertSamples(reader.read(0), (x, y) -> new int[] {gray8(x, y)}, 0);
        }
        finally {
            reader.dispose();
        }

        return warnings;
    }

    @Test
    void testMetadataExifXMP() throws IOException {
        // Uncompressed Exif and XMP, split codestream, and a corrupt brob box (garbage, claiming to be XMP)
        List<String> warnings = readMetadataAndImage("/jxl/gray8-meta.jxl", true, true);

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("could not decompress"), warnings.get(0));
    }

    @Test
    void testMetadataBrotliCompressed() throws IOException {
        assertTrue(JXLImageReader.BROTLI_AVAILABLE, "org.brotli:dec should be on the test class path");

        List<String> warnings = readMetadataAndImage("/jxl/gray8-brob.jxl", true, true);
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void testMetadataBrotliUnavailable() throws IOException {
        // Brotli compressed boxes are skipped with a warning, the image itself is still readable
        List<String> warnings = readMetadataAndImage("/jxl/gray8-brob.jxl", false, false);

        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("'Exif'") && warnings.get(0).contains("org.brotli:dec"), warnings.get(0));
        assertTrue(warnings.get(1).contains("'xml '") && warnings.get(1).contains("org.brotli:dec"), warnings.get(1));
    }

    @Test
    void testMetadataXYBIsLossy() throws IOException {
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/jxl/lenna.jxl"))) {
            reader.setInput(stream);
            IIOMetadataNode root = standardTree(reader.getImageMetadata(0));

            assertEquals("RGB", findNode(root, "ColorSpaceType").getAttribute("name"));
            assertEquals("FALSE", findNode(root, "Lossless").getAttribute("value"));
            assertEquals("none", findNode(root, "Alpha").getAttribute("value"));
            assertTrue(textEntries(root).isEmpty());
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    void testMetadataAlpha16() throws IOException {
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/jxl/rgba16.jxl"))) {
            reader.setInput(stream);
            IIOMetadataNode root = standardTree(reader.getImageMetadata(0));

            assertEquals("4", findNode(root, "NumChannels").getAttribute("value"));
            assertEquals("16 16 16 16", findNode(root, "BitsPerSample").getAttribute("value"));
            assertNull(findNode(root, "SignificantBitsPerSample"));
            assertEquals("nonpremultiplied", findNode(root, "Alpha").getAttribute("value"));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    void testMetadataSignificantBits() throws IOException {
        // 12 bits per sample (color and alpha), decoded to 16 bits. Only the header is read, the image is not decoded
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/jxl/blendmodes_5.jxl"))) {
            reader.setInput(stream);
            IIOMetadataNode root = standardTree(reader.getImageMetadata(0));

            assertEquals("16 16 16 16", findNode(root, "BitsPerSample").getAttribute("value"));
            assertEquals("12 12 12 12", findNode(root, "SignificantBitsPerSample").getAttribute("value"));
        }
        finally {
            reader.dispose();
        }
    }

    /** Minimal JPEG XL codestream header writer, bits are packed LSB first. */
    private static final class BitWriter {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private long buffer;
        private int bits;

        BitWriter write(final int value, final int count) {
            buffer |= ((long) value & ((1L << count) - 1)) << bits;
            bits += count;

            while (bits >= 8) {
                bytes.write((int) buffer & 0xFF);
                buffer >>>= 8;
                bits -= 8;
            }

            return this;
        }

        byte[] toByteArray(final int padding) {
            if (bits > 0) {
                write(0, 8 - bits);
            }

            byte[] result = bytes.toByteArray();
            return Arrays.copyOf(result, result.length + padding);
        }
    }

    private static byte[] codestreamHeader(final int width, final int height) {
        return new BitWriter()
                .write(0xFF, 8).write(0x0A, 8) // Signature
                .write(0, 1) // SizeHeader: div8 = false
                .write(2, 2).write(height - 1, 18) // height: U32 selector 2, 1 + u(18)
                .write(0, 3) // ratio = 0, explicit width
                .write(2, 2).write(width - 1, 18) // width: U32 selector 2, 1 + u(18)
                .write(1, 1) // ImageMetadata: all_default
                .write(1, 1) // CustomTransformData: all_default
                .toByteArray(16); // Some bytes for the (missing) frame data
    }

    @Test
    @Timeout(30)
    void testReadHugeDeclaredSize() throws IOException {
        // 32768 x 32768 RGB (within level 5 limits), would need 12 GB as int/float samples in JXLatte
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(codestreamHeader(32768, 32768)))) {
            reader.setInput(stream);

            // The header is fine
            assertEquals(32768, reader.getWidth(0));
            assertEquals(32768, reader.getHeight(0));
            assertEquals(3, reader.getRawImageType(0).getNumBands());

            // ...but decoding is refused, before allocating anything
            IIOException e = assertThrows(IIOException.class, () -> reader.read(0));
            assertTrue(e.getMessage().contains("too large"), e.getMessage());
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    void testEstimateDecodingBytes() throws IOException {
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(codestreamHeader(1000, 500)))) {
            reader.setInput(stream);
            reader.getWidth(0); // Reads the header

            // 3 channels, 4 bytes per sample
            assertEquals(1000L * 500 * 3 * 4, JXLImageReader.estimateDecodingBytes(readHeaderOf(reader)));
        }
        finally {
            reader.dispose();
        }
    }

    private static com.traneptora.jxlatte.bundle.ImageHeader readHeaderOf(final JXLImageReader reader) throws IOException {
        // Reads the header of the current input, using the JXLatte API directly
        ImageInputStream input = (ImageInputStream) reader.getInput();
        input.seek(0);

        return new com.traneptora.jxlatte.JXLDecoder(com.twelvemonkeys.imageio.util.IIOUtil.createStreamAdapter(input)).readHeader();
    }

    @Test
    @Timeout(30)
    void testReadCorruptExtensions() throws IOException {
        // Corrupt image header extensions, unpatched JXLatte allocates (up to) 2 GB, causing OutOfMemoryError
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/jxl/art-corrupt-extensions.jxl"))) {
            reader.setInput(stream);
            assertThrows(IOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    @Timeout(120)
    void testReadCorruptedFuzz() throws Exception {
        // Deterministic random byte changes and truncations of small samples:
        // reading must either succeed, or throw IOException. No other exceptions, errors or hangs.
        String[] resources = {
                "/jxl/art.jxl", "/jxl/gray8.jxl", "/jxl/graya8.jxl", "/jxl/white.jxl",
                "/jxl/rgb16.jxl", "/jxl/gray8-meta.jxl", "/jxl/gray8-brob.jxl", "/jxl/rgb8-icc.jxl"
        };

        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jxl-fuzz");
            thread.setDaemon(true); // Don't keep the JVM alive, if a case hangs
            return thread;
        });

        try {
            for (String resource : resources) {
                byte[] original = readResource(resource);
                Random random = new Random(resource.hashCode());

                for (int i = 0; i < 25; i++) {
                    byte[] data;
                    String mutation;

                    if (i % 4 == 3) {
                        int length = 2 + random.nextInt(original.length - 2);
                        data = Arrays.copyOf(original, length);
                        mutation = "truncated to " + length;
                    }
                    else {
                        data = original.clone();
                        StringBuilder positions = new StringBuilder("changed bytes @");

                        for (int k = random.nextInt(4); k >= 0; k--) {
                            // Keep the signature, so the reader is still selected
                            int pos = 2 + random.nextInt(data.length - 2);
                            data[pos] = (byte) random.nextInt(256);
                            positions.append(' ').append(pos);
                        }

                        mutation = positions.toString();
                    }

                    String description = resource + ", " + mutation;
                    Future<Throwable> result = executor.submit(() -> readCompletely(data));

                    Throwable thrown;
                    try {
                        thrown = result.get(20, TimeUnit.SECONDS);
                    }
                    catch (TimeoutException e) {
                        fail("Timeout (hang?): " + description);
                        return;
                    }

                    if (thrown != null && !(thrown instanceof IOException)) {
                        throw new AssertionError("Unexpected " + thrown + ": " + description, thrown);
                    }
                }
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    private Throwable readCompletely(final byte[] data) {
        JXLImageReader reader = createReaderUnchecked();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            reader.setInput(stream);
            reader.getWidth(0);
            reader.getImageTypes(0).next();
            reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            reader.read(0);

            return null;
        }
        catch (Throwable t) {
            return t;
        }
        finally {
            reader.dispose();
        }
    }

    private JXLImageReader createReaderUnchecked() {
        try {
            return createReader();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void testReadDefaultParamDoesNotCache() throws IOException {
        JXLImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/jxl/gray8.jxl"))) {
            reader.setInput(stream);

            // Default reads hand out the decoded image itself, each read must return a new image
            BufferedImage first = reader.read(0);
            BufferedImage second = reader.read(0, reader.getDefaultReadParam());
            assertNotSame(first, second);

            // Modifying the first doesn't affect later reads
            first.getRaster().setSample(0, 0, 0, 255);
            assertSamples(second, (x, y) -> new int[] {gray8(x, y)}, 0);

            // Reading with a source region still works (decodes and caches again)
            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new java.awt.Rectangle(10, 5, 20, 10));
            BufferedImage region = reader.read(0, param);
            assertSamples(region, (x, y) -> new int[] {gray8(x + 10, y + 5)}, 0);
        }
        finally {
            reader.dispose();
        }
    }

    private byte[] readResource(final String resource) throws IOException {
        try (InputStream input = getClassLoaderResource(resource).openStream()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) > 0) {
                bytes.write(buffer, 0, read);
            }

            return bytes.toByteArray();
        }
    }
}
