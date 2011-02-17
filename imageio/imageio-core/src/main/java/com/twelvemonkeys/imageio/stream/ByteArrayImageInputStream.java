package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * Experimental
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStream.java,v 1.0 May 15, 2008 2:12:12 PM haraldk Exp$
 */
public final class ByteArrayImageInputStream extends ImageInputStreamImpl {
    private final byte[] data;

    public ByteArrayImageInputStream(final byte[] pData) {
        Validate.notNull(pData, "data");
        data = pData;
    }

    public int read() throws IOException {
        if (streamPos >= data.length) {
            return -1;
        }

        bitOffset = 0;

        return data[((int) streamPos++)] & 0xff;
    }

    public int read(byte[] pBuffer, int pOffset, int pLength) throws IOException {
        if (streamPos >= data.length) {
            return -1;
        }

        int length = (int) Math.min(data.length - streamPos, pLength);
        bitOffset = 0;
        System.arraycopy(data, (int) streamPos, pBuffer, pOffset, length);
        streamPos += length;

        return length;
    }

    @Override
    public long length() {
        return data.length;
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public boolean isCachedMemory() {
        return true;
    }
}
