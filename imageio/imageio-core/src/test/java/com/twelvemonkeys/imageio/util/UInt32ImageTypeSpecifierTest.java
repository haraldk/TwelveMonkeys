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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(GRAY, new int [] {0}, false, false);

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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(GRAY, new int [] {0, 1}, true, false);
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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2}, false, false);

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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2, 3}, true, false);
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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2, 3}, true, true);
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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(CMYK, new int [] {0, 1, 2, 3}, false, false);

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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(CMYK, new int [] {0, 1, 2, 3, 4}, true, false);
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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier other = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier different = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2, 3}, true, false);
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
        ImageTypeSpecifier spec = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier other = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2}, false, false);
        ImageTypeSpecifier different = new UInt32ImageTypeSpecifier(sRGB, new int [] {0, 1, 2, 3}, true, false);
        // Equivalent, but broken, not equal
        ImageTypeSpecifier broken =
                ImageTypeSpecifier.createInterleaved(sRGB, new int [] {0, 1, 2}, DataBuffer.TYPE_INT, false, false);

        assertEquals(spec.hashCode(), other.hashCode());
        assertFalse(spec.hashCode() == different.hashCode());
        assertFalse(spec.hashCode() == broken.hashCode());
    }
}
