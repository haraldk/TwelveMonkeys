package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WebPImageReaderTest
 */
public class WebPImageReaderTest extends ImageReaderAbstractTest<WebPImageReader> {

    @Override
    protected List<TestData> getTestData() {
        return asList(
                // Original Google WebP sample files
                new TestData(getClassLoaderResource("/webp/1.webp"), new Dimension(550, 368)),
                new TestData(getClassLoaderResource("/webp/5.webp"), new Dimension(1024, 752)),
                // Various samples from javavp8codec project
                new TestData(getClassLoaderResource("/webp/bug3.webp"), new Dimension(95, 95)),
                new TestData(getClassLoaderResource("/webp/segment01.webp"), new Dimension(160, 160)),
                new TestData(getClassLoaderResource("/webp/segment02.webp"), new Dimension(160, 160)),
                new TestData(getClassLoaderResource("/webp/segment03.webp"), new Dimension(160, 160)),
                new TestData(getClassLoaderResource("/webp/small_1x1.webp"), new Dimension(1, 1)),
                new TestData(getClassLoaderResource("/webp/small_1x13.webp"), new Dimension(1, 13)),
                new TestData(getClassLoaderResource("/webp/small_13x1.webp"), new Dimension(13, 1)),
                new TestData(getClassLoaderResource("/webp/small_31x13.webp"), new Dimension(31, 13)),
                new TestData(getClassLoaderResource("/webp/test.webp"), new Dimension(128, 128)),
                new TestData(getClassLoaderResource("/webp/very_short.webp"), new Dimension(63, 66)),
                // Lossless
                new TestData(getClassLoaderResource("/webp/1_webp_ll.webp"), new Dimension(400, 301)),
                new TestData(getClassLoaderResource("/webp/2_webp_ll.webp"), new Dimension(386, 395)),
                new TestData(getClassLoaderResource("/webp/2_webp_ll_alt.webp"), new Dimension(386, 395)),
                new TestData(getClassLoaderResource("/webp/3_webp_ll.webp"), new Dimension(800, 600)),
                new TestData(getClassLoaderResource("/webp/4_webp_ll.webp"), new Dimension(421, 163)),
                new TestData(getClassLoaderResource("/webp/5_webp_ll.webp"), new Dimension(300, 300)),
                // Extended format: Alpha + VP8
                new TestData(getClassLoaderResource("/webp/1_webp_a.webp"), new Dimension(400, 301)),
                new TestData(getClassLoaderResource("/webp/2_webp_a.webp"), new Dimension(386, 395)),
                new TestData(getClassLoaderResource("/webp/3_webp_a.webp"), new Dimension(800, 600)),
                new TestData(getClassLoaderResource("/webp/4_webp_a.webp"), new Dimension(421, 163)),
                new TestData(getClassLoaderResource("/webp/5_webp_a.webp"), new Dimension(300, 300)),
                // Extended format: Anim
                new TestData(getClassLoaderResource("/webp/animated-webp-supported.webp"), new Dimension(400, 400),
                        new Dimension(400, 400), new Dimension(400, 400), new Dimension(400, 394),
                        new Dimension(371, 394), new Dimension(394, 382), new Dimension(400, 388),
                        new Dimension(394, 383), new Dimension(394, 394), new Dimension(372, 394),
                        new Dimension(400, 400), new Dimension(320, 382)),
                // Alpha transparency and Alpha filtering
                new TestData(getClassLoaderResource("/webp/alpha_filter.webp"), new Dimension(1600, 1600)),
                // Lossy with grayscale ICC profile
                new TestData(getClassLoaderResource("/webp/incompatible-icc-gray.webp"), new Dimension(766, 1100))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new WebPImageReaderSpi();
    }

    @Override
    protected List<String> getFormatNames() {
        return asList("webp", "WEBP");
    }

    @Override
    protected List<String> getSuffixes() {
        return asList("wbp", "webp");
    }

    @Override
    protected List<String> getMIMETypes() {
        return asList("image/webp", "image/x-webp");
    }

    @Test
    public void testReadAndApplyICCProfile() throws IOException {
        WebPImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/webp/photo-iccp-adobergb.webp"))) {
            reader.setInput(stream);

            // We'll read a small portion of the image into a destination type that use sRGB
            ImageReadParam param = new ImageReadParam();
            param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
            param.setSourceRegion(new Rectangle(20, 20));

            BufferedImage image = reader.read(0, param);
            assertRGBEquals("RGB values differ, incorrect ICC profile or conversion?", 0xFFEA9600, image.getRGB(10, 10), 8);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testRec601ColorConversion() throws IOException {
        WebPImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/webp/blue_tile.webp"))) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0, null);
            assertRGBEquals("RGB values differ, incorrect Y'CbCr -> RGB conversion", 0xFF72AED5, image.getRGB(80, 80), 1);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadFromUnknownStreamLength() throws IOException {
        // See #672, image was not decoded and returned all black, when the stream length was unknown (-1).
        WebPImageReader reader = createReader();

        try (ImageInputStream stream = new MemoryCacheImageInputStream(getClassLoaderResource("/webp/photo-iccp-adobergb.webp").openStream()) {
            @Override public long length() {
                return -1;
            }
        }) {
            reader.setInput(stream);

            // We'll read a small portion of the image into a destination type that use sRGB
            ImageReadParam param = new ImageReadParam();
            param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
            param.setSourceRegion(new Rectangle(10, 10, 20, 20));

            BufferedImage image = reader.read(0, param);
            assertRGBEquals("RGB values differ, image all black?", 0xFFEC9800, image.getRGB(5, 5), 8);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadAlphaTransparent() throws IOException {
        WebPImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/webp/1_webp_a.webp"))) {
            reader.setInput(stream);
            BufferedImage image = reader.read(0);

            assertEquals(Transparency.TRANSLUCENT, image.getTransparency());

            assertRGBEquals("Expected transparent corner (0, 0)", 0x00000000, image.getRGB(0, 0) & 0xFF000000, 8);
            assertRGBEquals("Expected opaque center (200, 150)", 0xff9a4e01, image.getRGB(200, 150), 8);
            assertRGBEquals("Expected transparent corner (399, 0)", 0x00000000, image.getRGB(399, 0) & 0xFF000000, 8);
            assertRGBEquals("Expected transparent corner (0, 300)", 0x00000000, image.getRGB(0, 300) & 0xFF000000, 8);
            assertRGBEquals("Expected transparent corner (399, 300)", 0x00000000, image.getRGB(399, 300) & 0xFF000000, 8);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testAlphaSubsampling() throws IOException {
        WebPImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/webp/alpha_filter.webp"))) {
            reader.setInput(stream);

            // Read the image using a subsampling factor of 2 
            ImageReadParam param = new ImageReadParam();
            param.setSourceSubsampling(2, 2, 0, 0);

            BufferedImage image = reader.read(0, param);

            assertRGBEquals("Expected transparent at (100, 265)", 0x00000000, image.getRGB(100, 265) & 0xFF000000, 8);
            assertRGBEquals("Expected transparent at (512, 320)", 0x00000000, image.getRGB(512, 320) & 0xFF000000, 8);
            assertRGBEquals("Expected opaque at (666, 444)", 0xFF000000, image.getRGB(666, 444) & 0xFF000000, 8);
            assertRGBEquals("Expected opaque corner (799, 799)", 0xFF000000, image.getRGB(699, 699) & 0xFF000000, 8);
        }
        finally {
            reader.dispose();
        }
    }
}
