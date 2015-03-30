package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.Validate;

import java.awt.color.ICC_Profile;
import java.util.Arrays;

/**
 * KCMSProfileCleaner.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: KCMSProfileCleaner.java,v 1.0 06/01/15 harald.kuhr Exp$
 */
final class KCMSSanitizerStrategy implements ICCProfileSanitizer {

    /** Value used instead of 'XYZ ' in problematic Corbis RGB Profiles */
    private static final byte[] CORBIS_RGB_ALTERNATE_XYZ = new byte[] {0x17, (byte) 0xA5, 0x05, (byte) 0xB8};

    public void fixProfile(final ICC_Profile profile, byte[] profileHeader) {
        Validate.notNull(profile, "profile");

        if (profileHeader != null) {
            profile.setData(ICC_Profile.icSigHead, profileHeader);
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
        if (data != null && Arrays.equals(Arrays.copyOfRange(data, 0, 4), CORBIS_RGB_ALTERNATE_XYZ)) {
            data[0] = 'X';
            data[1] = 'Y';
            data[2] = 'Z';
            data[3] = ' ';

            profile.setData(tagSignature, data);

            return true;
        }

        return false;
    }

}
