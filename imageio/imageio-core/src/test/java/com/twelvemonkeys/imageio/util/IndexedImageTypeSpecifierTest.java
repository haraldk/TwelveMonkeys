package com.twelvemonkeys.imageio.util;

import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import static org.junit.Assert.*;

/**
 * IndexedImageTypeSpecifierTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IndexedImageTypeSpecifierTestCase.java,v 1.0 Jun 9, 2008 2:42:03 PM haraldk Exp$
 */
public class IndexedImageTypeSpecifierTest {
    @Test
    public void testEquals() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);

        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier other = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier different = IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(2, 2, new int[]{0xff00ff, 0x00, 0xff00ff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE));

        assertEquals(spec, other);
        assertEquals(other, spec);

        assertEquals(spec.hashCode(), other.hashCode());

        assertTrue(spec.equals(other));
        assertTrue(other.equals(spec));

        // TODO: There is still a problem that IndexColorModel does not override equals,
        // so any model with the same number of bits, transparency, and transfer type will be treated as equal
        assertFalse(other.equals(different));
    }

    @Test
    public void testHashCode() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);

        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier other = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
        ImageTypeSpecifier different = IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(2, 2, new int[]{0xff00ff, 0x00, 0xff00ff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE));

        // TODO: There is still a problem that IndexColorModel does not override hashCode,
        // so any model with the same number of bits, transparency, and transfer type will have same hash
        assertEquals(spec.hashCode(), other.hashCode());
        assertFalse(spec.hashCode() == different.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        IndexedImageTypeSpecifier.createFromIndexColorModel(null);
    }

    @Test
    public void testCreateBufferedImageBinary() {
        IndexColorModel cm = new IndexColorModel(1, 2, new int[]{0xffffff, 0x00}, 0, false, -1, DataBuffer.TYPE_BYTE);
        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);

        BufferedImage image = spec.createBufferedImage(2, 2);

        assertNotNull(image);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, image.getType());
        assertEquals(cm, image.getColorModel());
    }

    @Test
    public void testCreateBufferedImageIndexed() {
        IndexColorModel cm = new IndexColorModel(8, 256, new int[256], 0, false, -1, DataBuffer.TYPE_BYTE);
        ImageTypeSpecifier spec = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);

        BufferedImage image = spec.createBufferedImage(2, 2);

        assertNotNull(image);
        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, image.getType());
        assertEquals(cm, image.getColorModel());
    }
}
