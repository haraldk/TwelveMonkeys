package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * ICNSProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ICNSProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class ICNSProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new ICNSProviderInfo();
    }
}