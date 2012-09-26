package com.twelvemonkeys.io;

import java.io.InputStream;

/**
 * MemoryCacheSeekableStreamTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/MemoryCacheSeekableStreamTestCase.java#2 $
 */
public class MemoryCacheSeekableStreamTestCase extends SeekableInputStreamAbstractTestCase {
    protected SeekableInputStream makeInputStream(final InputStream pStream) {
        return new MemoryCacheSeekableStream(pStream);
    }
}
