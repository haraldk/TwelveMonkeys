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

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
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

    private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.image.mapped.debug"));
    static final RasterFactory RASTER_FACTORY = createRasterFactory();

    private MappedImageFactory() {}

    public static BufferedImage createCompatibleMappedImage(int width, int height, int type) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        return createCompatibleMappedImage(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, GraphicsConfiguration configuration, int transparency) throws IOException {
//        BufferedImage temp = configuration.createCompatibleImage(1, 1, transparency);
//        return createCompatibleMappedImage(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
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
