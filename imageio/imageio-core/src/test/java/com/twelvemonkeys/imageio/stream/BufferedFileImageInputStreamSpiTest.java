package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.io.IOException;

public class BufferedFileImageInputStreamSpiTest extends ImageInputStreamSpiTest<File> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new BufferedFileImageInputStreamSpi();
    }

    @Override
    protected File createInput() throws IOException {
        return File.createTempFile("test-", ".tst");
    }
}