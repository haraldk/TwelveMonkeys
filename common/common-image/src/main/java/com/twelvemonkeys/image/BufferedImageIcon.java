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

package com.twelvemonkeys.image;

import javax.swing.Icon;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * An {@code Icon} implementation backed by a {@code BufferedImage}.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/BufferedImageIcon.java#2 $
 */
public class BufferedImageIcon implements Icon {
    private final BufferedImage mImage;
    private int mWidth;
    private int mHeight;
    private final boolean mFast;

    public BufferedImageIcon(BufferedImage pImage) {
        this(pImage, pImage.getWidth(), pImage.getHeight());
    }

    public BufferedImageIcon(BufferedImage pImage, int pWidth, int pHeight) {
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }
        if (pWidth <= 0 || pHeight <= 0) {
            throw new IllegalArgumentException("Icon size must be positive");
        }

        mImage = pImage;
        mWidth = pWidth;
        mHeight = pHeight;

        mFast = pImage.getWidth() == mWidth && pImage.getHeight() == mHeight;
    }

    public int getIconHeight() {
        return mHeight;
    }

    public int getIconWidth() {
        return mWidth;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (mFast || !(g instanceof Graphics2D)) {
            //System.out.println("Scaling fast");
            g.drawImage(mImage, x, y, mWidth, mHeight, null);
        }
        else {
            //System.out.println("Scaling using interpolation");
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
            xform.scale(mWidth / (double) mImage.getWidth(), mHeight / (double) mImage.getHeight());
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(mImage, xform, null);
        }
    }
}
