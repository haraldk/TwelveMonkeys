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

package com.twelvemonkeys.imageio.plugins.wmf;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.plugins.svg.SVGImageReader;
import com.twelvemonkeys.imageio.plugins.svg.SVGReadParam;
import com.twelvemonkeys.imageio.util.IIOUtil;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.wmf.tosvg.WMFTranscoder;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/**
 * WMFImageReader class description.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: WMFImageReader.java,v 1.0 29.jul.2004 13:00:59 haku Exp $
 */
// TODO: Probably possible to do less wrapping/unwrapping of data...
// TODO: Consider using temp file instead of in-memory stream
public class WMFImageReader extends ImageReaderBase {

    private SVGImageReader reader = null;

    public WMFImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
    }

    protected void resetMembers() {
        if (reader != null) {
            reader.dispose();
        }

        reader = null;
    }

    public BufferedImage read(int pIndex, ImageReadParam pParam) throws IOException {
        init();

        processImageStarted(pIndex);

        BufferedImage image = reader.read(pIndex, pParam);
        if (abortRequested()) {
            processReadAborted();
            return image;
        }

        processImageComplete();

        return image;
    }

    private synchronized void init() throws IOException {
        // Need the extra test, to avoid throwing an IOException from the Transcoder
        if (imageInput == null) {
            throw new IllegalStateException("input == null");
        }

        if (reader == null) {
            WMFTranscoder transcoder = new WMFTranscoder();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(output, "UTF8");
            try {
                TranscoderInput in = new TranscoderInput(IIOUtil.createStreamAdapter(imageInput));
                TranscoderOutput out = new TranscoderOutput(writer);

                // TODO: Transcodinghints?

                transcoder.transcode(in, out);
            }
            catch (TranscoderException e) {
                throw new IIOException(e.getMessage(), e);
            }

            reader = new SVGImageReader(getOriginatingProvider());
            reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(output.toByteArray())));
        }
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new SVGReadParam();
    }

    public int getWidth(int pIndex) throws IOException {
        init();
        return reader.getWidth(pIndex);
    }

    public int getHeight(int pIndex) throws IOException {
        init();
        return reader.getHeight(pIndex);
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(final int pImageIndex) throws IOException {
        init();
        return reader.getImageTypes(pImageIndex);
    }

}
