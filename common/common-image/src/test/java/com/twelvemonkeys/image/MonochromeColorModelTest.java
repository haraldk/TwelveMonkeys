/*
 * Copyright (c) 2026, Harald Kuhr
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

package com.twelvemonkeys.image;

import org.junit.jupiter.api.Test;
import java.awt.image.IndexColorModel;
import static org.junit.jupiter.api.Assertions.*;


/**
 * MonochromeColorModelTest
 */
class MonochromeColorModelTest {

    @Test
    void testGetInstance() {
        IndexColorModel instance1 = MonochromeColorModel.getInstance();
        IndexColorModel instance2 = MonochromeColorModel.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
        assertInstanceOf(MonochromeColorModel.class, instance1);
    }

    @Test
    void testProperties() {
        IndexColorModel cm = MonochromeColorModel.getInstance();
        assertEquals(1, cm.getPixelSize());
        assertEquals(2, cm.getMapSize());
        assertEquals(-1, cm.getTransparentPixel());

        int[] palette = new int[cm.getMapSize()];
        cm.getRGBs(palette);
        assertEquals(0xFF000000, palette[0]);
        assertEquals(0xFFFFFFFF, palette[1]);
    }

    @Test
    void testGetDataElements() {
        MonochromeColorModel cm = (MonochromeColorModel) MonochromeColorModel.getInstance();

        // Black
        assertPixelEquals(0, cm.getDataElements(0xFF000000, null));
        
        // White
        assertPixelEquals(1, cm.getDataElements(0xFFFFFFFF, null));

        // Dark gray (should be black)
        assertPixelEquals(0, cm.getDataElements(0xFF404040, null));

        // Light gray (0x81 = 129, should be white)
        assertPixelEquals(1, cm.getDataElements(0xFF818181, null));

        // Colors
        // Red (r=255, g=0, b=0) -> gray = (222 * 255 + 0 + 0) / 1000 = 56.61 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFFFF0000, null));

        // Green (r=0, g=255, b=0) -> gray = (0 + 707 * 255 + 0) / 1000 = 180.285 -> 1
        assertPixelEquals(1, cm.getDataElements(0xFF00FF00, null));

        // Blue (r=0, g=0, b=255) -> gray = (0 + 0 + 71 * 255) / 1000 = 18.105 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFF0000FF, null));

        // Mixed (r=128, g=128, b=128) -> gray = 128 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFF808080, null));

        // Mixed (r=129, g=129, b=129) -> gray = 129 -> 1
        assertPixelEquals(1, cm.getDataElements(0xFF818181, null));

        // Test alpha (should be ignored)
        assertPixelEquals(1, cm.getDataElements(0x00FFFFFF, null));
        assertPixelEquals(0, cm.getDataElements(0x00000000, null));

        // Edge cases for gray threshold (0x80)
        // (222 * r + 707 * g + 71 * b) / 1000
        // To get exactly 128:
        // If r=g=b=128 -> 128 (already tested)
        // If we want slightly more than 128:
        // r=129, g=128, b=128 -> (222*129 + 707*128 + 71*128)/1000 = (28638 + 90496 + 9088)/1000 = 128.222 -> 128 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFF818080, null));

        // r=130, g=128, b=128 -> (222*130 + 707*128 + 71*128)/1000 = (28860 + 90496 + 9088)/1000 = 128.444 -> 128 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFF828080, null));

        // Let's find something that results in 129
        // r=255, g=102, b=0 -> (222*255 + 707*102 + 0)/1000 = (56610 + 72114)/1000 = 128.724 -> 128 -> 0
        assertPixelEquals(0, cm.getDataElements(0xFFFF6600, null));
        // r=255, g=103, b=0 -> (222*255 + 707*103 + 0)/1000 = (56610 + 72821)/1000 = 129.431 -> 129 -> 1
        assertPixelEquals(1, cm.getDataElements(0xFFFF6700, null));
    }

    @Test
    void testGetDataElementsReuse() {
        MonochromeColorModel cm = (MonochromeColorModel) MonochromeColorModel.getInstance();
        byte[] pixel = new byte[1];
        
        Object result = cm.getDataElements(0xFFFFFFFF, pixel);
        assertSame(pixel, result);
        assertEquals(1, pixel[0]);

        result = cm.getDataElements(0xFF000000, pixel);
        assertSame(pixel, result);
        assertEquals(0, pixel[0]);
    }

    @Test
    void testGetRGB() {
        IndexColorModel cm = MonochromeColorModel.getInstance();
        assertEquals(0xFF000000, cm.getRGB(0));
        assertEquals(0xFFFFFFFF, cm.getRGB(1));
    }

    @Test
    void testGetDataElementsInvalid() {
        MonochromeColorModel cm = (MonochromeColorModel) MonochromeColorModel.getInstance();
        assertThrows(ClassCastException.class, () -> cm.getDataElements(0xFFFFFFFF, new int[1]));
    }

    private void assertPixelEquals(int expected, Object pixel) {
        assertInstanceOf(byte[].class, pixel);
        byte[] p = (byte[]) pixel;
        assertEquals(1, p.length);
        assertEquals((byte) expected, p[0]);
    }
}
