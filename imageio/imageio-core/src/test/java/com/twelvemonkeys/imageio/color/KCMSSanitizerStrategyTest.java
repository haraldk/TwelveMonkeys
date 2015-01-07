package com.twelvemonkeys.imageio.color;

import org.junit.Test;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KCMSSanitizerStrategyTest {
    private static final byte[] XYZ = new byte[] {'X', 'Y', 'Z', ' '};

    @Test(expected = IllegalArgumentException.class)
    public void testFixProfileNullProfile() throws Exception {
        new KCMSSanitizerStrategy().fixProfile(null, null);
    }

    @Test
    public void testFixProfileNullHeader() throws Exception {
        new KCMSSanitizerStrategy().fixProfile(((ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB)).getProfile(), null);
    }

    @Test
    public void testFixProfileUpdateHeader() throws Exception {
        ICC_Profile profile = mock(ICC_Profile.class);
        byte[] header = new byte[0];

        // Can't test that the values are actually changed, as the LCMS-backed implementation
        // of ICC_Profile does not change based on this invocation.
        new KCMSSanitizerStrategy().fixProfile(profile, header);

        // Verify that the method was invoked
        verify(profile).setData(ICC_Profile.icSigHead, header);
    }

    @Test
    public void testFixProfileCorbisRGB() throws IOException {
        // TODO: Consider re-writing this using mocks, to avoid dependencies on the CMS implementation
        ICC_Profile corbisRGB = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/Corbis RGB.icc"));

        new KCMSSanitizerStrategy().fixProfile(corbisRGB, null);

        // Make sure all known affected tags have type 'XYZ '
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigMediaWhitePointTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigRedColorantTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigGreenColorantTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigBlueColorantTag), 0, 4));
    }
}