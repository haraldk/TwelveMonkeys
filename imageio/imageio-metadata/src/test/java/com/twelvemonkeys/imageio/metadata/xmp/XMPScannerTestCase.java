package com.twelvemonkeys.imageio.metadata.xmp;

import junit.framework.TestCase;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * XMPScannerTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPScannerTestCase.java,v 1.0 Nov 13, 2009 3:59:43 PM haraldk Exp$
 */
public class XMPScannerTestCase extends TestCase {

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

    final Random mRandom = new Random(4934638567l);

    private InputStream createRandomStream(final int pLength) {
        byte[] bytes = new byte[pLength];
        mRandom.nextBytes(bytes);
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

    public void testScanForUTF8() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-8");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    public void testScanForUTF8singleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-8".replace("\"", "'"));

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    public void testScanForUTF16BE() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16BE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    public void testScanForUTF16BEsingleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16BE".replace("\"", "'"));

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    public void testScanForUTF16LE() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16LE");

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    public void testScanForUTF16LEsingleQuote() throws IOException {
        InputStream stream = createXMPStream(XMP, "UTF-16LE".replace("\"", "'"));

        Reader reader = XMPScanner.scanForXMPPacket(stream);

        assertNotNull(reader);
    }

    // TODO: Default Java installation on OS X don't seem to have UTF-32 installed. Hmmm..
//    public void testUTF32BE() throws IOException {
//        InputStream stream = createXMPStream("UTF-32BE");
//
//        Reader reader = XMPScanner.scanForXMPPacket(stream);
//
//        assertNotNull(reader);
//    }
//
//    public void testUTF32LE() throws IOException {
//        InputStream stream = createXMPStream("UTF-32LE");
//
//        Reader reader = XMPScanner.scanForXMPPacket(stream);
//
//        assertNotNull(reader);
//    }
}
