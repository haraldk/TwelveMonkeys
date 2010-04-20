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

package com.twelvemonkeys.io.enc;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

/**
 * {@code Encoder} implementation for standard DEFLATE encoding.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/DeflateEncoder.java#2 $
 *
 * @see <a href="http://tools.ietf.org/html/rfc1951">RFC 1951</a>
 * @see Deflater
 * @see InflateDecoder
 * @see java.util.zip.DeflaterOutputStream
 */
final class DeflateEncoder implements Encoder {

    private final Deflater mDeflater;
    private final byte[] mBuffer = new byte[1024];

    public DeflateEncoder() {
//        this(new Deflater());
        this(new Deflater(Deflater.DEFAULT_COMPRESSION, true)); // TODO: Should we use "no wrap"?
    }

    public DeflateEncoder(final Deflater pDeflater) {
        if (pDeflater == null) {
            throw new IllegalArgumentException("deflater == null");
        }

        mDeflater = pDeflater;
    }

    public void encode(final OutputStream pStream, final byte[] pBuffer, final int pOffset, final int pLength)
            throws IOException
    {
        System.out.println("DeflateEncoder.encode");
        mDeflater.setInput(pBuffer, pOffset, pLength);
        flushInputToStream(pStream);
    }

    private void flushInputToStream(final OutputStream pStream) throws IOException {
        System.out.println("DeflateEncoder.flushInputToStream");

        if (mDeflater.needsInput()) {
            System.out.println("Foo");
        }

        while (!mDeflater.needsInput()) {
            int deflated = mDeflater.deflate(mBuffer, 0, mBuffer.length);
            pStream.write(mBuffer, 0, deflated);
            System.out.println("flushed " + deflated);
        }
    }

//    public void flush() {
//        mDeflater.finish();
//    }
}
