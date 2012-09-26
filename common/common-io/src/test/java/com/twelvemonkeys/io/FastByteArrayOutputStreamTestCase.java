package com.twelvemonkeys.io;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * FastByteArrayOutputStreamTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/FastByteArrayOutputStreamTestCase.java#1 $
 */
public class FastByteArrayOutputStreamTestCase extends OutputStreamAbstractTestCase {
    protected FastByteArrayOutputStream makeObject() {
        return new FastByteArrayOutputStream(256);
    }

    @Test
    public void testCreateInputStream() throws IOException {
        FastByteArrayOutputStream out = makeObject();

        String hello = "Hello World";
        out.write(hello.getBytes("UTF-8"));

        InputStream in = out.createInputStream();

        byte[] read = FileUtil.read(in);

        assertEquals(hello, new String(read, "UTF-8"));
    }
}
