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
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.twelvemonkeys.xml;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementationList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import java.io.OutputStream;
import java.io.Writer;

/**
 * {@code DOMImplementationLS} backed implementation.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/xml/DOMSerializer.java#2 $
 */
public final class DOMSerializer {

    private static final String PARAM_PRETTY_PRINT = "format-pretty-print";
    private static final String PARAM_XML_DECLARATION = "xml-declaration";

    private final LSSerializer serializer;
    private final LSOutput output;

    private DOMSerializer() {
        DOMImplementationLS domImpl = Support.getImplementation();
        serializer = domImpl.createLSSerializer();
        output = domImpl.createLSOutput();
    }

    /**
     * Creates a serializer using the given byte stream and encoding.
     *
     * @param pStream   the byte stream.
     * @param pEncoding the encoding.
     * @throws IllegalStateException if no {@code DOMImplementation} with the right features can be instantiated.
     */
    public DOMSerializer(final OutputStream pStream, final String pEncoding) {
        this();

        output.setByteStream(pStream);
        output.setEncoding(pEncoding);
    }

    /**
     * Creates a serializer using the given character stream and encoding.
     *
     * @param pStream the characted stream.
     * @throws IllegalStateException if no {@code DOMImplementation} with the right features can be instantiated.
     */
    public DOMSerializer(final Writer pStream) {
        this();

        output.setCharacterStream(pStream);
    }

    /*
    // TODO: Is it useful?
    public void setNewLine(final String pNewLine) {
        serializer.setNewLine(pNewLine);
    }

    public String getNewLine() {
        return serializer.getNewLine();
    }
    */

    /**
     * Specifies wether the serializer should use indentation and optimize for
     * readability.
     * <p/>
     * Note: This is a hint, and may be ignored by DOM implemenations. 
     *
     * @param pPrettyPrint {@code true} to enable pretty printing
     */
    public void setPrettyPrint(final boolean pPrettyPrint) {
        DOMConfiguration configuration = serializer.getDomConfig();
        if (configuration.canSetParameter(PARAM_PRETTY_PRINT, pPrettyPrint)) {
            configuration.setParameter(PARAM_PRETTY_PRINT, pPrettyPrint);
        }
    }

    public boolean getPrettyPrint() {
        return Boolean.TRUE.equals(serializer.getDomConfig().getParameter(PARAM_PRETTY_PRINT));
    }

    private void setXMLDeclaration(boolean pXMLDeclaration) {
        serializer.getDomConfig().setParameter(PARAM_XML_DECLARATION, pXMLDeclaration);
    }

    /**
     * Serializes the entire document.
     *
     * @param pDocument the document.
     */
    public void serialize(final Document pDocument) {
        serializeImpl(pDocument, true);
    }

    /**
     * Serializes the given node, along with any subnodes.
     * Will not emit XML declaration.
     *
     * @param pNode the top node.
     */
    public void serialize(final Node pNode) {
        serializeImpl(pNode, false);
    }

    private void serializeImpl(final Node pNode, final boolean pOmitDecl) {
        setXMLDeclaration(pOmitDecl);
        serializer.write(pNode, output);
    }

    private static class Support {
        private final static DOMImplementationRegistry DOM_REGISTRY = createDOMRegistry();

        static DOMImplementationLS getImplementation() {
            DOMImplementationLS implementation = (DOMImplementationLS) DOM_REGISTRY.getDOMImplementation("LS 3.0");
            if (implementation == null) {

                DOMImplementationList list = DOM_REGISTRY.getDOMImplementationList("");
                System.err.println("DOM implementations (" + list.getLength() + "):");
                for (int i = 0; i < list.getLength(); i++) {
                    System.err.println("    " + list.item(i));
                }

                throw new IllegalStateException("Could not create DOM Implementation (no LS support found)");
            }
            return implementation;
        }

        private static DOMImplementationRegistry createDOMRegistry() {
            try {
                return DOMImplementationRegistry.newInstance();
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            catch (InstantiationException e) {
                throw new IllegalStateException(e);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
