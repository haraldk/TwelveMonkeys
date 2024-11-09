/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdobePathSegmentTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: AdobePathSegmentTest.java,v 1.0 13/12/14 harald.kuhr Exp$
 */
public class AdobePathSegmentTest {
    @Test
    public void testCreateBadSelectorNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(-1, 1));
    }

    @Test
    public void testCreateBadSelector() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(9, 2));
    }

    @Test
    public void testCreateOpenLengthRecordNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, -1));


    }

    @Test
    public void testCreateOpenLengthRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 42);

        assertEquals(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, segment.selector);
        assertEquals(42, segment.lengthOrRule);
        assertEquals(-1, segment.cppx, 0);
        assertEquals(-1, segment.cppy, 0);
        assertEquals(-1, segment.apx, 0);
        assertEquals(-1, segment.apy, 0);
        assertEquals(-1, segment.cplx, 0);
        assertEquals(-1, segment.cply, 0);
    }

    @Test
    public void testCreateClosedLengthRecordNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, -42));
    }

    @Test
    public void testCreateClosedLengthRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 27);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, segment.selector);
        assertEquals(27, segment.lengthOrRule);
        assertEquals(-1, segment.cppx, 0);
        assertEquals(-1, segment.cppy, 0);
        assertEquals(-1, segment.apx, 0);
        assertEquals(-1, segment.apy, 0);
        assertEquals(-1, segment.cplx, 0);
        assertEquals(-1, segment.cply, 0);
    }

    /// Open subpath

    @Test
    public void testCreateOpenLinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, segment.selector);
        assertEquals(-1, segment.lengthOrRule);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test
    public void testCreateOpenLinkedRecordBad() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 44));
    }

    @Test
    public void testCreateOpenLinkedRecordOutOfRangeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, -16.1, -16.1, 0, 0, 1, 1));
    }

    @Test
    public void testCreateOpenLinkedRecordOutOfRangePositive() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 16.1, 16.1, 0, 0, 1, 1));
    }

    @Test
    public void testCreateOpenUnlinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, segment.selector);
        assertEquals(-1, segment.lengthOrRule);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test
    public void testCreateOpenUnlinkedRecordBad() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, 44));
    }


    @Test
    public void testCreateOpenUnlinkedRecordOutOfRangeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, -16.5, 0, 0, 0, 1, 1));
    }

    @Test
    public void testCreateOpenUnlinkedRecorOutOfRangePositive() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, 0, -17, 0, 0, 16.5, 1));
    }

    /// Closed subpath

    @Test
    public void testCreateClosedLinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, segment.selector);
        assertEquals(-1, segment.lengthOrRule);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test
    public void testCreateClosedLinkedRecordBad() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 44));
    }

    @Test
    public void testCreateClosedLinkedRecordOutOfRangeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, -16.5, -.5, 0, 0, 1, 1));
    }

    @Test
    public void testCreateClosedLinkedRecordOutOfRangePositive() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, .5, 16.5, 0, 0, 1, 1));
    }

    @Test
    public void testCreateClosedUnlinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, segment.selector);
        assertEquals(-1, segment.lengthOrRule);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test
    public void testCreateClosedUnlinkedRecordBad() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, 44));
    }


    @Test
    public void testCreateClosedUnlinkedRecordOutOfRangeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, -.5, -16.5, 0, 0, 1, 1));
    }

    @Test
    public void testCreateClosedUnlinkedRecordOutOfRangePositive() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, 16.5, .5, 0, 0, 1, 1));
    }

    @Test
    public void testToStringRule() {
        String string = new AdobePathSegment(AdobePathSegment.INITIAL_FILL_RULE_RECORD, 0).toString();
        assertTrue(string.startsWith("Rule"), string);
        assertTrue(string.contains("Initial"), string);
        assertTrue(string.contains("fill"), string);
        assertTrue(string.contains("rule=0"), string);
    }

    @Test
    public void testToStringLength() {
        String string = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2).toString();
        assertTrue(string.startsWith("Len"), string);
        assertTrue(string.contains("Closed"), string);
        assertTrue(string.contains("subpath"), string);
        assertTrue(string.contains("length=2"), string);

        string = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 42).toString();
        assertTrue(string.startsWith("Len"), string);
        assertTrue(string.contains("Open"), string);
        assertTrue(string.contains("subpath"), string);
        assertTrue(string.contains("length=42"), string);
    }

    @Test
    public void testToStringOther() {
        String string = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0).toString();
        assertTrue(string.startsWith("Pt"), string);
        assertTrue(string.contains("Open"), string);
        assertTrue(string.contains("Bezier"), string);
        assertTrue(string.contains("linked"), string);

        string = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0).toString();
        assertTrue(string.startsWith("Pt"), string);
        assertTrue(string.contains("Closed"), string);
        assertTrue(string.contains("Bezier"), string);
        assertTrue(string.contains("linked"), string);
    }

    @Test
    public void testEqualsLength() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2);
        assertEquals(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2), segment);
        assertFalse(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 3).equals(segment));
        assertFalse(new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 2).equals(segment));
    }

    @Test
    public void testEqualsOther() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0);
        assertEquals(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0), segment);
        assertFalse(new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0).equals(segment));
        assertFalse(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, 0, 0, 1, 1, 0, 0).equals(segment));
        assertFalse(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 0, 0.1, 1, 1, 0, 0).equals(segment));
    }

    @Test
    public void testHashCodeLength() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2);
        assertEquals(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2).hashCode(), segment.hashCode());
        assertFalse(new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 3).hashCode() == segment.hashCode());
        assertFalse(new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 2).hashCode() == segment.hashCode());
    }
}
