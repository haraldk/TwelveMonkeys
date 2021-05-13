package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

final class XWDProviderInfo extends ReaderWriterProviderInfo {

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.xwd.debug"));

    protected XWDProviderInfo() {
        super(
                XWDProviderInfo.class,
                new String[] {
                        "XWD", "xwd"
                },
                new String[] {"xwd",},
                new String[] {
                        // No official IANA record exists
                        "image/xwd", "image/x-xwd"
                },
                "com.twelvemonkeys.imageio.plugins.xwd.XWDImageReader",
                new String[]{"com.twelvemonkeys.imageio.plugins.xwd.XWDImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
