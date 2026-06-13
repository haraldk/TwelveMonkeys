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

import static org.junit.jupiter.api.Assertions.*;


/**
 * BrightnessContrastFilterTest
 */
public class BrightnessContrastFilterTest {

    @Test
    public void testNoChange() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(0, 0);
        int argb = 0xFF123456;
        assertEquals(argb, filter.filterRGB(0, 0, argb));
    }

    @Test
    public void testBrightnessMax() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(2.0f, 0);
        int argb = 0xFF123456;
        assertEquals(0xFFFFFFFF, filter.filterRGB(0, 0, argb));
    }

    @Test
    public void testBrightnessMin() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(-2.0f, 0);
        int argb = 0xFF123456;
        assertEquals(0xFF000000, filter.filterRGB(0, 0, argb));
    }

    @Test
    public void testContrastMax() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(0, 1.0f);
        // Max contrast should result in primary colors

        // 0x40 is < 0x80 (128), so it should go towards 0
        // 0xC0 is > 0x80 (128), so it should go towards 255
        assertEquals(0xFF000000, filter.filterRGB(0, 0, 0xFF404040));
        assertEquals(0xFFFFFFFF, filter.filterRGB(0, 0, 0xFFC0C0C0));
        
        // Check mixed
        assertEquals(0xFFFF00FF, filter.filterRGB(0, 0, 0xFFC040C0));
    }

    @Test
    public void testContrastMin() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(0, -1.0f);
        // Min contrast should result in gray (127 or 128 depending on implementation, 
        // looking at the code: 127.5 * 1.0 + (i - 127) * 0 = 127.5 -> 127)
        int gray = 0xFF7F7F7F;
        assertEquals(gray, filter.filterRGB(0, 0, 0xFF000000));
        assertEquals(gray, filter.filterRGB(0, 0, 0xFFFFFFFF));
        assertEquals(gray, filter.filterRGB(0, 0, 0xFF123456));
    }

    @Test
    public void testDefaultConstructor() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter();
        // Default is 0.3, 0.3. Should be brighter and more contrast than the original.
        int argb = 0xFF808080;
        int filtered = filter.filterRGB(0, 0, argb);
        
        int r = (filtered >> 16) & 0xFF;
        int g = (filtered >> 8) & 0xFF;
        int b = filtered & 0xFF;
        
        assertTrue(r > 0x80);
        assertTrue(g > 0x80);
        assertTrue(b > 0x80);
    }

    @Test
    public void testAlphaRemains() {
        BrightnessContrastFilter filter = new BrightnessContrastFilter(0.5f, 0.5f);
        int argb = 0x12345678;
        int filtered = filter.filterRGB(0, 0, argb);
        assertEquals(0x12, (filtered >> 24) & 0xFF);
    }

    @Test
    public void testCanFilterIndexColorModel() {
        class BrightnessContrastFilterSub extends BrightnessContrastFilter {
            boolean canFilter() {
                return canFilterIndexColorModel;
            }
        }
        assertTrue(new BrightnessContrastFilterSub().canFilter());
    }
}
