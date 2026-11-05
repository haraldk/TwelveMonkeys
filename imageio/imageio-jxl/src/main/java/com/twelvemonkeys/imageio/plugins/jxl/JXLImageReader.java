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
 * NOTE: Work in progress. Currently, all images are decoded to 8 bits per sample.
 * Images with an embedded ICC profile are returned in that color space,
 * other images are converted to sRGB.
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

            rawType = ImageTypeSpecifier.createFromRenderedImage(decoded);
        }

        return decoded;
    }

    private static BufferedImage toBufferedImage(JXLImage image) {
        // All color channels share the same bit depth
        int colorBitDepth = image.getTaggedBitDepth(0);

        // NOTE: Get alpha before any color transform, as JXLatte's transforms don't necessarily retain extra channels
        boolean hasAlpha = image.hasAlpha();
        ImageBuffer alpha = null;
        int alphaBitDepth = 0;

        if (hasAlpha) {
            int alphaChannel = image.getColorChannelCount() + image.getAlphaIndex();
            alpha = image.getBuffer(false)[alphaChannel];
            alphaBitDepth = image.getTaggedBitDepth(alphaChannel);
        }

        boolean premultiplied = image.isAlphaPremultiplied();
        ColorSpace colorSpace = getEmbeddedColorSpace(image);

        if (colorSpace == null) {
            // No (usable) ICC profile, convert to sRGB
            image = image.transform(ColorManagement.PRI_SRGB, ColorManagement.WP_D65, ColorFlags.TF_SRGB, JXLOptions.PEAK_DETECT_AUTO);
            colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }

        // NOTE: Gray may still be single channel, even if the color space is now sRGB, in that case we expand
        ImageBuffer[] buffers = image.getBuffer(false);
        boolean gray = image.getColorChannelCount() == 1;
        int colorBands = colorSpace.getNumComponents();
        int bands = colorBands + (hasAlpha ? 1 : 0);

        ImageBuffer[] channels = new ImageBuffer[bands];
        int[] bitDepths = new int[bands];

        for (int c = 0; c < colorBands; c++) {
            channels[c] = buffers[gray ? 0 : c];
            bitDepths[c] = colorBitDepth;
        }

        if (hasAlpha) {
            channels[colorBands] = alpha;
            bitDepths[colorBands] = alphaBitDepth;

            if (premultiplied) {
                unpremultiply(channels, bitDepths);
            }
        }

        toBytes(channels, bitDepths);

        int width = image.getWidth();
        int height = image.getHeight();

        int[] bandOffsets = createBandOffsets(bands, hasAlpha);
        ImageTypeSpecifier type = ImageTypeSpecifiers.createInterleaved(colorSpace, bandOffsets, DataBuffer.TYPE_BYTE, hasAlpha, false);
        BufferedImage result = type.createBufferedImage(width, height);

        byte[] data = ((DataBufferByte) result.getRaster().getDataBuffer()).getData();
        int[] offsets = ((PixelInterleavedSampleModel) result.getSampleModel()).getBandOffsets();

        for (int c = 0; c < bands; c++) {
            int[][] samples = channels[c].getIntBuffer();
            int offset = offsets[c];

            for (int y = 0; y < height; y++) {
                int[] row = samples[y];
                int pos = y * width * bands + offset;

                for (int x = 0; x < width; x++) {
                    data[pos] = (byte) row[x];
                    pos += bands;
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
        // Use standard BGR/ABGR layout for RGB(A), for best performance
        int[] offsets = new int[bands];

        if (bands >= 3) {
            int colorBands = hasAlpha ? bands - 1 : bands;

            for (int c = 0; c < colorBands; c++) {
                offsets[c] = bands - 1 - c;
            }

            // Alpha is the first offset
        }
        else {
            for (int c = 0; c < bands; c++) {
                offsets[c] = c;
            }
        }

        return offsets;
    }

    private static void unpremultiply(final ImageBuffer[] channels, final int[] bitDepths) {
        ImageBuffer alphaBuffer = channels[channels.length - 1];
        alphaBuffer.castToFloat(bitDepths[channels.length - 1]);
        float[][] alpha = alphaBuffer.getFloatBuffer();

        for (int c = 0; c < channels.length - 1; c++) {
            ImageBuffer channel = channels[c];

            if (isDuplicate(channels, c)) {
                continue;
            }

            channel.castToFloat(bitDepths[c]);
            float[][] samples = channel.getFloatBuffer();

            for (int y = 0; y < channel.height; y++) {
                for (int x = 0; x < channel.width; x++) {
                    float a = alpha[y][x];
                    samples[y][x] = a > 0 ? samples[y][x] / a : 0;
                }
            }
        }
    }

    private static void toBytes(final ImageBuffer[] channels, final int[] bitDepths) {
        for (int c = 0; c < channels.length; c++) {
            ImageBuffer channel = channels[c];

            if (isDuplicate(channels, c)) {
                continue;
            }

            if (channel.isInt() && bitDepths[c] != 8) {
                channel.castToFloat(bitDepths[c]);
            }

            channel.castToIntWithMax(0xFF); // No-op for int buffers
            channel.clamp(0xFF);
        }
    }

    private static boolean isDuplicate(final ImageBuffer[] channels, final int index) {
        // Expanded gray uses the same buffer for multiple bands, these must only be converted once
        for (int c = 0; c < index; c++) {
            if (channels[c] == channels[index]) {
                return true;
            }
        }

        return false;
    }
}
