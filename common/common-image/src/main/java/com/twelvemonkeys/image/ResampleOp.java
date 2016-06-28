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
 *******************************************************************************
 *
 *  Based on example code found in Graphics Gems III, Filtered Image Rescaling
 *  (filter_rcg.c), available from http://www.acm.org/tog/GraphicsGems/.
 *
 *  Public Domain 1991 by Dale Schumacher. Mods by Ray Gardener
 *
 *  Original by Dale Schumacher (fzoom)
 *
 *  Additional changes by Ray Gardener, Daylon Graphics Ltd.
 *  December 4, 1999
 *
 *******************************************************************************
 *
 *  Aditional changes inspired by ImageMagick's resize.c.
 *
 *******************************************************************************
 *
 *  Java port and additional changes/bugfixes by Harald Kuhr, Twelvemonkeys.
 *  February 20, 2006
 *
 *******************************************************************************
 */

package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * Resamples (scales) a {@code BufferedImage} to a new width and height, using
 * high performance and high quality algorithms.
 * Several different interpolation algorithms may be specifed in the
 * constructor, either using the
 * <a href="#field_summary">filter type constants</a>, or one of the
 * {@code RendereingHints}.
 * <p/>
 * For fastest results, use {@link #FILTER_POINT} or {@link #FILTER_BOX}.
 * In most cases, {@link #FILTER_TRIANGLE} will produce acceptable results, while
 * being relatively fast.
 * For higher quality output, use more sophisticated interpolation algorithms,
 * like {@link #FILTER_MITCHELL} or {@link #FILTER_LANCZOS}.
 * <p/>
 * Example:
 * <blockquote><pre>
 * BufferedImage image;
 * <p/>
 * //...
 * <p/>
 * ResampleOp resampler = new ResampleOp(100, 100, ResampleOp.FILTER_TRIANGLE);
 * BufferedImage thumbnail = resampler.filter(image, null);
 * </pre></blockquote>
 * <p/>
 * If your imput image is very large, it's possible to first resample using the
 * very fast {@code FILTER_POINT} algorithm, then resample to the wanted size,
 * using a higher quality algorithm:
 * <blockquote><pre>
 * BufferedImage verylLarge;
 * <p/>
 * //...
 * <p/>
 * int w = 300;
 * int h = 200;
 * <p/>
 * BufferedImage temp = new ResampleOp(w * 2, h * 2, FILTER_POINT).filter(verylLarge, null);
 * <p/>
 * BufferedImage scaled = new ResampleOp(w, h).filter(temp, null);
 * </pre></blockquote>
 * <p/>
 * For maximum performance, this class will use native code, through
 * <a href="http://www.yeo.id.au/jmagick/">JMagick</a>, when available.
 * Otherwise, the class will silently fall back to pure Java mode.
 * Native code may be disabled globally, by setting the system property
 * {@code com.twelvemonkeys.image.accel} to {@code false}.
 * To allow debug of the native code, set the system property
 * {@code com.twelvemonkeys.image.magick.debug} to {@code true}.
 * <p/>
 * This {@code BufferedImageOp} is based on C example code found in
 * <a href="http://www.acm.org/tog/GraphicsGems/">Graphics Gems III</a>,
 * Filtered Image Rescaling, by Dale Schumacher (with additional improvments by
 * Ray Gardener).
 * Additional changes are inspired by
 * <a href="http://www.imagemagick.org/">ImageMagick</a> and
 * Marco Schmidt's <a href="http://schmidt.devlib.org/jiu/">Java Imaging Utilities</a>
 * (which are also adaptions of the same original code from Graphics Gems III).
 * <p/>
 * For a description of the various interpolation algorithms, see
 * <em>General Filtered Image Rescaling</em> in <em>Graphics Gems III</em>,
 * Academic Press, 1994.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/ResampleOp.java#1 $
 * @see #ResampleOp(int,int,int)
 * @see #ResampleOp(int,int,java.awt.RenderingHints)
 * @see BufferedImage
 * @see RenderingHints
 * @see AffineTransformOp
 */
// TODO: Consider using AffineTransformOp for more operations!?
public class ResampleOp implements BufferedImageOp/* TODO: RasterOp */ {

    // NOTE: These MUST correspond to ImageMagick filter types, for the
    // MagickAccelerator to work consistently (see magick.FilterType).

    /**
     * Undefined interpolation, filter method will use default filter.
     */
    public final static int FILTER_UNDEFINED = 0;
    /**
     * Point interpolation (also known as "nearest neighbour").
     * Very fast, but low quality
     * (similar to {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
     * and {@link Image#SCALE_REPLICATE}).
     */
    public final static int FILTER_POINT = 1;
    /**
     * Box interpolation. Fast, but low quality.
     */
    public final static int FILTER_BOX = 2;
    /**
     * Triangle interpolation (also known as "linear" or "bilinear").
     * Quite fast, with acceptable quality
     * (similar to {@link RenderingHints#VALUE_INTERPOLATION_BILINEAR} and
     * {@link Image#SCALE_AREA_AVERAGING}).
     */
    public final static int FILTER_TRIANGLE = 3;
    /**
     * Hermite interpolation.
     */
    public final static int FILTER_HERMITE = 4;
    /**
     * Hanning interpolation.
     */
    public final static int FILTER_HANNING = 5;
    /**
     * Hamming interpolation.
     */
    public final static int FILTER_HAMMING = 6;
    /**
     * Blackman interpolation..
     */
    public final static int FILTER_BLACKMAN = 7;
    /**
     * Gaussian interpolation.
     */
    public final static int FILTER_GAUSSIAN = 8;
    /**
     * Quadratic interpolation.
     */
    public final static int FILTER_QUADRATIC = 9;
    /**
     * Cubic interpolation.
     */
    public final static int FILTER_CUBIC = 10;
    /**
     * Catrom interpolation.
     */
    public final static int FILTER_CATROM = 11;
    /**
     * Mitchell interpolation. High quality.
     */
    public final static int FILTER_MITCHELL = 12; // IM default scale with palette or alpha, or scale up
    /**
     * Lanczos interpolation. High quality.
     */
    public final static int FILTER_LANCZOS = 13; // IM default
    /**
     * Blackman-Bessel interpolation. High quality.
     */
    public final static int FILTER_BLACKMAN_BESSEL = 14;
    /**
     * Blackman-Sinc interpolation. High quality.
     */
    public final static int FILTER_BLACKMAN_SINC = 15;

    /**
     * RenderingHints.Key specifying resampling interpolation algorithm.
     */
    public final static RenderingHints.Key KEY_RESAMPLE_INTERPOLATION = new Key("ResampleInterpolation");

    /**
     * @see #FILTER_POINT
     */
    public final static Object VALUE_INTERPOLATION_POINT =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Point", FILTER_POINT);
    /**
     * @see #FILTER_BOX
     */
    public final static Object VALUE_INTERPOLATION_BOX =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Box", FILTER_BOX);
    /**
     * @see #FILTER_TRIANGLE
     */
    public final static Object VALUE_INTERPOLATION_TRIANGLE =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Triangle", FILTER_TRIANGLE);
    /**
     * @see #FILTER_HERMITE
     */
    public final static Object VALUE_INTERPOLATION_HERMITE =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Hermite", FILTER_HERMITE);
    /**
     * @see #FILTER_HANNING
     */
    public final static Object VALUE_INTERPOLATION_HANNING =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Hanning", FILTER_HANNING);
    /**
     * @see #FILTER_HAMMING
     */
    public final static Object VALUE_INTERPOLATION_HAMMING =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Hamming", FILTER_HAMMING);
    /**
     * @see #FILTER_BLACKMAN
     */
    public final static Object VALUE_INTERPOLATION_BLACKMAN =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Blackman", FILTER_BLACKMAN);
    /**
     * @see #FILTER_GAUSSIAN
     */
    public final static Object VALUE_INTERPOLATION_GAUSSIAN =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Gaussian", FILTER_GAUSSIAN);
    /**
     * @see #FILTER_QUADRATIC
     */
    public final static Object VALUE_INTERPOLATION_QUADRATIC =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Quadratic", FILTER_QUADRATIC);
    /**
     * @see #FILTER_CUBIC
     */
    public final static Object VALUE_INTERPOLATION_CUBIC =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Cubic", FILTER_CUBIC);
    /**
     * @see #FILTER_CATROM
     */
    public final static Object VALUE_INTERPOLATION_CATROM =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Catrom", FILTER_CATROM);
    /**
     * @see #FILTER_MITCHELL
     */
    public final static Object VALUE_INTERPOLATION_MITCHELL =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Mitchell", FILTER_MITCHELL);
    /**
     * @see #FILTER_LANCZOS
     */
    public final static Object VALUE_INTERPOLATION_LANCZOS =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Lanczos", FILTER_LANCZOS);
    /**
     * @see #FILTER_BLACKMAN_BESSEL
     */
    public final static Object VALUE_INTERPOLATION_BLACKMAN_BESSEL =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Blackman-Bessel", FILTER_BLACKMAN_BESSEL);
    /**
     * @see #FILTER_BLACKMAN_SINC
     */
    public final static Object VALUE_INTERPOLATION_BLACKMAN_SINC =
            new Value(KEY_RESAMPLE_INTERPOLATION, "Blackman-Sinc", FILTER_BLACKMAN_SINC);

    // Member variables
    // Package access, to allow access from MagickAccelerator
    int width;
    int height;

    int filterType;

    /**
     * RendereingHints.Key implementation, works only with Value values.
     */
    // TODO: Move to abstract class AbstractBufferedImageOp?
    static class Key extends RenderingHints.Key {
        static int sIndex = 10000;

        private final String name;

        public Key(final String pName) {
            super(sIndex++);
            name = pName;
        }

        public boolean isCompatibleValue(Object pValue) {
            return pValue instanceof Value && ((Value) pValue).isCompatibleKey(this);
        }

        public String toString() {
            return name;
        }
    }

    /**
     * RenderingHints value implementation, works with Key keys.
     */
    // TODO: Extract abstract Value class, and move to AbstractBufferedImageOp
    static final class Value {
        final private RenderingHints.Key key;
        final private String name;
        final private int type;

        public Value(final RenderingHints.Key pKey, final String pName, final int pType) {
            key = pKey;
            name = pName;
            type = validateFilterType(pType);
        }

        public boolean isCompatibleKey(Key pKey) {
            return pKey == key;
        }

        public int getFilterType() {
            return type;
        }

        public String toString() {
            return name;
        }
    }

    /**
     * Creates a {@code ResampleOp} that will resample input images to the
     * given width and height, using the default interpolation filter.
     *
     * @param width  width of the re-sampled image
     * @param height height of the re-sampled image
     */
    public ResampleOp(int width, int height) {
        this(width, height, FILTER_UNDEFINED);
    }

    /**
     * Creates a {@code ResampleOp} that will resample input images to the
     * given width and height, using the interpolation filter specified by
     * the given hints.
     * If using {@code RenderingHints}, the hints are mapped as follows:
     * <ul>
     * <li>{@code KEY_RESAMPLE_INTERPOLATION} takes precedence over any
     * standard {@code java.awt} hints, and dictates interpolation
     * directly, see
     * <a href="#field_summary">{@code RenderingHints} constants</a>.</li>
     * <p/>
     * <li>{@code KEY_INTERPOLATION} takes precedence over other hints.
     * <ul>
     * <li>{@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR} specifies
     * {@code FILTER_POINT}</li>
     * <li>{@link RenderingHints#VALUE_INTERPOLATION_BILINEAR} specifies
     * {@code FILTER_TRIANGLE}</li>
     * <li>{@link RenderingHints#VALUE_INTERPOLATION_BICUBIC} specifies
     * {@code FILTER_QUADRATIC}</li>
     * </ul>
     * </li>
     * <p/>
     * <li>{@code KEY_RENDERING} or {@code KEY_COLOR_RENDERING}
     * <ul>
     * <li>{@link RenderingHints#VALUE_RENDER_SPEED} specifies
     * {@code FILTER_POINT}</li>
     * <li>{@link RenderingHints#VALUE_RENDER_QUALITY} specifies
     * {@code FILTER_MITCHELL}</li>
     * </ul>
     * </li>
     * </ul>
     * Other hints have no effect on this filter.
     *
     * @param width  width of the re-sampled image
     * @param height height of the re-sampled image
     * @param hints  rendering hints, affecting interpolation algorithm
     * @see #KEY_RESAMPLE_INTERPOLATION
     * @see RenderingHints#KEY_INTERPOLATION
     * @see RenderingHints#KEY_RENDERING
     * @see RenderingHints#KEY_COLOR_RENDERING
     */
    public ResampleOp(int width, int height, RenderingHints hints) {
        this(width, height, getFilterType(hints));
    }

    /**
     * Creates a {@code ResampleOp} that will resample input images to the
     * given width and height, using the given interpolation filter.
     *
     * @param width      width of the re-sampled image
     * @param height     height of the re-sampled image
     * @param filterType interpolation filter algorithm
     * @see <a href="#field_summary">filter type constants</a>
     */
    public ResampleOp(int width, int height, int filterType) {
        if (width <= 0 || height <= 0) {
            // NOTE: w/h == 0 makes the Magick DLL crash and the JVM dies.. :-P
            throw new IllegalArgumentException("width and height must be positive");
        }

        this.width = width;
        this.height = height;

        this.filterType = validateFilterType(filterType);
    }

    private static int validateFilterType(int pFilterType) {
        switch (pFilterType) {
            case FILTER_UNDEFINED:
            case FILTER_POINT:
            case FILTER_BOX:
            case FILTER_TRIANGLE:
            case FILTER_HERMITE:
            case FILTER_HANNING:
            case FILTER_HAMMING:
            case FILTER_BLACKMAN:
            case FILTER_GAUSSIAN:
            case FILTER_QUADRATIC:
            case FILTER_CUBIC:
            case FILTER_CATROM:
            case FILTER_MITCHELL:
            case FILTER_LANCZOS:
            case FILTER_BLACKMAN_BESSEL:
            case FILTER_BLACKMAN_SINC:
                return pFilterType;
            default:
                throw new IllegalArgumentException("Unknown filter type: " + pFilterType);
        }
    }

    /**
     * Gets the filter type specified by the given hints.
     *
     * @param pHints rendering hints
     * @return a filter type constant
     */
    private static int getFilterType(RenderingHints pHints) {
        if (pHints == null) {
            return FILTER_UNDEFINED;
        }

        if (pHints.containsKey(KEY_RESAMPLE_INTERPOLATION)) {
            Object value = pHints.get(KEY_RESAMPLE_INTERPOLATION);
            // NOTE: Workaround for a bug in RenderingHints constructor (Bug id# 5084832)
            if (!KEY_RESAMPLE_INTERPOLATION.isCompatibleValue(value)) {
                throw new IllegalArgumentException(value + " incompatible with key " + KEY_RESAMPLE_INTERPOLATION);
            }
            return value != null ? ((Value) value).getFilterType() : FILTER_UNDEFINED;
        }
        else if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR.equals(pHints.get(RenderingHints.KEY_INTERPOLATION))
                || (!pHints.containsKey(RenderingHints.KEY_INTERPOLATION)
                && (RenderingHints.VALUE_RENDER_SPEED.equals(pHints.get(RenderingHints.KEY_RENDERING))
                || RenderingHints.VALUE_COLOR_RENDER_SPEED.equals(pHints.get(RenderingHints.KEY_COLOR_RENDERING))))) {
            // Nearest neighbour, or prioritize speed
            return FILTER_POINT;
        }
        else if (RenderingHints.VALUE_INTERPOLATION_BILINEAR.equals(pHints.get(RenderingHints.KEY_INTERPOLATION))) {
            // Triangle equals bi-linear interpolation
            return FILTER_TRIANGLE;
        }
        else if (RenderingHints.VALUE_INTERPOLATION_BICUBIC.equals(pHints.get(RenderingHints.KEY_INTERPOLATION))) {
            return FILTER_QUADRATIC;// No idea if this is correct..?
        }
        else if (RenderingHints.VALUE_RENDER_QUALITY.equals(pHints.get(RenderingHints.KEY_RENDERING))
                || RenderingHints.VALUE_COLOR_RENDER_QUALITY.equals(pHints.get(RenderingHints.KEY_COLOR_RENDERING))) {
            // Prioritize quality
            return FILTER_MITCHELL;
        }

        // NOTE: Other hints are ignored
        return FILTER_UNDEFINED;
    }

    /**
     * Re-samples (scales) the image to the size, and using the algorithm
     * specified in the constructor.
     *
     * @param input  The {@code BufferedImage} to be filtered
     * @param output The {@code BufferedImage} in which to store the resampled
     *                image
     * @return The re-sampled {@code BufferedImage}.
     * @throws NullPointerException     if {@code input} is {@code null}
     * @throws IllegalArgumentException if {@code input == output}.
     * @see #ResampleOp(int,int,int)
     */
    public final BufferedImage filter(final BufferedImage input, final BufferedImage output) {
        if (input == null) {
            throw new NullPointerException("Input == null");
        }
        if (input == output) {
            throw new IllegalArgumentException("Output image cannot be the same as the input image");
        }

        InterpolationFilter filter;

        // Special case for POINT, TRIANGLE and QUADRATIC filter, as standard
        // Java implementation is very fast (possibly H/W accelerated)
        switch (filterType) {
            case FILTER_POINT:
                if (input.getType() != BufferedImage.TYPE_CUSTOM) {
                    return fastResample(input, output, width, height, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                }
                // Else fall through
            case FILTER_TRIANGLE:
                if (input.getType() != BufferedImage.TYPE_CUSTOM) {
                    return fastResample(input, output, width, height, AffineTransformOp.TYPE_BILINEAR);
                }
                // Else fall through
            case FILTER_QUADRATIC:
                if (input.getType() != BufferedImage.TYPE_CUSTOM) {
                    return fastResample(input, output, width, height, AffineTransformOp.TYPE_BICUBIC);
                }
                // Else fall through
            default:
                filter = createFilter(filterType);
                // NOTE: Workaround for filter throwing exceptions when input or output is less than support...
                if (Math.min(input.getWidth(), input.getHeight()) <= filter.support() || Math.min(width, height) <= filter.support()) {
                    return fastResample(input, output, width, height, AffineTransformOp.TYPE_BILINEAR);
                }
                // Fall through
        }

        // Try to use native ImageMagick code
        BufferedImage result = MagickAccelerator.filter(this, input, output);
        if (result != null) {
            return result;
        }

        // Otherwise, continue in pure Java mode

        // TODO: What if output != null and wrong size? Create new? Render on only a part? Document?

        // If filter type != POINT or BOX and input has IndexColorModel, convert
        // to true color, with alpha reflecting that of the original color model.
        BufferedImage temp;
        ColorModel cm;
        if (filterType != FILTER_POINT && filterType != FILTER_BOX && (cm = input.getColorModel()) instanceof IndexColorModel) {
            // TODO: OPTIMIZE: If color model has only b/w or gray, we could skip color info
            temp = ImageUtil.toBuffered(input, cm.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
        }
        else {
            temp = input;
        }

        // Create or convert output to a suitable image
        // TODO: OPTIMIZE: Don't really need to convert all types to same as input
        result = output != null && temp.getType() != BufferedImage.TYPE_CUSTOM ? /*output*/ ImageUtil.toBuffered(output, temp.getType()) : createCompatibleDestImage(temp, null);

        resample(temp, result, filter);

        // If output != null and needed to be converted, draw it back
        if (output != null && output != result) {
            //output.setData(output.getRaster());
            ImageUtil.drawOnto(output, result);
            result = output;
        }

        return result;
    }

    /*
    private static BufferedImage pointResample(final BufferedImage pInput, final BufferedImage pOutput, final int pWidth, final int pHeight) {
        double xScale = pWidth / (double) pInput.getWidth();
        double yScale = pHeight / (double) pInput.getHeight();

        // NOTE: This is extremely fast, native, possibly H/W accelerated code
        AffineTransform transform = AffineTransform.getScaleInstance(xScale, yScale);
        AffineTransformOp scale = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return scale.filter(pInput, pOutput);
    }
    */

    /*
    // TODO: This idea from Chet and Romain is actually not too bad..
    // It reuses the image/raster/graphics...
    // However, they don't end with a halve operation..
    private static BufferedImage getFasterScaledInstance(BufferedImage img,
            int targetWidth, int targetHeight, Object hint,
            boolean progressiveBilinear) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        int w, h;
        int prevW = ret.getWidth();
        int prevH = ret.getHeight();
        boolean isTranslucent = img.getTransparency() !=  Transparency.OPAQUE;

        if (progressiveBilinear) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // Use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;

            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        if (g2 != null) {
            g2.dispose();
        }

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;
    }
    */

    private static BufferedImage fastResample(final BufferedImage input, final BufferedImage output, final int width, final int height, final int type) {
        BufferedImage temp = input;

        double xScale;
        double yScale;

        AffineTransform transform;
        AffineTransformOp scale;

        if (type > AffineTransformOp.TYPE_NEAREST_NEIGHBOR) {
            // Initially scale so all remaining operations will halve the image
            if (width < input.getWidth() || height < input.getHeight()) {
                int w = width;
                int h = height;
                while (w < input.getWidth() / 2) {
                    w *= 2;
                }
                while (h < input.getHeight() / 2) {
                    h *= 2;
                }

                xScale = w / (double) input.getWidth();
                yScale = h / (double) input.getHeight();

                //System.out.println("First scale by x=" + xScale + ", y=" + yScale);

                transform = AffineTransform.getScaleInstance(xScale, yScale);
                scale = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
                temp = scale.filter(temp, null);
            }
        }

        scale = null; // NOTE: This resets!

        xScale = width / (double) temp.getWidth();
        yScale = height / (double) temp.getHeight();

        if (type > AffineTransformOp.TYPE_NEAREST_NEIGHBOR) {
            // TODO: Test skipping first scale (above), and instead scale once
            // more here, and a little less than .5 each time...
            // That would probably make the scaling smoother...
            while (xScale < 0.5 || yScale < 0.5) {
                if (xScale >= 0.5) {
                    //System.out.println("Halving by y=" + (yScale * 2.0));
                    transform = AffineTransform.getScaleInstance(1.0, 0.5);
                    scale = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);

                    yScale *= 2.0;
                }
                else if (yScale >= 0.5) {
                    //System.out.println("Halving by x=" + (xScale * 2.0));
                    transform = AffineTransform.getScaleInstance(0.5, 1.0);
                    scale = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);

                    xScale *= 2.0;
                }
                else {
                    //System.out.println("Halving by x=" + (xScale * 2.0)  + ", y=" + (yScale * 2.0));
                    xScale *= 2.0;
                    yScale *= 2.0;
                }

                if (scale == null) {
                    transform = AffineTransform.getScaleInstance(0.5, 0.5);
                    scale = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
                }

                temp = scale.filter(temp, null);
            }
        }

        //System.out.println("Rest to scale by x=" + xScale + ", y=" + yScale);

        transform = AffineTransform.getScaleInstance(xScale, yScale);
        scale = new AffineTransformOp(transform, type);

        return scale.filter(temp, output);
    }

    /**
     * Returns the current filter type constant.
     *
     * @return the current filter type constant.
     * @see <a href="#field_summary">filter type constants</a>
     */
    public int getFilterType() {
        return filterType;
    }

    private static InterpolationFilter createFilter(int pFilterType) {
        // TODO: Select correct filter based on scale up or down, if undefined!
        if (pFilterType == FILTER_UNDEFINED) {
            pFilterType = FILTER_LANCZOS;
        }

        switch (pFilterType) {
            case FILTER_POINT:
                return new PointFilter();
            case FILTER_BOX:
                return new BoxFilter();
            case FILTER_TRIANGLE:
                return new TriangleFilter();
            case FILTER_HERMITE:
                return new HermiteFilter();
            case FILTER_HANNING:
                return new HanningFilter();
            case FILTER_HAMMING:
                return new HammingFilter();
            case FILTER_BLACKMAN:
                return new BlacmanFilter();
            case FILTER_GAUSSIAN:
                return new GaussianFilter();
            case FILTER_QUADRATIC:
                return new QuadraticFilter();
            case FILTER_CUBIC:
                return new CubicFilter();
            case FILTER_CATROM:
                return new CatromFilter();
            case FILTER_MITCHELL:
                return new MitchellFilter();
            case FILTER_LANCZOS:
                return new LanczosFilter();
            case FILTER_BLACKMAN_BESSEL:
                return new BlackmanBesselFilter();
            case FILTER_BLACKMAN_SINC:
                return new BlackmanSincFilter();
            default:
                throw new IllegalStateException("Unknown filter type: " + pFilterType);
        }
    }

    public final BufferedImage createCompatibleDestImage(final BufferedImage pInput, final ColorModel pModel) {
        if (pInput == null) {
            throw new NullPointerException("pInput == null");
        }

        ColorModel cm = pModel != null ? pModel : pInput.getColorModel();

        // TODO: Might not work with all colormodels..
        // If indexcolormodel, we probably don't want to use that...
        // NOTE: Either BOTH or NONE of the images must have ALPHA

        return new BufferedImage(cm, ImageUtil.createCompatibleWritableRaster(pInput, cm, width, height),
                                 cm.isAlphaPremultiplied(), null);
    }

    public RenderingHints getRenderingHints() {
        Object value;
        switch (filterType) {
            case FILTER_UNDEFINED:
                return null;
            case FILTER_POINT:
                value = VALUE_INTERPOLATION_POINT;
                break;
            case FILTER_BOX:
                value = VALUE_INTERPOLATION_BOX;
                break;
            case FILTER_TRIANGLE:
                value = VALUE_INTERPOLATION_TRIANGLE;
                break;
            case FILTER_HERMITE:
                value = VALUE_INTERPOLATION_HERMITE;
                break;
            case FILTER_HANNING:
                value = VALUE_INTERPOLATION_HANNING;
                break;
            case FILTER_HAMMING:
                value = VALUE_INTERPOLATION_HAMMING;
                break;
            case FILTER_BLACKMAN:
                value = VALUE_INTERPOLATION_BLACKMAN;
                break;
            case FILTER_GAUSSIAN:
                value = VALUE_INTERPOLATION_GAUSSIAN;
                break;
            case FILTER_QUADRATIC:
                value = VALUE_INTERPOLATION_QUADRATIC;
                break;
            case FILTER_CUBIC:
                value = VALUE_INTERPOLATION_CUBIC;
                break;
            case FILTER_CATROM:
                value = VALUE_INTERPOLATION_CATROM;
                break;
            case FILTER_MITCHELL:
                value = VALUE_INTERPOLATION_MITCHELL;
                break;
            case FILTER_LANCZOS:
                value = VALUE_INTERPOLATION_LANCZOS;
                break;
            case FILTER_BLACKMAN_BESSEL:
                value = VALUE_INTERPOLATION_BLACKMAN_BESSEL;
                break;
            case FILTER_BLACKMAN_SINC:
                value = VALUE_INTERPOLATION_BLACKMAN_SINC;
                break;
            default:
                throw new IllegalStateException("Unknown filter type: " + filterType);
        }

        return new RenderingHints(KEY_RESAMPLE_INTERPOLATION, value);
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(width, height);
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        // TODO: This is wrong...
        // How can I possible know how much one point is scaled, without first knowing the ration?!
        // TODO: Maybe set all points outside of bounds, inside?
        // TODO: Assume input image of Integer.MAX_VAL x Integer.MAX_VAL?! ;-)
        if (dstPt == null) {
            if (srcPt instanceof Point2D.Double) {
                dstPt = new Point2D.Double();
            }
            else {
                dstPt = new Point2D.Float();
            }
            dstPt.setLocation(srcPt);
        }
        return dstPt;
    }

    /* -- Java port of filter_rcg.c below... -- */

    /*
    *	filter function definitions
    */

    interface InterpolationFilter {
        double filter(double t);

        double support();
    }

    static class HermiteFilter implements InterpolationFilter {
        public final double filter(double t) {
            /* f(t) = 2|t|^3 - 3|t|^2 + 1, -1 <= t <= 1 */
            if (t < 0.0) {
                t = -t;
            }
            if (t < 1.0) {
                return (2.0 * t - 3.0) * t * t + 1.0;
            }
            return 0.0;
        }

        public final double support() {
            return 1.0;
        }
    }

    static class PointFilter extends BoxFilter {
        public PointFilter() {
            super(0.0);
        }
    }

    static class BoxFilter implements InterpolationFilter {
        private final double mSupport;

        public BoxFilter() {
            mSupport = 0.5;
        }

        protected BoxFilter(double pSupport) {
            mSupport = pSupport;
        }

        public final double filter(final double t) {
            //if ((t > -0.5) && (t <= 0.5)) {
            if ((t >= -0.5) && (t < 0.5)) {// ImageMagick resample.c
                return 1.0;
            }
            return 0.0;
        }

        public final double support() {
            return mSupport;
        }
    }

    static class TriangleFilter implements InterpolationFilter {
        public final double filter(double t) {
            if (t < 0.0) {
                t = -t;
            }
            if (t < 1.0) {
                return 1.0 - t;
            }
            return 0.0;
        }

        public final double support() {
            return 1.0;
        }
    }

    static class QuadraticFilter implements InterpolationFilter {
        // AKA Bell
        public final double filter(double t)/* box (*) box (*) box */ {
            if (t < 0) {
                t = -t;
            }
            if (t < .5) {
                return .75 - (t * t);
            }
            if (t < 1.5) {
                t = (t - 1.5);
                return .5 * (t * t);
            }
            return 0.0;
        }

        public final double support() {
            return 1.5;
        }
    }

    static class CubicFilter implements InterpolationFilter {
        // AKA B-Spline
        public final double filter(double t)/* box (*) box (*) box (*) box */ {
            final double tt;

            if (t < 0) {
                t = -t;
            }
            if (t < 1) {
                tt = t * t;
                return (.5 * tt * t) - tt + (2.0 / 3.0);
            }
            else if (t < 2) {
                t = 2 - t;
                return (1.0 / 6.0) * (t * t * t);
            }
            return 0.0;
        }

        public final double support() {
            return 2.0;
        }
    }

    private static double sinc(double x) {
        x *= Math.PI;
        if (x != 0.0) {
            return Math.sin(x) / x;
        }
        return 1.0;
    }

    static class LanczosFilter implements InterpolationFilter {
        // AKA Lanczos3
        public final double filter(double t) {
            if (t < 0) {
                t = -t;
            }
            if (t < 3.0) {
                return sinc(t) * sinc(t / 3.0);
            }
            return 0.0;
        }

        public final double support() {
            return 3.0;
        }
    }

    private final static double B = 1.0 / 3.0;
    private final static double C = 1.0 / 3.0;
    private final static double P0 = (6.0 - 2.0 * B) / 6.0;
    private final static double P2 = (-18.0 + 12.0 * B + 6.0 * C) / 6.0;
    private final static double P3 = (12.0 - 9.0 * B - 6.0 * C) / 6.0;
    private final static double Q0 = (8.0 * B + 24.0 * C) / 6.0;
    private final static double Q1 = (-12.0 * B - 48.0 * C) / 6.0;
    private final static double Q2 = (6.0 * B + 30.0 * C) / 6.0;
    private final static double Q3 = (-1.0 * B - 6.0 * C) / 6.0;

    static class MitchellFilter implements InterpolationFilter {
        public final double filter(double t) {
            if (t < -2.0) {
                return 0.0;
            }
            if (t < -1.0) {
                return Q0 - t * (Q1 - t * (Q2 - t * Q3));
            }
            if (t < 0.0) {
                return P0 + t * t * (P2 - t * P3);
            }
            if (t < 1.0) {
                return P0 + t * t * (P2 + t * P3);
            }
            if (t < 2.0) {
                return Q0 + t * (Q1 + t * (Q2 + t * Q3));
            }
            return 0.0;
        }

        public final double support() {
            return 2.0;
        }
    }

    private static double j1(final double t) {
        final double[] pOne = {
                0.581199354001606143928050809e+21,
                -0.6672106568924916298020941484e+20,
                0.2316433580634002297931815435e+19,
                -0.3588817569910106050743641413e+17,
                0.2908795263834775409737601689e+15,
                -0.1322983480332126453125473247e+13,
                0.3413234182301700539091292655e+10,
                -0.4695753530642995859767162166e+7,
                0.270112271089232341485679099e+4
        };
        final double[] qOne = {
                0.11623987080032122878585294e+22,
                0.1185770712190320999837113348e+20,
                0.6092061398917521746105196863e+17,
                0.2081661221307607351240184229e+15,
                0.5243710262167649715406728642e+12,
                0.1013863514358673989967045588e+10,
                0.1501793594998585505921097578e+7,
                0.1606931573481487801970916749e+4,
                0.1e+1
        };

        double p = pOne[8];
        double q = qOne[8];
        for (int i = 7; i >= 0; i--) {
            p = p * t * t + pOne[i];
            q = q * t * t + qOne[i];
        }
        return p / q;
    }

    private static double p1(final double t) {
        final double[] pOne = {
                0.352246649133679798341724373e+5,
                0.62758845247161281269005675e+5,
                0.313539631109159574238669888e+5,
                0.49854832060594338434500455e+4,
                0.2111529182853962382105718e+3,
                0.12571716929145341558495e+1
        };
        final double[] qOne = {
                0.352246649133679798068390431e+5,
                0.626943469593560511888833731e+5,
                0.312404063819041039923015703e+5,
                0.4930396490181088979386097e+4,
                0.2030775189134759322293574e+3,
                0.1e+1
        };

        double p = pOne[5];
        double q = qOne[5];
        for (int i = 4; i >= 0; i--) {
            p = p * (8.0 / t) * (8.0 / t) + pOne[i];
            q = q * (8.0 / t) * (8.0 / t) + qOne[i];
        }
        return p / q;
    }

    private static double q1(final double t) {
        final double[] pOne = {
                0.3511751914303552822533318e+3,
                0.7210391804904475039280863e+3,
                0.4259873011654442389886993e+3,
                0.831898957673850827325226e+2,
                0.45681716295512267064405e+1,
                0.3532840052740123642735e-1
        };
        final double[] qOne = {
                0.74917374171809127714519505e+4,
                0.154141773392650970499848051e+5,
                0.91522317015169922705904727e+4,
                0.18111867005523513506724158e+4,
                0.1038187585462133728776636e+3,
                0.1e+1
        };

        double p = pOne[5];
        double q = qOne[5];
        for (int i = 4; i >= 0; i--) {
            p = p * (8.0 / t) * (8.0 / t) + pOne[i];
            q = q * (8.0 / t) * (8.0 / t) + qOne[i];
        }
        return p / q;
    }

    static double besselOrderOne(double t) {
        double p, q;

        if (t == 0.0) {
            return 0.0;
        }
        p = t;
        if (t < 0.0) {
            t = -t;
        }
        if (t < 8.0) {
            return p * j1(t);
        }
        q = Math.sqrt(2.0 / (Math.PI * t)) * (p1(t) * (1.0 / Math.sqrt(2.0) * (Math.sin(t) - Math.cos(t))) - 8.0 / t * q1(t) *
                (-1.0 / Math.sqrt(2.0) * (Math.sin(t) + Math.cos(t))));
        if (p < 0.0) {
            q = -q;
        }
        return q;
    }

    private static double bessel(final double t) {
        if (t == 0.0) {
            return Math.PI / 4.0;
        }
        return besselOrderOne(Math.PI * t) / (2.0 * t);
    }

    private static double blackman(final double t) {
        return 0.42 + 0.50 * Math.cos(Math.PI * t) + 0.08 * Math.cos(2.0 * Math.PI * t);
    }

    static class BlacmanFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return blackman(t);
        }

        public final double support() {
            return 1.0;
        }
    }

    static class CatromFilter implements InterpolationFilter {
        public final double filter(double t) {
            if (t < 0) {
                t = -t;
            }
            if (t < 1.0) {
                return 0.5 * (2.0 + t * t * (-5.0 + t * 3.0));
            }
            if (t < 2.0) {
                return 0.5 * (4.0 + t * (-8.0 + t * (5.0 - t)));
            }
            return 0.0;
        }

        public final double support() {
            return 2.0;
        }
    }

    static class GaussianFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return Math.exp(-2.0 * t * t) * Math.sqrt(2.0 / Math.PI);
        }

        public final double support() {
            return 1.25;
        }
    }

    static class HanningFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return 0.5 + 0.5 * Math.cos(Math.PI * t);
        }

        public final double support() {
            return 1.0;
        }
    }

    static class HammingFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return 0.54 + 0.46 * Math.cos(Math.PI * t);
        }

        public final double support() {
            return 1.0;
        }
    }

    static class BlackmanBesselFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return blackman(t / support()) * bessel(t);
        }

        public final double support() {
            return 3.2383;
        }
    }

    static class BlackmanSincFilter implements InterpolationFilter {
        public final double filter(final double t) {
            return blackman(t / support()) * sinc(t);
        }

        public final double support() {
            return 4.0;
        }
    }

    /*
    *	image rescaling routine
    */
    class Contributor {
        int pixel;
        double weight;
    }

    class ContributorList {
        int n;/* number of contributors (may be < p.length) */
        Contributor[] p;/* pointer to list of contributions */
    }

    /*
        round()

        Round an FP value to its closest int representation.
        General routine; ideally belongs in general math lib file.
    */

    static int round(double d) {
        // NOTE: This code seems to be faster than Math.round(double)...
        // Version that uses no function calls at all.
        int n = (int) d;
        double diff = d - (double) n;
        if (diff < 0) {
            diff = -diff;
        }
        if (diff >= 0.5) {
            if (d < 0) {
                n--;
            }
            else {
                n++;
            }
        }
        return n;
    }/* round */

    /*
        calcXContrib()

        Calculates the filter weights for a single target column.
        contribX->p must be freed afterwards.

        Returns -1 if error, 0 otherwise.
    */
    private ContributorList calcXContrib(double xscale, double fwidth, int srcwidth, InterpolationFilter pFilter, int i) {
        // TODO: What to do when fwidth > srcwidyj or dstwidth

        double width;
        double fscale;
        double center;
        double weight;

        ContributorList contribX = new ContributorList();

        if (xscale < 1.0) {
            /* Shrinking image */
            width = fwidth / xscale;
            fscale = 1.0 / xscale;

            if (width <= .5) {
                // Reduce to point sampling.
                width = .5 + 1.0e-6;
                fscale = 1.0;
            }

            //contribX.n = 0;
            contribX.p = new Contributor[(int) (width * 2.0 + 1.0 + 0.5)];

            center = (double) i / xscale;
            int left = (int) Math.ceil(center - width);// Note: Assumes width <= .5
            int right = (int) Math.floor(center + width);

            double density = 0.0;

            for (int j = left; j <= right; j++) {
                weight = center - (double) j;
                weight = pFilter.filter(weight / fscale) / fscale;
                int n;
                if (j < 0) {
                    n = -j;
                }
                else if (j >= srcwidth) {
                    n = (srcwidth - j) + srcwidth - 1;
                }
                else {
                    n = j;
                }

                /**/
                if (n >= srcwidth) {
                    n = n % srcwidth;
                }
                else if (n < 0) {
                    n = srcwidth - 1;
                }
                /**/

                int k = contribX.n++;
                contribX.p[k] = new Contributor();
                contribX.p[k].pixel = n;
                contribX.p[k].weight = weight;

                density += weight;

            }

            if ((density != 0.0) && (density != 1.0)) {
                //Normalize.
                density = 1.0 / density;
                for (int k = 0; k < contribX.n; k++) {
                    contribX.p[k].weight *= density;
                }
            }
        }
        else {
            /* Expanding image */
            //contribX.n = 0;
            contribX.p = new Contributor[(int) (fwidth * 2.0 + 1.0 + 0.5)];

            center = (double) i / xscale;
            int left = (int) Math.ceil(center - fwidth);
            int right = (int) Math.floor(center + fwidth);

            for (int j = left; j <= right; j++) {
                weight = center - (double) j;
                weight = pFilter.filter(weight);

                int n;
                if (j < 0) {
                    n = -j;
                }
                else if (j >= srcwidth) {
                    n = (srcwidth - j) + srcwidth - 1;
                }
                else {
                    n = j;
                }

                /**/
                if (n >= srcwidth) {
                    n = n % srcwidth;
                }
                else if (n < 0) {
                    n = srcwidth - 1;
                }
                /**/

                int k = contribX.n++;
                contribX.p[k] = new Contributor();
                contribX.p[k].pixel = n;
                contribX.p[k].weight = weight;
            }
        }
        return contribX;
    }/* calcXContrib */

    /*
        resample()

        Resizes bitmaps while resampling them.
    */
    private BufferedImage resample(BufferedImage pSource, BufferedImage pDest, InterpolationFilter pFilter) {
        final int dstWidth = pDest.getWidth();
        final int dstHeight = pDest.getHeight();

        final int srcWidth = pSource.getWidth();
        final int srcHeight = pSource.getHeight();

        /* create intermediate column to hold horizontal dst column zoom */
        final ColorModel cm = pSource.getColorModel();
//        final WritableRaster work = cm.createCompatibleWritableRaster(1, srcHeight);
        final WritableRaster work = ImageUtil.createCompatibleWritableRaster(pSource, cm, 1, srcHeight);

        double xscale = (double) dstWidth / (double) srcWidth;
        double yscale = (double) dstHeight / (double) srcHeight;

        ContributorList[] contribY = new ContributorList[dstHeight];
        for (int i = 0; i < contribY.length; i++) {
            contribY[i] = new ContributorList();
        }

        // TODO: What to do when fwidth > srcHeight or dstHeight
        double fwidth = pFilter.support();
        if (yscale < 1.0) {
            double width = fwidth / yscale;
            double fscale = 1.0 / yscale;

            if (width <= .5) {
                // Reduce to point sampling.
                width = .5 + 1.0e-6;
                fscale = 1.0;
            }

            for (int i = 0; i < dstHeight; i++) {
                //contribY[i].n = 0;
                contribY[i].p = new Contributor[(int) (width * 2.0 + 1 + 0.5)];

                double center = (double) i / yscale;
                int left = (int) Math.ceil(center - width);
                int right = (int) Math.floor(center + width);

                double density = 0.0;

                for (int j = left; j <= right; j++) {
                    double weight = center - (double) j;
                    weight = pFilter.filter(weight / fscale) / fscale;
                    int n;
                    if (j < 0) {
                        n = -j;
                    }
                    else if (j >= srcHeight) {
                        n = (srcHeight - j) + srcHeight - 1;
                    }
                    else {
                        n = j;
                    }

                    /**/
                    if (n >= srcHeight) {
                        n = n % srcHeight;
                    }
                    else if (n < 0) {
                        n = srcHeight - 1;
                    }
                    /**/

                    int k = contribY[i].n++;
                    contribY[i].p[k] = new Contributor();
                    contribY[i].p[k].pixel = n;
                    contribY[i].p[k].weight = weight;

                    density += weight;
                }

                if ((density != 0.0) && (density != 1.0)) {
                    //Normalize.
                    density = 1.0 / density;
                    for (int k = 0; k < contribY[i].n; k++) {
                        contribY[i].p[k].weight *= density;
                    }
                }
            }
        }
        else {
            for (int i = 0; i < dstHeight; ++i) {
                //contribY[i].n = 0;
                contribY[i].p = new Contributor[(int) (fwidth * 2 + 1 + 0.5)];

                double center = (double) i / yscale;
                double left = Math.ceil(center - fwidth);
                double right = Math.floor(center + fwidth);
                for (int j = (int) left; j <= right; ++j) {
                    double weight = center - (double) j;
                    weight = pFilter.filter(weight);
                    int n;
                    if (j < 0) {
                        n = -j;
                    }
                    else if (j >= srcHeight) {
                        n = (srcHeight - j) + srcHeight - 1;
                    }
                    else {
                        n = j;
                    }

                    /**/
                    if (n >= srcHeight) {
                        n = n % srcHeight;
                    }
                    else if (n < 0) {
                        n = srcHeight - 1;
                    }
                    /**/

                    int k = contribY[i].n++;
                    contribY[i].p[k] = new Contributor();
                    contribY[i].p[k].pixel = n;
                    contribY[i].p[k].weight = weight;
                }
            }
        }

        final Raster raster = pSource.getRaster();
        final WritableRaster out = pDest.getRaster();

        // TODO: This is not optimal for non-byte-packed rasters...
        // (What? Maybe I implemented the fix, but forgot to remove the TODO?)
        final int numChannels = raster.getNumBands();
        final int[] channelMax = new int[numChannels];
        for (int k = 0; k < numChannels; k++) {
            channelMax[k] = (1 << pSource.getColorModel().getComponentSize(k)) - 1;
        }

        for (int xx = 0; xx < dstWidth; xx++) {
            ContributorList contribX = calcXContrib(xscale, fwidth, srcWidth, pFilter, xx);
            /* Apply horiz filter to make dst column in tmp. */
            for (int k = 0; k < srcHeight; k++) {
                for (int channel = 0; channel < numChannels; channel++) {

                    double weight = 0.0;
                    boolean bPelDelta = false;
                    // TODO: This line throws index out of bounds, if the image
                    // is smaller than filter.support()
                    double pel = raster.getSample(contribX.p[0].pixel, k, channel);
                    for (int j = 0; j < contribX.n; j++) {
                        double pel2 = j == 0 ? pel : raster.getSample(contribX.p[j].pixel, k, channel);
                        if (pel2 != pel) {
                            bPelDelta = true;
                        }
                        weight += pel2 * contribX.p[j].weight;
                    }
                    weight = bPelDelta ? round(weight) : pel;

                    if (weight < 0) {
                        weight = 0;
                    }
                    else if (weight > channelMax[channel]) {
                        weight = channelMax[channel];
                    }

                    work.setSample(0, k, channel, weight);

                }
            }/* next row in temp column */

            /* The temp column has been built. Now stretch it vertically into dst column. */
            for (int i = 0; i < dstHeight; i++) {
                for (int channel = 0; channel < numChannels; channel++) {

                    double weight = 0.0;
                    boolean bPelDelta = false;
                    double pel = work.getSample(0, contribY[i].p[0].pixel, channel);

                    for (int j = 0; j < contribY[i].n; j++) {
                        // TODO: This line throws index out of bounds, if the image
                        // is smaller than filter.support()
                        double pel2 = j == 0 ? pel : work.getSample(0, contribY[i].p[j].pixel, channel);
                        if (pel2 != pel) {
                            bPelDelta = true;
                        }
                        weight += pel2 * contribY[i].p[j].weight;
                    }
                    weight = bPelDelta ? round(weight) : pel;
                    if (weight < 0) {
                        weight = 0;
                    }
                    else if (weight > channelMax[channel]) {
                        weight = channelMax[channel];
                    }

                    out.setSample(xx, i, channel, weight);
                }
            }/* next dst row */
        }/* next dst column */
        return pDest;
    }/* resample */
}