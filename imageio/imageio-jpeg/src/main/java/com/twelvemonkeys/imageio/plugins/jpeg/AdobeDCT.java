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
import java.io.IOException;

/**
 * AdobeDCTSegment
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AdobeDCTSegment.java,v 1.0 23.04.12 16:55 haraldk Exp$
 */
final class AdobeDCT extends Application {
    static final int Unknown = 0;
    static final int YCC = 1;
    static final int YCCK = 2;

    final int version;
    final int flags0;
    final int flags1;
    final int transform;

    private AdobeDCT(int version, int flags0, int flags1, int transform) {
        super(JPEG.APP14, "Adobe", new byte[]{'A', 'd', 'o', 'b', 'e', 0, (byte) version, (byte) (flags0 >> 8), (byte) (flags0 & 0xff), (byte) (flags1 >> 8), (byte) (flags1 & 0xff), (byte) transform});

        this.version = version; // 100 or 101
        this.flags0 = flags0;
        this.flags1 = flags1;
        this.transform = transform;
    }

    @Override
    public String toString() {
        return String.format(
                "AdobeDCT[ver: %d.%02d, flags: %s %s, transform: %d]",
                version / 100, version % 100, Integer.toBinaryString(flags0), Integer.toBinaryString(flags1), transform
        );
    }

    public static AdobeDCT read(final DataInput data, final int length) throws IOException {
        // TODO: Investigate http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6355567: 33/35 byte Adobe APP14 markers

        data.skipBytes(6); // A, d, o, b, e, \0

        // version (byte), flags (4bytes), color transform (byte: 0=unknown, 1=YCC, 2=YCCK)
        return new AdobeDCT(
                data.readUnsignedByte(),
                data.readUnsignedShort(),
                data.readUnsignedShort(),
                data.readUnsignedByte()
        );
    }
}
