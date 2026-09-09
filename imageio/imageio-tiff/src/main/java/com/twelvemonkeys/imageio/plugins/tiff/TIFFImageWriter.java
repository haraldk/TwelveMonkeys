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
import com.twelvemonkeys.imageio.color.ColorProfiles;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFWriter;
import com.twelvemonkeys.imageio.stream.SubImageOutputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.imageio.util.SequenceSupport;
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
import java.awt.color.*;
import java.awt.image.*;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
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
    // Support multiple strips (about 8K per strip, as recommended by the TIFF 6.0 spec) and tiled writing

    /** The TIFF 6.0 spec recommends writing strips of about 8K bytes (before compression). */
    private static final long DEFAULT_STRIP_SIZE = 8L * 1024;

    private final SequenceSupport sequence = new SequenceSupport();

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
        SampleModel sampleModel = renderedImage.getSampleModel();

        // Need ImageTypeSpecifiers.createFromRenderedImage in this case, as the JDK method does not consider
        // palette for TYPE_BYTE_BINARY/TYPE_BYTE_INDEXED...
        ImageTypeSpecifier spec = ImageTypeSpecifiers.createFromRenderedImage(renderedImage);

        // TODO: Handle case where convertImageMetadata returns null, due to unknown metadata format, or reconsider if that's a valid case...
        TIFFImageMetadata metadata = image.getMetadata() != null
                                     ? convertImageMetadata(image.getMetadata(), spec, param)
                                     : getDefaultImageMetadata(spec, param);

        if (param != null && (param.getSourceRegion() != null
                || param.getSourceXSubsampling() != 1 || param.getSourceYSubsampling() != 1
                || param.getSourceBands() != null)) {
            processWarningOccurred(imageIndex, "Source region, subsampling and band selection are not supported, writing the complete image");
        }

        short offsetType = tiffWriter.offsetSize() == 4 ? TIFF.TYPE_LONG : TIFF.TYPE_LONG8;

        Map<Integer, Entry> entries = new LinkedHashMap<>();
        // Copy metadata to output
        Directory metadataIFD = metadata.getIFD();
        for (Entry entry : metadataIFD) {
            entries.put((Integer) entry.getIdentifier(), entry);
        }

        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        entries.put(TIFF.TAG_IMAGE_WIDTH, new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, width));
        entries.put(TIFF.TAG_IMAGE_HEIGHT, new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, height));

        int compression = ((Number) entries.get(TIFF.TAG_COMPRESSION).getValue()).intValue();

        // Tiled or striped layout, depending on the tiling settings of the param.
        // NOTE: Updates the entries with TileWidth/TileLength or RowsPerStrip
        SegmentLayout layout = computeSegmentLayout(imageIndex, param, entries, sampleModel, width, height);

        long[] segmentOffsets = new long[layout.segsAcross * layout.segsDown];
        long[] segmentByteCounts = new long[segmentOffsets.length];

        long nextIFDPointerOffset;

        if (compression == TIFFBaseline.COMPRESSION_NONE) {
            // Uncompressed data has predictable size, so we write the IFD before the image data.
            // This implementation allows semi-streaming-compatible uncompressed TIFFs
            padToWordBoundary();

            long rowSize = ((long) layout.segmentWidth * computePixelSize(sampleModel) + 7L) / 8L;

            for (int segY = 0; segY < layout.segsDown; segY++) {
                // Strips are clipped to the image height, tiles are padded to the full tile height
                long rows = layout.tiled
                            ? layout.segmentHeight
                            : Math.min(layout.segmentHeight, height - (long) segY * layout.segmentHeight);

                for (int segX = 0; segX < layout.segsAcross; segX++) {
                    segmentByteCounts[segY * layout.segsAcross + segX] = rows * rowSize;
                }
            }

            // Two passes: Compute the IFD size using placeholder offsets, then update with the final values.
            // NOTE: The IFD size depends on the number of segments, not on the offset values, so it won't change
            putSegmentEntries(entries, layout.tiled, offsetType, segmentOffsets, segmentByteCounts);
            long ifdSize = tiffWriter.computeIFDSize(entries.values());

            long dataOffset = imageOutput.getStreamPosition() + tiffWriter.offsetSize() + ifdSize + tiffWriter.offsetSize();
            for (int i = 0; i < segmentOffsets.length; i++) {
                segmentOffsets[i] = dataOffset;
                dataOffset += segmentByteCounts[i];
            }

            putSegmentEntries(entries, layout.tiled, offsetType, segmentOffsets, segmentByteCounts);

            long ifdPointer = tiffWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes care of ordering tags
            nextIFDPointerOffset = imageOutput.getStreamPosition();

            tiffWriter.writeOffset(imageOutput, 0); // Update next IFD pointer later

            // The image data follows the IFD directly
            writeSegments(imageIndex, renderedImage, param, entries, layout, segmentOffsets, segmentByteCounts);

            // Link the previous IFD pointer (or the stream header) to the IFD just written.
            // NOTE: When at the start of the chain, writeIFD has already written the pointer, rewriting it is harmless
            long endPosition = imageOutput.getStreamPosition();
            imageOutput.seek(lastIFDPointerOffset);
            tiffWriter.writeOffset(imageOutput, ifdPointer);
            imageOutput.seek(endPosition);
        }
        else {
            if (imageOutput.getStreamPosition() == lastIFDPointerOffset) {
                // At the start of the IFD chain: Reserve space for the IFD pointer, written below
                tiffWriter.writeOffset(imageOutput, 0);
            }

            // Write the image data, one segment (strip or tile) at a time, collecting offsets and byte counts
            if (compression == TIFFExtension.COMPRESSION_JPEG) {
                writeJPEGSegments(imageIndex, image, renderedImage, param, layout, segmentOffsets, segmentByteCounts);
            }
            else {
                writeSegments(imageIndex, renderedImage, param, entries, layout, segmentOffsets, segmentByteCounts);
            }

            putSegmentEntries(entries, layout.tiled, offsetType, segmentOffsets, segmentByteCounts);

            padToWordBoundary();

            long ifdPointer = tiffWriter.writeIFD(entries.values(), imageOutput); // NOTE: Writer takes care of ordering tags
            nextIFDPointerOffset = imageOutput.getStreamPosition();

            tiffWriter.writeOffset(imageOutput, 0); // Update next IFD pointer later

            // Link the previous IFD pointer (or the stream header) to the IFD just written
            long endPosition = imageOutput.getStreamPosition();
            imageOutput.seek(lastIFDPointerOffset);
            tiffWriter.writeOffset(imageOutput, ifdPointer);
            imageOutput.seek(endPosition);
        }

        return nextIFDPointerOffset;
    }

    private static void putSegmentEntries(final Map<Integer, Entry> entries, final boolean tiled, final short offsetType,
                                          final long[] segmentOffsets, final long[] segmentByteCounts) {
        int offsetsTag = tiled ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS;
        int byteCountsTag = tiled ? TIFF.TAG_TILE_BYTE_COUNTS : TIFF.TAG_STRIP_BYTE_COUNTS;

        entries.put(offsetsTag, new TIFFEntry(offsetsTag, offsetType, segmentOffsets.length == 1 ? segmentOffsets[0] : segmentOffsets));
        entries.put(byteCountsTag, new TIFFEntry(byteCountsTag, offsetType, segmentByteCounts.length == 1 ? segmentByteCounts[0] : segmentByteCounts));
    }

    private static int computeRowsPerStrip(final int width, final int bitsPerPixel, final int height) {
        long bytesPerRow = ((long) width * bitsPerPixel + 7L) / 8L;

        return (int) Math.min(height, Math.max(1, DEFAULT_STRIP_SIZE / Math.max(1, bytesPerRow)));
    }

    /**
     * Computes the segment layout (tiles or strips) for a page, and updates the corresponding
     * entries (TileWidth/TileLength or RowsPerStrip).
     */
    private SegmentLayout computeSegmentLayout(final int imageIndex, final ImageWriteParam param, final Map<Integer, Entry> entries,
                                               final SampleModel sampleModel, final int width, final int height) {
        int compression = ((Number) entries.get(TIFF.TAG_COMPRESSION).getValue()).intValue();
        boolean jpeg = compression == TIFFExtension.COMPRESSION_JPEG;

        boolean tiled = param != null && param.canWriteTiles() && param.getTilingMode() == ImageWriteParam.MODE_EXPLICIT;

        int segmentWidth;
        int segmentHeight;
        int segsAcross;
        int segsDown;

        if (tiled) {
            // The TIFF spec requires tile dimensions to be multiples of 16
            int tileWidth = (Math.max(1, param.getTileWidth()) + 15) / 16 * 16;
            int tileHeight = (Math.max(1, param.getTileHeight()) + 15) / 16 * 16;

            if (tileWidth != param.getTileWidth() || tileHeight != param.getTileHeight()) {
                processWarningOccurred(imageIndex, String.format("Tile size rounded up to nearest multiple of 16: %d x %d", tileWidth, tileHeight));
            }

            segmentWidth = tileWidth;
            segmentHeight = tileHeight;
            segsAcross = (width + tileWidth - 1) / tileWidth;
            segsDown = (height + tileHeight - 1) / tileHeight;

            entries.remove(TIFF.TAG_ROWS_PER_STRIP);
            entries.put(TIFF.TAG_TILE_WIDTH, new TIFFEntry(TIFF.TAG_TILE_WIDTH, tileWidth));
            entries.put(TIFF.TAG_TILE_HEIGTH, new TIFFEntry(TIFF.TAG_TILE_HEIGTH, tileHeight));
        }
        else {
            segmentWidth = width;

            // JPEG data is written as a single strip, to avoid repeating the tables for each strip.
            // Otherwise, write strips of about 8K bytes (before compression), as recommended by the spec
            segmentHeight = jpeg ? height : computeRowsPerStrip(width, computePixelSize(sampleModel), height);

            segsAcross = 1;
            segsDown = (height + segmentHeight - 1) / segmentHeight;

            entries.put(TIFF.TAG_ROWS_PER_STRIP, new TIFFEntry(TIFF.TAG_ROWS_PER_STRIP, segmentHeight));
        }

        return new SegmentLayout(tiled, segmentWidth, segmentHeight, segsAcross, segsDown);
    }

    /**
     * Pads the output to a word (2 byte) boundary, as required for IFDs and value offsets
     * (TIFF 6.0 Specification, "Image File Directory", page 13-15).
     */
    private void padToWordBoundary() throws IOException {
        if ((imageOutput.getStreamPosition() & 1) != 0) {
            imageOutput.write(0);
        }
    }

    private static final class SegmentLayout {
        final boolean tiled;
        final int segmentWidth;
        final int segmentHeight;
        final int segsAcross;
        final int segsDown;

        SegmentLayout(final boolean tiled, final int segmentWidth, final int segmentHeight, final int segsAcross, final int segsDown) {
            this.tiled = tiled;
            this.segmentWidth = segmentWidth;
            this.segmentHeight = segmentHeight;
            this.segsAcross = segsAcross;
            this.segsDown = segsDown;
        }
    }

    private IIOImage imageOnly(final IIOImage image) {
        if (image.getMetadata() == null && image.getNumThumbnails() == 0) {
            // Just image data here, no need to copy
            return image;
        }

        return image.hasRaster()
                ? new IIOImage(image.getRaster(), null, null)
                : new IIOImage(image.getRenderedImage(), null, null);
    }

    // TODO: Candidate util method
    private ImageWriteParam copyParams(final ImageWriteParam param, final ImageWriter writer) {
        if (param == null) {
            return null;
        }

        // NOTE: Source region/subsampling/bands are NOT copied to the delegate: The dimensions encoded by
        // the delegate must match the IFD ImageWidth/ImageLength (or TileWidth/TileLength), and are taken
        // from the complete image (or tile)

        // Only if canWriteCompressed()
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
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

    private DataOutput createCompressorStream(final int columns, final int rows, final int samplesPerPixel, final int bitsPerSample,
                                              final ImageWriteParam param, final Map<Integer, Entry> entries) {
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
                if (param != null && param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
                    deflateSetting = Deflater.BEST_COMPRESSION - Math.round((Deflater.BEST_COMPRESSION - 1) * param.getCompressionQuality());
                }

                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new DeflaterOutputStream(stream, new Deflater(deflateSetting), 1024) {
                    @Override
                    public void close() throws IOException {
                        // NOTE: An explicitly supplied Deflater must be explicitly ended to release native
                        // memory, as one stream is now created per segment
                        try {
                            super.close();
                        }
                        finally {
                            def.end();
                        }
                    }
                };
                if (useHorizontalPredictor(entries)) {
                    stream = new HorizontalDifferencingStream(stream, columns, samplesPerPixel, bitsPerSample, imageOutput.getByteOrder());
                }

                return new DataOutputStream(stream);

            case TIFFExtension.COMPRESSION_LZW:
                stream = IIOUtil.createStreamAdapter(imageOutput);
                stream = new EncoderStream(stream, new LZWEncoder((((long) columns * samplesPerPixel * bitsPerSample + 7) / 8) * rows));
                if (useHorizontalPredictor(entries)) {
                    stream = new HorizontalDifferencingStream(stream, columns, samplesPerPixel, bitsPerSample, imageOutput.getByteOrder());
                }

                return new DataOutputStream(stream);

            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
            case TIFFExtension.COMPRESSION_CCITT_T4:
            case TIFFExtension.COMPRESSION_CCITT_T6:
                if (samplesPerPixel != 1 || bitsPerSample != 1) {
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
                stream = new CCITTFaxEncoderStream(stream, columns, rows, compression, fillOrder, option);

                return new DataOutputStream(stream);
        }

        throw new IllegalArgumentException(String.format("Unsupported TIFF compression: %d", compression));
    }

    private static boolean useHorizontalPredictor(final Map<Integer, Entry> entries) {
        Entry predictorEntry = entries.get(TIFF.TAG_PREDICTOR);

        return predictorEntry != null && predictorEntry.getValue().equals(TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING);
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

    /**
     * Writes the image data, one segment (strip or tile) at a time,
     * and stores the offsets and byte counts of the segments written.
     */
    private void writeSegments(final int imageIndex, final RenderedImage image, final ImageWriteParam param, final Map<Integer, Entry> entries,
                               final SegmentLayout layout, final long[] segmentOffsets, final long[] segmentByteCounts) throws IOException {
        processImageStarted(imageIndex);

        int width = image.getWidth();
        int height = image.getHeight();
        int minX = image.getMinX();
        int minY = image.getMinY();

        SampleModel sampleModel = image.getSampleModel();
        int samplesPerPixel = sampleModel.getNumBands();
        int bitsPerSample = validateBitsPerSample(sampleModel);
        ByteOrder byteOrder = imageOutput.getByteOrder();

        // Strips span the full image width, tiles (including partial edge tiles) are padded to the
        // full tile width, so the number of columns per segment is the same for every segment
        int columns = layout.segmentWidth;

        // Reused for all rows of all segments, to avoid re-allocating for each (possibly single row) segment
        byte[] rowBuffer = new byte[(int) (((long) columns * samplesPerPixel * bitsPerSample + 7) / 8)];
        int[] samples = new int[columns * samplesPerPixel];

        int segment = 0;

        for (int segY = 0; segY < layout.segsDown; segY++) {
            for (int segX = 0; segX < layout.segsAcross; segX++) {
                int x = segX * layout.segmentWidth;
                int y = segY * layout.segmentHeight;

                Rectangle region = new Rectangle(minX + x, minY + y,
                                                 Math.min(layout.segmentWidth, width - x), Math.min(layout.segmentHeight, height - y));
                Raster data = getRegion(image, region);

                // Strips are clipped to the image height, tiles are padded to the full tile height
                int rows = layout.tiled ? layout.segmentHeight : region.height;

                segmentOffsets[segment] = imageOutput.getStreamPosition();

                DataOutput stream = createCompressorStream(columns, rows, samplesPerPixel, bitsPerSample, param, entries);
                try {
                    writeSegmentRows(stream, data, region, rows, samplesPerPixel, bitsPerSample, byteOrder, rowBuffer, samples);
                }
                finally {
                    if (stream instanceof DataOutputStream) {
                        // Each segment is an independent stream of compressed data
                        ((DataOutputStream) stream).close();
                    }
                }

                segmentByteCounts[segment] = imageOutput.getStreamPosition() - segmentOffsets[segment];
                segment++;

                processImageProgress(segment * 100f / segmentOffsets.length);
            }
        }

        processImageComplete();
    }

    /**
     * Returns the given region of the image, avoiding a copy of the samples where possible.
     */
    private static Raster getRegion(final RenderedImage image, final Rectangle region) {
        if (image instanceof BufferedImage) {
            // NOTE: getData(Rectangle) copies the samples, createChild is a view of the same data.
            // This matters, as there may be a large number of (small) segments
            return ((BufferedImage) image).getRaster()
                    .createChild(region.x, region.y, region.width, region.height, region.x, region.y, null);
        }

        return image.getData(region);
    }

    private void writeSegmentRows(final DataOutput stream, final Raster data, final Rectangle region,
                                  final int rows, final int numBands, final int bitsPerSample, final ByteOrder byteOrder,
                                  final byte[] rowBuffer, final int[] samples) throws IOException {
        // Zeroed once per segment: packRow rewrites the sample bytes for every row, so only the padding
        // bytes of a partial (right edge) tile need to be zeroed, and they are never written to
        Arrays.fill(rowBuffer, (byte) 0);

        for (int row = 0; row < rows; row++) {
            if (row < region.height) {
                // NOTE: Samples are fetched one full row at a time, as the per-sample accessors are much slower
                data.getPixels(region.x, region.y + row, region.width, 1, samples);
                packRow(rowBuffer, samples, region.width, numBands, bitsPerSample, byteOrder);
            }
            else if (row == region.height) {
                // Rows below the image (bottom edge tile padding) are all zero
                Arrays.fill(rowBuffer, (byte) 0);
            }

            stream.write(rowBuffer, 0, rowBuffer.length);
            flushStream(stream);
        }
    }

    /**
     * Packs one row of band interleaved {@code samples} into {@code rowBuffer}, in TIFF layout:
     * Sub-byte samples (1/2/4 bit, single band) are packed left to right, multi-byte samples
     * are written in the given byte order (matching the output stream/file byte order).
     */
    private static void packRow(final byte[] rowBuffer, final int[] samples, final int columns, final int numBands,
                                final int bitsPerSample, final ByteOrder byteOrder) {
        boolean littleEndian = byteOrder == ByteOrder.LITTLE_ENDIAN;
        int sampleCount = columns * numBands;

        int pos = 0;

        switch (bitsPerSample) {
            case 1:
            case 2:
            case 4:
                // NOTE: Sub-byte samples are single band only, as validated by validateBitsPerSample
                int mask = (1 << bitsPerSample) - 1;
                int accumulated = 0;
                int bits = 0;

                for (int i = 0; i < sampleCount; i++) {
                    accumulated = accumulated << bitsPerSample | (samples[i] & mask);
                    bits += bitsPerSample;

                    if (bits == 8) {
                        rowBuffer[pos++] = (byte) accumulated;
                        accumulated = 0;
                        bits = 0;
                    }
                }

                if (bits != 0) {
                    // Left-justify the last partial byte
                    rowBuffer[pos] = (byte) (accumulated << (8 - bits));
                }

                break;

            case 8:
                for (int i = 0; i < sampleCount; i++) {
                    rowBuffer[pos++] = (byte) samples[i];
                }

                break;

            case 16:
                for (int i = 0; i < sampleCount; i++) {
                    int sample = samples[i];

                    if (littleEndian) {
                        rowBuffer[pos++] = (byte) sample;
                        rowBuffer[pos++] = (byte) (sample >>> 8);
                    }
                    else {
                        rowBuffer[pos++] = (byte) (sample >>> 8);
                        rowBuffer[pos++] = (byte) sample;
                    }
                }

                break;

            case 32:
                for (int i = 0; i < sampleCount; i++) {
                    int sample = samples[i];

                    if (littleEndian) {
                        rowBuffer[pos++] = (byte) sample;
                        rowBuffer[pos++] = (byte) (sample >>> 8);
                        rowBuffer[pos++] = (byte) (sample >>> 16);
                        rowBuffer[pos++] = (byte) (sample >>> 24);
                    }
                    else {
                        rowBuffer[pos++] = (byte) (sample >>> 24);
                        rowBuffer[pos++] = (byte) (sample >>> 16);
                        rowBuffer[pos++] = (byte) (sample >>> 8);
                        rowBuffer[pos++] = (byte) sample;
                    }
                }

                break;

            default:
                // Guarded by validateBitsPerSample
                throw new AssertionError("Unsupported BitsPerSample: " + bitsPerSample);
        }
    }

    /**
     * Validates that the sample layout of {@code sampleModel} can be written, and returns its
     * (uniform) BitsPerSample value.
     * <p>
     * NOTE: Multi-channel data is currently supported for 8 bit samples only, and floating point
     * samples are not yet supported.
     * </p>
     */
    private static int validateBitsPerSample(final SampleModel sampleModel) throws IIOException {
        // TODO: Support floating point (32/64 bit) samples
        int dataType = sampleModel.getDataType();
        if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
            throw new IIOException("Unsupported sample model, floating point samples not supported: " + sampleModel);
        }

        int[] sampleSize = sampleModel.getSampleSize();
        int bitsPerSample = sampleSize[0];

        for (int size : sampleSize) {
            if (size != bitsPerSample) {
                throw new IIOException("Unsupported sample model, varying sample sizes: " + Arrays.toString(sampleSize));
            }
        }

        switch (bitsPerSample) {
            case 8:
                break;

            case 1:
            case 2:
            case 4:
            case 16:
            case 32:
                // TODO: Support multiple channels for 16 and 32 bit samples
                if (sampleSize.length != 1) {
                    throw new IIOException(String.format("Unsupported BitsPerSample (%d) for %d samples per pixel (expected 1 sample per pixel)",
                                                         bitsPerSample, sampleSize.length));
                }

                break;

            default:
                throw new IIOException("Unsupported BitsPerSample: " + bitsPerSample);
        }

        return bitsPerSample;
    }

    /**
     * Writes JPEG compressed image data, using a delegate JPEG {@code ImageWriter}.
     * Striped JPEG data is written as a single strip, to avoid repeating the tables for each strip,
     * tiled JPEG data is written as one JPEG stream per tile.
     */
    private void writeJPEGSegments(final int imageIndex, final IIOImage image, final RenderedImage renderedImage, final ImageWriteParam param,
                                   final SegmentLayout layout, final long[] segmentOffsets, final long[] segmentByteCounts) throws IOException {
        // TODO: Cache JPEGImageWriter, dispose in dispose() method
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");

        if (!writers.hasNext()) {
            // This can only happen if someone deliberately uninstalled it
            throw new IIOException("No JPEG ImageWriter found!");
        }

        ImageWriter jpegWriter = writers.next();

        try {
            if (!layout.tiled && segmentOffsets.length == 1) {
                // The complete image is written as a single JPEG stream, delegate progress/warning events
                segmentOffsets[0] = imageOutput.getStreamPosition();

                jpegWriter.setOutput(new SubImageOutputStream(imageOutput));
                ListenerDelegate listener = new ListenerDelegate(imageIndex);
                jpegWriter.addIIOWriteProgressListener(listener);
                jpegWriter.addIIOWriteWarningListener(listener);
                jpegWriter.write(null, imageOnly(image), copyParams(param, jpegWriter));

                segmentByteCounts[0] = imageOutput.getStreamPosition() - segmentOffsets[0];
            }
            else {
                processImageStarted(imageIndex);

                int width = renderedImage.getWidth();
                int height = renderedImage.getHeight();
                int minX = renderedImage.getMinX();
                int minY = renderedImage.getMinY();
                ColorModel colorModel = renderedImage.getColorModel();

                int segment = 0;

                for (int segY = 0; segY < layout.segsDown; segY++) {
                    for (int segX = 0; segX < layout.segsAcross; segX++) {
                        int x = segX * layout.segmentWidth;
                        int y = segY * layout.segmentHeight;

                        Rectangle region = new Rectangle(minX + x, minY + y,
                                                         Math.min(layout.segmentWidth, width - x), Math.min(layout.segmentHeight, height - y));

                        segmentOffsets[segment] = imageOutput.getStreamPosition();

                        jpegWriter.setOutput(new SubImageOutputStream(imageOutput));

                        WritableRaster tileRaster = paddedTile(renderedImage, region, layout.segmentWidth, layout.segmentHeight);
                        BufferedImage tile = new BufferedImage(colorModel, tileRaster, colorModel.isAlphaPremultiplied(), null);
                        jpegWriter.write(null, new IIOImage(tile, null, null), copyParams(param, jpegWriter));

                        segmentByteCounts[segment] = imageOutput.getStreamPosition() - segmentOffsets[segment];
                        segment++;

                        processImageProgress(segment * 100f / segmentOffsets.length);
                    }
                }

                processImageComplete();
            }
        }
        finally {
            jpegWriter.dispose();
        }
    }

    /**
     * Returns the given region of the image as a raster of full tile size,
     * padding the area outside the image with zeros.
     */
    private static WritableRaster paddedTile(final RenderedImage image, final Rectangle region, final int tileWidth, final int tileHeight) {
        Raster data = image.getData(region);

        WritableRaster tileRaster = data.createCompatibleWritableRaster(tileWidth, tileHeight);
        tileRaster.setRect(0, 0, data.createChild(region.x, region.y, region.width, region.height, 0, 0, null));

        return tileRaster;
    }

    private static void flushStream(DataOutput stream) throws IOException {
        // Need to flush/start new compression for each row, for proper LZW/PackBits/Deflate/ZLib
        if (stream instanceof DataOutputStream) {
            DataOutputStream dataOutputStream = (DataOutputStream) stream;
            dataOutputStream.flush();
        }
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
            TIFFImageMetadata outData = new TIFFImageMetadata(Collections.emptySet());

            try {
                if (Arrays.asList(inData.getMetadataFormatNames()).contains(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME)) {
                    outData.setFromTree(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, inData.getAsTree(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME));
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
                processWarningOccurred(sequence.current(), "Could not convert image meta data: " + e.getMessage());
            }

            ifd = outData.getIFD();
        }

        // Overwrite in values with values from imageType and param as needed
        return initMeta(ifd, imageType, param);
    }

    private TIFFImageMetadata initMeta(final Directory ifd, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        Validate.notNull(imageType, "imageType");

        Map<Integer, Entry> entries = new LinkedHashMap<>(ifd != null ? ifd.size() + 10 : 20);

        // Set software as default, may be overwritten
        entries.put(TIFF.TAG_SOFTWARE, new TIFFEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO TIFF writer " + originatingProvider.getVersion()));
        entries.put(TIFF.TAG_ORIENTATION, new TIFFEntry(TIFF.TAG_ORIENTATION, 1)); // (optional)

        mergeSafeMetadata(ifd, entries);

        ColorModel colorModel = imageType.getColorModel();
        SampleModel sampleModel = imageType.getSampleModel();
        int numBands = sampleModel.getNumBands();
        int pixelSize = computePixelSize(sampleModel);

        entries.put(TIFF.TAG_BITS_PER_SAMPLE, new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, asShortArray(sampleModel.getSampleSize())));

        // Compression field from param or metadata
        int compression;
        if ((param == null || param.getCompressionMode() == TIFFImageWriteParam.MODE_COPY_FROM_METADATA)
                && ifd != null && ifd.getEntryById(TIFF.TAG_COMPRESSION) != null) {
            compression = ((Number) ifd.getEntryById(TIFF.TAG_COMPRESSION).getValue()).intValue();
        }
        else {
            compression = TIFFImageWriteParam.getCompressionType(param);
        }
        entries.put(TIFF.TAG_COMPRESSION, new TIFFEntry(TIFF.TAG_COMPRESSION, compression));

        // TODO: Allow metadata to take precedence?
        int photometricInterpretation = getPhotometricInterpretation(colorModel, compression);
        entries.put(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, TIFF.TYPE_SHORT, photometricInterpretation));

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

        switch (compression) {
            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
            case TIFFExtension.COMPRESSION_LZW:
                // TODO: Let param/metadata control predictor
                // TODO: Depending on param.getCompressionMode(): DISABLED/EXPLICIT/COPY_FROM_METADATA/DEFAULT
                if (pixelSize >= 8) {
                    entries.put(TIFF.TAG_PREDICTOR, new TIFFEntry(TIFF.TAG_PREDICTOR, TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING));
                }

                break;

            case TIFFExtension.COMPRESSION_CCITT_T4:
                Entry group3options = ifd != null ? ifd.getEntryById(TIFF.TAG_GROUP3OPTIONS) : null;

                if (group3options == null) {
                    group3options = new TIFFEntry(TIFF.TAG_GROUP3OPTIONS, (long) TIFFExtension.GROUP3OPT_2DENCODING);
                }

                entries.put(TIFF.TAG_GROUP3OPTIONS, group3options);

                break;

            case TIFFExtension.COMPRESSION_CCITT_T6:
                Entry group4options = ifd != null ? ifd.getEntryById(TIFF.TAG_GROUP4OPTIONS) : null;

                if (group4options == null) {
                    group4options = new TIFFEntry(TIFF.TAG_GROUP4OPTIONS, 0L);
                }

                entries.put(TIFF.TAG_GROUP4OPTIONS, group4options);

                break;

            default:
        }

        if (photometricInterpretation == TIFFBaseline.PHOTOMETRIC_PALETTE && colorModel instanceof IndexColorModel) {
            // TODO: Fix consistency between sampleModel.getSampleSize() and colorModel.getPixelSize()...
            // We should be able to support 1, 2, 4 and 8 bits per sample at least, and probably 3, 5, 6 and 7 too
            entries.put(TIFF.TAG_COLOR_MAP, new TIFFEntry(TIFF.TAG_COLOR_MAP, createColorMap((IndexColorModel) colorModel, sampleModel.getSampleSize(0))));
            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, 1));
        }
        else {
            entries.put(TIFF.TAG_SAMPLES_PER_PIXEL, new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, numBands));

            // Embed ICC profile if we have one that:
            // * is not sRGB (assuming sRGB to be the default RGB interpretation), and
            // * is not gray scale (assuming photometric either BlackIsZero or WhiteIsZero)
            ColorSpace colorSpace = colorModel.getColorSpace();
            if (colorSpace instanceof ICC_ColorSpace && !colorSpace.isCS_sRGB() && !ColorProfiles.isCS_GRAY(((ICC_ColorSpace) colorSpace).getProfile())) {
                entries.put(TIFF.TAG_ICC_PROFILE, new TIFFEntry(TIFF.TAG_ICC_PROFILE, ((ICC_ColorSpace) colorSpace).getProfile().getData()));
            }
        }

        // Default sample format SAMPLEFORMAT_UINT need not be written
        if (sampleModel.getDataType() == DataBuffer.TYPE_SHORT/* TODO: if isSigned(sampleModel.getDataType) or getSampleFormat(sampleModel) != 0 */) {
            entries.put(TIFF.TAG_SAMPLE_FORMAT, new TIFFEntry(TIFF.TAG_SAMPLE_FORMAT, TIFFExtension.SAMPLEFORMAT_INT));
        }
        // TODO: Float values!

        return new TIFFImageMetadata(entries.values());
    }

    private void mergeSafeMetadata(final Directory ifd, final Map<Integer, Entry> entries) {
        if (ifd == null) {
            return;
        }

        for (Entry entry : ifd) {
            int tagId = (Integer) entry.getIdentifier();

            switch (tagId) {
                // Baseline
                case TIFF.TAG_SUBFILE_TYPE:
                case TIFF.TAG_OLD_SUBFILE_TYPE:
                case TIFF.TAG_IMAGE_DESCRIPTION:
                case TIFF.TAG_MAKE:
                case TIFF.TAG_MODEL:
                case TIFF.TAG_ORIENTATION:
                case TIFF.TAG_X_RESOLUTION:
                case TIFF.TAG_Y_RESOLUTION:
                case TIFF.TAG_RESOLUTION_UNIT:
                case TIFF.TAG_SOFTWARE:
                case TIFF.TAG_DATE_TIME:
                case TIFF.TAG_ARTIST:
                case TIFF.TAG_HOST_COMPUTER:
                case TIFF.TAG_COPYRIGHT:
                    // Extension
                case TIFF.TAG_DOCUMENT_NAME:
                case TIFF.TAG_PAGE_NAME:
                case TIFF.TAG_X_POSITION:
                case TIFF.TAG_Y_POSITION:
                case TIFF.TAG_PAGE_NUMBER:
                case TIFF.TAG_XMP:
                    // Private/Custom
                case TIFF.TAG_IPTC:
                case TIFF.TAG_PHOTOSHOP:
                case TIFF.TAG_PHOTOSHOP_IMAGE_SOURCE_DATA:
                case TIFF.TAG_PHOTOSHOP_ANNOTATIONS:
                case TIFF.TAG_EXIF_IFD:
                case TIFF.TAG_GPS_IFD:
                case TIFF.TAG_INTEROP_IFD:
                    entries.put(tagId, entry);
                    break;
                default:
                    // Allow most extension and custom tags
                    if (tagId >= 1000 && tagId < 50706) {
                        entries.put(tagId, entry);
                    }
                    // Skip 50 706 - 57 080 (DNG tags)
                    else if (tagId > 50780 && tagId < 65000) {
                        entries.put(tagId, entry);
                    }
                    // Always allow "the reusable range"
                    else if (tagId >= 65000 && tagId <= 65535) {
                        entries.put(tagId, entry);
                    }
            }
        }
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
        sequence.start();

        assertOutput();
        configureStreamByteOrder(streamMetadata, imageOutput);

        sequenceTIFFWriter = new TIFFWriter(isBigTIFF() ? 8 : 4);
        sequenceTIFFWriter.writeTIFFHeader(imageOutput);
        sequenceLastIFDPos = imageOutput.getStreamPosition();
    }

    private boolean isBigTIFF() throws IOException {
        return "bigtiff".equalsIgnoreCase(getFormatName());
    }

    @Override
    public void writeToSequence(final IIOImage image, final ImageWriteParam param) throws IOException {
        int sequenceIndex = sequence.advance();

        if (sequenceIndex > 0) {
            imageOutput.flushBefore(sequenceLastIFDPos);
            imageOutput.seek(imageOutput.length());
        }

        sequenceLastIFDPos = writePage(sequenceIndex, image, param, sequenceTIFFWriter, sequenceLastIFDPos);
    }

    @Override
    public void endWriteSequence() throws IOException {
        sequence.end();

        sequenceTIFFWriter = null;
        sequenceLastIFDPos = -1;
        imageOutput.flush();
    }

    @Override
    protected void resetMembers() {
        super.resetMembers();

        sequence.reset();
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
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(file)) {
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
        TIFFImageWriter writer = new TIFFImageWriter(new TIFFImageWriterSpi());
//        ImageWriter writer = ImageIO.getImageWritersByFormatName("PNG").next();
//        ImageWriter writer = ImageIO.getImageWritersByFormatName("BMP").next();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
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
