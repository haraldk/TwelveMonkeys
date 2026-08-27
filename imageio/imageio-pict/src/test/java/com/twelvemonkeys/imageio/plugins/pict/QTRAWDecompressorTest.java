package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QTBMPDecompressorTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressorTest.java,v 1.0 24/03/2021 haraldk Exp$
 */
class QTRAWDecompressorTest {
    private ImageDesc createDescription(int bitDepth) {
        ImageDesc description = new ImageDesc();
        description.compressorVendor = QuickTime.VENDOR_APPLE;
        description.compressorIdentifer = "raw ";
        description.depth = (short) bitDepth;

        return description;
    }

    @Test
    void canDecompressRGB() {
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

    @Test
    void decompressRGBADataSizeTooSmall() {
        // width * height * 4 (256) is larger than the declared data size (16)
        ImageDesc description = createDescription(32);
        description.width = 8;
        description.height = 8;
        description.dataSize = 16;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayInputStream(new byte[description.dataSize])));
    }

    @Test
    void decompressRGBDataSizeTooSmall() {
        ImageDesc description = createDescription(24);
        description.width = 8;
        description.height = 8;
        description.dataSize = 16;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayInputStream(new byte[description.dataSize])));
    }

    @Test
    void decompressGrayDataSizeTooSmall() {
        ImageDesc description = createDescription(40);
        description.width = 8;
        description.height = 8;
        description.dataSize = 16;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayInputStream(new byte[description.dataSize])));
    }
}