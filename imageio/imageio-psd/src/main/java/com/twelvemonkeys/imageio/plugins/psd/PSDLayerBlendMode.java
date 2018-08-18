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
 * PSDLayerBlendMode
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDLayerBlendMode.java,v 1.0 May 8, 2008 4:34:35 PM haraldk Exp$
 */
final class PSDLayerBlendMode {
    final int blendMode;
    final int opacity; // 0-255
    final int clipping; // 0: base, 1: non-base
    final byte flags;

    public PSDLayerBlendMode(final ImageInputStream pInput) throws IOException {
        int blendModeSig = pInput.readInt();
        if (blendModeSig != PSD.RESOURCE_TYPE) {
            throw new IIOException("Illegal PSD Blend Mode signature, expected 8BIM: " + PSDUtil.intToStr(blendModeSig));
        }

        blendMode = pInput.readInt();

        opacity = pInput.readUnsignedByte();
        clipping = pInput.readUnsignedByte();
        flags = pInput.readByte();

        pInput.readByte(); // Pad
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());

        builder.append("[");
        builder.append("mode: \"").append(PSDUtil.intToStr(blendMode));
        builder.append("\", opacity: ").append(opacity);
        builder.append(", clipping: ").append(clipping);
        switch (clipping) {
            case 0:
                builder.append(" (base)");
                break;
            case 1:
                builder.append(" (non-base)");
                break;
            default:
                builder.append(" (unknown)");
                break;
        }
        builder.append(", flags: ").append(byteToBinary(flags));
        /*
        bit 0 = transparency protected; bit 1 = visible; bit 2 = obsolete; 
        bit 3 = 1 for Photoshop 5.0 and later, tells if bit 4 has useful information;
        bit 4 = pixel data irrelevant to appearance of document
         */
        builder.append(" (");
        if ((flags & 0x01) != 0) {
            builder.append("Transp. protected, ");
        }
        if ((flags & 0x02) != 0) {
            builder.append("Hidden, ");
        }
        if ((flags & 0x04) != 0) {
            builder.append("Obsolete bit, ");
        }
        if ((flags & 0x08) != 0) {
            builder.append("PS 5.0 data present, "); // "tells if next bit has useful information"...
        }
        if ((flags & 0x10) != 0) {
            builder.append("Pixel data irrelevant, ");
        }
        if ((flags & 0x20) != 0) {
            builder.append("Unknown bit 5, ");
        }
        if ((flags & 0x40) != 0) {
            builder.append("Unknown bit 6, ");
        }
        if ((flags & 0x80) != 0) {
            builder.append("Unknown bit 7, ");
        }

        // Stupidity...
        if (flags != 0) {
            builder.delete(builder.length() - 2, builder.length());
        }
                
        builder.append(")");

        builder.append("]");

        return builder.toString();
    }

    private static String byteToBinary(final int pFlags) {
        String flagStr = Integer.toBinaryString(pFlags);
        flagStr = "00000000".substring(flagStr.length()) + flagStr;
        return flagStr;
    }
}
