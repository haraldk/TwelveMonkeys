package com.twelvemonkeys.imageio.reference;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import com.twelvemonkeys.lang.SystemUtil;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assume.assumeNoException;

/**
 * JPEGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTest.java,v 1.0 Oct 9, 2009 3:37:25 PM haraldk Exp$
 */
public class JPEGImageReaderTest extends ImageReaderAbstractTest {
    private static final boolean IS_JAVA_6_OR_LATER = SystemUtil.isClassAvailable("java.util.Deque");
    
    protected final ImageReaderSpi provider = lookupSpi();

    private ImageReaderSpi lookupSpi() {
        try {
            return (ImageReaderSpi) IIORegistry.getDefaultInstance().getServiceProviderByClass(Class.forName("com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi"));
        }
        catch (ClassNotFoundException e) {
            assumeNoException(e);
        }

        return null;
    }

    @Override
    protected List<TestData> getTestData() {
        return Collections.singletonList(
                new TestData(getClassLoaderResource("/jpeg/R-7439-1151526181.jpeg"), new Dimension(386, 396))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected Class getReaderClass() {
        try {
            return Class.forName("com.sun.imageio.plugins.jpeg.JPEGImageReader");
        }
        catch (ClassNotFoundException e) {
            assumeNoException(e);
        }

        return null;
    }

    @Override
    protected ImageReader createReader() {
        try {
            return provider.createReaderInstance();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // These are NOT correct implementations, but I don't really care here
    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList(provider.getFormatNames());
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList(provider.getFileSuffixes());
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(provider.getMIMETypes());
    }

    @Test
    @Override
    public void testSetDestination() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6_OR_LATER) {
            super.testSetDestination();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

    @Test
    @Override
    public void testSetDestinationType() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6_OR_LATER) {
            super.testSetDestinationType();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

    @Test
    @Ignore("Known issue")
    @Override
    public void testReadAsRenderedImageIndexOutOfBounds() throws IIOException {
        super.testReadAsRenderedImageIndexOutOfBounds();
    }

    @Test
    @Ignore("No test data with JFIF thumbnail")
    @Override
    public void testNotBadCachingThumbnails() throws IOException {
        super.testNotBadCachingThumbnails();
    }
}
