package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * ThumbsDBProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ThumbsDBProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class ThumbsDBProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new ThumbsDBProviderInfo();
    }

    @Override
    public void formatNames() {
        String[] names = getProviderInfo().formatNames();
        assertNotNull(names);
        assertFalse(names.length == 0);

        List<String> list = new ArrayList<>(asList(names));
        assertTrue(list.remove("Thumbs DB")); // No dupes of this name

        for (String name : list) {
            assertNotNull(name);
            assertFalse(name.isEmpty());

            assertTrue(list.contains(name.toLowerCase()));
            assertTrue(list.contains(name.toUpperCase()));
        }
    }
}