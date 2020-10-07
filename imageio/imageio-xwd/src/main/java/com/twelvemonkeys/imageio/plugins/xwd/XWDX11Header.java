package com.twelvemonkeys.imageio.plugins.xwd;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class XWDX11Header {

    final int width;
    final int height;
    final ByteOrder byteOrder;
    final ByteOrder bitFillOrder;
    final int bitsPerPixel;
    final int bytesPerLine;

    final int visualClass;
    final int[] masks;
    final int bitsPerRGB;

    final IndexColorModel colorMap;
    final String windowName;

    final long pixelOffset;

    private XWDX11Header(int width, int height, ByteOrder byteOrder, ByteOrder bitFillOrder,
                         int bitsPerPixel, int bytesPerLine, int visualClass,
                         int readMask, int greenMask, int blueMask, int bitsPerRGB,
                         final IndexColorModel colorMap, final String windowName, long pixelOffset) {
        this.width = width;
        this.height = height;
        this.byteOrder = byteOrder;
        this.bitFillOrder = bitFillOrder;
        this.bitsPerPixel = bitsPerPixel;
        this.bytesPerLine = bytesPerLine;
        this.visualClass = visualClass;
        this.masks = new int[] {readMask, greenMask, blueMask, ~(readMask | greenMask | blueMask)};
        this.bitsPerRGB = bitsPerRGB;
        this.colorMap = colorMap;
        this.windowName = windowName;
        this.pixelOffset = pixelOffset;
    }

    static boolean isX11(final DataInput input) throws IOException {
        return input.readInt() >= X11.X11_HEADER_SIZE
                && input.readInt() == X11.X11_HEADER_VERSION;
    }

    static XWDX11Header read(final ImageInputStream input) throws IOException  {
        input.mark();
        if (!isX11(input)) {
            throw new IIOException("Not a valid X11 Window Dump");
        }
        input.reset();

        long pos = input.getStreamPosition();
        int length = input.readInt();
        input.readInt();

        int format = input.readInt(); // Pixel Format: 0 = 1 bit XYBitmap, 1 = single plane XYPixmap (gray?), 2 = two or more planes ZPixmap
        int depth = input.readInt();  // Pixel Bit Depth: 1-32 (never seen above 24...)

        int width = input.readInt();
        int height = input.readInt();
        int xOffset = input.readInt(); // Rarely used...

        int byteOrder = input.readInt();
        int unit = input.readInt();
        int bitOrder = input.readInt(); // Same as TIFF FillOrder...
        int pad = input.readInt();      // Can be inferred from bytePerLine?
        int bitsPerPixel = input.readInt();
        int bytePerLine = input.readInt(); // Scan line
        int visualClass = input.readInt();

        // TODO: Probably need these masks... Use them for creating color model?
        int redMask = input.readInt();
        int greenMask = input.readInt();
        int blueMask = input.readInt();

        // Size of each color mask in bits", basically bitPerSample
        // NOTE: This field can't be trusted... :-/
        int bitsPerRGB = input.readInt();
        if (bitsPerPixel == 24 && bitsPerRGB == 24) {
            bitsPerRGB = 8;
        }

        // Hmmm.. Unclear which of these that matters...
        // - Can the map be larger than colors used?
        // - Can numColors be > 0 for non-colormapped types?
        int numColors = input.readInt();
        int colorMapEntries = input.readInt();

        // Not useful? Metadata?
        int windowWidth = input.readInt();
        int windowHeight = input.readInt();
        int windowX = input.readInt();
        int windowY = input.readInt();
        int windowBorderWidth = input.readInt();

        byte[] windowNameData = new byte[length - X11.X11_HEADER_SIZE];
        input.readFully(windowNameData);
        String windowName = windowNameData.length <= 1 ? null : new String(windowNameData, 0, windowNameData.length - 1, StandardCharsets.UTF_8);

        if (XWDProviderInfo.DEBUG) {
            System.out.println("format = " + format);
            System.out.println("depth = " + depth);
            System.out.println("byteOrder = " + byteOrder);
            System.out.println("unit = " + unit);
            System.out.println("bitOrder = " + bitOrder);
            System.out.println("pad = " + pad);
            System.out.println("bitsPerPixel = " + bitsPerPixel);
            System.out.println("bytePerLine = " + bytePerLine);
            System.out.println("visualClass = " + visualClass);

            System.out.printf("redMask = 0x%08x%n", redMask);
            System.out.printf("greenMask = 0x%08x%n", greenMask);
            System.out.printf("blueMask = 0x%08x%n", blueMask);

            System.out.println("bitsPerRGB = " + bitsPerRGB);

            System.out.println("numColors = " + numColors);
            System.out.println("colorMapEntries = " + colorMapEntries);
            System.out.println("windowName = " + windowName);
        }

        byte[] colorMap = new byte[12 * colorMapEntries];
        input.readFully(colorMap);

        return new XWDX11Header(width, height,
                byteOrder == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN,
                bitOrder == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN,
                bitsPerPixel, bytePerLine, visualClass, redMask, greenMask, blueMask, bitsPerRGB,
                createColorMap(bitsPerRGB, colorMap),
                windowName, pos + length + colorMap.length);
    }

    private static IndexColorModel createColorMap(int bitDepth, byte[] colorMap) {
        if (colorMap.length == 0) {
            return null;
        }

        int size = colorMap.length / 12;
        int[] rgb = new int[size];

        for (int i = 0; i < size; i++) {
//            int index = (colorMap[i * 12] & 0xff) << 24 | (colorMap[i * 12 + 1] & 0xff) << 16 | (colorMap[i * 12 + 2] & 0xff) << 8 | (colorMap[i * 12 + 3] & 0xff);
//            System.out.println("index = " + index);

            // We'll just use the high 8 bits, as Java don't really have good support for 16 bit index color model
            rgb[i] = (colorMap[i * 12 + 4] & 0xff) << 16 | (colorMap[i * 12 + 6] & 0xff) << 8 | (colorMap[i * 12 + 8] & 0xff);
        }

        return new IndexColorModel(bitDepth, size, rgb, 0, false, -1, DataBuffer.TYPE_BYTE);
    }


    int numComponents() {
        return bitsPerPixel / bitsPerRGB;
    }
}
