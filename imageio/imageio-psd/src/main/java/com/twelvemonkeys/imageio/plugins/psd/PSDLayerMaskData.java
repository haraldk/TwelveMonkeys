/*
 * Copyright (c) 2014, Harald Kuhr
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
final class PSDLayerMaskData {
    private int top;
    private int left;
    private int bottom;
    private int right;
    private int defaultColor;
    private int flags;

    private boolean large;
    private int realFlags;
    private int realUserBackground;
    private int realTop;
    private int realLeft;
    private int realBottom;
    private int realRight;

    PSDLayerMaskData(ImageInputStream pInput, int pSize) throws IOException {
        if (pSize != 20 && pSize != 36) {
            throw new IIOException("Illegal PSD Layer Mask data size: " + pSize + " (expeced 20 or 36)");
        }
        top = pInput.readInt();
        left = pInput.readInt();
        bottom = pInput.readInt();
        right = pInput.readInt();

        defaultColor = pInput.readUnsignedByte();

        flags = pInput.readUnsignedByte();

        if (pSize == 20) {
            pInput.readShort(); // Pad
        }
        else {
            // TODO: What to make out of this?
            large = true;

            realFlags = pInput.readUnsignedByte();
            realUserBackground = pInput.readUnsignedByte();

            realTop = pInput.readInt();
            realLeft = pInput.readInt();
            realBottom = pInput.readInt();
            realRight = pInput.readInt();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("top: ").append(top);
        builder.append(", left: ").append(left);
        builder.append(", bottom: ").append(bottom);
        builder.append(", right: ").append(right);
        builder.append(", default color: ").append(defaultColor);
        builder.append(", flags: ").append(Integer.toBinaryString(flags));

        // TODO: Maybe the flag bits have oposite order?
        builder.append(" (");
        if ((flags & 0x01) != 0) {
            builder.append("Pos. rel. to layer");
        }
        else {
            builder.append("Pos. abs.");
        }
        if ((flags & 0x02) != 0) {
            builder.append(", Mask disabled");
        }
        else {
            builder.append(", Mask enabled");
        }
        if ((flags & 0x04) != 0) {
            builder.append(", Invert mask");
        }
        if ((flags & 0x08) != 0) {
            builder.append(", Unknown bit 3");
        }
        if ((flags & 0x10) != 0) {
            builder.append(", Unknown bit 4");
        }
        if ((flags & 0x20) != 0) {
            builder.append(", Unknown bit 5");
        }
        if ((flags & 0x40) != 0) {
            builder.append(", Unknown bit 6");
        }
        if ((flags & 0x80) != 0) {
            builder.append(", Unknown bit 7");
        }
        builder.append(")");

        builder.append("]");
        return builder.toString();
    }
}
