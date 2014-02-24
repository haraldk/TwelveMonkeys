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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;

/**
 * AbstractFilter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractFilter.java,v 1.0 18.06.12 16:55 haraldk Exp$
 */
public abstract class AbstractFilter implements BufferedImageOp {
    public abstract BufferedImage filter(BufferedImage src, BufferedImage dest);

    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        throw new UnsupportedOperationException("Method createCompatibleDestImage not implemented"); // TODO: Implement
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle2D.Double(0, 0, src.getWidth(), src.getHeight());
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double();
        }

        dstPt.setLocation(srcPt);

        return dstPt;
    }

    public RenderingHints getRenderingHints() {
        return null;
    }

    protected static void exercise(final String[] args, final BufferedImageOp filter, final Color background) throws IOException {
        boolean original = false;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.equals("-o") || arg.equals("--original")) {
                    original = true;
                }

                continue;
            }

            final File file = new File(arg);
            BufferedImage image = ImageIO.read(file);

            if (image.getWidth() > 640) {
                image = new ResampleOp(640, Math.round(image.getHeight() * (640f / image.getWidth())), null).filter(image, null);
            }

            if (!original) {
                filter.filter(image, image);
            }

            final Color bg = original ? Color.BLACK : background;
            final BufferedImage img = image;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame(filter.getClass().getSimpleName().replace("Filter", "") + "Test: " + file.getName());
                    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(final WindowEvent e) {
                            Window[] windows = Window.getWindows();
                            if (windows == null || windows.length == 0) {
                                System.exit(0);
                            }
                        }
                    });
                    frame.getRootPane().getActionMap().put("window-close", new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            Window window = SwingUtilities.getWindowAncestor((Component) e.getSource());
                            window.setVisible(false);
                            window.dispose();
                        }
                    });
                    frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "window-close");

                    JLabel label = new JLabel(new BufferedImageIcon(img));
                    if (bg != null) {
                        label.setOpaque(true);
                        label.setBackground(bg);
                    }
                    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    JScrollPane scrollPane = new JScrollPane(label);
                    scrollPane.setBorder(BorderFactory.createEmptyBorder());
                    frame.add(scrollPane);

                    frame.pack();
                    frame.setLocationByPlatform(true);
                    frame.setVisible(true);
                }
            });
        }
    }
}
