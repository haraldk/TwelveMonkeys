package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataWriter;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * IPTCWriter.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: IPTCWriter.java,v 1.0 28/05/15 harald.kuhr Exp$
 */
public final class IPTCWriter extends MetadataWriter {
    @Override
    public boolean write(final Directory directory, final ImageOutputStream stream) throws IOException {
        notNull(directory, "directory");
        notNull(stream, "stream");

        // TODO: Make sure we always write application record version (2.00)
        // TODO: Write encoding UTF8?

        for (Entry entry : directory) {
            int tag = (Integer) entry.getIdentifier();
            Object value = entry.getValue();

            if (IPTC.Tags.isArray((short) tag)) {
                Object[] values = (Object[]) value;

                for (Object v : values) {
                    stream.write(0x1c);
                    stream.writeShort(tag);
                    writeValue(stream, v);
                }
            }
            else {
                stream.write(0x1c);
                stream.writeShort(tag);
                writeValue(stream, value);
            }
        }

        return false;
    }

    private void writeValue(final ImageOutputStream stream, final Object value) throws IOException {
        if (value instanceof String) {
            byte[] data = ((String) value).getBytes(StandardCharsets.UTF_8);
            stream.writeShort(data.length);
            stream.write(data);
        }
        else if (value instanceof byte[]) {
            byte[] data = (byte[]) value;
            stream.writeShort(data.length);
            stream.write(data);
        }
        else if (value instanceof Integer) {
            // TODO: Need to know types from tag
            stream.writeShort(2);
            stream.writeShort((Integer) value);
        }
    }
}
