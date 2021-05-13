/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

/**
 * PSD format constants.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSD.java,v 1.0 Apr 29, 2008 4:47:47 PM haraldk Exp$
 *
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml">Adobe Photoshop File Formats Specification</a>
 * @see <a href="http://www.fileformat.info/format/psd/egff.htm">Adobe Photoshop File Format Summary<a>
 */
interface PSD extends com.twelvemonkeys.imageio.metadata.psd.PSD {
    /** PSD 2+ Native format (.PSD) identifier "8BPS" */
    int SIGNATURE_8BPS = ('8' << 24) + ('B' << 16) + ('P' << 8) + 'S';

    // This is never used, it seems. Spec says (and sample files uses) 8BPS + version == 2 for PSB...
    //** PSD 5+ Large Document Format (.PSB) identifier "8BPB" */
    //int SIGNATURE_8BPB = ('8' << 24) + ('B' << 16) + ('P' << 8) + 'B';

    int VERSION_PSD = 1;
    int VERSION_PSB = 2;

    int RESOURCE_TYPE_LONG = ('8' << 24) + ('B' << 16) + ('6' << 8) + '4';

    // Blending modes
    /** Pass through blending mode "pass"*/
    int BLEND_PASS = ('p' << 24) + ('a' << 16) + ('s' << 8) + 's';

    /** Normal blending mode "norm"*/
    int BLEND_NORM = ('n' << 24) + ('o' << 16) + ('r' << 8) + 'm';

    /** Darken blending mode "dark" */
    int BLEND_DARK = ('d' << 24) + ('a' << 16) + ('r' << 8) + 'k';

    /** Lighten blending mode "lite" */
    int BLEND_LITE = ('l' << 24) + ('i' << 16) + ('t' << 8) + 'e';

    /** Hue blending mode "hue " */
    int BLEND_HUE = ('h' << 24) + ('u' << 16) + ('e' << 8) + ' ';

    /** Saturation blending mode "sat " */
    int BLEND_SAT = ('s' << 24) + ('a' << 16) + ('t' << 8) + ' ';

    /** Color blending mode "colr" */
    int BLEND_COLR = ('c' << 24) + ('o' << 16) + ('l' << 8) + 'r';

    /** Luminosity blending mode "lum " */
    int BLEND_LUM = ('l' << 24) + ('u' << 16) + ('m' << 8) + ' ';

    /** Multiply blending mode "mul " */
    int BLEND_MUL = ('m' << 24) + ('u' << 16) + ('l' << 8) + ' ';

    /** Screen blending mode "scrn" */
    int BLEND_SCRN = ('s' << 24) + ('c' << 16) + ('r' << 8) + 'n';

    /** Dissolve blending mode "diss" */
    int BLEND_DISS = ('d' << 24) + ('i' << 16) + ('s' << 8) + 's';

    /** Overlay blending mode "over" */
    int BLEND_OVER = ('o' << 24) + ('v' << 16) + ('e' << 8) + 'r';

    /** Hard light blending mode "hLit" */
    int BLEND_HLIT = ('h' << 24) + ('L' << 16) + ('i' << 8) + 't';

    /** Soft light blending mode "sLit" */
    int BLEND_SLIT = ('s' << 24) + ('L' << 16) + ('i' << 8) + 't';

    /** Difference blending mode "diff" */
    int BLEND_DIFF = ('d' << 24) + ('i' << 16) + ('f' << 8) + 'f';

    /** Color burn blending mode "idiv" */
    int BLEND_IDIV = ('i' << 24) + ('d' << 16) + ('i' << 8) + 'v';

    /** Linear burn blending mode "lbrn" */
    int BLEND_LBRN = ('l' << 24) + ('b' << 16) + ('r' << 8) + 'n';

    /** Darker color blending mode "dkCl" */
    int BLEND_DKCL = ('d' << 24) + ('k' << 16) + ('C' << 8) + 'l';

    /** Color dodge blending mode "div " */
    int BLEND_DIV = ('d' << 24) + ('i' << 16) + ('v' << 8) + ' ';

    /** Linear dodge blending mode "lddg" */
    int BLEND_LDDG = ('l' << 24) + ('d' << 16) + ('d' << 8) + 'g';

    /** Lighter color blending mode "lgCl" */
    int BLEND_LGCL = ('l' << 24) + ('g' << 16) + ('C' << 8) + 'l';

    /** Vivid light blending mode "vLit" */
    int BLEND_VLIT = ('v' << 24) + ('L' << 16) + ('i' << 8) + 't';

    /** Linear light blending mode "lLit" */
    int BLEND_LLIT = ('l' << 24) + ('L' << 16) + ('i' << 8) + 't';

    /** Pin light blending mode "pLit" */
    int BLEND_PLIT = ('p' << 24) + ('L' << 16) + ('i' << 8) + 't';

    /** Hard mix blending mode "hMix" */
    int BLEND_HMIX = ('h' << 24) + ('M' << 16) + ('i' << 8) + 'x';

    /** Exclusion blending mode "smud" */
    int BLEND_SMUD = ('s' << 24) + ('m' << 16) + ('u' << 8) + 'd';

    /** Subtract blending mode "fsub" */
    int BLEND_FSUB = ('f' << 24) + ('s' << 16) + ('u' << 8) + 'b';

    /** Divide blending mode "fdiv" */
    int BLEND_FDIV = ('f' << 24) + ('d' << 16) + ('i' << 8) + 'v';

    // Compression modes
    /** No compression */
    int COMPRESSION_NONE = 0;

    /** PacBits RLE compression */
    int COMPRESSION_RLE = 1;
    
    /** ZIP compression */
    int COMPRESSION_ZIP = 2;

    /** ZIP compression with prediction */
    int COMPRESSION_ZIP_PREDICTION = 3;

    // Color Modes
    /** Bitmap (monochrome) */
    short COLOR_MODE_BITMAP = 0;

    /** Gray-scale */
    short COLOR_MODE_GRAYSCALE = 1;

    /** Indexed color (palette color) */
    short COLOR_MODE_INDEXED = 2;

    /** RGB color */
    short COLOR_MODE_RGB = 3;

    /** CMYK color */
    short COLOR_MODE_CMYK = 4;

    /** Multichannel color */
    short COLOR_MODE_MULTICHANNEL = 7;

    /** Duotone (halftone) */
    short COLOR_MODE_DUOTONE = 8;

    /** Lab color */
    short COLOR_MODE_LAB = 9;

    // TODO: Consider moving these constants to PSDImageResource
    //    ID values 03e8, 03eb, 03ff, and 0403 are considered obsolete. Values 03e8 and 03eb are associated with
    // Photoshop v2.0. The data format for values 03f2, 03f4-03fa, 03fc, 03fd, 0405-0bb7 is intentionally not
    // documented by Adobe, or the data is missing.
    //  Please refer to the Adobe Photoshop SDK for information on obtaining the IPTC-NAA record 2 structure definition.
    //    WORD[5]
    /** Channels, rows, columns, depth, and mode (Obsolete?Photoshop 2.0 only). */
    int RES_CHANNELS_ROWS_COLUMNS_DEPTH_MODE = 0x03e8;

    /** Optional Macintosh print manager information. */
    int RES_MAC_PRINT_MANAGER_INFO = 0x03e9;

    /** Indexed color table (Obsolete?Photoshop 2.0 only). */
    int RES_INDEXED_COLOR_TABLE = 0x03eb;

    /** Resolution information
    //    ID value 03ed indicates that the data is in the form of a ResolutionInfo structure:
    //
    //    typedef struct _ResolutionInfo
    //    {
    //       LONG hRes;              // Fixed-point number: pixels per inch
    //       WORD hResUnit;          // 1=pixels per inch, 2=pixels per centimeter
    //       WORD WidthUnit;         // 1=in, 2=cm, 3=pt, 4=picas, 5=columns
    //       LONG vRes;              // Fixed-point number: pixels per inch/
    //       WORD vResUnit;          // 1=pixels per inch, 2=pixels per centimeter
    //       WORD HeightUnit;        // 1=in, 2=cm, 3=pt, 4=picas, 5=columns
    //    } RESOLUTIONINFO;
    */
    int RES_RESOLUTION_INFO = 0x3ed;

    /** Alpha channel names (Pascal-format strings) */
    int RES_ALPHA_CHANNEL_INFO = 0x3ee;

    /** Display information for each channel
    //    ID value 03ef indicates that the data is stored as a DisplayInfo structure, which contains display information
    // associated with each channel:
    //
    //    typedef _DisplayInfo
    //    {
    //       WORD  ColorSpace;
    //       WORD  Color[4];
    //       WORD  Opacity;          // 0-100
    //       BYTE  Kind;             // 0=selected, 1=protected
    //       BYTE  Padding;          // Always zero
    //    } DISPLAYINFO;
    //
    */
    int RES_DISPLAY_INFO = 0x3ef;

    //    03f0
    //    BYTE[]
    /** Optional Pascal-format caption string */
    int RES_CAPTION = 0x03f0;

    //    03f1
    //    LONG, WORD
    /** Fixed-point border width, border units */
    int RES_BORDER_WIDTH = 0x03f1;

    //    03f2
    /** Background color */
    // 2 byte Color space: 0 = RGB (unsigned 16 bit), 1 = HSB (unsigned 16 bit), 2 = CMYK (unsigned 16 bit),
    //                     3 = Pantone matching system (undocumented), 4 = Focoltone colour system (undocumented),
    //                     5 = Truematch color (undocumented), 6 = Toyo 88 colorfinder 1050 (undocumented),
    //                     7 = Lab (lighntess 0...10000, chrominance -12800..127000, 8 = Grayscale 0...10000,
    //                     10 = HKS colors
    // 8 byte Color data: 6 first bytes used for RGB, HSB and Lab, all 8 for CMYK and only first two for grayscale
    int RES_BACKGROUND_COLOR = 0x03f2;

    //    03f3
    //    BYTE[8]
    /**
     * Print flags.
     * ID value 03f3 indicates that the data is a series of eight flags, indicating the enabled state of labels,
     * crop marks, color bars, registration marks, negative, flip, interpolate, and caption items in the
     * Photoshop Page Setup dialog box.
     */
    int RES_PRINT_FLAGS = 0x03f3;

    //    03f4
    /** Gray-scale and halftoning information */
    int RES_GRAYSCALE_HALFTONE_INFO = 0x03f4;

    //    03f5
    /** Color halftoning information */
    int RES_COLOR_HALFTONE_INFO = 0x03f5;

    //    03f6
    /** Duotone halftoning information */
    int RES_DUOTONE_HALFTONE_INFO = 0x03f6;

    //    03f7
    /** Gray-scale and multichannel transfer function */
    int RES_GRAYSCALE_MULTICHANNEL_TRANSFER_FUNCTION = 0x03f7;

    //    03f8
    /** Color transfer functions */
    int RES_COLOR_TRANSFER_FUNCTION = 0x03f8;

    //    03f9
    /** Duotone transfer functions */
    int RES_DUOTONE_TRANSFER_FUNCTOON = 0x03f9;

    //    03fa
    /** Duotone image information */
    int RES_DUOTONE_IMAGE_INFO = 0x03fa;

    //    03fb
    //    BYTE[2]
    /** Effective black and white value for dot range */
    int RES_EFFECTIVE_BLACK_WHITE = 0x03fb;

    //    03fc
    /** Obsolete undocumented resource. */
    int RES_03FC = 0x03fc;

    //    03fd
    /** EPS options */
    int RES_EPS_OPTIONS = 0x03fd;

    //    03fe
    //    WORD, BYTE
    /** Quick Mask channel ID, flag for mask initially empty */
    int RES_QUICK_MASK_CHANNEL_ID = 0x03fe;

    //    03ff
    /** Obsolete undocumented resource. */
    int RES_03ff = 0x03ff;

    //    0400
    //    WORD
    /** Index of target layer (0=bottom)*/
    int RES_INDEX_OF_TARGET_LAYER = 0x0400;

    //    0401
    /** Working path */
    int RES_WORKING_PATH = 0x0401;

    //    0402
    //    WORD[]
    /** Layers group info, group ID for dragging groups */
    int RES_LAYERS_GROUP_INFO = 0x0402;

    //
    //    0403
    /** Obsolete undocumented resource. */
    int RES_0403 = 0x0403;

    //    0404
    /** IPTC-NAA record */
    int RES_IPTC_NAA = 0x0404;

    //    0405
    /** Image mode for raw-format files */
    int RES_RAW_IMAGE_MODE = 0x0405;

    //    0406
    /** JPEG quality (Adobe internal) */
    int RES_JPEG_QUALITY = 0x0406;

    //    1032
    /** (Photoshop 4.0) Grid and guides information */
    int RES_GRID_AND_GUIDES_INFO = 0x0408;

    //    1033
    /**
     * (Photoshop 4.0) Thumbnail resource for Photoshop 4.0 only. BGR layout. Obsolete.
     * @see #RES_THUMBNAIL
     */
    int RES_THUMBNAIL_PS4 = 0x0409;

    //    1034
    /**
     * (Photoshop 4.0) Copyright flag
     * Boolean indicating whether image is copyrighted. Can be set via
     * Property suite or by user in File Info...
     */
    int RES_COPYRIGHT_FLAG = 0x040A;

    //    1035
    /**
     * (Photoshop 4.0) URL
     * Handle of a text string with uniform resource locator. Can be set via
     * Property suite or by user in File Info...
     */
    int RES_URL = 0x040B;

    //    1036
    /** (Photoshop 5.0) Thumbnail resource (supersedes resource 1033) */
    int RES_THUMBNAIL = 0x040C;

    //    1037
    /**
     * (Photoshop 5.0) Global Angle
     * 4 bytes that contain an integer between 0 and 359, which is the global
     * lighting angle for effects layer. If not present, assumed to be 30.
     */
    int RES_GLOBAL_ANGLE = 0x040D;

    //    1038
    /**
     * (Photoshop 5.0) Color samplers resource
     * See "Color samplers resource format" on page20.
     */
    int RES_COLOR_SAMPLERS = 0x040E;

    /**
     * (Photoshop 5.0) ICC Profile
     * The raw bytes of an ICC (International Color Consortium) format profile.
     */
    int RES_ICC_PROFILE = 0x040f;

    // 1040
    /**
     * (Photoshop 5.0) Watermark
     * One byte.
     */
    int RES_WATERMARK = 0x0410;

    // 1041
    /**
     * (Photoshop 5.0) ICC Untagged Profile
     * 1 byte that disables any assumed profile handling when opening the file.
     * 1 = intentionally untagged.
     */
    int RES_ICC_UNTAGGED_PROFILE = 0x0411;

    // 1042
    /**
     * (Photoshop 5.0) Effects visible
     * 1-byte global flag to show/hide all the effects layer. Only present when
     * they are hidden.
     */
    int RES_EFFECTS_VISIBLE = 0x0412;

    // 1043
    /**
     * (Photoshop 5.0) Spot Halftone
     * 4 bytes for version, 4 bytes for length, and the variable length data.
     */
    int RES_SPOT_HALFTONE = 0x0413;

    // 1044
    /**
     * (Photoshop 5.0) Document-specific IDs seed number
     * 4 bytes: Base value, starting at which layer IDs will be generated (or a
     * greater value if existing IDs already exceed it). Its purpose is to avoid the
     * case where we add layers, flatten, save, open, and then add more layers
     * that end up with the same IDs as the first set.
     */
    int RES_DOC_ID_SEED = 0x0414;

    // 1045
    /**
     * (Photoshop 5.0) Unicode Alpha Names
     * Unicode string (4 bytes length followed by string).
     */
    int RES_UNICODE_ALPHA_NAMES = 0x0415;

    // 1046
    /**
     * (Photoshop 6.0) Indexed Color Table Count
     * 2 bytes for the number of colors in table that are actually defined
     */
    int RES_INDEXED_COLOR_TABLE_COUNT = 0x0416;

    //1047
    /**
     * (Photoshop 6.0) Transparency Index.
     * 2 bytes for the index of transparent color, if any.
     */
    int RES_TRANSPARENCY_INDEX = 0x0417;

    //1049
    /**
     * (Photoshop 6.0) Global Altitude
     * 4 byte entry for altitude
     */
    int RES_GLOBAL_ALTITUDE = 0x0419;

    //1050
    /**
     * (Photoshop 6.0) Slices
     */
    int RES_SLICES = 0x041A;

    //1051
    /**
     * (Photoshop 6.0) Workflow URL
     * Unicode string
     */
    int RES_WORKFLOW_URL = 0x041B;

    // 1052
    /**
     * (Photoshop 6.0) Jump To XPEP
     * 2 bytes major version, 2 bytes minor version, 4 bytes count. Following is
     * repeated for count: 4 bytes block size, 4 bytes key, if key = 'jtDd', then
     * next is a Boolean for the dirty flag; otherwise its a 4 byte entry for the
     * mod date.
     */
    int RES_JUMP_TO_XPEP = 0x041C;

    // 1053
    /**
     * (Photoshop 6.0) Alpha Identifiers
     * 4 bytes of length, followed by 4 bytes each for every alpha identifier.
     */
    int RES_ALPHA_IDENTIFIERS = 0x041D;

    // 1054
    /**
     * (Photoshop 6.0) URL List
     * 4 byte count of URLs, followed by 4 byte long, 4 byte ID, and Unicode
     * string for each count.
     */
    int RES_URL_LIST = 0x041E;

    // 1057
    /**
     * (Photoshop 6.0) Version Info
     * 4 bytes version, 1 byte hasRealMergedData, Unicode string: writer
     * name, Unicode string: reader name, 4 bytes file version.
     */
    int RES_VERSION_INFO = 0x0421;

    // 1058
    /**
     * (Photoshop 7.0) EXIF data 1
     *
     * @see <a href="http://www.pima.net/standards/it10/PIMA15740/exif.htm">EXIF standard</a>
     */
    int RES_EXIF_DATA_1 = 0x0422;

    //1059
    /**
     * (Photoshop 7.0) EXIF data 3
     *
     * @see <a href="http://www.pima.net/standards/it10/PIMA15740/exif.htm">EXIF standard</a>
     */
    int RES_EXIF_DATA_3 = 0x0423;

    //1060
    /**
     * (Photoshop 7.0) XMP metadata
     * File info as XML description.
     *
     * @see <a href="http://Partners.adobe.com/asn/developer/xmp/main.html">XMP standard</a>
     */
    int RES_XMP_DATA = 0x0424;

    // 1061
    /**
     * (Photoshop 7.0) Caption digest
     * 16 bytes: RSA Data Security, MD5 message-digest algorithm
     */
    int RES_CAPTION_DIGEST = 0x0425;

    // 1062
    /**
     * (Photoshop 7.0) Print scale
     * 2 bytes style (0 = centered, 1 = size to fit, 2 = user defined). 4 bytes x
     * location (floating point). 4 bytes y location (floating point). 4 bytes scale
     * (floating point)
     */
    int RES_PRINT_SCALE = 0x0426;

    // 1064
    /**
     * (Photoshop CS) Pixel Aspect Ratio
     * 4 bytes (version = 1), 8 bytes double, x / y of a pixel
     */
    int RES_PIXEL_ASPECT_RATIO = 0x0428;


    // 1065
    /**
     * (Photoshop CS) Layer Comps
     * 4 bytes (descriptor version = 16), Descriptor.
     */
    int RES_LAYER_COMPS = 0x0429;

    // 1066
    /**
     * (Photoshop CS) Alternate Duotone Colors
     * 2 bytes (version = 1), 2 bytes count, following is repeated for each count:
     * [ Color: 2 bytes for space followed by 4 * 2 byte color component ],
     * following this is another 2 byte count, usually 256, followed by Lab colors
     * one byte each for L, a, b
     * This resource is not read or used by Photoshop.
     */
    int RES_ALTERNATE_DUOTONE_COLORS = 0x042A;

    // 1067
    /**
     * (Photoshop CS) Alternate Spot Colors
     * 2 bytes (version = 1), 2 bytes channel count, following is repeated for
     * each count: 4 bytes channel ID, Color: 2 bytes for space followed by 4 * 2
     * byte color component
     * This resource is not read or used by Photoshop.
     */
    int RES_ALTERNATE_SPOT_COLORS = 0x042B;

    /**
     * (Photoshop CS2) Layer Selection ID(s).
     * 2 bytes count, following is repeated for each count: 4 bytes layer ID.
     */
    int RES_LAYER_SELECTION_IDS = 0x042D;

    /**
     * (Photoshop CS2) HDR Toning information
     */
    int RES_HDR_TONING_INFO = 0x042E;

    /**
     * (Photoshop CS2) Print info
     */
    int RES_PRINT_INFO = 0x042F;

    /**
     * (Photoshop CS2) Layer Group(s) Enabled ID.
     * 1 byte for each layer in the document, repeated by length of the resource.
     * NOTE: Layer groups have start and end markers.
     */
    int RES_LAYER_GROUPS_ENABLED = 0x0430;

    /**
     * (Photoshop CS3) Color samplers resource.
     * Also see ID 1038 for old format.
     * See Color samplers resource format.
     */
    int RES_COLOR_SAMPLERS_RESOURCE = 0x0431;

    /**
     * (Photoshop CS3) Measurement Scale.
     * 4 bytes (descriptor version = 16), Descriptor (see Descriptor structure)
     */
    int RES_MEASUREMENT_SCALE = 0x0432;

    /**
     * (Photoshop CS3) Timeline Information.
     * 4 bytes (descriptor version = 16), Descriptor (see Descriptor structure)
     */
    int RES_TIMELINE_INFO = 0x0433;

    /**
     * (Photoshop CS3) Sheet Disclosure.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure)
     */
    int RES_SHEET_DISCLOSURE = 0x0434;

    /**
     * (Photoshop CS3) DisplayInfo structure to support floating point colors.
     * Also see ID 1007. See Appendix A in Photoshop API Guide.pdf .
     */
    int RES_DISPLAY_INFO_FP = 0x0435;

    /**
     * (Photoshop CS3) Onion Skins.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure)
     */
    int RES_ONION_SKINS = 0x0436;

    /**
     * (Photoshop CS4) Count Information.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure).
     * Information about the count in the document. See the Count Tool.
     */
    int RES_COUNT_INFO = 0x0438;

    /**
     * (Photoshop CS5) Print Information.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure).
     * Information about the current print settings in the document. The color management options.
     */
    int RES_PRINT_INFO_CMM = 0x043A;

    /**
     * (Photoshop CS5) Print Style.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure).
     * Information about the current print style in the document. The printing marks, labels, ornaments, etc.
     */
    int RES_PRINT_STYLE = 0x043B;

    /**
     * (Photoshop CC) Path Selection State.
     * 4 bytes (descriptor version = 16), Descriptor (see See Descriptor structure).
     * Information about the current path selection state.
     */
    int RES_PATH_SELECTION_STATE = 0x0440;

    //    07d0-0bb6
    /* Saved path information */

    //    0bb7
    /** Clipping path name */
    int RES_CLIPPING_PATH_NAME = 0x0bb7;

    //    2710
    /** Print flags information
     *    ID value 2710 signals that the Data section contains a WORD-length version number (should be 1),
     * a BYTE-length flag indicating crop marks, a BYTE-length field (should be 0), a LONG-length bleed width value, and a
     * WORD indicating the bleed width scale.
     */
    int RES_PRINT_FLAGS_INFORMATION = 0x2710;

    int RES_PATH_INFO_MAX = 0x0bb6;
    int RES_PATH_INFO_MIN = 0x07d0;

    /** Plug-In resource(s). Resources added by a plug-in. See the plug-in API found in the SDK documentation */
    int RES_PLUGIN_MIN = 0x0fa0;

    /** Plug-In resource(s). Resources added by a plug-in. See the plug-in API found in the SDK documentation */
    int RES_PLUGIN_MAX = 0x1387;

    // TODO: Better naming of these.. It's a kind of resource blocks as well..
    // "Additional Layer Information"
    int LMsk = 'L' << 24 | 'M' << 16 | 's' << 8 | 'k';
    int Lr16 = 'L' << 24 | 'r' << 16 | '1' << 8 | '6';
    int Lr32 = 'L' << 24 | 'r' << 16 | '3' << 8 | '2';
    int Layr = 'L' << 24 | 'a' << 16 | 'y' << 8 | 'r';
    int Mt16 = 'M' << 24 | 't' << 16 | '1' << 8 | '6';
    int Mt32 = 'M' << 24 | 't' << 16 | '3' << 8 | '2';
    int Mtrn = 'M' << 24 | 't' << 16 | 'r' << 8 | 'n';
    int Alph = 'A' << 24 | 'l' << 16 | 'p' << 8 | 'h';
    int FMsk = 'F' << 24 | 'M' << 16 | 's' << 8 | 'k';
    int lnk2 = 'l' << 24 | 'n' << 16 | 'k' << 8 | '2';
    int FEid = 'F' << 24 | 'E' << 16 | 'i' << 8 | 'd';
    int FXid = 'F' << 24 | 'X' << 16 | 'i' << 8 | 'd';
    int PxSD = 'P' << 24 | 'x' << 16 | 'S' << 8 | 'D';
    int luni = 'l' << 24 | 'u' << 16 | 'n' << 8 | 'i';
    int lyid = 'l' << 24 | 'y' << 16 | 'i' << 8 | 'd';
}
