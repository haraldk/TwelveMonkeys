package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BigTIFFImageReaderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BigTIFFImageReaderTest.java,v 1.0 26/04/2017 harald.kuhr Exp$
 */
public class BigTIFFImageReaderTest extends ImageReaderAbstractTest<TIFFImageReader> {
    private static final BigTIFFImageReaderSpi SPI = new BigTIFFImageReaderSpi();

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/bigtiff/BigTIFF.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFMotorola.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong8.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFMotorolaLongStrips.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong8Tiles.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFSubIFD4.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFSubIFD8.tif"), new Dimension(64, 64))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return SPI;
    }

    @Override
    protected Class<TIFFImageReader> getReaderClass() {
        return TIFFImageReader.class;
    }

    @Override
    protected TIFFImageReader createReader() {
        return SPI.createReaderInstance(null);
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("bigtiff", "BigTIFF", "BIGTIFF");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("tif", "tiff", "btf", "tf8");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/tiff");
    }

    // TODO: Test that all BigTIFFs are decoded equal to the classic TIFF

    // TODO: Test metadata
}
