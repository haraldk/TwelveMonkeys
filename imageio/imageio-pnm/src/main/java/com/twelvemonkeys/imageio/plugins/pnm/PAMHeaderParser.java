/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PAMHeaderParser extends HeaderParser {

    static final String ENDHDR = "ENDHDR";
    static final String WIDTH = "WIDTH";
    static final String HEIGHT = "HEIGHT";
    static final String MAXVAL = "MAXVAL";
    static final String DEPTH = "DEPTH";
    static final String TUPLTYPE = "TUPLTYPE";

    public PAMHeaderParser(final ImageInputStream input) {
        super(input);
    }

    @Override public PNMHeader parse() throws IOException {
        /* Note: Comments are allowed
        P7
        WIDTH 227
        HEIGHT 149
        DEPTH 3
        MAXVAL 255
        TUPLTYPE RGB
        ENDHDR
        */

        int width = -1;
        int height = -1;
        int depth = -1;
        int maxVal = -1;
        TupleType tupleType = null;
        List<String> comments = new ArrayList<String>();

        String line;
        while ((line = input.readLine()) != null && !line.startsWith(ENDHDR)) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(WIDTH)) {
                width = Integer.parseInt(line.substring(WIDTH.length() + 1));
            }
            else if (line.startsWith(HEIGHT)) {
                height = Integer.parseInt(line.substring(HEIGHT.length() + 1));
            }
            else if (line.startsWith(DEPTH)) {
                depth = Integer.parseInt(line.substring(DEPTH.length() + 1));
            }
            else if (line.startsWith(MAXVAL)) {
                maxVal = Integer.parseInt(line.substring(MAXVAL.length() + 1));
            }
            else if (line.startsWith(TUPLTYPE)) {
                tupleType = TupleType.valueOf(line.substring(TUPLTYPE.length() + 1));
            }
            else if (line.startsWith("#")) {
                comments.add(line.substring(1).trim());
            }
            else {
                throw new IIOException("Unknown PAM header token: '" + line + "'");
            }
        }

        if (tupleType == null) {
            // TODO: Assume a type, based on depth + maxVal, or at least, allow reading as raster
        }

        return new PNMHeader(PNM.PAM, tupleType, width, height, depth, maxVal, comments);
    }
}
