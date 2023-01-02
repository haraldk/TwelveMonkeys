/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.lang.SystemUtil;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.namespace.QName;

import javax.imageio.ImageReader;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.deregisterProvider;

/**
 * SVGImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: SVGImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 haku Exp $
 */
public final class SVGImageReaderSpi extends ImageReaderSpiBase {

    final static boolean SVG_READER_AVAILABLE = SystemUtil.isClassAvailable("com.twelvemonkeys.imageio.plugins.svg.SVGImageReader", SVGImageReaderSpi.class);

    static final QName SVG_ROOT = new QName("http://www.w3.org/2000/svg", "svg");

    /**
     * Creates an {@code SVGImageReaderSpi}.
     */
    @SuppressWarnings("WeakerAccess")
    public SVGImageReaderSpi() {
        super(new SVGProviderInfo());
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        return pSource instanceof ImageInputStream && canDecode((ImageInputStream) pSource);
    }

    private static boolean canDecode(final ImageInputStream pInput) throws IOException {
        QName doctype;
        pInput.mark();
        try {
            @SuppressWarnings("resource")
            InputStream stream = IIOUtil.createStreamAdapter(pInput);
            // XMLReader.parse() generally closes the input streams but the
            // stream adapter prevents closing the underlying stream (✔)
            doctype = DoctypeHandler.doctypeOf(new InputSource(stream));
        }
        catch (SAXException e) {
            // Malformed XML, or not an XML at all
            return false;
        }
        finally {
            //noinspection ThrowFromFinallyBlock
            pInput.reset();
        }
        return SVG_ROOT.equals(doctype);
    }

    public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new SVGImageReader(this);
    }

    public String getDescription(final Locale locale) {
        return "Scalable Vector Graphics (SVG) format image reader";
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        // TODO: Perhaps just try to create an instance, and de-register if we fail?
        if (!SVG_READER_AVAILABLE) {
            System.err.println("Could not instantiate SVGImageReader (missing support classes).");

            try {
                // NOTE: This will break, but it gives us some useful debug info
                new SVGImageReader(this);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }

            deregisterProvider(registry, this, category);
        }
    }
}

