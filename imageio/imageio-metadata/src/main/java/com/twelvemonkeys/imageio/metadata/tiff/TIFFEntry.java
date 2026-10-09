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

package com.twelvemonkeys.imageio.metadata.tiff;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIF;
import com.twelvemonkeys.imageio.metadata.exif.GPS;
import com.twelvemonkeys.lang.Validate;

import java.lang.reflect.Array;

/**
 * Represents a TIFF IFD entry.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFEntry.java,v 1.0 Nov 13, 2009 5:47:35 PM haraldk Exp$
 *
 * @see TIFF
 * @see IFD
 */
public final class TIFFEntry extends AbstractEntry {
    final private short type;

    /**
     * Creates a new {@code TIFFEntry}.
     *
     * @param identifier the TIFF tag identifier.
     * @param value the value of the entry.
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     *
     * @see #TIFFEntry(int, short, Object)
     */
    public TIFFEntry(final int identifier, final Object value) {
        this(identifier, guessType(value), value);
    }

    /**
     * Creates a new {@code TIFFEntry}.
     *
     * @param identifier the TIFF tag identifier.
     * @param type the type of the entry.
     * @param value the value of the entry.
     *
     * @throws IllegalArgumentException if {@code type} is not a legal TIFF type.
     *
     * @see TIFF
     */
    public TIFFEntry(final int identifier, final short type, final Object value) {
        super(identifier, value);

        if (type < 1 || type >= TIFF.TYPE_NAMES.length) {
            throw new IllegalArgumentException(String.format("Illegal TIFF type: %s", type));
        }

        // TODO: Validate that type is applicable to value?
        
        this.type = type;
    }

    public short getType() {
        return type;
    }

    @Override
    public String getFieldName() {
        switch ((Integer) getIdentifier()) {
            case TIFF.TAG_EXIF_IFD:
                return "EXIF";
            case TIFF.TAG_INTEROP_IFD:
                return "Interoperability";
            case TIFF.TAG_GPS_IFD:
                return "GPS";
            case TIFF.TAG_XMP:
                return "XMP";
            case TIFF.TAG_IPTC:
                return "IPTC";
            case TIFF.TAG_PHOTOSHOP:
                return "Adobe";
            case TIFF.TAG_PHOTOSHOP_IMAGE_SOURCE_DATA:
                return "ImageSourceData";
            case TIFF.TAG_ICC_PROFILE:
                return "ICCProfile";

            case TIFF.TAG_IMAGE_WIDTH:
                return "ImageWidth";
            case TIFF.TAG_IMAGE_HEIGHT:
                return "ImageHeight";
            case TIFF.TAG_BITS_PER_SAMPLE:
                return "BitsPerSample";
            case TIFF.TAG_COMPRESSION:
                return "Compression";
            case TIFF.TAG_PHOTOMETRIC_INTERPRETATION:
                return "PhotometricInterpretation";
            case TIFF.TAG_FILL_ORDER:
                return "FillOrder";
            case TIFF.TAG_DOCUMENT_NAME:
                return "DocumentName";
            case TIFF.TAG_IMAGE_DESCRIPTION:
                return "ImageDescription";
            case TIFF.TAG_MAKE:
                return "Make";
            case TIFF.TAG_MODEL:
                return "Model";
            case TIFF.TAG_STRIP_OFFSETS:
                return "StripOffsets";
            case TIFF.TAG_ORIENTATION:
                return "Orientation";
            case TIFF.TAG_SAMPLES_PER_PIXEL:
                return "SamplesPerPixel";
            case TIFF.TAG_ROWS_PER_STRIP:
                return "RowsPerStrip";
            case TIFF.TAG_STRIP_BYTE_COUNTS:
                return "StripByteCounts";
            case TIFF.TAG_X_RESOLUTION:
                return "XResolution";
            case TIFF.TAG_Y_RESOLUTION:
                return "YResolution";
            case TIFF.TAG_PLANAR_CONFIGURATION:
                return "PlanarConfiguration";
            case TIFF.TAG_RESOLUTION_UNIT:
                return "ResolutionUnit";
            case TIFF.TAG_PAGE_NAME:
                return "PageName";
            case TIFF.TAG_PAGE_NUMBER:
                return "PageNumber";
            case TIFF.TAG_SOFTWARE:
                return "Software";
            case TIFF.TAG_DATE_TIME:
                return "DateTime";
            case TIFF.TAG_ARTIST:
                return "Artist";
            case TIFF.TAG_HOST_COMPUTER:
                return "HostComputer";
            case TIFF.TAG_PREDICTOR:
                return "Predictor";
            case TIFF.TAG_TILE_WIDTH:
                return "TileWidth";
            case TIFF.TAG_TILE_HEIGTH:
                return "TileHeight";
            case TIFF.TAG_TILE_OFFSETS:
                return "TileOffsets";
            case TIFF.TAG_TILE_BYTE_COUNTS:
                return "TileByteCounts";
            case TIFF.TAG_COPYRIGHT:
                return "Copyright";
            case TIFF.TAG_YCBCR_COEFFICIENTS:
                return "YCbCrCoefficients";
            case TIFF.TAG_YCBCR_SUB_SAMPLING:
                return "YCbCrSubSampling";
            case TIFF.TAG_YCBCR_POSITIONING:
                return "YCbCrPositioning";
            case TIFF.TAG_REFERENCE_BLACK_WHITE:
                return "ReferenceBlackWhite";
            case TIFF.TAG_COLOR_MAP:
                return "ColorMap";
            case TIFF.TAG_INK_SET:
                return "InkSet";
            case TIFF.TAG_INK_NAMES:
                return "InkNames";
            case TIFF.TAG_EXTRA_SAMPLES:
                return "ExtraSamples";
            case TIFF.TAG_SAMPLE_FORMAT:
                return "SampleFormat";
            case TIFF.TAG_JPEG_TABLES:
                return "JPEGTables";
            case TIFF.TAG_JPEG_INTERCHANGE_FORMAT:
                return "JPEGInterchangeFormat";
            case TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH:
                return "JPEGInterchangeFormatLength";

            case TIFF.TAG_SUB_IFD:
                return "SubIFD";
            case TIFF.TAG_SUBFILE_TYPE:
                return "SubfileType";

            case EXIF.TAG_EXPOSURE_TIME:
                return "ExposureTime";
            case EXIF.TAG_F_NUMBER:
                return "FNumber";
            case EXIF.TAG_EXPOSURE_PROGRAM:
                return "ExposureProgram";
            case EXIF.TAG_ISO_SPEED_RATINGS:
                return "ISOSpeedRatings";
            case EXIF.TAG_SHUTTER_SPEED_VALUE:
                return "ShutterSpeedValue";
            case EXIF.TAG_APERTURE_VALUE:
                return "ApertureValue";
            case EXIF.TAG_BRIGHTNESS_VALUE:
                return "BrightnessValue";
            case EXIF.TAG_EXPOSURE_BIAS_VALUE:
                return "ExposureBiasValue";
            case EXIF.TAG_MAX_APERTURE_VALUE:
                return "MaxApertureValue";
            case EXIF.TAG_SUBJECT_DISTANCE:
                return "SubjectDistance";
            case EXIF.TAG_METERING_MODE:
                return "MeteringMode";
            case EXIF.TAG_LIGHT_SOURCE:
                return "LightSource";
            case EXIF.TAG_FLASH:
                return "Flash";
            case EXIF.TAG_FOCAL_LENGTH:
                return "FocalLength";
            case EXIF.TAG_SENSING_METHOD:
                return "SensingMethod";
            case EXIF.TAG_FILE_SOURCE:
                return "FileSource";
            case EXIF.TAG_SCENE_TYPE:
                return "SceneType";
            case EXIF.TAG_CFA_PATTERN:
                return "CFAPattern";
            case EXIF.TAG_CUSTOM_RENDERED:
                return "CustomRendered";
            case EXIF.TAG_EXPOSURE_MODE:
                return "ExposureMode";
            case EXIF.TAG_WHITE_BALANCE:
                return "WhiteBalance";
            case EXIF.TAG_DIGITAL_ZOOM_RATIO:
                return "DigitalZoomRatio";
            case EXIF.TAG_FOCAL_LENGTH_IN_35_MM_FILM:
                return "FocalLengthIn35mmFilm";
            case EXIF.TAG_SCENE_CAPTURE_TYPE:
                return "SceneCaptureType";
            case EXIF.TAG_GAIN_CONTROL:
                return "GainControl";
            case EXIF.TAG_CONTRAST:
                return "Contrast";
            case EXIF.TAG_SATURATION:
                return "Saturation";
            case EXIF.TAG_SHARPNESS:
                return "Sharpness";
            case EXIF.TAG_IMAGE_UNIQUE_ID:
                return "ImageUniqueID";

            case EXIF.TAG_FLASHPIX_VERSION:
                return "FlashpixVersion";

            case EXIF.TAG_EXIF_VERSION:
                return "ExifVersion";
            case EXIF.TAG_DATE_TIME_ORIGINAL:
                return "DateTimeOriginal";
            case EXIF.TAG_DATE_TIME_DIGITIZED:
                return "DateTimeDigitized";
            case EXIF.TAG_IMAGE_NUMBER:
                return "ImageNumber";
            case EXIF.TAG_MAKER_NOTE:
                return "MakerNote";
            case EXIF.TAG_USER_COMMENT:
                return "UserComment";

            case EXIF.TAG_COMPONENTS_CONFIGURATION:
                return "ComponentsConfiguration";
            case EXIF.TAG_COMPRESSED_BITS_PER_PIXEL:
                return "CompressedBitsPerPixel";

            case EXIF.TAG_COLOR_SPACE:
                return "ColorSpace";
            case EXIF.TAG_PIXEL_X_DIMENSION:
                return "PixelXDimension";
            case EXIF.TAG_PIXEL_Y_DIMENSION:
                return "PixelYDimension";

            case EXIF.TAG_SPECTRAL_SENSITIVITY:
                return "SpectralSensitivity";
            case EXIF.TAG_OECF:
                return "OECF";
            case EXIF.TAG_SUBJECT_AREA:
                return "SubjectArea";
            case EXIF.TAG_SUBSEC_TIME:
                return "SubsecTime";
            case EXIF.TAG_SUBSEC_TIME_ORIGINAL:
                return "SubsecTimeOriginal";
            case EXIF.TAG_SUBSEC_TIME_DIGITIZED:
                return "SubsecTimeDigitized";
            case EXIF.TAG_RELATED_SOUND_FILE:
                return "RelatedSoundFile";
            case EXIF.TAG_FLASH_ENERGY:
                return "FlashEnergy";
            case EXIF.TAG_SPATIAL_FREQUENCY_RESPONSE:
                return "SpatialFrequencyResponse";
            case EXIF.TAG_FOCAL_PLANE_X_RESOLUTION:
                return "FocalPlaneXResolution";
            case EXIF.TAG_FOCAL_PLANE_Y_RESOLUTION:
                return "FocalPlaneYResolution";
            case EXIF.TAG_FOCAL_PLANE_RESOLUTION_UNIT:
                return "FocalPlaneResolutionUnit";
            case EXIF.TAG_SUBJECT_LOCATION:
                return "SubjectLocation";
            case EXIF.TAG_EXPOSURE_INDEX:
                return "ExposureIndex";
            case EXIF.TAG_DEVICE_SETTING_DESCRIPTION:
                return "DeviceSettingDescription";
            case EXIF.TAG_SUBJECT_DISTANCE_RANGE:
                return "SubjectDistanceRange";

            // EXIF 2.3 and later
            case EXIF.TAG_SENSITIVITY_TYPE:
                return "SensitivityType";
            case EXIF.TAG_STANDARD_OUTPUT_SENSITIVITY:
                return "StandardOutputSensitivity";
            case EXIF.TAG_RECOMMENDED_EXPOSURE_INDEX:
                return "RecommendedExposureIndex";
            case EXIF.TAG_ISO_SPEED:
                return "ISOSpeed";
            case EXIF.TAG_OFFSET_TIME:
                return "OffsetTime";
            case EXIF.TAG_OFFSET_TIME_ORIGINAL:
                return "OffsetTimeOriginal";
            case EXIF.TAG_OFFSET_TIME_DIGITIZED:
                return "OffsetTimeDigitized";
            case EXIF.TAG_TEMPERATURE:
                return "Temperature";
            case EXIF.TAG_HUMIDITY:
                return "Humidity";
            case EXIF.TAG_PRESSURE:
                return "Pressure";
            case EXIF.TAG_WATER_DEPTH:
                return "WaterDepth";
            case EXIF.TAG_ACCELERATION:
                return "Acceleration";
            case EXIF.TAG_CAMERA_ELEVATION_ANGLE:
                return "CameraElevationAngle";
            case EXIF.TAG_CAMERA_OWNER_NAME:
                return "CameraOwnerName";
            case EXIF.TAG_BODY_SERIAL_NUMBER:
                return "BodySerialNumber";
            case EXIF.TAG_LENS_SPECIFICATION:
                return "LensSpecification";
            case EXIF.TAG_LENS_MAKE:
                return "LensMake";
            case EXIF.TAG_LENS_MODEL:
                return "LensModel";
            case EXIF.TAG_LENS_SERIAL_NUMBER:
                return "LensSerialNumber";
            case EXIF.TAG_COMPOSITE_IMAGE:
                return "CompositeImage";
            case EXIF.TAG_GAMMA:
                return "Gamma";

            // NOTE: GPS tags are only valid within a GPS IFD, however, as the ids below
            // don't collide with any other tags known here, they can be safely named.
            // GPS tags 1 (GPSLatitudeRef) and 2 (GPSLatitude) are deliberately NOT named,
            // as their ids collide with the Interoperability IFD tags 1 and 2
            case GPS.TAG_GPS_VERSION_ID:
                return "GPSVersionID";
            case GPS.TAG_GPS_LONGITUDE_REF:
                return "GPSLongitudeRef";
            case GPS.TAG_GPS_LONGITUDE:
                return "GPSLongitude";
            case GPS.TAG_GPS_ALTITUDE_REF:
                return "GPSAltitudeRef";
            case GPS.TAG_GPS_ALTITUDE:
                return "GPSAltitude";
            case GPS.TAG_GPS_TIME_STAMP:
                return "GPSTimeStamp";
            case GPS.TAG_GPS_SATELLITES:
                return "GPSSatellites";
            case GPS.TAG_GPS_STATUS:
                return "GPSStatus";
            case GPS.TAG_GPS_MEASURE_MODE:
                return "GPSMeasureMode";
            case GPS.TAG_GPS_DOP:
                return "GPSDOP";
            case GPS.TAG_GPS_SPEED_REF:
                return "GPSSpeedRef";
            case GPS.TAG_GPS_SPEED:
                return "GPSSpeed";
            case GPS.TAG_GPS_TRACK_REF:
                return "GPSTrackRef";
            case GPS.TAG_GPS_TRACK:
                return "GPSTrack";
            case GPS.TAG_GPS_IMG_DIRECTION_REF:
                return "GPSImgDirectionRef";
            case GPS.TAG_GPS_IMG_DIRECTION:
                return "GPSImgDirection";
            case GPS.TAG_GPS_MAP_DATUM:
                return "GPSMapDatum";
            case GPS.TAG_GPS_DEST_LATITUDE_REF:
                return "GPSDestLatitudeRef";
            case GPS.TAG_GPS_DEST_LATITUDE:
                return "GPSDestLatitude";
            case GPS.TAG_GPS_DEST_LONGITUDE_REF:
                return "GPSDestLongitudeRef";
            case GPS.TAG_GPS_DEST_LONGITUDE:
                return "GPSDestLongitude";
            case GPS.TAG_GPS_DEST_BEARING_REF:
                return "GPSDestBearingRef";
            case GPS.TAG_GPS_DEST_BEARING:
                return "GPSDestBearing";
            case GPS.TAG_GPS_DEST_DISTANCE_REF:
                return "GPSDestDistanceRef";
            case GPS.TAG_GPS_DEST_DISTANCE:
                return "GPSDestDistance";
            case GPS.TAG_GPS_PROCESSING_METHOD:
                return "GPSProcessingMethod";
            case GPS.TAG_GPS_AREA_INFORMATION:
                return "GPSAreaInformation";
            case GPS.TAG_GPS_DATE_STAMP:
                return "GPSDateStamp";
            case GPS.TAG_GPS_DIFFERENTIAL:
                return "GPSDifferential";
            case GPS.TAG_GPS_H_POSITIONING_ERROR:
                return "GPSHPositioningError";

            // TODO: More field names
            /*
            default:
                Class[] classes = new Class[] {TIFF.class, EXIF.class};

                for (Class cl : classes) {
                    Field[] fields = cl.getFields();

                    for (Field field : fields) {
                        try {
                            if (field.getType() == Integer.TYPE && field.getName().startsWith("TAG_")) {
                                if (field.get(null).equals(getIdentifier())) {
                                    return StringUtil.lispToCamel(field.getName().substring(4).replace("_", "-").toLowerCase(), true);
                                }
                            }
                        }
                        catch (IllegalAccessException e) {
                            // Should never happen, but in case, abort
                            break;
                        }
                    }
                }
            */
        }

        return null;
    }

    @Override
    public String getTypeName() {
        return TIFF.TYPE_NAMES[type];
    }

    static short getType(final Entry entry) {
        // For internal entries use type directly
        if (entry instanceof TIFFEntry) {
            TIFFEntry tiffEntry = (TIFFEntry) entry;
            return tiffEntry.getType();
        }

        // For other entries, use name if it matches
        Validate.notNull(entry, "entry");
        String typeName = entry.getTypeName();

        if (typeName != null) {
            for (int i = 1; i < TIFF.TYPE_NAMES.length; i++) {
                if (typeName.equals(TIFF.TYPE_NAMES[i])) {
                    return (short) i;
                }
            }
        }

        // Otherwise, fall back to guessing based on value's type
        return guessType(entry.getValue());
    }

    private static short guessType(final Object entryValue) {
        // Guess type based on native Java type
        Object value = Validate.notNull(entryValue);

        boolean array = value.getClass().isArray();
        if (array) {
            value = Array.get(value, 0);
        }

        // Note: This "narrowing" is to keep data consistent between read/write.
        // TODO: Check for negative values and use signed types?
        if (value instanceof Byte) {
            return TIFF.TYPE_BYTE;
        }
        if (value instanceof Short) {
            if (!array && (Short) value < Byte.MAX_VALUE) {
                return TIFF.TYPE_BYTE;
            }

            return TIFF.TYPE_SHORT;
        }
        if (value instanceof Integer) {
            if (!array && (Integer) value < Short.MAX_VALUE) {
                return TIFF.TYPE_SHORT;
            }

            return TIFF.TYPE_LONG;
        }
        if (value instanceof Long) {
            if (!array && (Long) value < Integer.MAX_VALUE) {
                return TIFF.TYPE_LONG;
            }
        }

        if (value instanceof Rational) {
            return TIFF.TYPE_RATIONAL;
        }

        if (value instanceof String) {
            return TIFF.TYPE_ASCII;
        }

        // TODO: More types

        throw new UnsupportedOperationException(String.format("Method guessType not implemented for type %s", value.getClass()));
    }

    static long getValueLength(final int pType, final long pCount) {
        if (pType > 0 && pType < TIFF.TYPE_LENGTHS.length) {
            return TIFF.TYPE_LENGTHS[pType] * pCount;
        }

        return -1;
    }
}
