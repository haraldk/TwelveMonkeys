package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * BufferedInputStreamImageInputStreamSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedInputStreamImageInputStreamSpiTest.java,v 1.0 08/09/2022 haraldk Exp$
 */
public class BufferedFileInputStreamImageInputStreamSpiTest extends ImageInputStreamSpiTest<InputStream> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new BufferedInputStreamImageInputStreamSpi();
    }

    @Override
    protected InputStream createInput() throws IOException {
        return Files.newInputStream(File.createTempFile("test-", ".tst").toPath());
    }
}