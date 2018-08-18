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

package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.xml.XMLSerializer;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SGIImageReader extends ImageReaderBase {

    private SGIHeader header;

    protected SGIImageReader(final ImageReaderSpi provider) {
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

        return header.getWidth();
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        // TODO: Implement
        specifiers.add(rawType);

        return specifiers.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        // NOTE: There doesn't seem to be any god way to determine color space, other than by convention
        // 1 channel: Gray, 2 channel: Gray + Alpha, 3 channel: RGB, 4 channel: RGBA (hopefully never CMYK...)

        int channels = header.getChannels();

        ColorSpace cs = channels < 3 ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpace.getInstance(ColorSpace.CS_sRGB);

        switch (header.getBytesPerPixel()) {
            case 1:
                return ImageTypeSpecifiers.createBanded(cs, createIndices(channels, 1), createIndices(channels, 0), DataBuffer.TYPE_BYTE, channels == 2 || channels == 4, false);
            case 2:
                return ImageTypeSpecifiers.createBanded(cs, createIndices(channels, 1), createIndices(channels, 0), DataBuffer.TYPE_USHORT, channels == 2 || channels == 4, false);
            default:
                throw new IIOException("Unknown number of bytes per pixel: " + header.getBytesPerPixel());
        }
    }

    private int[] createIndices(final int bands, int increment) {
        int[] indices = new int[bands];

        for (int i = 0; i < bands; i++) {
            indices[i] = i * increment;
        }

        return indices;
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        if (header.getColorMode() != SGI.COLORMODE_NORMAL) {
            processWarningOccurred(String.format("Unsupported color mode: %d, colors may look incorrect", header.getColorMode()));
        }

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, imageTypes, width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster destRaster = clipToRect(destination.getRaster(), destRegion, param != null ? param.getDestinationBands() : null);
        checkReadParamBandSettings(param, rawType.getNumBands(), destRaster.getNumBands());

        WritableRaster rowRaster = rawType.createBufferedImage(width, 1).getRaster();
        // Clip to source region
        Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                                          param != null ? param.getSourceBands() : null,
                                          param != null ? param.getSourceXSubsampling() : 1);

        int[] scanlineOffsets;
        int[] scanlineLengths;

        int compression = header.getCompression();
        if (compression == SGI.COMPRESSION_RLE) {
            scanlineOffsets = new int[height * header.getChannels()];
            scanlineLengths = new int[height * header.getChannels()];
            imageInput.readFully(scanlineOffsets, 0, scanlineOffsets.length);
            imageInput.readFully(scanlineLengths, 0, scanlineLengths.length);
        }
        else {
            scanlineOffsets = null;
            scanlineLengths = null;
        }

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        processImageStarted(imageIndex);

        for (int c = 0; c < header.getChannels(); c++) {
            WritableRaster destChannel = destRaster.createWritableChild(destRaster.getMinX(), destRaster.getMinY(), destRaster.getWidth(), destRaster.getHeight(), 0, 0, new int[] {c});
            Raster srcChannel = clippedRow.createChild(clippedRow.getMinX(), 0, clippedRow.getWidth(), 1, 0, 0, new int[] {c});

            // NOTE: SGI images are store bottom/up, thus y value is opposite of destination y
            for (int y = 0; y < height; y++) {
                switch (header.getBytesPerPixel()) {
                    case 1:
                        byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData(c);
                        readRowByte(height, srcRegion, scanlineOffsets, scanlineLengths, compression, xSub, ySub, c, rowDataByte, destChannel, srcChannel, y);
                        break;
                    case 2:
                        short[] rowDataUShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData(c);
                        readRowUShort(height, srcRegion, scanlineOffsets, scanlineLengths, compression, xSub, ySub, c, rowDataUShort, destChannel, srcChannel, y);
                        break;
                    default:
                        throw new AssertionError();
                }

                processImageProgress(100f * y / height * c / header.getChannels());

                if (height - 1 - y < srcRegion.y) {
                    break;
                }

                if (abortRequested()) {
                    break;
                }
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }

        processImageComplete();

        return destination;
    }

    private void readRowByte(int height, Rectangle srcRegion, int[] scanlineOffsets, int[] scanlineLengths, int compression, int xSub, int ySub, int c, byte[] rowDataByte, WritableRaster destChannel, Raster srcChannel, int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || height - 1 - y < srcRegion.y || height - 1 - y >= srcRegion.y + srcRegion.height) {
            if (compression == SGI.COMPRESSION_NONE) {
                imageInput.skipBytes(rowDataByte.length);
            }

            return;
        }

        // Wrap input
        DataInput input;
        if (compression == SGI.COMPRESSION_RLE) {
            int scanLineIndex = c * height + y;
            imageInput.seek(scanlineOffsets[scanLineIndex]);
            input = new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput, scanlineLengths[scanLineIndex]), new RLEDecoder()));
        } else {
            input = imageInput;
        }

        input.readFully(rowDataByte, 0, rowDataByte.length);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataByte[srcRegion.x + x] = rowDataByte[srcRegion.x + x * xSub];
            }
        }

        normalize(rowDataByte, 9, srcRegion.width / xSub);

        // Flip into position (SGI images are stored bottom/up)
        int dstY = (height - 1 - y - srcRegion.y) / ySub;
        destChannel.setDataElements(0, dstY, srcChannel);
    }

    private void readRowUShort(int height, Rectangle srcRegion, int[] scanlineOffsets, int[] scanlineLengths, int compression, int xSub, int ySub, int c, short[] rowDataUShort, WritableRaster destChannel, Raster srcChannel, int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || height - 1 - y < srcRegion.y || height - 1 - y >= srcRegion.y + srcRegion.height) {
            if (compression == SGI.COMPRESSION_NONE) {
                imageInput.skipBytes(rowDataUShort.length * 2);
            }

            return;
        }

        // Wrap input
        DataInput input;
        if (compression == SGI.COMPRESSION_RLE) {
            int scanLineIndex = c * height + y;
            imageInput.seek(scanlineOffsets[scanLineIndex]);
            input = new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput, scanlineLengths[scanLineIndex]), new RLEDecoder()));
        } else {
            input = imageInput;
        }

        readFully(input, rowDataUShort);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataUShort[srcRegion.x + x] = rowDataUShort[srcRegion.x + x * xSub];
            }
        }

        normalize(rowDataUShort, 9, srcRegion.width / xSub);

        // Flip into position (SGI images are stored bottom/up)
        int dstY = (height - 1 - y - srcRegion.y) / ySub;
        destChannel.setDataElements(0, dstY, srcChannel);
    }

    // TODO: Candidate util method
    private static void readFully(final DataInput input, final short[] shorts) throws IOException {
        if (input instanceof ImageInputStream) {
            // Optimization for ImageInputStreams, read all in one go
            ((ImageInputStream) input).readFully(shorts, 0, shorts.length);
        }
        else {
            for (int i = 0; i < shorts.length; i++) {
                shorts[i] = input.readShort();
            }
        }
    }

    private void normalize(final byte[] rowData, final int start, final int length) {
        int minValue = header.getMinValue();
        int maxValue = header.getMaxValue();
        if (minValue != 0 && maxValue != 0xff) {
            // Normalize
            for (int i = start; i < length; i++) {
                rowData[i] = (byte) (((rowData[i] - minValue) * 0xff) / maxValue);
            }
        }
    }

    private void normalize(final short[] rowData, final int start, final int length) {
        int minValue = header.getMinValue();
        int maxValue = header.getMaxValue();
        if (minValue != 0 && maxValue != 0xff) {
            // Normalize
            for (int i = start; i < length; i++) {
                rowData[i] = (byte) (((rowData[i] - minValue) * 0xff) / maxValue);
            }
        }
    }

    private Raster clipRowToRect(final Raster raster, final Rectangle rect, final int[] bands, final int xSub) {
        if (rect.contains(raster.getMinX(), 0, raster.getWidth(), 1)
                && xSub == 1
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createChild(rect.x / xSub, 0, rect.width / xSub, 1, 0, 0, bands);
    }

    private WritableRaster clipToRect(final WritableRaster raster, final Rectangle rect, final int[] bands) {
        if (rect.contains(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight())
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createWritableChild(rect.x, rect.y, rect.width, rect.height, 0, 0, bands);
    }

    private void readHeader() throws IOException {
        if (header == null) {
            header = SGIHeader.read(imageInput);

//            System.err.println("header: " + header);

            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    @Override public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new SGIMetadata(header);
    }

    public static void main(String[] args) throws IOException {
        SGIImageReader reader = new SGIImageReader(null);

        for (String arg : args) {
            File in = new File(arg);
            reader.setInput(ImageIO.createImageInputStream(in));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(reader.getImageTypes(0).next());
//            param.setSourceSubsampling(2, 3, 0, 0);
//            param.setSourceSubsampling(2, 1, 0, 0);
//
//            int width = reader.getWidth(0);
//            int height = reader.getHeight(0);
//
//            param.setSourceRegion(new Rectangle(width / 4, height / 4, width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2, width / 2, height / 2));

            BufferedImage image = reader.read(0, param);

            System.err.println("image: " + image);

            showIt(image, in.getName());

            new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);

//            File reference = new File(in.getParent() + "/../reference", in.getName().replaceAll("\\.p(a|b|g|p)m", ".png"));
//            if (reference.exists()) {
//                System.err.println("reference.getAbsolutePath(): " + reference.getAbsolutePath());
//                showIt(ImageIO.read(reference), reference.getName());
//            }

//            break;
        }
    }
}
