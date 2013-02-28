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
/*
******************************************************************************
*
* ============================================================================
*                   The Apache Software License, Version 1.1
* ============================================================================
*
* Copyright (C) 2000 The Apache Software Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modifica-
* tion, are permitted provided that the following conditions are met:
*
* 1. Redistributions of  source code must  retain the above copyright  notice,
*    this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* 3. The end-user documentation included with the redistribution, if any, must
*    include  the following  acknowledgment:  "This product includes  software
*    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
*    Alternately, this  acknowledgment may  appear in the software itself,  if
*    and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Batik" and  "Apache Software Foundation"  must not be  used to
*    endorse  or promote  products derived  from this  software without  prior
*    written permission. For written permission, please contact
*    apache@apache.org.
*
* 5. Products  derived from this software may not  be called "Apache", nor may
*    "Apache" appear  in their name,  without prior written permission  of the
*    Apache Software Foundation.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
* FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
* APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
* INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
* DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
* ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
* (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
* THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
* This software  consists of voluntary contributions made  by many individuals
* on  behalf  of the Apache Software  Foundation. For more  information on the
* Apache Software Foundation, please see <http://www.apache.org/>.
*
******************************************************************************
*
*/

package com.twelvemonkeys.image;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements an adaptive palette generator to reduce images
 * to a variable number of colors.
 * It can also render images into fixed color pallettes.
 * <p/>
 * Support for the default JVM (ordered/pattern) dither, Floyd-Steinberg like
 * error-diffusion and no dither, controlled by the hints
 * {@link #DITHER_DIFFUSION},
 * {@link #DITHER_NONE} and
 * {@link #DITHER_DEFAULT}.
 * <p/>
 * Color selection speed/accuracy can be controlled using the hints
 * {@link #COLOR_SELECTION_FAST},
 * {@link #COLOR_SELECTION_QUALITY} and
 * {@link #COLOR_SELECTION_DEFAULT}.
 * <p/>
 * Transparency support can be controlled using the hints
 * {@link #TRANSPARENCY_OPAQUE},
 * {@link #TRANSPARENCY_BITMASK} and
 * {@link #TRANSPARENCY_TRANSLUCENT}.
 * <p/>
 * <HR/>
 * <p/>
 * <PRE>
 * This product includes software developed by the Apache Software Foundation.
 * <p/>
 * This software  consists of voluntary contributions made  by many individuals
 * on  behalf  of the Apache Software  Foundation. For more  information on the
 * Apache Software Foundation, please see <A href="http://www.apache.org/">http://www.apache.org/</A>
 * </PRE>
 *
 * @author <A href="mailto:deweese@apache.org">Thomas DeWeese</A>
 * @author <A href="mailto:jun@oop-reserch.com">Jun Inamori</A>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IndexImage.java#1 $
 * @see DiffusionDither
 */
class IndexImage {

    /**
     * Dither mask
     */
    protected final static int DITHER_MASK = 0xFF;

    /**
     * Java default dither
     */
    public final static int DITHER_DEFAULT = 0x00;

    /**
     * No dither
     */
    public final static int DITHER_NONE = 0x01;

    /**
     * Error diffusion dither
     */
    public final static int DITHER_DIFFUSION = 0x02;

    /**
     * Error diffusion dither with alternating scans
     */
    public final static int DITHER_DIFFUSION_ALTSCANS = 0x03;

    /**
     * Color Selection mask
     */
    protected final static int COLOR_SELECTION_MASK = 0xFF00;

    /**
     * Default color selection
     */
    public final static int COLOR_SELECTION_DEFAULT = 0x0000;

    /**
     * Prioritize speed
     */
    public final static int COLOR_SELECTION_FAST = 0x0100;

    /**
     * Prioritize quality
     */
    public final static int COLOR_SELECTION_QUALITY = 0x0200;

    /**
     * Transparency mask
     */
    protected final static int TRANSPARENCY_MASK = 0xFF0000;

    /**
     * Default transparency (none)
     */
    public final static int TRANSPARENCY_DEFAULT = 0x000000;

    /**
     * Discard any alpha information
     */
    public final static int TRANSPARENCY_OPAQUE = 0x010000;

    /**
     * Convert alpha to bitmask
     */
    public final static int TRANSPARENCY_BITMASK = 0x020000;

    /**
     * Keep original alpha (not supported yet)
     */
    protected final static int TRANSPARENCY_TRANSLUCENT = 0x030000;

    /**
     * Used to track a color and the number of pixels of that colors
     */
    private static class Counter {

        /**
         * Field val
         */
        public int val;

        /**
         * Field count
         */
        public int count = 1;

        /**
         * Constructor Counter
         *
         * @param val the initial value
         */
        public Counter(int val) {
            this.val = val;
        }

        /**
         * Method add
         *
         * @param val the new value
         * @return {@code true} if the value was added, otherwise {@code false}
         */
        public boolean add(int val) {
            // See if the value matches us...
            if (this.val != val) {
                return false;
            }

            count++;

            return true;
        }
    }

    /**
     * Used to define a cube of the color space.  The cube can be split
     * approximately in half to generate two cubes.
     */
    private static class Cube {
        int[] min = {0, 0, 0};
        int[] max = {255, 255, 255};
        boolean done = false;
        List<Counter>[] colors = null;
        int count = 0;
        static final int RED = 0;
        static final int GRN = 1;
        static final int BLU = 2;

        /**
         * Define a new cube.
         *
         * @param colors contains the 3D color histogram to be subdivided
         * @param count  the total number of pixels in the 3D histogram.
         */
        public Cube(List<Counter>[] colors, int count) {
            this.colors = colors;
            this.count = count;
        }

        /**
         * If this returns true then the cube can not be subdivided any
         * further
         *
         * @return true if cube can not be subdivided any further
         */
        public boolean isDone() {
            return done;
        }

        /**
         * Splits the cube into two parts.  This cube is
         * changed to be one half and the returned cube is the other half.
         * This tries to pick the right channel to split on.
         *
         * @return the {@code Cube} containing the other half
         */
        public Cube split() {
            int dr = max[0] - min[0] + 1;
            int dg = max[1] - min[1] + 1;
            int db = max[2] - min[2] + 1;
            int c0, c1, splitChannel;

            // Figure out which axis is the longest and split along
            // that axis (this tries to keep cubes square-ish).
            if (dr >= dg) {
                c0 = GRN;
                if (dr >= db) {
                    splitChannel = RED;
                    c1 = BLU;
                }
                else {
                    splitChannel = BLU;
                    c1 = RED;
                }
            }
            else if (dg >= db) {
                splitChannel = GRN;
                c0 = RED;
                c1 = BLU;
            }
            else {
                splitChannel = BLU;
                c0 = RED;
                c1 = GRN;
            }
            
            Cube ret;

            ret = splitChannel(splitChannel, c0, c1);
            
            if (ret != null) {
                return ret;
            }
            
            ret = splitChannel(c0, splitChannel, c1);
            
            if (ret != null) {
                return ret;
            }
            
            ret = splitChannel(c1, splitChannel, c0);
            
            if (ret != null) {
                return ret;
            }
            
            done = true;

            return null;
        }

        /**
         * Splits the image according to the parameters.  It tries
         * to find a location where half the pixels are on one side
         * and half the pixels are on the other.
         *
         * @param splitChannel split channel
         * @param c0 channel 0
         * @param c1 channel 1
         * @return the {@code Cube} containing the other half
         */
        public Cube splitChannel(int splitChannel, int c0, int c1) {
            if (min[splitChannel] == max[splitChannel]) {
                return null;
            }
            int splitSh4 = (2 - splitChannel) * 4;
            int c0Sh4 = (2 - c0) * 4;
            int c1Sh4 = (2 - c1) * 4;

            //            int splitSh8 = (2-splitChannel)*8;
            //            int c0Sh8    = (2-c0)*8;
            //            int c1Sh8    = (2-c1)*8;
            //
            int half = count / 2;

            // Each entry is the number of pixels that have that value
            // in the split channel within the cube (so pixels
            // that have that value in the split channel aren't counted
            // if they are outside the cube in the other color channels.
            int counts[] = new int[256];
            int tcount = 0;

            // System.out.println("Cube: [" +
            //                    min[0] + "-" + max[0] + "] [" +
            //                    min[1] + "-" + max[1] + "] [" +
            //                    min[2] + "-" + max[2] + "]");
            int[] minIdx = {min[0] >> 4, min[1] >> 4, min[2] >> 4};
            int[] maxIdx = {max[0] >> 4, max[1] >> 4, max[2] >> 4};
            int minR = min[0], minG = min[1], minB = min[2];
            int maxR = max[0], maxG = max[1], maxB = max[2];
            int val;
            int[] vals = {0, 0, 0};

            for (int i = minIdx[splitChannel]; i <= maxIdx[splitChannel]; i++) {
                int idx1 = i << splitSh4;

                for (int j = minIdx[c0]; j <= maxIdx[c0]; j++) {
                    int idx2 = idx1 | (j << c0Sh4);

                    for (int k = minIdx[c1]; k <= maxIdx[c1]; k++) {
                        int idx = idx2 | (k << c1Sh4);
                        List<Counter> v = colors[idx];

                        if (v == null) {
                            continue;
                        }
                        
                        for (Counter c : v) {
                            val = c.val;
                            vals[0] = (val & 0xFF0000) >> 16;
                            vals[1] = (val & 0xFF00) >> 8;
                            vals[2] = (val & 0xFF);
                            if (((vals[0] >= minR) && (vals[0] <= maxR)) && ((vals[1] >= minG) && (vals[1] <= maxG))
                                    && ((vals[2] >= minB) && (vals[2] <= maxB))) {

                                // The val lies within this cube so count it.
                                counts[vals[splitChannel]] += c.count;
                                tcount += c.count;
                            }
                        }
                    }
                }

                // We've found the half way point.  Note that the
                // rest of counts is not filled out.
                if (tcount >= half) {
                    break;
                }
            }
            tcount = 0;
            int lastAdd = -1;

            // These indicate what the top value for the low cube and
            // the low value of the high cube should be in the split channel
            // (they may not be one off if there are 'dead' spots in the
            // counts array.)
            int splitLo = min[splitChannel], splitHi = max[splitChannel];

            for (int i = min[splitChannel]; i <= max[splitChannel]; i++) {
                int c = counts[i];

                if (c == 0) {
                    // No counts below this so move up bottom of cube.
                    if ((tcount == 0) && (i < max[splitChannel])) {
                        this.min[splitChannel] = i + 1;
                    }
                    continue;
                }
                if (tcount + c < half) {
                    lastAdd = i;
                    tcount += c;
                    continue;
                }
                if ((half - tcount) <= ((tcount + c) - half)) {
                    // Then lastAdd is a better top idx for this then i.
                    if (lastAdd == -1) {
                        // No lower place to break.
                        if (c == this.count) {

                            // All pixels are at this value so make min/max
                            // reflect that.
                            this.max[splitChannel] = i;
                            return null;// no split to make.
                        }
                        else {

                            // There are values about this one so
                            // split above.
                            splitLo = i;
                            splitHi = i + 1;
                            break;
                        }
                    }
                    splitLo = lastAdd;
                    splitHi = i;
                }
                else {
                    if (i == this.max[splitChannel]) {
                        if (c == this.count) {
                            // would move min up but that should
                            // have happened already.
                            return null;// no split to make.
                        }
                        else {
                            // Would like to break between i and i+1
                            // but no i+1 so use lastAdd and i;
                            splitLo = lastAdd;
                            splitHi = i;
                            break;
                        }
                    }

                    // Include c in counts
                    tcount += c;
                    splitLo = i;
                    splitHi = i + 1;
                }
                break;
            }

            // System.out.println("Split: " + splitChannel + "@"
            //                    + splitLo + "-"+splitHi +
            //                    " Count: " + tcount  + " of " + count +
            //                    " LA: " + lastAdd);
            // Create the new cube and update everyone's bounds & counts.
            Cube ret = new Cube(colors, tcount);

            this.count = this.count - tcount;
            ret.min[splitChannel] = this.min[splitChannel];
            ret.max[splitChannel] = splitLo;
            this.min[splitChannel] = splitHi;
            ret.min[c0] = this.min[c0];
            ret.max[c0] = this.max[c0];
            ret.min[c1] = this.min[c1];
            ret.max[c1] = this.max[c1];
            
            return ret;
        }

        /**
         * Returns the average color for this cube
         *
         * @return the average
         */
        public int averageColor() {
            if (this.count == 0) {
                return 0;
            }
            
            float red = 0, grn = 0, blu = 0;
            int minR = min[0], minG = min[1], minB = min[2];
            int maxR = max[0], maxG = max[1], maxB = max[2];
            int[] minIdx = {minR >> 4, minG >> 4, minB >> 4};
            int[] maxIdx = {maxR >> 4, maxG >> 4, maxB >> 4};
            int val, ired, igrn, iblu;
            float weight;

            for (int i = minIdx[0]; i <= maxIdx[0]; i++) {
                int idx1 = i << 8;

                for (int j = minIdx[1]; j <= maxIdx[1]; j++) {
                    int idx2 = idx1 | (j << 4);

                    for (int k = minIdx[2]; k <= maxIdx[2]; k++) {
                        int idx = idx2 | k;
                        List<Counter> v = colors[idx];

                        if (v == null) {
                            continue;
                        }

                        for (Counter c : v) {
                            val = c.val;
                            ired = (val & 0xFF0000) >> 16;
                            igrn = (val & 0x00FF00) >> 8;
                            iblu = (val & 0x0000FF);
                            
                            if (((ired >= minR) && (ired <= maxR)) && ((igrn >= minG) && (igrn <= maxG)) && ((iblu >= minB) && (iblu <= maxB))) {
                                weight = (c.count / (float) this.count);
                                red += ((float) ired) * weight;
                                grn += ((float) igrn) * weight;
                                blu += ((float) iblu) * weight;
                            }
                        }
                    }
                }
            }

            // System.out.println("RGB: [" + red + ", " +
            //                    grn + ", " + blu + "]");
            return (((int) (red + 0.5f)) << 16 | ((int) (grn + 0.5f)) << 8 | ((int) (blu + 0.5f)));
        }
    }// end Cube

    /**
     * You cannot create this
     */
    private IndexImage() {
    }

    /**
     * @param pImage          the image to get {@code IndexColorModel} from
     * @param pNumberOfColors the number of colors for the {@code IndexColorModel}
     * @param pFast            {@code true} if fast
     * @return an {@code IndexColorModel}
     * @see #getIndexColorModel(Image,int,int)
     *
     * @deprecated Use {@link #getIndexColorModel(Image,int,int)} instead!
     *             This version will be removed in a later version of the API.
     */
    public static IndexColorModel getIndexColorModel(Image pImage, int pNumberOfColors, boolean pFast) {
        return getIndexColorModel(pImage, pNumberOfColors, pFast ? COLOR_SELECTION_FAST : COLOR_SELECTION_QUALITY);
    }

    /**
     * Gets an {@code IndexColorModel} from the given image. If the image has an
     * {@code IndexColorModel}, this will be returned. Otherwise, an {@code IndexColorModel}
     * is created, using an adaptive palette.
     *
     * @param pImage          the image to get {@code IndexColorModel} from
     * @param pNumberOfColors the number of colors for the {@code IndexColorModel}
     * @param pHints          one of {@link #COLOR_SELECTION_FAST},
     *                        {@link #COLOR_SELECTION_QUALITY} or
     *                        {@link #COLOR_SELECTION_DEFAULT}.
     * @return The {@code IndexColorModel} from the given image, or a newly created
     *         {@code IndexColorModel} using an adaptive palette.
     * @throws ImageConversionException if an exception occurred during color
     *                                  model extraction.
     */
    public static IndexColorModel getIndexColorModel(Image pImage, int pNumberOfColors, int pHints) throws ImageConversionException {
        IndexColorModel icm = null;
        RenderedImage image = null;

        if (pImage instanceof RenderedImage) {
            image = (RenderedImage) pImage;
            ColorModel cm = image.getColorModel();

            if (cm instanceof IndexColorModel) {
                // Test if we have right number of colors
                if (((IndexColorModel) cm).getMapSize() <= pNumberOfColors) {
                    //System.out.println("IndexColorModel from BufferedImage");
                    icm = (IndexColorModel) cm;// Done
                }
            }

            // Else create from buffered image, hard way, see below
        }
        else {
            // Create from image using BufferedImageFactory
            BufferedImageFactory factory = new BufferedImageFactory(pImage);
            ColorModel cm = factory.getColorModel();

            if ((cm instanceof IndexColorModel) && ((IndexColorModel) cm).getMapSize() <= pNumberOfColors) {
                //System.out.println("IndexColorModel from Image");
                icm = (IndexColorModel) cm;// Done
            }
            else {
                // Else create from (buffered) image, hard way
                image = factory.getBufferedImage();
            }
        }

        // We now have at least a buffered image, create model from it
        if (icm == null) {
            icm = createIndexColorModel(ImageUtil.toBuffered(image), pNumberOfColors, pHints);
        }
        else if (!(icm instanceof InverseColorMapIndexColorModel)) {
            // If possible, use faster code
            icm = new InverseColorMapIndexColorModel(icm);
        }
        
        return icm;
    }

    /**
     * Creates an {@code IndexColorModel} from the given image, using an adaptive
     * palette.
     *
     * @param pImage          the image to get {@code IndexColorModel} from
     * @param pNumberOfColors the number of colors for the {@code IndexColorModel}
     * @param pHints          use fast mode if possible (might give slightly lower
     *                        quality)
     * @return a new {@code IndexColorModel} created from the given image
     */
    private static IndexColorModel createIndexColorModel(BufferedImage pImage, int pNumberOfColors, int pHints) {
        // TODO: Use ImageUtil.hasTransparentPixels(pImage, true) ||
        // -- haraldK, 20021024, experimental, try to use one transparent pixel
        boolean useTransparency = isTransparent(pHints);

        if (useTransparency) {
            pNumberOfColors--;
        }

        //System.out.println("Transp: " + useTransparency + " colors: " + pNumberOfColors);
        int width = pImage.getWidth();
        int height = pImage.getHeight();

        // Using 4 bits from R, G & B.
        @SuppressWarnings("unchecked")
        List<Counter>[] colors = new List[1 << 12];// [4096]

        // Speedup, doesn't decrease image quality much
        int step = 1;

        if (isFast(pHints)) {
            step += (width * height / 16384);// 128x128px
        }
        int sampleCount = 0;
        int rgb;

        //for (int x = 0; x < width; x++) {
        //for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            for (int y = x % step; y < height; y += step) {
                // Count the number of color samples
                sampleCount++;

                // Get ARGB pixel from image
                rgb = (pImage.getRGB(x, y) & 0xFFFFFF);

                // Get index from high four bits of each component.
                int index = (((rgb & 0xF00000) >>> 12) | ((rgb & 0x00F000) >>> 8) | ((rgb & 0x0000F0) >>> 4));

                // Get the 'hash vector' for that key.
                List<Counter> v = colors[index];

                if (v == null) {
                    // No colors in this bin yet so create vector and
                    // add color.
                    v = new ArrayList<Counter>();
                    v.add(new Counter(rgb));
                    colors[index] = v;
                }
                else {
                    // Find our color in the bin or create a counter for it.
                    Iterator i = v.iterator();

                    while (true) {
                        if (i.hasNext()) {
                            // try adding our color to each counter...
                            if (((Counter) i.next()).add(rgb)) {
                                break;
                            }
                        }
                        else {
                            v.add(new Counter(rgb));
                            break;
                        }
                    }
                }
            }
        }

        // All colours found, reduce to pNumberOfColors
        int numberOfCubes = 1;
        int fCube = 0;
        Cube[] cubes = new Cube[pNumberOfColors];

        cubes[0] = new Cube(colors, sampleCount);

        //cubes[0] = new Cube(colors, width * height);
        while (numberOfCubes < pNumberOfColors) {
            while (cubes[fCube].isDone()) {
                fCube++;
                
                if (fCube == numberOfCubes) {
                    break;
                }
            }
            
            if (fCube == numberOfCubes) {
                break;
            }
            
            Cube cube = cubes[fCube];
            Cube newCube = cube.split();

            if (newCube != null) {
                if (newCube.count > cube.count) {
                    Cube tmp = cube;

                    cube = newCube;
                    newCube = tmp;
                }

                int j = fCube;
                int count = cube.count;

                for (int i = fCube + 1; i < numberOfCubes; i++) {
                    if (cubes[i].count < count) {
                        break;
                    }
                    cubes[j++] = cubes[i];
                }

                cubes[j++] = cube;
                count = newCube.count;

                while (j < numberOfCubes) {
                    if (cubes[j].count < count) {
                        break;
                    }
                    j++;
                }

                System.arraycopy(cubes, j, cubes, j + 1, numberOfCubes - j);

                cubes[j/*++*/] = newCube;
                numberOfCubes++;
            }
        }

        // Create RGB arrays with correct number of colors
        // If we have transparency, the last color will be the transparent one
        byte[] r = new byte[useTransparency ? numberOfCubes + 1 : numberOfCubes];
        byte[] g = new byte[useTransparency ? numberOfCubes + 1 : numberOfCubes];
        byte[] b = new byte[useTransparency ? numberOfCubes + 1 : numberOfCubes];

        for (int i = 0; i < numberOfCubes; i++) {
            int val = cubes[i].averageColor();

            r[i] = (byte) ((val >> 16) & 0xFF);
            g[i] = (byte) ((val >> 8) & 0xFF);
            b[i] = (byte) ((val) & 0xFF);

            //System.out.println("Color [" + i + "]: #" +
            //                   (((val>>16)<16)?"0":"") +
            //                   Integer.toHexString(val));
        }

        // For some reason using less than 8 bits causes a bug in the dither
        //  - transparency added to all totally black colors?
        int numOfBits = 8;

        // -- haraldK, 20021024, as suggested by Thomas E. Deweese
        // plus adding a transparent pixel
        IndexColorModel icm;
        if (useTransparency) {
            icm = new InverseColorMapIndexColorModel(numOfBits, r.length, r, g, b, r.length - 1);
        }
        else {
            icm = new InverseColorMapIndexColorModel(numOfBits, r.length, r, g, b);
        }
        return icm;
    }

    /**
     * Converts the input image (must be {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. Generating an adaptive
     * palette (8 bit) from the color data in the image, and uses default
     * dither.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage the BufferedImage to index and get color information from.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED}, and use an
     *         {@code IndexColorModel}.
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage) {
        return getIndexedImage(pImage, 256, DITHER_DEFAULT);
    }

    /**
     * Tests if the hint {@code COLOR_SELECTION_QUALITY} is <EM>not</EM>
     * set.
     *
     * @param pHints hints
     * @return true if the hint {@code COLOR_SELECTION_QUALITY}
     *         is <EM>not</EM> set.
     */
    private static boolean isFast(int pHints) {
        return (pHints & COLOR_SELECTION_MASK) != COLOR_SELECTION_QUALITY;
    }

    /**
     * Tests if the hint {@code TRANSPARENCY_BITMASK} or
     * {@code TRANSPARENCY_TRANSLUCENT} is set.
     *
     * @param pHints hints
     * @return true if the hint {@code TRANSPARENCY_BITMASK} or
     *         {@code TRANSPARENCY_TRANSLUCENT} is set.
     */
    static boolean isTransparent(int pHints) {
        return (pHints & TRANSPARENCY_BITMASK) != 0 || (pHints & TRANSPARENCY_TRANSLUCENT) != 0;
    }

    /**
     * Converts the input image (must be {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. If the palette image
     * uses an {@code IndexColorModel}, this will be used. Otherwise, generating an
     * adaptive palette (8 bit) from the given palette image.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints}parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage   the BufferedImage to index
     * @param pPalette the Image to read color information from
     * @param pMatte   the background color, used where the original image was
     *                 transparent
     * @param pHints   hints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @throws ImageConversionException if an exception occurred during color
     *                                  model extraction.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, Image pPalette, Color pMatte, int pHints)
            throws ImageConversionException {
        return getIndexedImage(pImage, getIndexColorModel(pPalette, 256, pHints), pMatte, pHints);
    }

    /**
     * Converts the input image (must be  {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. Generating an adaptive
     * palette with the given number of colors.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints}parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage          the BufferedImage to index
     * @param pNumberOfColors the number of colors for the image
     * @param pMatte          the background color, used where the original image was
     *                        transparent
     * @param pHints          hints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, int pNumberOfColors, Color pMatte, int pHints) {
        // NOTE: We need to apply matte before creating color model, otherwise we
        // won't have colors for potential faded transitions
        IndexColorModel icm;

        if (pMatte != null) {
            icm = getIndexColorModel(createSolid(pImage, pMatte), pNumberOfColors, pHints);
        }
        else {
            icm = getIndexColorModel(pImage, pNumberOfColors, pHints);
        }

        // If we found less colors, then no need to dither
        if ((pHints & DITHER_MASK) != DITHER_NONE && (icm.getMapSize() < pNumberOfColors)) {
            pHints = (pHints & ~DITHER_MASK) | DITHER_NONE;
        }
        return getIndexedImage(pImage, icm, pMatte, pHints);
    }

    /**
     * Converts the input image (must be {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. Using the supplied
     * {@code IndexColorModel}'s palette.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints} parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage  the BufferedImage to index
     * @param pColors an {@code IndexColorModel} containing the color information
     * @param pMatte  the background color, used where the original image was
     *                transparent. Also note that any transparent antialias will be
     *                rendered against this color.
     * @param pHints  RenderingHints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, IndexColorModel pColors, Color pMatte, int pHints) {
        // TODO: Consider:
        /*
        if (pImage.getType() == BufferedImage.TYPE_BYTE_INDEXED
            || pImage.getType() == BufferedImage.TYPE_BYTE_BINARY) {
            pImage = ImageUtil.toBufferedImage(pImage, BufferedImage.TYPE_INT_ARGB);
        }
        */

        // Get dimensions
        final int width = pImage.getWidth();
        final int height = pImage.getHeight();

        // Support transparency?
        boolean transparency = isTransparent(pHints) && (pImage.getColorModel().getTransparency() != Transparency.OPAQUE) && (pColors.getTransparency() != Transparency.OPAQUE);

        // Create image with solid background
        BufferedImage solid = pImage;

        if (pMatte != null) { // transparency doesn't really matter
            solid = createSolid(pImage, pMatte);
        }

        BufferedImage indexed;

        // Support TYPE_BYTE_BINARY, but only for 2 bit images, as the default
        // dither does not work with TYPE_BYTE_BINARY it seems...
        if (pColors.getMapSize() > 2) {
            indexed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, pColors);
        }
        else {
            indexed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, pColors);
        }

        // Apply dither if requested
        switch (pHints & DITHER_MASK) {
            case DITHER_DIFFUSION:
            case DITHER_DIFFUSION_ALTSCANS:
                // Create a DiffusionDither to apply dither to indexed
                DiffusionDither dither = new DiffusionDither(pColors);

                if ((pHints & DITHER_MASK) == DITHER_DIFFUSION_ALTSCANS) {
                    dither.setAlternateScans(true);
                }

                dither.filter(solid, indexed);

                break;
            case DITHER_NONE:
                // Just copy pixels, without dither
                // NOTE: This seems to be slower than the method below, using
                // Graphics2D.drawImage, and VALUE_DITHER_DISABLE,
                // however you possibly end up getting a dithered image anyway,
                // therefore, do it slower and produce correct result. :-)
                CopyDither copy = new CopyDither(pColors);
                copy.filter(solid, indexed);

                break;
            case DITHER_DEFAULT:
                // This is the default
            default:
                // Render image data onto indexed image, using default
                // (probably we get dither, but it depends on the GFX engine).
                Graphics2D g2d = indexed.createGraphics();
                try {
                    RenderingHints hints = new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

                    g2d.setRenderingHints(hints);
                    g2d.drawImage(solid, 0, 0, null);
                }
                finally {
                    g2d.dispose();
                }

                break;
        }

        // Transparency support, this approach seems lame, but it's the only
        // solution I've found until now (that actually works).
        if (transparency) {
            // Re-apply the alpha-channel of the original image
            applyAlpha(indexed, pImage);
        }

        // Return the indexed BufferedImage
        return indexed;
    }

    /**
     * Converts the input image (must be  {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. Generating an adaptive
     * palette with the given number of colors.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints}parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage          the BufferedImage to index
     * @param pNumberOfColors the number of colors for the image
     * @param pHints          hints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, int pNumberOfColors, int pHints) {
        return getIndexedImage(pImage, pNumberOfColors, null, pHints);
    }

    /**
     * Converts the input image (must be {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. Using the supplied
     * {@code IndexColorModel}'s palette.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints}parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage  the BufferedImage to index
     * @param pColors an {@code IndexColorModel} containing the color information
     * @param pHints  RenderingHints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, IndexColorModel pColors, int pHints) {
        return getIndexedImage(pImage, pColors, null, pHints);
    }

    /**
     * Converts the input image (must be {@code TYPE_INT_RGB} or
     * {@code TYPE_INT_ARGB}) to an indexed image. If the palette image
     * uses an {@code IndexColorModel}, this will be used. Otherwise, generating an
     * adaptive palette (8 bit) from the given palette image.
     * Dithering, transparency and color selection is controlled with the
     * {@code pHints}parameter.
     * <p/>
     * The image returned is a new image, the input image is not modified.
     *
     * @param pImage   the BufferedImage to index
     * @param pPalette the Image to read color information from
     * @param pHints   hints that control output quality and speed.
     * @return the indexed BufferedImage. The image will be of type
     *         {@code BufferedImage.TYPE_BYTE_INDEXED} or
     *         {@code BufferedImage.TYPE_BYTE_BINARY}, and use an
     *         {@code IndexColorModel}.
     * @see #DITHER_DIFFUSION
     * @see #DITHER_NONE
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see IndexColorModel
     */
    public static BufferedImage getIndexedImage(BufferedImage pImage, Image pPalette, int pHints) {
        return getIndexedImage(pImage, pPalette, null, pHints);
    }

    /**
     * Creates a copy of the given image, with a solid background
     *
     * @param pOriginal   the original image
     * @param pBackground the background color
     * @return a new {@code BufferedImage}
     */
    private static BufferedImage createSolid(BufferedImage pOriginal, Color pBackground) {
        // Create a temporary image of same dimension and type
        BufferedImage solid = new BufferedImage(pOriginal.getColorModel(), pOriginal.copyData(null), pOriginal.isAlphaPremultiplied(), null);
        Graphics2D g = solid.createGraphics();

        try {
            // Clear in background color
            g.setColor(pBackground);
            g.setComposite(AlphaComposite.DstOver);// Paint "underneath"
            g.fillRect(0, 0, pOriginal.getWidth(), pOriginal.getHeight());
        }
        finally {
            g.dispose();
        }

        return solid;
    }

    /**
     * Applies the alpha-component of the alpha image to the given image.
     * The given image is modified in place.
     *
     * @param pImage the image to apply alpha to
     * @param pAlpha the image containing the alpha
     */
    private static void applyAlpha(BufferedImage pImage, BufferedImage pAlpha) {
        // Apply alpha as transparency, using threshold of 25%
        for (int y = 0; y < pAlpha.getHeight(); y++) {
            for (int x = 0; x < pAlpha.getWidth(); x++) {

                // Get alpha component of pixel, if less than 25% opaque
                // (0x40 = 64 => 25% of 256), the pixel will be transparent
                if (((pAlpha.getRGB(x, y) >> 24) & 0xFF) < 0x40) {
                    pImage.setRGB(x, y, 0x00FFFFFF); // 100% transparent
                }
            }
        }
    }

    /*
     * This class is also a command-line utility.
     */
    public static void main(String pArgs[]) {
        // Defaults
        int argIdx = 0;
        int speedTest = -1;
        boolean overWrite = false;
        boolean monochrome = false;
        boolean gray = false;
        int numColors = 256;
        String dither = null;
        String quality = null;
        String format = null;
        Color background = null;
        boolean transparency = false;
        String paletteFileName = null;
        boolean errArgs = false;

        // Parse args
        while ((argIdx < pArgs.length) && (pArgs[argIdx].charAt(0) == '-') && (pArgs[argIdx].length() >= 2)) {
            if ((pArgs[argIdx].charAt(1) == 's') || pArgs[argIdx].equals("--speedtest")) {
                argIdx++;

                // Get number of iterations
                if ((pArgs.length > argIdx) && (pArgs[argIdx].charAt(0) != '-')) {
                    try {
                        speedTest = Integer.parseInt(pArgs[argIdx++]);
                    }
                    catch (NumberFormatException nfe) {
                        errArgs = true;
                        break;
                    }
                }
                else {

                    // Default to 10 iterations
                    speedTest = 10;
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 'w') || pArgs[argIdx].equals("--overwrite")) {
                overWrite = true;
                argIdx++;
            }
            else if ((pArgs[argIdx].charAt(1) == 'c') || pArgs[argIdx].equals("--colors")) {
                argIdx++;

                try {
                    numColors = Integer.parseInt(pArgs[argIdx++]);
                }
                catch (NumberFormatException nfe) {
                    errArgs = true;
                    break;
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 'g') || pArgs[argIdx].equals("--grayscale")) {
                argIdx++;
                gray = true;
            }
            else if ((pArgs[argIdx].charAt(1) == 'm') || pArgs[argIdx].equals("--monochrome")) {
                argIdx++;
                numColors = 2;
                monochrome = true;
            }
            else if ((pArgs[argIdx].charAt(1) == 'd') || pArgs[argIdx].equals("--dither")) {
                argIdx++;
                dither = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'p') || pArgs[argIdx].equals("--palette")) {
                argIdx++;
                paletteFileName = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'q') || pArgs[argIdx].equals("--quality")) {
                argIdx++;
                quality = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'b') || pArgs[argIdx].equals("--bgcolor")) {
                argIdx++;
                try {
                    background = StringUtil.toColor(pArgs[argIdx++]);
                }
                catch (Exception e) {
                    errArgs = true;
                    break;
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 't') || pArgs[argIdx].equals("--transparency")) {
                argIdx++;
                transparency = true;
            }
            else if ((pArgs[argIdx].charAt(1) == 'f') || pArgs[argIdx].equals("--outputformat")) {
                argIdx++;
                format = StringUtil.toLowerCase(pArgs[argIdx++]);
            }
            else if ((pArgs[argIdx].charAt(1) == 'h') || pArgs[argIdx].equals("--help")) {
                argIdx++;

                // Setting errArgs to true, to print usage
                errArgs = true;
            }
            else {
                System.err.println("Unknown option \"" + pArgs[argIdx++] + "\"");
            }
        }
        if (errArgs || (pArgs.length < (argIdx + 1))) {
            System.err.println("Usage: IndexImage [--help|-h] [--speedtest|-s <integer>] [--bgcolor|-b <color>] [--colors|-c <integer> | --grayscale|g | --monochrome|-m | --palette|-p <file>] [--dither|-d (default|diffusion|none)] [--quality|-q (default|high|low)] [--transparency|-t] [--outputformat|-f (gif|jpeg|png|wbmp|...)] [--overwrite|-w] <input> [<output>]");
            System.err.print("Input format names: ");
            String[] readers = ImageIO.getReaderFormatNames();

            for (int i = 0; i < readers.length; i++) {
                System.err.print(readers[i] + ((i + 1 < readers.length)
                        ? ", "
                        : "\n"));
            }

            System.err.print("Output format names: ");
            String[] writers = ImageIO.getWriterFormatNames();

            for (int i = 0; i < writers.length; i++) {
                System.err.print(writers[i] + ((i + 1 < writers.length)
                        ? ", "
                        : "\n"));
            }
            System.exit(5);
        }

        // Read in image
        File in = new File(pArgs[argIdx++]);

        if (!in.exists()) {
            System.err.println("File \"" + in.getAbsolutePath() + "\" does not exist!");
            System.exit(5);
        }

        // Read palette if needed
        File paletteFile = null;

        if (paletteFileName != null) {
            paletteFile = new File(paletteFileName);
            if (!paletteFile.exists()) {
                System.err.println("File \"" + in.getAbsolutePath() + "\" does not exist!");
                System.exit(5);
            }
        }

        // Make sure we can write
        File out;

        if (argIdx < pArgs.length) {
            out = new File(pArgs[argIdx/*++*/]);

            // Get format from file extension
            if (format == null) {
                format = FileUtil.getExtension(out);
            }
        }
        else {
            // Create new file in current dir, same name + format extension
            String baseName = FileUtil.getBasename(in);

            // Use png as default format
            if (format == null) {
                format = "png";
            }
            out = new File(baseName + '.' + format);
        }

        if (!overWrite && out.exists()) {
            System.err.println("The file \"" + out.getAbsolutePath() + "\" allready exists!");
            System.exit(5);
        }

        // Do the image processing
        BufferedImage image = null;
        BufferedImage paletteImg = null;

        try {
            image = ImageIO.read(in);
            if (image == null) {
                System.err.println("No reader for image: \"" + in.getAbsolutePath() + "\"!");
                System.exit(5);
            }
            if (paletteFile != null) {
                paletteImg = ImageIO.read(paletteFile);
                if (paletteImg == null) {
                    System.err.println("No reader for image: \"" + paletteFile.getAbsolutePath() + "\"!");
                    System.exit(5);
                }
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            System.exit(5);
        }

        // Create hints
        int hints = DITHER_DEFAULT;

        if ("DIFFUSION".equalsIgnoreCase(dither)) {
            hints |= DITHER_DIFFUSION;
        }
        else if ("DIFFUSION_ALTSCANS".equalsIgnoreCase(dither)) {
            hints |= DITHER_DIFFUSION_ALTSCANS;
        }
        else if ("NONE".equalsIgnoreCase(dither)) {
            hints |= DITHER_NONE;
        }
        else {

            // Don't care, use default
        }
        if ("HIGH".equalsIgnoreCase(quality)) {
            hints |= COLOR_SELECTION_QUALITY;
        }
        else if ("LOW".equalsIgnoreCase(quality)) {
            hints |= COLOR_SELECTION_FAST;
        }
        else {

            // Don't care, use default
        }
        if (transparency) {
            hints |= TRANSPARENCY_BITMASK;
        }

        //////////////////////////////
        // Apply bg-color WORKAROUND!
        // This needs to be done BEFORE palette creation to have desired effect..
        if ((background != null) && (paletteImg == null)) {
            paletteImg = createSolid(image, background);
        }

        ///////////////////////////////
        // Index
        long start = 0;

        if (speedTest > 0) {
            // SPEED TESTING
            System.out.println("Measuring speed!");
            start = System.currentTimeMillis();
            // END SPEED TESTING
        }

        BufferedImage indexed;
        IndexColorModel colors;

        if (monochrome) {
            indexed = getIndexedImage(image, MonochromeColorModel.getInstance(), background, hints);
            colors = MonochromeColorModel.getInstance();
        }
        else if (gray) {
            //indexed = ImageUtil.toBuffered(ImageUtil.grayscale(image), BufferedImage.TYPE_BYTE_GRAY);
            image = ImageUtil.toBuffered(ImageUtil.grayscale(image));
            indexed = getIndexedImage(image, colors = getIndexColorModel(image, numColors, hints), background, hints);

            // In casse of speedtest, this makes sense...
            if (speedTest > 0) {
                colors = getIndexColorModel(indexed, numColors, hints);
            }
        }
        else if (paletteImg != null) {
            // Get palette from image
            indexed = getIndexedImage(ImageUtil.toBuffered(image, BufferedImage.TYPE_INT_ARGB),
                                      colors = getIndexColorModel(paletteImg, numColors, hints), background, hints);
        }
        else {
            image = ImageUtil.toBuffered(image, BufferedImage.TYPE_INT_ARGB);
            indexed = getIndexedImage(image, colors = getIndexColorModel(image, numColors, hints), background, hints);
        }

        if (speedTest > 0) {
            // SPEED TESTING
            System.out.println("Color selection + dither: " + (System.currentTimeMillis() - start) + " ms");
            // END SPEED TESTING
        }

        // Write output (in given format)
        try {
            if (!ImageIO.write(indexed, format, out)) {
                System.err.println("No writer for format: \"" + format + "\"!");
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }

        if (speedTest > 0) {
            // SPEED TESTING
            System.out.println("Measuring speed!");

            // Warmup!
            for (int i = 0; i < 10; i++) {
                getIndexedImage(image, colors, background, hints);
            }

            // Measure
            long time = 0;

            for (int i = 0; i < speedTest; i++) {
                start = System.currentTimeMillis();
                getIndexedImage(image, colors, background, hints);
                time += (System.currentTimeMillis() - start);
                System.out.print('.');
                if ((i + 1) % 10 == 0) {
                    System.out.println("\nAverage (after " + (i + 1) + " iterations): " + (time / (i + 1)) + "ms");
                }
            }

            System.out.println("\nDither only:");
            System.out.println("Total time (" + speedTest + " invocations): " + time + "ms");
            System.out.println("Average: " + time / speedTest + "ms");
            // END SPEED TESTING
        }
    }
}