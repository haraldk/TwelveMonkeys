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

package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.Random;

/**
 * InstaCRTFilter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: InstaCRTFilter.java,v 1.0 15.06.12 13:24 haraldk Exp$
 */
public class InstaCRTFilter extends AbstractFilter {

    // NOTE: This is a PoC, and not good code...
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        }

        // Make grayscale
        BufferedImage image = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), getRenderingHints()).filter(src, null);

        // Make image faded/too bright
        image = new RescaleOp(1.2f, 120f, getRenderingHints()).filter(image, image);

        // Blur
        image = ImageUtil.blur(image, 2.5f);

        Graphics2D g = dest.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.drawImage(image, 0, 0, null);

            // Rotate it slightly for a more analogue feeling
            double angle = .0055;
            g.rotate(angle);

            // Apply fake green-ish h-sync line at random position
            Random random = new Random();
            int lineStart = random.nextInt(image.getHeight() - 80);
            int lineHeight = random.nextInt(10) + 20;

            g.setComposite(AlphaComposite.SrcOver.derive(.3f));
            g.setPaint(new LinearGradientPaint(
                    0, lineStart, 0, lineStart + lineHeight,
                    new float[] {0, .3f, .9f, 1},
                    new Color[] {new Color(0, true), new Color(0x90AF66), new Color(0x99606F33, true), new Color(0, true)}
            ));
            g.fillRect(0, lineStart, image.getWidth(), lineHeight);

            // Apply fake large dot-pitch (black lines w/transparency)
            g.setComposite(AlphaComposite.SrcOver.derive(.55f));
            g.setColor(Color.BLACK);

            for (int y = 0; y < image.getHeight(); y += 3) {
                g.setStroke(new BasicStroke(random.nextFloat() / 3 + .8f));
                g.drawLine(0, y, image.getWidth(), y);
            }

            // Vignette/border
            g.setComposite(AlphaComposite.SrcOver.derive(.75f));
            int focus = Math.min(image.getWidth() / 8, image.getHeight() / 8);
            g.setPaint(new RadialGradientPaint(
                    new Point(image.getWidth() / 2, image.getHeight() / 2),
                    Math.max(image.getWidth(), image.getHeight()) / 1.6f,
                    new Point(focus, focus),
                    new float[] {0, .3f, .9f, 1f},
                    new Color[] {new Color(0x99FFFFFF, true), new Color(0x00FFFFFF, true), new Color(0x0, true), Color.BLACK},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g.fillRect(-2, -2, image.getWidth() + 4, image.getHeight() + 4);

            g.rotate(-angle);

            g.setComposite(AlphaComposite.SrcOver.derive(.35f));
            g.setPaint(new RadialGradientPaint(
                    new Point(image.getWidth() / 2, image.getHeight() / 2),
                    Math.max(image.getWidth(), image.getHeight()) / 1.65f,
                    new Point(image.getWidth() / 2, image.getHeight() / 2),
                    new float[] {0, .85f, 1f},
                    new Color[] {new Color(0x0, true), new Color(0x0, true), Color.BLACK},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            // Highlight
            g.setComposite(AlphaComposite.SrcOver.derive(.55f));
            g.setPaint(new RadialGradientPaint(
                    new Point(image.getWidth(), image.getHeight()),
                    Math.max(image.getWidth(), image.getHeight()) * 1.1f,
                    new Point(image.getWidth() / 2, image.getHeight() / 2),
                    new float[] {0, .75f, 1f},
                    new Color[] {new Color(0x00FFFFFF, true), new Color(0x00FFFFFF, true), Color.WHITE},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        }
        finally {
            g.dispose();
        }

        // Round corners
        BufferedImage foo = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = foo.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            double angle = -0.04;
            g.rotate(angle);
            graphics.fillRoundRect(1, 1, image.getWidth() - 2, image.getHeight() - 2, 20, 20);
        }
        finally {
            graphics.dispose();
        }

        foo = ImageUtil.blur(foo, 4.5f);

        // Compose image into rounded corners
        graphics = foo.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcIn);
            graphics.drawImage(dest, 0, 0, null);
        }
        finally {
            graphics.dispose();
        }

        // Draw it all back to dest
        g = dest.createGraphics();
        try {
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(foo, 0, 0, null);
        }
        finally {
            g.dispose();
        }

        return dest;
    }

    public static void main(String[] args) throws IOException {
        exercise(args, new InstaCRTFilter(), Color.BLACK);
    }
}
