/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.ImageWriterBase;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;

public final class PNMImageWriter extends ImageWriterBase {

    PNMImageWriter(final ImageWriterSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public boolean canWriteRasters() {
        return true;
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        // TODO: Issue warning if streamMetadata is non-null?
        // TODO: Issue warning if IIOImage contains thumbnails or other data we can't store?

        HeaderWriter.write(image, getOriginatingProvider(), imageOutput);

        // TODO: Sub region
        // TODO: Subsampling
        // TODO: Source bands

        processImageStarted(0);
        writeImageData(image);
        processImageComplete();
    }

    private void writeImageData(final IIOImage image) throws IOException {
        // - dump data as is (or convert, if TYPE_INT_xxx)
        // Enforce RGB/CMYK order for such data!

        // TODO: Loop over x/y tiles, using 0,0 is only valid for BufferedImage
        // TODO: PNM/PAM does not support tiling, we must iterate all tiles along the x-axis for each row we write
        Raster tile = image.hasRaster() ? image.getRaster() : image.getRenderedImage().getTile(0, 0);

        SampleModel sampleModel = tile.getSampleModel();

        DataBuffer dataBuffer = tile.getDataBuffer();

        int tileWidth = tile.getWidth();
        int tileHeight = tile.getHeight();

        final int transferType = sampleModel.getTransferType();
        Object data = null;
        for (int y = 0; y < tileHeight; y++) {
            data = sampleModel.getDataElements(0, y, tileWidth, 1, data, dataBuffer);
            // TODO: Support other (short, float) data types
            if (transferType == DataBuffer.TYPE_BYTE) {
                imageOutput.write((byte[]) data);
            }
            else if (transferType == DataBuffer.TYPE_USHORT) {
                short[] shortData = (short[]) data;
                imageOutput.writeShorts(shortData, 0, shortData.length);
            }

            processImageProgress(y * 100f / tileHeight); // TODO: Take tile y into account
            if (abortRequested()) {
                processWriteAborted();
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        File output = new File(input.getParentFile(), input.getName().replace('.', '_') + ".ppm");

        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            System.err.println("input Image == null");
            System.exit(-1);
        }

        System.out.println("image: " + image);

        ImageWriter writer = new PNMImageWriterSpi().createWriterInstance();

        if (!output.exists()) {
            writer.setOutput(ImageIO.createImageOutputStream(output));
            writer.write(image);
        }
        else {
            System.err.println("Output file " + output + " already exists.");
            System.exit(-1);
        }
    }
}
