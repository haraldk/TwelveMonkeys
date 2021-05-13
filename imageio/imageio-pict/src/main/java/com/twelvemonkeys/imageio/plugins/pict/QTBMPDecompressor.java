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

import com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

/**
 * QTBMPDecompressor
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressor.java,v 1.0 Feb 16, 2009 9:18:28 PM haraldk Exp$
 */
final class QTBMPDecompressor extends QTDecompressor {
    public boolean canDecompress(final ImageDesc description) {
        return QuickTime.VENDOR_APPLE.equals(description.compressorVendor)
                && "WRLE".equals(description.compressorIdentifer)
                && "bmp ".equals(idString(description.extraDesc, 4));
    }

    private static String idString(final byte[] data, final int offset) {
        return new String(data, offset, 4, StandardCharsets.US_ASCII);
    }

    public BufferedImage decompress(final ImageDesc description, final InputStream stream) throws IOException {
        return ImageIO.read(new SequenceInputStream(fakeBMPHeader(description), stream));
    }

    private InputStream fakeBMPHeader(final ImageDesc description) throws IOException {
        int bmpHeaderSize = 14;
        int dibHeaderSize = 12; // 12: OS/2 V1
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(bmpHeaderSize + dibHeaderSize);

        LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(out);

        // BMP header
        stream.writeByte('B');
        stream.writeByte('M');

        stream.writeInt(description.dataSize + bmpHeaderSize + dibHeaderSize); // Data size + BMP header + DIB header

        stream.writeShort(0x0); // Reserved
        stream.writeShort(0x0); // Reserved

        stream.writeInt(bmpHeaderSize + dibHeaderSize); // Image offset

        // DIB header
        stream.writeInt(dibHeaderSize);      // DIB header size

        stream.writeShort(description.width);
        stream.writeShort(description.height);

        stream.writeShort(1); // Planes, only legal value: 1
        stream.writeShort(description.depth); // Bit depth

        return out.createInputStream();
    }
}
