package com.twelvemonkeys.imageio.plugins.dds;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Enum that lists a certain types of DXGI Format this reader supports to read.
 *
 * @link <a href="https://learn.microsoft.com/en-us/windows/win32/api/dxgiformat/ne-dxgiformat-dxgi_format">DXGI Format List</a>
 */
public enum DX10DXGIFormat {
    BC1(DDSType.DXT1, 70, 72),
    BC2(DDSType.DXT2, 73, 75),
    BC3(DDSType.DXT5, 76, 78),
    //BC7(99),
    R8G8B8A8(DDSType.A8R8G8B8, 27, 32),
    B8G8R8A8(DDSType.A8B8G8R8, 87, 90, 91),
    B8G8R8X8(DDSType.X8B8G8R8, 88, 92, 93);
    private final DDSType ddsType;
    private final IntPredicate dxgiFormat;

    DX10DXGIFormat(DDSType ddsType, int... supportedFormats) {
        this.ddsType = ddsType;
        this.dxgiFormat = value -> Arrays.stream(supportedFormats).anyMatch(supportedFormat -> supportedFormat == value);
    }

    DX10DXGIFormat(DDSType ddsType, int dxgiFormatMin, int dxgiFormatMax) {
        this.dxgiFormat = value -> dxgiFormatMin <= value && value <= dxgiFormatMax;
        this.ddsType = ddsType;
    }

    DDSType getCorrespondingType() {
        return ddsType;
    }

    static DX10DXGIFormat getFormat(int value) {
        for (DX10DXGIFormat format : values()) {
            if (format.dxgiFormat.test(value)) return format;
        }

        throw new UnsupportedOperationException("Unsupported DXGI_FORMAT : " + value);
    }

}
