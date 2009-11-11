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
    List<String> mNames;

    PSDUnicodeAlphaNames(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        mNames = new ArrayList<String>();

        long left = mSize;
        while (left > 0) {
            String name = PSDUtil.readUTF16String(pInput);
            mNames.add(name);
            left -= name.length() * 2 + 4;
        }
    }
}
