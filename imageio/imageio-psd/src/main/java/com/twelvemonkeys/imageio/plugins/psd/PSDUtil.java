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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.stream.DirectImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.io.SubStream;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.zip.InflaterInputStream;

import static com.twelvemonkeys.imageio.util.IIOUtil.createStreamAdapter;
import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * PSDUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDUtil.java,v 1.0 Apr 29, 2008 5:05:00 PM haraldk Exp$
 */
final class PSDUtil {
    // TODO: Duplicated code from IFF plugin, move to some common util?
    static String intToStr(int value) {
        return new String(
                new byte[]{
                        (byte) ((value & 0xff000000) >>> 24),
                        (byte) ((value & 0x00ff0000) >> 16),
                        (byte) ((value & 0x0000ff00) >> 8),
                        (byte) ((value & 0x000000ff))
                }
        );
    }

    // TODO: Proably also useful for PICT reader, move to some common util?
    static String readPascalString(final DataInput pInput) throws IOException {
        int length = pInput.readUnsignedByte();

        if (length == 0) {
            return "";
        }

        byte[] bytes = new byte[length];
        pInput.readFully(bytes);

        return StringUtil.decode(bytes, 0, bytes.length, "ASCII");
    }

    // TODO: Probably also useful for PICT reader, move to some common util?
    static String readUnicodeString(final DataInput pInput) throws IOException {
        int length = pInput.readInt();

        if (length == 0) {
            return "";
        }

        byte[] bytes = new byte[length * 2];
        pInput.readFully(bytes);

        return StringUtil.decode(bytes, 0, bytes.length, "UTF-16");
    }

    public static float fixedPointToFloat(int pFP) {
        return ((pFP & 0xffff0000) >> 16) + (pFP & 0xffff) / (float) 0xffff;
    }

    static ImageInputStream createDecompressorStream(final ImageInputStream stream, int compression, int columns, int bitsPerSample,
                                                     final int[] byteCounts, long compressedLength) throws IOException {
        switch (compression) {
            case PSD.COMPRESSION_NONE:
                long streamLength = stream.length();
                return new SubImageInputStream(stream, streamLength < 0 ? Long.MAX_VALUE : streamLength);

            case PSD.COMPRESSION_RLE:
                int rowLength = (columns * bitsPerSample + 7) / 8;
                return new DirectImageInputStream(new SequenceInputStream(new LazyPackBitsStreamEnumeration(stream, byteCounts, rowLength)));

            case PSD.COMPRESSION_ZIP:
                return new DirectImageInputStream(new InflaterInputStream(createStreamAdapter(stream, compressedLength)));

            case PSD.COMPRESSION_ZIP_PREDICTION:
                return new DirectImageInputStream(new HorizontalDeDifferencingStream(new InflaterInputStream(createStreamAdapter(stream, compressedLength)), columns, 1, bitsPerSample, BIG_ENDIAN));

            default:
        }

        throw new IllegalArgumentException("Unknown PSD compression: " + compression);
    }

    private static class LazyPackBitsStreamEnumeration implements Enumeration<InputStream> {
        private final ImageInputStream stream;
        private final int[] byteCounts;
        private final int rowLength;
        private int index;

        public LazyPackBitsStreamEnumeration(ImageInputStream stream, int[] byteCounts, int rowLength) {
            this.stream = stream;
            this.byteCounts = byteCounts;
            this.rowLength = rowLength;
        }

        @Override
        public boolean hasMoreElements() {
            return index < byteCounts.length;
        }

        @Override
        public InputStream nextElement() {
            // Add 128 bytes extra buffer as worst case if the encoder (GIMP) added garbage bytes at the end
            return new SubStream(new DecoderStream(createStreamAdapter(stream, byteCounts[index++]), new PackBitsDecoder(), rowLength + 128), rowLength);
        }
    }
}
