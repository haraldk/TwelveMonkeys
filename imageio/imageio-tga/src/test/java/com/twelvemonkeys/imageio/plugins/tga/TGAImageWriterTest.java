/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;
import com.twelvemonkeys.io.FastByteArrayOutputStream;

import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.util.ImageReaderAbstractTest.assertImageDataEquals;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;


/**
 * TGAImageWriterTest
 */
public class TGAImageWriterTest extends ImageWriterAbstractTest<TGAImageWriter> {
    private static final ImageTypeSpecifier TYPE_USHORT_1555_ARGB = ImageTypeSpecifiers.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB), 0x7C00, 0x03E0, 0x001F, 0x8000, DataBuffer.TYPE_USHORT, false);

    @Override
    protected ImageWriterSpi createProvider() {
        return new TGAImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB_PRE)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR_PRE)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_555_RGB)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY)),
                drawSomething(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED)),
                drawSomething(TYPE_USHORT_1555_ARGB.createBufferedImage(10, 10))
        );
    }

    @Test
    public void testDefaultParamIsTGA() throws IOException {
        ImageWriter writer = createWriter();
        assertEquals(writer.getDefaultWriteParam().getClass(), TGAImageWriteParam.class);
        writer.dispose();
    }

    @Test
    public void testWriteRead() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);


        assumeTrue(reader != null, "Reader should not be null");

        for (RenderedImage testData : getTestData()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(stream);
                writer.write(drawSomething((BufferedImage) testData));
            }

            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                reader.setInput(stream);

                BufferedImage image = reader.read(0);

                assertNotNull(image);
                assertImageDataEquals("Images differ for " + testData, (BufferedImage) testData, image);
            }
        }

        writer.dispose();
        reader.dispose();
    }

    @Test
    public void testWriteReadRLE() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        assumeTrue(reader != null, "Reader should not be null");

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType("RLE");

        for (RenderedImage testData : getTestData()) {
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(4096);

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(drawSomething((BufferedImage) testData), null, null), param);
            }

            try (ImageInputStream stream = new ByteArrayImageInputStream(buffer.toByteArray())) {
                reader.setInput(stream);

                BufferedImage image = reader.read(0);

                assertNotNull(image);
                assertImageDataEquals("Images differ for " + testData, (BufferedImage) testData, image);
            }
        }

        writer.dispose();
        reader.dispose();
    }

    @Test
    public void testRewriteCompressionCopyFromMetadataUncompressed() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        assumeTrue(reader != null, "Reader should not be null");

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tga/UTC24.TGA"))) {
            reader.setInput(input);
            IIOImage image = reader.readAll(0, null);
            assertNull(findCompressionType(image.getMetadata())); // Sanity

            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(65536);

            try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(output);

                // Copy from metadata should be default, we'll validate here
                ImageWriteParam param = writer.getDefaultWriteParam();
                assertEquals(ImageWriteParam.MODE_COPY_FROM_METADATA, param.getCompressionMode());

                writer.write(null, image, param);
            }

            try (ImageInputStream stream = new ByteArrayImageInputStream(buffer.toByteArray())) {
                reader.setInput(stream);
                IIOMetadata metadata = reader.getImageMetadata(0);

                assertNull(findCompressionType(metadata));
            }
        }

        writer.dispose();
        reader.dispose();
    }

    @Test
    public void testRewriteCompressionCopyFromMetadataRLE() throws IOException {
        ImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        assumeTrue(reader != null, "Reader should not be null");

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tga/CTC24.TGA"))) {
            reader.setInput(input);
            IIOImage image = reader.readAll(0, null);
            assertEquals("RLE", findCompressionType(image.getMetadata())); // Sanity

            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(32768);

            try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(output);

                // Copy from metadata should be default, we'll just go with no param here
                writer.write(null, image, null);
            }

            try (ImageInputStream inputStream = new ByteArrayImageInputStream(buffer.toByteArray())) {
                reader.setInput(inputStream);
                IIOMetadata metadata = reader.getImageMetadata(0);

                assertEquals("RLE", findCompressionType(metadata));
            }
        }

        writer.dispose();
        reader.dispose();
    }

    private String findCompressionType(IIOMetadata metadata) {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList compressionTypeName = root.getElementsByTagName("CompressionTypeName");
        if (compressionTypeName.getLength() > 0) {
            return compressionTypeName.item(0).getAttributes().getNamedItem("value").getNodeValue();
        }

        return null;
    }
}