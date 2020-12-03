/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.junit.Test;
import org.w3c.dom.NodeList;

import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

/**
 * JPEGImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriterTest.java,v 1.0 06.02.12 17:05 haraldk Exp$
 */
public class JPEGImageWriterTest extends ImageWriterAbstractTest<JPEGImageWriter> {
    private static ImageWriterSpi lookupDelegateProvider() {
        return IIOUtil.lookupProviderByName(IIORegistry.getDefaultInstance(), "com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi", ImageWriterSpi.class);
    }

    @Override
    protected ImageWriterSpi createProvider() {
        return new JPEGImageWriterSpi(lookupDelegateProvider());
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        ColorModel cmyk = new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        return Arrays.asList(
                new BufferedImage(320, 200, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(32, 20, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(32, 20, BufferedImage.TYPE_INT_BGR),
                // Java 11+ no longer supports RGBA JPEG
//                new BufferedImage(32, 20, BufferedImage.TYPE_INT_ARGB),
//                new BufferedImage(32, 20, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(32, 20, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(cmyk, cmyk.createCompatibleWritableRaster(32, 20), cmyk.isAlphaPremultiplied(), null)
        );
    }

    @Test
    public void testReaderForWriter() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);
        assertNotNull(reader);
        assertEquals(writer.getClass().getPackage(), reader.getClass().getPackage());
    }

    private ByteArrayOutputStream transcode(final ImageReader reader, final URL resource, final ImageWriter writer, int outCSType) throws IOException {
        return transcode(reader, resource, writer, outCSType, true);
    }

    private ByteArrayOutputStream transcode(final ImageReader reader, final URL resource, final ImageWriter writer, int outCSType, boolean embedICCProfile) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(resource)) {
            reader.setInput(input);
            ImageTypeSpecifier specifier = null;

            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();

                if (type.getColorModel().getColorSpace().getType() == outCSType) {
                    specifier = type;
                    break;
                }
            }

            // Read image with requested color space
            ImageReadParam readParam = reader.getDefaultReadParam();
            readParam.setSourceRegion(new Rectangle(Math.min(100, reader.getWidth(0)), Math.min(100, reader.getHeight(0))));
            readParam.setDestinationType(specifier);
            IIOImage image = reader.readAll(0, readParam);

            if (!embedICCProfile) {
                // Get rid of the color model/icc profile
                ColorSpace fakeCS = mock(ColorSpace.class);
                when(fakeCS.getType()).thenReturn(ColorSpace.TYPE_CMYK);
                when(fakeCS.getNumComponents()).thenReturn(4);
                specifier = new ImageTypeSpecifier(new ComponentColorModel(fakeCS, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE), image.getRenderedImage().getSampleModel());
            }

            // Write it back
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
            try (ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
                writer.setOutput(output);
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                writeParam.setDestinationType(specifier);
                writer.write(null, image, writeParam);

                return bytes;
            }
        }
    }

    @Test
    public void testTranscodeWithMetadataRGBtoRGB() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        ByteArrayOutputStream stream = transcode(reader, getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"), writer, ColorSpace.TYPE_RGB);

        // TODO: Validate that correct warnings are emitted (if any are needed?)

        reader.reset();
        reader.setInput(new ByteArrayImageInputStream(stream.toByteArray()));
        BufferedImage image = reader.read(0);
        assertNotNull(image);

        // Test color space type RGB (encoded as YCbCr) in standard metadata
        IIOMetadata metadata = reader.getImageMetadata(0);
        IIOMetadataNode standard = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList colorSpaceType = standard.getElementsByTagName("ColorSpaceType");
        assertEquals(1, colorSpaceType.getLength());
        assertEquals("YCbCr", ((IIOMetadataNode) colorSpaceType.item(0)).getAttribute("name"));
    }

    @Test
    public void testTranscodeWithMetadataCMYKtoCMYK() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        ByteArrayOutputStream stream = transcode(reader, getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), writer, ColorSpace.TYPE_CMYK);

        reader.reset();
        reader.setInput(new ByteArrayImageInputStream(stream.toByteArray()));

        BufferedImage image = reader.read(0);
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());

        // Test color space type CMYK in standard metadata
        IIOMetadata metadata = reader.getImageMetadata(0);
        IIOMetadataNode standard = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList colorSpaceType = standard.getElementsByTagName("ColorSpaceType");
        assertEquals(1, colorSpaceType.getLength());
        assertEquals("CMYK", ((IIOMetadataNode) colorSpaceType.item(0)).getAttribute("name"));

        // Test APP2/ICC_PROFILE segments form native metadata
        IIOMetadataNode nativeMeta = (IIOMetadataNode) metadata.getAsTree(JPEGImage10Metadata.JAVAX_IMAGEIO_JPEG_IMAGE_1_0);
        NodeList unknown = nativeMeta.getElementsByTagName("unknown");
        assertEquals(11, unknown.getLength()); // We write longer segments than the original, so we get less segments

        ByteArrayOutputStream iccSegments = new ByteArrayOutputStream(1024 * 1024);

        for (int i = 0; i < unknown.getLength(); i++) {
            IIOMetadataNode node = (IIOMetadataNode) unknown.item(i);
            byte[] data = (byte[]) node.getUserObject();

            // 226 -> E2, FFE2 -> APP2 marker, ICC_PROFILE
            String markerId = "ICC_PROFILE";
            if (node.getAttribute("MarkerTag").equals("226")
                    && markerId.equals(new String(data, 0, markerId.length(), StandardCharsets.US_ASCII))) {
                int offset = markerId.length() + 3; // ICC_PROFILE + null + index + count
                iccSegments.write(Arrays.copyOfRange(data, offset, data.length));
            }
        }

        ICC_Profile profile = ICC_Profile.getInstance(iccSegments.toByteArray());
        assertNotNull(profile); // Assumption, we either have a valid profile, or getInstance blew up...
        assertEquals(ColorSpace.TYPE_CMYK, profile.getColorSpaceType());
    }

    @Test
    public void testTranscodeWithMetadataCMYKtoCMYKNoProfile() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        // TODO: Add flag to allow removing the ICC profile from image
        ByteArrayOutputStream stream = transcode(reader, getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), writer, ColorSpace.TYPE_CMYK, false);

        reader.reset();
        reader.setInput(new ByteArrayImageInputStream(stream.toByteArray()));

        BufferedImage image = reader.read(0);
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());

        // Test color space type CMYK in standard metadata
        IIOMetadata metadata = reader.getImageMetadata(0);
        IIOMetadataNode standard = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList colorSpaceType = standard.getElementsByTagName("ColorSpaceType");
        assertEquals(1, colorSpaceType.getLength());
        assertEquals("CMYK", ((IIOMetadataNode) colorSpaceType.item(0)).getAttribute("name"));

        // Test APP2/ICC_PROFILE segments form native metadata
        IIOMetadataNode nativeMeta = (IIOMetadataNode) metadata.getAsTree(JPEGImage10Metadata.JAVAX_IMAGEIO_JPEG_IMAGE_1_0);
        NodeList unknown = nativeMeta.getElementsByTagName("unknown");
        assertEquals(0, unknown.getLength());
    }

    // TODO: YCCK
}
