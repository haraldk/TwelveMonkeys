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

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

/**
 * JFXXSegment
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFXXSegment.java,v 1.0 23.04.12 16:54 haraldk Exp$
 */
final class JFXX extends Application {
    public static final int JPEG = 0x10;
    public static final int INDEXED = 0x11;
    public static final int RGB  = 0x13;

    final int extensionCode;
    final byte[] thumbnail;

    private JFXX(final int extensionCode, final byte[] thumbnail, final byte[] data) {
        super(com.twelvemonkeys.imageio.metadata.jpeg.JPEG.APP0, "JFXX", data);

        this.extensionCode = extensionCode;
        this.thumbnail = thumbnail;
    }

    @Override
    public String toString() {
        return String.format("APP0/JFXX extension (%s thumb size: %d)", extensionAsString(), thumbnail.length);
    }

    private String extensionAsString() {
        switch (extensionCode) {
            case JPEG:
                return "JPEG";
            case INDEXED:
                return "Indexed";
            case RGB:
                return "RGB";
            default:
                return String.valueOf(extensionCode);
        }
    }

    public static JFXX read(final DataInput data, final int length) throws IOException {
        data.readFully(new byte[5]);

        byte[] bytes = new byte[length - 2 - 5];
        data.readFully(bytes);

        return new JFXX(
                bytes[0] & 0xff,
                bytes.length - 1 > 0 ? Arrays.copyOfRange(bytes, 1, bytes.length - 1) : null,
                bytes
        );
    }
}
