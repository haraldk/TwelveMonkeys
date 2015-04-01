package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * ICNSProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ICNSProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class ICNSProviderInfo extends ReaderWriterProviderInfo {
    protected ICNSProviderInfo() {
        super(
                ICNSProviderInfo.class,
                new String[]{"icns", "ICNS"},
                new String[]{"icns"},
                new String[]{
                        "image/x-apple-icons",               // Common extension MIME
                },
                "com.twelvemonkeys.imageio.plugins.icns.ICNSImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.ics.ICNImageReaderSpi"},
                null, null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
