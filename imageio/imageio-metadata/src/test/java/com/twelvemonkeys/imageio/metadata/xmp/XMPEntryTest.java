/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.EntryAbstractTest;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * XMPEntryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPEntryTest.java,v 1.0 03.01.12 09:41 haraldk Exp$
 */
public class XMPEntryTest extends EntryAbstractTest {
    @Override
    protected Entry createEntry(Object value) {
        return new XMPEntry(XMP.NS_XAP + ":foo", value);
    }

    @Test
    public void testTypeNameMap() {
        assertEquals("Map", createEntry(new HashMap<>()).getTypeName());
        assertEquals("Map", createEntry(Collections.emptyMap()).getTypeName());
        assertEquals("Map", createEntry(Collections.singletonMap("foo", "bar")).getTypeName());
    }

    @Test
    public void testTypeNameSet() {
        assertEquals("Set", createEntry(new HashSet<>()).getTypeName());
        assertEquals("Set", createEntry(Collections.singleton("foo")).getTypeName());
        assertEquals("Set", createEntry(Collections.unmodifiableSet(Collections.singleton("foo"))).getTypeName());
    }

    @Test
    public void testTypeNameList() {
        assertEquals("List", createEntry(new ArrayList<>()).getTypeName());
        assertEquals("List", createEntry(Collections.emptyList()).getTypeName());
        assertEquals("List", createEntry(Arrays.asList("foo", "bar")).getTypeName());
    }
}
