package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

public class XWDProviderInfoTest extends ReaderWriterProviderInfoTest {
    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new XWDProviderInfo();
    }
}
