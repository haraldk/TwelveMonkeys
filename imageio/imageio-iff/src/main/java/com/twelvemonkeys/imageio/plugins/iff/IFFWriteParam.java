package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.ImageWriteParam;
import java.util.Locale;

/**
 * IFFWriteParam.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IFFWriteParam.java,v 1.0 03/02/2022 haraldk Exp$
 */
public final class IFFWriteParam extends ImageWriteParam {

    static final String[] COMPRESSION_TYPES = {"NONE", "RLE"};

    public IFFWriteParam(final Locale locale) {
        super(locale);

        compressionTypes = COMPRESSION_TYPES;
        compressionType = compressionTypes[1];

        canWriteCompressed = true;
    }
}
