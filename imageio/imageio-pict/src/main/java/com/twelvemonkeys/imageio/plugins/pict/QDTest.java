/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.image.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

/**
 * QDTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QDTest.java,v 1.0 Oct 10, 2007 6:06:55 PM haraldk Exp$
 */
public class QDTest {
    public static void main(String[] pArgs) {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        QuickDrawContext context = new QuickDrawContext(g);
        try {
            // Fill background, as Xor don't work with transparent bg
            context.fillRect(new Rectangle(200, 200), QuickDraw.WHITE);

            context.moveTo(10, 10);
            context.lineTo(10, 190);
            context.lineTo(190, 190);
            context.lineTo(190, 10);
            context.lineTo(10, 10);

            context.moveTo(10, 10);
            context.lineTo(190, 190);

            context.setPenSize(new Dimension(2, 2));
            context.frameRect(new Rectangle(15, 15, 20, 20));
            context.paintRect(new Rectangle(15, 45, 20, 20));
            context.fillRect(new Rectangle(15, 75, 20, 20), QuickDraw.DARK_GRAY);
            context.fillRect(new Rectangle(12, 102, 26, 26), new BitMapPattern(Color.GRAY));
            context.eraseRect(new Rectangle(15, 105, 20, 20));
            context.fillRect(new Rectangle(12, 132, 26, 8), QuickDraw.LIGT_GRAY);
            context.fillRect(new Rectangle(12, 140, 26, 10), new BitMapPattern(Color.RED));
            context.fillRect(new Rectangle(12, 150, 26, 8), QuickDraw.DARK_GRAY);
            context.invertRect(new Rectangle(15, 135, 20, 20));

            context.setPenSize(new Dimension(10, 10));
            context.moveTo(80, 30);
            context.line(80, 20);
            context.move(20, 0);
            context.line(0, -25);

            context.setPenPattern(QuickDraw.GRAY);
            context.moveTo(80, 70);
            context.line(80, 20);
            context.move(20, 0);
            context.line(0, -25);

            context.setPenPattern(new BitMapPattern(Color.GRAY));
            context.moveTo(80, 110);
            context.line(80, 20);
            context.move(20, 0);
            context.line(0, -25);

            context.setPenPattern(new BitMapPattern(Color.RED));
            context.moveTo(80, 150);
            context.line(80, 20);
            context.move(20, 0);
            context.line(0, -25);

            context.setPenPattern(new BitMapPattern(Color.ORANGE));
            context.setPenSize(new Dimension(2, 2));
            context.frameRoundRect(new Rectangle(45, 15, 20, 20), 4, 4);
            context.setPenPattern(new BitMapPattern(Color.DARK_GRAY));
            context.paintOval(new Rectangle(45, 45, 20, 20));
            context.invertArc(new Rectangle(45 + 1, 45, 20, 20), 45, 90);
            context.frameArc(new Rectangle(45 - 1, 75, 20, 20), 45, -270);
            context.fillArc(new Rectangle(45 + 1, 75, 20, 20), 45, 90, new BitMapPattern(Color.RED));

            context.invertPoly(new Polygon(new int[]{43, 55, 67}, new int[]{125, 103, 125}, 3));
            context.setPenPattern(new BitMapPattern(Color.ORANGE));
            Polygon star = new Polygon(
                    new int[]{43, 52, 55, 58, 68, 59, 63, 55, 47, 51},
                    new int[]{143, 143, 133, 143, 143, 148, 157, 152, 157, 148},
                    10
            );
            context.paintPoly(star);
            context.setPenNormal();
            context.framePoly(star);
            
            // TODO: FixMe: Seems like rectangle should be INSIDE? Or at least, one pixel less than AWT thinks..
//            context.frameRoundRect(new Rectangle(20, 10, 100, 165), 5, 4);

            context.moveTo(15, 185);
            context.drawString("Java QuickDraw test");
        }
        finally {
            context.closePicture();
        }

        showIt(image, "QuickDraw Test");
    }

    public static void showIt(final BufferedImage pImage, final String pTitle) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame(pTitle);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setLocationByPlatform(true);
                    JPanel pane = new JPanel(new BorderLayout());
                    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                    BufferedImageIcon icon = new BufferedImageIcon(ImageUtil.accelerate(pImage, gc));
                    JScrollPane scroll = new JScrollPane(new JLabel(icon));
                    scroll.setBorder(null);
                    pane.add(scroll);
                    frame.setContentPane(pane);
                    frame.pack();
                    frame.setVisible(true);
                }
            });
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
