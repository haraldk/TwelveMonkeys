/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.*;
import com.twelvemonkeys.imageio.util.IIOUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;

/**
 * XMPReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPReader.java,v 1.0 Nov 14, 2009 11:04:30 PM haraldk Exp$
 */
public final class XMPReader extends MetadataReader {
    @Override
    public Directory read(final ImageInputStream pInput) throws IOException {
        pInput.mark();

        BufferedReader reader = new BufferedReader(new InputStreamReader(IIOUtil.createStreamAdapter(pInput), Charset.forName("UTF-8")));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pInput.reset();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            // TODO: Consider parsing using SAX?
            // TODO: Determine encoding and parse using a Reader...
            // TODO: Refactor scanner to return inputstream?
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(IIOUtil.createStreamAdapter(pInput)));

//            XMLSerializer serializer = new XMLSerializer(System.err, System.getProperty("file.encoding"));
//            serializer.serialize(document);


            // Each rdf:Description is a Directory (but we can't really rely on that structure.. it's only convention)
            //  - Each element inside the rdf:Desc is an Entry

            Node rdfRoot = document.getElementsByTagNameNS(XMP.NS_RDF, "RDF").item(0);
            NodeList descriptions = document.getElementsByTagNameNS(XMP.NS_RDF, "Description");

            return parseDirectories(rdfRoot, descriptions);
        }
        catch (SAXException e) {
            throw new IIOException(e.getMessage(), e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e); // TODO: Or IOException?
        }
    }

    private XMPDirectory parseDirectories(final Node pParentNode, NodeList pNodes) {
        Map<String, List<Entry>> subdirs = new LinkedHashMap<String, List<Entry>>();

        for (Node desc : asIterable(pNodes)) {
            if (desc.getParentNode() != pParentNode) {
                continue;
            }

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

                Node parseType = node.getAttributes().getNamedItemNS(XMP.NS_RDF, "parseType");
                if (parseType != null && "Resource".equals(parseType.getNodeValue())) {
                    List<Entry> entries = new ArrayList<Entry>();

                    for (Node child : asIterable(node.getChildNodes())) {
                        if (child.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        entries.add(new XMPEntry(child.getNamespaceURI() + child.getLocalName(), child.getLocalName(), getChildTextValue(child)));
                    }
                    value = new XMPDirectory(entries);
                }
                else {
                    // TODO: Support alternative RDF syntax (short-form), using attributes on desc
//                    NamedNodeMap attributes = node.getAttributes();
//
//                    for (Node attr : asIterable(attributes)) {
//                        System.out.println("attr.getNodeName(): " + attr.getNodeName());
//                        System.out.println("attr.getNodeValue(): " + attr.getNodeValue());
//                    }

                    value = getChildTextValue(node);
                }

                XMPEntry entry = new XMPEntry(node.getNamespaceURI() + node.getLocalName(), node.getLocalName(), value);
                dir.add(entry);
            }
        }

        // TODO: Consider flattening the somewhat artificial directory structure
        List<Entry> entries = new ArrayList<Entry>();

        for (Map.Entry<String, List<Entry>> entry : subdirs.entrySet()) {
            entries.add(new XMPEntry(entry.getKey(), new XMPDirectory(entry.getValue())));
        }

        return new XMPDirectory(entries);
    }

    private Object getChildTextValue(Node node) {
        Object value;
        Node child = node.getFirstChild();

        String strVal = null;
        if (child != null) {
            strVal = child.getNodeValue();
        }

        value = strVal != null ? strVal.trim() : "";
        return value;
    }

    private Iterable<? extends Node> asIterable(final NamedNodeMap pNodeList) {
        return new Iterable<Node>() {
            public Iterator<Node> iterator() {
                return new Iterator<Node>() {
                    private int mIndex;

                    public boolean hasNext() {
                        return pNodeList != null && pNodeList.getLength() > mIndex;
                    }

                    public Node next() {
                        return pNodeList.item(mIndex++);
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
                    private int mIndex;

                    public boolean hasNext() {
                        return pNodeList != null && pNodeList.getLength() > mIndex;
                    }

                    public Node next() {
                        return pNodeList.item(mIndex++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Method remove not supported");
                    }
                };
            }
        };
    }

}
