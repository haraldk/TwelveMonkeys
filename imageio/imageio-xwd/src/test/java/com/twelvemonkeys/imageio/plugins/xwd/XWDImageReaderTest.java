package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class XWDImageReaderTest extends ImageReaderAbstractTest<XWDImageReader> {

    private final XWDImageReaderSpi provider = new XWDImageReaderSpi();

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/xwd/input.xwd"), new Dimension(70, 46)),
                new TestData(getClassLoaderResource("/xwd/brain.xwd"), new Dimension(520, 510)),
                new TestData(getClassLoaderResource("/xwd/sample_640x426.xwd"), new Dimension(640, 426))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected Class<XWDImageReader> getReaderClass() {
        return XWDImageReader.class;
    }

    @Override
    protected XWDImageReader createReader() {
        return provider.createReaderInstance(null);
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("xwd", "XWD");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("xwd");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/xwd", "image/x-xwd");
    }
}