package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

final class DDSHeader {

    // https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dx-graphics-dds-pguide
    private int flags;

    private int mipMapCount;
    private Dimension[] dimensions;

    private int pixelFormatFlags;
    private int fourCC;
    private int bitCount;
    private int redMask;
    private int greenMask;
    private int blueMask;
    private int alphaMask;

    public static DDSHeader read(final ImageInputStream imageInput) throws IOException {
        DDSHeader header = new DDSHeader();

        // Read MAGIC bytes [0,3]
        byte[] magic = new byte[DDS.MAGIC.length];
        imageInput.readFully(magic);
        if (!Arrays.equals(DDS.MAGIC, magic)) {
            throw new IIOException(String.format("Not a DDS file. Expected DDS magic %08x, read %08x", new BigInteger(1, DDS.MAGIC), new BigInteger(1, magic)));
        }

        // DDS_HEADER structure
        // https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header
        int dwSize = imageInput.readInt(); // [4,7]
        if (dwSize != DDS.HEADER_SIZE) {
            throw new IIOException(String.format("Invalid DDS header size (expected %d): %d", DDS.HEADER_SIZE, dwSize));
        }

        // Verify flags
        header.flags = imageInput.readInt(); // [8,11]
        if (header.getFlag(DDS.FLAG_CAPS
                & DDS.FLAG_HEIGHT
                & DDS.FLAG_WIDTH
                & DDS.FLAG_PIXELFORMAT)) {
            throw new IIOException("Required DDS Flag missing in header: " + Integer.toHexString(header.flags));
        }

        // Read Height & Width
        int dwHeight = imageInput.readInt(); // [12,15]
        int dwWidth = imageInput.readInt();  // [16,19]

        int dwPitchOrLinearSize = imageInput.readInt(); // [20,23]
        int dwDepth = imageInput.readInt(); // [24,27]
        header.mipMapCount = imageInput.readInt(); // [28,31]

        // build dimensions list
        header.addDimensions(dwWidth, dwHeight);

        byte[] dwReserved1 = new byte[11 * 4];  // [32,75]
        imageInput.readFully(dwReserved1);

        // DDS_PIXELFORMAT structure
        int px_dwSize = imageInput.readInt(); // [76,79]

        header.pixelFormatFlags = imageInput.readInt(); // [80,83]
        header.fourCC = imageInput.readInt(); // [84,87]
        header.bitCount = imageInput.readInt(); // [88,91]
        header.redMask = imageInput.readInt(); // [92,95]
        header.greenMask = imageInput.readInt(); // [96,99]
        header.blueMask = imageInput.readInt(); // [100,103]
        header.alphaMask = imageInput.readInt(); // [104,107]

        int dwCaps = imageInput.readInt(); // [108,111]
        int dwCaps2 = imageInput.readInt(); // [112,115]
        int dwCaps3 = imageInput.readInt(); // [116,119]
        int dwCaps4 = imageInput.readInt(); // [120,123]

        int dwReserved2 = imageInput.readInt(); // [124,127]

        return header;
    }

    private void addDimensions(int width, int height) {
        dimensions = new Dimension[getMipMapCount()];

        int w = width;
        int h = height;
        for (int i = 0; i < getMipMapCount(); i++) {
            dimensions[i] = new Dimension(w, h);
            w /= 2;
            h /= 2;
        }
    }

    private boolean getFlag(int mask) {
        return (flags & mask) != 0;
    }

    public int getWidth(int imageIndex) {
        int lim = dimensions[imageIndex].width;
        return (lim <= 0) ? 1 : lim;
    }

    public int getHeight(int imageIndex) {
        int lim =  dimensions[imageIndex].height;
        return (lim <= 0) ? 1 : lim;
    }

    public int getMipMapCount() {
        // 0 = (unused) or 1 = (1 level), but still only one 'base' image
        return (mipMapCount == 0) ? 1 : mipMapCount;
    }

    public int getAlphaMask() {
        return alphaMask;
    }

    public int getBitCount() {
        return bitCount;
    }

    public int getBlueMask() {
        return blueMask;
    }

    public int getFlags() {
        return flags;
    }

    public int getFourCC() {
        return fourCC;
    }

    public int getGreenMask() {
        return greenMask;
    }

    public int getPixelFormatFlags() {
        return pixelFormatFlags;
    }

    public int getRedMask() {
        return redMask;
    }
}
