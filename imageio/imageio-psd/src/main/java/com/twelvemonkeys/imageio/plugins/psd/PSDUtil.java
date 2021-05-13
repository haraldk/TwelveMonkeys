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

import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

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

    static DataInputStream createPackBitsStream(final ImageInputStream pInput, long pLength) {
        return new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(pInput, pLength), new PackBitsDecoder()));
    }

    static DataInputStream createZipStream(final ImageInputStream pInput, long pLength) {
        return new DataInputStream(new ZipInputStream(IIOUtil.createStreamAdapter(pInput, pLength)));
    }

    static DataInputStream createZipPredictorStream(final ImageInputStream pInput, long pLength) {
        throw new UnsupportedOperationException("Method createZipPredictonStream not implemented");
    }

    public static float fixedPointToFloat(int pFP) {
        return ((pFP & 0xffff0000) >> 16) + (pFP & 0xffff) / (float) 0xffff;
    }
}
