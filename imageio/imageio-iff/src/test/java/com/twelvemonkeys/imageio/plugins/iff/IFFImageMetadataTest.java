package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.nio.charset.StandardCharsets;

import static java.awt.image.BufferedImage.*;
import static org.junit.Assert.*;

public class IFFImageMetadataTest {

    private static final ImageTypeSpecifier TYPE_8_BIT_GRAY = ImageTypeSpecifiers.createFromBufferedImageType(TYPE_BYTE_GRAY);
    private static final ImageTypeSpecifier TYPE_8_BIT_PALETTE = ImageTypeSpecifiers.createFromBufferedImageType(TYPE_BYTE_INDEXED);
    private static final ImageTypeSpecifier TYPE_24_BIT_RGB = ImageTypeSpecifiers.createFromBufferedImageType(TYPE_3BYTE_BGR);
    private static final ImageTypeSpecifier TYPE_32_BIT_ARGB = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);
    private static final ImageTypeSpecifier TYPE_32_BIT_ARGB_DEEP = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE);

    @Test
    public void testStandardFeatures() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        final IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

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
    public void testStandardChromaGray() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_GRAY, header, header.colorMap());

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
    public void testStandardChromaRGB() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

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
    public void testStandardChromaPalette() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 1, BMHDChunk.MASK_TRANSPARENT_COLOR, BMHDChunk.COMPRESSION_BYTE_RUN, 1));

        byte[] bw = {0, (byte) 0xff};
        ImageTypeSpecifier fromIndexColorModel = ImageTypeSpecifiers.createFromIndexColorModel(new IndexColorModel(header.bitplanes(), bw.length, bw, bw, bw, header.transparentIndex()));
        IFFImageMetadata metadata = new IFFImageMetadata(fromIndexColorModel, header, header.colorMap());

        IIOMetadataNode chroma = getStandardNode(metadata, "Chroma");
        assertNotNull(chroma);
        assertEquals("Chroma", chroma.getNodeName());
        assertEquals(5, chroma.getLength());

        IIOMetadataNode colorSpaceType = (IIOMetadataNode) chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("RGB", colorSpaceType.getAttribute("name"));

        IIOMetadataNode numChannels = (IIOMetadataNode) colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("4", numChannels.getAttribute("value"));

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

        // BackgroundIndex == 1
        IIOMetadataNode backgroundIndex = (IIOMetadataNode) palette.getNextSibling();
        assertEquals("BackgroundIndex", backgroundIndex.getNodeName());
        assertEquals("1", backgroundIndex.getAttribute("value"));

        // No more elements
        assertNull(backgroundIndex.getNextSibling());
    }

    @Test
    public void testStandardCompressionRLE() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

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
    public void testStandardCompressionNone() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_NONE, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

        assertNull(getStandardNode(metadata, "Compression")); // No compression, all default...
    }

    @Test
    public void testStandardDataILBM_Gray() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_GRAY, header, header.colorMap());

        IIOMetadataNode data = getStandardNode(metadata, "Data");
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
    public void testStandardDataILBM_RGB() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

        IIOMetadataNode data = getStandardNode(metadata, "Data");
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
    public void testStandardDataILBM_RGBA() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 32, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_32_BIT_ARGB, header, header.colorMap());

        IIOMetadataNode data = getStandardNode(metadata, "Data");
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
    public void testStandardDataILBM_Palette() throws IIOException {
        for (int i = 1; i <= 8; i++) {
            Form header = Form.ofType(IFF.TYPE_ILBM)
                              .with(new BMHDChunk(300, 200, i, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

            byte[] rgb = new byte[2 << i]; // Colors doesn't really matter here
            ImageTypeSpecifier fromIndexColorModel = ImageTypeSpecifiers.createFromIndexColorModel(new IndexColorModel(header.bitplanes(), rgb.length, rgb, rgb, rgb, 0));
            IFFImageMetadata metadata = new IFFImageMetadata(fromIndexColorModel, header, header.colorMap());

            IIOMetadataNode data = getStandardNode(metadata, "Data");
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
    public void testStandardDataPBM_Gray() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_PBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_GRAY, header, header.colorMap());

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
    public void testStandardDataPBM_RGB() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_PBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

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
    public void testStandardDimensionNoViewport() throws IIOException {
        BMHDChunk bitmapHeader = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);
        bitmapHeader.xAspect = 0;
        bitmapHeader.yAspect = 0;

        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(bitmapHeader);

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");

        if (dimension != null) {
            assertEquals("Dimension", dimension.getNodeName());
            assertEquals(1, dimension.getLength());

            IIOMetadataNode imageOrientation = (IIOMetadataNode) dimension.getFirstChild();
            assertEquals("ImageOrientation", imageOrientation.getNodeName());
            assertEquals("Normal", imageOrientation.getAttribute("value"));

            assertNull(imageOrientation.getNextSibling()); // No more children
        }
    }

    @Test
    public void testStandardDimensionNormal() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0))
                          .with(new CAMGChunk(4));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");

        // No Dimension node is okay, or one with an aspect ratio of 1.0
        if (dimension != null) {
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
    }

    @Test
    public void testStandardDimensionHires() throws IIOException {
        BMHDChunk bitmapHeader = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);
        bitmapHeader.xAspect = 2;
        bitmapHeader.yAspect = 1;

        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x8000;

        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(bitmapHeader)
                          .with(viewPort);

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(2, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("2.0", pixelAspectRatio.getAttribute("value"));

        IIOMetadataNode imageOrientation = (IIOMetadataNode) pixelAspectRatio.getNextSibling();
        assertEquals("ImageOrientation", imageOrientation.getNodeName());
        assertEquals("Normal", imageOrientation.getAttribute("value"));

        assertNull(imageOrientation.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionInterlaced() throws IIOException {
        BMHDChunk bitmapHeader = new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0);
        bitmapHeader.xAspect = 1;
        bitmapHeader.yAspect = 2;

        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x4;

        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(bitmapHeader)
                          .with(viewPort);

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

        IIOMetadataNode dimension = getStandardNode(metadata, "Dimension");
        assertNotNull(dimension);
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(2, dimension.getLength());

        IIOMetadataNode pixelAspectRatio = (IIOMetadataNode) dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("0.5", pixelAspectRatio.getAttribute("value"));

        IIOMetadataNode imageOrientation = (IIOMetadataNode) pixelAspectRatio.getNextSibling();
        assertEquals("ImageOrientation", imageOrientation.getNodeName());
        assertEquals("Normal", imageOrientation.getAttribute("value"));

        assertNull(imageOrientation.getNextSibling()); // No more children
    }

    @Test
    public void testStandardDimensionHiresInterlaced() throws IIOException {
        CAMGChunk viewPort = new CAMGChunk(4);
        viewPort.camg = 0x8004;
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0))
                          .with(viewPort);

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

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
    public void testStandardDocument() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

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
    public void testStandardText() throws IIOException {
        int[] chunks = {IFF.CHUNK_ANNO, IFF.CHUNK_ANNO, IFF.CHUNK_UTF8};
        String[] texts = {"annotation", "dupe", "äñnótâtïøñ"};
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 8, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0))
                .with(new GenericChunk(chunks[0], texts[0].getBytes(StandardCharsets.US_ASCII)))
                .with(new GenericChunk(chunks[1], texts[1].getBytes(StandardCharsets.US_ASCII)))
                .with(new GenericChunk(chunks[2], texts[2].getBytes(StandardCharsets.UTF_8)));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_8_BIT_PALETTE, header, header.colorMap());

        IIOMetadataNode text = getStandardNode(metadata, "Text");
        assertNotNull(text);
        assertEquals("Text", text.getNodeName());
        assertEquals(texts.length, text.getLength());

        for (int i = 0; i < texts.length; i++) {
            IIOMetadataNode textEntry = (IIOMetadataNode) text.item(i);
            assertEquals("TextEntry", textEntry.getNodeName());
            assertEquals(IFFUtil.toChunkStr(chunks[i]), textEntry.getAttribute("keyword"));
            assertEquals(texts[i], textEntry.getAttribute("value"));
        }
    }

    @Test
    public void testStandardTransparencyRGB() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 24, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_24_BIT_RGB, header, header.colorMap());

        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");

        if (transparency != null) {
            assertEquals("Transparency", transparency.getNodeName());
            assertEquals(1, transparency.getLength());

            IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
            assertEquals("Alpha", alpha.getNodeName());
            assertEquals("none", alpha.getAttribute("value"));

            assertNull(alpha.getNextSibling()); // No more children
        }
        // Otherwise, no transparency, just defaults
    }

    @Test
    public void testStandardTransparencyRGBA() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 32, BMHDChunk.MASK_HAS_MASK, BMHDChunk.COMPRESSION_BYTE_RUN, 0));

        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_32_BIT_ARGB, header, header.colorMap());

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
    public void testStandardTransparencyPalette() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_ILBM)
                          .with(new BMHDChunk(300, 200, 1, BMHDChunk.MASK_TRANSPARENT_COLOR, BMHDChunk.COMPRESSION_BYTE_RUN, 1));

        byte[] bw = {0, (byte) 0xff};
        ImageTypeSpecifier fromIndexColorModel = ImageTypeSpecifiers.createFromIndexColorModel(new IndexColorModel(header.bitplanes(), bw.length, bw, bw, bw, header.transparentIndex()));
        IFFImageMetadata metadata = new IFFImageMetadata(fromIndexColorModel, header, header.colorMap());

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

    @Test
    public void testStandardRGB8() throws IIOException {
        Form header = Form.ofType(IFF.TYPE_RGB8)
                          .with(new BMHDChunk(300, 200, 25, BMHDChunk.MASK_NONE, BMHDChunk.COMPRESSION_BYTE_RUN, 0));
        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_32_BIT_ARGB, header, header.colorMap());

        // Chroma
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

        assertNull(blackIsZero.getNextSibling()); // No more children

        // Data
        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFormat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFormat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFormat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFormat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8 1", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children

        // Transparency
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
    public void testStandardDEEP() throws IIOException {
        DPELChunk dpel = new DPELChunk(20);
        dpel.typeDepths = new DPELChunk.TypeDepth[4];
        for (int i = 0; i < dpel.typeDepths.length; i++) {
            dpel.typeDepths[i] = new DPELChunk.TypeDepth(i == 0 ? 11 : i, 8);
        }

        Form header = Form.ofType(IFF.TYPE_DEEP)
                          .with(new DGBLChunk(8))
                          .with(dpel);
        IFFImageMetadata metadata = new IFFImageMetadata(TYPE_32_BIT_ARGB_DEEP, header, header.colorMap());

        // Chroma
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

        // TODO: BackgroundColor = 0x666666

        assertNull(blackIsZero.getNextSibling()); // No more children

        // Data
        IIOMetadataNode data = getStandardNode(metadata, "Data");
        assertNotNull(data);
        assertEquals("Data", data.getNodeName());
        assertEquals(3, data.getLength());

        IIOMetadataNode planarConfiguration = (IIOMetadataNode) data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", planarConfiguration.getAttribute("value"));

        IIOMetadataNode sampleFormat = (IIOMetadataNode) planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFormat.getNodeName());
        assertEquals("UnsignedIntegral", sampleFormat.getAttribute("value"));

        IIOMetadataNode bitsPerSample = (IIOMetadataNode) sampleFormat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8 8", bitsPerSample.getAttribute("value"));

        assertNull(bitsPerSample.getNextSibling()); // No more children

        // Transparency
        IIOMetadataNode transparency = getStandardNode(metadata, "Transparency");
        assertNotNull(transparency);
        assertEquals("Transparency", transparency.getNodeName());
        assertEquals(1, transparency.getLength());

        IIOMetadataNode alpha = (IIOMetadataNode) transparency.getFirstChild();
        assertEquals("Alpha", alpha.getNodeName());
        assertEquals("premultiplied", alpha.getAttribute("value"));

        assertNull(alpha.getNextSibling()); // No more children
    }

    // TODO: Test RGB8 + ColorMap

    private IIOMetadataNode getStandardNode(IIOMetadata metadata, String nodeName) {
        IIOMetadataNode asTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList nodes = asTree.getElementsByTagName(nodeName);

        return nodes.getLength() > 0 ? (IIOMetadataNode) nodes.item(0) : null;
    }
}