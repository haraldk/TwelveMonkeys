package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;

import static org.junit.Assert.*;

/**
 * WebPImageMetadataTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: WebPImageMetadataTest.java,v 1.0 21/11/2020 haraldk Exp$
 */
public class WebPImageMetadataTest {

    private static final ImageTypeSpecifier TYPE_3BYTE_BGR = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
    private static final ImageTypeSpecifier TYPE_4BYTE_ABGR = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);

    @Test
    public void testStandardFeatures() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8_, 27, 33);
        final WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        // Standard metadata format
        assertTrue(metadata.isStandardMetadataFormatSupported());
        Node root = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        assertNotNull(root);
        assertTrue(root instanceof IIOMetadataNode);

        // Other formats
        assertNull(metadata.getNativeMetadataFormatName());
        assertNull(metadata.getExtraMetadataFormatNames());
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                metadata.getAsTree("com_foo_bar_1.0");
            }
        });

        // Read-only
        assertTrue(metadata.isReadOnly());
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                metadata.mergeTree(IIOMetadataFormatImpl.standardMetadataFormatName, new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName));
            }
        });
    }

    @Test
    public void testStandardChromaRGB() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8_, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

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
    public void testStandardChromaRGBA() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        header.containsALPH = true;
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_4BYTE_ABGR, header);

        IIOMetadataNode chroma = getStandardNode(metadata, "Chroma");
        assertNotNull(chroma);
        assertEquals("Chroma", chroma.getNodeName());
        assertEquals(3, chroma.getLength());

        IIOMetadataNode colorSpaceType = (IIOMetadataNode) chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("RGB", colorSpaceType.getAttribute("name"));

        IIOMetadataNode numChannels = (IIOMetadataNode) colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("4", numChannels.getAttribute("value"));

        IIOMetadataNode blackIsZero = (IIOMetadataNode) numChannels.getNextSibling();
        assertEquals("BlackIsZero", blackIsZero.getNodeName());
        assertEquals("TRUE", blackIsZero.getAttribute("value"));

        assertNull(blackIsZero.getNextSibling());

    }

    @Test
    public void testStandardCompressionVP8() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8_, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode compression = getStandardNode(metadata, "Compression");
        assertNotNull(compression);
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getLength());

        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("VP8", compressionTypeName.getAttribute("value"));

        IIOMetadataNode lossless = (IIOMetadataNode) compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("FALSE", lossless.getAttribute("value"));

        assertNull(lossless.getNextSibling()); // No more children
    }

    @Test
    public void testStandardCompressionVP8L() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8L, 27, 33);
        header.isLossless = true;
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode compression = getStandardNode(metadata, "Compression");
        assertNotNull(compression);
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getLength());

        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("VP8L", compressionTypeName.getAttribute("value"));

        IIOMetadataNode lossless = (IIOMetadataNode) compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("TRUE", lossless.getAttribute("value"));

        assertNull(lossless.getNextSibling()); // No more children
    }

    @Test
    public void testStandardCompressionVP8X() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode compression = getStandardNode(metadata, "Compression");
        assertNotNull(compression);
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getLength());

        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("VP8", compressionTypeName.getAttribute("value"));

        IIOMetadataNode lossless = (IIOMetadataNode) compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("FALSE", lossless.getAttribute("value"));

        assertNull(lossless.getNextSibling()); // No more children
    }

    @Test
    public void testStandardCompressionVP8XLossless() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        header.isLossless = true;
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode compression = getStandardNode(metadata, "Compression");
        assertNotNull(compression);
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getLength());

        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("VP8L", compressionTypeName.getAttribute("value"));

        IIOMetadataNode lossless = (IIOMetadataNode) compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("TRUE", lossless.getAttribute("value"));

        assertNull(lossless.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataRGB() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8_, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

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
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        header.containsALPH = true;
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_4BYTE_ABGR, header);

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
    public void testStandardDimensionNormal() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

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
    public void testStandardDocument() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

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
    public void testStandardText() {
        // No text node yet...
    }

    @Test
    public void testStandardTransparencyVP8() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");

        if (transparency != null) {
            assertNotNull(transparency);
            assertEquals("Transparency", transparency.getNodeName());
            assertEquals(1, transparency.getLength());

            IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
            assertEquals("Alpha", alpha.getNodeName());
            assertEquals("none", alpha.getAttribute("value"));

            assertNull(alpha.getNextSibling()); // No more children
        }
        // Else no transparency, just defaults
    }

    @Test
    public void testStandardTransparencyVP8L() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_3BYTE_BGR, header);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        if (transparency != null) {
            assertNotNull(transparency);
            assertEquals("Transparency", transparency.getNodeName());
            assertEquals(1, transparency.getLength());

            IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
            assertEquals("Alpha", alpha.getNodeName());
            assertEquals("none", alpha.getAttribute("value"));

            assertNull(alpha.getNextSibling()); // No more children
        }
        // Else no transparency, just defaults
    }

    @Test
    public void testStandardTransparencyVP8X() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        header.containsALPH = true;
        WebPImageMetadata metadata = new WebPImageMetadata(TYPE_4BYTE_ABGR, header);

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", alpha.getNodeName());
        assertEquals("nonpremultiplied", alpha.getAttribute("value"));

        assertNull(alpha.getNextSibling()); // No more children
    }

    private IIOMetadataNode getStandardNode(IIOMetadata metadata, String nodeName) {
        IIOMetadataNode asTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList nodes = asTree.getElementsByTagName(nodeName);

        return nodes.getLength() > 0 ? (IIOMetadataNode) nodes.item(0) : null;
    }
}