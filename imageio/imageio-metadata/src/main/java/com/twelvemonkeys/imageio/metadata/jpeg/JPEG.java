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

package com.twelvemonkeys.imageio.metadata.jpeg;

/**
 * JPEG
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEG.java,v 1.0 11.02.11 15.51 haraldk Exp$
 */
public interface JPEG {
    /** Start of Image segment marker (SOI). */
    int SOI = 0xFFD8;
    /** End of Image segment marker (EOI). */
    int EOI = 0xFFD9;
    /** Start of Stream segment marker (SOS). */
    int SOS = 0xFFDA;

    /** Define Quantization Tables segment marker (DQT). */
    int DQT = 0xFFDB;

    // App segment markers (APPn).
    int APP0 = 0xFFE0;
    int APP1 = 0xFFE1;
    int APP2 = 0xFFE2;
    int APP3 = 0xFFE3;
    int APP4 = 0xFFE4;
    int APP5 = 0xFFE5;
    int APP6 = 0xFFE6;
    int APP7 = 0xFFE7;
    int APP8 = 0xFFE8;
    int APP9 = 0xFFE9;
    int APP10 = 0xFFEA;
    int APP11 = 0xFFEB;
    int APP12 = 0xFFEC;
    int APP13 = 0xFFED;
    int APP14 = 0xFFEE;
    int APP15 = 0xFFEF;

    // Start of Frame segment markers (SOFn).
    int SOF0 = 0xFFC0;
    int SOF1 = 0xFFC1;
    int SOF2 = 0xFFC2;
    int SOF3 = 0xFFC3;
    int SOF5 = 0xFFC5;
    int SOF6 = 0xFFC6;
    int SOF7 = 0xFFC7;
    int SOF9 = 0xFFC9;
    int SOF10 = 0xFFCA;
    int SOF11 = 0xFFCB;
    int SOF13 = 0xFFCD;
    int SOF14 = 0xFFCE;
    int SOF15 = 0xFFCF;

    // TODO: Known/Important APPn marker identifiers
    // "JFIF" APP0
    // "JFXX" APP0
    // "Exif" APP1
    // "ICC_PROFILE" APP2
    // "Adobe" APP14

    // Possibly
    // "http://ns.adobe.com/xap/1.0/" (XMP)
    // "Photoshop 3.0" (Contains IPTC)
}
