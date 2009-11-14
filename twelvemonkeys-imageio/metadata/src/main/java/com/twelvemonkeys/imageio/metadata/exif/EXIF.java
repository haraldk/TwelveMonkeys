package com.twelvemonkeys.imageio.metadata.exif;

/**
 * EXIF
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIF.java,v 1.0 Nov 11, 2009 5:36:04 PM haraldk Exp$
 */
interface EXIF {
    /*
    1 = BYTE 8-bit unsigned integer.
    2 = ASCII 8-bit byte that contains a 7-bit ASCII code; the last byte
    must be NUL (binary zero).
    3 = SHORT 16-bit (2-byte) unsigned integer.
    4 = LONG 32-bit (4-byte) unsigned integer.
    5 = RATIONAL Two LONGs:  the first represents the numerator of a
    fraction; the second, the denominator.

     TIFF 6.0 and above:
    6 = SBYTE An 8-bit signed (twos-complement) integer.
    7 = UNDEFINED An 8-bit byte that may contain anything, depending on
    the definition of the field.
    8 = SSHORT A 16-bit (2-byte) signed (twos-complement) integer.
    9 = SLONG A 32-bit (4-byte) signed (twos-complement) integer.
    10 = SRATIONAL Two SLONGs:  the first represents the numerator of a
    fraction, the second the denominator.
    11 = FLOAT Single precision (4-byte) IEEE format.
    12 = DOUBLE Double precision (8-byte) IEEE format.
     */
    
    static int EXIF_IFD = 0x8769;

    static String[] TYPE_NAMES = {
            "BYTE", "ASCII", "SHORT", "LONG", "RATIONAL",

            "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "FLOAT", "DOUBLE",
    };

    static int[] TYPE_LENGTHS = {
            1, 1, 2, 4, 8,

            1, 1, 2, 4, 8, 4, 8,
    };

    int TIFF_MAGIC = 42;
}
