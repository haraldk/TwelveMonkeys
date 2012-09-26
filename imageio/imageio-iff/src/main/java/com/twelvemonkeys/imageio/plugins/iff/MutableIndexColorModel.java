/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Parts of this code is based on ilbmtoppm.c
 *
 * Copyright (C) 1989 by Jef Poskanzer.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation.  This software is provided "as is" without express or
 * implied warranty.
 *
 * Multipalette-support by Ingo Wilken (Ingo.Wilken@informatik.uni-oldenburg.de)
 */

package com.twelvemonkeys.imageio.plugins.iff;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

/**
 * A mutable indexed color model.
 * For use with images that exploits Amiga hardware to change the color
 * lookup table between scan lines.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MutableIndexColorModel.java,v 1.0 29.03.12 15:00 haraldk Exp$
 */
final class MutableIndexColorModel extends ColorModel {
    final static int MP_REG_IGNORE = -1;

    final int[] rgbs;

    public MutableIndexColorModel(final IndexColorModel base) {
        super(base.getPixelSize(), base.getComponentSize(), base.getColorSpace(), base.hasAlpha(), base.isAlphaPremultiplied(), base.getTransparency(), base.getTransferType());

        this.rgbs = getRGBs(base);
    }

    private static int[] getRGBs(final IndexColorModel colorModel) {
        int[] rgbs = new int[colorModel.getMapSize()];
        colorModel.getRGBs(rgbs);
        return rgbs;
    }

    public void adjustColorMap(final PaletteChange[] changes) {
        for (int i = 0; i < changes.length; i++) {
            int index = changes[i].index;

            // TODO: Move validation to chunk (when reading)
            if (index >= rgbs.length) {
                // TODO: Issue IIO warning
                System.err.printf("warning - palette change register out of range\n");
                System.err.printf("    change structure %d  index=%d (max %d)\n", i, index, getMapSize() - 1);
                System.err.printf("    ignoring it... colors might get messed up from here\n");
            }
            else if (index != MP_REG_IGNORE) {
                updateRGB(index, ((changes[i].r & 0xff) << 16) | ((changes[i].g & 0xff) << 8) | (changes[i].b & 0xff));
            }
        }
    }

    @Override
    public int getRGB(int pixel) {
        return rgbs[pixel];
    }

    @Override
    public int getRed(int pixel) {
        return (rgbs[pixel] >> 16) & 0xff;
    }

    @Override
    public int getGreen(int pixel) {
        return (rgbs[pixel] >> 8) & 0xff;
    }

    @Override
    public int getBlue(int pixel) {
        return rgbs[pixel] & 0xff;
    }

    @Override
    public int getAlpha(int pixel) {
        return (rgbs[pixel] >> 24) & 0xff;
    }
    
    private void updateRGB(int index, int rgb) {
        rgbs[index] = rgb;
    }

    public int getMapSize() {
        return rgbs.length;
    }

    static class PaletteChange {
        /* palette index to change */
        public int index;
        /* new colors for index */
        public byte r;
        public byte g;
        public byte b;
    }
}
