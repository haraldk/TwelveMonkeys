package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

/**
* XMPEntry
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: XMPEntry.java,v 1.0 Nov 17, 2009 9:38:39 PM haraldk Exp$
*/
final class XMPEntry extends AbstractEntry {
    private final String mFieldName;

    public XMPEntry(final String pIdentifier, final Object pValue) {
        this(pIdentifier, null, pValue);
    }

    public XMPEntry(final String pIdentifier, final String pFieldName, final Object pValue) {
        super(pIdentifier, pValue);
        mFieldName = pFieldName;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public String getFieldName() {
        return mFieldName != null ? mFieldName : XMP.DEFAULT_NS_MAPPING.get(getIdentifier());
    }
}
