/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static com.twelvemonkeys.imageio.plugins.jpeg.JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0;

/**
 * JPEGImageWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriter.java,v 1.0 06.02.12 16:39 haraldk Exp$
 */
public final class JPEGImageWriter extends ImageWriterBase {
    /** Our JPEG writing delegate */
    private final ImageWriter delegate;

    /** Listens to progress updates in the delegate, and delegates back to this instance */
    private final ProgressDelegator progressDelegator;

    public JPEGImageWriter(final JPEGImageWriterSpi provider, final ImageWriter delegate) {
        super(provider);
        this.delegate = delegate;

        progressDelegator = new ProgressDelegator();
    }

    private void installListeners() {
        delegate.addIIOWriteProgressListener(progressDelegator);
        delegate.addIIOWriteWarningListener(progressDelegator);
    }

    @Override
    protected void resetMembers() {
        delegate.reset();

        installListeners();
    }

    @Override
    public void setOutput(final Object output) {
        super.setOutput(output);

        delegate.setOutput(output);
    }

    @Override
    public Object getOutput() {
        return delegate.getOutput();
    }

    @Override
    public Locale[] getAvailableLocales() {
        return delegate.getAvailableLocales();
    }

    @Override
    public void setLocale(Locale locale) {
        delegate.setLocale(locale);
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return delegate.getDefaultWriteParam();
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(final ImageWriteParam param) {
        return delegate.getDefaultStreamMetadata(param);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return delegate.getDefaultImageMetadata(imageType, param);
    }

    @Override
    public IIOMetadata convertStreamMetadata(final IIOMetadata inData, final ImageWriteParam param) {
        return delegate.convertStreamMetadata(inData, param);
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return delegate.convertImageMetadata(inData, imageType, param);
    }

    @Override
    public int getNumThumbnailsSupported(final ImageTypeSpecifier imageType, final ImageWriteParam param, final IIOMetadata streamMetadata, final IIOMetadata imageMetadata) {
        return delegate.getNumThumbnailsSupported(imageType, param, streamMetadata, imageMetadata);
    }

    @Override
    public Dimension[] getPreferredThumbnailSizes(final ImageTypeSpecifier imageType, final ImageWriteParam param, final IIOMetadata streamMetadata, final IIOMetadata imageMetadata) {
        return delegate.getPreferredThumbnailSizes(imageType, param, streamMetadata, imageMetadata);
    }

    @Override
    public boolean canWriteRasters() {
        return delegate.canWriteRasters();
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        if (isDestinationCMYK(image, param)) {
            writeCMYK(streamMetadata, image, param);
        }
        else {
            delegate.write(streamMetadata, image, param);
        }
    }

    private boolean isDestinationCMYK(final IIOImage image, final ImageWriteParam param) {
        // If destination type != null, rendered image type doesn't matter
        return !image.hasRaster() && image.getRenderedImage().getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK
                || param != null && param.getDestinationType() != null && param.getDestinationType().getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK;
    }

    private void writeCMYK(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        RenderedImage renderedImage = image.getRenderedImage();
        boolean overrideDestination = param != null && param.getDestinationType() != null;
        ImageTypeSpecifier destinationType = overrideDestination
                                             ? param.getDestinationType()
                                             : ImageTypeSpecifier.createFromRenderedImage(renderedImage);

        ColorSpace cmykCS = destinationType.getColorModel().getColorSpace();

        IIOMetadata metadata = delegate.getDefaultImageMetadata(destinationType, param);

        IIOMetadataNode jpegMeta = new IIOMetadataNode(JAVAX_IMAGEIO_JPEG_IMAGE_1_0);
        jpegMeta.appendChild(new IIOMetadataNode("JPEGVariety")); // Just leave as default

        IIOMetadataNode markerSequence = new IIOMetadataNode("markerSequence");
        jpegMeta.appendChild(markerSequence);

        IIOMetadataNode app14Adobe = new IIOMetadataNode("app14Adobe");
        app14Adobe.setAttribute("transform", "0"); // 0 for CMYK, 2 for YCCK
        markerSequence.appendChild(app14Adobe);

        if (cmykCS instanceof ICC_ColorSpace) {
            ICC_Profile profile = ((ICC_ColorSpace) cmykCS).getProfile();
            byte[] profileData = profile.getData();

            String segmentId = "ICC_PROFILE";
            int idLength = segmentId.length();
            byte[] segmentIdBytes = segmentId.getBytes(StandardCharsets.US_ASCII);

            int maxSegmentLength = Short.MAX_VALUE - Short.MIN_VALUE - idLength - 3 - 2;

            int count = (int) Math.ceil(profileData.length / (float) maxSegmentLength);

            for (int i = 0; i < count; i++) {
                // Insert unknown marker tags, as app2ICC can only be subtag of jpegVariety/JFIF :-P
                IIOMetadataNode icc = new IIOMetadataNode("unknown");
                icc.setAttribute("MarkerTag", String.valueOf(JPEG.APP2 & 0xFF));

                int segmentLength = Math.min(maxSegmentLength, profileData.length - i * maxSegmentLength);
                byte[] data = new byte[idLength + 3 + segmentLength];

                System.arraycopy(segmentIdBytes, 0, data, 0, idLength);
                data[idLength] = 0;     // null-terminator
                data[idLength + 1] = (byte) (1 + i); // index
                data[idLength + 2] = (byte) count;
                System.arraycopy(profileData, i * maxSegmentLength, data, idLength + 3, segmentLength);

                icc.setUserObject(data);

                markerSequence.appendChild(icc);
            }
        }

        metadata.mergeTree(JAVAX_IMAGEIO_JPEG_IMAGE_1_0, jpegMeta);

        Raster raster = new InvertedRaster(getRaster(renderedImage));

        // TODO: For YCCK we need oposite conversion
//            for (int i = 0; i < data.length; i += 4) {
//                YCbCrConverter.convertYCbCr2RGB(data, data, i);
//            }

        if (overrideDestination) {
            // Avoid javax.imageio.IIOException: Invalid argument to native writeImage
            param.setDestinationType(null);
        }

        try {
            delegate.write(streamMetadata, new IIOImage(raster, null, metadata), param);
        }
        finally {
            if (overrideDestination) {
                param.setDestinationType(destinationType);
            }
        }
    }

    // TODO: Candidate util method
    private static Raster getRaster(final RenderedImage image) {
        return image instanceof BufferedImage
               ? ((BufferedImage) image).getRaster()
               : image.getNumXTiles() == 1 && image.getNumYTiles() == 1
                 ? image.getTile(0, 0)
                 : image.getData();
    }

    @Override
    public boolean canWriteSequence() {
        return delegate.canWriteSequence();
    }

    @Override
    public void prepareWriteSequence(final IIOMetadata streamMetadata) throws IOException {
        delegate.prepareWriteSequence(streamMetadata);
    }

    @Override
    public void writeToSequence(final IIOImage image, final ImageWriteParam param) throws IOException {
        delegate.writeToSequence(image, param);
    }

    @Override
    public void endWriteSequence() throws IOException {
        delegate.endWriteSequence();
    }

    @Override
    public boolean canReplaceStreamMetadata() throws IOException {
        return delegate.canReplaceStreamMetadata();
    }

    @Override
    public void replaceStreamMetadata(final IIOMetadata streamMetadata) throws IOException {
        delegate.replaceStreamMetadata(streamMetadata);
    }

    @Override
    public boolean canReplaceImageMetadata(final int imageIndex) throws IOException {
        return delegate.canReplaceImageMetadata(imageIndex);
    }

    @Override
    public void replaceImageMetadata(final int imageIndex, final IIOMetadata imageMetadata) throws IOException {
        delegate.replaceImageMetadata(imageIndex, imageMetadata);
    }

    @Override
    public boolean canInsertImage(final int imageIndex) throws IOException {
        return delegate.canInsertImage(imageIndex);
    }

    @Override
    public void writeInsert(final int imageIndex, final IIOImage image, final ImageWriteParam param) throws IOException {
        delegate.writeInsert(imageIndex, image, param);
    }

    @Override
    public boolean canRemoveImage(final int imageIndex) throws IOException {
        return delegate.canRemoveImage(imageIndex);
    }

    @Override
    public void removeImage(final int imageIndex) throws IOException {
        delegate.removeImage(imageIndex);
    }

    @Override
    public boolean canWriteEmpty() throws IOException {
        return delegate.canWriteEmpty();
    }

    @Override
    public void prepareWriteEmpty(final IIOMetadata streamMetadata, final ImageTypeSpecifier imageType,
                                  final int width, final int height,
                                  final IIOMetadata imageMetadata, final List<? extends BufferedImage> thumbnails,
                                  final ImageWriteParam param) throws IOException {
        delegate.prepareWriteEmpty(streamMetadata, imageType, width, height, imageMetadata, thumbnails, param);
    }

    @Override
    public void endWriteEmpty() throws IOException {
        delegate.endWriteEmpty();
    }

    @Override
    public boolean canInsertEmpty(final int imageIndex) throws IOException {
        return delegate.canInsertEmpty(imageIndex);
    }

    @Override
    public void prepareInsertEmpty(final int imageIndex, final ImageTypeSpecifier imageType,
                                   final int width, final int height,
                                   final IIOMetadata imageMetadata, final List<? extends BufferedImage> thumbnails,
                                   final ImageWriteParam param) throws IOException {
        delegate.prepareInsertEmpty(imageIndex, imageType, width, height, imageMetadata, thumbnails, param);
    }

    @Override
    public void endInsertEmpty() throws IOException {
        delegate.endInsertEmpty();
    }

    @Override
    public boolean canReplacePixels(final int imageIndex) throws IOException {
        return delegate.canReplacePixels(imageIndex);
    }

    @Override
    public void prepareReplacePixels(final int imageIndex, final Rectangle region) throws IOException {
        delegate.prepareReplacePixels(imageIndex, region);
    }

    @Override
    public void replacePixels(final RenderedImage image, final ImageWriteParam param) throws IOException {
        delegate.replacePixels(image, param);
    }

    @Override
    public void replacePixels(final Raster raster, final ImageWriteParam param) throws IOException {
        delegate.replacePixels(raster, param);
    }

    @Override
    public void endReplacePixels() throws IOException {
        delegate.endReplacePixels();
    }

    @Override
    public void abort() {
        super.abort();
        delegate.abort();
    }

    @Override
    public void reset() {
        super.reset();
        delegate.reset();
    }

    @Override
    public void dispose() {
        super.dispose();
        delegate.dispose();
    }

    /**
     * Helper class, returns sample values inverted,
     * as CMYK values needs to be written inverted (255 - value).
     */
    private static class InvertedRaster extends WritableRaster {
        InvertedRaster(final Raster raster) {
            super(raster.getSampleModel(), new DataBuffer(raster.getDataBuffer().getDataType(), raster.getDataBuffer().getSize()) {
                private final DataBuffer delegate = raster.getDataBuffer();

                @Override
                public int getElem(final int bank, final int i) {
                    return (255 - delegate.getElem(bank, i));
                }

                @Override
                public void setElem(int bank, int i, int val) {
                    throw new UnsupportedOperationException("setElem");
                }
            }, new Point());
        }
    }

    private class ProgressDelegator extends ProgressListenerBase implements IIOWriteWarningListener {
        @Override
        public void imageComplete(ImageWriter source) {
            processImageComplete();
        }

        @Override
        public void imageProgress(ImageWriter source, float percentageDone) {
            processImageProgress(percentageDone);
        }

        @Override
        public void imageStarted(ImageWriter source, int imageIndex) {
                processImageStarted(imageIndex);
        }

        @Override
        public void thumbnailComplete(ImageWriter source) {
            processThumbnailComplete();
        }

        @Override
        public void thumbnailProgress(ImageWriter source, float percentageDone) {
            processThumbnailProgress(percentageDone);
        }

        @Override
        public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
            processThumbnailStarted(imageIndex, thumbnailIndex);
        }

        @Override
        public void writeAborted(ImageWriter source) {
            processWriteAborted();
        }

        public void warningOccurred(ImageWriter source, int imageIndex, String warning) {
            processWarningOccurred(imageIndex, warning);
        }
    }
}
