/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pict;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * BitMapPattern
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BitMapPattern.java,v 1.0 Mar 2, 2009 10:31:56 AM haraldk Exp$
 */
final class BitMapPattern extends Pattern {

    private final byte[] pattern;

    BitMapPattern(final Paint pColor) {
        this(pColor, null);
    }

    public BitMapPattern(final byte[] pPattern) {
        this(create8x8Pattern(pPattern), pPattern);
    }

    private BitMapPattern(final Paint pColor, final byte[] pPattern) {
        super(pColor);

        pattern = pPattern;
    }

    // TODO: Refactor, don't need both BitMapPattern constructors and create8x8Pattern methods?
    public BitMapPattern(final byte[] pPattern, Color fg, Color bg) {
        this(create8x8Pattern(pPattern, fg, bg));
    }

    BitMapPattern(final int pPattern) {
        this(create8x8Pattern(pPattern));
    }

    private static Paint create8x8Pattern(final int pPattern) {
//        // TODO: Creating a special purpose Pattern might be faster than piggy-backing on TexturePaint
//        WritableRaster raster = QuickDraw.MONOCHROME.createCompatibleWritableRaster(8, 8);
//        byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();
//
//        for (int i = 0; i < data.length; i += 4) {
//            data[i    ] = (byte) ((pPattern >> 24) & 0xFF);
//            data[i + 1] = (byte) ((pPattern >> 16) & 0xFF);
//            data[i + 2] = (byte) ((pPattern >>  8) & 0xFF);
//            data[i + 3] = (byte) ((pPattern      ) & 0xFF);
//        }
//
//        BufferedImage img = new BufferedImage(QuickDraw.MONOCHROME, raster, false, null);
//        return new TexturePaint(img, new Rectangle(8, 8));
        byte[] data = new byte[8];

        for (int i = 0; i < data.length; i += 4) {
            data[i    ] = (byte) ((pPattern >> 24) & 0xFF);
            data[i + 1] = (byte) ((pPattern >> 16) & 0xFF);
            data[i + 2] = (byte) ((pPattern >>  8) & 0xFF);
            data[i + 3] = (byte) ((pPattern      ) & 0xFF);
        }

        return create8x8Pattern(data);
    }

    private static Paint create8x8Pattern(final byte[] pPattern) {
        WritableRaster raster = Raster.createPackedRaster(new DataBufferByte(pPattern, 8), 8, 8, 1, new Point());
        BufferedImage img = new BufferedImage(QuickDraw.MONOCHROME, raster, false, null);
        return new TexturePaint(img, new Rectangle(8, 8));
    }

    private static Paint create8x8Pattern(final byte[] pPattern, Color fg, Color bg) {
        switch (isSolid(pPattern)) {
            case 0: // 0x00
                return bg;
            case -1: // 0xff
                return fg;
            default:
                // Fall through
        }

        WritableRaster raster = Raster.createPackedRaster(new DataBufferByte(pPattern, 8), 8, 8, 1, new Point());
        IndexColorModel cm = new IndexColorModel(1, 2, new int[] {bg.getRGB(), fg.getRGB()}, 0, false, -1, DataBuffer.TYPE_BYTE);
        BufferedImage img = new BufferedImage(cm, raster, false, null);
        return new TexturePaint(img, new Rectangle(8, 8));
    }

    private static int isSolid(byte[] pPattern) {
        int prev = pPattern[0];

        for (int i = 1; i < pPattern.length; i++) {
            if (prev != pPattern[i]) {
                return 1;
            }
        }

        return prev;
    }

    @Override
    public PaintContext createContext(ColorModel pModel, Rectangle pDeviceBounds, Rectangle2D pUserBounds, AffineTransform pTransform, RenderingHints pHints) {
//        switch (isSolid(pattern)) {
//        }
        return super.createContext(pModel, pDeviceBounds, pUserBounds, pTransform, pHints);
    }

    @Override
    public Pattern derive(final Color foreground, final Color background) {
        if (paint instanceof Color) {
            // TODO: This only holds for patterns that are already foregrounds...
            return new BitMapPattern(foreground);
        }

        return null;
    }
}
