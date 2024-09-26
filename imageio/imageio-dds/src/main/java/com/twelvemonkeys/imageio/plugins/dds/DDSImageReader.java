/*
 * Copyright (c) 2024, Paul Allen, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.dds;

import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;

public final class DDSImageReader extends ImageReaderBase {

    private DDSHeader header;

    public DDSImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        header = null;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getWidth(imageIndex);
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight(imageIndex);
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        assertInput();
        readHeader();

        return header.getMipMapCount();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        // TODO: Implement for the specific formats...
        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        return Collections.singletonList(getRawImageType(imageIndex)).iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        processImageStarted(imageIndex);

        DDSReader dds = new DDSReader(header);
        int[] pixels = dds.read(imageInput, imageIndex);

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();

        computeRegions(param, width, height, destination, srcRegion, destRegion);

        int srcXStep = param != null ? param.getSourceXSubsampling() : 1;
        int srcYStep = param != null ? param.getSourceYSubsampling() : 1;
        int srcMaxY = srcRegion.y + srcRegion.height;

        for (int y = 0, srcY = srcRegion.y, destY = destRegion.y; srcY < srcMaxY; y++, srcY += srcYStep, destY++) {
            int offset = width * srcY + srcRegion.x;

            subsampleRow(pixels, offset, width, pixels, offset, 4, 8, srcXStep);
            destination.setRGB(destRegion.x, destY, destRegion.width, 1, pixels, offset, width);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100f * y / destRegion.height);
        }

        processImageComplete();

        return destination;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        ImageTypeSpecifier imageType = getRawImageType(imageIndex);

        return new DDSMetadata(imageType, header);
    }

    private void readHeader() throws IOException {
        if (header == null) {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            header = DDSHeader.read(imageInput);

            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    public static void main(final String[] args) throws IOException {
        for (String arg : args) {
            File file = new File(arg);
            BufferedImage image = ImageIO.read(file);
            showIt(image, file.getName());
        }
    }
}
