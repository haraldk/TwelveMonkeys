/*
 * Copyright (c) 2011, Harald Kuhr
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
import com.twelvemonkeys.util.LRUHashMap;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;

import static com.twelvemonkeys.imageio.color.ColorProfiles.*;

/**
 * A helper class for working with ICC color profiles and color spaces.
 * <p>
 * Standard ICC color profiles are read from system-specific locations
 * for known operating systems.
 * </p>
 * <p>
 * Color profiles may be configured by placing a property-file
 * {@code com/twelvemonkeys/imageio/color/icc_profiles.properties}
 * on the classpath, specifying the full path to the profiles.
 * ICC color profiles are probably already present on your system, or
 * can be downloaded from
 * <a href="http://www.color.org/profiles2.xalter">ICC</a>,
 * <a href="http://www.adobe.com/downloads/">Adobe</a> or other places.
 *  * </p>
 * <p>
 * Example property file:
 * </p>
 * <pre>
 * # icc_profiles.properties
 * ADOBE_RGB_1998=/path/to/Adobe RGB 1998.icc
 * GENERIC_CMYK=/path/to/Generic CMYK.icc
 * </pre>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ColorSpaces.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class ColorSpaces {
    // TODO: Consider creating our own ICC profile class, which just wraps the byte array,
    // for easier access and manipulation until creating a "real" ICC_Profile/ColorSpace.
    // This will also let us work around the issues in the LCMS implementation.

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.color.debug"));

    // NOTE: java.awt.color.ColorSpace.CS_* uses 1000-1004, we'll use 5000+ to not interfere with future additions

    /** The Adobe RGB 1998 (or compatible) color space. Either read from disk or built-in. */
    @SuppressWarnings("WeakerAccess")
    public static final int CS_ADOBE_RGB_1998 = 5000;

    /** A best-effort "generic" CMYK color space. Either read from disk or built-in. */
    @SuppressWarnings("WeakerAccess")
    public static final int CS_GENERIC_CMYK = 5001;

    // TODO: Move to ColorProfiles OR cache ICC_ColorSpace instead?
    // Weak references to hold the color spaces while cached
    private static WeakReference<ICC_Profile> adobeRGB1998 = new WeakReference<>(null);
    private static WeakReference<ICC_Profile> genericCMYK = new WeakReference<>(null);

    // Cache for the latest used color spaces
    private static final Map<Key, ICC_ColorSpace> cache = new LRUHashMap<>(16);

    static {
        // In case we didn't activate through SPI already
        ProfileDeferralActivator.activateProfiles();
    }

    private ColorSpaces() {}

    /**
     * Creates an ICC color space from the given ICC color profile.
     * <p>
     * For standard Java color spaces, the built-in instance is returned.
     * Otherwise, color spaces are looked up from cache and created on demand.
     * </p>
     *
     * @param profile the ICC color profile. May not be {@code null}.
     * @return an ICC color space
     * @throws IllegalArgumentException if {@code profile} is {@code null}.
     * @throws java.awt.color.CMMException if {@code profile} is invalid.
     */
    public static ICC_ColorSpace createColorSpace(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        // Fix profile before lookup/create
        fixProfile(profile);

        byte[] profileHeader = getProfileHeaderWithProfileId(profile);

        ICC_ColorSpace cs = getInternalCS(profile.getColorSpaceType(), profileHeader);
        if (cs != null) {
            return cs;
        }

        return getCachedOrCreateCS(profile, profileHeader);
    }

    static ICC_ColorSpace getInternalCS(final int profileCSType, final byte[] profileHeader) {
        if (profileCSType == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, ColorProfiles.sRGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        else if (profileCSType == ColorSpace.TYPE_GRAY && Arrays.equals(profileHeader, ColorProfiles.GRAY.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }
        else if (profileCSType == ColorSpace.TYPE_3CLR && Arrays.equals(profileHeader, ColorProfiles.PYCC.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_PYCC);
        }
        else if (profileCSType == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, ColorProfiles.LINEAR_RGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        }
        else if (profileCSType == ColorSpace.TYPE_XYZ && Arrays.equals(profileHeader, ColorProfiles.CIEXYZ.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        }

        return null;
    }

    private static ICC_ColorSpace getCachedOrCreateCS(final ICC_Profile profile, final byte[] profileHeader) {
        Key key = new Key(profileHeader);

        synchronized (cache) {
            ICC_ColorSpace cs = getCachedCS(key);

            if (cs == null) {
                cs = new ICC_ColorSpace(profile);

                validateColorSpace(cs);

                cache.put(key, cs);

                // On LCMS, validation *alters* the profile header, need to re-generate key
                if (ColorProfiles.validationAltersProfileHeader()) {
                    cache.put(new Key(getProfileHeaderWithProfileId(cs.getProfile())), cs);
                }
            }

            return cs;
        }
    }

    private static ICC_ColorSpace getCachedCS(Key profileKey) {
        synchronized (cache) {
            return cache.get(profileKey);
        }
    }

    static ICC_ColorSpace getCachedCS(final byte[] profileHeader) {
        return getCachedCS(new Key(profileHeader));
    }

    static void validateColorSpace(final ICC_ColorSpace cs) {
        // Validate the color space, to avoid caching bad profiles/color spaces
        // Will throw IllegalArgumentException or CMMException if the profile is bad
        cs.fromRGB(new float[] {0.999f, 0.5f, 0.001f});

        // This breaks *sometimes* after validation of bad profiles,
        // we'll let it blow up early in this case
        cs.getProfile().getData();
    }

    /**
     * @deprecated Use {@link ColorProfiles#isCS_sRGB(ICC_Profile)} instead.
     */
    @Deprecated
    public static boolean isCS_sRGB(final ICC_Profile profile) {
        return ColorProfiles.isCS_sRGB(profile);
    }

    /**
     * @deprecated Use {@link ColorProfiles#isCS_GRAY(ICC_Profile)} instead.
     */
    @Deprecated
    public static boolean isCS_GRAY(final ICC_Profile profile) {
        return ColorProfiles.isCS_GRAY(profile);
    }

    /**
     * @deprecated Use {@link ColorProfiles#validateProfile(ICC_Profile)} instead.
     */
    @Deprecated
    public static ICC_Profile validateProfile(final ICC_Profile profile) {
        return ColorProfiles.validateProfile(profile);
    }

    /**
     * Returns the color space specified by the given color space constant.
     * <p>
     * For standard Java color spaces, the built-in instance is returned.
     * Otherwise, color spaces are looked up from cache and created on demand.
     * </p>
     *
     * @param colorSpace the color space constant.
     * @return the {@link ColorSpace} specified by the color space constant.
     * @throws IllegalArgumentException if {@code colorSpace} is not one of the defined color spaces ({@code CS_*}).
     * @see ColorSpace
     * @see ColorSpaces#CS_ADOBE_RGB_1998
     * @see ColorSpaces#CS_GENERIC_CMYK
     */
    public static ColorSpace getColorSpace(int colorSpace) {
        ICC_Profile profile;

        switch (colorSpace) {
            case CS_ADOBE_RGB_1998:
                synchronized (ColorSpaces.class) {
                    profile = adobeRGB1998.get();

                    if (profile == null) {
                        // Try to get system default or user-defined profile
                        profile = readProfileFromPath(Profiles.getPath("ADOBE_RGB_1998"));

                        if (profile == null) {
                            // Fall back to the bundled ClayRGB1998 public domain Adobe RGB 1998 compatible profile,
                            // which is identical for all practical purposes
                            profile = readProfileFromClasspathResource("/profiles/ClayRGB1998.icc");

                            if (profile == null) {
                                // Should never happen given we now bundle fallback profile...
                                throw new IllegalStateException("Could not read AdobeRGB1998 profile");
                            }
                        }

                        if (profile.getColorSpaceType() != ColorSpace.TYPE_RGB) {
                            throw new IllegalStateException("Configured AdobeRGB1998 profile is not TYPE_RGB");
                        }

                        adobeRGB1998 = new WeakReference<>(profile);
                    }
                }

                return createColorSpace(profile);

            case CS_GENERIC_CMYK:
                synchronized (ColorSpaces.class) {
                    profile = genericCMYK.get();

                    if (profile == null) {
                        // Try to get system default or user-defined profile
                        profile = readProfileFromPath(Profiles.getPath("GENERIC_CMYK"));

                        if (profile == null) {
                            if (DEBUG) {
                                System.out.println("Using fallback profile");
                            }

                            // Fall back to generic CMYK ColorSpace, which is *insanely slow* using ColorConvertOp... :-P
                            return CMYKColorSpace.getInstance();
                        }

                        if (profile.getColorSpaceType() != ColorSpace.TYPE_CMYK) {
                            throw new IllegalStateException("Configured Generic CMYK profile is not TYPE_CMYK");
                        }

                        genericCMYK = new WeakReference<>(profile);
                    }
                }

                return createColorSpace(profile);

            default:
                // Default cases for convenience
                return ColorSpace.getInstance(colorSpace);
        }
    }

    private static final class Key {
        private final byte[] data;

        Key(byte[] data) {
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

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        }
    }
}
