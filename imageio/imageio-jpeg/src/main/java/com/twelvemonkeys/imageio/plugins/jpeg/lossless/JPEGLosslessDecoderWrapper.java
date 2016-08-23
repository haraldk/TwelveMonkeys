package com.twelvemonkeys.imageio.plugins.jpeg.lossless;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.IOException;

/**
 * This class provides the conversion of input data
 * containing a JPEG Lossless to an BufferedImage.
 * <p>
 * Take care, that only the following lossless formats are supported:
 * 1.2.840.10008.1.2.4.57 JPEG Lossless, Nonhierarchical (Processes 14)
 * 1.2.840.10008.1.2.4.70 JPEG Lossless, Nonhierarchical (Processes 14 [Selection 1])
 * <p>
 * Currently the following conversions are supported
 * - 24Bit, RGB       -> BufferedImage.TYPE_INT_RGB
 * -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
 * - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
 *
 * @author Hermann Kroll
 */
public class JPEGLosslessDecoderWrapper {

    /**
     * Decodes a JPEG Lossless stream to a {@code BufferedImage}.
     * Currently the following conversions are supported:
     * - 24Bit, RGB       -> BufferedImage.TYPE_3BYTE_BGR
     * -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
     * - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
     *
     * @param input input stream which contains a jpeg lossless data
     * @return if successfully a BufferedImage is returned
     * @throws IOException is thrown if the decoder failed or a conversion is not supported
     */
    public BufferedImage readImage(final ImageInputStream input) throws IOException {
        JPEGLosslessDecoder decoder = new JPEGLosslessDecoder(input);

        int[][] decoded = decoder.decode();
        int width = decoder.getDimX();
        int height = decoder.getDimY();

        if (decoder.getNumComponents() == 1) {
            switch (decoder.getPrecision()) {
                case 8:
                    return to8Bit1ComponentGrayScale(decoded, width, height);
                case 16:
                    return to16Bit1ComponentGrayScale(decoded, width, height);
                default:
                    throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 1 component cannot be decoded");
            }
        }
        //rgb
        if (decoder.getNumComponents() == 3) {
            switch (decoder.getPrecision()) {
                case 8:
                    return to24Bit3ComponentRGB(decoded, width, height);

                default:
                    throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 3 components cannot be decoded");
            }
        }

        throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and " + decoder.getNumComponents() + " component(s) cannot be decoded");
    }

    public Raster readRaster(final ImageInputStream input) throws IOException {
        // TODO: Can perhaps be implemented faster
        return readImage(input).getRaster();
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 16 bit, componentCount = 1
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_USHORT_GRAY
     */
    private BufferedImage to16Bit1ComponentGrayScale(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        short[] imageBuffer = ((DataBufferUShort) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length; i++) {
            imageBuffer[i] = (short) decoded[0][i];
        }

        return image;
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 8 bit, componentCount = 1
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_BYTE_GRAY
     */
    private BufferedImage to8Bit1ComponentGrayScale(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length; i++) {
            imageBuffer[i] = (byte) decoded[0][i];
        }

        return image;
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 8 bit, componentCount = 3
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_3BYTE_RGB
     */
    private BufferedImage to24Bit3ComponentRGB(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length / 3; i++) {
            // Convert to RGB (BGR)
            imageBuffer[i * 3 + 2] = (byte) decoded[0][i];
            imageBuffer[i * 3 + 1] = (byte) decoded[1][i];
            imageBuffer[i * 3] = (byte) decoded[2][i];
        }

        return image;
    }

}
