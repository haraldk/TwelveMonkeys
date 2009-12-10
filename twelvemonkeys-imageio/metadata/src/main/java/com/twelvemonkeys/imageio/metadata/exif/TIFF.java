/*
 * Copyright (c) 2009, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

    /// A. Tags relating to image data structure:

    int TAG_IMAGE_WIDTH = 256;
    int TAG_IMAGE_HEIGHT = 257;
    int TAG_BITS_PER_SAMPLE = 258;
    int TAG_COMPRESSION = 259;
    int TAG_PHOTOMETRIC_INTERPRETATION = 262;
    int TAG_ORIENTATION = 274;
    int TAG_SAMPLES_PER_PIXELS = 277;
    int TAG_PLANAR_CONFIGURATION = 284;
    int TAG_YCBCR_SUB_SAMPLING = 530;
    int TAG_YCBCR_POSITIONING = 531;
    int TAG_X_RESOLUTION = 282;
    int TAG_Y_RESOLUTION = 283;
    int TAG_RESOLUTION_UNIT = 296;

    /// B. Tags relating to recording offset

    int TAG_STRIP_OFFSETS = 273;
    int TAG_ROWS_PER_STRIP = 278;
    int TAG_STRIP_BYTE_COUNTS = 279;
    int TAG_JPEG_INTERCHANGE_FORMAT = 513;
    int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;

    /// C. Tags relating to image data characteristics

    int TAG_TRANSFER_FUNCTION = 301;
    int TAG_WHITE_POINT = 318;
    int TAG_PRIMARY_CHROMATICITIES = 319;
    int TAG_YCBCR_COEFFICIENTS = 529;
    int TAG_REFERENCE_BLACK_WHITE = 532;

    /// D. Other tags

    int TAG_DATE_TIME = 306;
    int TAG_IMAGE_DESCRIPTION = 270;
    int TAG_MAKE = 271;
    int TAG_MODEL = 272;
    int TAG_SOFTWARE = 305;
    int TAG_ARTIST = 315;
    int TAG_COPYRIGHT = 33432;
}
