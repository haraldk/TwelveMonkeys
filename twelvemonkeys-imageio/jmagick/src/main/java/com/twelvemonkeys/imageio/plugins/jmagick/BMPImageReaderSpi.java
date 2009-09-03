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
 * BMPImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: BMPImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class BMPImageReaderSpi extends JMagickImageReaderSpiSupport {
    public BMPImageReaderSpi() {
        super(
                new String[]{"bmp", "BMP"},
                new String[]{"bmp", "rle", "dib"},
                new String[]{"image/bmp", "image/x-bmp", "image/x-windows-bmp", "image/x-ms-bmp"}, // http://en.wikipedia.org/wiki/Windows_Bitmap
                BMPImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.BMPImageWriterSpi"}
        );
    }

    @Override
    public BMPImageReader createReaderImpl(Object pExtension) throws IOException {
        return new BMPImageReader(this);
    }

    @Override
    public boolean canDecode(ImageInputStream pSource) throws IOException {
        //new byte[][] {new byte[] {'B', 'M'}, new byte[] {'M', 'B'}, },  // BMP
        //new byte[][] {new byte[] {0x00, 0x00, 0x02, 0x00,
        //                          0x01, 0x00, 0x20, 0x20}},             // CUR
        //new byte[][] {new byte[] {0x0, 0x0, 0x1, 0x0}},                 // ICO (best guess)
        byte[] magic = new byte[2];
        pSource.readFully(magic);

        return (magic[0] == 'B' && magic[1] == 'M') || (magic[0] == 'M' && magic[1] == 'B');
    }

}
