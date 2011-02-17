package com.twelvemonkeys.image;

import org.junit.Test;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageProducer;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * BufferedImageFactoryTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedImageFactoryTestCase.java,v 1.0 May 7, 2010 12:40:08 PM haraldk Exp$
 */
public class BufferedImageFactoryTestCase {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullImage() {
        new BufferedImageFactory((Image) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullProducer() {
        new BufferedImageFactory((ImageProducer) null);
    }

    // Image source = Toolkit.getDefaultToolkit().createImage((byte[]) null);      // - NPE in Toolkit, ok

    @Test(timeout = 1000, expected = IllegalArgumentException.class)
    public void testGetBufferedImageErrorSourceIP() {
        Image source = Toolkit.getDefaultToolkit().createImage((ImageProducer) null);

        new BufferedImageFactory(source);
    }

    // TODO: This is a quite serious bug, but it can be argued that the bug is in the
    // Toolkit, allowing such images in the first place... In any case, there's
    // not much we can do, except until someone is bored and kills the app... :-P
/*
    @Test(timeout = 1000, expected = ImageConversionException.class)
    public void testGetBufferedImageErrorSourceString() {
        Image source = Toolkit.getDefaultToolkit().createImage((String) null);

        BufferedImageFactory factory = new BufferedImageFactory(source);
        factory.getBufferedImage();
    }
*/

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

    // TODO: Test a GIF or PNG with PLTE chunk, and make sure we get an IndexColorModel

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
    public void testListener() {
        URL resource = getClass().getResource("/sunflower.jpg");
        assertNotNull(resource);
        Image source = Toolkit.getDefaultToolkit().createImage(resource);
        assertNotNull(source);

        BufferedImageFactory factory = new BufferedImageFactory(source);

        VerifyingListener listener = new VerifyingListener(factory);
        factory.addProgressListener(listener);
        factory.getBufferedImage();

        listener.verify();
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


        public void verify() {
            assertEquals(100f, progress, .1f); // Sanity test that the listener was invoked
        }
    }
}
