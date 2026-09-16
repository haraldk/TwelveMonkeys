/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.tiff;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.EntryAbstractTest;
import com.twelvemonkeys.imageio.metadata.exif.EXIF;
import com.twelvemonkeys.imageio.metadata.exif.GPS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TIFFEntryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFEntryTest.java,v 1.0 02.01.12 17:35 haraldk Exp$
 */
public class TIFFEntryTest extends EntryAbstractTest {
    @Override
    protected Entry createEntry(final Object value) {
        return createEXIFEntry(TIFF.TAG_COPYRIGHT, value, (short) 2);
    }

    private TIFFEntry createEXIFEntry(final int identifier, final Object value, final int type) {
        return new TIFFEntry(identifier, (short) type, value);
    }

    @Test
    public void testCreateEXIFEntryIllegalType() {
        assertThrows(IllegalArgumentException.class, () -> createEXIFEntry(0, null, -1));
    }

    @Test
    public void testGetFieldNameTIFF() {
        assertEquals("Make", new TIFFEntry(TIFF.TAG_MAKE, "Apple").getFieldName());
        assertEquals("FNumber", new TIFFEntry(EXIF.TAG_F_NUMBER, new Rational(28, 5)).getFieldName());
    }

    @Test
    public void testGetFieldNameEXIF23() {
        assertEquals("SensitivityType", new TIFFEntry(EXIF.TAG_SENSITIVITY_TYPE, 2).getFieldName());
        assertEquals("OffsetTimeOriginal", new TIFFEntry(EXIF.TAG_OFFSET_TIME_ORIGINAL, "-05:00").getFieldName());
        assertEquals("SubsecTimeOriginal", new TIFFEntry(EXIF.TAG_SUBSEC_TIME_ORIGINAL, "42").getFieldName());
        assertEquals("BodySerialNumber", new TIFFEntry(EXIF.TAG_BODY_SERIAL_NUMBER, "42").getFieldName());
        assertEquals("LensSpecification", new TIFFEntry(EXIF.TAG_LENS_SPECIFICATION, new int[] {24, 105, 0, 0}).getFieldName());
        assertEquals("LensMake", new TIFFEntry(EXIF.TAG_LENS_MAKE, "Apple").getFieldName());
        assertEquals("LensModel", new TIFFEntry(EXIF.TAG_LENS_MODEL, "iPhone 11 back dual wide camera 4.25mm f/1.8").getFieldName());
        assertEquals("CompositeImage", new TIFFEntry(EXIF.TAG_COMPOSITE_IMAGE, 2).getFieldName());
    }

    @Test
    public void testGetFieldNameGPS() {
        assertEquals("GPSVersionID", new TIFFEntry(GPS.TAG_GPS_VERSION_ID, new byte[] {2, 3, 0, 0}).getFieldName());
        assertEquals("GPSLongitudeRef", new TIFFEntry(GPS.TAG_GPS_LONGITUDE_REF, "W").getFieldName());
        assertEquals("GPSLongitude", new TIFFEntry(GPS.TAG_GPS_LONGITUDE, new Rational[] {new Rational(80), new Rational(29, 4), new Rational(0)}).getFieldName());
        assertEquals("GPSAltitude", new TIFFEntry(GPS.TAG_GPS_ALTITUDE, new Rational(10185, 881)).getFieldName());
        assertEquals("GPSDateStamp", new TIFFEntry(GPS.TAG_GPS_DATE_STAMP, "2011:10:27").getFieldName());

        // GPS tags 1 (GPSLatitudeRef) and 2 (GPSLatitude) can NOT be named,
        // as their ids collide with the Interoperability IFD tags 1 and 2
        assertNull(new TIFFEntry(GPS.TAG_GPS_LATITUDE_REF, "N").getFieldName());
        assertNull(new TIFFEntry(GPS.TAG_GPS_LATITUDE, new Rational[] {new Rational(25), new Rational(4917, 100), new Rational(0)}).getFieldName());
    }
}
