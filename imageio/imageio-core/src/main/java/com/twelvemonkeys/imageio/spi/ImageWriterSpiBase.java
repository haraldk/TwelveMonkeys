package com.twelvemonkeys.imageio.spi;

import javax.imageio.spi.ImageWriterSpi;

/**
 * ImageWriterSpiBase.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ImageWriterSpiBase.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
public abstract class ImageWriterSpiBase extends ImageWriterSpi {
    protected ImageWriterSpiBase(final ReaderWriterProviderInfo info) {
        super(
                info.getVendorName(), info.getVersion(),
                info.formatNames(), info.suffixes(), info.mimeTypes(),
                info.writerClassName(), info.outputTypes(),
                info.readerSpiClassNames(),
                info.supportsStandardStreamMetadataFormat(),
                info.nativeStreamMetadataFormatName(), info.nativeStreamMetadataFormatClassName(),
                info.extraStreamMetadataFormatNames(), info.extraStreamMetadataFormatClassNames(),
                info.supportsStandardImageMetadataFormat(),
                info.nativeImageMetadataFormatName(), info.nativeImageMetadataFormatClassName(),
                info.extraImageMetadataFormatNames(), info.extraImageMetadataFormatClassNames()
        );
    }
}
