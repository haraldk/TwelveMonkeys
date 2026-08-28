package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import javax.imageio.stream.MemoryCacheImageInputStream;
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
                () -> decompressor.decompress(description, new ByteArrayImageInputStream(new byte[description.dataSize])));
    }

    @Test
    void decompressRGBDataSizeTooSmall() {
        ImageDesc description = createDescription(24);
        description.width = 8;
        description.height = 8;
        description.dataSize = 16;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayImageInputStream(new byte[description.dataSize])));
    }

    @Test
    void decompressGrayDataSizeTooSmall() {
        ImageDesc description = createDescription(40);
        description.width = 8;
        description.height = 8;
        description.dataSize = 16;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayImageInputStream(new byte[description.dataSize])));
    }

    @Test
    void decompressRGBDataSizeTooBig() {
        ImageDesc description = createDescription(24);
        description.width = 10;
        description.height = 10;
        description.dataSize = Integer.MAX_VALUE; // java.lang.OutOfMemoryError if allocation is attempted

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertDoesNotThrow(() -> decompressor.decompress(description, new ByteArrayImageInputStream(new byte[300])));
    }

    @Test
    void decompressRGBPixelSizeTooBigKnownStreamLength() {
        ImageDesc description = createDescription(32);
        description.width = Short.MAX_VALUE >> 1;
        description.height = Short.MAX_VALUE >> 1;
        description.dataSize = Integer.MAX_VALUE;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new ByteArrayImageInputStream(new byte[300])));
    }

    @Test
    void decompressRGBPixelSizeTooBigUnknownStreamLength() {
        ImageDesc description = createDescription(32);
        description.width = Short.MAX_VALUE >> 1;
        description.height = Short.MAX_VALUE >> 1;
        description.dataSize = Integer.MAX_VALUE;

        QTDecompressor decompressor = new QTRAWDecompressor();
        assertThrows(IIOException.class,
                () -> decompressor.decompress(description, new MemoryCacheImageInputStream(new ByteArrayInputStream(new byte[100])) {
                    @Override
                    public long length() {
                        return -1L;
                    }
                }));
    }
}