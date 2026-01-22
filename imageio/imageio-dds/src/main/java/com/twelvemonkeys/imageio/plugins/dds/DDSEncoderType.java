package com.twelvemonkeys.imageio.plugins.dds;

/**
 * Lists a number of supported encoders for block compressors and uncompressed types.
 * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dxgiformat/ne-dxgiformat-dxgi_format">DXGI Format List</a>
 * <a href="https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression#compression-algorithms">Compression Algorithms</a>
 * <a href="https://github.com/microsoft/DirectXTK12/wiki/DDSTextureLoader#remarks">An extended Non-DX10 FourCC list</a>
 */
public enum DDSEncoderType {
    BC1(DDSType.DXT1.value(), DDS.DXGI_FORMAT_BC1_UNORM, 8),
    BC2(DDSType.DXT2.value(), DDS.DXGI_FORMAT_BC2_UNORM, 16),
    BC3(DDSType.DXT5.value(), DDS.DXGI_FORMAT_BC3_UNORM, 16),
    //DXTn supports BC1-3 so BC4+ are DXT10 exclusive
    BC4(0x31495441, DDS.DXGI_FORMAT_BC4_UNORM, 8),
    BC5(0x32495441, DDS.DXGI_FORMAT_BC5_UNORM, 16),

    RGBA32(29, 32, DDSReader.A8R8G8B8_MASKS),
    BRGA32(91, 32, DDSReader.A8B8G8R8_MASKS),
    BGRX32(DDS.DXGI_FORMAT_B8G8R8X8_UNORM_SRGB, 32, DDSReader.X8B8G8R8_MASKS);

    private final int fourCC;
    private final int dx10DxgiFormat;
    private final int bitCountOrBlockSize;
    private final int[] rgbaMask;

    //fourCC constructor
    DDSEncoderType(int fourCC, int dx10DxgiFormat, int blockSize) {
        this.fourCC = fourCC;
        this.dx10DxgiFormat = dx10DxgiFormat;
        bitCountOrBlockSize = blockSize;
        rgbaMask = null;
    }

    //non-fourCC constructor (e.g. A8R8G8B8)
    DDSEncoderType(int dx10DxgiFormat, int bitCount, int[] masks) {
        fourCC = 0;
        this.dx10DxgiFormat = dx10DxgiFormat;
        bitCountOrBlockSize = bitCount;
        rgbaMask = masks;
    }

    boolean isFourCC() {
        return fourCC != 0;
    }

    int getFourCC() {
        return fourCC;
    }

    boolean isAlphaMaskSupported() {
        return !isFourCC() && rgbaMask[3] > 0;
    }

    boolean isBlockCompression() {
        return this.isFourCC();
    }

    int getBitsOrBlockSize() {
        return bitCountOrBlockSize;
    }

    public int[] getRGBAMask() {
        return rgbaMask;
    }

    public int getDx10Format() {
        return dx10DxgiFormat;
    }
}
