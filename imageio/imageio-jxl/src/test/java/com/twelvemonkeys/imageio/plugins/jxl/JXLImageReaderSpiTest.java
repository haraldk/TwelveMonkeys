/*
 * Copyright (c) 2026, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jxl;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class JXLImageReaderSpiTest {

    private final ImageReaderSpi provider = new JXLImageReaderSpi();

    private static ImageInputStream stream(final int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }

        return new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
    }

    @Test
    public void canDecodeCodestream() throws IOException {
        assertTrue(provider.canDecodeInput(stream(0xFF, 0x0A, 0x5F, 0x40, 0x24, 0x08)));
    }

    @Test
    public void canDecodeCodestreamMinimal() throws IOException {
        assertTrue(provider.canDecodeInput(stream(0xFF, 0x0A)));
    }

    @Test
    public void canDecodeContainer() throws IOException {
        assertTrue(provider.canDecodeInput(stream(0x00, 0x00, 0x00, 0x0C, 'J', 'X', 'L', ' ', 0x0D, 0x0A, 0x87, 0x0A, 0x00, 0x00, 0x00, 0x14, 'f', 't', 'y', 'p')));
    }

    @Test
    public void cannotDecodeOtherFormats() throws IOException {
        // JPEG
        assertFalse(provider.canDecodeInput(stream(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01)));
        // PNG
        assertFalse(provider.canDecodeInput(stream(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D)));
        // Other ISO BMFF (HEIF/AVIF), ftyp box
        assertFalse(provider.canDecodeInput(stream(0x00, 0x00, 0x00, 0x1C, 'f', 't', 'y', 'p', 'a', 'v', 'i', 'f')));
        // JPEG 2000, similar signature box
        assertFalse(provider.canDecodeInput(stream(0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' ', 0x0D, 0x0A, 0x87, 0x0A)));
    }

    @Test
    public void cannotDecodeTruncated() throws IOException {
        assertFalse(provider.canDecodeInput(stream()));
        assertFalse(provider.canDecodeInput(stream(0xFF)));
        assertFalse(provider.canDecodeInput(stream(0x00, 0x00, 0x00, 0x0C, 'J', 'X', 'L', ' ', 0x0D, 0x0A, 0x87)));
    }

    @Test
    public void cannotDecodeNonStream() throws IOException {
        assertFalse(provider.canDecodeInput(null));
        assertFalse(provider.canDecodeInput(new Object()));
        assertFalse(provider.canDecodeInput(new byte[] {(byte) 0xFF, 0x0A}));
    }

    @Test
    public void canDecodeInputResetsStream() throws IOException {
        try (ImageInputStream input = stream(0x00, 0x00, 0x00, 0x0C, 'J', 'X', 'L', ' ', 0x0D, 0x0A, 0x87, 0x0A)) {
            input.seek(0);
            assertTrue(provider.canDecodeInput(input));
            assertEquals(0, input.getStreamPosition());
        }

        try (ImageInputStream input = stream(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D)) {
            assertFalse(provider.canDecodeInput(input));
            assertEquals(0, input.getStreamPosition());
        }
    }

    @Test
    public void jxlatteAvailable() {
        // JXLatte is a provided dependency, available on the test class path
        assertTrue(JXLImageReaderSpi.JXLATTE_AVAILABLE);
    }

    @Test
    public void registeredWithImageIO() {
        IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jxl");
        boolean found = false;
        while (readers.hasNext()) {
            found |= readers.next() instanceof JXLImageReader;
        }

        assertTrue(found, "JXLImageReader not registered");

        assertTrue(ImageIO.getImageReadersBySuffix("jxl").hasNext());
        assertTrue(ImageIO.getImageReadersByMIMEType("image/jxl").hasNext());
    }
}
