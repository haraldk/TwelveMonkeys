package com.twelvemonkeys.io;

import junit.framework.TestCase;

/**
 * SeekableAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/SeekableAbstractTestCase.java#1 $
 */
public abstract class SeekableAbstractTestCase extends TestCase implements SeekableInterfaceTest {

    protected abstract Seekable createSeekable();

    public void testFail() {
        fail();
    }

    public void testSeekable() {
        assertTrue(createSeekable() instanceof Seekable);
    }
}
