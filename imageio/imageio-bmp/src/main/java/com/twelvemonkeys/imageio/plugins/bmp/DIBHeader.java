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
import java.io.DataInput;
import java.io.DataOutput;
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
    // Roughly 72 DPI
    private final int DEFAULT_PIXELS_PER_METER = 2835;

    protected int size;
    protected int width;
    // NOTE: If a bitmask is present, this value includes the height of the mask
    // (so often header.height = entry.height * 2)
    protected int height;
    protected boolean topDown = false;

    protected int planes;
    protected int bitCount;

    /**
     * 0 = BI_RGB: No compression
     * 1 = BI_RLE8: 8 bit RLE Compression (8 bit only)
     * 2 = BI_RLE4: 4 bit RLE Compression (4 bit only)
     * 3 = BI_BITFIELDS: No compression (16 & 32 bit only)
     */
    protected int compression;

    // May be 0 if not known
    protected int imageSize;

    protected int xPixelsPerMeter;
    protected int yPixelsPerMeter;

    protected int colorsUsed;

    // 0 means all colors are important
    protected int colorsImportant;

    // V4+ members below
    protected int[] masks;
    protected int colorSpaceType;
    protected double[] cieXYZEndpoints;
    protected int[] gamma;

    // V5+ members below
    protected int intent;
    protected long profileData;
    protected long profileSize;

    protected DIBHeader() {
    }

    public static DIBHeader read(final DataInput pStream) throws IOException {
        int size = pStream.readInt();

        DIBHeader header = createHeader(size);
        header.read(size, pStream);

        return header;
    }

    private static DIBHeader createHeader(final int pSize) throws IOException {
        switch (pSize) {
            case DIB.BITMAP_CORE_HEADER_SIZE:
                return new BitmapCoreHeader();
            case DIB.OS2_V2_HEADER_16_SIZE:
            case DIB.OS2_V2_HEADER_SIZE:
                return new BitmapCoreHeaderV2();
            case DIB.BITMAP_INFO_HEADER_SIZE:
            case DIB.BITMAP_V2_INFO_HEADER_SIZE:
            case DIB.BITMAP_V3_INFO_HEADER_SIZE:
                // ICO and CUR always uses the Microsoft Windows 3.0 DIB header, which is 40 bytes.
                // This is also the most common format for persistent BMPs.
                return new BitmapInfoHeader();
            case DIB.BITMAP_V4_INFO_HEADER_SIZE:
                return new BitmapV4InfoHeader();
            case DIB.BITMAP_V5_INFO_HEADER_SIZE:
                return new BitmapV5InfoHeader();
            default:
                throw new IIOException(String.format("Unknown Bitmap Information Header (size: %s)", pSize));
        }
    }

    protected abstract void read(int pSize, DataInput pStream) throws IOException;

    public final int getSize() {
        return size;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final int getPlanes() {
        return planes;
    }

    public final int getBitCount() {
        return bitCount;
    }

    public int getCompression() {
        return compression;
    }

    public int getImageSize() {
        return imageSize != 0 ? imageSize : ((bitCount * width + 31) / 32) * 4 * height;
    }

    public int getXPixelsPerMeter() {
        return xPixelsPerMeter != 0 ? xPixelsPerMeter : DEFAULT_PIXELS_PER_METER;
    }

    public int getYPixelsPerMeter() {
        return yPixelsPerMeter != 0 ? yPixelsPerMeter : DEFAULT_PIXELS_PER_METER;
    }

    public int getColorsUsed() {
        return colorsUsed != 0 ? colorsUsed : 1 << Math.min(24, bitCount);
    }

    public int getColorsImportant() {
        return colorsImportant != 0 ? colorsImportant : getColorsUsed();
    }

    public boolean hasMasks() {
        return masks != null || compression == DIB.COMPRESSION_BITFIELDS || compression == DIB.COMPRESSION_ALPHA_BITFIELDS;
    }

    public String toString() {
        return String.format(
                "%s: size: %d bytes, " +
                        "width: %d, height: %d, planes: %d, bit count: %d, compression: %d, " +
                        "image size: %d%s, " +
                        "X pixels per m: %d, Y pixels per m: %d, " +
                        "colors used: %d%s, colors important: %d%s",
                getClass().getSimpleName(),
                getSize(), getWidth(), getHeight(), getPlanes(), getBitCount(), getCompression(),
                getImageSize(), (imageSize == 0 ? " (calculated)" : ""),
                getXPixelsPerMeter(),
                getYPixelsPerMeter(),
                getColorsUsed(), (colorsUsed == 0 ? " (unknown)" : ""),
                getColorsImportant(), (colorsImportant == 0 ? " (all)" : "")
        );
    }

    private static int[] readMasks(final DataInput pStream, final boolean hasAlphaMask) throws IOException {
        int maskCount = hasAlphaMask ? 4 : 3;
        int[] masks = new int[4];

        for (int i = 0; i < maskCount; i++) {
            masks[i] = pStream.readInt();
        }

        return masks;
    }

    protected abstract String getBMPVersion();

    // TODO: Get rid of code duplication below...

    static final class BitmapCoreHeader extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (pSize != DIB.BITMAP_CORE_HEADER_SIZE) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.BITMAP_CORE_HEADER_SIZE));
            }

            size = pSize;

            // NOTE: Unlike all other headers, width and height are unsigned SHORT values (16 bit)!
            width = pStream.readUnsignedShort();
            height = pStream.readUnsignedShort();

            if (height < 0) {
                height = -height;
                topDown = true;
            }

            planes = pStream.readUnsignedShort();
            bitCount = pStream.readUnsignedShort();
        }

        public String getBMPVersion() {
            return "BMP v. 2.x";
        }
    }

    /**
     * OS/2 BitmapCoreHeader Version 2.
     * <p/>
     * NOTE: According to the docs this header is <em>variable size</em>.
     * However, it seems that the size is either 16, 40 or 64, which is covered
     * (40 is the size of the normal {@link BitmapInfoHeader}, and has the same layout).
     *
     * @see <a href="http://www.fileformat.info/format/os2bmp/egff.htm">OS/2 Bitmap File Format Summary</a>
     */
    static final class BitmapCoreHeaderV2 extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (pSize != DIB.OS2_V2_HEADER_SIZE && pSize != DIB.OS2_V2_HEADER_16_SIZE) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.OS2_V2_HEADER_SIZE));
            }

            size = pSize;

            width = pStream.readInt();
            height = pStream.readInt();

            if (height < 0) {
                height = -height;
                topDown = true;
            }

            planes = pStream.readUnsignedShort();
            bitCount = pStream.readUnsignedShort();

            if (pSize != DIB.OS2_V2_HEADER_16_SIZE) {
                compression = pStream.readInt();

                imageSize = pStream.readInt();

                xPixelsPerMeter = pStream.readInt();
                yPixelsPerMeter = pStream.readInt();

                colorsUsed = pStream.readInt();
                colorsImportant = pStream.readInt();
            }

            // TODO: Use? These fields are not reflected in metadata as per now...
            int units = pStream.readShort();
            int reserved = pStream.readShort();
            int recording = pStream.readShort(); // Recording algorithm
            int rendering = pStream.readShort(); // Halftoning algorithm
            int size1 = pStream.readInt(); // Reserved for halftoning use
            int size2 = pStream.readInt(); // Reserved for halftoning use
            int colorEncoding = pStream.readInt(); // Color model used in bitmap
            int identifier = pStream.readInt(); // Reserved for application use
        }

        public String getBMPVersion() {
            return "BMP v. 2.2";
        }
    }


    /**
     * Represents the DIB (Device Independent Bitmap) Windows 3.0 Bitmap Information header structure.
     * This is the common format for persistent DIB structures, even if Windows
     * may use the later versions at run-time.
     * <p/>
     * Note: Some variations that includes the mask fields into the header size exists,
     * but is no longer part of the documented spec.
     *
     * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
     * @version $Id: DIBHeader.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
     * @see <a href="http://en.wikipedia.org/wiki/BMP_file_format">BMP file format (Wikipedia)</a>
     * @see <a href="https://forums.adobe.com/message/3272950#3272950">BITMAPV3INFOHEADER</a>.
     */
    static final class BitmapInfoHeader extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (!(pSize == DIB.BITMAP_INFO_HEADER_SIZE || pSize == DIB.BITMAP_V2_INFO_HEADER_SIZE || pSize == DIB.BITMAP_V3_INFO_HEADER_SIZE)) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.BITMAP_INFO_HEADER_SIZE));
            }

            size = pSize;

            width = pStream.readInt();
            height = pStream.readInt();

            if (height < 0) {
                height = -height;
                topDown = true;
            }

            planes = pStream.readUnsignedShort();
            bitCount = pStream.readUnsignedShort();
            compression = pStream.readInt();

            imageSize = pStream.readInt();

            xPixelsPerMeter = pStream.readInt();
            yPixelsPerMeter = pStream.readInt();

            colorsUsed = pStream.readInt();
            colorsImportant = pStream.readInt();

            // Read masks if we have V2 or V3
            // or if we have compression BITFIELDS or ALPHA_BITFIELDS
            if (size == DIB.BITMAP_V2_INFO_HEADER_SIZE || compression == DIB.COMPRESSION_BITFIELDS) {
                masks = readMasks(pStream, false);
            }
            else if (size == DIB.BITMAP_V3_INFO_HEADER_SIZE || compression == DIB.COMPRESSION_ALPHA_BITFIELDS) {
                masks = readMasks(pStream, true);
            }
        }

        void write(final DataOutput stream) throws IOException {
            stream.writeInt(DIB.BITMAP_INFO_HEADER_SIZE);

            stream.writeInt(width);
            stream.writeInt(topDown ? -height : height);

            stream.writeShort(planes);
            stream.writeShort(bitCount);
            stream.writeInt(compression);

            stream.writeInt(imageSize);

            stream.writeInt(xPixelsPerMeter);
            stream.writeInt(yPixelsPerMeter);

            stream.writeInt(colorsUsed);
            stream.writeInt(colorsImportant);

            // TODO: Write masks, if bitfields
        }

        public String getBMPVersion() {
            // This is to be compatible with the native metadata of the original com.sun....BMPMetadata
            return compression == DIB.COMPRESSION_BITFIELDS ? "BMP v. 3.x NT" : "BMP v. 3.x";
        }
    }

    /**
     * Represents the BITMAPV4INFOHEADER structure.
     */
    static final class BitmapV4InfoHeader extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (pSize != DIB.BITMAP_V4_INFO_HEADER_SIZE) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.BITMAP_V4_INFO_HEADER_SIZE));
            }

            size = pSize;

            width = pStream.readInt();
            height = pStream.readInt();

            if (height < 0) {
                height = -height;
                topDown = true;
            }

            planes = pStream.readUnsignedShort();
            bitCount = pStream.readUnsignedShort();
            compression = pStream.readInt();

            imageSize = pStream.readInt();

            xPixelsPerMeter = pStream.readInt();
            yPixelsPerMeter = pStream.readInt();

            colorsUsed = pStream.readInt();
            colorsImportant = pStream.readInt();

            masks = readMasks(pStream, true);

            colorSpaceType = pStream.readInt(); // Should be 0 for V4
            cieXYZEndpoints = new double[9];

            for (int i = 0; i < cieXYZEndpoints.length; i++) {
                cieXYZEndpoints[i] = pStream.readInt(); // TODO: Hmmm...?
            }

            gamma = new int[3];

            for (int i = 0; i < gamma.length; i++) {
                gamma[i] = pStream.readInt();
            }
        }

        public String getBMPVersion() {
            return "BMP v. 4.x";
        }
    }

    /**
     * Represents the BITMAPV5INFOHEADER structure.
     */
    static final class BitmapV5InfoHeader extends DIBHeader {
        protected void read(final int pSize, final DataInput pStream) throws IOException {
            if (pSize != DIB.BITMAP_V5_INFO_HEADER_SIZE) {
                throw new IIOException(String.format("Size: %s !=: %s", pSize, DIB.BITMAP_V5_INFO_HEADER_SIZE));
            }

            size = pSize;

            width = pStream.readInt();
            height = pStream.readInt();

            if (height < 0) {
                height = -height;
                topDown = true;
            }

            planes = pStream.readUnsignedShort();
            bitCount = pStream.readUnsignedShort();
            compression = pStream.readInt();

            imageSize = pStream.readInt();

            xPixelsPerMeter = pStream.readInt();
            yPixelsPerMeter = pStream.readInt();

            colorsUsed = pStream.readInt();
            colorsImportant = pStream.readInt();

            masks = readMasks(pStream, true);

            colorSpaceType = pStream.readInt();

            cieXYZEndpoints = new double[9];

            for (int i = 0; i < cieXYZEndpoints.length; i++) {
                cieXYZEndpoints[i] = pStream.readInt(); // TODO: Hmmm...?
            }

            gamma = new int[3];

            for (int i = 0; i < gamma.length; i++) {
                gamma[i] = pStream.readInt();
            }

            intent = pStream.readInt(); // TODO: Verify if this is same as ICC intent
            profileData = pStream.readInt() & 0xffffffffL;
            profileSize = pStream.readInt() & 0xffffffffL;
            pStream.readInt(); // Reserved
        }

        public String getBMPVersion() {
            return "BMP v. 5.x";
        }
    }
}