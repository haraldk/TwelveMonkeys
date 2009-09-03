package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * PSDThumbnail
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDThumbnail.java,v 1.0 Jul 29, 2009 4:41:06 PM haraldk Exp$
 */
class PSDThumbnail extends PSDImageResource {
    private BufferedImage mThumbnail;

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
        // TODO: Support for RAW RGB (format == 0)
        int format = pInput.readInt();
        switch (format) {
            case 0:
                throw new IIOException("RAW RGB format thumbnail not supported yet");
            case 1:
                break;
            default:
                throw new IIOException(String.format("Unsupported thumbnail format (%s) in PSD document", format));
        }

        // This data isn't really useful, unless we're dealing with raw bytes
        int width = pInput.readInt();
        int height = pInput.readInt();
        int widthBytes = pInput.readInt();
        int totalSize = pInput.readInt();

        // Consistency check
        int sizeCompressed = pInput.readInt();
        if (sizeCompressed != (mSize - 28)) {
            throw new IIOException("Corrupt thumbnail in PSD document");
        }

        // According to the spec, only 24 bits and 1 plane is supported
        int bits = pInput.readUnsignedShort();
        int planes = pInput.readUnsignedShort();
        if (bits != 24 && planes != 1) {
            // TODO: Warning/Exception
        }

        // TODO: Support BGR if id == RES_THUMBNAIL_PS4? Or is that already supported in the JPEG?
        mThumbnail = ImageIO.read(IIOUtil.createStreamAdapter(pInput, sizeCompressed));
    }

    public BufferedImage getThumbnail() {
        return mThumbnail;
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", ").append(mThumbnail);

        builder.append("]");

        return builder.toString();
    }
}
