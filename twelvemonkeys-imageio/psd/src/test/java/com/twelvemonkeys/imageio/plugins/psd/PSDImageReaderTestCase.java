package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * PSDImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class PSDImageReaderTestCase extends ImageReaderAbstractTestCase<PSDImageReader> {

    static ImageReaderSpi sProvider = new PSDImageReaderSpi();

    protected List<TestData> getTestData() {
        return Arrays.asList(
                // 5 channel, RGB
                new TestData(getClassLoaderResource("/psd/photoshopping.psd"), new Dimension(300, 225)),
                // 1 channel, gray, 8 bit samples
                new TestData(getClassLoaderResource("/psd/buttons.psd"), new Dimension(20, 20)),
                // 5 channel, CMYK
                new TestData(getClassLoaderResource("/psd/escenic-liquid-logo.psd"), new Dimension(595, 420)),
                // 3 channel RGB, no composite layer
                new TestData(getClassLoaderResource("/psd/jugware-icon.psd"), new Dimension(128, 128)),
                // 3 channel RGB, old data, no layer info/mask 
                new TestData(getClassLoaderResource("/psd/MARBLES.PSD"), new Dimension(1419, 1001)),
                // 1 channel, indexed color
                new TestData(getClassLoaderResource("/psd/coral_fish.psd"), new Dimension(800, 800))
                // 1 channel, bitmap, 1 bit samples
//                new TestData(getClassLoaderResource("/psd/test_bitmap.psd"), new Dimension(800, 600))
                // 1 channel, gray, 16 bit samples
//                new TestData(getClassLoaderResource("/psd/test_gray16.psd"), new Dimension(800, 600))
                // TODO: Need uncompressed PSD
                // TODO: Need more recent ZIP compressed PSD files from CS2/CS3+
        );
    }

    protected ImageReaderSpi createProvider() {
        return sProvider;
    }

    @Override
    protected PSDImageReader createReader() {
        return new PSDImageReader(sProvider);
    }

    protected Class<PSDImageReader> getReaderClass() {
        return PSDImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("psd");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("psd");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/x-psd");
    }
}