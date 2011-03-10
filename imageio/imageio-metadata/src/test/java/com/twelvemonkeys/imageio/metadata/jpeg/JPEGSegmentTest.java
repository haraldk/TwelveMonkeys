/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.jpeg;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * JPEGSegmentTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentTest.java,v 1.0 02.03.11 10.46 haraldk Exp$
 */
public class JPEGSegmentTest extends ObjectAbstractTestCase {
    @Test
    public void testCreate() {
        byte[] bytes = new byte[14];
        System.arraycopy("JFIF".getBytes(Charset.forName("ascii")), 0, bytes, 0, 4);

        JPEGSegment segment = new JPEGSegment(0xFFE0, bytes);

        assertEquals(0xFFE0, segment.marker());
        assertEquals("JFIF", segment.identifier());
        assertEquals(16, segment.segmentLength());
        assertEquals(bytes.length - 5, segment.length());
    }

    @Test
    public void testToStringAppSegment() {
        byte[] bytes = new byte[14];
        System.arraycopy("JFIF".getBytes(Charset.forName("ascii")), 0, bytes, 0, 4);
        JPEGSegment segment = new JPEGSegment(0xFFE0, bytes);

        assertEquals("JPEGSegment[ffe0/JFIF size: 16]", segment.toString());
    }

    @Test
    public void testToStringNonAppSegment() {
        byte[] bytes = new byte[40];
        JPEGSegment segment = new JPEGSegment(0xFFC4, bytes);

        assertEquals("JPEGSegment[ffc4 size: 42]", segment.toString());
    }

    @Override
    protected Object makeObject() {
        byte[] bytes = new byte[11];
        System.arraycopy("Exif".getBytes(Charset.forName("ascii")), 0, bytes, 0, 4);
        return new JPEGSegment(0xFFE1, bytes);
    }
}
