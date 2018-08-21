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

package com.twelvemonkeys.imageio.plugins.dng;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.LittleEndianDataInputStream;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Adobe Digital Negative DNG ImageReader.
 * <p/>
 *
 * @see <a href="http://wwwimages.adobe.com/content/dam/Adobe/en/products/photoshop/pdfs/dng_spec_1.4.0.0.pdf">Digital Negative (DNG) Specification</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DNGImageReader.java,v 1.0 07.04.14 21:31 haraldk Exp$
 */
public final class DNGImageReader extends ImageReaderBase {
    // SEE: http://wwwimages.adobe.com/content/dam/Adobe/en/products/photoshop/pdfs/dng_spec_1.4.0.0.pdf
    // TODO: Avoid duped code from TIFFImageReader
    // TODO: DNG is tightly tied to TIFF/EP. Would it make more sense to include all the functionality into the TIFFImageReader?
    // TODO: Probably a good idea to move some of the getAsShort/Int/Long/Array to TIFF/EXIF metadata module
    // TODO: Automatic EXIF rotation, if we find a good way to do that for JPEG/EXIF/TIFF and keeping the metadata sane...

    static final boolean DEBUG =  true; //"true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.dng.debug"));

    /** Somewhat arbitrary, but it's the current "largest" icon size for Apple icons, AppStore and Google Play. */
    private static final int MAX_THUMBNAIL_SIZE = 512;

    private static final ImageTypeSpecifier THUMB_SPEC = ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{0, 1, 2}, DataBuffer.TYPE_BYTE, false, false);

    private CompoundDirectory IFDs;
    private List<Directory> subIFDs;
    private Directory currentIFD;

    private int thumbnailIFD = -1;

    DNGImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        IFDs = null;
        subIFDs = null;
        thumbnailIFD = -1;

        currentIFD = null;
    }

    private void readMetadata() throws IOException {
        if (imageInput == null) {
            throw new IllegalStateException("input not set");
        }

        if (IFDs == null) {
            imageInput.seek(0);

            IFDs = (CompoundDirectory) new TIFFReader().read(imageInput); // NOTE: Sets byte order as a side effect

            // Pull up the sub-ifds now, as the DNG spec "recommends the use of SubIFD trees,
            // as described in the TIFF-EP specification. SubIFD chains are not supported".
            Entry subIFDEntry = IFDs.getEntryById(TIFF.TAG_SUB_IFD);

            if (subIFDEntry != null) {
                Object subIFD = subIFDEntry.getValue();

                if (subIFD instanceof Directory) {
                    subIFDs = Collections.singletonList((Directory) subIFD);
                }
                else {
                    Directory[] directories = (Directory[]) subIFD;
                    subIFDs = Arrays.asList(directories);
                }
            }
            else {
                subIFDs = Collections.emptyList();
            }

            // Find which, if any, (sub-) IFD contains the thumbnail
            // (bit 1 is set also for the JPEG preview, that is too large for a thumbnail)
            currentIFD = IFDs.getDirectory(0);
            determineThumbnailIFD(0); // Most likely, it's in IFD0

            for (int i = 0; i < subIFDs.size(); i++) {
                if (thumbnailIFD != -1) {
                    break;
                }

                currentIFD = subIFDs.get(i);
                determineThumbnailIFD(i + 1);
            }

            if (DEBUG) {
                System.err.println("Byte order: " + imageInput.getByteOrder());
                System.err.println("Number of IFDs: " + IFDs.directoryCount());

                for (int i = 0; i < IFDs.directoryCount(); i++) {
                    System.err.printf("IFD %d: %s\n", i, IFDs.getDirectory(i));
                }

                System.err.println("thumbnailIFD: " + thumbnailIFD);
            }
        }
    }

    private void determineThumbnailIFD(final int thumbnailIFD1) throws IIOException {
        // Look at TIFF.TAG_SUBFILE_TYPE. If the first bit is 1, this is a "reduced" version,
        // that MIGHT be the thumbnail (typical value is 1).
        // TODO: There could be more thumbnails, using the value 0x10001 ("alternate preview").
        Entry subFileType = currentIFD.getEntryById(TIFF.TAG_SUBFILE_TYPE);

        if (subFileType != null && ((Number) subFileType.getValue()).intValue() == 1) {
            int imageWidth = getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
            int imageHeight= getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");

            // Use heuristic: h & w <= 512 --> thumbnail
            if (imageWidth <= MAX_THUMBNAIL_SIZE && imageHeight <= MAX_THUMBNAIL_SIZE) {
                thumbnailIFD = thumbnailIFD1;
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

        int numIFDs = IFDs.directoryCount() + subIFDs.size();

        return thumbnailIFD != -1 ? numIFDs - 1 : numIFDs;
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        return imageIndex == 0 && thumbnailIFD >= 0 ? 1 : 0;
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(thumbnailIFD != -1 ? thumbnailIFD : 0); // Avoid reading bad thumbnail index, but still doing proper verifications

        if (imageIndex != 0 || thumbnailIFD == -1) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(thumbnailIFD != -1 ? thumbnailIFD : 0); // Avoid reading bad thumbnail index, but still doing proper verifications

        if (imageIndex != 0 || thumbnailIFD == -1) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(thumbnailIFD != -1 ? thumbnailIFD : 0); // Avoid reading bad thumbnail index, but still doing proper verifications

        if (imageIndex != 0 || thumbnailIFD == -1) {
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

    private int[] getValueAsIntArrayWithDefault(final int tag, final String tagName, int[] defaultValue) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);
        if (entry == null) {
            if (defaultValue == null) {
                throw new IIOException("Missing TIFF tag " + tagName);
            }

            return defaultValue;
        }

        int[] value;

        if (entry.valueCount() == 1) {
            // For single entries, this will be a boxed type
            value = new int[] {((Number) entry.getValue()).intValue()};
        }
        else if (entry.getValue() instanceof short[]) {
            short[] shorts = (short[]) entry.getValue();
            value = new int[shorts.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = shorts[i];
            }
        }
        else if (entry.getValue() instanceof int[]) {
            value = (int[]) entry.getValue();
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

    private int imageIndexToIFDNumber(int imageIndex) throws IOException {
        readMetadata();

        return thumbnailIFD != -1 && imageIndex >= thumbnailIFD ? imageIndex + 1 : imageIndex;
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
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        // TODO: For compression 7/34892, get from JPEGImageReader delegate?

        int photometricInterpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
        int samplesPerPixel = getValueAsInt(TIFF.TAG_SAMPLES_PER_PIXEL, "SamplesPerPixel");
        long[] bitsPerSample = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", true);
        int bitDepth = (int) bitsPerSample[0]; // Assume all equal!


        switch (photometricInterpretation) {
            case 1: // BlackIsZero
                if (samplesPerPixel == 1 && bitDepth == 8) {
                    return ImageTypeSpecifier.createGrayscale(bitDepth, DataBuffer.TYPE_BYTE, false);
                }
                else if (samplesPerPixel == 1 && bitDepth > 8 && bitDepth <= 16) {
                    return ImageTypeSpecifier.createGrayscale(bitDepth, DataBuffer.TYPE_USHORT, false);
                }

                break;

            case 2: // RGB
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

                if (samplesPerPixel == 3 && bitDepth == 8) {
                    return ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false);
                }
                else if (samplesPerPixel == 3 && bitDepth > 8 && bitDepth <= 16) {
                    return ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false);
                }

                break;

            case DNG.PHOTOMETRIC_CFA: // CFA
                // Return null as there really isn't a good Java ColorSpace for it...
                if (samplesPerPixel == 1) {
                   return null;
                }

                break;

            default:
                throw new IIOException("Unsupported photometricInterpretation: " + photometricInterpretation);
        }

        throw new IIOException(String.format("Unsupported bitsPerSample/photometricInterpretation: %s/%s", Arrays.toString(bitsPerSample), photometricInterpretation));
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        // TODO: For compression 7/34892, get from JPEGImageReader delegate?

        ImageTypeSpecifier rawImageType = getRawImageType(imageIndex);

        int photometricInterpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
        long[] bitsPerSample = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", true);
        int bitDepth = (int) bitsPerSample[0]; // Assume all equal!


        List<ImageTypeSpecifier> specs = new ArrayList<ImageTypeSpecifier>();

        switch (photometricInterpretation) {
            case 1: // BlackIsZero
            case 2: // RGB
                specs.add(rawImageType);
                break;

            case DNG.PHOTOMETRIC_CFA: // CFA
                // Will be expanded to RGB
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

                if (bitDepth == 8) {
                    specs.add(ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false));
                }
                else if (bitDepth > 8 && bitDepth <= 16) {
                    specs.add(ImageTypeSpecifier.createInterleaved(cs, new int [] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false));
                }

                // TODO: Support reading as raw grayscale as well...
                specs.add(rawImageType);

                break;
            default:
                throw new IIOException("Unsupported photometricInterpretation: " + photometricInterpretation);
        }

        return specs.iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        // TODO: Look at TIFF.TAG_SUBFILE_TYPE,
        // TODO: If subFileType == 0, this is the main image,
        // otherwise, it's a "reduced" (compressed or smaller dimensions) version
        // (bit 1 is set also for the JPEG preview, that is too large for a thumbnail) or Page/Mask
        // (not sure if these are supported for DNG)
        // See: http://www.awaresystems.be/imaging/tiff/tifftags/newsubfiletype.html
//            FILETYPE_REDUCEDIMAGE = 1;
//            FILETYPE_PAGE = 2;
//            FILETYPE_MASK = 4;

        // DNG spec:
        //  The highest-resolution and quality IFD should use NewSubFileType equal to 0. Reduced
        //  resolution (or quality) thumbnails or previews, if any, should use NewSubFileType equal to 1
        //  (for a primary preview) or 10001.H (for an alternate preview).


        int width = getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
        int height = getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");


        // TODO: Support compressions 1 (None), 7 (JPEG), 8 (Deflate) and 34892 (Lossy JPEG, only for PhotometricInterpretation = 34892/Linear RAW)
        int compression = getValueAsInt(TIFF.TAG_COMPRESSION, "Compression");
        int photometricInterpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");

//        System.err.println("compression: " + compression);
//        System.err.println("photometricInterpretation: " + photometricInterpretation);

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

        switch (compression) {
            case 8:
                // Deflate (handled below)
            case 1:
                // NONE
                // TODO: Read as uncompressed TIFF (share code with TIFFImageReader?)
                // TODO: Remove duped code!!
                BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
                BufferedImage temp;

                ImageTypeSpecifier rawType = getRawImageType(imageIndex);

                if (photometricInterpretation == DNG.PHOTOMETRIC_CFA) {
                    // If CFA, read as single channel (gray), expand to RGB after reading
                    long[] bitsPerSample = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", true);
                    int bitDepth = (int) bitsPerSample[0]; // Assume all equal!

                    checkReadParamBandSettings(param, 3, destination.getSampleModel().getNumBands());

                    if (bitDepth == 8) {
                        rawType = ImageTypeSpecifier.createGrayscale(bitDepth, DataBuffer.TYPE_BYTE, false);
                    }
                    else if (bitDepth > 8 && bitDepth <= 16) {
                        rawType = ImageTypeSpecifier.createGrayscale(bitDepth, DataBuffer.TYPE_USHORT, false);
                    }

                    temp = rawType.createBufferedImage(destination.getWidth(), destination.getHeight());
                }
                else {
                    checkReadParamBandSettings(param, rawType.getNumBands(), destination.getSampleModel().getNumBands());
                    temp = destination;
                }

                final Rectangle srcRegion = new Rectangle();
                final Rectangle dstRegion = new Rectangle();
                computeRegions(param, width, height, destination, srcRegion, dstRegion);

                int xSub = param != null ? param.getSourceXSubsampling() : 1;
                int ySub = param != null ? param.getSourceYSubsampling() : 1;

//                WritableRaster destRaster = clipToRect(destination.getRaster(), dstRegion, param != null ? param.getDestinationBands() : null);
                WritableRaster destRaster = clipToRect(temp.getRaster(), dstRegion, param != null ? param.getDestinationBands() : null);

                final int interpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
                final int predictor = getValueAsIntWithDefault(TIFF.TAG_PREDICTOR, 1);
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

                        DataInput input;
                        if (compression == 1 /*&& interpretation != TIFFExtension.PHOTOMETRIC_YCBCR*/) {
                            // No need for transformation, fast forward
                            input = imageInput;
                        }
                        else {
                            InputStream adapter = stripTileByteCounts != null
                                    ? IIOUtil.createStreamAdapter(imageInput, stripTileByteCounts[i])
                                    : IIOUtil.createStreamAdapter(imageInput);

                            adapter = createDecompressorStream(compression, width, adapter);
                            adapter = createUnpredictorStream(predictor, width, numBands, getBitsPerSample(), adapter, imageInput.getByteOrder());

                            // According to the spec, short/long/etc should follow order of containing stream
                            input = imageInput.getByteOrder() == ByteOrder.BIG_ENDIAN
                                    ? new DataInputStream(adapter)
                                    : new LittleEndianDataInputStream(adapter);
                        }

                        // Read a full strip/tile
                        Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                                param != null ? param.getSourceBands() : null,
                                param != null ? param.getSourceXSubsampling() : 1);
                        readStripTileData(clippedRow, srcRegion, xSub, ySub, numBands, interpretation, destRaster, col, row, colsInTile, rowsInTile, input);

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

                if (photometricInterpretation == DNG.PHOTOMETRIC_CFA) {
                    int layout = getValueAsIntWithDefault(DNG.TAG_CFA_LAYOUT, "CFALayout", 1);
                    int[] planeColor = getValueAsIntArrayWithDefault(DNG.TAG_CFA_PLANE_COLOR, "CFAPlaneColor", new int[]{0, 1, 2});
                    Entry cfaPatternDim = currentIFD.getEntryById(DNG.TAG_CFA_REPEAT_PATTERN_DIM);
                    if (cfaPatternDim == null || cfaPatternDim.valueCount() != 2 || !(cfaPatternDim.getValue() instanceof int[])) {
                        throw new IIOException("Missing/bad CFARepeatPatternDim tag for CFA DNG: " + cfaPatternDim);
                    }

                    int patternWidth = ((int[]) cfaPatternDim.getValue())[0];
                    int patternHeight = ((int[]) cfaPatternDim.getValue())[1];

                    Entry cfaPattern = currentIFD.getEntryById(DNG.TAG_CFA_PATTERN);
                    if (cfaPattern == null || cfaPattern.valueCount() != patternWidth * patternHeight || !(cfaPattern.getValue() instanceof byte[])) {
                        throw new IIOException("Missing/bad CFAPattern tag for CFA DNG: " + cfaPattern);
                    }

                    byte[] pattern = (byte[]) cfaPattern.getValue();

                    interpolateCFA2RGB(temp.getRaster(), destination.getRaster(), patternWidth, patternHeight, pattern, planeColor, layout);
                }

                return destination;
            case 7:
            case DNG.COMPRESSION_LOSSY_JPEG:
                // JPEG
                if (photometricInterpretation == 6 || photometricInterpretation == DNG.PHOTOMETRIC_LINEAR_RAW) {
                    // TODO: Merge strips/tiles into one image...
                    // "TIFF/EP_1 uses the TIFF/JPEG specification as described in
                    // Adobe Photoshop: TIFF Technical Notes (March 22, 2002).
                    // This method differs from the JPEG method described in the original TIFF 6.0 specification.
                    // In the method used within TIFF/EP_1, each image segment (tile or strip) contains a
                    // complete JPEG data stream that is valid according to the ISO JPEG standard (ISO/IEC 10918-1)."
                    for (int k = 0; k < stripTileOffsets.length; k++) {
                        imageInput.seek(stripTileOffsets[k]);

                        SubImageInputStream stream = new SubImageInputStream(imageInput, stripTileByteCounts[k]);
                        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream); // TODO: Prefer default JPEGImageReader
                        if (!readers.hasNext()) {
                            throw new IIOException("Could not find delegate reader for JPEG format!");
                        }

                        ImageReader reader = readers.next();

                        try {
                            if (param == null) {
                                param = reader.getDefaultReadParam();
                            }

                            reader.setInput(stream);
                            return reader.read(0, param);
                        }
                        finally {
                            reader.dispose(); // TODO: Don't dispose until this instance is disposed
                        }
                    }
                }
                else if (photometricInterpretation == DNG.PHOTOMETRIC_CFA) {
                    // Otherwise, this is lossless encoded linear or CFA
                    // TODO: Read JPEG Lossless!

                    return param != null ? param.getDestination() : null;
                }
                throw new IIOException("Unsupported photometricInterpretation for JPEG compressed DNG: " + photometricInterpretation);

            default:
                throw new IIOException("Unsupported compression for DNG: " + compression);
        }
    }

    private void interpolateCFA2RGB(final Raster cfa, final WritableRaster rgb,
                                    final int patternWidth, final int patternHeight, final byte[] pattern,
                                    final int[] planeColor, final int layout) {
        if (DEBUG) {
            System.err.println("patternWidth: " + patternWidth);
            System.err.println("patternHeight: " + patternHeight);
            System.err.println("pattern: " + Arrays.toString(pattern));
            System.err.println("planeColor: " + Arrays.toString(planeColor));
            System.err.println("layout: " + layout);
        }

        // Expand CFA to RGB (for now, using nearest neighbour type interpolation)
        // TODO: Properly interpolate (in some way).
        // TODO: What interpolation works best, depends on pattern/sensor/scene... (see dcraw?)

        byte[] cfaData = new byte[patternWidth * patternHeight];
        byte[] rgbData = new byte[patternWidth * patternHeight * 3];

        for (int y = 0; y < rgb.getHeight(); y += patternHeight) {
            for (int x = 0; x < rgb.getWidth(); x += patternWidth) {
                cfa.getDataElements(x, y, patternWidth, patternHeight, cfaData);

                // TODO: Take layout into consideration (for now, assume default: 1)

                for (int patternY = 0; patternY < patternHeight; patternY++) {
                    for (int patternX = 0; patternX < patternWidth; patternX++) {
                        int patternIndex = patternX + patternY * patternWidth;

                        int plane = pattern[patternIndex];

                        // TODO: This doesn't work properly for non-standard or non-2x2 patterns...
                        // How do we know how many times each plane/color should be repeated?
                        switch (planeColor[plane]) {
                            case 0: // Red
                                rgbData[0] = cfaData[patternIndex]; // *
                                rgbData[3] = cfaData[patternIndex];
                                rgbData[6] = cfaData[patternIndex];
                                rgbData[9] = cfaData[patternIndex];
                                break;
                            case 1: // Green
                                if (patternY == 0) {
                                    rgbData[1] = cfaData[patternIndex];
                                    rgbData[4] = cfaData[patternIndex]; // *
                                }
                                else {
                                    rgbData[7] = cfaData[patternIndex]; // *
                                    rgbData[10] = cfaData[patternIndex];
                                }
                                break;
                            case 2: // Blue
                                rgbData[2] = cfaData[patternIndex];
                                rgbData[5] = cfaData[patternIndex];
                                rgbData[8] = cfaData[patternIndex];
                                rgbData[11] = cfaData[patternIndex]; // *
                                break;
                        }

                    }
                }

                rgb.setDataElements(x, y, patternWidth, patternHeight, rgbData);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DNGImageReader reader = new DNGImageReader(new DNGImageReaderSpi());

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
