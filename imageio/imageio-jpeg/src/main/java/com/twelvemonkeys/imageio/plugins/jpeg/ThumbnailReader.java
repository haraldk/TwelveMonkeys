/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ThumbnailReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ThumbnailReader.java,v 1.0 18.04.12 12:22 haraldk Exp$
 */
abstract class ThumbnailReader {

    public abstract BufferedImage read() throws IOException;

    public abstract int getWidth() throws IOException;

    public abstract int getHeight() throws IOException;

    public IIOMetadata readMetadata() throws IOException {
        return null;
    }

    static class UncompressedThumbnailReader extends ThumbnailReader {
        private final int width;
        private final int height;
        private final byte[] data;
        private final int offset;

        public UncompressedThumbnailReader(int width, int height, byte[] data) {
            this(width, height, data, 0);
        }

        public UncompressedThumbnailReader(int width, int height, byte[] data, int offset) {
            this.width = isTrue(width > 0, width, "width");
            this.height = isTrue(height > 0, height, "height");;
            this.data = notNull(data, "data");
            this.offset = isTrue(offset >= 0 && offset < data.length, offset, "offset");
        }

        @Override
        public BufferedImage read() throws IOException {
            DataBufferByte buffer = new DataBufferByte(data, data.length, offset);
            WritableRaster raster;
            ColorModel cm;

            if (data.length == width * height) {
                raster = Raster.createInterleavedRaster(buffer, width, height, width, 1, new int[] {0}, null);
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            }
            else {
                raster = Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, new int[] {0, 1, 2}, null);
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            }

            return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        }

        @Override
        public int getWidth() throws IOException {
            return width;
        }

        @Override
        public int getHeight() throws IOException {
            return height;
        }
    }

    static class IndexedThumbnailReader extends ThumbnailReader {
        private final int width;
        private final int height;
        private final byte[] palette;
        private final int paletteOff;
        private final byte[] data;
        private final int dataOff;

        public IndexedThumbnailReader(final int width, int height, final byte[] palette, final int paletteOff, final byte[] data, final int dataOff) {
            this.width = isTrue(width > 0, width, "width");
            this.height = isTrue(height > 0, height, "height");;
            this.palette = notNull(palette, "palette");
            this.paletteOff = isTrue(paletteOff >= 0 && paletteOff < palette.length, paletteOff, "paletteOff");
            this.data = notNull(data, "data");
            this.dataOff = isTrue(dataOff >= 0 && dataOff < data.length, dataOff, "dataOff");
        }

        @Override
        public BufferedImage read() throws IOException {
            // 256 RGB triplets
            int[] rgbs = new int[256];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = (palette[paletteOff + 3 * i    ] & 0xff) << 16
                        | (palette[paletteOff + 3 * i + 1] & 0xff) << 8
                        | (palette[paletteOff + 3 * i + 2] & 0xff);
            }

            IndexColorModel icm = new IndexColorModel(8, rgbs.length, rgbs, 0, false, -1, DataBuffer.TYPE_BYTE);
            DataBufferByte buffer = new DataBufferByte(data, data.length - dataOff, dataOff);
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, 8, null);

            return new BufferedImage(icm, raster, icm.isAlphaPremultiplied(), null);
        }

        @Override
        public int getWidth() throws IOException {
            return width;
        }

        @Override
        public int getHeight() throws IOException {
            return height;
        }
    }

    static class JPEGThumbnailReader extends ThumbnailReader {
        private final ImageReader reader;
        private final ImageInputStream input;
        private final long offset;

        private Dimension dimension;

        public JPEGThumbnailReader(final ImageReader reader, final ImageInputStream input, final long offset) {
            this.reader = notNull(reader, "reader");
            this.input = notNull(input, "input");
            this.offset = isTrue(offset >= 0, offset, "offset");
        }

        private void initReader() throws IOException {
            if (reader.getInput() != input) {
                input.seek(offset);
                reader.setInput(input);
            }
        }

        @Override
        public BufferedImage read() throws IOException {
            initReader();
            return reader.read(0, null);
        }

        private Dimension readDimensions() throws IOException {
            if (dimension == null) {
                initReader();
                dimension = new Dimension(reader.getWidth(0), reader.getHeight(0));
            }

            return dimension;
        }

        @Override
        public int getWidth() throws IOException {
            return readDimensions().width;
        }

        @Override
        public int getHeight() throws IOException {
            return readDimensions().height;
        }

        @Override
        public IIOMetadata readMetadata() throws IOException {
            initReader();
            return reader.getImageMetadata(0);
        }
    }
}
