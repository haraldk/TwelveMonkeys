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

    private int maskParams;
    private int userMaskDensity;
    private double userMaskFeather;
    private int vectorMaskDensity;
    private double vectorMaskFeather;

    PSDLayerMaskData(final ImageInputStream pInput, final int pSize) throws IOException {
        if (pSize < 20 || pSize > 55) {
            throw new IIOException("Illegal PSD Layer Mask data size: " + pSize + " (expected between 20 and 55)");
        }

        // Rectangle enclosing layer mask: Top, left, bottom, right.
        top = pInput.readInt();
        left = pInput.readInt();
        bottom = pInput.readInt();
        right = pInput.readInt();

        // Default color. 0 or 255
        defaultColor = pInput.readUnsignedByte();

        // Flags.
        // bit 0 = position relative to layer
        // bit 1 = layer mask disabled
        // bit 2 = invert layer mask when blending (Obsolete)
        // bit 3 = indicates that the user mask actually came from rendering other data
        // bit 4 = indicates that the user and/or vector masks have parameters applied to them
        flags = pInput.readUnsignedByte();

        int dataLeft = pSize - 18;

        if ((flags & 0x10) != 0) {
            // Mask Parameters. Only present if bit 4 of Flags set above.
            maskParams = pInput.readUnsignedByte();
            dataLeft--;

            // Mask Parameters bit flags present as follows:
            // bit 0 = user mask density, 1 byte
            // bit 1 = user mask feather, 8 byte, double
            // bit 2 = vector mask density, 1 byte
            // bit 3 = vector mask feather, 8 bytes, double
            if ((maskParams & 0x01) != 0) {
                userMaskDensity = pInput.readByte();
                dataLeft--;
            }
            if ((maskParams & 0x02) != 0) {
                userMaskFeather = pInput.readDouble();
                dataLeft -= 8;
            }
            if ((maskParams & 0x04) != 0) {
                vectorMaskDensity = pInput.readByte();
                dataLeft--;
            }
            if ((maskParams & 0x08) != 0) {
                vectorMaskFeather = pInput.readDouble();
                dataLeft -= 8;
            }
        }

        // Padding. Only present if size = 20. Otherwise the following is present
        if (pSize == 20 && dataLeft == 2) {
            pInput.readShort(); // Pad
            dataLeft -= 2;
        }
        else {
            if (dataLeft >= 2) {
                // Real Flags. Same as Flags information above.
                flags = pInput.readUnsignedByte();
                // Real user mask background. 0 or 255.
                defaultColor = pInput.readUnsignedByte();
                dataLeft -= 2;
            }
            if (dataLeft >= 16) {
                // Rectangle enclosing layer mask: Top, left, bottom, right.
                top = pInput.readInt();
                left = pInput.readInt();
                bottom = pInput.readInt();
                right = pInput.readInt();
                dataLeft -= 16;
            }
        }

        if (dataLeft > 0) {
            pInput.skipBytes(dataLeft);
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

        builder.append(" (");
        if ((flags & 0x01) != 0) {
            builder.append("relative");
        }
        else {
            builder.append("absolute");
        }
        if ((flags & 0x02) != 0) {
            builder.append(", disabled");
        }
        else {
            builder.append(", enabled");
        }
        if ((flags & 0x04) != 0) {
            builder.append(", inverted");
        }
        if ((flags & 0x08) != 0) {
            builder.append(", from rendered data");
        }
        if ((flags & 0x10) != 0) {
            builder.append(", has parameters");
        }
        if ((flags & 0x20) != 0) {
            builder.append(", unknown flag (bit 5)");
        }
        if ((flags & 0x40) != 0) {
            builder.append(", unknown flag (bit 6)");
        }
        if ((flags & 0x80) != 0) {
            builder.append(", unknown flag (bit 7)");
        }
        builder.append(")");

        if ((flags & 0x10) != 0) {
            if ((maskParams & 0x01) != 0) {
                builder.append(", userMaskDensity: ").append(userMaskDensity);
            }
            if ((maskParams & 0x02) != 0) {
                builder.append(", userMaskFeather: ").append(userMaskFeather);
            }
            if ((maskParams & 0x04) != 0) {
                builder.append(", vectorMaskDensity: ").append(vectorMaskDensity);
            }
            if ((maskParams & 0x08) != 0) {
                builder.append(", vectorMaskFeather: ").append(vectorMaskFeather);
            }
        }

        builder.append("]");
        return builder.toString();
    }
}
