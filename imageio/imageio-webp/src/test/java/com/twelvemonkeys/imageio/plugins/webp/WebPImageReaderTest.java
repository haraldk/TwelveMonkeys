package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.List;

import static java.util.Arrays.asList;

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
                new TestData(getClassLoaderResource("/webp/very_short.webp"), new Dimension(63, 66))
                // TODO: Support lossless
//                 // Lossless
//                 new TestData(getClassLoaderResource("/webp/1_webp_ll.webp"), new Dimension(400, 301)),
//                 // Extended format: Alpha + VP8
//                 new TestData(getClassLoaderResource("/webp/1_webp_a.webp"), new Dimension(400, 301)),
//                 // Extendad format: Anim
//                new TestData(getClassLoaderResource("/webp/animated-webp-supported.webp"), new Dimension(400, 400))
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
}
