package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * PCXProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PCXProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class PCXProviderInfo extends ReaderWriterProviderInfo {
    protected PCXProviderInfo() {
        super(
                PCXProviderInfo.class,
                new String[]{
                        "pcx",
                        "PCX"
                },
                new String[]{"pcx"},
                new String[]{
                        // No official IANA record exists
                        "image/pcx",
                        "image/x-pcx",
                },
                "com.twelvemkonkeys.imageio.plugins.pcx.PCXImageReader",
                new String[] {"com.twelvemkonkeys.imageio.plugins.pcx.PCXImageReaderSpi"},
                null, null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
