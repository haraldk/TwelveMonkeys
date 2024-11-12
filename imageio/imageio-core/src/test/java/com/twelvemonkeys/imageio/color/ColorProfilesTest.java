package com.twelvemonkeys.imageio.color;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ColorProfilesTest {
    @Test
    public void testCreateColorSpaceFromBrokenProfileIsFixedCS_sRGB() {
        ICC_Profile internal = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ICC_Profile profile = createBrokenProfile(internal);
        assertNotSame(internal, profile); // Sanity check

        assertTrue(ColorProfiles.isOffendingColorProfile(profile));

        ICC_ColorSpace created = ColorSpaces.createColorSpace(profile);
        assertSame(ColorSpace.getInstance(ColorSpace.CS_sRGB), created);
        assertTrue(created.isCS_sRGB());
    }

    private ICC_Profile createBrokenProfile(ICC_Profile internal) {
        byte[] data = internal.getData();
        data[ICC_Profile.icHdrRenderingIntent] = 1; // Intent: 1 == Relative Colormetric Little Endian
        data[ICC_Profile.icHdrRenderingIntent + 1] = 0;
        data[ICC_Profile.icHdrRenderingIntent + 2] = 0;
        data[ICC_Profile.icHdrRenderingIntent + 3] = 0;
        return ICC_Profile.getInstance(data);
    }

    @Test
    public void testIsOffendingColorProfile() {
        ICC_Profile broken = createBrokenProfile(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
        assertTrue(ColorProfiles.isOffendingColorProfile(broken));
    }

    @Test
    public void testIsCS_sRGBTrue() {
        assertTrue(ColorProfiles.isCS_sRGB(ICC_Profile.getInstance(ColorSpace.CS_sRGB)));
    }

    @Test
    public void testIsCS_sRGBFalse() {
        assertFalse(ColorProfiles.isCS_sRGB(ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB)));
        assertFalse(ColorProfiles.isCS_sRGB(ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ)));
        assertFalse(ColorProfiles.isCS_sRGB(ICC_Profile.getInstance(ColorSpace.CS_GRAY)));
        assertFalse(ColorProfiles.isCS_sRGB(ICC_Profile.getInstance(ColorSpace.CS_PYCC)));
    }

    @Test
    public void testIsCS_sRGBNull() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.isCS_sRGB(null));
    }

    @Test
    public void testIsCS_GRAYTrue() {
        assertTrue(ColorProfiles.isCS_GRAY(ICC_Profile.getInstance(ColorSpace.CS_GRAY)));
    }

    @Test
    public void testIsCS_GRAYFalse() {
        assertFalse(ColorProfiles.isCS_GRAY(ICC_Profile.getInstance(ColorSpace.CS_sRGB)));
        assertFalse(ColorProfiles.isCS_GRAY(ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB)));
        assertFalse(ColorProfiles.isCS_GRAY(ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ)));
        assertFalse(ColorProfiles.isCS_GRAY(ICC_Profile.getInstance(ColorSpace.CS_PYCC)));
    }

    @Test
    public void testIsCS_GRAYNull() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.isCS_GRAY(null));
    }

    @Test
    public void testCreateProfileNull() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfile(null));
    }

    @Test
    public void testReadProfileNull() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfile(null));
    }

    @Test
    public void testCreateProfileRawNull() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfileRaw(null));
    }

    @Test
    public void testReadProfileRawNull() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfileRaw(null));
    }

    @Test
    public void testCreateProfileRaw() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        ICC_Profile profileRaw = ColorProfiles.createProfileRaw(data);
        assertArrayEquals(data, profileRaw.getData());
    }

    @Test
    public void testReadProfileRaw() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        ICC_Profile profileRaw = ColorProfiles.readProfileRaw(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));
        assertArrayEquals(data, profileRaw.getData());
    }

    @Test
    public void testCreateProfileRawBadData() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfileRaw(new byte[5]));
    }

    @Test
    public void testReadProfileRawBadData() throws IOException {
        // NOTE: The array here is larger, as there's a bug in OpenJDK 15 & 16, that throws
        // ArrayIndexOutOfBoundsException if the stream is shorter than the profile signature...
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfileRaw(new ByteArrayInputStream(new byte[40])));
    }

    @Test
    public void testCreateProfileBadData() {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfile(new byte[5]));
    }

    @Test
    public void testReadProfileBadData() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfile(new ByteArrayInputStream(new byte[5])));
    }

    @Test
    public void testCreateProfileRawTruncated() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfileRaw(Arrays.copyOf(data, 200)));
    }

    @Test
    public void testReadProfileRawTruncated() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfileRaw(new ByteArrayInputStream(data, 0, 200)));
    }

    @Test
    public void testCreateProfileTruncated() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfile(Arrays.copyOf(data, 200)));
    }

    @Test
    public void testReadProfileTruncated() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfile(new ByteArrayInputStream(data, 0, 200)));
    }

    @Test
    public void testCreateProfileRawTruncatedHeader() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfileRaw(Arrays.copyOf(data, 125)));
    }

    @Test
    public void testReadProfileRawTruncatedHeader() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfileRaw(new ByteArrayInputStream(data, 0, 125)));
    }

    @Test
    public void testCreateProfileTruncatedHeader() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.createProfile(Arrays.copyOf(data, 125)));
    }

    @Test
    public void testReadProfileTruncatedHeader() throws IOException {
        byte[] data = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")).getData();
        assertThrows(IllegalArgumentException.class, () -> ColorProfiles.readProfile(new ByteArrayInputStream(data, 0, 125)));
    }

    @Test
    public void testCreateProfileBytesSame() throws IOException {
        ICC_Profile profile = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));
        ICC_Profile profile1 = ColorProfiles.createProfile(profile.getData());
        ICC_Profile profile2 = ColorProfiles.createProfile(profile.getData());

        assertEquals(profile1, profile2);
        assertSame(profile1, profile2);
    }

    @Test
    public void testReadProfileInputStreamSame() throws IOException {
        ICC_Profile profile1 = ColorProfiles.readProfile(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));
        ICC_Profile profile2 = ColorProfiles.readProfile(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));

        assertEquals(profile1, profile2);
        assertSame(profile1, profile2);
    }

    @Test
    public void testReadProfileDifferent() throws IOException {
        // These profiles are extracted from various JPEGs, and have the exact same profile header (but are different profiles)...
        ICC_Profile profile1 = ColorProfiles.readProfile(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));
        ICC_Profile profile2 = ColorProfiles.readProfile(getClass().getResourceAsStream("/profiles/color_match_rgb.icc"));

        assertNotSame(profile1, profile2);
    }

    @Test
    public void testCreateProfileBytesSameAsCached() throws IOException {
        ICC_Profile profile = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));
        ICC_ColorSpace cs1 = ColorSpaces.createColorSpace(profile);
        ICC_Profile profile2 = ColorProfiles.createProfile(profile.getData());

        assertEquals(cs1.getProfile(), profile2);
        assertSame(cs1.getProfile(), profile2);
    }

    @Test
    public void testReadProfileInputStreamSameAsCached() throws IOException {
        ICC_ColorSpace cs1 = ColorSpaces.createColorSpace(ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc")));
        ICC_Profile profile2 = ColorProfiles.readProfile(getClass().getResourceAsStream("/profiles/adobe_rgb_1998.icc"));

        assertEquals(cs1.getProfile(), profile2);
        assertSame(cs1.getProfile(), profile2);
    }

    @Test
    public void testCreateProfileBytesSameAsInternal() {
        ICC_Profile profile1 = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ICC_Profile profile2 = ColorProfiles.createProfile(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());

        assertEquals(profile1, profile2);
        assertSame(profile1, profile2);
    }

    @Test
    public void testReadProfileInputStreamSameAsInternal() throws IOException {
        ICC_Profile profile1 = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ICC_Profile profile2 = ColorProfiles.readProfile(new ByteArrayInputStream(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData()));

        assertEquals(profile1, profile2);
        assertSame(profile1, profile2);
    }
}