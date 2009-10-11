package com.twelvemonkeys.imageio.reference;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import com.twelvemonkeys.lang.SystemUtil;

import javax.imageio.spi.ImageReaderSpi;
import java.util.Arrays;
import java.util.List;
import java.awt.*;
import java.io.IOException;

/**
 * JPEGImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTestCase.java,v 1.0 Oct 9, 2009 3:37:25 PM haraldk Exp$
 */
public class JPEGImageReaderTestCase extends ImageReaderAbstractTestCase<JPEGImageReader> {
    private static final boolean IS_JAVA_6 = SystemUtil.isClassAvailable("java.util.Deque");
    
    protected JPEGImageReaderSpi mProvider = new JPEGImageReaderSpi();

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/R-7439-1151526181.jpeg"), new Dimension(386, 396))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return mProvider;
    }

    @Override
    protected Class<JPEGImageReader> getReaderClass() {
        return JPEGImageReader.class;
    }

    @Override
    protected JPEGImageReader createReader() {
        try {
            return (JPEGImageReader) mProvider.createReaderInstance();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // These are NOT correct implementations, but I don't really care here
    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList(mProvider.getFormatNames());
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList(mProvider.getFileSuffixes());
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(mProvider.getMIMETypes());
    }

    @Override
    public void testSetDestination() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6) {
            super.testSetDestination();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

    @Override
    public void testSetDestinationType() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6) {
            super.testSetDestinationType();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

}
