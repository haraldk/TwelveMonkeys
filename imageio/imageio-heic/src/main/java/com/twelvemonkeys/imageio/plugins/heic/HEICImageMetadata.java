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
import openize.heic.decoder.ExifData;
import openize.heic.decoder.ExifDirectoryType;
import openize.heic.decoder.HeicImageFrame;

import javax.imageio.ImageTypeSpecifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

final class HEICImageMetadata extends StandardImageMetadataSupport {
    // TODO: Pull out more info from the appropriate boxes/EXIF as needed...

    private static final int TAG_DATE_TIME_ORIGINAL = 0x9003;
    private static final int TAG_DATE_TIME = 0x0132;

    HEICImageMetadata(ImageTypeSpecifier type, HeicImageFrame frame) {
        super(withFrameValues(builder(type), frame));
    }

    private static Builder withFrameValues(Builder builder, HeicImageFrame frame) {
        builder.withCompressionTypeName("HEVC")
               .withCompressionLossless(false);
        // NOTE: Orientation is left as Normal, the decoder
        // already applies 'irot'/'imir' transforms while decoding

        Calendar creationTime = creationTime(frame.Exif);
        if (creationTime != null) {
            builder.withDocumentCreationTime(creationTime);
        }

        return builder;
    }

    private static Calendar creationTime(ExifData exif) {
        if (exif == null) {
            return null;
        }

        try {
            String dateTime = exif.getExifString(ExifDirectoryType.ExifSubIfdDirectory, TAG_DATE_TIME_ORIGINAL);
            if (dateTime == null) {
                dateTime = exif.getExifString(ExifDirectoryType.ExifIfd0Directory, TAG_DATE_TIME);
            }

            if (dateTime != null && dateTime.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                DateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("UTC")); // EXIF date/time has no zone info
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTime(format.parse(dateTime));

                return calendar;
            }
        }
        catch (Exception ignore) {
            // Bad or unparseable EXIF should never prevent reading metadata
        }

        return null;
    }
}
