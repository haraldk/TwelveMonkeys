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
 * PNMImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: PNMImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class PNMImageReaderSpi extends JMagickImageReaderSpiSupport {
    public PNMImageReaderSpi() {
        super(
                new String[]{
                        "pnm", "PNM", "pbm", "PBM", "pgm", "PGM", "ppm", "PPM"
                },
                new String[]{
                        "pbm", "PBM", "pgm", "PGM", "ppm", "PPM"
                },
                new String[]{
                        "image/portable-pixmap", "image/x-portable-pixmap",
                        "image/portable-bitmap", "image/x-portable-bitmap",
                        "image/portable-graymap", "image/x-portable-graymap", "image/x-portable-graymap",
                        "image/portable-anymap", "image/x-portable-anymap", "application/x-portable-anymap"
                },
                PNMImageReader.class.getName(),
                null
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {'P', '1'}, new byte[] {'P', '4'}},    // PBM ascii & raw
        //    new byte[][] {new byte[] {'P', '2'}, new byte[] {'P', '5'}},    // PGM ascii & raw
        //    new byte[][] {new byte[] {'P', '3'}, new byte[] {'P', '6'}},    // PPM ascii & raw
        byte[] magic = new byte[2];
        pSource.readFully(magic);
        return magic[0] == 'P' && magic[1] >= '1' && magic[1] <= '6';
    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new PNMImageReader(this);
    }
}
