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

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDLayerMaskData
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDLayerMaskData.java,v 1.0 May 6, 2008 5:15:05 PM haraldk Exp$
 */
class PSDLayerMaskData {
    private int mTop;
    private int mLeft;
    private int mBottom;
    private int mRight;
    private int mDefaultColor;
    private int mFlags;

    private boolean mLarge;
    private int mRealFlags;
    private int mRealUserBackground;
    private int mRealTop;
    private int mRealLeft;
    private int mRealBottom;
    private int mRealRight;

    PSDLayerMaskData(ImageInputStream pInput, int pSize) throws IOException {
        if (pSize != 20 && pSize != 36) {
            throw new IIOException("Illegal PSD Layer Mask data size: " + pSize + " (expeced 20 or 36)");
        }
        mTop = pInput.readInt();
        mLeft = pInput.readInt();
        mBottom = pInput.readInt();
        mRight = pInput.readInt();

        mDefaultColor = pInput.readUnsignedByte();

        mFlags = pInput.readUnsignedByte();

        if (pSize == 20) {
            pInput.readShort(); // Pad
        }
        else {
            // TODO: What to make out of this?
            mLarge = true;

            mRealFlags = pInput.readUnsignedByte();
            mRealUserBackground = pInput.readUnsignedByte();

            mRealTop = pInput.readInt();
            mRealLeft = pInput.readInt();
            mRealBottom = pInput.readInt();
            mRealRight = pInput.readInt();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("top: ").append(mTop);
        builder.append(", left: ").append(mLeft);
        builder.append(", bottom: ").append(mBottom);
        builder.append(", right: ").append(mRight);
        builder.append(", default color: ").append(mDefaultColor);
        builder.append(", flags: ").append(Integer.toBinaryString(mFlags));

        // TODO: Maybe the flag bits have oposite order?
        builder.append(" (");
        if ((mFlags & 0x01) != 0) {
            builder.append("Pos. rel. to layer");
        }
        else {
            builder.append("Pos. abs.");
        }
        if ((mFlags & 0x02) != 0) {
            builder.append(", Mask disabled");
        }
        else {
            builder.append(", Mask enabled");
        }
        if ((mFlags & 0x04) != 0) {
            builder.append(", Invert mask");
        }
        if ((mFlags & 0x08) != 0) {
            builder.append(", Unknown bit 3");
        }
        if ((mFlags & 0x10) != 0) {
            builder.append(", Unknown bit 4");
        }
        if ((mFlags & 0x20) != 0) {
            builder.append(", Unknown bit 5");
        }
        if ((mFlags & 0x40) != 0) {
            builder.append(", Unknown bit 6");
        }
        if ((mFlags & 0x80) != 0) {
            builder.append(", Unknown bit 7");
        }
        builder.append(")");

        builder.append("]");
        return builder.toString();
    }
}
