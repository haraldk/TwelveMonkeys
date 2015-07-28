package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

/**
 * ToneMapper.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ToneMapper.java,v 1.0 28/07/15 harald.kuhr Exp$
 */
public interface ToneMapper {
    void map(float[] rgb);
}
