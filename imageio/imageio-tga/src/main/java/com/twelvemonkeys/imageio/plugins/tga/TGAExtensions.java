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

package com.twelvemonkeys.imageio.plugins.tga;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import static com.twelvemonkeys.imageio.plugins.tga.TGA.EXT_AREA_SIZE;

/**
 * TGAExtensions.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TGAExtensions.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
final class TGAExtensions {

    private String authorName;
    private String authorComments;

    private Calendar creationDate;
    private String jobId;

    private String softwareId;
    private String softwareVersion;

    private int backgroundColor;
    private double pixelAspectRatio;
    private double gamma;

    private long colorCorrectionOffset;
    private long postageStampOffset;
    private long scanLineOffset;

    private int attributeType;

    private TGAExtensions() {
    }

    static TGAExtensions read(final ImageInputStream stream) throws IOException {
        int extSize = stream.readUnsignedShort();

        // Should always be 495 for version 2.0, no newer version exists...
        if (extSize < EXT_AREA_SIZE) {
            throw new IIOException(String.format("TGA Extension Area size less than %d: %d", EXT_AREA_SIZE, extSize));
        }

        TGAExtensions extensions = new TGAExtensions();
        extensions.authorName = readString(stream, 41);
        extensions.authorComments = readString(stream, 324);
        extensions.creationDate = readDate(stream);
        extensions.jobId = readString(stream, 41);

        stream.skipBytes(6); // Job time, 3 shorts, hours/minutes/seconds elapsed

        extensions.softwareId = readString(stream, 41);

        // Software version (* 100) short + single byte ASCII (ie. 101 'b' for 1.01b)
        int softwareVersion = stream.readUnsignedShort();
        int softwareLetter = stream.readByte();

        extensions.softwareVersion = softwareVersion != 0 && softwareLetter != ' '
                                     ? String.format("%d.%d%d", softwareVersion / 100, softwareVersion % 100, softwareLetter).trim()
                                     : null;

        extensions.backgroundColor = stream.readInt(); // ARGB

        extensions.pixelAspectRatio = readRational(stream);
        extensions.gamma = readRational(stream);

        extensions.colorCorrectionOffset = stream.readUnsignedInt();
        extensions.postageStampOffset = stream.readUnsignedInt();
        extensions.scanLineOffset = stream.readUnsignedInt();

        // Offset 494 specifies Attribute type:
        // 0: no Alpha data included (bits 3-0 of field 5.6 should also be set to zero)
        // 1: undefined data in the Alpha field, can be ignored
        // 2: undefined data in the Alpha field, but should be retained
        // 3: useful Alpha channel data is present
        // 4: pre-multiplied Alpha (see description below)
        // 5 -127: RESERVED
        // 128-255: Un-assigned
        extensions.attributeType = stream.readUnsignedByte();

        return extensions;
    }

    private static double readRational(final ImageInputStream stream) throws IOException {
        int numerator = stream.readUnsignedShort();
        int denominator = stream.readUnsignedShort();

        return denominator != 0 ? numerator / (double) denominator : 1;
    }

    private static Calendar readDate(final ImageInputStream stream) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();

        int month = stream.readUnsignedShort();
        int date = stream.readUnsignedShort();
        int year = stream.readUnsignedShort();

        int hourOfDay = stream.readUnsignedShort();
        int minute = stream.readUnsignedShort();
        int second = stream.readUnsignedShort();

        // Unused
        if (month == 0 && year == 0 && date == 0 && hourOfDay == 0 && minute == 0 && second == 0) {
            return null;
        }

        calendar.set(year, month - 1, date, hourOfDay, minute, second);

        return calendar;
    }

    private static String readString(final ImageInputStream stream, final int maxLength) throws IOException {
        byte[] data = new byte[maxLength];
        stream.readFully(data);

        return asZeroTerminatedASCIIString(data);
    }

    private static String asZeroTerminatedASCIIString(final byte[] data) {
        int len = data.length;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                len = i;
            }
        }

        return new String(data, 0, len, StandardCharsets.US_ASCII);
    }

    public boolean hasAlpha() {
        switch (attributeType) {
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    public boolean isAlphaPremultiplied() {
        switch (attributeType) {
            case 4:
                return true;
            default:
                return false;
        }
    }

    public long getThumbnailOffset() {
        return postageStampOffset;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorComments() {
        return authorComments;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getSoftware() {
        return softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public double getPixelAspectRatio() {
        return pixelAspectRatio;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }
}
