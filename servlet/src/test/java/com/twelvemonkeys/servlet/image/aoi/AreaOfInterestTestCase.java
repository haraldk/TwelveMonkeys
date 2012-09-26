package com.twelvemonkeys.servlet.image.aoi;

import com.twelvemonkeys.servlet.image.aoi.DefaultAreaOfInterest;
import com.twelvemonkeys.servlet.image.aoi.UniformAreaOfInterest;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class AreaOfInterestTestCase {
    private static final Dimension SQUARE_200_200 = new Dimension(200, 200);
    private static final Dimension PORTRAIT_100_200 = new Dimension(100, 200);
    private static final Dimension LANDSCAPE_200_100 = new Dimension(200, 100);
    private static final Dimension SQUARE_100_100 = new Dimension(100, 100);
    // -----------------------------------------------------------------------------------------------------------------
    // Absolute AOI
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOIAbsolute() {
        assertEquals(new Rectangle(10, 10, 100, 100), new DefaultAreaOfInterest(SQUARE_200_200).getAOI(10, 10, 100, 100));
    }

    @Test
    public void testGetAOIAbsoluteOverflowX() {
        assertEquals(new Rectangle(10, 10, 90, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(10, 10, 100, 100));
    }

    @Test
    public void testGetAOIAbsoluteOverflowW() {

        assertEquals(new Rectangle(0, 10, 100, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(0, 10, 110, 100));
    }

    @Test
    public void testGetAOIAbsoluteOverflowY() {

        assertEquals(new Rectangle(10, 10, 100, 90), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(10, 10, 100, 100));
    }

    @Test
    public void testGetAOIAbsoluteOverflowH() {

        assertEquals(new Rectangle(10, 0, 100, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(10, 0, 100, 110));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Uniform AOI centered
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOIUniformCenteredS2SUp() {
        assertEquals(new Rectangle(0, 0, 100, 100), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 333, 333));
    }

    @Test
    public void testGetAOIUniformCenteredS2SDown() {
        assertEquals(new Rectangle(0, 0, 100, 100), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 33, 33));
    }

    @Test
    public void testGetAOIUniformCenteredS2SNormalized() {
        assertEquals(new Rectangle(0, 0, 100, 100), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOIUniformCenteredS2W() {
        assertEquals(new Rectangle(0, 25, 100, 50), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOIUniformCenteredS2WNormalized() {
        assertEquals(new Rectangle(0, 25, 100, 50), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOIUniformCenteredS2N() {
        assertEquals(new Rectangle(25, 0, 50, 100), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 200));
    }

    @Test
    public void testGetAOIUniformCenteredS2NNormalized() {
        assertEquals(new Rectangle(25, 0, 50, 100), new UniformAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 50, 100));
    }

    @Test
    public void testGetAOIUniformCenteredW2S() {
        assertEquals(new Rectangle(50, 0, 100, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 333, 333));
    }

    @Test
    public void testGetAOIUniformCenteredW2SNormalized() {
        assertEquals(new Rectangle(50, 0, 100, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOIUniformCenteredW2W() {
        assertEquals(new Rectangle(0, 0, 200, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOIUniformCenteredW2WW() {
        assertEquals(new Rectangle(0, 25, 200, 50), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 200, 50));
    }

    @Test
    public void testGetAOIUniformCenteredW2WN() {
        assertEquals(new Rectangle(25, 0, 150, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 75, 50));
    }

    @Test
    public void testGetAOIUniformCenteredW2WNNormalized() {
        assertEquals(new Rectangle(25, 0, 150, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 150, 100));
    }

    @Test
    public void testGetAOIUniformCenteredW2WNormalized() {
        assertEquals(new Rectangle(0, 0, 200, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOIUniformCenteredW2N() {
        assertEquals(new Rectangle(75, 0, 50, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 200));
    }

    @Test
    public void testGetAOIUniformCenteredW2NNormalized() {
        assertEquals(new Rectangle(75, 0, 50, 100), new UniformAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 50, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2S() {
        assertEquals(new Rectangle(0, 50, 100, 100), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 333, 333));
    }

    @Test
    public void testGetAOIUniformCenteredN2SNormalized() {
        assertEquals(new Rectangle(0, 50, 100, 100), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2W() {
        assertEquals(new Rectangle(0, 75, 100, 50), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2WNormalized() {
        assertEquals(new Rectangle(0, 75, 100, 50), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOIUniformCenteredN2N() {
        assertEquals(new Rectangle(0, 0, 100, 200), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 50, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2NN() {
        assertEquals(new Rectangle(25, 0, 50, 200), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 25, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2NW() {
        assertEquals(new Rectangle(0, 33, 100, 133), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 75, 100));
    }

    @Test
    public void testGetAOIUniformCenteredN2NWNormalized() {
        assertEquals(new Rectangle(0, 37, 100, 125), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 125));
    }

    @Test
    public void testGetAOIUniformCenteredN2NNormalized() {
        assertEquals(new Rectangle(0, 0, 100, 200), new UniformAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 200));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Absolute AOI centered
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOICenteredS2SUp() {
        assertEquals(new Rectangle(0, 0, 100, 100), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 333, 333));
    }

    @Test
    public void testGetAOICenteredS2SDown() {
        assertEquals(new Rectangle(33, 33, 33, 33), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 33, 33));
    }

    @Test
    public void testGetAOICenteredS2SSame() {
        assertEquals(new Rectangle(0, 0, 100, 100), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOICenteredS2WOverflow() {
        assertEquals(new Rectangle(0, 0, 100, 100), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOICenteredS2W() {
        assertEquals(new Rectangle(40, 45, 20, 10), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 20, 10));
    }

    @Test
    public void testGetAOICenteredS2WMax() {
        assertEquals(new Rectangle(0, 25, 100, 50), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOICenteredS2NOverflow() {
        assertEquals(new Rectangle(0, 0, 100, 100), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 100, 200));
    }

    @Test
    public void testGetAOICenteredS2N() {
        assertEquals(new Rectangle(45, 40, 10, 20), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 10, 20));
    }

    @Test
    public void testGetAOICenteredS2NMax() {
        assertEquals(new Rectangle(25, 0, 50, 100), new DefaultAreaOfInterest(SQUARE_100_100).getAOI(-1, -1, 50, 100));
    }

    @Test
    public void testGetAOICenteredW2SOverflow() {
        assertEquals(new Rectangle(0, 0, 200, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 333, 333));
    }

    @Test
    public void testGetAOICenteredW2S() {
        assertEquals(new Rectangle(75, 25, 50, 50), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 50, 50));
    }

    @Test
    public void testGetAOICenteredW2SMax() {
        assertEquals(new Rectangle(50, 0, 100, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOICenteredW2WOverflow() {
        assertEquals(new Rectangle(0, 0, 200, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 300, 200));
    }

    @Test
    public void testGetAOICenteredW2W() {
        assertEquals(new Rectangle(50, 25, 100, 50), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOICenteredW2WW() {
        assertEquals(new Rectangle(10, 40, 180, 20), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 180, 20));
    }

    @Test
    public void testGetAOICenteredW2WN() {
        assertEquals(new Rectangle(62, 25, 75, 50), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 75, 50));
    }

    @Test
    public void testGetAOICenteredW2WSame() {
        assertEquals(new Rectangle(0, 0, 200, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOICenteredW2NOverflow() {
        assertEquals(new Rectangle(50, 0, 100, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 100, 200));
    }

    @Test
    public void testGetAOICenteredW2N() {
        assertEquals(new Rectangle(83, 25, 33, 50), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 33, 50));
    }

    @Test
    public void testGetAOICenteredW2NMax() {
        assertEquals(new Rectangle(75, 0, 50, 100), new DefaultAreaOfInterest(LANDSCAPE_200_100).getAOI(-1, -1, 50, 100));
    }

    @Test
    public void testGetAOICenteredN2S() {
        assertEquals(new Rectangle(33, 83, 33, 33), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 33, 33));
    }

    @Test
    public void testGetAOICenteredN2SMax() {
        assertEquals(new Rectangle(0, 50, 100, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 100));
    }

    @Test
    public void testGetAOICenteredN2WOverflow() {
        assertEquals(new Rectangle(0, 50, 100, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 200, 100));
    }

    @Test
    public void testGetAOICenteredN2W() {
        assertEquals(new Rectangle(40, 95, 20, 10), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 20, 10));
    }

    @Test
    public void testGetAOICenteredN2WMax() {
        assertEquals(new Rectangle(0, 75, 100, 50), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 50));
    }

    @Test
    public void testGetAOICenteredN2N() {
        assertEquals(new Rectangle(45, 90, 10, 20), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 10, 20));
    }

    @Test
    public void testGetAOICenteredN2NSame() {
        assertEquals(new Rectangle(0, 0, 100, 200), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 200));
    }

    @Test
    public void testGetAOICenteredN2NN() {
        assertEquals(new Rectangle(37, 50, 25, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 25, 100));
    }

    @Test
    public void testGetAOICenteredN2NW() {
        assertEquals(new Rectangle(12, 50, 75, 100), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 75, 100));
    }

    @Test
    public void testGetAOICenteredN2NWMax() {
        assertEquals(new Rectangle(0, 37, 100, 125), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 125));
    }

    @Test
    public void testGetAOICenteredN2NMax() {
        assertEquals(new Rectangle(0, 0, 100, 200), new DefaultAreaOfInterest(PORTRAIT_100_200).getAOI(-1, -1, 100, 200));
    }


}
