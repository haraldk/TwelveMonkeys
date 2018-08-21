package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * BMPProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BMPProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class BMPProviderInfo extends ReaderWriterProviderInfo {
    protected BMPProviderInfo() {
        super(
                BMPProviderInfo.class,
                new String[] {"bmp", "BMP"},
                new String[] {"bmp", "rle"},
                new String[] {
                        "image/bmp",
                        "image/x-bmp"
//                        "image/vnd.microsoft.bitmap",   // TODO: Official IANA MIME
                },
                "com.twelvemonkeys.imageio.plugins.bmp.BMPImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.bmp.BMPImageReaderSpi"},
                "com.sun.imageio.plugins.bmp.BMPImageWriter",
                new String[]{"com.sun.imageio.plugins.bmp.BMPImageWriterSpi"}, // We support the same native metadata format
                false, null, null, null, null,
                true, BMPMetadata.nativeMetadataFormatName, "com.sun.imageio.plugins.bmp.BMPMetadataFormat", null, null
        );
    }
}
