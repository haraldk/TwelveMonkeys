package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class DDSImageWriterTest extends ImageWriterAbstractTest<DDSImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new DDSImageWriterSpi();
    }

    @Override
    protected List<BufferedImage> getTestData() {
        return Collections.singletonList(
                new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB_PRE)
        );
    }
}
