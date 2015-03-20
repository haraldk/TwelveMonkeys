package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * PICTProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PICTProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class PICTProviderInfo extends ReaderWriterProviderInfo {
    protected PICTProviderInfo() {
        super(
                PICTProviderInfo.class,
                new String[] {"pct", "PCT", "pict", "PICT"},
                new String[] {"pct", "pict"},
                new String[] {"image/pict", "image/x-pict"},
                "com.twelvemkonkeys.imageio.plugins.pict.PICTImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.pict.PICTImageReaderSpi"},
                "com.twelvemonkeys.imageio.plugins.pict.PICTImageWriter",
                new String[] {"com.twelvemkonkeys.imageio.plugins.pict.PICTImageWriterSpi"},
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
