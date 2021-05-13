package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Collections;
import java.util.List;

/**
 * BMPImageWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by : harald.kuhr$
 * @version : BMPImageWriterTest.java,v 1.0 25/06/2020 harald.kuhr Exp$
 */
public class BMPImageWriterTest extends ImageWriterAbstractTest<BMPImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new BMPImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Collections.singletonList(
                new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
        );
    }
}