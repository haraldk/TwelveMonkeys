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

import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import static org.junit.Assert.*;

/**
 * IndexedImageTypeSpecifierTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IndexedImageTypeSpecifierTestCase.java,v 1.0 Jun 9, 2008 2:42:03 PM haraldk Exp$
 */
public class IndexedImageTypeSpecifierTest {
    @Test
    public void testEquals() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);

        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier other = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier different = IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(2, 2, new int[]{0xff00ff, 0x00, 0xff00ff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE));

        assertEquals(spec, other);
        assertEquals(other, spec);

        assertEquals(spec.hashCode(), other.hashCode());

        assertTrue(spec.equals(other));
        assertTrue(other.equals(spec));

        // TODO: There is still a problem that IndexColorModel does not override equals,
        // so any model with the same number of bits, transparency, and transfer type will be treated as equal
        assertFalse(other.equals(different));
    }

    @Test
    public void testHashCode() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);

        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier other = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier different = IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(2, 2, new int[]{0xff00ff, 0x00, 0xff00ff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE));

        // TODO: There is still a problem that IndexColorModel does not override hashCode,
        // so any model with the same number of bits, transparency, and transfer type will have same hash
        assertEquals(spec.hashCode(), other.hashCode());
        assertFalse(spec.hashCode() == different.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        IndexedImageTypeSpecifier.createFromIndexColorModel(null);
    }

    @Test
    public void testCreateBufferedImageBinary() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);
        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);

        BufferedImage image = spec.createBufferedImage(2, 2);

        assertNotNull(image);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, image.getType());
        assertEquals(cm, image.getColorModel());
    }

    @Test
    public void testCreateBufferedImageIndexed() {
        IndexColorModel cm = new IndexColorModel(8, 256, new int[256], 0, false, -1, DataBuffer.TYPE_BYTE);
        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);

        BufferedImage image = spec.createBufferedImage(2, 2);

        assertNotNull(image);
        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, image.getType());
        assertEquals(cm, image.getColorModel());
    }
}
