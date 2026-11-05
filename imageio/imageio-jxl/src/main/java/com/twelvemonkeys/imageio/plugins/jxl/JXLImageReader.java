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
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import com.traneptora.jxlatte.JXLDecoder;
import com.traneptora.jxlatte.JXLImage;
import com.traneptora.jxlatte.JXLOptions;
import com.traneptora.jxlatte.color.ColorFlags;
import com.traneptora.jxlatte.color.ColorManagement;
import com.traneptora.jxlatte.io.InvalidBitstreamException;
import com.traneptora.jxlatte.io.Loggers;
import com.traneptora.jxlatte.util.ImageBuffer;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
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
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * ImageReader for JPEG XL (JXL) images, backed by the JXLatte pure Java decoder.
 * <p>
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

    private BufferedImage decoded;
    private ImageTypeSpecifier rawType;

    JXLImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        decoded = null;
        rawType = null;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        return decode().getWidth();
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        return decode().getHeight();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        decode();

        return rawType;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        return Collections.singletonList(getRawImageType(imageIndex)).iterator();
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

    private BufferedImage decode() throws IOException {
        assertInput();

        if (decoded == null) {
            JXLOptions options = new JXLOptions();
            options.verbosity = Loggers.LOG_BASE - 1; // Silence all logging

            JXLImage image;

            try {
                image = new JXLDecoder(IIOUtil.createStreamAdapter(imageInput), options).decode();
                decoded = toBufferedImage(image);
            }
            catch (InvalidBitstreamException e) {
                throw new IIOException("Invalid JPEG XL bitstream: " + e.getMessage(), e);
            }
            catch (RuntimeException e) {
                // JXLatte may throw various runtime exceptions on corrupt data, possibly wrapped by CompletionException
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IIOException("Could not decode JPEG XL image: " + cause.getMessage(), cause);
            }

            // NOTE: Don't use ImageTypeSpecifier.createFromRenderedImage, as it maps BufferedImages
            // with a standard type (ie. TYPE_3BYTE_BGR) to the standard (sRGB) type, losing any ICC color space
            rawType = new ImageTypeSpecifier(decoded.getColorModel(), decoded.getSampleModel());
        }

        return decoded;
    }

    private static BufferedImage toBufferedImage(JXLImage image) {
        int colors = image.getColorChannelCount();
        int colorBitDepth = image.getTaggedBitDepth(0); // All color channels share the same bit depth
        ImageBuffer[] buffers = image.getBuffer(false);

        // NOTE: Get alpha before any color transform, as JXLatte's transforms don't necessarily retain extra channels
        boolean hasAlpha = image.hasAlpha();
        ImageBuffer alpha = null;
        int alphaBitDepth = 0;

        if (hasAlpha) {
            int alphaChannel = colors + image.getAlphaIndex();
            alpha = buffers[alphaChannel];
            alphaBitDepth = image.getTaggedBitDepth(alphaChannel);

            if (image.isAlphaPremultiplied()) {
                // Un-premultiply before any color transform, as these are only correct for un-associated values
                unpremultiply(buffers, colors, colorBitDepth, alpha, alphaBitDepth);
            }
        }

        ColorSpace colorSpace = getEmbeddedColorSpace(image);

        if (colorSpace == null) {
            // No (usable) ICC profile, convert to sRGB (or gray with sRGB transfer function)
            if (image.getTransfer() == ColorFlags.TF_LINEAR) {
                // JXLatte's transfer functions scale int samples wrong for linear images (divides by bit depth, not max value)
                toFloat(buffers, colors, colorBitDepth);
            }

            if (colors == 1) {
                // NOTE: We can't use transform for gray, as JXLatte may expand gray to RGB,
                // without updating the bit depths, causing ArrayIndexOutOfBoundsException.
                // Following the convention of the JDK readers, gray is stored with the sRGB transfer function in CS_GRAY.
                image = image.transfer(ColorFlags.TF_SRGB, JXLOptions.PEAK_DETECT_AUTO);
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            }
            else {
                // HDR images (PQ or HLG transfer, wide gamut primaries) are tone mapped to SDR
                image = image.transform(ColorManagement.PRI_SRGB, ColorManagement.WP_D65, ColorFlags.TF_SRGB, JXLOptions.PEAK_DETECT_AUTO);
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            }

            buffers = image.getBuffer(false);
        }

        int bands = colors + (hasAlpha ? 1 : 0);
        ImageBuffer[] channels = new ImageBuffer[bands];
        int[] bitDepths = new int[bands];

        for (int c = 0; c < colors; c++) {
            channels[c] = buffers[c];
            bitDepths[c] = colorBitDepth;
        }

        if (hasAlpha) {
            channels[colors] = alpha;
            bitDepths[colors] = alphaBitDepth;
        }

        // Use 16 bits per sample for anything more than 8 bits, including float samples
        int bitsPerSample = Math.max(colorBitDepth, alphaBitDepth) > 8 ? 16 : 8;
        int maxValue = (1 << bitsPerSample) - 1;
        toIntSamples(channels, bitDepths, maxValue);

        int width = image.getWidth();
        int height = image.getHeight();

        int dataType = bitsPerSample == 8 ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
        ImageTypeSpecifier type = ImageTypeSpecifiers.createInterleaved(colorSpace, createBandOffsets(bands, hasAlpha), dataType, hasAlpha, false);
        BufferedImage result = type.createBufferedImage(width, height);

        DataBuffer dataBuffer = result.getRaster().getDataBuffer();
        byte[] bytes = dataType == DataBuffer.TYPE_BYTE ? ((DataBufferByte) dataBuffer).getData() : null;
        short[] shorts = dataType == DataBuffer.TYPE_USHORT ? ((DataBufferUShort) dataBuffer).getData() : null;
        int[] offsets = ((PixelInterleavedSampleModel) result.getSampleModel()).getBandOffsets();

        for (int c = 0; c < bands; c++) {
            int[][] samples = channels[c].getIntBuffer();

            for (int y = 0; y < height; y++) {
                int[] row = samples[y];
                int pos = y * width * bands + offsets[c];

                if (bytes != null) {
                    for (int x = 0; x < width; x++, pos += bands) {
                        bytes[pos] = (byte) row[x];
                    }
                }
                else {
                    for (int x = 0; x < width; x++, pos += bands) {
                        shorts[pos] = (short) row[x];
                    }
                }
            }
        }

        return result;
    }

    private static ColorSpace getEmbeddedColorSpace(final JXLImage image) {
        if (image.hasICCProfile()) {
            try {
                ColorSpace colorSpace = ColorSpaces.createColorSpace(ICC_Profile.getInstance(image.getICCProfile()));

                if (colorSpace.getNumComponents() == image.getColorChannelCount()) {
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
