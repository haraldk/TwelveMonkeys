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

package com.twelvemonkeys.image;

import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * BufferedImageFactoryTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedImageFactoryTestCase.java,v 1.0 May 7, 2010 12:40:08 PM haraldk Exp$
 */
public class BufferedImageFactoryTest {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullImage() {
        new BufferedImageFactory((Image) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullProducer() {
        new BufferedImageFactory((ImageProducer) null);
    }

    // NPE in Toolkit, ok
    @Test(expected = RuntimeException.class)
    public void testGetBufferedImageErrorSourceByteArray() {
        Image source = Toolkit.getDefaultToolkit().createImage((byte[]) null);

        new BufferedImageFactory(source);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBufferedImageErrorSourceImageProducer() {
        Image source = Toolkit.getDefaultToolkit().createImage((ImageProducer) null);

        new BufferedImageFactory(source);
    }

    // TODO: This is a quite serious bug, however, the bug is in the Toolkit, allowing such images in the first place...
    // In any case, there's not much we can do, except until someone is bored and kills the app/thread... :-P
    @Ignore("Bug in Toolkit")
    @Test(timeout = 1000, expected = ImageConversionException.class)
    public void testGetBufferedImageErrorSourceString() {
        Image source = Toolkit.getDefaultToolkit().createImage((String) null);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        factory.getBufferedImage();
    }

    // This is a little random, and it would be nicer if we could throw an IllegalArgumentException on create.
    // Unfortunately, the API doesn't allow this...
    @Test(timeout = 1000, expected = ImageConversionException.class)
    public void testGetBufferedImageErrorSourceURL() {
        Image source = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/META-INF/MANIFEST.MF"));

        BufferedImageFactory factory = new BufferedImageFactory(source);
        factory.getBufferedImage();
    }

    @Test
    public void testGetBufferedImageJPEG() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        BufferedImage image = factory.getBufferedImage();

        assertEquals(187, image.getWidth());
        assertEquals(283, image.getHeight());
    }

    @Test
    public void testGetColorModelJPEG() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        ColorModel colorModel = factory.getColorModel();

        assertNotNull(colorModel);
        assertEquals(3, colorModel.getNumColorComponents()); // getNumComponents may include alpha, we don't care
        assertEquals(ColorSpace.getInstance(ColorSpace.CS_sRGB), colorModel.getColorSpace());

        for (int i = 0; i < colorModel.getNumComponents(); i++) {
            assertEquals(8, colorModel.getComponentSize(i));
        }
    }

    @Test
    public void testGetBufferedImageGIF() {
        URL resource = getClass().getResource("/tux.gif");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        BufferedImage image = factory.getBufferedImage();

        assertEquals(250, image.getWidth());
        assertEquals(250, image.getHeight());
        
        assertEquals(Transparency.BITMASK, image.getTransparency());

        // All corners of image should be fully transparent
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertEquals(0, image.getRGB(249, 0) >>> 24);
        assertEquals(0, image.getRGB(0, 249) >>> 24);
        assertEquals(0, image.getRGB(249, 249) >>> 24);
    }

    @Test
    public void testGetColorModelGIF() {
        URL resource = getClass().getResource("/tux.gif");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        ColorModel colorModel = factory.getColorModel();

        assertNotNull(colorModel);

        assertEquals(3, colorModel.getNumColorComponents());
        assertEquals(ColorSpace.getInstance(ColorSpace.CS_sRGB), colorModel.getColorSpace());
        assertTrue(colorModel instanceof IndexColorModel);

        assertTrue(colorModel.hasAlpha());
        assertEquals(4, colorModel.getNumComponents());
        assertTrue(((IndexColorModel) colorModel).getTransparentPixel() >= 0);
        assertEquals(Transparency.BITMASK, colorModel.getTransparency());

        for (int i = 0; i < colorModel.getNumComponents(); i++) {
            assertEquals(8, colorModel.getComponentSize(i));
        }
    }

    @Test
    public void testGetBufferedImageSubsampled() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        BufferedImage original = factory.getBufferedImage();

        factory.setSourceSubsampling(2, 2);
        BufferedImage image = factory.getBufferedImage(); // Accidentally also tests reuse...

        // Values rounded up
        assertEquals(94, image.getWidth());
        assertEquals(142, image.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals("RGB[" + x + ", " + y + "]", original.getRGB(x * 2, y * 2), image.getRGB(x, y));
            }
        }
    }

    @Test
    public void testGetBufferedImageSourceRegion() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        BufferedImage original = factory.getBufferedImage();

        factory.setSourceRegion(new Rectangle(40, 40, 40, 40));
        BufferedImage image = factory.getBufferedImage(); // Accidentally also tests reuse...

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals("RGB[" + x + ", " + y + "]", original.getRGB(40 + x, 40 + y), image.getRGB(x, y));
            }
        }
    }

    @Test
    public void testGetBufferedImageSubsampledSourceRegion() throws Exception{
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        BufferedImage original = factory.getBufferedImage();

        factory.setSourceRegion(new Rectangle(40, 40, 40, 40));
        factory.setSourceSubsampling(2, 2);
        BufferedImage image = factory.getBufferedImage(); // Accidentally also tests reuse...

        assertEquals(20, image.getWidth());
        assertEquals(20, image.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals("RGB[" + x + ", " + y + "]", original.getRGB(40 + x * 2, 40 + y * 2), image.getRGB(x, y));
            }
        }
    }

    @Test
    public void testAbort() throws Exception {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        final BufferedImageFactory factory = new BufferedImageFactory(source);

        // Listener should abort ASAP
        factory.addProgressListener(new BufferedImageFactory.ProgressListener() {
            public void progress(BufferedImageFactory pFactory, float pPercentage) {
                if (pPercentage > 5) {
                    pFactory.abort();
                }
            }
        });

        BufferedImage image = factory.getBufferedImage();

        assertEquals(187, image.getWidth());
        assertEquals(283, image.getHeight());

        // Upper right should be loaded
        assertEquals((image.getRGB(186, 0) & 0xFF0000) >> 16 , 0x68, 10);
        assertEquals((image.getRGB(186, 0) & 0xFF00) >> 8, 0x91, 10);
        assertEquals(image.getRGB(186, 0) & 0xFF, 0xE0, 10);

        // Lower right should be blank
        assertEquals(image.getRGB(186, 282) & 0xFFFFFF, 0);
    }

    @Test
    public void testListener() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.getBufferedImage();

        listener.verify(100f);
    }

    @Test
    public void testRemoveListener() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.removeProgressListener(listener);
        factory.getBufferedImage();

        listener.verify(0);
    }

    @Test
    public void testRemoveNullListener() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.removeProgressListener(null);
        factory.getBufferedImage();

        listener.verify(100);
    }

    @Test
    public void testRemoveNotAdddedListener() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.removeProgressListener(new BufferedImageFactory.ProgressListener() {
            public void progress(BufferedImageFactory pFactory, float pPercentage) {
            }
        });
        factory.getBufferedImage();

        listener.verify(100);
    }

    @Test
    public void testRemoveAllListeners() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        VerifyingListener listener2 = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.addProgressListener(listener);
        factory.addProgressListener(listener2);
        factory.removeAllProgressListeners();
        factory.getBufferedImage();

        listener.verify(0);
        listener2.verify(0);
    }

    private static class VerifyingListener implements BufferedImageFactory.ProgressListener {
        private final BufferedImageFactory factory;
        private float progress;

        public VerifyingListener(BufferedImageFactory factory) {
            this.factory = factory;
        }

        public void progress(BufferedImageFactory pFactory, float pPercentage) {
            assertEquals(factory, pFactory);
            assertTrue(pPercentage >= progress && pPercentage <= 100f);

            progress = pPercentage;
        }


        public void verify(final float expectedProgress) {
            assertEquals(expectedProgress, progress, .1f); // Sanity test that the listener was invoked
        }
    }
}
