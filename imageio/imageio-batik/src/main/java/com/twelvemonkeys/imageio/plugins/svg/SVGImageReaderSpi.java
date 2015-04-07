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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

import static com.twelvemonkeys.imageio.plugins.svg.SVGProviderInfo.SVG_READER_AVAILABLE;

/**
 * SVGImageReaderSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: SVGImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 haku Exp $
 */
public final class SVGImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates an {@code SVGImageReaderSpi}.
     */
    public SVGImageReaderSpi() {
        super(new SVGProviderInfo());
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        return pSource instanceof ImageInputStream && SVG_READER_AVAILABLE && canDecode((ImageInputStream) pSource);
    }

    private static boolean canDecode(final ImageInputStream pInput) throws IOException {
        // NOTE: This test is quite quick as it does not involve any parsing,
        // however it requires the doctype to be "svg", which may not be correct
        // in all cases...
        try {
            pInput.mark();

            // TODO: This is not ok for UTF-16 and other wide encodings
            // TODO: Use an XML (encoding) aware Reader instance instead
            // Need to figure out pretty fast if this is XML or not
            int b;
            while (Character.isWhitespace((char) (b = pInput.read()))) {
                // Skip over leading WS
            }

            if (!((b == '<') && (pInput.read() == '?') && (pInput.read() == 'x') && (pInput.read() == 'm')
                    && (pInput.read() == 'l'))) {
                return false;
            }

            // Okay, we have XML. But, is it really SVG?
            boolean docTypeFound = false;
            while (!docTypeFound) {
                while (pInput.read() != '<') {
                    // Skip over, until begin tag
                }

                if ((b = pInput.read()) != '!') {
                    if (b == 's' && pInput.read() == 'v' && pInput.read() == 'g'
                            && (Character.isWhitespace((char) (b = pInput.read())) || b == ':')) {
                        // TODO: Support svg with prefix + recognize namespace (http://www.w3.org/2000/svg)!
                        return true;
                    }

                    // If this is not a comment, or the DOCTYPE declaration, the doc has no DOCTYPE and it can't be svg
                    return false;
                }

                // There might be comments before the doctype, unfortunately...
                // If next is "--", this is a comment
                if ((b = pInput.read()) == '-' && pInput.read() == '-') {
                    while (!(pInput.read() == '-' && pInput.read() == '-' && pInput.read() == '>')) {
                        // Skip until end of comment
                    }
                }

                // If we are lucky, this is DOCTYPE declaration
                if (b == 'D' && pInput.read() == 'O' && pInput.read() == 'C' && pInput.read() == 'T'
                        && pInput.read() == 'Y' && pInput.read() == 'P' && pInput.read() == 'E') {
                    docTypeFound = true;
                    while (Character.isWhitespace((char) (b = pInput.read()))) {
                        // Skip over WS
                    }

                    if (b == 's' && pInput.read() == 'v' && pInput.read() == 'g') {
                        return true;
                    }
                }
            }

            return false;
        }
        finally {
            pInput.reset();
        }
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
        if (!SVG_READER_AVAILABLE) {
            try {
                // NOTE: This will break, but it gives us some useful debug info
                new SVGImageReader(this);
            }
            catch (Throwable t) {
                System.err.println("Could not instantiate SVGImageReader (missing support classes).");
                t.printStackTrace();
            }

            IIOUtil.deregisterProvider(registry, this, category);
        }
    }
}

