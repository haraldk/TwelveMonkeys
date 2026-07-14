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
 * InverseColorMapTest
 */
class InverseColorMapTest {

    @Test
    void testConstructorByteArray() {
        byte[] colors = {
                (byte) 0, (byte) 0, (byte) 0, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255
        };
        InverseColorMap icm = new InverseColorMap(colors);
        assertEquals(0, icm.getIndexNearest(0x000000));
        assertEquals(1, icm.getIndexNearest(0xFFFFFF));
    }

    @Test
    void testConstructorIntArray() {
        int[] colors = {
                0xFF000000,
                0xFFFFFFFF
        };
        InverseColorMap icm = new InverseColorMap(colors);
        assertEquals(0, icm.getIndexNearest(0x000000));
        assertEquals(1, icm.getIndexNearest(0xFFFFFF));
    }

    @Test
    void testConstructorByteArrayTransparent() {
        byte[] colors = {
                (byte) 255, (byte) 0, (byte) 0, (byte) 255, // Red
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,     // Transparent
                (byte) 0, (byte) 0, (byte) 255, (byte) 255  // Blue
        };
        InverseColorMap icm = new InverseColorMap(colors, 1);
        assertEquals(0, icm.getIndexNearest(0xFF0000));
        assertEquals(2, icm.getIndexNearest(0x0000FF));
        // Transparent (1) should be ignored, so black should map to something else, likely red or blue depending on distance.
        int blackIndex = icm.getIndexNearest(0x000000);
        assertNotEquals(1, blackIndex);
    }

    @Test
    void testConstructorIntArrayTransparent() {
        int[] colors = {
                0xFFFF0000, // Red
                0x00000000, // Transparent
                0xFF0000FF  // Blue
        };
        InverseColorMap icm = new InverseColorMap(colors, 1);
        assertEquals(0, icm.getIndexNearest(0xFF0000));
        assertEquals(2, icm.getIndexNearest(0x0000FF));
        int blackIndex = icm.getIndexNearest(0x000000);
        assertNotEquals(1, blackIndex);
    }

    @Test
    void testGetIndexNearestRGB() {
        int[] colors = {
                0xFFFF0000, // 0: Red
                0xFF00FF00, // 1: Green
                0xFF0000FF  // 2: Blue
        };
        InverseColorMap icm = new InverseColorMap(colors);
        
        assertEquals(0, icm.getIndexNearest(255, 0, 0));
        assertEquals(1, icm.getIndexNearest(0, 255, 0));
        assertEquals(2, icm.getIndexNearest(0, 0, 255));

        // Near matches
        assertEquals(0, icm.getIndexNearest(200, 20, 20));
        assertEquals(1, icm.getIndexNearest(20, 200, 20));
        assertEquals(2, icm.getIndexNearest(20, 20, 200));
    }

    @Test
    void testGetIndexNearestColor() {
        int[] colors = {
                0xFFFF0000, // 0: Red
                0xFF00FF00, // 1: Green
                0xFF0000FF  // 2: Blue
        };
        InverseColorMap icm = new InverseColorMap(colors);

        assertEquals(0, icm.getIndexNearest(0xFF0000));
        assertEquals(1, icm.getIndexNearest(0x00FF00));
        assertEquals(2, icm.getIndexNearest(0x0000FF));

        // Near matches
        assertEquals(0, icm.getIndexNearest(0xC81414));
        assertEquals(1, icm.getIndexNearest(0x14C814));
        assertEquals(2, icm.getIndexNearest(0x1414C8));
    }

    @Test
    void testGetIndexNearestColorWithOffset() {
        // Test that color mapping is correct even with offsets in 24-bit color
        int[] colors = {
                0xFFFF0000,
                0xFF00FF00,
                0xFF0000FF
        };
        InverseColorMap icm = new InverseColorMap(colors);

        assertEquals(0, icm.getIndexNearest(0xFF0000));
        assertEquals(1, icm.getIndexNearest(0x00FF00));
        assertEquals(2, icm.getIndexNearest(0x0000FF));
    }

    @Test
    void testGetIndexNearestColorWithAlpha() {
        // Test that alpha bits are ignored in getIndexNearest(int pColor)
        int[] colors = {
                0xFFFF0000,
                0xFF00FF00,
                0xFF0000FF
        };
        InverseColorMap icm = new InverseColorMap(colors);

        assertEquals(0, icm.getIndexNearest(0x7FFF0000));
        assertEquals(1, icm.getIndexNearest(0x0000FF00));
        assertEquals(2, icm.getIndexNearest(0xFF0000FF));
    }

    @Test
    void testEmptyColorMap() {
        // It seems empty colormap doesn't throw exception but might cause issues later or just result in 0
        InverseColorMap icm = new InverseColorMap(new int[0]);
        assertEquals(0, icm.getIndexNearest(0x000000));
    }

    @Test
    void testNullColorMap() {
        assertThrows(NullPointerException.class, () -> new InverseColorMap((int[]) null));
        assertThrows(NullPointerException.class, () -> new InverseColorMap((byte[]) null));
    }
}
