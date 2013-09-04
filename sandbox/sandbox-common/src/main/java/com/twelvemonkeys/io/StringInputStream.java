package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * An {@code InputStream} that reads bytes from a {@code String}.
 *
 * This class properly converts characters into bytes using a {@code Charset},
 * unlike the deprecated {@link java.io.StringBufferInputStream}.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: StringInputStream.java,v 1.0 03.09.13 10:19 haraldk Exp$
 */
public final class StringInputStream extends InputStream {

    private final CharBuffer chars;
    private final CharsetEncoder encoder;
    private final ByteBuffer buffer;

    public StringInputStream(final String string, final Charset charset) {
        this(Validate.notNull(string, "string"), 0, string.length(), charset);
    }

    public StringInputStream(final String string, int offset, int length, final Charset charset) {
        chars = CharBuffer.wrap(Validate.notNull(string, "string"), offset, offset + length);
        encoder = Validate.notNull(charset, "charset").newEncoder();
        buffer = ByteBuffer.allocate(256);
        buffer.flip();
    }

    private boolean fillBuffer() {
        buffer.clear();
        encoder.encode(chars, buffer, chars.hasRemaining()); // TODO: Do we have to care about the result?
        buffer.flip();

        return buffer.hasRemaining();
    }

    private boolean ensureBuffer() {
        return buffer.hasRemaining() || (chars.hasRemaining() && fillBuffer());
    }

    @Override
    public int read() throws IOException {
        if (!ensureBuffer()) {
            return -1;
        }

        return buffer.get() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!ensureBuffer()) {
            return -1;
        }

        int count = Math.min(buffer.remaining(), len);
        buffer.get(b, off, count);
        return count;
    }

    @Override
    public long skip(long len) throws IOException {
        if (!ensureBuffer()) {
            return -1;
        }

        int count = (int) Math.min(buffer.remaining(), len);
        int position = buffer.position();
        buffer.position(position + count);
        return count;
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }
}
