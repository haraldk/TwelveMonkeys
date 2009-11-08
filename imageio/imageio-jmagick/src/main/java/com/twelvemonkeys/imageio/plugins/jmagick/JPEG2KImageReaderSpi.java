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
 * JPEG2KImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: JPEG2KImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class JPEG2KImageReaderSpi extends JMagickImageReaderSpiSupport {
    public JPEG2KImageReaderSpi() {
        super(
                new String[]{"jpeg2000", "jpeg 2000"},
                new String[]{"jp2"},
                new String[]{"image/jp2", "image/jpeg2000"},
                JPEG2KImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.JPEG2KImageWriterSpi"}
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' ', 0x0D, 0x0A, (byte) 0x87, 0x0A}, // JPEG 2000 JP2 format
        //                  new byte[] {(byte) 0xff, 0x4f}},                                                        // JPEG 2000 codestream format
        byte[] magic = new byte[12];
        pSource.readFully(magic);

        return (magic[0] == 0x00 && magic[1] == 0x00 && magic[2] == 0x00 &&
                magic[3] == 0x0C && magic[4] == 'j' && magic[5] == 'P' &&
                magic[6] == ' ' && magic[7] == ' ' && magic[8] == 0x0D &&
                magic[9] == 0x0A && magic[10] == (byte) 0x87 && magic[11] == 0x0A) ||
                (magic[0] == (byte) 0xFF && magic[1] == 0x4F);
    }

    protected JPEG2KImageReader createReaderImpl(final Object pExtension) throws IOException {
        return new JPEG2KImageReader(this);
    }
}
