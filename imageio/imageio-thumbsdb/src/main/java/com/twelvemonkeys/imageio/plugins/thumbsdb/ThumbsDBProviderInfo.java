package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * ThumbsDBProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ThumbsDBProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class ThumbsDBProviderInfo extends ReaderWriterProviderInfo {
    protected ThumbsDBProviderInfo() {
        super(
                ThumbsDBProviderInfo.class,
                new String[]{"thumbs", "THUMBS", "Thumbs DB"},
                new String[]{"db"},
                new String[]{"image/x-thumbs-db", "application/octet-stream"}, // TODO: Check IANA et al...
                "com.twelvemonkeys.imageio.plugins.thumbsdb.ThumbsDBImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.thumbsdb.ThumbsDBImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}
