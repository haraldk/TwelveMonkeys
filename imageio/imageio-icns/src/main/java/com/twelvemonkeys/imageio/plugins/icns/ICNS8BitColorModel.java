/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * ICNS8BitColorModel
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNS8BitColorModel.java,v 1.0 07.11.11 10:49 haraldk Exp$
 */
final class ICNS8BitColorModel extends IndexColorModel {
    private static final int[] CMAP = {
            0xffffffff, 0xffffffcc, 0xffffff99, 0xffffff66, 0xffffff33, 0xffffff00, 0xffffccff, 0xffffcccc,
            0xffffcc99, 0xffffcc66, 0xffffcc33, 0xffffcc00, 0xffff99ff, 0xffff99cc, 0xffff9999, 0xffff9966,
            0xffff9933, 0xffff9900, 0xffff66ff, 0xffff66cc, 0xffff6699, 0xffff6666, 0xffff6633, 0xffff6600,
            0xffff33ff, 0xffff33cc, 0xffff3399, 0xffff3366, 0xffff3333, 0xffff3300, 0xffff00ff, 0xffff00cc,
            0xffff0099, 0xffff0066, 0xffff0033, 0xffff0000, 0xffccffff, 0xffccffcc, 0xffccff99, 0xffccff66,
            0xffccff33, 0xffccff00, 0xffccccff, 0xffcccccc, 0xffcccc99, 0xffcccc66, 0xffcccc33, 0xffcccc00,
            0xffcc99ff, 0xffcc99cc, 0xffcc9999, 0xffcc9966, 0xffcc9933, 0xffcc9900, 0xffcc66ff, 0xffcc66cc,
            0xffcc6699, 0xffcc6666, 0xffcc6633, 0xffcc6600, 0xffcc33ff, 0xffcc33cc, 0xffcc3399, 0xffcc3366,
            0xffcc3333, 0xffcc3300, 0xffcc00ff, 0xffcc00cc, 0xffcc0099, 0xffcc0066, 0xffcc0033, 0xffcc0000,
            0xff99ffff, 0xff99ffcc, 0xff99ff99, 0xff99ff66, 0xff99ff33, 0xff99ff00, 0xff99ccff, 0xff99cccc,
            0xff99cc99, 0xff99cc66, 0xff99cc33, 0xff99cc00, 0xff9999ff, 0xff9999cc, 0xff999999, 0xff999966,
            0xff999933, 0xff999900, 0xff9966ff, 0xff9966cc, 0xff996699, 0xff996666, 0xff996633, 0xff996600,
            0xff9933ff, 0xff9933cc, 0xff993399, 0xff993366, 0xff993333, 0xff993300, 0xff9900ff, 0xff9900cc,
            0xff990099, 0xff990066, 0xff990033, 0xff990000, 0xff66ffff, 0xff66ffcc, 0xff66ff99, 0xff66ff66,
            0xff66ff33, 0xff66ff00, 0xff66ccff, 0xff66cccc, 0xff66cc99, 0xff66cc66, 0xff66cc33, 0xff66cc00,
            0xff6699ff, 0xff6699cc, 0xff669999, 0xff669966, 0xff669933, 0xff669900, 0xff6666ff, 0xff6666cc,
            0xff666699, 0xff666666, 0xff666633, 0xff666600, 0xff6633ff, 0xff6633cc, 0xff663399, 0xff663366,
            0xff663333, 0xff663300, 0xff6600ff, 0xff6600cc, 0xff660099, 0xff660066, 0xff660033, 0xff660000,
            0xff33ffff, 0xff33ffcc, 0xff33ff99, 0xff33ff66, 0xff33ff33, 0xff33ff00, 0xff33ccff, 0xff33cccc,
            0xff33cc99, 0xff33cc66, 0xff33cc33, 0xff33cc00, 0xff3399ff, 0xff3399cc, 0xff339999, 0xff339966,
            0xff339933, 0xff339900, 0xff3366ff, 0xff3366cc, 0xff336699, 0xff336666, 0xff336633, 0xff336600,
            0xff3333ff, 0xff3333cc, 0xff333399, 0xff333366, 0xff333333, 0xff333300, 0xff3300ff, 0xff3300cc,
            0xff330099, 0xff330066, 0xff330033, 0xff330000, 0xff00ffff, 0xff00ffcc, 0xff00ff99, 0xff00ff66,
            0xff00ff33, 0xff00ff00, 0xff00ccff, 0xff00cccc, 0xff00cc99, 0xff00cc66, 0xff00cc33, 0xff00cc00,
            0xff0099ff, 0xff0099cc, 0xff009999, 0xff009966, 0xff009933, 0xff009900, 0xff0066ff, 0xff0066cc,
            0xff006699, 0xff006666, 0xff006633, 0xff006600, 0xff0033ff, 0xff0033cc, 0xff003399, 0xff003366,
            0xff003333, 0xff003300, 0xff0000ff, 0xff0000cc, 0xff000099, 0xff000066, 0xff000033, 0xffee0000,
            0xffdd0000, 0xffbb0000, 0xffaa0000, 0xff880000, 0xff770000, 0xff550000, 0xff440000, 0xff220000,
            0xff110000, 0xff00ee00, 0xff00dd00, 0xff00bb00, 0xff00aa00, 0xff008800, 0xff007700, 0xff005500,
            0xff004400, 0xff002200, 0xff001100, 0xff0000ee, 0xff0000dd, 0xff0000bb, 0xff0000aa, 0xff000088,
            0xff000077, 0xff000055, 0xff000044, 0xff000022, 0xff000011, 0xffeeeeee, 0xffdddddd, 0xffbbbbbb,
            0xffaaaaaa, 0xff888888, 0xff777777, 0xff555555, 0xff444444, 0xff222222, 0xff111111, 0xff000000
    };

    static final IndexColorModel INSTANCE = new ICNS8BitColorModel();

    private ICNS8BitColorModel() {
        super(8, 256, CMAP, 0, false, -1, DataBuffer.TYPE_BYTE);
    }
}
