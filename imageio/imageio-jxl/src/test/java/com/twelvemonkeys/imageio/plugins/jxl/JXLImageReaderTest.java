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
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

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
                new TestData(getClassLoaderResource("/jxl/patches-lossless.jxl"), new Dimension(1600, 1096))
        );
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
    public void testReadModularSameAsReference() throws IOException {
        assertSameAsReference("art");
    }

    @Test
    public void testReadVarDCTSameAsReference() throws IOException {
        assertSameAsReference("white");
    }

    @Test
    public void testReadRGBRawType() throws IOException {
        BufferedImage image = read("/jxl/lenna.jxl", null);

        assertEquals(BufferedImage.TYPE_3BYTE_BGR, image.getType());
        assertTrue(image.getColorModel().getColorSpace().isCS_sRGB());
    }

    @Test
    public void testReadAlpha() throws IOException {
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
    public void testReadTruncated() throws IOException {
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
    public void testReadCorrupt() throws IOException {
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
