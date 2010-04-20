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

/**
 * IFF format constants.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IFF.java,v 1.0 07.mar.2006 15:31:48 haku Exp$
 */
interface IFF {
    /** IFF FORM group chunk */
    int CHUNK_FORM = ('F' << 24) + ('O' << 16) + ('R' << 8) + 'M';

    /** IFF ILBM form type */
    int TYPE_ILBM = ('I' << 24) + ('L' << 16) + ('B' << 8) + 'M';

    /** IFF PBM form type */
    int TYPE_PBM  = ('P' << 24) + ('B' << 16) + ('M' << 8) + ' ';

    /** Bitmap Header chunk */
    int CHUNK_BMHD = ('B' << 24) + ('M' << 16) + ('H' << 8) + 'D';

    /** Color map chunk */
    int CHUNK_CMAP = ('C' << 24) + ('M' << 16) + ('A' << 8) + 'P';

    /** Hotspot chunk (cursors, brushes) */
    int CHUNK_GRAB = ('G' << 24) + ('R' << 16) + ('A' << 8) + 'B';

    /** Destination merge data chunk */
    int CHUNK_DEST = ('D' << 24) + ('E' << 16) + ('S' << 8) + 'T';

    /** Sprite information chunk */
    int CHUNK_SPRT = ('S' << 24) + ('P' << 16) + ('R' << 8) + 'T';

    /** Commodore Amiga viewport mode chunk (used to determine HAM and EHB modes) */
    int CHUNK_CAMG = ('C' << 24) + ('A' << 16) + ('M' << 8) + 'G';

    /** Main data (body) chunk */
    int CHUNK_BODY = ('B' << 24) + ('O' << 16) + ('D' << 8) + 'Y';
}
