/*
 * Copyright (c) 2025, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.HeicImageFrame;
import openize.heic.decoder.PixelFormat;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

/**
 * ImageReader for ISO/IEC 23008-12:2017 HEIF (HEIC) format.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HEICImageReader.java,v 1.0 25.10.11 18:42 haraldk Exp$
 */
public final class HEICImageReader extends ImageReaderBase {

    private HeicImage heicImage;
    private long[] frameIds;

    public HEICImageReader() {
        this(new HEICImageReaderSpi());
    }

    HEICImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        heicImage = null;
    }

    void init() throws IIOException {
        assertInput();

        if (heicImage == null) {
            try {
                heicImage = HeicImage.load(new ImageInputStreamIOStreamAdapter(imageInput));
            }
            catch (openize.io.IOException rtioe) {
                throw new IIOException(rtioe.getMessage(), rtioe);
            }

            long defaultFrameId = heicImage.getHeader().getDefaultFrameId();

            // TODO: Sort? How?
            // TODO: Looks like we have thumbnails in here...
            long[] otherFrameIds = heicImage.getFrames().keySet().stream()
                    .mapToLong(Long::longValue)
                    .filter(l -> l != defaultFrameId)
                    .sorted()
                    .toArray();
            frameIds = new long[otherFrameIds.length + 1];
            frameIds[0] = defaultFrameId;
            System.arraycopy(otherFrameIds, 0, frameIds, 1, otherFrameIds.length);

//            System.out.println("heicImage.getHeader() = " + heicImage.getHeader());
//            System.out.println("heicImage.getFrames() = " + heicImage.getFrames());
//            System.out.println("heicImage.getAllFrames() = " + heicImage.getAllFrames());

            // TODO: Split normal frames and thumbnails (derivativeType == thmb)
            //  How are the thumbnails connected to the full size image?

            // TODO: Alpha seems to be applied, no need to decode as separate images, exclude?

            /*
            for (int i = 0; i < frameIds.length; i++) {
                HeicImageFrame heicImageFrame = getFrame(i);

                System.out.printf("frameId = %08x%n", frameIds[i]);
                System.out.println("heicImageFrame.width = " + heicImageFrame.getWidth());
                System.out.println("heicImageFrame.height = " + heicImageFrame.getHeight());
                System.out.println("heicImageFrame.hidden = " + heicImageFrame.isHidden());
                System.out.println("heicImageFrame.numberOfChannels = " + heicImageFrame.getNumberOfChannels());
                System.out.println("heicImageFrame.auxiliaryReferenceType = " + heicImageFrame.getAuxiliaryReferenceType()); // 'Alpha' if auxl and is alpha
                System.out.println("heicImageFrame.derivativeType = " + heicImageFrame.getDerivativeType()); // 'thmb' if thumbnail!, 'auxl' if Auxiliary
                System.out.println("heicImageFrame.alpha = " + heicImageFrame.hasAlpha());
                System.out.println("heicImageFrame.derived = " + heicImageFrame.isDerived());
                System.out.println("heicImageFrame.image = " + heicImageFrame.isImage());
                System.out.println("heicImageFrame.imageType = " + heicImageFrame.getImageType());
                System.out.println("heicImageFrame.textData = " + heicImageFrame.getTextData());
                System.out.println();
            }
             */
        }
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        init();

        return frameIds.length;
    }

    private HeicImageFrame getFrame(int imageIndex) {
        return heicImage.getAllFrames().get(frameIds[imageIndex]);
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        return (int) getFrame(imageIndex).getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        return (int) getFrame(imageIndex).getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        int bufferedImageType = getFrame(imageIndex).hasAlpha()
                                ? BufferedImage.TYPE_INT_ARGB
                                : BufferedImage.TYPE_INT_RGB;

        return Collections.singletonList(ImageTypeSpecifiers.createFromBufferedImageType(bufferedImageType)).iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        processImageStarted(imageIndex);
        try {
            processImageProgress(0);

            // If subsampling, destination isn't large enough, need to allocate a temp buffer
            int xSub = param != null ? param.getSourceXSubsampling() : 1;
            int ySub = param != null ? param.getSourceYSubsampling() : 1;
            boolean subsampling = xSub != 1 || ySub != 1;

            int[] destPixels = ((DataBufferInt) destination.getRaster().getDataBuffer()).getData();
            int[] pixels = subsampling ? new int[srcRegion.width * srcRegion.height] : destPixels;

            // Decode the entire source region
            HeicImageFrame frame = getFrame(imageIndex);
            frame.getInt32Array(PixelFormat.Argb32, new openize.heic.decoder.Rectangle(srcRegion.x, srcRegion.y, srcRegion.width, srcRegion.height), pixels);
            processImageProgress(90);

            // Subsample into dest if needed, otherwise we're good
            if (subsampling) {
                int ySteps = 1 + (srcRegion.height - 1) / ySub;

                for (int y = 0; y < ySteps; y++) {
                    int srcPos = y * ySub * srcRegion.width;
                    int destPos = y * destRegion.width;

                    subsampleRow(pixels, srcPos, srcRegion.width, destPixels, destPos, 1, 32, xSub);
                }
            }

            if (abortRequested()) {
                processReadAborted();
            }
            else {
                processImageProgress(100);
                processImageComplete();
            }
        }
        catch (openize.io.IOException rtioe) {
            throw new IIOException(rtioe.getMessage(), rtioe);
        }

        return destination;
    }
}
