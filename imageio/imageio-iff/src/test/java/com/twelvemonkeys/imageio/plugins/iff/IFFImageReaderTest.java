/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * IFFImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IFFImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class IFFImageReaderTest extends ImageReaderAbstractTestCase<IFFImageReader> {
    protected List<TestData> getTestData() {
        return Arrays.asList(
                // 32 bit - Ok
                new TestData(getClassLoaderResource("/iff/test.iff"), new Dimension(300, 200)),
                // 24 bit - Ok
                new TestData(getClassLoaderResource("/iff/survivor.iff"), new Dimension(800, 600)),
                // HAM6 - Ok (a lot of visual "fringe", would be interesting to see on a real HAM display)
                new TestData(getClassLoaderResource("/iff/A4000T_HAM6.IFF"), new Dimension(320, 512)),
                // HAM8 - Ok
                new TestData(getClassLoaderResource("/iff/A4000T_HAM8.IFF"), new Dimension(628, 512)),
                // 2 color indexed - Ok
                new TestData(getClassLoaderResource("/iff/owl.iff"), new Dimension(160, 174)),
                // 8 color indexed - Ok
                new TestData(getClassLoaderResource("/iff/AmigaAmiga.iff"), new Dimension(200, 150)),
                // 16 color indexed - Ok
                new TestData(getClassLoaderResource("/iff/Lion.iff"), new Dimension(704, 480)),
                // 32 color indexed - Ok
                new TestData(getClassLoaderResource("/iff/GoldPorsche.iff"), new Dimension(320, 200)),
                // 64 color indexed EHB - Ok
                new TestData(getClassLoaderResource("/iff/Bryce.iff"), new Dimension(320, 200)),
                // 256 color indexed - Ok
                new TestData(getClassLoaderResource("/iff/IKKEGOD.iff"), new Dimension(640, 256)),
                // PBM, indexed - Ok
                new TestData(getClassLoaderResource("/iff/ASH.PBM"), new Dimension(320, 240)),
                // 16 color indexed, multi palette (PCHG) - Ok
                new TestData(getClassLoaderResource("/iff/Manhattan.PCHG"), new Dimension(704, 440)),
                // 16 color indexed, multi palette (PCHG + SHAM) - Ok
                new TestData(getClassLoaderResource("/iff/Somnambulist-2.SHAM"), new Dimension(704, 440))
        );
    }

    protected ImageReaderSpi createProvider() {
        return new IFFImageReaderSpi();
    }

    protected Class<IFFImageReader> getReaderClass() {
        return IFFImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("iff");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("iff", "ilbm", "ham", "ham8", "lbm");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/iff", "image/x-iff");
    }

    // Regression tests

    @Test
    public void testEHBColors() throws IOException {
        IFFImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/iff/Bryce.iff")));

        BufferedImage image = reader.read(0);
        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, image.getType());

        ColorModel colorModel = image.getColorModel();
        assertNotNull(colorModel);
        assertTrue(colorModel instanceof IndexColorModel);

        IndexColorModel indexColorModel = (IndexColorModel) colorModel;

        assertEquals(64, indexColorModel.getMapSize());

        byte[] reds = new byte[indexColorModel.getMapSize()];
        indexColorModel.getReds(reds);
        byte[] blues = new byte[indexColorModel.getMapSize()];
        indexColorModel.getBlues(blues);
        byte[] greens = new byte[indexColorModel.getMapSize()];
        indexColorModel.getGreens(greens);

        for (int i = 0; i < 32; i++) {
            // Make sure the color model is really EHB
            assertEquals("red", (reds[i] & 0xff) / 2, reds[i + 32] & 0xff);
            assertEquals("blue", (blues[i] & 0xff) / 2, blues[i + 32] & 0xff);
            assertEquals("green", (greens[i] & 0xff) / 2, greens[i + 32] & 0xff);
        }
    }
}
