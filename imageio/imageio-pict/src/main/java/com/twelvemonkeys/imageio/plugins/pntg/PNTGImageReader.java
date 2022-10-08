/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static com.twelvemonkeys.imageio.plugins.pntg.PNTGImageReaderSpi.isMacBinaryPNTG;
import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

/**
 * PNTGImageReader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGImageReader.java,v 1.0 23/03/2021 haraldk Exp$
 */
public final class PNTGImageReader extends ImageReaderBase {

    private static final Set<ImageTypeSpecifier> IMAGE_TYPES =
            Collections.singleton(ImageTypeSpecifiers.createIndexed(new int[] {-1, 0}, false, -1, 1, DataBuffer.TYPE_BYTE));

    PNTGImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        return 576;
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        return 720;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        return IMAGE_TYPES.iterator();
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
        int[] destBands = param != null ? param.getDestinationBands() : null;

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        WritableRaster destRaster = destination.getRaster()
                                               .createWritableChild(destRegion.x, destRegion.y, destRegion.width, destRegion.height, 0, 0, destBands);

        Raster rowRaster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE, width, 1, 1, 1, null)
                                 .createChild(srcRegion.x, 0, destRegion.width, 1, 0, 0, destBands);

        processImageStarted(imageIndex);

        readData(srcRegion, destRegion, xSub, ySub, destRaster, rowRaster);

        processImageComplete();

        return destination;
    }

    private void readData(Rectangle srcRegion, Rectangle destRegion, int xSub, int ySub, WritableRaster destRaster, Raster rowRaster) throws IOException {
        byte[] rowData = ((DataBufferByte) rowRaster.getDataBuffer()).getData();

        try (DataInputStream decoderStream = new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput), new PackBitsDecoder()))) {
            int srcMaxY = srcRegion.y + srcRegion.height;
            for (int y = 0; y < srcMaxY; y++) {
                decoderStream.readFully(rowData);

                if (y >= srcRegion.y && y % ySub == 0) {
                    subsampleRow(rowData, srcRegion.x, srcRegion.width, rowData, destRegion.x, 1, 1, xSub);

                    int destY = (y - srcRegion.y) / ySub;
                    destRaster.setDataElements(0, destY, rowRaster);

                    processImageProgress(y / (float) srcMaxY);
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }
        }
    }

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        return new PNTGMetadata(getRawImageType(imageIndex));
    }

    private void readHeader() throws IOException {
        if (isMacBinaryPNTG(imageInput)) {
            // Seek to end of MacBinary header
            // TODO: Could actually get the file name, creation date etc metadata from this data
            imageInput.seek(128);
        }
        else {
            imageInput.seek(0);
        }

        // Skip pattern data section (usually all 0s)
        if (imageInput.skipBytes(512) != 512) {
            throw new IIOException("Could not skip pattern data");
        }
    }
}
