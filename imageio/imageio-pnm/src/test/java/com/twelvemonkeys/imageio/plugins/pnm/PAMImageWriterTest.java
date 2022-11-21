package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.spi.ImageWriterSpi;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.util.Arrays;
import java.util.List;

public class PAMImageWriterTest extends ImageWriterAbstractTest<PNMImageWriter> {
    // NOTE: It's the same writer, however, the different SPI configures PAM mode, and enables extra formats
    @Override
    protected ImageWriterSpi createProvider() {
        return new PAMImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_BINARY),
                new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(100, 100, BufferedImage.TYPE_USHORT_GRAY),
                new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_BGR),
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR),
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB_PRE),
                new BufferedImage(new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpace.CS_GRAY), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT), Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, 10, 10, 2, null), false, null),
                new BufferedImage(new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE), Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 10, 10, 4, null), false, null),
                new BufferedImage(new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT), Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, 10, 10, 5, null), false, null)
        );
    }
}