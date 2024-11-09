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

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.util.Calendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TGAMetadataTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TGAMetadataTest.java,v 1.0 08/04/2021 haraldk Exp$
 */
public class TGAMetadataTest {

    private static final ImageTypeSpecifier TYPE_BYTE_GRAY = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);

    private static final ImageTypeSpecifier TYPE_3BYTE_BGR = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);

    @Test
    public void testStandardFeatures() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR, false);
        final TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        // Standard metadata format
        assertTrue(metadata.isStandardMetadataFormatSupported());
        Node root = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        assertNotNull(root);
        assertTrue(root instanceof IIOMetadataNode);

        // Other formats
        assertNull(metadata.getNativeMetadataFormatName());
        assertNull(metadata.getExtraMetadataFormatNames());
        assertThrows(IllegalArgumentException.class, () -> metadata.getAsTree("com_foo_bar_1.0"));

        // Read-only
        assertTrue(metadata.isReadOnly());
        assertThrows(IllegalStateException.class, () -> metadata.mergeTree(IIOMetadataFormatImpl.standardMetadataFormatName, new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName)));
    }

    @Test
    public void testStandardChromaGray() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  false);
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, null);

        IIOMetadataNode chroma = getStandardNode(metadata, "Chroma");
        assertNotNull(chroma);
        assertEquals("Chroma", chroma.getNodeName());
        assertEquals(3, chroma.getLength());

        IIOMetadataNode colorSpaceType = (IIOMetadataNode) chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("GRAY", colorSpaceType.getAttribute("name"));

        IIOMetadataNode numChannels = (IIOMetadataNode) colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("1", numChannels.getAttribute("value"));

        IIOMetadataNode blackIsZero = (IIOMetadataNode) numChannels.getNextSibling();
        assertEquals("BlackIsZero", blackIsZero.getNodeName());
        assertEquals("TRUE", blackIsZero.getAttribute("value"));

        assertNull(blackIsZero.getNextSibling()); // No more children
    }

    @Test
    public void testStandardChromaRGB() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR,  false);
        TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        IIOMetadataNode chroma = getStandardNode(metadata, "Chroma");
        assertNotNull(chroma);
        assertEquals("Chroma", chroma.getNodeName());
        assertEquals(3, chroma.getLength());

        IIOMetadataNode colorSpaceType = (IIOMetadataNode) chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("RGB", colorSpaceType.getAttribute("name"));

        IIOMetadataNode numChannels = (IIOMetadataNode) colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("3", numChannels.getAttribute("value"));

        IIOMetadataNode blackIsZero = (IIOMetadataNode) numChannels.getNextSibling();
        assertEquals("BlackIsZero", blackIsZero.getNodeName());
        assertEquals("TRUE", blackIsZero.getAttribute("value"));

        assertNull(blackIsZero.getNextSibling()); // No more children
    }

    @Test
    public void testStandardChromaPalette() {
        byte[] bw = {0, (byte) 0xff};
        IndexColorModel indexColorModel = new IndexColorModel(8, bw.length, bw, bw, bw, -1);
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromIndexColorModel(indexColorModel);
        TGAHeader header = TGAHeader.from(type, false);
        TGAMetadata metadata = new TGAMetadata(type, header, null);

        IIOMetadataNode chroma = getStandardNode(metadata, "Chroma");
        assertNotNull(chroma);
        assertEquals("Chroma", chroma.getNodeName());
        assertEquals(4, chroma.getLength());

        IIOMetadataNode colorSpaceType = (IIOMetadataNode) chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("RGB", colorSpaceType.getAttribute("name"));

        IIOMetadataNode numChannels = (IIOMetadataNode) colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("3", numChannels.getAttribute("value"));

        IIOMetadataNode blackIsZero = (IIOMetadataNode) numChannels.getNextSibling();
        assertEquals("BlackIsZero", blackIsZero.getNodeName());
        assertEquals("TRUE", blackIsZero.getAttribute("value"));

        IIOMetadataNode palette = (IIOMetadataNode) blackIsZero.getNextSibling();
        assertEquals("Palette", palette.getNodeName());
        assertEquals(bw.length, palette.getLength());

        for (int i = 0;  i < palette.getLength(); i++) {
            IIOMetadataNode item0 = (IIOMetadataNode) palette.item(i);
            assertEquals("PaletteEntry", item0.getNodeName());
            assertEquals(String.valueOf(i), item0.getAttribute("index"));
            String rgb = String.valueOf(bw[i] & 0xff);
            assertEquals(rgb, item0.getAttribute("red"));
            assertEquals(rgb, item0.getAttribute("green"));
            assertEquals(rgb, item0.getAttribute("blue"));
        }

        // TODO: BackgroundIndex == 1??
    }

    @Test
    public void testStandardCompressionRLE() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        IIOMetadataNode compression = getStandardNode(metadata, "Compression");
        assertNotNull(compression);
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getLength());

        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("RLE", compressionTypeName.getAttribute("value"));

        IIOMetadataNode lossless = (IIOMetadataNode) compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("TRUE", lossless.getAttribute("value"));

        assertNull(lossless.getNextSibling()); // No more children
    }

    @Test
    public void testStandardCompressionNone() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR,  false);
        TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        assertNull(getStandardNode(metadata, "Compression")); // No compression, all default...
    }

    @Test
    public void testStandardDataGray() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, null);

        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataRGB() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataRGBA() {
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        TGAHeader header = TGAHeader.from(type, true);
        TGAMetadata metadata = new TGAMetadata(type, header, null);

        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8 8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataPalette() {
        byte[] rgb = new byte[1 << 8]; // Colors doesn't really matter here
        IndexColorModel indexColorModel = new IndexColorModel(8, rgb.length, rgb, rgb, rgb, 0);
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromIndexColorModel(indexColorModel);
        TGAHeader header = TGAHeader.from(type, true);
        TGAMetadata metadata = new TGAMetadata(type, header, null);

        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("Index", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionNormal() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, null);

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(2, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        IIOMetadataNode imageOrientation = (IIOMetadataNode) pixelAspectRatio.getNextSibling();
        assertEquals("ImageOrientation", imageOrientation.getNodeName());
        assertEquals("Normal", imageOrientation.getAttribute("value"));


        assertNull(imageOrientation.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionFlipH() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        header.origin = TGA.ORIGIN_LOWER_LEFT;
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, null);

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(2, dimension.getLength());


        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        IIOMetadataNode imageOrientation = (IIOMetadataNode) pixelAspectRatio.getNextSibling();
        assertEquals("ImageOrientation", imageOrientation.getNodeName());
        assertEquals("FlipH", imageOrientation.getAttribute("value"));


        assertNull(imageOrientation.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDocument() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, null);

        IIOMetadataNode document = getStandardNode(metadata, "Document");
        assertNotNull(document);
        assertEquals("Document", document.getNodeName());
        assertEquals(1, document.getLength());

        IIOMetadataNode formatVersion = (IIOMetadataNode) document.getFirstChild();
        assertEquals("FormatVersion", formatVersion.getNodeName());
        assertEquals("1.0", formatVersion.getAttribute("value"));

        assertNull(formatVersion.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDocumentExtensions() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        TGAExtensions extensions = new TGAExtensions();
        extensions.creationDate = Calendar.getInstance();
        extensions.creationDate.set(2021, Calendar.APRIL, 8, 18, 55, 0);
        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, extensions);

        IIOMetadataNode document = getStandardNode(metadata, "Document");
        assertNotNull(document);
        assertEquals("Document", document.getNodeName());
        assertEquals(2, document.getLength());

        IIOMetadataNode formatVersion = (IIOMetadataNode) document.getFirstChild();
        assertEquals("FormatVersion", formatVersion.getNodeName());
        assertEquals("2.0", formatVersion.getAttribute("value"));

        IIOMetadataNode imageCreationTime = (IIOMetadataNode) formatVersion.getNextSibling();
        assertEquals("ImageCreationTime", imageCreationTime.getNodeName());
        assertEquals("2021", imageCreationTime.getAttribute("year"));
        assertEquals("4", imageCreationTime.getAttribute("month"));
        assertEquals("8", imageCreationTime.getAttribute("day"));
        assertEquals("18", imageCreationTime.getAttribute("hour"));
        assertEquals("55", imageCreationTime.getAttribute("minute"));
        assertEquals("0", imageCreationTime.getAttribute("second"));

        assertNull(imageCreationTime.getNextSibling()); // No more children
    }

    @Test
    public void testStandardText() {
        TGAHeader header = TGAHeader.from(TYPE_BYTE_GRAY,  true);
        header.identification = "MY_FILE.TGA";

        TGAExtensions extensions = new TGAExtensions();
        extensions.softwareId = "TwelveMonkeys";
        extensions.authorName = "Harald K";
        extensions.authorComments = "Comments, comments... ";

        TGAMetadata metadata = new TGAMetadata(TYPE_BYTE_GRAY, header, extensions);

        IIOMetadataNode text = getStandardNode(metadata, "Text");
        assertNotNull(text);
        assertEquals("Text", text.getNodeName());
        assertEquals(4, text.getLength());

        IIOMetadataNode textEntry = (IIOMetadataNode) text.item(0);
        assertEquals("TextEntry", textEntry.getNodeName());
        assertEquals("DocumentName", textEntry.getAttribute("keyword"));
        assertEquals(header.getIdentification(), textEntry.getAttribute("value"));

        textEntry = (IIOMetadataNode) text.item(1);
        assertEquals("TextEntry", textEntry.getNodeName());
        assertEquals("Software", textEntry.getAttribute("keyword"));
        assertEquals(extensions.getSoftware(), textEntry.getAttribute("value"));

        textEntry = (IIOMetadataNode) text.item(2);
        assertEquals("TextEntry", textEntry.getNodeName());
        assertEquals("Artist", textEntry.getAttribute("keyword"));
        assertEquals(extensions.getAuthorName(), textEntry.getAttribute("value"));

        textEntry = (IIOMetadataNode) text.item(3);
        assertEquals("TextEntry", textEntry.getNodeName());
        assertEquals("UserComment", textEntry.getAttribute("keyword"));
        assertEquals(extensions.getAuthorComments(), textEntry.getAttribute("value"));
    }

    @Test
    public void testStandardTransparencyRGB() {
        TGAHeader header = TGAHeader.from(TYPE_3BYTE_BGR,  true);
        TGAMetadata metadata = new TGAMetadata(TYPE_3BYTE_BGR, header, null);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", alpha.getNodeName());
        assertEquals("none", alpha.getAttribute("value"));

        assertNull(alpha.getNextSibling()); // No more children
    }

    @Test
    public void testStandardTransparencyRGBA() {
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);
        TGAHeader header = TGAHeader.from(type, true);
        TGAMetadata metadata = new TGAMetadata(type, header, null);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", alpha.getNodeName());
        assertEquals("nonpremultiplied", alpha.getAttribute("value"));

        assertNull(alpha.getNextSibling()); // No more children
    }

    @Test
    public void testStandardTransparencyPalette() {
        byte[] bw = {0, (byte) 0xff};
        IndexColorModel indexColorModel = new IndexColorModel(8, bw.length, bw, bw, bw, 1);
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromIndexColorModel(indexColorModel);
        TGAHeader header = TGAHeader.from(type, true);
        TGAMetadata metadata = new TGAMetadata(type, header, null);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(2, transparency.getLength());

        IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", alpha.getNodeName());
        assertEquals("nonpremultiplied", alpha.getAttribute("value"));

        IIOMetadataNode transparentIndex = (IIOMetadataNode) alpha.getNextSibling();
        assertEquals("TransparentIndex", transparentIndex.getNodeName());
        assertEquals("1", transparentIndex.getAttribute("value"));

        assertNull(transparentIndex.getNextSibling()); // No more children
    }

    private IIOMetadataNode getStandardNode(IIOMetadata metadata, String nodeName) {
        IIOMetadataNode asTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList nodes = asTree.getElementsByTagName(nodeName);

        return nodes.getLength() > 0 ? (IIOMetadataNode) nodes.item(0) : null;
    }
}