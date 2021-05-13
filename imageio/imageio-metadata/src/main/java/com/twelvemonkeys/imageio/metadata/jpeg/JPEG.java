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

    /** Start of Scan segment marker (SOS). */
    int SOS = 0xFFDA;

    /** Define Quantization Tables segment marker (DQT). */
    int DQT = 0xFFDB;
    /** Define Huffman Tables segment marker (DHT). */
    int DHT = 0xFFC4;

    /** Comment (COM) */
    int COM = 0xFFFE;

    /** Define Number of Lines (DNL). */
    int DNL = 0xFFDC;
    /** Define Restart Interval (DRI). */
    int DRI = 0xFFDD;
    /** Define Hierarchical Progression (DHP). */
    int DHP = 0xFFDE;
    /** Expand reference components (EXP). */
    int EXP = 0xFFDF;
    /** Temporary use in arithmetic coding (TEM). */
    int TEM = 0xFF01;
    /** Define Define Arithmetic Coding conditioning (DAC). */
    int DAC = 0xFFCC;

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
    /** SOF0: Baseline DCT, Huffman coding. */
    int SOF0 = 0xFFC0;
    /** SOF0: Extended DCT, Huffman coding. */
    int SOF1 = 0xFFC1;
    /** SOF2: Progressive DCT, Huffman coding. */
    int SOF2 = 0xFFC2;
    /** SOF3: Lossless sequential, Huffman coding. */
    int SOF3 = 0xFFC3;
    /** SOF5: Sequential DCT, differential Huffman coding. */
    int SOF5 = 0xFFC5;
    /** SOF6: Progressive DCT, differential Huffman coding. */
    int SOF6 = 0xFFC6;
    /** SOF7: Lossless, Differential Huffman coding. */
    int SOF7 = 0xFFC7;
    /** SOF9: Extended sequential DCT, arithmetic coding. */
    int SOF9 = 0xFFC9;
    /** SOF10: Progressive DCT, arithmetic coding. */
    int SOF10 = 0xFFCA;
    /** SOF11: Lossless sequential, arithmetic coding. */
    int SOF11 = 0xFFCB;
    /** SOF13: Sequential DCT, differential arithmetic coding. */
    int SOF13 = 0xFFCD;
    /** SOF14: Progressive DCT, differential arithmetic coding. */
    int SOF14 = 0xFFCE;
    /** SOF15: Lossless, differential arithmetic coding. */
    int SOF15 = 0xFFCF;

    // JPEG-LS markers
    /** SOF55: JPEG-LS. */
    int SOF55 = 0xFFF7; // NOTE: Equal to a normal SOF segment
    int LSE = 0xFFF8;   // JPEG-LS Preset Parameter marker

    // TODO: Known/Important APPn marker identifiers
    // "JFIF" APP0
    // "JFXX" APP0
    // "Exif" APP1
    // "ICC_PROFILE" APP2
    // "Adobe" APP14

    // Possibly
    // "http://ns.adobe.com/xap/1.0/" (XMP) APP1
    // "Photoshop 3.0" (may contain IPTC) APP13
}
