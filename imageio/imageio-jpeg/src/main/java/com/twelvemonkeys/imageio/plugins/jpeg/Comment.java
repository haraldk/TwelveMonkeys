package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Comment.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Comment.java,v 1.0 23/08/16 harald.kuhr Exp$
 */
class Comment extends Segment {
    final String comment;

    private Comment(final String comment) {
        super(JPEG.COM);
        this.comment = comment;
    }

    public static Segment read(final DataInput data, final int length) throws IOException {
        byte[] ascii = new byte[length];
        data.readFully(ascii);

        return new Comment(new String(ascii, StandardCharsets.UTF_8));
    }
}
