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

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * JFIFSegment
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFIFSegment.java,v 1.0 23.04.12 16:52 haraldk Exp$
 */
final class JFIF extends Application {
    final int majorVersion;
    final int minorVersion;
    final int units;
    final int xDensity;
    final int yDensity;
    final int xThumbnail;
    final int yThumbnail;
    final byte[] thumbnail;

    private JFIF(int majorVersion, int minorVersion, int units, int xDensity, int yDensity, int xThumbnail, int yThumbnail, byte[] thumbnail, byte[] data) {
        super(JPEG.APP0, "JFIF", data);

        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.units = units;
        this.xDensity = xDensity;
        this.yDensity = yDensity;
        this.xThumbnail = xThumbnail;
        this.yThumbnail = yThumbnail;
        this.thumbnail = thumbnail;
    }

    @Override
    public String toString() {
        return String.format("APP0/JFIF v%d.%02d %dx%d %s (%s)", majorVersion, minorVersion, xDensity, yDensity, unitsAsString(), thumbnailToString());
    }

    private String unitsAsString() {
        switch (units) {
            case 0:
                return "(aspect only)";
            case 1:
                return "dpi";
            case 2:
                return "dpcm";
            default:
                return "(unknown unit)";
        }
    }

    private String thumbnailToString() {
        if (xThumbnail == 0 || yThumbnail == 0) {
            return "no thumbnail";
        }

        return String.format("thumbnail: %dx%d", xThumbnail, yThumbnail);
    }

    public static JFIF read(final DataInput data, int length) throws IOException {
        if (length < 2 + 5 + 9) {
            throw new EOFException();
        }

        data.readFully(new byte[5]);

        byte[] bytes = new byte[length - 2 - 5];
        data.readFully(bytes);

        int x, y;

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        return new JFIF(
                buffer.get() & 0xff,
                buffer.get() & 0xff,
                buffer.get() & 0xff,
                buffer.getShort() & 0xffff,
                buffer.getShort() & 0xffff,
                x = buffer.get() & 0xff,
                y = buffer.get() & 0xff,
                getBytes(buffer, Math.min(buffer.remaining(), x * y * 3)),
                bytes
        );
    }

    private static byte[] getBytes(ByteBuffer buffer, int len) {
        if (len == 0) {
            return null;
        }

        byte[] dst = new byte[len];
        buffer.get(dst);

        return dst;
    }
}
