package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * TIFFProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class TIFFProviderInfo extends ReaderWriterProviderInfo {
    protected TIFFProviderInfo() {
        super(
                TIFFProviderInfo.class,
                new String[] {"tiff", "TIFF"},
                new String[] {"tif", "tiff"},
                new String[] {
                        "image/tiff", "image/x-tiff"
                },
                "com.twelvemkonkeys.imageio.plugins.tiff.TIFFImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi"},
                "com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter",
                new String[] {"com.twelvemkonkeys.imageio.plugins.tif.TIFFImageWriterSpi"},
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
