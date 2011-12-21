/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * QTBMPDecompressor
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTBMPDecompressor.java,v 1.0 Feb 16, 2009 9:18:28 PM haraldk Exp$
 */
final class QTBMPDecompressor extends QTDecompressor {
    public boolean canDecompress(final QuickTime.ImageDesc pDescription) {
        return QuickTime.VENDOR_APPLE.equals(pDescription.compressorVendor) && "WRLE".equals(pDescription.compressorIdentifer)
                && "bmp ".equals(idString(pDescription.extraDesc, 4));
    }

    private static String idString(final byte[] pData, final int pOffset) {
        try {
            return new String(pData, pOffset, 4, "ASCII");
        }
        catch (UnsupportedEncodingException e) {
            throw new Error("ASCII charset must always be supported", e);
        }
    }

    public BufferedImage decompress(final QuickTime.ImageDesc pDescription, final InputStream pStream) throws IOException {
        return ImageIO.read(new SequenceInputStream(fakeBMPHeader(pDescription), pStream));
    }

    private InputStream fakeBMPHeader(final QuickTime.ImageDesc pDescription) throws IOException {
        int bmpHeaderSize = 14;
        int dibHeaderSize = 12; // 12: OS/2 V1
        ByteArrayOutputStream out = new FastByteArrayOutputStream(bmpHeaderSize + dibHeaderSize);

        LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(out);

        // BMP header
        stream.writeByte('B');
        stream.writeByte('M');

        stream.writeInt(pDescription.dataSize + bmpHeaderSize + dibHeaderSize); // Data size + BMP header + DIB header

        stream.writeShort(0x0); // Reserved
        stream.writeShort(0x0); // Reserved

        stream.writeInt(bmpHeaderSize + dibHeaderSize); // Image offset

        // DIB header
        stream.writeInt(dibHeaderSize);      // DIB header size

        stream.writeShort(pDescription.width);
        stream.writeShort(pDescription.height);

        stream.writeShort(1); // Planes, only legal value: 1
        stream.writeShort(pDescription.depth); // Bit depth

        return new ByteArrayInputStream(out.toByteArray());
    }
}
