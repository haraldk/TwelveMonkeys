package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest;

/**
 * HDRProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class HDRProviderInfoTest extends ReaderWriterProviderInfoTest {

    @Override
    protected ReaderWriterProviderInfo createProviderInfo() {
        return new HDRProviderInfo();
    }
}