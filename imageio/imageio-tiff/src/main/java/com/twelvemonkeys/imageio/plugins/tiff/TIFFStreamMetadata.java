/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.lang.Validate;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.twelvemonkeys.lang.Validate.notNull;
import static java.lang.String.format;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.util.Arrays.asList;

/**
 * TIFFStreamMetadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFStreamMetadata.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public final class TIFFStreamMetadata extends IIOMetadata {

    public static final String SUN_NATIVE_STREAM_METADATA_FORMAT_NAME = "com_sun_media_imageio_plugins_tiff_stream_1.0";

    ByteOrder byteOrder = BIG_ENDIAN;

    public TIFFStreamMetadata() {
        super(false, SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, null, null, null);
    }

    TIFFStreamMetadata(final ByteOrder byteOrder) {
        this();
        this.byteOrder = byteOrder;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Node getAsTree(final String formatName) {
        Validate.isTrue(nativeMetadataFormatName.equals(formatName), formatName, "Unsupported metadata format: %s");

        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

        IIOMetadataNode byteOrderNode = new IIOMetadataNode("ByteOrder");
        root.appendChild(byteOrderNode);
        byteOrderNode.setAttribute("value", byteOrder.toString());

        return root;
    }

    @Override
    public void mergeTree(final String formatName, final Node root) throws IIOInvalidTreeException {
        Validate.isTrue(nativeMetadataFormatName.equals(formatName), formatName, "Unsupported metadata format: %s");
        notNull(root, "root");

        if (!nativeMetadataFormatName.equals(root.getNodeName())) {
            throw new IIOInvalidTreeException("Root must be " + nativeMetadataFormatName, root);
        }

        Node node = root.getFirstChild();
        if (node == null || !node.getNodeName().equals("ByteOrder")) {
            throw new IIOInvalidTreeException("Missing \"ByteOrder\" node", node);
        }

        NamedNodeMap attributes = node.getAttributes();

        String value = attributes.getNamedItem("value").getNodeValue();
        if (value == null) {
            throw new IIOInvalidTreeException("Missing \"value\" attribute in \"ByteOrder\" node", node);
        }

        ByteOrder order = getByteOrder(value.toUpperCase());
        if (order == null) {
            throw new IIOInvalidTreeException("Unknown ByteOrder \"value\" attribute: " + value, node);
        }
        else {
            byteOrder = order;
        }
    }

    private ByteOrder getByteOrder(final String value) {
        switch (value) {
            case "BIG_ENDIAN":
                return ByteOrder.BIG_ENDIAN;
            case "LITTLE_ENDIAN":
                return ByteOrder.LITTLE_ENDIAN;
            default:
                return null;
        }
    }

    @Override
    public void reset() {
        // Big endian is always the default
        byteOrder = BIG_ENDIAN;
    }

    static void configureStreamByteOrder(final IIOMetadata streamMetadata, final ImageOutputStream imageOutput) throws IIOInvalidTreeException {
        notNull(imageOutput, "imageOutput");

        if (streamMetadata instanceof TIFFStreamMetadata) {
            imageOutput.setByteOrder(((TIFFStreamMetadata) streamMetadata).byteOrder);
        }
        else if (streamMetadata != null) {
            TIFFStreamMetadata metadata = new TIFFStreamMetadata();

            Validate.isTrue(asList(streamMetadata.getMetadataFormatNames()).contains(metadata.nativeMetadataFormatName),
                            format("Unsupported stream metadata format, expected %s: %s", metadata.nativeMetadataFormatName,
                                   Arrays.toString(streamMetadata.getMetadataFormatNames())));

            // Will throw exception if stream format differs from native
            metadata.mergeTree(metadata.nativeMetadataFormatName, streamMetadata.getAsTree(metadata.nativeMetadataFormatName));
            imageOutput.setByteOrder(metadata.byteOrder);
        }
        // else, leave as-is
    }
}
