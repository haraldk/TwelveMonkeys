package com.twelvemonkeys.imageio.plugins.jpeg;

import java.io.DataInput;
import java.io.IOException;

/**
 * Unknown.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Unknown.java,v 1.0 22/08/16 harald.kuhr Exp$
 */
final class Unknown extends Segment {
    final byte[] data;

    private Unknown(final int marker, final byte[] data) {
        super(marker);

        this.data = data;
    }

    public static Segment read(int marker, int length, DataInput data) throws IOException {
        byte[] bytes = new byte[length - 2];
        data.readFully(bytes);
        return new Unknown(marker, bytes);
    }
}
