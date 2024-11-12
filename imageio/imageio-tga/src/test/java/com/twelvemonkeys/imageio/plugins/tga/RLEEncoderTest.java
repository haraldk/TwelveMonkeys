/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.io.enc.EncoderAbstractTest;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * RLEEncoderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RLEEncoderTest.java,v 1.0 07/04/2021 haraldk Exp$
 */

public class RLEEncoderTest extends EncoderAbstractTest {
    @Override
    protected Encoder createEncoder() {
        return new RLEEncoder(8);
    }

    @Override
    protected Decoder createCompatibleDecoder() {
        return new RLEDecoder(8);
    }

    @Test
    public void testRLE8() throws IOException {
        RLEEncoder encoder = new RLEEncoder(8);

        // Literal run, 2 bytes, compressed run, 8 bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {(byte) 0xFF, (byte) 0xF1, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE});
        ByteArrayOutputStream stream = new ByteArrayOutputStream(10);

        encoder.encode(stream, buffer);

        assertArrayEquals(new byte[] {1, (byte) 0xFF, (byte) 0xF1, (byte) 0x87, (byte) 0xFE}, stream.toByteArray());
    }

    @Test
    public void testRLE16() throws IOException {
        RLEEncoder encoder = new RLEEncoder(16);

        // Literal run, 2 * 2 bytes, compressed run, 8 * 2 bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xF1,
                               (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE,
                               (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0xFE});
        ByteArrayOutputStream stream = new ByteArrayOutputStream(20);

        encoder.encode(stream, buffer);

        assertArrayEquals(new byte[] {1, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xF1, (byte) 0x87, (byte) 0x00, (byte) 0xFE}, stream.toByteArray());
    }

    @Test
    public void testRLE24() throws IOException {
        RLEEncoder encoder = new RLEEncoder(24);

        // Literal run, 2 * 3 bytes, compressed run, 8 * 3 bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {
                (byte) 0x00, (byte) 0xFF,  (byte) 0xFF, (byte) 0x00, (byte) 0xF1, (byte) 0xF1,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE
        });
        ByteArrayOutputStream stream = new ByteArrayOutputStream(30);

        encoder.encode(stream, buffer);

        assertArrayEquals(new byte[] {
                1, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xF1, (byte) 0xF1,
                (byte) 0x87, (byte) 0x00, (byte) 0xFE, (byte) 0xFE
        }, stream.toByteArray());
    }

    @Test
    public void testRLE32() throws IOException {
        RLEEncoder encoder = new RLEEncoder(32);

        // Literal run, 2 * 4 bytes, compressed run, 8 * 4 bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {
                (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xF1, (byte) 0xF1, (byte) 0xF1,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE
        });
        ByteArrayOutputStream stream = new ByteArrayOutputStream(40);

        encoder.encode(stream, buffer);

        assertArrayEquals(new byte[] {
                1, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xF1, (byte) 0xF1, (byte) 0xF1,
                (byte) 0x87, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE
        }, stream.toByteArray());
    }
}