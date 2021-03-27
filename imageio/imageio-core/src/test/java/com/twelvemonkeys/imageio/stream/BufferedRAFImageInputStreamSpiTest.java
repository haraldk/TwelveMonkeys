package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRAFImageInputStreamSpiTest extends ImageInputStreamSpiTest<RandomAccessFile> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new BufferedRAFImageInputStreamSpi();
    }

    @Override
    protected RandomAccessFile createInput() throws IOException {
        return new RandomAccessFile(File.createTempFile("test-", ".tst"), "r");
    }
}