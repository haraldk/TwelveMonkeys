package com.twelvemonkeys.imageio.stream;

import org.junit.Test;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;

public class BufferedFileImageInputStreamSpiTest extends ImageInputStreamSpiTest<File> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new BufferedFileImageInputStreamSpi();
    }

    @Override
    protected File createInput() throws IOException {
        return File.createTempFile("test-", ".tst");
    }

    @Test
    public void testReturnNullWhenFileDoesNotExist() throws IOException {
        // This is really stupid behavior, but it is consistent with the JRE bundled SPIs.
        File input = new File("a-file-that-should-not-exist-ever.fnf");
        assumeFalse("File should not exist: " + input.getPath(), input.exists());
        assertNull(provider.createInputStreamInstance(input));
    }
}