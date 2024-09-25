package com.twelvemonkeys.imageio.plugins.dds;

import static com.twelvemonkeys.imageio.util.IIOUtil.subsampleRow;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;

public final class DDSImageReader extends ImageReaderBase {

    private DDSHeader header;

    public DDSImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        header = null;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getWidth(imageIndex);
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight(imageIndex);
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        assertInput();
        readHeader();

        return header.getMipMapCount();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        return Collections.singletonList(getRawImageType(imageIndex)).iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        processImageStarted(imageIndex);

        DDSReader dds = new DDSReader(header);
        int[] pixels = dds.read(imageInput, imageIndex);

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();

        computeRegions(param, width, height, destination, srcRegion, destRegion);

        int srcXStep = param != null ? param.getSourceXSubsampling() : 1;
        int srcYStep = param != null ? param.getSourceYSubsampling() : 1;
        int srcMaxY = srcRegion.y + srcRegion.height;

        for (int srcY = srcRegion.y, destY = destRegion.y; srcY < srcMaxY; srcY += srcYStep, destY++) {
            int offset = width * srcY + srcRegion.x;

            subsampleRow(pixels, offset, width, pixels, offset, 1, 32, srcXStep);
            destination.setRGB(destRegion.x, destY, destRegion.width, 1, pixels, offset, width);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100f * srcY / srcRegion.height);
        }

        processImageComplete();

        return destination;
    }

    private void readHeader() throws IOException {
        if (header == null) {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            header = DDSHeader.read(imageInput);

            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    public static void main(final String[] args) throws IOException {
        for (String arg : args) {
            File file = new File(arg);
            BufferedImage image = ImageIO.read(file);
            showIt(image, file.getName());
        }
    }
}
