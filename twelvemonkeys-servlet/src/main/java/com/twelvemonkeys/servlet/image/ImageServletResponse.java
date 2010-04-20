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

package com.twelvemonkeys.servlet.image;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;

/**
 * ImageServletResponse.
 * <p/>
 * The request attributes regarding image size and source region (AOI) are used
 * in the decoding process, and must be set before the first invocation of
 * {@link #getImage()} to have any effect.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ImageServletResponse.java#4 $
 */
public interface ImageServletResponse extends ServletResponse {
    /**
     * Request attribute of type {@link java.awt.Dimension} controlling image
     * size.
     * If either {@code width} or {@code height} is negative, the size is
     * computed, using uniform scaling.
     * Else, if {@code SIZE_UNIFORM} is {@code true}, the size will be
     * computed to the largest possible area (with correct aspect ratio)
     * fitting inside the target area.
     * Otherwise, the image is scaled to the given size, with no regard to
     * aspect ratio.
     * <p/>
     * Defaults to {@code null} (original image size).
     */
    String ATTRIB_SIZE = "com.twelvemonkeys.servlet.image.ImageServletResponse.SIZE";

    /**
     * Request attribute of type {@link Boolean} controlling image sizing.
     * <p/>
     * Defaults to {@code Boolean.TRUE}.
     */
    String ATTRIB_SIZE_UNIFORM = "com.twelvemonkeys.servlet.image.ImageServletResponse.SIZE_UNIFORM";

    /**
     * Request attribute of type {@link Boolean} controlling image sizing.
     * <p/>
     * Defaults to {@code Boolean.FALSE}.
     */
    String ATTRIB_SIZE_PERCENT = "com.twelvemonkeys.servlet.image.ImageServletResponse.SIZE_PERCENT";

    /**
     * Request attribute of type {@link java.awt.Rectangle} controlling image
     * source region (area of interest).
     * <p/>
     * Defaults to {@code null} (the entire image).
     */
    String ATTRIB_AOI = "com.twelvemonkeys.servlet.image.ImageServletResponse.AOI";

    /**
     * Request attribute of type {@link Boolean} controlling image AOI.
     * <p/>
     * Defaults to {@code Boolean.FALSE}.
     */
    String ATTRIB_AOI_UNIFORM = "com.twelvemonkeys.servlet.image.ImageServletResponse.AOI_UNIFORM";

    /**
     * Request attribute of type {@link Boolean} controlling image AOI.
     * <p/>
     * Defaults to {@code Boolean.FALSE}.
     */
    String ATTRIB_AOI_PERCENT = "com.twelvemonkeys.servlet.image.ImageServletResponse.AOI_PERCENT";

    /**
     * Request attribute of type {@link java.awt.Color} controlling background
     * color for any transparent/translucent areas of the image.
     * <p/>
     * Defaults to {@code null} (keeps the transparent areas transparent).
     */
    String ATTRIB_BG_COLOR = "com.twelvemonkeys.servlet.image.ImageServletResponse.BG_COLOR";

    /**
     * Request attribute of type {@link Float} controlling image output compression/quality.
     * Used for formats that accepts compression or quality settings,
     * like JPEG (quality), PNG (compression only) etc.
     * <p/>
     * Defaults to {@code 0.8f} for JPEG.
     */
    String ATTRIB_OUTPUT_QUALITY = "com.twelvemonkeys.servlet.image.ImageServletResponse.OUTPUT_QUALITY";

    /**
     * Request attribute of type {@link Double} controlling image read
     * subsampling factor. Controls the maximum sample pixels in each direction,
     * that is read per pixel in the output image, if the result will be
     * downscaled.
     * Larger values will result in better quality, at the expense of higher
     * memory consumption and CPU usage.
     * However, using values above {@code 3.0} will usually not improve image
     * quality.
     * Legal values are in the range {@code [1.0 .. positive infinity&gt;}.
     * <p/>
     * Defaults to {@code 2.0}.
     */
    String ATTRIB_READ_SUBSAMPLING_FACTOR = "com.twelvemonkeys.servlet.image.ImageServletResponse.READ_SUBSAMPLING_FACTOR";

    /**
     * Request attribute of type {@link Integer} controlling image resample
     * algorithm.
     * Legal values are {@link java.awt.Image#SCALE_DEFAULT SCALE_DEFAULT},
     * {@link java.awt.Image#SCALE_FAST SCALE_FAST} or
     * {@link java.awt.Image#SCALE_SMOOTH SCALE_SMOOTH}.
     * <p/>
     * Note: When using a value of {@code SCALE_FAST}, you should also use a
     * subsampling factor of {@code 1.0}, for fast read/scale.
     * Otherwise, use a subsampling factor of {@code 2.0} for better quality.
     * <p/>
     * Defaults to {@code SCALE_DEFAULT}.
     */
    String ATTRIB_IMAGE_RESAMPLE_ALGORITHM = "com.twelvemonkeys.servlet.image.ImageServletResponse.IMAGE_RESAMPLE_ALGORITHM";

    /**
     * Gets the image format for this response, such as "image/gif" or "image/jpeg".
     * If not set, the default format is that of the original image.
     *
     * @return the image format for this response.
     * @see #setOutputContentType(String)
     */
    String getOutputContentType();

    /**
     * Sets the image format for this response, such as "image/gif" or "image/jpeg".
     * <p/>
     * As an example, a custom filter could do content negotiation based on the
     * request header fields and write the image back in an appropriate format.
     * <p/>
     * If not set, the default format is that of the original image.
     *
     * @param pImageFormat the image format for this response.
     */
    void setOutputContentType(String pImageFormat);

    //TODO: ?? void setCompressionQuality(float pQualityFactor);
    //TODO: ?? float getCompressionQuality();

    /**
     * Writes the image to the original {@code ServletOutputStream}.
     * If no format is {@linkplain #setOutputContentType(String) set} in this response,
     * the image is encoded in the same format as the original image.
     *
     * @throws java.io.IOException if an I/O exception occurs during writing
     */
    void flush() throws IOException;

    /**
     * Gets the decoded image from the response.
     *
     * @return a {@code BufferedImage} or {@code null} if the image could not be read.
     *
     * @throws java.io.IOException if an I/O exception occurs during reading
     */
    BufferedImage getImage() throws IOException;

    /**
     * Sets the image for this response.
     *
     * @param pImage the new response image.
     */
    void setImage(RenderedImage pImage);
}
