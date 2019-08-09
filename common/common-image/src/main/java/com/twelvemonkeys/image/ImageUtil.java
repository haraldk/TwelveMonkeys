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

package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Hashtable;

/**
 * This class contains methods for basic image manipulation and conversion.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: common/common-image/src/main/java/com/twelvemonkeys/image/ImageUtil.java#3 $
 */
public final class ImageUtil {
    // TODO: Split palette generation out, into ColorModel classes (?)

    public final static int ROTATE_90_CCW = -90;
    public final static int ROTATE_90_CW = 90;
    public final static int ROTATE_180 = 180;

    public final static int FLIP_VERTICAL = -1;
    public final static int FLIP_HORIZONTAL = 1;

    /**
     * Alias for {@link ConvolveOp#EDGE_ZERO_FILL}.
     * @see #convolve(java.awt.image.BufferedImage, java.awt.image.Kernel, int)
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_ZERO_FILL = ConvolveOp.EDGE_ZERO_FILL;

    /**
     * Alias for {@link ConvolveOp#EDGE_NO_OP}.
     * @see #convolve(java.awt.image.BufferedImage, java.awt.image.Kernel, int)
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_NO_OP = ConvolveOp.EDGE_NO_OP;

    /**
     * Adds a border to the image while convolving. The border will reflect the
     * edges of the original image. This is usually a good default.
     * Note that while this mode typically provides better quality than the
     * standard modes {@code EDGE_ZERO_FILL} and {@code EDGE_NO_OP}, it does so
     * at the expense of higher memory consumption and considerable more computation.
     * @see #convolve(java.awt.image.BufferedImage, java.awt.image.Kernel, int)
     */
    public static final int EDGE_REFLECT = 2; // as JAI BORDER_REFLECT

    /**
     * Adds a border to the image while convolving. The border will wrap the
     * edges of the original image. This is usually the best choice for tiles.
     * Note that while this mode typically provides better quality than the
     * standard modes {@code EDGE_ZERO_FILL} and {@code EDGE_NO_OP}, it does so
     * at the expense of higher memory consumption and considerable more computation.
     * @see #convolve(java.awt.image.BufferedImage, java.awt.image.Kernel, int)
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_WRAP = 3; // as JAI BORDER_WRAP

    /**
     * Java default dither
     */
    public final static int DITHER_DEFAULT = IndexImage.DITHER_DEFAULT;

    /**
     * No dither
     */
    public final static int DITHER_NONE = IndexImage.DITHER_NONE;

    /**
     * Error diffusion dither
     */
    public final static int DITHER_DIFFUSION = IndexImage.DITHER_DIFFUSION;

    /**
     * Error diffusion dither with alternating scans
     */
    public final static int DITHER_DIFFUSION_ALTSCANS = IndexImage.DITHER_DIFFUSION_ALTSCANS;

    /**
     * Default color selection
     */
    public final static int COLOR_SELECTION_DEFAULT = IndexImage.COLOR_SELECTION_DEFAULT;

    /**
     * Prioritize speed
     */
    public final static int COLOR_SELECTION_FAST = IndexImage.COLOR_SELECTION_FAST;

    /**
     * Prioritize quality
     */
    public final static int COLOR_SELECTION_QUALITY = IndexImage.COLOR_SELECTION_QUALITY;

    /**
     * Default transparency (none)
     */
    public final static int TRANSPARENCY_DEFAULT = IndexImage.TRANSPARENCY_DEFAULT;

    /**
     * Discard any alpha information
     */
    public final static int TRANSPARENCY_OPAQUE = IndexImage.TRANSPARENCY_OPAQUE;

    /**
     * Convert alpha to bitmask
     */
    public final static int TRANSPARENCY_BITMASK = IndexImage.TRANSPARENCY_BITMASK;

    /**
     * Keep original alpha (not supported yet)
     */
    protected final static int TRANSPARENCY_TRANSLUCENT = IndexImage.TRANSPARENCY_TRANSLUCENT;

    /** Passed to the createXxx methods, to indicate that the type does not matter */
    private final static int BI_TYPE_ANY = -1;
    /*
    public final static int BI_TYPE_ANY_TRANSLUCENT = -1;
    public final static int BI_TYPE_ANY_BITMASK = -2;
    public final static int BI_TYPE_ANY_OPAQUE = -3;*/

    /** Tells wether this WM may support acceleration of some images */
    private static boolean VM_SUPPORTS_ACCELERATION = true;

    /** The sharpen matrix */
    private static final float[] SHARPEN_MATRIX = new float[] {
            0.0f, -0.3f, 0.0f,
            -0.3f, 2.2f, -0.3f,
            0.0f, -0.3f, 0.0f
    };

    /**
     * The sharpen kernel. Uses the following 3 by 3 matrix:
     * <table border="1" cellspacing="0">
     *     <caption>Sharpen Kernel Matrix</caption>
     *     <tr><td>0.0</td><td>-0.3</td><td>0.0</td></tr>
     *     <tr><td>-0.3</td><td>2.2</td><td>-0.3</td></tr>
     *     <tr><td>0.0</td><td>-0.3</td><td>0.0</td></tr>
     * </table>
     */
    private static final Kernel SHARPEN_KERNEL = new Kernel(3, 3, SHARPEN_MATRIX);

    /**
     * Component that can be used with the MediaTracker etc.
     */
    private static final Component NULL_COMPONENT = new Component() {};

    /** Our static image tracker */
    private static MediaTracker sTracker = new MediaTracker(NULL_COMPONENT);

    /** */
    protected static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();
    /** */
    protected static final Point LOCATION_UPPER_LEFT = new Point(0, 0);

    /** */
    private static final GraphicsConfiguration DEFAULT_CONFIGURATION = getDefaultGraphicsConfiguration();

    private static GraphicsConfiguration getDefaultGraphicsConfiguration() {
        try {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (!env.isHeadlessInstance()) {
                return env.getDefaultScreenDevice().getDefaultConfiguration();
            }
        }
        catch (LinkageError e) {
            // Means we are not in a 1.4+ VM, so skip testing for headless again
            VM_SUPPORTS_ACCELERATION = false;
        }
        return null;
    }

    /** Creates an ImageUtil. Private constructor. */
    private ImageUtil() {
    }

    /**
     * Converts the {@code RenderedImage} to a {@code BufferedImage}.
     * The new image will have the <em>same</em> {@code ColorModel},
     * {@code Raster} and properties as the original image, if possible.
     * <p>
     * If the image is already a {@code BufferedImage}, it is simply returned
     * and no conversion takes place.
     * </p>
     *
     * @param pOriginal the image to convert.
     *
     * @return a {@code BufferedImage}
     */
    public static BufferedImage toBuffered(RenderedImage pOriginal) {
        // Don't convert if it already is a BufferedImage
        if (pOriginal instanceof BufferedImage) {
            return (BufferedImage) pOriginal;
        }
        if (pOriginal == null) {
            throw new IllegalArgumentException("original == null");
        }

        // Copy properties
        Hashtable<String, Object> properties;
        String[] names = pOriginal.getPropertyNames();
        if (names != null && names.length > 0) {
            properties = new Hashtable<String, Object>(names.length);

            for (String name : names) {
                properties.put(name, pOriginal.getProperty(name));
            }
        }
        else {
            properties = null;
        }

        // NOTE: This is a workaround for the broken Batik '*Red' classes, that
        // throw NPE if copyData(null) is used. This may actually be faster too.
        // See RenderedImage#copyData / RenderedImage#getData
        Raster data = pOriginal.getData();
        WritableRaster raster;
        if (data instanceof WritableRaster) {
            raster = (WritableRaster) data;
        }
        else {
            raster = data.createCompatibleWritableRaster();
            raster = pOriginal.copyData(raster);
        }

        // Create buffered image
        ColorModel colorModel = pOriginal.getColorModel();
        return new BufferedImage(colorModel, raster,
                                 colorModel.isAlphaPremultiplied(),
                                 properties);
    }

    /**
     * Converts the {@code RenderedImage} to a {@code BufferedImage} of the
     * given type.
     * <p>
     * If the image is already a {@code BufferedImage} of the given type, it
     * is simply returned and no conversion takes place.
     * </p>
     *
     * @param pOriginal the image to convert.
     * @param pType the type of buffered image
     *
     * @return a {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pOriginal == null}
     * or {@code pType} is not a valid type for {@code BufferedImage}
     *
     * @see java.awt.image.BufferedImage#getType()
     */
    public static BufferedImage toBuffered(RenderedImage pOriginal, int pType) {
        // Don't convert if it already is BufferedImage and correct type
        if ((pOriginal instanceof BufferedImage) && ((BufferedImage) pOriginal).getType() == pType) {
            return (BufferedImage) pOriginal;
        }
        if (pOriginal == null) {
            throw new IllegalArgumentException("original == null");
        }

        // Create a buffered image
        BufferedImage image = createBuffered(pOriginal.getWidth(),
                                             pOriginal.getHeight(),
                                             pType, Transparency.TRANSLUCENT);

        // Draw the image onto the buffer
        // NOTE: This is faster than doing a raster conversion in most cases
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawRenderedImage(pOriginal, IDENTITY_TRANSFORM);
        }
        finally {
            g.dispose();
        }

        return image;
    }

    /**
     * Converts the {@code BufferedImage} to a {@code BufferedImage} of the
     * given type. The new image will have the same {@code ColorModel},
     * {@code Raster} and properties as the original image, if possible.
     * <p>
     * If the image is already a {@code BufferedImage} of the given type, it
     * is simply returned and no conversion takes place.
     * </p>
     * <p>
     * This method simply invokes
     * {@link #toBuffered(RenderedImage,int) toBuffered((RenderedImage) pOriginal, pType)}.
     * </p>
     *
     * @param pOriginal the image to convert.
     * @param pType the type of buffered image
     *
     * @return a {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pOriginal == null}
     * or if {@code pType} is not a valid type for {@code BufferedImage}
     *
     * @see java.awt.image.BufferedImage#getType()
     */
    public static BufferedImage toBuffered(BufferedImage pOriginal, int pType) {
        return toBuffered((RenderedImage) pOriginal, pType);
    }

    /**
     * Converts the {@code Image} to a {@code BufferedImage}.
     * The new image will have the same {@code ColorModel}, {@code Raster} and
     * properties as the original image, if possible.
     * <p>
     * If the image is already a {@code BufferedImage}, it is simply returned
     * and no conversion takes place.
     * </p>
     *
     * @param pOriginal the image to convert.
     *
     * @return a {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pOriginal == null}
     * @throws ImageConversionException if the image cannot be converted
     */
    public static BufferedImage toBuffered(Image pOriginal) {
        // Don't convert if it already is BufferedImage
        if (pOriginal instanceof BufferedImage) {
            return (BufferedImage) pOriginal;
        }
        if (pOriginal == null) {
            throw new IllegalArgumentException("original == null");
        }

        //System.out.println("--> Doing full BufferedImage conversion...");

        BufferedImageFactory factory = new BufferedImageFactory(pOriginal);
        return factory.getBufferedImage();
    }

    /**
     * Creates a deep copy of the given image. The image will have the same
     * color model and raster type, but will not share image (pixel) data
     * with the input image.
     *
     * @param pImage the image to clone.
     *
     * @return a new {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pImage} is {@code null}
     */
    public static BufferedImage createCopy(final BufferedImage pImage) {
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }

        ColorModel cm = pImage.getColorModel();

        BufferedImage img = new BufferedImage(cm,
                                              cm.createCompatibleWritableRaster(pImage.getWidth(), pImage.getHeight()),
                                              cm.isAlphaPremultiplied(), null);

        drawOnto(img, pImage);

        return img;
    }

    /**
     * Creates a {@code WritableRaster} for the given {@code ColorModel} and
     * pixel data.
     * <p>
     * This method is optimized for the most common cases of {@code ColorModel}
     * and pixel data combinations. The raster's backing {@code DataBuffer} is
     * created directly from the pixel data, as this is faster and more
     * resource-friendly than using
     * {@code ColorModel.createCompatibleWritableRaster(w, h)}.
     * </p>
     * <p>
     * For uncommon combinations, the method will fallback to using
     * {@code ColorModel.createCompatibleWritableRaster(w, h)} and
     * {@code WritableRaster.setDataElements(w, h, pixels)}
     * </p>
     * <p>
     * Note that the {@code ColorModel} and pixel data are <em>not</em> cloned
     * (in most cases).
     * </p>
     *
     * @param pWidth the requested raster width
     * @param pHeight the requested raster height
     * @param pPixels the pixels, as an array, of a type supported by the
     *        different {@link DataBuffer}
     * @param pColorModel the color model to use
     * @return a new {@code WritableRaster}
     *
     * @throws NullPointerException if either {@code pColorModel} or
     *         {@code pPixels} are {@code null}.
     * @throws RuntimeException if {@code pWidth} and {@code pHeight} does not
     *         match the pixel data in {@code pPixels}.
     *
     * @see ColorModel#createCompatibleWritableRaster(int, int)
     * @see ColorModel#createCompatibleSampleModel(int, int)
     * @see WritableRaster#setDataElements(int, int, Object)
     * @see DataBuffer
     */
    static WritableRaster createRaster(int pWidth, int pHeight, Object pPixels, ColorModel pColorModel) {
        // NOTE: This is optimized code for most common cases.
        // We create a DataBuffer from the pixel array directly,
        // and creating a raster based on the DataBuffer and ColorModel.
        // Creating rasters this way is faster and more resource-friendly, as
        // cm.createCompatibleWritableRaster allocates an
        // "empty" DataBuffer with a storage array of w*h. This array is
        // later discarded, and replaced in the raster.setDataElements() call.
        // The "old" way is kept as a more compatible fall-back mode.

        DataBuffer buffer = null;
        WritableRaster raster = null;

        int bands;
        if (pPixels instanceof int[]) {
            int[] data = (int[]) pPixels;
            buffer = new DataBufferInt(data, data.length);
            bands = pColorModel.getNumComponents();
        }
        else if (pPixels instanceof short[]) {
            short[] data = (short[]) pPixels;
            buffer = new DataBufferUShort(data, data.length);
            bands = data.length / (pWidth * pHeight);
        }
        else if (pPixels instanceof byte[]) {
            byte[] data = (byte[]) pPixels;
            buffer = new DataBufferByte(data, data.length);

            // NOTE: This only holds for gray and indexed with one byte per pixel...
            if (pColorModel instanceof IndexColorModel) {
                bands = 1;
            }
            else {
                bands = data.length / (pWidth * pHeight);
            }
        }
        else {
            // Fallback mode, slower & requires more memory, but compatible
            bands = -1;

            // Create raster from color model, w and h
            raster = pColorModel.createCompatibleWritableRaster(pWidth, pHeight);
            raster.setDataElements(0, 0, pWidth, pHeight, pPixels); // Note: This is known to throw ClassCastExceptions..
        }

        if (raster == null) {
            if (pColorModel instanceof IndexColorModel && isIndexedPacked((IndexColorModel) pColorModel)) {
                raster = Raster.createPackedRaster(buffer, pWidth, pHeight, pColorModel.getPixelSize(), LOCATION_UPPER_LEFT);
            }
            else if (pColorModel instanceof PackedColorModel) {
                PackedColorModel pcm = (PackedColorModel) pColorModel;
                raster = Raster.createPackedRaster(buffer, pWidth, pHeight, pWidth, pcm.getMasks(), LOCATION_UPPER_LEFT);
            }
            else {
                // (A)BGR order... For TYPE_3BYTE_BGR/TYPE_4BYTE_ABGR/TYPE_4BYTE_ABGR_PRE.
                int[] bandsOffsets = new int[bands];
                for (int i = 0; i < bands;) {
                    bandsOffsets[i] = bands - (++i);
                }

                raster = Raster.createInterleavedRaster(buffer, pWidth, pHeight, pWidth * bands, bands, bandsOffsets, LOCATION_UPPER_LEFT);
            }
        }

        return raster;
    }

    private static boolean isIndexedPacked(IndexColorModel pColorModel) {
        return (pColorModel.getPixelSize() == 1 || pColorModel.getPixelSize() == 2 || pColorModel.getPixelSize() == 4);
    }

    /**
     * Workaround for bug: TYPE_3BYTE_BGR, TYPE_4BYTE_ABGR and
     * TYPE_4BYTE_ABGR_PRE are all converted to TYPE_CUSTOM when using the
     * default createCompatibleWritableRaster from ComponentColorModel.
     *
     * @param pOriginal the orignal image
     * @param pModel the original color model
     * @param width the requested width of the raster
     * @param height the requested height of the raster
     *
     * @return a new WritableRaster
     */
    static WritableRaster createCompatibleWritableRaster(BufferedImage pOriginal, ColorModel pModel, int width, int height) {
        if (pModel == null || equals(pOriginal.getColorModel(), pModel)) {
            int[] bOffs;
            switch (pOriginal.getType()) {
                case BufferedImage.TYPE_3BYTE_BGR:
                    bOffs = new int[]{2, 1, 0}; // NOTE: These are reversed from what the cm.createCompatibleWritableRaster would return
                    return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                          width, height,
                                                          width * 3, 3,
                                                          bOffs, null);
                case BufferedImage.TYPE_4BYTE_ABGR:
                case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                    bOffs = new int[] {3, 2, 1, 0}; // NOTE: These are reversed from what the cm.createCompatibleWritableRaster would return
                    return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                          width, height,
                                                          width * 4, 4,
                                                          bOffs, null);
                case BufferedImage.TYPE_CUSTOM:
                    // Peek into the sample model to see if we have a sample model that will be incompatible with the default case
                    SampleModel sm = pOriginal.getRaster().getSampleModel();
                    if (sm instanceof ComponentSampleModel) {
                        bOffs = ((ComponentSampleModel) sm).getBandOffsets();
                        return Raster.createInterleavedRaster(sm.getDataType(),
                                                              width, height,
                                                              width * bOffs.length, bOffs.length,
                                                              bOffs, null);
                    }
                    // Else fall through
                default:
                    return pOriginal.getColorModel().createCompatibleWritableRaster(width, height);
            }
        }

        return pModel.createCompatibleWritableRaster(width, height);
    }

    /**
     * Converts the {@code Image} to a {@code BufferedImage} of the given type.
     * The new image will have the same {@code ColorModel}, {@code Raster} and
     * properties as the original image, if possible.
     * <p>
     * If the image is already a {@code BufferedImage} of the given type, it
     * is simply returned and no conversion takes place.
     * </p>
     *
     * @param pOriginal the image to convert.
     * @param pType the type of buffered image
     *
     * @return a {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pOriginal == null}
     * or if {@code pType} is not a valid type for {@code BufferedImage}
     *
     * @see java.awt.image.BufferedImage#getType()
     */
    public static BufferedImage toBuffered(Image pOriginal, int pType) {
        return toBuffered(pOriginal, pType, null);
    }

    /**
     *
     * @param pOriginal the original image
     * @param pType the type of {@code BufferedImage} to create
     * @param pICM the optional {@code IndexColorModel} to use. If not
     * {@code null} the {@code pType} must be compatible with the color model
     * @return a {@code BufferedImage}
     * @throws IllegalArgumentException if {@code pType} is not compatible with
     * the color model
     */
    private static BufferedImage toBuffered(Image pOriginal, int pType, IndexColorModel pICM) {
        // Don't convert if it already is BufferedImage and correct type
        if ((pOriginal instanceof BufferedImage)
                && ((BufferedImage) pOriginal).getType() == pType
                && (pICM == null || equals(((BufferedImage) pOriginal).getColorModel(), pICM))) {
            return (BufferedImage) pOriginal;
        }
        if (pOriginal == null) {
            throw new IllegalArgumentException("original == null");
        }

        //System.out.println("--> Doing full BufferedImage conversion, using Graphics.drawImage().");

        // Create a buffered image
        // NOTE: The getWidth and getHeight methods, will wait for the image
        BufferedImage image;
        if (pICM == null) {
            image = createBuffered(getWidth(pOriginal), getHeight(pOriginal), pType, Transparency.TRANSLUCENT);//new BufferedImage(getWidth(pOriginal), getHeight(pOriginal), pType);
        }
        else {
            image = new BufferedImage(getWidth(pOriginal), getHeight(pOriginal), pType, pICM);
        }

        // Draw the image onto the buffer
        drawOnto(image, pOriginal);

        return image;
    }

    /**
     * Draws the source image onto the buffered image, using
     * {@code AlphaComposite.Src} and coordinates {@code 0, 0}.
     *
     * @param pDestination the image to draw on
     * @param pSource the source image to draw
     *
     * @throws NullPointerException if {@code pDestination} or {@code pSource} is {@code null}
     */
    static void drawOnto(final BufferedImage pDestination, final Image pSource) {
        Graphics2D g = pDestination.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g.drawImage(pSource, 0, 0, null);
        }
        finally {
            g.dispose();
        }
    }

    /**
     * Creates a flipped version of the given image.
     *
     * @param pImage the image to flip
     * @param pAxis the axis to flip around
     * @return a new {@code BufferedImage}
     */
    public static BufferedImage createFlipped(final Image pImage, final int pAxis) {
        switch (pAxis) {
            case FLIP_HORIZONTAL:
            case FLIP_VERTICAL:
            // TODO case FLIP_BOTH:?? same as rotate 180?
                break;
            default:
                throw new IllegalArgumentException("Illegal direction: " + pAxis);
        }
        BufferedImage source = toBuffered(pImage);
        AffineTransform transform;
        if (pAxis == FLIP_HORIZONTAL) {
            transform = AffineTransform.getTranslateInstance(0, source.getHeight());
            transform.scale(1, -1);
        }
        else {
            transform = AffineTransform.getTranslateInstance(source.getWidth(), 0);
            transform.scale(-1, 1);
        }
        AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return transformOp.filter(source, null);
    }


    /**
     * Rotates the image 90 degrees, clockwise (aka "rotate right"),
     * counter-clockwise (aka "rotate left") or 180 degrees, depending on the
     * {@code pDirection} argument.
     * <p>
     * The new image will be completely covered with pixels from the source
     * image.
     * </p>
     *
     * @param pImage the source image.
     * @param pDirection the direction, must be either {@link #ROTATE_90_CW},
     * {@link #ROTATE_90_CCW} or {@link #ROTATE_180}
     *
     * @return a new {@code BufferedImage}
     *
     */
    public static BufferedImage createRotated(final Image pImage, final int pDirection) {
        switch (pDirection) {
            case ROTATE_90_CW:
            case ROTATE_90_CCW:
            case ROTATE_180:
                return createRotated(pImage, Math.toRadians(pDirection));
            default:
                throw new IllegalArgumentException("Illegal direction: " + pDirection);
        }
    }

    /**
     * Rotates the image to the given angle. Areas not covered with pixels from
     * the source image will be left transparent, if possible.
     *
     * @param pImage the source image
     * @param pAngle the angle of rotation, in radians
     *
     * @return a new {@code BufferedImage}, unless {@code pAngle == 0.0}
     */
    public static BufferedImage createRotated(final Image pImage, final double pAngle) {
        return createRotated0(toBuffered(pImage), pAngle);
    }

    private static BufferedImage createRotated0(final BufferedImage pSource, final double pAngle) {
        if ((Math.abs(Math.toDegrees(pAngle)) % 360) == 0) {
            return pSource;
        }

        final boolean fast = ((Math.abs(Math.toDegrees(pAngle)) % 90) == 0.0);
        final int w = pSource.getWidth();
        final int h = pSource.getHeight();

        // Compute new width and height
        double sin = Math.abs(Math.sin(pAngle));
        double cos = Math.abs(Math.cos(pAngle));

        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        AffineTransform transform = AffineTransform.getTranslateInstance((newW - w) / 2.0, (newH - h) / 2.0);
        transform.rotate(pAngle, w / 2.0, h / 2.0);

        // TODO: Figure out if this is correct
        BufferedImage dest = createTransparent(newW, newH);

        // See: http://weblogs.java.net/blog/campbell/archive/2007/03/java_2d_tricker_1.html
        Graphics2D g = dest.createGraphics();
        try {
            g.transform(transform);
            if (!fast) {
                // Max quality
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                                   RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                   RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new TexturePaint(pSource,
                                            new Rectangle2D.Float(0, 0, pSource.getWidth(), pSource.getHeight())));
                g.fillRect(0, 0, pSource.getWidth(), pSource.getHeight());
            }
            else {
                g.drawImage(pSource, 0, 0, null);
            }
        }
        finally {
            g.dispose();
        }

        return dest;
    }

    /**
     * Creates a scaled instance of the given {@code Image}, and converts it to
     * a {@code BufferedImage} if needed.
     * If the original image is a {@code BufferedImage} the result will have
     * same type and color model. Note that this implies overhead, and is
     * probably not useful for anything but {@code IndexColorModel} images.
     *
     * @param pImage the {@code Image} to scale
     * @param pWidth width in pixels
     * @param pHeight height in pixels
     * @param pHints scaling ints
     *
     * @return a {@code BufferedImage}
     *
     * @throws NullPointerException if {@code pImage} is {@code null}.
     *
     * @see #createResampled(java.awt.Image, int, int, int)
     * @see Image#getScaledInstance(int,int,int)
     * @see Image#SCALE_AREA_AVERAGING
     * @see Image#SCALE_DEFAULT
     * @see Image#SCALE_FAST
     * @see Image#SCALE_REPLICATE
     * @see Image#SCALE_SMOOTH
     */
    public static BufferedImage createScaled(Image pImage, int pWidth, int pHeight, int pHints) {
        ColorModel cm;
        int type = BI_TYPE_ANY;
        if (pImage instanceof RenderedImage) {
            cm = ((RenderedImage) pImage).getColorModel();
            if (pImage instanceof BufferedImage) {
                type = ((BufferedImage) pImage).getType();
            }
        }
        else {
            BufferedImageFactory factory = new BufferedImageFactory(pImage);
            cm = factory.getColorModel();
        }

        BufferedImage scaled = createResampled(pImage, pWidth, pHeight, pHints);

        // Convert if color models or type differ, to behave as documented
        if (type != scaled.getType() && type != BI_TYPE_ANY || !equals(scaled.getColorModel(), cm)) {
            //System.out.print("Converting TYPE " + scaled.getType() + " -> " + type + "... ");
            //long start = System.currentTimeMillis();
            WritableRaster raster;
            if (pImage instanceof BufferedImage) {
                raster = createCompatibleWritableRaster((BufferedImage) pImage, cm, pWidth, pHeight);
            }
            else {
                raster = cm.createCompatibleWritableRaster(pWidth, pHeight);
            }

            BufferedImage temp = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

            if (cm instanceof IndexColorModel && pHints == Image.SCALE_SMOOTH) {
                // TODO: DiffusionDither does not support transparency at the moment, this will create bad results
                new DiffusionDither((IndexColorModel) cm).filter(scaled, temp);
            }
            else {
                drawOnto(temp, scaled);
            }

            scaled = temp;
            //long end = System.currentTimeMillis();
            //System.out.println("Time: " + (end - start) + " ms");
        }

        return scaled;
    }

    private static boolean equals(ColorModel pLeft, ColorModel pRight) {
        if (pLeft == pRight) {
            return true;
        }

        if (!pLeft.equals(pRight)) {
            return false;
        }

        // Now, the models are equal, according to the equals method
        // Test indexcolormodels for equality, the maps must be equal
        if (pLeft instanceof IndexColorModel) {
            IndexColorModel icm1 = (IndexColorModel) pLeft;
            IndexColorModel icm2 = (IndexColorModel) pRight; // NOTE: Safe, they're equal


            final int mapSize1 = icm1.getMapSize();
            final int mapSize2 = icm2.getMapSize();

            if (mapSize1 != mapSize2) {
                return false;
            }

            for (int i = 0; i > mapSize1; i++) {
                if (icm1.getRGB(i) != icm2.getRGB(i)) {
                    return false;
                }
            }

            return true;

        }

        return true;
    }

    /**
     * Creates a scaled instance of the given {@code Image}, and converts it to
     * a {@code BufferedImage} if needed.
     *
     * @param pImage the {@code Image} to scale
     * @param pWidth width in pixels
     * @param pHeight height in pixels
     * @param pHints scaling mHints
     *
     * @return a {@code BufferedImage}
     *
     * @throws NullPointerException if {@code pImage} is {@code null}.
     *
     * @see Image#SCALE_AREA_AVERAGING
     * @see Image#SCALE_DEFAULT
     * @see Image#SCALE_FAST
     * @see Image#SCALE_REPLICATE
     * @see Image#SCALE_SMOOTH
     * @see ResampleOp
     */
    public static BufferedImage createResampled(Image pImage, int pWidth, int pHeight, int pHints) {
        // NOTE: TYPE_4BYTE_ABGR or TYPE_3BYTE_BGR is more efficient when accelerated...
        BufferedImage image = pImage instanceof  BufferedImage
                ? (BufferedImage) pImage
                : toBuffered(pImage, BufferedImage.TYPE_4BYTE_ABGR);
        return createResampled(image, pWidth, pHeight, pHints);
    }

    /**
     * Creates a scaled instance of the given {@code RenderedImage}, and
     * converts it to a {@code BufferedImage} if needed.
     *
     * @param pImage the {@code RenderedImage} to scale
     * @param pWidth width in pixels
     * @param pHeight height in pixels
     * @param pHints scaling mHints
     *
     * @return a {@code BufferedImage}
     *
     * @throws NullPointerException if {@code pImage} is {@code null}.
     *
     * @see Image#SCALE_AREA_AVERAGING
     * @see Image#SCALE_DEFAULT
     * @see Image#SCALE_FAST
     * @see Image#SCALE_REPLICATE
     * @see Image#SCALE_SMOOTH
     * @see ResampleOp
     */
    public static BufferedImage createResampled(RenderedImage pImage, int pWidth, int pHeight, int pHints) {
        // NOTE: TYPE_4BYTE_ABGR or TYPE_3BYTE_BGR is more efficient when accelerated...
        BufferedImage image = pImage instanceof  BufferedImage
                ? (BufferedImage) pImage
                : toBuffered(pImage, pImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
        return createResampled(image, pWidth, pHeight, pHints);
    }

    /**
     * Creates a scaled instance of the given {@code BufferedImage}.
     *
     * @param pImage the {@code BufferedImage} to scale
     * @param pWidth width in pixels
     * @param pHeight height in pixels
     * @param pHints scaling mHints
     *
     * @return a {@code BufferedImage}
     *
     * @throws NullPointerException if {@code pImage} is {@code null}.
     *
     * @see Image#SCALE_AREA_AVERAGING
     * @see Image#SCALE_DEFAULT
     * @see Image#SCALE_FAST
     * @see Image#SCALE_REPLICATE
     * @see Image#SCALE_SMOOTH
     * @see ResampleOp
     */
    public static BufferedImage createResampled(BufferedImage pImage, int pWidth, int pHeight, int pHints) {
        // Hints are converted between java.awt.Image hints and filter types
        return new ResampleOp(pWidth, pHeight, convertAWTHints(pHints)).filter(pImage, null);
    }

    private static int convertAWTHints(int pHints) {
        switch (pHints) {
            case Image.SCALE_FAST:
            case Image.SCALE_REPLICATE:
                return ResampleOp.FILTER_POINT;
            case Image.SCALE_AREA_AVERAGING:
                return ResampleOp.FILTER_BOX;
                //return ResampleOp.FILTER_CUBIC;
            case Image.SCALE_SMOOTH:
                return ResampleOp.FILTER_LANCZOS;
            default:
                //return ResampleOp.FILTER_TRIANGLE;
                return ResampleOp.FILTER_QUADRATIC;
        }
    }

    /**
     * Extracts an {@code IndexColorModel} from the given image.
     *
     * @param pImage the image to get the color model from
     * @param pColors the maximum number of colors in the resulting color model
     * @param pHints hints controlling transparency and color selection
     *
     * @return the extracted {@code IndexColorModel}
     *
     * @see #COLOR_SELECTION_DEFAULT
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #TRANSPARENCY_DEFAULT
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see #TRANSPARENCY_TRANSLUCENT
     */
    public static IndexColorModel getIndexColorModel(Image pImage, int pColors, int pHints) {
        return IndexImage.getIndexColorModel(pImage, pColors, pHints);
    }

    /**
     * Creates an indexed version of the given image (a {@code BufferedImage}
     * with an {@code IndexColorModel}.
     * The resulting image will have a maximum of 256 different colors.
     * Transparent parts of the original will be replaced with solid black.
     * Default (possibly HW accelerated) dither will be used.
     *
     * @param pImage the image to convert
     *
     * @return an indexed version of the given image
     */
    public static BufferedImage createIndexed(Image pImage) {
        return IndexImage.getIndexedImage(toBuffered(pImage), 256, Color.black, IndexImage.DITHER_DEFAULT);
    }

    /**
     * Creates an indexed version of the given image (a {@code BufferedImage}
     * with an {@code IndexColorModel}.
     *
     * @param pImage the image to convert
     * @param pColors number of colors in the resulting image
     * @param pMatte color to replace transparent parts of the original.
     * @param pHints hints controlling dither, transparency and color selection
     *
     * @return an indexed version of the given image
     *
     * @see #COLOR_SELECTION_DEFAULT
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #DITHER_NONE
     * @see #DITHER_DEFAULT
     * @see #DITHER_DIFFUSION
     * @see #DITHER_DIFFUSION_ALTSCANS
     * @see #TRANSPARENCY_DEFAULT
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see #TRANSPARENCY_TRANSLUCENT
     */
    public static BufferedImage createIndexed(Image pImage, int pColors, Color pMatte, int pHints) {
        return IndexImage.getIndexedImage(toBuffered(pImage), pColors, pMatte, pHints);
    }

    /**
     * Creates an indexed version of the given image (a {@code BufferedImage}
     * with an {@code IndexColorModel}.
     *
     * @param pImage the image to convert
     * @param pColors the {@code IndexColorModel} to be used in the resulting
     * image.
     * @param pMatte color to replace transparent parts of the original.
     * @param pHints hints controlling dither, transparency and color selection
     *
     * @return an indexed version of the given image
     *
     * @see #COLOR_SELECTION_DEFAULT
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #DITHER_NONE
     * @see #DITHER_DEFAULT
     * @see #DITHER_DIFFUSION
     * @see #DITHER_DIFFUSION_ALTSCANS
     * @see #TRANSPARENCY_DEFAULT
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see #TRANSPARENCY_TRANSLUCENT
     */
    public static BufferedImage createIndexed(Image pImage, IndexColorModel pColors, Color pMatte, int pHints) {
        return IndexImage.getIndexedImage(toBuffered(pImage), pColors, pMatte, pHints);
    }

    /**
     * Creates an indexed version of the given image (a {@code BufferedImage}
     * with an {@code IndexColorModel}.
     *
     * @param pImage the image to convert
     * @param pColors an {@code Image} used to get colors from. If the image is
     * has an {@code IndexColorModel}, it will be uesd, otherwise an
     * {@code IndexColorModel} is created from the image.
     * @param pMatte color to replace transparent parts of the original.
     * @param pHints hints controlling dither, transparency and color selection
     *
     * @return an indexed version of the given image
     *
     * @see #COLOR_SELECTION_DEFAULT
     * @see #COLOR_SELECTION_FAST
     * @see #COLOR_SELECTION_QUALITY
     * @see #DITHER_NONE
     * @see #DITHER_DEFAULT
     * @see #DITHER_DIFFUSION
     * @see #DITHER_DIFFUSION_ALTSCANS
     * @see #TRANSPARENCY_DEFAULT
     * @see #TRANSPARENCY_OPAQUE
     * @see #TRANSPARENCY_BITMASK
     * @see #TRANSPARENCY_TRANSLUCENT
     */
    public static BufferedImage createIndexed(Image pImage, Image pColors, Color pMatte, int pHints) {
        return IndexImage.getIndexedImage(toBuffered(pImage),
                                          IndexImage.getIndexColorModel(pColors, 255, pHints),
                                          pMatte, pHints);
    }

    /**
     * Sharpens an image using a convolution matrix.
     * The sharpen kernel used, is defined by the following 3 by 3 matrix:
     * <table border="1" cellspacing="0">
     *     <caption>Sharpen Kernel Matrix</caption>
     *     <tr><td>0.0</td><td>-0.3</td><td>0.0</td></tr>
     *     <tr><td>-0.3</td><td>2.2</td><td>-0.3</td></tr>
     *     <tr><td>0.0</td><td>-0.3</td><td>0.0</td></tr>
     * </table>
     * <p>
     * This is the same result returned as
     * {@code sharpen(pOriginal, 0.3f)}.
     * </p>
     *
     * @param pOriginal the BufferedImage to sharpen
     *
     * @return a new BufferedImage, containing the sharpened image.
     */
    public static BufferedImage sharpen(BufferedImage pOriginal) {
        return convolve(pOriginal, SHARPEN_KERNEL, EDGE_REFLECT);
    }

    /**
     * Sharpens an image using a convolution matrix.
     * The sharpen kernel used, is defined by the following 3 by 3 matrix:
     * <table border="1" cellspacing="0">
     *     <caption>Sharpen Kernel Matrix</caption>
     *     <tr><td>0.0</td><td>-{@code pAmount}</td><td>0.0</td></tr>
     *     <tr><td>-{@code pAmount}</td>
     *         <td>4.0 * {@code pAmount} + 1.0</td>
     *         <td>-{@code pAmount}</td></tr>
     *     <tr><td>0.0</td><td>-{@code pAmount}</td><td>0.0</td></tr>
     * </table>
     *
     * @param pOriginal the BufferedImage to sharpen
     * @param pAmount the amount of sharpening
     *
     * @return a BufferedImage, containing the sharpened image.
     */
    public static BufferedImage sharpen(BufferedImage pOriginal, float pAmount) {
        if (pAmount == 0f) {
            return pOriginal;
        }

        // Create the convolution matrix
        float[] data = new float[] {
            0.0f, -pAmount, 0.0f, -pAmount, 4f * pAmount + 1f, -pAmount, 0.0f, -pAmount, 0.0f
        };

        // Do the filtering
        return convolve(pOriginal, new Kernel(3, 3, data), EDGE_REFLECT);
    }

    /**
     * Creates a blurred version of the given image.
     *
     * @param pOriginal the original image
     *
     * @return a new {@code BufferedImage} with a blurred version of the given image
     */
    public static BufferedImage blur(BufferedImage pOriginal) {
        return blur(pOriginal, 1.5f);
    }

    // Some work to do... Is okay now, for range 0...1, anything above creates
    // artifacts.
    // The idea here is that the sum of all terms in the matrix must be 1.

    /**
     * Creates a blurred version of the given image.
     *
     * @param pOriginal the original image
     * @param pRadius the amount to blur
     *
     * @return a new {@code BufferedImage} with a blurred version of the given image
     */
    public static BufferedImage blur(BufferedImage pOriginal, float pRadius) {
        if (pRadius <= 1f) {
            return pOriginal;
        }

        // TODO: Re-implement using two-pass one-dimensional gaussion blur
        // See: http://en.wikipedia.org/wiki/Gaussian_blur#Implementation
        // Also see http://www.jhlabs.com/ip/blurring.html

        // TODO: Rethink... Fixed amount and scale matrix instead?
//        pAmount = 1f - pAmount;
//        float pAmount = 1f - pRadius;
//
//        // Normalize amount
//        float normAmt = (1f - pAmount) / 24;
//
//        // Create the convolution matrix
//        float[] data = new float[] {
//            normAmt / 2, normAmt, normAmt, normAmt, normAmt / 2,
//            normAmt, normAmt, normAmt * 2, normAmt, normAmt,
//            normAmt, normAmt * 2, pAmount, normAmt * 2, normAmt,
//            normAmt, normAmt, normAmt * 2, normAmt, normAmt,
//            normAmt / 2, normAmt, normAmt, normAmt, normAmt / 2
//        };
//
//        // Do the filtering
//        return convolve(pOriginal, new Kernel(5, 5, data), EDGE_REFLECT);

        Kernel horizontal = makeKernel(pRadius);
        Kernel vertical = new Kernel(horizontal.getHeight(), horizontal.getWidth(), horizontal.getKernelData(null));

        BufferedImage temp = addBorder(pOriginal, horizontal.getWidth() / 2, vertical.getHeight() / 2, EDGE_REFLECT);

        temp = convolve(temp, horizontal, EDGE_NO_OP);
        temp = convolve(temp, vertical, EDGE_NO_OP);

        return temp.getSubimage(
                horizontal.getWidth() / 2, vertical.getHeight() / 2, pOriginal.getWidth(), pOriginal.getHeight()
        );
    }

    /**
     * Make a Gaussian blur {@link Kernel}.
     *
     * @param radius the blur radius
     * @return a new blur {@code Kernel}
     */
    private static Kernel makeKernel(float radius) {
        int r = (int) Math.ceil(radius);
        int rows = r * 2 + 1;
        float[] matrix = new float[rows];
        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        float sigmaPi2 = (float) (2 * Math.PI * sigma);
        float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
        float radius2 = radius * radius;
        float total = 0;
        int index = 0;
        for (int row = -r; row <= r; row++) {
            float distance = row * row;
            if (distance > radius2) {
                matrix[index] = 0;
            }
            else {
                matrix[index] = (float) Math.exp(-(distance) / sigma22) / sqrtSigmaPi2;
            }
            total += matrix[index];
            index++;
        }
        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }

        return new Kernel(rows, 1, matrix);
    }


    /**
     * Convolves an image, using a convolution matrix.
     *
     * @param pOriginal the BufferedImage to sharpen
     * @param pKernel the kernel
     * @param pEdgeOperation the edge operation. Must be one of {@link #EDGE_NO_OP}, 
     * {@link #EDGE_ZERO_FILL}, {@link #EDGE_REFLECT} or {@link #EDGE_WRAP}
     *
     * @return a new BufferedImage, containing the sharpened image.
     */
    public static BufferedImage convolve(BufferedImage pOriginal, Kernel pKernel, int pEdgeOperation) {
        // Allow for 2 more edge operations
        BufferedImage original;
        switch (pEdgeOperation) {
            case EDGE_REFLECT:
            case EDGE_WRAP:
                original = addBorder(pOriginal, pKernel.getWidth() / 2, pKernel.getHeight() / 2, pEdgeOperation);
                break;
            default:
                original = pOriginal;
                break;
        }

        // Create convolution operation
        ConvolveOp convolve = new ConvolveOp(pKernel, pEdgeOperation, null);

        // Workaround for what seems to be a Java2D bug:
        // ConvolveOp needs explicit destination image type for some "uncommon"
        // image types. However, TYPE_3BYTE_BGR is what javax.imageio.ImageIO
        // normally returns for color JPEGs... :-/
        BufferedImage result = null;
        if (original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            result = createBuffered(
                    pOriginal.getWidth(), pOriginal.getHeight(),
                    pOriginal.getType(), pOriginal.getColorModel().getTransparency()
            );
        }

        // Do the filtering (if result is null, a new image will be created)
        BufferedImage image = convolve.filter(original, result);

        if (pOriginal != original) {
            // Remove the border
            image = image.getSubimage(
                    pKernel.getWidth() / 2, pKernel.getHeight() / 2, pOriginal.getWidth(), pOriginal.getHeight()
            );
        }

        return image;
    }

    private static BufferedImage addBorder(final BufferedImage pOriginal, final int pBorderX, final int pBorderY, final int pEdgeOperation) {
        // TODO: Might be faster if we could clone raster and strech it...
        int w = pOriginal.getWidth();
        int h = pOriginal.getHeight();

        ColorModel cm = pOriginal.getColorModel();
        WritableRaster raster = cm.createCompatibleWritableRaster(w + 2 * pBorderX, h + 2 * pBorderY);
        BufferedImage bordered = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        Graphics2D g = bordered.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

            // Draw original in center
            g.drawImage(pOriginal, pBorderX, pBorderY, null);

            // TODO: I guess we need the top/left etc, if the corner pixels are covered by the kernel
            switch (pEdgeOperation) {
                case EDGE_REFLECT:
                    // Top/left (empty)
                    g.drawImage(pOriginal, pBorderX, 0, pBorderX + w, pBorderY, 0, 0, w, 1, null); // Top/center
                    // Top/right (empty)

                    g.drawImage(pOriginal, -w + pBorderX, pBorderY, pBorderX, h + pBorderY, 0, 0, 1, h, null); // Center/left
                    // Center/center (already drawn)
                    g.drawImage(pOriginal, w + pBorderX, pBorderY, 2 * pBorderX + w, h + pBorderY, w - 1, 0, w, h, null); // Center/right

                    // Bottom/left (empty)
                    g.drawImage(pOriginal, pBorderX, pBorderY + h, pBorderX + w, 2 * pBorderY + h, 0, h - 1, w, h, null); // Bottom/center
                    // Bottom/right (empty)
                    break;
                case EDGE_WRAP:
                    g.drawImage(pOriginal, -w + pBorderX, -h + pBorderY, null); // Top/left
                    g.drawImage(pOriginal, pBorderX, -h + pBorderY, null); // Top/center
                    g.drawImage(pOriginal, w + pBorderX, -h + pBorderY, null); // Top/right

                    g.drawImage(pOriginal, -w + pBorderX, pBorderY, null); // Center/left
                    // Center/center (already drawn)
                    g.drawImage(pOriginal, w + pBorderX, pBorderY, null); // Center/right

                    g.drawImage(pOriginal, -w + pBorderX, h + pBorderY, null); // Bottom/left
                    g.drawImage(pOriginal, pBorderX, h + pBorderY, null); // Bottom/center
                    g.drawImage(pOriginal, w + pBorderX, h + pBorderY, null); // Bottom/right
                    break;
                default:
                    throw new IllegalArgumentException("Illegal edge operation " + pEdgeOperation);
            }

        }
        finally {
            g.dispose();
        }

        //ConvolveTester.showIt(bordered, "jaffe");

        return bordered;
    }

    /**
     * Adds contrast
     *
     * @param pOriginal the BufferedImage to add contrast to
     *
     * @return an {@code Image}, containing the contrasted image.
     */
    public static Image contrast(Image pOriginal) {
        return contrast(pOriginal, 0.3f);
    }

    /**
     * Changes the contrast of the image
     *
     * @param pOriginal the {@code Image} to change
     * @param pAmount the amount of contrast in the range [-1.0..1.0].
     *
     * @return an {@code Image}, containing the contrasted image.
     */
    public static Image contrast(Image pOriginal, float pAmount) {
        // No change, return original
        if (pAmount == 0f) {
            return pOriginal;
        }

        // Create filter
        RGBImageFilter filter = new BrightnessContrastFilter(0f, pAmount);

        // Return contrast adjusted image
        return filter(pOriginal, filter);
    }


    /**
     * Changes the brightness of the original image.
     *
     * @param pOriginal the {@code Image} to change
     * @param pAmount the amount of brightness in the range [-2.0..2.0].
     *
     * @return an {@code Image}
     */
    public static Image brightness(Image pOriginal, float pAmount) {
        // No change, return original
        if (pAmount == 0f) {
            return pOriginal;
        }

        // Create filter
        RGBImageFilter filter = new BrightnessContrastFilter(pAmount, 0f);

        // Return brightness adjusted image
        return filter(pOriginal, filter);
    }


    /**
     * Converts an image to grayscale.
     *
     * @see GrayFilter
     * @see RGBImageFilter
     *
     * @param pOriginal the image to convert.
     * @return a new Image, containing the gray image data.
     */
    public static Image grayscale(Image pOriginal) {
        // Create filter
        RGBImageFilter filter = new GrayFilter();

        // Convert to gray
        return filter(pOriginal, filter);
    }

    /**
     * Filters an image, using the given {@code ImageFilter}.
     *
     * @param pOriginal the original image
     * @param pFilter the filter to apply
     *
     * @return the new {@code Image}
     */
    public static Image filter(Image pOriginal, ImageFilter pFilter) {
        // Create a filtered source
        ImageProducer source = new FilteredImageSource(pOriginal.getSource(), pFilter);

        // Create new image
        return Toolkit.getDefaultToolkit().createImage(source);
    }

    /**
     * Tries to use H/W-accelerated code for an image for display purposes.
     * Note that transparent parts of the image might be replaced by solid
     * color. Additional image information not used by the current diplay
     * hardware may be discarded, like extra bith depth etc.
     *
     * @param pImage any {@code Image}
     * @return a {@code BufferedImage}
     */
    public static BufferedImage accelerate(Image pImage) {
        return accelerate(pImage, null, DEFAULT_CONFIGURATION);
    }

    /**
     * Tries to use H/W-accelerated code for an image for display purposes.
     * Note that transparent parts of the image might be replaced by solid
     * color. Additional image information not used by the current diplay
     * hardware may be discarded, like extra bith depth etc.
     *
     * @param pImage any {@code Image}
     * @param pConfiguration the {@code GraphicsConfiguration} to accelerate
     * for
     *
     * @return a {@code BufferedImage}
     */
    public static BufferedImage accelerate(Image pImage, GraphicsConfiguration pConfiguration) {
        return accelerate(pImage, null, pConfiguration);
    }

    /**
     * Tries to use H/W-accelerated code for an image for display purposes.
     * Note that transparent parts of the image will be replaced by solid
     * color. Additional image information not used by the current diplay
     * hardware may be discarded, like extra bith depth etc.
     *
     * @param pImage any {@code Image}
     * @param pBackgroundColor the background color to replace any transparent
     * parts of the image.
     * May be {@code null}, in such case the color is undefined.
     * @param pConfiguration the graphics configuration
     * May be {@code null}, in such case the color is undefined.
     *
     * @return a {@code BufferedImage}
     */
    static BufferedImage accelerate(Image pImage, Color pBackgroundColor, GraphicsConfiguration pConfiguration) {
        // Skip acceleration if the layout of the image and color model is already ok
        if (pImage instanceof BufferedImage) {
            BufferedImage buffered = (BufferedImage) pImage;
            // TODO: What if the createCompatibleImage insist on TYPE_CUSTOM...? :-P
            if (buffered.getType() != BufferedImage.TYPE_CUSTOM && equals(buffered.getColorModel(), pConfiguration.getColorModel(buffered.getTransparency()))) {
                return buffered;
            }
        }
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }

        int w = ImageUtil.getWidth(pImage);
        int h = ImageUtil.getHeight(pImage);

        // Create accelerated version
        BufferedImage temp = createClear(w, h, BI_TYPE_ANY, getTransparency(pImage), pBackgroundColor, pConfiguration);
        drawOnto(temp, pImage);

        return temp;
    }

    private static int getTransparency(Image pImage) {
        if (pImage instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) pImage;
            return bi.getTransparency();
        }
        return Transparency.OPAQUE;
    }

    /**
     * Creates a transparent image.
     *
     * @param pWidth the requested width of the image
     * @param pHeight the requested height of the image
     *
     * @throws IllegalArgumentException if {@code pType} is not a valid type
     * for {@code BufferedImage}
     *
     * @return the new image
     */
    public static BufferedImage createTransparent(int pWidth, int pHeight) {
        return createTransparent(pWidth, pHeight, BI_TYPE_ANY);
    }

    /**
     * Creates a transparent image.
     *
     * @see BufferedImage#BufferedImage(int,int,int)
     *
     * @param pWidth the requested width of the image
     * @param pHeight the requested height of the image
     * @param pType the type of {@code BufferedImage} to create
     *
     * @throws IllegalArgumentException if {@code pType} is not a valid type
     * for {@code BufferedImage}
     *
     * @return the new image
     */
    public static BufferedImage createTransparent(int pWidth, int pHeight, int pType) {
        // Create
        BufferedImage image = createBuffered(pWidth, pHeight, pType, Transparency.TRANSLUCENT);

        // Clear image with transparent alpha by drawing a rectangle
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, pWidth, pHeight);
        }
        finally {
            g.dispose();
        }

        return image;
    }

    /**
     * Creates a clear image with the given background color.
     *
     * @see BufferedImage#BufferedImage(int,int,int)
     *
     * @param pWidth the requested width of the image
     * @param pHeight the requested height of the image
     * @param pBackground the background color. The color may be translucent.
     * May be {@code null}, in such case the color is undefined.
     *
     * @throws IllegalArgumentException if {@code pType} is not a valid type
     * for {@code BufferedImage}
     *
     * @return the new image
     */
    public static BufferedImage createClear(int pWidth, int pHeight, Color pBackground) {
        return createClear(pWidth, pHeight, BI_TYPE_ANY, pBackground);
    }

    /**
     * Creates a clear image with the given background color.
     *
     * @see BufferedImage#BufferedImage(int,int,int)
     *
     * @param pWidth the width of the image to create
     * @param pHeight the height of the image to create
     * @param pType the type of image to create (one of the constants from
     * {@link BufferedImage} or {@link #BI_TYPE_ANY})
     * @param pBackground the background color. The color may be translucent.
     * May be {@code null}, in such case the color is undefined.
     *
     * @throws IllegalArgumentException if {@code pType} is not a valid type
     * for {@code BufferedImage}
     *
     * @return the new image
     */
    public static BufferedImage createClear(int pWidth, int pHeight, int pType, Color pBackground) {
        return createClear(pWidth, pHeight, pType, Transparency.OPAQUE, pBackground, DEFAULT_CONFIGURATION);
    }

    static BufferedImage createClear(int pWidth, int pHeight, int pType, int pTransparency, Color pBackground, GraphicsConfiguration pConfiguration) {
        // Create
        int transparency = (pBackground != null) ? pBackground.getTransparency() : pTransparency;
        BufferedImage image = createBuffered(pWidth, pHeight, pType, transparency, pConfiguration);

        if (pBackground != null) {
            // Clear image with clear color, by drawing a rectangle
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);  // Allow color to be translucent
                g.setColor(pBackground);
                g.fillRect(0, 0, pWidth, pHeight);
            }
            finally {
                g.dispose();
            }
        }

        return image;
    }

    /**
     * Creates a {@code BufferedImage} of the given size and type. If possible,
     * uses accelerated versions of BufferedImage from GraphicsConfiguration.
     *
     * @param pWidth the width of the image to create
     * @param pHeight the height of the image to create
     * @param pType the type of image to create (one of the constants from
     * {@link BufferedImage} or {@link #BI_TYPE_ANY})
     * @param pTransparency the transparency type (from {@link Transparency})
     *
     * @return a {@code BufferedImage}
     */
    private static BufferedImage createBuffered(int pWidth, int pHeight, int pType, int pTransparency) {
        return createBuffered(pWidth, pHeight, pType, pTransparency, DEFAULT_CONFIGURATION);
    }

    static BufferedImage createBuffered(int pWidth, int pHeight, int pType, int pTransparency,
                                                GraphicsConfiguration pConfiguration) {
        if (VM_SUPPORTS_ACCELERATION && pType == BI_TYPE_ANY) {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (supportsAcceleration(env)) {
                return getConfiguration(pConfiguration).createCompatibleImage(pWidth, pHeight, pTransparency);
            }
        }

        return new BufferedImage(pWidth, pHeight, getImageType(pType, pTransparency));
    }

    private static GraphicsConfiguration getConfiguration(final GraphicsConfiguration pConfiguration) {
        return pConfiguration != null ? pConfiguration : DEFAULT_CONFIGURATION;
    }

    private static int getImageType(int pType, int pTransparency) {
        // TODO: Handle TYPE_CUSTOM?
        if (pType != BI_TYPE_ANY) {
             return pType;
        }
        else {
            switch (pTransparency) {
                case Transparency.OPAQUE:
                    return BufferedImage.TYPE_INT_RGB;
                case Transparency.BITMASK:
                case Transparency.TRANSLUCENT:
                    return BufferedImage.TYPE_INT_ARGB;
                default:
                    throw new IllegalArgumentException("Unknown transparency type: " + pTransparency);
            }
        }
    }

    /**
     * Tests if the given {@code GraphicsEnvironment} supports accelleration
     *
     * @param pEnv the environment
     * @return {@code true} if the {@code GraphicsEnvironment} supports
     * acceleration
     */
    private static boolean supportsAcceleration(GraphicsEnvironment pEnv) {
        try {
            // Acceleration only supported in non-headless environments, on 1.4+ VMs
            return /*VM_SUPPORTS_ACCELERATION &&*/ !pEnv.isHeadlessInstance();
        }
        catch (LinkageError ignore) {
            // Means we are not in a 1.4+ VM, so skip testing for headless again
            VM_SUPPORTS_ACCELERATION = false;
        }

        // If the invocation fails, assume no accelleration is possible
        return false;
    }

    /**
     * Gets the width of an Image.
     * This method has the side-effect of completely loading the image.
     *
     * @param pImage an image.
     *
     * @return the width of the image, or -1 if the width could not be
     *         determined (i.e. an error occured while waiting for the
     *         image to load).
     */
    public static int getWidth(Image pImage) {
        int width = pImage.getWidth(NULL_COMPONENT);
        if (width < 0) {
            if (!waitForImage(pImage)) {
                return -1;  // Error while waiting
            }
            width = pImage.getWidth(NULL_COMPONENT);
        }

        return width;
    }

    /**
     * Gets the height of an Image.
     * This method has the side-effect of completely loading the image.
     *
     * @param pImage an image.
     *
     * @return the height of the image, or -1 if the height could not be
     *         determined (i.e. an error occured while waiting for the
     *         image to load).
     */
    public static int getHeight(Image pImage) {
        int height = pImage.getHeight(NULL_COMPONENT);
        if (height < 0) {
            if (!waitForImage(pImage)) {
                return -1;  // Error while waiting
            }
            height =  pImage.getHeight(NULL_COMPONENT);
        }

        return height;
    }

    /**
     * Waits for an image to load completely.
     * Will wait forever.
     *
     * @param pImage an Image object to wait for.
     *
     * @return true if the image was loaded successfully, false if an error
     *         occured, or the wait was interrupted.
     *
     * @see #waitForImage(Image,long)
     */
    public static boolean waitForImage(Image pImage) {
        return waitForImages(new Image[]{pImage}, -1L);
    }

    /**
     * Waits for an image to load completely.
     * Will wait the specified time.
     *
     * @param pImage an Image object to wait for.
     * @param pTimeOut the time to wait, in milliseconds.
     *
     * @return true if the image was loaded successfully, false if an error
     *         occurred, or the wait was interrupted.
     *
     * @see #waitForImages(Image[],long)
     */
    public static boolean waitForImage(Image pImage, long pTimeOut) {
        return waitForImages(new Image[]{pImage}, pTimeOut);
    }

    /**
     * Waits for a number of images to load completely.
     * Will wait forever.
     *
     * @param pImages an array of Image objects to wait for.
     *
     * @return true if the images was loaded successfully, false if an error
     *         occurred, or the wait was interrupted.
     *
     * @see #waitForImages(Image[],long)
     */
    public static boolean waitForImages(Image[] pImages) {
        return waitForImages(pImages, -1L);
    }

    /**
     * Waits for a number of images to load completely.
     * Will wait the specified time.
     *
     * @param pImages an array of Image objects to wait for
     * @param pTimeOut the time to wait, in milliseconds
     *
     * @return true if the images was loaded successfully, false if an error
     *         occurred, or the wait was interrupted.
     */
    public static boolean waitForImages(Image[] pImages, long pTimeOut) {
        // TODO: Need to make sure that we don't wait for the same image many times
        // Use hashcode as id? Don't remove images from tracker? Hmmm...
        boolean success = true;

        // Create a local id for use with the mediatracker
        int imageId;

        // NOTE: This is very experimental...
        imageId = pImages.length == 1 ? System.identityHashCode(pImages[0]) : System.identityHashCode(pImages);

        // Add images to tracker
        for (Image image : pImages) {
            sTracker.addImage(image, imageId);

            // Start loading immediately
            if (sTracker.checkID(imageId, false)) {
                // Image is done, so remove again
                sTracker.removeImage(image, imageId);
            }
        }

        try {
            if (pTimeOut < 0L) {
                // Just wait
                sTracker.waitForID(imageId);
            }
            else {
                // Wait until timeout
                // NOTE: waitForID(int, long) return value is undocumented.
                // I assume that it returns true, if the image(s) loaded
                // successfully before the timeout, however, I always check
                // isErrorID later on, just in case...
                success = sTracker.waitForID(imageId, pTimeOut);
            }
        }
        catch (InterruptedException ie) {
            // Interrupted while waiting, image not loaded
            success = false;
        }
        finally {
            // Remove images from mediatracker
            for (Image pImage : pImages) {
                sTracker.removeImage(pImage, imageId);
            }
        }

        // If the wait was successfull, and no errors were reported for the
        // images, return true
        return success && !sTracker.isErrorID(imageId);
    }

    /**
     * Tests whether the image has any transparent or semi-transparent pixels.
     *
     * @param pImage the image
     * @param pFast if {@code true}, the method tests maximum 10 x 10 pixels,
     * evenly spaced out in the image.
     *
     * @return {@code true} if transparent pixels are found, otherwise
     * {@code false}.
     */
    public static boolean hasTransparentPixels(RenderedImage pImage, boolean pFast) {
        if (pImage == null) {
            return false;
        }

        // First, test if the ColorModel supports alpha...
        ColorModel cm = pImage.getColorModel();
        if (!cm.hasAlpha()) {
            return false;
        }

        if (cm.getTransparency() != Transparency.BITMASK
                && cm.getTransparency() != Transparency.TRANSLUCENT) {
            return false;
        }

        // ... if so, test the pixels of the image hard way
        Object data = null;

        // Loop over tiles (noramally, BufferedImages have only one)
        for (int yT = pImage.getMinTileY(); yT < pImage.getNumYTiles(); yT++) {
            for (int xT = pImage.getMinTileX(); xT < pImage.getNumXTiles(); xT++) {
                // Test pixels of each tile
                Raster raster = pImage.getTile(xT, yT);
                int xIncrement = pFast ? Math.max(raster.getWidth() / 10, 1) : 1;
                int yIncrement = pFast ? Math.max(raster.getHeight() / 10, 1) : 1;

                for (int y = 0; y < raster.getHeight(); y += yIncrement) {
                    for (int x = 0; x < raster.getWidth(); x += xIncrement) {
                        // Copy data for each pixel, without allocation array
                        data = raster.getDataElements(x, y, data);

                        // Test alpha value
                        if (cm.getAlpha(data) != 0xff) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Creates a translucent version of the given color.
     *
     * @param pColor the original color
     * @param pTransparency the transparency level ({@code 0 - 255})
     * @return a translucent color
     *
     * @throws NullPointerException if {@code pColor} is {@code null}
     */
    public static Color createTranslucent(Color pColor, int pTransparency) {
        //return new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), pTransparency);
        return new Color(((pTransparency & 0xff) << 24) | (pColor.getRGB() & 0x00ffffff), true);
    }

    /**
     * Blends two ARGB values half and half, to create a tone in between.
     *
     * @param pRGB1 color 1
     * @param pRGB2 color 2
     * @return the new rgb value
     */
    static int blend(int pRGB1, int pRGB2) {
        // Slightly modified from http://www.compuphase.com/graphic/scale3.htm
        // to support alpha values
        return (((pRGB1 ^ pRGB2) & 0xfefefefe) >> 1) + (pRGB1 & pRGB2);
    }

    /**
     * Blends two colors half and half, to create a tone in between.
     *
     * @param pColor color 1
     * @param pOther color 2
     * @return a new {@code Color}
     */
    public static Color blend(Color pColor, Color pOther) {
        return new Color(blend(pColor.getRGB(), pOther.getRGB()), true);

        /*
        return new Color((pColor.getRed() + pOther.getRed()) / 2,
                (pColor.getGreen() + pOther.getGreen()) / 2,
                (pColor.getBlue() + pOther.getBlue()) / 2,
                (pColor.getAlpha() + pOther.getAlpha()) / 2);
                */
    }

    /**
     * Blends two colors, controlled by the blending factor.
     * A factor of {@code 0.0} will return the first color,
     * a factor of {@code 1.0} will return the second.
     *
     * @param pColor color 1
     * @param pOther color 2
     * @param pBlendFactor {@code [0...1]}
     * @return a new {@code Color}
     */
    public static Color blend(Color pColor, Color pOther, float pBlendFactor) {
        float inverseBlend = (1f - pBlendFactor);
        return new Color(
                clamp((pColor.getRed() * inverseBlend) + (pOther.getRed() * pBlendFactor)),
                clamp((pColor.getGreen() * inverseBlend) + (pOther.getGreen() * pBlendFactor)),
                clamp((pColor.getBlue() * inverseBlend) + (pOther.getBlue() * pBlendFactor)),
                clamp((pColor.getAlpha() * inverseBlend) + (pOther.getAlpha() * pBlendFactor))
        );
    }

    private static int clamp(float f) {
        return (int) f;
    }
}