/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.plugins.bmp.DIBHeader.BitmapInfoHeader;

import javax.imageio.IIOException;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * DIBImageWriter
 */
abstract class DIBImageWriter extends ImageWriterBase {
    DIBImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output);
        imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    }

    void writeDIBHeader(int infoHeaderSize, int width, int height, boolean isTopDown, int pixelSize, int compression) throws IOException {
        switch (infoHeaderSize) {
            case DIB.BITMAP_INFO_HEADER_SIZE:
                BitmapInfoHeader header = new BitmapInfoHeader();
                // TODO: Consider a constructor/factory for this
                header.width = width;
                header.height = height;
                header.topDown = isTopDown;

                header.planes = 1; // Always 1 plane
                header.bitCount = pixelSize;
                header.compression = compression;

                header.colorsUsed = 0;      // Means 2 ^ bitCount
                header.colorsImportant = 0; // Means all colors important

                header.imageSize = header.height * ((header.width * header.bitCount + 31) / 32) * 4; // Rows padded to 32 bit

                header.xPixelsPerMeter = 2835; // 72 DPI
                header.yPixelsPerMeter = 2835;

                header.write(imageOutput);
                break;
            default:
                throw new IIOException("Unsupported header size: " + infoHeaderSize);
        }
    }

    void writeUncompressed(boolean isTopDown, BufferedImage img, int height, int width) throws IOException {
        // TODO: Fix
        if (img.getType() != BufferedImage.TYPE_4BYTE_ABGR) {
            throw new IIOException("Blows!");
        }

        // Support
        // - TODO: IndexColorModel (ucompressed, RLE4, RLE8 or BI_PNG)
        // - TODO: ComponentColorModel (1 channel gray, 3 channel BGR and 4 channel BGRA, uncompressed and RLE8? BI_BITFIELDS? BI_PNG? BI_JPEG?)
        // - TODO: Packed/DirectColorModel (16 and 32 bit, BI_BITFIELDS, BI_PNG? BI_JPEG?)

        Raster raster = img.getRaster();
        WritableRaster rowRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, 1, width * 4, 4, new int[]{2, 1, 0, 3}, null);
        byte[] row = ((DataBufferByte) rowRaster.getDataBuffer()).getData();

        for (int i = 0; i < height; i++) {
            int line = isTopDown ? i : height - 1 - i;
            rowRaster.setDataElements(0, 0, raster.createChild(0, line, width, 1, 0, 0, new int[]{2, 1, 0, 3}));

            imageOutput.write(row);

            if (abortRequested()) {
                processWriteAborted();
                break;
            }

            processImageProgress(100f * i / (float) height);
        }
    }
}
