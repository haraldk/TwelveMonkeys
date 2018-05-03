package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * JPEGProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: JPEGProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class JPEGProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new JPEGProviderInfo();
    }
}