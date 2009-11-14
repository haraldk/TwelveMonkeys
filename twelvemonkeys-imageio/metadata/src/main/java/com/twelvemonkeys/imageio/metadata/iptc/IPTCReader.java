package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

/**
 * IPTCReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IPTCReader.java,v 1.0 Nov 13, 2009 8:37:23 PM haraldk Exp$
 */
public class IPTCReader extends MetadataReader {
    private static final int ENCODING_UNKNOWN = -1;
    private static final int ENCODING_UNSPECIFIED = 0;
    private static final int ENCODING_UTF_8 = 0x1b2547;

    private int mEncoding = ENCODING_UNSPECIFIED;


    @Override
    public Directory read(final ImageInputStream pInput) throws IOException {
        final List<Entry> entries = new ArrayList<Entry>();

        // 0x1c identifies start of a tag
        while (pInput.read() == 0x1c) {
            int tagId = pInput.readShort();
            int tagByteCount = pInput.readUnsignedShort();

            Entry entry = readEntry(pInput, tagId, tagByteCount);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return new IPTCDirectory(entries);
    }

    private Entry readEntry(final ImageInputStream pInput, final int pTagId, final int pLength) throws IOException {
        Object value = null;

        switch (pTagId) {
            case IPTC.TAG_CODED_CHARACTER_SET:
                // TODO: Mapping from ISO 646 to Java supported character sets?
                // TODO: Move somewhere else?
                mEncoding = parseEncoding(pInput, pLength);
                return null;
            case IPTC.TAG_RECORD_VERSION:
                // A single unsigned short value
                value = pInput.readUnsignedShort();
                break;
//                case IPTC.TAG_RELEASE_DATE:
//                case IPTC.TAG_EXPIRATION_DATE:
//                case IPTC.TAG_REFERENCE_DATE:
//                case IPTC.TAG_DATE_CREATED:
//                case IPTC.TAG_DIGITAL_CREATION_DATE:
//                    // Date object
//                    Date date = parseISO8601DatePart(pInput, tagByteCount);
//                    if (date != null) {
//                        directory.setDate(tagIdentifier, date);
//                        return;
//                    }
//                case IPTC.TAG_RELEASE_TIME:
//                case IPTC.TAG_EXPIRATION_TIME:
//                case IPTC.TAG_TIME_CREATED:
//                case IPTC.TAG_DIGITAL_CREATION_TIME:
//                    // NOTE: Spec says fields should be sent in order, so this is okay
//                    date = getDateForTime(directory, tagIdentifier);
//
//                    Date time = parseISO8601TimePart(pInput, tagByteCount, date);
//                    if (time != null) {
//                        directory.setDate(tagIdentifier, time);
//                        return;
//                    }
//
            default:
                // Skip non-Application fields, as they are typically not human readable
                if ((pTagId & 0xff00) != IPTC.APPLICATION_RECORD) {
                    pInput.skipBytes(pLength);
                    return null;
                }

                // fall through
        }

        // If we don't have a value, treat it as a string
        if (value == null) {
            if (pLength < 1) {
                value = "(No value)";
            }
            else {
                value = parseString(pInput, pLength);
            }
        }

        return new IPTCEntry(pTagId, value);
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
            if (mEncoding == ENCODING_UTF_8) {
                throw new IIOException("Wrong encoding of IPTC data, explicitly set to UTF-8 in DataSet 1:90", notUTF8);
            }

            // Fall back to use ISO-8859-1
            // This will not fail, but may may create wrong fallback-characters
            return StringUtil.decode(data, 0, data.length, "ISO8859_1");
        }
    }

}
