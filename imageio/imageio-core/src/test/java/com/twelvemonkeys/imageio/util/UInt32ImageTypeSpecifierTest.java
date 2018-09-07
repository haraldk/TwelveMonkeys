/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.imageio.color.ColorSpaces;
import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class UInt32ImageTypeSpecifierTest {
    private static final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    private static final ColorSpace GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    private static final ColorSpace CMYK = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);

    @Test
    public void testGray() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int [] {0}, false, false);

        assertEquals(1, spec.getNumBands());
        assertEquals(1, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertFalse(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(1, spec.getColorModel().getNumComponents());
        assertEquals(1, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(1, spec.getSampleModel().getNumBands());
        assertEquals(1, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testGrayAlpha() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int [] {0, 1}, true, false);
        assertEquals(2, spec.getNumBands());
        assertEquals(2, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertTrue(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(2, spec.getColorModel().getNumComponents());
        assertEquals(1, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(2, spec.getSampleModel().getNumBands());
        assertEquals(2, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testRGB() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, false, false);

        assertEquals(3, spec.getNumBands());
        assertEquals(3, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));
        assertEquals(32, spec.getBitsPerBand(2));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertFalse(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(3, spec.getColorModel().getNumComponents());
        assertEquals(3, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(3, spec.getSampleModel().getNumBands());
        assertEquals(3, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testRGBAlpha() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2, 3}, true, false);
        assertEquals(4, spec.getNumBands());
        assertEquals(4, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));
        assertEquals(32, spec.getBitsPerBand(2));
        assertEquals(32, spec.getBitsPerBand(3));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertTrue(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(4, spec.getColorModel().getNumComponents());
        assertEquals(3, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(4, spec.getSampleModel().getNumBands());
        assertEquals(4, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testRGBAlphaPre() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2, 3}, true, true);
        assertEquals(4, spec.getNumBands());
        assertEquals(4, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));
        assertEquals(32, spec.getBitsPerBand(2));
        assertEquals(32, spec.getBitsPerBand(3));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertTrue(spec.getColorModel().hasAlpha());
        assertTrue(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(4, spec.getColorModel().getNumComponents());
        assertEquals(3, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(4, spec.getSampleModel().getNumBands());
        assertEquals(4, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testCMYK() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(CMYK, new int [] {0, 1, 2, 3}, false, false);

        assertEquals(4, spec.getNumBands());
        assertEquals(4, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));
        assertEquals(32, spec.getBitsPerBand(2));
        assertEquals(32, spec.getBitsPerBand(3));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertFalse(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(4, spec.getColorModel().getNumComponents());
        assertEquals(4, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(4, spec.getSampleModel().getNumBands());
        assertEquals(4, spec.getSampleModel().getNumDataElements());
    }

    @Test
    public void testCMYKAlpha() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(CMYK, new int [] {0, 1, 2, 3, 4}, true, false);
        assertEquals(5, spec.getNumBands());
        assertEquals(5, spec.getNumComponents());
        assertEquals(32, spec.getBitsPerBand(0));
        assertEquals(32, spec.getBitsPerBand(1));
        assertEquals(32, spec.getBitsPerBand(2));
        assertEquals(32, spec.getBitsPerBand(3));
        assertEquals(32, spec.getBitsPerBand(4));

        assertThat(spec.getColorModel(), is(ComponentColorModel.class));
        assertTrue(spec.getColorModel().hasAlpha());
        assertFalse(spec.getColorModel().isAlphaPremultiplied());
        assertEquals(5, spec.getColorModel().getNumComponents());
        assertEquals(4, spec.getColorModel().getNumColorComponents());

        assertThat(spec.getSampleModel(), is(PixelInterleavedSampleModel.class));
        assertEquals(5, spec.getSampleModel().getNumBands());
        assertEquals(5, spec.getSampleModel().getNumDataElements());
    }


    @Test
    public void testEquals() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier other = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier different = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2, 3}, true, false);
        // Equivalent, but broken, not equal
        ImageTypeSpecifier broken =
                ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, DataBuffer.TYPE_INT, false, false);

        assertEquals(spec, other);
        assertEquals(other, spec);

        assertTrue(spec.equals(other));
        assertTrue(other.equals(spec));
        assertFalse(spec.equals(different));
        assertFalse(different.equals(spec));
        assertFalse(spec.equals(broken));
        assertFalse(broken.equals(spec));
    }

    @Test
    public void testHashCode() {
        ImageTypeSpecifier spec = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier other = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier different = UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2, 3}, true, false);
        // Equivalent, but broken, not equal
        ImageTypeSpecifier broken =
                ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, DataBuffer.TYPE_INT, false, false);

        assertEquals(spec.hashCode(), other.hashCode());
        assertFalse(spec.hashCode() == different.hashCode());
        assertFalse(spec.hashCode() == broken.hashCode());
    }
}
