package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.IOException;

/**
 * ImageInputStream that writes through a delegate, but keeps local position and bit offset.
 * Note: Flushing or closing this stream will *not* have an effect on the delegate.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SubImageOutputStream.java,v 1.0 30/03/15 harald.kuhr Exp$
 */
public class SubImageOutputStream extends ImageOutputStreamImpl {
    private final ImageOutputStream stream;

    public SubImageOutputStream(final ImageOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void write(int b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public boolean isCached() {
        return stream.isCached();
    }

    @Override
    public boolean isCachedMemory() {
        return stream.isCachedMemory();
    }

    @Override
    public boolean isCachedFile() {
        return stream.isCachedFile();
    }
}
