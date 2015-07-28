package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.plugins.hdr.tonemap.DefaultToneMapper;
import com.twelvemonkeys.imageio.plugins.hdr.tonemap.ToneMapper;

import javax.imageio.ImageReadParam;

/**
 * HDRImageReadParam.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRImageReadParam.java,v 1.0 28/07/15 harald.kuhr Exp$
 */
public final class HDRImageReadParam extends ImageReadParam {
    static final ToneMapper DEFAULT_TONE_MAPPER = new DefaultToneMapper(.1f);

    private ToneMapper toneMapper = DEFAULT_TONE_MAPPER;

    public ToneMapper getToneMapper() {
        return toneMapper;
    }

    public void setToneMapper(final ToneMapper toneMapper) {
        this.toneMapper = toneMapper != null ? toneMapper : DEFAULT_TONE_MAPPER;
    }
}
