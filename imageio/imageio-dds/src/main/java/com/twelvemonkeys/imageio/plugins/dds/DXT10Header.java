package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header-dxt10">DDS_HEADER_DXT10 structure</a>
 */
final class DXT10Header {
    final int dxgiFormat;
    final int resourceDimension;
    final int miscFlag;
    final int arraySize;
    final int miscFlags2;

    private final DDSType type;

    private DXT10Header(int dxgiFormat, int resourceDimension, int miscFlag, int arraySize, int miscFlags2) {
        type = DDSType.fromDXGIFormat(dxgiFormat); // Validates dxgiFormat
        if (resourceDimension != DDS.D3D10_RESOURCE_DIMENSION_TEXTURE2D) {
            throw new IllegalArgumentException(String.format("Resource dimension %d is not supported, expected: %d",
                resourceDimension, DDS.D3D10_RESOURCE_DIMENSION_TEXTURE2D));
        }

        this.dxgiFormat = dxgiFormat;
        this.resourceDimension = resourceDimension;
        this.miscFlag = miscFlag;
        this.arraySize = arraySize;
        this.miscFlags2 = miscFlags2;
    }

    static DXT10Header read(ImageInputStream inputStream) throws IOException {
        int dxgiFormat = inputStream.readInt();
        int resourceDimension = inputStream.readInt();
        int miscFlag = inputStream.readInt();
        int arraySize = inputStream.readInt();
        int miscFlags2 = inputStream.readInt();

        return new DXT10Header(dxgiFormat, resourceDimension, miscFlag, arraySize, miscFlags2);
    }

    DDSType getType() {
        return type;
    }
}
