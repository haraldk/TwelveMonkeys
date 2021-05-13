package com.twelvemonkeys.imageio.plugins.crw;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * CRWProviderInfo
 */
final class CRWProviderInfo extends ReaderWriterProviderInfo {
    CRWProviderInfo() {
        super(
                CRWProviderInfo.class,
                new String[] {"crw", "CRW"},
                new String[] {"crw"},
                new String[] {
                        "image/x-canon-raw",               // TODO: Look up
                },
                "CRWImageReader",
                new String[] {"CRWImageReaderSpi"},
                null,
                null,
                false,
                null, null,
                null, null,
                true,
                null, null,
                null, null
                );
    }
}
