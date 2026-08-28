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

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.ImageReaderBase;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;

import static com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;
import static com.twelvemonkeys.imageio.plugins.pict.QuickTime.VENDOR_APPLE;

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

    @Override
    public boolean canDecompress(final ImageDesc description) {
        return VENDOR_APPLE.equals(description.compressorVendor)
                && "raw ".equals(description.compressorIdentifer)
                && (description.depth == 24 || description.depth == 32 || description.depth == 40);
    }

    @Override
    public BufferedImage decompress(final ImageDesc description, final ImageInputStream stream) throws IOException {
        // Width, height and dataSize are independent fields from the 'idsc' Atom, so a crafted stream may
        // declare a data size smaller than width * height * bytesPerPixel. Validate before allocating and
        // indexing the buffer by pixel geometry below.
        // Note width and height are unsigned 16 bit, so the product must be computed as long.
        int bytesPerPixel = description.depth == 24 ? 3 : description.depth == 32 ? 4 : 1;
        long imageDataSize = (long) description.width * description.height * bytesPerPixel;
        if (description.width <= 0 || description.height <= 0 || imageDataSize > description.dataSize) {
            throw new IIOException(String.format(
                    "Corrupt QuickTime RAW: data size %d too small for %dx%d at depth %d",
                    description.dataSize, description.width, description.height, description.depth));
        }

        // TODO: Replace with destination size check from ImageReaderBase when API is done
        if (stream.length() < 0 && imageDataSize > 512L * 1024 * 1024) {
            throw new IIOException(String.format("Image dimensions imply an allocation of %d bytes, exceeding 512 MB", imageDataSize));
        }
        else if (stream.length() >= 0 && imageDataSize > stream.length() - stream.getStreamPosition()) {
            throw new IIOException(String.format("Image dimensions imply an allocation of %d bytes, exceeding %s available bytes", imageDataSize, stream.length() - stream.getStreamPosition()));
        }

        byte[] data = new byte[(int) imageDataSize];
        stream.readFully(data, 0, data.length);
        stream.skipBytes(description.dataSize - imageDataSize);

        DataBuffer buffer = new DataBufferByte(data, data.length);

        WritableRaster raster;

        // TODO: Depth parameter can be 1-32 (color) or 33-40 (gray scale)
        switch (description.depth) {
            case 40: // 8 bit gray (untested)
                raster = Raster.createInterleavedRaster(
                        buffer,
                        description.width, description.height,
                        description.width, 1,
                        new int[] {0},
                        null
                );
                break;
            case 24: // 24 bit RGB
                raster = Raster.createInterleavedRaster(
                        buffer,
                        description.width, description.height,
                        description.width * 3, 3,
                        new int[] {0, 1, 2},
                        null
                );
                break;
            case 32: // 32 bit ARGB
                // WORKAROUND: There is a bug in the way Java 2D interprets the band offsets in
                // Raster.createInterleavedRaster (see below) before Java 6. So, instead of
                // passing a correct offset array below, we swap channel 1 & 3 to make it ABGR...
                for (int y = 0; y < description.height; y++) {
                    for (int x = 0; x < description.width; x++) {
                        int offset = 4 * y * description.width + x * 4;
                        byte temp = data[offset + 1];
                        data[offset + 1] = data[offset + 3];
                        data[offset + 3] = temp;
                    }
                }

                raster = Raster.createInterleavedRaster(
                        buffer,
                        description.width, description.height,
                        description.width * 4, 4,
                        new int[] {3, 2, 1, 0}, // B & R mixed up. {1, 2, 3, 0} is correct
                        null
                );
                break;
            default:
                throw new IIOException("Unsupported QuickTime RAW depth: " + description.depth);
        }

        ColorModel cm = new ComponentColorModel(
                description.depth <= 32 ? ColorSpace.getInstance(ColorSpace.CS_sRGB) : ColorSpace.getInstance(ColorSpace.CS_GRAY),
                description.depth == 32,
                false,
                description.depth == 32 ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE
        );

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }
}
