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
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.twelvemonkeys.image;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Random;

/**
 * NoiseFilter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: NoiseFilter.java,v 1.0 15.06.12 22:59 haraldk Exp$
 */
public class NoiseFilter extends AbstractFilter {

    /**
     * Gaussian distribution for the noise.
     */
    public final static int GAUSSIAN = 0;

    /**
     * Uniform distribution for the noise.
     */
    public final static int UNIFORM = 1;

    private int amount = 25;
    private int distribution = UNIFORM;
    private boolean monochrome = false;
    private float density = 1;
    private Random randomNumbers = new Random();

    public NoiseFilter() {
    }

    /**
     * Set the amount of effect.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     * @see #getAmount
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Get the amount of noise.
     *
     * @return the amount
     * @see #setAmount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the distribution of the noise.
     *
     * @param distribution the distribution
     * @see #getDistribution
     */
    public void setDistribution(int distribution) {
        this.distribution = distribution;
    }

    /**
     * Get the distribution of the noise.
     *
     * @return the distribution
     * @see #setDistribution
     */
    public int getDistribution() {
        return distribution;
    }

    /**
     * Set whether to use monochrome noise.
     *
     * @param monochrome true for monochrome noise
     * @see #getMonochrome
     */
    public void setMonochrome(boolean monochrome) {
        this.monochrome = monochrome;
    }

    /**
     * Get whether to use monochrome noise.
     *
     * @return true for monochrome noise
     * @see #setMonochrome
     */
    public boolean getMonochrome() {
        return monochrome;
    }

    /**
     * Set the density of the noise.
     *
     * @param density the density
     * @see #getDensity
     */
    public void setDensity(float density) {
        this.density = density;
    }

    /**
     * Get the density of the noise.
     *
     * @return the density
     * @see #setDensity
     */
    public float getDensity() {
        return density;
    }

    private int random() {
        return (int) (((distribution == GAUSSIAN ? randomNumbers.nextGaussian() : 2 * randomNumbers.nextFloat() - 1)) * amount);
    }

    private static int clamp(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > 0xff) {
            return 0xff;
        }
        return x;
    }

    public int filterRGB(int x, int y, int rgb) {
        if (randomNumbers.nextFloat() <= density) {
            int a = rgb & 0xff000000;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;

            if (monochrome) {
                int n = random();
                r = clamp(r + n);
                g = clamp(g + n);
                b = clamp(b + n);
            }
            else {
                r = clamp(r + random());
                g = clamp(g + random());
                b = clamp(b + random());
            }
            return a | (r << 16) | (g << 8) | b;
        }
        return rgb;
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        int type = src.getType();
        WritableRaster srcRaster = src.getRaster();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        WritableRaster dstRaster = dst.getRaster();

        int[] inPixels = new int[width];
        for (int y = 0; y < height; y++) {
            // We try to avoid calling getRGB on images as it causes them to become unmanaged, causing horrible performance problems.
            if (type == BufferedImage.TYPE_INT_ARGB) {
                srcRaster.getDataElements(0, y, width, 1, inPixels);
                for (int x = 0; x < width; x++) {
                    inPixels[x] = filterRGB(x, y, inPixels[x]);
                }
                dstRaster.setDataElements(0, y, width, 1, inPixels);
            }
            else {
                src.getRGB(0, y, width, 1, inPixels, 0, width);
                for (int x = 0; x < width; x++) {
                    inPixels[x] = filterRGB(x, y, inPixels[x]);
                }
                dst.setRGB(0, y, width, 1, inPixels, 0, width);
            }
        }

        return dst;
    }
}
