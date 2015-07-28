package com.twelvemonkeys.imageio.plugins.hdr;

/**
 * HDR.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDR.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
interface HDR {
    byte[] RADIANCE_MAGIC = new byte[] {'#', '?', 'R', 'A', 'D', 'I', 'A', 'N', 'C', 'E'};
    byte[] RGBE_MAGIC = new byte[] {'#', '?', 'R', 'G', 'B', 'E'};
}
