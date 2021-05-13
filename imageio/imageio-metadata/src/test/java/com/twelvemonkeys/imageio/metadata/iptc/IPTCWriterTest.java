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
import com.twelvemonkeys.imageio.metadata.MetadataWriter;
import com.twelvemonkeys.imageio.metadata.MetadataWriterAbstractTest;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

/**
 * IPTCWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: IPTCWriterTest.java,v 1.0 05/06/15 harald.kuhr Exp$
 */
public class IPTCWriterTest extends MetadataWriterAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/iptc/iptc-jpeg-segment.bin").openStream();
    }

    @Override
    protected MetadataWriter createWriter() {
        return new IPTCWriter();
    }

    private IPTCReader createReader() {
        return new IPTCReader();
    }

    @Test
    public void testRewriteExisting() throws IOException {
        IPTCReader reader = createReader();
        Directory iptc = reader.read(getDataAsIIS());
        assumeNotNull(iptc);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes);
        createWriter().write(iptc, stream);
        stream.close();

        Directory written = reader.read(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertEquals(iptc, written);
    }

    @Test
    public void testWrite() throws IOException {
        List<IPTCEntry> entries = new ArrayList<>();
        entries.add(new IPTCEntry(IPTC.TAG_KEYWORDS, new String[] {"Uno", "Due", "Tre"}));

        Directory iptc = new IPTCDirectory(entries);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes);
        createWriter().write(iptc, stream);
        stream.close();

        Directory written = createReader().read(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertEquals(iptc, written);
    }
}