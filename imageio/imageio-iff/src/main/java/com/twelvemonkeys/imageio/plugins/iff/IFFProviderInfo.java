package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * IFFProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: IFFProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class IFFProviderInfo extends ReaderWriterProviderInfo {
    protected IFFProviderInfo() {
        super(
                IFFProviderInfo.class,
                new String[] {"iff", "IFF"},
                new String[] {"iff", "lbm", "ham", "ham8", "ilbm"},
                new String[] {"image/iff", "image/x-iff"},
                "com.twelvemonkeys.imageio.plugins.iff.IFFImageReader",
                new String[]{"com.twelvemonkeys.imageio.plugins.iff.IFFImageReaderSpi"},
                "com.twelvemonkeys.imageio.plugins.iff.IFFImageWriter",
                new String[] {"com.twelvemonkeys.imageio.plugins.iff.IFFImageWriterSpi"},
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
