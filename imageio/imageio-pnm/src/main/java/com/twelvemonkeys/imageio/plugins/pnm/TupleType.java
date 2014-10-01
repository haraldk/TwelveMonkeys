package com.twelvemonkeys.imageio.plugins.pnm;

import java.awt.Transparency;

enum TupleType {
    // Official:
    /** B/W, but uses 1 byte (8 bits) per pixel. Black is zero (oposite of PBM) */
    BLACKANDWHITE(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE),
    /** B/W + bit mask, uses 2 bytes per pixel. Black is zero (oposite of PBM) */
    BLACKANDWHITE_ALPHA(2, PNM.MAX_VAL_1BIT, PNM.MAX_VAL_1BIT, Transparency.BITMASK),
    /** Grayscale, as PGM. */
    GRAYSCALE(1, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** Grayscale + alpha. YA order. */
    GRAYSCALE_ALPHA(2, 2, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),
    /** RGB color, as PPM. RGB order. */
    RGB(3, 1, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** RGB color + alpha. RGBA order. */
    RGB_ALPHA(4, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // De facto (documented on the interwebs):
    /** CMYK color. CMYK order. */
    CMYK(4, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** CMYK color + alpha. CMYKA order. */
    CMYK_ALPHA(5, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // Custom for PBM compatibility
    /** 1 bit B/W. White is zero (as PBM) */
    BLACKANDWHITE_WHITE_IS_ZERO(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE);

    private final int samplesPerPixel;
    private final int minMaxSample;
    private final int maxMaxSample;
    private final int transparency;

    TupleType(int samplesPerPixel, int minMaxSample, int maxMaxSample, int transparency) {
        this.samplesPerPixel = samplesPerPixel;
        this.minMaxSample = minMaxSample;
        this.maxMaxSample = maxMaxSample;
        this.transparency = transparency;
    }

    public int getTransparency() {
        return transparency;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

    public boolean isValidMaxSample(int maxSample) {
        return maxSample >= minMaxSample && maxSample <= maxMaxSample;
    }

}
