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

package com.twelvemonkeys.imageio.metadata.xmp;

import org.junit.Test;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertNotNull;

/**
 * XMPScannerTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPScannerTestCase.java,v 1.0 Nov 13, 2009 3:59:43 PM haraldk Exp$
 */
public class XMPScannerTest {
    static final String XMP =
            "<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>" +
                    "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 4.1-c036 46.276720, Fri Nov 13 2009 15:59:43        \">\n"+
                    "   <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"+
                    "      <rdf:Description rdf:about=\"\"\n"+
                    "            xmlns:photoshop=\"http://ns.adobe.com/photoshop/1.0/\">\n"+
                    "         <photoshop:Source>twelvemonkeys.com</photoshop:Source>\n"+
                    "      </rdf:Description>\n"+
                    "      <rdf:Description rdf:about=\"\"\n"+
                    "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"+
                    "         <dc:format>application/vnd.adobe.photoshop</dc:format>\n"+
                    "      </rdf:Description>\n"+
                    "   </rdf:RDF>\n"+
                    "</x:xmpmeta>" +
                    "<?xpacket end=\"w\"?>";

    final Random random = new Random(4934638567l);

    private InputStream createRandomStream(final int pLength) {
        byte[] bytes = new byte[pLength];
        random.nextBytes(bytes);
        return new ByteArrayInputStream(bytes);
    }

    private InputStream createXMPStream(final String pXMP, final String pCharsetName) {
        try {
            return new SequenceInputStream(
                    Collections.enumeration(
                            Arrays.asList(
                                    createRandomStream(79),
                                    new ByteArrayInputStream(pXMP.getBytes(pCharsetName)),
                                    createRandomStream(31)
                            )
                    )
            );
        }
        catch (UnsupportedEncodingException e) {
            UnsupportedCharsetException uce = new UnsupportedCharsetException(pCharsetName);
            uce.initCause(e);
            throw uce;
        }
    }

    @Test
    public void testScanForUTF8() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-8");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testScanForUTF8SingleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP.replace("\"", "'"), "UTF-8");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testScanForUTF16BE() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16BE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testScanForUTF16BESingleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP.replace("\"", "'"), "UTF-16BE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testScanForUTF16LE() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16LE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testScanForUTF16LESingleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP.replace("\"", "'"), "UTF-16LE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    @Test
    public void testUTF32BE() throws IOException {
        try {
            InputStream stream = createXMPStream(XMP, "UTF-32BE");

            Reader reader = XMPScanner.scanForXMPPacket(stream);

            assertNotNull(reader);
        }
        catch (UnsupportedCharsetException ignore) {
            System.err.println("Warning: Unsupported charset. Test skipped. " + ignore);
        }
    }

    @Test
    public void testUTF32LE() throws IOException {
        try {
            InputStream stream = createXMPStream(XMP, "UTF-32LE");

            Reader reader = XMPScanner.scanForXMPPacket(stream);

            assertNotNull(reader);
        }
        catch (UnsupportedCharsetException ignore) {
            System.err.println("Warning: Unsupported charset. Test skipped. " + ignore);
        }
    }
}
