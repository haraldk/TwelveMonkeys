/*
 * Copyright (c) 2023, Harald Kuhr
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

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parses XML up to the root element to identify the document type.
 */
class DoctypeHandler extends DefaultHandler2 {

    private static ThreadLocal<DoctypeHandler> localHandler = new ThreadLocal<DoctypeHandler>() {
        @Override protected DoctypeHandler initialValue() {
            return new DoctypeHandler();
        }
    };

    private static SAXParserFactory saxParserFactory;

    private QName rootElement;

    private XMLReader xmlReader;

    private DoctypeHandler() {
        try {
            xmlReader = saxParserFactory().newSAXParser().getXMLReader();
        }
        catch (SAXException | ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        xmlReader.setEntityResolver(this);
        try {
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", this);
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            // Optional
        }
    }

    public static QName doctypeOf(InputSource source)
            throws IOException, SAXException {
        return localHandler.get().parse(source);
    }

    private QName parse(InputSource source)
            throws IOException, SAXException {
        rootElement = null;
        try {
            xmlReader.parse(source);
        }
        catch (StopParseException e) {
            // Found root element
        }
        return rootElement;
    }

    @Override
    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
        if (name.equals("svg") || name.endsWith(":svg")) {
            // Speculate it is a legitimate SVG
            rootElement = SVGImageReaderSpi.SVG_ROOT;
        }
        else {
            rootElement = new QName(publicId, name);
        }

        throw StopParseException.INSTANCE;
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes)
            throws SAXException {
        rootElement = new QName(uri, localName);

        throw StopParseException.INSTANCE;
    }

    @Override
    public InputSource resolveEntity(String name,
                                     String publicId,
                                     String baseURI,
                                     String systemId) {
        return new InputSource(new StringReader("")); // empty entity
    }

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


}
