package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.iptc.IPTCReader;
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
import java.util.Iterator;
import java.util.List;

/**
 * PSDIPTCData
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDIPTCData.java,v 1.0 Nov 7, 2009 9:52:14 PM haraldk Exp$
 */
final class PSDIPTCData extends PSDImageResource {
    // TODO: Refactor to be more like PSDEXIF1Data...
    // TODO: Extract IPTC/EXIF/XMP metadata extraction/parsing to separate module(s)
    com.twelvemonkeys.imageio.metadata.Directory mDirectory;

    PSDIPTCData(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        // Read IPTC directory
        mDirectory = new IPTCReader().read(pInput);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();
        builder.append(", ").append(mDirectory);
        builder.append("]");
        return builder.toString();
    }

    static class Entry {
        final int mTagId;
        private Object mValue;

        public Entry(final int pTagId, final Object pValue) {
            mTagId = pTagId;
            mValue = pValue;
        }

        @Override
        public String toString() {
            return String.format("%d:%d: %s", mTagId >> 8, mTagId & 0xff, mValue);
        }

        public final String getTypeName() {
            // TODO: Should this really look like EXIF?
            if (mTagId == IPTC.TAG_RECORD_VERSION) {
                return "SHORT";
            }
            else if (mValue instanceof String) {
                return "ASCII";
            }

            return "Unknown type";
        }

        public final String getValueAsString() {
            return String.valueOf(mValue);
        }

        public final Object getValue() {
            return mValue;
        }
    }
    
    static class Directory implements Iterable<Entry> {
        private static final int ENCODING_UNKNOWN = -1;
        private static final int ENCODING_UNSPECIFIED = 0;
        private static final int ENCODING_UTF_8 = 0x1b2547;

        private int mEncoding = ENCODING_UNSPECIFIED;

        final List<Entry> mEntries = new ArrayList<Entry>();

        private Directory() {}

        @Override
        public String toString() {
            return "Directory" + mEntries.toString();
        }

        public Entry get(int pTagId) {
            for (Entry entry : mEntries) {
                if (entry.mTagId == pTagId) {
                    return entry;
                }
            }

            return null;
        }

        public Iterator<Entry> iterator() {
            return mEntries.iterator();
        }

        public static Directory read(final ImageInputStream pInput, final long pSize) throws IOException {
            Directory directory = new Directory();

            final long streamEnd = pInput.getStreamPosition() + pSize;

            // For each tag
            while (pInput.getStreamPosition() < streamEnd) {
                // Identifies start of a tag
                byte marker = pInput.readByte();

                if (marker != 0x1c) {
                    throw new IIOException(String.format("Corrupt IPTC stream segment, found 0x%02x (expected 0x1c)", marker));
                }

                int tagId = pInput.readShort();
                int tagByteCount = pInput.readUnsignedShort();

                directory.readEntry(pInput, tagId, tagByteCount);
            }

            return directory;
        }

        private void readEntry(final ImageInputStream pInput, final int pTagId, final int pLength) throws IOException {
            Object value = null;

            switch (pTagId) {
                case IPTC.TAG_CODED_CHARACTER_SET:
                    // TODO: Mapping from ISO 646 to Java supported character sets?
                    // TODO: Move somewhere else?
                    mEncoding = parseEncoding(pInput, pLength);
                    return;
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
                        return;
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

            mEntries.add(new Entry(pTagId, value));
        }

//        private Date getDateForTime(final Directory directory, final int tagIdentifier) {
//            int dateTag;
//
//            switch (tagIdentifier) {
//                case IPTC.TAG_RELEASE_TIME:
//                    dateTag = IPTC.TAG_RELEASE_DATE;
//                    break;
//                case IPTC.TAG_EXPIRATION_TIME:
//                    dateTag = IPTC.TAG_EXPIRATION_DATE;
//                    break;
//                case IPTC.TAG_TIME_CREATED:
//                    dateTag = IPTC.TAG_DATE_CREATED;
//                    break;
//                case IPTC.TAG_DIGITAL_CREATION_TIME:
//                    dateTag = IPTC.TAG_DIGITAL_CREATION_DATE;
//                    break;
//                default:
//                    return new Date(0l);
//            }
//
//            return directory.containsTag(dateTag) ? directory.getDate(dateTag) : new Date(0l);
//        }


        private int parseEncoding(final ImageInputStream pInput, int tagByteCount) throws IOException {
            return tagByteCount == 3
                    && (pInput.readUnsignedByte() << 16 | pInput.readUnsignedByte() << 8 | pInput.readUnsignedByte()) == ENCODING_UTF_8
                    ? ENCODING_UTF_8 : ENCODING_UNKNOWN;
        }

//        private Date parseISO8601TimePart(final ImageInputStream pInputStream, int tagByteCount, final Date date) throws IOException {
//            // ISO 8601: HHMMSS±HHMM
//            if (tagByteCount >= 11) {
//                String timeStr = parseString(pInputStream, tagByteCount);
//                try {
//                    int hour = Integer.parseInt(timeStr.substring(0, 2));
//                    int minute = Integer.parseInt(timeStr.substring(2, 4));
//                    int second = Integer.parseInt(timeStr.substring(4, 6));
//                    String tzOffset = timeStr.substring(6, 11);
//
//                    TimeZone zone = new SimpleTimeZone(Integer.parseInt(tzOffset.charAt(0) == '+' ? tzOffset.substring(1) : tzOffset), tzOffset);
//
//                    GregorianCalendar calendar = new GregorianCalendar(zone);
//                    calendar.setTime(date);
//
//                    calendar.add(Calendar.HOUR_OF_DAY, hour);
//                    calendar.add(Calendar.MINUTE, minute);
//                    calendar.add(Calendar.SECOND, second);
//
//                    return calendar.getTime();
//                }
//                catch (NumberFormatException e) {
//                    // fall through and we'll store whatever was there as a String
//                }
//            }
//            return null;
//        }
//
//        private Date parseISO8601DatePart(final ImageInputStream pInputStream, int tagByteCount) throws IOException {
//            // ISO 8601: CCYYMMDD
//            if (tagByteCount >= 8) {
//                String dateStr = parseString(pInputStream, tagByteCount);
//                try {
//                    int year = Integer.parseInt(dateStr.substring(0, 4));
//                    int month = Integer.parseInt(dateStr.substring(4, 6)) - 1;
//                    int day = Integer.parseInt(dateStr.substring(6, 8));
//                    GregorianCalendar calendar = new GregorianCalendar(year, month, day);
//                    return calendar.getTime();
//                }
//                catch (NumberFormatException e) {
//                    // fall through and we'll store whatever was there as a String
//                }
//            }
//            return null;
//        }

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

    static interface IPTC {
        static final int ENVELOPE_RECORD = 1 << 8;
        static final int APPLICATION_RECORD = 2 << 8;

        static final int TAG_CODED_CHARACTER_SET = ENVELOPE_RECORD | 90;

        /** 2:00 Record Version (mandatory) */
        public static final int TAG_RECORD_VERSION = APPLICATION_RECORD;       // 0x0200
//        /** 2:03 Object Type Reference */
//        public static final int TAG_OBJECT_TYPE_REFERENCE = APPLICATION_RECORD | 3;
//        /** 2:04 Object Attribute Reference (repeatable) */
//        public static final int TAG_OBJECT_ATTRIBUTE_REFERENCE = APPLICATION_RECORD | 4;
//        /** 2:05 Object Name */
//        public static final int TAG_OBJECT_NAME = APPLICATION_RECORD | 5;   // 0x0205
//        /** 2:07 Edit Status */
//        public static final int TAG_EDIT_STATUS = APPLICATION_RECORD | 7;
//        /** 2:08 Editorial Update */
//        public static final int TAG_EDITORIAL_UPDATE = APPLICATION_RECORD | 8;
//        /** 2:10 Urgency */
//        public static final int TAG_URGENCY = APPLICATION_RECORD | 10;
//        /** 2:12 Subect Reference (repeatable) */
//        public static final int TAG_SUBJECT_REFERENCE = APPLICATION_RECORD | 12;
//        /** 2:15 Category */
//        public static final int TAG_CATEGORY = APPLICATION_RECORD | 15;  // 0x020f
//        /** 2:20 Supplemental Category (repeatable) */
//        public static final int TAG_SUPPLEMENTAL_CATEGORIES = APPLICATION_RECORD | 20;
//        /** 2:22 Fixture Identifier */
//        public static final int TAG_FIXTURE_IDENTIFIER = APPLICATION_RECORD | 22;
//        /** 2:25 Keywords (repeatable) */
//        public static final int TAG_KEYWORDS = APPLICATION_RECORD | 25;
//        /** 2:26 Content Locataion Code (repeatable) */
//        public static final int TAG_CONTENT_LOCATION_CODE = APPLICATION_RECORD | 26;
//        /** 2:27 Content Locataion Name (repeatable) */
//        public static final int TAG_CONTENT_LOCATION_NAME = APPLICATION_RECORD | 27;
//        /** 2:30 Release Date */
//        public static final int TAG_RELEASE_DATE = APPLICATION_RECORD | 30;
//        /** 2:35 Release Time */
//        public static final int TAG_RELEASE_TIME = APPLICATION_RECORD | 35;
//        /** 2:37 Expiration Date */
//        public static final int TAG_EXPIRATION_DATE = APPLICATION_RECORD | 37;
//        /** 2:38 Expiration Time */
//        public static final int TAG_EXPIRATION_TIME = APPLICATION_RECORD | 38;
//        /** 2:40 Special Instructions */
//        public static final int TAG_SPECIAL_INSTRUCTIONS = APPLICATION_RECORD | 40;  // 0x0228
//        /** 2:42 Action Advised (1: Kill, 2: Replace, 3: Append, 4: Reference) */
//        public static final int TAG_ACTION_ADVICED = APPLICATION_RECORD | 42;
//        /** 2:45 Reference Service (repeatable in triplets with 2:47 and 2:50) */
//        public static final int TAG_REFERENCE_SERVICE = APPLICATION_RECORD | 45;
//        /** 2:47 Reference Date (mandatory if 2:45 present) */
//        public static final int TAG_REFERENCE_DATE = APPLICATION_RECORD | 47;
//        /** 2:50 Reference Number (mandatory if 2:45 present) */
//        public static final int TAG_REFERENCE_NUMBER = APPLICATION_RECORD | 50;
//        /** 2:55 Date Created */
//        public static final int TAG_DATE_CREATED = APPLICATION_RECORD | 55; // 0x0237
//        /** 2:60 Time Created */
//        public static final int TAG_TIME_CREATED = APPLICATION_RECORD | 60;
//        /** 2:62 Digital Creation Date */
//        public static final int TAG_DIGITAL_CREATION_DATE = APPLICATION_RECORD | 62;
//        /** 2:63 Digital Creation Date */
//        public static final int TAG_DIGITAL_CREATION_TIME = APPLICATION_RECORD | 63;
//        /** 2:65 Originating Program */
//        public static final int TAG_ORIGINATING_PROGRAM = APPLICATION_RECORD | 65;
//        /** 2:70 Program Version (only valid if 2:65 present) */
//        public static final int TAG_PROGRAM_VERSION = APPLICATION_RECORD | 70;
//        /** 2:75 Object Cycle (a: morning, p: evening, b: both) */
//        public static final int TAG_OBJECT_CYCLE = APPLICATION_RECORD | 75;
//        /** 2:80 By-line (repeatable) */
//        public static final int TAG_BY_LINE = APPLICATION_RECORD | 80;  // 0x0250
//        /** 2:85 By-line Title (repeatable) */
//        public static final int TAG_BY_LINE_TITLE = APPLICATION_RECORD | 85;  // 0x0255
//        /** 2:90 City */
//        public static final int TAG_CITY = APPLICATION_RECORD | 90;  // 0x025a
//        /** 2:92 Sub-location */
//        public static final int TAG_SUB_LOCATION = APPLICATION_RECORD | 92;
//        /** 2:95 Province/State */
//        public static final int TAG_PROVINCE_OR_STATE = APPLICATION_RECORD | 95;  // 0x025f
//        /** 2:100 Country/Primary Location Code */
//        public static final int TAG_COUNTRY_OR_PRIMARY_LOCATION_CODE = APPLICATION_RECORD | 100;
//        /** 2:101 Country/Primary Location Name */
//        public static final int TAG_COUNTRY_OR_PRIMARY_LOCATION = APPLICATION_RECORD | 101; // 0x0265
//        /** 2:103 Original Transmission Reference */
//        public static final int TAG_ORIGINAL_TRANSMISSION_REFERENCE = APPLICATION_RECORD | 103; // 0x0267
//        /** 2:105 Headline */
//        public static final int TAG_HEADLINE = APPLICATION_RECORD | 105; // 0x0269
//        /** 2:110 Credit */
//        public static final int TAG_CREDIT = APPLICATION_RECORD | 110; // 0x026e
//        /** 2:115 Source */
//        public static final int TAG_SOURCE = APPLICATION_RECORD | 115; // 0x0273
//        /** 2:116 Copyright Notice */
//        public static final int TAG_COPYRIGHT_NOTICE = APPLICATION_RECORD | 116; // 0x0274
//        /** 2:118 Contact */
//        public static final int TAG_CONTACT = APPLICATION_RECORD | 118;
//        /** 2:120 Catption/Abstract */
//        public static final int TAG_CAPTION = APPLICATION_RECORD | 120; // 0x0278
//        /** 2:122 Writer/Editor (repeatable) */
//        public static final int TAG_WRITER = APPLICATION_RECORD | 122; // 0x027a
//        /** 2:125 Rasterized Caption (binary data) */
//        public static final int TAG_RASTERIZED_CATPTION = APPLICATION_RECORD | 125;
//        /** 2:130 Image Type */
//        public static final int TAG_IMAGE_TYPE = APPLICATION_RECORD | 130;
//        /** 2:131 Image Orientation */
//        public static final int TAG_IMAGE_ORIENTATION = APPLICATION_RECORD | 131;
//        /** 2:135 Language Identifier */
//        public static final int TAG_LANGUAGE_IDENTIFIER = APPLICATION_RECORD | 135;
//
//        // TODO: Should we expose this field?
//        /**
//         * 2:199 JobMinder Assignment Data (Custom IPTC field).
//         * A common custom IPTC field used by a now discontinued application called JobMinder.
//         *
//         * @see <a href="http://www.jobminder.net/">JobMinder Homepage</a>
//         */
//        static final int CUSTOM_TAG_JOBMINDER_ASSIGMENT_DATA = APPLICATION_RECORD | 199;
//
//        // TODO: 2:150-2:154 Audio and 2:200-2:202 Object Preview Data

    }
}
