/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * XMPReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPReaderTest.java,v 1.0 04.01.12 10:47 haraldk Exp$
 */
public class XMPReaderTest extends MetadataReaderAbstractTest {

    @Override
    protected InputStream getData() throws IOException {
        return getResource("/xmp/xmp-jpeg-example.xml").openStream();
    }

    private ImageInputStream getResourceAsIIS(final String name) throws IOException {
        return ImageIO.createImageInputStream(getResource(name));
    }

    @Override
    protected XMPReader createReader() {
       return new XMPReader();
    }

    @Test
    public void testDirectoryContent() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertEquals(29, directory.size());

        // photoshop|http://ns.adobe.com/photoshop/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/DateCreated"), hasValue("2008-07-01"));
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/ColorMode"), hasValue("4"));
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/ICCProfile"), hasValue("U.S. Web Coated (SWOP) v2"));
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/History"), hasValue(""));

        // xapMM|http://ns.adobe.com/xap/1.0/mm/
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DocumentID"), hasValue("uuid:54A8D5F8654711DD9226A85E1241887A"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/InstanceID"), hasValue("uuid:54A8D5F9654711DD9226A85E1241887A"));

        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DerivedFrom"), hasValue(
                new RDFDescription(Arrays.asList(
                        // stRef|http://ns.adobe.com/xap/1.0/sType/ResourceRef#
                        new XMPEntry("http://ns.adobe.com/xap/1.0/sType/ResourceRef#instanceID", "instanceID", "uuid:3B52F3610F49DD118831FCA29C13B8DE"),
                        new XMPEntry("http://ns.adobe.com/xap/1.0/sType/ResourceRef#documentID", "documentID", "uuid:3A52F3610F49DD118831FCA29C13B8DE")
                ))
        ));

        // dc|http://purl.org/dc/elements/1.1/
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/description"), hasValue(Collections.singletonMap("x-default", "Picture 71146")));
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/format"), hasValue("image/jpeg"));

        // tiff|http://ns.adobe.com/tiff/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/ImageWidth"), hasValue("3601"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/ImageLength"), hasValue("4176"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/BitsPerSample"), hasValue(Arrays.asList("8", "8", "8")));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/Compression"), hasValue("1"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/PhotometricInterpretation"), hasValue("2"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/SamplesPerPixel"), hasValue("3"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/PlanarConfiguration"), hasValue("1"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/XResolution"), hasValue("3000000/10000"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/YResolution"), hasValue("3000000/10000"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/ResolutionUnit"), hasValue("2"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/Orientation"), hasValue("1"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/NativeDigest"), hasValue("256,257,258,259,262,274,277,284,530,531,282,283,296,301,318,319,529,532,306,270,271,272,305,315,33432;C21EE6D33E4CCA3712ECB1F5E9031A49"));

        // xap|http://ns.adobe.com/xap/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/ModifyDate"), hasValue("2008-08-06T12:43:05+10:00"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/CreatorTool"), hasValue("Adobe Photoshop CS2 Macintosh"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/CreateDate"), hasValue("2008-08-06T12:43:05+10:00"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/MetadataDate"), hasValue("2008-08-06T12:43:05+10:00"));

        // exif|http://ns.adobe.com/exif/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/ColorSpace"), hasValue("-1")); // SIC
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/PixelXDimension"), hasValue("3601"));
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/PixelYDimension"), hasValue("4176"));
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/NativeDigest"), hasValue("36864,40960,40961,37121,37122,40962,40963,37510,40964,36867,36868,33434,33437,34850,34852,34855,34856,37377,37378,37379,37380,37381,37382,37383,37384,37385,37386,37396,41483,41484,41486,41487,41488,41492,41493,41495,41728,41729,41730,41985,41986,41987,41988,41989,41990,41991,41992,41993,41994,41995,41996,42016,0,2,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,22,23,24,25,26,27,28,30;297AD344CC15F29D5283460ED026368F"));
    }

    @Test
    public void testCompoundDirectory() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;
        assertEquals(6, compound.directoryCount());

        int size = 0;
        for (int i = 0; i < compound.directoryCount(); i++) {
            Directory sub = compound.getDirectory(i);
            assertNotNull(sub);
            size += sub.size();
        }

        assertEquals(directory.size(), size);
    }

    @Test
    public void testCompoundDirectoryContentPhotoshop() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // photoshop|http://ns.adobe.com/photoshop/1.0/
        Directory photoshop = compound.getDirectory(0);
        assertEquals(4, photoshop.size());
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/DateCreated"), hasValue("2008-07-01"));
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/ColorMode"), hasValue("4"));
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/ICCProfile"), hasValue("U.S. Web Coated (SWOP) v2"));
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/History"), hasValue(""));

    }

    @Test
    public void testCompoundDirectoryContentMM() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // xapMM|http://ns.adobe.com/xap/1.0/mm/
        Directory mm = compound.getDirectory(1);
        assertEquals(3, mm.size());
        assertThat(mm.getEntryById("http://ns.adobe.com/xap/1.0/mm/DocumentID"), hasValue("uuid:54A8D5F8654711DD9226A85E1241887A"));
        assertThat(mm.getEntryById("http://ns.adobe.com/xap/1.0/mm/InstanceID"), hasValue("uuid:54A8D5F9654711DD9226A85E1241887A"));
        assertThat(mm.getEntryById("http://ns.adobe.com/xap/1.0/mm/DerivedFrom"), hasValue(
                new RDFDescription(Arrays.asList(
                        // stRef|http://ns.adobe.com/xap/1.0/sType/ResourceRef#
                        new XMPEntry("http://ns.adobe.com/xap/1.0/sType/ResourceRef#instanceID", "instanceID", "uuid:3B52F3610F49DD118831FCA29C13B8DE"),
                        new XMPEntry("http://ns.adobe.com/xap/1.0/sType/ResourceRef#documentID", "documentID", "uuid:3A52F3610F49DD118831FCA29C13B8DE")
                ))
        ));

    }

    @Test
    public void testCompoundDirectoryContentDC() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // dc|http://purl.org/dc/elements/1.1/
        Directory dc = compound.getDirectory(2);
        assertEquals(2, dc.size());
        assertThat(dc.getEntryById("http://purl.org/dc/elements/1.1/description"), hasValue(Collections.singletonMap("x-default", "Picture 71146")));
        assertThat(dc.getEntryById("http://purl.org/dc/elements/1.1/format"), hasValue("image/jpeg"));

    }

    @Test
    public void testCompoundDirectoryContentTIFF() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // tiff|http://ns.adobe.com/tiff/1.0/
        Directory tiff = compound.getDirectory(3);
        assertEquals(12, tiff.size());
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/ImageWidth"), hasValue("3601"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/ImageLength"), hasValue("4176"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/BitsPerSample"), hasValue(Arrays.asList("8", "8", "8")));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/Compression"), hasValue("1"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/PhotometricInterpretation"), hasValue("2"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/SamplesPerPixel"), hasValue("3"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/PlanarConfiguration"), hasValue("1"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/XResolution"), hasValue("3000000/10000"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/YResolution"), hasValue("3000000/10000"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/ResolutionUnit"), hasValue("2"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/Orientation"), hasValue("1"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/NativeDigest"), hasValue("256,257,258,259,262,274,277,284,530,531,282,283,296,301,318,319,529,532,306,270,271,272,305,315,33432;C21EE6D33E4CCA3712ECB1F5E9031A49"));
    }

    @Test
    public void testCompoundDirectoryContentXAP() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // xap|http://ns.adobe.com/xap/1.0/
        Directory xap = compound.getDirectory(4);
        assertEquals(4, xap.size());
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/ModifyDate"), hasValue("2008-08-06T12:43:05+10:00"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/CreatorTool"), hasValue("Adobe Photoshop CS2 Macintosh"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/CreateDate"), hasValue("2008-08-06T12:43:05+10:00"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/MetadataDate"), hasValue("2008-08-06T12:43:05+10:00"));
    }

    @Test
    public void testCompoundDirectoryContentEXIF() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // exif|http://ns.adobe.com/exif/1.0/
        Directory exif = compound.getDirectory(5);
        assertEquals(4, exif.size());
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/ColorSpace"), hasValue("-1")); // SIC. Same as unsigned short 65535, meaning "uncalibrated"?
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/PixelXDimension"), hasValue("3601"));
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/PixelYDimension"), hasValue("4176"));
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/NativeDigest"), hasValue("36864,40960,40961,37121,37122,40962,40963,37510,40964,36867,36868,33434,33437,34850,34852,34855,34856,37377,37378,37379,37380,37381,37382,37383,37384,37385,37386,37396,41483,41484,41486,41487,41488,41492,41493,41495,41728,41729,41730,41985,41986,41987,41988,41989,41990,41991,41992,41993,41994,41995,41996,42016,0,2,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,22,23,24,25,26,27,28,30;297AD344CC15F29D5283460ED026368F"));
    }

    @Test
    public void testRDFBag() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-bag-example.xml"));

        assertEquals(1, directory.size());
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/subject"), hasValue(Arrays.asList("XMP", "metadata", "ISO standard"))); // Order does not matter
    }

    @Test
    public void testRDFSeq() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-seq-example.xml"));

        assertEquals(1, directory.size());
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/subject"), hasValue(Arrays.asList("XMP", "metadata", "ISO standard")));
    }
    
    @Test
    public void testRDFAlt() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-alt-example.xml"));

        assertEquals(1, directory.size());
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/subject"), hasValue(new HashMap<String, String>() {{
            put("x-default", "One");
            put("en-us", "One");
            put("de", "Ein");
            put("no-nb", "En");
        }}));
    }

    @Test
    public void testRDFAttributeSyntax() throws IOException {
        // Alternate RDF syntax, using attribute values instead of nested tags
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertEquals(20, directory.size());

        // dc|http://purl.org/dc/elements/1.1/
        assertThat(directory.getEntryById("http://purl.org/dc/elements/1.1/format"), hasValue("image/jpeg"));

        // xap|http://ns.adobe.com/xap/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/ModifyDate"), hasValue("2008-07-16T14:44:49-07:00"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/CreatorTool"), hasValue("Adobe Photoshop CS3 Windows"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/CreateDate"), hasValue("2008-07-16T14:44:49-07:00"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/MetadataDate"), hasValue("2008-07-16T14:44:49-07:00"));

        // tiff|http://ns.adobe.com/tiff/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/Orientation"), hasValue("1"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/XResolution"), hasValue("720000/10000"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/YResolution"), hasValue("720000/10000"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/ResolutionUnit"), hasValue("2"));
        assertThat(directory.getEntryById("http://ns.adobe.com/tiff/1.0/NativeDigest"), hasValue("256,257,258,259,262,274,277,284,530,531,282,283,296,301,318,319,529,532,306,270,271,272,305,315,33432;C08D8E93274C4BEE83E86CF999955A87"));

        // exif|http://ns.adobe.com/exif/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/ColorSpace"), hasValue("-1")); // SIC. Same as unsigned short 65535, meaning "uncalibrated"?
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/PixelXDimension"), hasValue("426"));
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/PixelYDimension"), hasValue("550"));
        assertThat(directory.getEntryById("http://ns.adobe.com/exif/1.0/NativeDigest"), hasValue("36864,40960,40961,37121,37122,40962,40963,37510,40964,36867,36868,33434,33437,34850,34852,34855,34856,37377,37378,37379,37380,37381,37382,37383,37384,37385,37386,37396,41483,41484,41486,41487,41488,41492,41493,41495,41728,41729,41730,41985,41986,41987,41988,41989,41990,41991,41992,41993,41994,41995,41996,42016,0,2,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,22,23,24,25,26,27,28,30;A7F21D25E2C562F152B2C4ECC9E534DA"));

        // photoshop|http://ns.adobe.com/photoshop/1.0/
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/ColorMode"), hasValue("1"));
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/ICCProfile"), hasValue("Dot Gain 20%"));
        assertThat(directory.getEntryById("http://ns.adobe.com/photoshop/1.0/History"), hasValue(""));

        // xapMM|http://ns.adobe.com/xap/1.0/mm/
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DocumentID"), hasValue("uuid:6DCA50CC7D53DD119F20F5A7EA4C9BEC"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/InstanceID"), hasValue("uuid:6ECA50CC7D53DD119F20F5A7EA4C9BEC"));

        // Custom test, as NamedNodeMap does not preserve order (tests can't rely on XML impl specifics)
        Entry derivedFrom = directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DerivedFrom");
        assertNotNull(derivedFrom);
        assertThat(derivedFrom.getValue(), instanceOf(RDFDescription.class));

        // stRef|http://ns.adobe.com/xap/1.0/sType/ResourceRef#
        RDFDescription stRef = (RDFDescription) derivedFrom.getValue();
        assertThat(stRef.getEntryById("http://ns.adobe.com/xap/1.0/sType/ResourceRef#instanceID"), hasValue("uuid:74E1C905B405DD119306A1902BA5AA28"));
        assertThat(stRef.getEntryById("http://ns.adobe.com/xap/1.0/sType/ResourceRef#documentID"), hasValue("uuid:7A6C79768005DD119306A1902BA5AA28"));
    }

    @Test
    public void testRDFAttributeSyntaxCompound() throws IOException {
        // Alternate RDF syntax, using attribute values instead of nested tags
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;
        assertEquals(6, compound.directoryCount());

        int size = 0;
        for (int i = 0; i < compound.directoryCount(); i++) {
            Directory sub = compound.getDirectory(i);
            assertNotNull(sub);
            size += sub.size();
        }

        assertEquals(directory.size(), size);
    }

    private Directory getDirectoryByNS(final CompoundDirectory compound, final String namespace) {
        for (int i = 0; i < compound.directoryCount(); i++) {
            Directory candidate = compound.getDirectory(i);

            Iterator<Entry> entries = candidate.iterator();
            if (entries.hasNext()) {
                Entry entry = entries.next();
                if (entry.getIdentifier() instanceof String && ((String) entry.getIdentifier()).startsWith(namespace)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentPhotoshop() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // photoshop|http://ns.adobe.com/photoshop/1.0/
        Directory photoshop = getDirectoryByNS(compound, XMP.NS_PHOTOSHOP);

        assertEquals(3, photoshop.size());
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/ColorMode"), hasValue("1"));
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/ICCProfile"), hasValue("Dot Gain 20%"));
        assertThat(photoshop.getEntryById("http://ns.adobe.com/photoshop/1.0/History"), hasValue(""));
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentMM() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // xapMM|http://ns.adobe.com/xap/1.0/mm/
        Directory mm = getDirectoryByNS(compound, XMP.NS_XAP_MM);
        assertEquals(3, mm.size());
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DocumentID"), hasValue("uuid:6DCA50CC7D53DD119F20F5A7EA4C9BEC"));
        assertThat(directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/InstanceID"), hasValue("uuid:6ECA50CC7D53DD119F20F5A7EA4C9BEC"));

        // Custom test, as NamedNodeMap does not preserve order (tests can't rely on XML impl specifics)
        Entry derivedFrom = directory.getEntryById("http://ns.adobe.com/xap/1.0/mm/DerivedFrom");
        assertNotNull(derivedFrom);
        assertThat(derivedFrom.getValue(), instanceOf(RDFDescription.class));

        // stRef|http://ns.adobe.com/xap/1.0/sType/ResourceRef#
        RDFDescription stRef = (RDFDescription) derivedFrom.getValue();
        assertThat(stRef.getEntryById("http://ns.adobe.com/xap/1.0/sType/ResourceRef#instanceID"), hasValue("uuid:74E1C905B405DD119306A1902BA5AA28"));
        assertThat(stRef.getEntryById("http://ns.adobe.com/xap/1.0/sType/ResourceRef#documentID"), hasValue("uuid:7A6C79768005DD119306A1902BA5AA28"));
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentDC() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // dc|http://purl.org/dc/elements/1.1/
        Directory dc = getDirectoryByNS(compound, XMP.NS_DC);
        assertEquals(1, dc.size());

        assertThat(dc.getEntryById("http://purl.org/dc/elements/1.1/format"), hasValue("image/jpeg"));
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentTIFF() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // tiff|http://ns.adobe.com/tiff/1.0/
        Directory tiff = getDirectoryByNS(compound, XMP.NS_TIFF);
        assertEquals(5, tiff.size());
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/Orientation"), hasValue("1"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/XResolution"), hasValue("720000/10000"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/YResolution"), hasValue("720000/10000"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/ResolutionUnit"), hasValue("2"));
        assertThat(tiff.getEntryById("http://ns.adobe.com/tiff/1.0/NativeDigest"), hasValue("256,257,258,259,262,274,277,284,530,531,282,283,296,301,318,319,529,532,306,270,271,272,305,315,33432;C08D8E93274C4BEE83E86CF999955A87"));
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentXAP() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // xap|http://ns.adobe.com/xap/1.0/
        Directory xap = getDirectoryByNS(compound, XMP.NS_XAP);
        assertEquals(4, xap.size());
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/ModifyDate"), hasValue("2008-07-16T14:44:49-07:00"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/CreatorTool"), hasValue("Adobe Photoshop CS3 Windows"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/CreateDate"), hasValue("2008-07-16T14:44:49-07:00"));
        assertThat(xap.getEntryById("http://ns.adobe.com/xap/1.0/MetadataDate"), hasValue("2008-07-16T14:44:49-07:00"));
    }

    @Test
    public void testRDFAttributeSyntaxCompoundDirectoryContentEXIF() throws IOException {
        Directory directory = createReader().read(getResourceAsIIS("/xmp/rdf-attribute-shorthand.xml"));

        assertThat(directory, instanceOf(CompoundDirectory.class));
        CompoundDirectory compound = (CompoundDirectory) directory;

        // exif|http://ns.adobe.com/exif/1.0/
        Directory exif = getDirectoryByNS(compound, XMP.NS_EXIF);
        assertEquals(4, exif.size());
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/ColorSpace"), hasValue("-1")); // SIC. Same as unsigned short 65535, meaning "uncalibrated"?
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/PixelXDimension"), hasValue("426"));
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/PixelYDimension"), hasValue("550"));
        assertThat(exif.getEntryById("http://ns.adobe.com/exif/1.0/NativeDigest"), hasValue("36864,40960,40961,37121,37122,40962,40963,37510,40964,36867,36868,33434,33437,34850,34852,34855,34856,37377,37378,37379,37380,37381,37382,37383,37384,37385,37386,37396,41483,41484,41486,41487,41488,41492,41493,41495,41728,41729,41730,41985,41986,41987,41988,41989,41990,41991,41992,41993,41994,41995,41996,42016,0,2,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,22,23,24,25,26,27,28,30;A7F21D25E2C562F152B2C4ECC9E534DA"));
    }
}
