package com.twelvemonkeys.imageio.color;

import org.junit.Test;

import java.awt.color.ICC_Profile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LCMSSanitizerStrategyTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFixProfileNullProfile() throws Exception {
        new LCMSSanitizerStrategy().fixProfile(null, null);
    }

    @Test
    public void testFixProfileNoHeader() throws Exception {
        ICC_Profile profile = mock(ICC_Profile.class);
        new LCMSSanitizerStrategy().fixProfile(profile, null);

        verifyNoMoreInteractions(profile);
    }

    @Test
    public void testFixProfile() throws Exception {
        ICC_Profile profile = mock(ICC_Profile.class);
        new LCMSSanitizerStrategy().fixProfile(profile, new byte[0]);

        verifyNoMoreInteractions(profile);
    }
}