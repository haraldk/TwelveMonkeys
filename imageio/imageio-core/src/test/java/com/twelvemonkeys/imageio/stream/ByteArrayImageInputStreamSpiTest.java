package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;

public class ByteArrayImageInputStreamSpiTest extends ImageInputStreamSpiTest<byte[]> {

    @Override
    protected ImageInputStreamSpi createProvider() {
        return new ByteArrayImageInputStreamSpi();
    }

    @Override
    protected byte[] createInput() {
        return new byte[0];
    }
}