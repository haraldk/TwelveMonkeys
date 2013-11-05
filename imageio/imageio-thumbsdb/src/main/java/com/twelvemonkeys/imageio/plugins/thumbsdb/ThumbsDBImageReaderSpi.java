/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.ole2.CompoundDocument;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * ThumbsDBImageReaderSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ThumbsDBImageReaderSpi.java,v 1.0 28.feb.2006 19:21:05 haku Exp$
 */
public class ThumbsDBImageReaderSpi extends ImageReaderSpi {
    private ImageReaderSpi jpegProvider;

    /**
     * Creates a {@code ThumbsDBImageReaderSpi}.
     */
    public ThumbsDBImageReaderSpi() {
        this(IIOUtil.getProviderInfo(ThumbsDBImageReaderSpi.class));
    }

    private ThumbsDBImageReaderSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(),
                pProviderInfo.getVersion(),
                new String[]{"thumbs", "THUMBS", "Thumbs DB"},
                new String[]{"db"},
                new String[]{"image/x-thumbs-db", "application/octet-stream"}, // TODO: Check IANA et al...
                "com.twelvemonkeys.imageio.plugins.thumbsdb.ThumbsDBImageReader",
                new Class[] {ImageInputStream.class},
                null,
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canDecodeInput(Object source) throws IOException {
        return source instanceof ImageInputStream && canDecode((ImageInputStream) source);
    }

    public boolean canDecode(ImageInputStream pInput) throws IOException {
        maybeInitJPEGProvider();
        // If this is a OLE 2 CompoundDocument, we could try...
        // TODO: How do we know it's thumbs.db format (structure), without reading quite a lot?
        return jpegProvider != null && CompoundDocument.canRead(pInput);
    }

    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new ThumbsDBImageReader(this);
    }

    private void maybeInitJPEGProvider() {
        // NOTE: Can't do this from constructor, as ImageIO itself is not initialized yet,
        // and the lookup below will produce a NPE..

        // TODO: A better approach...
        //       - Could have a list with known working JPEG decoders?
        //       - System property?
        //       - Class path lookup of properties file with reader?
        // This way we could deregister immediately

        if (jpegProvider == null) {
            ImageReaderSpi provider = null;
            try {
                Iterator<ImageReaderSpi> providers = getJPEGProviders();

                while (providers.hasNext()) {
                    provider = providers.next();

                    // Prefer the one we know
                    if ("Sun Microsystems, Inc.".equals(provider.getVendorName())) {
                        break;
                    }
                }
            }
            catch (Exception ignore) {
                // It's pretty safe to assume there's always a JPEG reader out there
                // In any case, we deregister the provider if there isn't one
                IIORegistry.getDefaultInstance().deregisterServiceProvider(this, ImageReaderSpi.class);
            }
            jpegProvider = provider;
        }
    }

    private Iterator<ImageReaderSpi> getJPEGProviders() {
        return IIORegistry.getDefaultInstance().getServiceProviders(
                ImageReaderSpi.class,
                new ServiceRegistry.Filter() {
                    public boolean filter(Object provider) {
                        if (provider instanceof ImageReaderSpi) {
                            ImageReaderSpi spi = (ImageReaderSpi) provider;
                            for (String format : spi.getFormatNames()) {
                                if ("JPEG".equals(format)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                }, true
        );
    }

    /**
     * Returns a new {@code ImageReader} that can read JPEG images.
     *
     * @return a new {@code ImageReader} that can read JPEG images.
     * @throws IllegalStateException if no JPEG provider was found
     * @throws Error                 if the reader can't be instantiated
     */
    ImageReader createJPEGReader() {
        maybeInitJPEGProvider();
        if (jpegProvider == null) {
            throw new IllegalStateException("No suitable JPEG reader provider found");
        }

        try {
            return jpegProvider.createReaderInstance();
        }
        catch (IOException e) {
            // NOTE: The default Sun version never throws IOException here
            throw new Error("Could not create JPEG reader: " + e.getMessage(), e);
        }
    }

    public String getDescription(Locale locale) {
        return "Microsoft Windows Thumbs DB (Thumbs.db) image reader";
    }

//    @Override
//    public void onRegistration(ServiceRegistry registry, Class<?> category) {
//        System.out.println("ThumbsDBImageReaderSpi.onRegistration");
//        maybeInitJPEGProvider();
//        if (jpegProvider == null) {
//            System.out.println("Deregistering");
//            registry.deregisterServiceProvider(this, ImageReaderSpi.class);
//        }
//    }

}
