package com.twelvemonkeys.imageio.metadata;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * MetadataReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MetadataReader.java,v 1.0 Nov 13, 2009 8:38:11 PM haraldk Exp$
 */
public abstract class MetadataReader {
    public abstract Directory read(ImageInputStream pInput) throws IOException;
}
