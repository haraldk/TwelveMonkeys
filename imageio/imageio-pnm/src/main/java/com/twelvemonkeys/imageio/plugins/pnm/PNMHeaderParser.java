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

import com.twelvemonkeys.io.FastByteArrayOutputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PNMHeaderParser extends HeaderParser {
    private final short fileType;
    private final TupleType tupleType;

    public PNMHeaderParser(final ImageInputStream input, final short type) {
        super(input);
        this.fileType = type;
        this.tupleType = asTupleType(type);
    }

    static TupleType asTupleType(int fileType) {
        switch (fileType) {
            case PNM.PBM:
            case PNM.PBM_PLAIN:
                return TupleType.BLACKANDWHITE_WHITE_IS_ZERO;
            case PNM.PGM:
            case PNM.PGM_PLAIN:
                return TupleType.GRAYSCALE;
            case PNM.PPM:
            case PNM.PPM_PLAIN:
                return TupleType.RGB;
            default:
                throw new AssertionError("Illegal PNM type :" + fileType);
        }
    }

    @Override
    public PNMHeader parse() throws IOException {
        int width = 0;
        int height = 0;
        int maxSample = tupleType == TupleType.BLACKANDWHITE_WHITE_IS_ZERO ? 1 : 0; // PBM has no maxSample line

        List<String> comments = new ArrayList<>();

        StringBuilder tokenBuffer = new StringBuilder();

        while (width == 0 || height == 0 || maxSample == 0) {
            tokenBuffer.delete(0, tokenBuffer.length());

            while (tokenBuffer.length() < 16) { // Limit reads if we should read across into the binary part...
                byte read = input.readByte();

                if (read == '#') {
                    // Read rest of the line as comment
                    String comment = readLineUTF8(input).trim();

                    if (!comment.isEmpty()) {
                        comments.add(comment);
                    }

                    break;
                }
                else if (Character.isWhitespace((char) read)) {
                    if (tokenBuffer.length() > 0) {
                        break;
                    }
                }
                else {
                    tokenBuffer.append((char) read);
                }
            }

            String token = tokenBuffer.toString().trim();

            if (!token.isEmpty()) {
                // We have tokens...
                if (width == 0) {
                    width = Integer.parseInt(token);
                }
                else if (height == 0) {
                    height = Integer.parseInt(token);
                }
                else {
                    maxSample = Integer.parseInt(token);
                }
            }
        }

        return new PNMHeader(fileType, tupleType, width, height, tupleType.getSamplesPerPixel(), maxSample, comments);
    }

    // Similar to DataInput.readLine, except it uses UTF8 encoding
    private static String readLineUTF8(final ImageInputStream input) throws IOException {
        ByteArrayOutputStream buffer = new FastByteArrayOutputStream(128);

        int value;
        do {
            switch (value = input.read()) {
                case '\r':
                    // Check for CR + LF pattern and skip, otherwise fall through
                    if (input.read() != '\n') {
                        input.seek(input.getStreamPosition() - 1);
                    }
                case '\n':
                case -1:
                    value = -1;
                    break;
                default:
                    buffer.write(value);
            }
        } while (value != -1);

        return buffer.toString("UTF8");
    }
}
