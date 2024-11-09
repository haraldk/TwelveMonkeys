package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * PNTGImageReaderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGImageReaderTest.java,v 1.0 23/03/2021 haraldk Exp$
 */
public class PNTGImageReaderTest extends ImageReaderAbstractTest<PNTGImageReader> {

    @Override
    protected ImageReaderSpi createProvider() {
        return new PNTGImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(new TestData(getClassLoaderResource("/mac/porsches.mac"), new Dimension(576, 720)),
                new TestData(getClassLoaderResource("/mac/MARBLES.MAC"), new Dimension(576, 720)));
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("PNTG", "pntg");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("mac", "pntg");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/x-pntg");
    }

    @Override
    public void testProviderCanRead() throws IOException {
        // TODO: This a kind of hack...
        //  Currently, the provider don't claim to read the MARBLES.MAC image,
        //  as it lacks the MacBinary header and thus no way to identify format.
        //  We can still read it, so we'll include it in the other tests.
        List<TestData> testData = getTestData().subList(0, 1);

        for (TestData data : testData) {
            ImageInputStream stream = data.getInputStream();
            assertNotNull(stream);
            assertTrue(provider.canDecodeInput(stream), "Provider is expected to be able to decode data: " + data);
        }
    }
}