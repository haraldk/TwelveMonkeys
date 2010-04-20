package com.twelvemonkeys.imageio.util;

import junit.framework.TestCase;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * IndexedImageTypeSpecifierTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IndexedImageTypeSpecifierTestCase.java,v 1.0 Jun 9, 2008 2:42:03 PM haraldk Exp$
 */
public class IndexedImageTypeSpecifierTestCase extends TestCase {
    public void testEquals() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);

        IndexedImageTypeSpecifier spec = new IndexedImageTypeSpecifier(cm);
        IndexedImageTypeSpecifier other = new IndexedImageTypeSpecifier(cm);

        assertEquals(spec, other);
        assertEquals(other, spec);

        assertTrue(spec.equals(other));
        assertTrue(other.equals(spec));
    }
}
