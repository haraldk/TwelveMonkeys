package com.twelvemonkeys.contrib.exif;

import com.twelvemonkeys.contrib.tiff.TIFFUtilities;

/**
 * Orientation.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version : Orientation.java,v 1.0 10/07/2020 harald.kuhr
 */
public enum Orientation {
    Normal(TIFFUtilities.TIFFBaseline.ORIENTATION_TOPLEFT),
    FlipH(TIFFUtilities.TIFFExtension.ORIENTATION_TOPRIGHT),
    Rotate180(TIFFUtilities.TIFFExtension.ORIENTATION_BOTRIGHT),
    FlipV(TIFFUtilities.TIFFExtension.ORIENTATION_BOTLEFT),
    FlipVRotate90(TIFFUtilities.TIFFExtension.ORIENTATION_LEFTTOP),
    Rotate270(TIFFUtilities.TIFFExtension.ORIENTATION_RIGHTTOP),
    FlipHRotate90(TIFFUtilities.TIFFExtension.ORIENTATION_RIGHTBOT),
    Rotate90(TIFFUtilities.TIFFExtension.ORIENTATION_LEFTBOT);

                             // name as defined in javax.imageio metadata
    private final int value; // value as defined in TIFF spec

    Orientation(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Orientation fromMetadataOrientation(final String orientationName) {
        if (orientationName != null) {
            try {
                return valueOf(orientationName);
            }
            catch (IllegalArgumentException e) {
                // Not found, try ignore case match, as some metadata implementations are known to return "normal" etc.
                String lowerCaseName = orientationName.toLowerCase();

                for (Orientation orientation : values()) {
                    if (orientation.name().toLowerCase().equals(lowerCaseName)) {
                        return orientation;
                    }
                }
            }
        }

        // Metadata does not have other orientations, default to Normal
        return Normal;
    }

    public static Orientation fromTIFFOrientation(final int tiffOrientation) {
        for (Orientation orientation : values()) {
            if (orientation.value() == tiffOrientation) {
                return orientation;
            }
        }

        // No other TIFF orientations possible, default to Normal
        return Normal;
    }
}
