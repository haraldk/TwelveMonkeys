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

package com.twelvemonkeys.imageio;

import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Abstract base class for image writers.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageWriterBase.java,v 1.0 Sep 24, 2007 12:22:28 AM haraldk Exp$
 */
public abstract class ImageWriterBase extends ImageWriter {

    /**
     * For convenience. Only set if the output is an {@code ImageInputStream}.
     * @see #setOutput(Object)
     */
    protected ImageOutputStream imageOutput;

    /**
     * Constructs an {@code ImageWriter} and sets its
     * {@code originatingProvider} instance variable to the
     * supplied value.
     * <p/>
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature {@code (ImageWriterSpi,
     * Object)} in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * {@code IllegalArgumentException} should be thrown.
     *
     * @param provider the {@code ImageWriterSpi} that is constructing this object, or {@code null}.
     */
    protected ImageWriterBase(final ImageWriterSpi provider) {
        super(provider);
    }

    public String getFormatName() throws IOException {
        return getOriginatingProvider().getFormatNames()[0];
    }

    @Override
    public void setOutput(final Object output) {
        resetMembers();
        super.setOutput(output);

        if (output instanceof ImageOutputStream) {
            imageOutput = (ImageOutputStream) output;
        }
        else {
            imageOutput = null;
        }
    }

    /**
     * Makes sure output is set.
     *
     * @throws IllegalStateException if {@code getOutput() == null}.
     */
    protected void assertOutput() {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null");
        }
    }

    @Override
    public void dispose() {
        resetMembers();
        super.dispose();
    }

    @Override
    public void reset() {
        resetMembers();
        super.reset();
    }

    protected void resetMembers() {
    }

    /**
     * Returns {@code null}
     *
     * @param param ignored.
     * @return {@code null}.
     */
    public IIOMetadata getDefaultStreamMetadata(final ImageWriteParam param) {
        return null;
    }

    /**
     * Returns {@code null}
     *
     * @param inData ignored.
     * @param param ignored.
     * @return {@code null}.
     */
    public IIOMetadata convertStreamMetadata(final IIOMetadata inData, final ImageWriteParam param) {
        return null;
    }

    protected static Rectangle getSourceRegion(final ImageWriteParam pParam, final int pWidth, final int pHeight) {
        return IIOUtil.getSourceRegion(pParam, pWidth, pHeight);
    }

    /**
     * Utility method for getting the area of interest (AOI) of an image.
     * The AOI is defined by the {@link javax.imageio.IIOParam#setSourceRegion(java.awt.Rectangle)}
     * method.
     * <p/>
     * Note: If it is possible for the writer to write the AOI directly, such a
     * method should be used instead, for efficiency.
     *
     * @param pImage the image to get AOI from
     * @param pParam the param optionally specifying the AOI
     *
     * @return a {@code BufferedImage} containing the area of interest (source
     * region), or the original image, if no source region was set, or
     * {@code pParam} was {@code null}
     */
    protected static BufferedImage fakeAOI(final BufferedImage pImage, final ImageWriteParam pParam) {
        return IIOUtil.fakeAOI(pImage, getSourceRegion(pParam, pImage.getWidth(), pImage.getHeight()));
    }

    /**
     * Utility method for getting the subsampled image.
     * The subsampling is defined by the
     * {@link javax.imageio.IIOParam#setSourceSubsampling(int, int, int, int)}
     * method.
     * <p/>
     * NOTE: This method does not take the subsampling offsets into
     * consideration.
     * <p/>
     * Note: If it is possible for the writer to subsample directly, such a
     * method should be used instead, for efficiency.
     *
     * @param pImage the image to subsample
     * @param pParam the param optionally specifying subsampling
     *
     * @return an {@code Image} containing the subsampled image, or the
     * original image, if no subsampling was specified, or
     * {@code pParam} was {@code null}
     */
    protected static Image fakeSubsampling(final Image pImage, final ImageWriteParam pParam) {
        return IIOUtil.fakeSubsampling(pImage, pParam);
    }
}