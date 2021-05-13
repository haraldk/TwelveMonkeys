/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.image;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Monochrome B/W color model.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class MonochromeColorModel extends IndexColorModel {
    
    private final static int[] MONO_PALETTE = {0x00000000, 0x00FFFFFF};
    
    private static MonochromeColorModel sInstance = new MonochromeColorModel();
    
    private MonochromeColorModel() {
        super(1, 2, MONO_PALETTE, 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    public static IndexColorModel getInstance() {
        return sInstance;
    }

    public synchronized Object getDataElements(int pRGB, Object pPixel) {
        // Get color components
        int r = pRGB >> 16 & 0xFF;
        int g = pRGB >>  8 & 0xFF;
        int b = pRGB       & 0xFF;

        // ITU standard:  Gray scale=(222*Red+707*Green+71*Blue)/1000
        int gray = (222 * r + 707 * g + 71 * b) / 1000;

        byte[] pixel;
        if (pPixel != null) {
            pixel = (byte[]) pPixel;
        }
        else {
            pixel = new byte[1];
        }

        if (gray <= 0x80) {
            pixel[0] = 0;
        }
        else {
            pixel[0] = 1;
        }

        return pixel;
    }
}
