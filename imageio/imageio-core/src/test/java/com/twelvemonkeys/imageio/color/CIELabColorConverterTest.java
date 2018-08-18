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
