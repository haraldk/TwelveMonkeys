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
import openize.isobmff.BoxType;
import openize.isobmff.ItemReferenceBox;
import openize.isobmff.SingleItemTypeReferenceBox;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

/**
 * ImageReader for ISO/IEC 23008-12:2017 HEIF (HEIC) format.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HEICImageReader.java,v 1.0 25.10.11 18:42 haraldk Exp$
 */
public final class HEICImageReader extends ImageReaderBase {

    /**
     * Maximum plausible decoded-to-input expansion ratio for HEVC compressed data,
     * used to bound the allocation against the input length.
     */
    private static final int MAX_EXPANSION_RATIO = 2048;

    private HeicImage heicImage;
    private long[] frameIds;
    private Map<Long, List<Long>> thumbnailIds;

    public HEICImageReader() {
        this(new HEICImageReaderSpi());
    }

    HEICImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        heicImage = null;
        frameIds = null;
        thumbnailIds = null;
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
            // Exclude thumbnails ('thmb') and auxiliary images like alpha and depth maps ('auxl'),
            // thumbnails are exposed through the thumbnail API, and alpha is already applied
            long[] otherFrameIds = heicImage.getFrames().entrySet().stream()
                    .filter(e -> e.getKey() != defaultFrameId)
                    .filter(e -> {
                        BoxType derivativeType = e.getValue().getDerivativeType();
                        return derivativeType != BoxType.thmb && derivativeType != BoxType.auxl;
                    })
                    .mapToLong(Map.Entry::getKey)
                    .sorted()
                    .toArray();
            frameIds = new long[otherFrameIds.length + 1];
            frameIds[0] = defaultFrameId;
            System.arraycopy(otherFrameIds, 0, frameIds, 1, otherFrameIds.length);

            // Link thumbnails to their master images, using the 'thmb' item references
            thumbnailIds = new HashMap<>();
            ItemReferenceBox iref = heicImage.getHeader().getMeta().getiref();
            if (iref != null && iref.references != null) {
                for (SingleItemTypeReferenceBox reference : iref.references) {
                    if (reference.type == BoxType.thmb && heicImage.getAllFrames().containsKey(reference.from_item_ID)) {
                        for (long masterId : reference.to_item_ID) {
                            thumbnailIds.computeIfAbsent(masterId, k -> new ArrayList<>())
                                        .add(reference.from_item_ID);
                        }
                    }
                }
            }

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

        validateSourceSize(getRawImageType(imageIndex), width, height, imageInput.length(), MAX_EXPANSION_RATIO);
        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster raster = destination.getRaster();
        if (raster.getDataBuffer().getDataType() != DataBuffer.TYPE_INT
                || !(raster.getSampleModel() instanceof SinglePixelPackedSampleModel)) {
            throw new IIOException(String.format("Unsupported destination image: %s", destination));
        }

        int[] destPixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        SinglePixelPackedSampleModel sampleModel = (SinglePixelPackedSampleModel) raster.getSampleModel();

        processImageStarted(imageIndex);
        try {
            processImageProgress(0);

            if (abortRequested()) {
                processReadAborted();
                return destination;
            }

            int xSub = param != null ? param.getSourceXSubsampling() : 1;
            int ySub = param != null ? param.getSourceYSubsampling() : 1;

            // We can only decode directly into the destination buffer, if the destination
            // region starts at origin and has the exact same layout as the source region
            boolean direct = xSub == 1 && ySub == 1
                    && destRegion.x == 0 && destRegion.y == 0
                    && destRegion.width == srcRegion.width && destRegion.height == srcRegion.height
                    && sampleModel.getScanlineStride() == srcRegion.width
                    && destOffset(sampleModel, raster, 0, 0) == 0;

            // If not, we need to decode to a temp buffer and copy rows into place
            int[] pixels = direct ? destPixels : new int[srcRegion.width * srcRegion.height];

            // Decode the entire source region
            HeicImageFrame frame = getFrame(imageIndex);
            frame.getInt32Array(PixelFormat.Argb32, new openize.heic.decoder.Rectangle(srcRegion.x, srcRegion.y, srcRegion.width, srcRegion.height), pixels);
            processImageProgress(90);

            if (!direct) {
                // Copy (and subsample if needed) rows into destination,
                // honoring destination offset and scanline stride
                int srcWidth = Math.min(srcRegion.width, (destRegion.width - 1) * xSub + 1);

                for (int y = 0; y < destRegion.height; y++) {
                    if (abortRequested()) {
                        processReadAborted();
                        return destination;
                    }

                    int srcPos = y * ySub * srcRegion.width;
                    int destPos = destOffset(sampleModel, raster, destRegion.x, destRegion.y + y);

                    subsampleRow(pixels, srcPos, srcWidth, destPixels, destPos, 1, 32, xSub);
                    processImageProgress(90 + 10f * y / destRegion.height);
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

    private static int destOffset(SinglePixelPackedSampleModel sampleModel, Raster raster, int x, int y) {
        // Computes the data buffer offset for pixel (x, y), taking scanline stride
        // and possible raster translation (e.g. sub-rasters) into account
        return sampleModel.getOffset(x - raster.getSampleModelTranslateX(), y - raster.getSampleModelTranslateY());
    }

    // Metadata

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        return new HEICImageMetadata(getImageTypes(imageIndex).next(), getFrame(imageIndex));
    }

    // Thumbnail support

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        List<Long> thumbnails = thumbnailIds.get(frameIds[imageIndex]);
        return thumbnails != null ? thumbnails.size() : 0;
    }

    private HeicImageFrame getThumbnailFrame(int imageIndex, int thumbnailIndex) throws IOException {
        int numThumbnails = getNumThumbnails(imageIndex);
        if (thumbnailIndex < 0 || thumbnailIndex >= numThumbnails) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds (" + thumbnailIndex + " >= " + numThumbnails + ")");
        }

        return heicImage.getAllFrames().get(thumbnailIds.get(frameIds[imageIndex]).get(thumbnailIndex));
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        return (int) getThumbnailFrame(imageIndex, thumbnailIndex).getWidth();
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        return (int) getThumbnailFrame(imageIndex, thumbnailIndex).getHeight();
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        HeicImageFrame thumbnail = getThumbnailFrame(imageIndex, thumbnailIndex);

        int width = (int) thumbnail.getWidth();
        int height = (int) thumbnail.getHeight();

        processThumbnailStarted(imageIndex, thumbnailIndex);

        BufferedImage image = new BufferedImage(width, height, thumbnail.hasAlpha()
                                                              ? BufferedImage.TYPE_INT_ARGB
                                                              : BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        try {
            thumbnail.getInt32Array(PixelFormat.Argb32, new openize.heic.decoder.Rectangle(0, 0, width, height), pixels);
        }
        catch (openize.io.IOException rtioe) {
            throw new IIOException(rtioe.getMessage(), rtioe);
        }

        processThumbnailProgress(100);
        processThumbnailComplete();

        return image;
    }
}
