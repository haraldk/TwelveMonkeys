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

import javax.imageio.IIOParam;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
     * <p/>
     * Note: The adapter is buffered, and <em>MUST</em> be properly flushed/closed after use,
     * otherwise data may be lost.
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

        if (pSourceRegion != null) {
            if (pSourceRegion.x != 0 || pSourceRegion.y != 0 || pSourceRegion.width != pImage.getWidth() || pSourceRegion.height != pImage.getHeight()) {
                return pImage.getSubimage(pSourceRegion.x, pSourceRegion.y, pSourceRegion.width, pSourceRegion.height);
            }
        }

        return pImage;
    }

    /**
     * THIS METHOD WILL ME MOVED/RENAMED, DO NOT USE.
     *
     * @param registry the registry to unregister from.
     * @param provider the provider to unregister.
     * @param category the category to unregister from.
     */
    public static <T> void deregisterProvider(final ServiceRegistry registry, final IIOServiceProvider provider, final Class<T> category) {
        // http://www.ibm.com/developerworks/java/library/j-jtp04298.html
        registry.deregisterServiceProvider(category.cast(provider), category);
    }

    /**
     * THIS METHOD WILL ME MOVED/RENAMED, DO NOT USE.
     *
     * @param registry the registry to lookup from.
     * @param providerClassName name of the provider class.
     * @param category provider category
     *
     * @return the provider instance, or {@code null}.
     */
    public static <T> T lookupProviderByName(final ServiceRegistry registry, final String providerClassName, Class<T> category) {
        try {
            return category.cast(registry.getServiceProviderByClass(Class.forName(providerClassName)));
        }
        catch (ClassNotFoundException ignore) {
            return null;
        }
    }

    /**
     * Returns a sorted array of format names, that can be read by ImageIO.
     * The names are all upper-case, and contains no duplicates.
     *
     * @return a normalized array of {@code String}s.
     * @see javax.imageio.ImageIO#getReaderFormatNames()
     */
    public static String[] getNormalizedReaderFormatNames() {
        return normalizeNames(ImageIO.getReaderFormatNames());
    }

    /**
     * Returns a sorted array of format names, that can be written by ImageIO.
     * The names are all upper-case, and contains no duplicates.
     *
     * @return a normalized array of {@code String}s.
     * @see javax.imageio.ImageIO#getWriterFormatNames()  
     */
    public static String[] getNormalizedWriterFormatNames() {
        return normalizeNames(ImageIO.getWriterFormatNames());
    }

    private static String[] normalizeNames(final String[] names) {
        SortedSet<String> normalizedNames = new TreeSet<>();

        for (String name : names) {
            normalizedNames.add(name.toUpperCase());
        }

        return normalizedNames.toArray(new String[normalizedNames.size()]);
    }
}