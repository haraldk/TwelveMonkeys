package com.twelvemonkeys.io;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * StringInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: StringInputStreamTest.java,v 1.0 03.09.13 10:40 haraldk Exp$
 */
public class StringInputStreamTest {

    static final Charset UTF8 = Charset.forName("UTF-8");
    static final String LONG_STRING = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse id est lobortis, elementum nisi id, mollis urna. Morbi lorem nulla, vehicula ut ultricies ut, blandit sit amet metus. Praesent ut urna et arcu commodo tempus. Aenean dapibus commodo ligula, non vehicula leo dictum a. Aenean at leo ut eros hendrerit pellentesque. Phasellus sagittis arcu non faucibus faucibus. Sed volutpat vulputate metus sed consequat. Aenean auctor sapien sit amet erat dictum laoreet. Nullam libero felis, rutrum scelerisque elit eu, porta mollis nisi. Vestibulum vel ultricies turpis, vel dignissim arcu.\n" +
            "Ut convallis erat et dapibus feugiat. Pellentesque eu dictum ligula, et interdum nibh. Sed rutrum justo a leo faucibus eleifend. Proin est justo, porttitor vel nulla egestas, faucibus scelerisque lacus. Vivamus sit amet gravida nibh. Praesent odio diam, ornare vitae mi nec, pretium ultrices tellus. Pellentesque vitae felis consequat mauris lacinia condimentum in ut nibh. In odio quam, laoreet luctus velit vel, suscipit mollis leo. Etiam justo nulla, posuere et massa non, pretium vehicula diam. Sed porta molestie mauris quis condimentum. Sed quis gravida ipsum, eget porttitor felis. Vivamus volutpat velit vitae dolor convallis, nec malesuada est porttitor. Proin sed purus vel leo pretium suscipit. Morbi ut nibh quis tortor vehicula porttitor non sit amet lorem. Proin tempor vel sem sit amet accumsan.\n" +
            "Cras vulputate orci a lorem luctus, vel egestas leo porttitor. Duis venenatis odio et mauris molestie rutrum. Mauris gravida volutpat odio at consequat. Mauris eros purus, bibendum in vulputate vitae, laoreet quis libero. Quisque lacinia, neque sed semper fringilla, elit dolor sagittis est, nec tincidunt ipsum risus ut sem. Maecenas consectetur aliquam augue. Etiam neque mi, euismod eget metus quis, molestie lacinia odio. Sed eget sollicitudin metus. Phasellus facilisis augue et sem facilisis, consequat mollis augue ultricies.\n" +
            "Vivamus in porta massa. Sed eget lorem non lectus viverra pretium. Curabitur convallis posuere est vestibulum vulputate. Maecenas placerat risus ut dui hendrerit, sed suscipit magna tincidunt. Etiam ut mattis dolor, quis dictum velit. Donec ut dui sit amet libero convallis euismod. Phasellus dapibus dolor in nibh volutpat, eu scelerisque neque tempus. Maecenas a rhoncus velit. Etiam sollicitudin, leo non euismod vehicula, lectus risus aliquet metus, quis cursus purus orci non turpis. Nulla vel enim tortor. Quisque nec mi vulputate, convallis orci vel, suscipit nibh. Sed sed tellus id elit commodo laoreet ut euismod ligula. Mauris suscipit commodo interdum. Phasellus scelerisque arcu nec nibh porta, et semper massa rutrum. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.\n" +
            "Praesent cursus, sapien ut venenatis malesuada, turpis nulla venenatis velit, nec tristique leo turpis auctor purus. Curabitur non porta urna. Sed vitae felis massa. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Phasellus scelerisque id dolor nec fermentum. Etiam suscipit tincidunt odio, sed molestie elit fringilla in. Phasellus nec euismod lacus. Suspendisse bibendum vulputate viverra. Fusce mollis pharetra imperdiet. Phasellus tortor eros, rhoncus volutpat diam in, scelerisque viverra felis. Ut ornare urna commodo, pretium mauris eget, eleifend ipsum.";
    static final String SHORT_STRING = "Java";

    @Test
    public void testReadShortString() throws IOException {
        StringInputStream stream = new StringInputStream(SHORT_STRING, UTF8);

        byte[] value = SHORT_STRING.getBytes(UTF8);
        for (int i = 0; i < value.length; i++) {
            int read = stream.read();
            assertEquals(String.format("Wrong value at offset %s: '%s' != '%s'", i, String.valueOf((char) value[i]), String.valueOf((char) (byte) read)), value[i] &0xff, read);
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadSubString() throws IOException {
        StringInputStream stream = new StringInputStream("foo bar xyzzy", 4, 3, UTF8);

        byte[] value = "bar".getBytes(UTF8);
        for (int i = 0; i < value.length; i++) {
            int read = stream.read();
            assertEquals(String.format("Wrong value at offset %s: '%s' != '%s'", i, String.valueOf((char) value[i]), String.valueOf((char) (byte) read)), value[i] &0xff, read);
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadNonAsciiString() throws IOException {
        String string = "\u00c6\u00d8\u00c5\u00e6\u00f8\u00e5\u00e1\u00e9\u00c0\u00c8\u00fc\u00dc\u00df";
        StringInputStream stream = new StringInputStream(string, UTF8);

        byte[] value = string.getBytes(UTF8);
        for (int i = 0; i < value.length; i++) {
            int read = stream.read();
            assertEquals(String.format("Wrong value at offset %s: '%s' != '%s'", i, String.valueOf((char) value[i]), String.valueOf((char) (byte) read)), value[i] &0xff, read);
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadLongString() throws IOException {
        StringInputStream stream = new StringInputStream(LONG_STRING, UTF8);

        byte[] value = LONG_STRING.getBytes(UTF8);
        for (int i = 0; i < value.length; i++) {
            int read = stream.read();
            assertEquals(String.format("Wrong value at offset %s: '%s' != '%s'", i, String.valueOf((char) value[i]), String.valueOf((char) (byte) read)), value[i] &0xff, read);
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArrayLongString() throws IOException {
        StringInputStream stream = new StringInputStream(LONG_STRING, UTF8);

        byte[] value = LONG_STRING.getBytes(UTF8);
        byte[] buffer = new byte[17];
        int count;
        for (int i = 0; i < value.length; i += count) {
            count = stream.read(buffer);
            assertArrayEquals(String.format("Wrong value at offset %s", i), Arrays.copyOfRange(value, i, i + count), Arrays.copyOfRange(buffer, 0, count));
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArraySkipLongString() throws IOException {
        StringInputStream stream = new StringInputStream(LONG_STRING, UTF8);

        byte[] value = LONG_STRING.getBytes(UTF8);
        byte[] buffer = new byte[17];
        int count;
        for (int i = 0; i < value.length; i += count) {
            if (i % 2 == 0) {
                count = (int) stream.skip(buffer.length);
            }
            else {
                count = stream.read(buffer);
                assertArrayEquals(String.format("Wrong value at offset %s", i), Arrays.copyOfRange(value, i, i + count), Arrays.copyOfRange(buffer, 0, count));
            }
        }

        assertEquals(-1, stream.read());
    }

    /*@Test
    public */void testPerformance() throws IOException {
        for (int i = 0; i < 100000; i++) {
            StringInputStream stream = new StringInputStream(LONG_STRING, UTF8);
            while(stream.read() != -1) {
                stream.available();
            }
        }

    }
}
