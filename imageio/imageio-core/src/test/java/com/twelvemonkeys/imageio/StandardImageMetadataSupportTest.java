package com.twelvemonkeys.imageio;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport.ColorSpaceType;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.ImageOrientation;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.PlanarConfiguration;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.SubimageInterpretation;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.junit.Test;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.util.Arrays;
import java.util.Collection;

import static com.twelvemonkeys.imageio.StandardImageMetadataSupport.builder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StandardImageMetadataSupportTest {
    @Test(expected = IllegalArgumentException.class)
    public void createNullBuilder() {
        new StandardImageMetadataSupport(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNullType() {
        new StandardImageMetadataSupport(builder(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderNullType() {
        builder(null).build();
    }

    @Test
    public void createValid() {
        IIOMetadata metadata = new StandardImageMetadataSupport(builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)));
        assertNotNull(metadata);
    }

    @Test
    public void builderValid() {
        IIOMetadata metadata = builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB))
                                       .build();
        assertNotNull(metadata);
    }


    @Test
    public void withPlanarColorspaceType() {
        // See: https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
        Collection<String> allowedValues = Arrays.asList(
                "XYZ", "Lab", "Luv", "YCbCr", "Yxy", "YCCK", "PhotoYCC",
                "RGB", "GRAY", "HSV", "HLS", "CMYK", "CMY",
                "2CLR", "3CLR", "4CLR", "5CLR", "6CLR", "7CLR", "8CLR",
                "9CLR", "ACLR", "BCLR", "CCLR", "DCLR", "ECLR", "FCLR"
        );

        for (ColorSpaceType value : ColorSpaceType.values()) {
            StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                                                                                           .withColorSpaceType(value)
                                                                                           .build();
            assertNotNull(metadata);

            IIOMetadataNode documentNode = metadata.getStandardChromaNode();
            assertNotNull(documentNode);

            IIOMetadataNode subImageInterpretation = (IIOMetadataNode) documentNode.getElementsByTagName("ColorSpaceType").item(0);
            assertEquals(value.toString(), subImageInterpretation.getAttribute("name")); // Format oddity: Why is this not "value"?
            assertTrue(allowedValues.contains(value.toString()));
        }
    }

    @Test
    public void withPlanarConfiguration() {
        // See: https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
        Collection<String> allowedValues = Arrays.asList("PixelInterleaved", "PlaneInterleaved", "LineInterleaved", "TileInterleaved");

        for (PlanarConfiguration value : PlanarConfiguration.values()) {
            StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR))
                                                                                           .withPlanarConfiguration(value)
                                                                                           .build();
            assertNotNull(metadata);

            IIOMetadataNode documentNode = metadata.getStandardDataNode();
            assertNotNull(documentNode);

            IIOMetadataNode subImageInterpretation = (IIOMetadataNode) documentNode.getElementsByTagName("PlanarConfiguration").item(0);
            assertEquals(value.toString(), subImageInterpretation.getAttribute("value"));
            assertTrue(allowedValues.contains(value.toString()));
        }
    }

    @Test
    public void withImageOrientation() {
        // See: https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
        Collection<String> allowedValues = Arrays.asList("Normal", "Rotate90", "Rotate180", "Rotate270", "FlipH", "FlipV", "FlipHRotate90", "FlipVRotate90");

        for (ImageOrientation value : ImageOrientation.values()) {
            StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                                                                                           .withOrientation(value)
                                                                                           .build();
            assertNotNull(metadata);

            IIOMetadataNode documentNode = metadata.getStandardDimensionNode();
            assertNotNull(documentNode);

            IIOMetadataNode subImageInterpretation = (IIOMetadataNode) documentNode.getElementsByTagName("ImageOrientation").item(0);
            assertEquals(value.toString(), subImageInterpretation.getAttribute("value"));
            assertTrue(allowedValues.contains(value.toString()));
        }
    }

    @Test
    public void withSubimageInterpretation() {
        // See: https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
        Collection<String> allowedValues = Arrays.asList(
                "Standalone", "SinglePage", "FullResolution", "ReducedResolution", "PyramidLayer",
                "Preview", "VolumeSlice", "ObjectView", "Panorama", "AnimationFrame",
                "TransparencyMask", "CompositingLayer", "SpectralSlice", "Unknown"
        );

        for (SubimageInterpretation value : SubimageInterpretation.values()) {
            StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB))
                                                                                           .withSubimageInterpretation(value)
                                                                                           .build();
            assertNotNull(metadata);

            IIOMetadataNode documentNode = metadata.getStandardDocumentNode();
            assertNotNull(documentNode);

            IIOMetadataNode subImageInterpretation = (IIOMetadataNode) documentNode.getElementsByTagName("SubimageInterpretation").item(0);
            assertEquals(value.toString(), subImageInterpretation.getAttribute("value"));
            assertTrue(allowedValues.contains(value.toString()));
        }
    }
}