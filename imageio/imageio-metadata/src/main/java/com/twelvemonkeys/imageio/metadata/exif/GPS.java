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

package com.twelvemonkeys.imageio.metadata.exif;

/**
 * GPS tag constants, for tags found in the EXIF GPS Info IFD.
 * <p>
 * NOTE: These tag ids are only valid within a GPS IFD
 * (referenced from tag {@link com.twelvemonkeys.imageio.metadata.tiff.TIFF#TAG_GPS_IFD}),
 * as they overlap numerically with the regular TIFF tag id space.
 * </p>
 *
 * @see <a href="https://www.awaresystems.be/imaging/tiff/tifftags/privateifd/gps.html">GPS tags</a>
 */
@SuppressWarnings("UnusedDeclaration")
public interface GPS {
    int TAG_GPS_VERSION_ID = 0;
    int TAG_GPS_LATITUDE_REF = 1;
    int TAG_GPS_LATITUDE = 2;
    int TAG_GPS_LONGITUDE_REF = 3;
    int TAG_GPS_LONGITUDE = 4;
    int TAG_GPS_ALTITUDE_REF = 5;
    int TAG_GPS_ALTITUDE = 6;
    int TAG_GPS_TIME_STAMP = 7;
    int TAG_GPS_SATELLITES = 8;
    int TAG_GPS_STATUS = 9;
    int TAG_GPS_MEASURE_MODE = 10;
    int TAG_GPS_DOP = 11;
    int TAG_GPS_SPEED_REF = 12;
    int TAG_GPS_SPEED = 13;
    int TAG_GPS_TRACK_REF = 14;
    int TAG_GPS_TRACK = 15;
    int TAG_GPS_IMG_DIRECTION_REF = 16;
    int TAG_GPS_IMG_DIRECTION = 17;
    int TAG_GPS_MAP_DATUM = 18;
    int TAG_GPS_DEST_LATITUDE_REF = 19;
    int TAG_GPS_DEST_LATITUDE = 20;
    int TAG_GPS_DEST_LONGITUDE_REF = 21;
    int TAG_GPS_DEST_LONGITUDE = 22;
    int TAG_GPS_DEST_BEARING_REF = 23;
    int TAG_GPS_DEST_BEARING = 24;
    int TAG_GPS_DEST_DISTANCE_REF = 25;
    int TAG_GPS_DEST_DISTANCE = 26;
    int TAG_GPS_PROCESSING_METHOD = 27;
    int TAG_GPS_AREA_INFORMATION = 28;
    int TAG_GPS_DATE_STAMP = 29;
    int TAG_GPS_DIFFERENTIAL = 30;
    int TAG_GPS_H_POSITIONING_ERROR = 31;
}
