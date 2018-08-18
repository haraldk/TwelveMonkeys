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
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;

/**
 * ThumbnailReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ThumbnailReader.java,v 1.0 18.04.12 12:22 haraldk Exp$
 */
abstract class ThumbnailReader {

    private final ThumbnailReadProgressListener progressListener;
    protected final int imageIndex;
    protected final int thumbnailIndex;

    protected ThumbnailReader(final ThumbnailReadProgressListener progressListener, final int imageIndex, final int thumbnailIndex) {
        this.progressListener = progressListener != null ? progressListener : new NullProgressListener();
        this.imageIndex = imageIndex;
        this.thumbnailIndex = thumbnailIndex;
    }

    protected final void processThumbnailStarted() {
        progressListener.thumbnailStarted(imageIndex, thumbnailIndex);
    }

    protected final void processThumbnailProgress(float percentageDone) {
        progressListener.thumbnailProgress(percentageDone);
    }

    protected final void processThumbnailComplete() {
        progressListener.thumbnailComplete();
    }

    static protected BufferedImage readJPEGThumbnail(final ImageReader reader, final ImageInputStream stream) throws IOException {
        reader.setInput(stream);

        return reader.read(0);
    }

    static protected BufferedImage readRawThumbnail(final byte[] thumbnail, final int size, final int offset, int w, int h) {
        DataBufferByte buffer = new DataBufferByte(thumbnail, size, offset);
        WritableRaster raster;
        ColorModel cm;

        if (thumbnail.length == w * h) {
            raster = Raster.createInterleavedRaster(buffer, w, h, w, 1, new int[] {0}, null);
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        }
        else {
            raster = Raster.createInterleavedRaster(buffer, w, h, w * 3, 3, new int[] {0, 1, 2}, null);
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        }

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    public abstract BufferedImage read() throws IOException;

    public abstract int getWidth() throws IOException;

    public abstract int getHeight() throws IOException;

    private static class NullProgressListener implements ThumbnailReadProgressListener {
        @Override
        public void thumbnailStarted(int imageIndex, int thumbnailIndex) {
        }

        @Override
        public void thumbnailProgress(float percentageDone) {
        }

        @Override
        public void thumbnailComplete() {
        }
    }
}
