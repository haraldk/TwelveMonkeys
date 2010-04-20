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
 * PNGImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: PNGImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class PNGImageReaderSpi extends JMagickImageReaderSpiSupport {
    public PNGImageReaderSpi() {
        super(
                new String[]{"png", "PNG"},
                new String[]{"png"},
                new String[]{"image/png", "image/x-png"},
                PNGImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.PNGImageWriterSpi"}
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {(byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', // PNG
        //                              0x0d, 0x0a, 0x1a, 0x0a,}},
        byte[] magic = new byte[8];
        pSource.readFully(magic);
        return magic[0] == (byte) 0x89 &&
                magic[1] == 'P' && magic[2] == 'N' && magic[3] == 'G' &&
                magic[4] == 0x0d && magic[5] == 0x0a &&
                magic[6] == 0x1a && magic[7] == 0x0a;

    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new PNGImageReader(this);
    }
}
