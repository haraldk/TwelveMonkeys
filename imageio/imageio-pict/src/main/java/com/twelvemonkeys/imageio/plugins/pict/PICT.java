/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pict;

/**
 * PICT format constants.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: PICT.java,v 1.0 06.apr.2006 12:53:17 haku Exp$
 */
interface PICT {
    /** PICT V1 identifier, two bytes, mask with 0xffff0000 */
    int MAGIC_V1 = 0x11010000;
    /** PICT V2 identifier, four bytes */
    int MAGIC_V2 = 0x001102ff;

    int PICT_NULL_HEADER_SIZE = 512;

    // V2 Header, -1 (int)
    int HEADER_V2 = 0xffffffff;
    // V2 Extended header, -2 (short) + reserved (short) 
    int HEADER_V2_EXT = 0xfffe0000;

    // PICT/QuickDraw uses 16 bit precision per color component internally
    int COLOR_COMP_SIZE = 2;

    /** Default Apple Macintosh DPI setting (72 DPI). */
    int MAC_DEFAULT_DPI = 72;

    /**
     * PICT opcodes.
     */
    int OP_HEADER_OP = 0x0C00;
    int NOP = 0x00;
    int OP_CLIP_RGN = 0x01;
    int OP_BK_PAT = 0x02;
    int OP_TX_FONT = 0x03;
    int OP_TX_FACE = 0x04;
    int OP_TX_MODE = 0x05;
    int OP_SP_EXTRA = 0x06;
    int OP_PN_SIZE = 0x07;
    int OP_PN_MODE = 0x08;
    int OP_PN_PAT = 0x09;
    int OP_FILL_PAT = 0x0A;
    int OP_OV_SIZE = 0x0B;
    int OP_ORIGIN = 0x0C;
    int OP_TX_SIZE = 0x0D;
    int OP_FG_COLOR = 0x0E;
    int OP_BK_COLOR = 0x0F;
    int OP_TX_RATIO = 0x10;
    int OP_VERSION = 0x11;
    /* Not implemented */
    int OP_BK_PIX_PAT = 0x12;
    int OP_PN_PIX_PAT = 0x13;
    int OP_FILL_PIX_PAT = 0x14;
    int OP_PN_LOC_H_FRAC = 0x15;
    int OP_CH_EXTRA = 0x16;
    int OP_RGB_FG_COL = 0x1A;
    int OP_RGB_BK_COL = 0x1B;
    int OP_HILITE_MODE = 0x1C;
    int OP_HILITE_COLOR = 0x1D;
    int OP_DEF_HILITE = 0x1E;
    int OP_OP_COLOR = 0x1F;
    int OP_LINE = 0x20;
    int OP_LINE_FROM = 0x21;
    int OP_SHORT_LINE = 0x22;
    int OP_SHORT_LINE_FROM = 0x23;
    int OP_LONG_TEXT = 0x28;
    int OP_DH_TEXT = 0x29;
    int OP_DV_TEXT = 0x2A;
    int OP_DHDV_TEXT = 0x2B;
    int OP_FONT_NAME = 0x2C;
    int OP_LINE_JUSTIFY = 0x2D;
    int OP_GLYPH_STATE = 0x2E;
    int OP_FRAME_RECT = 0x30;
    int OP_PAINT_RECT = 0x31;
    int OP_ERASE_RECT = 0x32;
    int OP_INVERT_RECT = 0x33;
    int OP_FILL_RECT = 0x34;
    int OP_FRAME_SAME_RECT = 0x38;
    int OP_PAINT_SAME_RECT = 0x39;
    int OP_ERASE_SAME_RECT = 0x3A;
    int OP_INVERT_SAME_RECT = 0x3B;
    int OP_FILL_SAME_RECT = 0x3C;
    int OP_FRAME_R_RECT = 0x40;
    int OP_PAINT_R_RECT = 0x41;
    int OP_ERASE_R_RECT = 0x42;
    int OP_INVERT_R_RECT = 0x43;
    int OP_FILL_R_RECT = 0x44;
    int OP_FRAME_SAME_R_RECT = 0x48;
    int OP_PAINT_SAME_R_RECT = 0x49;
    int OP_ERASE_SAME_R_RECT = 0x4A;
    int OP_INVERT_SAME_R_RECT = 0x4B;
    int OP_FILL_SAME_R_RECT = 0x4C;
    int OP_FRAME_OVAL = 0x50;
    int OP_PAINT_OVAL = 0x51;
    int OP_ERASE_OVAL = 0x52;
    int OP_INVERT_OVAL = 0x53;
    int OP_FILL_OVAL = 0x54;
    int OP_FRAME_SAME_OVAL = 0x58;
    int OP_PAINT_SAME_OVAL = 0x59;
    int OP_ERASE_SAME_OVAL = 0x5A;
    int OP_INVERT_SAME_OVAL = 0x5B;
    int OP_FILL_SAME_OVAL = 0x5C;
    int OP_FRAME_ARC = 0x60;
    int OP_PAINT_ARC = 0x61;
    int OP_ERASE_ARC = 0x62;
    int OP_INVERT_ARC = 0x63;
    int OP_FILL_ARC = 0x64;
    int OP_FRAME_SAME_ARC = 0x68;
    int OP_PAINT_SAME_ARC = 0x69;
    int OP_ERASE_SAME_ARC = 0x6A;
    int OP_INVERT_SAME_ARC = 0x6B;
    int OP_FILL_SAME_ARC = 0x6C;
    int OP_FRAME_POLY = 0x70;
    int OP_PAINT_POLY = 0x71;
    int OP_ERASE_POLY = 0x72;
    int OP_INVERT_POLY = 0x73;
    int OP_FILL_POLY = 0x74;
    int OP_FRAME_SAME_POLY = 0x78;
    int OP_PAINT_SAME_POLY = 0x79;
    int OP_ERASE_SAME_POLY = 0x7A;
    int OP_INVERT_SAME_POLY = 0x7B;
    int OP_FILL_SAME_POLY = 0x7C;
    int OP_FRAME_RGN = 0x80;
    int OP_PAINT_RGN = 0x81;
    int OP_ERASE_RGN = 0x82;
    int OP_INVERT_RGN = 0x83;
    int OP_FILL_RGN = 0x84;
    int OP_FRAME_SAME_RGN = 0x88;
    int OP_PAINT_SAME_RGN = 0x89;
    int OP_ERASE_SAME_RGN = 0x8A;
    int OP_INVERT_SAME_RGN = 0x8B;
    int OP_FILL_SAME_RGN = 0x8C;
    /* Not implemented */
    int OP_BITS_RECT = 0x90;
    int OP_BITS_RGN = 0x91;
    int OP_PACK_BITS_RECT = 0x98;
    int OP_PACK_BITS_RGN = 0x99;
    int OP_DIRECT_BITS_RECT = 0x9A;
    /* Not implemented */
    int OP_DIRECT_BITS_RGN = 0x9B;
    int OP_SHORT_COMMENT = 0xA0;
    int OP_LONG_COMMENT = 0xA1;
    int OP_END_OF_PICTURE = 0xFF;
    int OP_VERSION_2 = 0x2FF;
    int OP_COMPRESSED_QUICKTIME = 0x8200;
    int OP_UNCOMPRESSED_QUICKTIME = 0x8201;

    String APPLE_USE_RESERVED_FIELD = "Reserved for Apple use.";

    /*
     * Picture comment 'kind' codes from: http://developer.apple.com/technotes/qd/qd_10.html
    int TextBegin = 150;
    int TextEnd = 151;
    int StringBegin = 152;
    int StringEnd = 153;
    int TextCenter = 154;
    int LineLayoutOff = 155;
    int LineLayoutOn = 156;
    int ClientLineLayout = 157;
    int PolyBegin = 160;
    int PolyEnd = 161;
    int PolyIgnore = 163;
    int PolySmooth = 164;
    int PolyClose = 165;
    int DashedLine = 180;
    int DashedStop = 181;
    int SetLineWidth = 182;
    int PostScriptBegin = 190;
    int PostScriptEnd = 191;
    int PostScriptHandle = 192;
    int PostScriptFile = 193;
    int TextIsPostScript = 194;
    int ResourcePS = 195;
    int PSBeginNoSave = 196;
    int SetGrayLevel = 197;
    int RotateBegin = 200;
    int RotateEnd = 201;
    int RotateCenter = 202;
    int FormsPrinting = 210;
    int EndFormsPrinting = 211;
    int ICC_Profile = 224;
    int Photoshop_Data = 498;
    int BitMapThinningOff = 1000;
    int BitMapThinningOn = 1001;
     */
}
