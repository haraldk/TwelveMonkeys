/*
 * Copyright (c) 2009, Harald Kuhr
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
package com.twelvemonkeys.imageio.plugins.bmp;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

/**
 * Describes an indexed bitmap structure (1, 4, or 8 bits per pixes).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: BitmapIndexed.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
class BitmapIndexed extends BitmapDescriptor {
    protected final int[] bits;
    protected final int[] colors;

    public BitmapIndexed(final DirectoryEntry pEntry, final DIBHeader pHeader) {
        super(pEntry, pHeader);
        bits = new int[getWidth() * getHeight()];

        // NOTE: We're adding space for one extra color, for transparency
        colors = new int[getColorCount() + 1];
    }

    public BufferedImage createImageIndexed() {
        // TODO: This is very stupid, maybe we need a TYPE_CUSTOM image, with separate alphaRaster?!
        // As ICO has a separate bitmask, not related to palette index (allows 256 colors + trans) :-P

        IndexColorModel icm = createColorModel();

        // This is slightly obscure, and should probably be moved..
        Hashtable<String, Object> properties = null;
        if (entry instanceof DirectoryEntry.CUREntry) {
            properties = new Hashtable<>(1);
            properties.put("cursor_hotspot", ((DirectoryEntry.CUREntry) this.entry).getHotspot());
        }

        BufferedImage image = new BufferedImage(
                icm,
                icm.createCompatibleWritableRaster(getWidth(), getHeight()),
                icm.isAlphaPremultiplied(), properties
        );

        WritableRaster raster = image.getRaster();

        // Make pixels transparent according to mask
        final int trans = icm.getTransparentPixel();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (mask.isTransparent(x, y)) {
                    bits[x + getWidth() * y] = trans;
                }
            }
        }

        raster.setSamples(0, 0, getWidth(), getHeight(), 0, bits);

        return image;
    }

    /**
     * @return Color model created from color palette in entry
     */
    IndexColorModel createColorModel() {
        // NOTE: This is a hack to make room for transparent pixel for mask
        int bits = getBitCount();

        int colors = this.colors.length;
        int transparent = -1;

        // Try to avoid USHORT transfertype, as it results in BufferedImage TYPE_CUSTOM
        // NOTE: This code assumes icons are small, and is NOT optimized for performance...
        if (colors > (1 << getBitCount())) {
            int index = findTransparentIndexMaybeRemap(this.colors, this.bits);

            if (index == -1) {
                // No duplicate found, increase bitcount
                bits++;
                transparent = this.colors.length - 1;
            }
            else {
                // Found a duplicate, use it as transparent
                transparent = index;
                colors--;
            }
        }

        // NOTE: Setting hasAlpha to true, makes things work on 1.2
        return new IndexColorModel(
                bits, colors, this.colors, 0, true, transparent,
                bits <= 8 ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT
        );
    }

    private static int findTransparentIndexMaybeRemap(final int[] colors, final int[] bits) {
        // Look for unused colors, to use as transparent
        boolean[] used = new boolean[colors.length - 1];
        for (int bit : bits) {
            if (!used[bit]) {
                used[bit] = true;
            }
        }

        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return i;
            }
        }

        // Try to find duplicates in colormap, and remap
        int transparent = -1;
        int duplicate = -1;
        for (int i = 0; transparent == -1 && i < colors.length - 1; i++) {
            for (int j = i + 1; j < colors.length - 1; j++) {
                if (colors[i] == colors[j]) {
                    transparent = j;
                    duplicate = i;
                    break;
                }
            }
        }

        if (transparent != -1) {
            // Remap duplicate
            for (int i = 0; i < bits.length; i++) {
                if (bits[i] == transparent) {
                    bits[i] = duplicate;
                }
            }
        }

        return transparent;
    }

    public BufferedImage getImage() {
        if (image == null) {
            image = createImageIndexed();
        }

        return image;
    }
}
