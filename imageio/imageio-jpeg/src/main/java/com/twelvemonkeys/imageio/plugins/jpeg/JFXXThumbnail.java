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

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.plugins.jpeg.ThumbnailReader.IndexedThumbnailReader;
import com.twelvemonkeys.imageio.plugins.jpeg.ThumbnailReader.JPEGThumbnailReader;
import com.twelvemonkeys.imageio.plugins.jpeg.ThumbnailReader.UncompressedThumbnailReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import java.io.IOException;

/**
 * JFXXThumbnailReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFXXThumbnailReader.java,v 1.0 18.04.12 12:19 haraldk Exp$
 */
final class JFXXThumbnail {

    private JFXXThumbnail() {
    }

    static ThumbnailReader from(final JFXX segment, final ImageReader thumbnailReader) throws IOException {
        if (segment != null) {
            if (segment.thumbnail != null && segment.thumbnail.length > 2) {
                switch (segment.extensionCode) {
                    case JFXX.JPEG:
                        if (((segment.thumbnail[0] & 0xff) << 8 | segment.thumbnail[1] & 0xff) == JPEG.SOI) {
                            return new JPEGThumbnailReader(thumbnailReader, new ByteArrayImageInputStream(segment.thumbnail), 0);
                        }

                        break;

                    case JFXX.INDEXED:
                        int w = segment.thumbnail[0] & 0xff;
                        int h = segment.thumbnail[1] & 0xff;

                        if (segment.thumbnail.length >= 2 + 768 + w * h) {
                            return new IndexedThumbnailReader(w, h, segment.thumbnail, 2, segment.thumbnail, 2 + 768);
                        }

                        break;

                    case JFXX.RGB:
                        w = segment.thumbnail[0] & 0xff;
                        h = segment.thumbnail[1] & 0xff;

                        if (segment.thumbnail.length >= 2 + w * h * 3) {
                            return new UncompressedThumbnailReader(w, h, segment.thumbnail, 2);
                        }

                        break;

                    default:
                        throw new IIOException(String.format("Unknown JFXX extension code: %d, ignoring thumbnail", segment.extensionCode));
                }
            }

            throw new IIOException("JFXX segment truncated, ignoring thumbnail");
        }

        return null;
    }
}
