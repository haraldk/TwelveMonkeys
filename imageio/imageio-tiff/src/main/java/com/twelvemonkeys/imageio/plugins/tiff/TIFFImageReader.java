/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.Rational;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;

import javax.imageio.*;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * ImageReader implementation for Aldus/Adobe Tagged Image File Format (TIFF).
 * <p/>
 * The reader is supposed to be fully "Baseline TIFF" compliant, and supports the following image types:
 * <ul>
 *     <li>Class B (Bi-level), all relevant compression types, 1 bit per sample</li>
 *     <li>Class G (Gray), all relevant compression types, 2, 4, 8, 16 or 32 bits per sample, unsigned integer</li>
 *     <li>Class P (Palette/indexed color), all relevant compression types, 1, 2, 4, 8 or 16 bits per sample, unsigned integer</li>
 *     <li>Class R (RGB), all relevant compression types, 8 or 16 bits per sample, unsigned integer</li>
 * </ul>
 * In addition, it supports many common TIFF extensions such as:
 * <ul>
 *     <li>Tiling</li>
 *     <li>LZW Compression (type 5)</li>
 *     <li>JPEG Compression (type 7)</li>
 *     <li>ZLib (aka Adobe-style Deflate) Compression (type 8)</li>
 *     <li>Deflate Compression (type 32946)</li>
 *     <li>Horizontal differencing Predictor (type 2) for LZW, ZLib, Deflate and PackBits compression</li>
 *     <li>Alpha channel (ExtraSamples type 1/Associated Alpha)</li>
 *     <li>CMYK data (PhotometricInterpretation type 5/Separated)</li>
 *     <li>YCbCr data (PhotometricInterpretation type 6/YCbCr) for JPEG</li>
 *     <li>Planar data (PlanarConfiguration type 2/Planar)</li>
 *     <li>ICC profiles (ICCProfile)</li>
 *     <li>BitsPerSample values up to 16 for most PhotometricInterpretations</li>
 * </ul>
 *
 * @see <a href="http://partners.adobe.com/public/developer/tiff/index.html">Adobe TIFF developer resources</a>
 * @see <a href="http://en.wikipedia.org/wiki/Tagged_Image_File_Format">Wikipedia</a>
 * @see <a href="http://www.awaresystems.be/imaging/tiff.html">AWare Systems TIFF pages</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReader.java,v 1.0 08.05.12 15:14 haraldk Exp$
 */
public class TIFFImageReader extends ImageReaderBase {
    // TODOs ImageIO basic functionality:
    // TODO: Subsampling (*tests should be failing*)
    // TODO: Source region (*tests should be failing*)
    // TODO: TIFFImageWriter + Spi

    // TODOs ImageIO advanced functionality:
    // TODO: Implement readAsRenderedImage to allow tiled renderImage?
    //       For some layouts, we could do reads super-fast with a memory mapped buffer.
    // TODO: Implement readAsRaster directly
    // TODO: IIOMetadata

    // TODOs Full BaseLine support:
    // TODO: Support ExtraSamples (an array, if multiple extra samples!)
    //       (0: Unspecified (not alpha), 1: Associated Alpha (pre-multiplied), 2: Unassociated Alpha (non-multiplied)
    // TODO: Support Compression 2 (CCITT Modified Huffman) for bi-level images

    // TODOs Extension support
    // TODO: Support PlanarConfiguration 2
    // TODO: Support ICCProfile (fully)
    // TODO: Support Compression 3 & 4 (CCITT T.4 & T.6)
    // TODO: Support Compression 6 ('Old-style' JPEG)
    // TODO: Support Compression 34712 (JPEG2000)? Depends on JPEG2000 ImageReader
    // TODO: Support Compression 34661 (JBIG)? Depends on JBIG ImageReader

    // DONE:
    // Handle SampleFormat (and give up if not == 1)

    private final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.tiff.debug"));

    // NOTE: DO NOT MODIFY OR EXPOSE!
    static final double[] CCIR_601_1_COEFFICIENTS = new double[] {299.0 / 1000.0, 587.0 / 1000.0, 114.0 / 1000.0};

    private CompoundDirectory IFDs;
    private Directory currentIFD;

    TIFFImageReader(final TIFFImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        IFDs = null;
        currentIFD = null;
    }

    private void readMetadata() throws IOException {
        if (imageInput == null) {
            throw new IllegalStateException("input not set");
        }

        if (IFDs == null) {
            IFDs = (CompoundDirectory) new EXIFReader().read(imageInput); // NOTE: Sets byte order as a side effect

            if (DEBUG) {
                for (int i = 0; i < IFDs.directoryCount(); i++) {
                    System.err.printf("ifd[%d]: %s\n", i, IFDs.getDirectory(i));
                }

                System.err.println("Byte order: " + imageInput.getByteOrder());
                System.err.println("numImages: " + IFDs.directoryCount());
            }
        }
    }

    private void readIFD(final int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);
        currentIFD = IFDs.getDirectory(imageIndex);
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        readMetadata();

        return IFDs.directoryCount();
    }

    private int getValueAsIntWithDefault(final int tag, String tagName, Integer defaultValue) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);

        if (entry == null) {
            if (defaultValue != null)  {
                return defaultValue;
            }

            throw new IIOException("Missing TIFF tag: " + (tagName != null ? tagName : tag));
        }

        return ((Number) entry.getValue()).intValue();
    }

    private int getValueAsIntWithDefault(final int tag, Integer defaultValue) throws IIOException {
        return getValueAsIntWithDefault(tag, null, defaultValue);
    }

    private int getValueAsInt(final int tag, String tagName) throws IIOException {
        return getValueAsIntWithDefault(tag, tagName, null);
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readIFD(imageIndex);

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readIFD(imageIndex);

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        readIFD(imageIndex);

        getSampleFormat(); // We don't support anything but SAMPLEFORMAT_UINT at the moment, just sanity checking input
        int planarConfiguration = getValueAsIntWithDefault(TIFF.TAG_PLANAR_CONFIGURATION, TIFFExtension.PLANARCONFIG_PLANAR);
        int interpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
        int samplesPerPixel = getValueAsIntWithDefault(TIFF.TAG_SAMPLES_PER_PIXEL, 1);
        int bitsPerSample = getBitsPerSample();
        int dataType = bitsPerSample <= 8 ? DataBuffer.TYPE_BYTE : bitsPerSample <= 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_INT;

        // Read embedded cs
        ICC_Profile profile = getICCProfile();
        ColorSpace cs;

        switch (interpretation) {
            // TIFF 6.0 baseline
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // WhiteIsZero
                // NOTE: We handle this by inverting the values when reading, as Java has no ColorModel that easily supports this.
                // TODO: Consider returning null?
            case TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO:
                // BlackIsZero
                // Gray scale or B/W
                switch (samplesPerPixel) {
                    case 1:
                        // TIFF 6.0 Spec says: 1, 4 or 8 for baseline (1 for bi-level, 4/8 for gray)
                        // ImageTypeSpecifier supports 1, 2, 4, 8 or 16 bits, we'll go with that for now
                        cs = profile == null ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpaces.createColorSpace(profile);

                        if (cs == ColorSpace.getInstance(ColorSpace.CS_GRAY) && (bitsPerSample == 1 || bitsPerSample == 2 || bitsPerSample == 4 || bitsPerSample == 8 || bitsPerSample == 16)) {
                            return ImageTypeSpecifier.createGrayscale(bitsPerSample, dataType, false);
                        }
                        else if (bitsPerSample == 1 || bitsPerSample == 2 || bitsPerSample == 4 || bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32) {
                            return ImageTypeSpecifier.createInterleaved(cs, new int[] {0}, dataType, false, false);
                        }
                    default:
                        // TODO: If ExtraSamples is used, PlanarConfiguration must be taken into account also for gray data

                        throw new IIOException(String.format("Unsupported SamplesPerPixel/BitsPerSample combination for Bi-level/Gray TIFF (expected 1/1, 1/2, 1/4, 1/8 or 1/16): %d/%d", samplesPerPixel, bitsPerSample));
                }

            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // JPEG reader will handle YCbCr to RGB for us, we'll have to do it ourselves if not JPEG...
                // TODO: Sanity check that we have SamplesPerPixel == 3, BitsPerSample == [8,8,8] and Compression == 1 (none), 5 (LZW), or 6 (JPEG)
                // TODO: Handle YCbCrSubsampling (up-scaler stream, or read data as-is + up-sample (sub-)raster after read? Apply smoothing?)
            case TIFFBaseline.PHOTOMETRIC_RGB:
                // RGB
                cs = profile == null ? ColorSpace.getInstance(ColorSpace.CS_sRGB) : ColorSpaces.createColorSpace(profile);

                switch (samplesPerPixel) {
                    case 3:
                        if (bitsPerSample == 8 || bitsPerSample == 16) {
                            switch (planarConfiguration) {
                                case TIFFBaseline.PLANARCONFIG_CHUNKY:
                                    if (bitsPerSample == 8 && cs.isCS_sRGB()) {
                                        return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
                                    }

                                    return ImageTypeSpecifier.createInterleaved(cs, new int[] {0, 1, 2}, dataType, false, false);

                                case TIFFExtension.PLANARCONFIG_PLANAR:
                                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, dataType, false, false);
                            }
                        }
                    case 4:
                        // TODO: Consult ExtraSamples!
                        if (bitsPerSample == 8 || bitsPerSample == 16) {
                            switch (planarConfiguration) {
                                case TIFFBaseline.PLANARCONFIG_CHUNKY:
                                    if (bitsPerSample == 8 && cs.isCS_sRGB()) {
                                        return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);
                                    }

                                    return ImageTypeSpecifier.createInterleaved(cs, new int[] {0, 1, 2, 3}, dataType, true, false);

                                case TIFFExtension.PLANARCONFIG_PLANAR:
                                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, dataType, false, false);
                            }
                        }
                        // TODO: More samples might be ok, if multiple alpha or unknown samples
                    default:
                        throw new IIOException(String.format("Unsupported SamplesPerPixels/BitsPerSample combination for RGB TIF (expected 3/8, 4/8, 3/16 or 4/16): %d/%d", samplesPerPixel, bitsPerSample));
                }
            case TIFFBaseline.PHOTOMETRIC_PALETTE:
                // Palette
                if (samplesPerPixel != 1) {
                    throw new IIOException("Bad SamplesPerPixel value for Palette TIFF (expected 1): " + samplesPerPixel);
                }
                else if (bitsPerSample <= 0 || bitsPerSample > 16) {
                    throw new IIOException("Bad BitsPerSample value for Palette TIFF (expected <= 16): " + bitsPerSample);
                }
                // NOTE: If ExtraSamples is used, PlanarConfiguration must be taken into account also for pixel data

                Entry colorMap = currentIFD.getEntryById(TIFF.TAG_COLOR_MAP);
                if (colorMap == null) {
                    throw new IIOException("Missing ColorMap for Palette TIFF");
                }

                int[] cmapShort = (int[]) colorMap.getValue();
                int[] cmap = new int[colorMap.valueCount() / 3];

                // All reds, then greens, and finally blues
                for (int i = 0; i < cmap.length; i++) {
                    cmap[i] = (cmapShort[i                  ] / 256) << 16
                            | (cmapShort[i +     cmap.length] / 256) << 8
                            | (cmapShort[i + 2 * cmap.length] / 256);
                }

                IndexColorModel icm = new IndexColorModel(bitsPerSample, cmap.length, cmap, 0, false, -1, dataType);

                return IndexedImageTypeSpecifier.createFromIndexColorModel(icm);

            case TIFFExtension.PHOTOMETRIC_SEPARATED:
                // Separated (CMYK etc)
                // TODO: Consult the 332/InkSet (1=CMYK, 2=Not CMYK; see InkNames), 334/NumberOfInks (def=4) and optionally 333/InkNames
                // If "Not CMYK" we'll need an ICC profile to be able to display (in a useful way), readAsRaster should still work.
                cs = profile == null ? ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK) : ColorSpaces.createColorSpace(profile);

                switch (samplesPerPixel) {
                    case 4:
                        if (bitsPerSample == 8 || bitsPerSample == 16) {
                            switch (planarConfiguration) {
                                case TIFFBaseline.PLANARCONFIG_CHUNKY:
                                    return ImageTypeSpecifier.createInterleaved(cs, new int[] {0, 1, 2, 3}, dataType, false, false);
                                case TIFFExtension.PLANARCONFIG_PLANAR:
                                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, dataType, false, false);
                            }
                        }

                        // TODO: More samples might be ok, if multiple alpha or unknown samples, consult ExtraSamples

                    default:
                        throw new IIOException(
                                String.format("Unsupported TIFF SamplesPerPixels/BitsPerSample combination for Separated TIFF (expected 4/8 or 4/16): %d/%s", samplesPerPixel, bitsPerSample)
                        );
                }
            case TIFFBaseline.PHOTOMETRIC_MASK:
                // Transparency mask

                // TODO: Known extensions
                throw new IIOException("Unsupported TIFF PhotometricInterpretation value: " + interpretation);
            default:
                throw new IIOException("Unknown TIFF PhotometricInterpretation value: " + interpretation);
        }
    }

    private int getSampleFormat() throws IIOException {
        long[] value = getValueAsLongArray(TIFF.TAG_SAMPLE_FORMAT, "SampleFormat", false);

        if (value != null) {
            long sampleFormat = value[0];

            for (int i = 1; i < value.length; i++) {
                if (value[i] != sampleFormat) {
                    throw new IIOException("Variable TIFF SampleFormat not supported: " + Arrays.toString(value));
                }
            }

            if (sampleFormat != TIFFBaseline.SAMPLEFORMAT_UINT) {
                throw new IIOException("Unsupported TIFF SampleFormat (expected 1/Unsigned Integer): " + sampleFormat);
            }
        }

        // The default, and the only value we support
        return TIFFBaseline.SAMPLEFORMAT_UINT;
    }

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

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readIFD(imageIndex);

        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        List<ImageTypeSpecifier> specs = new ArrayList<ImageTypeSpecifier>();

        // TODO: Based on raw type, we can probably convert to most RGB types at least, maybe gray etc
        // TODO: Planar to chunky by default
        if (!rawType.getColorModel().getColorSpace().isCS_sRGB() && rawType.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB) {
            if (rawType.getNumBands() == 3 && rawType.getBitsPerBand(0) == 8) {
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            }
            else if (rawType.getNumBands() == 4 && rawType.getBitsPerBand(0) == 8) {
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE));
            }
        }

        specs.add(rawType);

        return specs.iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readIFD(imageIndex);

        System.err.println("currentIFD.getEntryById(TIFF.TAG_REFERENCE_BLACK_WHITE): " + currentIFD.getEntryById(TIFF.TAG_REFERENCE_BLACK_WHITE));
        System.err.println("currentIFD.getEntryById(TIFF.TAG_TRANSFER_FUNCTION): " + currentIFD.getEntryById(TIFF.TAG_TRANSFER_FUNCTION));
        System.err.println("currentIFD.getEntryById(TIFF.TAG_TRANSFER_RANGE): " + currentIFD.getEntryById(TIFF.TAG_TRANSFER_RANGE));

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        checkReadParamBandSettings(param, rawType.getNumBands(), destination.getSampleModel().getNumBands());

        final Rectangle source = new Rectangle();
        final Rectangle dest = new Rectangle();
        computeRegions(param, width, height, destination, source, dest);

        WritableRaster raster = destination.getRaster();

        final int interpretation = getValueAsInt(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation");
        final int compression = getValueAsIntWithDefault(TIFF.TAG_COMPRESSION, TIFFBaseline.COMPRESSION_NONE);
        final int predictor = getValueAsIntWithDefault(TIFF.TAG_PREDICTOR, 1);
        final int planarConfiguration = getValueAsIntWithDefault(TIFF.TAG_PLANAR_CONFIGURATION, TIFFBaseline.PLANARCONFIG_CHUNKY);
        final int numBands = planarConfiguration == TIFFExtension.PLANARCONFIG_PLANAR ? 1 : raster.getNumBands();

        // NOTE: We handle strips as tiles of tileWidth == width by tileHeight == rowsPerStrip
        //       Strips are top/down, tiles are left/right, top/down
        int stripTileWidth = width;
        int stripTileHeight = getValueAsIntWithDefault(TIFF.TAG_ROWS_PER_STRIP, height);
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
        WritableRaster rowRaster = rawType.getColorModel().createCompatibleWritableRaster(stripTileWidth, 1);
        int row = 0;

        // Read data
        processImageStarted(imageIndex);

        switch (compression) {
            // TIFF Baseline
            case TIFFBaseline.COMPRESSION_NONE:
                // No compression
            case TIFFExtension.COMPRESSION_DEFLATE:
                // 'PKZIP-style' Deflate
            case TIFFBaseline.COMPRESSION_PACKBITS:
                // PackBits
            case TIFFExtension.COMPRESSION_LZW:
                // LZW
            case TIFFExtension.COMPRESSION_ZLIB:
                // 'Adobe-style' Deflate

                int[] yCbCrSubsampling = null;
                int yCbCrPos = 1;
                double[] yCbCrCoefficients = null;
                if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR) {
                    // getRawImageType does the lookup/conversion for these
                    if (raster.getNumBands() != 3) {
                        throw new IIOException("TIFF PhotometricInterpreatation YCbCr requires SamplesPerPixel == 3: " + raster.getNumBands());
                    }
                    if (raster.getTransferType() != DataBuffer.TYPE_BYTE) {
                        throw new IIOException("TIFF PhotometricInterpreatation YCbCr requires BitsPerSample == [8,8,8]");
                    }

                    yCbCrPos = getValueAsIntWithDefault(TIFF.TAG_YCBCR_POSITIONING, 1);

                    Entry subSampling = currentIFD.getEntryById(TIFF.TAG_YCBCR_SUB_SAMPLING);

                    if (subSampling != null) {
                        try {
                            yCbCrSubsampling = (int[]) subSampling.getValue();
                        }
                        catch (ClassCastException e) {
                            throw new IIOException("Unknown TIFF YCbCrSubSampling value type: " + subSampling.getTypeName(), e);
                        }

                        if (yCbCrSubsampling.length != 2 ||
                                yCbCrSubsampling[0] != 1 && yCbCrSubsampling[0] != 2 && yCbCrSubsampling[0] != 4 ||
                                yCbCrSubsampling[1] != 1 && yCbCrSubsampling[1] != 2 && yCbCrSubsampling[1] != 4 ||
                                yCbCrSubsampling[0] < yCbCrSubsampling[1]) {
                            throw new IIOException("Bad TIFF YCbCrSubSampling value: " + Arrays.toString(yCbCrSubsampling));
                        }
                    }
                    else {
                        yCbCrSubsampling = new int[] {2, 2};
                    }

                    Entry coefficients = currentIFD.getEntryById(TIFF.TAG_YCBCR_COEFFICIENTS);
                    if (coefficients != null) {
                        Rational[] value = (Rational[]) coefficients.getValue();
                        yCbCrCoefficients = new double[] {value[0].doubleValue(), value[1].doubleValue(), value[2].doubleValue()};
                    }
                    else {
                        // Default to y CCIR Recommendation 601-1 values
                        yCbCrCoefficients = CCIR_601_1_COEFFICIENTS;
                    }
                }

                // TODO: Read only tiles that lies within region
                // General uncompressed/compressed reading
                for (int y = 0; y < tilesDown; y++) {
                    int col = 0;
                    int rowsInTile = Math.min(stripTileHeight, height - row);

                    for (int x = 0; x < tilesAcross; x++) {
                        int colsInTile = Math.min(stripTileWidth, width - col);
                        int i = y * tilesAcross + x;

                        imageInput.seek(stripTileOffsets[i]);

                        DataInput input;
                        if (compression == TIFFBaseline.COMPRESSION_NONE && interpretation != TIFFExtension.PHOTOMETRIC_YCBCR) {
                            // No need for transformation, fast forward
                            input = imageInput;
                        }
                        else {
                            InputStream adapter = stripTileByteCounts != null
                                    ? IIOUtil.createStreamAdapter(imageInput, stripTileByteCounts[i])
                                    : IIOUtil.createStreamAdapter(imageInput);

                            adapter = createDecoderInputStream(compression, adapter);

                            if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR) {
                                adapter = new YCbCrUpsamplerStream(adapter, yCbCrSubsampling, colsInTile, yCbCrCoefficients);
                            }

                            // According to the spec, short/long/etc should follow order of containing stream
                            input = imageInput.getByteOrder() == ByteOrder.BIG_ENDIAN
                                    ? new DataInputStream(adapter)
                                    : new LittleEndianDataInputStream(adapter);
                        }

                        // Read a full strip/tile
                        readStripTileData(rowRaster, interpretation, predictor, raster, numBands, col, row, colsInTile, rowsInTile, input);

                        if (abortRequested()) {
                            break;
                        }

                        col += colsInTile;
                    }

                    processImageProgress(100f * row / (float) height);

                    if (abortRequested()) {
                        processReadAborted();
                        break;
                    }

                    row += rowsInTile;
                }

                break;

            case TIFFExtension.COMPRESSION_JPEG:
                // JPEG ('new-style' JPEG)
                // TODO: Refactor all JPEG reading out to separate JPEG support class?

                // TIFF is strictly ISO JPEG, so we should probably stick to the standard reader
                ImageReader jpegReader = new JPEGImageReader(getOriginatingProvider());
                JPEGImageReadParam jpegParam = (JPEGImageReadParam) jpegReader.getDefaultReadParam();

                // JPEG_TABLES should be a full JPEG 'abbreviated table specification', containing:
                // SOI, DQT, DHT, (optional markers that we ignore)..., EOI
                Entry tablesEntry = currentIFD.getEntryById(TIFF.TAG_JPEG_TABLES);
                byte[] tablesValue = tablesEntry != null ? (byte[]) tablesEntry.getValue() : null;
                if (tablesValue != null) {
                    // TODO: Work this out...
                    // Whatever values I pass the reader as the read param, it never gets the same quality as if
                    // I just invoke jpegReader.getStreamMetadata...
                    // Might have something to do with subsampling?
                    // How do we pass the chroma-subsampling parameter from the TIFF structure to the JPEG reader?

                    // TODO: Consider splicing the TAG_JPEG_TABLES into the streams for each tile, for a more
                    // compatible approach..?

                    jpegReader.setInput(new ByteArrayImageInputStream(tablesValue));

                    // NOTE: This initializes the tables AND MORE secret internal settings for the reader (as if by magic).
                    // This is probably a bug, as later setInput calls should clear/override the tables.
                    // However, it would be extremely convenient, not having to actually fiddle with the stream meta data (as below)
                    /*IIOMetadata streamMetadata = */jpegReader.getStreamMetadata();

                    /*
                    IIOMetadataNode root = (IIOMetadataNode) streamMetadata.getAsTree(streamMetadata.getNativeMetadataFormatName());
                    NodeList dqt = root.getElementsByTagName("dqt");
                    NodeList dqtables = ((IIOMetadataNode) dqt.item(0)).getElementsByTagName("dqtable");
                    JPEGQTable[] qTables = new JPEGQTable[dqtables.getLength()];
                    for (int i = 0; i < dqtables.getLength(); i++) {
                        qTables[i] = (JPEGQTable) ((IIOMetadataNode) dqtables.item(i)).getUserObject();
                        System.err.println("qTables: " + qTables[i]);
                    }

                    List<JPEGHuffmanTable> acHTables = new ArrayList<JPEGHuffmanTable>();
                    List<JPEGHuffmanTable> dcHTables = new ArrayList<JPEGHuffmanTable>();

                    NodeList dht = root.getElementsByTagName("dht");
                    for (int i = 0; i < dht.getLength(); i++) {
                        NodeList dhtables = ((IIOMetadataNode) dht.item(i)).getElementsByTagName("dhtable");
                        for (int j = 0; j < dhtables.getLength(); j++) {
                            System.err.println("dhtables.getLength(): " + dhtables.getLength());
                            IIOMetadataNode dhtable = (IIOMetadataNode) dhtables.item(j);
                            JPEGHuffmanTable userObject = (JPEGHuffmanTable) dhtable.getUserObject();
                            if ("0".equals(dhtable.getAttribute("class"))) {
                                dcHTables.add(userObject);
                            }
                            else {
                                acHTables.add(userObject);
                            }
                        }
                    }

                    JPEGHuffmanTable[] dcTables = dcHTables.toArray(new JPEGHuffmanTable[dcHTables.size()]);
                    JPEGHuffmanTable[] acTables = acHTables.toArray(new JPEGHuffmanTable[acHTables.size()]);
*/
//                    JPEGTables tables = new JPEGTables(new ByteArrayImageInputStream(tablesValue));
//                    JPEGQTable[] qTables = tables.getQTables();
//                    JPEGHuffmanTable[] dcTables = tables.getDCHuffmanTables();
//                    JPEGHuffmanTable[] acTables = tables.getACHuffmanTables();

//                    System.err.println("qTables: " + Arrays.toString(qTables));
//                    System.err.println("dcTables: " + Arrays.toString(dcTables));
//                    System.err.println("acTables: " + Arrays.toString(acTables));

//                    jpegParam.setDecodeTables(qTables, dcTables, acTables);
                }
                else {
                    processWarningOccurred("Missing JPEGTables for TIFF with compression: 7 (JPEG)");
                    // ...and the JPEG reader will probably choke on missing tables...
                }

                for (int y = 0; y < tilesDown; y++) {
                    int col = 0;
                    int rowsInTile = Math.min(stripTileHeight, height - row);

                    for (int x = 0; x < tilesAcross; x++) {
                        int i = y * tilesAcross + x;
                        int colsInTile = Math.min(stripTileWidth, width - col);

                        imageInput.seek(stripTileOffsets[i]);
                        SubImageInputStream subStream = new SubImageInputStream(imageInput, stripTileByteCounts != null ? (int) stripTileByteCounts[i] : Short.MAX_VALUE);
                        try {
                            jpegReader.setInput(subStream);
                            jpegParam.setSourceRegion(new Rectangle(0, 0, colsInTile, rowsInTile));
                            jpegParam.setDestinationOffset(new Point(col, row));
                            jpegParam.setDestination(destination);
                            // TODO: This works only if Gray/YCbCr/RGB, not CMYK/LAB/etc...
                            // In the latter case we will have to use readAsRaster
                            jpegReader.read(0, jpegParam);
                        }
                        finally {
                            subStream.close();
                        }

                        if (abortRequested()) {
                            break;
                        }

                        col += colsInTile;
                    }

                    processImageProgress(100f * row / (float) height);

                    if (abortRequested()) {
                        processReadAborted();
                        break;
                    }

                    row += rowsInTile;
                }

                break;

            case TIFFBaseline.COMPRESSION_CCITT_HUFFMAN:
                // CCITT modified Huffman
                // Additionally, the specification defines these values as part of the TIFF extensions:
            case TIFFExtension.COMPRESSION_CCITT_T4:
                // CCITT Group 3 fax encoding
            case TIFFExtension.COMPRESSION_CCITT_T6:
                // CCITT Group 4 fax encoding
            case TIFFExtension.COMPRESSION_OLD_JPEG:
                // JPEG ('old-style' JPEG, later overridden in Technote2)

                throw new IIOException("Unsupported TIFF Compression value: " + compression);
            default:
                throw new IIOException("Unknown TIFF Compression value: " + compression);
        }

        processImageComplete();

        return destination;
    }

    private void readStripTileData(final WritableRaster rowRaster, final int interpretation, final int predictor,
                                   final WritableRaster raster, final int numBands, final int col, final int startRow,
                                   final int colsInStrip, final int rowsInStrip, final DataInput input)
            throws IOException {
        switch (rowRaster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                byte[] rowData = ((DataBufferByte) rowRaster.getDataBuffer()).getData();

                for (int j = 0; j < rowsInStrip; j++) {
                    int row = startRow + j;

                    if (row >= raster.getHeight()) {
                        break;
                    }

                    input.readFully(rowData);

                    unPredict(predictor, colsInStrip, 1, numBands, rowData);
                    normalizeBlack(interpretation, rowData);

                    if (colsInStrip == rowRaster.getWidth() && col + colsInStrip <= raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster);
                    }
                    else if (col >= raster.getMinX() && col < raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster.createChild(0, 0, Math.min(colsInStrip, raster.getWidth() - col), 1, 0, 0, null));
                    }
                    // Else skip data
                }

                break;
            case DataBuffer.TYPE_USHORT:
                short [] rowDataShort = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();

                for (int j = 0; j < rowsInStrip; j++) {
                    int row = startRow + j;

                    if (row >= raster.getHeight()) {
                        break;
                    }

                    for (int k = 0; k < rowDataShort.length; k++) {
                        rowDataShort[k] = input.readShort();
                    }

                    unPredict(predictor, colsInStrip, 1, numBands, rowDataShort);
                    normalizeBlack(interpretation, rowDataShort);

                    if (colsInStrip == rowRaster.getWidth() && col + colsInStrip <= raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster);
                    }
                    else if (col >= raster.getMinX() && col < raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster.createChild(0, 0, Math.min(colsInStrip, raster.getWidth() - col), 1, 0, 0, null));
                    }
                    // Else skip data
                }

                break;
            case DataBuffer.TYPE_INT:
                int [] rowDataInt = ((DataBufferInt) rowRaster.getDataBuffer()).getData();

                for (int j = 0; j < rowsInStrip; j++) {
                    int row = startRow + j;

                    if (row >= raster.getHeight()) {
                        break;
                    }

                    for (int k = 0; k < rowDataInt.length; k++) {
                        rowDataInt[k] = input.readInt();
                    }

                    unPredict(predictor, colsInStrip, 1, numBands, rowDataInt);
                    normalizeBlack(interpretation, rowDataInt);

                    if (colsInStrip == rowRaster.getWidth() && col + colsInStrip <= raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster);
                    }
                    else if (col >= raster.getMinX() && col < raster.getWidth()) {
                        raster.setDataElements(col, row, rowRaster.createChild(0, 0, Math.min(colsInStrip, raster.getWidth() - col), 1, 0, 0, null));
                    }
                    // Else skip data
                }

                break;
        }
    }

    private void normalizeBlack(int photometricInterpretation, short[] data) {
        if (photometricInterpretation == TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (short) (0xffff - data[i] & 0xffff);
            }
        }
    }

    private void normalizeBlack(int photometricInterpretation, int[] data) {
        if (photometricInterpretation == TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (0xffffffff - data[i]);
            }
        }
    }

    private void normalizeBlack(int photometricInterpretation, byte[] data) {
        if (photometricInterpretation == TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO) {
            // Inverse values
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (0xff - data[i] & 0xff);
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void unPredict(final int predictor, int scanLine, int rows, int bands, int[] data) throws IIOException {
        // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.
        switch (predictor) {
            case TIFFBaseline.PREDICTOR_NONE:
                break;
            case TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING:
                // TODO: Implement
            case TIFFExtension.PREDICTOR_HORIZONTAL_FLOATINGPOINT:
                throw new IIOException("Unsupported TIFF Predictor value: " + predictor);
            default:
                throw new IIOException("Unknown TIFF Predictor value: " + predictor);
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void unPredict(final int predictor, int scanLine, int rows, int bands, short[] data) throws IIOException {
        // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.
        switch (predictor) {
            case TIFFBaseline.PREDICTOR_NONE:
                break;
            case TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING:
                // TODO: Implement
            case TIFFExtension.PREDICTOR_HORIZONTAL_FLOATINGPOINT:
                throw new IIOException("Unsupported TIFF Predictor value: " + predictor);
            default:
                throw new IIOException("Unknown TIFF Predictor value: " + predictor);
        }
    }

    private void unPredict(final int predictor, int scanLine, int rows, final int bands, byte[] data) throws IIOException {
        // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.
        switch (predictor) {
            case TIFFBaseline.PREDICTOR_NONE:
                break;
            case TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING:
                for (int y = 0; y < rows; y++) {
                    for (int x = 1; x < scanLine; x++) {
                        // TODO: For planar data (PlanarConfiguration == 2), treat as bands == 1
                        for (int b = 0; b < bands; b++) {
                            int off = y * scanLine + x;
                            data[off * bands + b] = (byte) (data[(off - 1) * bands + b] + data[off * bands + b]);
                        }
                    }
                }

                break;
            case TIFFExtension.PREDICTOR_HORIZONTAL_FLOATINGPOINT:
                throw new IIOException("Unsupported TIFF Predictor value: " + predictor);
            default:
                throw new IIOException("Unknown TIFF Predictor value: " + predictor);
        }
    }

    private InputStream createDecoderInputStream(final int compression, final InputStream stream) throws IOException {
        switch (compression) {
            case TIFFBaseline.COMPRESSION_NONE:
                return stream;
            case TIFFBaseline.COMPRESSION_PACKBITS:
                return new DecoderStream(stream, new PackBitsDecoder(), 1024);
            case TIFFExtension.COMPRESSION_LZW:
                return new DecoderStream(stream, LZWDecoder.create(LZWDecoder.isOldBitReversedStream(stream)), 1024);
            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
                // TIFFphotoshop.pdf (aka TIFF specification, supplement 2) says ZLIB (8) and DEFLATE (32946) algorithms are identical
                return new InflaterInputStream(stream, new Inflater(), 1024);
            default:
                throw new IllegalArgumentException("Unsupported TIFF compression: " + compression);
        }
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

    public ICC_Profile getICCProfile() {
        Entry entry = currentIFD.getEntryById(TIFF.TAG_ICC_PROFILE);
        if (entry == null) {
            return null;
        }

        byte[] value = (byte[]) entry.getValue();
        return ICC_Profile.getInstance(value);
    }

    public static void main(final String[] args) throws IOException {
        for (final String arg : args) {
            File file = new File(arg);

            ImageInputStream input = ImageIO.createImageInputStream(file);
            if (input == null) {
                System.err.println("Could not read file: " + file);
                continue;
            }

            deregisterOSXTIFFImageReaderSpi();

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                System.err.println("No reader for: " + file);
                continue;
            }

            ImageReader reader = readers.next();
            System.err.println("Reading using: " + reader);

            reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                public void warningOccurred(ImageReader source, String warning) {
                    System.err.println("Warning: " + arg + ": " + warning);
                }
            });
            reader.addIIOReadProgressListener(new ProgressListenerBase() {
                private static final int MAX_W = 78;
                int lastProgress = 0;

                @Override
                public void imageStarted(ImageReader source, int imageIndex) {
                    System.out.print("[");
                }

                @Override
                public void imageProgress(ImageReader source, float percentageDone) {
                    int steps = ((int) (percentageDone * MAX_W) / 100);

                    for (int i = lastProgress; i < steps; i++) {
                        System.out.print(".");
                    }

                    System.out.flush();
                    lastProgress = steps;
                }

                @Override
                public void imageComplete(ImageReader source) {
                    for (int i = lastProgress; i < MAX_W; i++) {
                        System.out.print(".");
                    }

                    System.out.println("]");
                }
            });

            reader.setInput(input);

            try {
                ImageReadParam param = reader.getDefaultReadParam();
                int numImages = reader.getNumImages(true);
                for (int imageNo = 0; imageNo < numImages; imageNo++) {
                    //            if (args.length > 1) {
                    //                int sub = Integer.parseInt(args[1]);
                    //                int sub = 4;
                    //                param.setSourceSubsampling(sub, sub, 0, 0);
                    //            }

                    long start = System.currentTimeMillis();
//                    param.setSourceRegion(new Rectangle(100, 100, 100, 100));
//                    param.setDestinationOffset(new Point(50, 150));
//                    param.setSourceSubsampling(2, 2, 0, 0);
                    BufferedImage image = reader.read(imageNo, param);
                    System.err.println("Read time: " + (System.currentTimeMillis() - start) + " ms");
//                System.err.println("image: " + image);

//                    File tempFile = File.createTempFile("lzw-", ".bin");
//                    byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
//                    FileOutputStream stream = new FileOutputStream(tempFile);
//                    try {
//                        FileUtil.copy(new ByteArrayInputStream(data, 45 * image.getWidth() * 3, 5 * image.getWidth() * 3), stream);
//
//                        showIt(image.getSubimage(0, 45, image.getWidth(), 5), tempFile.getAbsolutePath());
//                    }
//                    finally {
//                        stream.close();
//                    }
//
//                    System.err.println("tempFile: " + tempFile.getAbsolutePath());

                    //            image = new ResampleOp(reader.getWidth(0) / 4, reader.getHeight(0) / 4, ResampleOp.FILTER_LANCZOS).filter(image, null);
//
//                int maxW = 800;
//                int maxH = 800;
//
//                if (image.getWidth() > maxW || image.getHeight() > maxH) {
//                    start = System.currentTimeMillis();
//                    float aspect = reader.getAspectRatio(0);
//                    if (aspect >= 1f) {
//                        image = ImageUtil.createResampled(image, maxW, Math.round(maxW / aspect), Image.SCALE_DEFAULT);
//                    }
//                    else {
//                        image = ImageUtil.createResampled(image, Math.round(maxH * aspect), maxH, Image.SCALE_DEFAULT);
//                    }
//    //                    System.err.println("Scale time: " + (System.currentTimeMillis() - start) + " ms");
//                }

                    showIt(image, String.format("Image: %s [%d x %d]", file.getName(), reader.getWidth(imageNo), reader.getHeight(imageNo)));

                    try {
                        int numThumbnails = reader.getNumThumbnails(0);
                        for (int thumbnailNo = 0; thumbnailNo < numThumbnails; thumbnailNo++) {
                            BufferedImage thumbnail = reader.readThumbnail(imageNo, thumbnailNo);
                            //                        System.err.println("thumbnail: " + thumbnail);
                            showIt(thumbnail, String.format("Thumbnail: %s [%d x %d]", file.getName(), thumbnail.getWidth(), thumbnail.getHeight()));
                        }
                    }
                    catch (IIOException e) {
                        System.err.println("Could not read thumbnails: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            catch (Throwable t) {
                System.err.println(file);
                t.printStackTrace();
            }
            finally {
                input.close();
            }
        }
    }

    private static void deregisterOSXTIFFImageReaderSpi() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator<ImageReaderSpi> providers = registry.getServiceProviders(ImageReaderSpi.class, new ServiceRegistry.Filter() {
            public boolean filter(Object provider) {
                return provider.getClass().getName().equals("com.sun.imageio.plugins.tiff.TIFFImageReaderSpi");
            }
        }, false);

        while (providers.hasNext()) {
            ImageReaderSpi next = providers.next();
            registry.deregisterServiceProvider(next);
        }
    }
}
