package com.twelvemonkeys.imageio.plugins.pcx;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Arrays;

final class PCXHeader {
    private int version;
    private int compression;
    private int bitsPerPixel;
    private int width;
    private int height;
    private int hdpi;
    private int vdpi;
    private byte[] palette;
    private int channels;
    private int bytesPerLine;
    private int paletteInfo;
    private int hScreenSize;
    private int vScreenSize;

    public int getVersion() {
        return version;
    }

    public int getCompression() {
        return compression;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getHdpi() {
        return hdpi;
    }

    public int getVdpi() {
        return vdpi;
    }

    public int getChannels() {
        return channels;
    }

    public int getBytesPerLine() {
        return bytesPerLine;
    }

    public int getPaletteInfo() {
        return paletteInfo;
    }

    public IndexColorModel getEGAPalette() {
        // TODO: Figure out when/how to enable CGA palette... The test below isn't good enough.
//        if (channels == 1 && (bitsPerPixel == 1  || bitsPerPixel == 2)) {
//            return CGAColorModel.create(palette, bitsPerPixel);
//        }

        int bits = channels * bitsPerPixel;
        return new IndexColorModel(bits, Math.min(16, 1 << bits), palette, 0, false);
    }

    @Override public String toString() {
        return "PCXHeader{" +
                "version=" + version +
                ", compression=" + compression +
                ", bitsPerPixel=" + bitsPerPixel +
                ", width=" + width +
                ", height=" + height +
                ", hdpi=" + hdpi +
                ", vdpi=" + vdpi +
                ", channels=" + channels +
                ", bytesPerLine=" + bytesPerLine +
                ", paletteInfo=" + paletteInfo +
                ", hScreenSize=" + hScreenSize +
                ", vScreenSize=" + vScreenSize +
                ", palette=" + Arrays.toString(palette) +
                '}';
    }

    public static PCXHeader read(final ImageInputStream imageInput) throws IOException {
//        typedef struct _PcxHeader
//        {
//            BYTE	Identifier;        /* PCX Id Number (Always 0x0A) */
//            BYTE	Version;           /* Version Number */
//            BYTE	Encoding;          /* Encoding Format */
//            BYTE	BitsPerPixel;      /* Bits per Pixel */
//            WORD	XStart;            /* Left of image */
//            WORD	YStart;            /* Top of Image */
//            WORD	XEnd;              /* Right of Image
//            WORD	YEnd;              /* Bottom of image */
//            WORD	HorzRes;           /* Horizontal Resolution */
//            WORD	VertRes;           /* Vertical Resolution */
//            BYTE	Palette[48];       /* 16-Color EGA Palette */
//            BYTE	Reserved1;         /* Reserved (Always 0) */
//            BYTE	NumBitPlanes;      /* Number of Bit Planes */
//            WORD	BytesPerLine;      /* Bytes per Scan-line */
//            WORD	PaletteType;       /* Palette Type */
//            WORD	HorzScreenSize;    /* Horizontal Screen Size */
//            WORD	VertScreenSize;    /* Vertical Screen Size */
//            BYTE	Reserved2[54];     /* Reserved (Always 0) */
//        } PCXHEAD;

        byte magic = imageInput.readByte();
        if (magic != PCX.MAGIC) {
            throw new IIOException(String.format("Not a PCX image. Expected PCX magic %02x, read %02x", PCX.MAGIC, magic));
        }

        PCXHeader header = new PCXHeader();

        header.version = imageInput.readUnsignedByte();
        header.compression = imageInput.readUnsignedByte();
        header.bitsPerPixel = imageInput.readUnsignedByte();

        int xStart = imageInput.readUnsignedShort();
        int yStart = imageInput.readUnsignedShort();
        header.width = imageInput.readUnsignedShort() - xStart + 1;
        header.height = imageInput.readUnsignedShort() - yStart + 1;

        header.hdpi = imageInput.readUnsignedShort();
        header.vdpi = imageInput.readUnsignedShort();

        byte[] palette = new byte[48];
        imageInput.readFully(palette); // 16 RGB triplets
        header.palette = palette;

        imageInput.readUnsignedByte(); // Reserved, should be 0

        header.channels = imageInput.readUnsignedByte();
        header.bytesPerLine = imageInput.readUnsignedShort(); // Must be even!

        header.paletteInfo = imageInput.readUnsignedShort(); // 1 == Color/BW, 2 == Gray

        header.hScreenSize = imageInput.readUnsignedShort();
        header.vScreenSize = imageInput.readUnsignedShort();

        imageInput.skipBytes(PCX.HEADER_SIZE - imageInput.getStreamPosition());

        return header;
    }
}
