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
import java.util.*;

/**
 * JPEGSegmentUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentUtil.java,v 1.0 24.01.11 17.37 haraldk Exp$
 */
public final class JPEGSegmentUtil {
    public static final List<String> ALL_IDS = Collections.unmodifiableList(new AllIdsList());
    public static final Map<Integer, List<String>> ALL_SEGMENTS = Collections.unmodifiableMap(new AllSegmentsMap());
    public static final Map<Integer, List<String>> APP_SEGMENTS = Collections.unmodifiableMap(createAppSegmentsMap());

    private JPEGSegmentUtil() {}

    private static Map<Integer, List<String>> createAppSegmentsMap() {
        Map<Integer, List<String>> identifiers = new HashMap<Integer, List<String>>();

        for (int i = 0xFFE0; i <= 0xFFEF; i++) {
            identifiers.put(i, JPEGSegmentUtil.ALL_IDS);
        }

        return identifiers;
    }

    /**
     * Reads the requested JPEG segments from the stream.
     * The stream position must be directly before the SOI marker, and only segments for the current image is read.
     *
     * @param stream the stream to read from.
     * @param marker the segment marker to read
     * @param identifier the identifier to read, or {@code null} to match any segment
     * @return a list of segments with the given app marker and optional identifier. If no segments are found, an
     *         empty list is returned.
     * @throws IIOException if a JPEG format exception occurs during reading
     * @throws IOException if an I/O exception occurs during reading
     */
    public static List<JPEGSegment> readSegments(final ImageInputStream stream, final int marker, final String identifier) throws IOException {
        return readSegments(stream, Collections.singletonMap(marker, identifier != null ? Collections.singletonList(identifier) : ALL_IDS));
    }

    /**
     * Reads the requested JPEG segments from the stream.
     * The stream position must be directly before the SOI marker, and only segments for the current image is read.
     *
     * @param stream the stream to read from.
     * @param segmentIdentifiers the segment identifiers
     * @return a list of segments with the given app markers and optional identifiers. If no segments are found, an
     *         empty list is returned.
     * @throws IIOException if a JPEG format exception occurs during reading
     * @throws IOException if an I/O exception occurs during reading
     *
     * @see #ALL_SEGMENTS
     * @see #APP_SEGMENTS
     * @see #ALL_IDS
     */
    public static List<JPEGSegment> readSegments(final ImageInputStream stream, final Map<Integer, List<String>> segmentIdentifiers) throws IOException {
        readSOI(stream);

        List<JPEGSegment> segments = Collections.emptyList();

        JPEGSegment segment;
        try {
//            while (!isImageDone(segment = readSegment(stream, segmentIdentifiers))) {
            while (!isImageDone(segment = readSegment(stream, ALL_SEGMENTS))) {
                System.err.println("segment: " + segment);

                if (isRequested(segment, segmentIdentifiers)) {
                    if (segments == Collections.EMPTY_LIST) {
                        segments = new ArrayList<JPEGSegment>();
                    }

                    segments.add(segment);
                }
            }
        }
        catch (EOFException ignore) {
            // Just end here, in case of malformed stream
        }

        // TODO: Should probably skip until EOI, so that multiple invocations succeeds for multiple image streams.

        return segments;
    }

    private static boolean isRequested(JPEGSegment segment, Map<Integer, List<String>> segmentIdentifiers) {
        return segmentIdentifiers == ALL_SEGMENTS ||
                (segmentIdentifiers.containsKey(segment.marker) && (segmentIdentifiers.get(segment.marker) == ALL_IDS ||
                (segment.identifier() == null && segmentIdentifiers.get(segment.marker) == null || containsSafe(segment, segmentIdentifiers))));
    }

    private static boolean containsSafe(JPEGSegment segment, Map<Integer, List<String>> segmentIdentifiers) {
        List<String> identifiers = segmentIdentifiers.get(segment.marker);
        return identifiers != null && identifiers.contains(segment.identifier());
    }

    private static boolean isImageDone(final JPEGSegment segment) {
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

    static JPEGSegment readSegment(final ImageInputStream stream, Map<Integer, List<String>> segmentIdentifiers) throws IOException {
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

        return new JPEGSegment(marker, data);
    }

    private static class AllIdsList extends ArrayList<String> {
        @Override
        public String toString() {
            return "[All ids]";
        }
    }

    private static class AllSegmentsMap extends HashMap<Integer, List<String>> {
        @Override
        public String toString() {
            return "{All segments}";
        }
    }
}
