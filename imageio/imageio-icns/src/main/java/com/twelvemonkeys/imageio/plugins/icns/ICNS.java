/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

/**
 * ICNS
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNS.java,v 1.0 25.10.11 19:10 haraldk Exp$
 */
interface ICNS {
    /** Resource header size (8). */
    int RESOURCE_HEADER_SIZE = 8;

    /** ICNS magic identifier ("icns"). */
    int MAGIC = ('i' << 24) + ('c' << 16) + ('n' << 8) + 's';

    /** 32×32 1-bit mono icon. */
    int ICON = ('I' << 24) + ('C' << 16) + ('O' << 8) + 'N';
    /** 32×32 1-bit mono icon with 1-bit mask. */
    int ICN_ = ('I' << 24) + ('C' << 16) + ('N' << 8) + '#';

    /** 16×12 1 bit mask. */
    int icm_ = ('i' << 24) + ('c' << 16) + ('m' << 8) + '#';
    /** 16×12 4 bit icon. */
    int icm4 = ('i' << 24) + ('c' << 16) + ('m' << 8) + '4';
    /** 16×12 8 bit icon. */
    int icm8 = ('i' << 24) + ('c' << 16) + ('m' << 8) + '8';

    /** 16×16 1-bit icon with 1-bit mask. */
    int ics_ = ('i' << 24) + ('c' << 16) + ('s' << 8) + '#';
    /** 16×16 4-bit icon. */
    int ics4 = ('i' << 24) + ('c' << 16) + ('s' << 8) + '4';
    /** 16×16 8-bit icon. */
    int ics8 = ('i' << 24) + ('c' << 16) + ('s' << 8) + '8';
    /** 16×16 24-bit icon, possibly run-length compressed. */
    int is32 = ('i' << 24) + ('s' << 16) + ('3' << 8) + '2';
    /** 16x16 8-bit mask. */
    int s8mk = ('s' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 32×32 4-bit icon. */
    int icl4 = ('i' << 24) + ('c' << 16) + ('l' << 8) + '4';
    /** 32×32 8-bit icon. */
    int icl8 = ('i' << 24) + ('c' << 16) + ('l' << 8) + '8';
    /** 32×32 24-bit icon, possibly run-length compressed. */
    int il32 = ('i' << 24) + ('l' << 16) + ('3' << 8) + '2';
    /** 32×32 8-bit mask. */
    int l8mk = ('l' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 48×48 1-bit icon with 1 bit mask. */
    int ich_ = ('i' << 24) + ('c' << 16) + ('h' << 8) + '#';
    /** 48×48 4-bit icon. */
    int ich4 = ('i' << 24) + ('c' << 16) + ('h' << 8) + '4';
    /** 48×48 8-bit icon. */
    int ich8 = ('i' << 24) + ('c' << 16) + ('h' << 8) + '8';
    /** 48×48 24-bit icon, possibly run-length compressed. */
    int ih32 = ('i' << 24) + ('h' << 16) + ('3' << 8) + '2';
    /** 48×48 8-bit mask. */
    int h8mk = ('h' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 128×128 24-bit icon, possibly run-length compressed. */
    int it32 = ('i' << 24) + ('t' << 16) + ('3' << 8) + '2';
    /** 128×128 8-bit mask. */
    int t8mk = ('t' << 24) + ('8' << 16) + ('m' << 8) + 'k';

    /** 256×256 JPEG 2000 or PNG icon (10.x+). */
    int ic08 = ('i' << 24) + ('c' << 16) + ('0' << 8) + '8';

    /** 512×512 JPEG 2000 or PNG icon (10.x+). */
    int ic09 = ('i' << 24) + ('c' << 16) + ('0' << 8) + '9';

    /** 1024×1024 PNG icon (10.7+). */
    int ic10 = ('i' << 24) + ('c' << 16) + ('1' << 8) + '0';

    /** Unknown (Version). */
    int icnV = ('i' << 24) + ('c' << 16) + ('n' << 8) + 'V';

    /** Unknown (Table of Contents). */
    int TOC_ = ('T' << 24) + ('O' << 16) + ('C' << 8) + ' ';

    /** JPEG 2000 magic header. */
    byte[] JPEG_2000_MAGIC = new byte[] {0x00, 0x00, 0x00, 0x0C, 'j', 'P', 0x20, 0x20, 0x0D, 0x0A, (byte) 0x87, 0x0A};

    /** PNG magic header. */
    byte[] PNG_MAGIC = new byte[] {(byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', 0x0d, 0x0a, 0x1a, 0x0a};
}
