package com.twelvemonkeys.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * FileCacheSeekableStreamTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/FileCacheSeekableStreamTestCase.java#3 $
 */
public class FileCacheSeekableStreamTestCase extends SeekableInputStreamAbstractTestCase {
    protected SeekableInputStream makeInputStream(final InputStream pStream) {
        try {
            return new FileCacheSeekableStream(pStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
