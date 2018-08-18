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
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;

/**
 * PSDColorData
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDColorData.java,v 1.0 Apr 29, 2008 5:33:01 PM haraldk Exp$
 */
final class PSDColorData {
    final byte[] colors;
    private IndexColorModel colorModel;

    PSDColorData(final ImageInputStream pInput) throws IOException {
        int length = pInput.readInt();
        if (length == 0) {
            throw new IIOException("No palette information in PSD");
        }
        else if (length % 3 != 0) {
            throw new IIOException("Wrong palette information in PSD");
        }

        // NOTE: Spec says length may only be 768 bytes (256 RGB triplets)
        colors = new byte[length];
        pInput.readFully(colors);

        // NOTE: Could be a padding byte here, if not even..
    }

    IndexColorModel getIndexColorModel() {
        if (colorModel == null) {
            int[] rgb = toInterleavedRGB(colors);
            colorModel = new IndexColorModel(8, rgb.length, rgb, 0, false, -1, DataBuffer.TYPE_BYTE);
        }

        return colorModel;
    }

    private static int[] toInterleavedRGB(final byte[] pColors) {
        int[] rgb = new int[pColors.length / 3];

        for (int i = 0; i < rgb.length; i++) {
            // Pack the non-interleaved samples into interleaved form
            int r = pColors[                 i] & 0xff;
            int g = pColors[    rgb.length + i] & 0xff;
            int b = pColors[2 * rgb.length + i] & 0xff;

            rgb[i] = (r << 16) | (g << 8) | b;
        }

        return rgb;
    }
}
