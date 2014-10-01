package com.twelvemonkeys.imageio.plugins.pnm;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;

import com.twelvemonkeys.imageio.ImageWriterBase;
import org.w3c.dom.NodeList;

public final class PNMImageWriter extends ImageWriterBase {

    PNMImageWriter(final ImageWriterSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public boolean canWriteRasters() {
        return true;
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        // TODO: Issue warning if streamMetadata is non-null?
        // TODO: Issue warning if IIOImage contains thumbnails or other data we can't store?

        HeaderWriter.write(image, getOriginatingProvider(), imageOutput);

        // TODO: Sub region
        // TODO: Subsampling
        // TODO: Source bands

        processImageStarted(0);
        writeImageData(image);
        processImageComplete();
    }

    private void writeImageData(final IIOImage image) throws IOException {
        // - dump data as is (or convert, if TYPE_INT_xxx)
        // Enforce RGB/CMYK order for such data!

        // TODO: Loop over x/y tiles, using 0,0 is only valid for BufferedImage
        // TODO: PNM/PAM does not support tiling, we must iterate all tiles along the x-axis for each row we write
        Raster tile = image.hasRaster() ? image.getRaster() : image.getRenderedImage().getTile(0, 0);

        SampleModel sampleModel = tile.getSampleModel();

        DataBuffer dataBuffer = tile.getDataBuffer();

        int tileWidth = tile.getWidth();
        int tileHeight = tile.getHeight();

        final int transferType = sampleModel.getTransferType();
        Object data = null;
        for (int y = 0; y < tileHeight; y++) {
            data = sampleModel.getDataElements(0, y, tileWidth, 1, data, dataBuffer);
            // TODO: Support other (short, float) data types
            if (transferType == DataBuffer.TYPE_BYTE) {
                imageOutput.write((byte[]) data);
            }
            else if (transferType == DataBuffer.TYPE_USHORT) {
                short[] shortData = (short[]) data;
                imageOutput.writeShorts(shortData, 0, shortData.length);
            }

            processImageProgress(y * 100f / tileHeight); // TODO: Take tile y into account
            if (abortRequested()) {
                processWriteAborted();
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        File output = new File(input.getParentFile(), input.getName().replace('.', '_') + ".ppm");

        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            System.err.println("input Image == null");
            System.exit(-1);
        }

        System.out.println("image: " + image);

        ImageWriter writer = new PNMImageWriterSpi().createWriterInstance();

        if (!output.exists()) {
            writer.setOutput(ImageIO.createImageOutputStream(output));
            writer.write(image);
        }
        else {
            System.err.println("Output file " + output + " already exists.");
            System.exit(-1);
        }
    }
}
