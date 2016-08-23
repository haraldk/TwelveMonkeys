package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import java.io.DataInput;
import java.io.IOException;

/**
 * ICCProfile.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ICCProfile.java,v 1.0 22/08/16 harald.kuhr Exp$
 */
final class ICCProfile extends AppSegment {
    protected ICCProfile(final byte[] data) {
        super(JPEG.APP2, "ICC_PROFILE", data);
    }

    // TODO: Create util method to concat all ICC segments to one and return ICC_Profile (move from JPEGImageReader)

    public static ICCProfile read(DataInput data, int length) throws IOException {
        byte[] bytes = new byte[length - 2];
        data.readFully(bytes);

        return new ICCProfile(bytes);
    }
}
