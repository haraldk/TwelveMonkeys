/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp;

import static com.twelvemonkeys.imageio.plugins.webp.WebPImageReader.fourCC;

/**
 * An abstract RIFF chunk.
 * <p/>
 * RIFF was introduced in 1991 by Microsoft and IBM, and was presented
 * by Microsoft as the default format for Windows 3.1 multimedia files. It is
 * based on Electronic Arts' Interchange File Format, introduced in 1985 on
 * the Commodore Amiga, the only difference being that multi-byte integers are
 * in little-endian format, native to the 80x86 processor series used in
 * IBM PCs, rather than the big-endian format native to the 68k processor
 * series used in Amiga and Apple Macintosh computers, where IFF files were
 * heavily used.
 * <p/>
 * In 2010 Google introduced the WebP picture format, which uses RIFF as a
 * container.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Resource_Interchange_File_Format">Resource Interchange Format</a>
 */
abstract class RIFFChunk {

    final int fourCC;
    final long length;
    final long offset;

     RIFFChunk(int fourCC, long length, long offset) {
        this.fourCC = fourCC;
        this.length = length;
        this.offset = offset;
     }

    @Override
    public String toString() {
        return fourCC(fourCC).replace(' ', '_')
               + "Chunk@" + offset + "|" + length;
    }

}
