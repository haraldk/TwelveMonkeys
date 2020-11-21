package com.twelvemonkeys.imageio.plugins.iff;

import static org.junit.Assert.*;

import java.awt.image.IndexColorModel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.w3c.dom.Node;

public class IFFImageMetadataTest {
    @Test
    public void testStandardFeatures() {
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        final IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

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
    public void testStandardChromaGray() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode chroma = metadata.getStandardChromaNode();
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
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

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
    public void testStandardChromaPalette() {
        BMHDChunk header = new BMHDChunk(300, 200, 1, BMHDChunk.MASK_TRANSPARENT_COLOR, BMHDChunk.COMPRESSION_BYTE_RUN, 1);

        byte[] bw = {0, (byte) 0xff};
        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, new IndexColorModel(header.bitplanes, bw.length, bw, bw, bw, header.transparentIndex), null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode chroma = metadata.getStandardChromaNode();
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
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode compression = metadata.getStandardCompressionNode();
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
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_NONE, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        assertNull(metadata.getStandardCompressionNode()); // No compression, all default...
    }

    @Test
    public void testStandardDataILBM_Gray() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode data = metadata.getStandardDataNode();
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PlaneInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataILBM_RGB() {
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode data = metadata.getStandardDataNode();
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PlaneInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataILBM_RGBA() {
        BMHDChunk header = new BMHDChunk(300, 200, 32, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode data = metadata.getStandardDataNode();
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PlaneInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFomat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFomat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8 8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataILBM_Palette() {
        for (int i = 1; i <= 8; i++) {
            BMHDChunk header = new BMHDChunk(300, 200, i, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

            byte[] rgb = new byte[2 << i]; // Colors doesn't really matter here
            IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, new IndexColorModel(header.bitplanes, rgb.length, rgb, rgb, rgb, 0), null, Collections.<GenericChunk>emptyList());

            IIOMetadataNode data = metadata.getStandardDataNode();
            assertNotNull(data);
            assertEquals("Data", data.getNodeName());
            assertEquals(3, data.getLength());

            IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
            assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
            assertEquals("PlaneInterleaved", planarConfiguration.getAttribute("value"));

            IIOMetadataNode sampleFomat = (IIOMetadataNode) planarConfiguration.getNextSibling();
            assertEquals("SampleFormat", sampleFomat.getNodeName());
            assertEquals("Index", sampleFomat.getAttribute("value"));

            IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFomat.getNextSibling();
            assertEquals("BitsPerSample", bitsPerSample.getNodeName());
            assertEquals(String.valueOf(i), bitsPerSample.getAttribute("value"));

            assertNull(bitsPerSample.getNextSibling()); // No more children
        }
    }

    @Test
    public void testStandardDataPBM_Gray() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_PBM, header, null, null, Collections.<GenericChunk>emptyList());

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
        assertEquals("8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDataPBM_RGB() {
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_PBM, header, null, null, Collections.<GenericChunk>emptyList());

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
    public void testStandardDimensionNoViewport() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNull(dimension);
    }

    @Test
    public void testStandardDimensionNormal() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, new CAMGChunk(4), Collections.<GenericChunk>emptyList());

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(1, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionHires() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);
        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x8000;

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, viewPort, Collections.<GenericChunk>emptyList());

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(1, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("2.0", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionInterlaced() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x4;

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, viewPort, Collections.<GenericChunk>emptyList());

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(1, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("0.5", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionHiresInterlaced() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x8004;

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, viewPort, Collections.<GenericChunk>emptyList());

        IIOMetadataNode dimension = metadata.getStandardDimensionNode();
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(1, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDocument() {
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

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
        BMHDChunk header = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        String[] texts = {"annotation", "äñnótâtïøñ"};
        List<GenericChunk> meta = Arrays.asList(new GenericChunk(IFF.CHUNK_ANNO, texts[0].getBytes(StandardCharsets.US_ASCII)),
                                                new GenericChunk(IFF.CHUNK_UTF8, texts[1].getBytes(StandardCharsets.UTF_8)));
        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, meta);

        IIOMetadataNode text = metadata.getStandardTextNode();
        assertNotNull(text);
        assertEquals("Text", text.getNodeName());
        assertEquals(texts.length, text.getLength());

        for (int i = 0; i < texts.length; i++) {
            IIOMetadataNode textEntry = (IIOMetadataNode) text.item(i);
            assertEquals("TextEntry", textEntry.getNodeName());
            assertEquals(IFFUtil.toChunkStr(meta.get(i).chunkId), textEntry.getAttribute("keyword"));
            assertEquals(texts[i], textEntry.getAttribute("value"));
        }
    }

    @Test
    public void testStandardTransparencyRGB() {
        BMHDChunk header = new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNull(transparency); // No transparency, just defaults
    }

    @Test
    public void testStandardTransparencyRGBA() {
        BMHDChunk header = new BMHDChunk(300, 200, 32, BMHDChunk.MASK_HAS_MASK, BMHDChunk.COMPRESSION_BYTE_RUN, 0);

        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, null, null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", pixelAspectRatio.getNodeName());
        assertEquals("nonpremultiplied", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }

    @Test
    public void testStandardTransparencyPalette() {
        BMHDChunk header = new BMHDChunk(300, 200, 1, BMHDChunk.MASK_TRANSPARENT_COLOR, BMHDChunk.COMPRESSION_BYTE_RUN, 1);

        byte[] bw = {0, (byte) 0xff};
        IFFImageMetadata metadata = new IFFImageMetadata(IFF.TYPE_ILBM, header, new IndexColorModel(header.bitplanes, bw.length, bw, bw, bw, header.transparentIndex), null, Collections.<GenericChunk>emptyList());

        IIOMetadataNode transparency = metadata.getStandardTransparencyNode();
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("TransparentIndex", pixelAspectRatio.getNodeName());
        assertEquals("1", pixelAspectRatio.getAttribute("value"));

        assertNull(pixelAspectRatio.getNextSibling()); // No more children
    }
}