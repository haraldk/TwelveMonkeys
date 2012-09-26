package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PSDUnicodeAlphaNames
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDUnicodeAlphaNames.java,v 1.0 Nov 7, 2009 9:16:56 PM haraldk Exp$
 */
final class PSDUnicodeAlphaNames extends PSDImageResource {
    List<String> names;

    PSDUnicodeAlphaNames(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        names = new ArrayList<String>();

        long left = size;
        while (left > 0) {
            String name = PSDUtil.readUnicodeString(pInput);
            names.add(name);
            left -= name.length() * 2 + 4;
        }
    }
}
