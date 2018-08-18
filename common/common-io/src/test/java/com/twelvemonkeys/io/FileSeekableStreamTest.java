/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

/**
 * MemoryCacheSeekableStreamTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/FileSeekableStreamTestCase.java#3 $
 */
public class FileSeekableStreamTest extends SeekableInputStreamAbstractTest {
    protected SeekableInputStream makeInputStream(final InputStream pStream) {
        try {
            return new FileSeekableStream(createFileWithContent(pStream));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createFileWithContent(final InputStream pStream) throws IOException {
        File temp = File.createTempFile("tm-io-junit", null);
        temp.deleteOnExit();
        OutputStream os = new FileOutputStream(temp);
        try {
            FileUtil.copy(pStream, os);
        }
        finally {
            os.close();
            pStream.close();
        }
        return temp;
    }

    @Test
    @Override
    public void testCloseUnderlyingStream() throws IOException {
        // There is no underlying stream here...
    }

    @Test
    public void testCloseUnderlyingFile() throws IOException {
        final boolean[] closed = new boolean[1];

        File file = createFileWithContent(new ByteArrayInputStream(makeRandomArray(256)));

        RandomAccessFile raf = new RandomAccessFile(file, "r") {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        FileSeekableStream stream = new FileSeekableStream(raf);

        try {
            FileUtil.read(stream); // Read until EOF

            assertEquals("EOF not reached (test case broken)", -1, stream.read());
            assertFalse("Underlying stream closed before close", closed[0]);
        }
        finally {
            stream.close();
        }

        assertTrue("Underlying stream not closed", closed[0]);
    }
}
