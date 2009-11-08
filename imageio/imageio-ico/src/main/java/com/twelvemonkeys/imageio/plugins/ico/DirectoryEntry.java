/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.ico;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.*;

/**
 * DirectoryEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DirectoryEntry.java,v 1.0 Apr 4, 2009 4:29:53 PM haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/ICO_(icon_image_file_format)#Directory">Wikipedia</a>
 */
abstract class DirectoryEntry {
    private int mWidth;
    private int mHeight;
    private int mColorCount;
    int mPlanes;
    int mBitCount;
    private int mSize;
    private int mOffset;

    private DirectoryEntry() {
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
        mWidth = w == 0 ? 256 : w;
        int h = pStream.readUnsignedByte();
        mHeight = h == 0 ? 256 : h;
        
        // Color count = 0, means 256 or more colors
        mColorCount = pStream.readUnsignedByte();

        // Ignore. Should be 0, but .NET (System.Drawing.Icon.Save) sets this value to 255, according to Wikipedia
        pStream.readUnsignedByte();

        mPlanes = pStream.readUnsignedShort();     // Should be 0 or 1 for ICO, x hotspot for CUR
        mBitCount = pStream.readUnsignedShort();   // bit count for ICO, y hotspot for CUR

        // Size of bitmap in bytes
        mSize = pStream.readInt();
        mOffset = pStream.readInt();
    }

    public String toString() {
        return String.format(
                "%s: width: %d, height: %d, colors: %d, planes: %d, bit count: %d, size: %d, offset: %d",
                getClass().getSimpleName(),
                mWidth, mHeight, mColorCount, mPlanes, mBitCount, mSize, mOffset
        );
    }

    public int getBitCount() {
        return mBitCount;
    }

    public int getColorCount() {
        return mColorCount;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getOffset() {
        return mOffset;
    }

    public int getPlanes() {
        return mPlanes;
    }

    public int getSize() {
        return mSize;
    }

    public int getWidth() {
        return mWidth;
    }

    /**
     * Cursor directory entry.
     */
    static class CUREntry extends DirectoryEntry {
        private int mXHotspot;
        private int mYHotspot;

        @Override
        protected void read(final DataInput pStream) throws IOException {
            super.read(pStream);

            // NOTE: This is a hack...
            mXHotspot = mPlanes;
            mYHotspot = mBitCount;

            mPlanes = 1;    // Always 1 for all BMP types
            mBitCount = 0;
        }

        public Point getHotspot() {
            return new Point(mXHotspot, mYHotspot);
        }
    }

    /**
     * Icon directory entry.
     */
    static final class ICOEntry extends DirectoryEntry {
    }
}