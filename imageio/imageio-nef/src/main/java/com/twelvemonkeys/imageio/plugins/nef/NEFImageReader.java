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

package com.twelvemonkeys.imageio.plugins.nef;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Nikon NEF RAW ImageReader
 * <p/>
 * Acknowledgement:
 * This ImageReader is based on the excellent work of Laurent Clevy, and would probably not exist without it.
 *
 * @see <a href="http://lclevy.free.fr/nef/">Nikon Electronic File (NEF) file format description</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: NEFImageReader.java,v 1.0 07.04.14 21:31 haraldk Exp$
 */
public final class NEFImageReader extends ImageReaderBase {
    // See http://lclevy.free.fr/nef/
    // TODO: Avoid duped code from TIFFImageReader
    // TODO: Probably a good idea to move some of the getAsShort/Int/Long/Array to TIFF/EXIF metadata module
    // TODO: Automatic EXIF rotation, if we find a good way to do that for JPEG/EXIF/TIFF and keeping the metadata sane...

    static final boolean DEBUG =  true; //"true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.nef.debug"));
    private static final ImageTypeSpecifier THUMB_SPEC = ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{0, 1, 2}, DataBuffer.TYPE_BYTE, false, false);

    // The Makernote has the NikonImagePreview tag (0x0011) which contains a thumbnail image (in lossy jpeg).
    // IFD#0 also contains a thumbnail image in uncompressed TIFF, size is 160x120.
    // Thumbnail is always in IFD0..
    private final static int THUMBNAIL_IFD = 0;

    private CompoundDirectory IFDs;
    private List<Directory> subIFDs;
    private Directory currentIFD;

    NEFImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        IFDs = null;
        subIFDs = null;
        currentIFD = null;
    }

    private void readMetadata() throws IOException {
        if (imageInput == null) {
            throw new IllegalStateException("input not set");
        }

        if (IFDs == null) {
            imageInput.seek(0);

            IFDs = (CompoundDirectory) new TIFFReader().read(imageInput); // NOTE: Sets byte order as a side effect

            // Pull up the sub-ifds now
            Entry subIFDEntry = IFDs.getEntryById(TIFF.TAG_SUB_IFD);

            if (subIFDEntry != null) {
                Object subIFD = subIFDEntry.getValue();

                if (subIFD instanceof Directory) {
                    subIFDs = Collections.singletonList((Directory) subIFD);
                }
                else {
                    Directory[] directories = (Directory[]) subIFD;

                    if (directories.length != 2) {
                        throw new IIOException("Unexpected number of SubIFDs in NEF: " + directories.length);
                    }

                    subIFDs = Arrays.asList(directories);
                }
            }
            else {
                throw new IIOException("Unexpected number of SubIFDs in NEF: " + 0);
            }

            if (DEBUG) {
                System.err.println("Byte order: " + imageInput.getByteOrder());
                System.err.println("Number of IFDs: " + IFDs.directoryCount());

                for (int i = 0; i < IFDs.directoryCount(); i++) {
                    System.err.printf("IFD %d: %s\n", i, IFDs.getDirectory(i));
                }
            }
        }
    }

    private void readIFD(final int ifdIndex) throws IOException {
        readMetadata();

        if (ifdIndex < 0) {
            throw new IndexOutOfBoundsException("index < minIndex");
        }
        else {
            int numIFDs = IFDs.directoryCount() + subIFDs.size();

            if (ifdIndex >= numIFDs) {
                throw new IndexOutOfBoundsException("index >= numIFDs (" + ifdIndex + " >= " + numIFDs + ")");
            }
        }

        // Depth first (...but a DNG should only contain one IFD with subIFDs)
        if (ifdIndex == 0) {
            currentIFD = IFDs.getDirectory(ifdIndex);
        }
        else if (ifdIndex <= subIFDs.size()) {
            currentIFD = subIFDs.get(ifdIndex - 1);
        }
        else {
            currentIFD = IFDs.getDirectory(ifdIndex - subIFDs.size());
        }
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        readMetadata();

        return subIFDs.size(); // IFD0 is always thumbnail
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        return imageIndex == 0 ? 1 : 0;
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(THUMBNAIL_IFD);

        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(THUMBNAIL_IFD);

        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(THUMBNAIL_IFD);

        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        // Read uncompressed RGB
        int imageWidth = getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
        int imageHeight = getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");

        // NEF thumbnail simplification: single strip
        long stripOffset = getValueAsLong(TIFF.TAG_STRIP_OFFSETS, "StripOffsets");
        long stripCount = getValueAsLong(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts");

        BufferedImage thumbnail = THUMB_SPEC.createBufferedImage(imageWidth, imageHeight);

        WritableRaster raster = thumbnail.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();

        imageInput.seek(stripOffset);
        ImageInputStream stream = new SubImageInputStream(imageInput, stripCount);

        try {
            stream.readFully(dataBuffer.getData());
        }
        finally {
            stream.close();
        }

        return thumbnail;
    }

    private long[] getValueAsLongArray(final int tag, final String tagName, boolean required) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);
        if (entry == null) {
            if (required) {
                throw new IIOException("Missing TIFF tag " + tagName);
            }

            return null;
        }

        long[] value;

        if (entry.valueCount() == 1) {
            // For single entries, this will be a boxed type
            value = new long[] {((Number) entry.getValue()).longValue()};
        }
        else if (entry.getValue() instanceof short[]) {
            short[] shorts = (short[]) entry.getValue();
            value = new long[shorts.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = shorts[i];
            }
        }
        else if (entry.getValue() instanceof int[]) {
            int[] ints = (int[]) entry.getValue();
            value = new long[ints.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = ints[i];
            }
        }
        else if (entry.getValue() instanceof long[]) {
            value = (long[]) entry.getValue();
        }
        else {
            throw new IIOException(String.format("Unsupported %s type: %s (%s)", tagName, entry.getTypeName(), entry.getValue().getClass()));
        }

        return value;
    }

    private Number getValueAsNumberWithDefault(final int tag, final String tagName, final Number defaultValue) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);

        if (entry == null) {
            if (defaultValue != null)  {
                return defaultValue;
            }

            throw new IIOException("Missing TIFF tag: " + (tagName != null ? tagName : tag));
        }

        return (Number) entry.getValue();
    }

    private long getValueAsLongWithDefault(final int tag, final String tagName, final Long defaultValue) throws IIOException {
        return getValueAsNumberWithDefault(tag, tagName, defaultValue).longValue();
    }

    private long getValueAsLongWithDefault(final int tag, final Long defaultValue) throws IIOException {
        return getValueAsLongWithDefault(tag, null, defaultValue);
    }

    private long getValueAsLong(final int tag, String tagName) throws IIOException {
        return getValueAsLongWithDefault(tag, tagName, null);
    }

    private int getValueAsIntWithDefault(final int tag, final String tagName, final Integer defaultValue) throws IIOException {
        return getValueAsNumberWithDefault(tag, tagName, defaultValue).intValue();
    }

    private int getValueAsIntWithDefault(final int tag, Integer defaultValue) throws IIOException {
        return getValueAsIntWithDefault(tag, null, defaultValue);
    }

    private int getValueAsInt(final int tag, String tagName) throws IIOException {
        return getValueAsIntWithDefault(tag, tagName, null);
    }

    private int imageIndexToIFDNumber(int imageIndex) {
        return imageIndex >= THUMBNAIL_IFD ? imageIndex + 1 : imageIndex;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        int photometricInterpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
        long[] bitsPerSample = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", true);
        int bitDepth = (int) bitsPerSample[0]; // Assume all equal!

        ColorSpace cs;

        if (photometricInterpretation == 2) {
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        else {
            cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        }

        if (bitDepth == 8) {
            return Arrays.asList(ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false)).iterator();
        }
        else if (bitDepth > 8 && bitDepth <= 16) {
            return Arrays.asList(ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false)).iterator();
        }

        throw new IIOException("Unsupported bit depth: " + bitDepth);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        int compression = getValueAsInt(TIFF.TAG_COMPRESSION, "Compression");
        int width;
        int height;

        ImageInputStream stream;
        switch (compression) {
            case 1: // Uncompressed
                width = getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
                height = getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");

                // TODO: Read as uncompressed TIFF (share code with TIFFImageReader?)
                // TODO: Remove duped code!!
                BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

                // NOTE: We handle strips as tiles of tileWidth == width by tileHeight == rowsPerStrip
                //       Strips are top/down, tiles are left/right, top/down
                int stripTileWidth = width;
                long rowsPerStrip = getValueAsLongWithDefault(TIFF.TAG_ROWS_PER_STRIP, (1l << 32) - 1);
                int stripTileHeight = rowsPerStrip < height ? (int) rowsPerStrip : height;

                long[] stripTileOffsets = getValueAsLongArray(TIFF.TAG_TILE_OFFSETS, "TileOffsets", false);
                long[] stripTileByteCounts;

                if (stripTileOffsets != null) {
                    stripTileByteCounts = getValueAsLongArray(TIFF.TAG_TILE_BYTE_COUNTS, "TileByteCounts", false);
                    if (stripTileByteCounts == null) {
                        processWarningOccurred("Missing TileByteCounts for tiled TIFF with compression: " + compression);
                    }

                    stripTileWidth = getValueAsInt(TIFF.TAG_TILE_WIDTH, "TileWidth");
                    stripTileHeight = getValueAsInt(TIFF.TAG_TILE_HEIGTH, "TileHeight");
                }
                else {
                    stripTileOffsets = getValueAsLongArray(TIFF.TAG_STRIP_OFFSETS, "StripOffsets", true);
                    stripTileByteCounts = getValueAsLongArray(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts", false);
                    if (stripTileByteCounts == null) {
                        processWarningOccurred("Missing StripByteCounts for TIFF with compression: " + compression);
                    }

                    // NOTE: This is really against the spec, but libTiff seems to handle it. TIFF 6.0 says:
                    //       "Do not use both strip- oriented and tile-oriented fields in the same TIFF file".
                    stripTileWidth = getValueAsIntWithDefault(TIFF.TAG_TILE_WIDTH, "TileWidth", stripTileWidth);
                    stripTileHeight = getValueAsIntWithDefault(TIFF.TAG_TILE_HEIGTH, "TileHeight", stripTileHeight);
                }

                int tilesAcross = (width + stripTileWidth - 1) / stripTileWidth;
                int tilesDown = (height + stripTileHeight - 1) / stripTileHeight;



                ImageTypeSpecifier rawType = getRawImageType(imageIndex);
                checkReadParamBandSettings(param, rawType.getNumBands(), destination.getSampleModel().getNumBands());

                final Rectangle srcRegion = new Rectangle();
                final Rectangle dstRegion = new Rectangle();
                computeRegions(param, width, height, destination, srcRegion, dstRegion);

                int xSub = param != null ? param.getSourceXSubsampling() : 1;
                int ySub = param != null ? param.getSourceYSubsampling() : 1;

                WritableRaster destRaster = clipToRect(destination.getRaster(), dstRegion, param != null ? param.getDestinationBands() : null);

                final int interpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
                final int predictor = getValueAsIntWithDefault(TIFF.TAG_PREDICTOR, 1);
//                final int planarConfiguration = getValueAsIntWithDefault(TIFF.TAG_PLANAR_CONFIGURATION, TIFFBaseline.PLANARCONFIG_CHUNKY);
//                final int numBands = planarConfiguration == TIFFExtension.PLANARCONFIG_PLANAR ? 1 : rawType.getNumBands();
                final int numBands = rawType.getNumBands();

                WritableRaster rowRaster = rawType.getColorModel().createCompatibleWritableRaster(stripTileWidth, 1);
                int row = 0;

                // General uncompressed/compressed reading
                for (int y = 0; y < tilesDown; y++) {
                    int col = 0;
                    int rowsInTile = Math.min(stripTileHeight, height - row);

                    for (int x = 0; x < tilesAcross; x++) {
                        int colsInTile = Math.min(stripTileWidth, width - col);
                        int i = y * tilesAcross + x;

                        imageInput.seek(stripTileOffsets[i]);

                        // Read a full strip/tile
                        Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                                param != null ? param.getSourceBands() : null,
                                param != null ? param.getSourceXSubsampling() : 1);
                        readStripTileData(clippedRow, srcRegion, xSub, ySub, numBands, interpretation, destRaster, col, row, colsInTile, rowsInTile, imageInput);

                        if (abortRequested()) {
                            break;
                        }

                        col += colsInTile;
                    }

                    processImageProgress(100f * row / height);

                    if (abortRequested()) {
                        processReadAborted();
                        break;
                    }

                    row += rowsInTile;
                }
                return getDestination(param, getImageTypes(imageIndex), width, height);

            case 6: // Old-style JPEG
                long jpegFormatStart = getValueAsLong(TIFF.TAG_JPEG_INTERCHANGE_FORMAT, "JPEGInterchangeFormat");
                long jpegFormatLength = getValueAsLong(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, "JPEGInterchangeFormatLength");
                imageInput.seek(jpegFormatStart);

                stream = new SubImageInputStream(imageInput, jpegFormatLength);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream); // TODO: Prefer default JPEGImageReader
                if (!readers.hasNext()) {
                    throw new IIOException("Could not find delegate reader for JPEG format!");
                }

                ImageReader reader = readers.next();

                try {
                    reader.setInput(stream);
                    return reader.read(0, param);
                }
                finally {
                    reader.dispose(); // TODO: Don't dispose until this instance is disposed
                }

            case 34713: // Nikon NEF compressed
                width = getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
                height = getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");

                // TODO: Read Nikon NEF compressed RAW

                return param != null ? param.getDestination() : null;

            default:
                throw new IIOException("Unsupported compression for NEF: " + compression);
        }
    }

    public static void main(String[] args) throws IOException {
        NEFImageReader reader = new NEFImageReader(new NEFImageReaderSpi());

        for (String arg : args) {
            ImageInputStream stream = ImageIO.createImageInputStream(new File(arg));

            reader.setInput(stream);

            int numImages = reader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                int numThumbnails = reader.getNumThumbnails(i);
                for (int n = 0; n < numThumbnails; n++) {
                    showIt(reader.readThumbnail(i, n), arg + " image thumbnail" + n);
                }

                showIt(reader.read(i), arg + " image " + i);
            }
        }
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DUPED CODE BELOW //// DUPED CODE BELOW //// DUPED CODE BELOW //// DUPED CODE BELOW //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private int getBitsPerSample() throws IIOException {
        long[] value = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", false);

        if (value == null || value.length == 0) {
            return 1;
        }
        else {
            int bitsPerSample = (int) value[0];

            for (int i = 1; i < value.length; i++) {
                if (value[i] != bitsPerSample) {
                    throw new IIOException("Variable BitsPerSample not supported: " + Arrays.toString(value));
                }
            }

            return bitsPerSample;
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

    private void readStripTileData(final Raster tileRowRaster, final Rectangle srcRegion, final int xSub, final int ySub,
                                   final int numBands, final int interpretation,
                                   final WritableRaster raster, final int startCol, final int startRow,
                                   final int colsInTile, final int rowsInTile, final DataInput input)
            throws IOException {

        switch (tileRowRaster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                byte[] rowDataByte = ((DataBufferByte) tileRowRaster.getDataBuffer()).getData();

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    input.readFully(rowDataByte);

                    if (row % ySub == 0 && row >= srcRegion.y) {
                        normalizeBlack(interpretation, rowDataByte);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + srcRegion.width) / xSub) * numBands; x += numBands) {
                                for (int b = 0; b < numBands; b++) {
                                    rowDataByte[x + b] = rowDataByte[x * xSub + b];
                                }
                            }
                        }

                        raster.setDataElements(startCol, (row - srcRegion.y) / ySub, tileRowRaster);
                    }
                    // Else skip data
                }

                break;
            case DataBuffer.TYPE_USHORT:
                short[] rowDataShort = ((DataBufferUShort) tileRowRaster.getDataBuffer()).getData();

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    readFully(input, rowDataShort);

                    if (row >= srcRegion.y) {
                        normalizeBlack(interpretation, rowDataShort);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + srcRegion.width) / xSub) * numBands; x += numBands) {
                                for (int b = 0; b < numBands; b++) {
                                    rowDataShort[x + b] = rowDataShort[x * xSub + b];
                                }
                            }
                        }

                        raster.setDataElements(startCol, row - srcRegion.y, tileRowRaster);
                    }
                    // Else skip data
                }

                break;
            case DataBuffer.TYPE_INT:
                int[] rowDataInt = ((DataBufferInt) tileRowRaster.getDataBuffer()).getData();

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    readFully(input, rowDataInt);

                    if (row >= srcRegion.y) {
                        normalizeBlack(interpretation, rowDataInt);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + srcRegion.width) / xSub) * numBands; x += numBands) {
                                for (int b = 0; b < numBands; b++) {
                                    rowDataInt[x + b] = rowDataInt[x * xSub + b];
                                }
                            }
                        }

                        raster.setDataElements(startCol, row - srcRegion.y, tileRowRaster);
                    }
                    // Else skip data
                }

                break;
        }
    }

    // TODO: Candidate util method (with off/len + possibly byte order)
    private void readFully(final DataInput input, final int[] rowDataInt) throws IOException {
        if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.readFully(rowDataInt, 0, rowDataInt.length);
        }
        else {
            for (int k = 0; k < rowDataInt.length; k++) {
                rowDataInt[k] = input.readInt();
            }
        }
    }

    // TODO: Candidate util method (with off/len + possibly byte order)
    private void readFully(final DataInput input, final short[] rowDataShort) throws IOException {
        if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.readFully(rowDataShort, 0, rowDataShort.length);
        }
        else {
            for (int k = 0; k < rowDataShort.length; k++) {
                rowDataShort[k] = input.readShort();
            }
        }
    }

    private void normalizeBlack(int photometricInterpretation, short[] data) {
        if (photometricInterpretation == 0 /*TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO*/) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (short) (0xffff - data[i] & 0xffff);
            }
        }
    }

    private void normalizeBlack(int photometricInterpretation, int[] data) {
        if (photometricInterpretation == 0 /*TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO*/) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (0xffffffff - data[i]);
            }
        }
    }

    private void normalizeBlack(int photometricInterpretation, byte[] data) {
        if (photometricInterpretation == 0/*TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO*/) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (0xff - data[i] & 0xff);
            }
        }
    }

    private InputStream createDecompressorStream(final int compression, final int width, final InputStream stream) throws IOException {
        switch (compression) {
//            case TIFFBaseline.COMPRESSION_NONE:
            case 1:
                return stream;
//            case TIFFBaseline.COMPRESSION_PACKBITS:
//            case TIFFExtension.COMPRESSION_ZLIB:
            case 8:
                // TIFFphotoshop.pdf (aka TIFF specification, supplement 2) says ZLIB (8) and DEFLATE (32946) algorithms are identical
                return new InflaterInputStream(stream, new Inflater(), 1024);
            default:
                throw new IllegalArgumentException("Unsupported TIFF compression: " + compression);
        }
    }

    private InputStream createUnpredictorStream(final int predictor, final int width, final int samplesPerPixel, final int bitsPerSample, final InputStream stream, final ByteOrder byteOrder) throws IOException {
        switch (predictor) {
//            case TIFFBaseline.PREDICTOR_NONE:
            case 1:
                return stream;
//            case TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING:
            case 2:
//                return new HorizontalDeDifferencingStream(stream, width, samplesPerPixel, bitsPerSample, byteOrder);
//            case TIFFExtension.PREDICTOR_HORIZONTAL_FLOATINGPOINT:
            case 3:
                throw new IIOException("Unsupported TIFF Predictor value: " + predictor);
            default:
                throw new IIOException("Unknown TIFF Predictor value: " + predictor);
        }
    }
}

