package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

/**
 * DefaultToneMapper.
 * <p/>
 * Normalizes values to range [0...1] using:
 *
 * <p><em>V<sub>out</sub> =  V<sub>in</sub> / (V<sub>in</sub> + C)</em></p>
 *
 * Where <em>C</em> is constant.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: DefaultToneMapper.java,v 1.0 28/07/15 harald.kuhr Exp$
 */
public final class DefaultToneMapper implements ToneMapper {

    private final float constant;

    public DefaultToneMapper() {
        this(1);
    }

    public DefaultToneMapper(final float constant) {
        this.constant = constant;
    }

    @Override
    public void map(final float[] rgb) {
        // Default Vo =  Vi / (Vi + 1)
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = rgb[i] / (rgb[i] + constant);
        }
    }
}
