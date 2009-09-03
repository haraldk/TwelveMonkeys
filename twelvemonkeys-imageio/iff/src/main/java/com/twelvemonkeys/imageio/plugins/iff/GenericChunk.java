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

package com.twelvemonkeys.imageio.plugins.iff;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * UnknownChunk
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: UnknownChunk.java,v 1.0 28.feb.2006 00:53:47 haku Exp$
 */
class GenericChunk extends IFFChunk {

    byte[] mData;

    protected GenericChunk(int pChunkId, int pChunkLength) {
        super(pChunkId, pChunkLength);
        mData = new byte[pChunkLength <= 50 ? pChunkLength : 47];
    }

    protected GenericChunk(int pChunkId, byte[] pChunkData) {
        super(pChunkId, pChunkData.length);
        mData = pChunkData;
    }

    void readChunk(DataInput pInput) throws IOException {
        pInput.readFully(mData, 0, mData.length);

        int toSkip = mChunkLength - mData.length;
        while (toSkip > 0) {
            toSkip -= pInput.skipBytes(toSkip);
        }

        // Read pad
        if (mChunkLength % 2 != 0) {
            pInput.readByte();
        }
    }

    void writeChunk(DataOutput pOutput) throws IOException {
        pOutput.writeInt(mChunkId);
        pOutput.writeInt(mChunkLength);
        pOutput.write(mData, 0, mData.length);

        if (mData.length % 2 != 0) {
            pOutput.writeByte(0); // PAD
        }
    }

    public String toString() {
        return super.toString() + " {value=\""
                + new String(mData, 0, mData.length <= 50 ? mData.length : 47)
                + (mChunkLength <= 50 ? "" : "...") + "\"}";
    }
}
