package com.twelvemonkeys.imageio.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageReadParam;

/**
 * IIOUtilTest
 */
public class IIOUtilTest {

    @Test
    public void subsampleRowPeriod2Byte() {
        int period = 2;

        byte[] input = {-1, 0, (byte) 0xAA, 0, -1};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {-1, (byte) 0xAA, -1};

        IIOUtil.subsampleRow(input, 0, input.length, output, 0, 1, 8, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2ByteStride3() {
        int period = 2;

        byte[] input = {-1, -1, -1, 0, 0, 0, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, 0, 0, 0, -1, -1, -1};
        byte[] output = new byte[9];
        byte[] expected = {-1, -1, -1, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, -1, -1, -1};

        IIOUtil.subsampleRow(input, 0, input.length / 3, output, 0, 3, 8, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2Byte1() {
        int period = 2;

        byte[] input = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 0, input.length * 8, output, 0, 1, 1, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod3_1Bit() {
        int period = 3;

        byte[] input = {(byte) 0x92, (byte) 0x49, (byte) 0x24};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff};

        IIOUtil.subsampleRow(input, 0, input.length * 8, output, 0, 1, 1, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_2Bit() {
        int period = 2;

        byte[] input = {(byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 0, input.length * 4, output, 0, 1, 2, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_4Bit() {
        int period = 2;

        byte[] input = {(byte) 0xf0, (byte) 0xf0, (byte) 0xf0};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 0, input.length * 2, output, 0, 1, 4, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_1Bit2Samples() {
        int period = 2;

        byte[] input = {(byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 0, input.length * 4, output, 0, 2, 1, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_2Bit2Samples() {
        int period = 2;

        byte[] input = {(byte) 0xf0, (byte) 0xf0, (byte) 0xf0};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 0, input.length * 2, output, 0, 2, 2, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_4Bit2Samples() {
        int period = 2;

        byte[] input = {-1, 0, (byte) 0xAA, 0, -1};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {-1, (byte) 0xAA, -1};

        IIOUtil.subsampleRow(input, 0, input.length, output, 0, 2, 4, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod2_1BitOffset1() {
        int period = 2;

        byte[] input = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa};
        byte[] output = new byte[divCeil(input.length, period)];
        byte[] expected = {(byte) 0xff, (byte) 0xf0};

        IIOUtil.subsampleRow(input, 1, input.length * 8, output, 0, 1, 1, period);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod1ByteSameArray() {
        byte[] inputOutput = {-1, 0, (byte) 0xAA, 0, -1};
        byte[] expected = {-1, 0, (byte) 0xAA, 0, -1};

        IIOUtil.subsampleRow(inputOutput, 0, inputOutput.length, inputOutput, 0, 1, 8, 1);

        assertArrayEquals(expected, inputOutput);
    }

    @Test
    public void subsampleRowPeriod1ByteDifferentArray() {
        byte[] input = {-1, 0, (byte) 0xAA, 0, -1};
        byte[] output = new byte[input.length];
        byte[] expected = {-1, 0, (byte) 0xAA, 0, -1};

        IIOUtil.subsampleRow(input, 0, input.length, output, 0, 1, 8, 1);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod1ShortSameArray() {
        short[] inputOutput = {-1, 0, (short) 0xAA77, 0, -1};
        short[] expected = {-1, 0, (short) 0xAA77, 0, -1};

        IIOUtil.subsampleRow(inputOutput, 0, inputOutput.length, inputOutput, 0, 4, 4, 1);

        assertArrayEquals(expected, inputOutput);
    }

    @Test
    public void subsampleRowPeriod1ShortDifferentArray() {
        short[] input = {-1, 0, (short) 0xAA77, 0, -1};
        short[] output = new short[input.length];
        short[] expected = {-1, 0, (short) 0xAA77, 0, -1};

        IIOUtil.subsampleRow(input, 0, input.length, output, 0, 1, 16, 1);

        assertArrayEquals(expected, output);
    }

    @Test
    public void subsampleRowPeriod1IntSameArray() {
        int[] inputOutput = {-1, 0, 0xAA997755, 0, -1};
        int[] expected = {-1, 0, 0xAA997755, 0, -1};

        IIOUtil.subsampleRow(inputOutput, 0, inputOutput.length, inputOutput, 0, 1, 32, 1);

        assertArrayEquals(expected, inputOutput);
    }

    @Test
    public void subsampleRowPeriod1IntDifferentArray() {
        int[] input = {-1, 0, 0xAA997755, 0, -1};
        int[] output = new int[input.length];
        int[] expected = {-1, 0, 0xAA997755, 0, -1};

        IIOUtil.subsampleRow(input, 0, input.length, output, 0, 4, 8, 1);

        assertArrayEquals(expected, output);
    }

    private int divCeil(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    @Test
    void copyStandardParamsDestinationNull() {
        ImageReadParam param = new ImageReadParam();

        assertThrows(NullPointerException.class, () -> IIOUtil.copyStandardParams(null, null));
        assertThrows(NullPointerException.class, () -> IIOUtil.copyStandardParams(param, null));
    }

    @Test
    void copyStandardParamsSame() {
        ImageReadParam param = new ImageReadParam();
        assertThrows(IllegalArgumentException.class, () -> IIOUtil.copyStandardParams(param, param));
    }

    @Test
    void copyStandardParamsSourceNull() {
        ImageReadParam param = new ImageReadParam() {
            @Override
            public void setSourceRegion(Rectangle sourceRegion) {
                fail("Should not be invoked");
            }
        };

        assertSame(param, IIOUtil.copyStandardParams(null, param));
    }

    @Test
    void copyStandardParamsImageReadParam() {
        int sourceXSubsampling = 3;
        int sourceYSubsampling = 4;
        int subsamplingXOffset = 1;
        int subsamplingYOffset = 2;
        Rectangle sourceRegion = new Rectangle(1, 2, 42, 43);
        int[] sourceBands = { 0, 1, 2 };

        Point destinationOffset = new Point(7, 9);
        int[] destinationBands = { 2, 1, 0 };

        ImageReadParam sourceParam = new ImageReadParam();
        sourceParam.setSourceRegion(sourceRegion);
        sourceParam.setSourceSubsampling(sourceXSubsampling, sourceYSubsampling, subsamplingXOffset, subsamplingYOffset);
        sourceParam.setSourceBands(sourceBands);

        sourceParam.setDestinationOffset(destinationOffset);
        sourceParam.setDestinationBands(destinationBands);

        JPEGImageReadParam jpegParam = IIOUtil.copyStandardParams(sourceParam, new JPEGImageReadParam());

        assertEquals(sourceRegion, jpegParam.getSourceRegion());
        assertEquals(sourceXSubsampling, jpegParam.getSourceXSubsampling());
        assertEquals(sourceYSubsampling, jpegParam.getSourceYSubsampling());
        assertEquals(subsamplingXOffset, jpegParam.getSubsamplingXOffset());
        assertEquals(subsamplingYOffset, jpegParam.getSubsamplingYOffset());
        assertArrayEquals(sourceBands, jpegParam.getSourceBands());

        assertEquals(destinationOffset, jpegParam.getDestinationOffset());
        assertArrayEquals(destinationBands, jpegParam.getDestinationBands());
    }

    @Test
    void copyStandardParamsImageReadParamDestination() {
        // Destination and destination type is mutually exclusive
        BufferedImage destination = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);

        ImageReadParam sourceParam = new ImageReadParam();
        sourceParam.setDestination(destination);

        assertEquals(destination, IIOUtil.copyStandardParams(sourceParam, new JPEGImageReadParam()).getDestination());
    }

    @Test
    void copyStandardParamsImageReadParamDestinationType() {
        // Destination and destination type is mutually exclusive
        ImageTypeSpecifier destinationType = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);

        ImageReadParam sourceParam = new ImageReadParam();
        sourceParam.setDestinationType(destinationType);

        assertEquals(destinationType, IIOUtil.copyStandardParams(sourceParam, new JPEGImageReadParam()).getDestinationType());
    }

    @Test
    void copyStandardParamsReadToWrite() {
        int sourceXSubsampling = 3;
        int sourceYSubsampling = 4;
        int subsamplingXOffset = 1;
        int subsamplingYOffset = 2;
        Rectangle sourceRegion = new Rectangle(1, 2, 42, 43);
        int[] sourceBands = { 0, 1, 2 };

        Point destinationOffset = new Point(7, 9);

        ImageWriteParam sourceParam = new ImageWriteParam(null);
        sourceParam.setSourceRegion(sourceRegion);
        sourceParam.setSourceSubsampling(sourceXSubsampling, sourceYSubsampling, subsamplingXOffset, subsamplingYOffset);
        sourceParam.setSourceBands(sourceBands);

        sourceParam.setDestinationOffset(destinationOffset);

        JPEGImageReadParam jpegParam = IIOUtil.copyStandardParams(sourceParam, new JPEGImageReadParam());

        assertEquals(sourceRegion, jpegParam.getSourceRegion());
        assertEquals(sourceXSubsampling, jpegParam.getSourceXSubsampling());
        assertEquals(sourceYSubsampling, jpegParam.getSourceYSubsampling());
        assertEquals(subsamplingXOffset, jpegParam.getSubsamplingXOffset());
        assertEquals(subsamplingYOffset, jpegParam.getSubsamplingYOffset());
        assertArrayEquals(sourceBands, jpegParam.getSourceBands());

        assertEquals(destinationOffset, jpegParam.getDestinationOffset());
        assertNull(jpegParam.getDestinationBands()); // Only in read param
    }

    @Test
    void copyStandardParamsImageWriteParam() {
        int sourceXSubsampling = 3;
        int sourceYSubsampling = 4;
        int subsamplingXOffset = 1;
        int subsamplingYOffset = 2;
        Rectangle sourceRegion = new Rectangle(1, 2, 42, 43);
        int[] sourceBands = { 0, 1, 2 };

        Point destinationOffset = new Point(7, 9);

        ImageWriteParam sourceParam = new ImageWriteParam(null);
        sourceParam.setSourceRegion(sourceRegion);
        sourceParam.setSourceSubsampling(sourceXSubsampling, sourceYSubsampling, subsamplingXOffset, subsamplingYOffset);
        sourceParam.setSourceBands(sourceBands);

        sourceParam.setDestinationOffset(destinationOffset);

        BMPImageWriteParam fooParam = IIOUtil.copyStandardParams(sourceParam, new BMPImageWriteParam());

        assertEquals(sourceRegion, fooParam.getSourceRegion());
        assertEquals(sourceXSubsampling, fooParam.getSourceXSubsampling());
        assertEquals(sourceYSubsampling, fooParam.getSourceYSubsampling());
        assertEquals(subsamplingXOffset, fooParam.getSubsamplingXOffset());
        assertEquals(subsamplingYOffset, fooParam.getSubsamplingYOffset());
        assertArrayEquals(sourceBands, fooParam.getSourceBands());

        assertEquals(destinationOffset, fooParam.getDestinationOffset());
    }

    @Test
    void copyStandardParamsImageWriteParamEverything() {
        int sourceXSubsampling = 3;
        int sourceYSubsampling = 4;
        int subsamplingXOffset = 1;
        int subsamplingYOffset = 2;
        Rectangle sourceRegion = new Rectangle(1, 2, 42, 43);
        int[] sourceBands = { 0, 1, 2 };

        Point destinationOffset = new Point(7, 9);

        String compressionType = "Foo";
        float quality = 0.42f;

        ImageWriteParam sourceParam = new ImageWriteParam() {
            {
                canWriteProgressive = true;

                canWriteTiles = true;
                canOffsetTiles = true;

                canWriteCompressed = true;
                compressionTypes = new String[] { "Foo", "Bar" };
            }
        };
        sourceParam.setSourceRegion(sourceRegion);
        sourceParam.setSourceSubsampling(sourceXSubsampling, sourceYSubsampling, subsamplingXOffset, subsamplingYOffset);
        sourceParam.setSourceBands(sourceBands);

        sourceParam.setDestinationOffset(destinationOffset);

        sourceParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT); // Default is COPY_FROM_METADATA...
        sourceParam.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
        sourceParam.setTiling(1, 2, 3, 4);
        sourceParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        sourceParam.setCompressionType(compressionType);
        sourceParam.setCompressionQuality(quality);

        FooImageWriteParam fooParam = IIOUtil.copyStandardParams(sourceParam, new FooImageWriteParam());

        assertEquals(sourceRegion, fooParam.getSourceRegion());
        assertEquals(sourceXSubsampling, fooParam.getSourceXSubsampling());
        assertEquals(sourceYSubsampling, fooParam.getSourceYSubsampling());
        assertEquals(subsamplingXOffset, fooParam.getSubsamplingXOffset());
        assertEquals(subsamplingYOffset, fooParam.getSubsamplingYOffset());
        assertArrayEquals(sourceBands, fooParam.getSourceBands());

        assertEquals(destinationOffset, fooParam.getDestinationOffset());

        assertEquals(ImageWriteParam.MODE_DEFAULT, fooParam.getProgressiveMode());

        assertEquals(ImageWriteParam.MODE_EXPLICIT, fooParam.getTilingMode());
        assertEquals(1, fooParam.getTileWidth());
        assertEquals(2, fooParam.getTileHeight());
        assertEquals(3, fooParam.getTileGridXOffset());
        assertEquals(4, fooParam.getTileGridYOffset());

        assertEquals(ImageWriteParam.MODE_EXPLICIT, fooParam.getCompressionMode());
        assertEquals(compressionType, fooParam.getCompressionType());
        assertEquals(quality, fooParam.getCompressionQuality());
    }

    // A basic param that supports "everything"
    static class FooImageWriteParam extends ImageWriteParam {
        FooImageWriteParam() {
            canWriteProgressive = true;

            canWriteTiles = true;
            canOffsetTiles = true;

            canWriteCompressed = true;
            compressionType = "Unset";
            compressionTypes = new String[] { "Bar", "Foo" };
        }
    }
}