/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

abstract class HeaderWriter {
    protected static final Charset UTF8 = Charset.forName("UTF8");
    protected final ImageOutputStream imageOutput;

    protected HeaderWriter(final ImageOutputStream imageOutput) {
        this.imageOutput = imageOutput;
    }

    public static void write(final IIOImage image, final ImageWriterSpi provider, final ImageOutputStream imageOutput) throws IOException {
        // TODO: This is somewhat sketchy...
        if (provider.getFormatNames()[0].equals("pam")) {
            new PAMHeaderWriter(imageOutput).writeHeader(image, provider);
        }
        else if (provider.getFormatNames()[0].equals("pnm")) {
            new PNMHeaderWriter(imageOutput).writeHeader(image, provider);
        }
        else {
            throw new AssertionError("Unsupported provider: " + provider);
        }
    }

    public abstract void writeHeader(IIOImage image, final ImageWriterSpi provider) throws IOException;

    protected final int getWidth(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getWidth() : image.getRenderedImage().getWidth();
    }

    protected final int getHeight(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getHeight() : image.getRenderedImage().getHeight();
    }

    protected final int getNumBands(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getNumBands() : image.getRenderedImage().getSampleModel().getNumBands();
    }

    protected int getMaxVal(final IIOImage image) {
        int transferType = getTransferType(image);

        if (transferType == DataBuffer.TYPE_BYTE) {
            return PNM.MAX_VAL_8BIT;
        }
        else if (transferType == DataBuffer.TYPE_USHORT) {
            return PNM.MAX_VAL_16BIT;
        }
//        else if (transferType == DataBuffer.TYPE_INT) {
        // TODO: Support TYPE_INT through conversion, if number of channels is 3 or 4 (TYPE_INT_RGB, TYPE_INT_ARGB)
//        }
        else {
            throw new IllegalArgumentException("Unsupported data type: " + transferType);
        }
    }

    protected final int getTransferType(final IIOImage image) {
        return  image.hasRaster() ? image.getRaster().getTransferType() : image.getRenderedImage().getSampleModel().getTransferType();
    }

    protected final void writeComments(final IIOMetadata metadata, final ImageWriterSpi provider) throws IOException {
        // TODO: Only write creator if not already present
        imageOutput.write(String.format("# CREATOR: %s %s\n", provider.getVendorName(), provider.getDescription(Locale.getDefault())).getBytes(UTF8));

        // Comments from metadata
        if (metadata != null && metadata.isStandardMetadataFormatSupported()) {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            NodeList textEntries = root.getElementsByTagName("TextEntry");

            for (int i = 0; i < textEntries.getLength(); i++) {
                // TODO: Write on the format "# KEYWORD: value" (if keyword != comment)?
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                imageOutput.write(String.format("# %s", textEntry.getAttribute("value")).getBytes(UTF8));
            }
        }
    }

}
