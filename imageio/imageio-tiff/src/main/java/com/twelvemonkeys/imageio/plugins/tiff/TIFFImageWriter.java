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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.metadata.AbstractEntry;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFWriter;
import com.twelvemonkeys.imageio.metadata.exif.Rational;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.stream.SubImageOutputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.enc.PackBitsEncoder;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * TIFFImageWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriter.java,v 1.0 18.09.13 12:46 haraldk Exp$
 */
public final class TIFFImageWriter extends ImageWriterBase {
    // Short term
    // TODO: Support more of the ImageIO metadata (ie. compression from metadata, etc)

    // Long term
    // TODO: Support tiling
    // TODO: Support thumbnails
    // TODO: Support CCITT Modified Huffman compression (2)
    // TODO: Full "Baseline TIFF" support (pending CCITT compression 2)
    // TODO: CCITT compressions T.4 and T.6
    // TODO: Support JPEG compression of CMYK data (pending JPEGImageWriter CMYK write support)
    // ----
    // TODO: Support storing multiple images in one stream (multi-page TIFF)
    // TODO: Support use-case: Transcode multi-layer PSD to multi-page TIFF with metadata
    // TODO: Support use-case: Transcode multi-page TIFF to multiple single-page TIFFs with metadata
    // TODO: Support use-case: Losslessly transcode JPEG to JPEG-in-TIFF with (EXIF) metadata (and back)

    // Very long term...
    // TODO: Support JBIG compression via ImageIO plugin/delegate? Pending support in Reader
    // TODO: Support JPEG2000 compression via ImageIO plugin/delegate? Pending support in Reader

    // Done
    // Create a basic writer that supports most inputs. Store them using the simplest possible format.
    // Support no compression (None/1) - BASELINE
    // Support predictor. See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.
    // Support PackBits compression (32773) - easy - BASELINE
    // Support ZLIB (/Deflate) compression (8) - easy
    // Support LZW compression (5)
    // Support JPEG compression (7) - might need extra input to allow multiple images with single DQT
    // Use sensible defaults for compression based on input? None is sensible... :-)
    // Support resolution, resolution unit and software tags from ImageIO metadata

    public static final Rational STANDARD_DPI = new Rational(72);

    /**
     * Flag for active sequence writing
     */
    private boolean isWritingSequence = false;

    /**
     * Metadata writer for sequence writing
     */
    private EXIFWriter sequenceExifWriter = null;

    /**
     * Position of last IFD Pointer on active sequence writing
     */
    private long sequenceLastIFDPos = -1;

    TIFFImageWriter(final ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public void setOutput(final Object output) {
        super.setOutput(output);

        // TODO: Allow appending/partly overwrite of existing file...
    }

    static final class TIFFEntry extends AbstractEntry {
        // TODO: Expose a merge of this and the EXIFEntry class...
        private final short type;

        private static short guessType(final Object val) {
            // TODO: This code is duplicated in EXIFWriter.getType, needs refactor!
            Object value = Validate.notNull(val);

            boolean array = value.getClass().isArray();
            if (array) {
                value = Array.get(value, 0);
            }

            // Note: This "narrowing" is to keep data consistent between read/write.
            // TODO: Check for negative values and use signed types?
            if (value instanceof Byte) {
                return TIFF.TYPE_BYTE;
            }
            if (value instanceof Short) {
                if (!array && (Short) value < Byte.MAX_VALUE) {
                    return TIFF.TYPE_BYTE;
                }

                return TIFF.TYPE_SHORT;
            }
            if (value instanceof Integer) {
                if (!array && (Integer) value < Short.MAX_VALUE) {
                    return TIFF.TYPE_SHORT;
                }

                return TIFF.TYPE_LONG;
            }
            if (value instanceof Long) {
                if (!array && (Long) value < Integer.MAX_VALUE) {
                    return TIFF.TYPE_LONG;
                }
            }

            if (value instanceof Rational) {
                return TIFF.TYPE_RATIONAL;
            }

            if (value instanceof String) {
                return TIFF.TYPE_ASCII;
            }

            // TODO: More types

            throw new UnsupportedOperationException(String.format("Method guessType not implemented for value of type %s", value.getClass()));
        }

        TIFFEntry(final int identifier, final Object value) {
            this(identifier, guessType(value), value);
        }

        TIFFEntry(int identifier, short type, Object value) {
            super(identifier, value);
            this.type = type;
        }

        @Override
        public String getTypeName() {
            return TIFF.TYPE_NAMES[type];
        }
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        // TODO: Validate input

        assertOutput();
        // TODO: streamMetadata?

        // TODO: Consider writing TIFF header, offset to IFD0 (leave blank), write image data with correct
        // tiling/compression/etc, then write IFD0, go back and update IFD0 offset?

        // Write minimal TIFF header (required "Baseline" fields)
        // Use EXIFWriter to write leading metadata (TODO: consider rename to TTIFFWriter, again...)
        // TODO: Make TIFFEntry and possibly TIFFDirectory? public
        EXIFWriter exifWriter = new EXIFWriter();
        exifWriter.writeTIFFHeader(imageOutput);
        long IFD0Pos = imageOutput.getStreamPosition();
        imageOutput.writeInt(0); // IFD0 pointer, will be updated later

        writePage(image, param, exifWriter, IFD0Pos);
        imageOutput.flush();
    }

    private long writePage(IIOImage image, ImageWriteParam param, EXIFWriter exifWriter, long lastIFDPointer)
            throws IOException {
        RenderedImage renderedImage = image.getRenderedImage();
        ColorModel colorModel = renderedImage.getColorModel();
        int numComponents = colorModel.getNumComponents();

        TIFFImageMetadata metadata;
        if (image.getMetadata() != null) {
            metadata = convertImageMetadata(image.getMetadata(), ImageTypeSpecifier.createFromRenderedImage(renderedImage), param);
        }
        else {
            metadata = initMeta(null, ImageTypeSpecifier.createFromRenderedImage(renderedImage), param);
        }

        SampleModel sampleModel = renderedImage.getSampleModel();

        int[] bandOffsets;
        int[] bitOffsets;
        if (sampleModel instanceof ComponentSampleModel) {
            bandOffsets = ((ComponentSampleModel) sampleModel).getBandOffsets();
            bitOffsets = null;
        }
        else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            bitOffsets = ((SinglePixelPackedSampleModel) sampleModel).getBitOffsets();
            bandOffsets = null;
        }
        else if (sampleModel instanceof MultiPixelPackedSampleModel) {
            bitOffsets = null;
            bandOffsets = new int[] {0};
        }
        else {
            throw new IllegalArgumentException("Unknown bit/bandOffsets for sample model: " + sampleModel);
        }

        Map<Integer, Entry> entries = new LinkedHashMap<>();
        entries.put(TIFF.TAG_IMAGE_WIDTH, new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, renderedImage.getWidth()));
        entries.put(TIFF.TAG_IMAGE_HEIGHT, new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, renderedImage.getHeight()));
        // entries.add(new TIFFEntry(TIFF.TAG_ORIENTATION, 1)); // (optional)
        entries.put(TIFF.TAG_BITS_PER_SAMPLE, new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, asShortArray(sampleModel.getSampleSize())));
        // If numComponents > numColorComponents, write ExtraSamples
        if (numComponents > colorModel.getNumColorComponents()) {
            // TODO: Write per component > numColorComponents
            if (colorModel.hasAlpha()) {
                entries.put(TIFF.TAG_EXTRA_SAMPLES, new TIFFEntry(TIFF.TAG_EXTRA_SAMPLES, colorModel.isAlphaPremultiplied()
                                                                                          ? TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA
                                                                                          : TIFFBaseline.EXTRASAMPLE_UNASSOCIATED_ALPHA));
            }
            else {
                entries.put(TIFF.TAG_EXTRA_SAMPLES, new TIFFEntry(TIFF.TAG_EXTRA_SAMPLES, TIFFBaseline.EXTRASAMPLE_UNSPECIFIED));
            }
        }

        // Write compression field from param or metadata
        int compression;
        if ((param == null || param.getCompressionMode() == TIFFImageWriteParam.MODE_COPY_FROM_METADATA)
                && image.getMetadata() != null && metadata.getIFD().getEntryById(TIFF.TAG_COMPRESSION) != null) {
            compression = (int) metadata.getIFD().getEntryById(TIFF.TAG_COMPRESSION).getValue();
        }
        else {
            compression = TIFFImageWriteParam.getCompressionType(param);
        }
        entries.put(TIFF.TAG_COMPRESSION, new TIFFEntry(TIFF.TAG_COMPRESSION, compression));

        // TODO: Let param/metadata control predictor
        // TODO: Depending on param.getCompressionMode(): DISABLED/EXPLICIT/COPY_FROM_METADATA/DEFAULT
        switch (compression) {
            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
            case TIFFExtension.COMPRESSION_LZW:
                entries.put(TIFF.TAG_PREDICTOR, new TIFFEntry(TIFF.TAG_PREDICTOR, TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING));
                break;
            case TIFFExtension.COMPRESSION_CCITT_T4:
                Entry group3options = metadata.getIFD().getEntryById(TIFF.TAG_GROUP3OPTIONS);
                if (group3options == null) {
                    group3options = new TIFFEntry(TIFF.TAG_GROUP3OPTIONS, (long) TIFFExtension.GROUP3OPT_2DENCODING);
                }
                entries.put(TIFF.TAG_GROUP3OPTIONS, group3options);
                break;
            case TIFFExtension.COMPRESSION_CCITT_T6:
                Entry group4options = metadata.getIFD().getEntryById(TIFF.TAG_GROUP4OPTIONS);
                if (group4options == null) {
                    group4options = new TIFFEntry(TIFF.TAG_GROUP4OPTIONS, 0L);
                }
                entries.put(TIFF.TAG_GROUP4OPTIONS, group4options);
                break;
            default:
        }

        // TODO: We might want to support CMYK in JPEG as well... Pending JPEG CMYK write support.
        int photometric = compression == TIFFExtension.COMPRESSION_JPEG ?
                          TIFFExtension.PHOTOMETRIC_YCBCR :
                          getPhotometricInterpretation(colorModel);
        entries.put(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, photometric));

        if (photometric == TIFFBaseline.PHOTOMETRIC_PALETTE && colorModel instanceof IndexColorModel) {
            entries.put(TIFF.TAG_COLOR_MAP, new TIFFEntry(TIFF.TAG_COLOR_MAP, createColorMap((IndexColorModel) colorModel)));
            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, 1));
        }
        else {
            if (colorModel.getPixelSize() == 1) {
                numComponents = 1;
            }

            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, numComponents));

            // Note: Assuming sRGB to be the default RGB interpretation
            ColorSpace colorSpace = colorModel.getColorSpace();
            if (colorSpace instanceof ICC_ColorSpace && !colorSpace.isCS_sRGB()) {
                entries.put(TIFF.TAG_ICC_PROFILE, new TIFFEntry(TIFF.TAG_ICC_PROFILE, ((ICC_ColorSpace) colorSpace).getProfile().getData()));
            }
        }

        // Default sample format SAMPLEFORMAT_UINT need not be written
        if (sampleModel.getDataType() == DataBuffer.TYPE_SHORT/* TODO: if isSigned(sampleModel.getDataType) or getSampleFormat(sampleModel) != 0 */) {
            entries.put(TIFF.TAG_SAMPLE_FORMAT, new TIFFEntry(TIFF.TAG_SAMPLE_FORMAT, TIFFExtension.SAMPLEFORMAT_INT));
        }
        // TODO: Float values!

        // Get Software from metadata, or use default
        Entry software = metadata.getIFD().getEntryById(TIFF.TAG_SOFTWARE);
        entries.put(TIFF.TAG_SOFTWARE, software != null
                                       ? software
                                       : new TIFFEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO TIFF writer " + originatingProvider.getVersion()));

        // Get X/YResolution and ResolutionUnit from metadata if set, otherwise use defaults
        // TODO: Add logic here OR in metadata merging, to make sure these 3 values are consistent.
        Entry xRes = metadata.getIFD().getEntryById(TIFF.TAG_X_RESOLUTION);
        entries.put(TIFF.TAG_X_RESOLUTION, xRes != null ? xRes : new TIFFEntry(TIFF.TAG_X_RESOLUTION, STANDARD_DPI));
        Entry yRes = metadata.getIFD().getEntryById(TIFF.TAG_Y_RESOLUTION);
        entries.put(TIFF.TAG_Y_RESOLUTION, yRes != null ? yRes : new TIFFEntry(TIFF.TAG_Y_RESOLUTION, STANDARD_DPI));
        Entry resUnit = metadata.getIFD().getEntryById(TIFF.TAG_RESOLUTION_UNIT);
        entries.put(TIFF.TAG_RESOLUTION_UNIT,
                resUnit != null ? resUnit : new TIFFEntry(TIFF.TAG_RESOLUTION_UNIT, TIFFBaseline.RESOLUTION_UNIT_DPI));

        // TODO: RowsPerStrip - can be entire image (or even 2^32 -1), but it's recommended to write "about 8K bytes" per strip
        entries.put(TIFF.TAG_ROWS_PER_STRIP, new TIFFEntry(TIFF.TAG_ROWS_PER_STRIP, Integer.MAX_VALUE)); // TODO: Allowed but not recommended
        // - StripByteCounts - for no compression, entire image data... (TODO: How to know the byte counts prior to writing data?)
        TIFFEntry dummyStripByteCounts = new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS, -1);
        entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, dummyStripByteCounts); // Updated later
        // - StripOffsets - can be offset to single strip only (TODO: but how large is the IFD data...???)
        TIFFEntry dummyStripOffsets = new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, -1);
        entries.put(TIFF.TAG_STRIP_OFFSETS, dummyStripOffsets); // Updated later

        // TODO: If tiled, write tile indexes etc
        // Depending on param.getTilingMode
        long nextIFDPointer = -1;
        long stripOffset = -1;
        long stripByteCount = 0;
        if (compression == TIFFBaseline.COMPRESSION_NONE) {
            // This implementation, allows semi-streaming-compatible uncompressed TIFFs
            long streamOffset = imageOutput.getStreamPosition() + exifWriter.computeIFDSize(entries.values()) + 4; // IFD + 4 byte EOF, (header, ifd0 + n images already written)

            entries.remove(dummyStripByteCounts);
            entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS,
                    renderedImage.getWidth() * renderedImage.getHeight() * numComponents));
            entries.remove(dummyStripOffsets);
            entries.put(TIFF.TAG_STRIP_OFFSETS, new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, streamOffset));

            long idfOffset = exifWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes case of ordering tags
            nextIFDPointer = imageOutput.getStreamPosition();
            // Update IFD0 pointer
            imageOutput.seek(lastIFDPointer);
            imageOutput.writeInt((int) idfOffset);
            imageOutput.seek(nextIFDPointer);
            imageOutput.flush();
            imageOutput.writeInt(0); // EOF
        }

        stripOffset = imageOutput.getStreamPosition();
        // TODO: Create compressor stream per Tile/Strip
        if (compression == TIFFExtension.COMPRESSION_JPEG) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");

            if (!writers.hasNext()) {
                // This can only happen if someone deliberately uninstalled it
                throw new IIOException("No JPEG ImageWriter found!");
            }

            ImageWriter jpegWriter = writers.next();
            try {
                jpegWriter.setOutput(new SubImageOutputStream(imageOutput));
                jpegWriter.write(renderedImage);
            }
            finally {
                jpegWriter.dispose();
            }
        }
        else {
            // Write image data
            writeImageData(createCompressorStream(renderedImage, param, entries), renderedImage, numComponents, bandOffsets,
                    bitOffsets);
        }
        stripByteCount = imageOutput.getStreamPosition() - stripOffset;

        // Update IFD0-pointer, and write IFD
        if (compression != TIFFBaseline.COMPRESSION_NONE) {
            entries.remove(dummyStripOffsets);
            entries.put(TIFF.TAG_STRIP_OFFSETS, new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, stripOffset));
            entries.remove(dummyStripByteCounts);
            entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS, stripByteCount));

            long idfOffset = exifWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes case of ordering tags
            nextIFDPointer = imageOutput.getStreamPosition();
            // Update IFD0 pointer
            imageOutput.seek(lastIFDPointer);
            imageOutput.writeInt((int) idfOffset);
            imageOutput.seek(nextIFDPointer);
            imageOutput.flush();
            imageOutput.writeInt(0); // EOF
        }

        return nextIFDPointer;
    }

    private DataOutput createCompressorStream(RenderedImage image, ImageWriteParam param, Map<Integer, Entry> entries) {
        /*
        36 MB test data:

        No compression:
        Write time: 450 ms
        output.length: 36000226

        PackBits:
        Write time: 688 ms
        output.length: 30322187

        Deflate, BEST_SPEED (1):
        Write time: 1276 ms
        output.length: 14128866

        Deflate, 2:
        Write time: 1297 ms
        output.length: 13848735

        Deflate, 3:
        Write time: 1594 ms
        output.length: 13103224

        Deflate, 4:
        Write time: 1663 ms
        output.length: 13380899 (!!)

        5
        Write time: 1941 ms
        output.length: 13171244

        6
        Write time: 2311 ms
        output.length: 12845101

        7: Write time: 2853 ms
        output.length: 12759426

        8:
        Write time: 4429 ms
        output.length: 12624517

        Deflate: DEFAULT_COMPRESSION (6?):
        Write time: 2357 ms
        output.length: 12845101

        Deflate, BEST_COMPRESSION (9):
        Write time: 4998 ms
        output.length: 12600399
         */

        // Use predictor by default for LZW and ZLib/Deflate
        // TODO: Unless explicitly disabled in TIFFImageWriteParam
        int compression = (int) entries.get(TIFF.TAG_COMPRESSION).getValue();
        OutputStream stream;

        switch (compression) {
            case TIFFBaseline.COMPRESSION_NONE:
                return imageOutput;
            case TIFFBaseline.COMPRESSION_PACKBITS:
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new EncoderStream(stream, new PackBitsEncoder(), true);
                // NOTE: PackBits + Predictor is possible, but not generally supported, disable it by default
                // (and probably not even allow it, see http://stackoverflow.com/questions/20337400/tiff-packbits-compression-with-predictor-step)
//                stream = new HorizontalDifferencingStream(stream, image.getTileWidth(), image.getTile(0, 0).getNumBands(), image.getColorModel().getComponentSize(0), imageOutput.getByteOrder());
                return new DataOutputStream(stream);

            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
                int deflateSetting = Deflater.BEST_SPEED; // This is consistent with default compression quality being 1.0 and 0 meaning max compression...
                if (param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
                    // TODO: Determine how to interpret compression quality...
                    // Docs says:
                    // A compression quality setting of 0.0 is most generically interpreted as "high compression is important,"
                    // while a setting of 1.0 is most generically interpreted as "high image quality is important."
                    // Is this what JAI TIFFImageWriter (TIFFDeflater) does? No, it does:
                    /*
                    if (param & compression etc...) {
                        float quality = param.getCompressionQuality();
                        deflateLevel = (int)(1 + 8*quality);
                    } else {
                        deflateLevel = Deflater.DEFAULT_COMPRESSION;
                    }
                    */
                    // PS: PNGImageWriter just uses hardcoded BEST_COMPRESSION... :-P
                    deflateSetting = 9 - Math.round(8 * (param.getCompressionQuality())); // This seems more correct
                }

                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new DeflaterOutputStream(stream, new Deflater(deflateSetting), 1024);
                stream = new HorizontalDifferencingStream(stream, image.getTileWidth(), image.getTile(0, 0).getNumBands(), image.getColorModel().getComponentSize(0), imageOutput.getByteOrder());

                return new DataOutputStream(stream);

            case TIFFExtension.COMPRESSION_LZW:
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new EncoderStream(stream, new LZWEncoder((image.getTileWidth() * image.getTileHeight()
                        * image.getTile(0, 0).getNumBands() * image.getColorModel().getComponentSize(0) + 7) / 8));
                stream = new HorizontalDifferencingStream(stream, image.getTileWidth(), image.getTile(0, 0).getNumBands(),
                        image.getColorModel().getComponentSize(0), imageOutput.getByteOrder());

                return new DataOutputStream(stream);
            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
            case TIFFExtension.COMPRESSION_CCITT_T4:
            case TIFFExtension.COMPRESSION_CCITT_T6:
                long option = 0L;
                if (compression != TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE) {
                    option = (long) entries.get(compression == TIFFExtension.COMPRESSION_CCITT_T4
                                                ? TIFF.TAG_GROUP3OPTIONS
                                                : TIFF.TAG_GROUP4OPTIONS).getValue();
                }
                Entry fillOrderEntry = entries.get(TIFF.TAG_FILL_ORDER);
                int fillOrder = (int) (fillOrderEntry != null
                                       ? fillOrderEntry.getValue()
                                       : TIFFBaseline.FILL_LEFT_TO_RIGHT);
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new CCITTFaxEncoderStream(stream, image.getTileWidth(), image.getTileHeight(), compression, fillOrder, option);

                return new DataOutputStream(stream);
        }

        throw new IllegalArgumentException(String.format("Unsupported TIFF compression: %d", compression));
    }

    private int getPhotometricInterpretation(final ColorModel colorModel) {
        if (colorModel.getPixelSize() == 1) {
            if (colorModel instanceof IndexColorModel) {
                if (colorModel.getRGB(0) == 0xFFFFFFFF && colorModel.getRGB(1) == 0xFF000000) {
                    return TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO;
                }
                else if (colorModel.getRGB(0) != 0xFF000000 || colorModel.getRGB(1) != 0xFFFFFFFF) {
                    return TIFFBaseline.PHOTOMETRIC_PALETTE;
                }
                // Else, fall through to default, BLACK_IS_ZERO
            }

            return TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO;
        }
        else if (colorModel instanceof IndexColorModel) {
            return TIFFBaseline.PHOTOMETRIC_PALETTE;
        }

        switch (colorModel.getColorSpace().getType()) {
            case ColorSpace.TYPE_GRAY:
                return TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO;
            case ColorSpace.TYPE_RGB:
                return TIFFBaseline.PHOTOMETRIC_RGB;
            case ColorSpace.TYPE_CMYK:
                return TIFFExtension.PHOTOMETRIC_SEPARATED;
        }

        throw new IllegalArgumentException("Can't determine PhotometricInterpretation for color model: " + colorModel);
    }

    private short[] createColorMap(final IndexColorModel colorModel) {
        // TIFF6.pdf p. 23:
        // A TIFF color map is stored as type SHORT, count = 3 * (2^BitsPerSample)
        // "In a TIFF ColorMap, all the Red values come first, followed by the Green values, then the Blue values.
        // In the ColorMap, black is represented by 0,0,0 and white is represented by 65535, 65535, 65535."
        short[] colorMap = new short[(int) (3 * Math.pow(2, colorModel.getPixelSize()))];

        for (int i = 0; i < colorModel.getMapSize(); i++) {
            int color = colorModel.getRGB(i);
            colorMap[i] = (short) upScale((color >> 16) & 0xff);
            colorMap[i + colorMap.length / 3] = (short) upScale((color >> 8) & 0xff);
            colorMap[i + 2 * colorMap.length / 3] = (short) upScale((color) & 0xff);
        }

        return colorMap;
    }

    private int upScale(final int color) {
        return 257 * color;
    }

    private short[] asShortArray(final int[] integers) {
        short[] shorts = new short[integers.length];

        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) integers[i];
        }

        return shorts;
    }

    private void writeImageData(DataOutput stream, RenderedImage renderedImage, int numComponents, int[] bandOffsets, int[] bitOffsets) throws IOException {
        // Store 3BYTE, 4BYTE as is (possibly need to re-arrange to RGB order)
        // Store INT_RGB as 3BYTE, INT_ARGB as 4BYTE?, INT_ABGR must be re-arranged
        // Store IndexColorModel as is
        // Store BYTE_GRAY as is
        // Store USHORT_GRAY as is

        processImageStarted(0);

        final int minTileY = renderedImage.getMinTileY();
        final int maxYTiles = minTileY + renderedImage.getNumYTiles();
        final int minTileX = renderedImage.getMinTileX();
        final int maxXTiles = minTileX + renderedImage.getNumXTiles();

        // Use buffer to have longer, better performing writes
        final int tileHeight = renderedImage.getTileHeight();
        final int tileWidth = renderedImage.getTileWidth();

        // TODO: SampleSize may differ between bands/banks
        int sampleSize = renderedImage.getSampleModel().getSampleSize(0);
        final ByteBuffer buffer;
        if (sampleSize == 1) {
            buffer = ByteBuffer.allocate((tileWidth + 7) / 8);
        }
        else {
            buffer = ByteBuffer.allocate(tileWidth * renderedImage.getSampleModel().getNumBands() * sampleSize / 8);
        }
        // System.err.println("tileWidth: " + tileWidth);

        for (int yTile = minTileY; yTile < maxYTiles; yTile++) {
            for (int xTile = minTileX; xTile < maxXTiles; xTile++) {
                final Raster tile = renderedImage.getTile(xTile, yTile);
                final DataBuffer dataBuffer = tile.getDataBuffer();
                final int numBands = tile.getNumBands();

                switch (dataBuffer.getDataType()) {
                    case DataBuffer.TYPE_BYTE:

//                        System.err.println("Writing " + numBands + "BYTE -> " + numBands + "BYTE");
                        for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                            for (int y = 0; y < tileHeight; y++) {
                                int steps = sampleSize == 1 ? (tileWidth + 7) / 8 : tileWidth;
                                final int yOff = y * steps * numBands;

                                for (int x = 0; x < steps; x++) {
                                    final int xOff = yOff + x * numBands;

                                    for (int s = 0; s < numBands; s++) {
                                        buffer.put((byte) (dataBuffer.getElem(b, xOff + bandOffsets[s]) & 0xff));
                                    }
                                }

                                flushBuffer(buffer, stream);

                                if (stream instanceof DataOutputStream) {
                                    DataOutputStream dataOutputStream = (DataOutputStream) stream;
                                    dataOutputStream.flush();
                                }
                            }
                        }

                        break;

                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_SHORT:
                        if (numComponents == 1) {
                            // TODO: This is foobar...
//                            System.err.println("Writing USHORT -> " + numBands * 2 + "_BYTES");
                            for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                                for (int y = 0; y < tileHeight; y++) {
                                    final int yOff = y * tileWidth;

                                    for (int x = 0; x < tileWidth; x++) {
                                        final int xOff = yOff + x;

                                        buffer.putShort((short) (dataBuffer.getElem(b, xOff) & 0xffff));
                                    }

                                    flushBuffer(buffer, stream);

                                    if (stream instanceof DataOutputStream) {
                                        DataOutputStream dataOutputStream = (DataOutputStream) stream;
                                        dataOutputStream.flush();
                                    }
                                }
                            }
                        }
                        else {
//                            for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
//                                for (int y = 0; y < tileHeight; y++) {
//                                    final int yOff = y * tileWidth;
//
//                                    for (int x = 0; x < tileWidth; x++) {
//                                        final int xOff = yOff + x;
//                                        int element = dataBuffer.getElem(b, xOff);
//
//                                        for (int s = 0; s < numBands; s++) {
//                                            buffer.put((byte) ((element >> bitOffsets[s]) & 0xff));
//                                        }
//                                    }
//
//                                    flushBuffer(buffer, stream);
//                                    if (stream instanceof DataOutputStream) {
//                                        DataOutputStream dataOutputStream = (DataOutputStream) stream;
//                                        dataOutputStream.flush();
//                                    }
//                                }
//                            }
                            throw new IllegalArgumentException("Not implemented for data type: " + dataBuffer.getDataType());
                        }

                        break;

                    case DataBuffer.TYPE_INT:
                        // TODO: This is incorrect for 32 bits/sample, only works for packed (INT_(A)RGB)
//                        System.err.println("Writing INT -> " + numBands + "_BYTES");
                        for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                            for (int y = 0; y < tileHeight; y++) {
                                final int yOff = y * tileWidth;

                                for (int x = 0; x < tileWidth; x++) {
                                    final int xOff = yOff + x;
                                    int element = dataBuffer.getElem(b, xOff);

                                    for (int s = 0; s < numBands; s++) {
                                        buffer.put((byte) ((element >> bitOffsets[s]) & 0xff));
                                    }
                                }

                                flushBuffer(buffer, stream);
                                if (stream instanceof DataOutputStream) {
                                    DataOutputStream dataOutputStream = (DataOutputStream) stream;
                                    dataOutputStream.flush();
                                }
                            }
                        }

                        break;
                    default:
                        throw new IllegalArgumentException("Not implemented for data type: " + dataBuffer.getDataType());
                }
            }

            // TODO: Need to flush/start new compression for each row, for proper LZW/PackBits/Deflate/ZLib
            if (stream instanceof DataOutputStream) {
                DataOutputStream dataOutputStream = (DataOutputStream) stream;
                dataOutputStream.flush();
            }

            // TODO: Report better progress
            processImageProgress((100f * yTile) / maxYTiles);
        }

        if (stream instanceof DataOutputStream) {
            DataOutputStream dataOutputStream = (DataOutputStream) stream;
            dataOutputStream.close();
        }

        processImageComplete();
    }

    // TODO: Would be better to solve this on stream level... But writers would then have to explicitly flush the buffer before done.
    private void flushBuffer(final ByteBuffer buffer, final DataOutput stream) throws IOException {
        buffer.flip();
        stream.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());

        buffer.clear();
    }

    // Metadata

    @Override
    public TIFFImageMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return initMeta(null, imageType, param);
    }

    @Override
    public TIFFImageMetadata convertImageMetadata(final IIOMetadata inData,
                                                  final ImageTypeSpecifier imageType,
                                                  final ImageWriteParam param) {
        Validate.notNull(inData, "inData");
        Validate.notNull(imageType, "imageType");

        Directory ifd;

        if (inData instanceof TIFFImageMetadata) {
            ifd = ((TIFFImageMetadata) inData).getIFD();
        }
        else {
            TIFFImageMetadata outData = new TIFFImageMetadata(Collections.<Entry>emptySet());

            try {
                if (Arrays.asList(inData.getMetadataFormatNames()).contains(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME)) {
                    outData.setFromTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, inData.getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME));
                }
                else if (inData.isStandardMetadataFormatSupported()) {
                    outData.setFromTree(IIOMetadataFormatImpl.standardMetadataFormatName, inData.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName));
                }
                else {
                    // Unknown format, we can't convert it
                    return null;
                }
            }
            catch (IIOInvalidTreeException e) {
                // TODO: How to issue warning when warning requires imageIndex??? Use -1?
            }

            ifd = outData.getIFD();
        }

        // Overwrite in values with values from imageType and param as needed
        return initMeta(ifd, imageType, param);
    }

    private TIFFImageMetadata initMeta(final Directory ifd, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        Validate.notNull(imageType, "imageType");

        Map<Integer, Entry> entries = new LinkedHashMap<>(ifd != null ? ifd.size() + 10 : 20);

        if (ifd != null) {
            for (Entry entry : ifd) {
                entries.put((Integer) entry.getIdentifier(), entry);
            }
        }

        // TODO: Set values from imageType
        entries.put(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFF.TYPE_SHORT, getPhotometricInterpretation(imageType.getColorModel())));

        // TODO: Set values from param if != null + combined values...

        return new TIFFImageMetadata(entries.values());
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(final ImageWriteParam param) {
        return super.getDefaultStreamMetadata(param);
    }

    @Override
    public IIOMetadata convertStreamMetadata(final IIOMetadata inData, final ImageWriteParam param) {
        return super.convertStreamMetadata(inData, param);
    }

    // Param

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new TIFFImageWriteParam();
    }

    @Override
    public boolean canWriteSequence() {
        return true;
    }

    @Override
    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        if (isWritingSequence) {
            throw new IllegalStateException("sequence writing has already been started!");
        }

        // Ignore streamMetadata. ByteOrder is determined from OutputStream
        assertOutput();
        isWritingSequence = true;
        sequenceExifWriter = new EXIFWriter();
        sequenceExifWriter.writeTIFFHeader(imageOutput);
        sequenceLastIFDPos = imageOutput.getStreamPosition();
        imageOutput.writeInt(0); // IFD0
    }

    @Override
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
        if (!isWritingSequence) {
            throw new IllegalStateException("prepareWriteSequence() must be called before writeToSequence()!");
        }
        sequenceLastIFDPos = writePage(image, param, sequenceExifWriter, sequenceLastIFDPos);
    }

    @Override
    public void endWriteSequence() throws IOException {
        if (!isWritingSequence) {
            throw new IllegalStateException("prepareWriteSequence() must be called before endWriteSequence()!");
        }

        isWritingSequence = false;
        sequenceExifWriter = null;
        sequenceLastIFDPos = -1;
        imageOutput.flush();
    }

    @Override
    protected void resetMembers() {
        super.resetMembers();

        isWritingSequence = false;
        sequenceExifWriter = null;
        sequenceLastIFDPos = -1;
    }

    // Test

    public static void main(String[] args) throws IOException {
        int argIdx = 0;

        // TODO: Proper argument parsing: -t <type> -c <compression>
        int type = args.length > argIdx + 1 ? Integer.parseInt(args[argIdx++]) : -1;
        int compression = args.length > argIdx + 1 ? Integer.parseInt(args[argIdx++]) : 0;

        if (args.length <= argIdx) {
            System.err.println("No file specified");
            System.exit(1);
        }

        File file = new File(args[argIdx++]);

        BufferedImage original;
//        BufferedImage original = ImageIO.read(file);
        ImageInputStream inputStream = ImageIO.createImageInputStream(file);
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);

            if (!readers.hasNext()) {
                System.err.println("No reader for: " + file);
                System.exit(1);
            }

            ImageReader reader = readers.next();
            reader.setInput(inputStream);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(reader.getRawImageType(0));

            if (param.getDestinationType() == null) {
                Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

                while (types.hasNext()) {
                    ImageTypeSpecifier typeSpecifier = types.next();

                    if (typeSpecifier.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
                        param.setDestinationType(typeSpecifier);
                    }
                }
            }

            System.err.println("param.getDestinationType(): " + param.getDestinationType());

            original = reader.read(0, param);
        }
        finally {
            inputStream.close();
        }

        System.err.println("original: " + original);

//        BufferedImage image = original;
//        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
//        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
//        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_BGR);
//        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage image;
        if (type <= 0 || type == original.getType()) {
            image = original;
        }
        else if (type == BufferedImage.TYPE_BYTE_INDEXED) {
//            image = ImageUtil.createIndexed(original, 256, null, ImageUtil.COLOR_SELECTION_QUALITY | ImageUtil.DITHER_DIFFUSION_ALTSCANS);
            image = ImageUtil.createIndexed(original, 256, null, ImageUtil.COLOR_SELECTION_FAST | ImageUtil.DITHER_DIFFUSION_ALTSCANS);
        }
        else {
            image = new BufferedImage(original.getWidth(), original.getHeight(), type);
            Graphics2D graphics = image.createGraphics();

            try {
                graphics.drawImage(original, 0, 0, null);
            }
            finally {
                graphics.dispose();
            }
        }

        original = null;

        File output = File.createTempFile(file.getName().replace('.', '-'), ".tif");
//        output.deleteOnExit();

        System.err.println("output: " + output);
        TIFFImageWriter writer = new TIFFImageWriter(null);
//        ImageWriter writer = ImageIO.getImageWritersByFormatName("PNG").next();
//        ImageWriter writer = ImageIO.getImageWritersByFormatName("BMP").next();
        ImageOutputStream stream = ImageIO.createImageOutputStream(output);

        try {
            writer.setOutput(stream);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            param.setCompressionType("None");
//            param.setCompressionType("PackBits");
//            param.setCompressionType("ZLib");
            param.setCompressionType(param.getCompressionTypes()[compression]);
//            if (compression == 2) {
//                param.setCompressionQuality(0);
//            }
            System.err.println("compression: " + param.getLocalizedCompressionTypeName());

            long start = System.currentTimeMillis();
            writer.write(null, new IIOImage(image, null, null), param);
            System.err.println("Write time: " + (System.currentTimeMillis() - start) + " ms");
        }
        finally {
            stream.close();
        }

        System.err.println("output.length: " + output.length());

        // TODO: Support writing multipage TIFF
//        ImageOutputStream stream = ImageIO.createImageOutputStream(output);
//        try {
//            writer.setOutput(stream);
//            writer.prepareWriteSequence(null);
//            for(int i = 0; i < images.size(); i ++){
//                writer.writeToSequence(new IIOImage(images.get(i), null, null), null);
//            }
//            writer.endWriteSequence();
//        }
//        finally {
//            stream.close();
//        }
//        writer.dispose();

        image = null;

        BufferedImage read = ImageIO.read(output);
        System.err.println("read: " + read);

        TIFFImageReader.showIt(read, output.getName());
    }
}
