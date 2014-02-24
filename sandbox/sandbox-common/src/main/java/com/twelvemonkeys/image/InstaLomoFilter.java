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
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.Random;

/**
 * InstaLomoFilter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: InstaLomoFilter.java,v 1.0 15.06.12 13:24 haraldk Exp$
 */
public class InstaLomoFilter extends AbstractFilter {
    final private Random random = new Random();

    // NOTE: This is a PoC, and not good code...
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        }

        // Make image faded/washed out/red-ish
        // DARK WARM
        float[] scales = new float[] { 2.2f, 2.0f, 1.55f};
        float[] offsets = new float[] {-20.0f, -90.0f, -110.0f};

        // BRIGHT NATURAL
//        float[] scales = new float[] { 1.1f, .9f, .7f};
//        float[] offsets = new float[] {20, 30, 80};

        // Faded, old-style
//        float[] scales = new float[] { 1.1f, .7f, .3f};
//        float[] offsets = new float[] {20, 30, 80};

//        float[] scales = new float[] { 1.2f, .4f, .4f};
//        float[] offsets = new float[] {0, 120, 120};

        // BRIGHT WARM
//        float[] scales = new float[] {1.1f, .8f, 1.6f};
//        float[] offsets = new float[] {60, 70, -80};
        BufferedImage image = new RescaleOp(scales, offsets, getRenderingHints()).filter(src, null);

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

            // Scratches
            g.setComposite(AlphaComposite.SrcOver.derive(.025f));
            for (int i = 0; i < 100; i++) {
                g.setColor(random.nextBoolean() ? Color.WHITE : Color.BLACK);
                g.setStroke(new BasicStroke(random.nextFloat() * 2f));
                int x = random.nextInt(image.getWidth());

                int off = random.nextInt(100);
                for (int j = random.nextInt(3); j > 0; j--) {
                    g.drawLine(x + j, 0, x + off - 50 + j, image.getHeight());
                }
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
            g.setComposite(AlphaComposite.SrcOver.derive(.35f));
            g.setPaint(new RadialGradientPaint(
                    new Point(image.getWidth(), image.getHeight()),
                    Math.max(image.getWidth(), image.getHeight()) * 1.1f,
                    new Point(image.getWidth() / 2, image.getHeight() / 2),
                    new float[] {0, .75f, 1f},
                    new Color[] {new Color(0x00FFFFFF, true), new Color(0x00FFFFFF, true), Color.PINK},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        }
        finally {
            g.dispose();
        }

        // Noise
        NoiseFilter noise = new NoiseFilter();
        noise.setAmount(10);
        noise.setDensity(2);
        dest = noise.filter(dest, dest);

        // Round corners
        BufferedImage foo = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = foo.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            double angle = (random.nextDouble() * .01) - .005;
            graphics.rotate(angle);
            graphics.fillRoundRect(4, 4, image.getWidth() - 8, image.getHeight() - 8, 20, 20);
        }
        finally {
            graphics.dispose();
        }

        noise.setAmount(20);
        noise.setDensity(1);
        noise.setMonochrome(true);
        foo = noise.filter(foo, foo);

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
            if (dest.getTransparency() != Transparency.OPAQUE) {
                g.setComposite(AlphaComposite.Clear);
            }
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(foo, 0, 0, null);
        }
        finally {
            g.dispose();
        }

        return dest;
    }

    public static void main(String[] args) throws IOException {
        exercise(args, new InstaLomoFilter(), Color.WHITE);
    }
}
