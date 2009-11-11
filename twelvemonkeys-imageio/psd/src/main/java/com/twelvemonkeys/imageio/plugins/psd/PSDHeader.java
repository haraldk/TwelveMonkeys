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

package com.twelvemonkeys.imageio.plugins.psd;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;
import java.io.IOException;

/**
 * PSDHeader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDHeader.java,v 1.0 Apr 29, 2008 5:18:22 PM haraldk Exp$
 */
class PSDHeader {
//    The header is 26 bytes in length and is structured as follows:
//
//    typedef struct _PSD_HEADER
//    {
//       BYTE Signature[4];   /* File ID "8BPS" */
//       WORD Version;        /* Version number, always 1 */
//       BYTE Reserved[6];    /* Reserved, must be zeroed */
//       WORD Channels;       /* Number of color channels (1-24) including alpha
//                               channels */
//       LONG Rows;           /* Height of image in pixels (1-30000) */
//       LONG Columns;        /* Width of image in pixels (1-30000) */
//       WORD Depth;          /* Number of bits per channel (1, 8, and 16) */
//       WORD Mode;           /* Color mode */
//    } PSD_HEADER;

    final short mChannels;
    final int mWidth;
    final int mHeight;
    final short mBits;
    final short mMode;

    PSDHeader(final ImageInputStream pInput) throws IOException {
        int signature = pInput.readInt();
        if (signature != PSD.SIGNATURE_8BPS) {
            throw new IIOException("Not a PSD document, expected signature \"8BPS\": \"" + PSDUtil.intToStr(signature) + "\" (0x" + Integer.toHexString(signature) + ")");
        }

        int version = pInput.readUnsignedShort();

        switch (version) {
            case 1:
                break;
            case 2:
                throw new IIOException("Photoshop Large Document Format (PSB) not supported yet.");
            default:
                throw new IIOException(String.format("Unknown PSD version, expected 1 or 2: 0x%08x", version));
        }

        byte[] reserved = new byte[6];
        pInput.readFully(reserved);

        mChannels = pInput.readShort();
        mHeight = pInput.readInt(); // Rows
        mWidth = pInput.readInt(); // Columns
        mBits = pInput.readShort();
        mMode = pInput.readShort();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[Channels: ");
        builder.append(mChannels);
        builder.append(", width: ");
        builder.append(mWidth);
        builder.append(", height: ");
        builder.append(mHeight);
        builder.append(", depth: ");
        builder.append(mBits);
        builder.append(", mode: ");
        builder.append(mMode);
        switch (mMode) {
            case PSD.COLOR_MODE_MONOCHROME:
                builder.append(" (Monochrome)");
                break;
            case PSD.COLOR_MODE_GRAYSCALE:
                builder.append(" (Grayscale)");
                break;
            case PSD.COLOR_MODE_INDEXED:
                builder.append(" (Indexed)");
                break;
            case PSD.COLOR_MODE_RGB:
                builder.append(" (RGB)");
                break;
            case PSD.COLOR_MODE_CMYK:
                builder.append(" (CMYK)");
                break;
            case PSD.COLOR_MODE_MULTICHANNEL:
                builder.append(" (Multi channel)");
                break;
            case PSD.COLOR_MODE_DUOTONE:
                builder.append(" (Duotone)");
                break;
            case PSD.COLOR_MODE_LAB:
                builder.append(" (Lab color)");
                break;
            default:
                builder.append(" (Unkown mode)");
        }
        builder.append("]");

        return builder.toString();
    }
}
