package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * CURProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: CURProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class ICOProviderInfo extends ReaderWriterProviderInfo {
    protected ICOProviderInfo() {
        super(
                ICOProviderInfo.class,
                new String[]{"ico", "ICO"},
                new String[]{"ico"},
                new String[]{
                        "image/vnd.microsoft.icon", // Official IANA MIME
                        "image/x-icon",             // Common extension MIME
                        "image/ico"                 // Unofficial, but common
                },
                "com.twelvemonkeys.imageio.plugins.bmp.ICOImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.bmp.ICOImageReaderSpi"},
                null, null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
