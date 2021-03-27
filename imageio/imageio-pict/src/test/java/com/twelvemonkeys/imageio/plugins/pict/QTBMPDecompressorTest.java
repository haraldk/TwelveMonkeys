package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

/**
 * QTBMPDecompressorTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressorTest.java,v 1.0 24/03/2021 haraldk Exp$
 */
public class QTBMPDecompressorTest {
    @Test
    public void canDecompress() {
        QTDecompressor decompressor = new QTBMPDecompressor();

        ImageDesc description = new ImageDesc();
        description.compressorVendor = QuickTime.VENDOR_APPLE;
        description.compressorIdentifer = "WRLE";
        description.extraDesc = "....bmp ...something...".getBytes(StandardCharsets.UTF_8);

        assertTrue(decompressor.canDecompress(description));
    }
}