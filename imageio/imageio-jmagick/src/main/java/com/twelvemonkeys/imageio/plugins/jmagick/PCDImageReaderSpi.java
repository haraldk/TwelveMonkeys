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
 * PCDImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: PCDImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 wmhakur Exp $
 */
public class PCDImageReaderSpi extends JMagickImageReaderSpiSupport {
    public PCDImageReaderSpi() {
        super(
                new String[]{"pcd", "PCD"},
                new String[]{"pcd", "PCD"},
                new String[]{"image/pcd", "image/x-pcd"},
                PCDImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.PCXImageWriterSpi"}
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //final static byte[] PCD_MAGIC = new byte[] {0x50, 0x43, 0x44, 0x5f, 0x49, 0x50, 0x49};
        if (pSource.length() > 2055) {
            pSource.seek(2048);

            byte[] magic = new byte[7];
            pSource.readFully(magic);

            // Kodak PhotoCD PCD_IPI
            return magic[0] == 'P' && magic[1] == 'C' && magic[2] == 'D'
                    && magic[3] == '_' && magic[4] == 'I' && magic[5] == 'P'
                    && magic[6] == 'I';
        }
        
        return false;
    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new PCDImageReader(this);
    }
}
