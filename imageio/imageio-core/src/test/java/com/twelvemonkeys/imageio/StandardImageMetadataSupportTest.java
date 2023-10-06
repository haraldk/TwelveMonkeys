package com.twelvemonkeys.imageio;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport.ColorSpaceType;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.ImageOrientation;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.PlanarConfiguration;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.SubimageInterpretation;
import com.twelvemonkeys.imageio.StandardImageMetadataSupport.TextEntry;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.twelvemonkeys.imageio.StandardImageMetadataSupport.builder;
import static org.junit.Assert.*;

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
    public void compressionValuesUnspecified() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .build();

        assertNull(metadata.getStandardCompressionNode());
    }

    @Test
    public void compressionValuesNone() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withCompressionTypeName("nOnE") // Case-insensitive
                .build();

        assertNull(metadata.getStandardCompressionNode());
    }

    @Test
    public void compressionValuesName() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withCompressionTypeName("foo")
                .build();

        IIOMetadataNode compressionNode = metadata.getStandardCompressionNode();
        assertNotNull(compressionNode);

        IIOMetadataNode compressionName = (IIOMetadataNode) compressionNode.getElementsByTagName("CompressionTypeName").item(0);
        assertEquals("foo", compressionName.getAttribute("value"));

        // Defaults to lossless true
        IIOMetadataNode compressionLossless = (IIOMetadataNode) compressionNode.getElementsByTagName("Lossless").item(0);
        assertEquals("TRUE", compressionLossless.getAttribute("value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withCompressionLossyIllegal() {
        builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withCompressionLossless(false);
    }

    @Test
    public void compressionValuesLossy() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withCompressionTypeName("bar")
                .withCompressionLossless(false)
                .build();

        IIOMetadataNode compressionNode = metadata.getStandardCompressionNode();
        assertNotNull(compressionNode);

        IIOMetadataNode compressionName = (IIOMetadataNode) compressionNode.getElementsByTagName("CompressionTypeName").item(0);
        assertEquals("bar", compressionName.getAttribute("value"));

        IIOMetadataNode compressionLossless = (IIOMetadataNode) compressionNode.getElementsByTagName("Lossless").item(0);
        assertEquals("FALSE", compressionLossless.getAttribute("value"));
    }

    @Test
    public void withDocumentValuesDefault() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .build();

        IIOMetadataNode documentNode = metadata.getStandardDocumentNode();
        assertNull(documentNode);
    }

    @Test
    public void withDocumentValues() {
        Calendar creationTime = Calendar.getInstance();
        creationTime.set(2022, Calendar.SEPTEMBER, 8, 14, 5, 0);

        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withFormatVersion("42")
                .withDocumentCreationTime(creationTime)
                .build();

        IIOMetadataNode documentNode = metadata.getStandardDocumentNode();
        assertNotNull(documentNode);

        IIOMetadataNode formatVersion = (IIOMetadataNode) documentNode.getElementsByTagName("FormatVersion").item(0);
        assertEquals("42", formatVersion.getAttribute("value"));

        IIOMetadataNode imageCreationTime = (IIOMetadataNode) documentNode.getElementsByTagName("ImageCreationTime").item(0);
        assertEquals("2022", imageCreationTime.getAttribute("year"));
        assertEquals("9", imageCreationTime.getAttribute("month"));
        assertEquals("8", imageCreationTime.getAttribute("day"));
        assertEquals("14", imageCreationTime.getAttribute("hour"));
        assertEquals("5", imageCreationTime.getAttribute("minute"));
        assertEquals("0", imageCreationTime.getAttribute("second"));
    }

    @Test
    public void withTextValuesDefault() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .build();

        IIOMetadataNode textNode = metadata.getStandardTextNode();
        assertNull(textNode);
    }

    @Test
    public void withTextValuesSingle() {
        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withTextEntry("foo", "bar")
                .build();

        IIOMetadataNode textNode = metadata.getStandardTextNode();
        assertNotNull(textNode);

        IIOMetadataNode textEntry = (IIOMetadataNode) textNode.getElementsByTagName("TextEntry").item(0);
        assertEquals("foo", textEntry.getAttribute("keyword"));
        assertEquals("bar", textEntry.getAttribute("value"));
    }

    @Test
    public void withTextValuesMap() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("foo", "bar");
        entries.put("bar", "xyzzy");

        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withTextEntries(entries)
                .build();

        IIOMetadataNode textNode = metadata.getStandardTextNode();
        assertNotNull(textNode);

        NodeList textEntries = textNode.getElementsByTagName("TextEntry");
        assertEquals(entries.size(), textEntries.getLength());

        int i = 0;
        for (Entry<String, String> entry : entries.entrySet()) {
            IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
            assertEquals(entry.getKey(), textEntry.getAttribute("keyword"));
            assertEquals(entry.getValue(), textEntry.getAttribute("value"));

            i++;
        }
    }

    @Test
    public void withTextValuesList() {
        List<TextEntry> entries = Arrays.asList(
                new TextEntry(null, "foo"), // No key allowed
                new TextEntry("foo", "bar"),
                new TextEntry("bar", "xyzzy"),
                new TextEntry("bar", "nothing happens..."), // Duplicates allowed
                new TextEntry("everything", "válüè", "unknown", "UTF-8", "zip")
        );

        StandardImageMetadataSupport metadata = (StandardImageMetadataSupport) builder(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY))
                .withTextEntries(entries)
                .build();

        IIOMetadataNode textNode = metadata.getStandardTextNode();
        assertNotNull(textNode);

        NodeList textEntries = textNode.getElementsByTagName("TextEntry");
        assertEquals(entries.size(), textEntries.getLength());

        for (int i = 0; i < entries.size(); i++) {
            TextEntry entry = entries.get(i);
            IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);

            assertAttributeEqualOrAbsent(entry.keyword, textEntry, "keyword");

            assertEquals(entry.value, textEntry.getAttribute("value"));

            assertAttributeEqualOrAbsent(entry.language, textEntry, "language");
            assertAttributeEqualOrAbsent(entry.encoding, textEntry, "encoding");
            assertAttributeEqualOrAbsent(entry.compression, textEntry, "compression");
        }
    }

    private static void assertAttributeEqualOrAbsent(final String expectedValue, IIOMetadataNode node, final String attribute) {
        if (expectedValue != null) {
            assertEquals(expectedValue, node.getAttribute(attribute));
        }
        else {
            assertFalse(node.hasAttribute(attribute));
        }
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