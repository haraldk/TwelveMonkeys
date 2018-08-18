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
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * GRABChunk
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: GRABChunk.java,v 1.0 28.feb.2006 01:55:05 haku Exp$
 */
final class GRABChunk extends IFFChunk {
//   typedef struct {
//      WORD x, y;  /* relative coordinates (pixels) */
//   } Point2D;

    Point2D point;

    protected GRABChunk(int pChunkLength) {
        super(IFF.CHUNK_GRAB, pChunkLength);
    }

    protected GRABChunk(Point2D pPoint) {
        super(IFF.CHUNK_GRAB, 4);
        point = pPoint;
    }

    void readChunk(DataInput pInput) throws IOException {
        if (chunkLength != 4) {
            throw new IIOException("Unknown GRAB chunk size: " + chunkLength);
        }
        point = new Point(pInput.readShort(), pInput.readShort());
    }

    void writeChunk(DataOutput pOutput) throws IOException {
        pOutput.writeShort((int) point.getX());
        pOutput.writeShort((int) point.getY());
    }

    public String toString() {
        return super.toString() + " {point=" + point + "}";
    }
}
