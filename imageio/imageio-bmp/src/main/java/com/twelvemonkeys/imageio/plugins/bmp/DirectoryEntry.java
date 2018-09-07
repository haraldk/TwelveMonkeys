/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import javax.imageio.IIOException;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DirectoryEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DirectoryEntry.java,v 1.0 Apr 4, 2009 4:29:53 PM haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/ICO_(icon_image_file_format)#Directory">Wikipedia</a>
 */
abstract class DirectoryEntry {
    int width;
    int height;
    int colorCount;
    int planes;
    int bitCount;
    int size;
    int offset;

    DirectoryEntry() {
    }

    public static DirectoryEntry read(final int pType, final DataInput pStream) throws IOException {
        DirectoryEntry entry = createEntry(pType);
        entry.read(pStream);
        return entry;
    }

    private static DirectoryEntry createEntry(int pType) throws IIOException {
        switch (pType) {
            case DIB.TYPE_ICO:
                return new ICOEntry();
            case DIB.TYPE_CUR:
                return new CUREntry();
            default:
                throw new IIOException(
                        String.format(
                                "Unknown DIB type: %s, expected: %s (ICO) or %s (CUR)",
                                pType, DIB.TYPE_ICO, DIB.TYPE_CUR
                        )
                );
        }
    }

    protected void read(final DataInput pStream) throws IOException {
        // Width/height = 0, means 256
        int w = pStream.readUnsignedByte();
        width = w == 0 ? 256 : w;
        int h = pStream.readUnsignedByte();
        height = h == 0 ? 256 : h;
        
        // Color count = 0, means 256 or more colors
        colorCount = pStream.readUnsignedByte();

        // Ignore. Should be 0, but .NET (System.Drawing.Icon.Save) sets this value to 255, according to Wikipedia
        pStream.readUnsignedByte();

        planes = pStream.readUnsignedShort();     // Should be 0 or 1 for ICO, x hotspot for CUR
        bitCount = pStream.readUnsignedShort();   // bit count for ICO, y hotspot for CUR

        // Size of bitmap in bytes
        size = pStream.readInt();
        offset = pStream.readInt();
    }

    void write(final DataOutput output) throws IOException {
        output.writeByte(width  % 256);
        output.writeByte(height % 256);
        output.writeByte(colorCount);
        output.writeByte(0); // Reserved
        output.writeShort(1); // Color planes 0 or 1
        output.writeShort(bitCount);
        output.writeInt(size); // Size, depends on compression...
        output.writeInt(offset);

    }

    public String toString() {
        return String.format(
                "%s: width: %d, height: %d, colors: %d, planes: %d, bit count: %d, size: %d, offset: %d",
                getClass().getSimpleName(),
                width, height, colorCount, planes, bitCount, size, offset
        );
    }

    public int getBitCount() {
        return bitCount;
    }

    public int getColorCount() {
        return colorCount;
    }

    public int getHeight() {
        return height;
    }

    public int getOffset() {
        return offset;
    }

    public int getPlanes() {
        return planes;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    /**
     * Cursor directory entry.
     */
    static class CUREntry extends DirectoryEntry {
        private int xHotspot;
        private int yHotspot;

        @Override
        protected void read(final DataInput pStream) throws IOException {
            super.read(pStream);

            // NOTE: This is a hack...
            xHotspot = planes;
            yHotspot = bitCount;

            planes = 1;    // Always 1 for all BMP types
            bitCount = 0;
        }

        public Point getHotspot() {
            return new Point(xHotspot, yHotspot);
        }
    }

    /**
     * Icon directory entry.
     */
    static final class ICOEntry extends DirectoryEntry {
        private ICOEntry() {}

        ICOEntry(final int width, final int height, final ColorModel colorModel, int size, final int offset) {
            this.width = width;
            this.height = height;
            this.colorCount = colorModel instanceof IndexColorModel ? ((IndexColorModel) colorModel).getMapSize() : 0;
            this.planes = 1;
            this.bitCount = colorModel.getPixelSize();
            this.size = size;
            this.offset = offset;
        }
    }
}