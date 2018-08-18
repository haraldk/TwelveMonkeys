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

import com.twelvemonkeys.imageio.color.YCbCrConverter;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;

/**
 * EXIFThumbnail
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFThumbnail.java,v 1.0 18.04.12 12:19 haraldk Exp$
 */
final class EXIFThumbnailReader extends ThumbnailReader {
    private final ImageReader reader;
    private final Directory ifd;
    private final ImageInputStream stream;
    private final int compression;

    private transient SoftReference<BufferedImage> cachedThumbnail;

    EXIFThumbnailReader(final ThumbnailReadProgressListener progressListener, final ImageReader jpegReader, final int imageIndex, final int thumbnailIndex, final Directory ifd, final ImageInputStream stream) {
        super(progressListener, imageIndex, thumbnailIndex);
        this.reader = Validate.notNull(jpegReader);
        this.ifd = ifd;
        this.stream = stream;

        Entry compression = ifd.getEntryById(TIFF.TAG_COMPRESSION);

        this.compression = compression != null ? ((Number) compression.getValue()).intValue() : 6;
    }

    @Override
    public BufferedImage read() throws IOException {
        if (compression == 1) { // 1 = no compression
            processThumbnailStarted();
            BufferedImage thumbnail = readUncompressed();
            processThumbnailProgress(100f);
            processThumbnailComplete();

            return thumbnail;
        }
        else if (compression == 6) { // 6 = JPEG compression
            processThumbnailStarted();
            BufferedImage thumbnail = readJPEGCached(true);
            processThumbnailProgress(100f);
            processThumbnailComplete();

            return thumbnail;
        }
        else {
            throw new IIOException("Unsupported EXIF thumbnail compression: " + compression);
        }
    }

    private BufferedImage readJPEGCached(final boolean pixelsExposed) throws IOException {
        BufferedImage thumbnail = cachedThumbnail != null ? cachedThumbnail.get() : null;

        if (thumbnail == null) {
            thumbnail = readJPEG();
        }

        cachedThumbnail = pixelsExposed ? null : new SoftReference<>(thumbnail);

        return thumbnail;
    }

    private BufferedImage readJPEG() throws IOException {
        // IFD1 should contain JPEG offset for JPEG thumbnail
        Entry jpegOffset = ifd.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT);

        if (jpegOffset != null) {
            stream.seek(((Number) jpegOffset.getValue()).longValue());
            InputStream input = IIOUtil.createStreamAdapter(stream);

            // For certain EXIF files (encoded with TIFF.TAG_YCBCR_POSITIONING = 2?), we need
            // EXIF information to read the thumbnail correctly (otherwise the colors are messed up).
            // Probably related to: http://bugs.sun.com/view_bug.do?bug_id=4881314

            // HACK: Splice empty EXIF information into the thumbnail stream
            byte[] fakeEmptyExif = {
                    // SOI (from original data)
                    (byte) input.read(), (byte) input.read(),
                    // APP1 + len (016) + 'Exif' + 0-term + pad
                    (byte) 0xFF, (byte) 0xE1, 0, 16, 'E', 'x', 'i', 'f', 0, 0,
                    // Big-endian BOM (MM), TIFF magic (042), offset (0000)
                    'M', 'M', 0, 42, 0, 0, 0, 0,
            };

            input = new SequenceInputStream(new ByteArrayInputStream(fakeEmptyExif), input);

            try {

                try (MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(input)) {
                    return readJPEGThumbnail(reader, stream);
                }
            }
            finally {
                input.close();
            }
        }

        throw new IIOException("Missing JPEGInterchangeFormat tag for JPEG compressed EXIF thumbnail");
    }

    private BufferedImage readUncompressed() throws IOException {
        // Read ImageWidth, ImageLength (height) and BitsPerSample (=8 8 8, always)
        // PhotometricInterpretation (2=RGB, 6=YCbCr), SamplesPerPixel (=3, always),
        Entry width = ifd.getEntryById(TIFF.TAG_IMAGE_WIDTH);
        Entry height = ifd.getEntryById(TIFF.TAG_IMAGE_HEIGHT);

        if (width == null || height == null) {
            throw new IIOException("Missing dimensions for uncompressed EXIF thumbnail");
        }

        Entry bitsPerSample = ifd.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
        Entry samplesPerPixel = ifd.getEntryById(TIFF.TAG_SAMPLES_PER_PIXEL);
        Entry photometricInterpretation = ifd.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION);

        // Required
        int w = ((Number) width.getValue()).intValue();
        int h = ((Number) height.getValue()).intValue();

        if (bitsPerSample != null) {
            int[] bpp = (int[]) bitsPerSample.getValue();
            if (!Arrays.equals(bpp, new int[] {8, 8, 8})) {
                throw new IIOException("Unknown BitsPerSample value for uncompressed EXIF thumbnail (expected [8, 8, 8]): " + bitsPerSample.getValueAsString());
            }
        }

        if (samplesPerPixel != null && (Integer) samplesPerPixel.getValue() != 3) {
            throw new IIOException("Unknown SamplesPerPixel value for uncompressed EXIF thumbnail (expected 3): " + samplesPerPixel.getValueAsString());
        }

        int interpretation = photometricInterpretation != null ? ((Number) photometricInterpretation.getValue()).intValue() : 2;

        // IFD1 should contain strip offsets for uncompressed images
        Entry offset = ifd.getEntryById(TIFF.TAG_STRIP_OFFSETS);
        if (offset != null) {
            stream.seek(((Number) offset.getValue()).longValue());

            // Read raw image data, either RGB or YCbCr
            int thumbSize = w * h * 3;
            byte[] thumbData = JPEGImageReader.readFully(stream, thumbSize);

            switch (interpretation) {
                case 2:
                    // RGB
                    break;
                case 6:
                    // YCbCr
                    for (int i = 0; i < thumbSize; i += 3) {
                        YCbCrConverter.convertYCbCr2RGB(thumbData, thumbData, i);
                    }
                    break;
                default:
                    throw new IIOException("Unknown PhotometricInterpretation value for uncompressed EXIF thumbnail (expected 2 or 6): " + interpretation);
            }

            return ThumbnailReader.readRawThumbnail(thumbData, thumbSize, 0, w, h);
        }

        throw new IIOException("Missing StripOffsets tag for uncompressed EXIF thumbnail");
    }

    @Override
    public int getWidth() throws IOException {
        if (compression == 1) { // 1 = no compression
            Entry width = ifd.getEntryById(TIFF.TAG_IMAGE_WIDTH);

            if (width == null) {
                throw new IIOException("Missing dimensions for uncompressed EXIF thumbnail");
            }

            return ((Number) width.getValue()).intValue();
        }
        else if (compression == 6) { // 6 = JPEG compression
            return readJPEGCached(false).getWidth();
        }
        else {
            throw new IIOException("Unsupported EXIF thumbnail compression (expected 1 or 6): " + compression);
        }
    }

    @Override
    public int getHeight() throws IOException {
        if (compression == 1) { // 1 = no compression
            Entry height = ifd.getEntryById(TIFF.TAG_IMAGE_HEIGHT);

            if (height == null) {
                throw new IIOException("Missing dimensions for uncompressed EXIF thumbnail");
            }

            return ((Number) height.getValue()).intValue();
        }
        else if (compression == 6) { // 6 = JPEG compression
            return readJPEGCached(false).getHeight();
        }
        else {
            throw new IIOException("Unsupported EXIF thumbnail compression  (expected 1 or 6): " + compression);
        }
    }
}
