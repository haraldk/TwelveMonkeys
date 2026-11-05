/*
 * Copyright (c) 2026, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jxl;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import com.traneptora.jxlatte.JXLDecoder;
import com.traneptora.jxlatte.JXLImage;
import com.traneptora.jxlatte.JXLOptions;
import com.traneptora.jxlatte.bundle.ImageHeader;
import com.traneptora.jxlatte.color.ColorFlags;
import com.traneptora.jxlatte.color.ColorManagement;
import com.traneptora.jxlatte.io.InvalidBitstreamException;
import com.traneptora.jxlatte.io.Loggers;
import com.traneptora.jxlatte.util.ImageBuffer;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.Rectangle;
import java.awt.color.CMMException;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * ImageReader for JPEG XL (JXL) images, backed by the JXLatte pure Java decoder.
 * <p>
 * Size and image type are read from the header, without decoding the image.
 * JXLatte always decodes the full image, so the image is decoded once per input,
 * and the result is cached. Source region, subsampling and band selection
 * are applied when copying the cached image to the destination.
 * </p>
 * <p>
 * Images with up to 8 bits per sample are decoded to 8 bits per sample, anything more
 * (including float samples) to 16 bits per sample.
 * Images with an embedded ICC profile are returned in that color space.
 * Other color images are converted to sRGB (HDR images are tone mapped),
 * gray images are converted to the sRGB transfer function and returned as {@code CS_GRAY},
 * like the JDK readers do.
 * </p>
 * <p>
 * See README.md in this module for capabilities, limits and quirks.
 * </p>
 */
final class JXLImageReader extends ImageReaderBase {

    // Ignore unreasonably large Exif/XMP boxes
    private static final long MAX_METADATA_BOX_LENGTH = 64 * 1024 * 1024;

    private long streamStart = -1;
    private JXLDecoder decoder;

    // From the header
    private ImageHeader header;
    private ColorSpace embeddedColorSpace; // null, unless the image has a usable ICC profile
    private ImageTypeSpecifier rawType;

    // From the container boxes, read on demand
    private boolean metadataBoxesRead;
    private Directory exif;
    private String xmp;

    // Cached decoded image
    private BufferedImage decoded;

    JXLImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        streamStart = -1;
        decoder = null;
        header = null;
        embeddedColorSpace = null;
        rawType = null;
        metadataBoxesRead = false;
        exif = null;
        xmp = null;
        decoded = null;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getOrientedSize().width;
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getOrientedSize().height;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return rawType;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        return Collections.singletonList(getRawImageType(imageIndex)).iterator();
    }

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();
        readMetadataBoxes();

        return new JXLImageMetadata(rawType, header, exif, xmp);
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        Raster source = decode().getRaster();
        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        int[] srcBands = param != null ? param.getSourceBands() : null;
        int[] destBands = param != null ? param.getDestinationBands() : null;
        checkReadParamBandSettings(param, source.getNumBands(), destination.getSampleModel().getNumBands());

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        Raster src = source.createChild(srcRegion.x, srcRegion.y, srcRegion.width, srcRegion.height, 0, 0, srcBands);
        WritableRaster dest = destination.getRaster().createWritableChild(destRegion.x, destRegion.y, destRegion.width, destRegion.height, 0, 0, destBands);

        processImageStarted(imageIndex);

        Object data = null;

        for (int y = 0; y < destRegion.height; y++) {
            int srcY = y * ySub;

            if (xSub == 1) {
                data = src.getDataElements(0, srcY, destRegion.width, 1, data);
                dest.setDataElements(0, y, destRegion.width, 1, data);
            }
            else {
                for (int x = 0; x < destRegion.width; x++) {
                    data = src.getDataElements(x * xSub, srcY, data);
                    dest.setDataElements(x, y, data);
                }
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100f * (y + 1) / destRegion.height);
        }

        processImageComplete();

        return destination;
    }

    private void readHeader() throws IOException {
        assertInput();

        if (header == null) {
            if (streamStart < 0) {
                streamStart = imageInput.getStreamPosition();
            }

            // NOTE: Always create a new decoder here, as a previous attempt may have failed
            decoder = createDecoder();

            header = jxlatte(decoder::readHeader);
            embeddedColorSpace = jxlatte(() -> getEmbeddedColorSpace(header));
            rawType = createImageType(header, embeddedColorSpace);
        }
    }

    private void readMetadataBoxes() throws IOException {
        if (metadataBoxesRead) {
            return;
        }

        // NOTE: The decoder may still continue reading after the header, restore position and byte order when done
        long position = imageInput.getStreamPosition();
        ByteOrder byteOrder = imageInput.getByteOrder();

        try {
            imageInput.setByteOrder(ByteOrder.BIG_ENDIAN);
            imageInput.seek(streamStart);

            byte[] signature = new byte[JXL.CONTAINER_SIGNATURE.length];
            imageInput.readFully(signature);

            if (Arrays.equals(signature, JXL.CONTAINER_SIGNATURE)) {
                readBoxes(streamStart + signature.length);
            }
            // ...else, a raw codestream has no metadata boxes

            metadataBoxesRead = true;
        }
        catch (EOFException e) {
            // Truncated file, use what we have
            processWarningOccurred("Truncated JPEG XL container: " + e.getMessage());
            metadataBoxesRead = true;
        }
        finally {
            imageInput.setByteOrder(byteOrder);
            imageInput.seek(position);
        }
    }

    private void readBoxes(long boxStart) throws IOException {
        while (true) {
            imageInput.seek(boxStart);

            if (imageInput.read() < 0) {
                break; // Normal end of file
            }

            imageInput.seek(boxStart);
            long size = imageInput.readUnsignedInt();
            int type = imageInput.readInt();
            long headerLength = 8;

            if (size == 1) {
                size = imageInput.readLong();
                headerLength = 16;
            }

            // Size 0 means the box extends to the end of the file
            long contentLength = size == 0 ? imageInput.length() - boxStart - headerLength : size - headerLength;

            if (size != 0 && contentLength < 0) {
                throw new IIOException(String.format("Invalid JPEG XL container box size: %d", size));
            }

            if (contentLength >= 0 && contentLength <= MAX_METADATA_BOX_LENGTH) {
                // NOTE: Only the first box of each type is used, compressed (brob) boxes are not supported
                if (type == JXL.BOX_EXIF && exif == null) {
                    exif = readExif(contentLength);
                }
                else if (type == JXL.BOX_XML && xmp == null) {
                    byte[] bytes = new byte[(int) contentLength];
                    imageInput.readFully(bytes);
                    xmp = new String(bytes, StandardCharsets.UTF_8);
                }
            }

            if (size == 0) {
                break;
            }

            boxStart += size;
        }
    }

    private Directory readExif(final long contentLength) throws IOException {
        // Exif box content starts with a 4 byte offset to the TIFF header
        long tiffOffset = contentLength >= 4 ? imageInput.readUnsignedInt() : -1;

        if (tiffOffset < 0 || tiffOffset > contentLength - 4 - 8) {
            processWarningOccurred("Invalid Exif box, ignoring");
            return null;
        }

        imageInput.skipBytes(tiffOffset);

        try {
            return new TIFFReader().read(new SubImageInputStream(imageInput, contentLength - 4 - tiffOffset));
        }
        catch (IIOException e) {
            processWarningOccurred("Invalid Exif data, ignoring: " + e.getMessage());
            return null;
        }
    }

    private JXLDecoder createDecoder() throws IOException {
        imageInput.seek(streamStart);

        JXLOptions options = new JXLOptions();
        options.verbosity = Loggers.LOG_BASE - 1; // Silence all logging

        return new JXLDecoder(IIOUtil.createStreamAdapter(imageInput), options);
    }

    private BufferedImage decode() throws IOException {
        readHeader();

        if (decoded == null) {
            if (decoder == null) {
                // A previous decode failed, start over (the new decoder reads the header again)
                decoder = createDecoder();
            }

            try {
                JXLImage image = jxlatte(decoder::decode);
                decoded = jxlatte(() -> toBufferedImage(image, header, embeddedColorSpace, rawType));
            }
            finally {
                // The decoder is no longer needed after decoding (and keeps large buffers), or is unusable after failure
                decoder = null;
            }
        }

        return decoded;
    }

    private interface JXLatteOperation<T> {
        T run() throws IOException;
    }

    private static <T> T jxlatte(final JXLatteOperation<T> operation) throws IOException {
        try {
            return operation.run();
        }
        catch (InvalidBitstreamException e) {
            throw new IIOException("Invalid JPEG XL bitstream: " + e.getMessage(), e);
        }
        catch (RuntimeException e) {
            // JXLatte may throw various runtime exceptions on corrupt data, possibly wrapped by CompletionException
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IIOException("Could not decode JPEG XL image: " + cause.getMessage(), cause);
        }
    }

    private static ImageTypeSpecifier createImageType(final ImageHeader header, final ColorSpace embeddedColorSpace) {
        int colors = header.getColorChannelCount();
        boolean hasAlpha = header.hasAlpha();

        int colorBitDepth = header.getBitDepthHeader().bitsPerSample;
        int alphaBitDepth = hasAlpha ? header.getExtraChannelInfo(header.getAlphaIndex(0)).bitDepth.bitsPerSample : 0;
        // Use 16 bits per sample for anything more than 8 bits, including float samples
        int dataType = Math.max(colorBitDepth, alphaBitDepth) > 8 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;

        ColorSpace colorSpace = embeddedColorSpace;

        if (colorSpace == null) {
            // Without a usable ICC profile, images are converted to sRGB, gray uses the sRGB transfer function in CS_GRAY
            colorSpace = ColorSpace.getInstance(colors == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        }

        int bands = colors + (hasAlpha ? 1 : 0);

        return ImageTypeSpecifiers.createInterleaved(colorSpace, createBandOffsets(bands, hasAlpha), dataType, hasAlpha, false);
    }

    private static BufferedImage toBufferedImage(final JXLImage image, final ImageHeader header,
                                                 final ColorSpace embeddedColorSpace, final ImageTypeSpecifier type) throws IIOException {
        validateAgainstHeader(image, header);

        int colors = image.getColorChannelCount();
        int colorBitDepth = image.getTaggedBitDepth(0); // All color channels share the same bit depth
        ImageBuffer[] buffers = image.getBuffer(false);

        // NOTE: Get alpha before any color transform, as JXLatte's transforms don't necessarily retain extra channels
        ImageBuffer alpha = null;
        int alphaBitDepth = 0;

        if (image.hasAlpha()) {
            int alphaChannel = colors + image.getAlphaIndex();
            alpha = buffers[alphaChannel];
            alphaBitDepth = image.getTaggedBitDepth(alphaChannel);

            if (image.isAlphaPremultiplied()) {
                // Un-premultiply before any color transform, as these are only correct for un-associated values
                unpremultiply(buffers, colors, colorBitDepth, alpha, alphaBitDepth);
            }
        }

        if (embeddedColorSpace == null) {
            buffers = convertToSRGB(image, colors, colorBitDepth).getBuffer(false);
        }

        int bands = colors + (alpha != null ? 1 : 0);
        ImageBuffer[] channels = new ImageBuffer[bands];
        int[] bitDepths = new int[bands];

        for (int c = 0; c < colors; c++) {
            channels[c] = buffers[c];
            bitDepths[c] = colorBitDepth;
        }

        if (alpha != null) {
            channels[colors] = alpha;
            bitDepths[colors] = alphaBitDepth;
        }

        int maxValue = type.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE ? 0xFF : 0xFFFF;
        toIntSamples(channels, bitDepths, maxValue);

        BufferedImage result = type.createBufferedImage(image.getWidth(), image.getHeight());
        interleave(channels, result);

        return result;
    }

    private static void validateAgainstHeader(final JXLImage image, final ImageHeader header) throws IIOException {
        // Sanity check, the decoded image should always match the header
        if (image.getWidth() != header.getOrientedSize().width || image.getHeight() != header.getOrientedSize().height
                || image.getColorChannelCount() != header.getColorChannelCount() || image.hasAlpha() != header.hasAlpha()) {
            throw new IIOException(String.format("Decoded JPEG XL image (%dx%d, %d colors, alpha: %b) does not match header",
                    image.getWidth(), image.getHeight(), image.getColorChannelCount(), image.hasAlpha()));
        }
    }

    private static JXLImage convertToSRGB(final JXLImage image, final int colors, final int colorBitDepth) {
        // No (usable) ICC profile, convert to sRGB (or gray with sRGB transfer function)
        if (image.getTransfer() == ColorFlags.TF_LINEAR) {
            // JXLatte's transfer functions scale int samples wrong for linear images (divides by bit depth, not max value)
            toFloat(image.getBuffer(false), colors, colorBitDepth);
        }

        if (colors == 1) {
            // NOTE: We can't use transform for gray, as JXLatte may expand gray to RGB,
            // without updating the bit depths, causing ArrayIndexOutOfBoundsException.
            // Following the convention of the JDK readers, gray is stored with the sRGB transfer function in CS_GRAY.
            return image.transfer(ColorFlags.TF_SRGB, JXLOptions.PEAK_DETECT_AUTO);
        }

        // HDR images (PQ or HLG transfer, wide gamut primaries) are tone mapped to SDR
        return image.transform(ColorManagement.PRI_SRGB, ColorManagement.WP_D65, ColorFlags.TF_SRGB, JXLOptions.PEAK_DETECT_AUTO);
    }

    private static void interleave(final ImageBuffer[] channels, final BufferedImage destination) {
        // Copies the (int) samples of each channel into the pixel interleaved destination
        int width = destination.getWidth();
        int height = destination.getHeight();
        int bands = channels.length;

        DataBuffer dataBuffer = destination.getRaster().getDataBuffer();
        int[] offsets = ((PixelInterleavedSampleModel) destination.getSampleModel()).getBandOffsets();

        for (int c = 0; c < bands; c++) {
            int[][] samples = channels[c].getIntBuffer();

            for (int y = 0; y < height; y++) {
                int pos = y * width * bands + offsets[c];

                if (dataBuffer instanceof DataBufferByte) {
                    copyRow(samples[y], width, ((DataBufferByte) dataBuffer).getData(), pos, bands);
                }
                else {
                    copyRow(samples[y], width, ((DataBufferUShort) dataBuffer).getData(), pos, bands);
                }
            }
        }
    }

    private static void copyRow(final int[] row, final int width, final byte[] destination, int pos, final int stride) {
        for (int x = 0; x < width; x++, pos += stride) {
            destination[pos] = (byte) row[x];
        }
    }

    private static void copyRow(final int[] row, final int width, final short[] destination, int pos, final int stride) {
        for (int x = 0; x < width; x++, pos += stride) {
            destination[pos] = (short) row[x];
        }
    }

    private static ColorSpace getEmbeddedColorSpace(final ImageHeader header) throws IOException {
        // NOTE: JXLatte ignores the ICC profile for XYB encoded images, and converts from the header's color encoding
        byte[] iccProfile = header.isXYBEncoded() ? null : header.getDecodedICC();

        if (iccProfile != null) {
            try {
                ColorSpace colorSpace = ColorSpaces.createColorSpace(ICC_Profile.getInstance(iccProfile));

                if (colorSpace.getNumComponents() == header.getColorChannelCount()) {
                    return colorSpace;
                }
            }
            catch (IllegalArgumentException | CMMException ignore) {
                // Invalid or unsupported profile, fall through
            }
        }

        return null;
    }

    private static int[] createBandOffsets(final int bands, final boolean hasAlpha) {
        int[] offsets = new int[bands];

        if (bands >= 3) {
            // Use standard BGR/ABGR layout for RGB(A), alpha (if any) is first
            int colorBands = hasAlpha ? bands - 1 : bands;

            for (int c = 0; c < colorBands; c++) {
                offsets[c] = bands - 1 - c;
            }
        }
        else {
            // Gray, gray + alpha
            for (int c = 0; c < bands; c++) {
                offsets[c] = c;
            }
        }

        return offsets;
    }

    private static int maxValue(final int bitDepth) {
        return bitDepth >= 31 ? Integer.MAX_VALUE : (1 << bitDepth) - 1;
    }

    private static void toFloat(final ImageBuffer[] buffers, final int count, final int bitDepth) {
        for (int c = 0; c < count; c++) {
            buffers[c].castToFloatWithMax(maxValue(bitDepth)); // No-op for float buffers
        }
    }

    private static void unpremultiply(final ImageBuffer[] buffers, final int colors, final int colorBitDepth,
                                      final ImageBuffer alphaBuffer, final int alphaBitDepth) {
        alphaBuffer.castToFloatWithMax(maxValue(alphaBitDepth));
        toFloat(buffers, colors, colorBitDepth);

        float[][] alpha = alphaBuffer.getFloatBuffer();

        for (int c = 0; c < colors; c++) {
            float[][] samples = buffers[c].getFloatBuffer();

            for (int y = 0; y < alphaBuffer.height; y++) {
                for (int x = 0; x < alphaBuffer.width; x++) {
                    float a = alpha[y][x];
                    samples[y][x] = a > 0 ? samples[y][x] / a : 0;
                }
            }
        }
    }

    private static void toIntSamples(final ImageBuffer[] channels, final int[] bitDepths, final int maxValue) {
        for (int c = 0; c < channels.length; c++) {
            ImageBuffer channel = channels[c];

            if (channel.isInt() && maxValue(bitDepths[c]) != maxValue) {
                channel.castToFloatWithMax(maxValue(bitDepths[c]));
            }

            channel.castToIntWithMax(maxValue); // Scales and rounds float buffers, no-op for int buffers
            channel.clamp(maxValue);
        }
    }
}
