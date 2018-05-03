/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.hdr;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * HDRHeader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRHeader.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
final class HDRHeader {
    private static final String KEY_FORMAT = "FORMAT=";
    private static final String KEY_PRIMARIES = "PRIMARIES=";
    private static final String KEY_EXPOSURE = "EXPOSURE=";
    private static final String KEY_GAMMA = "GAMMA=";
    private static final String KEY_SOFTWARE = "SOFTWARE=";

    private int width;
    private int height;

    private String software;

    public static HDRHeader read(final ImageInputStream stream) throws IOException {
        HDRHeader header = new HDRHeader();

        while (true) {
            String line = stream.readLine().trim();

            if (line.isEmpty()) {
                // This is the last line before the dimensions
                break;
            }

            if (line.startsWith("#?")) {
                // Program specifier, don't need that...
            }
            else if (line.startsWith("#")) {
                // Comment (ignore)
            }
            else if (line.startsWith(KEY_FORMAT)) {
                String format = line.substring(KEY_FORMAT.length()).trim();

                if (!format.equals("32-bit_rle_rgbe")) {
                    throw new IIOException("Unsupported format \"" + format + "\"(expected \"32-bit_rle_rgbe\")");
                }
                // TODO: Support the 32-bit_rle_xyze format
            }
            else if (line.startsWith(KEY_PRIMARIES)) {
                // TODO: We are going to need these values...
                // Should contain 8 (RGB + white point) coordinates
            }
            else if (line.startsWith(KEY_EXPOSURE)) {
                // TODO: We are going to need these values...
            }
            else if (line.startsWith(KEY_GAMMA)) {
                // TODO: We are going to need these values...
            }
            else if (line.startsWith(KEY_SOFTWARE)) {
                header.software = line.substring(KEY_SOFTWARE.length()).trim();
            }
            else {
                // ...ignore
            }
        }

        // TODO: Proper parsing of width/height and orientation!
        String dimensionsLine = stream.readLine().trim();
        String[] dims = dimensionsLine.split("\\s");

        if (dims[0].equals("-Y") && dims[2].equals("+X")) {
            header.height = Integer.parseInt(dims[1]);
            header.width = Integer.parseInt(dims[3]);

            return header;
        }
        else {
            throw new IIOException("Unsupported RGBE orientation (expected \"-Y ... +X ...\")");
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getSoftware() {
        return software;
    }
}
