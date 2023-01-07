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

import org.junit.Test;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.imageio.spi.ImageReaderSpi;

import static com.twelvemonkeys.imageio.plugins.svg.SVGImageReaderSpiTest.VALID_INPUTS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SVGImageReaderSpi canDecode DOM input tests.
 */
public class SVGImageReaderSpiDOMTest {

    private final ImageReaderSpi provider = new SVGImageReaderSpi();

    @Test
    public void canDecodeSVGDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        DocumentBuilder domBuilder = dbf.newDocumentBuilder();
        for (String validInput : VALID_INPUTS) {
            Document input = domBuilder.parse(getClass().getResource(validInput).toString());
            assertTrue("Can't read valid input: " + validInput, provider.canDecodeInput(input));
        }
    }

    @Test
    public void cannotDecodeNamespaceUnawareDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder domBuilder = dbf.newDocumentBuilder();
        for (String validInput : VALID_INPUTS) {
            Document input = domBuilder.parse(getClass().getResource(validInput).toString());
            assertFalse("Claims to read namespace unaware document: " + validInput,
                        provider.canDecodeInput(input));
        }
    }

    @Test
    public void cannotDecodeNonSVGNamespaceDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        Document document = dbf.newDocumentBuilder().newDocument();
        document.appendChild(document
                .createElementNS("http://www.w3.org/2000/doovde", "svg"));

        assertFalse("Claims to read document with root element: {"
                + document.getDocumentElement().getNamespaceURI() + "}"
                + document.getDocumentElement().getLocalName(),
                provider.canDecodeInput(document));
    }

    @Test
    public void cannotDecodeNonSVGRootElement() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        Document document = dbf.newDocumentBuilder().newDocument();
        document.appendChild(document
                .createElementNS("http://www.w3.org/2000/svg", "path"));

        assertFalse("Claims to read document with root element: {"
                + document.getDocumentElement().getNamespaceURI() + "}"
                + document.getDocumentElement().getLocalName(),
                provider.canDecodeInput(document));
    }
}