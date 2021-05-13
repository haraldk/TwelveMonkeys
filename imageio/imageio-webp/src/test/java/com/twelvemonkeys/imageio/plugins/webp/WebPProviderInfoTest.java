package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * WebPProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: WebPProviderInfoTest.java,v 1.0 21/11/2020 haraldk Exp$
 */
public class WebPProviderInfoTest extends ReaderWriterProviderInfoTest {
    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new WebPProviderInfo();
    }
}