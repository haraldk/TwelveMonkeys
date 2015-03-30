/*
 * Copyright (c) 2010, Harald Kuhr
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

import com.twelvemonkeys.lang.Validate;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * A factory for creating {@link BufferedImage}s backed by memory mapped files.
 * The data buffers will be allocated outside the normal JVM heap, allowing more efficient
 * memory usage for large images.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedImageFactory.java,v 1.0 May 26, 2010 5:07:01 PM haraldk Exp$
 */
public final class MappedImageFactory {

    // TODO: Create a way to do ColorConvertOp (or other color space conversion) on these images. 
    // - Current implementation of CCOp delegates to internal sun.awt classes that assumes java.awt.DataBufferByte for type byte buffers :-/
    // - Might be possible (but slow) to copy parts to memory and do CCOp on these copies

    private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.image.mapped.debug"));

    /* Constants for DirectColorModel masks, from BufferedImage. */
    private static final int DCM_RED_MASK   = 0x00ff0000;
    private static final int DCM_GREEN_MASK = 0x0000ff00;
    private static final int DCM_BLUE_MASK  = 0x000000ff;
    private static final int DCM_ALPHA_MASK = 0xff000000;
    private static final int DCM_565_RED_MASK = 0xf800;
    private static final int DCM_565_GRN_MASK = 0x07E0;
    private static final int DCM_565_BLU_MASK = 0x001F;
    private static final int DCM_555_RED_MASK = 0x7C00;
    private static final int DCM_555_GRN_MASK = 0x03E0;
    private static final int DCM_555_BLU_MASK = 0x001F;
    private static final int DCM_BGR_RED_MASK = 0x0000ff;
    private static final int DCM_BGR_GRN_MASK = 0x00ff00;
    private static final int DCM_BGR_BLU_MASK = 0xff0000;

    static final RasterFactory RASTER_FACTORY = createRasterFactory();

    private MappedImageFactory() {}

    public static BufferedImage createCompatibleMappedImage(int width, int height, int type) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        return createCompatibleMappedImage(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, GraphicsConfiguration configuration, int transparency) throws IOException {
        return createCompatibleMappedImage(width, height, configuration.getColorModel(transparency));
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, ImageTypeSpecifier type) throws IOException {
        return createCompatibleMappedImage(width, height, type.getSampleModel(width, height), type.getColorModel());
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, ColorModel cm) throws IOException {
        return createCompatibleMappedImage(width, height, cm.createCompatibleSampleModel(width, height), cm);
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, SampleModel sm, ColorModel cm) throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);

        return new BufferedImage(cm, RASTER_FACTORY.createRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
    }

    /**
     * <p>
     * Returns the {@code BufferedImage} image type that is compatible with the data in {@code image}.
     * This method will return <em>compatible</em> types, even if {@code BufferedImage.getType()} returns
     * {@code BufferedImage.TYPE_CUSTOM}.
     * </p>
     * <p>
     * This method is defined to work so that, for any valid {@code BufferedImage} <em>type</em>
     * (except {@code BufferedImage.TYPE_CUSTOM}), the following is {@code true}:
     * <br/>
     * {@code getCompatibleBufferedImageType(createCompatibleMappedImage(w, h, type)) == type}
     * </p>
     * <p>
     * If no standard type is compatible with the image data, {@code BufferedImage.TYPE_CUSTOM} is returned.
     * </p>
     *
     * @param image the image to test, may not be {@code null}.
     *
     * @return the {@code BufferedImage} type.
     *
     * @throws java.lang.IllegalArgumentException if {@code image} is {@code null}.
     *
     * @see java.awt.image.BufferedImage#getType()
     */
    public static int getCompatibleBufferedImageType(final BufferedImage image) {
        Validate.notNull(image, "image");

        WritableRaster raster = image.getRaster();
        SampleModel sm = raster.getSampleModel();
        int numBands = raster.getNumBands();

        ColorModel cm = image.getColorModel();
        ColorSpace cs = cm.getColorSpace();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        int csType = cs.getType();

        int dataType = raster.getDataBuffer().getDataType();

        if (csType != ColorSpace.TYPE_RGB) {
            if (csType == ColorSpace.TYPE_GRAY && cm instanceof ComponentColorModel) {
                if (sm instanceof ComponentSampleModel && ((ComponentSampleModel) sm).getPixelStride() != numBands) {
                    return BufferedImage.TYPE_CUSTOM;
                }
                else if (dataType == DataBuffer.TYPE_BYTE && raster.getNumBands() == 1 &&
                        cm.getComponentSize(0) == 8 && ((ComponentSampleModel) sm).getPixelStride() == 1) {
                    return BufferedImage.TYPE_BYTE_GRAY;
                }
                else if (dataType == DataBuffer.TYPE_USHORT && raster.getNumBands() == 1 &&
                        cm.getComponentSize(0) == 16 && ((ComponentSampleModel) sm).getPixelStride() == 1) {
                    return BufferedImage.TYPE_USHORT_GRAY;
                }
            }
            else {
                return BufferedImage.TYPE_CUSTOM;
            }
        }

        if ((dataType == DataBuffer.TYPE_INT) && (numBands == 3 || numBands == 4)) {
            // Check if the raster params and the color model are correct
            int pixSize = cm.getPixelSize();

            if (cm instanceof DirectColorModel && sm.getNumDataElements() == 1 && (pixSize == 32 || pixSize == 24)) {
                // Now check on the DirectColorModel params
                DirectColorModel dcm = (DirectColorModel) cm;
                int rmask = dcm.getRedMask();
                int gmask = dcm.getGreenMask();
                int bmask = dcm.getBlueMask();

                if (rmask == DCM_RED_MASK && gmask == DCM_GREEN_MASK && bmask == DCM_BLUE_MASK) {
                    if (dcm.getAlphaMask() == DCM_ALPHA_MASK) {
                        return isAlphaPre ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_ARGB;
                    }
                    else if (!dcm.hasAlpha()) {
                        // No Alpha
                        return BufferedImage.TYPE_INT_RGB;
                    }
                }
                else if (rmask == DCM_BGR_RED_MASK && gmask == DCM_BGR_GRN_MASK && bmask == DCM_BGR_BLU_MASK) {
                    if (!dcm.hasAlpha()) {
                        return BufferedImage.TYPE_INT_BGR;
                    }
                }
            }
        }
        else if ((cm instanceof IndexColorModel) && (numBands == 1) && (!cm.hasAlpha() || !isAlphaPre)) {
            IndexColorModel icm = (IndexColorModel) cm;
            int pixSize = icm.getPixelSize();

            if (dataType == DataBuffer.TYPE_BYTE && sm instanceof MultiPixelPackedSampleModel) {
                return BufferedImage.TYPE_BYTE_BINARY;
            }
            if (dataType == DataBuffer.TYPE_BYTE && sm instanceof ComponentSampleModel) {
                ComponentSampleModel csm = (ComponentSampleModel) sm;

                if (csm.getPixelStride() == 1 && pixSize <= 8) {
                    return BufferedImage.TYPE_BYTE_INDEXED;
                }
            }
        }
        else if ((dataType == DataBuffer.TYPE_USHORT) &&
                (cm instanceof DirectColorModel) && (numBands == 3) && !cm.hasAlpha()) {
            DirectColorModel dcm = (DirectColorModel) cm;

            if (dcm.getRedMask() == DCM_565_RED_MASK &&
                    dcm.getGreenMask() == DCM_565_GRN_MASK && dcm.getBlueMask() == DCM_565_BLU_MASK) {
                return BufferedImage.TYPE_USHORT_565_RGB;
            }
            else if (dcm.getRedMask() == DCM_555_RED_MASK &&
                    dcm.getGreenMask() == DCM_555_GRN_MASK && dcm.getBlueMask() == DCM_555_BLU_MASK) {
                return BufferedImage.TYPE_USHORT_555_RGB;
            }
        }
        else if (dataType == DataBuffer.TYPE_BYTE && cm instanceof ComponentColorModel &&
                raster.getSampleModel() instanceof PixelInterleavedSampleModel && (numBands == 3 || numBands == 4)) {
            ComponentColorModel ccm = (ComponentColorModel) cm;
            PixelInterleavedSampleModel csm = (PixelInterleavedSampleModel) raster.getSampleModel();

            int[] offs = csm.getBandOffsets();
            int[] nBits = ccm.getComponentSize();
            boolean is8bit = true;

            for (int i = 0; i < numBands; i++) {
                if (nBits[i] != 8) {
                    is8bit = false;
                    break;
                }
            }

            if (is8bit && csm.getPixelStride() == numBands &&
                    offs[0] == numBands - 1 && offs[1] == numBands - 2 && offs[2] == numBands - 3) {
                if (numBands == 3 && !ccm.hasAlpha()) {
                    return BufferedImage.TYPE_3BYTE_BGR;
                }
                else if (offs[3] == 0 && ccm.hasAlpha()) {
                    return isAlphaPre ? BufferedImage.TYPE_4BYTE_ABGR_PRE : BufferedImage.TYPE_4BYTE_ABGR;
                }
            }
        }

        return BufferedImage.TYPE_CUSTOM;
    }

    private static RasterFactory createRasterFactory() {
        try {
            // Try to instantiate, will throw LinkageError if it fails
            return new SunRasterFactory();
        }
        catch (LinkageError e) {
            if (DEBUG) {
                e.printStackTrace();
            }

            System.err.println("Could not instantiate SunWritableRaster, falling back to GenericWritableRaster.");
        }

        // Fall back
        return new GenericRasterFactory();
    }

    static interface RasterFactory {
        WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin);
    }

    /**
     * Generic implementation that should work for any JRE, and creates a custom subclass of {@link WritableRaster}.
     */
    static final class GenericRasterFactory implements RasterFactory {
        public WritableRaster createRaster(final SampleModel model, final DataBuffer buffer, final Point origin) {
            return new GenericWritableRaster(model, buffer, origin);
        }
    }

    /**
     * Sun/Oracle JRE-specific implementation that creates {@code sun.awt.image.SunWritableRaster}.
     * Callers must catch {@link LinkageError}.
     */
    static final class SunRasterFactory implements RasterFactory {
        final private Constructor<WritableRaster> factoryMethod = getFactoryMethod();

        @SuppressWarnings("unchecked")
        private static Constructor<WritableRaster> getFactoryMethod() {
            try {
                Class<?> cls = Class.forName("sun.awt.image.SunWritableRaster");

                if (Modifier.isAbstract(cls.getModifiers())) {
                    throw new IncompatibleClassChangeError("sun.awt.image.SunWritableRaster has become abstract and can't be instantiated");
                }

                return (Constructor<WritableRaster>) cls.getConstructor(SampleModel.class, DataBuffer.class, Point.class);
            }
            catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
            catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        }

        public WritableRaster createRaster(final SampleModel model, final DataBuffer buffer, final Point origin) {
            try {
                return factoryMethod.newInstance(model, buffer, origin);
            }
            catch (InstantiationException e) {
                throw new Error("Could not create SunWritableRaster: ", e); // Should never happen, as we test for abstract class
            }
            catch (IllegalAccessException e) {
                throw new Error("Could not create SunWritableRaster: ", e); // Should never happen, only public constructors are reflected
            }
            catch (InvocationTargetException e) {
                // Unwrap to allow normal exception flow
                Throwable cause = e.getCause();

                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                else if (cause instanceof Error) {
                    throw (Error) cause;
                }

                throw new UndeclaredThrowableException(cause);
            }
        }
    }
}
