package com.twelvemonkeys.imageio.metadata.tiff;

import com.twelvemonkeys.io.FastByteArrayOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HalfTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HalfTest.java,v 1.0 10/04/2021 haraldk Exp$
 */
public class HalfTest {
    Random random = new Random(8374698541237L);

    @Test
    public void testSize() {
        assertEquals(16, Half.SIZE);
    }

    @Test
    public void testRoundTrip() {
        for (int i = 0; i < 1024; i++) {
            short half = (short) random.nextInt(Short.MAX_VALUE & 0x3FFF);
            float floatValue = Half.shortBitsToFloat(half);
            assertEquals(half, Half.floatToShortBits(floatValue));
        }
    }

    @Test
    public void testExactEncoding() {
        for (short half = -2048; half < 2048; half++) {
            assertEquals(half, Half.shortBitsToFloat(Half.floatToShortBits(half)), 0, String.valueOf(half));
        }
    }

    @Test
    public void testRoundTripBack() {
        for (int i = 0; i < 1024; i++) {
            float floatValue = random.nextFloat();
            short half = Half.floatToShortBits(floatValue);
            assertEquals(floatValue, Half.shortBitsToFloat(half), 0.0003); // Might lose some precision 32 -> 16 bit
        }
    }

    @Test
    public void testHashCode() {
        for (int i = 0; i < 1024; i++) {
            short halfBits = (short) random.nextInt(Short.MAX_VALUE);
            Half half = new Half(halfBits);
            assertEquals(halfBits, half.hashCode());
        }
    }

    @Test
    public void testEquals() {
        for (int i = 0; i < 1024; i++) {
            short halfBits = (short) random.nextInt(Short.MAX_VALUE);
            Half half = new Half(halfBits);
            assertEquals(new Half(halfBits), half);
        }
    }

    @Test
    public void testCompareEquals() {
        for (int i = 0; i < 1024; i++) {
            short halfBits = (short) random.nextInt(Short.MAX_VALUE);
            Half half = new Half(halfBits);
            assertEquals(0, new Half(halfBits).compareTo(half));
        }
    }

    @Test
    public void testCompareLess() {
        for (int i = 0; i < 1024; i++) {
            short halfBits = (short) random.nextInt(Short.MAX_VALUE & 0x3FFF);
            Half half = new Half(halfBits );
            assertEquals(-1, new Half((short) (halfBits - 2)).compareTo(half));
        }
    }

    @Test
    public void testCompareGreater() {
        for (int i = 0; i < 1024; i++) {
            short halfBits = (short) random.nextInt(Short.MAX_VALUE & 0x3FFF);
            Half half = new Half(halfBits);
            assertEquals(1, new Half((short) (halfBits + 2)).compareTo(half));
        }
    }

    @Test
    public void testToString() {
        assertEquals("0.0", new Half((short) 0).toString());
        // TODO: More... But we just delegate to Float.toString, so no worries... :-)
    }

    @Test
    public void testParseHalfNull() {
        assertThrows(NullPointerException.class, () -> Half.parseHalf(null));
    }

    @Test
    public void testParseHalfBad() {
        assertThrows(NumberFormatException.class, () -> Half.parseHalf("foo"));
    }

    @Test
    public void testParseHalf() {
        short half = Half.parseHalf("9876.5432");
        assertEquals(Half.floatToShortBits(9876.5432f), half);
        // TODO: More... But we just delegate to Float.valueOf, so no worries... :-)
    }

    @Test
    public void testValueOfNull() {
        assertThrows(NullPointerException.class, () -> Half.valueOf(null));
    }

    @Test
    public void testValueOfBad() {
        assertThrows(NumberFormatException.class, () -> Half.valueOf("foo"));
    }

    @Test
    public void testValueOf() {
        Half half = Half.valueOf("12.3456");
        assertEquals(new Half(Half.floatToShortBits(12.3456f)), half);
        // TODO: More... But we just delegate to Float.valueOf, so no worries... :-)
    }

    @Test
    public void testIntValue() {
        for (int i = 0; i < 1024; i++) {
            int intValue = i << 1;
            Half half = new Half(Half.floatToShortBits((float) intValue));
            assertEquals(intValue, half.intValue());
        }
    }

    @Test
    public void testLongValue() {
        for (int i = 0; i < 1024; i++) {
            long longValue = i << 2;
            Half half = new Half(Half.floatToShortBits((float) longValue));
            assertEquals(longValue, half.longValue());
        }
    }

    @Test
    public void testFloatValue() {
        for (int i = 0; i < 1024; i++) {
            float floatValue = random.nextFloat();
            Half half = new Half(Half.floatToShortBits(floatValue));
            assertEquals(floatValue, half.floatValue(), 0.0003);
        }
    }

    @Test
    public void testDoubleValue() {
        for (int i = 0; i < 1024; i++) {
            double doubleValue = random.nextDouble();
            Half half = new Half(Half.floatToShortBits((float) doubleValue));
            assertEquals(doubleValue, half.doubleValue(), 0.0003);
        }
    }

    @Test
    public void testSerializationRoundTrip() throws IOException, ClassNotFoundException {
        Half original = new Half((short) 0x3D75);
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(64);
        new ObjectOutputStream(bytes).writeObject(original);

        Object restored = new ObjectInputStream(bytes.createInputStream()).readObject();
        assertTrue(restored instanceof Half);
        assertEquals(original, restored); // Only tests bits, not transient float value

        assertEquals(original.floatValue(), ((Half) restored).floatValue(), 0);
    }
}