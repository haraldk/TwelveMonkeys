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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFWriter;
import com.twelvemonkeys.imageio.stream.SubImageOutputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.enc.PackBitsEncoder;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.*;
import javax.imageio.event.IIOWriteWarningListener;
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
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static com.twelvemonkeys.imageio.plugins.tiff.TIFFStreamMetadata.configureStreamByteOrder;

/**
 * TIFFImageWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriter.java,v 1.0 18.09.13 12:46 haraldk Exp$
 */
public final class TIFFImageWriter extends ImageWriterBase {
    // Long term
    // TODO: Support tiling
    // TODO: Support thumbnails
    // TODO: Support JPEG compression of CMYK data (pending JPEGImageWriter CMYK write support)
    // ----
    // TODO: Support use-case: Transcode multi-layer PSD to multi-page TIFF with metadata (hard, as Photoshop don't store layers as multi-page TIFF...)
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
    // Support CCITT Modified Huffman compression (2)
    // Full "Baseline TIFF" support (pending CCITT compression 2)
    // CCITT compressions T.4 and T.6
    // Support storing multiple images in one stream (multi-page TIFF)
    // Support more of the ImageIO metadata (ie. compression from metadata, etc)

    private static final Rational STANDARD_DPI = new Rational(72);

    /**
     * Flag for active sequence writing
     */
    private boolean writingSequence = false;

    private int sequenceIndex = 0;

    /**
     * Metadata writer for sequence writing
     */
    private TIFFWriter sequenceTIFFWriter = null;

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

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        prepareWriteSequence(streamMetadata);
        writeToSequence(image, param);
        endWriteSequence();
    }

    private long writePage(int imageIndex, IIOImage image, ImageWriteParam param, TIFFWriter tiffWriter, long lastIFDPointerOffset)
            throws IOException {
        RenderedImage renderedImage = image.getRenderedImage();

        TIFFImageMetadata metadata = image.getMetadata() != null
                                     ? convertImageMetadata(image.getMetadata(), ImageTypeSpecifier.createFromRenderedImage(renderedImage), param)
                                     : getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(renderedImage), param);

        ColorModel colorModel = renderedImage.getColorModel();
        SampleModel sampleModel = renderedImage.getSampleModel();
        int numBands = sampleModel.getNumBands();
        int pixelSize = computePixelSize(sampleModel);

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

        // TODO: There shouldn't be necessary to create a separate map here, this should be handled in the
        // convertImageMetadata/getDefaultImageMetadata methods....
        Map<Integer, Entry> entries = new LinkedHashMap<>();
        entries.put(TIFF.TAG_IMAGE_WIDTH, new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, renderedImage.getWidth()));
        entries.put(TIFF.TAG_IMAGE_HEIGHT, new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, renderedImage.getHeight()));
        entries.put(TIFF.TAG_ORIENTATION, new TIFFEntry(TIFF.TAG_ORIENTATION, 1)); // (optional)
        entries.put(TIFF.TAG_BITS_PER_SAMPLE, new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, asShortArray(sampleModel.getSampleSize())));

        // If numComponents > numColorComponents, write ExtraSamples
        if (numBands > colorModel.getNumColorComponents()) {
            // TODO: Write per component > numColorComponents
            if (colorModel.hasAlpha()) {
                entries.put(TIFF.TAG_EXTRA_SAMPLES, new TIFFEntry(TIFF.TAG_EXTRA_SAMPLES, colorModel.isAlphaPremultiplied() ? TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA : TIFFBaseline.EXTRASAMPLE_UNASSOCIATED_ALPHA));
            }
            else {
                entries.put(TIFF.TAG_EXTRA_SAMPLES, new TIFFEntry(TIFF.TAG_EXTRA_SAMPLES, TIFFBaseline.EXTRASAMPLE_UNSPECIFIED));
            }
        }

        // Write compression field from param or metadata
        int compression;
        if ((param == null || param.getCompressionMode() == TIFFImageWriteParam.MODE_COPY_FROM_METADATA)
                && image.getMetadata() != null && metadata.getIFD().getEntryById(TIFF.TAG_COMPRESSION) != null) {
            compression = ((Number) metadata.getIFD().getEntryById(TIFF.TAG_COMPRESSION).getValue()).intValue();
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
                if (pixelSize >= 8) {
                    entries.put(TIFF.TAG_PREDICTOR, new TIFFEntry(TIFF.TAG_PREDICTOR, TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING));
                }

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

        int photometric = getPhotometricInterpretation(colorModel, compression);
        entries.put(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, photometric));

        if (photometric == TIFFBaseline.PHOTOMETRIC_PALETTE && colorModel instanceof IndexColorModel) {
            // TODO: Fix consistency between sampleModel.getSampleSize() and colorModel.getPixelSize()...
            // We should be able to support 1, 2, 4 and 8 bits per sample at least, and probably 3, 5, 6 and 7 too
            entries.put(TIFF.TAG_COLOR_MAP, new TIFFEntry(TIFF.TAG_COLOR_MAP, createColorMap((IndexColorModel) colorModel, sampleModel.getSampleSize(0))));
            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, 1));
        }
        else {
            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, numBands));

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

        // TODO: Again, this should be handled in the metadata conversion....
        // Get Software from metadata, or use default
        Entry software = metadata.getIFD().getEntryById(TIFF.TAG_SOFTWARE);
        entries.put(TIFF.TAG_SOFTWARE, software != null ? software : new TIFFEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO TIFF writer " + originatingProvider.getVersion()));

        // Copy metadata to output
        int[] copyTags = {
                TIFF.TAG_ORIENTATION,
                TIFF.TAG_DATE_TIME,
                TIFF.TAG_DOCUMENT_NAME,
                TIFF.TAG_IMAGE_DESCRIPTION,
                TIFF.TAG_MAKE,
                TIFF.TAG_MODEL,
                TIFF.TAG_PAGE_NAME,
                TIFF.TAG_PAGE_NUMBER,
                TIFF.TAG_ARTIST,
                TIFF.TAG_HOST_COMPUTER,
                TIFF.TAG_COPYRIGHT
        };
        for (int tagID : copyTags) {
            Entry entry = metadata.getIFD().getEntryById(tagID);
            if (entry != null) {
                entries.put(tagID, entry);
            }
        }

        // Get X/YResolution and ResolutionUnit from metadata if set, otherwise use defaults
        // TODO: Add logic here OR in metadata merging, to make sure these 3 values are consistent.
        Entry xRes = metadata.getIFD().getEntryById(TIFF.TAG_X_RESOLUTION);
        entries.put(TIFF.TAG_X_RESOLUTION, xRes != null ? xRes : new TIFFEntry(TIFF.TAG_X_RESOLUTION, STANDARD_DPI));
        Entry yRes = metadata.getIFD().getEntryById(TIFF.TAG_Y_RESOLUTION);
        entries.put(TIFF.TAG_Y_RESOLUTION, yRes != null ? yRes : new TIFFEntry(TIFF.TAG_Y_RESOLUTION, STANDARD_DPI));
        Entry resUnit = metadata.getIFD().getEntryById(TIFF.TAG_RESOLUTION_UNIT);
        entries.put(TIFF.TAG_RESOLUTION_UNIT, resUnit != null ? resUnit : new TIFFEntry(TIFF.TAG_RESOLUTION_UNIT, TIFFBaseline.RESOLUTION_UNIT_DPI));

        // TODO: RowsPerStrip - can be entire image (or even 2^32 -1), but it's recommended to write "about 8K bytes" per strip
        entries.put(TIFF.TAG_ROWS_PER_STRIP, new TIFFEntry(TIFF.TAG_ROWS_PER_STRIP, renderedImage.getHeight()));
        // - StripByteCounts - for no compression, entire image data... (TODO: How to know the byte counts prior to writing data?)
        entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS, -1)); // Updated later
        // - StripOffsets - can be offset to single strip only (TODO: but how large is the IFD data...???)
        entries.put(TIFF.TAG_STRIP_OFFSETS, new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, -1)); // Updated later

        // TODO: If tiled, write tile indexes etc
        // Depending on param.getTilingMode
        long nextIFDPointerOffset = -1;

        if (compression == TIFFBaseline.COMPRESSION_NONE) {
            // This implementation, allows semi-streaming-compatible uncompressed TIFFs
            long streamPosition = imageOutput.getStreamPosition();

            long ifdSize = tiffWriter.computeIFDSize(entries.values());
            long stripOffset = streamPosition + 4 +  ifdSize + 4;
            long stripByteCount = (renderedImage.getWidth() * renderedImage.getHeight() * pixelSize + 7) / 8;

            entries.put(TIFF.TAG_STRIP_OFFSETS, new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, TIFF.TYPE_LONG, stripOffset));
            entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS, TIFF.TYPE_LONG, stripByteCount));

            long ifdPointer = tiffWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes case of ordering tags
            nextIFDPointerOffset = imageOutput.getStreamPosition();

            // If we have a previous IFD, update pointer
            if (streamPosition > lastIFDPointerOffset) {
                imageOutput.seek(lastIFDPointerOffset);
                imageOutput.writeInt((int) ifdPointer);
                imageOutput.seek(nextIFDPointerOffset);
            }

            imageOutput.writeInt(0); // Update next IFD pointer later
        }
        else {
            imageOutput.writeInt(0); // Update current IFD pointer later
        }

        long stripOffset = imageOutput.getStreamPosition();

        // TODO: Create compressor stream per Tile/Strip
        // TODO: Cache JPEGImageWriter, dispose in dispose() method
        if (compression == TIFFExtension.COMPRESSION_JPEG) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");

            if (!writers.hasNext()) {
                // This can only happen if someone deliberately uninstalled it
                throw new IIOException("No JPEG ImageWriter found!");
            }

            ImageWriter jpegWriter = writers.next();
            try {
                jpegWriter.setOutput(new SubImageOutputStream(imageOutput));
                ListenerDelegate listener = new ListenerDelegate(imageIndex);
                jpegWriter.addIIOWriteProgressListener(listener);
                jpegWriter.addIIOWriteWarningListener(listener);
                jpegWriter.write(null, image, copyParams(param, jpegWriter));
            }
            finally {
                jpegWriter.dispose();
            }
        }
        else {
            // Write image data
            writeImageData(createCompressorStream(renderedImage, param, entries), imageIndex, renderedImage, numBands, bandOffsets, bitOffsets);
        }

        long stripByteCount = imageOutput.getStreamPosition() - stripOffset;

        // Update IFD0-pointer, and write IFD
        if (compression != TIFFBaseline.COMPRESSION_NONE) {
            entries.put(TIFF.TAG_STRIP_OFFSETS, new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, TIFF.TYPE_LONG, stripOffset));
            entries.put(TIFF.TAG_STRIP_BYTE_COUNTS, new TIFFEntry(TIFF.TAG_STRIP_BYTE_COUNTS, TIFF.TYPE_LONG, stripByteCount));

            long ifdPointer = tiffWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes case of ordering tags

            nextIFDPointerOffset = imageOutput.getStreamPosition();

            // TODO: This is slightly duped....
            // However, need to update here, because to the writeIFD method writes the pointer, but at the incorrect offset
            // TODO: Refactor writeIFD to take an offset
            imageOutput.seek(lastIFDPointerOffset);
            imageOutput.writeInt((int) ifdPointer);
            imageOutput.seek(nextIFDPointerOffset);

            imageOutput.writeInt(0); // Next IFD pointer updated later
        }

        return nextIFDPointerOffset;
    }

    // TODO: Candidate util method
    private ImageWriteParam copyParams(final ImageWriteParam param, final ImageWriter writer) {
        if (param == null) {
            return null;
        }

        // Always safe
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setSourceSubsampling(param.getSourceXSubsampling(), param.getSourceYSubsampling(), param.getSubsamplingXOffset(), param.getSubsamplingYOffset());
        writeParam.setSourceRegion(param.getSourceRegion());
        writeParam.setSourceBands(param.getSourceBands());

        // Only if canWriteCompressed()
        writeParam.setCompressionMode(param.getCompressionMode());
        if (param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
            writeParam.setCompressionQuality(param.getCompressionQuality());
        }

        return writeParam;
    }

    // TODO: Candidate util method
    private int computePixelSize(final SampleModel sampleModel) {
        int size = 0;

        for (int i = 0; i < sampleModel.getNumBands(); i++) {
            size += sampleModel.getSampleSize(i);
        }

        return size;
    }

    private DataOutput createCompressorStream(final RenderedImage image, final ImageWriteParam param, final Map<Integer, Entry> entries) {
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

        int samplesPerPixel = (Integer) entries.get(TIFF.TAG_SAMPLES_PER_PIXEL).getValue();
        int bitPerSample = ((short[]) entries.get(TIFF.TAG_BITS_PER_SAMPLE).getValue())[0];

        // Use predictor by default for LZW and ZLib/Deflate
        // TODO: Unless explicitly disabled in TIFFImageWriteParam
        int compression = ((Number) entries.get(TIFF.TAG_COMPRESSION).getValue()).intValue();
        OutputStream stream;

        switch (compression) {
            case TIFFBaseline.COMPRESSION_NONE:
                return imageOutput;
            case TIFFBaseline.COMPRESSION_PACKBITS:
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new EncoderStream(stream, new PackBitsEncoder(), true);
                // NOTE: PackBits + Predictor is possible, but not generally supported, disable it by default
                // (and probably not even allow it, see http://stackoverflow.com/questions/20337400/tiff-packbits-compression-with-predictor-step)
                return new DataOutputStream(stream);

            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
                // NOTE: This interpretation does the opposite of the JAI TIFFImageWriter, but seems more correct.
                // API Docs says:
                // A compression quality setting of 0.0 is most generically interpreted as "high compression is important,"
                // while a setting of 1.0 is most generically interpreted as "high image quality is important."
                // However, the JAI TIFFImageWriter uses:
                //    if (param & compression etc...) {
                //        float quality = param.getCompressionQuality();
                //        deflateLevel = (int)(1 + 8*quality);
                //    } else {
                //        deflateLevel = Deflater.DEFAULT_COMPRESSION;
                //    }
                // (in other words, 0.0 means 1 == BEST_SPEED, 1.0 means 9 == BEST_COMPRESSION)
                // PS: PNGImageWriter just uses hardcoded BEST_COMPRESSION... :-P
                int deflateSetting = Deflater.BEST_SPEED; // This is consistent with default compression quality being 1.0 and 0 meaning max compression...
                if (param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
                    deflateSetting = Deflater.BEST_COMPRESSION - Math.round((Deflater.BEST_COMPRESSION - 1) * param.getCompressionQuality());
                }

                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new DeflaterOutputStream(stream, new Deflater(deflateSetting), 1024);
                if (entries.containsKey(TIFF.TAG_PREDICTOR) && entries.get(TIFF.TAG_PREDICTOR).getValue().equals(TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING)) {
                    stream = new HorizontalDifferencingStream(stream, image.getTileWidth(), samplesPerPixel, bitPerSample, imageOutput.getByteOrder());
                }

                return new DataOutputStream(stream);

            case TIFFExtension.COMPRESSION_LZW:
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new EncoderStream(stream, new LZWEncoder(((image.getTileWidth() * samplesPerPixel * bitPerSample + 7) / 8) * image.getTileHeight()));
                if (entries.containsKey(TIFF.TAG_PREDICTOR) && entries.get(TIFF.TAG_PREDICTOR).getValue().equals(TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING)) {
                    stream = new HorizontalDifferencingStream(stream, image.getTileWidth(), samplesPerPixel, bitPerSample, imageOutput.getByteOrder());
                }

                return new DataOutputStream(stream);

            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
            case TIFFExtension.COMPRESSION_CCITT_T4:
            case TIFFExtension.COMPRESSION_CCITT_T6:
                if (image.getSampleModel().getNumBands() != 1 || image.getSampleModel().getSampleSize(0) != 1) {
                    throw new IllegalArgumentException("CCITT compressions supports 1 sample/pixel, 1 bit/sample only");
                }

                long option = 0L;

                if (compression != TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE) {
                    Entry optionsEntry = entries.get(compression == TIFFExtension.COMPRESSION_CCITT_T4 ? TIFF.TAG_GROUP3OPTIONS : TIFF.TAG_GROUP4OPTIONS);
                    option = ((Number) optionsEntry.getValue()).longValue();
                }

                Entry fillOrderEntry = entries.get(TIFF.TAG_FILL_ORDER);
                int fillOrder = (int) (fillOrderEntry != null ? fillOrderEntry.getValue() : TIFFBaseline.FILL_LEFT_TO_RIGHT);
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new CCITTFaxEncoderStream(stream, image.getTileWidth(), image.getTileHeight(), compression, fillOrder, option);

                return new DataOutputStream(stream);
        }

        throw new IllegalArgumentException(String.format("Unsupported TIFF compression: %d", compression));
    }

    private int getPhotometricInterpretation(final ColorModel colorModel, int compression) {
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
                return compression == TIFFExtension.COMPRESSION_JPEG ? TIFFExtension.PHOTOMETRIC_YCBCR : TIFFBaseline.PHOTOMETRIC_RGB;
            case ColorSpace.TYPE_CMYK:
                return TIFFExtension.PHOTOMETRIC_SEPARATED;
        }

        throw new IllegalArgumentException("Can't determine PhotometricInterpretation for color model: " + colorModel);
    }

    private short[] createColorMap(final IndexColorModel colorModel, final int sampleSize) {
        // TIFF6.pdf p. 23:
        // A TIFF color map is stored as type SHORT, count = 3 * (2^BitsPerSample)
        // "In a TIFF ColorMap, all the Red values come first, followed by the Green values, then the Blue values.
        // In the ColorMap, black is represented by 0,0,0 and white is represented by 65535, 65535, 65535."
        short[] colorMap = new short[(int) (3 * Math.pow(2, sampleSize))];

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

    private void writeImageData(DataOutput stream, int imageIndex, RenderedImage renderedImage, int numComponents, int[] bandOffsets, int[] bitOffsets) throws IOException {
        // Store 3BYTE, 4BYTE as is (possibly need to re-arrange to RGB order)
        // Store INT_RGB as 3BYTE, INT_ARGB as 4BYTE?, INT_ABGR must be re-arranged
        // Store IndexColorModel as is
        // Store BYTE_GRAY as is
        // Store USHORT_GRAY as is

        processImageStarted(imageIndex);

        final int minTileY = renderedImage.getMinTileY();
        final int maxYTiles = minTileY + renderedImage.getNumYTiles();
        final int minTileX = renderedImage.getMinTileX();
        final int maxXTiles = minTileX + renderedImage.getNumXTiles();

        // Use buffer to have longer, better performing writes
        final int tileHeight = renderedImage.getTileHeight();
        final int tileWidth = renderedImage.getTileWidth();

        // TODO: SampleSize may differ between bands/banks
        final int sampleSize = renderedImage.getSampleModel().getSampleSize(0);
        final int numBands = renderedImage.getSampleModel().getNumBands();

        final ByteBuffer buffer = ByteBuffer.allocate((tileWidth * numBands * sampleSize + 7) / 8);

        for (int yTile = minTileY; yTile < maxYTiles; yTile++) {
            for (int xTile = minTileX; xTile < maxXTiles; xTile++) {
                final Raster tile = renderedImage.getTile(xTile, yTile);

                // Model translation
                final int offsetX = tile.getMinX() - tile.getSampleModelTranslateX();
                final int offsetY = tile.getMinY() - tile.getSampleModelTranslateY();

                // Scanline stride, not accounting for model translation
                final int stride = (tile.getSampleModel().getWidth() * sampleSize + 7) / 8;
                final DataBuffer dataBuffer = tile.getDataBuffer();

                switch (dataBuffer.getDataType()) {
                    case DataBuffer.TYPE_BYTE:
//                        System.err.println("Writing " + numBands + "BYTE -> " + numBands + "BYTE");
                        int steps = (tileWidth * sampleSize + 7) / 8;
                        // Shift needed for "packed" samples with "odd" offset
                        int shift = offsetX % 8;

                        // TODO: Generalize this code, to always use row raster
                        final WritableRaster rowRaster = shift != 0 ? tile.createCompatibleWritableRaster(tile.getWidth(), 1) : null;
                        final DataBuffer rowBuffer = shift != 0 ? rowRaster.getDataBuffer() : null;

                        for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                            for (int y = offsetY; y < tileHeight + offsetY; y++) {
                                final int yOff = y * stride * numBands;

                                if (shift != 0) {
                                    rowRaster.setDataElements(0, 0, tile.createChild(0, y - offsetY, tile.getWidth(), 1, 0, 0, null));
                                }

                                for (int x = offsetX; x < steps + offsetX; x++) {
                                    final int xOff = yOff + x * numBands;

                                    for (int s = 0; s < numBands; s++) {
                                        if (sampleSize == 8 || shift == 0) {
                                            // Normal interleaved/planar case
                                            buffer.put((byte) (dataBuffer.getElem(b, xOff + bandOffsets[s]) & 0xff));
                                        }
                                        else {
                                            // "Packed" case
                                            buffer.put((byte) (rowBuffer.getElem(b, x - offsetX + bandOffsets[s]) & 0xff));
                                        }
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
//                            System.err.println("Writing USHORT -> " + numBands * 2 + "_BYTES");

                            for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                                for (int y = offsetY; y < tileHeight + offsetY; y++) {
                                    int yOff = y * stride / 2;

                                    for (int x = offsetX; x < tileWidth + offsetX; x++) {
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
                        if (1 == numComponents) {
//                            System.err.println("Writing INT -> " + numBands * 4 + "_BYTES");

                            for (int b = 0; b < dataBuffer.getNumBanks(); b++) {
                                for (int y = offsetY; y < tileHeight + offsetY; y++) {
                                    int yOff = y * stride / 4;

                                    for (int x = offsetX; x < tileWidth + offsetX; x++) {
                                        final int xOff = yOff + x;

                                        buffer.putInt(dataBuffer.getElem(b, xOff));
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
//                            System.err.println("Writing INT -> " + numBands + "_BYTES");

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
            processImageProgress((100f * (yTile + 1)) / maxYTiles);
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
                processWarningOccurred(sequenceIndex, "Could not convert image meta data: " + e.getMessage());
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

        int compression;
        if ((param == null || param.getCompressionMode() == TIFFImageWriteParam.MODE_COPY_FROM_METADATA)
                && ifd != null && ifd.getEntryById(TIFF.TAG_COMPRESSION) != null) {
            compression = ((Number) ifd.getEntryById(TIFF.TAG_COMPRESSION).getValue()).intValue();
        }
        else {
            compression = TIFFImageWriteParam.getCompressionType(param);
        }

        int photometricInterpretation = getPhotometricInterpretation(imageType.getColorModel(), compression);
        entries.put(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFF.TYPE_SHORT, photometricInterpretation));

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
    public void prepareWriteSequence(final IIOMetadata streamMetadata) throws IOException {
        if (writingSequence) {
            throw new IllegalStateException("sequence writing has already been started!");
        }

        assertOutput();
        configureStreamByteOrder(streamMetadata, imageOutput);

        writingSequence = true;
        sequenceTIFFWriter = new TIFFWriter();
        sequenceTIFFWriter.writeTIFFHeader(imageOutput);
        sequenceLastIFDPos = imageOutput.getStreamPosition();
    }

    @Override
    public void writeToSequence(final IIOImage image, final ImageWriteParam param) throws IOException {
        if (!writingSequence) {
            throw new IllegalStateException("prepareWriteSequence() must be called before writeToSequence()!");
        }

        if (sequenceIndex > 0) {
            imageOutput.flushBefore(sequenceLastIFDPos);
            imageOutput.seek(imageOutput.length());
        }

        sequenceLastIFDPos = writePage(sequenceIndex++, image, param, sequenceTIFFWriter, sequenceLastIFDPos);
    }

    @Override
    public void endWriteSequence() throws IOException {
        if (!writingSequence) {
            throw new IllegalStateException("prepareWriteSequence() must be called before endWriteSequence()!");
        }

        writingSequence = false;
        sequenceIndex = 0;
        sequenceTIFFWriter = null;
        sequenceLastIFDPos = -1;
        imageOutput.flush();
    }

    @Override
    protected void resetMembers() {
        super.resetMembers();

        writingSequence = false;
        sequenceIndex = 0;
        sequenceTIFFWriter = null;
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

    private class ListenerDelegate extends ProgressListenerBase implements IIOWriteWarningListener {
        private final int imageIndex;

        public ListenerDelegate(final int imageIndex) {
            this.imageIndex = imageIndex;
        }

        @Override
        public void imageComplete(ImageWriter source) {
            processImageComplete();
        }

        @Override
        public void imageProgress(ImageWriter source, float percentageDone) {
            processImageProgress(percentageDone);
        }

        @Override
        public void imageStarted(ImageWriter source, int imageIndex) {
            processImageStarted(this.imageIndex);
        }

        @Override
        public void thumbnailComplete(ImageWriter source) {
            processThumbnailComplete();
        }

        @Override
        public void thumbnailProgress(ImageWriter source, float percentageDone) {
            processThumbnailProgress(percentageDone);
        }

        @Override
        public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
            processThumbnailStarted(this.imageIndex, thumbnailIndex);
        }

        @Override
        public void writeAborted(ImageWriter source) {
            processWriteAborted();
        }

        @Override
        public void warningOccurred(ImageWriter source, int imageIndex, String warning) {
            processWarningOccurred(this.imageIndex, warning);
        }
    }
}
