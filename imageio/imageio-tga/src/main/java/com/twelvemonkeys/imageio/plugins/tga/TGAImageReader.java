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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.lang.Validate;
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
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

final class TGAImageReader extends ImageReaderBase {
    // http://www.fileformat.info/format/tga/egff.htm
    // http://www.gamers.org/dEngine/quake3/TGA.txt

    private TGAHeader header;
    private TGAExtensions extensions;

    TGAImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        header = null;
        extensions = null;
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

        List<ImageTypeSpecifier> specifiers = new ArrayList<>();
        specifiers.add(rawType);

        if (rawType.getBufferedImageType() == BufferedImage.TYPE_INT_RGB) {
            specifiers.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
        }
        else if (rawType.getBufferedImageType() == BufferedImage.TYPE_INT_ARGB) {
            specifiers.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE));
        }
        else if (rawType.getBufferedImageType() == BufferedImage.TYPE_INT_ARGB_PRE) {
            specifiers.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
        }

        return specifiers.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        switch (header.getImageType()) {
            case TGA.IMAGETYPE_COLORMAPPED:
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
                return ImageTypeSpecifiers.createFromIndexColorModel(header.getColorMap());
            case TGA.IMAGETYPE_MONOCHROME:
            case TGA.IMAGETYPE_MONOCHROME_RLE:
                switch (header.getPixelDepth()) {
                    case 8:
                        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
                    case 16:
                        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY);
                    default:
                        throw new IIOException("Unknown pixel depth for monochrome: " + header.getPixelDepth());
                }
            case TGA.IMAGETYPE_TRUECOLOR:
            case TGA.IMAGETYPE_TRUECOLOR_RLE:
                ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

                boolean hasAlpha = header.getAttributeBits() > 0 && (extensions == null || extensions.hasAlpha());
                boolean isAlphaPremultiplied = extensions != null && extensions.isAlphaPremultiplied();

                switch (header.getPixelDepth()) {
                    case 16:
                        if (hasAlpha) {
                            // USHORT_1555_ARGB...
                            return ImageTypeSpecifiers.createPacked(sRGB, 0x7C00, 0x03E0, 0x001F, 0x8000, DataBuffer.TYPE_USHORT, isAlphaPremultiplied);
                        }
                        // Default mask out alpha
                        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_555_RGB);
                    case 24:
                        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
                    case 32:
                        // NOTE: We'll read using little endian byte order, thus the file layout is BGRA/BGRx
                        if (hasAlpha) {
                            return ImageTypeSpecifier.createFromBufferedImageType(isAlphaPremultiplied ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_ARGB);
                        }

                        return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
                    default:
                        throw new IIOException("Unknown pixel depth for true color: " + header.getPixelDepth());
                }
            default:
                throw new IIOException("Unknown image type: " + header.getImageType());
        }
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
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

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        processImageStarted(imageIndex);

        int imageType = header.getImageType();

        // Wrap input if RLE encoded.
        // NOTE: As early specs said it was ok to compress across boundaries, we need to support that.
        DataInput input;
        if (imageType == TGA.IMAGETYPE_COLORMAPPED_RLE || imageType == TGA.IMAGETYPE_TRUECOLOR_RLE || imageType == TGA.IMAGETYPE_MONOCHROME_RLE) {
            input = new LittleEndianDataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput), new RLEDecoder(header.getPixelDepth())));
        }
        else {
            input = imageInput;
        }

        int pixelDepth = header.getPixelDepth();
        boolean flipped = isOriginLowerLeft(header.getOrigin());

        for (int y = 0; y < height; y++) {
            switch (pixelDepth) {
                case 8:
                case 24:
                    byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    readRowByte(input, height, srcRegion, flipped, xSub, ySub, rowDataByte, destRaster, clippedRow, y);
                    break;
                case 16:
                    short[] rowDataUShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                    readRowUShort(input, height, srcRegion, flipped, xSub, ySub, rowDataUShort, destRaster, clippedRow, y);
                    break;
                case 32:
                    int[] rowDataInt = ((DataBufferInt) rowRaster.getDataBuffer()).getData();
                    readRowInt(input, height, srcRegion, flipped, xSub, ySub, rowDataInt, destRaster, clippedRow, y);
                    break;
                default:
                    throw new AssertionError("Unsupported pixel depth: " + pixelDepth);
            }

            processImageProgress(100f * y / height);

            if (height - 1 - y < srcRegion.y) {
                break;
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }

        processImageComplete();

        return destination;
    }

    private boolean isOriginLowerLeft(final int origin) throws IIOException {
        switch (origin) {
            case TGA.ORIGIN_LOWER_LEFT:
                return true;
            case TGA.ORIGIN_UPPER_LEFT:
                return false;
            default:
                // Other orientations are not supported
                throw new IIOException("Unsupported origin: " + origin);
        }
    }

    private void readRowByte(final DataInput input, int height, Rectangle srcRegion, boolean flip, int xSub, int ySub,
                             byte[] rowDataByte, WritableRaster destChannel, Raster srcChannel, int y) throws IOException {
        // Flip into position?
        int srcY = flip ? height - 1 - y : y;
        int dstY = (srcY - srcRegion.y) / ySub;

        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || srcY < srcRegion.y || srcY >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataByte.length);

            return;
        }

        input.readFully(rowDataByte, 0, rowDataByte.length);

        int numBands = srcChannel.getNumBands();
        if (numBands == 4 && (header.getAttributeBits() == 0 || extensions != null && !extensions.hasAlpha())) {
            // Remove the alpha channel (make pixels opaque) if there are no "attribute bits" (alpha bits)
            removeAlpha32(rowDataByte);
        }

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + srcRegion.width) / xSub) * numBands; x += numBands) {
                System.arraycopy(rowDataByte, x * xSub, rowDataByte, x, numBands);
            }
        }

        destChannel.setDataElements(0, dstY, srcChannel);
    }

    private void removeAlpha32(final byte[] rowData) {
        for (int i = 3; i < rowData.length; i += 4) {
            rowData[i] = (byte) 0xFF;
        }
    }

    private void readRowUShort(final DataInput input, int height, Rectangle srcRegion, boolean flip, int xSub, int ySub,
                               short[] rowDataUShort, WritableRaster destChannel, Raster srcChannel, int y) throws IOException {
        // Flip into position?
        int srcY = flip ? height - 1 - y : y;
        int dstY = (srcY - srcRegion.y) / ySub;

        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || srcY < srcRegion.y || srcY >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataUShort.length * 2);

            return;
        }

        readFully(input, rowDataUShort);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataUShort[srcRegion.x + x] = rowDataUShort[srcRegion.x + x * xSub];
            }
        }

        destChannel.setDataElements(0, dstY, srcChannel);
    }

    private void readRowInt(final DataInput input, int height, Rectangle srcRegion, boolean flip, int xSub, int ySub,
                            int[] rowDataInt, WritableRaster destChannel, Raster srcChannel, int y) throws IOException {
        // Flip into position?
        int srcY = flip ? height - 1 - y : y;
        int dstY = (srcY - srcRegion.y) / ySub;

        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || srcY < srcRegion.y || srcY >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataInt.length * 4);

            return;
        }

        readFully(input, rowDataInt);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = srcRegion.x / xSub; x < ((srcRegion.x + srcRegion.width) / xSub); x++) {
                rowDataInt[x] = rowDataInt[x * xSub];
            }
        }

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

    // TODO: Candidate util method
    private static void readFully(final DataInput input, final int[] ints) throws IOException {
        if (input instanceof ImageInputStream) {
            // Optimization for ImageInputStreams, read all in one go
            ((ImageInputStream) input).readFully(ints, 0, ints.length);
        }
        else {
            for (int i = 0; i < ints.length; i++) {
                ints[i] = input.readInt();
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
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // Read header
            header = TGAHeader.read(imageInput);
            imageInput.flushBefore(imageInput.getStreamPosition());

            // Read footer, if 2.0 format (ends with TRUEVISION-XFILE\0)
            skipToEnd(imageInput);
            imageInput.seek(imageInput.getStreamPosition() - 26);

            long extOffset = imageInput.readUnsignedInt();
            /*long devOffset = */imageInput.readUnsignedInt(); // Ignored for now

            byte[] magic = new byte[18];
            imageInput.readFully(magic);

            if (Arrays.equals(magic, TGA.MAGIC) && extOffset > 0) {
                imageInput.seek(extOffset);
                int extSize = imageInput.readUnsignedShort();
                extensions = extSize == 0 ? null : TGAExtensions.read(imageInput, extSize);
            }
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    // TODO: Candidate util method
    private static void skipToEnd(final ImageInputStream stream) throws IOException {
        if (stream.length() > 0) {
            // Seek to end of file
            stream.seek(stream.length());
        }
        else {
            // Skip to end
            long lastGood = stream.getStreamPosition();

            while (stream.read() != -1) {
                lastGood = stream.getStreamPosition();
                stream.skipBytes(1024);
            }

            stream.seek(lastGood);

            while (true) {
                if (stream.read() == -1) {
                    break;
                }
                // Just continue reading to EOF...
            }
        }
    }

    // Thumbnail support

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public boolean hasThumbnails(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return extensions != null && extensions.getThumbnailOffset() > 0;
    }

    @Override
    public int getNumThumbnails(final int imageIndex) throws IOException {
        return hasThumbnails(imageIndex) ? 1 : 0;
    }

    @Override
    public int getThumbnailWidth(final int imageIndex, final int thumbnailIndex) throws IOException {
        checkBounds(imageIndex);
        Validate.isTrue(thumbnailIndex >= 0 && thumbnailIndex < getNumThumbnails(imageIndex), "thumbnailIndex >= numThumbnails");

        imageInput.seek(extensions.getThumbnailOffset());

        return imageInput.readUnsignedByte();
    }

    @Override
    public int getThumbnailHeight(final int imageIndex, final int thumbnailIndex) throws IOException {
        getThumbnailWidth(imageIndex, thumbnailIndex); // Laziness...

        return imageInput.readUnsignedByte();
    }

    @Override
    public BufferedImage readThumbnail(final int imageIndex, final int thumbnailIndex) throws IOException {
        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        int width = getThumbnailWidth(imageIndex, thumbnailIndex);
        int height = getThumbnailHeight(imageIndex, thumbnailIndex);

        // For thumbnail, always read entire image
        Rectangle srcRegion = new Rectangle(width, height);

        BufferedImage destination = getDestination(null, imageTypes, width, height);
        WritableRaster destRaster = destination.getRaster();
        WritableRaster rowRaster = rawType.createBufferedImage(width, 1).getRaster();

        processThumbnailStarted(imageIndex, thumbnailIndex);
        processThumbnailProgress(0f);

        // Thumbnail is always stored non-compressed, no need for RLE support
        imageInput.seek(extensions.getThumbnailOffset() + 2);

        int pixelDepth = header.getPixelDepth();
        boolean flipped = isOriginLowerLeft(header.getOrigin());

        for (int y = 0; y < height; y++) {
            switch (pixelDepth) {
                case 8:
                case 24:
                    byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    readRowByte(imageInput, height, srcRegion, flipped, 1, 1, rowDataByte, destRaster, rowRaster, y);
                    break;
                case 16:
                    short[] rowDataUShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                    readRowUShort(imageInput, height, srcRegion, flipped, 1, 1, rowDataUShort, destRaster, rowRaster, y);
                    break;
                case 32:
                    int[] rowDataInt = ((DataBufferInt) rowRaster.getDataBuffer()).getData();
                    readRowInt(imageInput, height, srcRegion, flipped, 1, 1, rowDataInt, destRaster, rowRaster, y);
                    break;
                default:
                    throw new AssertionError("Unsupported pixel depth: " + pixelDepth);
            }

            processThumbnailProgress(100f * y / height);

            if (height - 1 - y < srcRegion.y) {
                break;
            }
        }

        processThumbnailProgress(100f);
        processThumbnailComplete();

        return destination;
    }

    // Metadata support

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new TGAMetadata(header, extensions);
    }

    public static void main(String[] args) throws IOException {
        TGAImageReaderSpi provider = new TGAImageReaderSpi();
        TGAImageReader reader = new TGAImageReader(provider);

        for (String arg : args) {
            File in = new File(arg);
            ImageInputStream stream = ImageIO.createImageInputStream(in);

            System.err.println("Can read?: " + provider.canDecodeInput(stream));

            reader.setInput(stream);

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

            System.err.println("reader.header: " + reader.header);
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
