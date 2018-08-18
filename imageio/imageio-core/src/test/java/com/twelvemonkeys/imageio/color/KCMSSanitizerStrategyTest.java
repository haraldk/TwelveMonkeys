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

import org.junit.Test;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class KCMSSanitizerStrategyTest {
    private static final byte[] XYZ = new byte[] {'X', 'Y', 'Z', ' '};

    @Test(expected = IllegalArgumentException.class)
    public void testFixProfileNullProfile() throws Exception {
        new KCMSSanitizerStrategy().fixProfile(null);
    }

    @Test
    public void testFixProfile() throws Exception {
        new KCMSSanitizerStrategy().fixProfile(((ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB)).getProfile());
    }

    @Test
    public void testFixProfileUpdateHeader() throws Exception {
        byte[] header = new byte[128];
        header[ICC_Profile.icHdrRenderingIntent + 3] = 1;
        ICC_Profile profile = mock(ICC_Profile.class);
        when(profile.getData(ICC_Profile.icSigHead)).thenReturn(header);

        // Can't test that the values are actually changed, as the LCMS-backed implementation
        // of ICC_Profile does not change based on this invocation.
        new KCMSSanitizerStrategy().fixProfile(profile);

        // Verify that the method was invoked
        verify(profile).setData(eq(ICC_Profile.icSigHead), any(byte[].class));
    }

    @Test
    public void testFixProfileCorbisRGB() throws IOException {
        // TODO: Consider re-writing this using mocks, to avoid dependencies on the CMS implementation
        ICC_Profile corbisRGB = ICC_Profile.getInstance(getClass().getResourceAsStream("/profiles/Corbis RGB.icc"));

        new KCMSSanitizerStrategy().fixProfile(corbisRGB);

        // Make sure all known affected tags have type 'XYZ '
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigMediaWhitePointTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigRedColorantTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigGreenColorantTag), 0, 4));
        assertArrayEquals(XYZ, Arrays.copyOfRange(corbisRGB.getData(ICC_Profile.icSigBlueColorantTag), 0, 4));
    }
}