package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

/**
 * ICOImageWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by : harald.kuhr$
 * @version : ICOImageWriterTest.java,v 1.0 25/06/2020 harald.kuhr Exp$
 */
public class ICOImageWriterTest extends ImageWriterAbstractTest<ICOImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new ICOImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(8, 8, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR)
        );
    }
}