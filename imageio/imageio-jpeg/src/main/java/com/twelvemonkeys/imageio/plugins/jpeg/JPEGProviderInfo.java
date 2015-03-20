package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * JPEGProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: JPEGProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class JPEGProviderInfo extends ReaderWriterProviderInfo {
    protected JPEGProviderInfo() {
        super(
                JPEGProviderInfo.class,
                new String[] {"JPEG", "jpeg", "JPG", "jpg"},
                new String[] {"jpg", "jpeg"},
                new String[] {"image/jpeg"},
                "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi"},
                "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter",
                new String[] {"com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriterSpi"},
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
