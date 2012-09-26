package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ICOImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class PICTImageReaderTest extends ImageReaderAbstractTestCase<PICTImageReader> {

    static ImageReaderSpi sProvider = new PICTImageReaderSpi();

    // TODO: Should also test the clipboard format (without 512 byte header)
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/pict/test.pct"), new Dimension(300, 200)),
                new TestData(getClassLoaderResource("/pict/food.pct"), new Dimension(146, 194)),
                new TestData(getClassLoaderResource("/pict/carte.pict"), new Dimension(782, 598)),
                // Embedded QuickTime image... Should at least include the embedded fallback text
                new TestData(getClassLoaderResource("/pict/u2.pict"), new Dimension(160, 159)),
                // Obsolete V2 format with weird header
                new TestData(getClassLoaderResource("/pict/FLAG_B24.PCT"), new Dimension(124, 124)),
                // 1000 DPI with bounding box not matching DPI
                new TestData(getClassLoaderResource("/pict/oom.pict"), new Dimension(1713, 1263))
        );
    }

    protected ImageReaderSpi createProvider() {
        return sProvider;
    }

    @Override
    protected PICTImageReader createReader() {
        return new PICTImageReader(sProvider);
    }

    protected Class<PICTImageReader> getReaderClass() {
        return PICTImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("pict");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("pct", "pict");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/pict", "image/x-pict");
    }

    // Regression tests

    @Test
    public void testProviderNotMatchJPEG() throws IOException {
        // This JPEG contains PICT magic bytes at locations a PICT would normally have them.
        // We should not claim to be able read it.
        assertFalse(sProvider.canDecodeInput(
                new TestData(getClassLoaderResource("/jpeg/R-7439-1151526181.jpeg"),
                new Dimension(386, 396)
        )));
    }
}