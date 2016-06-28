/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Describes an RGB/true color bitmap structure (16, 24 and 32 bits per pixel).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: BitmapRGB.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
class BitmapRGB extends BitmapDescriptor {

    public BitmapRGB(final DirectoryEntry pEntry, final DIBHeader pHeader) {
        super(pEntry, pHeader);
    }

    public BufferedImage getImage() {
        // Test is mask != null rather than hasMask(), as 32 bit (w/alpha)
        // might still have bitmask, but we don't read or use it.
        if (mask != null) {
            image = createMaskedImage();
            mask = null;
        }

        return image;
    }

    private BufferedImage createMaskedImage() {
        BufferedImage masked = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D graphics = masked.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        }
        finally {
            graphics.dispose();
        }

        WritableRaster alphaRaster = masked.getAlphaRaster();

        byte[] trans = {0x0};
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (mask.isTransparent(x, y)) {
                    alphaRaster.setDataElements(x, y, trans);
                }
            }
        }

        return masked;
    }
}
