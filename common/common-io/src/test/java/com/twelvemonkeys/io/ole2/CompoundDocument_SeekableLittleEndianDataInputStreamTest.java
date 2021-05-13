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

package com.twelvemonkeys.io.ole2;

import com.twelvemonkeys.io.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * CompoundDocument_SeekableLittleEndianDataInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CompoundDocument_SeekableLittleEndianDataInputStreamTestCase.java,v 1.0 18.10.11 16:35 haraldk Exp$
 */
public class CompoundDocument_SeekableLittleEndianDataInputStreamTest extends InputStreamAbstractTest implements SeekableInterfaceTest {
    private final SeekableInterfaceTest seekableTest = new SeekableAbstractTest() {
        @Override
        protected Seekable createSeekable() {
            return (Seekable) makeInputStream();
        }
    };

    @Override
    protected CompoundDocument.SeekableLittleEndianDataInputStream makeInputStream(byte[] pBytes) {
        return new CompoundDocument.SeekableLittleEndianDataInputStream(new MemoryCacheSeekableStream(new ByteArrayInputStream(pBytes)));
    }

    @Test
    public void testSeekable() {
        seekableTest.testSeekable();
    }
}
