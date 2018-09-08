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

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * TGAImageWriter
 */
final class TGAImageWriter extends ImageWriterBase {
    TGAImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        TGAHeader header = TGAHeader.from(imageType.createBufferedImage(1, 1), param);
        return new TGAMetadata(header, null);
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output);

        if (imageOutput != null) {
            imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        assertOutput();

        if (image.hasRaster()) {
            throw new UnsupportedOperationException("Raster not supported");
        }

        RenderedImage renderedImage = image.getRenderedImage();
        TGAHeader header = TGAHeader.from(renderedImage, param);

        header.write(imageOutput);

        processImageStarted(0);

        WritableRaster rowRaster = header.getPixelDepth() == 32
                ? ImageTypeSpecifiers.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{2, 1, 0, 3}, DataBuffer.TYPE_BYTE, true, false)
                .createBufferedImage(renderedImage.getWidth(), 1)
                .getRaster()
                : renderedImage.getSampleModel().getTransferType() == DataBuffer.TYPE_INT
                ? ImageTypeSpecifiers.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{2, 1, 0}, DataBuffer.TYPE_BYTE, false, false)
                .createBufferedImage(renderedImage.getWidth(), 1)
                .getRaster()
                : ImageTypeSpecifier.createFromRenderedImage(renderedImage)
                .createBufferedImage(renderedImage.getWidth(), 1)
                .getRaster();

        DataBuffer buffer = rowRaster.getDataBuffer();

        for (int tileY = 0; tileY < renderedImage.getNumYTiles(); tileY++) {
            for (int tileX = 0; tileX < renderedImage.getNumXTiles(); tileX++) {
                if (abortRequested()) {
                    break;
                }

                // Wraps TYPE_INT rasters to TYPE_BYTE
                Raster raster = asByteRaster(renderedImage.getTile(tileX, tileY), renderedImage.getColorModel());

                for (int y = 0; y < raster.getHeight(); y++) {
                    if (abortRequested()) {
                        break;
                    }

                    switch (buffer.getDataType()) {
                        case DataBuffer.TYPE_BYTE:
                            rowRaster.setDataElements(0, 0, raster.createChild(0, y, raster.getWidth(), 1, 0, 0, null));
                            imageOutput.write(((DataBufferByte) buffer).getData());
                            break;
                        case DataBuffer.TYPE_USHORT:
                            rowRaster.setDataElements(0, 0, raster.createChild(0, y, raster.getWidth(), 1, 0, 0, null));
                            short[] shorts = ((DataBufferUShort) buffer).getData();
                            imageOutput.writeShorts(shorts, 0, shorts.length);
                            break;
                        default:
                            throw new IIOException("Unsupported data");
                    }

                    processImageProgress(tileY * 100f / renderedImage.getNumYTiles());
                }

            }
        }

        // TODO: If we have thumbnails, we need to write extension too.

        processImageComplete();

    }

    // Vi kan lage en DataBuffer wrapper-klasse,
    // som gjør TYPE_INT_RGB/INT_ARGB/INT_ARGB_PRE/INT_BGR til tilsvarende TYPE_xBYTE-klasser.
    // Ytelse er ikke viktig her, siden vi uansett må konvertere når vi skal skrive/lese.
    // TODO: Refactore dette til felles lag?
    // TODO: Implementere writable også, slik at vi kan bruke i lesing?
    private Raster asByteRaster(final Raster raster, ColorModel colorModel) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                return raster;
            case DataBuffer.TYPE_USHORT:
                return raster; // TODO: we handle ushort especially for now..
            case DataBuffer.TYPE_INT:
                final int bands = colorModel.getNumComponents();
                final DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int w = raster.getWidth();
                int h = raster.getHeight();
                int size = buffer.getSize();

                return new Raster(
                        new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, bands, w * bands, createBandOffsets(colorModel)),
                        new DataBuffer(DataBuffer.TYPE_BYTE, size * bands) {
                    @Override
                    public int getElem(int bank, int i) {
                        int index = i / bands;
                        int shift = (i % bands) * 8;

                        return (buffer.getElem(index) >>> shift) & 0xFF;
                    }

                    @Override
                    public void setElem(int bank, int i, int val) {
                        throw new UnsupportedOperationException("Wrapped buffer is read-only");
                    }
                }, new Point()) {};
            default:
                throw new IllegalArgumentException(String.format("Raster type %d not supported", raster.getTransferType()));
        }
    }

    private int[] createBandOffsets(final ColorModel colorModel) {
        notNull(colorModel, "colorModel");

        if (colorModel instanceof DirectColorModel) {
            DirectColorModel dcm = (DirectColorModel) colorModel;
            int[] masks = dcm.getMasks();
            int[] offs = new int[masks.length];

            for (int i = 0; i < masks.length; i++) {
                int mask = masks[i];
                int off = 0;

                // TODO: FixMe! This only works for standard 8 bit masks (0xFF)
                if (mask != 0) {
                    while ((mask & 0xFF) == 0) {
                        mask >>>= 8;
                        off++;
                    }
                }

                offs[i] = off;
            }

            return offs;
        }

        throw new IllegalArgumentException(String.format("%s not supported", colorModel.getClass().getSimpleName()));
    }

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File(args[0]));
        ImageIO.write(image, "TGA", new File("foo.tga"));
    }
}
