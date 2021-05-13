package com.twelvemonkeys.imageio.plugins.webp;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import static org.junit.Assert.*;

/**
 * WebPImageMetadataTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: WebPImageMetadataTest.java,v 1.0 21/11/2020 haraldk Exp$
 */
public class WebPImageMetadataTest {
    @Test
    public void testStandardFeatures() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8_, 27, 33);
        final WebPImageMetadata metadata = new WebPImageMetadata(header);

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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode chroma = metadata.getStandardChromaNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode chroma = metadata.getStandardChromaNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode compression = metadata.getStandardCompressionNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode compression = metadata.getStandardCompressionNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode compression = metadata.getStandardCompressionNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode compression = metadata.getStandardCompressionNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode data = metadata.getStandardDataNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode data = metadata.getStandardDataNode();
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
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(2, dimension.getLength());

        IIOMetadataNode imageOrientation = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("ImageOrientation", imageOrientation.getNodeName());
        assertEquals("Normal", imageOrientation.getAttribute("value"));

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) imageOrientation.getNextSibling();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDocument() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode document = metadata.getStandardDocumentNode();
        assertNotNull(document);
        assertEquals("Document", document.getNodeName());
        assertEquals(1, document.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) document.getFirstChild();
        assertEquals("FormatVersion", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardText() {
    }

    @Test
    public void testStandardTransparencyVP8() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNull(transparency); // No transparency, just defaults
    }

    @Test
    public void testStandardTransparencyVP8L() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNull(transparency); // No transparency, just defaults
    }

    @Test
    public void testStandardTransparencyVP8X() {
        VP8xChunk header = new VP8xChunk(WebP.CHUNK_VP8X, 27, 33);
        header.containsALPH = true;
        WebPImageMetadata metadata = new WebPImageMetadata(header);

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", pixelAspectRatio.getNodeName());
        assertEquals("nonpremultiplied", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }
}