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
import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import org.apache.batik.ext.awt.image.codec.ImageEncodeParam;
import org.apache.batik.ext.awt.image.codec.tiff.TIFFEncodeParam;
import org.apache.batik.ext.awt.image.codec.tiff.TIFFImageEncoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * TIFFImageWriter class description.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: TIFFImageWriter.java,v 1.0 29.jul.2004 12:52:54 haku Exp $
 */
public class TIFFImageWriter extends ImageWriterBase {

    private TIFFImageEncoder mEncoder = null;

    protected TIFFImageWriter(final ImageWriterSpi pProvider) {
        super(pProvider);
    }

    @Override
    public void setOutput(final Object output) {
        mEncoder = null;
        super.setOutput(output);
    }

    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        throw new UnsupportedOperationException("Method getDefaultImageMetadata not implemented");// TODO: Implement
    }

    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        throw new UnsupportedOperationException("Method convertImageMetadata not implemented");// TODO: Implement
    }

    public void write(final IIOMetadata pStreamMetadata, final IIOImage pImage, final ImageWriteParam pParam) throws IOException {
        RenderedImage renderedImage = pImage.getRenderedImage();
        init();

        ImageEncodeParam param;
        if (pParam != null) {
            param = new TIFFEncodeParam();
            // TODO: Convert params

            mEncoder.setParam(param);
        }

        BufferedImage image;

        // FIX: TIFFEnocder chokes on a any of the TYPE_INT_* types...
        // (The TIFFEncoder expects int types to have 1 sample of size 32
        // while there actually is 4 samples of size 8, according to the
        // SampleModel...)
        if (renderedImage instanceof BufferedImage && (
                ((BufferedImage) renderedImage).getType() == BufferedImage.TYPE_INT_ARGB
                || ((BufferedImage) renderedImage).getType() == BufferedImage.TYPE_INT_ARGB_PRE)) {
            image = ImageUtil.toBuffered(renderedImage, BufferedImage.TYPE_4BYTE_ABGR);
        }
        else if (renderedImage instanceof BufferedImage && (
                ((BufferedImage) renderedImage).getType() == BufferedImage.TYPE_INT_BGR
                || ((BufferedImage) renderedImage).getType() == BufferedImage.TYPE_INT_RGB)) {
            image = ImageUtil.toBuffered(renderedImage, BufferedImage.TYPE_3BYTE_BGR);
        }
        else {
            image = ImageUtil.toBuffered(renderedImage);
        }

        image = fakeAOI(image, pParam);
        image = ImageUtil.toBuffered(fakeSubsampling(image, pParam));

        /*
        System.out.println("Image: " + pImage);
        SampleModel sampleModel = pImage.getSampleModel();
        System.out.println("SampleModel: " + sampleModel);
        int sampleSize[] = sampleModel.getSampleSize();
        System.out.println("Samples: " + sampleSize.length);
        for (int i = 0; i < sampleSize.length; i++) {
            System.out.println("SampleSize[" + i + "]: " + sampleSize[i]);
        }
        int dataType = sampleModel.getDataType();
        System.out.println("DataType: " + dataType);
        */

        processImageStarted(0);

        mEncoder.encode(image);
        imageOutput.flush();

        processImageComplete();
    }

    public void dispose() {
        super.dispose();
        mEncoder = null;
    }

    private synchronized void init() {
        if (mEncoder == null) {
            if (imageOutput == null) {
                throw new IllegalStateException("output == null");
            }
            mEncoder = new TIFFImageEncoder(IIOUtil.createStreamAdapter(imageOutput), null);
        }
    }
}
