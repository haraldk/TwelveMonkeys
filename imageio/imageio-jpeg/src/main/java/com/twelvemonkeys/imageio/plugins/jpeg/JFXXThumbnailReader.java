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

import com.twelvemonkeys.image.InverseColorMapIndexColorModel;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.IOException;
import java.lang.ref.SoftReference;

/**
 * JFXXThumbnailReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFXXThumbnailReader.java,v 1.0 18.04.12 12:19 haraldk Exp$
 */
final class JFXXThumbnailReader extends ThumbnailReader {

    private final ImageReader reader;
    private final JFXX segment;

    private transient SoftReference<BufferedImage> cachedThumbnail;

    JFXXThumbnailReader(final ThumbnailReadProgressListener progressListener, final ImageReader jpegReader, final int imageIndex, final int thumbnailIndex, final JFXX segment) {
        super(progressListener, imageIndex, thumbnailIndex);
        this.reader = Validate.notNull(jpegReader);
        this.segment = segment;
    }

    @Override
    public BufferedImage read() throws IOException {
        processThumbnailStarted();

        BufferedImage thumbnail;
        switch (segment.extensionCode) {
            case JFXX.JPEG:
                thumbnail = readJPEGCached(true);
                break;
            case JFXX.INDEXED:
                thumbnail = readIndexed();
                break;
            case JFXX.RGB:
                thumbnail = readRGB();
                break;
            default:
                throw new IIOException(String.format("Unsupported JFXX extension code: %d", segment.extensionCode));
        }

        processThumbnailProgress(100f);
        processThumbnailComplete();

        return thumbnail;
    }

    IIOMetadata readMetadata() throws IOException {
        ImageInputStream input = new ByteArrayImageInputStream(segment.thumbnail);

        try {
            reader.setInput(input);

            return reader.getImageMetadata(0);
        }
        finally {
            input.close();
        }
    }

    private BufferedImage readJPEGCached(boolean pixelsExposed) throws IOException {
        BufferedImage thumbnail = cachedThumbnail != null ? cachedThumbnail.get() : null;

        if (thumbnail == null) {
            ImageInputStream stream = new ByteArrayImageInputStream(segment.thumbnail);
            try {
                thumbnail = readJPEGThumbnail(reader, stream);
            }
            finally {
                stream.close();
            }
        }

        cachedThumbnail = pixelsExposed ? null : new SoftReference<BufferedImage>(thumbnail);

        return thumbnail;
    }

    @Override
    public int getWidth() throws IOException {
        switch (segment.extensionCode) {
            case JFXX.RGB:
            case JFXX.INDEXED:
                return segment.thumbnail[0] & 0xff;
            case JFXX.JPEG:
                return readJPEGCached(false).getWidth();
            default:
                throw new IIOException(String.format("Unsupported JFXX extension code: %d", segment.extensionCode));
        }
    }

    @Override
    public int getHeight() throws IOException {
        switch (segment.extensionCode) {
            case JFXX.RGB:
            case JFXX.INDEXED:
                return segment.thumbnail[1] & 0xff;
            case JFXX.JPEG:
                return readJPEGCached(false).getHeight();
            default:
                throw new IIOException(String.format("Unsupported JFXX extension code: %d", segment.extensionCode));
        }
    }

    private BufferedImage readIndexed() {
        // 1 byte: xThumb
        // 1 byte: yThumb
        // 768 bytes: palette
        // x * y bytes: 8 bit indexed pixels
        int w = segment.thumbnail[0] & 0xff;
        int h = segment.thumbnail[1] & 0xff;

        int[] rgbs = new int[256];
        for (int i = 0; i < rgbs.length; i++) {
            rgbs[i] = (segment.thumbnail[3 * i + 2] & 0xff) << 16
                    | (segment.thumbnail[3 * i + 3] & 0xff) << 8
                    | (segment.thumbnail[3 * i + 4] & 0xff);
        }

        IndexColorModel icm = new InverseColorMapIndexColorModel(8, rgbs.length, rgbs, 0, false, -1, DataBuffer.TYPE_BYTE);
        DataBufferByte buffer = new DataBufferByte(segment.thumbnail, segment.thumbnail.length - 770, 770);
        WritableRaster raster = Raster.createPackedRaster(buffer, w, h, 8, null);

        return new BufferedImage(icm, raster, icm.isAlphaPremultiplied(), null);
    }

    private BufferedImage readRGB() {
        // 1 byte: xThumb
        // 1 byte: yThumb
        // 3 * x * y bytes: 24 bit RGB pixels
        int w = segment.thumbnail[0] & 0xff;
        int h = segment.thumbnail[1] & 0xff;

        return ThumbnailReader.readRawThumbnail(segment.thumbnail, segment.thumbnail.length - 2, 2, w, h);
    }
}
