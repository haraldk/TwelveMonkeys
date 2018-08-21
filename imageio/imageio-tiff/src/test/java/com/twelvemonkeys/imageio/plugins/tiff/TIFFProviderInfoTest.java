package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * TIFFProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class TIFFProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new TIFFProviderInfo();
    }
}