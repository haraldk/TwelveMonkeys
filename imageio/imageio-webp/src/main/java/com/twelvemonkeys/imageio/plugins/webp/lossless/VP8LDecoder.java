/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp.lossless;

import com.twelvemonkeys.imageio.plugins.webp.LSBBitReader;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.ColorIndexingTransform;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.ColorTransform;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.PredictorTransform;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.SubtractGreenTransform;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.Transform;
import com.twelvemonkeys.imageio.plugins.webp.lossless.transform.TransformType;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * VP8LDecoder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public final class VP8LDecoder {
    private final ImageInputStream imageInput;
    private final LSBBitReader lsbBitReader;

    public VP8LDecoder(final ImageInputStream imageInput, final boolean debug) {
        this.imageInput = imageInput;
        lsbBitReader = new LSBBitReader(imageInput);
    }

    public void readVP8Lossless(final WritableRaster raster, final boolean topLevel) throws IOException {
        //https://github.com/webmproject/libwebp/blob/666bd6c65483a512fe4c2eb63fbc198b6fb4fae4/src/dec/vp8l_dec.c#L1114

        int xSize = raster.getWidth();
        int ySize = raster.getHeight();

        // Read transforms
        ArrayList<Transform> transforms = new ArrayList<>();
        while (topLevel && lsbBitReader.readBit() == 1) {
            xSize = readTransform(xSize, ySize, transforms);
        }

        // Read color cache size
        int colorCacheBits = 0;
        if (lsbBitReader.readBit() == 1) {
            colorCacheBits = (int) lsbBitReader.readBits(4);
            if (colorCacheBits < 1 || colorCacheBits > 11) {
                throw new IIOException("Corrupt WebP stream, colorCacheBits < 1 || > 11: " + colorCacheBits);
            }
        }

        // Read Huffman codes
        readHuffmanCodes(colorCacheBits, topLevel);

        ColorCache colorCache = null;

        if (colorCacheBits > 0) {
            colorCache = new ColorCache(colorCacheBits);
        }

        // Use the Huffman trees to decode the LZ77 encoded data.
//        decodeImageData(raster, )
    }

    private int readTransform(int xSize, int ySize, List<Transform> transforms) throws IOException {
        int transformType = (int) lsbBitReader.readBits(2);

        // TODO: Each transform type can only be present once in the stream.

        switch (transformType) {
            case TransformType.PREDICTOR_TRANSFORM:
                System.err.println("transformType: PREDICTOR_TRANSFORM");
                //Intentional Fallthrough
            case TransformType.COLOR_TRANSFORM: {
                // The two first transforms contains the exact same data, can be combined
                if (transformType == TransformType.COLOR_TRANSFORM) {
                    System.err.println("transformType: COLOR_TRANSFORM");
                }

                byte sizeBits = (byte) (lsbBitReader.readBits(3) + 2);

                int blockWidth = subSampleSize(xSize, sizeBits);
                int blockHeight = subSampleSize(ySize, sizeBits);
                WritableRaster raster =
                        Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, blockWidth, blockHeight, 4 * blockWidth, 4,
                                new int[] {0, 1, 2, 3}, null);
                readVP8Lossless(raster, false);

                //Keep data as raster for convenient (x,y) indexing
                if (transformType == TransformType.PREDICTOR_TRANSFORM) {
                    transforms.add(0, new PredictorTransform(raster, sizeBits));
                }
                else {
                    transforms.add(0, new ColorTransform(raster, sizeBits));
                }

                break;
            }
            case TransformType.SUBTRACT_GREEN: {
                System.err.println("transformType: SUBTRACT_GREEN");
                // No data here
                transforms.add(0, new SubtractGreenTransform());
                break;
            }
            case TransformType.COLOR_INDEXING_TRANSFORM: {
                System.err.println("transformType: COLOR_INDEXING_TRANSFORM");

                // 8 bit value for color table size
                int colorTableSize = ((int) lsbBitReader.readBits(8)) + 1; // 1-256
                System.err.println("colorTableSize: " + colorTableSize);

                // If the index is equal or larger than color_table_size,
                // the argb color value should be set to 0x00000000
                // We handle this by allocating a possibly larger buffer
                int safeColorTableSize = colorTableSize > 16 ? 256 :
                                         colorTableSize > 4 ? 16 :
                                         colorTableSize > 2 ? 4 : 2;

                System.err.println("safeColorTableSize: " + safeColorTableSize);

                byte[] colorTable = new byte[safeColorTableSize * 4];

                // The color table can be obtained by reading an image,
                // without the RIFF header, image size, and transforms,
                // assuming a height of one pixel and a width of
                // color_table_size. The color table is always
                // subtraction-coded to reduce image entropy.
                readVP8Lossless(
                        Raster.createInterleavedRaster(
                                new DataBufferByte(colorTable, colorTableSize * 4),
                                colorTableSize, 1, colorTableSize * 4,
                                4, new int[] {0, 1, 2, 3}, null)
                        , false);


                //resolve subtraction code
                for (int i = 4; i < colorTable.length; i++) {
                    colorTable[i] += colorTable[i - 4];
                }

                // The number of pixels packed into each green sample (byte)
                byte widthBits = (byte) (colorTableSize > 16 ? 0 :
                                         colorTableSize > 4 ? 1 :
                                         colorTableSize > 2 ? 2 : 3);

                xSize = subSampleSize(xSize, widthBits);

                // The colors components are stored in ARGB order at 4*index, 4*index + 1, 4*index + 2, 4*index + 3
                // TODO: Can we use this to produce an image with IndexColorModel instead of expanding the values in-memory?
                transforms.add(0, new ColorIndexingTransform(colorTable, widthBits));

                break;
            }
            default:
                throw new AssertionError("Invalid transformType: " + transformType);
        }

        return xSize;
    }

    private void readHuffmanCodes(int colorCacheBits, boolean allowRecursion) {

    }

    private static int subSampleSize(final int size, final int samplingBits) {
        return (size + (1 << samplingBits) - 1) >> samplingBits;
    }
}
