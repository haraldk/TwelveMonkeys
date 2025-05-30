/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStreamSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.twelvemonkeys.imageio.plugins.pict.PICTImageReaderSpi.isOtherFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ICOImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class PICTImageReaderTest extends ImageReaderAbstractTest<PICTImageReader> {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new ByteArrayImageInputStreamSpi());
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new PICTImageReaderSpi();
    }

    // TODO: Should also test the clipboard format (without 512 byte header)
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/pict/test.pct"), new Dimension(300, 200)),
                new TestData(getClassLoaderResource("/pict/food.pct"), new Dimension(146, 194)),
                new TestData(getClassLoaderResource("/pict/carte.pict"), new Dimension(782, 598)),
                // Embedded QuickTime image... Should at least include the embedded fallback text
                new TestData(getClassLoaderResource("/pict/u2.pict"), new Dimension(160, 159)),
                // Obsolete V2 format with weird header
                new TestData(getClassLoaderResource("/pict/FLAG_B24.PCT"), new Dimension(124, 124)),
                // PixMap
                new TestData(getClassLoaderResource("/pict/FC10.PCT"), new Dimension(2265, 2593)),
                // 1000 DPI with bounding box not matching DPI
                new TestData(getClassLoaderResource("/pict/oom.pict"), new Dimension(1713, 1263)),
                new TestData(getClassLoaderResource("/pict/cow.pict"), new Dimension(787, 548)),
                new TestData(getClassLoaderResource("/pict/CatDV==2.0=1=.pict"), new Dimension(375, 165)),
                new TestData(getClassLoaderResource("/pict/Picture14.pict"), new Dimension(404, 136)),
                new TestData(getClassLoaderResource("/pict/J19.pict"), new Dimension(640, 480)),

                // Sample data from http://developer.apple.com/documentation/mac/QuickDraw/QuickDraw-458.html
                new TestData(DATA_V1, new Dimension(168, 108)),
                new TestData(DATA_V2, new Dimension(168, 108)),
                new TestData(DATA_EXT_V2, new Dimension(168, 108)),

                // Examples from http://developer.apple.com/technotes/qd/qd_14.html
                new TestData(DATA_V1_COPY_BITS, new Dimension(100, 165)),
                new TestData(DATA_V1_OVAL_RECT, new Dimension(100, 165)),
                new TestData(DATA_V1_OVERPAINTED_ARC, new Dimension(100, 165))
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Collections.singletonList("pict");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("pct", "pict");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/pict", "image/x-pict");
    }

    @Test
    public void testQTSliceFallback() throws IOException {
        PICTImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/pict/J19.pict"))) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0, null);
            assertRGBEquals("RGB values differ", 0xff5e6e47, image.getRGB(0, 240), 1);    // black when it fails
            assertRGBEquals("RGB values differ", 0xff312c28, image.getRGB(320, 240), 1);  // white when it fails
            assertRGBEquals("RGB values differ", 0xff5d5059, image.getRGB(320, 10), 1);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testIsOtherFormat() throws IOException {
        assertFalse(isOtherFormat(new ByteArrayImageInputStream(new byte[8])));

        // BMP
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'B', 'M', 0, 0, 0, 0, 0, 0})));

        // GIF
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'G', 'I', 'F', '8', '7', 'a', 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'G', 'I', 'F', '8', '9', 'a', 0, 0})));

        // JPEG (JFIF, EXIF, "raw")
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0, 0, 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB, 0, 0, 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xC4, 0, 0, 0, 0})));

        // PNG
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})));

        // PSD
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'8', 'B', 'P', 'S', 0, 0, 0, 0})));

        // TIFF (Little Endian, Big Endian)
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'I', 'I', 42, 0, 0, 0, 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'M', 'M', 0, 42, 0, 0, 0, 0})));

        // BigTIFF (Little Endian, Big Endian)
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'I', 'I', 43, 0, 0, 0, 0, 0})));
        assertTrue(isOtherFormat(new ByteArrayImageInputStream(new byte[] {'M', 'M', 0, 43, 0, 0, 0, 0})));
    }

    @Disabled("Known issue")
    @Test
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }

    @Test
    public void testProviderShouldNotConsumeExistingMarks() throws IOException {
        try (ImageInputStream stream = new ByteArrayImageInputStream(new byte[1024])) {
            stream.seek(42);
            stream.mark();
            stream.seek(123);

            ((ImageReaderSpi) new PICTImageReaderSpi()).canDecodeInput(stream);

            assertEquals(123, stream.getStreamPosition());
            stream.reset();
            assertEquals(42, stream.getStreamPosition());
        }
    }

    // Regression tests

    @Test
    public void testProviderNotMatchJPEG() throws IOException {
        // This JPEG contains PICT magic bytes at locations a PICT would normally have them.
        // We should not claim to be able read it.
        assertFalse(((ImageReaderSpi) new PICTImageReaderSpi()).canDecodeInput(
                new TestData(getClassLoaderResource("/jpeg/R-7439-1151526181.jpeg"),
                new Dimension(386, 396)
        )));
        assertFalse(((ImageReaderSpi) new PICTImageReaderSpi()).canDecodeInput(
                new TestData(getClassLoaderResource("/jpeg/89497426-adc19a00-d7ff-11ea-8ad1-0cbcd727b62a.jpeg"),
                new Dimension(640, 480)
        )));
    }

    @Test
    public void testDataExtV2() throws IOException {
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_EXT_V2));
        reader.read(0);
    }

    @Test
    public void testDataV2() throws IOException {
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_V2));
        reader.read(0);
    }

    @Test
    public void testDataV1() throws IOException {
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_V1));
        reader.read(0);
    }

    @Test
    public void testDataV1OvalRect() throws IOException {
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_V1_OVAL_RECT));
        reader.read(0);
    }

    @Test
    public void testDataV1OverpaintedArc() throws IOException, InterruptedException {
        // TODO: Doesn't look right
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_V1_OVERPAINTED_ARC));
        reader.read(0);
        BufferedImage image = reader.read(0);

        if (!GraphicsEnvironment.isHeadless()) {
            PICTImageReader.showIt(image, "dataV1CopyBits");
            Thread.sleep(10000);
        }
    }

    @Test
    public void testDataV1CopyBits() throws IOException {
        PICTImageReader reader = createReader();
        reader.setInput(new ByteArrayImageInputStream(DATA_V1_COPY_BITS));
        reader.read(0);
    }

    @Test
    public void testFrameBoundsIssue() throws IOException {
        PICTImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/pict/cow.pict"))) {
            reader.setInput(stream);
            // This file is from Apache POI test data. It should render as a black silhouette of a cow on a transparent
            // background. Without the fix the image is completely transparent.
            BufferedImage image = reader.read(0, null);
            assertRGBEquals("RGB values differ", 0xff000000, image.getRGB(100, 100), 1);    // was transparent 00ffffff
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testBoundsIssue() throws IOException {
        PICTImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/pict/Picture14.pict"))) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0, null);
            assertRGBEquals("RGB values differ", 0xffcccccc, image.getRGB(4, 4), 1);    // was transparent 00ffffff
            assertRGBEquals("RGB values differ", 0xffcccccc, image.getRGB(5, 118), 1);  // was red ffcc6666
            assertRGBEquals("RGB values differ", 0xffcc6666, image.getRGB(28, 60), 1);  // was grey ffcccccc
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testQTMaskBytesSkipped() throws IOException {
        assumeTrue(ImageIO.getImageReadersByFormatName("TIFF").hasNext(), "No TIFF plugin available, skipping test");
        
        PICTImageReader reader = createReader();
        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/pict/P30946BDC.pict"))) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0, null);
            assertRGBEquals("RGB values differ", 0xfff3f3f3, image.getRGB(0, 0), 1);
            assertRGBEquals("RGB values differ", 0xffe1e1e1, image.getRGB(80, 80), 1);
            assertRGBEquals("RGB values differ", 0xffe1e1e1, image.getRGB(290, 266), 1);
        }
        finally {
            reader.dispose();
        }
    }

    private static final byte[] DATA_EXT_V2 = {
            0x00, 0x78, /* picture size; don't use this value for picture size */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x6C, 0x00, (byte) 0xA8, /* bounding rectangle of picture at 72 dpi */
            0x00, 0x11, /* VersionOp opcode; always $0011 for extended version 2 */
            0x02, (byte) 0xFF, /* Version opcode; always $02FF for extended version 2 */
            0x0C, 0x00, /* HeaderOp opcode; always $0C00 for extended version 2 */
            /* next 24 bytes contain header information */
            (byte) 0xFF, (byte) 0xFE, /* version; always -2 for extended version 2 */
            0x00, 0x00, /* reserved */
            0x00, 0x48, 0x00, 0x00, /* best horizontal resolution: 72 dpi */
            0x00, 0x48, 0x00, 0x00, /* best vertical resolution: 72 dpi */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* optimal source rectangle for 72 dpi horizontal
                              and 72 dpi vertical resolutions */
            0x00, 0x00, /* reserved */
            0x00, 0x1E, /* DefHilite opcode to use default hilite color */
            0x00, 0x01, /* Clip opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for clipping region */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x00, 0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x00, 0x5C, /* fillSameOval opcode */
            0x00, 0x08, /* PnMode opcode */
            0x00, 0x08, /* pen mode data */
            0x00, 0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            0x00, (byte) 0xFF, /* OpEndPic opcode; end of picture */
    };

    private static final byte[] DATA_V2 = {
            0x00, 0x78, /* picture size; don't use this value for picture size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle of picture */
            0x00, 0x11, /* VersionOp opcode; always $0x00, 0x11, for version 2 */
            0x02, (byte) 0xFF, /* Version opcode; always $0x02, 0xFF, for version 2 */
            0x0C, 0x00, /* HeaderOp opcode; always $0C00 for version 2 */
            /* next 24 bytes contain header information */
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /* version; always -1 (long) for version 2 */
            0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, (byte) 0xAA, 0x00, 0x00, 0x00, 0x6E, 0x00, 0x00, /* fixed-point bounding
                                                   rectangle for picture */
            0x00, 0x00, 0x00, 0x00, /* reserved */
            0x00, 0x1E, /* DefHilite opcode to use default hilite color */
            0x00, 0x01, /* Clip opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for clipping region */
            0x00, 0x0A, /* FillPat opcode; fill pattern specifed in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x00, 0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x00, 0x5C, /* fillSameOval opcode */
            0x00, 0x08, /* PnMode opcode */
            0x00, 0x08, /* pen mode data */
            0x00, 0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            0x00, (byte) 0xFF, /* OpEndPic opcode; end of picture */
    };

    private static final byte[] DATA_V1 = {
            0x00, 0x4F, /* picture size; this value is reliable for version 1 pictures */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle of picture */
            0x11, /* picVersion opcode for version 1 */
            0x01, /* version number 1 */
            0x01, /* ClipRgn opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for region */
            0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x5C, /* fillSameOval opcode */
            0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            (byte) 0xFF, /* EndOfPicture opcode; end of picture */
    };

    private static final byte[] DATA_V1_OVAL_RECT = {
            0x00, 0x26, /*size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x0B, 0x00, 0x04, 0x00, 0x05, /* ovSize point */
            0x40, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* frameRRect rectangle */
            (byte) 0xFF, /* fin */
    };

    private static final byte[] DATA_V1_OVERPAINTED_ARC = {
            0x00, 0x36, /* size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x61, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, 0x00, 0x03, 0x00, 0x2D, /* paintArc rectangle,startangle,endangle */
            0x08, 0x00, 0x0A, /* pnMode patXor -- note that the pnMode comes before the pnPat */
            0x09, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, /* pnPat gray */
            0x69, 0x00, 0x03, 0x00, 0x2D, /* paintSameArc startangle,endangle */
            (byte) 0xFF, /* fin */
    };

    private static final byte[] DATA_V1_COPY_BITS = {
            0x00, 0x48, /* size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x31, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* paintRect rectangle */
            (byte) 0x90, 0x00, 0x02, 0x00, 0x0A, 0x00, 0x14, 0x00, 0x0F, 0x00, 0x1C, /* BitsRect rowbytes bounds (note that bounds is wider than smallr) */
            0x00, 0x0A, 0x00, 0x14, 0x00, 0x0F, 0x00, 0x19, /* srcRect */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x14, 0x00, 0x1E, /* dstRect */
            0x00, 0x06, /* mode=notSrcXor */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 5 rows of empty bitmap (we copied from a still-blank window) */
            (byte) 0xFF, /* fin */
    };
}
