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

import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * PSDImageResource
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageResource.java,v 1.0 Apr 29, 2008 5:49:06 PM haraldk Exp$
 */
class PSDImageResource {
    final short mId;
    final String mName;
    final long mSize;

    PSDImageResource(final short pId, final ImageInputStream pInput) throws IOException {
        mId = pId;

        mName = PSDUtil.readPascalString(pInput);

        // Skip pad
        int nameSize = mName.length() + 1;
        if (nameSize % 2 != 0) {
            pInput.readByte();
        }

        mSize = pInput.readUnsignedInt();
        readData(pInput);

        // Data is even-padded
        if (mSize % 2 != 0) {
            pInput.read();
        }
    }

    /**
     * This default implementation simply skips the data.
     *
     * @param pInput the input
     * @throws IOException if an I/O exception occurs
     */
    protected void readData(final ImageInputStream pInput) throws IOException {
        // TODO: This design is ugly, as subclasses readData is invoked BEFORE their respective constructor...
        pInput.skipBytes(mSize);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", data length: ");
        builder.append(mSize);
        builder.append("]");

        return builder.toString();
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());

        String fakeType = resourceTypeForId(mId);
        if (fakeType != null) {
            builder.append("(").append(fakeType).append(")");
        }

        builder.append("[ID: 0x");
        builder.append(Integer.toHexString(mId));
        if (mName != null && mName.trim().length() != 0) {
            builder.append(", name: \"");
            builder.append(mName);
            builder.append("\"");
        }

        return builder;
    }

    static String resourceTypeForId(final short pId) {
        switch (pId) {
            case PSD.RES_RESOLUTION_INFO:
            case PSD.RES_ALPHA_CHANNEL_INFO:
            case PSD.RES_DISPLAY_INFO:
            case PSD.RES_PRINT_FLAGS:
            case PSD.RES_THUMBNAIL_PS4:
            case PSD.RES_THUMBNAIL:
            case PSD.RES_ICC_PROFILE:
            case PSD.RES_VERSION_INFO:
            case PSD.RES_EXIF_DATA_1:
//            case PSD.RES_EXIF_DATA_3:
            case PSD.RES_XMP_DATA:
            case PSD.RES_PRINT_FLAGS_INFORMATION:
                return null;
            default:
                try {
                    for (Field field : PSD.class.getDeclaredFields()) {
                        if (field.getName().startsWith("RES_") && field.getInt(null) == pId) {
                            String name = field.getName().substring(4);
                            return StringUtil.lispToCamel(name.replace("_", "-").toLowerCase(), true);
                        }
                    }
                }
                catch (IllegalAccessException ignore) {
                }
                
                return "unknown resource";
        }
    }

    public static PSDImageResource read(final ImageInputStream pInput) throws IOException {
        int type = pInput.readInt();
        if (type != PSD.RESOURCE_TYPE) {
            throw new IIOException(String.format("Wrong image resource type, expected '8BIM': '%s'", PSDUtil.intToStr(type)));
        }

        // TODO: Process more of the resource stuff, most important are IPTC, EXIF and XMP data,
        // version info, and thumbnail for thumbnail-support.
        short id = pInput.readShort();
        switch (id) {
            case PSD.RES_RESOLUTION_INFO:
                return new PSDResolutionInfo(id, pInput);
            case PSD.RES_ALPHA_CHANNEL_INFO:
                return new PSDAlphaChannelInfo(id, pInput);
            case PSD.RES_DISPLAY_INFO:
                return new PSDDisplayInfo(id, pInput);
            case PSD.RES_PRINT_FLAGS:
                return new PSDPrintFlags(id, pInput);
            case PSD.RES_THUMBNAIL_PS4:
            case PSD.RES_THUMBNAIL:
                return new PSDThumbnail(id, pInput);
            case PSD.RES_ICC_PROFILE:
                return new ICCProfile(id, pInput);
            case PSD.RES_VERSION_INFO:
                return new PSDVersionInfo(id, pInput);
            case PSD.RES_EXIF_DATA_1:
                return new PSDEXIF1Data(id, pInput);
            case PSD.RES_XMP_DATA:
                return new PSDXMPData(id, pInput);
            case PSD.RES_PRINT_FLAGS_INFORMATION:
                return new PSDPrintFlagsInformation(id, pInput);
            default:
                if (id >= 0x07d0 && id <= 0x0bb6) {
                    // TODO: Parse saved path information
                    return new PSDImageResource(id, pInput);
                }
                else {
                    return new PSDImageResource(id, pInput);
                }
        }

    }
}
