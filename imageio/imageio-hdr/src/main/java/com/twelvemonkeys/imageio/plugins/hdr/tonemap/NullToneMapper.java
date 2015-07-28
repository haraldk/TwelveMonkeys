package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

/**
 * NullToneMapper.
 * <p/>
 * This {@code ToneMapper} does *not* normalize or clamp values
 * to range [0...1], but leaves the values as-is.
 * Useful for applications that implements custom tone mapping.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: NullToneMapper.java,v 1.0 28/07/15 harald.kuhr Exp$
 */
public final class NullToneMapper implements ToneMapper {
    @Override
    public void map(float[] rgb) {
    }
}
