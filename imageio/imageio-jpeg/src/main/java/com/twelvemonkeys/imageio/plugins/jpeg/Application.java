/*
 * Copyright (c) 2016, Harald Kuhr
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

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * Application.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Application.java,v 1.0 22/08/16 harald.kuhr Exp$
 */
class Application extends Segment {

    final String identifier;
    final byte[] data;

    Application(final int marker, final String identifier, final byte[] data) {
        super(marker);

        this.identifier = identifier; // NOTE: Some JPEGs contain APP segments without NULL-terminated identifier
        this.data = data;
    }

    InputStream data() {
        int offset = identifier.length() + 1;
        return new ByteArrayInputStream(data, offset, data.length - offset);
    }

    @Override
    public String toString() {
        return "APP" + (marker & 0x0f) + "/" + identifier + "[length: " + data.length + "]";
    }

    public static Application read(final int marker, final String identifier, final DataInput data, final int length) throws IOException {
        switch (marker) {
            case JPEG.APP0:
                // JFIF
                if ("JFIF".equals(identifier)) {
                    return JFIF.read(data, length);
                }
            case JPEG.APP1:
                // JFXX
                if ("JFXX".equals(identifier)) {
                    return JFXX.read(data, length);
                }
                // TODO: Exif?
            case JPEG.APP2:
                // ICC_PROFILE
                if ("ICC_PROFILE".equals(identifier)) {
                    return ICCProfile.read(data, length);
                }
            case JPEG.APP14:
                // Adobe
                if ("Adobe".equals(identifier)) {
                    return AdobeDCT.read(data, length);
                }

            default:
                // Generic APPn segment
                byte[] bytes = new byte[Math.max(0, length - 2)];
                data.readFully(bytes);
                return new Application(marker, identifier, bytes);
        }
    }
}
