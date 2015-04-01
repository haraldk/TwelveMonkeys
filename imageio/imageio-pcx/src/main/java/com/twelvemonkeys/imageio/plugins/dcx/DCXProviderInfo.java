package com.twelvemonkeys.imageio.plugins.dcx;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * DCXProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: DCXProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class DCXProviderInfo extends ReaderWriterProviderInfo {
    protected DCXProviderInfo() {
        super(
                DCXProviderInfo.class,
                new String[]{
                        "dcx",
                        "DCX"
                },
                new String[]{"dcx"},
                new String[]{
                        // No official IANA record exists
                        "image/dcx",
                        "image/x-dcx",
                },
                "com.twelvemkonkeys.imageio.plugins.dcx.DCXImageReader",
                new String[] {"com.twelvemkonkeys.imageio.plugins.dcx.DCXImageReaderSpi"},
                null, null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
