package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * SGIProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SGIProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class SGIProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new SGIProviderInfo();
    }
}