package com.twelvemonkeys.imageio.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
}