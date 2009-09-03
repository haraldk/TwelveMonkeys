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

package com.twelvemonkeys.imageio.plugins.pict;

import javax.imageio.IIOException;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * QTRAWDecompressor
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTRAWDecompressor.java,v 1.0 Feb 16, 2009 9:29:18 PM haraldk Exp$
 */
final class QTRAWDecompressor extends QTDecompressor {
    // TODO: Create a RAWImageReader for ImageIO to delegate to?
    //  - Would have to require a parameter controlling bit depth and pixel layout
    //  - Have a look at com.sun.media.imageio.stream.RawImageInputStream...
    // TODO: Support different bit depths

    public boolean canDecompress(final QuickTime.ImageDesc pDescription) {
        return QuickTime.VENDOR_APPLE.equals(pDescription.compressorVendor)
                && "raw ".equals(pDescription.compressorIdentifer)
                && (pDescription.depth == 24 || pDescription.depth == 32);
    }

    public BufferedImage decompress(final QuickTime.ImageDesc pDescription, final InputStream pStream) throws IOException {
        DataInputStream stream = new DataInputStream(pStream);

        byte[] data = new byte[pDescription.dataSize];
        stream.readFully(data, 0, pDescription.dataSize);
        stream.close();

        DataBuffer buffer = new DataBufferByte(data, data.length);

        WritableRaster raster;
        // TODO: Depth parameter can be 1-32 (color) or 33-40 (grayscale)
        switch (pDescription.depth) {
            case 24:
                raster = Raster.createInterleavedRaster(buffer, pDescription.width, pDescription.height, pDescription.width * 3, 3, new int[] {0, 1, 2}, null);
                break;
            case 32:
                raster = Raster.createInterleavedRaster(buffer, pDescription.width, pDescription.height, pDescription.width * 4, 4, new int[] {1, 2, 3, 0}, null);
                break;
            default:
                throw new IIOException("Unsupported RAW depth: " + pDescription.depth);
        }

        ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                pDescription.depth == 32,
                false,
                Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE
        );

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }
}
