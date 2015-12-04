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
import java.io.EOFException;
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
        // however it may not recognize all kinds of SVG documents.
        try {
            pInput.mark();

            // TODO: This is not ok for UTF-16 and other wide encodings
            // TODO: Use an XML (encoding) aware Reader instance instead
            // Need to figure out pretty fast if this is XML or not
            int b;
            while (Character.isWhitespace((char) (b = pInput.read()))) {
                // Skip over leading WS
            }

            // If it's not a tag, this can't be valid XML
            if (b != '<') {
                return false;
            }

            // Algorithm for detecting SVG:
            //  - Skip until begin tag '<' and read 4 bytes
            //  - if next is "?" skip until "?>" and start over
            //  - else if next is "!--" skip until  "-->" and start over
            //  - else if next is  "!DOCTYPE " skip any whitespace
            //      - compare next 3 bytes against "svg", return result
            //  - else
            //      - compare next 3 bytes against "svg", return result

            byte[] buffer = new byte[4];
            while (true) {
                pInput.readFully(buffer);

                if (buffer[0] == '?') {
                    // This is the XML declaration or a processing instruction
                    while (!(pInput.read() == '?' && pInput.read() == '>')) {
                        // Skip until end of XML declaration or processing instruction
                    }
                }
                else if (buffer[0] == '!') {
                    if (buffer[1] == '-' && buffer[2] == '-') {
                        // This is a comment
                        while (!(pInput.read() == '-' && pInput.read() == '-' && pInput.read() == '>')) {
                            // Skip until end of comment
                        }
                    }
                    else if (buffer[1] == 'D' && buffer[2] == 'O' && buffer[3] == 'C'
                            && pInput.read() == 'T' && pInput.read() == 'Y'
                            && pInput.read() == 'P' && pInput.read() == 'E') {
                        // This is the DOCTYPE declaration
                        while (Character.isWhitespace((char) (b = pInput.read()))) {
                            // Skip over WS
                        }

                        if (b == 's' && pInput.read() == 'v' && pInput.read() == 'g') {
                            // It's SVG, identified by DOCTYPE
                            return true;
                        }

                        // DOCTYPE found, but not SVG
                        return false;
                    }

                    // Something else, we'll skip
                }
                else {
                    // This is a normal tag
                    if (buffer[0] == 's' && buffer[1] == 'v' && buffer[2] == 'g'
                            && (Character.isWhitespace((char) buffer[3]) || buffer[3] == ':')) {
                        // It's SVG, identified by root tag
                        // TODO: Support svg with prefix + recognize namespace (http://www.w3.org/2000/svg)!
                        return true;
                    }

                    // If the tag is not "svg", this isn't SVG
                    return false;
                }

                while (pInput.read() != '<') {
                    // Skip over, until next begin tag
                }
            }
        }
        catch (EOFException ignore) {
            // Possible for small files...
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

