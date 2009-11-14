package com.twelvemonkeys.imageio.metadata.exif;

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.Entry;

import java.util.Collection;
import java.util.List;

/**
 * EXIFDirectory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFDirectory.java,v 1.0 Nov 11, 2009 5:02:59 PM haraldk Exp$
 */
final class EXIFDirectory extends AbstractDirectory {
    EXIFDirectory(final Collection<? extends Entry> pEntries) {
        super(pEntries);
    }
}
