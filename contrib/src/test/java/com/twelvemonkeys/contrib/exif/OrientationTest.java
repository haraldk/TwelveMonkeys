package com.twelvemonkeys.contrib.exif;

import org.junit.Test;

import static com.twelvemonkeys.contrib.exif.Orientation.*;
import static org.junit.Assert.assertEquals;

/**
 * OrientationTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by : harald.kuhr$
 * @version : OrientationTest.java,v 1.0 10/07/2020 harald.kuhr Exp$
 */
public class OrientationTest {
    @Test
    public void testFromMetadataOrientationNull() {
        assertEquals(Normal, Orientation.fromMetadataOrientation(null));
    }

    @Test
    public void testFromMetadataOrientation() {
        assertEquals(Normal, Orientation.fromMetadataOrientation("Normal"));
        assertEquals(Rotate90, Orientation.fromMetadataOrientation("Rotate90"));
        assertEquals(Rotate180, Orientation.fromMetadataOrientation("Rotate180"));
        assertEquals(Rotate270, Orientation.fromMetadataOrientation("Rotate270"));
        assertEquals(FlipH, Orientation.fromMetadataOrientation("FlipH"));
        assertEquals(FlipV, Orientation.fromMetadataOrientation("FlipV"));
        assertEquals(FlipHRotate90, Orientation.fromMetadataOrientation("FlipHRotate90"));
        assertEquals(FlipVRotate90, Orientation.fromMetadataOrientation("FlipVRotate90"));
    }

    @Test
    public void testFromMetadataOrientationIgnoreCase() {
        assertEquals(Normal, Orientation.fromMetadataOrientation("normal"));
        assertEquals(Rotate90, Orientation.fromMetadataOrientation("rotate90"));
        assertEquals(Rotate180, Orientation.fromMetadataOrientation("ROTATE180"));
        assertEquals(Rotate270, Orientation.fromMetadataOrientation("ROTATE270"));
        assertEquals(FlipH, Orientation.fromMetadataOrientation("FLIPH"));
        assertEquals(FlipV, Orientation.fromMetadataOrientation("flipv"));
        assertEquals(FlipHRotate90, Orientation.fromMetadataOrientation("FLIPhrotate90"));
        assertEquals(FlipVRotate90, Orientation.fromMetadataOrientation("fLiPVRotAte90"));
    }

    @Test
    public void testFromMetadataOrientationUnknown() {
        assertEquals(Normal, Orientation.fromMetadataOrientation("foo"));
        assertEquals(Normal, Orientation.fromMetadataOrientation("90"));
        assertEquals(Normal, Orientation.fromMetadataOrientation("randomStringWithNumbers180"));
    }

    @Test
    public void testFromTIFFOrientation() {
        assertEquals(Normal, Orientation.fromTIFFOrientation(1));
        assertEquals(FlipH, Orientation.fromTIFFOrientation(2));
        assertEquals(Rotate180, Orientation.fromTIFFOrientation(3));
        assertEquals(FlipV, Orientation.fromTIFFOrientation(4));
        assertEquals(FlipVRotate90, Orientation.fromTIFFOrientation(5));
        assertEquals(Rotate270, Orientation.fromTIFFOrientation(6));
        assertEquals(FlipHRotate90, Orientation.fromTIFFOrientation(7));
        assertEquals(Rotate90, Orientation.fromTIFFOrientation(8));
    }

    @Test
    public void testFromTIFFOrientationUnknown() {
        assertEquals(Normal, Orientation.fromTIFFOrientation(-1));
        assertEquals(Normal, Orientation.fromTIFFOrientation(0));
        assertEquals(Normal, Orientation.fromTIFFOrientation(9));
        for (int i = 10; i < 1024; i++) {
            assertEquals(Normal, Orientation.fromTIFFOrientation(i));
        }
        assertEquals(Normal, Orientation.fromTIFFOrientation(Integer.MAX_VALUE));
        assertEquals(Normal, Orientation.fromTIFFOrientation(Integer.MIN_VALUE));
    }
}