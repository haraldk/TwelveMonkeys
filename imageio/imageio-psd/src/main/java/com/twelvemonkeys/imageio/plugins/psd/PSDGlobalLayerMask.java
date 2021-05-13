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

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDGlobalLayerMask
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDGlobalLayerMask.java,v 1.0 May 8, 2008 5:33:48 PM haraldk Exp$
 */
final class PSDGlobalLayerMask {

    static final PSDGlobalLayerMask NULL_MASK = new PSDGlobalLayerMask();

    final int colorSpace;
    final short[] colors = new short[4];
    final int opacity;
    final int kind;

    PSDGlobalLayerMask(final ImageInputStream pInput, final long globalLayerMaskLength) throws IOException {
        colorSpace = pInput.readUnsignedShort(); // Undocumented

        pInput.readFully(colors, 0, colors.length);

        opacity = pInput.readUnsignedShort(); // 0-100

        // Kind: 0: Selected (ie inverted), 1: Color protected, 128: Use value stored per layer (actually, the value is 80, not 0x80)
        kind = pInput.readUnsignedByte();

        // Skip "Variable: Filler zeros"
        pInput.skipBytes(globalLayerMaskLength - 17);
    }

    private PSDGlobalLayerMask() {
        colorSpace = 0;
        opacity = 0;
        kind = 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("color space: 0x").append(Integer.toHexString(colorSpace));
        builder.append(", colors: [");

        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append("0x").append(Integer.toHexString(colors[i]));
        }

        builder.append("], opacity: ").append(opacity);
        builder.append(", kind: ").append(kind);
        builder.append("]");

        return builder.toString();
    }
}
