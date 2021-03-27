package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * QTBMPDecompressorTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressorTest.java,v 1.0 24/03/2021 haraldk Exp$
 */
public class QTGenericDecompressorTest {
    private ImageDesc createDescription(final String identifer, final String name, final int depth) {
        ImageDesc description = new ImageDesc();
        description.compressorVendor = QuickTime.VENDOR_APPLE;
        description.compressorIdentifer = identifer;
        description.compressorName = name;
        description.depth = (short) depth;

        return description;
    }

    @Test
    public void canDecompressJPEG() {
        QTDecompressor decompressor = new QTGenericDecompressor();

        assertTrue(decompressor.canDecompress(createDescription("jpeg", "Photo - JPEG", 8)));
        assertTrue(decompressor.canDecompress(createDescription("jpeg", "Photo - JPEG", 24)));
    }

    @Test
    public void canDecompressPNG() {
        QTDecompressor decompressor = new QTGenericDecompressor();

        assertTrue(decompressor.canDecompress(createDescription("png ", "PNG", 8)));
        assertTrue(decompressor.canDecompress(createDescription("png ", "PNG", 24)));
        assertTrue(decompressor.canDecompress(createDescription("png ", "PNG", 32)));
    }

    @Test
    public void canDecompressTIFF() {
        QTDecompressor decompressor = new QTGenericDecompressor();

        assertTrue(decompressor.canDecompress(createDescription("tiff", "TIFF", 8)));
        assertTrue(decompressor.canDecompress(createDescription("tiff", "TIFF", 24)));
        assertTrue(decompressor.canDecompress(createDescription("tiff", "TIFF", 32)));
    }
}