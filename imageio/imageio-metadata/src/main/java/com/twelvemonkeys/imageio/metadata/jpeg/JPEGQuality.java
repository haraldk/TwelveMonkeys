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

package com.twelvemonkeys.imageio.metadata.jpeg;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.stream.ImageInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Determines an approximate JPEG compression quality value from the quantization tables.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGQuality.java,v 1.0 16.02.12 17:07 haraldk Exp$
 */
public final class JPEGQuality {
    static final int NUM_QUANT_TABLES = 4; /* Quantization tables are numbered 0..3 */
    static final int DCT_SIZE_2 = 64; /* DCT_SIZE squared; # of elements in a block */

    /**
     * Determines an approximate JPEG compression quality value from the quantization tables.
     * The value will be in the range {@code [0...1]}, where {@code 1} is the best possible value.
     *
     * @param segments a list of JPEG segments containing the DQT quantization tables.
     * @return a float in the range {@code [0...1]}, representing the JPEG quality,
     *         or {@code -1} if the quality can't be determined.
     * @throws IIOException if a JPEG format error is found during parsing.
     * @throws IOException if an I/O exception occurs during parsing.
     *
     * @see javax.imageio.plugins.jpeg.JPEGImageWriteParam#setCompressionQuality(float)
     * @see JPEG#DQT
     */
    public static float getJPEGQuality(final List<JPEGSegment> segments) throws IOException {
        int quality = getJPEGQuality(getQuantizationTables(segments));
        return quality >= 0 ? quality / 100f : quality;
    }

    /**
     * Determines an approximate JPEG compression quality value from the quantization tables.
     * The value will be in the range {@code [0...1]}, where {@code 1} is the best possible value.
     *
     * @param input an image input stream containing JPEG data.
     * @return a float in the range {@code [0...1]}, representing the JPEG quality,
     *         or {@code -1} if the quality can't be determined.
     * @throws IIOException if a JPEG format error is found during parsing.
     * @throws IOException if an I/O exception occurs during parsing.
     *
     * @see javax.imageio.plugins.jpeg.JPEGImageWriteParam#setCompressionQuality(float)
     * @see JPEG#DQT
     */
    public static float getJPEGQuality(final ImageInputStream input) throws IOException {
        return getJPEGQuality(JPEGSegmentUtil.readSegments(input, JPEG.DQT, null));
    }

    // Adapted from ImageMagick coders/jpeg.c & http://blog.apokalyptik.com/2009/09/16/quality-time-with-your-jpegs/
    private static int getJPEGQuality(final int[][] quantizationTables) throws IOException {
//        System.err.println("tables: " + Arrays.deepToString(tables));

        // TODO: Determine lossless JPEG, it's an entirely different algorithm

        int qvalue;
        
        // Determine the JPEG compression quality from the quantization tables.
        int sum = 0;
        for (int i = 0; i < NUM_QUANT_TABLES; i++) {
            if (quantizationTables[i] != null) {
                for (int j = 0; j < DCT_SIZE_2; j++) {
                    sum += quantizationTables[i][j];
                }
            }
        }

        int[] hash, sums;

        if (quantizationTables[0] != null && quantizationTables[1] != null) {
            // TODO: Make constant
            hash = new int[] {
                    1020, 1015, 932, 848, 780, 735, 702, 679, 660, 645,
                    632, 623, 613, 607, 600, 594, 589, 585, 581, 571,
                    555, 542, 529, 514, 494, 474, 457, 439, 424, 410,
                    397, 386, 373, 364, 351, 341, 334, 324, 317, 309,
                    299, 294, 287, 279, 274, 267, 262, 257, 251, 247,
                    243, 237, 232, 227, 222, 217, 213, 207, 202, 198,
                    192, 188, 183, 177, 173, 168, 163, 157, 153, 148,
                    143, 139, 132, 128, 125, 119, 115, 108, 104, 99,
                    94, 90, 84, 79, 74, 70, 64, 59, 55, 49,
                    45, 40, 34, 30, 25, 20, 15, 11, 6, 4,
                    0
            };
            sums = new int[] {
                    32640, 32635, 32266, 31495, 30665, 29804, 29146, 28599, 28104,
                    27670, 27225, 26725, 26210, 25716, 25240, 24789, 24373, 23946,
                    23572, 22846, 21801, 20842, 19949, 19121, 18386, 17651, 16998,
                    16349, 15800, 15247, 14783, 14321, 13859, 13535, 13081, 12702,
                    12423, 12056, 11779, 11513, 11135, 10955, 10676, 10392, 10208,
                    9928, 9747, 9564, 9369, 9193, 9017, 8822, 8639, 8458,
                    8270, 8084, 7896, 7710, 7527, 7347, 7156, 6977, 6788,
                    6607, 6422, 6236, 6054, 5867, 5684, 5495, 5305, 5128,
                    4945, 4751, 4638, 4442, 4248, 4065, 3888, 3698, 3509,
                    3326, 3139, 2957, 2775, 2586, 2405, 2216, 2037, 1846,
                    1666, 1483, 1297, 1109, 927, 735, 554, 375, 201,
                    128, 0
            };

            qvalue = quantizationTables[0][2] + quantizationTables[0][53] + quantizationTables[1][0] + quantizationTables[1][DCT_SIZE_2 - 1];
        }
        else if (quantizationTables[0] != null) {
            // TODO: Make constant
            hash = new int[] {
                    510, 505, 422, 380, 355, 338, 326, 318, 311, 305,
                    300, 297, 293, 291, 288, 286, 284, 283, 281, 280,
                    279, 278, 277, 273, 262, 251, 243, 233, 225, 218,
                    211, 205, 198, 193, 186, 181, 177, 172, 168, 164,
                    158, 156, 152, 148, 145, 142, 139, 136, 133, 131,
                    129, 126, 123, 120, 118, 115, 113, 110, 107, 105,
                    102, 100, 97, 94, 92, 89, 87, 83, 81, 79,
                    76, 74, 70, 68, 66, 63, 61, 57, 55, 52,
                    50, 48, 44, 42, 39, 37, 34, 31, 29, 26,
                    24, 21, 18, 16, 13, 11, 8, 6, 3, 2,
                    0
            };
            sums = new int[] {
                    16320, 16315, 15946, 15277, 14655, 14073, 13623, 13230, 12859,
                    12560, 12240, 11861, 11456, 11081, 10714, 10360, 10027, 9679,
                    9368, 9056, 8680, 8331, 7995, 7668, 7376, 7084, 6823,
                    6562, 6345, 6125, 5939, 5756, 5571, 5421, 5240, 5086,
                    4976, 4829, 4719, 4616, 4463, 4393, 4280, 4166, 4092,
                    3980, 3909, 3835, 3755, 3688, 3621, 3541, 3467, 3396,
                    3323, 3247, 3170, 3096, 3021, 2952, 2874, 2804, 2727,
                    2657, 2583, 2509, 2437, 2362, 2290, 2211, 2136, 2068,
                    1996, 1915, 1858, 1773, 1692, 1620, 1552, 1477, 1398,
                    1326, 1251, 1179, 1109, 1031, 961, 884, 814, 736,
                    667, 592, 518, 441, 369, 292, 221, 151, 86,
                    64, 0
            };

            qvalue = quantizationTables[0][2] + quantizationTables[0][53];
        }
        else {
            return -1;
        }

        for (int i = 0; i < 100; i++) {
            if (qvalue < hash[i] && sum < sums[i]) {
                continue;
            }

            if (qvalue <= hash[i] && sum <= sums[i] || i >= 50) {
                return i + 1;
            }

            break;
        }

        return -1;
    }

    public static JPEGQTable[] getQTables(final List<JPEGSegment> segments) throws IOException {
        int[][] tables = getQuantizationTables(segments);

        List<JPEGQTable> qTables = new ArrayList<JPEGQTable>();
        for (int[] table : tables) {
            if (table != null) {
                qTables.add(new JPEGQTable(table));
            }
        }

        return qTables.toArray(new JPEGQTable[qTables.size()]);
    }

    private static int[][] getQuantizationTables(final List<JPEGSegment> dqtSegments) throws IOException {
        Validate.notNull(dqtSegments, "segments");

        int[][] tables = new int[4][];

        // JPEG may contain multiple DQT marker segments
        for (JPEGSegment segment : dqtSegments) {
            if (segment.marker() != JPEG.DQT) {
                continue;
            }

            DataInputStream data = new DataInputStream(segment.data());
            int read = 0;

            // A single DQT marker segment may contain multiple tables
            while (read < segment.length()) {
                int qtInfo = data.read();
                read++;
//                System.err.printf("qtInfo: 0x%02x\n", qtInfo);

                int num = qtInfo & 0x0f; // 0-3
                int bits = qtInfo >> 4; // 0 == 8 bits, 1 == 16 bits

                if (num >= 4) {
                    throw new IIOException("Bad DQT table index: " + num);
                }
                else if (tables[num] != null) {
                    throw new IIOException("Duplicate DQT table index: " + num);
                }

                if (bits < 0 || bits > 1) {
                    throw new IIOException("Bad DQT bit info: " + bits);
                }

                byte[] qtData = new byte[DCT_SIZE_2 * (bits + 1)];
                data.readFully(qtData);
                read += qtData.length;
                tables[num] = new int[DCT_SIZE_2];

                // Expand (this is slightly inefficient)
                switch (bits) {
                    case 0:
                        for (int j = 0, qtDataLength = qtData.length; j < qtDataLength; j++) {
                            tables[num][j] = (short) (qtData[j] & 0xff);
                        }

                        break;
                    case 1:
                        for (int j = 0, qtDataLength = qtData.length; j < qtDataLength; j += 2) {
                            tables[num][j / 2] = (short) ((qtData[j] & 0xff) << 8 | (qtData[j + 1] & 0xff));
                        }

                        break;
                }
            }
        }

        return tables;
    }

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            float quality = getJPEGQuality(ImageIO.createImageInputStream(new File(arg)));
            System.err.println(arg + " quality: " + quality + "/" + (int) (quality * 100));
        }
    }
}
