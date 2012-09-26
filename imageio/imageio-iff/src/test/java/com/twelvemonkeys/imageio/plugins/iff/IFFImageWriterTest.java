/*
 * Copyright (c) 2012, Harald Kuhr
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

import com.twelvemonkeys.image.MonochromeColorModel;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JPEG2000ImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEG2000ImageWriterTest.java,v 1.0 20.01.12 12:19 haraldk Exp$
 */
public class IFFImageWriterTest extends ImageWriterAbstractTestCase {
    private final IFFImageWriterSpi provider = new IFFImageWriterSpi();

    @Override
    protected ImageWriter createImageWriter() {
        return new IFFImageWriter(provider);
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(33, 20, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(31, 23, BufferedImage.TYPE_BYTE_INDEXED),
                new BufferedImage(30, 27, BufferedImage.TYPE_BYTE_BINARY),
                new BufferedImage(29, 29, BufferedImage.TYPE_BYTE_INDEXED, MonochromeColorModel.getInstance()),
                new BufferedImage(28, 31, BufferedImage.TYPE_INT_BGR),
                new BufferedImage(27, 33, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(24, 37, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(23, 41, BufferedImage.TYPE_4BYTE_ABGR)
        );
    }

    @Test
    public void testWriteReadCompare() throws IOException {
        ImageWriter writer = createImageWriter();

        List<? extends RenderedImage> testData = getTestData();

        for (int i = 0; i < testData.size(); i++) {
            try {
                RenderedImage image = testData.get(i);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ImageOutputStream stream = ImageIO.createImageOutputStream(buffer);
                writer.setOutput(stream);
    
                BufferedImage original = drawSomething((BufferedImage) image);
    
                try {
                    writer.write(original);
                }
                catch (IOException e) {
                    fail(e.getMessage());
                }
                finally {
                    stream.close(); // Force data to be written
                }
    
                assertTrue("No image data written", buffer.size() > 0);
    
                ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(buffer.toByteArray()));
                BufferedImage written = ImageIO.read(input);
    
                assertNotNull(written);
                assertEquals(original.getWidth(), written.getWidth());
                assertEquals(original.getHeight(), written.getHeight());
                assertSameType(original, written);
                assertSameData(original, written);
            }
            catch (IOException e) {
                AssertionError fail = new AssertionError("Failure writing test data " + i + " " + e);
                fail.initCause(e);
                throw fail;
            }
        }
    }

    private static void assertSameData(BufferedImage expected, BufferedImage actual) {
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int expectedRGB = expected.getRGB(x, y);
                int actualRGB = actual.getRGB(x, y);

                if (expected.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
                    // NOTE: For some reason, gray data seems to be one step off...
                    assertEquals("R(" + x + "," + y + ")", expectedRGB & 0xff0000, actualRGB & 0xff0000, 0x10000);
                    assertEquals("G(" + x + "," + y + ")", expectedRGB & 0x00ff00, actualRGB & 0x00ff00, 0x100);
                    assertEquals("B(" + x + "," + y + ")", expectedRGB & 0x0000ff, actualRGB & 0x0000ff, 0x1);
                }
                else {
                    assertEquals("R(" + x + "," + y + ")", expectedRGB & 0xff0000, actualRGB & 0xff0000);
                    assertEquals("G(" + x + "," + y + ")", expectedRGB & 0x00ff00, actualRGB & 0x00ff00);
                    assertEquals("B(" + x + "," + y + ")", expectedRGB & 0x0000ff, actualRGB & 0x0000ff);
                }
            }
        }
    }

    private static void assertSameType(BufferedImage expected, BufferedImage actual) {
        if (expected.getType() != actual.getType()) {
            if (expected.getType() == BufferedImage.TYPE_INT_RGB || expected.getType() == BufferedImage.TYPE_INT_BGR) {
                assertEquals(BufferedImage.TYPE_3BYTE_BGR, actual.getType());
            }
            else if (expected.getType() == BufferedImage.TYPE_INT_ARGB || expected.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
                assertEquals(BufferedImage.TYPE_4BYTE_ABGR, actual.getType());
            }
            else if (expected.getType() == BufferedImage.TYPE_BYTE_INDEXED && expected.getColorModel().getPixelSize() <= 16) {
                assertEquals(BufferedImage.TYPE_BYTE_BINARY, actual.getType());
            }

            // NOTE: Actually, TYPE_GRAY may be converted to TYPE_BYTE_INDEXED with linear gray color-map,
            // without being a problem (just a waste of time and space).
        }
    }
}
