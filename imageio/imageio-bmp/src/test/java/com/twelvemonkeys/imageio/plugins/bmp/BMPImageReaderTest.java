package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * BMPImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BMPImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class BMPImageReaderTest extends ImageReaderAbstractTest<BMPImageReader> {
    protected List<TestData> getTestData() {
        return Arrays.asList(
                // BMP Suite "Good"
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1bg.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal1wb.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal4.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal4rle.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8-0.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8nonsquare.bmp"), new Dimension(127, 32)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8os2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8rle.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8topdown.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8v4.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8v5.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w124.bmp"), new Dimension(124, 61)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w125.bmp"), new Dimension(125, 62)),
                new TestData(getClassLoaderResource("/bmpsuite/g/pal8w126.bmp"), new Dimension(126, 63)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16-565.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16-565pal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb16.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb24.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb24pal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb32.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/g/rgb32bf.bmp"), new Dimension(127, 64)),

                // BMP Suite "Questionable"
                new TestData(getClassLoaderResource("/bmpsuite/q/pal1p1.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal4rletrns.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8offs.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2sp.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2v2.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8os2v2-16.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8oversizepal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/pal8rletrns.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb16-231.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba16-4444.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24jpeg.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24largepal.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24lprof.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24png.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb24prof.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb32-111110.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgb32fakealpha.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba32abf.bmp"), new Dimension(127, 64)),
                new TestData(getClassLoaderResource("/bmpsuite/q/rgba32.bmp"), new Dimension(127, 64)),

                // OS/2 samples
                new TestData(getClassLoaderResource("/os2/money-2-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-16-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-256-(os2).bmp"), new Dimension(455, 341)),
                new TestData(getClassLoaderResource("/os2/money-24bit-os2.bmp"), new Dimension(455, 341)),

                // Various other samples
                new TestData(getClassLoaderResource("/bmp/Blue Lace 16.bmp"), new Dimension(48, 48)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_mono.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8-IM.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_gray.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask444.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask555.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask565.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_24.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888_reversed.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/24bitpalette.bmp"), new Dimension(320, 200))
        );
    }

    protected ImageReaderSpi createProvider() {
        return new BMPImageReaderSpi();
    }

    @Override
    protected BMPImageReader createReader() {
        return new BMPImageReader(createProvider());
    }

    protected Class<BMPImageReader> getReaderClass() {
        return BMPImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Collections.singletonList("bmp");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("bmp", "rle");
    }

    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/bmp");
    }

    @Override
    @Test
    public void testGetTypeSpecifiers() throws IOException {
        final ImageReader reader = createReader();
        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            ImageTypeSpecifier rawType = reader.getRawImageType(0);

            // As the JPEGImageReader we delegate to returns null for YCbCr, we'll have to do the same
            if (rawType == null && data.getInput().toString().contains("jpeg")) {
                continue;
            }
            assertNotNull(rawType);

            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertNotNull(types);
            assertTrue(types.hasNext());

            // TODO: This might fail even though the specifiers are obviously equal, if the
            // color spaces they use are not the SAME instance, as ColorSpace uses identity equals
            // and Interleaved ImageTypeSpecifiers are only equal if color spaces are equal...
            boolean rawFound = false;
            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();
                if (type.equals(rawType)) {
                    rawFound = true;
                    break;
                }
            }

            assertTrue("ImageTypeSepcifier from getRawImageType should be in the iterator from getImageTypes", rawFound);
        }
    }

    @Ignore("Known issue: Subsampled reading is currently broken")
    @Test
    public void testReadWithSubsampleParamPixelsIndexed8() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);
    }

    // TODO: 1. Subsampling is currently broken, should fix it.
    //       2. BMPs are (normally) stored bottom/up, meaning y subsampling offsets will differ from normal
    //          subsampling of the same data with an offset... Should we deal with this in the reader? Yes?
    @Ignore("Known issue: Subsampled reading is currently broken")
    @Test
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(19); // RGB 24
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);
    }

    @Test(expected = IIOException.class)
    public void testReadCorruptCausesIIOException() throws IOException {
        // See https://bugs.openjdk.java.net/browse/JDK-8066904
        // NullPointerException when calling ImageIO.read(InputStream) with corrupt BMP
        BMPImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/broken-bmp/corrupted-bmp.bmp")));
            reader.read(0);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testAddIIOReadProgressListenerCallbacksJPEG() {
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/bmpsuite/q/rgb24jpeg.bmp"), new Dimension(127, 64));
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener);
        ordered.verify(listener).imageStarted(reader, 0);
        ordered.verify(listener, atLeastOnce()).imageProgress(eq(reader), anyInt());
        ordered.verify(listener).imageComplete(reader);
    }

    @Test
    public void testAddIIOReadProgressListenerCallbacksPNG() {
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/bmpsuite/q/rgb24png.bmp"), new Dimension(127, 64));
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener);
        ordered.verify(listener).imageStarted(reader, 0);
        ordered.verify(listener, atLeastOnce()).imageProgress(eq(reader), anyInt());
        ordered.verify(listener).imageComplete(reader);
    }

    @Test
    public void testMetadataEqualsJRE() throws IOException, URISyntaxException {
        ImageReader jreReader;
        try {
            @SuppressWarnings("unchecked")
            Class<ImageReader> jreReaderClass = (Class<ImageReader>) Class.forName("com.sun.imageio.plugins.bmp.BMPImageReader");
            Constructor<ImageReader> constructor = jreReaderClass.getConstructor(ImageReaderSpi.class);
            jreReader = constructor.newInstance(new Object[] {null});
        }
        catch (Exception e) {
            e.printStackTrace();
            // Ignore this test if not on an Oracle JRE (com.sun...BMPImageReader not available)
            assumeNoException(e);
            return;
        }

        ImageReader reader = createReader();

        for (TestData data : getTestData()) {
            if (data.getInput().toString().contains("pal8offs")) {
                continue;
            }

            reader.setInput(data.getInputStream());
            jreReader.setInput(data.getInputStream());

            IIOMetadata metadata = reader.getImageMetadata(0);

            // WORKAROUND: JRE reader does not reset metadata on setInput. Invoking getWidth forces re-read of header and metadata.
            try {
                jreReader.getWidth(0);
            }
            catch (Exception e) {
                System.err.println("WARNING: Reading " + data + " caused exception: " + e.getMessage());
                continue;
            }
            IIOMetadata jreMetadata = jreReader.getImageMetadata(0);

            assertEquals(true, metadata.isStandardMetadataFormatSupported());
            assertEquals(jreMetadata.getNativeMetadataFormatName(), metadata.getNativeMetadataFormatName());
            assertArrayEquals(jreMetadata.getExtraMetadataFormatNames(), metadata.getExtraMetadataFormatNames());

            // TODO: Allow our standard metadata to be richer, but contain at least the information from the JRE impl

            for (String format : jreMetadata.getMetadataFormatNames()) {
                String absolutePath = data.toString();
                String localPath = absolutePath.substring(absolutePath.lastIndexOf("test-classes") + 12);

                Node expectedTree = jreMetadata.getAsTree(format);
                Node actualTree = metadata.getAsTree(format);

//                try {
                    assertNodeEquals(localPath + " - " + format, expectedTree, actualTree);
//                }
//                catch (AssertionError e) {
//                    ByteArrayOutputStream expected = new ByteArrayOutputStream();
//                    ByteArrayOutputStream actual = new ByteArrayOutputStream();
//
//                    new XMLSerializer(expected, "UTF-8").serialize(expectedTree, false);
//                    new XMLSerializer(actual, "UTF-8").serialize(actualTree, false);
//
//                    assertEquals(e.getMessage(), new String(expected.toByteArray(), "UTF-8"), new String(actual.toByteArray(), "UTF-8"));
//
//                    throw e;
//                }
            }
        }
    }

    private void assertNodeEquals(final String message, final Node expected, final Node actual) {
        assertEquals(message + " class differs", expected.getClass(), actual.getClass());

        if (!excludeEqualValueTest(expected)) {
            assertEquals(message, expected.getNodeValue(), actual.getNodeValue());

            if (expected instanceof IIOMetadataNode) {
                IIOMetadataNode expectedIIO = (IIOMetadataNode) expected;
                IIOMetadataNode actualIIO = (IIOMetadataNode) actual;

                assertEquals(message, expectedIIO.getUserObject(), actualIIO.getUserObject());
            }
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

    private boolean excludeEqualValueTest(final Node expected) {
        if (expected.getLocalName().equals("ImageSize")) {
            // JRE metadata returns 0, even if known in reader...
            return true;
        }
        if (expected.getLocalName().equals("ColorsImportant")) {
            // JRE metadata returns 0, even if known in reader...
            return true;
        }
        if (expected.getParentNode() != null && expected.getParentNode().getLocalName().equals("PaletteEntry") && !expected.getNodeValue().equals("Green")) {
            // JRE metadata returns RGB colors in BGR order
            // JRE metadata returns 0 for alpha, when -1 (0xff) is at least just as correct (why contain alpha at all?)
            return true;
        }
        if (expected.getLocalName().equals("Height") && expected.getNodeValue().startsWith("-")) {
            // JRE metadata returns negative height for bottom/up images
            // TODO: Decide if we should do the same, as there is no "orientation" or flag for bottom/up
            return true;
        }

        return false;
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