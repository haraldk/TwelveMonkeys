/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.tiff;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RationalTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RationalTest.java,v 1.0 Nov 18, 2009 3:23:17 PM haraldk Exp$
 */
public class RationalTest {
    @Test
    public void testZeroDenominator() {
        assertThrows(IllegalArgumentException.class, () -> new Rational(1, 0));
    }

    // TODO: Find a solution to this problem, as we should be able to work with it...
    @Test
    public void testLongMinValueNumerator() {
        assertThrows(IllegalArgumentException.class, () -> new Rational(Long.MIN_VALUE, 1));
    }

    @Test
    public void testLongMinValueDenominator() {
        assertThrows(IllegalArgumentException.class, () -> new Rational(1, Long.MIN_VALUE));
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

    @Test
    public void testReciprocal() {
        assertEquals(new Rational(1, 99), new Rational(99, 1).reciprocal());
        assertEquals(new Rational(-1, 1234567), new Rational(-1234567, 1).reciprocal());
    }

    @Test
    public void testNegate() {
        assertEquals(new Rational(-1, 99), new Rational(1, 99).negate());
        assertEquals(new Rational(1, 1234567), new Rational(1, -1234567).negate());
    }

    @Test
    public void testPlus() {
        Rational x, y;

        // 1/2 + 1/3 = 5/6
        x = new Rational(1, 2);
        y = new Rational(1, 3);
        assertEquals(new Rational(5, 6), x.plus(y));

        // 8/9 + 1/9 = 1
        x = new Rational(8, 9);
        y = new Rational(1, 9);
        assertEquals(new Rational(1, 1), x.plus(y));

        // 1/200000000 + 1/300000000 = 1/120000000
        x = new Rational(1, 200000000);
        y = new Rational(1, 300000000);
        assertEquals(new Rational(1, 120000000), x.plus(y));

        // 1073741789/20 + 1073741789/30 = 1073741789/12
        x = new Rational(1073741789, 20);
        y = new Rational(1073741789, 30);
        assertEquals(new Rational(1073741789, 12), x.plus(y));

        // x + 0 = x
        assertEquals(x, x.plus(Rational.ZERO));
    }

    @Test
    public void testTimes() {
        Rational x, y;

        //  4/17 * 17/4 = 1
        x = new Rational(4, 17);
        y = new Rational(17, 4);
        assertEquals(new Rational(1, 1), x.times(y));

        // 3037141/3247033 * 3037547/3246599 = 841/961
        x = new Rational(3037141, 3247033);
        y = new Rational(3037547, 3246599);
        assertEquals(new Rational(841, 961), x.times(y));

        // x * 0 = 0
        assertEquals(Rational.ZERO, x.times(Rational.ZERO));
    }

    @Test
    public void testMinus() {
        // 1/6 - -4/-8 = -1/3
        Rational x = new Rational(1, 6);
        Rational y = new Rational(-4, -8);
        assertEquals(new Rational(-1, 3), x.minus(y));

        // x - 0 = x
        assertEquals(x, x.minus(Rational.ZERO));
    }

    @Test
    public void testDivides() {
        // 3037141/3247033 / 3246599/3037547 = 841/961
        Rational x = new Rational(3037141, 3247033);
        Rational y = new Rational(3246599, 3037547);
        assertEquals(new Rational(841, 961), x.divides(y));

        // 0 / x = 0
        assertEquals(Rational.ZERO, new Rational(0, 386).divides(x));
    }

    @Test
    public void testDivideZero() {
        assertThrows(ArithmeticException.class, () -> new Rational(3037141, 3247033).divides(new Rational(0, 1)));
    }
}
