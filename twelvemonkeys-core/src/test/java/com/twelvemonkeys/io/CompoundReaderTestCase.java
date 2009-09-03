package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.CollectionUtil;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;

/**
 * CompoundReaderTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/CompoundReaderTestCase.java#2 $
 */
public class CompoundReaderTestCase extends ReaderAbstractTestCase {

    protected Reader makeReader(String pInput) {
        // Split
        String[] input = StringUtil.toStringArray(pInput, " ");
        List<Reader> readers = new ArrayList<Reader>(input.length);

        // Reappend spaces...
        // TODO: Add other readers
        for (int i = 0; i < input.length; i++) {
            if (i != 0) {
                input[i] = " " + input[i];
            }
            readers.add(new StringReader(input[i]));
        }

        return new CompoundReader(readers.iterator());
    }

    public void testNullConstructor() {
        try {
            new CompoundReader(null);
            fail("Should not allow null argument");
        }
        catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testEmptyIteratorConstructor() throws IOException {
        Reader reader = new CompoundReader(CollectionUtil.iterator(new Reader[0]));
        assertEquals(-1, reader.read());
    }

    public void testIteratorWithNullConstructor() throws IOException {
        try {
            new CompoundReader(CollectionUtil.iterator(new Reader[] {null}));
            fail("Should not allow null in iterator argument");
        }
        catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }
}
