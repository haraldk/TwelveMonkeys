/*
 * Copyright (c) 2026, Harald Kuhr
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

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PCHGChunkTest {

    private static IndexColorModel base() {
        return new IndexColorModel(4, 16, new int[16], 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    private static PCHGChunk parse(final byte[] body) throws IOException {
        PCHGChunk chunk = new PCHGChunk(body.length);
        chunk.readChunk(new ByteArrayImageInputStream(body));
        return chunk;
    }

    // 20 byte PCHG header followed by 12 bytes of change data:
    // uncompressed, 32 bit (BigLineChanges), startLine 0, lineCount 2,
    // one changed line (row 1), one change carrying a palette register.
    private static byte[] chunkWithRegister(final int reg) {
        return new byte[] {
                0x00, 0x00,             // compression = PCHG_COMP_NONE
                0x00, 0x02,             // flags = PCHGF_32BIT
                0x00, 0x00,             // startLine = 0
                0x00, 0x02,             // lineCount = 2
                0x00, 0x01,             // changedLines = 1
                0x00, 0x00,             // minReg = 0
                0x00, 0x00,             // maxReg = 0
                0x00, 0x00,             // maxChangesPerLine (ignored)
                0x00, 0x00, 0x00, 0x01, // totalChanges = 1
                0x40, 0x00, 0x00, 0x00, // line mask: only row 1 changed
                0x00, 0x01,             // changeCount = 1
                (byte) ((reg >> 8) & 0xff), (byte) (reg & 0xff), // register
                0x00, 0x11, 0x22, 0x33, // alpha (skipped), r, b, g
        };
    }

    @Test
    void specialIngoreRegisterIsIgnored() throws IOException {
        // reg 0xffff reads back as a negative short -1; adjustColorMap must not index the palette with it
        PCHGChunk chunk = parse(chunkWithRegister(0xffff));
        IndexColorModel base = base();

        ColorModel palette = assertDoesNotThrow(() -> chunk.getColorModel(base, 1, false));

        assertNotNull(palette);
    }
    @Test
    void parseNegativeRegisterThrows() {
        // reg 0xfffe reads back as a negative short -2; adjustColorMap must not index the palette with it
        assertThrows(IIOException.class, () -> parse(chunkWithRegister(0xfffe)));
    }

    @Test
    void inRangeRegisterIsApplied() throws IOException {
        PCHGChunk chunk = parse(chunkWithRegister(5));
        IndexColorModel base = base();

        ColorModel palette = chunk.getColorModel(base, 1, false);

        // r=0x11 b=0x22 g=0x33 -> 0x113322
        assertEquals(0x113322, palette.getRGB(5) & 0xffffff);
    }

    @Test
    void truncatedChunkLengthThrows() {
        // Declared chunk length shorter than the 20 byte PCHG header: data = new byte[length - 20] must not go negative
        PCHGChunk chunk = new PCHGChunk(10);
        assertThrows(IIOException.class, () -> chunk.readChunk(new ByteArrayImageInputStream(new byte[24])));
    }

    @Test
    void negativeLineRangeThrows() {
        // startLine + lineCount is used as the changes array length, and must not go negative
        byte[] body = new byte[32];
        body[3] = 0x02;                 // flags = PCHGF_32BIT
        body[4] = (byte) 0xFF;          // startLine = -100
        body[5] = (byte) 0x9C;
        // lineCount = 0

        assertThrows(IIOException.class, () -> parse(body));
    }

    @Test
    void invalidInitialRegisterRangeThrows() {
        // startLine < 0 allocates initialChanges from maxReg - minReg + 1, which must not go negative
        byte[] body = new byte[32];
        body[3] = 0x02;                 // flags = PCHGF_32BIT
        body[4] = (byte) 0xFF;          // startLine = -1
        body[5] = (byte) 0xFF;
        body[7] = 0x02;                 // lineCount = 2
        body[9] = 0x01;                 // changedLines = 1
        body[11] = 0x0A;                // minReg = 10
        // maxReg = 0

        assertThrows(IIOException.class, () -> parse(body));
    }

    @Test
    void initialChangeRegisterOutOfRangeThrows() {
        // reg is used as initialChanges[reg - minReg]; a register outside [minReg, maxReg] must not index out of bounds
        byte[] body = new byte[] {
                0x00, 0x00,             // compression = PCHG_COMP_NONE
                0x00, 0x02,             // flags = PCHGF_32BIT
                (byte) 0xFF, (byte) 0xFF, // startLine = -1
                0x00, 0x02,             // lineCount = 2
                0x00, 0x01,             // changedLines = 1
                0x00, 0x00,             // minReg = 0
                0x00, 0x00,             // maxReg = 0 -> initialChanges length 1
                0x00, 0x00,             // maxChangesPerLine (ignored)
                0x00, 0x00, 0x00, 0x01, // totalChanges = 1
                (byte) 0x80, 0x00, 0x00, 0x00, // line mask: row -1 changed
                0x00, 0x01,             // changeCount = 1
                0x00, 0x05,             // register 5, outside [0, 0]
                0x00, 0x11, 0x22, 0x33, // alpha (skipped), r, b, g
        };

        assertThrows(IIOException.class, () -> parse(body));
    }

    @Test
    void negativeStartLineWithVisibleChangeParses() {
        // startLine < 0 with a change on a visible (row >= 0) line: the per-line loop must not index changes[] with a negative row
        byte[] body = new byte[] {
                0x00, 0x00,             // compression = PCHG_COMP_NONE
                0x00, 0x02,             // flags = PCHGF_32BIT
                (byte) 0xFF, (byte) 0xFF, // startLine = -1
                0x00, 0x03,             // lineCount = 3 -> changes length 2
                0x00, 0x02,             // changedLines = 2
                0x00, 0x00,             // minReg = 0
                0x00, 0x1F,             // maxReg = 31 -> initialChanges length 32
                0x00, 0x00,             // maxChangesPerLine (ignored)
                0x00, 0x00, 0x00, 0x02, // totalChanges = 2
                (byte) 0xC0, 0x00, 0x00, 0x00, // line mask: row -1 and the first visible row changed
                0x00, 0x01, 0x00, 0x03, 0x00, 0x11, 0x22, 0x33, // initial change, register 3
                0x00, 0x01, 0x00, 0x04, 0x00, 0x44, 0x55, 0x66, // visible change, register 4
        };

        assertDoesNotThrow(() -> parse(body));
    }
}
