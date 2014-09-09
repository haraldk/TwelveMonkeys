/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.xmp.XMPReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.Charset;

/**
 * XMP metadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPData.java,v 1.0 Jul 28, 2009 5:50:34 PM haraldk Exp$
 *
 * @see <a href="http://www.adobe.com/products/xmp/">Adobe Extensible Metadata Platform (XMP)</a>
 * @see <a href="http://www.adobe.com/devnet/xmp/">Adobe XMP Developer Center</a>
 */
final class PSDXMPData extends PSDImageResource {
    protected byte[] data;
    Directory directory;

    PSDXMPData(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        data = new byte[(int) size]; // TODO: Fix potential overflow, or document why that can't happen (read spec)
        pInput.readFully(data);

        // Chop off potential trailing null-termination/padding that SAX parsers don't like...
        int len = data.length;
        for (; len > 0; len--) {
            if (data[len - 1] != 0) {
                break;
            }
        }

        directory = new XMPReader().read(new ByteArrayImageInputStream(data, 0, len));
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        int length = Math.min(256, data.length);
        String data = StringUtil.decode(this.data, 0, length, "UTF-8").replace('\n', ' ').replaceAll("\\s+", " ");
        builder.append(", data: \"").append(data);

        if (length < this.data.length) {
            builder.append("...");
        }

        builder.append("\"]");

        return builder.toString();
    }   

    /**
     * Returns a character stream containing the XMP metadata (XML).
     *
     * @return the XMP metadata.
     */
    public Reader getData() {
        return new InputStreamReader(new ByteArrayInputStream(data), Charset.forName("UTF-8"));
    }
}
