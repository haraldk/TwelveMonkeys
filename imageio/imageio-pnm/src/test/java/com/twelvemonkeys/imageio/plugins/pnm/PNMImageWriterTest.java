package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

public class PNMImageWriterTest extends ImageWriterAbstractTest<PNMImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new PNMImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(100, 100, BufferedImage.TYPE_USHORT_GRAY),
                new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR)
        );
    }
}