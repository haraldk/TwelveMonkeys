package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * A wrapper for {@link ImageInputStream} to limit the number of bytes that can be read.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SubImageInputStream.java,v 1.0 Nov 8, 2009 2:50:58 PM haraldk Exp$
 */
public final class SubImageInputStream extends ImageInputStreamImpl {
    // NOTE: This class is based on com.sun.imageio.plugins.common.SubImageInputStream, but fixes some of its bugs.

    private final ImageInputStream stream;
    private final long startPos;
    private final long length;

    /**
     * Creates a {@link ImageInputStream}, reading up to a maximum number of bytes from the underlying stream.
     *
     * @param pStream the underlying stream
     * @param pLength the maximum length to read from the stream.
     * Note that {@code pStream} may contain less than this maximum number of bytes.
     *
     * @throws IOException if {@code pStream}'s position can't be determined.
     * @throws IllegalArgumentException if {@code pStream == null} or {@code pLength < 0}
     */
    public SubImageInputStream(final ImageInputStream pStream, final long pLength) throws IOException {
        Validate.notNull(pStream, "stream");
        Validate.isTrue(pLength >= 0, pLength, "length < 0: %d");

        stream = pStream;
        startPos = pStream.getStreamPosition();
        length = pLength;
    }

    public int read() throws IOException {
        if (streamPos >= length) { // Local EOF
            return -1;
        }
        else {
            int read = stream.read();

            if (read >= 0) {
                streamPos++;
            }
            
            return read;
        }
    }

    public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        if (streamPos >= length) { // Local EOF
            return -1;
        }

        // Safe cast, as pLength can never cause int overflow
        int length = (int) Math.min(pLength, this.length - streamPos);
        int count = stream.read(pBytes, pOffset, length);

        if (count >= 0) {
            streamPos += count;
        }

        return count;
    }

    @Override
    public long length() {
        try {
            long length = stream.length();
            return length < 0 ? -1 : Math.min(length - startPos, this.length);
        }
        catch (IOException ignore) {
        }

        return -1;
    }

    @Override
    public void seek(final long pPosition) throws IOException {
        if (pPosition < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPosition");
        }

        stream.seek(startPos + pPosition);
        streamPos = pPosition;
    }

    @SuppressWarnings({"FinalizeDoesntCallSuperFinalize"})
    @Override
    protected void finalize() throws Throwable {
        // Empty finalizer (for improved performance; no need to call super.finalize() in this case)
    }
}
