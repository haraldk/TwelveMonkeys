package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import java.net.URL;

public class URLImageInputStreamSpiTest extends ImageInputStreamSpiTest<URL> {
    @Override
    protected ImageInputStreamSpi createProvider() {
        return new URLImageInputStreamSpi();
    }

    @Override
    protected URL createInput() {
        return getClass().getResource("/empty-stream.txt");
    }
}