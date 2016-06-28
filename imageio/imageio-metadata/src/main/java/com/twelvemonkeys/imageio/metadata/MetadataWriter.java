package com.twelvemonkeys.imageio.metadata;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * MetadataWriter.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: MetadataWriter.java,v 1.0 28/05/15 harald.kuhr Exp$
 */
public abstract class MetadataWriter {
    abstract public boolean write(Directory directory, ImageOutputStream stream) throws IOException;
}
