package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QTBMPDecompressorTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressorTest.java,v 1.0 24/03/2021 haraldk Exp$
 */
public class QTRAWDecompressorTest {
    private ImageDesc createDescription(int bitDepth) {
        ImageDesc description = new ImageDesc();
        description.compressorVendor = QuickTime.VENDOR_APPLE;
        description.compressorIdentifer = "raw ";
        description.depth = (short) bitDepth;

        return description;
    }

    @Test
    public void canDecompressRGB() {
        QTDecompressor decompressor = new QTRAWDecompressor();

        assertTrue(decompressor.canDecompress(createDescription(24)));
    }

    @Test
    public void canDecompressRGBA() {
        QTDecompressor decompressor = new QTRAWDecompressor();

        assertTrue(decompressor.canDecompress(createDescription(32)));
    }

    @Test
    public void canDecompressGray() {
        QTDecompressor decompressor = new QTRAWDecompressor();

        assertTrue(decompressor.canDecompress(createDescription(40)));
    }
}