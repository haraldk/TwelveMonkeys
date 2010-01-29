package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.iptc.IPTCReader;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDIPTCData
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDIPTCData.java,v 1.0 Nov 7, 2009 9:52:14 PM haraldk Exp$
 */
final class PSDIPTCData extends PSDImageResource {
    Directory mDirectory;

    PSDIPTCData(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        // Read IPTC directory
        mDirectory = new IPTCReader().read(pInput);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();
        builder.append(", ").append(mDirectory);
        builder.append("]");

        return builder.toString();
    }
}
