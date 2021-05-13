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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.CollectionUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * CompoundReaderTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/CompoundReaderTestCase.java#2 $
 */
public class CompoundReaderTest extends ReaderAbstractTest {
    protected Reader makeReader(String pInput) {
        // Split
        String[] input = StringUtil.toStringArray(pInput, " ");
        List<Reader> readers = new ArrayList<Reader>(input.length);

        // Reappend spaces...
        // TODO: Add other readers
        for (int i = 0; i < input.length; i++) {
            if (i != 0) {
                input[i] = " " + input[i];
            }
            readers.add(new StringReader(input[i]));
        }

        return new CompoundReader(readers.iterator());
    }

    @Test
    public void testNullConstructor() {
        try {
            new CompoundReader(null);
            fail("Should not allow null argument");
        }
        catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testEmptyIteratorConstructor() throws IOException {
        Reader reader = new CompoundReader(CollectionUtil.iterator(new Reader[0]));
        assertEquals(-1, reader.read());
    }

    @Test
    public void testIteratorWithNullConstructor() throws IOException {
        try {
            new CompoundReader(CollectionUtil.iterator(new Reader[] {null}));
            fail("Should not allow null in iterator argument");
        }
        catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }
}
