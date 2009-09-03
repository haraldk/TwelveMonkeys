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
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * SubsampleTester
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/SubsampleTester.java#1 $
 */
public class SubsampleTester {

    // Initial testing shows we need at least 9 pixels (sampleFactor == 3) to make a good looking image..
    // Also, using Lanczos is much better than (and allmost as fast as) halving using AffineTransform
    // - But I guess those numbers depend on the data type of the input image... 

    public static void main(String[] pArgs) throws IOException {
        // To/from larger than or equal to 4x4
        //ImageUtil.createResampled(new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB), 4, 4, BufferedImage.SCALE_SMOOTH);
        //ImageUtil.createResampled(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), 5, 5, BufferedImage.SCALE_SMOOTH);

        // To/from smaller than or equal to 4x4 with fast scale
        //ImageUtil.createResampled(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), 10, 10, BufferedImage.SCALE_FAST);
        //ImageUtil.createResampled(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB), 3, 3, BufferedImage.SCALE_FAST);

        // To/from smaller than or equal to 4x4 with default scale
        //ImageUtil.createResampled(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), 10, 10, BufferedImage.SCALE_DEFAULT);
        //ImageUtil.createResampled(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB), 3, 3, BufferedImage.SCALE_DEFAULT);

        // To/from smaller than or equal to 4x4 with smooth scale
        try {
            ImageUtil.createResampled(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), 10, 10, BufferedImage.SCALE_SMOOTH);
        }
        catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        //try {
        //    ImageUtil.createResampled(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB), 3, 3, BufferedImage.SCALE_SMOOTH);
        //}
        //catch (IndexOutOfBoundsException e) {
        //    e.printStackTrace();
        //    return;
        //}

        File input = new File(pArgs[0]);
        ImageInputStream stream = ImageIO.createImageInputStream(input);

        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
        if (readers.hasNext()) {
            if (stream == null) {
                return;
            }
            ImageReader reader = readers.next();
            reader.setInput(stream);

            ImageReadParam param = reader.getDefaultReadParam();

            for (int i = 0; i < 25; i++) {
                //readImage(pArgs, reader, param);
            }

            long start = System.currentTimeMillis();

            BufferedImage image = readImage(pArgs, reader, param);

            long end = System.currentTimeMillis();

            System.out.println("elapsed time: " + (end - start) + " ms");

            int subX = param.getSourceXSubsampling();
            int subY = param.getSourceYSubsampling();

            System.out.println("image: " + image);

            //ImageIO.write(image, "png", new File(input.getParentFile(), input.getName().replace('.', '_') + "_new.png"));

            ConvolveTester.showIt(image, input.getName() + (subX > 1 || subY > 1 ? " (subsampled " + subX + " by " + subY + ")" : ""));
        }
        else {
            System.err.println("No reader found for input: " + input.getAbsolutePath());
        }
    }

    private static BufferedImage readImage(final String[] pArgs, final ImageReader pReader, final ImageReadParam pParam) throws IOException {
        double sampleFactor; // Minimum number of samples (in each dimension) pr pixel in output

        int width = pArgs.length > 1 ? Integer.parseInt(pArgs[1]) : 300;
        int height = pArgs.length > 2 ? Integer.parseInt(pArgs[2]) : 200;

        if (pArgs.length > 3 && (sampleFactor = Double.parseDouble(pArgs[3])) > 0) {
            int originalWidth = pReader.getWidth(0);
            int originalHeight = pReader.getHeight(0);

            System.out.println("originalWidth: " + originalWidth);
            System.out.println("originalHeight: " + originalHeight);

            int subX = (int) Math.max(originalWidth / (double) (width * sampleFactor), 1.0);
            int subY = (int) Math.max(originalHeight / (double) (height * sampleFactor), 1.0);

            if (subX > 1 || subY > 1) {
                System.out.println("subX: " + subX);
                System.out.println("subY: " + subY);
                pParam.setSourceSubsampling(subX, subY, subX > 1 ? subX / 2 : 0, subY > 1 ? subY / 2 : 0);
            }
        }

        BufferedImage image = pReader.read(0, pParam);

        System.out.println("image: " + image);

        int algorithm = BufferedImage.SCALE_DEFAULT;
        if (pArgs.length > 4) {
            if ("smooth".equals(pArgs[4].toLowerCase())) {
                algorithm = BufferedImage.SCALE_SMOOTH;
            }
            else if ("fast".equals(pArgs[4].toLowerCase())) {
                algorithm = BufferedImage.SCALE_FAST;
            }
        }        

        if (image.getWidth() != width || image.getHeight() != height) {
            image = ImageUtil.createScaled(image, width, height, algorithm);
        }

        return image;
    }
}
