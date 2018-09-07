/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.stream.BufferedImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.Charset;

/**
 * XMPScanner
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPScanner.java,v 1.0 Nov 11, 2009 4:49:00 PM haraldk Exp$
 */
public final class XMPScanner {
    /**
     * {@code &lt;?xpacket begin=}
     * <p/>
     * <ul>
     * <li>
     * 8-bit (UTF-8):
     * 0x3C 0x3F 0x78 0x70 0x61 0x63 0x6B 0x65 0x74 0x20
     * 0x62 0x65 0x67 0x69 0x6E 0x3D
     * </li>
     * <li>16-bit encoding (UCS-2, UTF-16): (either big- or little-endian order)
     * 0x3C 0x00 0x3F 0x00 0x78 0x00 0x70 0x00 0x61 0x00
     * 0x63 0x00 0x6B 0x00 0x65 0x00 0x74 0x00 0x20 0x00 0x62 0x00
     * 0x65 0x00 0x67 0x00 0x69 0x00 0x6E 0x00 0x3D [0x00]
     * </li>
     * <li>32-bit encoding (UCS-4):
     * As 16 bit UCS2, with three 0x00 instead of one.</li>
     * </ul>
     */
    private static final byte[] XMP_PACKET_BEGIN = {
            0x3C, 0x3F, 0x78, 0x70, 0x61, 0x63, 0x6B, 0x65, 0x74, 0x20,
            0x62, 0x65, 0x67, 0x69, 0x6E, 0x3D
    };

    /**
     * {@code &lt;?xpacket end=}
     */
    private static final byte[] XMP_PACKET_END = {
            0x3C, 0x3F, 0x78, 0x70, 0x61, 0x63, 0x6B, 0x65, 0x74, 0x20,
            0x65, 0x6E, 0x64, 0x3D
    };

    /**
     * Scans the given input for an XML metadata packet.
     * The scanning process involves reading every byte in the file, while searching for an XMP packet.
     * This process is very inefficient, compared to reading a known file format.
     * <p/>
     * <em>NOTE: The XMP Specification says this method of reading an XMP packet
     * should be considered a last resort.</em><br/>
     * This is because files may contain multiple XMP packets, some which may be related to embedded resources,
     * some which may be obsolete (or even incomplete).
     *
     * @param pInput the input to scan. The input may be an {@link javax.imageio.stream.ImageInputStream} or
     * any object that can be passed to {@link ImageIO#createImageInputStream(Object)}.
     * Typically this may be a {@link File}, {@link InputStream} or {@link java.io.RandomAccessFile}.
     *
     * @return a character Reader
     *
     * @throws java.nio.charset.UnsupportedCharsetException if the encoding specified within the BOM is not supported
     *         by the JRE.
     * @throws IOException if an I/O exception occurs reading from {@code pInput}.
     * @see ImageIO#createImageInputStream(Object)
     */
    static public Reader scanForXMPPacket(final Object pInput) throws IOException {
        ImageInputStream stream = pInput instanceof ImageInputStream ? (ImageInputStream) pInput : ImageIO.createImageInputStream(pInput);

        // TODO: Consider if BufferedIIS is a good idea
        if (!(stream instanceof BufferedImageInputStream)) {
            stream = new BufferedImageInputStream(stream);
        }

        // TODO: Might be more than one XMP block per file (it's possible to re-start for now)..
        long pos;
        pos = scanForSequence(stream, XMP_PACKET_BEGIN);

        if (pos >= 0) {
            // Skip ' OR " (plus possible nulls for 16/32 bit)
            byte quote = stream.readByte();

            if (quote == '\'' || quote == '"') {
                Charset cs = null;

                // Read BOM
                byte[] bom = new byte[4];
                stream.readFully(bom);

                // NOTE: Empty string should be treated as UTF-8 for backwards compatibility
                if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF && bom[3] == quote ||
                        bom[0] == quote) {
                    // UTF-8
                    cs = Charset.forName("UTF-8");
                }
                else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF && bom[2] == 0x00 && bom[3] == quote) {
                    // UTF-16 BIG endian
                    cs = Charset.forName("UTF-16BE");
                }
                else if (bom[0] == 0x00 && bom[1] == (byte) 0xFF && bom[2] == (byte) 0xFE && bom[3] == quote) {
                    stream.skipBytes(1); // Alignment

                    // UTF-16 little endian
                    cs = Charset.forName("UTF-16LE");
                }
                else if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
                    // NOTE: 32-bit character set not supported by default
                    // UTF 32 BIG endian
                    cs = Charset.forName("UTF-32BE");
                }
                else if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == 0x00 && bom[3] == (byte) 0xFF && stream.read() == 0xFE) {
                    stream.skipBytes(2); // Alignment
                    // NOTE: 32-bit character set not supported by default
                    // UTF 32 little endian
                    cs = Charset.forName("UTF-32LE");
                }

                if (cs != null) {
                    // Read all bytes until <?xpacket end= up-front or filter stream
                    stream.mark();
                    long end = scanForSequence(stream, XMP_PACKET_END);
                    stream.reset();

                    long length = end - stream.getStreamPosition();
                    Reader reader = new InputStreamReader(IIOUtil.createStreamAdapter(stream, length), cs);

                    // Skip until ?>
                    while (reader.read() != '>') {
                    }

                    // Return reader?
                    // How to decide between w or r?!
                    return reader;
                }
            }
        }

        return null;
    }

    /**
     * Scans for a given ASCII sequence.
     *
     * @param pStream the stream to scan
     * @param pSequence the byte sequence to search for
     *
     * @return the start position of the given sequence.
     *
     * @throws IOException if an I/O exception occurs during scanning
     */
    private static long scanForSequence(final ImageInputStream pStream, final byte[] pSequence) throws IOException {
        long start = -1l;

        int index = 0;
        int nullBytes = 0;

        for (int read; (read = pStream.read()) >= 0;) {
            if (pSequence[index] == (byte) read) {
                // If this is the first byte in the sequence, store position
                if (start == -1) {
                    start = pStream.getStreamPosition() - 1;
                }

                // Inside the sequence, there might be 1 or 3 null bytes, depending on 16/32 byte encoding
                if (nullBytes == 1 || nullBytes == 3) {
                    pStream.skipBytes(nullBytes);
                }

                index++;

                // If we found the entire sequence, we're done, return start position
                if (index == pSequence.length) {
                    return start;
                }
            }
            else if (index == 1 && read == 0 && nullBytes < 3) {
                // Skip 1 or 3 null bytes for 16/32 bit encoding
                nullBytes++;
            }
            else if (index != 0) {
                // Start over
                index = 0;
                start = -1;
                nullBytes = 0;
            }
        }

        return -1l;
    }

    public static void main(final String[] pArgs) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(new File(pArgs[0]));

        Reader xmp;
        while ((xmp = scanForXMPPacket(stream)) != null) {
            BufferedReader reader = new BufferedReader(xmp);
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        stream.close();
    }
}
