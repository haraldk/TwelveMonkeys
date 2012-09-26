package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDPrintScale
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDPrintScale.java,v 1.0 Nov 7, 2009 9:41:17 PM haraldk Exp$
 */
final class PSDPrintScale extends PSDImageResource {
    // 2 bytes style (0 = centered, 1 = size to fit, 2 = user defined).
    // 4 bytes x location (floating point).
    // 4 bytes y location (floating point).
    // 4 bytes scale (floating point)

    short style;
    float xLocation;
    float ylocation;
    float scale;

    PSDPrintScale(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        style = pInput.readShort();
        xLocation = pInput.readFloat();
        ylocation = pInput.readFloat();
        scale = pInput.readFloat();
    }
}
