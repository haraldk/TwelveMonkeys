package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.Entry;

import java.util.List;

/**
* XMPDirectory
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: XMPDirectory.java,v 1.0 Nov 17, 2009 9:38:58 PM haraldk Exp$
*/
final class XMPDirectory extends AbstractDirectory {
    // TODO: Store size of root directory, to allow serializing
    // TODO: XMPDirectory, maybe not even an AbstractDirectory
    //       - Keeping the Document would allow for easier serialization
    // TODO: Or use direct SAX parsing
    public XMPDirectory(List<Entry> pEntries) {
        super(pEntries);
    }
}
