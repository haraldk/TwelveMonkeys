package com.twelvemonkeys.imageio.metadata.tiff;

/**
 * IEEE 754 half-precision floating point data type.
 *
 * @see <a href="https://stackoverflow.com/a/6162687/259991">Stack Overflow answer by x4u</a>
 * @see <a href="https://en.wikipedia.org/wiki/Half-precision_floating-point_format">Wikipedia</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Half.java,v 1.0 10/04/2021 haraldk Exp$
 */
public final class Half extends Number implements Comparable<Half> {
    // Short, Int, Long
    // Half, Float, Double :-)

    public static final int SIZE = 16;

    private final short shortBits;
    private final transient float floatValue;

    public Half(short shortBits) {
        this.shortBits = shortBits;
        this.floatValue = shortBitsToFloat(shortBits);
    }

    @Override
    public int intValue() {
        return (int) floatValue;
    }

    @Override
    public long longValue() {
        return (long) floatValue;
    }

    @Override
    public float floatValue() {
        return floatValue;
    }

    @Override
    public double doubleValue() {
        return floatValue;
    }

    public int hashCode() {
        return shortBits;
    }

    public boolean equals(Object other) {
        return (other instanceof Half)
                && ((Half) other).shortBits == shortBits;
    }

    @Override
    public int compareTo(final Half other) {
        return Float.compare(floatValue, other.floatValue);
    }

    @Override
    public String toString() {
        return Float.toString(floatValue);
    }

    public static Half valueOf(String value) throws NumberFormatException {
        return new Half(parseHalf(value));
    }

    public static short parseHalf(String value) throws NumberFormatException {
        return floatToShortBits(Float.parseFloat(value));
    }

    /**
     * Converts an IEEE 754 half-precision data type to single-precision.
     *
     * @param shortBits a 16 bit half precision value
     * @return an IEE 754 single precision float
     *
     */
    public static float shortBitsToFloat(final short shortBits) {
        int mantissa = shortBits & 0x03ff;         // 10 bits mantissa
        int exponent = shortBits & 0x7c00;         //  5 bits exponent

        if (exponent == 0x7c00) {                   // NaN/Inf
            exponent = 0x3fc00;                     // -> NaN/Inf
        }
        else if (exponent != 0) {                   // Normalized value
            exponent += 0x1c000;                    // exp - 15 + 127

            // Smooth transition
            if (mantissa == 0 && exponent > 0x1c400) {
                return Float.intBitsToFloat((shortBits & 0x8000) << 16 | exponent << 13 | 0x3ff);
            }
        }
        else if (mantissa != 0) {                   // && exp == 0 -> subnormal
            exponent = 0x1c400;                     // Make it normal

            do {
                mantissa <<= 1;                     // mantissa * 2
                exponent -= 0x400;                  // Decrease exp by 1
            } while ((mantissa & 0x400) == 0);      // while not normal

            mantissa &= 0x3ff;                      // Discard subnormal bit
        }                                           // else +/-0 -> +/-0

        // Combine all parts,  sign << (31 - 15), value << (23 - 10)
        return Float.intBitsToFloat((shortBits & 0x8000) << 16 | (exponent | mantissa) << 13);
    }

    /**
     * Converts a float value to IEEE 754 half-precision bits.
     *
     * @param floatValue a float value
     * @return the IEE 754 single precision 16 bits value
     *
     */
    public static short floatToShortBits(final float floatValue) {
        return (short) floatTo16Bits(floatValue);
    }

    private static int floatTo16Bits(final float floatValue) {
        int fbits = Float.floatToIntBits(floatValue);
        int sign = fbits >>> 16 & 0x8000;           // sign only
        int val = (fbits & 0x7fffffff) + 0x1000;    // rounded value

        if (val >= 0x47800000) {                    // might be or become NaN/Inf, avoid Inf due to rounding
            if ((fbits & 0x7fffffff) >= 0x47800000) {  // is or must become NaN/Inf
                if (val < 0x7f800000) {             // was value but too large
                    return sign | 0x7c00;           // make it +/-Inf
                }

                return sign | 0x7c00 |              // remains +/-Inf or NaN
                        (fbits & 0x007fffff) >>> 13;// keep NaN (and Inf) bits
            }

            return sign | 0x7bff;                   // unrounded not quite Inf
        }

        if (val >= 0x38800000) {                    // remains normalized value
            return sign | val - 0x38000000 >>> 13;  // exp - 127 + 15
        }

        if (val < 0x33000000) {                     // too small for subnormal
            return sign;                            // becomes +/-0
        }

        val = (fbits & 0x7fffffff) >>> 23;          // tmp exp for subnormal calc

        return sign | ((fbits & 0x7fffff | 0x800000)// add subnormal bit
                + (0x800000 >>> val - 102)          // round depending on cut off
                >>> 126 - val);                     // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
    }

    // Restores the floatValue on de-serialization
    private Object readResolve() {
        return new Half(shortBits);
    }
}
