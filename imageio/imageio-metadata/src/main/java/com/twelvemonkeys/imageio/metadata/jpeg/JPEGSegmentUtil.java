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

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.psd.PSD;
import com.twelvemonkeys.imageio.metadata.psd.PSDReader;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.metadata.xmp.XMP;
import com.twelvemonkeys.imageio.metadata.xmp.XMPReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static com.twelvemonkeys.lang.Validate.notNull;

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
    public static final Map<Integer, List<String>> APP_SEGMENTS = Collections.unmodifiableMap(new AllAppSegmentsMap());

    private JPEGSegmentUtil() {}

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
        readSOI(notNull(stream, "stream"));

        List<JPEGSegment> segments = Collections.emptyList();

        JPEGSegment segment;
        try {
            do {
                segment = readSegment(stream, segmentIdentifiers);
//                System.err.println("segment: " + segment);

                if (isRequested(segment, segmentIdentifiers)) {
                    if (segments == Collections.EMPTY_LIST) {
                        segments = new ArrayList<>();
                    }

                    segments.add(segment);
                }
            }
            while (!isImageDone(segment));
        }
        catch (EOFException ignore) {
            // Just end here, in case of malformed stream
        }

        // TODO: Should probably skip until EOI, so that multiple invocations succeeds for multiple image streams.

        return segments;
    }

    private static boolean isRequested(JPEGSegment segment, Map<Integer, List<String>> segmentIdentifiers) {
        return (segmentIdentifiers.containsKey(segment.marker) &&
                (segment.identifier() == null && segmentIdentifiers.get(segment.marker) == null || containsSafe(segment, segmentIdentifiers)));
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
            if (data[offset + i] == 0 || i > 255) {
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

    static JPEGSegment readSegment(final ImageInputStream stream, final Map<Integer, List<String>> segmentIdentifiers) throws IOException {
//        int trash = 0;
        int marker = stream.readUnsignedByte();

        while (!isKnownJPEGMarker(marker)) {
            // Skip trash padding before the marker
            while (marker != 0xff) {
                marker = stream.readUnsignedByte();
//            trash++;
            }

//        if (trash != 0) {
            // TODO: Issue warning?
//            System.err.println("trash: " + trash);
//        }

            marker = 0xff00 | stream.readUnsignedByte();

            // Skip over 0xff padding between markers
            while (marker == 0xffff) {
                marker = 0xff00 | stream.readUnsignedByte();
            }
        }

        if ((marker >> 8 & 0xff) != 0xff) {
            throw new IIOException(String.format("Bad marker: %04x", marker));
        }

        int length = stream.readUnsignedShort(); // Length including length field itself

        byte[] data;

        if (segmentIdentifiers.containsKey(marker)) {
            data = new byte[Math.max(0, length - 2)];
            stream.readFully(data);
        }
        else {
            if (JPEGSegment.isAppSegmentMarker(marker)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
                int read;

                // NOTE: Read until null-termination (0) or EOF
                while ((read = stream.read()) > 0) {
                    buffer.write(read);
                }

                data = buffer.toByteArray();

                stream.skipBytes(length - 3 - data.length);
            }
            else {
                data = null;
                stream.skipBytes(length - 2);
            }
        }

        return new JPEGSegment(marker, data, length);
    }

    public static boolean isKnownJPEGMarker(final int marker) {
        switch (marker) {
            case JPEG.SOI:
            case JPEG.EOI:
            case JPEG.DHT:
            case JPEG.SOS:
            case JPEG.DQT:
            case JPEG.COM:
            case JPEG.SOF0:
            case JPEG.SOF1:
            case JPEG.SOF2:
            case JPEG.SOF3:
            case JPEG.SOF5:
            case JPEG.SOF6:
            case JPEG.SOF7:
            case JPEG.SOF9:
            case JPEG.SOF10:
            case JPEG.SOF11:
            case JPEG.SOF13:
            case JPEG.SOF14:
            case JPEG.SOF15:
            case JPEG.SOF55:
            case JPEG.APP0:
            case JPEG.APP1:
            case JPEG.APP2:
            case JPEG.APP3:
            case JPEG.APP4:
            case JPEG.APP5:
            case JPEG.APP6:
            case JPEG.APP7:
            case JPEG.APP8:
            case JPEG.APP9:
            case JPEG.APP10:
            case JPEG.APP11:
            case JPEG.APP12:
            case JPEG.APP13:
            case JPEG.APP14:
            case JPEG.APP15:
            case JPEG.DRI:
            case JPEG.TEM:
            case JPEG.DAC:
            case JPEG.DHP:
            case JPEG.DNL:
            case JPEG.EXP:
            case JPEG.LSE:
                return true;
            default:
                return false;
        }
    }

    private static class AllIdsList extends ArrayList<String> {
        @Override
        public String toString() {
            return "[All ids]";
        }

        @Override
        public boolean contains(final Object o) {
            return true;
        }
    }

    private static class AllSegmentsMap extends HashMap<Integer, List<String>> {
        @Override
        public String toString() {
            return "{All segments}";
        }

        @Override
        public List<String> get(final Object key) {
            return key instanceof Integer && JPEGSegment.isAppSegmentMarker((Integer) key) ? ALL_IDS : null;

        }

        @Override
        public boolean containsKey(final Object key) {
            return true;
        }
    }

    private static class AllAppSegmentsMap extends HashMap<Integer, List<String>> {
        @Override
        public String toString() {
            return "{All APPn segments}";
        }

        @Override
        public List<String> get(final Object key) {
            return containsKey(key) ? ALL_IDS : null;

        }

        @Override
        public boolean containsKey(Object key) {
            return key instanceof Integer && JPEGSegment.isAppSegmentMarker((Integer) key);
        }
    }

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            if (args.length > 1) {
                System.out.println("File: " + arg);
                System.out.println("------");
            }

            List<JPEGSegment> segments = readSegments(ImageIO.createImageInputStream(new File(arg)), ALL_SEGMENTS);

            for (JPEGSegment segment : segments) {
                System.err.println("segment: " + segment);

                if ("Exif".equals(segment.identifier())) {
                    ImageInputStream stream = new ByteArrayImageInputStream(segment.data, segment.offset() + 1, segment.length() - 1);

                    // Root entry is TIFF, that contains the EXIF sub-IFD
                    Directory tiff = new TIFFReader().read(stream);
                    System.err.println("EXIF: " + tiff);
                }
                else if (XMP.NS_XAP.equals(segment.identifier())) {
                    Directory xmp = new XMPReader().read(new ByteArrayImageInputStream(segment.data, segment.offset(), segment.length()));
                    System.err.println("XMP: " + xmp);
                    System.err.println(TIFFReader.HexDump.dump(segment.data));
                }
                else if ("Photoshop 3.0".equals(segment.identifier())) {
                    // TODO: The "Photoshop 3.0" segment contains several image resources, of which one might contain
                    //       IPTC metadata. Probably duplicated in the XMP though...
                    ImageInputStream stream = new ByteArrayImageInputStream(segment.data, segment.offset(), segment.length());
                    Directory psd = new PSDReader().read(stream);
                    Entry iccEntry = psd.getEntryById(PSD.RES_ICC_PROFILE);
                    if (iccEntry != null) {
                        ICC_ColorSpace colorSpace = new ICC_ColorSpace(ICC_Profile.getInstance((byte[]) iccEntry.getValue()));
                        System.err.println("colorSpace: " + colorSpace);
                    }
                    System.err.println("PSD: " + psd);
                    System.err.println(TIFFReader.HexDump.dump(segment.data));
                }
                else if ("ICC_PROFILE".equals(segment.identifier())) {
                    // Skip
                }
                else {
                    System.err.println(TIFFReader.HexDump.dump(segment.data));
                }
            }

            if (args.length > 1) {
                System.out.println("------");
                System.out.println();
            }
        }
    }
}
