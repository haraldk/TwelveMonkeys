package com.twelvemonkeys.imageio.metadata.exif;

/**
 * TIFF
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFF.java,v 1.0 Nov 15, 2009 3:02:24 PM haraldk Exp$
 */
public interface TIFF {
    int TIFF_MAGIC = 42;

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
    String[] TYPE_NAMES = {
            "BYTE", "ASCII", "SHORT", "LONG", "RATIONAL",

            "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "FLOAT", "DOUBLE",
    };
    int[] TYPE_LENGTHS = {
            1, 1, 2, 4, 8,

            1, 1, 2, 4, 8, 4, 8,
    };

    int IFD_EXIF = 0x8769;
    int IFD_GPS = 0x8825;
    int IFD_INTEROP = 0xA005;


    int TAG_SOFTWARE = 305;
    int TAG_DATE_TIME = 306;
    int TAG_ARTIST = 315;
    int TAG_COPYRIGHT = 33432;
}
