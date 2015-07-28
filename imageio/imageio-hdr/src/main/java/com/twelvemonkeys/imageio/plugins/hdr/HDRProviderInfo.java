package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * HDRProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRProviderInfo.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
final class HDRProviderInfo extends ReaderWriterProviderInfo {
    protected HDRProviderInfo() {
        super(
                HDRProviderInfo.class,
                new String[] {"HDR", "hdr", "RGBE", "rgbe"},
                new String[] {"hdr", "rgbe", "xyze", "pic"},
                new String[] {"image/vnd.radiance"},
                "com.twelvemonkeys.imageio.plugins.hdr.HDRImageReader",
                new String[]{"com.twelvemonkeys.imageio.plugins.hdr.HDRImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
