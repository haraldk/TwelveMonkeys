package com.twelvemonkeys.imageio.path;

import java.awt.geom.Path2D;
import java.io.DataInput;
import java.io.IOException;

/**
 * AdobePathBuilder.
 *
 * @deprecated Use {@link AdobePathReader} instead. This class will be removed in a future release.
 */
public final class AdobePathBuilder {

    private final AdobePathReader delegate;

    /**
     * @see AdobePathReader#AdobePathReader(DataInput)
     */
    public AdobePathBuilder(final DataInput data) {
        this.delegate = new AdobePathReader(data);
    }

    /**
     * @see AdobePathReader#AdobePathReader(byte[])
     */
    public AdobePathBuilder(final byte[] data) {
        this.delegate = new AdobePathReader(data);
    }

    /**
     * @see AdobePathReader#path()
     */
    public Path2D path() throws IOException {
        return delegate.path();
    }
}
