/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PNMImageReader extends ImageReaderBase {
    // TODO: Allow reading unknown tuple types as Raster!
    // TODO: readAsRenderedImage?

    private PNMHeader header;

    PNMImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override protected void resetMembers() {
        header = null;
    }

    private void readHeader() throws IOException {
        if (header == null) {
            header = HeaderParser.parse(imageInput);

            imageInput.flushBefore(imageInput.getStreamPosition());
            imageInput.setByteOrder(header.getByteOrder()); // For PFM support
        } else {
            imageInput.seek(imageInput.getFlushedPosition());
        }
    }

    static String asASCII(final short type) {
        byte[] asciiBytes = {(byte) ((type >> 8) & 0xff), (byte) (type & 0xff)};
        return new String(asciiBytes, Charset.forName("ASCII"));
    }

    @Override public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getWidth();
    }

    @Override public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight();
    }

    @Override public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        int bitsPerSample = header.getBitsPerSample();
        int transferType = header.getTransferType();
        int samplesPerPixel = header.getSamplesPerPixel();
        boolean hasAlpha = header.getTransparency() != Transparency.OPAQUE;

        switch (header.getTupleType()) {
            case BLACKANDWHITE_WHITE_IS_ZERO:
                // PBM: As TIFF WhiteIsZero
                // NOTE: We handle this by inverting the values when reading, as Java has no ColorModel that easily supports this.
            case BLACKANDWHITE_ALPHA:
            case GRAYSCALE_ALPHA:
            case BLACKANDWHITE:
            case GRAYSCALE:
                // PGM: Linear or non-linear gray?
                ColorSpace gray = ColorSpace.getInstance(ColorSpace.CS_GRAY);

                if (header.getTransferType() == DataBuffer.TYPE_FLOAT) {
                    return ImageTypeSpecifiers.createInterleaved(gray, createBandOffsets(samplesPerPixel), transferType, hasAlpha, false);
                }
                if (header.getMaxSample() <= PNM.MAX_VAL_16BIT) {
                    return hasAlpha ? ImageTypeSpecifiers.createGrayscale(bitsPerSample, transferType, false)
                            : ImageTypeSpecifiers.createGrayscale(bitsPerSample, transferType);
                }

                return ImageTypeSpecifiers.createInterleaved(gray, createBandOffsets(samplesPerPixel), transferType, hasAlpha, false);

            case RGB:
            case RGB_ALPHA:
                // Using sRGB seems sufficient for PPM, as it is very close to ITU-R Recommendation BT.709 (same gamut and white point CIE D65)
                ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                if (header.getTransferType() == DataBuffer.TYPE_FLOAT) {
                    return ImageTypeSpecifiers.createInterleaved(sRGB, createBandOffsets(samplesPerPixel), transferType, hasAlpha, false);
                }

                return ImageTypeSpecifiers.createInterleaved(sRGB, createBandOffsets(samplesPerPixel), transferType, hasAlpha, false);

            case CMYK:
            case CMYK_ALPHA:
                ColorSpace cmyk = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);
                return ImageTypeSpecifiers.createInterleaved(cmyk, createBandOffsets(samplesPerPixel), transferType, hasAlpha, false);

            default:
                // TODO: Allow reading unknown tuple types as Raster!

                throw new AssertionError("Unknown PNM tuple type: " + header.getTupleType());
        }
    }

    private int[] createBandOffsets(int numBands) {
        int[] offsets = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            offsets[i] = i;
        }

        return offsets;
    }

    @Override public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        switch (header.getTupleType()) {
            case RGB:
                if (header.getTransferType() == DataBuffer.TYPE_BYTE) {
                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
                }

                break;

            case RGB_ALPHA:
                if (header.getTransferType() == DataBuffer.TYPE_BYTE) {
                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));
                    // TODO: Why does ColorConvertOp choke on these (Ok, because it misinterprets the alpha channel for a color component, but how do we make it work)?
//                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE));
//                    specifiers.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE));
                }

                break;
        }

        if (rawType != null) {
            specifiers.add(rawType);
        }

        return specifiers.iterator();
    }

    @Override public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

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

        int transferType = rowRaster.getTransferType();
        int samplesPerPixel = header.getSamplesPerPixel();

        byte[] rowDataByte = null;
        short[] rowDataUShort = null;
        float[] rowDataFloat = null;

        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
                rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                break;
            case DataBuffer.TYPE_USHORT:
                rowDataUShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                break;
            case DataBuffer.TYPE_FLOAT:
                rowDataFloat = ((DataBufferFloat) rowRaster.getDataBuffer()).getData();
                break;
            default:
                throw new AssertionError("Unsupported transfer type: " + transferType);
        }

        ColorConvertOp colorConvert = null;
        if (!destination.getColorModel().isCompatibleRaster(rowRaster)) {
            colorConvert = new ColorConvertOp(rawType.getColorModel().getColorSpace(), destination.getColorModel().getColorSpace(), null);
        }

        int xSub = param == null ? 1 : param.getSourceXSubsampling();
        int ySub = param == null ? 1 : param.getSourceYSubsampling();

        DataInput input = wrapInput();

        processImageStarted(imageIndex);

        for (int y = 0; y < height; y++) {
            switch (transferType) {
                case DataBuffer.TYPE_BYTE:
                    readRowByte(destRaster, clippedRow, colorConvert, rowDataByte, samplesPerPixel, input, y, srcRegion, xSub, ySub);
                    break;
                case DataBuffer.TYPE_USHORT:
                    readRowUShort(destRaster, clippedRow, rowDataUShort, samplesPerPixel, input, y, srcRegion, xSub, ySub);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    readRowFloat(destRaster, clippedRow, rowDataFloat, samplesPerPixel, input, y, srcRegion, xSub, ySub);
                    break;
                default:
                    throw new AssertionError("Unsupported transfer type: " + transferType);
            }

            processImageProgress(100f * y / height);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            if (y >= srcRegion.y + srcRegion.height) {
                // We're done
                break;
            }
        }

        processImageComplete();

        return destination;
    }

    private DataInput wrapInput() throws IIOException {
        switch (header.getFileType()) {
            case PNM.PBM_PLAIN:
                return new DataInputStream(new Plain1BitDecoder(IIOUtil.createStreamAdapter(imageInput), header.getWidth() * header.getSamplesPerPixel()));
            case PNM.PGM_PLAIN:
            case PNM.PPM_PLAIN:
                if (header.getBitsPerSample() <= 8) {
                    return  new DataInputStream(new Plain8BitDecoder(IIOUtil.createStreamAdapter(imageInput)));
                }
                if (header.getBitsPerSample() <= 16) {
                    return  new DataInputStream(new Plain16BitDecoder(IIOUtil.createStreamAdapter(imageInput)));
                }
                throw new IIOException("Unsupported bit depth for type: " + asASCII(header.getFileType()));
            case PNM.PBM:
            case PNM.PGM:
            case PNM.PPM:
            case PNM.PAM:
            case PNM.PFM_GRAY:
            case PNM.PFM_RGB:
                return imageInput;
            default:
                throw new AssertionError("Unknown input type: " + asASCII(header.getFileType()));
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

    private void readRowByte(final WritableRaster destRaster,
                             Raster rowRaster,
                             final ColorConvertOp colorConvert,
                             final byte[] rowDataByte,
                             final int samplesPerPixel,
                             final DataInput input, final int y,
                             final Rectangle srcRegion,
                             final int xSub, final int ySub) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || y < srcRegion.y || y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataByte.length);
            return;
        }

        input.readFully(rowDataByte);

        // Subsample (horizontal)
        subsampleHorizontal(rowDataByte, rowDataByte.length, samplesPerPixel, xSub);

        normalize(rowDataByte, 0, rowDataByte.length / xSub);

        int destY = (y - srcRegion.y) / ySub;
        if (colorConvert != null) {
            colorConvert.filter(rowRaster, destRaster.createWritableChild(0, destY, rowRaster.getWidth(), 1, 0, 0, null));
        } else {
            destRaster.setDataElements(0, destY, rowRaster);
        }
    }

    private void readRowUShort(final WritableRaster destRaster,
                               Raster rowRaster,
                               final short[] rowDataUShort,
                               final int samplesPerPixel, final DataInput input, final int y,
                               final Rectangle srcRegion, final int xSub, final int ySub) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || y < srcRegion.y || y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataUShort.length * 2);
            return;
        }

        readFully(input, rowDataUShort);

        // Subsample (horizontal)
        subsampleHorizontal(rowDataUShort, rowDataUShort.length, samplesPerPixel, xSub);

        normalize(rowDataUShort);

        int destY = (y - srcRegion.y) / ySub;
        // TODO: ColorConvertOp if needed
        destRaster.setDataElements(0, destY, rowRaster);
    }

    private void readRowFloat(final WritableRaster destRaster,
                              Raster rowRaster,
                              final float[] rowDataFloat,
                              final int samplesPerPixel, final DataInput input, final int y,
                              final Rectangle srcRegion, final int xSub, final int ySub) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || y < srcRegion.y || y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataFloat.length * 4);
            return;
        }

        readFully(input, rowDataFloat);

        // Subsample (horizontal)
        subsampleHorizontal(rowDataFloat, rowDataFloat.length, samplesPerPixel, xSub);

        normalize(rowDataFloat);

        int destY = (y - srcRegion.y) / ySub;
        // TODO: ColorConvertOp if needed
        destRaster.setDataElements(0, destY, rowRaster);
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

    // TODO: Candidate util method
    private static void readFully(final DataInput input, final float[] floats) throws IOException {
        if (input instanceof ImageInputStream) {
            // Optimization for ImageInputStreams, read all in one go
            ((ImageInputStream) input).readFully(floats, 0, floats.length);
        }
        else {
            for (int i = 0; i < floats.length; i++) {
                floats[i] = input.readFloat();
            }
        }
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private void subsampleHorizontal(final Object data, final int length, final int samplesPerPixel, final int xSub) {
        if (xSub == 1) {
            return;
        }

        // TODO: Super-special 1 bit subsampling handling for PBM

        for (int x = 0; x < length / xSub; x += samplesPerPixel) {
            System.arraycopy(data, x * xSub, data, x, samplesPerPixel);
        }
    }

    private void normalize(final byte[] rowData, final int start, final int length) {
        switch (header.getTupleType()) {
            case BLACKANDWHITE:
            case BLACKANDWHITE_ALPHA:
                // Do nothing
                break;
            case BLACKANDWHITE_WHITE_IS_ZERO:
                // Invert
                for (int i = start; i < length; i++) {
                    rowData[i] = (byte) ~rowData[i];
                }
                break;
            case GRAYSCALE:
            case GRAYSCALE_ALPHA:
            case RGB:
            case RGB_ALPHA:
            case CMYK:
            case CMYK_ALPHA:
                // Normalize
                for (int i = start; i < length; i++) {
                    rowData[i] = (byte) ((rowData[i] * PNM.MAX_VAL_8BIT) / header.getMaxSample());
                }
                break;
        }
    }

    private void normalize(final short[] rowData) {
        // Normalize
        for (int i = 0; i < rowData.length; i++) {
            rowData[i] = (short) ((rowData[i] * PNM.MAX_VAL_16BIT) / header.getMaxSample());
        }
    }

    private void normalize(final float[] rowData) {
        // TODO: Do the real thing, find min/max and normalize to range 0...255? But only if not reading raster..? Only support reading as raster?
        // Normalize
        for (int i = 0; i < rowData.length; i++) {
//            if (rowData[i] > 275f /*header.getMaxSampleFloat()*/) {
//                System.out.println("rowData[" + i + "]: " + rowData[i]);
//            }
//            rowData[i] = rowData[i] / 275f /*header.getMaxSampleFloat()*/;
        }
    }

    @Override public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new PNMMetadata(header);
    }

    public static void main(String[] args) throws IOException {
        PNMImageReader reader = new PNMImageReader(null);

        for (String arg : args) {
            File in = new File(arg);
            reader.setInput(ImageIO.createImageInputStream(in));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(reader.getImageTypes(0).next());
//            param.setSourceSubsampling(2, 3, 0, 0);
//
//            int width = reader.getWidth(0);
//            int height = reader.getHeight(0);
//
//            param.setSourceRegion(new Rectangle(width / 4, height / 4, width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2));

            showIt(reader.read(0, param), in.getName());

//            new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);

//            File reference = new File(in.getParent() + "/../reference", in.getName().replaceAll("\\.p(a|b|g|p)m", ".png"));
//            if (reference.exists()) {
//                System.err.println("reference.getAbsolutePath(): " + reference.getAbsolutePath());
//                showIt(ImageIO.read(reference), reference.getName());
//            }

//            break;
        }
    }
}
