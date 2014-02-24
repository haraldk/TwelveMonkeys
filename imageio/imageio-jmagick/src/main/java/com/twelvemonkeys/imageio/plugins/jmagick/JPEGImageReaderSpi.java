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
 * JPEGImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: JPEGImageReaderSpi.java,v 1.0 30.jul.2004 20:39:48 haku Exp $
 */
public class JPEGImageReaderSpi extends JMagickImageReaderSpiSupport {
    public JPEGImageReaderSpi() {
        super(
                new String[]{"jpeg", "JPEG", "exif", "EXIF", "jfif", "JFIF", "jpg", "JPG"},
                new String[]{"jpg", "jpeg", "jpe"},
                new String[]{"image/jpeg", "image/jfif", "image/x-exif"},
                JPEGImageReader.class.getName(),
                new String[]{"com.twelvemonkeys.imageio.plugins.jmagick.JPEGImageWriterSpi"}
        );
    }

    boolean canDecode(ImageInputStream pSource) throws IOException {
        //    new byte[][] {new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0},
        //                  new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1}},                   // JPEG
        //    new byte[][] {new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xed}},                   // PHOTOSHOP 3 JPEG
        //    new byte[][] {new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xee}},                   // JPG
        byte[] magic = new byte[4];
        pSource.readFully(magic);

        return magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8 && magic[2] == (byte) 0xFF &&
                (magic[3] == (byte) 0xE0 || magic[3] == (byte) 0xE1 || magic[3] == (byte) 0xED || magic[3] == (byte) 0xEE);

    }

    protected JMagickReader createReaderImpl(final Object pExtension) throws IOException {
        return new JPEGImageReader(this);
    }
}
