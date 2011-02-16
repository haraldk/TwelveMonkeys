/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  Neither the name "TwelveMonkeys" nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.LRUHashMap;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * A helper class for working with ICC color profiles and color spaces.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ColorSpaces.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class ColorSpaces {
    // NOTE: java.awt.color.ColorSpace.CS_* uses 1000-1004, we'll use 5000+ to not interfere with future additions

    /** The Adobe RGB 1998 (or compatible) color space. Either read from disk or built-in. */
    public static final int CS_ADOBE_RGB_1998 = 5000;

    /** A best-effort "generic" CMYK color space. Either read from disk or built-in. */
    public static final int CS_GENERIC_CMYK = 5001;

    private static final Map<Key, ICC_ColorSpace> cache = new LRUHashMap<Key, ICC_ColorSpace>(10);

    private ColorSpaces() {}

    /**
     * Creates an ICC color space from the given ICC color profile.
     * <p />
     * For standard Java color spaces, the built-in instance is returned.
     * Otherwise, color spaces are looked up from cache and created on demand.
     *
     * @param profile the ICC color profile. May not be {@code null}.
     * @return an ICC color space
     * @throws IllegalArgumentException if {@code profile} is {@code null}
     */
    public static ICC_ColorSpace createColorSpace(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        byte[] profileHeader = profile.getData(ICC_Profile.icSigHead);

        ICC_ColorSpace cs = getInternalCS(profile, profileHeader);
        if (cs != null) {
            return cs;
        }

        // Special case for color profiles with rendering intent != 0, see isOffendingColorProfile method
        // NOTE: Rendering intent is really a 4 byte value, but legal values are 0-3 (ICC1v42_2006_05_1.pdf, 7.2.15, p. 19)
        if (profileHeader[ICC_Profile.icHdrRenderingIntent] != 0) {
            profileHeader[ICC_Profile.icHdrRenderingIntent] = 0;

            // Test again if this is an internal CS
            cs = getInternalCS(profile, profileHeader);
            if (cs != null) {
                return cs;
            }

            // Fix profile
            profile.setData(ICC_Profile.icSigHead, profileHeader);
        }

        return getCachedCS(profile, profileHeader);
    }

    private static ICC_ColorSpace getInternalCS(final ICC_Profile profile, final byte[] profileHeader) {
        if (profile.getColorSpaceType() == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, sRGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        if (profile.getColorSpaceType() == ColorSpace.TYPE_GRAY && Arrays.equals(profileHeader, GRAY.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }
        if (profile.getColorSpaceType() == ColorSpace.TYPE_3CLR && Arrays.equals(profileHeader, PYCC.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_PYCC);
        }
        if (profile.getColorSpaceType() == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, LINEAR_RGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        }
        if (profile.getColorSpaceType() == ColorSpace.TYPE_XYZ && Arrays.equals(profileHeader, CIEXYZ.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        }

        return null;
    }

    private static ICC_ColorSpace getCachedCS(final ICC_Profile profile, final byte[] profileHeader) {
        Key key = new Key(profileHeader);

        synchronized (cache) {
            ICC_ColorSpace cs = cache.get(key);

            if (cs == null) {
                cs = new ICC_ColorSpace(profile);
                cache.put(key, cs);
            }

            return cs;
        }
    }

    /**
     * Tests whether an ICC color profile is known to cause problems for {@link java.awt.image.ColorConvertOp}.
     * <p />
     * <em>
     * Note that this method only tests if a color conversion using this profile is known to fail.
     * There's no guarantee that the color conversion will succeed even if this method returns {@code false}.
     * </em>
     *
     * @param profile the ICC color profile. May not be {@code null}.
     * @return {@code true} if known to be offending, {@code false} otherwise
     * @throws IllegalArgumentException if {@code profile} is {@code null}
     */
    public static boolean isOffendingColorProfile(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        // NOTE:
        // Several embedded ICC color profiles are non-compliant with Java and throws CMMException
        // The problem with these embedded ICC profiles seems to be the rendering intent
        // being 1 (01000000) - "Media Relative Colormetric" in the offending profiles,
        // and 0 (00000000) - "Perceptual" in the good profiles
        // (that is 1 single bit of difference right there.. ;-)

        // This is particularly annoying, as the byte copying isn't really necessary,
        // except the getRenderingIntent method is package protected in java.awt.color
        byte[] data = profile.getData(ICC_Profile.icSigHead);
        return data[ICC_Profile.icHdrRenderingIntent] != 0;
    }

    // TODO: Use internal cache (needs mapping between ID and Key...)
    // TODO: Allow system-property/config file on class path to configure location of color profiles
    // TODO: Document how to download, install and configure Adobe color profiles or other profiles

    /**
     * Returns the color space specified by the given color space constant.
     * <p />
     * For standard Java color spaces, the built-in instance is returned.
     * Otherwise, color spaces are looked up from cache and created on demand.
     *
     * @param colorSpace the color space constant.
     * @return the {@link ColorSpace} specified by the color space constant.
     * @throws IllegalArgumentException if {@code colorSpace} is not one of the defined color spaces ({@code CS_*}).
     * @see ColorSpace
     * @see ColorSpaces#CS_ADOBE_RGB_1998
     * @see ColorSpaces#CS_GENERIC_CMYK
     */
    public static ColorSpace getColorSpace(int colorSpace) {
        switch (colorSpace) {
            // Default cases for convenience
            case ColorSpace.CS_sRGB:
            case ColorSpace.CS_GRAY:
            case ColorSpace.CS_PYCC:
            case ColorSpace.CS_CIEXYZ:
            case ColorSpace.CS_LINEAR_RGB:
                return ColorSpace.getInstance(colorSpace);

            case CS_ADOBE_RGB_1998:
                // TODO: Read profile specified by config file instead of hard coded
                try {
                    // This works for OS X only
                    return createColorSpace(ICC_Profile.getInstance("/System/Library/ColorSync/Profiles/AdobeRGB1998.icc"));
                }
                catch (IOException ignore) {
                }

                // Fall back to the bundled ClayRGB1998 public domain Adobe RGB 1998 compatible profile,
                // identical for all practical purposes
                InputStream stream = ColorSpaces.class.getResourceAsStream("/profiles/ClayRGB1998.icc");
                try {
                    return createColorSpace(ICC_Profile.getInstance(stream));
                }
                catch (IOException ignore) {
                }
                finally {
                    FileUtil.close(stream);
                }

                // Should never happen given we now bundle the profile...
                throw new RuntimeException("Could not read AdobeRGB1998 profile");

            case CS_GENERIC_CMYK:
                // TODO: Read profile specified by config file instead of hard coded
                // TODO: C:\Windows\System32\spool\drivers\color\RSWOP.icm for Windows Vista?
                try {
                    // This works for OS X only
//                    return createColorSpace(ICC_Profile.getInstance("/C:/Windows/System32/spool/drivers/color/RSWOP.icm"));
                    return createColorSpace(ICC_Profile.getInstance("/System/Library/ColorSync/Profiles/Generic CMYK Profile.icc"));
//                    return createColorSpace(ICC_Profile.getInstance("/Downloads/coated_FOGRA39L_argl.icc"));
//                    return createColorSpace(ICC_Profile.getInstance("/Downloads/RSWOP.icm"));
//                    return createColorSpace(ICC_Profile.getInstance("/Downloads/USWebCoatedSWOP.icc"));
                }
                catch (IOException ignore) {
                }
                
                // Fall back to generic CMYK ColorSpace, which is *insanely slow* using ColorConvertOp... :-P
                return CMYKColorSpace.getInstance();
            default:

            // TODO: Allow more customizable models based on the config file?
        }

        throw new IllegalArgumentException(String.format("Unsupported color space: %s", colorSpace));
    }

    private static final class Key {
        private final byte[] data;

        public Key(byte[] data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key && Arrays.equals(data, ((Key) other).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

    // Cache header profile data to avoid excessive array creation/copying in static inner class for on-demand lazy init
    private static class sRGB {
        private static final byte[] header = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData(ICC_Profile.icSigHead);
    }
    private static class CIEXYZ {
        private static final byte[] header = ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ).getData(ICC_Profile.icSigHead);
    }
    private static class PYCC {
        private static final byte[] header = ICC_Profile.getInstance(ColorSpace.CS_PYCC).getData(ICC_Profile.icSigHead);
    }
    private static class GRAY {
        private static final byte[] header = ICC_Profile.getInstance(ColorSpace.CS_GRAY).getData(ICC_Profile.icSigHead);
    }
    private static class LINEAR_RGB {
        private static final byte[] header = ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB).getData(ICC_Profile.icSigHead);
    }
}
