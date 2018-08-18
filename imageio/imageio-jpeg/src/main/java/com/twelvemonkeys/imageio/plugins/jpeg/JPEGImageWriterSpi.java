/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.deregisterProvider;
import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;

/**
 * JPEGImageWriterSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriterSpi.java,v 1.0 06.02.12 16:09 haraldk Exp$
 */
public class JPEGImageWriterSpi extends ImageWriterSpiBase {
    private ImageWriterSpi delegateProvider;

    /**
     * Constructor for use by {@link javax.imageio.spi.IIORegistry} only.
     * The instance created will not work without being properly registered.
     */
    public JPEGImageWriterSpi() {
        super(new JPEGProviderInfo());
    }

    /**
     * Creates a {@code JPEGImageWriterSpi} with the given delegate.
     *
     * @param delegateProvider a {@code ImageWriterSpi} that can write JPEG.
     */
    protected JPEGImageWriterSpi(final ImageWriterSpi delegateProvider) {
        this();

        this.delegateProvider = Validate.notNull(delegateProvider);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        if (delegateProvider == null) {
            // Install delegate now
            delegateProvider = lookupProviderByName(registry, "com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi", ImageWriterSpi.class);
        }

        if (delegateProvider != null) {
            // Order before com.sun provider, to aid ImageIO in selecting our writer
            registry.setOrdering((Class<ImageWriterSpi>) category, this, delegateProvider);
        }
        else {
            // Or, if no delegate is found, silently deregister from the registry
            deregisterProvider(registry, this, category);
        }
    }

    @Override
    public String getVendorName() {
        return String.format("%s/%s", super.getVendorName(), delegateProvider.getVendorName());
    }

    @Override
    public String getVersion() {
        return String.format("%s/%s", super.getVersion(), delegateProvider.getVersion());
    }

    @Override
    public ImageWriter createWriterInstance(final Object extension) throws IOException {
        return new JPEGImageWriter(this, delegateProvider.createWriterInstance(extension));
    }

    @Override
    public String[] getFormatNames() {
        // NOTE: Can't use super.getFormatNames() which includes JPEG-Lossless
        return delegateProvider.getFormatNames();
    }

    @Override
    public boolean isStandardStreamMetadataFormatSupported() {
        return delegateProvider.isStandardStreamMetadataFormatSupported();
    }

    @Override
    public String getNativeStreamMetadataFormatName() {
        return delegateProvider.getNativeStreamMetadataFormatName();
    }

    @Override
    public String[] getExtraStreamMetadataFormatNames() {
        return delegateProvider.getExtraStreamMetadataFormatNames();
    }

    @Override
    public boolean isStandardImageMetadataFormatSupported() {
        return delegateProvider.isStandardImageMetadataFormatSupported();
    }

    @Override
    public String getNativeImageMetadataFormatName() {
        return delegateProvider.getNativeImageMetadataFormatName();
    }

    @Override
    public String[] getExtraImageMetadataFormatNames() {
        return delegateProvider.getExtraImageMetadataFormatNames();
    }

    @Override
    public IIOMetadataFormat getStreamMetadataFormat(final String formatName) {
        return delegateProvider.getStreamMetadataFormat(formatName);
    }

    @Override
    public IIOMetadataFormat getImageMetadataFormat(final String formatName) {
        return delegateProvider.getImageMetadataFormat(formatName);
    }

    @Override
    public boolean canEncodeImage(final ImageTypeSpecifier type) {
        return delegateProvider.canEncodeImage(type);
    }

    @Override
    public boolean canEncodeImage(final RenderedImage im) {
        return delegateProvider.canEncodeImage(im);
    }

    @Override
    public String getDescription(final Locale locale) {
        return delegateProvider.getDescription(locale);
    }

    @Override
    public boolean isFormatLossless() {
        return delegateProvider.isFormatLossless();
    }

    @Override
    public Class[] getOutputTypes() {
        return delegateProvider.getOutputTypes();
    }
}
