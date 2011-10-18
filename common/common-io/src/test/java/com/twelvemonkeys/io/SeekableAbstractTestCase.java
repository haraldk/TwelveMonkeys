package com.twelvemonkeys.io;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SeekableAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/SeekableAbstractTestCase.java#1 $
 */
public abstract class SeekableAbstractTestCase implements SeekableInterfaceTest {

    protected abstract Seekable createSeekable();

    @Test
    public void testFail() {
        fail("Do not create stand-alone test classes based on this class. Instead, create an inner class and delegate to it.");
    }

    @Test
    public void testSeekable() {
        assertTrue(createSeekable() instanceof Seekable);
    }
}
