package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;

import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

final class XWDImageReader extends ImageReaderBase {
    // TODO: This table is also in c.t.i.p.tiff.ReverseInputStream, consider moving to common util?
    // http://graphics.stanford.edu/~seander/bithacks.html
    static final byte[] BIT_REVERSE_TABLE = {
            0x00, (byte) 0x80, 0x40, (byte) 0xC0, 0x20, (byte) 0xA0, 0x60, (byte) 0xE0, 0x10, (byte) 0x90, 0x50, (byte) 0xD0, 0x30, (byte) 0xB0, 0x70, (byte) 0xF0,
            0x08, (byte) 0x88, 0x48, (byte) 0xC8, 0x28, (byte) 0xA8, 0x68, (byte) 0xE8, 0x18, (byte) 0x98, 0x58, (byte) 0xD8, 0x38, (byte) 0xB8, 0x78, (byte) 0xF8,
            0x04, (byte) 0x84, 0x44, (byte) 0xC4, 0x24, (byte) 0xA4, 0x64, (byte) 0xE4, 0x14, (byte) 0x94, 0x54, (byte) 0xD4, 0x34, (byte) 0xB4, 0x74, (byte) 0xF4,
            0x0C, (byte) 0x8C, 0x4C, (byte) 0xCC, 0x2C, (byte) 0xAC, 0x6C, (byte) 0xEC, 0x1C, (byte) 0x9C, 0x5C, (byte) 0xDC, 0x3C, (byte) 0xBC, 0x7C, (byte) 0xFC,
            0x02, (byte) 0x82, 0x42, (byte) 0xC2, 0x22, (byte) 0xA2, 0x62, (byte) 0xE2, 0x12, (byte) 0x92, 0x52, (byte) 0xD2, 0x32, (byte) 0xB2, 0x72, (byte) 0xF2,
            0x0A, (byte) 0x8A, 0x4A, (byte) 0xCA, 0x2A, (byte) 0xAA, 0x6A, (byte) 0xEA, 0x1A, (byte) 0x9A, 0x5A, (byte) 0xDA, 0x3A, (byte) 0xBA, 0x7A, (byte) 0xFA,
            0x06, (byte) 0x86, 0x46, (byte) 0xC6, 0x26, (byte) 0xA6, 0x66, (byte) 0xE6, 0x16, (byte) 0x96, 0x56, (byte) 0xD6, 0x36, (byte) 0xB6, 0x76, (byte) 0xF6,
            0x0E, (byte) 0x8E, 0x4E, (byte) 0xCE, 0x2E, (byte) 0xAE, 0x6E, (byte) 0xEE, 0x1E, (byte) 0x9E, 0x5E, (byte) 0xDE, 0x3E, (byte) 0xBE, 0x7E, (byte) 0xFE,
            0x01, (byte) 0x81, 0x41, (byte) 0xC1, 0x21, (byte) 0xA1, 0x61, (byte) 0xE1, 0x11, (byte) 0x91, 0x51, (byte) 0xD1, 0x31, (byte) 0xB1, 0x71, (byte) 0xF1,
            0x09, (byte) 0x89, 0x49, (byte) 0xC9, 0x29, (byte) 0xA9, 0x69, (byte) 0xE9, 0x19, (byte) 0x99, 0x59, (byte) 0xD9, 0x39, (byte) 0xB9, 0x79, (byte) 0xF9,
            0x05, (byte) 0x85, 0x45, (byte) 0xC5, 0x25, (byte) 0xA5, 0x65, (byte) 0xE5, 0x15, (byte) 0x95, 0x55, (byte) 0xD5, 0x35, (byte) 0xB5, 0x75, (byte) 0xF5,
            0x0D, (byte) 0x8D, 0x4D, (byte) 0xCD, 0x2D, (byte) 0xAD, 0x6D, (byte) 0xED, 0x1D, (byte) 0x9D, 0x5D, (byte) 0xDD, 0x3D, (byte) 0xBD, 0x7D, (byte) 0xFD,
            0x03, (byte) 0x83, 0x43, (byte) 0xC3, 0x23, (byte) 0xA3, 0x63, (byte) 0xE3, 0x13, (byte) 0x93, 0x53, (byte) 0xD3, 0x33, (byte) 0xB3, 0x73, (byte) 0xF3,
            0x0B, (byte) 0x8B, 0x4B, (byte) 0xCB, 0x2B, (byte) 0xAB, 0x6B, (byte) 0xEB, 0x1B, (byte) 0x9B, 0x5B, (byte) 0xDB, 0x3B, (byte) 0xBB, 0x7B, (byte) 0xFB,
            0x07, (byte) 0x87, 0x47, (byte) 0xC7, 0x27, (byte) 0xA7, 0x67, (byte) 0xE7, 0x17, (byte) 0x97, 0x57, (byte) 0xD7, 0x37, (byte) 0xB7, 0x77, (byte) 0xF7,
            0x0F, (byte) 0x8F, 0x4F, (byte) 0xCF, 0x2F, (byte) 0xAF, 0x6F, (byte) 0xEF, 0x1F, (byte) 0x9F, 0x5F, (byte) 0xDF, 0x3F, (byte) 0xBF, 0x7F, (byte) 0xFF
    };

    private XWDX11Header header;

    XWDImageReader(ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new XWDImageMetadata(header);
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.height;
    }

    private void readHeader() throws IOException {
        assertInput();

        if (header == null) {
            header = XWDX11Header.read(imageInput);
        }
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ImageTypeSpecifier rawImageType = getRawImageType(imageIndex);

//        ArrayList<ImageTypeSpecifier> specs = new ArrayList<>();
        // TODO: Add TYPE_3BYTE_BGR & TYPE_4BYTE_ABGR + TYPE_INT_ types?
//        if (rawImageType )

        return Collections.singletonList(rawImageType).iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        switch (header.visualClass) {
            case X11.VISUAL_CLASS_STATIC_GRAY:
            case X11.VISUAL_CLASS_GRAY_SCALE:
                return ImageTypeSpecifiers.createGrayscale(header.bitsPerPixel, DataBuffer.TYPE_BYTE, false);
            case X11.VISUAL_CLASS_STATIC_COLOR:
            case X11.VISUAL_CLASS_PSEUDO_COLOR:
                return ImageTypeSpecifiers.createFromIndexColorModel(header.colorMap);
            case X11.VISUAL_CLASS_TRUE_COLOR:
            case X11.VISUAL_CLASS_DIRECT_COLOR:
                int numComponents = header.numComponents();
                return ImageTypeSpecifiers.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), createBandArray(numComponents, header), DataBuffer.TYPE_BYTE, numComponents > 3, false);
            default:
                throw new IIOException(String.format("Unknown visual class: %d", header.visualClass));
        }
    }

    private Raster createRowRaster(byte[] row) {
        DataBuffer databuffer = new DataBufferByte(row, row.length);

        switch (header.visualClass) {
            case X11.VISUAL_CLASS_TRUE_COLOR:
            case X11.VISUAL_CLASS_DIRECT_COLOR:
                return Raster.createInterleavedRaster(databuffer, header.width, 1, header.bytesPerLine, header.bitsPerPixel / 8, createBandArray(header.numComponents(), header), null);
            default:
                return Raster.createPackedRaster(databuffer, header.width, 1, header.bitsPerPixel, null);
        }
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), header.width, header.height);
        WritableRaster raster = destination.getRaster();
        checkReadParamBandSettings(param, header.numComponents(), raster.getNumBands());

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, header.width, header.height, destination, srcRegion, destRegion);

        byte[] row = new byte[header.bytesPerLine];
        Raster rowRaster = createRowRaster(row).createChild(srcRegion.x, 0, destRegion.width, 1, 0, 0, null);

        final boolean reverseBits = header.bitsPerPixel < 8 && header.bitFillOrder == ByteOrder.LITTLE_ENDIAN;
        final boolean clearAlpha = destination.getColorModel().hasAlpha();
        final int xSub = param == null ? 1 : param.getSourceXSubsampling();
        final int ySub = param == null ? 1 : param.getSourceYSubsampling();

        imageInput.seek(header.pixelOffset);

        processImageStarted(imageIndex);

        for (int y = 0; y < srcRegion.y + srcRegion.height; y++) {
            if (y < srcRegion.y || y % ySub != 0) {
                // Skip row
                imageInput.skipBytes(row.length);
                continue;
            }

            imageInput.readFully(row);

            if (reverseBits) {
                for (int i = 0; i < row.length; i++) {
                    // TODO: Why do we have to inverse (XOR) as well?
                    row[i] = (byte) ~BIT_REVERSE_TABLE[row[i] & 0xff];
                }
            }

            if (clearAlpha) {
                // If we have alpha, inverse the alpha sample (or set it to opaque?)
                // TODO: Where's the alpha! First or last depending on byte order?
                for (int i = 0; i < row.length; i += rowRaster.getNumBands()) {
                    row[i] = (byte) ~row[i];
                }
            }

            if (xSub != 1) {
                // Horizontal subsampling
                int samplesPerPixel = header.numComponents();
                subsampleRow(row, srcRegion.x * samplesPerPixel, srcRegion.width, row, srcRegion.x * samplesPerPixel, samplesPerPixel, header.bitsPerRGB, xSub);
            }

            raster.setDataElements(0, (y - srcRegion.y) / ySub, rowRaster);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress((y - srcRegion.y) * 100f / srcRegion.height);
        }

        processImageComplete();

        return destination;
    }

    private int bytePos(int bitMask, int numBands) {
        switch (bitMask) {
            case 0xff:
                return numBands - 1;
            case 0xff00:
                return numBands - 2;
            case 0xff0000:
                return numBands - 3;
            case 0xff000000:
                return numBands - 4;
            default:
                // We only support full byte masks for now
                throw new IllegalArgumentException(String.format("Unsupported bitmask: 0x%08x", bitMask));
        }
    }

    private int[] createBandArray(int numBands, final XWDX11Header header) {
        int[] offsets = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            offsets[i] = bytePos(header.masks[i], numBands);
        }

        return offsets;
    }

    @Override
    protected void resetMembers() {
        header = null;
    }
}
