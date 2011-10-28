/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

/**
 * ICNS
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNS.java,v 1.0 25.10.11 19:10 haraldk Exp$
 */
interface ICNS {
    /** "icns" magic identifier */
    int MAGIC = ('i' << 24) + ('c' << 16) + ('n' << 8) + 's';

    /** 32×32 1-bit mono icon */
    int ICON = ('I' << 24) + ('C' << 16) + ('O' << 8) + 'N';
    /** 32×32 1-bit mono icon with 1-bit mask*/
    int ICN_ = ('I' << 24) + ('C' << 16) + ('N' << 8) + '#';

    /** 16×12 1 bit mask*/
    int icm_ = ('i' << 24) + ('c' << 16) + ('m' << 8) + '#';
    /** 16×12 4 bit icon */
    int icm4 = ('i' << 24) + ('c' << 16) + ('m' << 8) + '4';
    /** 16×12 8 bit icon */
    int icm8 = ('i' << 24) + ('c' << 16) + ('m' << 8) + '8';

    /** 16×16 1-bit mask */
    int ics_ = ('i' << 24) + ('c' << 16) + ('s' << 8) + '#';
    /** 16×16 4-bit icon */
    int ics4 = ('i' << 24) + ('c' << 16) + ('s' << 8) + '4';
    /** 16×16 8-bit icon */
    int ics8 = ('i' << 24) + ('c' << 16) + ('s' << 8) + '8';
    /** 16×16 24-bit icon, run-length compressed */
    int is32 = ('i' << 24) + ('s' << 16) + ('3' << 8) + '2';
    /** 16x16 8-bit mask */
    int s8mk = ('s' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 32×32 4-bit icon */
    int icl4 = ('i' << 24) + ('c' << 16) + ('l' << 8) + '4';
    /** 32×32 8-bit icon */
    int icl8 = ('i' << 24) + ('c' << 16) + ('l' << 8) + '8';
    /** 32×32 24-bit icon, run-length compressed */
    int il32 = ('i' << 24) + ('l' << 16) + ('3' << 8) + '2';
    /** 32×32 8-bit mask */
    int l8mk = ('l' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 48×48 1-bit mask */
    int ich_ = ('i' << 24) + ('c' << 16) + ('h' << 8) + '#';
    /** 48×48 4-bit icon */
    int ich4 = ('i' << 24) + ('c' << 16) + ('h' << 8) + '4';
    /** 48×48 8-bit icon */
    int ich8 = ('i' << 24) + ('c' << 16) + ('h' << 8) + '8';
    /** 48×48 24-bit icon, run-length compressed */
    int ih32 = ('i' << 24) + ('h' << 16) + ('3' << 8) + '2';
    /** 48×48 8-bit mask */
    int h8mk = ('h' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 128×128 24-bit icon, run-length compressed */
    int it32 = ('i' << 24) + ('t' << 16) + ('3' << 8) + '2';
    /** 128×128 8-bit mask */
    int t8mk = ('t' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 256×256 JPEG 2000 or PNG icon */
    int ic08 = ('i' << 24) + ('c' << 16) + ('0' << 8) + '8';

    /** 512×512 JPEG 2000 or PNG icon */
    int ic09 = ('i' << 24) + ('c' << 16) + ('0' << 8) + '9';

    /** 1024×1024 PNG icon (10.7)*/
    int ic10 = ('i' << 24) + ('c' << 16) + ('1' << 8) + '0';

    /*
    ICN#	256	32	32×32 1-bit mono icon with 1-bit mask
    icm#	24	16	16×12 1 bit mask
    icm4	96	16	16×12 4 bit icon
    icm8	192	16	16×12 8 bit icon
    ics#	32	16	16×16 1-bit mask
    ics4	128	16	16×16 4-bit icon
    ics8	256	16	16x16 8 bit icon
    is32	varies (768)	16	16×16 24-bit icon
    s8mk	256	16	16x16 8-bit mask
    icl4	512	32	32×32 4-bit icon
    icl8	1,024	32	32×32 8-bit icon
    il32	varies (3,072)	32	32x32 24-bit icon
    l8mk	1,024	32	32×32 8-bit mask
    ich#	288	48	48×48 1-bit mask
    ich4	1,152	48	48×48 4-bit icon
    ich8	2,304	48	48×48 8-bit icon
    ih32	varies (6,912)	48	48×48 24-bit icon
    h8mk	2,304	48	48×48 8-bit mask
    it32	varies (49,152)	128	128×128 24-bit icon
    t8mk	16,384	128	128×128 8-bit mask
    ic08	varies	256	256×256 icon in JPEG 2000 or PNG format
    ic09	varies	512	512×512 icon in JPEG 2000 or PNG format
    ic10	varies	1024	1024×1024 icon in PNG format (added in Mac OS X 10.7)
    */

    byte[] JPEG_2000_MAGIC = new byte[] {0x00, 0x00, 0x00, 0x0C, 'j', 'P', 0x20, 0x20, 0x0D, 0x0A, (byte) 0x87, 0x0A};
    byte[] PNG_MAGIC = new byte[] {(byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', 0x0d, 0x0a, 0x1a, 0x0a};
}
