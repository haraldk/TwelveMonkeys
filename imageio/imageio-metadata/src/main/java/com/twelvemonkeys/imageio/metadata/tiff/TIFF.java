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

package com.twelvemonkeys.imageio.metadata.tiff;

/**
 * TIFF
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFF.java,v 1.0 Nov 15, 2009 3:02:24 PM haraldk Exp$
 */
@SuppressWarnings("UnusedDeclaration")
public interface TIFF {
    short BYTE_ORDER_MARK_BIG_ENDIAN = ('M' << 8) | 'M';
    short BYTE_ORDER_MARK_LITTLE_ENDIAN = ('I' << 8) | 'I';

    int TIFF_MAGIC = 42;
    int BIGTIFF_MAGIC = 43;

    short TYPE_BYTE = 1;
    short TYPE_ASCII = 2;
    short TYPE_SHORT = 3;
    short TYPE_LONG = 4;
    short TYPE_RATIONAL = 5;

    short TYPE_SBYTE = 6;
    short TYPE_UNDEFINED = 7;
    short TYPE_SSHORT = 8;
    short TYPE_SLONG = 9;
    short TYPE_SRATIONAL = 10;
    short TYPE_FLOAT = 11;
    short TYPE_DOUBLE = 12;
    short TYPE_IFD = 13;
    // BigTIFF
    short TYPE_LONG8 = 16;
    short TYPE_SLONG8 = 17;
    short TYPE_IFD8 = 18;
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

    See http://www.awaresystems.be/imaging/tiff/tifftags/subifds.html
    13 = IFD, same as LONG

    TODO: BigTiff specifies more types
    See http://www.awaresystems.be/imaging/tiff/bigtiff.html, http://www.remotesensing.org/libtiff/bigtiffdesign.html
    (what about 14-15??)
    16 = TIFF_LONG8, being unsigned 8byte integer
    17 = TIFF_SLONG8, being signed 8byte integer
    18 = TIFF_IFD8, being a new unsigned 8byte IFD offset.
    Should probably all map to Java long (and fail if high bit is set for the unsigned types???)
     */
    String[] TYPE_NAMES = {
            null,
            "BYTE", "ASCII", "SHORT", "LONG", "RATIONAL",
            "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "FLOAT", "DOUBLE",
            "IFD",
            null, null,
            "LONG8", "SLONG8", "IFD8"
    };
    /** Length of the corresponding type, in bytes. */
    int[] TYPE_LENGTHS = {
            -1,
            1, 1, 2, 4, 8,
            1, 1, 2, 4, 8, 4, 8,
            4,
            -1, -1,
            8, 8, 8
    };

    /// EXIF defined TIFF tags

    int TAG_EXIF_IFD = 34665;
    int TAG_GPS_IFD = 34853;
    int TAG_INTEROP_IFD = 40965;

    /// A. Tags relating to image data structure:

    int TAG_IMAGE_WIDTH = 256;
    int TAG_IMAGE_HEIGHT = 257;
    int TAG_BITS_PER_SAMPLE = 258;
    int TAG_COMPRESSION = 259;
    int TAG_PHOTOMETRIC_INTERPRETATION = 262;
    int TAG_FILL_ORDER = 266;
    int TAG_ORIENTATION = 274;
    int TAG_SAMPLES_PER_PIXEL = 277;
    int TAG_PLANAR_CONFIGURATION = 284;
    int TAG_SAMPLE_FORMAT = 339;
    int TAG_YCBCR_SUB_SAMPLING = 530;
    int TAG_YCBCR_POSITIONING = 531;
    int TAG_X_RESOLUTION = 282;
    int TAG_Y_RESOLUTION = 283;
    int TAG_X_POSITION = 286;
    int TAG_Y_POSITION = 287;
    int TAG_RESOLUTION_UNIT = 296;

    /// B. Tags relating to recording offset

    int TAG_STRIP_OFFSETS = 273;
    int TAG_ROWS_PER_STRIP = 278;
    int TAG_STRIP_BYTE_COUNTS = 279;
    int TAG_FREE_OFFSETS = 288; // "Not recommended for general interchange."
    // "Old-style" JPEG (still used as EXIF thumbnail)
    int TAG_JPEG_INTERCHANGE_FORMAT = 513;
    int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;
    
    int TAG_GROUP3OPTIONS = 292;
    int TAG_GROUP4OPTIONS = 293;

    /// C. Tags relating to image data characteristics

    int TAG_TRANSFER_FUNCTION = 301;
    int TAG_PREDICTOR = 317;
    int TAG_WHITE_POINT = 318;
    int TAG_PRIMARY_CHROMATICITIES = 319;
    int TAG_COLOR_MAP = 320;
    int TAG_INK_SET = 332;
    int TAG_INK_NAMES = 333;
    int TAG_NUMBER_OF_INKS = 334;
    int TAG_EXTRA_SAMPLES = 338;
    int TAG_TRANSFER_RANGE = 342;
    int TAG_YCBCR_COEFFICIENTS = 529;
    int TAG_REFERENCE_BLACK_WHITE = 532;

    /// D. Other tags

    int TAG_DATE_TIME = 306;
    int TAG_DOCUMENT_NAME = 269;
    int TAG_IMAGE_DESCRIPTION = 270;
    int TAG_MAKE = 271;
    int TAG_MODEL = 272;
    int TAG_PAGE_NAME = 285;
    int TAG_PAGE_NUMBER = 297;
    int TAG_SOFTWARE = 305;
    int TAG_ARTIST = 315;
    int TAG_HOST_COMPUTER = 316;
    int TAG_COPYRIGHT = 33432;

    int TAG_SUBFILE_TYPE = 254;
    int TAG_OLD_SUBFILE_TYPE = 255; // Deprecated NO NOT WRITE!
    int TAG_SUB_IFD = 330;

    /**
     * XMP record.
     * @see com.twelvemonkeys.imageio.metadata.xmp.XMP
     */
    int TAG_XMP = 700;

    /**
     * IPTC record.
     * @see com.twelvemonkeys.imageio.metadata.iptc.IPTC
     */
    int TAG_IPTC = 33723;

    /**
     * Photoshop image resources.
     * @see com.twelvemonkeys.imageio.metadata.psd.PSD
     */
    int TAG_PHOTOSHOP = 34377;

    /**
     * Photoshop layer and mask information (byte order follows TIFF container).
     * Layer and mask information found in a typical layered Photoshop file.
     * Starts with a character string of "Adobe Photoshop Document Data Block"
     * (or "Adobe Photoshop Document Data V0002" for PSB)
     * including the null termination character.
     * @see com.twelvemonkeys.imageio.metadata.psd.PSD
     */
    int TAG_PHOTOSHOP_IMAGE_SOURCE_DATA = 37724;

    int TAG_PHOTOSHOP_ANNOTATIONS = 50255;

    /**
     * ICC Color Profile.
     * @see java.awt.color.ICC_Profile
     */
    int TAG_ICC_PROFILE = 34675;

    // Microsoft Office Document Imaging (MODI)
    // http://msdn.microsoft.com/en-us/library/aa167596%28office.11%29.aspx
    int TAG_MODI_BLC = 34718;
    int TAG_MODI_VECTOR = 34719;
    int TAG_MODI_PTC = 34720;

    // http://blogs.msdn.com/b/openspecification/archive/2009/12/08/details-of-three-tiff-tag-extensions-that-microsoft-office-document-imaging-modi-software-may-write-into-the-tiff-files-it-generates.aspx
    int TAG_MODI_PLAIN_TEXT = 37679;
    int TAG_MODI_OLE_PROPERTY_SET = 37680;
    int TAG_MODI_TEXT_POS_INFO = 37681;

    int TAG_TILE_WIDTH = 322;
    int TAG_TILE_HEIGTH = 323;
    int TAG_TILE_OFFSETS = 324;
    int TAG_TILE_BYTE_COUNTS = 325;

    // JPEG
    int TAG_JPEG_TABLES = 347;

    // "Old-style" JPEG (Obsolete) DO NOT WRITE!
    int TAG_OLD_JPEG_PROC = 512;
    int TAG_OLD_JPEG_Q_TABLES = 519;
    int TAG_OLD_JPEG_DC_TABLES = 520;
    int TAG_OLD_JPEG_AC_TABLES = 521;
}
