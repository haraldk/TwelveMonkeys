package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.imageio.color.CIELabColorConverter.Illuminant;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * CIELabColorConverterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: CIELabColorConverterTest.java,v 1.0 22/10/15 harald.kuhr Exp$
 */
public class CIELabColorConverterTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNoIllumninant() {
        new CIELabColorConverter(null);
    }

    @Test
    public void testD50() {
        CIELabColorConverter converter = new CIELabColorConverter(Illuminant.D50);
        float[] rgb = new float[3];

        converter.toRGB(100, -128, -128, rgb);
        assertArrayEquals(new float[] {0, 255, 255}, rgb, 1);

        converter.toRGB(100, 0, 0, rgb);
        assertArrayEquals(new float[] {255, 252, 220}, rgb, 5);

        converter.toRGB(0, 0, 0, rgb);
        assertArrayEquals(new float[] {0, 0, 0}, rgb, 1);

        converter.toRGB(100, 0, 127, rgb);
        assertArrayEquals(new float[] {255, 249, 0}, rgb, 5);

        converter.toRGB(50, -128, 127, rgb);
        assertArrayEquals(new float[] {0, 152, 0}, rgb, 2);

        converter.toRGB(50, 127, -128, rgb);
        assertArrayEquals(new float[] {222, 0, 255}, rgb, 2);
    }

    @Test
    public void testD65() {
        CIELabColorConverter converter = new CIELabColorConverter(Illuminant.D65);
        float[] rgb = new float[3];

        converter.toRGB(100, -128, -128, rgb);
        assertArrayEquals(new float[] {0, 255, 255}, rgb, 1);

        converter.toRGB(100, 0, 0, rgb);
        assertArrayEquals(new float[] {255, 252, 255}, rgb, 5);

        converter.toRGB(0, 0, 0, rgb);
        assertArrayEquals(new float[] {0, 0, 0}, rgb, 1);

        converter.toRGB(100, 0, 127, rgb);
        assertArrayEquals(new float[] {255, 250, 0}, rgb, 5);

        converter.toRGB(50, -128, 127, rgb);
        assertArrayEquals(new float[] {0, 152, 0}, rgb, 3);

        converter.toRGB(50, 127, -128, rgb);
        assertArrayEquals(new float[] {184, 0, 255}, rgb, 5);
    }
}
