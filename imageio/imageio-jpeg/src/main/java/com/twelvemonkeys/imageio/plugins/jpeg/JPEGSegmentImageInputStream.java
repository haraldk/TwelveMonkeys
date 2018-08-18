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

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil.isKnownJPEGMarker;
import static com.twelvemonkeys.lang.Validate.notNull;
import static java.util.Arrays.copyOf;

/**
 * ImageInputStream implementation that filters out or rewrites
 * certain JPEG segments.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentImageInputStream.java,v 1.0 30.01.12 16:15 haraldk Exp$
 */
final class JPEGSegmentImageInputStream extends ImageInputStreamImpl {
    // TODO: Rewrite JPEGSegment (from metadata) to store stream pos/length, and be able to replay data, and use instead of Segment?
    // TODO: Support multiple JPEG streams (SOI...EOI, SOI...EOI, ...) in a single file

    final private ImageInputStream stream;
    final private JPEGSegmentStreamWarningListener warningListener;

    final private ComponentIdSet componentIds = new ComponentIdSet();

    private final List<Segment> segments = new ArrayList<Segment>(64);
    private int currentSegment = -1;
    private Segment segment;


    JPEGSegmentImageInputStream(final ImageInputStream stream, final JPEGSegmentStreamWarningListener warningListener) {
        this.stream = notNull(stream, "stream");
        this.warningListener = notNull(warningListener, "warningListener");
    }

    JPEGSegmentImageInputStream(final ImageInputStream stream) {
        this(stream, JPEGSegmentStreamWarningListener.NULL_LISTENER);
    }

    private void processWarningOccured(final String warning) {
        warningListener.warningOccurred(warning);
    }

    private Segment fetchSegment() throws IOException {
        // Stream init
        if (currentSegment == -1) {
            streamInit();
        }
        else {
            segment = segments.get(currentSegment);
        }

        if (streamPos >= segment.end()) {
            // Go forward in cache
            int cachedSegment = currentSegment;
            while (++cachedSegment < segments.size()) {
                currentSegment = cachedSegment;
                segment = segments.get(currentSegment);

                if (streamPos >= segment.start && streamPos < segment.end()) {
                    segment.seek(stream, streamPos);

                    return segment;
                }
            }

            stream.seek(segment.realEnd());

            // Scan forward
            while (true) {
                int trash = 0;
                int marker = stream.readUnsignedByte();

                while (!isKnownJPEGMarker(marker)) {
                    marker &= 0xff;

                    // Skip bad padding before the marker
                    while (marker != 0xff) {
                        marker = stream.readUnsignedByte();
                        trash++;
                    }

                    marker = 0xff00 | stream.readUnsignedByte();

                    // Skip over 0xff padding between markers
                    while (marker == 0xffff) {
                        marker = 0xff00 | stream.readUnsignedByte();
                        trash++;
                    }
                }

                if (trash != 0) {
                    // NOTE: We previously allowed these bytes to pass through to the native reader, as it could cope
                    // and issued the correct warning. However, the native metadata chokes on it, so we'll mask it out.
                    processWarningOccured(String.format("Corrupt JPEG data: %d extraneous bytes before marker 0x%02x", trash, marker & 0xff));
                }

                long realPosition = stream.getStreamPosition() - 2;

                // We are now handling all important segments ourselves, except APP1/Exif and APP14/Adobe,
                // as these segments affects image decoding.
                boolean appSegmentMarker = isAppSegmentMarker(marker);
                boolean isApp14Adobe = marker == JPEG.APP14 && isAppSegmentWithId("Adobe", stream);
                boolean isApp1Exif = marker == JPEG.APP1 && isAppSegmentWithId("Exif", stream);

                if (appSegmentMarker && !(isApp1Exif || isApp14Adobe)) {
                    int length = stream.readUnsignedShort(); // Length including length field itself
                    stream.seek(realPosition + 2 + length);  // Skip marker (2) + length
                }
                else {
                    if (marker == JPEG.EOI) {
                        segment = new Segment(marker, realPosition, segment.end(), 2);
                        segments.add(segment);
                    }
                    else {
                        // Length including length field itself
                        long length = 2 + stream.readUnsignedShort();

                        if (isApp14Adobe && length != 16) {
                            // Need to rewrite this segment, so that it gets length 16 and discard the remaining bytes...
                            segment = new AdobeAPP14Replacement(realPosition, segment.end(), length, stream);
                        }
                        else if (marker == JPEG.DQT) {
                            // TODO: Do we need to know SOF precision before determining if the DQT precision is bad?
                            // Inspect segment, see if we have 16 bit precision (assuming segments will not contain
                            // multiple quality tables with varying precision)
                            int qtInfo = stream.read();
                            if ((qtInfo & 0x10) == 0x10) {
                                processWarningOccured("16 bit DQT encountered");
                                segment = new DownsampledDQTReplacement(realPosition, segment.end(), length, qtInfo, stream);
                            }
                            else {
                                segment = new Segment(marker, realPosition, segment.end(), length);
                            }
                        }
                        else if (isSOFMarker(marker)) {
                            // TODO: Warning + ignore if we already have a SOF
                            // Replace duplicate SOFn component ids
                            byte[] data = readReplaceDuplicateSOFnComponentIds(marker, length);
                            segment = new ReplacementSegment(marker, realPosition, segment.end(), length, data);
                        }
                        else if (marker == JPEG.SOS) {
                            // Replace duplicate SOS component selectors
                            byte[] data = readReplaceDuplicateSOSComponentSelectors(length);

                            segment = new ReplacementSegment(marker, realPosition, segment.end(), length, data);
                        }
                        else {
                            segment = new Segment(marker, realPosition, segment.end(), length);
                        }

                        segments.add(segment);
                    }

                    currentSegment = segments.size() - 1;

                    if (marker == JPEG.SOS) {
                        // Treat rest of stream as a single segment (scanning for EOI is too much work)
                        // TODO: For progressive, there will be more than one SOS...
                        segments.add(new Segment(-1, segment.realEnd(), segment.end(), Long.MAX_VALUE - segment.realEnd()));
                    }

                    if (streamPos >= segment.start && streamPos < segment.end()) {
                        segment.seek(stream, streamPos);

                        break;
                    }
                    else {
                        stream.seek(segment.realEnd());
                        // Else continue forward scan
                    }
                }
            }
        }
        else if (streamPos < segment.start) {
            // Go back in cache
            int cachedSegment = currentSegment;
            while (--cachedSegment >= 0) {
                currentSegment = cachedSegment;
                segment = segments.get(currentSegment);

                if (streamPos >= segment.start && streamPos < segment.end()) {
                    segment.seek(stream, streamPos);

                    break;
                }
            }
        }
        else {
            segment.seek(stream, streamPos);
        }

        return segment;
    }

    private byte[] readReplaceDuplicateSOSComponentSelectors(final long length) throws IOException {
        // See: http://www.hackerfactor.com/blog/index.php?/archives/588-JPEG-Patches.html
        byte[] data = readSegment(JPEG.SOS, (int) length, stream);

        // Detect duplicates
        ComponentIdSet componentSelectors = new ComponentIdSet();
        boolean duplicatesFound = false;
        int off = 5;

        while (off < length - 3) {
            int selector = data[off] & 0xff;
            if (!componentSelectors.add(selector)) {
                processWarningOccured(String.format("Duplicate component ID %d in SOS", selector));
                duplicatesFound = true;
            }

            off += 2;
        }

        // Replace all with the component ids in order, as this is the most likely situation
        if (duplicatesFound) {
            off = 5;

            for (int i = 0; i < componentIds.size() && off < length - 3; i++, off += 2) {
                data[off] = (byte) componentIds.get(i);
            }
        }

        return data;
    }

    private byte[] readReplaceDuplicateSOFnComponentIds(final int marker, final long length) throws IOException {
        byte[] data = readSegment(marker, (int) length, stream);

        int off = 10;

        while (off < length) {
            int id = data[off] & 0xff;
            if (!componentIds.add(id)) {
                processWarningOccured(String.format("Duplicate component ID %d in SOF", id));

                id++;
                while (!componentIds.add(id) && componentIds.size() <= 16) {
                    id++;
                }

                data[off] = (byte) id;
            }

            off += 3;
        }

        return data;
    }

    private static byte[] readSegment(final int marker, final int length, final ImageInputStream stream) throws IOException {
        byte[] data = new byte[length];

        data[0] = (byte) ((marker >> 8) & 0xff);
        data[1] = (byte) (marker & 0xff);
        data[2] = (byte) (((length - 2) >> 8) & 0xff);
        data[3] = (byte) ((length - 2) & 0xff);

        stream.readFully(data, 4, length - 4);

        return data;
    }

    private static boolean isAppSegmentWithId(final String segmentId, final ImageInputStream stream) throws IOException {
        notNull(segmentId, "segmentId");

        stream.mark();

        try {
            int length = stream.readUnsignedShort(); // Length including length field itself

            byte[] data = new byte[Math.min(segmentId.length() + 1, length - 2)];
            stream.readFully(data);

            return segmentId.equals(asNullTerminatedAsciiString(data, 0));
        }
        finally {
            stream.reset();
        }
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

    private void streamInit() throws IOException {
        stream.seek(0);

        try {
            int soi = stream.readUnsignedShort();

            if (soi != JPEG.SOI) {
                throw new IIOException(String.format("Not a JPEG stream (starts with: 0x%04x, expected SOI: 0x%04x)", soi, JPEG.SOI));
            }

            segment = new Segment(soi, 0, 0, 2);

            segments.add(segment);
            currentSegment = segments.size() - 1; // 0
        }
        catch (EOFException eof) {
            throw new IIOException(String.format("Not a JPEG stream (short stream. expected SOI: 0x%04x)", JPEG.SOI), eof);
        }
    }

    static boolean isAppSegmentMarker(final int marker) {
        return marker >= JPEG.APP0 && marker <= JPEG.APP15;
    }

    static boolean isSOFMarker(final int marker) {
        switch (marker) {
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
                return true;
            default:
                return false;
        }
    }

    private void repositionAsNecessary() throws IOException {
        if (segment == null || streamPos < segment.start || streamPos >= segment.end()) {
            try {
                fetchSegment();
            }
            catch (EOFException ignore) {
                segments.add(new Segment(0, segment.realEnd(), segment.end(), Integer.MAX_VALUE * 2L - segment.realEnd()));
                // This might happen if the segment lengths in the stream are bad.
                // We MUST leave internal state untouched in this case.
                // We ignore this exception here, but client code will get
                // an EOFException (or -1 return code) on subsequent reads.
            }
        }
    }

    @Override
    public int read() throws IOException {
        bitOffset = 0;

        repositionAsNecessary();

        int read = segment.read(stream);

        if (read != -1) {
            streamPos++;
        }

        return read;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        bitOffset = 0;

        // NOTE: There is a bug in the JPEGMetadata constructor (JPEGBuffer.loadBuf() method) that expects read to
        // always read len bytes. Therefore, this is more complicated than it needs to... :-/
        int total = 0;

        while (total < len) {
            repositionAsNecessary();

            long bytesLeft = segment.end() - streamPos; // If no more bytes after reposition, we're at EOF
            int count = bytesLeft <= 0 ? -1 : segment.read(stream, b, off + total, (int) Math.min(len - total, bytesLeft));

            if (count == -1) {
                // EOF
                if (total == 0) {
                    return -1;
                }

                break;
            }
            else {
                streamPos += count;
                total += count;
            }
        }

        return total;
    }

    @SuppressWarnings({"FinalizeDoesntCallSuperFinalize"})
    @Override
    protected void finalize() throws Throwable {
        // Empty finalizer (for improved performance; no need to call super.finalize() in this case)
    }

    static class Segment {
        final int marker;

        final long realStart;
        final long start;
        final long length;

        Segment(final int marker, final long realStart, final long start, final long length) {
            this.marker = marker;
            this.realStart = realStart;
            this.start = start;
            this.length = length;
        }

        long realEnd() {
            return realStart + length;
        }

        long end() {
            return start + length;
        }

        public void seek(final ImageInputStream stream, final long newPos) throws IOException {
            stream.seek(realStart + newPos - start);
        }

        public int read(final ImageInputStream stream) throws IOException {
            return stream.read();
        }

        public int read(final ImageInputStream stream, byte[] b, int off, int len) throws IOException {
            return stream.read(b, off, len);
        }

        @Override
        public String toString() {
            return String.format("0x%04x[%d-%d]", marker, realStart, realEnd());
        }
    }

    /**
     * Workaround for a known bug in com.sun.imageio.plugins.jpeg.AdobeMarkerSegment, leaving the buffer in an
     * inconsistent state, if the length of the APP14/Adobe is not exactly 16 bytes.
     *
     * @see <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6355567">Bug report</a>
     */
    static final class AdobeAPP14Replacement extends ReplacementSegment {

        AdobeAPP14Replacement(final long realStart, final long start, final long realLength, final ImageInputStream stream) throws IOException {
            super(JPEG.APP14, realStart, start, realLength, createMarkerFixedLength(stream));
        }

        private static byte[] createMarkerFixedLength(final ImageInputStream stream) throws IOException {
            return readSegment(JPEG.APP14, 16, stream);
        }
    }

    /**
     * Workaround for a known bug in com.sun.imageio.plugins.jpeg.DQTMarkerSegment, throwing exception,
     * if the DQT precision is 16 bits (not 8 bits). Native reader seems to cope fine though.
     * This downsampling of the quality tables, creates visually same results, with no exceptions thrown.
     */
    static final class DownsampledDQTReplacement extends ReplacementSegment {

        DownsampledDQTReplacement(final long realStart, final long start, final long realLength, final int qtInfo, final ImageInputStream stream) throws IOException {
            super(JPEG.DQT, realStart, start, realLength, createMarkerFixedLength((int) realLength, qtInfo, stream));
        }

        private static byte[] createMarkerFixedLength(final int length, final int qtInfo, final ImageInputStream stream) throws IOException {
            int numQTs = length / 128;
            int newSegmentLength = 2 + (1 + 64) * numQTs; // Len + (qtInfo + qtSize) * numQTs

            byte[] replacementData = new byte[length];
            replacementData[0] = (byte) ((JPEG.DQT >> 8) & 0xff);
            replacementData[1] = (byte) (JPEG.DQT & 0xff);
            replacementData[2] = (byte) ((newSegmentLength >> 8) & 0xff);
            replacementData[3] = (byte) (newSegmentLength & 0xff);
            replacementData[4] = (byte) (qtInfo & 0x0f);
            stream.readFully(replacementData, 5, replacementData.length - 5);

            // Downsample tables to 8 bits by discarding lower 8 bits...
            int newOff = 4;
            int oldOff = 4;
            for (int q = 0; q < numQTs; q++) {
                replacementData[newOff++] = (byte) (replacementData[oldOff++] & 0x0f);

                for (int i = 0; i < 64; i++) {
                    replacementData[newOff + i] = replacementData[oldOff + 1 + i * 2];
                }

                newOff += 64;
                oldOff += 128;
            }

            return Arrays.copyOfRange(replacementData, 0, newSegmentLength + 2);
        }
    }

    static class ReplacementSegment extends Segment {
        final long realLength;
        final byte[] data;

        int pos;

        ReplacementSegment(final int marker, final long realStart, final long start, final long realLength, final byte[] replacementData) {
            super(marker, realStart, start, replacementData.length);
            this.realLength = realLength;
            this.data = replacementData;
        }

        @Override
        long realEnd() {
            return realStart + realLength;
        }

        @Override
        public void seek(final ImageInputStream stream, final long newPos) throws IOException {
            pos = (int) (newPos - start);
            super.seek(stream, newPos);
        }

        @Override
        public int read(final ImageInputStream stream) {
            return data[pos++] & 0xff;
        }

        @Override
        public int read(final ImageInputStream stream, byte[] b, int off, int len) {
            int length = Math.min(data.length - pos, len);
            System.arraycopy(data, pos, b, off, length);
            pos += length;

            return length;
        }
    }

    static final class ComponentIdSet {
        final int[] values = new int[4]; // The native code don't support more than 4 components
        int size;

        boolean add(final int value) {
            if (contains(value) || size >= values.length) {
                return false;
            }

            values[size++] = value;

            return true;
        }

        boolean contains(final int value) {
            for (int i = 0; i < size; i++) {
                if (values[i] == value) {
                    return true;
                }
            }

            return false;
        }

        int size() {
            return size;
        }

        int get(final int index) {
            return values[index];
        }

        @Override
        public String toString() {
            return Arrays.toString(copyOf(values, size));
        }
    }
}
