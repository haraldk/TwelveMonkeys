package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDPixelAspectRatio
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDPixelAspectRatio.java,v 1.0 Nov 7, 2009 8:23:09 PM haraldk Exp$
 */
final class PSDPixelAspectRatio extends PSDImageResource {
    // 4 bytes (version = 1), 8 bytes double, x / y of a pixel
    int mVersion;
    double mAspect;

    PSDPixelAspectRatio(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        mVersion = pInput.readInt();
        mAspect = pInput.readDouble();
    }
}
