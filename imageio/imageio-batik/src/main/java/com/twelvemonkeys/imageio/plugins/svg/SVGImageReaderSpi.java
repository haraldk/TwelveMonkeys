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

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.imageio.ImageReader;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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

    static final String SVG_NS_URI = "http://www.w3.org/2000/svg";

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

    @SuppressWarnings("StatementWithEmptyBody")
    private static boolean canDecode(final ImageInputStream pInput) throws IOException {
        DoctypeHandler doctype = new DoctypeHandler();
        pInput.mark();
        try {
            XMLReader xmlReader = XMLReaderFactory.getXMLReader();
            xmlReader.setContentHandler(doctype);
            xmlReader.setErrorHandler(doctype);
            xmlReader.setEntityResolver(doctype);
            @SuppressWarnings("resource")
            InputStream stream = IIOUtil.createStreamAdapter(pInput);
            // XMLReader.parse() generally closes the input streams but the
            // stream adapter prevents closing the underlying stream (✔)
            xmlReader.parse(new InputSource(stream));
        }
        catch (StopParseException e) {
            // Found root element
        }
        catch (SAXException e) {
            // Malformed XML, or not an XML at all
            return false;
        }
        finally {
            //noinspection ThrowFromFinallyBlock
            pInput.reset();
        }
        return "svg".equals(doctype.rootLocalName)
                && SVG_NS_URI.equals(doctype.rootNamespaceURI);
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


    private static class DoctypeHandler extends DefaultHandler2 {

        String rootNamespaceURI;
        String rootLocalName;

        @Override
        public void startDTD(String name, String publicId, String systemId)
                throws SAXException {
            if (name.equals("svg") || name.endsWith(":svg")) {
                // Speculate it is a legitimate SVG
                rootLocalName = "svg";
                rootNamespaceURI = SVG_NS_URI;
            }
            else {
                rootLocalName = name;
                rootNamespaceURI = publicId;
            }

            throw StopParseException.INSTANCE;
        }

        @Override
        public void startElement(String uri,
                                 String localName,
                                 String qName,
                                 Attributes attributes)
                throws SAXException {
            rootNamespaceURI = uri;
            rootLocalName = localName;

            throw StopParseException.INSTANCE;
        }

        @Override
        public InputSource resolveEntity(String name,
                                         String publicId,
                                         String baseURI,
                                         String systemId) {
            return new InputSource(new StringReader("")); // empty entity
        }
    }


    private static class StopParseException extends SAXException {

        private static final long serialVersionUID = 7645435205561343094L;

        static final StopParseException INSTANCE = new StopParseException();

        private StopParseException() {
            super("Parsing stopped from content handler");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; // Don't fill in stack trace
        }
    }


    private static class XMLReaderFactory {

        private static ThreadLocal<XMLReader> localXMLReader = new ThreadLocal<XMLReader>() {
            @Override protected XMLReader initialValue() {
                synchronized (XMLReaderFactory.class) {
                    try {
                        return saxParserFactory().newSAXParser().getXMLReader();
                    }
                    catch (SAXException | ParserConfigurationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };

        private static SAXParserFactory saxParserFactory;

        private static SAXParserFactory saxParserFactory() {
            if (saxParserFactory == null) {
                try {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    spf.setNamespaceAware(true);
                    spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    saxParserFactory = spf;
                } catch (SAXException | ParserConfigurationException e) {
                    throw new FactoryConfigurationError(e);
                }
            }
            return saxParserFactory;
        }

        static XMLReader getXMLReader() {
            return localXMLReader.get();
        }
    }


}

