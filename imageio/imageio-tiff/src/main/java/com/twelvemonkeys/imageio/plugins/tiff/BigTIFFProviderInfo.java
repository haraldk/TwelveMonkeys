package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * BigTIFFProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BigTIFFProviderInfo.java,v 1.0 26/04/2017 harald.kuhr Exp$
 */
final class BigTIFFProviderInfo extends ReaderWriterProviderInfo {
    protected BigTIFFProviderInfo() {
        super(
                BigTIFFProviderInfo.class,
                new String[] {"bigtiff", "BigTIFF", "BIGTIFF"},
                new String[] {"tif", "tiff", "btf", "tf8", "btiff"},
                new String[] {
                        "image/tiff", "image/x-tiff"
                },
                "com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.tiff.BigTIFFImageReaderSpi"},
                null,
                null,
                false, TIFFStreamMetadata.SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, "com.twelvemonkeys.imageio.plugins.tiff.TIFFStreamMetadataFormat", null, null,
                true, TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, "com.twelvemonkeys.imageio.plugins.tiff.TIFFMedataFormat", null, null
        );
    }
}
