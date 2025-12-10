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
        //only support D3D10_RESOURCE_DIMENSION_TEXTURE2D = 3
        if (this.resourceDimension != 3) throw new UnsupportedOperationException("Resource dimension " + resourceDimension + " is not supported");
        this.miscFlag = miscFlag;
        this.arraySize = arraySize;
        this.miscFlags2 = miscFlags2;
    }

    static DX10Header read(ImageInputStream inputStream) throws IOException {
        final int dxgiFormat = inputStream.readInt(), resourceDimension = inputStream.readInt(), miscFlag = inputStream.readInt(), arraySize = inputStream.readInt(), miscFlags2 = inputStream.readInt();
        return new DX10Header(dxgiFormat, resourceDimension, miscFlag, arraySize, miscFlags2);
    }

    DDSType getDDSType() {
        return dxgiFormat.getCorrespondingType();
    }
}
