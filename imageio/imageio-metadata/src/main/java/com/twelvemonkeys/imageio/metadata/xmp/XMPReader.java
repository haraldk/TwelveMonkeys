/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

/**
 * XMPReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPReader.java,v 1.0 Nov 14, 2009 11:04:30 PM haraldk Exp$
 */
public final class XMPReader extends MetadataReader {
    // See http://www.scribd.com/doc/56852716/XMPSpecificationPart1

    // TODO: Types? Probably defined in XMP/RDF XML schema. Or are we happy that everything is a string?

    @Override
    public Directory read(final ImageInputStream input) throws IOException {
        Validate.notNull(input, "input");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            // TODO: Consider parsing using SAX?
            // TODO: Determine encoding and parse using a Reader...
            // TODO: Refactor scanner to return inputstream?
            // TODO: Be smarter about ASCII-NULL termination/padding (the SAXParser aka Xerces DOMParser doesn't like it)...
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            Document document = builder.parse(new InputSource(IIOUtil.createStreamAdapter(input)));

//            XMLSerializer serializer = new XMLSerializer(System.err, System.getProperty("file.encoding"));
//            serializer.serialize(document);

            String toolkit = getToolkit(document);
            Node rdfRoot = document.getElementsByTagNameNS(XMP.NS_RDF, "RDF").item(0);
            NodeList descriptions = document.getElementsByTagNameNS(XMP.NS_RDF, "Description");

            return parseDirectories(rdfRoot, descriptions, toolkit);
        }
        catch (SAXException e) {
            throw new IIOException(e.getMessage(), e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e); // TODO: Or IOException?
        }
    }

    private String getToolkit(Document document) {
        NodeList xmpmeta = document.getElementsByTagNameNS(XMP.NS_X, "xmpmeta");

        if (xmpmeta == null || xmpmeta.getLength() <= 0) {
            return null;
        }

        Node toolkit = xmpmeta.item(0).getAttributes().getNamedItemNS(XMP.NS_X, "xmptk");

        return toolkit != null ? toolkit.getNodeValue() : null;
    }

    private XMPDirectory parseDirectories(final Node pParentNode, NodeList pNodes, String toolkit) {
        Map<String, List<Entry>> subdirs = new LinkedHashMap<String, List<Entry>>();

        for (Node desc : asIterable(pNodes)) {
            if (desc.getParentNode() != pParentNode) {
                continue;
            }

            // Support attribute short-hand syntax
            parseAttributesForKnownElements(subdirs, desc);

            for (Node node : asIterable(desc.getChildNodes())) {
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                // Lookup
                List<Entry> dir = subdirs.get(node.getNamespaceURI());
                if (dir == null) {
                    dir = new ArrayList<Entry>();
                    subdirs.put(node.getNamespaceURI(), dir);
                }

                Object value;

                if (isResourceType(node)) {
                    value = parseAsResource(node);
                }
                else {
                    // TODO: This method contains loads of duplication an should be cleaned up...
                    // Support attribute short-hand syntax
                    Map<String, List<Entry>> subsubdirs = new LinkedHashMap<String, List<Entry>>();

                    parseAttributesForKnownElements(subsubdirs, node);

                    if (!subsubdirs.isEmpty()) {
                        List<Entry> entries = new ArrayList<Entry>();

                        for (Map.Entry<String, List<Entry>> entry : subsubdirs.entrySet()) {
                            entries.addAll(entry.getValue());
                        }

                        value = new RDFDescription(entries);
                    }
                    else {
                        value = getChildTextValue(node);
                    }
                }

                dir.add(new XMPEntry(node.getNamespaceURI() + node.getLocalName(), node.getLocalName(), value));
            }
        }

        List<Directory> entries = new ArrayList<Directory>();

        // TODO: Should we still allow asking for a subdirectory by item id?
        for (Map.Entry<String, List<Entry>> entry : subdirs.entrySet()) {
            entries.add(new RDFDescription(entry.getKey(), entry.getValue()));
        }

        return new XMPDirectory(entries, toolkit);
    }

    private boolean isResourceType(Node node) {
        Node parseType = node.getAttributes().getNamedItemNS(XMP.NS_RDF, "parseType");

        return parseType != null && "Resource".equals(parseType.getNodeValue());
    }

    private RDFDescription parseAsResource(Node node) {
        // See: http://www.w3.org/TR/REC-rdf-syntax/#section-Syntax-parsetype-resource
        List<Entry> entries = new ArrayList<Entry>();

        for (Node child : asIterable(node.getChildNodes())) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            entries.add(new XMPEntry(child.getNamespaceURI() + child.getLocalName(), child.getLocalName(), getChildTextValue(child)));
        }

        return new RDFDescription(entries);
    }

    private void parseAttributesForKnownElements(Map<String, List<Entry>> subdirs, Node desc) {
        // NOTE: NamedNodeMap does not have any particular order...
        NamedNodeMap attributes = desc.getAttributes();

        for (Node attr : asIterable(attributes)) {
            if (!XMP.ELEMENTS.contains(attr.getNamespaceURI())) {
                continue;
            }

            List<Entry> dir = subdirs.get(attr.getNamespaceURI());

            if (dir == null) {
                dir = new ArrayList<Entry>();
                subdirs.put(attr.getNamespaceURI(), dir);
            }

            dir.add(new XMPEntry(attr.getNamespaceURI() + attr.getLocalName(), attr.getLocalName(), attr.getNodeValue()));
        }
    }

    private Object getChildTextValue(final Node node) {
        for (Node child : asIterable(node.getChildNodes())) {
            if (XMP.NS_RDF.equals(child.getNamespaceURI()) && "Alt".equals(child.getLocalName())) {
                // Support for <rdf:Alt><rdf:li> -> return a Map<String, Object> keyed on xml:lang
                Map<String, Object> alternatives = new LinkedHashMap<String, Object>();
                for (Node alternative : asIterable(child.getChildNodes())) {
                    if (XMP.NS_RDF.equals(alternative.getNamespaceURI()) && "li".equals(alternative.getLocalName())) {
                        NamedNodeMap attributes = alternative.getAttributes();
                        Node key = attributes.getNamedItem("xml:lang");
                        alternatives.put(key == null ? null : key.getTextContent(), getChildTextValue(alternative));
                    }
                }

                return alternatives;
            }
            else if (XMP.NS_RDF.equals(child.getNamespaceURI()) && ("Seq".equals(child.getLocalName()) || "Bag".equals(child.getLocalName()))) {
                // Support for <rdf:Seq><rdf:li> -> return array
                // Support for <rdf:Bag><rdf:li> -> return array/unordered collection (how can a serialized collection not have order?)
                List<Object> seq = new ArrayList<Object>();

                for (Node sequence : asIterable(child.getChildNodes())) {
                    if (XMP.NS_RDF.equals(sequence.getNamespaceURI()) && "li".equals(sequence.getLocalName())) {
                        Object value = getChildTextValue(sequence);
                        seq.add(value);
                    }
                }

                // TODO: Strictly a bag should not be a list, but there's no Bag type (or similar) in Java.
                // Consider something like Google collections Multiset or Apache commons Bag (the former seems more well-defined)
                // Note: Collection does not have defined equals() semantics, and so using
                // Collections.unmodifiableCollection() doesn't work for comparing values (uses Object.equals())
                return Collections.unmodifiableList(seq);
            }
        }

        // Need to support rdf:parseType="Resource" here as well...
        if (isResourceType(node)) {
            return parseAsResource(node);
        }

        Node child = node.getFirstChild();
        String strVal = child != null ? child.getNodeValue() : null;
        return strVal != null ? strVal.trim() : "";
    }

    private Iterable<? extends Node> asIterable(final NamedNodeMap pNodeList) {
        return new Iterable<Node>() {
            public Iterator<Node> iterator() {
                return new Iterator<Node>() {
                    private int index;

                    public boolean hasNext() {
                        return pNodeList != null && pNodeList.getLength() > index;
                    }

                    public Node next() {
                        return pNodeList.item(index++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Method remove not supported");
                    }
                };
            }
        };
    }

    private Iterable<? extends Node> asIterable(final NodeList pNodeList) {
        return new Iterable<Node>() {
            public Iterator<Node> iterator() {
                return new Iterator<Node>() {
                    private int index;

                    public boolean hasNext() {
                        return pNodeList != null && pNodeList.getLength() > index;
                    }

                    public Node next() {
                        return pNodeList.item(index++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Method remove not supported");
                    }
                };
            }
        };
    }
}
