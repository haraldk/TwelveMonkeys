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
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.Rational;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTestCase;
import org.junit.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadataTest.createTIFFFieldNode;
import static org.junit.Assert.*;

/**
 * TIFFImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriterTest.java,v 1.0 19.09.13 13:22 haraldk Exp$
 */
public class TIFFImageWriterTest extends ImageWriterAbstractTestCase {

    public static final TIFFImageWriterSpi PROVIDER = new TIFFImageWriterSpi();

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
//                new BufferedImage(300, 200, BufferedImage.TYPE_BYTE_BINARY), // TODO!
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

        Directory ifds = new EXIFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));

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

        Directory ifds = new EXIFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));
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

        Directory ifds = new EXIFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));

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

        Directory ifds = new EXIFReader().read(new ByteArrayImageInputStream(buffer.toByteArray()));
        Entry software = ifds.getEntryById(TIFF.TAG_SOFTWARE);
        assertNotNull(software);
        assertEquals(softwareString, software.getValueAsString());
    }
}
