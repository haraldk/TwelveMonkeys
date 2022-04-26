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

import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

/**
 * PICTImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: PICTImageReaderSpi.java,v 1.0 28.feb.2006 19:21:05 haraldk Exp$
 */
public final class PICTImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates a {@code PICTImageReaderSpi}.
     */
    @SuppressWarnings("WeakerAccess")
    public PICTImageReaderSpi() {
        super(new PICTProviderInfo());
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        // PICT format doesn't have good magic and our method often gives false positives,
        // We'll check for other known formats (BMP, GIF, JPEG, PNG, PSD, TIFF) first
        if (isOtherFormat(stream)) {
            return false;
        }

        stream.mark();

        try {
            if (isPICT(stream)) {
                // If PICT clipboard format, return true immediately
                return true;
            }
            else {
                // We need to reset AND set mark again, to make sure the reset call in
                // the finally-block will not consume existing marks
                stream.reset();
                stream.mark();

                // Skip header 512 bytes for file-based streams
                skipNullHeader(stream);
            }

            return isPICT(stream);
        }
        catch (EOFException ignore) {
            return false;
        }
        finally {
            stream.reset();
        }
    }

    // TODO: Candidate util method.
    //      Might need to be able to exclude some formats
    //      Might need to return the format matched...
    static boolean isOtherFormat(final ImageInputStream stream) throws IOException {
        stream.mark();

        try {
            byte[] signature = new byte[8];
            stream.readFully(signature);

            // BMP: off: 0, len: 2, magic: 'B', 'M'
            // GIF: off: 0, len: 6, magic: 'G', 'I', 'F', '8', ('7' | '9'), 'a' (use only "GIF8"?)
            // JPEG: off 0, len: 2, magic: 0xffd8 (SOI marker)
            // PNG: off: 0, len: 8, magic: 0x89, 'P', 'N', 'G', 0x0d0a1a0a
            // PSD: off: 0, len: 4, magic: '8', 'B', 'P', 'S'
            // TIFF: off: 0, len: 4, magic: ('I', 'I', 0x00, (0x42 | 0x43)) | ('M', 'M', (0x42 | 0x43), 0x00) (43 is bigTiff)
            if (signature[0] == 'B' && signature[1] == 'M') {
                // BMP
                return true;
            }
            if (signature[0] == 'G' && signature[1] == 'I' && signature[2] == 'F' && signature[3] == '8'
                    && (signature[4] == '7' || signature[4] == '9') && signature[5] == 'a') {
                // GIF
                return true;
            }
            if (signature[0] == (byte) 0xFF && signature[1] == (byte) 0xD8 && signature[2] == (byte) 0xFF) {
                // JPEG
                return true;
            }
            if (signature[0] == (byte) 0x89 && signature[1] == 'P' && signature[2] == 'N' && signature[3] == 'G'
                    && signature[4] == 0x0D && signature[5] == 0x0A && signature[6] == 0x1A && signature[7] == 0x0A) {
                // PNG
                return true;
            }
            if (signature[0] == '8' && signature[1] == 'B' && signature[2] == 'P' && signature[3] == 'S') {
                // PSD
                return true;
            }
            if ((signature[0] == 'I' && signature[1] == 'I' && (signature[2] == 42 ||  signature[2] == 43) && signature[3] == 0x00)
                || signature[0] == 'M' && signature[1] == 'M' && signature[2] == 0x00 && (signature[3] == 42 ||  signature[3] == 43)) {
                // TIFF/BigTIFF
                return true;
            }
        }
        catch (EOFException ignore) {
            // Can't be any of the formats
        }
        finally {
            stream.reset();
        }

        return false;
    }

    static void skipNullHeader(final ImageInputStream pStream) throws IOException {
        // NOTE: Only skip if FILE FORMAT, not needed for macOS DnD
        // Spec says "platform dependent", may not be all nulls...
        pStream.skipBytes(PICT.PICT_NULL_HEADER_SIZE);
    }

    // NOTE: As the PICT format has a very weak identifier, a true return value is not necessarily a PICT...
    private boolean isPICT(final ImageInputStream pStream) throws IOException {
        // Size may be 0, so we can't use this for validation...
        pStream.readUnsignedShort();

        // Sanity check bounding box
        int y1 = pStream.readUnsignedShort();
        int x1 = pStream.readUnsignedShort();

        int y2 = pStream.readUnsignedShort();
        int x2 = pStream.readUnsignedShort();
        if (x2 - x1 < 0 || y2 - y1 < 0) {
            return false;
        }

        // Validate magic
        int magic = pStream.readInt();
        return (magic & 0xffff0000) == PICT.MAGIC_V1 || magic == PICT.MAGIC_V2;
    }

    public ImageReader createReaderInstance(final Object pExtension) {
        return new PICTImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Apple MacPaint/QuickDraw Picture (PICT) image reader";
    }
}
