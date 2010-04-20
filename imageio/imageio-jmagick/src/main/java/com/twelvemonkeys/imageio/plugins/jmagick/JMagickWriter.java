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

package com.twelvemonkeys.imageio.plugins.jmagick;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.image.MagickUtil;
import com.twelvemonkeys.imageio.ImageWriterBase;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * JMagickWriter
 * <p/>
 * <em>NOTE: This ImageWriter is probably a waste of time and space, as
 * all images are converted from the given Rendered/BufferedImage,
 * first to 16bit raw ARGB samples, and then to the requested output format.
 * This is due to a limitation in the current JMagick API.</em>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: JMagickWriter.java,v 1.0 16.jan.2006 13:34:46 haku Exp$
 */
abstract class JMagickWriter extends ImageWriterBase {

    static {
        // Make sure the JMagick init is run...
        JMagick.init();
    }

    protected JMagickWriter(final JMagickImageWriterSpiSupport pProvider) {
        super(pProvider);
    }

    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method convertImageMetadata not implemented");// TODO: Implement
    }

    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method getDefaultImageMetadata not implemented");// TODO: Implement
    }

    public void write(IIOMetadata pStreamMetadata, IIOImage pImage, ImageWriteParam pParam) throws IOException {
        assertOutput();

        if (pImage.hasRaster()) {
            throw new UnsupportedOperationException("Cannot write raster");
        }

        processImageStarted(0);
        MagickImage image = null;
        try {
            // AOI & subsampling
            BufferedImage buffered = fakeAOI(ImageUtil.toBuffered(pImage.getRenderedImage()), pParam);
            buffered = ImageUtil.toBuffered(fakeSubsampling(buffered, pParam));

            // Convert to MagickImage
            image = MagickUtil.toMagick(buffered);
            processImageProgress(33f);

            // Get bytes blob from MagickImage
            String format = getFormatName().toLowerCase();
            image.setImageFormat(format);
            ImageInfo info = new ImageInfo();
            if (pParam != null && pParam.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
                int quality = (int) (pParam.getCompressionQuality() * 100);
                //System.out.println("pParam.getCompressionQuality() = " + pParam.getCompressionQuality());
                //System.out.println("quality = " + quality);
                //info.setCompression(CompressionType.JPEGCompression);
                //image.setCompression(CompressionType.JPEGCompression);

                // TODO: Is quality really correct in all cases?
                // TODO: This does not seem to do the trick..
                info.setQuality(quality);
            }
            byte[] bytes = image.imageToBlob(info);
            if (bytes == null) {
                throw new IIOException("Could not write image data in " + format + " format.");
            }
            processImageProgress(67);

            // Write blob to output
            mImageOutput.write(bytes);
            mImageOutput.flush();
            processImageProgress(100);
        }
        catch (MagickException e) {
            throw new IIOException(e.getMessage(), e);
        }
        finally {
            if (image != null) {
                image.destroyImages();// Dispose native memory
            }
        }

        processImageComplete();
    }

    public ImageWriteParam getDefaultWriteParam() {
        return createDefaultWriteParam();
    }

    protected abstract ImageWriteParam createDefaultWriteParam();

    @Override
    public void dispose() {
        // Clean up!
        super.dispose();
    }
}
