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

package com.twelvemonkeys.io.ole2;

import com.twelvemonkeys.io.InputStreamAbstractTest;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;
import com.twelvemonkeys.io.MemoryCacheSeekableStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * CompoundDocument_StreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CompoundDocument_StreamTestCase.java,v 1.0 13.10.11 12:01 haraldk Exp$
 */
public class CompoundDocument_StreamTest extends InputStreamAbstractTest {
    @Override
    protected InputStream makeInputStream(byte[] data) {
        try {
            // Set up fake document
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            LittleEndianDataOutputStream dataStream = new LittleEndianDataOutputStream(stream);

            dataStream.write(CompoundDocument.MAGIC);                   // 8 bytes magic
            dataStream.write(new byte[16]);                             // UUID 16 bytes, all zero
            dataStream.write(new byte[]{0x3E, 0, 3, 0});                // version (62), rev (3)
            // 28
            dataStream.write(new byte[]{(byte) 0xfe, (byte) 0xff});     // Byte order
            dataStream.write(new byte[]{9, 0, 6, 0});                   // Sector size (1 << x), short sector size
            dataStream.write(new byte[10]);                             // Reserved 10 bytes
            // 44
            dataStream.writeInt(1);                                     // SAT size (1)
            dataStream.writeInt(1);                                     // Directory SId
            dataStream.write(new byte[4]);                              // Reserved 4 bytes
            // 56
            dataStream.writeInt(4096);                                  // Min stream size (4096)
            dataStream.writeInt(3);                                     // Short SAT SId
            dataStream.writeInt(1);                                     // Short SAT size
            dataStream.writeInt(-2);                                    // Master SAT SId (-2, end of chain)
            // 72
            dataStream.writeInt(0);                                     // Master SAT size
            dataStream.writeInt(0);                                     // Master SAT entry 0 (0)
            dataStream.writeInt(128);                                   // Master SAT entry 1 (128)
            // 84
            dataStream.write(createPad(428, (byte) -1));                // Pad (until 512 bytes)
            // 512 -- end header

            //  SId 0
            //        SAT
            dataStream.writeInt(-3);                                    // SAT entry 0 (SAT)
            dataStream.writeInt(-2);                                    // SAT entry 1 (EOS)
            dataStream.write(createPad(512 - 8, (byte) -1));            // Pad (until 512 bytes)
            // 1024 -- end SAT

            //  SId 1
            //         Directory
            //          64 bytes UTF16LE ("Root Entry" + null-termination)
            byte[] name = "Root Entry".getBytes(Charset.forName("UTF-16LE"));
            dataStream.write(name);                                     // Name
            dataStream.write(createPad(64 - name.length, (byte) 0));    // Pad name to 64 bytes
            dataStream.writeShort((short) (name.length + 2));           // 2 byte length (incl null-term)
            dataStream.write(new byte[]{5, 0});                         // type (root), node color
            dataStream.writeInt(-1);                                    // prevDId, -1
            dataStream.writeInt(-1);                                    // nextDId, -1
            dataStream.writeInt(1);                                     // rootNodeDId
            dataStream.write(createPad(36, (byte) 0));                  // UID + flags + 2 x long timestamps
            dataStream.writeInt(2);                                     // Start SId
            dataStream.writeInt(8);                                     // Stream size
            dataStream.writeInt(0);                                     // Reserved

            name = "data".getBytes(Charset.forName("UTF-16LE"));
            dataStream.write(name);                                     // Name
            dataStream.write(createPad(64 - name.length, (byte) 0));    // Pad name to 64 bytes
            dataStream.writeShort((short) (name.length + 2));           // 2 byte length (incl null-term)
            dataStream.write(new byte[]{2, 0});                         // type (user stream), node color
            dataStream.writeInt(-1);                                    // prevDId, -1
            dataStream.writeInt(-1);                                    // nextDId, -1
            dataStream.writeInt(-1);                                    // rootNodeDId
            dataStream.write(createPad(36, (byte) 0));                  // UID + flags + 2 x long timestamps
            dataStream.writeInt(0);                                     // Start SId
            dataStream.writeInt(data.length);                           // Stream size
            dataStream.writeInt(0);                                     // Reserved

            dataStream.write(createPad(512 - 256, (byte) -1));          // Pad to full sector (512 bytes)
            // 1536 -- end Directory

            //  SId 2
            //          Data
            dataStream.write(data);                                     // The data
            dataStream.write(createPad(512 - data.length, (byte) -1));  // Pad to full sector (512 bytes)
            // 2048 -- end Data

            //  SId 3
            //         Short SAT
            dataStream.writeInt(2);                                     // Short SAT entry 0
            dataStream.writeInt(-2);                                    // Short SAT entry 1 (EOS)
            dataStream.write(createPad(512 - 8, (byte) -1));            // Pad to full sector (512 bytes)
            // 2560 -- end Short SAT


            InputStream input = new ByteArrayInputStream(stream.toByteArray());
            CompoundDocument document = new CompoundDocument(new MemoryCacheSeekableStream(input));

            Entry entry = document.getRootEntry().getChildEntries().first();

            return entry.getInputStream();
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private byte[] createPad(final int length, final byte val) {
        byte[] pad = new byte[length];
        Arrays.fill(pad, val);
        return pad;
    }

    @Test
    public void testStreamRead() throws IOException {
        InputStream stream = makeInputStream(makeOrderedArray(32));

        int read;
        int count = 0;
        while ((read = stream.read()) >= 0) {
            assertEquals(count, read);
            count++;
        }

        assertFalse("Short stream", count < 32);
        assertFalse("Stream overrun", count > 32);
    }

    @Test
    public void testInputStreamSkip() throws IOException {
        InputStream stream = makeInputStream();

        // BUGFIX: Would skip and return 0 for first skip
        assertTrue(stream.skip(10) > 0);
    }
}
