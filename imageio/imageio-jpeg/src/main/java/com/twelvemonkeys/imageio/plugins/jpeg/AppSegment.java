package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.lang.Validate;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * AppSegment.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: AppSegment.java,v 1.0 22/08/16 harald.kuhr Exp$
 */
class AppSegment extends Segment {

    final String identifier;
    final byte[] data;

    AppSegment(int marker, final String identifier, final byte[] data) {
        super(marker);

        this.identifier = Validate.notEmpty(identifier, "identifier");
        this.data = data;
    }

    protected AppSegment(int marker, final String identifier) {
        this(marker, identifier, null);
    }

    InputStream data() {
        int offset = identifier.length() + 1;
        return new ByteArrayInputStream(data, offset, data.length - offset);
    }

    public static AppSegment read(final int marker, final String identifier, final DataInput data, final int length) throws IOException {
        switch (marker) {
            case JPEG.APP0:
                // JFIF
                if ("JFIF".equals(identifier)) {
                    return JFIF.read(data, length);
                }
            case JPEG.APP1:
                // JFXX
                if ("JFXX".equals(identifier)) {
                    return JFXX.read(data, length);
                }
                // TODO: Exif?
            case JPEG.APP2:
                // ICC_PROFILE
                if ("ICC_PROFILE".equals(identifier)) {
                    return ICCProfile.read(data, length);
                }
            case JPEG.APP14:
                // Adobe
                if ("Adobe".equals(identifier)) {
                    return AdobeDCT.read(data, length);
                }

            default:
                // Generic APPn segment
                byte[] bytes = new byte[length - 2];
                data.readFully(bytes);
                return new AppSegment(marker, identifier, bytes);
        }
    }
}
