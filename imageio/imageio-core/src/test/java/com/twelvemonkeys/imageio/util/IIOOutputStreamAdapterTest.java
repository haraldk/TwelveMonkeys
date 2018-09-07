/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.io.OutputStreamAbstractTest;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

/**
 * IIOOutputStreamAdapterTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IIOOutputStreamAdapterTestCase.java,v 1.0 30.11.11 12:21 haraldk Exp$
 */
public class IIOOutputStreamAdapterTest extends OutputStreamAbstractTest {
    @Override
    protected OutputStream makeObject() {
        return new IIOOutputStreamAdapter(new MemoryCacheImageOutputStream(new ByteArrayOutputStream()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new IIOOutputStreamAdapter(null);
    }

    @Test
    public void testFlushOnAdapterDoesNotMoveFlushedPositionInBacking() throws IOException {
        MemoryCacheImageOutputStream backing = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        IIOOutputStreamAdapter adapter = new IIOOutputStreamAdapter(backing);

        // Sanity check
        assertEquals(0, backing.getFlushedPosition());

        // Write & flush
        adapter.write(0xCA);
        adapter.write(new byte[8]);
        adapter.write(0xFE);
        adapter.flush();

        // Assertions
        assertEquals(10, backing.length());
        assertEquals(10, backing.getStreamPosition());
        assertEquals(0, backing.getFlushedPosition());

        // Just make sure we can safely seek back to start and read data back
        backing.seek(0);
        assertEquals(0, backing.getStreamPosition());

        // If this can be read, I think the contract of flush is also fulfilled (kind of)
        assertEquals(0xCA, backing.read());
        assertEquals(8, backing.skipBytes(8));
        assertEquals(0xFE, backing.read());
    }
}
