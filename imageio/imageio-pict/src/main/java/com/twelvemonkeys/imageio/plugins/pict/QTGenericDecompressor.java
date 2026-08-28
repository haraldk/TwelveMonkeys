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

package com.twelvemonkeys.imageio.plugins.pict;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import static com.twelvemonkeys.imageio.plugins.pict.QuickTime.ImageDesc;

/**
 * QTGenericDecompressor
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: QTGenericDecompressor.java,v 1.0 Feb 16, 2009 9:26:13 PM haraldk Exp$
 */
final class QTGenericDecompressor extends QTDecompressor {
    @Override
    public boolean canDecompress(final ImageDesc description) {
        // Instead of testing, we just allow everything, and might eventually fail on decompress later...
        return true;
    }

    @Override
    public BufferedImage decompress(final ImageDesc description, final ImageInputStream stream) throws IOException {
        // First try reading based on stream contents
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if (!readers.hasNext()) {
            // Fall back to reading using format name
            readers = ImageIO.getImageReadersByFormatName(description.compressorIdentifer.trim());
        }

        if (readers.hasNext()) {
            ImageReader reader = readers.next();

            try {
                reader.setInput(stream, true, true);
                return reader.read(0);
            }
            finally {
                reader.dispose();
            }
        }

        return null;
    }

}
