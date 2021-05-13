/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import javax.imageio.ImageWriteParam;
import java.util.Locale;

/**
 * TIFFImageWriteParam
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriteParam.java,v 1.0 18.09.13 12:47 haraldk Exp$
 */
public final class TIFFImageWriteParam extends ImageWriteParam {
    // TODO: Support CCITT Modified Huffman compression (2) BASELINE!!
    // TODO: Support CCITT T.4 (3)
    // TODO: Support CCITT T.6 (4)
    // TODO: Support JBIG compression via ImageIO plugin/delegate?
    // TODO: Support JPEG2000 compression via ImageIO plugin/delegate?
    // TODO: Support tiling
    // TODO: Support OPTIONAL predictor. See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.

    // DONE:
    // Support no compression (None/1)
    // Support ZLIB (/Deflate) compression (8)
    // Support PackBits compression (32773)
    // Support LZW compression (5)?
    // Support JPEG compression (7)

    TIFFImageWriteParam() {
        this(Locale.getDefault());
    }

    TIFFImageWriteParam(final Locale locale) {
        super(locale);

        // NOTE: We use the same spelling/casing as the JAI equivalent to be as compatible as possible
        // See: http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/TIFFImageWriteParam.html
        compressionTypes = new String[] {
                "None",
                "CCITT RLE", "CCITT T.4", "CCITT T.6",
                "LZW", "JPEG", "ZLib", "PackBits", "Deflate",
                null/* "EXIF JPEG" */ // A well-defined form of "Old-style JPEG", no tables/process, only 513 (offset) and 514 (length)
        };
        compressionType = compressionTypes[0];
        canWriteCompressed = true;
    }

    @Override
    public float[] getCompressionQualityValues() {
        super.getCompressionQualityValues();

        // TODO: Special case for JPEG and ZLib/Deflate

        return null;
    }

    @Override
    public String[] getCompressionQualityDescriptions() {
        super.getCompressionQualityDescriptions();

        // TODO: Special case for JPEG and ZLib/Deflate

        return null;
    }

    static int getCompressionType(final ImageWriteParam param) {
        // TODO: Support mode COPY_FROM_METADATA (when we have metadata...)
        if (param == null || param.getCompressionMode() != MODE_EXPLICIT || param.getCompressionType() == null || param.getCompressionType().equals("None")) {
            return TIFFBaseline.COMPRESSION_NONE;
        }
        else if (param.getCompressionType().equals("PackBits")) {
            return TIFFBaseline.COMPRESSION_PACKBITS;
        }
        else if (param.getCompressionType().equals("ZLib")) {
            return TIFFExtension.COMPRESSION_ZLIB;
        }
        else if (param.getCompressionType().equals("Deflate")) {
            return TIFFExtension.COMPRESSION_DEFLATE;
        }
        else if (param.getCompressionType().equals("LZW")) {
            return TIFFExtension.COMPRESSION_LZW;
        }
        else if (param.getCompressionType().equals("JPEG")) {
            return TIFFExtension.COMPRESSION_JPEG;
        }
        else if (param.getCompressionType().equals("CCITT RLE")) {
            return TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE;
        }
        else if (param.getCompressionType().equals("CCITT T.4")) {
            return TIFFExtension.COMPRESSION_CCITT_T4;
        }
        else if (param.getCompressionType().equals("CCITT T.6")) {
            return TIFFExtension.COMPRESSION_CCITT_T6;
        }
//        else if (param.getCompressionType().equals("EXIF JPEG")) {
//            return TIFFExtension.COMPRESSION_OLD_JPEG;
//        }

        throw new IllegalArgumentException(String.format("Unsupported compression type: %s", param.getCompressionType()));
    }
}
