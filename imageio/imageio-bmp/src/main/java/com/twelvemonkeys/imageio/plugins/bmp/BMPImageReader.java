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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.xml.XMLSerializer;

import javax.imageio.*;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
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
import java.util.Arrays;
import java.util.Iterator;

/**
 * ImageReader for Microsoft Windows Bitmap (BMP) format.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BMPImageReader.java,v 1.0 Apr 20, 2009 11:54:28 AM haraldk Exp$
 * @see ICOImageReader
 */
public final class BMPImageReader extends ImageReaderBase {
    private long pixelOffset;
    private DIBHeader header;
    private int[] colors;
    private IndexColorModel colorMap;

    private ImageReader jpegReaderDelegate;
    private ImageReader pngReaderDelegate;

    public BMPImageReader() {
        super(new BMPImageReaderSpi());
    }

    protected BMPImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
    }

    @Override
    protected void resetMembers() {
        pixelOffset = 0;
        header = null;
        colors = null;
        colorMap = null;

        if (pngReaderDelegate != null) {
            pngReaderDelegate.dispose();
            pngReaderDelegate = null;
        }
        if (jpegReaderDelegate != null) {
            jpegReaderDelegate.dispose();
            jpegReaderDelegate = null;
        }
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        readHeader();

        return 1;
    }

    private void readHeader() throws IOException {
        assertInput();

        if (header == null) {
            // BMP files have Intel origin, always little endian
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // Read BMP file header
            byte[] fileHeader = new byte[DIB.BMP_FILE_HEADER_SIZE - 4]; // We'll read the last 4 bytes later
            imageInput.readFully(fileHeader);

            if (fileHeader[0] != 'B' || fileHeader[1] != 'M') {
                throw new IIOException("Not a BMP");
            }

            // Ignore rest of data, it's redundant...
            pixelOffset = imageInput.readUnsignedInt();

            // Read DIB header
            header = DIBHeader.read(imageInput);
        }
    }

    private IndexColorModel readColorMap() throws IOException {
        readHeader();

        if (colors == null) {
            if (header.getBitCount() > 8 && header.colorsUsed == 0) {
                // RGB without color map
                colors = new int[0];
            }
            else {
                int offset = DIB.BMP_FILE_HEADER_SIZE + header.getSize();
                if (offset != imageInput.getStreamPosition()) {
                    imageInput.seek(offset);
                }

                if (header.getSize() == DIB.BITMAP_CORE_HEADER_SIZE) {
                    colors = new int[Math.min(header.getColorsUsed(), (int) (pixelOffset - DIB.BMP_FILE_HEADER_SIZE - header.getSize()) / 3)];

                    // Byte triplets in BGR form
                    for (int i = 0; i < colors.length; i++) {
                        int b = imageInput.readUnsignedByte();
                        int g = imageInput.readUnsignedByte();
                        int r = imageInput.readUnsignedByte();
                        colors[i] = r << 16 | g << 8 | b | 0xff000000;
                    }
                }
                else {
                    colors = new int[Math.min(header.getColorsUsed(), (int) (pixelOffset - DIB.BMP_FILE_HEADER_SIZE - header.getSize()) / 4)];

                    // Byte quadruples in BGRa (or little-endian ints in aRGB) form, where a is "Reserved"
                    for (int i = 0; i < colors.length; i++) {
                        colors[i] = imageInput.readInt() & 0x00ffffff | 0xff000000;
                    }
                }

                if (colors.length > 0) {
                    // There might be more entries in the color map, but we ignore these for reading
                    int mapSize = Math.min(colors.length, 1 << header.getBitCount());

                    // Compute bits for > 8 bits (used only for meta data)
                    int bits = header.getBitCount() <= 8 ? header.getBitCount() : mapSize <= 256 ? 8 : 16;

                    colorMap = new IndexColorModel(bits, mapSize, colors, 0, false, -1, DataBuffer.TYPE_BYTE);
                }
            }
        }

        return colorMap;
    }

    @Override
    public int getWidth(int pImageIndex) throws IOException {
        checkBounds(pImageIndex);

        return header.getWidth();
    }

    @Override
    public int getHeight(int pImageIndex) throws IOException {
        checkBounds(pImageIndex);

        return header.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int pImageIndex) throws IOException {
        checkBounds(pImageIndex);

        // TODO: Better implementation, include INT_RGB types for 3BYTE_BGR and 4BYTE_ABGR for INT_ARGB
        return Arrays.asList(getRawImageType(pImageIndex)).iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int pImageIndex) throws IOException {
        checkBounds(pImageIndex);

        if (header.getPlanes() != 1) {
            throw new IIOException("Multiple planes not supported");
        }

        switch (header.getBitCount()) {
            case 1:
            case 2:
            case 4:
            case 8:
                return ImageTypeSpecifiers.createFromIndexColorModel(readColorMap());

            case 16:
                if (header.hasMasks()) {
                    return ImageTypeSpecifiers.createPacked(
                            ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            header.masks[0], header.masks[1], header.masks[2], header.masks[3],
                            DataBuffer.TYPE_USHORT, false
                    );
                }

                // Default if no mask is 555
                return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_555_RGB);

            case 24:
                if (header.getCompression() != DIB.COMPRESSION_RGB) {
                    throw new IIOException("Unsupported compression for RGB: " + header.getCompression());
                }

                return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);

            case 32:
                if (header.hasMasks()) {
                    return ImageTypeSpecifiers.createPacked(
                            ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            header.masks[0], header.masks[1], header.masks[2], header.masks[3],
                            DataBuffer.TYPE_INT, false
                    );
                }

                // Default if no mask
                return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

            case 0:
                if (header.getCompression() == DIB.COMPRESSION_JPEG || header.getCompression() == DIB.COMPRESSION_PNG) {
                    return initReaderDelegate(header.getCompression()).getRawImageType(0);
                }
            default:
                throw new IIOException("Unsupported bit count: " + header.getBitCount());
        }
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        // Delegate reading for JPEG/PNG compression
        if (header.getCompression() == DIB.COMPRESSION_JPEG || header.getCompression() == DIB.COMPRESSION_PNG) {
            return readUsingDelegate(header.getCompression(), param);
        }

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        ColorModel colorModel = destination.getColorModel();
        if (colorModel instanceof IndexColorModel && ((IndexColorModel) colorModel).getMapSize() < header.getColorsUsed()) {
            processWarningOccurred(
                    String.format("Color map contains more colors than raster allows (%d). Ignoring entries above %d.",
                            header.getColorsUsed(), ((IndexColorModel) colorModel).getMapSize())
            );
        }

        // BMP rows are padded to 4 byte boundary
        int rowSizeBytes = ((header.getBitCount() * width + 31) / 32) * 4;

        // Wrap input according to compression
        imageInput.seek(pixelOffset);
        DataInput input;

        switch (header.getCompression()) {
            case DIB.COMPRESSION_RLE4:
                if (header.getBitCount() != 4) {
                    throw new IIOException(String.format("Unsupported combination of bitCount/compression: %s/%s", header.getBitCount(), header.getCompression()));
                }

                input = new LittleEndianDataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput), new RLE4Decoder(width), rowSizeBytes));
                break;

            case DIB.COMPRESSION_RLE8:
                if (header.getBitCount() != 8) {
                    throw new IIOException(String.format("Unsupported combination of bitCount/compression: %s/%s", header.getBitCount(), header.getCompression()));
                }
                input = new LittleEndianDataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput), new RLE8Decoder(width), rowSizeBytes));
                break;

            case DIB.COMPRESSION_BITFIELDS:
            case DIB.COMPRESSION_ALPHA_BITFIELDS:
                // TODO: Validate bitCount for these
            case DIB.COMPRESSION_RGB:
                input = imageInput;
                break;

            default:
                throw new IIOException("Unsupported compression: " + header.getCompression());

        }

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster destRaster = clipToRect(destination.getRaster(), destRegion, param != null ? param.getDestinationBands() : null);
        checkReadParamBandSettings(param, rawType.getNumBands(), destRaster.getNumBands());

        WritableRaster rowRaster;

        switch (header.getBitCount()) {
            case 1:
            case 2:
            case 4:
                rowRaster = Raster.createPackedRaster(new DataBufferByte(rowSizeBytes), width, 1, header.getBitCount(), null);
                break;
            case 8:
            case 24:
                rowRaster = Raster.createInterleavedRaster(new DataBufferByte(rowSizeBytes), width, 1, rowSizeBytes, header.getBitCount() / 8, createOffsets(rawType.getNumBands()), null);
                break;
            case 16:
            case 32:
                rowRaster = rawType.createBufferedImage(width, 1).getRaster();
                break;
            default:
                throw new IIOException("Unsupported pixel depth: " + header.getBitCount());
        }

        // Clip to source region
        Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                param != null ? param.getSourceBands() : null,
                param != null ? param.getSourceXSubsampling() : 1);

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        processImageStarted(imageIndex);
        for (int y = 0; y < height; y++) {
            switch (header.getBitCount()) {
                case 1:
                case 2:
                case 4:
                case 8:
                case 24:
                    byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    readRowByte(input, height, srcRegion, xSub, ySub, rowDataByte, destRaster, clippedRow, y);
                    break;

                case 16:
                    short[] rowDataUShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                    readRowUShort(input, height, srcRegion, xSub, ySub, rowDataUShort, destRaster, clippedRow, y);
                    break;

                case 32:
                    int[] rowDataInt = ((DataBufferInt) rowRaster.getDataBuffer()).getData();
                    readRowInt(input, height, srcRegion, xSub, ySub, rowDataInt, destRaster, clippedRow, y);
                    break;

                default:
                    throw new AssertionError("Unsupported pixel depth: " + header.getBitCount());
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

    private BufferedImage readUsingDelegate(final int compression, final ImageReadParam param) throws IOException {
        return initReaderDelegate(compression).read(0, param);
    }

    private ImageReader initReaderDelegate(int compression) throws IOException {
        ImageReader reader = getImageReaderDelegate(compression);

        imageInput.seek(pixelOffset);
        reader.setInput(new SubImageInputStream(imageInput, header.getImageSize()));

        return reader;
    }

    private ImageReader getImageReaderDelegate(int compression) throws IIOException {
        String format;

        switch (compression) {
            case DIB.COMPRESSION_JPEG:
                if (jpegReaderDelegate != null) {
                    return jpegReaderDelegate;
                }

                format = "JPEG";
                break;

            case DIB.COMPRESSION_PNG:
                if (pngReaderDelegate != null) {
                    return pngReaderDelegate;
                }

                format = "PNG";
                break;

            default:
                throw new AssertionError("Unsupported BMP compression: " + compression);
        }

        // Consider looking for specific PNG and JPEG implementations.
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);

        if (!readers.hasNext()) {
            throw new IIOException(String.format("Delegate ImageReader for %s format not found", format));
        }

        ImageReader reader = readers.next();

        // Install listener
        ListenerDelegator listenerDelegator = new ListenerDelegator();
        reader.addIIOReadWarningListener(listenerDelegator);
        reader.addIIOReadProgressListener(listenerDelegator);
        reader.addIIOReadUpdateListener(listenerDelegator);

        // Cache for later use
        switch (compression) {
            case DIB.COMPRESSION_JPEG:
                jpegReaderDelegate = reader;
                break;
            case DIB.COMPRESSION_PNG:
                pngReaderDelegate = reader;
                break;
        }

        return reader;
    }

    private int[] createOffsets(int numBands) {
        int[] offsets = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            offsets[i] = numBands - i - 1;
        }

        return offsets;
    }

    private void readRowByte(final DataInput input, final int height, final Rectangle srcRegion, final int xSub, final int ySub,
                             final byte[] rowDataByte, final WritableRaster destChannel, final Raster srcChannel, final int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || height - 1 - y < srcRegion.y || height - 1 - y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataByte.length);

            return;
        }

        input.readFully(rowDataByte, 0, rowDataByte.length);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataByte[srcRegion.x + x] = rowDataByte[srcRegion.x + x * xSub];
            }
        }

        if (header.topDown) {
            destChannel.setDataElements(0, y, srcChannel);
        } else {
            // Flip into position
            int dstY = (height - 1 - y - srcRegion.y) / ySub;
            destChannel.setDataElements(0, dstY, srcChannel);
        }
    }

    private void readRowUShort(final DataInput input, final int height, final Rectangle srcRegion, final int xSub, final int ySub,
                               final short[] rowDataUShort, final WritableRaster destChannel, final Raster srcChannel, final int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || height - 1 - y < srcRegion.y || height - 1 - y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataUShort.length * 2 + (rowDataUShort.length % 2) * 2);

            return;
        }

        readFully(input, rowDataUShort);

        // Skip 2 bytes, if not ending on 32 bit/4 byte boundary
        if (rowDataUShort.length % 2 != 0) {
            input.skipBytes(2);
        }

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataUShort[srcRegion.x + x] = rowDataUShort[srcRegion.x + x * xSub];
            }
        }

        if (header.topDown) {
            destChannel.setDataElements(0, y, srcChannel);
        } else {
            // Flip into position
            int dstY = (height - 1 - y - srcRegion.y) / ySub;
            destChannel.setDataElements(0, dstY, srcChannel);
        }
    }

    private void readRowInt(final DataInput input, final int height, final Rectangle srcRegion, final int xSub, final int ySub,
                            final int[] rowDataInt, final WritableRaster destChannel, final Raster srcChannel, final int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || height - 1 - y < srcRegion.y || height - 1 - y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(rowDataInt.length * 4);

            return;
        }

        readFully(input, rowDataInt);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataInt[srcRegion.x + x] = rowDataInt[srcRegion.x + x * xSub];
            }
        }

        if (header.topDown) {
            destChannel.setDataElements(0, y, srcChannel);
        } else {
            // Flip into position
            int dstY = (height - 1 - y - srcRegion.y) / ySub;
            destChannel.setDataElements(0, dstY, srcChannel);
        }
    }

    // TODO: Candidate util method
    private static void readFully(final DataInput input, final short[] shorts) throws IOException {
        if (input instanceof ImageInputStream) {
            // Optimization for ImageInputStreams, read all in one go
            ((ImageInputStream) input).readFully(shorts, 0, shorts.length);
        } else {
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
        } else {
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

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        readHeader();

        switch (header.getBitCount()) {
            case 1:
            case 2:
            case 4:
            case 8:
                readColorMap();
                break;

            default:
                if (header.colorsUsed > 0) {
                    readColorMap();
                }
                break;
        }

        // Why, oh why..? Instead of accepting it's own native format as it should,
        // The DIBImageWriter only accepts instances of com.sun.imageio.plugins.bmp.BMPMetadata...
        // TODO: Consider reflectively construct a BMPMetadata and inject fields
        return new BMPMetadata(header, colors);
    }

    public static void main(String[] args) throws IOException {
        BMPImageReaderSpi provider = new BMPImageReaderSpi();
        BMPImageReader reader = new BMPImageReader(provider);

        for (String arg : args) {
            try {
                File in = new File(arg);
                ImageInputStream stream = ImageIO.createImageInputStream(in);

                System.err.println("Can read?: " + provider.canDecodeInput(stream));

                reader.reset();
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

                System.err.println("reader.header: " + reader.header);

                BufferedImage image = reader.read(0, param);

                System.err.println("image: " + image);

                showIt(image, in.getName());


                IIOMetadata imageMetadata = reader.getImageMetadata(0);
                if (imageMetadata != null) {
                    new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(imageMetadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);
                }
            }
            catch (Throwable t) {
                if (args.length > 1) {
                    System.err.println("---");
                    System.err.println("---> " + t.getClass().getSimpleName() + ": " + t.getMessage() + " for " + arg);
                    System.err.println("---");
                }
                else {
                    throwAs(RuntimeException.class, t);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    static <T extends Throwable> void throwAs(final Class<T> pType, final Throwable pThrowable) throws T {
        throw (T) pThrowable;
    }

    private class ListenerDelegator extends ProgressListenerBase implements IIOReadUpdateListener, IIOReadWarningListener {
        @Override
        public void imageComplete(ImageReader source) {
            processImageComplete();
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            processImageProgress(percentageDone);
        }

        @Override
        public void imageStarted(ImageReader source, int imageIndex) {
            processImageStarted(imageIndex);
        }

        @Override
        public void readAborted(ImageReader source) {
            processReadAborted();
        }

        @Override
        public void sequenceComplete(ImageReader source) {
            processSequenceComplete();
        }

        @Override
        public void sequenceStarted(ImageReader source, int minIndex) {
            processSequenceStarted(minIndex);
        }

        @Override
        public void thumbnailComplete(ImageReader source) {
            processThumbnailComplete();
        }

        @Override
        public void thumbnailProgress(ImageReader source, float percentageDone) {
            processThumbnailProgress(percentageDone);
        }

        @Override
        public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
            processThumbnailStarted(imageIndex, thumbnailIndex);
        }

        public void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
            processPassStarted(theImage, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
        }

        public void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
            processImageUpdate(theImage, minX, minY, width, height, periodX, periodY, bands);
        }

        public void passComplete(ImageReader source, BufferedImage theImage) {
            processPassComplete(theImage);
        }

        public void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
            processThumbnailPassStarted(theThumbnail, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
        }

        public void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
            processThumbnailUpdate(theThumbnail, minX, minY, width, height, periodX, periodY, bands);
        }

        public void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail) {
            processThumbnailPassComplete(theThumbnail);
        }

        public void warningOccurred(ImageReader source, String warning) {
            processWarningOccurred(warning);
        }
    }
}
