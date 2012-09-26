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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * ConvolveTester
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/ConvolveTester.java#1 $
 */
public class ConvolveTester {

    // Initial sample timings (avg, 1000 iterations)
    //              PNG, type 0:        JPEG, type 3:
    // ZERO_FILL:    5.4 ms              4.6 ms
    // NO_OP:        5.4 ms              4.6 ms
    // REFLECT:     42.4 ms             24.9 ms
    // WRAP:        86.9 ms             29.5 ms

    final static int ITERATIONS = 1000;

    public static void main(String[] pArgs) throws IOException {
        File input = new File(pArgs[0]);
        BufferedImage image = ImageIO.read(input);
        BufferedImage result = null;

        System.out.println("image: " + image);

        if (pArgs.length > 1) {
            float ammount = Float.parseFloat(pArgs[1]);

            int edgeOp = pArgs.length > 2 ? Integer.parseInt(pArgs[2]) : ImageUtil.EDGE_REFLECT;

            long start = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                result = sharpen(image, ammount, edgeOp);
            }
            long end = System.currentTimeMillis();
            System.out.println("Time: " + ((end - start) / (double) ITERATIONS) + "ms");

            showIt(result, "Sharpened " + ammount + " " + input.getName());
        }
        else {
            showIt(image, "Original " + input.getName());            
        }

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

    static BufferedImage sharpen(BufferedImage pOriginal, final float pAmmount, int pEdgeOp) {
        if (pAmmount == 0f) {
            return pOriginal;
        }

        // Create the convolution matrix
        float[] data = new float[]{
                0.0f, -pAmmount, 0.0f,
                -pAmmount, 4f * pAmmount + 1f, -pAmmount,
                0.0f, -pAmmount, 0.0f
        };

        // Do the filtering
        return ImageUtil.convolve(pOriginal, new Kernel(3, 3, data), pEdgeOp);

    }
}
