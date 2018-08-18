/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.exif;

/**
 * Represents a rational number with a {@code long} numerator and {@code long} denominator.
 * Rational numbers are stored in reduced form with the sign stored with the numerator.
 * Rationals are immutable.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Rational.java,v 1.0 Nov 18, 2009 1:12:00 AM haraldk Exp$
 *
 * @deprecated Use com.twelvemonkeys.imageio.metadata.tiff.Rational instead.
 */
@SuppressWarnings("deprecation")
public final class Rational extends Number implements Comparable<Rational> {
    private final com.twelvemonkeys.imageio.metadata.tiff.Rational delegate;

    public Rational(final long pNumber) {
        this(new com.twelvemonkeys.imageio.metadata.tiff.Rational(pNumber, 1));
    }

    public Rational(final long pNumerator, final long pDenominator) {
        this(new com.twelvemonkeys.imageio.metadata.tiff.Rational(pNumerator, pDenominator));
    }

    private Rational(final com.twelvemonkeys.imageio.metadata.tiff.Rational delegate) {
        this.delegate = delegate;
    }

    public long numerator() {
        return delegate.numerator();
    }

    public long denominator() {
        return delegate.denominator();
    }

    @Override
    public byte byteValue() {
        return delegate.byteValue();
    }

    @Override
    public short shortValue() {
        return delegate.shortValue();
    }

    @Override
    public int intValue() {
        return delegate.intValue();
    }

    @Override
    public long longValue() {
        return delegate.longValue();
    }

    @Override
    public float floatValue() {
        return delegate.floatValue();
    }

    @Override
    public double doubleValue() {
        return delegate.doubleValue();
    }

    public int compareTo(Rational pOther) {
        return delegate.compareTo(pOther.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(final Object pOther) {
        return pOther == this || pOther instanceof Rational && delegate.equals(((Rational) pOther).delegate);

    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
