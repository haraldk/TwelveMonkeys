package com.twelvemonkeys.imageio.plugins.dds;

import com.sun.imageio.plugins.bmp.BMPImageWriter;
import com.twelvemonkeys.imageio.ImageWriterBase;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A designated class to begin writing DDS file with headers, class {@link DDSImageDataEncoder} will handle image data encoding process
 */
class DDSImageWriter extends ImageWriterBase {
    //maybe we move it to the DDSType enum as an extra constructor param ?
    private static final Map<DDSType, int[]> ARGB_MASKS;
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

        DDSWriterParam param = !(p instanceof DDSWriterParam) ? this.getDefaultWriteParam() : ((DDSWriterParam) p);
        //throw new IllegalArgumentException("ImageWriteParam must be a DDSWriterParam, got " + p.getClass().getSimpleName());

        imageOutput.setByteOrder(ByteOrder.BIG_ENDIAN);
        imageOutput.writeInt(DDS.MAGIC);
        imageOutput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        writeHeader(image, param);
        writeDXT10Header(param);

        //image data encoding
        DDSImageDataEncoder.writeImageData(imageOutput, renderedImage, param.getType());
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
        writeFourCC(param.getType());
        writeRGBAData(param.getType());
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

    private void writeRGBAData(DDSType type) throws IOException {
        if (!type.isFourCC()) {
            //dwRGBBitCount
            imageOutput.writeInt(getBitsPerPixel(type));

            int[] mask = ARGB_MASKS.get(type);
            Objects.requireNonNull(mask, "no RBGA mask found for type " + type);
            if (mask.length != 4) throw new IllegalStateException("RBGA mask length is not 4, got " + mask.length);

            //dwRBitMask
            imageOutput.writeInt(mask[0]);
            //dwGBitMask
            imageOutput.writeInt(mask[1]);
            //dwBitMask
            imageOutput.writeInt(mask[2]);
            //dwABitMask
            imageOutput.writeInt(mask[3]);
        } else {
            //write 5 zero integers
            imageOutput.write(new byte[20]);
        }
    }

    private void writeFourCC(DDSType type) throws IOException {
        if (type.isFourCC())
            imageOutput.writeInt(type.value());
    }

    private void writePixelFormatFlags(DDSWriterParam param) throws IOException {
        if (param.isUsingDxt10() || param.getType().isFourCC()) {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_FOURCC);
        } else {
            imageOutput.writeInt(DDS.PIXEL_FORMAT_FLAG_RGB | (doesFormatSupportAlpha(param.getType()) ? DDS.PIXEL_FORMAT_FLAG_ALPHAPIXELS : 0));
        }
    }

    private void writePitchOrLinearSize(int height, int width, DDSWriterParam param) throws IOException {
        DDSType type = param.getType();
        if (type.isBlockCompression()) {
            //we don't have BC4 support yet and DXT1 and BC1 is pretty much the same so we keep it this way for now.
            int blockSize = (type == DDSType.DXT1 ? 8 : 16);
            imageOutput.writeInt(((width + 3) / 4) * ((height + 3) / 4) * blockSize);
        } else {
            imageOutput.writeInt(width * getBitsPerPixel(type));
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

    static {
        ARGB_MASKS = new HashMap<>();
        ARGB_MASKS.put(DDSType.A1R5G5B5, DDSReader.A1R5G5B5_MASKS);
        ARGB_MASKS.put(DDSType.X1R5G5B5, DDSReader.X1R5G5B5_MASKS);
        ARGB_MASKS.put(DDSType.A4R4G4B4, DDSReader.A4R4G4B4_MASKS);
        ARGB_MASKS.put(DDSType.X4R4G4B4, DDSReader.X4R4G4B4_MASKS);
        ARGB_MASKS.put(DDSType.R5G6B5, DDSReader.R5G6B5_MASKS);
        ARGB_MASKS.put(DDSType.R8G8B8, DDSReader.R8G8B8_MASKS);
        ARGB_MASKS.put(DDSType.A8B8G8R8, DDSReader.A8B8G8R8_MASKS);
        ARGB_MASKS.put(DDSType.X8B8G8R8, DDSReader.X8B8G8R8_MASKS);
        ARGB_MASKS.put(DDSType.A8R8G8B8, DDSReader.A8R8G8B8_MASKS);
        ARGB_MASKS.put(DDSType.X8R8G8B8, DDSReader.X8R8G8B8_MASKS);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Use 1 input file at a time.");
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(new File("test_output.dds"))) {
            DDSImageWriter writer = new DDSImageWriter(null);
            writer.setOutput(outputStream);
            writer.write(ImageIO.read(new File(args[0])));
        }
    }
}
