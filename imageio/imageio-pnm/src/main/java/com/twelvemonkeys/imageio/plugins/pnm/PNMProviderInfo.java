package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

/**
 * PNMProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PNMProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
class PNMProviderInfo extends ProviderInfo {
    // NOTE: Because the ReaderSpi and the two WriterSpis supports different formats,
    // we don't use the standard ImageReaderWriterProviderInfo superclass here.

    public PNMProviderInfo() {
        super(PNMProviderInfo.class.getPackage());
    }
}
