package com.twelvemonkeys.imageio.plugins.psd;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;

class PSDMetadataTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    protected final PSDImageReaderSpi provider = createProvider();

    private PSDImageReaderSpi createProvider() {
        return new PSDImageReaderSpi();
    }

    private PSDImageReader createReader() throws IOException {
        return (PSDImageReader) provider.createReaderInstance(null);
    }

    protected URL getClassLoaderResource(final String resource) {
        return getClass().getResource(resource);
    }

    @Test
    void testLayerInfo() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psd/photoshopping.psd"))) {
            imageReader.setInput(stream);

            IIOMetadata metadata = imageReader.getImageMetadata(0);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(PSDMetadata.NATIVE_METADATA_FORMAT_NAME);
            NodeList layerInfos = root.getElementsByTagName("LayerInfo");

            assertEquals(5, layerInfos.getLength()); // Sanity

            IIOMetadataNode layer1Info = (IIOMetadataNode) layerInfos.item(0);
            assertEquals("Layer 1", layer1Info.getAttribute("name"));
            assertEquals("2", layer1Info.getAttribute("layerId"));
            assertEquals("0", layer1Info.getAttribute("top"));
            assertEquals("0", layer1Info.getAttribute("left"));
            assertEquals("225", layer1Info.getAttribute("bottom"));
            assertEquals("300", layer1Info.getAttribute("right"));
            assertEquals("norm", layer1Info.getAttribute("blendMode"));
            assertEquals("255", layer1Info.getAttribute("opacity"));
            assertEquals("base", layer1Info.getAttribute("clipping"));
            assertEquals("true", layer1Info.getAttribute("visible"));
            assertEquals("8", layer1Info.getAttribute("flags"));

            IIOMetadataNode layer2Info = (IIOMetadataNode) layerInfos.item(1);
            assertEquals("Layer 0 copy", layer2Info.getAttribute("name"));
            assertEquals("11", layer2Info.getAttribute("layerId"));
            assertEquals("0", layer2Info.getAttribute("top"));
            assertEquals("0", layer2Info.getAttribute("left"));
            assertEquals("225", layer2Info.getAttribute("bottom"));
            assertEquals("300", layer2Info.getAttribute("right"));
            assertEquals("norm", layer2Info.getAttribute("blendMode"));
            assertEquals("255", layer2Info.getAttribute("opacity"));
            assertEquals("base", layer2Info.getAttribute("clipping"));
            assertEquals("true", layer2Info.getAttribute("visible"));
            assertEquals("8", layer2Info.getAttribute("flags"));

            IIOMetadataNode layer3Info = (IIOMetadataNode) layerInfos.item(2);
            assertEquals("Layer 0 copy 2", layer3Info.getAttribute("name"));
            assertEquals("12", layer3Info.getAttribute("layerId"));
            assertEquals("0", layer3Info.getAttribute("top"));
            assertEquals("0", layer3Info.getAttribute("left"));
            assertEquals("225", layer3Info.getAttribute("bottom"));
            assertEquals("159", layer3Info.getAttribute("right"));
            assertEquals("norm", layer3Info.getAttribute("blendMode"));
            assertEquals("255", layer3Info.getAttribute("opacity"));
            assertEquals("base", layer3Info.getAttribute("clipping"));
            assertEquals("true", layer3Info.getAttribute("visible"));
            assertEquals("8", layer3Info.getAttribute("flags"));

            IIOMetadataNode layer4Info = (IIOMetadataNode) layerInfos.item(3);
            assertEquals("Layer 0 copy 3", layer4Info.getAttribute("name"));
            assertEquals("13", layer4Info.getAttribute("layerId"));
            assertEquals("0", layer4Info.getAttribute("top"));
            assertEquals("0", layer4Info.getAttribute("left"));
            assertEquals("225", layer4Info.getAttribute("bottom"));
            assertEquals("300", layer4Info.getAttribute("right"));
            assertEquals("norm", layer4Info.getAttribute("blendMode"));
            assertEquals("255", layer4Info.getAttribute("opacity"));
            assertEquals("base", layer4Info.getAttribute("clipping"));
            assertEquals("false", layer4Info.getAttribute("visible"));
            assertEquals("10", layer4Info.getAttribute("flags"));

            IIOMetadataNode layer5Info = (IIOMetadataNode) layerInfos.item(4);
            assertEquals("Layer 0", layer5Info.getAttribute("name"));
            assertEquals("3", layer5Info.getAttribute("layerId"));
            assertEquals("0", layer5Info.getAttribute("top"));
            assertEquals("0", layer5Info.getAttribute("left"));
            assertEquals("225", layer5Info.getAttribute("bottom"));
            assertEquals("300", layer5Info.getAttribute("right"));
            assertEquals("norm", layer5Info.getAttribute("blendMode"));
            assertEquals("255", layer5Info.getAttribute("opacity"));
            assertEquals("base", layer5Info.getAttribute("clipping"));
            assertEquals("false", layer5Info.getAttribute("visible"));
            assertEquals("10", layer5Info.getAttribute("flags"));
        }
    }
}