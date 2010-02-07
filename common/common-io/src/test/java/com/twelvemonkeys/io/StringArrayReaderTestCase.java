package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.StringUtil;

import java.io.Reader;
import java.io.IOException;

/**
 * StringArrayReaderTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/StringArrayReaderTestCase.java#1 $
 */
public class StringArrayReaderTestCase extends ReaderAbstractTestCase {

    protected Reader makeReader(String pInput) {
        // Split
        String[] input = StringUtil.toStringArray(pInput, " ");
        // Reappend spaces...
        for (int i = 0; i < input.length; i++) {
            if (i != 0) {
                input[i] = " " + input[i];
            }
        }

        return new StringArrayReader(input);
    }

    public void testNullConstructor() {
        try {
            new StringArrayReader(null);
            fail("Should not allow null argument");
        }
        catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testEmptyArrayConstructor() throws IOException {
        Reader reader = new StringArrayReader(new String[0]);
        assertEquals(-1, reader.read());
    }

    public void testEmptyStringConstructor() throws IOException {
        Reader reader = new StringArrayReader(new String[] {""});
        assertEquals(-1, reader.read());
    }

    
}
