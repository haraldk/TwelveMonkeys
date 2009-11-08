package com.twelvemonkeys.imageio.plugins.ico;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * ICOImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class ICOImageReaderTestCase extends ImageReaderAbstractTestCase<ICOImageReader> {
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/ico/JavaCup.ico"),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16)
                ),
                new TestData(getClassLoaderResource("/ico/favicon.ico"), new Dimension(32, 32)),
                new TestData(
                        getClassLoaderResource("/ico/joypad.ico"),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48)
                ),
                // Windows Vista icon, PNG encoded for 256x256 sizes
                new TestData(
                        getClassLoaderResource("/ico/down.ico"),
                        new Dimension(16, 16), new Dimension(16, 16), new Dimension(32, 32), new Dimension(32, 32),
                        new Dimension(48, 48), new Dimension(48, 48), new Dimension(256, 256), new Dimension(256, 256),
                        new Dimension(16, 16), new Dimension(32, 32), new Dimension(48, 48), new Dimension(256, 256)
                )
        );
    }

    protected ImageReaderSpi createProvider() {
        return new ICOImageReaderSpi();
    }

    @Override
    protected ICOImageReader createReader() {
        return new ICOImageReader();
    }

    protected Class<ICOImageReader> getReaderClass() {
        return ICOImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("ico");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("ico");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/vnd.microsoft.icon", "image/ico", "image/x-icon");
    }
}