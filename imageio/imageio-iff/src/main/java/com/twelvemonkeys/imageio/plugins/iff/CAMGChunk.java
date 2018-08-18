/*
 * Copyright (c) 2008, Harald Kuhr
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
 * CAMGChunk
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: CAMGChunk.java,v 1.0 28.feb.2006 02:10:07 haku Exp$
 */
final class CAMGChunk extends IFFChunk {
    // HIRES=0x8000, LACE=0x4
    // #define CAMG_HAM 0x800   /* hold and modify */
    // #define CAMG_EHB 0x80    /* extra halfbrite */

    private int camg;

    public CAMGChunk(int pLength) {
        super(IFF.CHUNK_CAMG, pLength);
    }

    void readChunk(DataInput pInput) throws IOException {
        if (chunkLength != 4) {
            throw new IIOException("Unknown CAMG chunk length: " + chunkLength);
        }

        camg = pInput.readInt();
    }

    void writeChunk(DataOutput pOutput) throws IOException {
        throw new InternalError("Not implemented: writeChunk()");
    }

    boolean isHires() {
        return (camg & 0x8000) != 0;
    }

    boolean isLaced() {
        return (camg & 0x4) != 0;
    }

    boolean isHAM() {
        return (camg & 0x800) != 0;
    }

    boolean isEHB() {
        return (camg & 0x80) != 0;
    }

    public String toString() {
        return super.toString() + " {mode=" + (isHAM() ? "HAM" : isEHB() ? "EHB" : "Normal") + "}";
    }
}
