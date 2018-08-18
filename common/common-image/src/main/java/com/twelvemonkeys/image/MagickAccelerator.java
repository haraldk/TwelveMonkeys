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

import com.twelvemonkeys.lang.SystemUtil;
import magick.MagickImage;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * This class accelerates certain graphics operations, using
 * JMagick and ImageMagick, if available.
 * If those libraries are not installed, this class silently does nothing.
 * <p/>
 * Set the system property {@code "com.twelvemonkeys.image.accel"} to
 * {@code false}, to disable, even if JMagick is installed.
 * Set the system property {@code "com.twelvemonkeys.image.magick.debug"} to
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/MagickAccelerator.java#3 $
 */
final class MagickAccelerator {

    private static final boolean DEBUG = Magick.DEBUG;
    private static final boolean USE_MAGICK = useMagick();

    private static final int RESAMPLE_OP = 0;

    private static Class[] nativeOp = new Class[1];

    static {
        try {
            nativeOp[RESAMPLE_OP] = Class.forName("com.twelvemonkeys.image.ResampleOp");
        }
        catch (ClassNotFoundException e) {
            System.err.println("Could not find class: " + e);
        }
    }

    private static boolean useMagick() {
        try {
            boolean available = SystemUtil.isClassAvailable("magick.MagickImage");

            if (DEBUG && !available) {
                System.err.print("ImageMagick bindings not available.");
            }

            boolean useMagick =
                    available && !"FALSE".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.image.accel"));

            if (DEBUG) {
                System.err.println(
                        useMagick
                                ? "Will use ImageMagick bindings to accelerate image resampling operations."
                                : "Will not use ImageMagick to accelerate image resampling operations."
                );
            }

            return useMagick;
        }
        catch (Throwable t) {
            // Most probably in case of a SecurityManager
            System.err.println("Could not enable ImageMagick bindings: " + t);
            return false;
        }
    }

    private static int getNativeOpIndex(Class pOpClass) {
        for (int i = 0; i < nativeOp.length; i++) {
            if (pOpClass == nativeOp[i]) {
                return i;
            }
        }

        return -1;
    }

    public static BufferedImage filter(BufferedImageOp pOperation, BufferedImage pInput, BufferedImage pOutput) {
        if (!USE_MAGICK) {
            return null;
        }

        BufferedImage result = null;
        switch (getNativeOpIndex(pOperation.getClass())) {
            case RESAMPLE_OP:
                ResampleOp resample = (ResampleOp) pOperation;
                result = resampleMagick(pInput, resample.width, resample.height, resample.filterType);

                // NOTE: If output parameter is non-null, we have to return that
                // image, instead of result
                if (pOutput != null) {
                    //pOutput.setData(result.getRaster()); // Fast, but less compatible
                    // NOTE: For some reason, this is sometimes super-slow...?
                    ImageUtil.drawOnto(pOutput, result);
                    result = pOutput;
                }

                break;

            default:
                // Simply fall through, allowing acceleration to be added later
                break;

        }
        
        return result;
    }

    private static BufferedImage resampleMagick(BufferedImage pSrc, int pWidth, int pHeight, int pFilterType) {
        // Convert to Magick, scale and convert back
        MagickImage image = null;
        MagickImage scaled = null;
        try {
            image = MagickUtil.toMagick(pSrc);

            long start = 0;
            if (DEBUG) {
                start = System.currentTimeMillis();
            }

            // NOTE: setFilter affects zoomImage, NOT scaleImage
            image.setFilter(pFilterType);
            scaled = image.zoomImage(pWidth, pHeight);
            //scaled = image.scaleImage(pWidth, pHeight); // AREA_AVERAGING

            if (DEBUG) {
                long time = System.currentTimeMillis() - start;
                System.out.println("Filtered: " + time + " ms");
            }

            return MagickUtil.toBuffered(scaled);
        }
        //catch (MagickException e) {
        catch (Exception e) {
            // NOTE: Stupid workaround: If MagickException is caught, a
            // NoClassDefFoundError is thrown, when MagickException class is
            // unavailable...
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }

            throw new ImageConversionException(e.getMessage(), e);
        }
        finally {
            // NOTE: ImageMagick might be unstable after a while, if image data
            // is not deallocated. The GC/finalize method handles this, but in
            // special circumstances, it's not triggered often enough.
            if (image != null) {
                image.destroyImages();
            }
            if (scaled != null) {
                scaled.destroyImages();
            }
        }
    }
}