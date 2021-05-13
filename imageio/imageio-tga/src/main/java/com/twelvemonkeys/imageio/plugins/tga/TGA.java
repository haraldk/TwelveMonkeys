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

package com.twelvemonkeys.imageio.plugins.tga;

interface TGA {
    byte[] MAGIC = {'T', 'R', 'U', 'E', 'V', 'I', 'S', 'I', 'O', 'N', '-', 'X', 'F', 'I', 'L', 'E', '.', 0};

    /** Fixed header size: 18.*/
    int HEADER_SIZE = 18;

    int EXT_AREA_SIZE = 495;

    /** No color map included. */
    int COLORMAP_NONE = 0;
    /** Color map included. */
    int COLORMAP_PALETTE = 1;

    /** No image data included. */
    int IMAGETYPE_NONE = 0;
    /** Uncompressed, color-mapped images. */
    int IMAGETYPE_COLORMAPPED = 1;
    /** Uncompressed, RGB images. */
    int IMAGETYPE_TRUECOLOR = 2;
    /** Uncompressed, black and white images. */
    int IMAGETYPE_MONOCHROME = 3;
    /** Runlength encoded color-mapped images. */
    int IMAGETYPE_COLORMAPPED_RLE = 9;
    /** Runlength encoded RGB images. */
    int IMAGETYPE_TRUECOLOR_RLE = 10;
    /** Compressed, black and white images. */
    int IMAGETYPE_MONOCHROME_RLE = 11;

    /* From http://www.gamers.org/dEngine/quake3/TGA.txt: */
    /** Compressed color-mapped data, using Huffman, Delta, and runlength encoding. */
    int IMAGETYPE_COLORMAPPED_HUFFMAN = 32;
    /** Compressed color-mapped data, using Huffman, Delta, and runlength encoding.  4-pass quadtree-type process. */
    int IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE = 33;

    /* Only origin lower left and upper left supported. */
    int ORIGIN_LOWER_LEFT = 0;
    int ORIGIN_LOWER_RIGHT = 1;
    int ORIGIN_UPPER_LEFT = 2;
    int ORIGIN_UPPER_RIGHT = 3;

    /* From http://www.gamers.org/dEngine/quake3/TGA.txt: */
    int INTERLEAVED_NON_INTERLEAVED = 0;
    int INTERLEAVED_TWO_WAY = 1;
    int INTERLEAVED_FOUR_WAY = 2;
    // The value 3 is reserved...
}
