/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * JPEGSegmentImageInputStream.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentImageInputStream.java,v 1.0 30.01.12 16:15 haraldk Exp$
 */
final class JPEGSegmentImageInputStream extends ImageInputStreamImpl {
    // TODO: Rewrite JPEGSegment (from metadata) to store stream pos/length, and be able to replay data, and use instead of Segment?
    // TODO: Change order of segments, to make sure APP0/JFIF is always before APP14/Adobe?
    // TODO: Insert fake APP0/JFIF if needed by the reader?

    final private ImageInputStream stream;
    
    private final List<Segment> segments = new ArrayList<Segment>(64);
    private int currentSegment = -1;
    private Segment segment;

    JPEGSegmentImageInputStream(final ImageInputStream stream) {
        this.stream = notNull(stream, "stream");
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
            while (++currentSegment < segments.size()) {
                segment = segments.get(currentSegment);

                if (streamPos >= segment.start && streamPos < segment.end()) {
                    stream.seek(segment.realStart + streamPos - segment.start);

                    return segment;
                }
            }

            stream.seek(segment.realEnd());

            // Scan forward
            while (true) {
                long realPosition = stream.getStreamPosition();
                int marker = stream.readUnsignedShort();

                // TODO: Refactor to make various segments optional, we probably only want the "Adobe" APP14 segment, 'Exif' APP1 and very few others
                if (isAppSegmentMarker(marker) && marker != JPEG.APP0 && marker != JPEG.APP1 && marker != JPEG.APP14) {
                    int length = stream.readUnsignedShort(); // Length including length field itself
                    stream.seek(realPosition + 2 + length);  // Skip marker (2) + length
                }
                else {
                    if (marker == JPEG.EOI) {
                        segment = new Segment(marker, realPosition, segment.end(), 2);
                        segments.add(segment);
                    }
                    else {
                        int length = stream.readUnsignedShort(); // Length including length field itself
                        segment = new Segment(marker, realPosition, segment.end(), 2 + length);
                        segments.add(segment);
                    }

                    currentSegment = segments.size() - 1;

                    if (streamPos >= segment.start && streamPos < segment.end()) {
                        stream.seek(segment.realStart + streamPos - segment.start);

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
            while (--currentSegment >= 0) {
                segment = segments.get(currentSegment);

                if (streamPos >= segment.start && streamPos < segment.end()) {
                    stream.seek(segment.realStart + streamPos - segment.start);

                    break;
                }
            }
        }
        else {
            stream.seek(segment.realStart + streamPos - segment.start);
        }
        
        return segment;
    }

    private void streamInit() throws IOException {
        stream.seek(0);

        int soi = stream.readUnsignedShort();
        if (soi != JPEG.SOI) {
            throw new IIOException(String.format("Not a JPEG stream (starts with: 0x%04x, expected SOI: 0x%04x)", soi, JPEG.SOI));
        }
        else {
            segment = new Segment(soi, 0, 0, 2);

            segments.add(segment);
            currentSegment = segments.size() - 1; // 0
        }
    }

    static boolean isAppSegmentMarker(final int marker) {
        return marker >= JPEG.APP0 && marker <= JPEG.APP15;
    }

    private void repositionAsNecessary() throws IOException {
        if (segment == null || streamPos < segment.start || streamPos >= segment.end()) {
            fetchSegment();
        }
    }

    @Override
    public int read() throws IOException {
        bitOffset = 0;

        repositionAsNecessary();

        int read = stream.read();

        if (read != -1) {
            streamPos++;
        }

        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        bitOffset = 0;

        // NOTE: There is a bug in the JPEGMetadata constructor (JPEGBuffer.loadBuf() method) that expects read to
        // always read len bytes. Therefore, this is more complicated than it needs to... :-/
        int total = 0;

        while (total < len) {
            repositionAsNecessary();

            int count = stream.read(b, off + total, (int) Math.min(len - total, segment.end() - streamPos));

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
        private final int marker;

        final long realStart;
        final long start;
        final long length;

        Segment(int marker, long realStart, long start, long length) {
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

        @Override
        public String toString() {
            return String.format("0x%04x[%d-%d]", marker, realStart, realEnd());
        }
    }
}
