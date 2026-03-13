package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.SequenceSupport;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import java.awt.Dimension;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ImageWriter implementation for Microsoft DirectDraw Surface (DDS) format.
 *
 * @author KhanTypo
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
class DDSImageWriter extends ImageWriterBase {

    private final SequenceSupport mipmapSequence = new SequenceSupport();

    private long headerStartPos;
    private DDSType mipmapType;
    private Dimension mipmapDimension;

    protected DDSImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public DDSImageWriteParam getDefaultWriteParam() {
        return new DDSImageWriteParam();
    }

    @Override
    protected void resetMembers() {
        headerStartPos = 0;
        mipmapSequence.reset();
        mipmapType = null;
        mipmapDimension = null;
    }

    @Override
    public boolean canWriteRasters() {
        return true;
    }

    @Override
    public boolean canWriteSequence() {
        return true;
    }

    @Override
    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        assertOutput();
        mipmapSequence.start();

        imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        imageOutput.writeInt(DDS.MAGIC);
        imageOutput.flush();

        headerStartPos = imageOutput.getStreamPosition();
    }

    @Override
    public void endWriteSequence() throws IOException {
        int mipmapCount = mipmapSequence.end();

        // Go back and update header
        updateHeader(mipmapCount);

        mipmapType = null;
        mipmapDimension = null;

        imageOutput.flush();
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        prepareWriteSequence(streamMetadata);
        writeToSequence(image, param);
        endWriteSequence();
    }

    @Override
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
        int mipmapIndex = mipmapSequence.advance();

        Raster raster = getRaster(image);
        ensureImageChannels(raster);
        ensureTextureDimension(raster);
        mipmapDimension = new Dimension(raster.getWidth(), raster.getHeight());

        DDSImageWriteParam ddsParam = param instanceof DDSImageWriteParam
            ? ((DDSImageWriteParam) param)
            : IIOUtil.copyStandardParams(param, getDefaultWriteParam());

        DDSType type = ddsParam.type();
        if (mipmapType == null) {
            mipmapType = type;
        }
        else if (type != mipmapType) {
            processWarningOccurred(mipmapIndex, "All images in DDS mipmap must use same pixel format and compression");
        }
        if (mipmapType == null) {
            throw new IIOException("Only compressed DDS using DXT1-5 or DXT10 with block compression is currently supported");
        }

        if (mipmapIndex == 0) {
            writeHeader(raster.getWidth(), raster.getHeight(), mipmapType, ddsParam.isWriteDXT10());
            if (ddsParam.isWriteDXT10()) {
                writeDXT10Header(ddsParam.getDxgiFormat());
            }
        }

        processImageStarted(mipmapIndex);
        processImageProgress(0f);

        DDSImageDataEncoder.writeImageData(imageOutput, raster, mipmapType.compression);

        processImageProgress(100f);
        processImageComplete();
    }

    private static Raster getRaster(IIOImage image) throws IIOException {
        if (image.hasRaster()) {
            return image.getRaster();
        }
        else {
            RenderedImage renderedImage = image.getRenderedImage();

            if (renderedImage.getNumXTiles() != 1 || renderedImage.getNumYTiles() != 1) {
                throw new IIOException("Only single tile images supported");
            }

            return renderedImage.getTile(0, 0);
        }
    }

    /**
     * Checking if the image has 3 channels (RGB) or 4 channels (RGBA) and if image has 8 bits/channel.
     *
     * @see DDSImageWriterSpi#canEncodeImage(ImageTypeSpecifier)
     */
    private void ensureImageChannels(Raster data) throws IIOException {
        int numBands = data.getNumBands();
        if (numBands < 3 || numBands > 4) {
            throw new IIOException(
                "Only image with 3 channels (RGB) or 4 channels (RGBA) is supported, got " + numBands + " channels");
        }

        int sampleSize = data.getSampleModel().getSampleSize(0);
        if (sampleSize != 8) {
            throw new IIOException("Only image with 8 bits/channel is supported, got " + sampleSize);
        }
    }

    /**
     * Checking if an image can be evenly divided into blocks of 4x4, ideally a power of 2.
     * e.g. 16x16, 32x32, 512x128, 512x512, 1024x512, 1024x1024, 2048x1024...
     */
    private void ensureTextureDimension(Raster raster) throws IIOException {
        int width = raster.getWidth();
        int height = raster.getHeight();

        // Should also allow mipmaps 2x2 and 1x1?
        if (width % 4 != 0 || height % 4 != 0) {
            throw new IIOException(String.format("Image dimensions must be dividable by 4, ideally a power of 2; got %dx%d", width, height));
        }

        if (mipmapDimension != null && (mipmapDimension.width != width * 2|| mipmapDimension.height != height * 2)) {
            throw new IIOException(
                String.format("For mipmap, image dimensions must be exactly half of previous (%dx%d); got %dx%d",
                mipmapDimension.width, mipmapDimension.height, width, height)
            );
        }
    }

    private void writeHeader(int width, int height, DDSType type, boolean writeDXT10) throws IOException {
        imageOutput.writeInt(DDS.HEADER_SIZE);
        int linearSizeOrPitch = type.isBlockCompression() ? DDS.FLAG_LINEARSIZE : DDS.FLAG_PITCH;
        imageOutput.writeInt(DDS.FLAG_CAPS | DDS.FLAG_HEIGHT | DDS.FLAG_WIDTH | DDS.FLAG_PIXELFORMAT | linearSizeOrPitch);

        imageOutput.writeInt(height);
        imageOutput.writeInt(width);
        writePitchOrLinearSize(height, width, type);
        //dwDepth
        imageOutput.writeInt(0);
        //dwMipmapCount
        imageOutput.writeInt(1); // Should probably write 0 here for non-mipmap?
        //reserved
        imageOutput.write(new byte[44]);
        //pixFmt
        writePixelFormat(type, writeDXT10);
        //dwCaps, right now we keep it simple by only using DDSCAP_TEXTURE as it is required.
        imageOutput.writeInt(DDS.DDSCAPS_TEXTURE);
        //dwCaps2, unused for now as we are not working with cube maps
        imageOutput.writeInt(0);
        //dwCaps3, dwCaps4, dwReserved2 : 3 unused integers
        imageOutput.write(new byte[12]);
    }

    private void updateHeader(int mipmapCount) throws IOException {
        if (mipmapCount == 1) {
            // Fast case, nothing to do
            return;
        }

        long streamPosition = imageOutput.getStreamPosition();
        imageOutput.seek(headerStartPos + 4); // Seek back to header start, skip 4 byte header size

        int flags = imageOutput.readInt();
        imageOutput.seek(imageOutput.getStreamPosition() - 4);
        imageOutput.writeInt(flags | DDS.FLAG_MIPMAPCOUNT);

        imageOutput.seek(imageOutput.getStreamPosition() + 16);
        imageOutput.writeInt(mipmapCount);

        imageOutput.seek(streamPosition); // Restore pos
    }

    //https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-pixelformat
    private void writePixelFormat(DDSType type, boolean writeDXT10) throws IOException {
        imageOutput.writeInt(DDS.PIXELFORMAT_SIZE);
        writePixelFormatFlags(type, writeDXT10);
        writeFourCC(type, writeDXT10);
        writeRGBAData(type, writeDXT10);
    }

    private void writeDXT10Header(int dxgiFormat) throws IOException {
        //dxgiFormat
        imageOutput.writeInt(dxgiFormat);
        //resourceDimension
        imageOutput.writeInt(DDS.D3D10_RESOURCE_DIMENSION_TEXTURE2D);
        //miscFlag
        imageOutput.writeInt(0);
        //arraySize
        imageOutput.writeInt(1);
        //miscFlag2
        imageOutput.writeInt(0);
    }

    private void writeRGBAData(DDSType type, boolean writeDXT10) throws IOException {
        if (!writeDXT10 && !type.isFourCC()) {
            //dwRGBBitCount
            imageOutput.writeInt(type.blockSize() * 8); // TODO: Is bitcount always a multiple of 8?

            //dwRBitMask
            imageOutput.writeInt(type.rgbaMasks[0]);
            //dwGBitMask
            imageOutput.writeInt(type.rgbaMasks[1]);
            //dwBBitMask
            imageOutput.writeInt(type.rgbaMasks[2]);
            //dwABitMask
            imageOutput.writeInt(type.rgbaMasks[3]);
        }
        else {
            //write 5 zero integers as fourCC is used
            imageOutput.write(new byte[20]);
        }
    }

    private void writeFourCC(DDSType type, boolean writeDXT10) throws IOException {
        if (writeDXT10) {
            imageOutput.writeInt(DDSType.DXT10.fourCC());
        }
        else if (type.isFourCC()) {
            imageOutput.writeInt(type.fourCC());
        }
        else {
            // No fourCC, custom format...
            imageOutput.writeInt(0);
        }
    }

    private void writePixelFormatFlags(DDSType type, boolean writeDXT10) throws IOException {
        if (writeDXT10 || type.isFourCC()) {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_FOURCC);
        }
        else {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_RGB
                | (type.rgbaMasks != null && type.rgbaMasks[3] != 0 ? DDS.PIXEL_FORMAT_FLAG_ALPHAPIXELS : 0));
        }
    }

    private void writePitchOrLinearSize(int height, int width, DDSType type) throws IOException {
        if (type.isBlockCompression()) {
            imageOutput.writeInt(((width + 3) / 4) * ((height + 3) / 4) * type.blockSize());
        }
        else {
            imageOutput.writeInt(width * type.blockSize());
        }
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        DDSType type = param instanceof DDSImageWriteParam
            ? ((DDSImageWriteParam) param).type()
            : DDSImageWriteParam.DEFAULT_TYPE;

        return new DDSImageMetadata(imageType, type);
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        // Nothing useful to convert here...
        return getDefaultImageMetadata(imageType, param);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Use 1 input file at a time.");
        }

        ImageIO.write(ImageIO.read(new File(args[0])), "dds", new MemoryCacheImageOutputStream(Files.newOutputStream(Paths.get("output.dds"))));
    }
}
