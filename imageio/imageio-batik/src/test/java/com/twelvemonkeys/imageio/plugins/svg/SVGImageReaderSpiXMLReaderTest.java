/*
 * Copyright (c) 2023, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * SVGImageReaderSpi.canDecodeInput() using XML reader.
 */
public class SVGImageReaderSpiXMLReaderTest extends SVGImageReaderSpiTest {

    @BeforeClass
    public static void setSuiteUp() {
        System.setProperty(SVGImageReaderSpi.XML_READER_DETECT, "true");
    }

    @AfterClass
    public static void tearSuiteDown() {
        System.setProperty(SVGImageReaderSpi.XML_READER_DETECT, "false");
    }

    @Test
    public void canDecodeSpeculativeDoctype() throws Exception {
        String svg = "<!DOCTYPE svg><svg xmlns=\"http://www.w3.org/2023/fake\">";

        try (ImageInputStream input = new ByteArrayImageInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
            assertTrue("Can't read speculative DOCTYPE: " + svg, provider.canDecodeInput(input));
        }
    }

    @Test
    public void canDecodeUTFBOMInput() throws Exception {
        String svgRoot = "\uFEFF<svg xmlns='http://www.w3.org/2000/svg'>";
        Charset[] utfCharsets = { StandardCharsets.UTF_8,
                                  StandardCharsets.UTF_16LE,
                                  StandardCharsets.UTF_16BE };

        for (Charset charset : utfCharsets) {
            try (ImageInputStream input = new ByteArrayImageInputStream(svgRoot.getBytes(charset))) {
                assertTrue("Can't read " + charset + " input with BOM", provider.canDecodeInput(input));
            }
        }
    }

    @Test
    public void canDecodeUTFWithoutBOM() throws Exception {
        String svgRoot = "<?xml version='1.0' encoding='%s'?>"
                + "<svg xmlns='http://www.w3.org/2000/svg'>";
        Charset[] utfCharsets = { StandardCharsets.UTF_8,
                                  StandardCharsets.UTF_16LE,
                                  StandardCharsets.UTF_16BE };

        for (Charset charset : utfCharsets) {
            byte[] bytes = String.format(svgRoot, charset.name()).getBytes(charset);
            try (ImageInputStream input = new ByteArrayImageInputStream(bytes)) {
                assertTrue("Can't read " + charset + " input w/o BOM", provider.canDecodeInput(input));
            }
        }
    }

    @Test
    public void canDecodeEBCDICInput() throws Exception {
        // The SAX parser implementation may support EBCDIC regardless of
        // the JVM Charset.availableCharsets();  The XML declaration is
        // required for reliable detection:
        //
        // <?xml version='1.0' encoding='IBM01140'?>
        // <svg xmlns='http://www.w3.org/2000/svg'>
        //
        byte[] validInput = bytes(0x4C, 0x6F, 0xA7, 0x94, 0x93, 0x40, 0xA5,
                0x85, 0x99, 0xA2, 0x89, 0x96, 0x95, 0x7E, 0x7D, 0xF1, 0x4B,
                0xF0, 0x7D, 0x40, 0x85, 0x95, 0x83, 0x96, 0x84, 0x89, 0x95,
                0x87, 0x7E, 0x7D, 0xC9, 0xC2, 0xD4, 0xF0, 0xF1, 0xF1, 0xF4,
                0xF0, 0x7D, 0x6F, 0x6E, 0x4C, 0xA2, 0xA5, 0x87, 0x40, 0xA7,
                0x94, 0x93, 0x95, 0xA2, 0x7E, 0x7D, 0x88, 0xA3, 0xA3, 0x97,
                0x7A, 0x61, 0x61, 0xA6, 0xA6, 0xA6, 0x4B, 0xA6, 0xF3, 0x4B,
                0x96, 0x99, 0x87, 0x61, 0xF2, 0xF0, 0xF0, 0xF0, 0x61, 0xA2,
                0xA5, 0x87, 0x7D, 0x6E);

        try (ImageInputStream input = new ByteArrayImageInputStream(validInput)) {
            assumeTrue("Can't read valid IBM01140 (EBCDIC) input", provider.canDecodeInput(input));
        }
    }

    private static byte[] bytes(int... bytes) {
        byte[] array = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            array[i] = (byte) bytes[i];
        }
        return array;
    }
}
