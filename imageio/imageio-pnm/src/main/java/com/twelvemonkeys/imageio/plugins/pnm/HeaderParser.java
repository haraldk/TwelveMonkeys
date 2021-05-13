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

package com.twelvemonkeys.imageio.plugins.pnm;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

import static com.twelvemonkeys.lang.Validate.notNull;

abstract class HeaderParser {
    protected final ImageInputStream input;

    protected HeaderParser(final ImageInputStream input) {
        this.input = notNull(input);
    }

    public abstract PNMHeader parse() throws IOException;

    public static PNMHeader parse(ImageInputStream input) throws IOException {
        short type = input.readShort();

        return createParser(input, type).parse();
    }

    private static HeaderParser createParser(final ImageInputStream input, final short type) throws IOException {
        switch (type) {
            case PNM.PBM_PLAIN:
            case PNM.PBM:
            case PNM.PGM_PLAIN:
            case PNM.PGM:
            case PNM.PPM_PLAIN:
            case PNM.PPM:
                return new PNMHeaderParser(input, type);
            case PNM.PAM:
                return new PAMHeaderParser(input);
            case PNM.PFM_GRAY:
            case PNM.PFM_RGB:
                return new PFMHeaderParser(input, type);
            default:
                throw new IIOException("Unexpected type for PBM, PGM or PPM format: " + type);
        }
    }
}
