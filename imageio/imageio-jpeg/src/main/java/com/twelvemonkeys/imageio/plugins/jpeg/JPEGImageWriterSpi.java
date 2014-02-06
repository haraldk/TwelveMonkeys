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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * JPEGImageWriterSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriterSpi.java,v 1.0 06.02.12 16:09 haraldk Exp$
 */
public class JPEGImageWriterSpi extends ImageWriterSpi {
    private ImageWriterSpi delegateProvider;

    /**
     * Constructor for use by {@link javax.imageio.spi.IIORegistry} only.
     * The instance created will not work without being properly registered.
     */
    public JPEGImageWriterSpi() {
        this(IIOUtil.getProviderInfo(JPEGImageWriterSpi.class));
    }

    private JPEGImageWriterSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{"JPEG", "jpeg", "JPG", "jpg"},
                new String[]{"jpg", "jpeg"},
                new String[]{"image/jpeg"},
                "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter",
                new Class[] { ImageOutputStream.class },
                new String[] {"com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi"},
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    /**
     * Creates a {@code JPEGImageWriterSpi} with the given delegate.
     *
     * @param delegateProvider a {@code ImageWriterSpi} that can write JPEG.
     */
    protected JPEGImageWriterSpi(final ImageWriterSpi delegateProvider) {
        this(IIOUtil.getProviderInfo(JPEGImageReaderSpi.class));

        this.delegateProvider = Validate.notNull(delegateProvider);
    }
    
    static ImageWriterSpi lookupDelegateProvider(final ServiceRegistry registry) {
        Iterator<ImageWriterSpi> providers = registry.getServiceProviders(ImageWriterSpi.class, true);

        while (providers.hasNext()) {
            ImageWriterSpi provider = providers.next();

            if (provider.getClass().getName().equals("com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi")) {
                return provider;
            }
        }

        return null;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        if (delegateProvider == null) {
            // Install delegate now
            delegateProvider = lookupDelegateProvider(registry);
        }

        if (delegateProvider != null) {
            // Order before com.sun provider, to aid ImageIO in selecting our reader
            registry.setOrdering((Class<ImageWriterSpi>) category, this, delegateProvider);
        }
        else {
            // Or, if no delegate is found, silently deregister from the registry
            IIOUtil.deregisterProvider(registry, this, category);
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
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new JPEGImageWriter(this, delegateProvider.createWriterInstance(extension));
    }

    @Override
    public String[] getFormatNames() {
        return delegateProvider.getFormatNames();
    }

    @Override
    public String[] getFileSuffixes() {
        return delegateProvider.getFileSuffixes();
    }

    @Override
    public String[] getMIMETypes() {
        return delegateProvider.getMIMETypes();
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
    public IIOMetadataFormat getStreamMetadataFormat(String formatName) {
        return delegateProvider.getStreamMetadataFormat(formatName);
    }

    @Override
    public IIOMetadataFormat getImageMetadataFormat(String formatName) {
        return delegateProvider.getImageMetadataFormat(formatName);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return delegateProvider.canEncodeImage(type);
    }

    @Override
    public boolean canEncodeImage(RenderedImage im) {
        return delegateProvider.canEncodeImage(im);
    }

    @Override
    public String getDescription(Locale locale) {
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
