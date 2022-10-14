package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * BufferedInputStreamImageInputStreamSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedInputStreamImageInputStreamSpiTest.java,v 1.0 08/09/2022 haraldk Exp$
 */
public class BufferedInputStreamImageInputStreamSpiTest extends ImageInputStreamSpiTest<InputStream> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new BufferedInputStreamImageInputStreamSpi();
    }

    @Override
    protected InputStream createInput() {
        return new ByteArrayInputStream(new byte[0]);
    }
}