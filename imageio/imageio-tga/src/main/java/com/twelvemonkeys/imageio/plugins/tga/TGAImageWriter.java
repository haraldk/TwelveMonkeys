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
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.twelvemonkeys.imageio.plugins.tga.TGAImageWriteParam.isRLE;
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
        Validate.notNull(imageType, "imageType");

        return new TGAMetadata(imageType, TGAHeader.from(imageType, isRLE(param, null)), null);
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        Validate.notNull(inData, "inData");
        Validate.notNull(imageType, "imageType");

        if (inData instanceof TGAMetadata) {
            return inData;
        }

        // TODO: Make metadata mutable, and do actual merge
        return getDefaultImageMetadata(imageType, param);
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output);

        if (imageOutput != null) {
            imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new TGAImageWriteParam(getLocale());
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        assertOutput();
        Validate.notNull(image, "image");

        if (image.hasRaster()) {
            throw new UnsupportedOperationException("Raster not supported");
        }

        final boolean compressed = isRLE(param, image.getMetadata());
        RenderedImage renderedImage = image.getRenderedImage();
        ImageTypeSpecifier type = ImageTypeSpecifiers.createFromRenderedImage(renderedImage);
        TGAHeader header = TGAHeader.from(type, renderedImage.getWidth(), renderedImage.getHeight(), compressed);

        header.write(imageOutput);

        processImageStarted(0);

        WritableRaster rowRaster = header.getPixelDepth() == 32
                                   ? ImageTypeSpecifiers.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {2, 1, 0, 3}, DataBuffer.TYPE_BYTE, true, false).createBufferedImage(renderedImage.getWidth(), 1).getRaster()
                                   : renderedImage.getSampleModel().getTransferType() == DataBuffer.TYPE_INT
                                     ? ImageTypeSpecifiers.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {2, 1, 0}, DataBuffer.TYPE_BYTE, false, false).createBufferedImage(renderedImage.getWidth(), 1).getRaster()
                                     : type.createBufferedImage(renderedImage.getWidth(), 1).getRaster();

        final DataBuffer buffer = rowRaster.getDataBuffer();

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

                    DataOutput imageOutput = compressed ? createRLEStream(this.imageOutput, header.getPixelDepth()) : this.imageOutput;

                    switch (buffer.getDataType()) {
                        case DataBuffer.TYPE_BYTE:
                            rowRaster.setDataElements(0, 0, raster.createChild(0, y, raster.getWidth(), 1, 0, 0, null));
                            imageOutput.write(((DataBufferByte) buffer).getData());
                            break;
                        case DataBuffer.TYPE_USHORT:
                            rowRaster.setDataElements(0, 0, raster.createChild(0, y, raster.getWidth(), 1, 0, 0, null));
                            short[] shorts = ((DataBufferUShort) buffer).getData();

                            // TODO: Get rid of this, due to stupid design in EncoderStream...
                            ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            bb.asShortBuffer().put(shorts);
                            imageOutput.write(bb.array());
                            // TODO: The below should work just as good
//                             for (short value : shorts) {
//                                 imageOutput.writeShort(value);
//                             }
                            break;
                        default:
                            throw new IIOException("Unsupported data type");
                    }

                    if (compressed) {
                        ((LittleEndianDataOutputStream) imageOutput).close();
                    }
                }

                processImageProgress(tileY * 100f / renderedImage.getNumYTiles());
            }
        }

        // TODO: If we have thumbnails, we need to write extension too.

        processImageComplete();
    }

    private static LittleEndianDataOutputStream createRLEStream(final ImageOutputStream stream, int pixelDepth) {
        return new LittleEndianDataOutputStream(new EncoderStream(IIOUtil.createStreamAdapter(stream), new RLEEncoder(pixelDepth)));
    }

    // TODO: Refactor to common util
    // TODO: Implement WritableRaster too, for use in reading
    private Raster asByteRaster(final Raster raster, ColorModel colorModel) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                return raster;
            case DataBuffer.TYPE_USHORT:
                return raster; // TODO: We handle USHORT especially for now..
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
