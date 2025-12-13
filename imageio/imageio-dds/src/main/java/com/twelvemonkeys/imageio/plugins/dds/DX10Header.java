package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

//https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header-dxt10
public final class DX10Header {
    final DX10DXGIFormat dxgiFormat;
    final int resourceDimension, miscFlag, arraySize, miscFlags2;

    private DX10Header(int dxgiFormat, int resourceDimension, int miscFlag, int arraySize, int miscFlags2) {
        this.dxgiFormat = DX10DXGIFormat.getFormat(dxgiFormat);
        this.resourceDimension = resourceDimension;
        if (this.resourceDimension != DDS.D3D10_RESOURCE_DIMENSION_TEXTURE2D)
            throw new IllegalArgumentException("Resource dimension " + resourceDimension + " is not supported, expected 3.");
        this.miscFlag = miscFlag;
        this.arraySize = arraySize;
        this.miscFlags2 = miscFlags2;
    }

    static DX10Header read(ImageInputStream inputStream) throws IOException {
        int dxgiFormat = inputStream.readInt();
        int resourceDimension = inputStream.readInt();
        int miscFlag = inputStream.readInt();
        int arraySize = inputStream.readInt();
        int miscFlags2 = inputStream.readInt();
        return new DX10Header(dxgiFormat, resourceDimension, miscFlag, arraySize, miscFlags2);
    }

    DDSType getDDSType() {
        return dxgiFormat.getCorrespondingType();
    }
}
