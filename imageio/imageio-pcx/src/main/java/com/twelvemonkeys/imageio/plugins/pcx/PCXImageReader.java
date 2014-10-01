package com.twelvemonkeys.imageio.plugins.pcx;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.xml.XMLSerializer;

public final class PCXImageReader extends ImageReaderBase {
    /** 8 bit ImageTypeSpecifer used for reading bitplane images. */
    private static final ImageTypeSpecifier GRAYSCALE = ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, false);

    private PCXHeader header;
    private boolean readPalette;
    private IndexColorModel vgaPalette;

    public PCXImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        header = null;
        readPalette = false;
        vgaPalette = null;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getWidth();
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        // TODO: Implement
        specifiers.add(rawType);

        return specifiers.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        int channels = header.getChannels();
        int paletteInfo = header.getPaletteInfo();
        ColorSpace cs = paletteInfo == PCX.PALETTEINFO_GRAY ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpace.getInstance(ColorSpace.CS_sRGB);

        switch (header.getBitsPerPixel()) {
            case 1:
            case 2:
            case 4:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(header.getEGAPalette());
            case 8:
                // We may have IndexColorModel here for 1 channel images
                if (channels == 1 && paletteInfo != PCX.PALETTEINFO_GRAY) {
                    IndexColorModel palette = getVGAPalette();
                    if (palette == null) {
                        throw new IIOException("Expected VGA palette not found");
                    }

                    return IndexedImageTypeSpecifier.createFromIndexColorModel(palette);
                }

                // PCX has 1 or 3 channels for 8 bit gray or 24 bit RGB, will be validated by ImageTypeSpecifier
                return ImageTypeSpecifier.createBanded(cs, createIndices(channels, 1), createIndices(channels, 0), DataBuffer.TYPE_BYTE, false, false);
            case 24:
                // Some sources says this is possible... Untested.
                return ImageTypeSpecifier.createInterleaved(cs, createIndices(channels, 0), DataBuffer.TYPE_BYTE, false, false);
            default:
                throw new IIOException("Unknown number of bytes per pixel: " + header.getBitsPerPixel());
        }
    }

    private int[] createIndices(final int bands, int increment) {
        int[] indices = new int[bands];

        for (int i = 0; i < bands; i++) {
            indices[i] = i * increment;
        }

        return indices;
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        if (header.getPaletteInfo() != PCX.PALETTEINFO_COLOR && header.getPaletteInfo() != PCX.PALETTEINFO_GRAY) {
            processWarningOccurred(String.format("Unsupported color mode: %d, colors may look incorrect", header.getPaletteInfo()));
        }

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, imageTypes, width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster destRaster = clipToRect(destination.getRaster(), destRegion, param != null ? param.getDestinationBands() : null);
        checkReadParamBandSettings(param, rawType.getNumBands(), destRaster.getNumBands());

        int compression = header.getCompression();

        // Wrap input (COMPRESSION_RLE is really the only value allowed)
        DataInput input = compression == PCX.COMPRESSION_RLE
                ? new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(imageInput), new RLEDecoder()))
                : imageInput;

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        processImageStarted(imageIndex);

        if (rawType.getColorModel() instanceof IndexColorModel && header.getChannels() > 1) {
            // Bit planes!
            // Create raster from a default 8 bit layout
            WritableRaster rowRaster = GRAYSCALE.createBufferedImage(header.getWidth(), 1).getRaster();

            // Clip to source region
            Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                                              param != null ? param.getSourceBands() : null,
                                              param != null ? param.getSourceXSubsampling() : 1);

            int planeWidth = header.getBytesPerLine();
            byte[] planeData = new byte[planeWidth * 8];

            byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData();

            for (int y = 0; y < height; y++) {
                switch (header.getBitsPerPixel()) {
                    case 1:
                        readRowByte(input, srcRegion, xSub, ySub, planeData, 0, planeWidth * header.getChannels(), destRaster, clippedRow, y);
                        break;
                    default:
                        throw new AssertionError();
                }

                int pixelPos = 0;
                for (int planePos = 0; planePos < planeWidth; planePos++) {
                    BitRotator.bitRotateCW(planeData, planePos, planeWidth, rowDataByte, pixelPos, 1);
                    pixelPos += 8;
                }

                processImageProgress(100f * y / height);

                if (y < srcRegion.y) {
                    break;
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }
        }
        else {
            // Can't use width here, as we need to take bytesPerLine into account, and re-create a width based on this
            int rowWidth = (header.getBytesPerLine() * 8) / header.getBitsPerPixel();
            WritableRaster rowRaster = rawType.createBufferedImage(rowWidth, 1).getRaster();

            // Clip to source region
            Raster clippedRow = clipRowToRect(rowRaster, srcRegion,
                                              param != null ? param.getSourceBands() : null,
                                              param != null ? param.getSourceXSubsampling() : 1);

            for (int y = 0; y < height; y++) {
                for (int c = 0; c < header.getChannels(); c++) {
                    WritableRaster destChannel = destRaster.createWritableChild(destRaster.getMinX(), destRaster.getMinY(), destRaster.getWidth(), destRaster.getHeight(), 0, 0, new int[] {c});
                    Raster srcChannel = clippedRow.createChild(clippedRow.getMinX(), 0, clippedRow.getWidth(), 1, 0, 0, new int[] {c});

                    switch (header.getBitsPerPixel()) {
                        case 1:
                        case 2:
                        case 4:
                        case 8:
                            byte[] rowDataByte = ((DataBufferByte) rowRaster.getDataBuffer()).getData(c);
                            readRowByte(input, srcRegion, xSub, ySub, rowDataByte, 0, rowDataByte.length, destChannel, srcChannel, y);
                            break;
                        default:
                            throw new AssertionError();
                    }

                    processImageProgress(100f * y / height * c / header.getChannels());

                    if (y < srcRegion.y) {
                        break;
                    }

                    if (abortRequested()) {
                        break;
                    }
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }
        }

        processImageComplete();

        return destination;
    }

    private void readRowByte(final DataInput input,
                             Rectangle srcRegion,
                             int xSub,
                             int ySub,
                             byte[] rowDataByte, final int off, final int length,
                             WritableRaster destChannel,
                             Raster srcChannel,
                             int y) throws IOException {
        // If subsampled or outside source region, skip entire row
        if (y % ySub != 0 || y < srcRegion.y || y >= srcRegion.y + srcRegion.height) {
            input.skipBytes(length);

            return;
        }

        input.readFully(rowDataByte, off, length);

        // Subsample horizontal
        if (xSub != 1) {
            for (int x = 0; x < srcRegion.width / xSub; x++) {
                rowDataByte[srcRegion.x + x] = rowDataByte[srcRegion.x + x * xSub];
            }
        }

        int dstY = (y - srcRegion.y) / ySub;
        destChannel.setDataElements(0, dstY, srcChannel);
    }

    private Raster clipRowToRect(final Raster raster, final Rectangle rect, final int[] bands, final int xSub) {
        if (rect.contains(raster.getMinX(), 0, raster.getWidth(), 1)
                && xSub == 1
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createChild(rect.x / xSub, 0, rect.width / xSub, 1, 0, 0, bands);
    }

    private WritableRaster clipToRect(final WritableRaster raster, final Rectangle rect, final int[] bands) {
        if (rect.contains(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight())
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createWritableChild(rect.x, rect.y, rect.width, rect.height, 0, 0, bands);
    }

    private void readHeader() throws IOException {
        if (header == null) {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            header = PCXHeader.read(imageInput);
            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    @Override public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new PCXMetadata(header, getVGAPalette());
    }

    private IndexColorModel getVGAPalette() throws IOException {
        if (!readPalette) {
            readHeader();

            // Mark palette as read, to avoid further attempts
            readPalette = true;

            // Wee can't simply skip to an offset, as the RLE compression makes the file size unpredictable
            skiptToEOF(imageInput);

            // Seek backwards from EOF
            long paletteStart = imageInput.getStreamPosition() - 769;
            if (paletteStart <= imageInput.getFlushedPosition()) {
                return null;
            }

            imageInput.seek(paletteStart);

            byte val = imageInput.readByte();

            if (val == PCX.VGA_PALETTE_MAGIC) {
                byte[] palette = new byte[768]; // 256 * 3 for RGB
                imageInput.readFully(palette);

                vgaPalette = new IndexColorModel(8, 256, palette, 0, false);

                return vgaPalette;
            }

            return null;
        }

        return vgaPalette;
    }

    // TODO: Candidate util method
    private static long skiptToEOF(final ImageInputStream stream) throws IOException {
        long length = stream.length();

        if (length > 0) {
            // Known length, skip there and we're done.
            stream.seek(length);
        }
        else {
            // Otherwise, seek to EOF the hard way.
            // First, store stream position...
            long pos = stream.getStreamPosition();

            // ...skip 1k blocks until we're passed EOF...
            while (stream.skipBytes(1024l) > 0) {
                if (stream.read() == -1) {
                    break;
                }

                pos = stream.getStreamPosition();
            }

            // ...go back to last known pos...
            stream.seek(pos);

            // ...finally seek until EOF one byte at a time. Done.
            while (stream.read() != -1) {
            }
        }

        return stream.getStreamPosition();
    }

    public static void main(String[] args) throws IOException {
        PCXImageReader reader = new PCXImageReader(null);

        for (String arg : args) {
            File in = new File(arg);
            reader.setInput(ImageIO.createImageInputStream(in));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(reader.getImageTypes(0).next());
//            param.setSourceSubsampling(2, 3, 0, 0);
//            param.setSourceSubsampling(2, 1, 0, 0);
//
//            int width = reader.getHdpi(0);
//            int height = reader.getVdpi(0);
//
//            param.setSourceRegion(new Rectangle(width / 4, height / 4, width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2, width / 2, height / 2));

            System.err.println("header: " + reader.header);

            BufferedImage image = reader.read(0, param);

            System.err.println("image: " + image);

            showIt(image, in.getName());

            new XMLSerializer(System.out, System.getProperty("file.encoding"))
                    .serialize(reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);

//            File reference = new File(in.getParent() + "/../reference", in.getName().replaceAll("\\.p(a|b|g|p)m", ".png"));
//            if (reference.exists()) {
//                System.err.println("reference.getAbsolutePath(): " + reference.getAbsolutePath());
//                showIt(ImageIO.read(reference), reference.getName());
//            }

//            break;
        }
    }
}
