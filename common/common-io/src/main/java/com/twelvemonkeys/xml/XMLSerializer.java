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

package com.twelvemonkeys.xml;

import com.twelvemonkeys.lang.StringUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * XMLSerializer
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/xml/XMLSerializer.java#1 $
 */
public class XMLSerializer {
    // TODO: Replace with DOMSerializer? Test performance, pretty printing etc...
    // Main problem: Sun's Java 5 does not have LS 3.0 support
    // This class has no dependencies, which probably makes it more useful

    // TODO: Don't insert initial and ending line-break for text-nodes
    // TODO: Support not inserting line-breaks, to preserve space
    // TODO: Support line breaking (at configurable width)
    // TODO: Support standalone?
    // TODO: Support more than version 1.0?
    // TODO: Consider using IOException to communicate trouble, rather than RTE,
    // to be more compatible...

    private final OutputStream output;
    private final Charset encoding;
    private final SerializationContext context;

    public XMLSerializer(final OutputStream pOutput, final String pEncoding) {
        output = pOutput;
        encoding = Charset.forName(pEncoding);
        context = new SerializationContext();
    }

    public final XMLSerializer indentation(String pIndent) {
        // TODO: Verify that indent value is only whitespace?
        context.indent = pIndent != null ? pIndent : "\t";
        return this;
    }

    public final XMLSerializer stripComments(boolean pStrip) {
        context.stripComments = pStrip;
        return this;
    }

    /**
     * Serializes the entire document, along with the XML declaration
     * ({@code &lt;?xml version="1.0" encoding="..."?&gt;}).
     *
     * @param pDocument the document to serialize.
     */
    public void serialize(final Document pDocument) {
        serialize(pDocument, true);
    }

    /**
     * Serializes the entire sub tree starting at {@code pRootNode}, along with an optional XML declaration
     * ({@code &lt;?xml version="1.0" encoding="..."?&gt;}).
     *
     * @param pRootNode the root node to serialize.
     * @param pWriteXMLDeclaration {@code true} if the XML declaration should be included, otherwise {@code false}.
     */
    public void serialize(final Node pRootNode, final boolean pWriteXMLDeclaration) {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(output, encoding));
        try {
            if (pWriteXMLDeclaration) {
                writeXMLDeclaration(out);
            }
            writeXML(out, pRootNode, context.copy());
        }
        finally {
            out.flush();
        }
    }

    private void writeXMLDeclaration(final PrintWriter pOut) {
        pOut.print("<?xml version=\"1.0\" encoding=\"");
        pOut.print(encoding.name());
        pOut.println("\"?>");
    }

    private void writeXML(final PrintWriter pOut, final Node pDocument, final SerializationContext pContext) {
        writeNodeRecursive(pOut, pDocument, pContext);
    }

    private void writeNodeRecursive(final PrintWriter pOut, final Node pNode, final SerializationContext pContext) {
        if (pNode.getNodeType() != Node.TEXT_NODE) {
            indentToLevel(pOut, pContext);
        }

        switch (pNode.getNodeType()) {
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                writeDocument(pOut, pNode, pContext);
                break;
            case Node.DOCUMENT_TYPE_NODE:
                writeDoctype(pOut, (DocumentType) pNode);
                break;
            case Node.ELEMENT_NODE:
                boolean preserveSpace = pContext.preserveSpace;
                updatePreserveSpace(pNode, pContext);
                writeElement(pOut, (Element) pNode, pContext);
                pContext.preserveSpace = preserveSpace;
                break;
            case Node.CDATA_SECTION_NODE:
                writeCData(pOut, pNode);
                break;
            case Node.TEXT_NODE:
                writeText(pOut, pNode, pContext);
                break;
            case Node.COMMENT_NODE:
                writeComment(pOut, pNode, pContext);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                writeProcessingInstruction(pOut, (ProcessingInstruction) pNode);
                break;
            case Node.ATTRIBUTE_NODE:
                throw new IllegalArgumentException("Malformed input Document: Attribute nodes should only occur inside Element nodes");
            case Node.ENTITY_NODE:
                // '<!ENTITY ' + getNodeName + ... + '>'
            case Node.ENTITY_REFERENCE_NODE:
                // ( '&' | '%' ) + getNodeName + ';'
            case Node.NOTATION_NODE:
                // '<!NOTATION ' + getNodeName + ( ExternalID | PublicID ) + '>'
            default:
                throw new InternalError("Lazy programmer never implemented serialization of " + pNode.getClass());
        }
    }

    private void writeProcessingInstruction(final PrintWriter pOut, final ProcessingInstruction pNode) {
        pOut.print("\n<?");
        pOut.print(pNode.getTarget());
        String value = pNode.getData();

        if (value != null) {
            pOut.print(" ");
            pOut.print(value);
        }

        pOut.println("?>");
    }

    private void writeText(final PrintWriter pOut, final Node pNode, final SerializationContext pContext) {
        // TODO: Is this really as specified?
        String value = pNode.getNodeValue();
        if (pContext.preserveSpace) {
            pOut.print(maybeEscapeElementValue(value));
        }
        else if (!StringUtil.isEmpty(value)) {
            String escapedValue = maybeEscapeElementValue(value.trim());
            //if (escapedValue.length() + (pContext.level * pContext.indent.length()) > 78) {
                indentToLevel(pOut, pContext);
            //}
            pOut.println(escapedValue);
        }
    }

    private void writeCData(final PrintWriter pOut, final Node pNode) {
        pOut.print("<![CDATA[");
        pOut.print(validateCDataValue(pNode.getNodeValue()));
        pOut.println("]]>");
    }

    private static void updatePreserveSpace(final Node pNode, final SerializationContext pContext) {
        NamedNodeMap attributes = pNode.getAttributes();
        if (attributes != null) {
            Node space = attributes.getNamedItem("xml:space");
            if (space != null) {
                if ("preserve".equals(space.getNodeValue())) {
                    pContext.preserveSpace = true;
                }
                else if ("default".equals(space.getNodeValue())) {
                    pContext.preserveSpace = false;
                }
                // No other values are allowed per spec, ignore
            }
        }
    }

    private static void indentToLevel(final PrintWriter pOut, final SerializationContext pContext) {
        for (int i = 0; i < pContext.level; i++) {
            pOut.print(pContext.indent);
        }
    }

    private void writeComment(final PrintWriter pOut, final Node pNode, final SerializationContext pContext) {
        if (pContext.stripComments) {
            return;
        }

        String value = pNode.getNodeValue();
        validateCommentValue(value);

        if (value.startsWith(" ")) {
            pOut.print("<!--");
        }
        else {
            pOut.print("<!-- ");
        }
        pOut.print(value);
        if (value.endsWith(" ")) {
            pOut.println("-->");
        }
        else {
            pOut.println(" -->");
        }
    }

    /**
     * Returns an escaped version of the input string. The string is guaranteed
     * to not contain illegal XML characters ({@code &<>}).
     * If no escaping is needed, the input string is returned as is. 
     *
     * @param pValue the input string that might need escaping.
     * @return an escaped version of the input string.
     */
    static String maybeEscapeElementValue(final String pValue) {
        int startEscape = needsEscapeElement(pValue);

        if (startEscape < 0) {
            // If no escaping is needed, simply return original
            return pValue;
        }
        else {
            // Otherwise, start replacing
            StringBuilder builder = new StringBuilder(pValue.substring(0, startEscape));
            builder.ensureCapacity(pValue.length() + 30);

            int pos = startEscape;
            for (int i = pos; i < pValue.length(); i++) {
                switch (pValue.charAt(i)) {
                    case '&':
                        pos = appendAndEscape(pValue, pos, i, builder, "&amp;");
                        break;
                    case '<':
                        pos = appendAndEscape(pValue, pos, i, builder, "&lt;");
                        break;
                    case '>':
                        pos = appendAndEscape(pValue, pos, i, builder, "&gt;");
                        break;
                    //case '\'':
                    //case '"':
                    default:
                        break;
                }
            }

            builder.append(pValue.substring(pos));
            return builder.toString();
        }
    }

    private static int appendAndEscape(final String pString, int pStart, final int pEnd, final StringBuilder pBuilder, final String pEntity) {
        pBuilder.append(pString.substring(pStart, pEnd));
        pBuilder.append(pEntity);
        return pEnd + 1;
    }

    /**
     * Returns an the first index from the input string that should be escaped
     * if escaping is needed, otherwise {@code -1}.
     *
     * @param pString the input string that might need escaping.
     * @return the first index from the input string that should be escaped,
     *         or {@code -1}.
     */
    private static int needsEscapeElement(final String pString) {
        for (int i = 0; i < pString.length(); i++) {
            switch (pString.charAt(i)) {
                case '&':
                case '<':
                case '>':
                //case '\'':
                //case '"':
                    return i;
                default:
            }
        }
        return -1;
    }

    private static String maybeEscapeAttributeValue(final String pValue) {
        int startEscape = needsEscapeAttribute(pValue);

        if (startEscape < 0) {
            return pValue;
        }
        else {
            StringBuilder builder = new StringBuilder(pValue.substring(0, startEscape));
            builder.ensureCapacity(pValue.length() + 16);

            int pos = startEscape;
            for (int i = pos; i < pValue.length(); i++) {
                switch (pValue.charAt(i)) {
                    case '&':
                        pos = appendAndEscape(pValue, pos, i, builder, "&amp;");
                        break;
                    case '"':
                        pos = appendAndEscape(pValue, pos, i, builder, "&quot;");
                        break;
                    default:
                        break;
                }
            }

            builder.append(pValue.substring(pos));

            return builder.toString();
        }
    }

    /**
     * Returns an the first index from the input string that should be escaped
     * if escaping is needed, otherwise {@code -1}.
     *
     * @param pString the input string that might need escaping.
     * @return the first index from the input string that should be escaped,
     *         or {@code -1}.
     */
    private static int needsEscapeAttribute(final String pString) {
        for (int i = 0; i < pString.length(); i++) {
            switch (pString.charAt(i)) {
                case '&':
                //case '<':
                //case '>':
                //case '\'':
                case '"':
                    return i;
                default:
            }
        }

        return -1;
    }

    private static String validateCDataValue(final String pValue) {
        if (pValue.contains("]]>")) {
            throw new IllegalArgumentException("Malformed input document: CDATA block may not contain the string ']]>'");
        }
        return pValue;
    }

    private static String validateCommentValue(final String pValue) {
        if (pValue.contains("--")) {
            throw new IllegalArgumentException("Malformed input document: Comment may not contain the string '--'");
        }
        return pValue;
    }

    private void writeDocument(final PrintWriter pOut, final Node pNode, final SerializationContext pContext) {
        // Document fragments might not have child nodes...
        if (pNode.hasChildNodes()) {
            NodeList nodes = pNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                writeNodeRecursive(pOut, nodes.item(i), pContext);
            }
        }
    }

    private void writeElement(final PrintWriter pOut, final Element pNode, final SerializationContext pContext) {
        pOut.print("<");
        pOut.print(pNode.getTagName());

        // TODO: Attributes should probably include namespaces, so that it works
        // even if the document was created using attributes instead of namespaces...
        // In that case, prefix will be null...

        // Handle namespace
        String namespace = pNode.getNamespaceURI();
        if (namespace != null && !namespace.equals(pContext.defaultNamespace)) {
            String prefix = pNode.getPrefix();
            if (prefix == null) {
                pContext.defaultNamespace = namespace;
                pOut.print(" xmlns");
            }
            else {
                pOut.print(" xmlns:");
                pOut.print(prefix);
            }
            pOut.print("=\"");
            pOut.print(namespace);
            pOut.print("\"");
        }

        // Iterate attributes if any
        if (pNode.hasAttributes()) {
            NamedNodeMap attributes = pNode.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                String name = attribute.getName();
                if (!(name.startsWith("xmlns") && (name.length() == 5 || name.charAt(5) == ':'))) {
                    pOut.print(" ");
                    pOut.print(name);
                    pOut.print("=\"");
                    pOut.print(maybeEscapeAttributeValue(attribute.getValue()));
                    pOut.print("\"");
                }
                //else {
                //    System.err.println("attribute.getName(): " + name);
                //}
            }
        }

        // TODO: Consider not indenting/newline if the first child is a text node
        // Iterate children if any
        if (pNode.hasChildNodes()) {
            pOut.print(">");
            if (!pContext.preserveSpace) {
                pOut.println();
            }

            NodeList children = pNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                writeNodeRecursive(pOut, children.item(i), pContext.push());
            }

            if (!pContext.preserveSpace) {
                indentToLevel(pOut, pContext);
            }

            pOut.print("</");
            pOut.print(pNode.getTagName());
            pOut.println(">");
        }
        else if (pNode.getNodeValue() != null) {
            // NOTE: This is NOT AS SPECIFIED, but we do this to support
            // the weirdness that is the javax.imageio.metadata.IIOMetadataNode.
            // According to the spec, the nodeValue of an Element is null.
            pOut.print(">");
            pOut.print(pNode.getNodeValue());
            pOut.print("</");
            pOut.print(pNode.getTagName());
            pOut.println(">");
        }
        else {
            pOut.println("/>");
        }

    }

    private void writeDoctype(final PrintWriter pOut, final DocumentType pDoctype) {
        // NOTE: The DOMImplementationLS LSSerializer actually inserts SYSTEM or
        // PUBLIC identifiers even if they are empty strings. The result is, it
        // will create invalid documents.
        // Testing for empty strings seems to be more compatible.
        if (pDoctype != null) {
            pOut.print("<!DOCTYPE ");
            pOut.print(pDoctype.getName());

            String publicId = pDoctype.getPublicId();
            if (!StringUtil.isEmpty(publicId)) {
                pOut.print(" PUBLIC ");
                pOut.print(publicId);
            }

            String systemId = pDoctype.getSystemId();
            if (!StringUtil.isEmpty(systemId)) {
                if (StringUtil.isEmpty(publicId)) {
                    pOut.print(" SYSTEM \"");
                }
                else {
                    pOut.print(" \"");
                }
                pOut.print(systemId);
                pOut.print("\"");
            }

            String internalSubset = pDoctype.getInternalSubset();
            if (!StringUtil.isEmpty(internalSubset)) {
                pOut.print(" [ ");
                pOut.print(internalSubset);
                pOut.print(" ]");
            }
            pOut.println(">");
        }
    }

    public static void main(String[] pArgs) throws IOException, SAXException {
        // Build XML tree (Document) and write
        // Find the implementation
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            //noinspection ThrowableInstanceNeverThrown BOGUS
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }

        DOMImplementation dom = builder.getDOMImplementation();

        Document document = dom.createDocument("http://www.twelvemonkeys.com/xml/test", "test", dom.createDocumentType("test", null, null));

        Element root = document.getDocumentElement();

        // This is probably not the correct way of setting a default namespace
        //root.setAttribute("xmlns", "http://www.twelvemonkeys.com/xml/test");

        // Create and insert the normal Properties headers as XML comments
        document.insertBefore(document.createComment(new Date().toString()), root);

        Element test = document.createElement("sub");
        root.appendChild(test);
        Element more = document.createElementNS("http://more.com/1999/namespace", "more:more");
        more.setAttribute("foo", "test");
        more.setAttribute("bar", "'really' \"legal\" & ok");
        test.appendChild(more);
        more.appendChild(document.createTextNode("Simply some text."));
        more.appendChild(document.createCDATASection("&something escaped;"));
        more.appendChild(document.createTextNode("More & <more>!"));
        more.appendChild(document.createTextNode("\"<<'&'>>\""));
        Element another = document.createElement("another");
        test.appendChild(another);
        Element yet = document.createElement("yet-another");
        yet.setAttribute("this-one", "with-params");
        test.appendChild(yet);

        Element pre = document.createElementNS("http://www.twelvemonkeys.com/xml/test", "pre");
        pre.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
        pre.appendChild(document.createTextNode(" \t \n\r some text & white ' '   \n   "));
        test.appendChild(pre);

        Element pre2 = document.createElementNS("http://www.twelvemonkeys.com/xml/test", "tight");
        pre2.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
        pre2.appendChild(document.createTextNode("no-space-around-me"));
        test.appendChild(pre2);

        // Create serializer and output document
        //XMLSerializer serializer = new XMLSerializer(pOutput, new OutputFormat(document, UTF_8_ENCODING, true));
        System.out.println("XMLSerializer:");
        XMLSerializer serializer = new XMLSerializer(System.out, "UTF-8");
        serializer.serialize(document);
        System.out.println();

        System.out.println("DOMSerializer:");
        DOMSerializer serializerD = new DOMSerializer(System.out, "UTF-8");
        serializerD.setPrettyPrint(true);
        serializerD.serialize(document);
        System.out.println();
        
        System.out.println("\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLSerializer serializer2 = new XMLSerializer(out, "UTF-8");
        serializer2.serialize(document);

        ByteArrayOutputStream outD = new ByteArrayOutputStream();
        DOMSerializer serializer2D = new DOMSerializer(outD, "UTF-8");
        serializer2D.serialize(document);

        Document document2 = builder.parse(new ByteArrayInputStream(out.toByteArray()));
        System.out.println("XMLSerializer reparsed XMLSerializer:");
        serializer.serialize(document2);
        System.out.println();
        System.out.println("DOMSerializer reparsed XMLSerializer:");
        serializerD.serialize(document2);
        System.out.println();


        Document documentD = builder.parse(new ByteArrayInputStream(outD.toByteArray()));
        System.out.println("XMLSerializer reparsed DOMSerializer:");
        serializer.serialize(documentD);
        System.out.println();
        System.out.println("DOMSerializer reparsed DOMSerializer:");
        serializerD.serialize(documentD);
        System.out.println();
    }

    static class SerializationContext implements Cloneable {
        String indent = "\t";
        int level = 0;
        boolean preserveSpace = false;
        boolean stripComments = false;
        String defaultNamespace;

        public SerializationContext copy() {
            try {
                return (SerializationContext) clone();
            }
            catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }

        public SerializationContext push() {
            SerializationContext context = copy();
            context.level++;
            return context;
        }
    }
}