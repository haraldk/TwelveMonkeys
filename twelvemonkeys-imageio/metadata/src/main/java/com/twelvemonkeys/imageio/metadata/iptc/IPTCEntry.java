package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

/**
* IPTCEntry
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: IPTCEntry.java,v 1.0 Nov 13, 2009 8:57:04 PM haraldk Exp$
*/
class IPTCEntry extends AbstractEntry {
    public IPTCEntry(final int pTagId, final Object pValue) {
        super(pTagId, pValue);
    }

    @Override
    public String getFieldName() {
        switch ((Integer) getIdentifier()) {
            case IPTC.TAG_SOURCE:
                return "Source";
            // TODO: More tags...
        }

        return null;
    }
}
