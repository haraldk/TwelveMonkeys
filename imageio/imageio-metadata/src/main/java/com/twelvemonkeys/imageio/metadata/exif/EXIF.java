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

package com.twelvemonkeys.imageio.metadata.exif;

/**
 * EXIF
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIF.java,v 1.0 Nov 11, 2009 5:36:04 PM haraldk Exp$
 */
@SuppressWarnings("UnusedDeclaration")
public interface EXIF {
    // See http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html
    int TAG_EXPOSURE_TIME = 33434;
    int TAG_F_NUMBER = 33437;
    int TAG_EXPOSURE_PROGRAM = 34850;
    int TAG_SPECTRAL_SENSITIVITY = 34852;
    int TAG_ISO_SPEED_RATINGS = 34855;
    int TAG_OECF = 34856;
    int TAG_EXIF_VERSION = 36864;
    int TAG_DATE_TIME_ORIGINAL = 36867;
    int TAG_DATE_TIME_DIGITIZED = 36868;
    int TAG_COMPONENTS_CONFIGURATION = 37121;
    int TAG_COMPRESSED_BITS_PER_PIXEL = 37122;
    int TAG_SHUTTER_SPEED_VALUE = 37377;
    int TAG_APERTURE_VALUE = 37378;
    int TAG_BRIGHTNESS_VALUE = 37379;
    int TAG_EXPOSURE_BIAS_VALUE = 37380;
    int TAG_MAX_APERTURE_VALUE = 37381;
    int TAG_SUBJECT_DISTANCE = 37382;
    int TAG_METERING_MODE = 37383;
    int TAG_LIGHT_SOURCE = 37384;
    int TAG_FLASH = 37385;
    int TAG_FOCAL_LENGTH = 37386;
    int TAG_IMAGE_NUMBER = 37393;
    int TAG_SUBJECT_AREA = 37396;
    int TAG_MAKER_NOTE = 37500;
    int TAG_USER_COMMENT = 37510;
    int TAG_SUBSEC_TIME = 37520;
    int TAG_SUBSEC_TIME_ORIGINAL = 37521;
    int TAG_SUBSEC_TIME_DIGITIZED = 37522;
    int TAG_FLASHPIX_VERSION = 40960;
    int TAG_COLOR_SPACE = 40961;
    int TAG_PIXEL_X_DIMENSION = 40962;
    int TAG_PIXEL_Y_DIMENSION = 40963;
    int TAG_RELATED_SOUND_FILE = 40964;
    int TAG_FLASH_ENERGY = 41483;
    int TAG_SPATIAL_FREQUENCY_RESPONSE = 41484;
    int TAG_FOCAL_PLANE_X_RESOLUTION = 41486;
    int TAG_FOCAL_PLANE_Y_RESOLUTION = 41487;
    int TAG_FOCAL_PLANE_RESOLUTION_UNIT = 41488;
    int TAG_SUBJECT_LOCATION = 41492;
    int TAG_EXPOSURE_INDEX = 41493;
    int TAG_SENSING_METHOD = 41495;
    int TAG_FILE_SOURCE = 41728;
    int TAG_SCENE_TYPE = 41729;
    int TAG_CFA_PATTERN = 41730;
    int TAG_CUSTOM_RENDERED = 41985;
    int TAG_EXPOSURE_MODE = 41986;
    int TAG_WHITE_BALANCE = 41987;
    int TAG_DIGITAL_ZOOM_RATIO = 41988;
    int TAG_FOCAL_LENGTH_IN_35_MM_FILM = 41989;
    int TAG_SCENE_CAPTURE_TYPE = 41990;
    int TAG_GAIN_CONTROL = 41991;
    int TAG_CONTRAST = 41992;
    int TAG_SATURATION = 41993;
    int TAG_SHARPNESS = 41994;
    int TAG_DEVICE_SETTING_DESCRIPTION = 41995;
    int TAG_SUBJECT_DISTANCE_RANGE = 41996;
    int TAG_IMAGE_UNIQUE_ID = 42016;
}
