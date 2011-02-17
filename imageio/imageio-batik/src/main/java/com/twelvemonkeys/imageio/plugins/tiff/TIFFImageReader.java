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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import org.apache.batik.ext.awt.image.codec.SeekableStream;
import org.apache.batik.ext.awt.image.codec.tiff.TIFFDecodeParam;
import org.apache.batik.ext.awt.image.codec.tiff.TIFFImageDecoder;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TIFFImageReader class description.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: TIFFImageReader.java,v 1.0 29.jul.2004 12:52:33 haku Exp $
 */
// TODO: Massive clean-up
// TODO: Support raster decoding...
public class TIFFImageReader extends ImageReaderBase {

    private TIFFImageDecoder mDecoder = null;
    private List<RenderedImage> mImages = new ArrayList<RenderedImage>();

    protected TIFFImageReader(final ImageReaderSpi pOriginatingProvider) {
        super(pOriginatingProvider);
    }

    protected void resetMembers() {
        mDecoder = null;
    }

    public BufferedImage read(int pIndex, ImageReadParam pParam) throws IOException {
        // Decode image, convert and return as BufferedImage
        RenderedImage image = readAsRenderedImage(pIndex, pParam);
        return ImageUtil.toBuffered(image);
    }

    public RenderedImage readAsRenderedImage(int pIndex, ImageReadParam pParam) throws IOException {
        init(pIndex);

        processImageStarted(pIndex);

        if (pParam == null) {
            // Cache image for use by getWidth and getHeight methods
            RenderedImage image;
            if (mImages.size() > pIndex && mImages.get(pIndex) != null) {
                image = mImages.get(pIndex);
            }
            else {
                // Decode
                image = mDecoder.decodeAsRenderedImage(pIndex);

                // Make room
                for (int i = mImages.size(); i < pIndex; i++) {
                    mImages.add(pIndex, null);
                }
                mImages.add(pIndex, image);
            }

            if (abortRequested()) {
                processReadAborted();
                return image;
            }

            processImageComplete();
            return image;
        }
        else {
            // TODO: Parameter conversion
            mDecoder.setParam(new TIFFDecodeParam());

            RenderedImage image = mDecoder.decodeAsRenderedImage(pIndex);

            // Subsample and apply AOI
            if (pParam.getSourceRegion() != null) {
                image = fakeAOI(ImageUtil.toBuffered(image), pParam);
            }
            if (pParam.getSourceXSubsampling() > 1 || pParam.getSourceYSubsampling() > 1) {
                image = ImageUtil.toBuffered(fakeSubsampling(ImageUtil.toBuffered(image), pParam));
            }

            processImageComplete();
            return image;
        }
    }

    private void init(int pIndex) throws IOException {
        init();
        checkBounds(pIndex);
    }

    protected void checkBounds(int index) throws IOException {
        if (index < getMinIndex()){
            throw new IndexOutOfBoundsException("index < minIndex");
        }
        else if (index >= getNumImages(true)) {
            throw new IndexOutOfBoundsException("index > numImages");
        }
    }

    private synchronized void init() {
        if (mDecoder == null) {
            if (imageInput == null) {
                throw new IllegalStateException("input == null");
            }

            mDecoder = new TIFFImageDecoder(new SeekableStream() {
                public int read() throws IOException {
                    return imageInput.read();
                }

                public int read(final byte[] pBytes, final int pStart, final int pLength) throws IOException {
                    return imageInput.read(pBytes, pStart, pLength);
                }

                public long getFilePointer() throws IOException {
                    return imageInput.getStreamPosition();
                }

                public void seek(final long pPos) throws IOException {
                    imageInput.seek(pPos);
                }
            }, null);
        }
    }

    public int getWidth(int pIndex) throws IOException {
        init(pIndex);

        // TODO: Use cache...
        return mDecoder.decodeAsRenderedImage(pIndex).getWidth();
    }

    public int getHeight(int pIndex) throws IOException {
        init(pIndex);

        // TODO: Use cache...
        return mDecoder.decodeAsRenderedImage(pIndex).getHeight();
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Method getImageTypes not implemented");// TODO: Implement
    }

    public int getNumImages(boolean allowSearch) throws IOException {
        init();
        if (allowSearch) {
            return mDecoder.getNumPages();
        }
        return -1;
    }
}
