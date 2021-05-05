package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

/**
 * PSDImageWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageWriterTest.java,v 1.0 05/05/2021 haraldk Exp$
 */
public class PSDImageWriterTest extends ImageWriterAbstractTest<PSDImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new PSDImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(301, 199, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(299, 201, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(160, 90, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(90, 160, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(30, 20, BufferedImage.TYPE_USHORT_GRAY),
                new BufferedImage(30, 20, BufferedImage.TYPE_BYTE_BINARY),
                new BufferedImage(30, 20, BufferedImage.TYPE_BYTE_INDEXED)
        );
    }
}
