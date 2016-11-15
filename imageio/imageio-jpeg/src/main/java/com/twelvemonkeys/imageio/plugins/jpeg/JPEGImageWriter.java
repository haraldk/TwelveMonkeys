/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * JPEGImageWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageWriter.java,v 1.0 06.02.12 16:39 haraldk Exp$
 */
public final class JPEGImageWriter extends ImageWriterBase {
    // TODO: Extend with functionality to be able to write CMYK JPEGs as well?

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
        installListeners();
    }

    @Override
    public void setOutput(Object output) {
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
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return delegate.getDefaultStreamMetadata(param);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        return delegate.getDefaultImageMetadata(imageType, param);
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
        return delegate.convertStreamMetadata(inData, param);
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        return delegate.convertImageMetadata(inData, imageType, param);
    }

    @Override
    public int getNumThumbnailsSupported(ImageTypeSpecifier imageType, ImageWriteParam param, IIOMetadata streamMetadata, IIOMetadata imageMetadata) {
        return delegate.getNumThumbnailsSupported(imageType, param, streamMetadata, imageMetadata);
    }

    @Override
    public Dimension[] getPreferredThumbnailSizes(ImageTypeSpecifier imageType, ImageWriteParam param, IIOMetadata streamMetadata, IIOMetadata imageMetadata) {
        return delegate.getPreferredThumbnailSizes(imageType, param, streamMetadata, imageMetadata);
    }

    @Override
    public boolean canWriteRasters() {
        return delegate.canWriteRasters();
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        delegate.write(streamMetadata, image, param);
    }

    @Override
    public void write(IIOImage image) throws IOException {
        delegate.write(image);
    }

    @Override
    public void write(RenderedImage image) throws IOException {
        delegate.write(image);
    }

    @Override
    public boolean canWriteSequence() {
        return delegate.canWriteSequence();
    }

    @Override
    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        delegate.prepareWriteSequence(streamMetadata);
    }

    @Override
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
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
    public void replaceStreamMetadata(IIOMetadata streamMetadata) throws IOException {
        delegate.replaceStreamMetadata(streamMetadata);
    }

    @Override
    public boolean canReplaceImageMetadata(int imageIndex) throws IOException {
        return delegate.canReplaceImageMetadata(imageIndex);
    }

    @Override
    public void replaceImageMetadata(int imageIndex, IIOMetadata imageMetadata) throws IOException {
        delegate.replaceImageMetadata(imageIndex, imageMetadata);
    }

    @Override
    public boolean canInsertImage(int imageIndex) throws IOException {
        return delegate.canInsertImage(imageIndex);
    }

    @Override
    public void writeInsert(int imageIndex, IIOImage image, ImageWriteParam param) throws IOException {
        delegate.writeInsert(imageIndex, image, param);
    }

    @Override
    public boolean canRemoveImage(int imageIndex) throws IOException {
        return delegate.canRemoveImage(imageIndex);
    }

    @Override
    public void removeImage(int imageIndex) throws IOException {
        delegate.removeImage(imageIndex);
    }

    @Override
    public boolean canWriteEmpty() throws IOException {
        return delegate.canWriteEmpty();
    }

    @Override
    public void prepareWriteEmpty(IIOMetadata streamMetadata, ImageTypeSpecifier imageType, int width, int height, IIOMetadata imageMetadata, List<? extends BufferedImage> thumbnails, ImageWriteParam param) throws IOException {
        delegate.prepareWriteEmpty(streamMetadata, imageType, width, height, imageMetadata, thumbnails, param);
    }

    @Override
    public void endWriteEmpty() throws IOException {
        delegate.endWriteEmpty();
    }

    @Override
    public boolean canInsertEmpty(int imageIndex) throws IOException {
        return delegate.canInsertEmpty(imageIndex);
    }

    @Override
    public void prepareInsertEmpty(int imageIndex, ImageTypeSpecifier imageType, int width, int height, IIOMetadata imageMetadata, List<? extends BufferedImage> thumbnails, ImageWriteParam param) throws IOException {
        delegate.prepareInsertEmpty(imageIndex, imageType, width, height, imageMetadata, thumbnails, param);
    }

    @Override
    public void endInsertEmpty() throws IOException {
        delegate.endInsertEmpty();
    }

    @Override
    public boolean canReplacePixels(int imageIndex) throws IOException {
        return delegate.canReplacePixels(imageIndex);
    }

    @Override
    public void prepareReplacePixels(int imageIndex, Rectangle region) throws IOException {
        delegate.prepareReplacePixels(imageIndex, region);
    }

    @Override
    public void replacePixels(RenderedImage image, ImageWriteParam param) throws IOException {
        delegate.replacePixels(image, param);
    }

    @Override
    public void replacePixels(Raster raster, ImageWriteParam param) throws IOException {
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
