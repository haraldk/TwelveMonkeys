package com.twelvemonkeys.imageio.plugins.dds;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class DDSImageWriteParamTest {
    @Test
    void compressionTypes() {
        DDSImageWriteParam param = new DDSImageWriteParam();

        String[] compressionTypes = param.getCompressionTypes();
        DDSType[] values = Arrays.stream(DDSType.values())
            .filter(DDSType::isBlockCompression)
            .toArray(DDSType[]::new);

        assertEquals(values.length + 1, compressionTypes.length);

        for (int i = 0; i < values.length; i++) {
            DDSType type = values[i];
            assertEquals(type.name(), compressionTypes[i + 1]);
        }

        assertEquals("None", compressionTypes[0]);
    }

    @Test
    void defaultParam() {
        DDSImageWriteParam param = new DDSImageWriteParam();
//        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Meh...

        assertEquals(DDSImageWriteParam.DEFAULT_TYPE, param.type());
//        assertEquals(DDSImageWriterParam.DEFAULT_TYPE.name(), param.getCompressionType());
    }
}