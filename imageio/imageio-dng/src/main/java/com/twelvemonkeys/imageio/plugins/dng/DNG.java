package com.twelvemonkeys.imageio.plugins.dng;

/**
 * DNG
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DNG.java,v 1.0 03.10.14 10:49 haraldk Exp$
 */
interface DNG {
    // TODO: Some (all?) of these tags are defined by TIFF/EP, should we reflect that in package/class names?

    /** CFA (Color Filter Array). */
    int PHOTOMETRIC_CFA = 32803;
    /** LinearRaw. */
    int PHOTOMETRIC_LINEAR_RAW = 34892;

    /**
     * Lossy JPEG.
     * <p/>
     * Lossy JPEG (34892) is allowed for IFDs that use PhotometricInterpretation = 34892
     * (LinearRaw) and 8-bit integer data. This new compression code is required to let the DNG
     * reader know to use a lossy JPEG decoder rather than a lossless JPEG decoder for this
     * combination of PhotometricInterpretation and BitsPerSample.
     */
    int COMPRESSION_LOSSY_JPEG = 34892;

    /**
     * CFARepeatPatternDim
     * <p/>
     * This tag encodes the number of pixels horizontally and vertically that are needed to uniquely define the repeat
     * pattern of the color filter array (CFA) pattern used in the color image sensor. It is mandatory when
     * PhotometricInterpretation = 32803, and there are no defaults allowed. It is optional when
     * PhotometricInterpretation = 2 or 6 and SensingMethod = 2, where it can be used to indicate the original sensor
     * sampling positions.
     */
    int TAG_CFA_REPEAT_PATTERN_DIM = 33421;
    /**
     * Indicates the color filter array (CFA) geometric pattern of the image sensor
     * when a one-chip color area sensor is used.
     * NOTE: This tag (defined in TIFF/EP) is different from the CFAPattern defined in normal TIFF.
     */
    int TAG_CFA_PATTERN = 33422;

    /** Indicates the image sensor type on the camera or input device. */
    int TAG_SENSING_METHOD = 37399;

    // From http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif/cfapattern.html
    byte CFA_PATTERN_RED = 0;
    byte CFA_PATTERN_GREEN = 1;
    byte CFA_PATTERN_BLUE = 2;
    byte CFA_PATTERN_CYAN = 3;
    byte CFA_PATTERN_MAGENTA = 4;
    byte CFA_PATTERN_YELLOW = 5;
    byte CFA_PATTERN_WHITE = 6; // ???? Should be KEY?

    /**
     * This tag encodes the DNG four-tier version number. For files compliant with this version of
     * the DNG specification (1.4.0.0), this tag should contain the bytes: 1, 4, 0, 0.
     */
    int TAG_DNG_VERSION = 50706;
    int TAG_DNG_BACKWARD_VERSION = 50707;

    /** UniqueCameraModel defines a unique, non-localized name for the camera model that created the image in the raw file. */
    int TAG_UNIQUE_CAMERA_MODEL = 50708;
    int TAG_LOCALIZED_CAMERA_MODEL = 50709;

    /** CFA plane to RGB mapping (default: [0, 1, 2]). */
    int TAG_CFA_PLANE_COLOR = 50710;
    /** CFA spatial layout (default: 1). */
    int TAG_CFA_LAYOUT = 50711;

    /** 1 = Rectangular (or square) layout. */
    int CFA_LAYOUT_RECTANGULAR = 1;
    /** 2 = Staggered layout A: even columns are offset down by 1/2 row. */
    int CFA_LAYOUT_STAGGERED_A = 2;
    /** 3 = Staggered layout B: even columns are offset up by 1/2 row. */
    int CFA_LAYOUT_STAGGERED_B = 3;
    /** 4 = Staggered layout C: even rows are offset right by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_C = 4;
    /** 5 = Staggered layout D: even rows are offset left by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_D = 5;
    /** 6 = Staggered layout E: even rows are offset up by 1/2 row, even columns are offset left by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_E = 6;
    /** 7 = Staggered layout F: even rows are offset up by 1/2 row, even columns are offset right by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_F = 7;
    /** 8 = Staggered layout G: even rows are offset down by 1/2 row, even columns are offset left by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_G = 8;
    /** 9 = Staggered layout H: even rows are offset down by 1/2 row, even columns are offset right by 1/2 column. */
    int CFA_LAYOUT_STAGGERED_H = 9;

    /** LinearizationTable describes a lookup table that maps stored values into linear values. */
    int TAG_LINEARIZATION_TABLE = 50712;

    /** This tag specifies repeat pattern size for the BlackLevel tag. Default: [1, 1]. */
    int TAG_BLACK_LEVEL_REPEAT_DIM = 50713;

    /**
     * This tag specifies the zero light (a.k.a. thermal black or black current) encoding level,
     * as a repeating pattern. Default: 0.
     */
    int TAG_BLACK_LEVEL = 50714;

    // TODO: Rest of DNG tags.
    // ...

    /**
     * Horizontal Difference X2.
     * Same as Horizontal Difference except the pixel two to the left is used rather than the pixel one to the left.
     */
    int PREDICTOR_HORIZONTAL_X2 = 34892;
    /**
     * Horizontal Difference X4.
     * Same as Horizontal Difference except the pixel four to the left is used rather than the pixel one to the left.
     */
    int PREDICTOR_HORIZONTAL_X4 = 34893;
    /**
     * Floating Point X2.
     * Same as Floating Point except the pixel two to the left is used rather than the pixel one to the left.
     */
    int PREDICTOR_FLOATINGPOINT_X2 = 34894;
    /**
     * Floating Point X4.
     * Same as Floating Point except the pixel four to the left is used rather than the pixel one to the left
     */
    int PREDICTOR_FLOATINGPOINT_X4 = 34895;
}
