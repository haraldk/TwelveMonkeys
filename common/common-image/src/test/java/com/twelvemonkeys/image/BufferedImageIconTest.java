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

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;


/**
 * BufferedImageIconTest
 */
public class BufferedImageIconTest {

    @Test
    public void testConstructorNullImage() {
        assertThrows(NullPointerException.class, () -> new BufferedImageIcon(null));
    }

    @Test
    public void testConstructorDefault() {
        BufferedImage image = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImageIcon icon = new BufferedImageIcon(image);
        assertEquals(10, icon.getIconWidth());
        assertEquals(20, icon.getIconHeight());
    }

    @Test
    public void testConstructorCustomSize() {
        BufferedImage image = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImageIcon icon = new BufferedImageIcon(image, 30, 40);
        assertEquals(30, icon.getIconWidth());
        assertEquals(40, icon.getIconHeight());
    }

    @Test
    public void testConstructorIllegalSize() {
        BufferedImage image = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
        assertThrows(IllegalArgumentException.class, () -> new BufferedImageIcon(image, 0, 40));
        assertThrows(IllegalArgumentException.class, () -> new BufferedImageIcon(image, 30, 0));
        assertThrows(IllegalArgumentException.class, () -> new BufferedImageIcon(image, -1, 40));
        assertThrows(IllegalArgumentException.class, () -> new BufferedImageIcon(image, 30, -1));
    }
}
