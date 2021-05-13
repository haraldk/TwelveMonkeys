/*
 * Copyright (c) 2015, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
