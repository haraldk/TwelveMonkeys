/*
 * Copyright (c) 2022, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DLOCChunk.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DLOCChunk.java,v 1.0 31/01/2022 haraldk Exp$
 */
final class DLOCChunk extends IFFChunk {
    int width;
    int height;
    int x;
    int y;

     DLOCChunk(final int chunkLength) {
        super(IFF.CHUNK_DLOC, chunkLength);
    }

    @Override
    void readChunk(final DataInput input) throws IOException {
        if (chunkLength != 8) {
            throw new IIOException("Unknown DLOC chunk length: " + chunkLength);
        }

        width = input.readUnsignedShort();
        height = input.readUnsignedShort();
        x = input.readShort();
        y = input.readShort();
    }

    @Override
    void writeChunk(final DataOutput output) {
        throw new InternalError("Not implemented: writeChunk()");
    }

    @Override
    public String toString() {
        return super.toString() +
                "{width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y + '}';
    }
}
