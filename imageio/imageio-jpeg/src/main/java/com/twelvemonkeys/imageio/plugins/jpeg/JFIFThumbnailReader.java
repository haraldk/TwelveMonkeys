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

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * JFIFThumbnailReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFIFThumbnailReader.java,v 1.0 18.04.12 12:19 haraldk Exp$
 */
final class JFIFThumbnailReader extends ThumbnailReader {
    private final JFIF segment;

    JFIFThumbnailReader(final ThumbnailReadProgressListener progressListener, final int imageIndex, final int thumbnailIndex, final JFIF segment) {
        super(progressListener, imageIndex, thumbnailIndex);
        this.segment = segment;
    }

    @Override
    public BufferedImage read() {
        processThumbnailStarted();
        BufferedImage thumbnail = readRawThumbnail(segment.thumbnail, segment.thumbnail.length, 0, segment.xThumbnail, segment.yThumbnail);
        processThumbnailProgress(100f);
        processThumbnailComplete();

        return thumbnail;
    }

    @Override
    public int getWidth() throws IOException {
        return segment.xThumbnail;
    }

    @Override
    public int getHeight() throws IOException {
        return segment.yThumbnail;
    }
}
