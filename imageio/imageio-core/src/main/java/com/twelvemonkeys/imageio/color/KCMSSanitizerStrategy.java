package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.Validate;

import java.awt.color.ICC_Profile;

/**
 * KCMSProfileCleaner.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: KCMSProfileCleaner.java,v 1.0 06/01/15 harald.kuhr Exp$
 */
final class KCMSSanitizerStrategy implements ICCProfileSanitizer {

    /** Value used instead of 'XYZ ' in problematic Corbis RGB Profiles */
    private static final int CORBIS_RGB_ALTERNATE_XYZ = 0x17 << 24 | 0xA5 << 16 | 0x05 << 8 | 0xB8;

    public void fixProfile(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        // Special case for color profiles with rendering intent != 0, see ColorSpaces.isOffendingColorProfile method
        // NOTE: Rendering intent is a 4 byte value, legal values are 0-3 (ICC1v42_2006_05_1.pdf, 7.2.15, p. 19)
        byte[] header = profile.getData(ICC_Profile.icSigHead);
        if (intFromBigEndian(header, ICC_Profile.icHdrRenderingIntent) != ICC_Profile.icPerceptual) {
            intToBigEndian(ICC_Profile.icPerceptual, header, ICC_Profile.icHdrRenderingIntent);
            profile.setData(ICC_Profile.icSigHead, header);
        }

        // Special handling to detect problematic Corbis RGB ICC Profile for KCMS.
        // This makes sure tags that are expected to be of type 'XYZ ' really have this expected type.
        // Should leave other ICC profiles unchanged.
        if (fixProfileXYZTag(profile, ICC_Profile.icSigMediaWhitePointTag)) {
            fixProfileXYZTag(profile, ICC_Profile.icSigRedColorantTag);
            fixProfileXYZTag(profile, ICC_Profile.icSigGreenColorantTag);
            fixProfileXYZTag(profile, ICC_Profile.icSigBlueColorantTag);
        }
    }

    /**
     * Fixes problematic 'XYZ ' tags in Corbis RGB profile.
     *
     * @return {@code true} if found and fixed, otherwise {@code false} for short-circuiting
     * to avoid unnecessary array copying.
     */
    private static boolean fixProfileXYZTag(final ICC_Profile profile, final int tagSignature) {
        byte[] data = profile.getData(tagSignature);

        // The CMM expects 0x64 65 73 63 ('XYZ ') but is 0x17 A5 05 B8..?
        if (data != null && intFromBigEndian(data, 0) == CORBIS_RGB_ALTERNATE_XYZ) {
            intToBigEndian(ICC_Profile.icSigXYZData, data, 0);
            profile.setData(tagSignature, data);

            return true;
        }

        return false;
    }

    // TODO: Move to some common util
    static int intFromBigEndian(final byte[] array, final int index) {
        return ((array[index     ] & 0xff) << 24) |
                ((array[index + 1] & 0xff) << 16) |
                ((array[index + 2] & 0xff) <<  8) |
                ((array[index + 3] & 0xff)      );
    }

    // TODO: Move to some common util
    static void intToBigEndian(final int value, final byte[] array, final int index) {
        array[index    ] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >>  8);
        array[index + 3] = (byte) (value      );
    }
}
