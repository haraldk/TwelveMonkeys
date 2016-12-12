package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import com.twelvemonkeys.lang.StringUtil;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * TIFFImageMetadataTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFImageMetadataTest.java,v 1.0 30/07/15 harald.kuhr Exp$
 */
public class TIFFImageMetadataTest {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    // TODO: Candidate super method
    private URL getClassLoaderResource(final String resource) {
        return getClass().getResource(resource);
    }

    // TODO: Candidate abstract super method
    private IIOMetadata createMetadata(final String resource) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource(resource))) {
            Directory ifd = new TIFFReader().read(input);
//            System.err.println("ifd: " + ifd);
            return new TIFFImageMetadata(ifd);
        }
    }

    @Test
    public void testMetadataStandardFormat() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/smallliz.tif");
        Node root = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        // Root: "javax_imageio_1.0"
        assertNotNull(root);
        assertEquals(IIOMetadataFormatImpl.standardMetadataFormatName, root.getNodeName());
        assertEquals(6, root.getChildNodes().getLength());

        // "Chroma"
        Node chroma = root.getFirstChild();
        assertEquals("Chroma", chroma.getNodeName());

        assertEquals(3, chroma.getChildNodes().getLength());

        Node colorSpaceType = chroma.getFirstChild();
        assertEquals("ColorSpaceType", colorSpaceType.getNodeName());
        assertEquals("YCbCr", ((Element) colorSpaceType).getAttribute("value"));

        Node numChannels = colorSpaceType.getNextSibling();
        assertEquals("NumChannels", numChannels.getNodeName());
        assertEquals("3", ((Element) numChannels).getAttribute("value"));

        Node blackIsZero = numChannels.getNextSibling();
        assertEquals("BlackIsZero", blackIsZero.getNodeName());
        assertEquals(0, blackIsZero.getAttributes().getLength());

        // "Compression"
        Node compression = chroma.getNextSibling();
        assertEquals("Compression", compression.getNodeName());
        assertEquals(2, compression.getChildNodes().getLength());

        Node compressionTypeName = compression.getFirstChild();
        assertEquals("CompressionTypeName", compressionTypeName.getNodeName());
        assertEquals("Old JPEG", ((Element) compressionTypeName).getAttribute("value"));

        Node lossless = compressionTypeName.getNextSibling();
        assertEquals("Lossless", lossless.getNodeName());
        assertEquals("FALSE", ((Element) lossless).getAttribute("value"));

        // "Data"
        Node data = compression.getNextSibling();
        assertEquals("Data", data.getNodeName());
        assertEquals(4, data.getChildNodes().getLength());

        Node planarConfiguration = data.getFirstChild();
        assertEquals("PlanarConfiguration", planarConfiguration.getNodeName());
        assertEquals("PixelInterleaved", ((Element) planarConfiguration).getAttribute("value"));

        Node sampleFormat = planarConfiguration.getNextSibling();
        assertEquals("SampleFormat", sampleFormat.getNodeName());
        assertEquals("UnsignedIntegral", ((Element) sampleFormat).getAttribute("value"));

        Node bitsPerSample = sampleFormat.getNextSibling();
        assertEquals("BitsPerSample", bitsPerSample.getNodeName());
        assertEquals("8 8 8", ((Element) bitsPerSample).getAttribute("value"));

        Node sampleMSB = bitsPerSample.getNextSibling();
        assertEquals("SampleMSB", sampleMSB.getNodeName());
        assertEquals("0 0 0", ((Element) sampleMSB).getAttribute("value"));

        // "Dimension"
        Node dimension = data.getNextSibling();
        assertEquals("Dimension", dimension.getNodeName());
        assertEquals(3, dimension.getChildNodes().getLength());

        Node pixelAspectRatio = dimension.getFirstChild();
        assertEquals("PixelAspectRatio", pixelAspectRatio.getNodeName());
        assertEquals("1.0", ((Element) pixelAspectRatio).getAttribute("value"));

        Node horizontalPixelSize = pixelAspectRatio.getNextSibling();
        assertEquals("HorizontalPixelSize", horizontalPixelSize.getNodeName());
        assertEquals("0.254", ((Element) horizontalPixelSize).getAttribute("value"));

        Node verticalPixelSize = horizontalPixelSize.getNextSibling();
        assertEquals("VerticalPixelSize", verticalPixelSize.getNodeName());
        assertEquals("0.254", ((Element) verticalPixelSize).getAttribute("value"));

        // "Document"
        Node document = dimension.getNextSibling();
        assertEquals("Document", document.getNodeName());
        assertEquals(1, document.getChildNodes().getLength());

        Node formatVersion = document.getFirstChild();
        assertEquals("FormatVersion", formatVersion.getNodeName());
        assertEquals("6.0", ((Element) formatVersion).getAttribute("value"));

        // "Text"
        Node text = document.getNextSibling();
        assertEquals("Text", text.getNodeName());
        assertEquals(1, text.getChildNodes().getLength());

        // NOTE: Could be multiple "TextEntry" elements, with different "keyword" attributes
        Node textEntry = text.getFirstChild();
        assertEquals("TextEntry", textEntry.getNodeName());
        assertEquals("Software", ((Element) textEntry).getAttribute("keyword"));
        assertEquals("HP IL v1.1", ((Element) textEntry).getAttribute("value"));
    }

    @Test
    public void testMetadataNativeFormat() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/quad-lzw.tif");
        Node root = metadata.getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME);

        // Root: "com_sun_media_imageio_plugins_tiff_image_1.0"
        assertNotNull(root);
        assertEquals(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, root.getNodeName());
        assertEquals(1, root.getChildNodes().getLength());

        // IFD: "TIFFIFD"
        Node ifd = root.getFirstChild();
        assertEquals("TIFFIFD", ifd.getNodeName());

        NodeList entries = ifd.getChildNodes();
        assertEquals(13, entries.getLength());

        String[] stripOffsets = {
                "8", "150", "292", "434", "576", "718", "860", "1002", "1144", "1286",
                "1793", "3823", "7580", "12225", "17737", "23978", "30534", "36863", "42975", "49180",
                "55361", "61470", "67022", "71646", "74255", "75241", "75411", "75553", "75695", "75837",
                "75979", "76316", "77899", "80466", "84068", "88471", "93623", "99105", "104483", "109663",
                "114969", "120472", "126083", "131289", "135545", "138810", "140808", "141840", "141982", "142124",
                "142266", "142408", "142615", "144074", "146327", "149721", "154066", "158927", "164022", "169217",
                "174409", "179657", "185166", "190684", "196236", "201560", "206064", "209497", "211612", "212419",
                "212561", "212703", "212845", "212987", "213129", "213271", "213413"
        };

        String[] stripByteCounts = {
                "142", "142", "142", "142", "142", "142", "142", "142", "142", "507",
                "2030", "3757", "4645", "5512", "6241", "6556", "6329", "6112", "6205", "6181",
                "6109", "5552", "4624", "2609", "986", "170", "142", "142", "142", "142",
                "337", "1583", "2567", "3602", "4403", "5152", "5482", "5378", "5180", "5306",
                "5503", "5611", "5206", "4256", "3265", "1998", "1032", "142", "142", "142",
                "142", "207", "1459", "2253", "3394", "4345", "4861", "5095", "5195", "5192",
                "5248", "5509", "5518", "5552", "5324", "4504", "3433", "2115", "807", "142",
                "142", "142", "142", "142", "142", "142", "128"
        };

        // The 13 entries
        assertSingleNodeWithValue(entries, TIFF.TAG_IMAGE_WIDTH, TIFF.TYPE_SHORT, "512");
        assertSingleNodeWithValue(entries, TIFF.TAG_IMAGE_HEIGHT, TIFF.TYPE_SHORT, "384");
        assertSingleNodeWithValue(entries, TIFF.TAG_BITS_PER_SAMPLE, TIFF.TYPE_SHORT, "8", "8", "8");
        assertSingleNodeWithValue(entries, TIFF.TAG_COMPRESSION, TIFF.TYPE_SHORT, "5");
        assertSingleNodeWithValue(entries, TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFF.TYPE_SHORT, "2");
        assertSingleNodeWithValue(entries, TIFF.TAG_STRIP_OFFSETS, TIFF.TYPE_LONG, stripOffsets);
        assertSingleNodeWithValue(entries, TIFF.TAG_SAMPLES_PER_PIXEL, TIFF.TYPE_SHORT, "3");
        assertSingleNodeWithValue(entries, TIFF.TAG_ROWS_PER_STRIP, TIFF.TYPE_LONG, "5");
        assertSingleNodeWithValue(entries, TIFF.TAG_STRIP_BYTE_COUNTS, TIFF.TYPE_LONG, stripByteCounts);
        assertSingleNodeWithValue(entries, TIFF.TAG_PLANAR_CONFIGURATION, TIFF.TYPE_SHORT, "1");
        assertSingleNodeWithValue(entries, TIFF.TAG_X_POSITION, TIFF.TYPE_RATIONAL, "0");
        assertSingleNodeWithValue(entries, TIFF.TAG_Y_POSITION, TIFF.TYPE_RATIONAL, "0");
        assertSingleNodeWithValue(entries, 32995, TIFF.TYPE_SHORT, "0"); // Matteing tag, obsoleted by ExtraSamples tag in TIFF 6.0
    }

    @Test
    public void testTreeDetached() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        Node nativeTree = metadata.getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME);
        assertNotNull(nativeTree);

        Node nativeTree2 = metadata.getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME);
        assertNotNull(nativeTree2);

        assertNotSame(nativeTree, nativeTree2);
        assertNodeEquals("Unmodified trees differs", nativeTree, nativeTree2); // Both not modified

        // Modify one of the trees
        Node ifdNode = nativeTree2.getFirstChild();
        ifdNode.removeChild(ifdNode.getFirstChild());
        IIOMetadataNode tiffField = new IIOMetadataNode("TIFFField");
        ifdNode.appendChild(tiffField);

        assertNodeNotEquals("Modified tree does not differ", nativeTree, nativeTree2);
    }

    @Test
    public void testMergeTree() throws IOException {
        TIFFImageMetadata metadata = (TIFFImageMetadata) createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;

        Node nativeTree = metadata.getAsTree(nativeFormat);
        assertNotNull(nativeTree);

        IIOMetadataNode newTree = new IIOMetadataNode("com_sun_media_imageio_plugins_tiff_image_1.0");
        IIOMetadataNode ifdNode = new IIOMetadataNode("TIFFIFD");
        newTree.appendChild(ifdNode);

        createTIFFFieldNode(ifdNode, TIFF.TAG_RESOLUTION_UNIT, TIFF.TYPE_SHORT, TIFFBaseline.RESOLUTION_UNIT_DPI);
        createTIFFFieldNode(ifdNode, TIFF.TAG_X_RESOLUTION, TIFF.TYPE_RATIONAL, new Rational(300));
        createTIFFFieldNode(ifdNode, TIFF.TAG_Y_RESOLUTION, TIFF.TYPE_RATIONAL, new Rational(30001, 100));

        metadata.mergeTree(nativeFormat, newTree);

        Directory ifd = metadata.getIFD();

        assertNotNull(ifd.getEntryById(TIFF.TAG_X_RESOLUTION));
        assertEquals(new Rational(300), ifd.getEntryById(TIFF.TAG_X_RESOLUTION).getValue());
        assertNotNull(ifd.getEntryById(TIFF.TAG_Y_RESOLUTION));
        assertEquals(new Rational(30001, 100), ifd.getEntryById(TIFF.TAG_Y_RESOLUTION).getValue());
        assertNotNull(ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT));
        assertEquals(TIFFBaseline.RESOLUTION_UNIT_DPI, ((Number) ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT).getValue()).intValue());

        Node mergedTree = metadata.getAsTree(nativeFormat);
        NodeList fields = mergedTree.getFirstChild().getChildNodes();

        // Validate there's one and only one resolution unit, x res and y res
        // Validate resolution unit == 1, x res & y res
        assertSingleNodeWithValue(fields, TIFF.TAG_RESOLUTION_UNIT, TIFF.TYPE_SHORT, String.valueOf(TIFFBaseline.RESOLUTION_UNIT_DPI));
        assertSingleNodeWithValue(fields, TIFF.TAG_X_RESOLUTION, TIFF.TYPE_RATIONAL, "300");
        assertSingleNodeWithValue(fields, TIFF.TAG_Y_RESOLUTION, TIFF.TYPE_RATIONAL, "30001/100");
    }

    @Test
    public void testMergeTreeStandardFormat() throws IOException {
        TIFFImageMetadata metadata = (TIFFImageMetadata) createMetadata("/tiff/zackthecat.tif");

        String standardFormat = IIOMetadataFormatImpl.standardMetadataFormatName;

        Node standardTree = metadata.getAsTree(standardFormat);
        assertNotNull(standardTree);

        IIOMetadataNode newTree = new IIOMetadataNode(standardFormat);
        IIOMetadataNode dimensionNode = new IIOMetadataNode("Dimension");
        newTree.appendChild(dimensionNode);

        IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
        dimensionNode.appendChild(horizontalPixelSize);
        horizontalPixelSize.setAttribute("value", String.valueOf(300 / 25.4));

        IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
        dimensionNode.appendChild(verticalPixelSize);
        verticalPixelSize.setAttribute("value", String.valueOf(300 / 25.4));

        metadata.mergeTree(standardFormat, newTree);

        Directory ifd = metadata.getIFD();

        assertNotNull(ifd.getEntryById(TIFF.TAG_X_RESOLUTION));
        assertEquals(new Rational(300), ifd.getEntryById(TIFF.TAG_X_RESOLUTION).getValue());
        assertNotNull(ifd.getEntryById(TIFF.TAG_Y_RESOLUTION));
        assertEquals(new Rational(300), ifd.getEntryById(TIFF.TAG_Y_RESOLUTION).getValue());

        // Should keep DPI as unit
        assertNotNull(ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT));
        assertEquals(TIFFBaseline.RESOLUTION_UNIT_DPI, ((Number) ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT).getValue()).intValue());
    }

    @Test
    public void testMergeTreeStandardFormatAspectOnly() throws IOException {
        TIFFImageMetadata metadata = (TIFFImageMetadata) createMetadata("/tiff/sm_colors_tile.tif");

        String standardFormat = IIOMetadataFormatImpl.standardMetadataFormatName;

        Node standardTree = metadata.getAsTree(standardFormat);
        assertNotNull(standardTree);

        IIOMetadataNode newTree = new IIOMetadataNode(standardFormat);
        IIOMetadataNode dimensionNode = new IIOMetadataNode("Dimension");
        newTree.appendChild(dimensionNode);

        IIOMetadataNode aspectRatio = new IIOMetadataNode("PixelAspectRatio");
        dimensionNode.appendChild(aspectRatio);
        aspectRatio.setAttribute("value", String.valueOf(3f / 2f));

        metadata.mergeTree(standardFormat, newTree);

        Directory ifd = metadata.getIFD();

        assertNotNull(ifd.getEntryById(TIFF.TAG_X_RESOLUTION));
        assertEquals(new Rational(3, 2), ifd.getEntryById(TIFF.TAG_X_RESOLUTION).getValue());
        assertNotNull(ifd.getEntryById(TIFF.TAG_Y_RESOLUTION));
        assertEquals(new Rational(1), ifd.getEntryById(TIFF.TAG_Y_RESOLUTION).getValue());
        assertNotNull(ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT));
        assertEquals(TIFFBaseline.RESOLUTION_UNIT_NONE, ((Number) ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT).getValue()).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeTreeUnsupportedFormat() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = "com_foo_bar_tiff_42";
        metadata.mergeTree(nativeFormat, new IIOMetadataNode(nativeFormat));
    }

    @Test(expected = IIOInvalidTreeException.class)
    public void testMergeTreeFormatMisMatch() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        metadata.mergeTree(nativeFormat, new IIOMetadataNode("com_foo_bar_tiff_42"));
    }

    @Test(expected = IIOInvalidTreeException.class)
    public void testMergeTreeInvalid() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        metadata.mergeTree(nativeFormat, new IIOMetadataNode(nativeFormat)); // Requires at least one child node
    }

    // TODO: Test that failed merge leaves metadata unchanged

    @Test
    public void testSetFromTreeEmpty() throws IOException {
        // Read from file, set empty to see that all is cleared
        TIFFImageMetadata metadata = (TIFFImageMetadata) createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        IIOMetadataNode root = new IIOMetadataNode(nativeFormat);
        root.appendChild(new IIOMetadataNode("TIFFIFD"));

        metadata.setFromTree(nativeFormat, root);

        Directory ifd = metadata.getIFD();
        assertNotNull(ifd);
        assertEquals(0, ifd.size());

        Node tree = metadata.getAsTree(nativeFormat);

        assertNotNull(tree);
        assertNotNull(tree.getFirstChild());
        assertEquals(1, tree.getChildNodes().getLength());
    }

    @Test
    public void testSetFromTree() throws IOException {
        String softwareString = "12M UberTIFF 1.0";

        TIFFImageMetadata metadata = new TIFFImageMetadata(Collections.<Entry>emptySet());

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        IIOMetadataNode root = new IIOMetadataNode(nativeFormat);

        IIOMetadataNode ifdNode = new IIOMetadataNode("TIFFIFD");
        root.appendChild(ifdNode);

        createTIFFFieldNode(ifdNode, TIFF.TAG_SOFTWARE, TIFF.TYPE_ASCII, softwareString);

        metadata.setFromTree(nativeFormat, root);

        Directory ifd = metadata.getIFD();
        assertNotNull(ifd);
        assertEquals(1, ifd.size());

        assertNotNull(ifd.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals(softwareString, ifd.getEntryById(TIFF.TAG_SOFTWARE).getValue());

        Node tree = metadata.getAsTree(nativeFormat);

        assertNotNull(tree);
        assertNotNull(tree.getFirstChild());
        assertEquals(1, tree.getChildNodes().getLength());
    }

    @Test
    public void testSetFromTreeStandardFormat() throws IOException {
        String softwareString = "12M UberTIFF 1.0";
        String copyrightString = "Copyright (C) TwelveMonkeys, 2015";

        TIFFImageMetadata metadata = new TIFFImageMetadata(Collections.<Entry>emptySet());

        String standardFormat = IIOMetadataFormatImpl.standardMetadataFormatName;
        IIOMetadataNode root = new IIOMetadataNode(standardFormat);

        IIOMetadataNode textNode = new IIOMetadataNode("Text");
        root.appendChild(textNode);

        IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
        textNode.appendChild(textEntry);

        textEntry.setAttribute("keyword", "SOFTWARE"); // Spelling should not matter
        textEntry.setAttribute("value", softwareString);

        textEntry = new IIOMetadataNode("TextEntry");
        textNode.appendChild(textEntry);

        textEntry.setAttribute("keyword", "copyright"); // Spelling should not matter
        textEntry.setAttribute("value", copyrightString);

        metadata.setFromTree(standardFormat, root);

        Directory ifd = metadata.getIFD();
        assertNotNull(ifd);
        assertEquals(2, ifd.size());

        assertNotNull(ifd.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals(softwareString, ifd.getEntryById(TIFF.TAG_SOFTWARE).getValue());

        assertNotNull(ifd.getEntryById(TIFF.TAG_COPYRIGHT));
        assertEquals(copyrightString, ifd.getEntryById(TIFF.TAG_COPYRIGHT).getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetFromTreeUnsupportedFormat() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = "com_foo_bar_tiff_42";
        metadata.setFromTree(nativeFormat, new IIOMetadataNode(nativeFormat));
    }

    @Test(expected = IIOInvalidTreeException.class)
    public void testSetFromTreeFormatMisMatch() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        metadata.setFromTree(nativeFormat, new IIOMetadataNode("com_foo_bar_tiff_42"));
    }

    @Test(expected = IIOInvalidTreeException.class)
    public void testSetFromTreeInvalid() throws IOException {
        IIOMetadata metadata = createMetadata("/tiff/sm_colors_tile.tif");

        String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
        metadata.setFromTree(nativeFormat, new IIOMetadataNode(nativeFormat)); // Requires at least one child node
    }

    @Test
    public void testStandardChromaSamplesPerPixel() {
        Set<Entry> entries = new HashSet<>();
        entries.add(new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFFBaseline.PHOTOMETRIC_RGB));
        entries.add(new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, 4));
        entries.add(new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, new int[] {8, 8, 8})); // This is incorrect, just making sure the correct value is selected

        IIOMetadataNode chromaNode = new TIFFImageMetadata(entries).getStandardChromaNode();
        assertNotNull(chromaNode);

        IIOMetadataNode numChannels = (IIOMetadataNode) chromaNode.getElementsByTagName("NumChannels").item(0);
        assertEquals("4", numChannels.getAttribute("value"));
    }

    @Test
    public void testStandardChromaSamplesPerPixelFallbackBitsPerSample() {
        Set<Entry> entries = new HashSet<>();
        entries.add(new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFFBaseline.PHOTOMETRIC_RGB));
        entries.add(new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, new int[] {8, 8, 8}));

        IIOMetadataNode chromaNode = new TIFFImageMetadata(entries).getStandardChromaNode();
        assertNotNull(chromaNode);

        IIOMetadataNode numChannels = (IIOMetadataNode) chromaNode.getElementsByTagName("NumChannels").item(0);
        assertEquals("3", numChannels.getAttribute("value"));
    }

    @Test
    public void testStandardChromaSamplesPerPixelFallbackDefault() {
        Set<Entry> entries = new HashSet<>();
        entries.add(new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO));

        IIOMetadataNode chromaNode = new TIFFImageMetadata(entries).getStandardChromaNode();
        assertNotNull(chromaNode);
        IIOMetadataNode numChannels = (IIOMetadataNode) chromaNode.getElementsByTagName("NumChannels").item(0);
        assertEquals("1", numChannels.getAttribute("value"));
    }

    @Test
    public void testStandardDataBitsPerSampleFallbackDefault() {
        Set<Entry> entries = new HashSet<>();
        entries.add(new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO));

        IIOMetadataNode dataNode = new TIFFImageMetadata(entries).getStandardDataNode();
        assertNotNull(dataNode);
        IIOMetadataNode numChannels = (IIOMetadataNode) dataNode.getElementsByTagName("BitsPerSample").item(0);
        assertEquals("1", numChannels.getAttribute("value"));
    }

    @Test
    public void testStandardNodeSamplesPerPixelFallbackDefault() {
        Set<Entry> entries = new HashSet<>();
        entries.add(new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFFBaseline.PHOTOMETRIC_RGB));

        // Just to make sure we haven't accidentally missed something
        IIOMetadataNode standardTree = (IIOMetadataNode) new TIFFImageMetadata(entries).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        assertNotNull(standardTree);
    }

    // TODO: Test that failed set leaves metadata unchanged

    private void assertSingleNodeWithValue(final NodeList fields, final int tag, int type, final String... expectedValue) {
        String tagNumber = String.valueOf(tag);
        String typeName = StringUtil.capitalize(TIFF.TYPE_NAMES[type].toLowerCase());

        boolean foundTag = false;

        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);

            if (tagNumber.equals(field.getAttribute("number"))) {
                assertFalse("Duplicate tag " + tagNumber + " found", foundTag);

                assertEquals(1, field.getChildNodes().getLength());
                Node containerNode = field.getFirstChild();
                assertEquals("TIFF" + typeName + "s", containerNode.getNodeName());

                NodeList valueNodes = containerNode.getChildNodes();
                assertEquals("Unexpected number of values for tag " + tagNumber, expectedValue.length, valueNodes.getLength());

                for (int j = 0; j < expectedValue.length; j++) {
                    Element valueNode = (Element) valueNodes.item(j);
                    assertEquals("TIFF" + typeName, valueNode.getNodeName());
                    assertEquals("Unexpected tag " + tagNumber + " value", expectedValue[j], valueNode.getAttribute("value"));
                }

                foundTag = true;
            }
        }

        assertTrue("No tag " + tagNumber + " found", foundTag);
    }

    static void createTIFFFieldNode(final IIOMetadataNode parentIFDNode, int tag, short type, Object value) {
        IIOMetadataNode fieldNode = new IIOMetadataNode("TIFFField");
        parentIFDNode.appendChild(fieldNode);

        fieldNode.setAttribute("number", String.valueOf(tag));

        switch (type) {
            case TIFF.TYPE_ASCII:
                createTIFFFieldContainerNode(fieldNode, "Ascii", value);
                break;
            case TIFF.TYPE_BYTE:
                createTIFFFieldContainerNode(fieldNode, "Byte", value);
                break;
            case TIFF.TYPE_SHORT:
                createTIFFFieldContainerNode(fieldNode, "Short", value);
                break;
            case TIFF.TYPE_RATIONAL:
                createTIFFFieldContainerNode(fieldNode, "Rational", value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    static void createTIFFFieldContainerNode(final IIOMetadataNode fieldNode, final String type, final Object value) {
        IIOMetadataNode containerNode = new IIOMetadataNode("TIFF" + type + "s");
        fieldNode.appendChild(containerNode);

        IIOMetadataNode valueNode = new IIOMetadataNode("TIFF" + type);
        valueNode.setAttribute("value", String.valueOf(value));
        containerNode.appendChild(valueNode);
    }

    private void assertNodeNotEquals(final String message, final Node expected, final Node actual) {
        // Lame, lazy implementation...
        try {
            assertNodeEquals(message, expected, actual);
        }
        catch (AssertionError ignore) {
            return;
        }

        fail(message);
    }

    private void assertNodeEquals(final String message, final Node expected, final Node actual) {
        assertEquals(message + " class differs", expected.getClass(), actual.getClass());
        assertEquals(message, expected.getNodeValue(), actual.getNodeValue());

        if (expected instanceof IIOMetadataNode) {
            IIOMetadataNode expectedIIO = (IIOMetadataNode) expected;
            IIOMetadataNode actualIIO = (IIOMetadataNode) actual;

            assertEquals(message, expectedIIO.getUserObject(), actualIIO.getUserObject());
        }

        NodeList expectedChildNodes = expected.getChildNodes();
        NodeList actualChildNodes = actual.getChildNodes();

        assertEquals(message + " child length differs: " + toString(expectedChildNodes) + " != " + toString(actualChildNodes),
                expectedChildNodes.getLength(), actualChildNodes.getLength());

        for (int i = 0; i < expectedChildNodes.getLength(); i++) {
            Node expectedChild = expectedChildNodes.item(i);
            Node actualChild = actualChildNodes.item(i);

            assertEquals(message + " node name differs", expectedChild.getLocalName(), actualChild.getLocalName());
            assertNodeEquals(message + "/" + expectedChild.getLocalName(), expectedChild, actualChild);
        }
    }

    private String toString(final NodeList list) {
        if (list.getLength() == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < list.getLength(); i++) {
            if (i > 0) {
                builder.append(", ");
            }

            Node node = list.item(i);
            builder.append(node.getLocalName());
        }
        builder.append("]");

        return builder.toString();
    }
}
