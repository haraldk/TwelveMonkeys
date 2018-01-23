/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTestCase;
import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JPEGImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriterTest.java,v 1.0 06.02.12 17:05 haraldk Exp$
 */
public class JPEGImageWriterTest extends ImageWriterAbstractTestCase {

    private static final JPEGImageWriterSpi SPI = new JPEGImageWriterSpi(lookupDelegateProvider());

    private static ImageWriterSpi lookupDelegateProvider() {
        return IIOUtil.lookupProviderByName(IIORegistry.getDefaultInstance(), "com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi", ImageWriterSpi.class);
    }

    @Override
    protected ImageWriter createImageWriter() {
        try {
            return SPI.createWriterInstance();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(320, 200, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(32, 20, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(32, 20, BufferedImage.TYPE_INT_BGR),
                new BufferedImage(32, 20, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(32, 20, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(32, 20, BufferedImage.TYPE_BYTE_GRAY)
        );
    }

    @Test
    public void testReaderForWriter() {
        ImageWriter writer = createImageWriter();
        ImageReader reader = ImageIO.getImageReader(writer);
        assertNotNull(reader);
        assertEquals(writer.getClass().getPackage(), reader.getClass().getPackage());
    }

    private ByteArrayOutputStream transcode(final ImageReader reader, final URL resource, final ImageWriter writer, int outCSType) throws IOException {
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
        ImageWriter writer = createImageWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        ByteArrayOutputStream stream = transcode(reader, getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"), writer, ColorSpace.TYPE_RGB);

//        FileUtil.write(new File("/Downloads/foo-rgb.jpg"), stream.toByteArray());

        // TODO: Validate that correct warnings are emitted (if any are needed?)

        reader.reset();
        reader.setInput(new ByteArrayImageInputStream(stream.toByteArray()));
        BufferedImage image = reader.read(0);
        assertNotNull(image);
    }

    @Test
    public void testTranscodeWithMetadataCMYKtoCMYK() throws IOException {
        ImageWriter writer = createImageWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        // TODO: Find a smaller test sample, to waste less time?
        ByteArrayOutputStream stream = transcode(reader, getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), writer, ColorSpace.TYPE_CMYK);

        reader.reset();
        reader.setInput(new ByteArrayImageInputStream(stream.toByteArray()));

        BufferedImage image = reader.read(0);
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());

        IIOMetadata metadata = reader.getImageMetadata(0);
        IIOMetadataNode standard = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList colorSpaceType = standard.getElementsByTagName("ColorSpaceType");
        assertEquals(1, colorSpaceType.getLength());
        assertEquals("CMYK", ((IIOMetadataNode) colorSpaceType.item(0)).getAttribute("name"));
    }

    // TODO: YCCK
}
