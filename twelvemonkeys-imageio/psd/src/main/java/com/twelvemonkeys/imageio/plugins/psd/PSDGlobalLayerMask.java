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
import java.io.IOException;

/**
 * PSDGlobalLayerMask
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDGlobalLayerMask.java,v 1.0 May 8, 2008 5:33:48 PM haraldk Exp$
 */
class PSDGlobalLayerMask {
    private int mColorSpace;
    private int mColor1;
    private int mColor2;
    private int mColor3;
    private int mColor4;
    private int mOpacity;
    private int mKind;

    PSDGlobalLayerMask(ImageInputStream pInput) throws IOException {
        mColorSpace = pInput.readUnsignedShort();

        mColor1 = pInput.readUnsignedShort();
        mColor2 = pInput.readUnsignedShort();
        mColor3 = pInput.readUnsignedShort();
        mColor4 = pInput.readUnsignedShort();

        mOpacity = pInput.readUnsignedShort();

        mKind = pInput.readUnsignedByte();
        
        pInput.readByte(); // Pad
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("color space: 0x").append(Integer.toHexString(mColorSpace));
        builder.append(", colors: [0x").append(Integer.toHexString(mColor1));
        builder.append(", 0x").append(Integer.toHexString(mColor2));
        builder.append(", 0x").append(Integer.toHexString(mColor3));
        builder.append(", 0x").append(Integer.toHexString(mColor4));
        builder.append("], opacity: ").append(mOpacity);
        builder.append(", kind: ").append(mKind);
        builder.append("]");
        return builder.toString();
    }
}
