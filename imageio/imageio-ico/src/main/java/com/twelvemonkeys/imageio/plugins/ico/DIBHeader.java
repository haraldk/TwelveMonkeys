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

/**
 * Represents the DIB (Device Independent Bitmap) Information header structure.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DIBHeader.java,v 1.0 May 5, 2009 10:45:31 AM haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/BMP_file_format">BMP file format (Wikipedia)</a>
 */
abstract class DIBHeader {
    protected int mSize;

    protected int mWidth;

    // NOTE: If a bitmask is present, this value includes the height of the mask
    // (so often header.height = entry.height * 2)
    protected int mHeight;

    protected int mPlanes;
    protected int mBitCount;

    /**
     * 0 = BI_RGB: No compression
     * 1 = BI_RLE8: 8 bit RLE Compression (8 bit only)
     * 2 = BI_RLE4: 4 bit RLE Compression (4 bit only)
     * 3 = BI_BITFIELDS: No compression (16 & 32 bit only)
     */
    protected int mCompression;

    // May be 0 if not known
    protected int mImageSize;

    protected int mXPixelsPerMeter;
    protected int mYPixelsPerMeter;

    protected int mColorsUsed;

    // 0 means all colors are important
    protected int mColorsImportant;

    protected DIBHeader() {
    }

    public static DIBHeader read(final DataInput pStream) throws IOException {
        int size = pStream.readInt();

        // ICO always uses the Microsoft Windows V3 DIB header, which is 40 bytes
        DIBHeader header = createHeader(size);
        header.read(size, pStream);

        return header;
    }

    private static DIBHeader createHeader(final int pSize) throws IOException {
        switch (pSize) {
            case DIB.OS2_V1_HEADER_SIZE:
            case DIB.OS2_V2_HEADER_SIZE:
                throw new IIOException(String.format("OS/2 Bitmap Information Header (size: %s) not supported", pSize));
            case DIB.WINDOWS_V3_HEADER_SIZE:
                return new WindowsV3DIBHeader();
            case DIB.WINDOWS_V4_HEADER_SIZE:
            case DIB.WINDOWS_V5_HEADER_SIZE:
                throw new IIOException(String.format("Windows Bitmap Information Header (size: %s) not supported", pSize));
            default:
                throw new IIOException(String.format("Unknown Bitmap Information Header (size: %s)", pSize));
        }
    }

    protected abstract void read(int pSize, DataInput pStream) throws IOException;

    public final int getSize() {
        return mSize;
    }

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    public final int getPlanes() {
        return mPlanes;
    }

    public final int getBitCount() {
        return mBitCount;
    }

    public int getCompression() {
        return mCompression;
    }

    public int getImageSize() {
        return mImageSize;
    }

    public int getXPixelsPerMeter() {
        return mXPixelsPerMeter;
    }

    public int getYPixelsPerMeter() {
        return mYPixelsPerMeter;
    }

    public int getColorsUsed() {
        return mColorsUsed;
    }

    public int getColorsImportant() {
        return mColorsImportant;
    }

    public String toString() {
        return String.format(
                "%s: size: %d bytes, " +
                        "width: %d, height: %d, planes: %d, bit count: %d, compression: %d, " +
                        "image size: %d%s, " +
                        "X pixels per m: %d, Y pixels per m: %d, " +
                        "colors used: %d, colors important: %d%s",
                getClass().getSimpleName(),
                getSize(), getWidth(), getHeight(), getPlanes(), getBitCount(), getCompression(),
                getImageSize(), (getImageSize() == 0 ? " (unknown)" : ""),
                getXPixelsPerMeter(), getYPixelsPerMeter(),
                getColorsUsed(), getColorsImportant(), (getColorsImportant() == 0 ? " (all)" : "")
        );
    }

    /**
     * Represents the DIB (Device Independent Bitmap) Windows V3 Bitmap Information header structure.
     * This is the common format for persistent DIB structures, even if Windows
     * may use the later versions at run-time.
     * <p/>
     *
     * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
     * @version $Id: DIBHeader.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
     * @see <a href="http://en.wikipedia.org/wiki/BMP_file_format">BMP file format (Wikipedia)</a>
     */
    static final class WindowsV3DIBHeader extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (pSize != DIB.WINDOWS_V3_HEADER_SIZE) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.WINDOWS_V3_HEADER_SIZE));
            }

            mSize = pSize;

            mWidth = pStream.readInt();
            mHeight = pStream.readInt();

            mPlanes = pStream.readUnsignedShort();
            mBitCount = pStream.readUnsignedShort();
            mCompression = pStream.readInt();

            mImageSize = pStream.readInt();

            mXPixelsPerMeter = pStream.readInt();
            mYPixelsPerMeter = pStream.readInt();

            mColorsUsed = pStream.readInt();
            mColorsImportant = pStream.readInt();
        }
    }
}