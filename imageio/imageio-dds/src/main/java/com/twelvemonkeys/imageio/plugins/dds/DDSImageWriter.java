package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.ImageWriterBase;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * A designated class to begin writing DDS file with headers, class {@link DDSImageDataEncoder} will handle image data encoding process
 */
class DDSImageWriter extends ImageWriterBase {
    protected DDSImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public DDSWriterParam getDefaultWriteParam() {
        return DDSWriterParam.DEFAULT_PARAM;
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam p) throws IOException {
        assertOutput();
        RenderedImage renderedImage = image.getRenderedImage();
        ensureTextureSize(renderedImage);
        ensureImageChannels(renderedImage);

        DDSWriterParam param = !(p instanceof DDSWriterParam) ? this.getDefaultWriteParam() : ((DDSWriterParam) p);
        //throw new IllegalArgumentException("ImageWriteParam must be a DDSWriterParam, got " + p.getClass().getSimpleName());

        imageOutput.setByteOrder(ByteOrder.BIG_ENDIAN);
        imageOutput.writeInt(DDS.MAGIC);
        imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        writeHeader(image, param);
        writeDXT10Header(param);

        //image data encoding
        DDSImageDataEncoder.writeImageData(imageOutput, renderedImage, param.getEncoderType());
        imageOutput.flush();
        long flushed = imageOutput.getFlushedPosition();
        int i = 0;
    }

    /**
     * Checking if the image has 3 channels (RGB) or 4 channels (RGBA) and if image has 8 bits/channel.
     */

    private void ensureImageChannels(RenderedImage renderedImage) {
        Raster data = renderedImage.getData();
        int numBands = data.getNumBands();
        if (numBands < 3)
            throw new IllegalStateException("Only image with 3 channels (RGB) or 4 channels (RGBA) is supported, got " + numBands + " channels");
        int sampleSize = data.getSampleModel().getSampleSize(0);
        if (sampleSize != 8)
            throw new IllegalStateException("Only image with 8 bits/channel is supported, got " + sampleSize);
    }

    /**
     * Checking if an image can be evenly divided into blocks of 4x4, ideally a power of 2.
     * e.g. 16x16, 32x32, 512x128, 512x512, 1024x512, 1024x1024, 2048x1024, ...
     */
    private void ensureTextureSize(RenderedImage renderedImage) {
        int w = renderedImage.getWidth();
        int h = renderedImage.getHeight();
        if (w % 4 != 0 || h % 4 != 0)
            throw new IllegalStateException(String.format("Image size must be dividable by 4, ideally a power of 2; got (%d x %d)", w, h));
    }


    private void writeHeader(IIOImage image, DDSWriterParam param) throws IOException {
        imageOutput.writeInt(DDS.HEADER_SIZE);
        imageOutput.writeInt(DDS.FLAG_CAPS | DDS.FLAG_HEIGHT | DDS.FLAG_WIDTH | DDS.FLAG_PIXELFORMAT | param.getOptionalBitFlags());
        RenderedImage renderedImage = image.getRenderedImage();
        int height = renderedImage.getHeight();
        imageOutput.writeInt(height);
        int width = renderedImage.getWidth();
        imageOutput.writeInt(width);
        writePitchOrLinearSize(height, width, param);
        //dwDepth
        imageOutput.writeInt(0);
        //dwMipmapCount
        imageOutput.writeInt(1);
        //reserved
        imageOutput.write(new byte[44]);
        //pixFmt
        writePixelFormat(param);
        //dwCaps, right now we keep it simple by only using DDSCAP_TEXTURE as it is required.
        imageOutput.writeInt(DDS.DDSCAPS_TEXTURE);
        //dwCaps2, unused for now as we are not working with cube maps
        imageOutput.writeInt(0);
        //dwCaps3, dwCaps4, dwReserved2 : 3 unused integers
        imageOutput.write(new byte[12]);

    }

    //https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-pixelformat
    private void writePixelFormat(DDSWriterParam param) throws IOException {
        imageOutput.writeInt(DDS.DDSPF_SIZE);
        writePixelFormatFlags(param);
        writeFourCC(param);
        writeRGBAData(param);
    }

    private void writeDXT10Header(DDSWriterParam param) throws IOException {
        if (param.isUsingDxt10()) {
            //dxgiFormat
            imageOutput.writeInt(param.getDxgiFormat());
            //resourceDimension
            imageOutput.writeInt(DDS.D3D10_RESOURCE_DIMENSION_TEXTURE2D);
            //miscFlag
            imageOutput.writeInt(0);
            //arraySize
            imageOutput.writeInt(1);
            //miscFlag2
            imageOutput.writeInt(0);
        }
    }

    private void writeRGBAData(DDSWriterParam param) throws IOException {
        if (!param.isUsingDxt10() && !param.getEncoderType().isFourCC()) {
            //dwRGBBitCount
            imageOutput.writeInt(param.getEncoderType().getBitsOrBlockSize());

            int[] mask = param.getEncoderType().getRGBAMask();
            //dwRBitMask
            imageOutput.writeInt(mask[0]);
            //dwGBitMask
            imageOutput.writeInt(mask[1]);
            //dwBitMask
            imageOutput.writeInt(mask[2]);
            //dwABitMask
            imageOutput.writeInt(mask[3]);
        } else {
            //write 5 zero integers as fourCC is used
            imageOutput.write(new byte[20]);
        }
    }

    private void writeFourCC(DDSWriterParam param) throws IOException {
        if (param.isUsingDxt10()) {
            imageOutput.writeInt(DDSType.DXT10.value());
        } else if (param.getEncoderType().isFourCC())
            imageOutput.writeInt(param.getEncoderType().getFourCC());

    }

    private void writePixelFormatFlags(DDSWriterParam param) throws IOException {
        if (param.isUsingDxt10() || param.getEncoderType().isFourCC()) {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_FOURCC);
        } else {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_RGB | (param.getEncoderType().isAlphaMaskSupported() ? DDS.PIXEL_FORMAT_FLAG_ALPHAPIXELS : 0));
        }
    }

    private void writePitchOrLinearSize(int height, int width, DDSWriterParam param) throws IOException {
        DDSEncoderType type = param.getEncoderType();
        int bitsOrBlockSize = type.getBitsOrBlockSize();
        if (type.isBlockCompression()) {
            imageOutput.writeInt(((width + 3) / 4) * ((height + 3) / 4) * bitsOrBlockSize);
        } else {
            imageOutput.writeInt(width * bitsOrBlockSize);
        }
    }

    private boolean doesFormatSupportAlpha(DDSType type) {
        switch (type) {
            case X8B8G8R8:
                return false;
            case A8B8G8R8:
            case A8R8G8B8:
                return true;
            default:
                throw new IllegalArgumentException("FOURCC formats are not expected.");
        }
    }

    private int getBitsPerPixel(DDSType type) {
        switch (type) {
            case A1R5G5B5:
            case X1R5G5B5:
            case A4R4G4B4:
            case X4R4G4B4:
            case R5G6B5:
                return 16;
            case R8G8B8:
                return 24;
            case A8B8G8R8:
            case X8B8G8R8:
            case A8R8G8B8:
            case X8R8G8B8:
                return 32;

            default:
                throw new IllegalArgumentException("Cannot determine bits per pixel with " + type);
        }
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Direct Draw Surface does not support metadata.");
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Direct Draw Surface does not support metadata.");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Use 1 input file at a time.");
        try (ImageOutputStream outputStream
                     //RandomAccessFile-based output stream seems to take a bit more time to write and output size tend to double the expected
                     //this is expected to write data in a linear way, and not depended on RAF.
                     = new MemoryCacheImageOutputStream(Files.newOutputStream(Paths.get("test_output.dds"), StandardOpenOption.TRUNCATE_EXISTING))) {
            DDSImageWriter writer = new DDSImageWriter(null);
            writer.setOutput(outputStream);
            writer.write(ImageIO.read(new File(args[0])));
        }
    }
}
