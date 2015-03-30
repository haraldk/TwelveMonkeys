package com.twelvemonkeys.imageio.path;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * AdobePathSegmentTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: AdobePathSegmentTest.java,v 1.0 13/12/14 harald.kuhr Exp$
 */
public class AdobePathSegmentTest {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateBadSelectorNegative() {
        new AdobePathSegment(-1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBadSelector() {
        new AdobePathSegment(9, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOpenLengthRecordNegative() {
        new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, -1);

    }

    @Test
    public void testCreateOpenLengthRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 42);

        assertEquals(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, segment.selector);
        assertEquals(42, segment.length);
        assertEquals(-1, segment.cppx, 0);
        assertEquals(-1, segment.cppy, 0);
        assertEquals(-1, segment.apx, 0);
        assertEquals(-1, segment.apy, 0);
        assertEquals(-1, segment.cplx, 0);
        assertEquals(-1, segment.cply, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateClosedLengthRecordNegative() {
        new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, -42);
    }

    @Test
    public void testCreateClosedLengthRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 27);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, segment.selector);
        assertEquals(27, segment.length);
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
        assertEquals(-1, segment.length);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOpenLinkedRecordBad() {
        new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 44);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOpenLinkedRecordNegative() {
        new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, -.5, -.5, 0, 0, 1, 1);
    }

    @Test
    public void testCreateOpenUnlinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, segment.selector);
        assertEquals(-1, segment.length);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOpenUnlinkedRecordBad() {
        new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, 44);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testCreateOpenUnlinkedRecordNegative() {
        new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED, -.5, -.5, 0, 0, 1, 1);
    }

    /// Closed subpath

    @Test
    public void testCreateClosedLinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, segment.selector);
        assertEquals(-1, segment.length);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateClosedLinkedRecordBad() {
        new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 44);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateClosedLinkedRecordNegative() {
        new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, -.5, -.5, 0, 0, 1, 1);
    }

    @Test
    public void testCreateClosedUnlinkedRecord() {
        AdobePathSegment segment = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, .5, .5, 0, 0, 1, 1);

        assertEquals(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, segment.selector);
        assertEquals(-1, segment.length);
        assertEquals(.5, segment.cppx, 0);
        assertEquals(.5, segment.cppy, 0);
        assertEquals(0, segment.apx, 0);
        assertEquals(0, segment.apy, 0);
        assertEquals(1, segment.cplx, 0);
        assertEquals(1, segment.cply, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateClosedUnlinkedRecordBad() {
        new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, 44);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testCreateClosedUnlinkedRecordNegative() {
        new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED, -.5, -.5, 0, 0, 1, 1);
    }

    @Test
    public void testToStringRule() {
        String string = new AdobePathSegment(AdobePathSegment.INITIAL_FILL_RULE_RECORD, 2).toString();
        assertTrue(string, string.startsWith("Rule"));
        assertTrue(string, string.contains("Initial"));
        assertTrue(string, string.contains("fill"));
        assertTrue(string, string.contains("rule=2"));
    }

    @Test
    public void testToStringLength() {
        String string = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD, 2).toString();
        assertTrue(string, string.startsWith("Len"));
        assertTrue(string, string.contains("Closed"));
        assertTrue(string, string.contains("subpath"));
        assertTrue(string, string.contains("totalPoints=2"));

        string = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD, 42).toString();
        assertTrue(string, string.startsWith("Len"));
        assertTrue(string, string.contains("Open"));
        assertTrue(string, string.contains("subpath"));
        assertTrue(string, string.contains("totalPoints=42"));
    }

    @Test
    public void testToStringOther() {
        String string = new AdobePathSegment(AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0).toString();
        assertTrue(string, string.startsWith("Pt"));
        assertTrue(string, string.contains("Open"));
        assertTrue(string, string.contains("Bezier"));
        assertTrue(string, string.contains("linked"));

        string = new AdobePathSegment(AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 1, 1, 0, 0).toString();
        assertTrue(string, string.startsWith("Pt"));
        assertTrue(string, string.contains("Closed"));
        assertTrue(string, string.contains("Bezier"));
        assertTrue(string, string.contains("linked"));
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
