package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

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
        data = notNull(pData, "data");
        dataOffset = isBetween(0, pData.length, offset, "offset");
        dataLength = isBetween(0, pData.length - offset, length, "length");
    }

    private static int isBetween(final int low, final int high, final int value, final String name) {
        return isTrue(value >= low && value <= high, value, String.format("%s out of range [%d, %d]: %d", name, low, high, value));
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
