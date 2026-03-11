package com.twelvemonkeys.imageio.plugins.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

class DDSImageMetadataTest {
    @Test
    void standardMetadataDXT1() {
        DDSImageMetadata metadata = createDDSImageMetadata(BufferedImage.TYPE_INT_ARGB, DDSType.DXT1);
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        NodeList compressionTypeNames = tree.getElementsByTagName("CompressionTypeName");
        assertEquals(1, compressionTypeNames.getLength());
        IIOMetadataNode compressionTypeName = (IIOMetadataNode) compressionTypeNames.item(0);
        assertEquals("DXT1", compressionTypeName.getAttribute("value"));

        NodeList losslesses = tree.getElementsByTagName("Lossless");
        assertEquals(1, losslesses.getLength());
        IIOMetadataNode lossless = (IIOMetadataNode) losslesses.item(0);
        assertEquals("FALSE", lossless.getAttribute("value"));

        NodeList bitsPerSamples = tree.getElementsByTagName("BitsPerSample");
        assertEquals(1, bitsPerSamples.getLength());
        IIOMetadataNode bitsPerSample = (IIOMetadataNode) bitsPerSamples.item(0);
        assertEquals("8 8 8 8", bitsPerSample.getAttribute("value"));

        NodeList alphas = tree.getElementsByTagName("Alpha");
        assertEquals(1, alphas.getLength());
        IIOMetadataNode alpha = (IIOMetadataNode) alphas.item(0);
        assertEquals("nonpremultiplied", alpha.getAttribute("value"));
    }

    @Test
    void standardMetadataA8R8G8B8() {
        DDSImageMetadata metadata = createDDSImageMetadata(BufferedImage.TYPE_INT_ARGB, DDSType.A8R8G8B8);
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        NodeList compressions = tree.getElementsByTagName("Compression");
        assertEquals(0, compressions.getLength());

        NodeList bitsPerSamples = tree.getElementsByTagName("BitsPerSample");
        assertEquals(1, bitsPerSamples.getLength());
        IIOMetadataNode bitsPerSample = (IIOMetadataNode) bitsPerSamples.item(0);
        assertEquals("8 8 8 8", bitsPerSample.getAttribute("value"));

        NodeList alphas = tree.getElementsByTagName("Alpha");
        assertEquals(1, alphas.getLength());
        IIOMetadataNode alpha = (IIOMetadataNode) alphas.item(0);
        assertEquals("nonpremultiplied", alpha.getAttribute("value"));
    }

    @Test
    void standardMetadataX8R8G8B8() {
        DDSImageMetadata metadata = createDDSImageMetadata(BufferedImage.TYPE_INT_RGB, DDSType.X8R8G8B8);
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        NodeList compressions = tree.getElementsByTagName("Compression");
        assertEquals(0, compressions.getLength());

        NodeList bitsPerSamples = tree.getElementsByTagName("BitsPerSample");
        assertEquals(1, bitsPerSamples.getLength());
        IIOMetadataNode bitsPerSample = (IIOMetadataNode) bitsPerSamples.item(0);
        assertEquals("8 8 8 0", bitsPerSample.getAttribute("value")); // Or just 8 8 8?

        NodeList alphas = tree.getElementsByTagName("Alpha");
        assertEquals(1, alphas.getLength());
        IIOMetadataNode alpha = (IIOMetadataNode) alphas.item(0);
        assertEquals("none", alpha.getAttribute("value"));
    }

    @Test
    void standardMetadataX1R5G5B5() {
        DDSImageMetadata metadata = createDDSImageMetadata(BufferedImage.TYPE_INT_RGB, DDSType.X1R5G5B5);
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        NodeList compressions = tree.getElementsByTagName("Compression");
        assertEquals(0, compressions.getLength());

        NodeList bitsPerSamples = tree.getElementsByTagName("BitsPerSample");
        assertEquals(1, bitsPerSamples.getLength());
        IIOMetadataNode bitsPerSample = (IIOMetadataNode) bitsPerSamples.item(0);
        assertEquals("5 5 5 0", bitsPerSample.getAttribute("value"));  // Or just 5 5 5?

        NodeList alphas = tree.getElementsByTagName("Alpha");
        assertEquals(1, alphas.getLength());
        IIOMetadataNode alpha = (IIOMetadataNode) alphas.item(0);
        assertEquals("none", alpha.getAttribute("value"));
    }

    private static DDSImageMetadata createDDSImageMetadata(int bufferedImageType, DDSType ddsType) {
        return new DDSImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(bufferedImageType), ddsType);
    }
}