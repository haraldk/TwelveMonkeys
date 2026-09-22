/*
 * Copyright (c) 2025, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIF;
import com.twelvemonkeys.imageio.metadata.exif.GPS;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import openize.heic.decoder.ExifData;
import openize.heic.decoder.HeicImageFrame;

import javax.imageio.ImageTypeSpecifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

final class HEICImageMetadata extends StandardImageMetadataSupport {
    // TODO: Pull out more info from the appropriate boxes as needed (ICC profile, pixel aspect ratio etc.)

    HEICImageMetadata(ImageTypeSpecifier type, HeicImageFrame frame) {
        super(withFrameValues(builder(type), frame));
    }

    private static Builder withFrameValues(Builder builder, HeicImageFrame frame) {
        builder.withCompressionTypeName("HEVC")
               .withCompressionLossless(false);
        // NOTE: Orientation is deliberately left as Normal: The decoder already applies the
        // authoritative 'irot'/'imir' transforms while decoding, reporting the EXIF
        // orientation here would cause clients to rotate twice

        Directory exif = parseExif(frame.Exif);
        if (exif != null) {
            Calendar creationTime = creationTime(exif);
            if (creationTime != null) {
                builder.withDocumentCreationTime(creationTime);
            }

            builder.withTextEntries(textEntries(exif));
        }

        return builder;
    }

    private static Directory parseExif(ExifData exifData) {
        if (exifData == null) {
            return null;
        }

        try {
            byte[] rawExif = exifData.getRawBytes();
            int offset = tiffHeaderOffset(rawExif);
            if (offset < 0) {
                return null;
            }

            return new TIFFReader().read(new ByteArrayImageInputStream(rawExif, offset, rawExif.length - offset));
        }
        catch (Exception ignore) {
            // Bad or unparseable EXIF should never prevent reading metadata
            return null;
        }
    }

    private static int tiffHeaderOffset(byte[] rawExif) {
        if (rawExif == null || rawExif.length < 8) {
            return -1;
        }

        // The data is usually a plain TIFF stream, but be lenient,
        // and skip a possible "Exif\0\0" preamble or similar
        for (int i = 0; i < Math.min(rawExif.length - 1, 16); i++) {
            if ((rawExif[i] == 'I' && rawExif[i + 1] == 'I') || (rawExif[i] == 'M' && rawExif[i + 1] == 'M')) {
                return i;
            }
        }

        return -1;
    }

    private static Calendar creationTime(Directory exif) {
        // Prefer EXIF DateTimeOriginal (time of capture), fall back to TIFF DateTime (time of change)
        Entry dateTime = null;

        Entry exifIfdEntry = exif.getEntryById(TIFF.TAG_EXIF_IFD);
        if (exifIfdEntry != null && exifIfdEntry.getValue() instanceof Directory) {
            dateTime = ((Directory) exifIfdEntry.getValue()).getEntryById(EXIF.TAG_DATE_TIME_ORIGINAL);
        }
        if (dateTime == null) {
            dateTime = exif.getEntryById(TIFF.TAG_DATE_TIME);
        }
        if (dateTime == null || !(dateTime.getValue() instanceof String)) {
            return null;
        }

        String value = ((String) dateTime.getValue()).trim();
        if (!value.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC")); // EXIF date/time has no time zone info
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTime(format.parse(value));

            return calendar;
        }
        catch (ParseException ignore) {
            return null;
        }
    }

    private static Map<String, String> textEntries(Directory exif) {
        Map<String, String> entries = new LinkedHashMap<>();

        addTextEntries(entries, exif);

        Entry exifIfdEntry = exif.getEntryById(TIFF.TAG_EXIF_IFD);
        if (exifIfdEntry != null && exifIfdEntry.getValue() instanceof Directory) {
            addTextEntries(entries, (Directory) exifIfdEntry.getValue());
        }

        Entry gpsIfdEntry = exif.getEntryById(TIFF.TAG_GPS_IFD);
        if (gpsIfdEntry != null && gpsIfdEntry.getValue() instanceof Directory) {
            addTextEntries(entries, (Directory) gpsIfdEntry.getValue(), true);
        }

        return entries;
    }

    private static void addTextEntries(Map<String, String> entries, Directory directory) {
        addTextEntries(entries, directory, false);
    }

    private static void addTextEntries(Map<String, String> entries, Directory directory, boolean isGPSIFD) {
        for (Entry entry : directory) {
            Object value = entry.getValue();
            if (value == null || value instanceof Directory || value instanceof byte[]) {
                // Skip sub-IFDs and binary values (MakerNote, UserComment etc.)
                continue;
            }
            if (entry.getIdentifier().equals(TIFF.TAG_ORIENTATION)) {
                // Skip EXIF Orientation: The authoritative rotation ('irot'/'imir')
                // is already applied by the decoder, exposing it here would mislead clients
                continue;
            }

            String string = entry.getValueAsString();
            if (string == null) {
                continue;
            }

            string = string.trim();
            if (string.isEmpty() || string.length() > 200) {
                continue;
            }

            entries.putIfAbsent(keyword(entry, isGPSIFD), string);
        }
    }

    private static String keyword(Entry entry, boolean isGPSIFD) {
        if (isGPSIFD) {
            // GPS tags 1 and 2 are ambiguous outside a GPS IFD
            // (they collide with the Interoperability IFD tags), and thus not named by TIFFEntry
            if (entry.getIdentifier().equals(GPS.TAG_GPS_LATITUDE_REF)) {
                return "GPSLatitudeRef";
            }
            if (entry.getIdentifier().equals(GPS.TAG_GPS_LATITUDE)) {
                return "GPSLatitude";
            }
        }

        String fieldName = entry.getFieldName();
        return fieldName != null ? fieldName : "Tag" + entry.getIdentifier();
    }
}
