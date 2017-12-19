/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTestCase;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.io.NullOutputStream;
import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadataTest.createTIFFFieldNode;
import static com.twelvemonkeys.imageio.util.ImageReaderAbstractTest.assertRGBEquals;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Mockito.*;

/**
 * TIFFImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriterTest.java,v 1.0 19.09.13 13:22 haraldk Exp$
 */
public class TIFFImageWriterTest extends ImageWriterAbstractTestCase {

    private static final TIFFImageWriterSpi PROVIDER = new TIFFImageWriterSpi();

    @Override
    protected ImageWriter createImageWriter() {
        return new TIFFImageWriter(PROVIDER);
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(300, 200, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(300, 200, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(300, 200, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(300, 200, BufferedImage.TYPE_USHORT_GRAY),
                new BufferedImage(300, 200, BufferedImage.TYPE_BYTE_BINARY),
                new BufferedImage(300, 200, BufferedImage.TYPE_BYTE_INDEXED)
        );
    }

    // TODO: Test write bilevel stays bilevel
    // TODO: Test write indexed stays indexed

    @Test
    public void testWriteWithCustomResolutionNative() throws IOException {
        // Issue 139 Writing TIFF files with custom resolution value
        Rational resolutionValue = new Rational(1200);
        int resolutionUnitValue = TIFFBaseline.RESOLUTION_UNIT_CENTIMETER;

        RenderedImage image = getTestData(0);

        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(stream);

            String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
            IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);

            IIOMetadataNode customMeta = new IIOMetadataNode(nativeFormat);

            IIOMetadataNode ifd = new IIOMetadataNode("TIFFIFD");
            customMeta.appendChild(ifd);

            createTIFFFieldNode(ifd, TIFF.TAG_RESOLUTION_UNIT, TIFF.TYPE_SHORT, resolutionUnitValue);
            createTIFFFieldNode(ifd, TIFF.TAG_X_RESOLUTION, TIFF.TYPE_RATIONAL, resolutionValue);
            createTIFFFieldNode(ifd, TIFF.TAG_Y_RESOLUTION, TIFF.TYPE_RATIONAL, resolutionValue);

            metadata.mergeTree(nativeFormat, customMeta);

            writer.write(null, new IIOImage(image, null, metadata), null);
        }
        catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue("No image data written", buffer.size() > 0);

        Directory ifds = new TIFFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));

        Entry resolutionUnit = ifds.getEntryById(TIFF.TAG_RESOLUTION_UNIT);
        assertNotNull(resolutionUnit);
        assertEquals(resolutionUnitValue, ((Number) resolutionUnit.getValue()).intValue());

        Entry xResolution = ifds.getEntryById(TIFF.TAG_X_RESOLUTION);
        assertNotNull(xResolution);
        assertEquals(resolutionValue, xResolution.getValue());

        Entry yResolution = ifds.getEntryById(TIFF.TAG_Y_RESOLUTION);
        assertNotNull(yResolution);
        assertEquals(resolutionValue, yResolution.getValue());
    }

    @Test
    public void testWriteWithCustomSoftwareNative() throws IOException {
        String softwareString = "12M TIFF Test 1.0 (build $foo$)";

        RenderedImage image = getTestData(0);

        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(stream);

            String nativeFormat = TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
            IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);

            IIOMetadataNode customMeta = new IIOMetadataNode(nativeFormat);

            IIOMetadataNode ifd = new IIOMetadataNode("TIFFIFD");
            customMeta.appendChild(ifd);

            createTIFFFieldNode(ifd, TIFF.TAG_SOFTWARE, TIFF.TYPE_ASCII, softwareString);

            metadata.mergeTree(nativeFormat, customMeta);

            writer.write(null, new IIOImage(image, null, metadata), null);
        }
        catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue("No image data written", buffer.size() > 0);

        Directory ifds = new TIFFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));
        Entry software = ifds.getEntryById(TIFF.TAG_SOFTWARE);
        assertNotNull(software);
        assertEquals(softwareString, software.getValueAsString());
    }

    @Test
    public void testWriteWithCustomResolutionStandard() throws IOException {
        // Issue 139 Writing TIFF files with custom resolution value
        double resolutionValue = 300 / 25.4; // 300 dpi, 1 inch = 2.54 cm or 25.4 mm
        int resolutionUnitValue = TIFFBaseline.RESOLUTION_UNIT_CENTIMETER;
        Rational expectedResolutionValue = new Rational(Math.round(resolutionValue * 10 * TIFFImageMetadata.RATIONAL_SCALE_FACTOR), TIFFImageMetadata.RATIONAL_SCALE_FACTOR);

        RenderedImage image = getTestData(0);

        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(stream);

            String standardFormat = IIOMetadataFormatImpl.standardMetadataFormatName;
            IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);

            IIOMetadataNode customMeta = new IIOMetadataNode(standardFormat);

            IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
            customMeta.appendChild(dimension);

            IIOMetadataNode xSize = new IIOMetadataNode("HorizontalPixelSize");
            dimension.appendChild(xSize);
            xSize.setAttribute("value", String.valueOf(resolutionValue));

            IIOMetadataNode ySize = new IIOMetadataNode("VerticalPixelSize");
            dimension.appendChild(ySize);
            ySize.setAttribute("value", String.valueOf(resolutionValue));

            metadata.mergeTree(standardFormat, customMeta);

            writer.write(null, new IIOImage(image, null, metadata), null);
        }
        catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue("No image data written", buffer.size() > 0);

        Directory ifds = new TIFFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));

        Entry resolutionUnit = ifds.getEntryById(TIFF.TAG_RESOLUTION_UNIT);
        assertNotNull(resolutionUnit);
        assertEquals(resolutionUnitValue, ((Number) resolutionUnit.getValue()).intValue());

        Entry xResolution = ifds.getEntryById(TIFF.TAG_X_RESOLUTION);
        assertNotNull(xResolution);
        assertEquals(expectedResolutionValue, xResolution.getValue());

        Entry yResolution = ifds.getEntryById(TIFF.TAG_Y_RESOLUTION);
        assertNotNull(yResolution);
        assertEquals(expectedResolutionValue, yResolution.getValue());
    }

    @Test
    public void testWriteWithCustomSoftwareStandard() throws IOException {
        String softwareString = "12M TIFF Test 1.0 (build $foo$)";

        RenderedImage image = getTestData(0);

        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(stream);

            String standardFormat = IIOMetadataFormatImpl.standardMetadataFormatName;
            IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);

            IIOMetadataNode customMeta = new IIOMetadataNode(standardFormat);

            IIOMetadataNode dimension = new IIOMetadataNode("Text");
            customMeta.appendChild(dimension);

            IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
            dimension.appendChild(textEntry);
            textEntry.setAttribute("keyword", "Software");
            textEntry.setAttribute("value", softwareString);

            metadata.mergeTree(standardFormat, customMeta);

            writer.write(null, new IIOImage(image, null, metadata), null);
        }
        catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue("No image data written", buffer.size() > 0);

        Directory ifds = new TIFFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));
        Entry software = ifds.getEntryById(TIFF.TAG_SOFTWARE);
        assertNotNull(software);
        assertEquals(softwareString, software.getValueAsString());
    }

    @Test
    public void testWriterCanWriteSequence() {
        ImageWriter writer = createImageWriter();
        assertTrue("Writer should support sequence writing", writer.canWriteSequence());
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteSequenceWithoutPrepare() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(output);
            writer.writeToSequence(new IIOImage(new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR), null, null), null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testEndSequenceWithoutPrepare() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(output);
            writer.endWriteSequence();
        }
    }

    @Test
    public void testWriteSequence() throws IOException {
        BufferedImage[] images = new BufferedImage[] {
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(110, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(120, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(130, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(140, 100, BufferedImage.TYPE_BYTE_BINARY)
        };

        Color[] colors = new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.WHITE};

        for (int i = 0; i < images.length; i++) {
            BufferedImage image = images[i];
            Graphics2D g2d = image.createGraphics();
            try {
                g2d.setColor(colors[i]);
                g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
            finally {
                g2d.dispose();
            }
        }

        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(output);

            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            try {
                writer.prepareWriteSequence(null);

                params.setCompressionType("LZW");
                writer.writeToSequence(new IIOImage(images[0], null, null), params);

                params.setCompressionType("None");
                writer.writeToSequence(new IIOImage(images[1], null, null), params);

                params.setCompressionType("JPEG");
                writer.writeToSequence(new IIOImage(images[2], null, null), params);

                params.setCompressionType("PackBits");
                writer.writeToSequence(new IIOImage(images[3], null, null), params);

                params.setCompressionType("CCITT T.6");
                writer.writeToSequence(new IIOImage(images[4], null, null), params);

                writer.endWriteSequence();
            }
            catch (IOException e) {
                fail(e.getMessage());
            }
        }

        FileUtil.write(new File("/Downloads/multi-foo.tiff"), buffer.toByteArray());


        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            assertEquals("wrong image count", images.length, reader.getNumImages(true));

            for (int i = 0; i < reader.getNumImages(true); i++) {
                BufferedImage image = reader.read(i);

                assertEquals(images[i].getWidth(), image.getWidth());
                assertEquals(images[i].getHeight(), image.getHeight());

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        assertRGBEquals("RGB differ for image " + i + " (" + x + "," + y + ")", images[i].getRGB(x, y), image.getRGB(x, y), 5); // Allow room for JPEG compression
                    }
                }
            }
        }
    }

    @Test
    public void testWriteSequenceProgress() throws IOException {
        BufferedImage[] images = new BufferedImage[] {
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(110, 100, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(120, 100, BufferedImage.TYPE_INT_RGB)
        };

        ImageWriter writer = createImageWriter();
        IIOWriteProgressListener progress = mock(IIOWriteProgressListener.class, "progress");
        writer.addIIOWriteProgressListener(progress);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(new NullOutputStream())) {
            writer.setOutput(output);

            try {
                writer.prepareWriteSequence(null);

                for (int i = 0; i < images.length; i++) {
                    reset(progress);

                    ImageWriteParam param = writer.getDefaultWriteParam();

                    if (i == images.length - 1) {
                        // Make sure that the JPEG delegation outputs the correct indexes
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionType("JPEG");
                    }

                    writer.writeToSequence(new IIOImage(images[i], null, null), param);

                    verify(progress, times(1)).imageStarted(writer, i);
                    verify(progress, atLeastOnce()).imageProgress(eq(writer), anyFloat());
                    verify(progress, times(1)).imageComplete(writer);
                }

                writer.endWriteSequence();
            }
            catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testWriteParamJPEGQuality() throws IOException {
        ImageWriter writer = createImageWriter();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(new NullOutputStream())) {
            writer.setOutput(output);

            try {
                ImageWriteParam param = writer.getDefaultWriteParam();
                // Make sure that the JPEG delegation outputs the correct indexes
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType("JPEG");
                param.setCompressionQuality(.1f);

                writer.write(null, new IIOImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), null, null), param);

                // In a perfect world, we should verify that the parameter was passed to the JPEG delegate...
            }
            catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testReadWriteRead1BitLZW() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClass().getResource("/tiff/a33.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            writer.write(original);
            writer.dispose();
        }

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode compression = (IIOMetadataNode) tree.getElementsByTagName("CompressionTypeName").item(0);
            assertEquals("LZW", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertEquals("IrfanView", textEntry.getAttribute("value"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testReadWriteRead1BitDeflate() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/a33.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("Deflate");

            writer.write(null, original, param);
            writer.dispose();
        }

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode compression = (IIOMetadataNode) tree.getElementsByTagName("CompressionTypeName").item(0);
            assertEquals("Deflate", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertEquals("IrfanView", textEntry.getAttribute("value"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testReadWriteRead1BitNone() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/a33.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("None");

            writer.write(null, original, param);
            writer.dispose();
        }

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            NodeList compressions = tree.getElementsByTagName("CompressionTypeName");
            IIOMetadataNode compression = (IIOMetadataNode) compressions.item(0);
            assertEquals("None", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertEquals("IrfanView", textEntry.getAttribute("value"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testReadWriteRead24BitLZW() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/quad-lzw.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            writer.write(original);
            writer.dispose();
        }

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode compression = (IIOMetadataNode) tree.getElementsByTagName("CompressionTypeName").item(0);
            assertEquals("LZW", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertTrue(textEntry.getAttribute("value").startsWith("TwelveMonkeys ImageIO TIFF"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testReadWriteRead24BitDeflate() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/quad-lzw.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("Deflate");

            writer.write(null, original, param);
            writer.dispose();
        }

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode compression = (IIOMetadataNode) tree.getElementsByTagName("CompressionTypeName").item(0);
            assertEquals("Deflate", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertTrue(textEntry.getAttribute("value").startsWith("TwelveMonkeys ImageIO TIFF"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testReadWriteRead24BitNone() throws IOException {
        // Read original LZW compressed TIFF
        IIOImage original;

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/quad-lzw.tif"))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);

            original = reader.readAll(0, null);
            reader.dispose();
        }

        assumeNotNull(original);

        // Write it back, using same compression (copied from metadata)
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            ImageWriter writer = createImageWriter();
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("None");

            writer.write(null, original, param);
            writer.dispose();
        }

//        Path tempFile = Files.createTempFile("test-", ".tif");
//        Files.write(tempFile, buffer.toByteArray());
//        System.out.println("open " + tempFile.toAbsolutePath());

        // Try re-reading the same TIFF
        try (ImageInputStream input = ImageIO.createImageInputStream(buffer.createInputStream())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            BufferedImage orig = (BufferedImage) original.getRenderedImage();

            int maxH = Math.min(300, image.getHeight());
            for (int y = 0; y < maxH; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertRGBEquals("Pixel differ: ", orig.getRGB(x, y), image.getRGB(x, y), 0);
                }
            }

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode compression = (IIOMetadataNode) tree.getElementsByTagName("CompressionTypeName").item(0);
            assertEquals("None", compression.getAttribute("value"));

            boolean softwareFound = false;
            NodeList textEntries = tree.getElementsByTagName("TextEntry");
            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                if ("Software".equals(textEntry.getAttribute("keyword"))) {
                    softwareFound = true;
                    assertTrue(textEntry.getAttribute("value").startsWith("TwelveMonkeys ImageIO TIFF"));
                }
            }

            assertTrue("Software metadata not found", softwareFound);
        }
    }

    @Test
    public void testWriteCropped() throws IOException {
        List<URL> testData = Arrays.asList(
                getClassLoaderResource("/tiff/quad-lzw.tif"),
                getClassLoaderResource("/tiff/grayscale-alpha.tiff"),
                getClassLoaderResource("/tiff/ccitt/group3_1d.tif"),
                getClassLoaderResource("/tiff/depth/flower-palette-02.tif"),
                getClassLoaderResource("/tiff/depth/flower-palette-04.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-16.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-32.tif")
        );

        for (URL resource : testData) {
            // Read it
            BufferedImage original = ImageIO.read(resource);

            // Crop it
            BufferedImage subimage = original.getSubimage(original.getWidth() / 4, original.getHeight() / 4, original.getWidth() / 2, original.getHeight() / 2);

            // Store cropped
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                ImageWriter imageWriter = createImageWriter();
                imageWriter.setOutput(output);
                imageWriter.write(subimage);
            }

            // Re-read cropped
            BufferedImage cropped = ImageIO.read(new ByteArrayImageInputStream(bytes.toByteArray()));

            // Compare
            assertImageEquals(String.format("Cropped output differs: %s", resource.getFile()), subimage, cropped, 0);
        }
    }

    private void assertImageEquals(final String message, final BufferedImage expected, final BufferedImage actual, final int tolerance) {
        assertNotNull(message, expected);
        assertNotNull(message, actual);
        assertEquals(message + ", widths differ", expected.getWidth(), actual.getWidth());
        assertEquals(message + ", heights differ", expected.getHeight(), actual.getHeight());

        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertRGBEquals(String.format("%s, ARGB differs at (%s,%s)", message, x, y), expected.getRGB(x, y), actual.getRGB(x, y), tolerance);
            }
        }
    }

    @Test
    public void testWriteStreamMetadataDefaultMM() throws IOException {
        ImageWriter writer = createImageWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN); // Should pass through
            writer.setOutput(stream);

            writer.write(null, new IIOImage(getTestData(0), null, null), null);
        }

        byte[] bytes = output.toByteArray();
        assertArrayEquals(new byte[] {'M', 'M', 0, 42}, Arrays.copyOf(bytes, 4));
    }

    @Test
    public void testWriteStreamMetadataDefaultII() throws IOException {
        ImageWriter writer = createImageWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN); // Should pass through
            writer.setOutput(stream);

            writer.write(null, new IIOImage(getTestData(0), null, null), null);
        }

        byte[] bytes = output.toByteArray();
        assertArrayEquals(new byte[] {'I', 'I', 42, 0}, Arrays.copyOf(bytes, 4));
    }

    @Test
    public void testWriteStreamMetadataMM() throws IOException {
        ImageWriter writer = createImageWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN); // Should be overridden by stream metadata
            writer.setOutput(stream);

            writer.write(new TIFFStreamMetadata(ByteOrder.BIG_ENDIAN), new IIOImage(getTestData(0), null, null), null);
        }

        byte[] bytes = output.toByteArray();
        assertArrayEquals(new byte[] {'M', 'M', 0, 42}, Arrays.copyOf(bytes, 4));
    }

    @Test
    public void testWriteStreamMetadataII() throws IOException {
        ImageWriter writer = createImageWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN); // Should be overridden by stream metadata
            writer.setOutput(stream);

            writer.write(new TIFFStreamMetadata(ByteOrder.LITTLE_ENDIAN), new IIOImage(getTestData(0), null, null), null);
        }

        byte[] bytes = output.toByteArray();
        assertArrayEquals(new byte[] {'I', 'I', 42, 0}, Arrays.copyOf(bytes, 4));
    }

    @Test
    public void testRewrite() throws IOException {
        ImageWriter writer = createImageWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        List<URL> testData = Arrays.asList(
                getClassLoaderResource("/tiff/pixtiff/17-tiff-binary-ccitt-group3.tif"),
                getClassLoaderResource("/tiff/pixtiff/36-tiff-8-bit-gray-jpeg.tif"),
                getClassLoaderResource("/tiff/pixtiff/51-tiff-24-bit-color-jpeg.tif"),
                getClassLoaderResource("/tiff/pixtiff/58-plexustiff-binary-ccitt-group4.tif"),
                getClassLoaderResource("/tiff/balloons.tif"),
                getClassLoaderResource("/tiff/ColorCheckerCalculator.tif"),
                getClassLoaderResource("/tiff/quad-jpeg.tif"),
                getClassLoaderResource("/tiff/quad-lzw.tif"),
                getClassLoaderResource("/tiff/bali.tif"),
                getClassLoaderResource("/tiff/lzw-colormap-iiobe.tif"),
                // TODO: FixMe for ColorMap + ExtraSamples (custom ColorModel)
//                getClassLoaderResource("/tiff/colormap-with-extrasamples.tif"),

                getClassLoaderResource("/tiff/depth/flower-minisblack-02.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-04.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-06.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-08.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-10.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-12.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-14.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-16.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-24.tif"),
                getClassLoaderResource("/tiff/depth/flower-minisblack-32.tif"),

                getClassLoaderResource("/tiff/depth/flower-palette-02.tif"),
                getClassLoaderResource("/tiff/depth/flower-palette-04.tif"),
                getClassLoaderResource("/tiff/depth/flower-palette-08.tif"),
                getClassLoaderResource("/tiff/depth/flower-palette-16.tif"),

                getClassLoaderResource("/tiff/depth/flower-rgb-contig-08.tif"),
                // TODO: FixMe for RGB > 8 bits / sample
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-10.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-12.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-14.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-16.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-24.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-contig-32.tif"),

                getClassLoaderResource("/tiff/depth/flower-rgb-planar-08.tif"),
                // TODO: FixMe for planar RGB > 8 bits / sample
//                getClassLoaderResource("/tiff/depth/flower-rgb-planar-10.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-planar-12.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-planar-14.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-planar-16.tif"),
//                getClassLoaderResource("/tiff/depth/flower-rgb-planar-24.tif"),

                getClassLoaderResource("/tiff/scan-mono-iccgray.tif"),
                getClassLoaderResource("/tiff/old-style-jpeg-inconsistent-metadata.tif"),
                getClassLoaderResource("/tiff/ccitt/group3_1d.tif"),
                getClassLoaderResource("/tiff/ccitt/group3_2d.tif"),
                getClassLoaderResource("/tiff/ccitt/group3_1d_fill.tif"),
                getClassLoaderResource("/tiff/ccitt/group3_2d_fill.tif"),
                getClassLoaderResource("/tiff/ccitt/group4.tif")
        );

        for (URL url : testData) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (ImageInputStream input = ImageIO.createImageInputStream(url);
                 ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                reader.setInput(input);
                writer.setOutput(stream);

                List<ImageInfo> infos = new ArrayList<>(20);

                writer.prepareWriteSequence(null);

                for (int i = 0; i < reader.getNumImages(true); i++) {
                    IIOImage image = reader.readAll(i, null);

                    // If compression is Old JPEG, rewrite as JPEG
                    // Normally, use the getAsTree method, but we don't care here if we are tied to our impl
                    TIFFImageMetadata metadata = (TIFFImageMetadata) image.getMetadata();
                    Directory ifd = metadata.getIFD();
                    Entry compressionEntry = ifd.getEntryById(TIFF.TAG_COMPRESSION);

                    int compression = compressionEntry != null ? ((Number) compressionEntry.getValue()).intValue() : TIFFBaseline.COMPRESSION_NONE;

                    infos.add(new ImageInfo(image.getRenderedImage().getWidth(), image.getRenderedImage().getHeight(), compression));

                    ImageWriteParam param = writer.getDefaultWriteParam();

                    if (compression == TIFFExtension.COMPRESSION_OLD_JPEG) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Override the copy from metadata
                        param.setCompressionType("JPEG");
                    }

                    writer.writeToSequence(image, param);
                }

                writer.endWriteSequence();

//                File tempFile = File.createTempFile("foo-", ".tif");
//                System.err.println("open " + tempFile.getAbsolutePath());
//                FileUtil.write(tempFile, output.toByteArray());

                try (ImageInputStream inputAfter = new ByteArrayImageInputStream(output.toByteArray())) {
                    reader.setInput(inputAfter);

                    int numImages = reader.getNumImages(true);

                    assertEquals("Number of pages differs from original", infos.size(), numImages);

                    for (int i = 0; i < numImages; i++) {
                        IIOImage after = reader.readAll(i, null);
                        ImageInfo info = infos.get(i);

                        TIFFImageMetadata afterMetadata = (TIFFImageMetadata) after.getMetadata();
                        Directory afterIfd = afterMetadata.getIFD();
                        Entry afterCompressionEntry = afterIfd.getEntryById(TIFF.TAG_COMPRESSION);

                        if (info.compression == TIFFExtension.COMPRESSION_OLD_JPEG) {
                            // Should rewrite this from old-style to new style
                            assertEquals("Old JPEG compression not rewritten as JPEG", TIFFExtension.COMPRESSION_JPEG, ((Number) afterCompressionEntry.getValue()).intValue());
                        }
                        else {
                            assertEquals("Compression differs from original", info.compression, ((Number) afterCompressionEntry.getValue()).intValue());
                        }

                        assertEquals("Image width differs from original", info.width, after.getRenderedImage().getWidth());
                        assertEquals("Image height differs from original", info.height, after.getRenderedImage().getHeight());
                    }
                }
            }
        }
    }

    private class ImageInfo {
        final int width;
        final int height;

        final int compression;

        private ImageInfo(int width, int height, int compression) {
            this.width = width;
            this.height = height;
            this.compression = compression;
        }
    }
}
