package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

/**
 * GammaToneMapper.
 * <p/>
 * Normalizes values to range [0...1] using:
 *
 * <p><em>V<sub>out</sub> = A V<sub>in</sub><sup>\u03b3</sup></em></p>
 *
 * Where <em>A</em> is constant and <em>\u03b3</em> is the gamma.
 * Values > 1 are clamped.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: GammaToneMapper.java,v 1.0 28/07/15 harald.kuhr Exp$
 */
public final class GammaToneMapper implements ToneMapper {

    private final float constant;
    private final float gamma;

    public GammaToneMapper() {
        this(0.5f, .25f);
    }

    public GammaToneMapper(final float constant, final float gamma) {
        this.constant = constant;
        this.gamma = gamma;
    }

    @Override
    public void map(final float[] rgb) {
        // Gamma Vo = A * Vi^y
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = Math.min(1f, (float) (constant * Math.pow(rgb[i], gamma)));
        }
    }
}
