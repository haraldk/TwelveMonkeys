package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class DDSImageWriterTest extends ImageWriterAbstractTest<DDSImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new DDSImageWriterSpi();
    }

    @Override
    protected List<BufferedImage> getTestData() {
        return Arrays.asList(
            new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB_PRE),
            new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB),
            new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
            new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR),
            new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR)
        );
    }
}
