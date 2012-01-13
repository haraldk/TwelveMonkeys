package com.twelvemonkeys.imageio.metadata.xmp;

import org.junit.Test;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * XMPScannerTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPScannerTestCase.java,v 1.0 Nov 13, 2009 3:59:43 PM haraldk Exp$
 */
public class XMPScannerTestCase  {
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
