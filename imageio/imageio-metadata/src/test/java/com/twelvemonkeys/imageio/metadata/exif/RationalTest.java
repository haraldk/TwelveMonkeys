package com.twelvemonkeys.imageio.metadata.exif;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * RationalTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RationalTest.java,v 1.0 Nov 18, 2009 3:23:17 PM haraldk Exp$
 */
@SuppressWarnings("deprecation")
public class RationalTest {
    @Test(expected = IllegalArgumentException.class)
    public void testZeroDenominator() {
        new Rational(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLongMinValueNumerator() {
        new Rational(Long.MIN_VALUE, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLongMinValueDenominator() {
        new Rational(1, Long.MIN_VALUE);
    }

    @Test
    public void testEquals() {
        assertEquals(new Rational(0, 1), new Rational(0, 999));
        assertEquals(new Rational(0, 1), new Rational(0, -1));
        assertEquals(new Rational(1, 2), new Rational(1000000, 2000000));
        assertEquals(new Rational(1, -2), new Rational(-1, 2));

        Rational x = new Rational(1, -2);
        Rational y = new Rational(-1000000, 2000000);
        assertEquals(x, y);
        assertEquals(x.numerator(), y.numerator());
        assertEquals(x.denominator(), y.denominator());
    }

    @Test
    public void testEqualsBoundaries() {
        assertEquals(new Rational(Long.MAX_VALUE, Long.MAX_VALUE), new Rational(1, 1));

        // NOTE: Math.abs(Long.MIN_VALUE) == Long.MIN_VALUE... :-P
        assertEquals(new Rational(Long.MIN_VALUE + 1, Long.MIN_VALUE + 1), new Rational(1, 1));
        assertEquals(new Rational(Long.MIN_VALUE + 1, Long.MAX_VALUE), new Rational(-1, 1));
        assertEquals(new Rational(Long.MAX_VALUE, Long.MIN_VALUE + 1), new Rational(-1, 1));
    }
}
