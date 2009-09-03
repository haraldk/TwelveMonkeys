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

package com.twelvemonkeys.imageio.plugins.jmagick;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * TargaImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: TargaImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 wmhakur Exp $
 */
public class TIFFImageReaderSpi extends JMagickImageReaderSpiSupport {
    public TIFFImageReaderSpi() {
        super(
                new String[]{"tiff", "TIFF"},
                new String[]{"tif", "tiff"},
                new String[]{"image/x-tiff", "image/tiff"},
                TIFFImageReader.class.getName(),
                new String[] {"com.twlevemonkeys.imageio.plugins.jmagick.TIFFImageWriterSpi"}
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {'M', 'M', 0, 42},                     // TIFF Motorola byte order
        //                  new byte[] {'I', 'I', 42, 0}},                    // TIFF Intel byte order
        byte[] magic = new byte[4];
        pSource.readFully(magic);
        return (magic[0] == 'M' && magic[1] == 'M' && magic[2] == 0 && magic[3] == 42) ||
                (magic[0] == 'I' && magic[1] == 'I' && magic[2] == 42 && magic[3] == 0);
    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new TIFFImageReader(this);
    }
}
