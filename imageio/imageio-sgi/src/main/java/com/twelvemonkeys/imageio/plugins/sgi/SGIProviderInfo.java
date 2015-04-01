package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * SGIProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SGIProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class SGIProviderInfo extends ReaderWriterProviderInfo {
    protected SGIProviderInfo() {
        super(
                SGIProviderInfo.class,
                new String[] {
                        "sgi",
                        "SGI"
                },
                new String[] {"sgi"},
                new String[] {
                        // No official IANA record exists
                        "image/sgi",
                        "image/x-sgi",
                },
                "com.twelvemkonkeys.imageio.plugins.sgi.SGIImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.sgi.SGIImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
