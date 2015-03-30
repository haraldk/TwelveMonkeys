package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * SGIProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SGIProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class TGAProviderInfo extends ReaderWriterProviderInfo {
    protected TGAProviderInfo() {
        super(
                TGAProviderInfo.class,
                new String[]{
                        "tga", "TGA",
                        "targa", "TARGA"
                },
                new String[]{"tga", "tpic"},
                new String[]{
                        // No official IANA record exists
                        "image/tga", "image/x-tga",
                        "image/targa", "image/x-targa",
                },
                "com.twelvemkonkeys.imageio.plugins.tga.TGAImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.tga.TGAImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
