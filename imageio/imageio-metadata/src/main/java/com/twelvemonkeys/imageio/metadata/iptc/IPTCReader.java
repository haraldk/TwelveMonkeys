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

package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IPTCReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IPTCReader.java,v 1.0 Nov 13, 2009 8:37:23 PM haraldk Exp$
 */
public final class IPTCReader extends MetadataReader {
    private static final int ENCODING_UNKNOWN = -1;
    private static final int ENCODING_UNSPECIFIED = 0;
    private static final int ENCODING_UTF_8 = 0x1b2547;

    private int encoding = ENCODING_UNSPECIFIED;

    @Override
    public Directory read(final ImageInputStream input) throws IOException {
        Validate.notNull(input, "input");

        Map<Short, Entry> entries = new LinkedHashMap<>();

        // 0x1c identifies start of a tag
        while (input.read() == 0x1c) {
            short tagId = input.readShort();
            int tagByteCount = input.readUnsignedShort();

            boolean array = IPTC.Tags.isArray(tagId);
            Entry entry = readEntry(input, tagId, tagByteCount, array, array ? entries.get(tagId) : null);

            if (entry != null) {
                entries.put(tagId, entry);
            }
        }

        return new IPTCDirectory(entries.values());
    }

    private IPTCEntry mergeEntries(final short tagId, final Object newValue, final Entry oldEntry) {
        Object[] oldValue = oldEntry != null ? (Object[]) oldEntry.getValue() : null;
        Object[] value;

        if (newValue instanceof String) {
            if (oldValue == null) {
                value = new String[] {(String) newValue};
            }
            else {
                String[] array = (String[]) oldValue;
                value = Arrays.copyOf(array, array.length + 1);
                value[value.length - 1] = newValue;
            }
        }
        else {
            if (oldValue == null) {
                value = new Object[] {newValue};
            }
            else {
                value = Arrays.copyOf(oldValue, oldValue.length + 1);
                value [value .length - 1] = newValue;
            }
        }

        return new IPTCEntry(tagId, value);
    }

    private IPTCEntry readEntry(final ImageInputStream pInput, final short pTagId, final int pLength, final boolean array, final Entry oldEntry) throws IOException {
        Object value;

        switch (pTagId) {
            case IPTC.TAG_CODED_CHARACTER_SET:
                // TODO: Mapping from ISO 646 to Java supported character sets?
                encoding = parseEncoding(pInput, pLength);
                return null;
            case IPTC.TAG_RECORD_VERSION:
                // TODO: Assert length == 2?
                // A single unsigned short value
                value = pInput.readUnsignedShort();
                break;
            default:
                // TODO: Create Tags.getType(tag), to allow for more flexible types
                if ((pTagId & 0xff00) == IPTC.APPLICATION_RECORD) {
                    // Treat Application records as Strings
                    if (pLength < 1) {
                        value = null;
                    }
                    else {
                        value = parseString(pInput, pLength);
                    }
                }
                else {
                    // Non-Application fields, typically not human readable
                    byte[] data = new byte[pLength];
                    pInput.readFully(data);
                    value = data;
                }
        }

        return array ? mergeEntries(pTagId, value, oldEntry) : new IPTCEntry(pTagId, value);
    }

    private int parseEncoding(final ImageInputStream pInput, int tagByteCount) throws IOException {
        return tagByteCount == 3
                && (pInput.readUnsignedByte() << 16 | pInput.readUnsignedByte() << 8 | pInput.readUnsignedByte()) == ENCODING_UTF_8
                ? ENCODING_UTF_8 : ENCODING_UNKNOWN;
    }

    // TODO: Pass encoding as parameter? Use if specified
    private String parseString(final ImageInputStream pInput, final int pLength) throws IOException {
        byte[] data = new byte[pLength];
        pInput.readFully(data);

        // NOTE: The IPTC specification says character data should use ISO 646 or ISO 2022 encoding.
        // UTF-8 contains all 646 characters, but not 2022.
        // This is however close to what libiptcdata does, see: http://libiptcdata.sourceforge.net/docs/iptc-i18n.html
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();

        try {
            // First try to decode using UTF-8 (which seems to be the de-facto standard)
            // Will fail fast on illegal UTF-8-sequences
            CharBuffer chars = decoder.onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data));
            return chars.toString();
        }
        catch (CharacterCodingException notUTF8) {
            if (encoding == ENCODING_UTF_8) {
                throw new IIOException("Wrong encoding of IPTC data, explicitly set to UTF-8 in DataSet 1:90", notUTF8);
            }

            // Fall back to use ISO-8859-1
            // This will not fail, but may create wrong fallback-characters
            return StringUtil.decode(data, 0, data.length, "ISO8859_1");
        }
    }

}
