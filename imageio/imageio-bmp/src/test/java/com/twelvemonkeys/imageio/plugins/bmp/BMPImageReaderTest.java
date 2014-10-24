package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * BMPImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BMPImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class BMPImageReaderTest extends ImageReaderAbstractTestCase<BMPImageReader> {
    protected List<TestData> getTestData() {
        return Arrays.asList(
                // BMP Suite "Good"
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1bg.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1wb.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal4.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal4rle.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8-0.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8nonsquare.bmp"), new Dimension(127, 32)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8os2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8rle.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8topdown.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8v4.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8v5.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w124.bmp"), new Dimension(124, 61)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w125.bmp"), new Dimension(125, 62)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w126.bmp"), new Dimension(126, 63)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16-565.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16-565pal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb24.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb24pal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb32.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb32bf.bmp"), new Dimension(127, 64)),

                // BMP Suite "Questionable"
                new TestData(getClassLoaderResource("/bmpsuite/q/pal1p1.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal4rletrns.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8offs.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2sp.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2v2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2v2-16.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8oversizepal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8rletrns.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb16-231.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba16-4444.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24jpeg.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24largepal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24lprof.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24png.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24prof.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb32-111110.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb32fakealpha.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba32abf.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba32.bmp"), new Dimension(127, 64)),

                // OS/2 samples
                new TestData(getClassLoaderResource("/os2/money-2-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-16-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-256-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-24bit-os2.bmp"), new Dimension(455, 341)),

                // Vaious other samples
                new TestData(getClassLoaderResource("/bmp/Blue Lace 16.bmp"), new Dimension(48, 48)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_mono.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8-IM.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_gray.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask444.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask555.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask565.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_24.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888_reversed.bmp"), new Dimension(301, 331))
        );
    }

    protected ImageReaderSpi createProvider() {
        return new BMPImageReaderSpi();
    }

    @Override
    protected BMPImageReader createReader() {
        return new BMPImageReader(createProvider());
    }

    protected Class<BMPImageReader> getReaderClass() {
        return BMPImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("bmp");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("bmp", "rle");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/bmp");
    }

    @Override
    @Test
    public void testGetTypeSpecifiers() throws IOException {
        final ImageReader reader = createReader();
        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            ImageTypeSpecifier rawType = reader.getRawImageType(0);

            // As the JPEGImageReader we delegate to returns null for YCbCr, we'll have to do the same
            if (rawType == null && data.getInput().toString().contains("jpeg")) {
                continue;
            }
            assertNotNull(rawType);

            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertNotNull(types);
            assertTrue(types.hasNext());

            // TODO: This might fail even though the specifiers are obviously equal, if the
            // color spaces they use are not the SAME instance, as ColorSpace uses identity equals
            // and Interleaved ImageTypeSpecifiers are only equal if color spaces are equal...
            boolean rawFound = false;
            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();
                if (type.equals(rawType)) {
                    rawFound = true;
                    break;
                }
            }

            assertTrue("ImageTypeSepcifier from getRawImageType should be in the iterator from getImageTypes", rawFound);
        }
    }

}