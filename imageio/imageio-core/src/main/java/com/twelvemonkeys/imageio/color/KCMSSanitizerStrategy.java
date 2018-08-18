/*
 * Copyright (c) 2015, Harald Kuhr
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

    @Override
    public boolean validationAltersProfileHeader() {
        return false;
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
    private static int intFromBigEndian(final byte[] array, final int index) {
        return ((array[index     ] & 0xff) << 24) |
                ((array[index + 1] & 0xff) << 16) |
                ((array[index + 2] & 0xff) <<  8) |
                ((array[index + 3] & 0xff)      );
    }

    // TODO: Move to some common util
    private static void intToBigEndian(final int value, final byte[] array, final int index) {
        array[index    ] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >>  8);
        array[index + 3] = (byte) (value      );
    }
}
