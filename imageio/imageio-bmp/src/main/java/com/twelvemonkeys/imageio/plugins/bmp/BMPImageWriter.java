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

package com.twelvemonkeys.imageio.plugins.bmp;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * BMPImageWriter
 */
public final class BMPImageWriter extends DIBImageWriter {
    protected BMPImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        // We can use the existing BMPImageWriteParam, as it's part of the javax.imageio API.
        return new BMPImageWriteParam(getLocale());
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        // TODO
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        // TODO: Support both our own and the com.sun.. metadata + standard metadata
        return null;
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        assertOutput();

        if (image == null) {
            throw new IllegalArgumentException("image may not be null");
        }

        if (image.hasRaster()) {
            // TODO: The default BMPWriter seems to support this, consider doing so as well
            throw new UnsupportedOperationException("image has a Raster!");
        }

        imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        clearAbortRequest();
        processImageStarted(0);

        if (param == null) {
            param = getDefaultWriteParam();
        }

        // Default to bottom-up
        // TODO: top-down only allowed for RGB and BITFIELDS compressions (not RLE or PNG)!
        // Though Windows seems to support top-down for RLE too...
        final boolean isTopDown = param instanceof BMPImageWriteParam && ((BMPImageWriteParam) param).isTopDown();
//        int compression = DIB.COMPRESSION_ALPHA_BITFIELDS; // BMP can use BIFTFIELDS or ALPHA_BITFIELDS
        int compression = DIB.COMPRESSION_RGB;

        // TODO: Fix
        BufferedImage img = (BufferedImage) image.getRenderedImage();

        int height = img.getHeight();
        int width = img.getWidth();

        // Write File header
        // TODO: Always use V4/V5 header, when writing with alpha, to avoid ambiguity
        // TODO: Allow writing normal BITMAP_INFO_HEADER_SIZE with "fake" alpha as well?
        int infoHeaderSize = DIB.BITMAP_INFO_HEADER_SIZE;
        boolean hasExtraMasks = infoHeaderSize == DIB.BITMAP_INFO_HEADER_SIZE && (compression == DIB.COMPRESSION_BITFIELDS || compression == DIB.COMPRESSION_ALPHA_BITFIELDS);
        // TODO: Allow writing without file header for ICO/CUR support
        writeFileHeader(infoHeaderSize, DIB.BMP_FILE_HEADER_SIZE + infoHeaderSize + width * height * 4, hasExtraMasks);
        writeDIBHeader(infoHeaderSize, img.getWidth(), img.getHeight(), isTopDown, img.getColorModel().getPixelSize(), compression);
//        writeDIBHeader(infoHeaderSize, img, isTopDown, DIB.COMPRESSION_RGB);

        if (hasExtraMasks) {
            imageOutput.writeInt(0x000000FF); // B
            imageOutput.writeInt(0x0000FF00); // G
            imageOutput.writeInt(0x00FF0000); // R
            imageOutput.writeInt(0xFF000000); // A
        }

        writeUncompressed(isTopDown, img, height, width);

        processImageComplete();
    }

    private void writeFileHeader(int infoHeaderSize, int fileSize, boolean hasMasks) throws IOException {
        // 14 bytes
        imageOutput.writeShort('M' << 8 | 'B');
        imageOutput.writeInt(fileSize + (hasMasks ? 16 : 0)); // File size (only known at this time if uncompressed!)
        imageOutput.writeShort(0); // Reserved
        imageOutput.writeShort(0); // Reserved

        imageOutput.writeInt(DIB.BMP_FILE_HEADER_SIZE + infoHeaderSize + (hasMasks ? 16 : 0)); // Offset to image data
    }

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        File output = new File(args[0].replace('.', '_') + "_copy.bmp");

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            DIBImageWriter writer = new BMPImageWriter(null);
            writer.setOutput(stream);
            writer.write(ImageIO.read(input));
        }
    }

}
