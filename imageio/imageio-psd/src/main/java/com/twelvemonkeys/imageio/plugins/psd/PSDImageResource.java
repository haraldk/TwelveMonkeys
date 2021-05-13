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

import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * PSDImageResource
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageResource.java,v 1.0 Apr 29, 2008 5:49:06 PM haraldk Exp$
 */
public class PSDImageResource {
    // TODO: Refactor image resources to separate package
    // TODO: Change constructor to store stream offset and length only (+ possibly the name), defer reading

    final short id;
    final String name;
    final long size;

    PSDImageResource(final short resourceId, final ImageInputStream input) throws IOException {
        id = resourceId;

        name = PSDUtil.readPascalString(input);

        // Skip pad
        int nameSize = name.length() + 1;
        if (nameSize % 2 != 0) {
            input.readByte();
        }

        size = input.readUnsignedInt();
        long startPos = input.getStreamPosition();

        readData(new SubImageInputStream(input, size));

        // NOTE: This should never happen, however it's safer to keep it here to 
        if (input.getStreamPosition() != startPos + size) {
            input.seek(startPos + size);
        }

        // Data is even-padded (word aligned)
        if (size % 2 != 0) {
            input.read();
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
        pInput.skipBytes(size);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", data length: ");
        builder.append(size);
        builder.append("]");

        return builder.toString();
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());

        String fakeType = resourceTypeForId(id);
        if (fakeType != null) {
            builder.append("(").append(fakeType).append(")");
        }

        builder.append("[ID: 0x");
        builder.append(Integer.toHexString(id));
        if (name != null && !name.trim().isEmpty()) {
            builder.append(", name: \"");
            builder.append(name);
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
            case PSD.RES_IPTC_NAA:
            case PSD.RES_GRID_AND_GUIDES_INFO:
            case PSD.RES_THUMBNAIL_PS4:
            case PSD.RES_THUMBNAIL:
            case PSD.RES_ICC_PROFILE:
            case PSD.RES_VERSION_INFO:
            case PSD.RES_EXIF_DATA_1:
//            case PSD.RES_EXIF_DATA_3:
            case PSD.RES_XMP_DATA:
            case PSD.RES_PRINT_SCALE:
            case PSD.RES_PIXEL_ASPECT_RATIO:
            case PSD.RES_PRINT_FLAGS_INFORMATION:
                return null;
            default:
                if (pId >= PSD.RES_PATH_INFO_MIN && pId <= PSD.RES_PATH_INFO_MAX) {
                    return "PathInformationResource";
                }
                if (pId >= PSD.RES_PLUGIN_MIN && pId <= PSD.RES_PLUGIN_MAX) {
                    return "PluginResource";
                }

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

                return "UnknownResource";
        }
    }

    public static PSDImageResource read(final ImageInputStream pInput) throws IOException {
        int type = pInput.readInt();
        switch (type) {
            case com.twelvemonkeys.imageio.metadata.psd.PSD.RESOURCE_TYPE_IMAGEREADY:
            case com.twelvemonkeys.imageio.metadata.psd.PSD.RESOURCE_TYPE_PHOTODELUXE:
            case com.twelvemonkeys.imageio.metadata.psd.PSD.RESOURCE_TYPE_LIGHTROOM:
            case com.twelvemonkeys.imageio.metadata.psd.PSD.RESOURCE_TYPE_DCSR:
                // TODO: Warning for these types!
            case com.twelvemonkeys.imageio.metadata.psd.PSD.RESOURCE_TYPE:
                break;
            default:
                throw new IIOException(String.format("Wrong image resource type, expected '8BIM': '%s'", PSDUtil.intToStr(type)));
        }

        // TODO: Have PSDImageResources defer actual parsing? (Just store stream offsets)
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
            case PSD.RES_IPTC_NAA:
                return new PSDIPTCData(id, pInput);
            case PSD.RES_GRID_AND_GUIDES_INFO:
                return new PSDGridAndGuideInfo(id, pInput);
            case PSD.RES_THUMBNAIL_PS4:
            case PSD.RES_THUMBNAIL:
                return new PSDThumbnail(id, pInput);
            case PSD.RES_ICC_PROFILE:
                return new ICCProfile(id, pInput);
            case PSD.RES_UNICODE_ALPHA_NAMES:
                return new PSDUnicodeAlphaNames(id, pInput);
            case PSD.RES_VERSION_INFO:
                return new PSDVersionInfo(id, pInput);
            case PSD.RES_EXIF_DATA_1:
                return new PSDEXIF1Data(id, pInput);
            case PSD.RES_XMP_DATA:
                return new PSDXMPData(id, pInput);
            case PSD.RES_PRINT_SCALE:
                return new PSDPrintScale(id, pInput);
            case PSD.RES_PIXEL_ASPECT_RATIO:
                return new PSDPixelAspectRatio(id, pInput);
            case PSD.RES_PRINT_FLAGS_INFORMATION:
                return new PSDPrintFlagsInformation(id, pInput);
            default:
                if (id >= PSD.RES_PATH_INFO_MIN && id <= PSD.RES_PATH_INFO_MAX) {
                    return new PSDPathResource(id, pInput);
                }
                else {
                    return new PSDImageResource(id, pInput);
                }
        }

    }
}
