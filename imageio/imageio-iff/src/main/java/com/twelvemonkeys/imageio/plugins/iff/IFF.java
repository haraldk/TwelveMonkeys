/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

/**
 * IFF format constants.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IFF.java,v 1.0 07.mar.2006 15:31:48 haku Exp$
 */
interface IFF {
    /** IFF FORM group chunk */
    int CHUNK_FORM = ('F' << 24) + ('O' << 16) + ('R' << 8) + 'M';

    /** IFF ILBM form type (Interleaved bitmap) */
    int TYPE_ILBM = ('I' << 24) + ('L' << 16) + ('B' << 8) + 'M';

    /** IFF PBM form type (Packed bitmap) */
    int TYPE_PBM  = ('P' << 24) + ('B' << 16) + ('M' << 8) + ' ';

    // TODO:
    /** IFF DEEP form type (TVPaint) */
    int TYPE_DEEP  = ('D' << 24) + ('E' << 16) + ('E' << 8) + 'P';
    /** IFF RGB8 form type (TurboSilver) */
    int TYPE_RGB8  = ('R' << 24) + ('G' << 16) + ('B' << 8) + '8';
    /** IFF RGBN form type (TurboSilver) */
    int TYPE_RGBN  = ('R' << 24) + ('G' << 16) + ('B' << 8) + 'N';
    /** IFF ACBM form type (Amiga Basic) */
    int TYPE_ACBM  = ('A' << 24) + ('C' << 16) + ('B' << 8) + 'M';

    /** Bitmap Header chunk */
    int CHUNK_BMHD = ('B' << 24) + ('M' << 16) + ('H' << 8) + 'D';

    /** Color map chunk */
    int CHUNK_CMAP = ('C' << 24) + ('M' << 16) + ('A' << 8) + 'P';

    /** Hotspot chunk (cursors, brushes) */
    int CHUNK_GRAB = ('G' << 24) + ('R' << 16) + ('A' << 8) + 'B';

    /** Destination merge data chunk */
    int CHUNK_DEST = ('D' << 24) + ('E' << 16) + ('S' << 8) + 'T';

    /** Sprite information chunk */
    int CHUNK_SPRT = ('S' << 24) + ('P' << 16) + ('R' << 8) + 'T';

    /** Commodore Amiga viewport mode chunk (used to determine HAM and EHB modes) */
    int CHUNK_CAMG = ('C' << 24) + ('A' << 16) + ('M' << 8) + 'G';

    /** Main data (body) chunk */
    int CHUNK_BODY = ('B' << 24) + ('O' << 16) + ('D' << 8) + 'Y';

    /** Junk (to allow garbage data in files, without re-writing the entire file) */
    int CHUNK_JUNK = ('J' << 24) + ('U' << 16) + ('N' << 8) + 'K';

    /** EA IFF 85 Generic Author chunk */
    int CHUNK_AUTH = ('A' << 24) + ('U' << 16) + ('T' << 8) + 'H';
    /** EA IFF 85 Generic character string chunk */
    int CHUNK_CHRS = ('C' << 24) + ('H' << 16) + ('R' << 8) + 'S';
    /** EA IFF 85 Generic Name of art, music, etc. chunk */
    int CHUNK_NAME = ('N' << 24) + ('A' << 16) + ('M' << 8) + 'E';
    /** EA IFF 85 Generic unformatted ASCII text chunk */
    int CHUNK_TEXT = ('T' << 24) + ('E' << 16) + ('X' << 8) + 'T';
    /** EA IFF 85 Generic Copyright text chunk */
    int CHUNK_COPY = ('(' << 24) + ('c' << 16) + (')' << 8) + ' ';

    /** color cycling */
    int CHUNK_CRNG = ('C' << 24) + ('R' << 16) + ('N' << 8) + 'G';
    /** color cycling */
    int CHUNK_CCRT = ('C' << 24) + ('C' << 16) + ('R' << 8) + 'T';
    /** Color Lookup Table chunk */
    int CHUNK_CLUT = ('C' << 24) + ('L' << 16) + ('U' << 8) + 'T';
    /** Dots per inch chunk */
    int CHUNK_DPI  = ('D' << 24) + ('P' << 16) + ('I' << 8) + ' ';
    /** DPaint perspective chunk (EA) */
    int CHUNK_DPPV = ('D' << 24) + ('P' << 16) + ('P' << 8) + 'V';
    /** DPaint IV enhanced color cycle chunk (EA) */
    int CHUNK_DRNG = ('D' << 24) + ('R' << 16) + ('N' << 8) + 'G';
    /** Encapsulated Postscript chunk */
    int CHUNK_EPSF = ('E' << 24) + ('P' << 16) + ('S' << 8) + 'F';
    /** Cyan, Magenta, Yellow, & Black color map (Soft-Logik) */
    int CHUNK_CMYK = ('C' << 24) + ('M' << 16) + ('Y' << 8) + 'K';
    /** Color naming chunk (Soft-Logik) */
    int CHUNK_CNAM = ('C' << 24) + ('N' << 16) + ('A' << 8) + 'M';
    /** Line by line palette control information (Sebastiano Vigna) */
    int CHUNK_PCHG = ('P' << 24) + ('C' << 16) + ('H' << 8) + 'G';
    /** A mini duplicate ILBM used for preview (Gary Bonham) */
    int CHUNK_PRVW = ('P' << 24) + ('R' << 16) + ('V' << 8) + 'W';
    /** eXtended BitMap Information (Soft-Logik) */
    int CHUNK_XBMI = ('X' << 24) + ('B' << 16) + ('M' << 8) + 'I';
    /** Newtek Dynamic Ham color chunk */
    int CHUNK_CTBL = ('C' << 24) + ('T' << 16) + ('B' << 8) + 'L';
    /** Newtek Dynamic Ham chunk */
    int CHUNK_DYCP = ('D' << 24) + ('Y' << 16) + ('C' << 8) + 'P';
    /** Sliced HAM color chunk */
    int CHUNK_SHAM = ('S' << 24) + ('H' << 16) + ('A' << 8) + 'M';
    /** ACBM body chunk */
    int CHUNK_ABIT = ('A' << 24) + ('B' << 16) + ('I' << 8) + 'T';
    /** unofficial direct color */
    int CHUNK_DCOL = ('D' << 24) + ('C' << 16) + ('O' << 8) + 'L';
}
