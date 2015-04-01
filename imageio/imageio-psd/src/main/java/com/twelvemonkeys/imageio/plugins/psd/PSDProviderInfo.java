package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * PSDProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PSDProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class PSDProviderInfo extends ReaderWriterProviderInfo {
    protected PSDProviderInfo() {
        super(
                PSDProviderInfo.class,
                new String[] {"psd", "PSD"},
                new String[] {"psd"},
                new String[] {
                        "image/vnd.adobe.photoshop",        // Official, IANA registered
                        "application/vnd.adobe.photoshop",  // Used in XMP
                        "image/x-psd",
                        "application/x-photoshop",
                        "image/x-photoshop"
                },
                "com.twelvemkonkeys.imageio.plugins.psd.PSDImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.psd.PSDImageReaderSpi"},
                null,
                null, // new String[] {"com.twelvemkonkeys.imageio.plugins.psd.PSDImageWriterSpi"},
                false, null, null, null, null,
                true, PSDMetadata.NATIVE_METADATA_FORMAT_NAME, PSDMetadata.NATIVE_METADATA_FORMAT_CLASS_NAME, null, null
        );
    }
}
