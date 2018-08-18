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

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDResolutionInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDResolutionInfo.java,v 1.0 May 2, 2008 3:58:19 PM haraldk Exp$
 */
final class PSDDisplayInfo extends PSDImageResource {
    // TODO: Size of this struct should be 14.. Does not compute... Something bogus here

    // ColorSpace definitions:
    //    PSD_CS_RGB       = 0,                 /* RGB */
    //    PSD_CS_HSB       = 1,                 /* Hue, Saturation, Brightness */
    //    PSD_CS_CMYK      = 2,                 /* CMYK */
    //    PSD_CS_PANTONE   = 3,                 /* Pantone matching system (Lab)*/
    //    PSD_CS_FOCOLTONE = 4,                 /* Focoltone colour system (CMYK)*/
    //    PSD_CS_TRUMATCH  = 5,                 /* Trumatch color (CMYK)*/
    //    PSD_CS_TOYO      = 6,                 /* Toyo 88 colorfinder 1050 (Lab)*/
    //    PSD_CS_LAB       = 7,                 /* L*a*b*/
    //    PSD_CS_GRAYSCALE = 8,                 /* Grey scale */
    //    PSD_CS_HKS       = 10,                /* HKS colors (CMYK)*/
    //    PSD_CS_DIC       = 11,                /* DIC color guide (Lab)*/
    //    PSD_CS_ANPA      = 3000,              /* Anpa color (Lab)*/
        
    //typedef _DisplayInfo
    //{
    //   WORD  ColorSpace;
    //   WORD  Color[4];
    //   WORD  Opacity;          /* 0-100 */
    //   BYTE  Kind;             /* 0=selected, 1=protected */
    //   BYTE  Padding;          /* Always zero */
    //} DISPLAYINFO;

    int colorSpace;
    short[] colors;
    short opacity;
    byte kind;

    PSDDisplayInfo(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(ImageInputStream pInput) throws IOException {
        if (size % 14 != 0) {
            throw new IIOException("Display info length expected to be mod 14: " + size);
        }

//        long left = size;
//        while (left > 0) {
        colorSpace = pInput.readShort();

        // Color[4]...?
        colors = new short[4];
        colors[0] = pInput.readShort();
        colors[1] = pInput.readShort();
        colors[2] = pInput.readShort();
        colors[3] = pInput.readShort();

        opacity = pInput.readShort();

        kind = pInput.readByte();

        pInput.readByte(); // Pad
//            left -= 14;
//        }
        pInput.skipBytes(size - 14);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", ColorSpace: ").append(colorSpace);
        builder.append(", Colors: {");
        builder.append(colors[0]);
        builder.append(", ");
        builder.append(colors[1]);
        builder.append(", ");
        builder.append(colors[2]);
        builder.append(", ");
        builder.append(colors[3]);
        builder.append("}, Opacity: ").append(opacity);
        builder.append(", Kind: ").append(kind(kind));

        builder.append("]");

        return builder.toString();
    }

    private String kind(final byte pKind) {
        switch (pKind) {
            case 0:
                return "selected";
            case 1:
                return "protected";
            default:
                return "unknown kind: " + Integer.toHexString(pKind & 0xff);
        }
    }
}