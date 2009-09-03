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

import java.io.IOException;
import javax.imageio.stream.ImageInputStream;

/**
 * WBMPImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: WBMPImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class WBMPImageReaderSpi extends JMagickImageReaderSpiSupport {
    public WBMPImageReaderSpi() {
        super(
                new String[]{"wbmp", "WBMP"},
                new String[]{"wbmp"},
                new String[]{"image/vnd.wap.wbmp"},
                WBMPImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.WBMPImageWriterSpi"});
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {0, 0}},                               // WBMP
        byte[] magic = new byte[2];
        pSource.readFully(magic);
        return magic[0] == 0x00 && magic[1] == 0x00 &&
                readMultiByteInteger(pSource) > 0 && readMultiByteInteger(pSource) > 0;// Positive size
        // TODO: Consider testin if the size of the stream after the header matches
        // the dimensions: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6331418
    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new WBMPImageReader(this);
    }

    private static int readMultiByteInteger(ImageInputStream pStream) throws IOException {
        int value = 0;
        int b;

        // Read while continuation bit is set
        while ((b = pStream.read()) >= 0) {
            value = (value << 7) + (b & 0x7f);

            // Test continuation bit, if not set, return value
            if ((b & 0x80) == 0) {
                return value;
            }
        }

        // If we got here, value could not be read
        return -1;
    }

}
