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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGQuality;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;

import javax.imageio.IIOException;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.stream.ImageInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * JPEGTables
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGTables.java,v 1.0 11.05.12 09:13 haraldk Exp$
 */
class JPEGTables {
    private static final int DHT_LENGTH = 16;
    private static final Map<Integer, List<String>> SEGMENT_IDS = createSegmentIdsMap();

    private JPEGQTable[] qTables;
    private JPEGHuffmanTable[] dcHTables;
    private JPEGHuffmanTable[] acHTables;

    private static Map<Integer, List<String>> createSegmentIdsMap() {
        Map<Integer, List<String>> segmentIds = new HashMap<Integer, List<String>>();
        segmentIds.put(JPEG.DQT, null);
        segmentIds.put(JPEG.DHT, null);

        return Collections.unmodifiableMap(segmentIds);
    }

    private final List<JPEGSegment> segments;

    public JPEGTables(ImageInputStream input) throws IOException {
        segments = JPEGSegmentUtil.readSegments(input, SEGMENT_IDS);
    }

    public JPEGQTable[] getQTables() throws IOException {
        if (qTables == null) {
            qTables = JPEGQuality.getQTables(segments);
        }

        return qTables;
    }

    private void getHuffmanTables() throws IOException {
        if (dcHTables == null || acHTables == null) {
            List<JPEGHuffmanTable> dc = new ArrayList<JPEGHuffmanTable>();
            List<JPEGHuffmanTable> ac = new ArrayList<JPEGHuffmanTable>();

            // JPEG may contain multiple DHT marker segments
            for (JPEGSegment segment : segments) {
                if (segment.marker() != JPEG.DHT) {
                    continue;
                }

                DataInputStream data = new DataInputStream(segment.data());
                int read = 0;

                // A single DHT marker segment may contain multiple tables
                while (read < segment.length()) {
                    int htInfo = data.read();
                    read++;

                    int num = htInfo & 0x0f; // 0-3
                    int type = htInfo >> 4; // 0 == DC, 1 == AC

                    if (type > 1) {
                        throw new IIOException("Bad DHT type: " + type);
                    }
                    if (num >= 4) {
                        throw new IIOException("Bad DHT table index: " + num);
                    }
                    else if (type == 0 ? dc.size() > num : ac.size() > num) {
                        throw new IIOException("Duplicate DHT table index: " + num);
                    }

                    // Read lengths as short array
                    short[] lengths = new short[DHT_LENGTH];
                    for (int i = 0; i < DHT_LENGTH; i++) {
                        lengths[i] = (short) data.readUnsignedByte();
                    }
                    read += lengths.length;

                    int sum = 0;
                    for (short length : lengths) {
                        sum += length;
                    }

                    // Expand table to short array
                    short[] table = new short[sum];
                    for (int j = 0; j < sum; j++) {
                        table[j] = (short) data.readUnsignedByte();
                    }

                    JPEGHuffmanTable hTable = new JPEGHuffmanTable(lengths, table);
                    if (type == 0) {
                        dc.add(num, hTable);
                    }
                    else {
                        ac.add(num, hTable);
                    }

                    read += sum;
                }
            }

            dcHTables = dc.toArray(new JPEGHuffmanTable[dc.size()]);
            acHTables = ac.toArray(new JPEGHuffmanTable[ac.size()]);
        }
    }

    public JPEGHuffmanTable[] getDCHuffmanTables() throws IOException {
        getHuffmanTables();
        return dcHTables;
    }

    public JPEGHuffmanTable[] getACHuffmanTables() throws IOException {
        getHuffmanTables();
        return acHTables;
    }
}
