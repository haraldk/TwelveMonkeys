package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * CURProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: CURProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class CURProviderInfo extends ReaderWriterProviderInfo {
    protected CURProviderInfo() {
        super(
                CURProviderInfo.class,
                new String[]{"cur", "CUR"},
                new String[]{"cur"},
                new String[]{
                        "image/vnd.microsoft.cursor",   // Official IANA MIME
                        "image/x-cursor",               // Common extension MIME
                        "image/cursor"                  // Unofficial, but common
                },
                "com.twelvemonkeys.imageio.plugins.bmp.CURImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.bmp.CURImageReaderSpi"},
                null, null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
