package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * PSDThumbnail
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDThumbnail.java,v 1.0 Jul 29, 2009 4:41:06 PM haraldk Exp$
 */
class PSDThumbnail extends PSDImageResource {
    private int format;
    private int width;
    private int height;
    private int widthBytes;
    private byte[] data;

    public PSDThumbnail(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    /*
    Thumbnail header, size 28
    4 Format. 1 = kJpegRGB . Also supports kRawRGB (0).
    4 Width of thumbnail in pixels.
    4 Height of thumbnail in pixels.
    4 Widthbytes: Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
    4 Total size = widthbytes * height * planes
    4 Size after compression. Used for consistency check.
    2 Bits per pixel. = 24
    2 Number of planes. = 1
     */
    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        format = pInput.readInt();

        width = pInput.readInt();
        height = pInput.readInt();

        // This data isn't really useful, unless we're dealing with raw bytes
        widthBytes = pInput.readInt();
        int totalSize = pInput.readInt(); // Hmm.. Is this really useful at all?

        // Consistency check
        int sizeCompressed = pInput.readInt();
        if (sizeCompressed != (size - 28)) {
            throw new IIOException("Corrupt thumbnail in PSD document");
        }

        // According to the spec, only 24 bits and 1 plane is supported
        int bits = pInput.readUnsignedShort();
        int planes = pInput.readUnsignedShort();
        if (bits != 24 && planes != 1) {
            // TODO: Warning/Exception
        }

        data = new byte[sizeCompressed];
        pInput.readFully(data);
    }

    BufferedImage imageFromRawData(int width, int height, int scanLine, byte[] data) {
        DataBuffer buffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createInterleavedRaster(
                buffer, width, height,
                scanLine, 3,
                new int[]{0, 1, 2},
                null
        );
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final BufferedImage getThumbnail() throws IOException {
        switch (format) {
            case 0:
                // RAW RGB
                return imageFromRawData(width, height, widthBytes, data.clone()); // Clone data, as image is mutable
            case 1:
                // JPEG
                // TODO: Support BGR if id == RES_THUMBNAIL_PS4? Or is that already supported in the JPEG reader?
                return ImageIO.read(new ByteArrayInputStream(data));
            default:
                throw new IIOException(String.format("Unsupported thumbnail format (%s) in PSD document", format));
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", format: ");
        switch (format) {
            case 0:
                // RAW RGB
                builder.append("RAW RGB");
                break;
            case 1:
                // JPEG
                builder.append("JPEG");
                break;
            default:
                builder.append("Unknown");
                break;
        }

        builder.append(", size: ").append(data != null ? data.length : -1);

        builder.append("]");

        return builder.toString();
    }
}
