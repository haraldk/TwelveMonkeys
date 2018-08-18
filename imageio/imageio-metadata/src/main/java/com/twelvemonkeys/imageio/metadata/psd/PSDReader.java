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

package com.twelvemonkeys.imageio.metadata.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PhotoshopReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PhotoshopReader.java,v 1.0 04.01.12 11:56 haraldk Exp$
 */
public final class PSDReader extends MetadataReader {

    // TODO: Add constructor to allow optional parsing of resources
    // TODO: Maybe this should be modelled more like the JPEG segment parsing, as it's all binary data...
    // - Segment/SegmentReader + List<Segment>

    @Override
    public Directory read(final ImageInputStream input) throws IOException {
        Validate.notNull(input, "input");

        List<PSDEntry> entries = new ArrayList<>();

        while (true) {
            try {
                int type = input.readInt();

                switch (type) {
                    case PSD.RESOURCE_TYPE_IMAGEREADY:
                    case PSD.RESOURCE_TYPE_PHOTODELUXE:
                    case PSD.RESOURCE_TYPE_LIGHTROOM:
                    case PSD.RESOURCE_TYPE_DCSR:
                        // TODO: Warning for these types!
                    case PSD.RESOURCE_TYPE:
                        break;
                    default:
                        throw new IIOException(String.format("Wrong image resource type, expected '8BIM': '%08x'", type));
                }

                short id = input.readShort();

                PSDResource resource = new PSDResource(id, input);
                entries.add(new PSDEntry(id, resource.name(), resource.data()));

            }
            catch (EOFException e) {
                break;
            }
        }

        return new PSDDirectory(entries);
    }

    protected static class PSDResource {
        static String readPascalString(final DataInput pInput) throws IOException {
            int length = pInput.readUnsignedByte();

            if (length == 0) {
                return "";
            }

            byte[] bytes = new byte[length];
            pInput.readFully(bytes);

            return StringUtil.decode(bytes, 0, bytes.length, "ASCII");
        }

        final short id;
        final String name;
        final long size;

        byte[] data;

        PSDResource(final short resourceId, final ImageInputStream input) throws IOException {
            id = resourceId;

            name = readPascalString(input);

            // Skip pad
            int nameSize = name.length() + 1;
            if (nameSize % 2 != 0) {
                input.readByte();
            }

            size = input.readUnsignedInt();
            long startPos = input.getStreamPosition();

            readData(new SubImageInputStream(input, size));

            // NOTE: This should never happen, however it's safer to keep it here for future compatibility
            if (input.getStreamPosition() != startPos + size) {
                input.seek(startPos + size);
            }

            // Data is even-padded (word aligned)
            if (size % 2 != 0) {
                input.read();
            }
        }

        protected void readData(final ImageInputStream pInput) throws IOException {
            // TODO: This design is ugly, as subclasses readData is invoked BEFORE their respective constructor...
            data = new byte[(int) size];
            pInput.readFully(data);
        }

        public final int id() {
            return id;
        }

        public final byte[] data() {
            return data;
        }

        public String name() {
            return name;
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

            builder.append("[ID: 0x");
            builder.append(Integer.toHexString(id));
            if (name != null && name.trim().length() != 0) {
                builder.append(", name: \"");
                builder.append(name);
                builder.append("\"");
            }

            return builder;
        }
    }

}
