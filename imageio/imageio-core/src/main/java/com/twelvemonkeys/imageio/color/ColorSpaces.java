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

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.Platform;
import com.twelvemonkeys.lang.SystemUtil;
import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.LRUHashMap;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * A helper class for working with ICC color profiles and color spaces.
 * <p />
 * Standard ICC color profiles are read from system-specific locations
 * for known operating systems.
 * <p />
 * Color profiles may be configured by placing a property-file
 * {@code com/twelvemonkeys/imageio/color/icc_profiles.properties}
 * on the classpath, specifying the full path to the profiles.
 * ICC color profiles are probably already present on your system, or
 * can be downloaded from
 * <a href="http://www.color.org/profiles2.xalter">ICC</a>,
 * <a href="http://www.adobe.com/downloads/">Adobe</a> or other places.
 * <p />
 * Example property file:
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

    /** We need special ICC profile handling for KCMS vs LCMS. Delegate to specific strategy. */
    private final static ICCProfileSanitizer profileCleaner = ICCProfileSanitizer.Factory.get();

    // NOTE: java.awt.color.ColorSpace.CS_* uses 1000-1004, we'll use 5000+ to not interfere with future additions

    /** The Adobe RGB 1998 (or compatible) color space. Either read from disk or built-in. */
    @SuppressWarnings("WeakerAccess")
    public static final int CS_ADOBE_RGB_1998 = 5000;

    /** A best-effort "generic" CMYK color space. Either read from disk or built-in. */
    @SuppressWarnings("WeakerAccess")
    public static final int CS_GENERIC_CMYK = 5001;

    // Weak references to hold the color spaces while cached
    private static WeakReference<ICC_Profile> adobeRGB1998 = new WeakReference<>(null);
    private static WeakReference<ICC_Profile> genericCMYK = new WeakReference<>(null);

    // Cache for the latest used color spaces
    private static final Map<Key, ICC_ColorSpace> cache = new LRUHashMap<>(10);

    static {
        try {
            // Force invocation of ProfileDeferralMgr.activateProfiles() to avoid JDK-6986863
            ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        }
        catch (Throwable disasters) {
            System.err.println("ICC Color Profile not properly activated due to the exception below.");
            System.err.println("Expect to see JDK-6986863 in action, and consider filing a bug report to your JRE provider.");

            disasters.printStackTrace();
        }
    }

    private ColorSpaces() {}

    /**
     * Creates an ICC color space from the given ICC color profile.
     * <p />
     * For standard Java color spaces, the built-in instance is returned.
     * Otherwise, color spaces are looked up from cache and created on demand.
     *
     * @param profile the ICC color profile. May not be {@code null}.
     * @return an ICC color space
     * @throws IllegalArgumentException if {@code profile} is {@code null}.
     * @throws java.awt.color.CMMException if {@code profile} is invalid.
     */
    public static ICC_ColorSpace createColorSpace(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        // Fix profile before lookup/create
        profileCleaner.fixProfile(profile);

        byte[] profileHeader = getProfileHeaderWithProfileId(profile);

        ICC_ColorSpace cs = getInternalCS(profile.getColorSpaceType(), profileHeader);
        if (cs != null) {
            return cs;
        }

        return getCachedOrCreateCS(profile, profileHeader);
    }

    private static byte[] getProfileHeaderWithProfileId(final ICC_Profile profile) {
        // Get *entire profile data*... :-/
        byte[] data = profile.getData();

        // Clear out preferred CMM, platform & creator, as these does not affect the profile in any way
        // - LCMS updates CMM + creator to "lcms" and platform to current platform
        // - KCMS keeps the values in the file...
        Arrays.fill(data, ICC_Profile.icHdrCmmId, ICC_Profile.icHdrCmmId + 4, (byte) 0);
        Arrays.fill(data, ICC_Profile.icHdrPlatform, ICC_Profile.icHdrPlatform + 4, (byte) 0);
        // + Clear out rendering intent, as this may be updated by application
        Arrays.fill(data, ICC_Profile.icHdrRenderingIntent, ICC_Profile.icHdrRenderingIntent + 4, (byte) 0);
        Arrays.fill(data, ICC_Profile.icHdrCreator, ICC_Profile.icHdrCreator + 4, (byte) 0);

        // Clear out any existing MD5, as it is no longer correct
        Arrays.fill(data, ICC_Profile.icHdrProfileID, ICC_Profile.icHdrProfileID + 16, (byte) 0);

        // Generate new MD5 and store in header
        byte[] md5 = computeMD5(data);
        System.arraycopy(md5, 0, data, ICC_Profile.icHdrProfileID, md5.length);

        // ICC profile header is the first 128 bytes
        return Arrays.copyOf(data, 128);
    }

    private static byte[] computeMD5(byte[] data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing MD5 MessageDigest");
        }
    }

    private static ICC_ColorSpace getInternalCS(final int profileCSType, final byte[] profileHeader) {
        if (profileCSType == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, sRGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        else if (profileCSType == ColorSpace.TYPE_GRAY && Arrays.equals(profileHeader, GRAY.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }
        else if (profileCSType == ColorSpace.TYPE_3CLR && Arrays.equals(profileHeader, PYCC.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_PYCC);
        }
        else if (profileCSType == ColorSpace.TYPE_RGB && Arrays.equals(profileHeader, LINEAR_RGB.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        }
        else if (profileCSType == ColorSpace.TYPE_XYZ && Arrays.equals(profileHeader, CIEXYZ.header)) {
            return (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        }

        return null;
    }

    private static ICC_ColorSpace getCachedOrCreateCS(final ICC_Profile profile, final byte[] profileHeader) {
        Key key = new Key(profileHeader);

        synchronized (cache) {
            ICC_ColorSpace cs = cache.get(key);

            if (cs == null) {
                cs = new ICC_ColorSpace(profile);

                validateColorSpace(cs);

                // On LCMS, validation *alters* the profile header, need to re-generate key
                key = profileCleaner.validationAltersProfileHeader()
                      ? new Key(getProfileHeaderWithProfileId(cs.getProfile()))
                      : key;

                cache.put(key, cs);
            }

            return cs;
        }
    }

    private static void validateColorSpace(ICC_ColorSpace cs) {
        // Validate the color space, to avoid caching bad color spaces
        // Will throw IllegalArgumentException or CMMException if the profile is bad
        cs.fromRGB(new float[] {1f, 0f, 0f});
    }

    /**
     * Tests whether an ICC color profile is equal to the default sRGB profile.
     *
     * @param profile the ICC profile to test. May not be {@code null}.
     * @return {@code true} if {@code profile} is equal to the default sRGB profile.
     * @throws IllegalArgumentException if {@code profile} is {@code null}
     *
     * @see java.awt.color.ColorSpace#isCS_sRGB()
     */
    public static boolean isCS_sRGB(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        return profile.getColorSpaceType() == ColorSpace.TYPE_RGB && Arrays.equals(getProfileHeaderWithProfileId(profile), sRGB.header);
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
    static boolean isOffendingColorProfile(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");

        // NOTE:
        // Several embedded ICC color profiles are non-compliant with Java pre JDK7 and throws CMMException
        // The problem with these embedded ICC profiles seems to be the rendering intent
        // being 1 (01000000) - "Media Relative Colormetric" in the offending profiles,
        // and 0 (00000000) - "Perceptual" in the good profiles
        // (that is 1 single bit of difference right there.. ;-)
        // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7064516

        // This is particularly annoying, as the byte copying isn't really necessary,
        // except the getRenderingIntent method is package protected in java.awt.color
        byte[] header = profile.getData(ICC_Profile.icSigHead);

        return header[ICC_Profile.icHdrRenderingIntent] != 0 || header[ICC_Profile.icHdrRenderingIntent + 1] != 0
                || header[ICC_Profile.icHdrRenderingIntent + 2] != 0 || header[ICC_Profile.icHdrRenderingIntent + 3] > 3;
    }

    /**
     * Tests whether an ICC color profile is valid.
     * Invalid profiles are known to cause problems for {@link java.awt.image.ColorConvertOp}.
     * <p />
     * <em>
     * Note that this method only tests if a color conversion using this profile is known to fail.
     * There's no guarantee that the color conversion will succeed even if this method returns {@code false}.
     * </em>
     *
     * @param profile the ICC color profile. May not be {@code null}.
     * @return {@code profile} if valid.
     * @throws IllegalArgumentException if {@code profile} is {@code null}
     * @throws java.awt.color.CMMException if {@code profile} is invalid.
     */
    public static ICC_Profile validateProfile(final ICC_Profile profile) {
        // Fix profile before validation
        profileCleaner.fixProfile(profile);
        validateColorSpace(new ICC_ColorSpace(profile));

        return profile;
    }

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

    @SuppressWarnings("SameParameterValue")
    private static ICC_Profile readProfileFromClasspathResource(final String profilePath) {
        InputStream stream = ColorSpaces.class.getResourceAsStream(profilePath);

        if (stream != null) {
            if (DEBUG) {
                System.out.println("Loading profile from classpath resource: " + profilePath);
            }

            try {
                return ICC_Profile.getInstance(stream);
            }
            catch (IOException ignore) {
                if (DEBUG) {
                    ignore.printStackTrace();
                }
            }
            finally {
                FileUtil.close(stream);
            }
        }

        return null;
    }

    private static ICC_Profile readProfileFromPath(final String profilePath) {
        if (profilePath != null) {
            if (DEBUG) {
                System.out.println("Loading profile from: " + profilePath);
            }

            try {
                return ICC_Profile.getInstance(profilePath);
            }
            catch (SecurityException | IOException ignore) {
                if (DEBUG) {
                    ignore.printStackTrace();
                }
            }
        }

        return null;
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

    // Cache header profile data to avoid excessive array creation/copying in static inner class for on-demand lazy init
    private static class sRGB {
        private static final byte[] header = getProfileHeaderWithProfileId(ICC_Profile.getInstance(ColorSpace.CS_sRGB));
    }

    private static class CIEXYZ {
        private static final byte[] header = getProfileHeaderWithProfileId(ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ));
    }

    private static class PYCC {
        private static final byte[] header = getProfileHeaderWithProfileId(ICC_Profile.getInstance(ColorSpace.CS_PYCC));
    }

    private static class GRAY {
        private static final byte[] header = getProfileHeaderWithProfileId(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
    }

    private static class LINEAR_RGB {
        private static final byte[] header = getProfileHeaderWithProfileId(ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB));
    }

    private static class Profiles {
        // TODO: Honour java.iccprofile.path property?
        private static final Properties PROFILES = loadProfiles();

        private static Properties loadProfiles() {
            Properties systemDefaults;

            try {
                systemDefaults = SystemUtil.loadProperties(
                        ColorSpaces.class,
                        "com/twelvemonkeys/imageio/color/icc_profiles_" + Platform.os().id()
                );
            }
            catch (SecurityException | IOException ignore) {
                System.err.printf(
                        "Warning: Could not load system default ICC profile locations from %s, will use bundled fallback profiles.\n",
                        ignore.getMessage()
                );

                if (DEBUG) {
                    ignore.printStackTrace();
                }

                systemDefaults = null;
            }

            // Create map with defaults and add user overrides if any
            Properties profiles = new Properties(systemDefaults);

            try {
                Properties userOverrides = SystemUtil.loadProperties(
                        ColorSpaces.class,
                        "com/twelvemonkeys/imageio/color/icc_profiles"
                );
                profiles.putAll(userOverrides);
            }
            catch (SecurityException | IOException ignore) {
                // Most likely, this file won't be there
            }

            if (DEBUG) {
                System.out.println("User ICC profiles: " + profiles);
                System.out.println("System ICC profiles : " + systemDefaults);
            }

            return profiles;
        }

        static String getPath(final String profileName) {
            return PROFILES.getProperty(profileName);
        }
    }
}
