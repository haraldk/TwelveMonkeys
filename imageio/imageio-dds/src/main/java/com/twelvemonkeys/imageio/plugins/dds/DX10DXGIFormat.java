package com.twelvemonkeys.imageio.plugins.dds;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Enum that lists a certain types of DXGI Format this reader supports to read.
 *
 * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dxgiformat/ne-dxgiformat-dxgi_format">DXGI Format List</a>
 */
public enum DX10DXGIFormat {
    BC1(DDSType.DXT1, rangeInclusive(70, 72)),
    BC2(DDSType.DXT2, rangeInclusive(73, 75)),
    BC3(DDSType.DXT5, rangeInclusive(76, 78)),
    //BC7(99),
    B8G8R8A8(DDSType.A8B8G8R8, exactly(87, 90, 91)),
    B8G8R8X8(DDSType.X8B8G8R8, exactly(88, 92, 93)),
    R8G8B8A8(DDSType.A8R8G8B8, rangeInclusive(27, 32));
    private final DDSType ddsType;
    private final IntPredicate dxgiFormat;

    DX10DXGIFormat(DDSType ddsType, IntPredicate dxgiFormat) {
        this.ddsType = ddsType;
        this.dxgiFormat = dxgiFormat;
    }

    DDSType getCorrespondingType() {
        return ddsType;
    }

    static DX10DXGIFormat getFormat(int value) {
        for (DX10DXGIFormat format : values()) {
            if (format.dxgiFormat.test(value)) return format;
        }

        throw new IllegalArgumentException("Unsupported DXGI_FORMAT : " + value);
    }


    /**
     * @param acceptedValues values in DXGI Formats List, passed values are expected to be in ascending order
     */
    private static IntPredicate exactly(int... acceptedValues) {
        return test -> Arrays.binarySearch(acceptedValues, test) >= 0;
    }

    private static IntPredicate rangeInclusive(int from, int to) {
        return test -> from <= test && test <= to;
    }
}
