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
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.plugins.jpeg.ThumbnailReader.JPEGThumbnailReader;
import com.twelvemonkeys.imageio.plugins.jpeg.ThumbnailReader.UncompressedThumbnailReader;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * EXIFThumbnail
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFThumbnail.java,v 1.0 18.04.12 12:19 haraldk Exp$
 */
final class EXIFThumbnail {
    private EXIFThumbnail() {
    }

    static ThumbnailReader from(final EXIF segment, final CompoundDirectory exif, final ImageReader jpegThumbnailReader) throws IOException {
        if (segment != null && exif != null && exif.directoryCount() >= 2) {
            ImageInputStream stream = segment.exifData(); // NOTE This is an in-memory stream and must not be closed...

            Directory ifd1 = exif.getDirectory(1);

            // Compression: 1 = no compression, 6 = JPEG compression (default)
            Entry compressionEntry = ifd1.getEntryById(TIFF.TAG_COMPRESSION);
            int compression = compressionEntry == null ? 6 : ((Number) compressionEntry.getValue()).intValue();

            switch (compression) {
                case 1:
                    return createUncompressedThumbnailReader(stream, ifd1);
                case 6:
                    return createJPEGThumbnailReader(segment, jpegThumbnailReader, stream, ifd1);
                default:
                    throw new IIOException("EXIF IFD with unknown thumbnail compression (expected 1 or 6): " + compression);
            }
        }

        return null;
    }

    private static UncompressedThumbnailReader createUncompressedThumbnailReader(ImageInputStream stream, Directory ifd1) throws IOException {
        Entry stripOffEntry = ifd1.getEntryById(TIFF.TAG_STRIP_OFFSETS);
        Entry width = ifd1.getEntryById(TIFF.TAG_IMAGE_WIDTH);
        Entry height = ifd1.getEntryById(TIFF.TAG_IMAGE_HEIGHT);

        if (stripOffEntry != null && width != null && height != null) {
            Entry bitsPerSample = ifd1.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
            Entry samplesPerPixel = ifd1.getEntryById(TIFF.TAG_SAMPLES_PER_PIXEL);
            Entry photometricInterpretation = ifd1.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION);

            // Required
            int w = ((Number) width.getValue()).intValue();
            int h = ((Number) height.getValue()).intValue();

            if (bitsPerSample != null && !Arrays.equals((int[]) bitsPerSample.getValue(), new int[] {8, 8, 8})) {
                throw new IIOException("Unknown BitsPerSample value for uncompressed EXIF thumbnail (expected [8, 8, 8]): " + bitsPerSample.getValueAsString());
            }

            if (samplesPerPixel != null && ((Number) samplesPerPixel.getValue()).intValue() != 3) {
                throw new IIOException("Unknown SamplesPerPixel value for uncompressed EXIF thumbnail (expected 3): " + samplesPerPixel.getValueAsString());
            }

            int interpretation = photometricInterpretation != null ? ((Number) photometricInterpretation.getValue()).intValue() : 2;
            long stripOffset = ((Number) stripOffEntry.getValue()).longValue();

            int thumbLength = w * h * 3;
            if (stripOffset >= 0 && stripOffset + thumbLength <= stream.length()) {
                // Read raw image data, either RGB or YCbCr
                stream.seek(stripOffset);
                byte[] thumbData = new byte[thumbLength];
                stream.readFully(thumbData);

                switch (interpretation) {
                    case 2:
                        // RGB
                        break;
                    case 6:
                        // YCbCr
                        for (int i = 0; i < thumbLength; i += 3) {
                            YCbCrConverter.convertJPEGYCbCr2RGB(thumbData, thumbData, i);
                        }
                        break;
                    default:
                        throw new IIOException("Unknown PhotometricInterpretation value for uncompressed EXIF thumbnail (expected 2 or 6): " + interpretation);
                }

                return new UncompressedThumbnailReader(w, h, thumbData);
            }
        }

        throw new IIOException("EXIF IFD with empty or incomplete uncompressed thumbnail");
    }

    private static JPEGThumbnailReader createJPEGThumbnailReader(EXIF exif, ImageReader jpegThumbnailReader, ImageInputStream stream, Directory ifd1) throws IOException {
        Entry jpegOffEntry = ifd1.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT);
        if (jpegOffEntry != null) {
            Entry jpegLenEntry = ifd1.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

            // Test if Exif thumbnail is contained within the Exif segment (offset + length <= segment.length)
            long jpegOffset = ((Number) jpegOffEntry.getValue()).longValue();
            long jpegLength = jpegLenEntry != null ? ((Number) jpegLenEntry.getValue()).longValue() : -1;

            if (jpegLength > 0 && jpegOffset + jpegLength <= exif.data.length) {
                // Verify first bytes are FFD8
                stream.seek(jpegOffset);
                stream.setByteOrder(ByteOrder.BIG_ENDIAN);

                if (stream.readUnsignedShort() == JPEG.SOI) {
                    return new JPEGThumbnailReader(jpegThumbnailReader, stream, jpegOffset);
                }
            }
        }

        throw new IIOException("EXIF IFD with empty or incomplete JPEG thumbnail");
    }
}
