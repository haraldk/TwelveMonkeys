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
    private final int dataOffset;
    private final int dataLength;

    public ByteArrayImageInputStream(final byte[] pData) {
        this(pData, 0, pData != null ? pData.length : -1);
    }

    public ByteArrayImageInputStream(final byte[] pData, int offset, int length) {
        Validate.notNull(pData, "data");
        Validate.isTrue(offset >= 0 && offset <= pData.length, offset, "offset out of range: %d");
        Validate.isTrue(length >= 0 && length <= pData.length - offset, length, "length out of range: %d");

        data = pData;
        dataOffset = offset;
        dataLength = length;
    }

    public int read() throws IOException {
        if (streamPos >= dataLength) {
            return -1;
        }

        bitOffset = 0;

        return data[((int) streamPos++) + dataOffset] & 0xff;
    }

    public int read(byte[] pBuffer, int pOffset, int pLength) throws IOException {
        if (streamPos >= dataLength) {
            return -1;
        }

        int length = (int) Math.min(this.dataLength - streamPos, pLength);
        bitOffset = 0;
        System.arraycopy(data, (int) streamPos + dataOffset, pBuffer, pOffset, length);
        streamPos += length;

        return length;
    }

    @Override
    public long length() {
        return dataLength;
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
