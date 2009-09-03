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

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;
import java.io.IOException;

/**
 * PSDResolutionInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDResolutionInfo.java,v 1.0 May 2, 2008 3:58:19 PM haraldk Exp$
 */
class PSDDisplayInfo extends PSDImageResource {
    // TODO: Size of this struct should be 14.. Does not compute... 
    //typedef _DisplayInfo
    //{
    //   WORD  ColorSpace;
    //   WORD  Color[4];
    //   WORD  Opacity;          /* 0-100 */
    //   BYTE  Kind;             /* 0=selected, 1=protected */
    //   BYTE  Padding;          /* Always zero */
    //} DISPLAYINFO;

    private int mColorSpace;
    private short[] mColors;
    private short mOpacity;
    private byte mKind;

    PSDDisplayInfo(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(ImageInputStream pInput) throws IOException {
        if (mSize % 14 != 0) {
            throw new IIOException("Display info length expected to be mod 14: " + mSize);
        }

//        long left = mSize;
//        while (left > 0) {
            mColorSpace = pInput.readShort();

            // Color[4]...?
        mColors = new short[4];
            mColors[0] = pInput.readShort();
            mColors[1] = pInput.readShort();
            mColors[2] = pInput.readShort();
            mColors[3] = pInput.readShort();

            mOpacity = pInput.readShort();

            mKind = pInput.readByte();

            pInput.readByte(); // Pad
//            left -= 14;
//        }
        pInput.skipBytes(mSize - 14);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", ColorSpace: ").append(mColorSpace);
        builder.append(", Colors: {");
        builder.append(mColors[0]);
        builder.append(", ");
        builder.append(mColors[1]);
        builder.append(", ");
        builder.append(mColors[2]);
        builder.append(", ");
        builder.append(mColors[3]);
        builder.append("}, Opacity: ").append(mOpacity);
        builder.append(", Kind: ").append(kind(mKind));

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