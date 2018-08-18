/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.IOException;

/**
 * PSDHeader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDHeader.java,v 1.0 Apr 29, 2008 5:18:22 PM haraldk Exp$
 */
final class PSDHeader {
//    The header is 26 bytes in length and is structured as follows:
//
//    typedef struct _PSD_HEADER
//    {
//       BYTE Signature[4];   /* File ID "8BPS" */
//       WORD Version;        /* Version number, always 1. 2 for PSB */
//       BYTE Reserved[6];    /* Reserved, must be zeroed */
//       WORD Channels;       /* Number of color channels (1-56) including alpha
//                               channels */
//       LONG Rows;           /* Height of image in pixels (1-30000/1-300000 for PSB) */
//       LONG Columns;        /* Width of image in pixels (1-30000/1-300000 for PSB) */
//       WORD Depth;          /* Number of bits per channel (1, 8, 16 or 32) */
//       WORD Mode;           /* Color mode */
//    } PSD_HEADER;

    private static final int PSD_MAX_SIZE = 30000;
    private static final int PSB_MAX_SIZE = 300000;

    final short channels;
    final int width;
    final int height;
    final short bits;
    final short mode;
    final boolean largeFormat;

    PSDHeader(final DataInput pInput) throws IOException {
        int signature = pInput.readInt();
        if (signature != PSD.SIGNATURE_8BPS) {
            throw new IIOException("Not a PSD document, expected signature \"8BPS\": \"" + PSDUtil.intToStr(signature) + "\" (0x" + Integer.toHexString(signature) + ")");
        }

        int version = pInput.readUnsignedShort();

        switch (version) {
            case PSD.VERSION_PSD:
                largeFormat = false;
                break;
            case PSD.VERSION_PSB:
                largeFormat = true;
                break;
            default:
                throw new IIOException(String.format("Unknown PSD version, expected 1 or 2: 0x%08x", version));
        }

        byte[] reserved = new byte[6];
        pInput.readFully(reserved); // We don't really care

        channels = pInput.readShort();
        if (channels < 1 || channels > 56) {
            throw new IIOException(String.format("Unsupported number of channels for PSD: %d", channels));
        }

        height = pInput.readInt(); // Rows
        width = pInput.readInt(); // Columns

        bits = pInput.readShort();

        switch (bits) {
            case 1:
            case 8:
            case 16:
            case 32:
                break;
            default:
                throw new IIOException(String.format("Unsupported bit depth for PSD: %d bits", bits));
        }

        mode = pInput.readShort();

        switch (mode) {
            case PSD.COLOR_MODE_BITMAP:
            case PSD.COLOR_MODE_GRAYSCALE:
            case PSD.COLOR_MODE_INDEXED:
            case PSD.COLOR_MODE_RGB:
            case PSD.COLOR_MODE_CMYK:
            case PSD.COLOR_MODE_MULTICHANNEL:
            case PSD.COLOR_MODE_DUOTONE:
            case PSD.COLOR_MODE_LAB:
                break;
            default:
                throw new IIOException(String.format("Unsupported color mode for PSD: %d", mode));
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[version: ")
                .append(largeFormat ? "2" : "1")
                .append(", channels: ")
                .append(channels)
                .append(", width: ")
                .append(width)
                .append(", height: ")
                .append(height)
                .append(", depth: ")
                .append(bits)
                .append(", mode: ")
                .append(mode)
                .append(" (")
                .append(modeAsString())
                .append(")]")
                .toString();
    }

    int getMaxSize() {
        return largeFormat ? PSB_MAX_SIZE : PSD_MAX_SIZE;
    }

    boolean hasValidDimensions() {
        return width <= getMaxSize() && height <= getMaxSize();
    }

    private String modeAsString() {
        switch (mode) {
            case PSD.COLOR_MODE_BITMAP:
                return "Monochrome";
            case PSD.COLOR_MODE_GRAYSCALE:
                return "Grayscale";
            case PSD.COLOR_MODE_INDEXED:
                return "Indexed";
            case PSD.COLOR_MODE_RGB:
                return "RGB";
            case PSD.COLOR_MODE_CMYK:
                return "CMYK";
            case PSD.COLOR_MODE_MULTICHANNEL:
                return "Multi channel";
            case PSD.COLOR_MODE_DUOTONE:
                return "Duotone";
            case PSD.COLOR_MODE_LAB:
                return "Lab color";
            default:
                return "Unkown mode";
        }
    }
}
