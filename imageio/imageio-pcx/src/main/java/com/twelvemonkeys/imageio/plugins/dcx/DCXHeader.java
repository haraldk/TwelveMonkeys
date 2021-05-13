/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.dcx;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;

final class DCXHeader {
    private final int[] offsetTable;

    DCXHeader(final int[] offsetTable) {
        this.offsetTable = offsetTable;
    }

    public int getCount() {
        return offsetTable.length;
    }

    public long getOffset(final int index) {
        return (0xffffffffL & offsetTable[index]);
    }

    public static DCXHeader read(final ImageInputStream imageInput) throws IOException {
        // typedef struct _DcxHeader
        // {
        //     DWORD Id;                      /* DCX Id number */
        //     DWORD PageTable[1024];         /* Image offsets */
        // } DCXHEAD;

        int magic = imageInput.readInt();
        if (magic != DCX.MAGIC) {
            throw new IIOException(String.format("Not a DCX file. Expected DCX magic %02x, read %02x", DCX.MAGIC, magic));
        }

        int[] offsets = new int[1024];

        int count = 0;
        do {
            offsets[count] = imageInput.readInt();
        }
        while (offsets[count] != 0 && count++ < offsets.length);

        return new DCXHeader(count == offsets.length ? offsets : Arrays.copyOf(offsets, count));
    }

    @Override
    public String toString() {
        return "DCXHeader[" +
                "offsetTable=" + Arrays.toString(offsetTable) +
                ']';
    }
}
