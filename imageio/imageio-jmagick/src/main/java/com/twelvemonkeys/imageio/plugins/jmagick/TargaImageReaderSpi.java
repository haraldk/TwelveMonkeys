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
public class TargaImageReaderSpi extends JMagickImageReaderSpiSupport {
    public TargaImageReaderSpi() {
        super(
                new String[]{"tga", "targa", "TGA", "TARGA"},
                new String[]{"tga"},
                new String[]{"image/x-tga", "image/targa"},
                TargaImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.TargaImageWriterSpi"}
        );
    }

    boolean canDecode(final ImageInputStream pSource) throws IOException {
        //    // TODO: Targa 1989 signature look like (bytes 8-23 of 26 LAST bytes):
        //    // 'T', 'R', 'U', 'E', 'V', 'I', 'S', 'I', 'O', 'N', '-', 'X', 'F', 'I', 'L', 'E'
        //    // Targa 1987:
        //    new byte[][] {new byte[] {-1, 0x01, 0x01},                      // Type 1: CM
        //                  /*
        //                  TODO: Figure out how to not interfere with CUR: 0x00000200
        //                  new byte[] {-1, 0x00, 0x02},*/ new byte[] {-1, 0x01, 0x02}, // T2: RGB w & w/o CM
        //                  new byte[] {-1, 0x00, 0x03},                      // Type 3: B/W
        //                  new byte[] {-1, 0x01, 0x09},                      // Type 9: RLE CM
        //                  new byte[] {-1, 0x00, 0x0a}, new byte[] {-1, 0x01, 0x0a}, // T10: RLE RGB w & w/o CM
        //                  new byte[] {-1, 0x00, 0x0b},                      // Type 11: Compressed B/W
        //                  new byte[] {-1, 0x01, 0x20},                      // Type 31: Compressed CM
        //                  new byte[] {-1, 0x01, 0x21},                      // Type 32: Compressed CM, 4 pass
        //                  },

        // If we don't know the stream length, just give up, as the Targa format has trailing magic bytes...
        if (pSource.length() < 0) {
            return false;
        }

        pSource.seek(pSource.length() - 18);
        byte[] magic = new byte[18];
        pSource.readFully(magic);

        return "TRUEVISIOM-XFILE".equals(new String(magic, 0, 16));
    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new TargaImageReader(this);
    }
}
