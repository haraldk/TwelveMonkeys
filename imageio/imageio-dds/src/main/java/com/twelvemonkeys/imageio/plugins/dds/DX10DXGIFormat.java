package com.twelvemonkeys.imageio.plugins.dds;

/**
 * Enum that lists a certain types of DXGI Format this reader supports to read.
 * All int values follow this DXGI_FORMAT_*_UNORM_SRGB format (with '*' being one of the enum element) are supported.
 *
 * @link <a href="https://learn.microsoft.com/en-us/windows/win32/api/dxgiformat/ne-dxgiformat-dxgi_format">DXGI Format List</a>
 */
public enum DX10DXGIFormat {
    BC1(72, DDSType.DXT1),
    BC2(75, DDSType.DXT2),
    BC3(78, DDSType.DXT5),
    //BC7(99),
    R8G8B8A8(29, DDSType.A8R8G8B8),
    B8G8R8A8(91, DDSType.A8B8G8R8),
    B8G8R8X8(93, DDSType.X8B8G8R8);

    private final int dxgiFormatNum;
    private final DDSType ddsType;

    DX10DXGIFormat(int dxgiFormat, DDSType ddsType) {
        this.dxgiFormatNum = dxgiFormat;
        this.ddsType = ddsType;
    }

    DDSType getCorrespondingType() {
        return ddsType;
    }

    static DX10DXGIFormat getFormat(int value) {
        for (DX10DXGIFormat format : values()) {
            if (format.dxgiFormatNum == value) return format;
        }

        throw new UnsupportedOperationException("Unsupported DXGI_FORMAT : " + value);
    }
}
