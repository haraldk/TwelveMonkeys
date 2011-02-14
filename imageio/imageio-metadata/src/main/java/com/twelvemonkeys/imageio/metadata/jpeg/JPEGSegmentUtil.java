/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  Neither the name "TwelveMonkeys" nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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

package com.twelvemonkeys.imageio.metadata.jpeg;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JPEGSegmentUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentUtil.java,v 1.0 24.01.11 17.37 haraldk Exp$
 */
public final class JPEGSegmentUtil {
    public static final Map<Integer, List<String>> ALL_SEGMENTS = Collections.emptyMap();

    private JPEGSegmentUtil() {}

    // TODO: Allow for multiple images (multiple SOI markers), using specified index?
    public static List<Segment> readSegments(final ImageInputStream stream, final int appMarker, final String segmentName) throws IOException {
        return readSegments(stream, Collections.singletonMap(appMarker, Collections.singletonList(segmentName)));
    }

    public static List<Segment> readSegments(final ImageInputStream stream, Map<Integer, List<String>> segmentIdentifiers) throws IOException {
        readSOI(stream);

        List<Segment> segments = Collections.emptyList();

        Segment segment;
        try {
            while (!isImageDone(segment = readSegment(stream, segmentIdentifiers))) {
//            while (!isImageDone(segment = readSegment(stream, ALL_SEGMENTS))) {
//                System.err.println("segment: " + segment);

                if (isRequested(segment, segmentIdentifiers)) {
                    if (segments == Collections.EMPTY_LIST) {
                        segments = new ArrayList<Segment>();
                    }

                    segments.add(segment);
                }
            }
        }
        catch (EOFException ignore) {
            // Just end here, in case of malformed stream
        }

        return segments;
    }

    private static boolean isRequested(Segment segment, Map<Integer, List<String>> segmentIdentifiers) {
        return segmentIdentifiers == ALL_SEGMENTS ||
                (segmentIdentifiers.containsKey(segment.marker) &&
                (segment.identifier() == null && segmentIdentifiers.get(segment.marker) == null || containsSafe(segment, segmentIdentifiers)));
    }

    private static boolean containsSafe(Segment segment, Map<Integer, List<String>> segmentIdentifiers) {
        List<String> identifiers = segmentIdentifiers.get(segment.marker);
        return identifiers != null && identifiers.contains(segment.identifier());
    }

    private static boolean isImageDone(final Segment segment) {
        // We're done with this image if we encounter a SOS, EOI (or a new SOI, but that should never happen)
        return segment.marker == JPEG.SOS || segment.marker == JPEG.EOI || segment.marker == JPEG.SOI;
    }

    static String asNullTerminatedAsciiString(final byte[] data, final int offset) {
        for (int i = 0; i < data.length - offset; i++) {
            if (data[i] == 0 || i > 255) {
                return asAsciiString(data, offset, offset + i);
            }
        }

        return null;
    }

    static String asAsciiString(final byte[] data, final int offset, final int length) {
        return new String(data, offset, length, Charset.forName("ascii"));
    }

    static void readSOI(final ImageInputStream stream) throws IOException {
        if (stream.readUnsignedShort() != JPEG.SOI) {
            throw new IIOException("Not a JPEG stream");
        }
    }

    static Segment readSegment(final ImageInputStream stream, Map<Integer, List<String>> segmentIdentifiers) throws IOException {
        int marker = stream.readUnsignedShort();
        int length = stream.readUnsignedShort(); // Length including length field itself

        byte[] data;

        if (segmentIdentifiers == ALL_SEGMENTS || segmentIdentifiers.containsKey(marker)) {
            data = new byte[length - 2];
            stream.readFully(data);
        }
        else {
            data = null;
            stream.skipBytes(length - 2);
        }

        return new Segment(marker, data);
    }

    public static final class Segment {
        private final int marker;
        private final byte[] data;
        private String id;

        Segment(int marker, byte[] data) {
            this.marker = marker;
            this.data = data;
        }

        int segmentLength() {
            // This is the length field as read from the stream
            return data.length + 2;
        }

        public int marker() {
            return marker;
        }

        public String identifier() {
            if (id == null) {
                if (marker >= 0xFFE0 && marker <= 0xFFEF) {
                    // Only for APPn markers
                    id = asNullTerminatedAsciiString(data, 0);
                }
            }

            return id;
        }

        public InputStream data() {
            return new ByteArrayInputStream(data, offset(), length());
        }

        public int length() {
            return data.length - offset();
        }

        private int offset() {
            String identifier = identifier();
            return identifier == null ? 0 : identifier.length() + 1;
        }

        @Override
        public String toString() {
            return String.format("Segment[%04x/%s size: %d]", marker, identifier(), segmentLength());
        }
    }
}
