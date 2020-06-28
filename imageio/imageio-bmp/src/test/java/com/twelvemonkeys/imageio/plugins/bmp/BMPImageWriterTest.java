package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

/**
 * BMPImageWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by : harald.kuhr$
 * @version : BMPImageWriterTest.java,v 1.0 25/06/2020 harald.kuhr Exp$
 */
public class BMPImageWriterTest extends ImageWriterAbstractTest {

    private final BMPImageWriterSpi provider = new BMPImageWriterSpi();

    @Override
    protected ImageWriter createImageWriter() {
        return provider.createWriterInstance(null);
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
        );
    }
}