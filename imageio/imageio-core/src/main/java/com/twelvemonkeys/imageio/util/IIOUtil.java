/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOParam;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * IIOUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IIOUtil.java,v 1.0 May 8, 2008 3:04:54 PM haraldk Exp$
 */
public final class IIOUtil {
    private IIOUtil() {}

    /**
     * Creates an {@code InputStream} adapter that reads from an underlying {@code ImageInputStream}.
     * The input stream will read until the end of {@code pStream}.
     *
     * @param pStream the stream to read from.
     * @return an {@code InputStream} reading from {@code pStream}.
     */
    public static InputStream createStreamAdapter(final ImageInputStream pStream) {
        // TODO: Include stream start pos?
        // TODO: Skip buffering for known in-memory implementations?
        return new BufferedInputStream(new IIOInputStreamAdapter(pStream));
    }

    /**
     * Creates an {@code InputStream} adapter that reads from an underlying {@code ImageInputStream}.
     * The input stream will read until the end of {@code pStream}, or at most {@code pLength} bytes has been read.
     *
     * @param pStream the stream to read from.
     * @param pLength the maximum number of bytes that can be read from {@code pStream}.
     * @return an {@code InputStream} reading from {@code pStream}.
     */
    public static InputStream createStreamAdapter(final ImageInputStream pStream, final long pLength) {
        // TODO: Include stream start pos?
        // TODO: Skip buffering for known in-memory implementations?
        return new BufferedInputStream(new IIOInputStreamAdapter(pStream, pLength));
    }

    /**
     * Creates an {@code OutputStream} adapter that writes to an underlying {@code ImageOutputStream}.
     * <p>
     * Note: The adapter is buffered, and <em>MUST</em> be properly flushed/closed after use,
     * otherwise data may be lost.
     * </p>
     *
     * @param pStream the stream to write to.
     * @return an {@code OutputSteam} writing to {@code pStream}.
     */
    public static OutputStream createStreamAdapter(final ImageOutputStream pStream) {
        return new BufferedOutputStream(new IIOOutputStreamAdapter(pStream));
    }

    public static Image fakeSubsampling(final Image pImage, final IIOParam pParam) {
        if (pImage == null) {
            return null;
        }

        if (pParam != null) {
            int x = pParam.getSourceXSubsampling();
            int y = pParam.getSourceYSubsampling();

            // 1 is default
            if (x > 1 || y > 1) {
                int w = (ImageUtil.getWidth(pImage) + x - 1) / x;
                int h = (ImageUtil.getHeight(pImage) + y - 1) / y;

                // Fake subsampling by scaling fast
                return pImage.getScaledInstance(w, h, Image.SCALE_FAST);
            }
        }

        return pImage;
    }

    public static Rectangle getSourceRegion(final IIOParam pParam, final int pSrcWidth, final int pSrcHeight) {
        Rectangle sourceRegion = new Rectangle(pSrcWidth, pSrcHeight);

        // If param is present, calculate region
        if (pParam != null) {
            // Get intersection with source region
            Rectangle region = pParam.getSourceRegion();
            if (region != null) {
                sourceRegion = sourceRegion.intersection(region);
            }

            // Scale according to subsampling offsets
            int subsampleXOffset = pParam.getSubsamplingXOffset();
            int subsampleYOffset = pParam.getSubsamplingYOffset();
            sourceRegion.x += subsampleXOffset;
            sourceRegion.y += subsampleYOffset;
            sourceRegion.width -= subsampleXOffset;
            sourceRegion.height -= subsampleYOffset;
        }

        return sourceRegion;
    }

    public static BufferedImage fakeAOI(final BufferedImage pImage, final Rectangle pSourceRegion) {
        if (pImage == null) {
            return null;
        }

        if (pSourceRegion != null
            && (pSourceRegion.x != 0 || pSourceRegion.y != 0 || pSourceRegion.width != pImage.getWidth() || pSourceRegion.height != pImage.getHeight())) {
            return pImage.getSubimage(pSourceRegion.x, pSourceRegion.y, pSourceRegion.width, pSourceRegion.height);
        }

        return pImage;
    }

    /**
     * THIS METHOD WILL BE MOVED/RENAMED, DO NOT USE.
     *
     * @param registry the registry to unregister from.
     * @param provider the provider to unregister.
     * @param category the category to unregister from.
     */
    public static <T> void deregisterProvider(final ServiceRegistry registry, final IIOServiceProvider provider, final Class<T> category) {
        registry.deregisterServiceProvider(category.cast(provider), category);
    }

    /**
     * THIS METHOD WILL BE MOVED/RENAMED, DO NOT USE.
     *
     * @param registry the registry to lookup from.
     * @param providerClassName name of the provider class.
     * @param category provider category
     *
     * @return the provider instance, or {@code null} if not found
     */
    public static <T> T lookupProviderByName(final ServiceRegistry registry, final String providerClassName, Class<T> category) {
        // NOTE: While more verbose, this is more OSGi-friendly than using
        // registry.getServiceProviderByClass(Class.forName(providerClassName))
        Iterator<T> providers = registry.getServiceProviders(category, true);

        while (providers.hasNext()) {
            T provider = providers.next();

            if (provider.getClass().getName().equals(providerClassName)) {
                return provider;
            }
        }

        return null;
    }

    /**
     * Returns a sorted array of format names, that can be read by ImageIO.
     * The names are all upper-case, and contains no duplicates.
     *
     * @return a normalized array of {@code String}s.
     * @see ImageIO#getReaderFormatNames()
     */
    public static String[] getNormalizedReaderFormatNames() {
        return normalizeNames(ImageIO.getReaderFormatNames());
    }

    /**
     * Returns a sorted array of format names, that can be written by ImageIO.
     * The names are all upper-case, and contains no duplicates.
     *
     * @return a normalized array of {@code String}s.
     * @see ImageIO#getWriterFormatNames()
     */
    public static String[] getNormalizedWriterFormatNames() {
        return normalizeNames(ImageIO.getWriterFormatNames());
    }

    private static String[] normalizeNames(final String[] names) {
        SortedSet<String> normalizedNames = new TreeSet<>();

        for (String name : names) {
            normalizedNames.add(name.toUpperCase());
        }

        return normalizedNames.toArray(new String[0]);
    }

    // TODO: RasterUtils? Subsampler?
    public static void subsampleRow(byte[] srcRow, int srcPos, int srcWidth,
                                    byte[] destRow, int destPos,
                                    int samplesPerPixel, int bitsPerSample, int samplePeriod) {
        // Period == 1 is a no-op...
        if (samplePeriod == 1) {
            if (srcRow != destRow) {
                System.arraycopy(srcRow, srcPos, destRow, destPos, srcWidth);
            }

            return;
        }

        Validate.isTrue(samplePeriod > 1, "samplePeriod must be > 1");
        Validate.isTrue(bitsPerSample > 0 && bitsPerSample <= 8 && (bitsPerSample == 1 || bitsPerSample % 2 == 0),
                "bitsPerSample must be > 0 and <= 8 and a power of 2");
        Validate.isTrue(samplesPerPixel > 0, "samplesPerPixel must be > 0");
        Validate.isTrue(samplesPerPixel * bitsPerSample <= 8 || samplesPerPixel * bitsPerSample % 8 == 0,
                "samplesPerPixel * bitsPerSample must be < 8 or a multiple of 8 ");

        if (bitsPerSample * samplesPerPixel % 8 == 0) {
            int pixelStride = bitsPerSample * samplesPerPixel / 8;
            for (int x = 0; x < srcWidth * pixelStride; x += samplePeriod * pixelStride) {
                // System.arraycopy should be intrinsic, but consider using direct array access for pixelStride == 1
                System.arraycopy(srcRow, srcPos + x, destRow, destPos + x / samplePeriod, pixelStride);
            }
        }
        else {
            // Start bit fiddling...
            int pixelStride = bitsPerSample * samplesPerPixel;
            int mask = (1 << pixelStride) - 1;

            for (int x = 0; x < srcWidth; x += samplePeriod) {
                int dstOff = (destPos + x / samplePeriod) * pixelStride / 8;
                int srcOff = (srcPos + x) * pixelStride / 8;

                int srcBitPos = 8 - pixelStride - (x * pixelStride) % 8;
                int srcMask = mask << srcBitPos;

                int dstBitPos = 8 - pixelStride - (x * pixelStride / samplePeriod) % 8;
                int dstMask = ~(mask << dstBitPos);

                int val = ((srcRow[srcOff] & srcMask) >> srcBitPos);
                destRow[dstOff] = (byte) ((destRow[dstOff] & dstMask) | val << dstBitPos);
            }
        }
    }

    public static void subsampleRow(short[] srcRow, int srcPos, int srcWidth,
                                    short[] destRow, int destPos,
                                    int samplesPerPixel, int bitsPerSample, int samplePeriod) {
        // Period == 1 is a no-op...
        if (samplePeriod == 1) {
            if (srcRow != destRow) {
                System.arraycopy(srcRow, srcPos, destRow, destPos, srcWidth);
            }

            return;
        }

        Validate.isTrue(samplePeriod > 1, "samplePeriod must be > 1");
        Validate.isTrue(bitsPerSample > 0 && bitsPerSample <= 16 && (bitsPerSample == 1 || bitsPerSample % 2 == 0),
                "bitsPerSample must be > 0 and <= 16 and a power of 2");
        Validate.isTrue(samplesPerPixel > 0, "samplesPerPixel must be > 0");
        Validate.isTrue(samplesPerPixel * bitsPerSample <= 16 || samplesPerPixel * bitsPerSample % 16 == 0,
                "samplesPerPixel * bitsPerSample must be < 16 or a multiple of 16");

        int pixelStride = bitsPerSample * samplesPerPixel / 16;
        for (int x = 0; x < srcWidth * pixelStride; x += samplePeriod * pixelStride) {
            // System.arraycopy should be intrinsic, but consider using direct array access for pixelStride == 1
            System.arraycopy(srcRow, srcPos + x, destRow, destPos + x / samplePeriod, pixelStride);
        }
    }

    public static void subsampleRow(int[] srcRow, int srcPos, int srcWidth,
                                    int[] destRow, int destPos,
                                    int samplesPerPixel, int bitsPerSample, int samplePeriod) {
        // Period == 1 is a no-op...
        if (samplePeriod == 1) {
            if (srcRow != destRow) {
                System.arraycopy(srcRow, srcPos, destRow, destPos, srcWidth);
            }

            return;
        }

        Validate.isTrue(samplePeriod > 1, "samplePeriod must be > 1");
        Validate.isTrue(bitsPerSample > 0 && bitsPerSample <= 32 && (bitsPerSample == 1 || bitsPerSample % 2 == 0),
                "bitsPerSample must be > 0 and <= 32 and a power of 2");
        Validate.isTrue(samplesPerPixel > 0, "samplesPerPixel must be > 0");
        Validate.isTrue(samplesPerPixel * bitsPerSample <= 32 || samplesPerPixel * bitsPerSample % 32 == 0,
                "samplesPerPixel * bitsPerSample must be < 32 or a multiple of 32");

        int pixelStride = bitsPerSample * samplesPerPixel / 32;
        for (int x = 0; x < srcWidth * pixelStride; x += samplePeriod * pixelStride) {
            // System.arraycopy should be intrinsic, but consider using direct array access for pixelStride == 1
            System.arraycopy(srcRow, srcPos + x, destRow, destPos + x / samplePeriod, pixelStride);
        }
    }

    public static void subsampleRow(float[] srcRow, int srcPos, int srcWidth,
                                    float[] destRow, int destPos,
                                    int samplesPerPixel, int bitsPerSample, int samplePeriod) {
        Validate.isTrue(samplePeriod > 1, "samplePeriod must be > 1"); // Period == 1 could be a no-op...
        Validate.isTrue(bitsPerSample > 0 && bitsPerSample <= 32 && (bitsPerSample == 1 || bitsPerSample % 2 == 0),
                "bitsPerSample must be > 0 and <= 32 and a power of 2");
        Validate.isTrue(samplesPerPixel > 0, "samplesPerPixel must be > 0");
        Validate.isTrue(samplesPerPixel * bitsPerSample <= 32 || samplesPerPixel * bitsPerSample % 32 == 0,
                "samplesPerPixel * bitsPerSample must be < 32 or a multiple of 32");

        int pixelStride = bitsPerSample * samplesPerPixel / 32;
        for (int x = 0; x < srcWidth * pixelStride; x += samplePeriod * pixelStride) {
            // System.arraycopy should be intrinsic, but consider using direct array access for pixelStride == 1
            System.arraycopy(srcRow, srcPos + x, destRow, destPos + x / samplePeriod, pixelStride);
        }
    }

    public static void subsampleRow(double[] srcRow, int srcPos, int srcWidth,
                                    double[] destRow, int destPos,
                                    int samplesPerPixel, int bitsPerSample, int samplePeriod) {
        Validate.isTrue(samplePeriod > 1, "samplePeriod must be > 1"); // Period == 1 could be a no-op...
        Validate.isTrue(bitsPerSample > 0 && bitsPerSample <= 64 && (bitsPerSample == 1 || bitsPerSample % 2 == 0),
                "bitsPerSample must be > 0 and <= 64 and a power of 2");
        Validate.isTrue(samplesPerPixel > 0, "samplesPerPixel must be > 0");
        Validate.isTrue(samplesPerPixel * bitsPerSample <= 64 || samplesPerPixel * bitsPerSample % 64 == 0,
                "samplesPerPixel * bitsPerSample must be < 64 or a multiple of 64");

        int pixelStride = bitsPerSample * samplesPerPixel / 64;
        for (int x = 0; x < srcWidth * pixelStride; x += samplePeriod * pixelStride) {
            // System.arraycopy should be intrinsic, but consider using direct array access for pixelStride == 1
            System.arraycopy(srcRow, srcPos + x, destRow, destPos + x / samplePeriod, pixelStride);
        }
    }
}