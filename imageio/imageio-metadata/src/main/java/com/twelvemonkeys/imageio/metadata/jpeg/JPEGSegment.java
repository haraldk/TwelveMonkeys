/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.jpeg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a JPEG segment.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegment.java,v 1.0 02.03.11 10.44 haraldk Exp$
 */
public final class JPEGSegment implements Serializable {
    final int marker;
    final byte[] data;
    private final int length;

    private transient String id;

    JPEGSegment(int marker, byte[] data, int length) {
        this.marker = marker;
        this.data = data;
        this.length = length;
    }

    public int segmentLength() {
        // This is the length field as read from the stream
        return length;
    }

    public InputStream segmentData() {
        return data != null ? new ByteArrayInputStream(data) : null;
    }

    public int marker() {
        return marker;
    }

    public String identifier() {
        if (id == null) {
            if (isAppSegmentMarker(marker)) {
                // Only for APPn markers
                id = JPEGSegmentUtil.asNullTerminatedAsciiString(data, 0);
            }
        }

        return id;
    }

    static boolean isAppSegmentMarker(final int marker) {
        return marker >= 0xFFE0 && marker <= 0xFFEF;
    }

    // TODO: Consider returning an ImageInputStream and use ByteArrayImageInputStream directly, for less wrapping and better performance
    // TODO: BUT: Must find a way to skip padding in/after segment identifier (eg: Exif has null-term + null-pad, ICC_PROFILE has only null-term). Is data always word-aligned?
    public InputStream data() {
        return data != null ? new ByteArrayInputStream(data, offset(), length()) : null;
    }

    public int length() {
        return data != null ? data.length - offset() : 0;
    }

    int offset() {
        String identifier = identifier();

        return identifier == null ? 0 : identifier.length() + 1;
    }

    @Override
    public String toString() {
        String identifier = identifier();

        if (identifier != null) {
            return String.format("JPEGSegment[%04x/%s size: %d]", marker, identifier, segmentLength());
        }

        return String.format("JPEGSegment[%04x size: %d]", marker, segmentLength());
    }

    @Override
    public int hashCode() {
        String identifier = identifier();

        return marker() << 16 | (identifier != null ? identifier.hashCode() : 0) & 0xFFFF;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof JPEGSegment &&
                ((JPEGSegment) other).marker == marker && Arrays.equals(((JPEGSegment) other).data, data);
    }
}
