package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

public class PNMImageWriterTest extends ImageWriterAbstractTest {

    private final PNMImageWriterSpi provider = new PNMImageWriterSpi();

    @Override
    protected ImageWriter createImageWriter() {
        return provider.createWriterInstance(null);
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